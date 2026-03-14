import AVFoundation
import Combine

/// Captures audio from the device microphone using AVAudioEngine.
///
/// Matching Android specs: 44,100 Hz sample rate, 4096-sample frames,
/// 1024-sample hop size, mono Float32.
final class AudioCaptureEngine: ObservableObject, @unchecked Sendable {
    static let sampleRate: Double = 44100
    static let frameSize: Int = 4096
    static let hopSize: Int = 1024

    @Published private(set) var isCapturing = false

    private let engine = AVAudioEngine()
    private var hardwareSampleRate: Double = sampleRate
    private var resampleAccumulator: Double = 0.0

    /// Ring buffer holding the most recent `frameSize` samples.
    private var ringBuffer: [Float]
    private var writePos = 0
    private var filled = 0

    /// Called on a background queue with each analysis frame.
    var onBuffer: (([Float]) -> Void)?

    init() {
        ringBuffer = [Float](repeating: 0, count: Self.frameSize)
    }

    func start() {
        guard !isCapturing else { return }

        let inputNode = engine.inputNode
        let inputFormat = inputNode.inputFormat(forBus: 0)
        hardwareSampleRate = inputFormat.sampleRate
        resampleAccumulator = 0.0

        // Tap MUST use hardware sample rate; AVAudioEngine cannot resample on input taps.
        // Use mono Float32 at the hardware rate, then resample to 44.1kHz in processBuffer.
        guard let tapFormat = AVAudioFormat(
            commonFormat: .pcmFormatFloat32,
            sampleRate: hardwareSampleRate,
            channels: 1,
            interleaved: false
        ) else { return }

        inputNode.installTap(onBus: 0, bufferSize: AVAudioFrameCount(Self.hopSize), format: tapFormat) { [weak self] buffer, _ in
            self?.processBuffer(buffer)
        }

        do {
            try engine.start()
            Task { @MainActor in
                self.isCapturing = true
            }
        } catch {
            print("AudioCaptureEngine failed to start: \(error)")
            inputNode.removeTap(onBus: 0)
        }
    }

    func stop() {
        guard isCapturing else { return }
        engine.inputNode.removeTap(onBus: 0)
        engine.stop()
        writePos = 0
        filled = 0
        resampleAccumulator = 0.0
        ringBuffer = [Float](repeating: 0, count: Self.frameSize)
        Task { @MainActor in
            self.isCapturing = false
        }
    }

    private func processBuffer(_ buffer: AVAudioPCMBuffer) {
        guard let channelData = buffer.floatChannelData else { return }
        let samples = channelData[0]
        let count = Int(buffer.frameLength)

        let needsResample = abs(hardwareSampleRate - Self.sampleRate) > 1.0

        if needsResample {
            // Resample from hardware rate to 44.1kHz via linear interpolation.
            // srcPos tracks fractional position in the input buffer, carried across calls.
            let step = hardwareSampleRate / Self.sampleRate
            var srcPos = resampleAccumulator
            while Int(srcPos) + 1 < count {
                let idx = Int(srcPos)
                let frac = Float(srcPos - Double(idx))
                let s0 = samples[idx]
                let s1 = samples[idx + 1]
                ringBuffer[writePos] = s0 + frac * (s1 - s0)
                writePos = (writePos + 1) % Self.frameSize
                filled += 1
                srcPos += step
            }
            resampleAccumulator = srcPos - Double(count)
            if resampleAccumulator < 0 { resampleAccumulator = 0 }
        } else {
            for i in 0..<count {
                ringBuffer[writePos] = samples[i]
                writePos = (writePos + 1) % Self.frameSize
            }
            filled += count
        }

        if filled >= Self.frameSize {
            var analysisBuf = [Float](repeating: 0, count: Self.frameSize)
            for i in 0..<Self.frameSize {
                analysisBuf[i] = ringBuffer[(writePos + i) % Self.frameSize]
            }
            onBuffer?(analysisBuf)
            filled -= Self.hopSize
        }
    }

    deinit {
        stop()
    }
}
