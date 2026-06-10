import Foundation
import onnxruntime
import shared

final class NeuralPitchSupervisor: @unchecked Sendable {
    static let minFreqHz: Double = 46.875
    static let maxFreqHz: Double = 2093.75
    static let minConfidence: Double = 0.50

    static func isValidEstimate(frequencyHz: Double, confidence: Double) -> Bool {
        frequencyHz >= minFreqHz && frequencyHz <= maxFreqHz && confidence >= minConfidence
    }

    private let api: UnsafePointer<OrtApi>
    private let session: OpaquePointer
    private let env: OpaquePointer
    private let inputName: String

    init?() {
        guard let modelPath = Bundle.main.path(forResource: "swift_f0_model", ofType: "onnx") else {
            return nil
        }

        guard let apiBase = OrtGetApiBase() else { return nil }
        guard let apiPtr = apiBase.pointee.GetApi(UInt32(ORT_API_VERSION)) else { return nil }
        self.api = apiPtr

        var envPtr: OpaquePointer?
        let envStatus = api.pointee.CreateEnv(ORT_LOGGING_LEVEL_WARNING, "onnx", &envPtr)
        guard envStatus == nil, let envPtr = envPtr else {
            if let s = envStatus { api.pointee.ReleaseStatus(s) }
            return nil
        }
        self.env = envPtr

        var optionsPtr: OpaquePointer?
        let optStatus = api.pointee.CreateSessionOptions(&optionsPtr)
        guard optStatus == nil, let optionsPtr = optionsPtr else {
            if let s = optStatus { api.pointee.ReleaseStatus(s) }
            api.pointee.ReleaseEnv(env)
            return nil
        }
        api.pointee.SetInterOpNumThreads(optionsPtr, 1)
        api.pointee.SetIntraOpNumThreads(optionsPtr, 1)

        var sessionPtr: OpaquePointer?
        let sessStatus = api.pointee.CreateSession(env, modelPath, optionsPtr, &sessionPtr)
        api.pointee.ReleaseSessionOptions(optionsPtr)
        guard sessStatus == nil, let sessionPtr = sessionPtr else {
            if let s = sessStatus { api.pointee.ReleaseStatus(s) }
            api.pointee.ReleaseEnv(env)
            return nil
        }
        self.session = sessionPtr

        // Get input name
        var allocator: UnsafeMutablePointer<OrtAllocator>?
        api.pointee.GetAllocatorWithDefaultOptions(&allocator)
        guard let allocator = allocator else {
            api.pointee.ReleaseSession(session)
            api.pointee.ReleaseEnv(env)
            return nil
        }

        var namePtr: UnsafeMutablePointer<CChar>?
        let nameStatus = api.pointee.SessionGetInputName(session, 0, allocator, &namePtr)
        guard nameStatus == nil, let namePtr = namePtr else {
            if let s = nameStatus { api.pointee.ReleaseStatus(s) }
            api.pointee.ReleaseSession(session)
            api.pointee.ReleaseEnv(env)
            return nil
        }
        self.inputName = String(cString: namePtr)
        api.pointee.AllocatorFree(allocator, namePtr)
    }

    deinit {
        api.pointee.ReleaseSession(session)
        api.pointee.ReleaseEnv(env)
    }

    func estimate(_ samples44k: [Float]) -> NeuralPitchResult? {
        // Downsample via KMP AudioResampler
        let kotlinInput = KotlinFloatArray(size: Int32(samples44k.count))
        for i in 0..<samples44k.count {
            kotlinInput.set(index: Int32(i), value: samples44k[i])
        }

        let kotlinOutput = AudioResampler.shared.downsample44kTo16k(input: kotlinInput)
        let numSamples = Int(kotlinOutput.size)
        if numSamples == 0 { return nil }

        var samples16k = [Float](repeating: 0, count: numSamples)
        for i in 0..<numSamples {
            samples16k[i] = kotlinOutput.get(index: Int32(i))
        }

        // Create memory info
        var memInfo: OpaquePointer?
        let memStatus = api.pointee.CreateCpuMemoryInfo(OrtArenaAllocator, OrtMemTypeDefault, &memInfo)
        guard memStatus == nil, let memInfo = memInfo else {
            if let s = memStatus { api.pointee.ReleaseStatus(s) }
            return nil
        }
        defer { api.pointee.ReleaseMemoryInfo(memInfo) }

        // Create input tensor and run inference inside buffer scope so
        // samples16k stays alive for the full lifetime of the OrtValue.
        return samples16k.withUnsafeMutableBufferPointer { buf -> NeuralPitchResult? in
            var inputShape: [Int64] = [1, Int64(numSamples)]
            var inputTensor: OpaquePointer?
            let tensorStatus = api.pointee.CreateTensorWithDataAsOrtValue(
                memInfo,
                buf.baseAddress,
                numSamples * MemoryLayout<Float>.size,
                &inputShape,
                2,
                ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT,
                &inputTensor
            )
            guard tensorStatus == nil, let inputTensor = inputTensor else {
                if let s = tensorStatus { api.pointee.ReleaseStatus(s) }
                return nil
            }
            defer { api.pointee.ReleaseValue(inputTensor) }

            // Get output count
            var outputCount: Int = 0
            let countStatus = api.pointee.SessionGetOutputCount(session, &outputCount)
            if let s = countStatus { api.pointee.ReleaseStatus(s); return nil }
            let actualOutputCount = min(outputCount, 2)

            // Get allocator for output names
            var allocator: UnsafeMutablePointer<OrtAllocator>?
            api.pointee.GetAllocatorWithDefaultOptions(&allocator)
            guard let allocator = allocator else { return nil }

            // Get output names
            var outputNamePtrs: [UnsafeMutablePointer<CChar>?] = []
            var outputNameCPtrs: [UnsafePointer<CChar>?] = []
            for i in 0..<actualOutputCount {
                var namePtr: UnsafeMutablePointer<CChar>?
                let nameStatus = api.pointee.SessionGetOutputName(session, i, allocator, &namePtr)
                if let s = nameStatus {
                    api.pointee.ReleaseStatus(s)
                    for ptr in outputNamePtrs {
                        if let ptr = ptr { api.pointee.AllocatorFree(allocator, ptr) }
                    }
                    return nil
                }
                outputNamePtrs.append(namePtr)
                outputNameCPtrs.append(namePtr.map { UnsafePointer($0) })
            }
            defer {
                for ptr in outputNamePtrs {
                    if let ptr = ptr { api.pointee.AllocatorFree(allocator, ptr) }
                }
            }

            // Run inference
            var inputNameCPtrs: [UnsafePointer<CChar>?] = [inputName.withCString { UnsafePointer(strdup($0)) }]
            defer { inputNameCPtrs.compactMap { $0 }.forEach { free(UnsafeMutablePointer(mutating: $0)) } }

            var inputValues: [OpaquePointer?] = [inputTensor]
            var outputValues = [OpaquePointer?](repeating: nil, count: actualOutputCount)

            let runStatus = api.pointee.Run(
                session,
                nil,
                &inputNameCPtrs,
                &inputValues,
                1,
                &outputNameCPtrs,
                actualOutputCount,
                &outputValues
            )
            guard runStatus == nil else {
                if let s = runStatus { api.pointee.ReleaseStatus(s) }
                return nil
            }
            defer {
                for val in outputValues {
                    if let val = val { api.pointee.ReleaseValue(val) }
                }
            }

            return extractMiddleEstimate(outputValues: outputValues, outputCount: actualOutputCount)
        }
    }

    private func extractMiddleEstimate(outputValues: [OpaquePointer?], outputCount: Int) -> NeuralPitchResult? {
        guard outputCount > 0, let pitchValue = outputValues[0] else { return nil }

        guard let pitchArray = extractFloatArray(pitchValue) else { return nil }
        if pitchArray.isEmpty { return nil }

        let confidenceArray: [Float]?
        if outputCount >= 2, let confValue = outputValues[1] {
            confidenceArray = extractFloatArray(confValue)
        } else {
            confidenceArray = nil
        }

        let idx = pitchArray.count / 2
        let freq = Double(pitchArray[idx])
        let conf: Double
        if let ca = confidenceArray, !ca.isEmpty {
            let cIdx = min(idx, ca.count - 1)
            conf = Double(ca[max(0, cIdx)])
        } else {
            conf = 0.0
        }

        guard freq >= Self.minFreqHz && freq <= Self.maxFreqHz else { return nil }
        guard conf >= Self.minConfidence else { return nil }

        return NeuralPitchResult(frequencyHz: freq, confidence: conf)
    }

    private func extractFloatArray(_ value: OpaquePointer) -> [Float]? {
        var dataPtr: UnsafeMutableRawPointer?
        let status = api.pointee.GetTensorMutableData(value, &dataPtr)
        guard status == nil, let dataPtr = dataPtr else {
            if let s = status { api.pointee.ReleaseStatus(s) }
            return nil
        }

        var typeInfo: OpaquePointer?
        api.pointee.GetTensorTypeAndShape(value, &typeInfo)
        guard let typeInfo = typeInfo else { return nil }
        defer { api.pointee.ReleaseTensorTypeAndShapeInfo(typeInfo) }

        var elementCount: Int = 0
        api.pointee.GetTensorShapeElementCount(typeInfo, &elementCount)
        if elementCount == 0 { return nil }

        let floatPtr = dataPtr.assumingMemoryBound(to: Float.self)
        return Array(UnsafeBufferPointer(start: floatPtr, count: elementCount))
    }
}
