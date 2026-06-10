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
    private let sessionQueue = DispatchQueue(label: "com.baijum.ukufretboard.audiocapture")
    private let bufferLock = NSLock()
    private var _isRunning = false  // only accessed on sessionQueue
    private var hardwareSampleRate: Double = sampleRate
    private var resampleAccumulator: Double = 0.0
    private var lastResampleSample: Float = 0
    private var hasLastResampleSample = false

    /// Ring buffer holding the most recent `frameSize` samples.
    private var ringBuffer: [Float]
    /// Reusable linear buffer passed to `onBuffer`. Callers must not hold a
    /// reference beyond the callback invocation.
    private var analysisBuf: [Float]
    private var writePos = 0
    private var filled = 0

    /// Called on a background queue with each analysis frame.
    var onBuffer: (([Float]) -> Void)?

    /// Called on the main actor when the system interrupts capture (phone call, Siri, etc.).
    var onInterrupted: (() -> Void)?
    private var interruptionObserver: NSObjectProtocol?

    init() {
        ringBuffer = [Float](repeating: 0, count: Self.frameSize)
        analysisBuf = [Float](repeating: 0, count: Self.frameSize)
    }

    func start() {
        // `return` inside sessionQueue.sync exits the closure, not start().
        // `_isRunning` is set inside the queue; `started` carries the result out.
        let started: Bool = sessionQueue.sync {
            guard !_isRunning else { return false }

            #if !targetEnvironment(simulator)
            let session = AVAudioSession.sharedInstance()
            do {
                try session.setCategory(.playAndRecord, mode: .measurement,
                                        options: [.defaultToSpeaker, .allowBluetooth])
                try session.setActive(true)
            } catch {
                print("AudioCaptureEngine: failed to configure recording session: \(error)")
                return false
            }
            #endif

            let inputNode = engine.inputNode
            let inputFormat = inputNode.inputFormat(forBus: 0)
            guard inputFormat.channelCount > 0, inputFormat.sampleRate > 0 else {
                print("AudioCaptureEngine: invalid input format (channels=\(inputFormat.channelCount), rate=\(inputFormat.sampleRate))")
                deactivateRecordingSession()
                return false
            }
            hardwareSampleRate = inputFormat.sampleRate
            resampleAccumulator = 0.0

            // Tap MUST use hardware sample rate; AVAudioEngine cannot resample on input taps.
            // Use mono Float32 at the hardware rate, then resample to 44.1kHz in processBuffer.
            guard let tapFormat = AVAudioFormat(
                commonFormat: .pcmFormatFloat32,
                sampleRate: hardwareSampleRate,
                channels: 1,
                interleaved: false
            ) else {
                deactivateRecordingSession()
                return false
            }

            inputNode.installTap(onBus: 0, bufferSize: AVAudioFrameCount(Self.hopSize), format: tapFormat) { [weak self] buffer, _ in
                self?.processBuffer(buffer)
            }

            do {
                try engine.start()
                _isRunning = true
                #if !targetEnvironment(simulator)
                interruptionObserver = NotificationCenter.default.addObserver(
                    forName: AVAudioSession.interruptionNotification,
                    object: AVAudioSession.sharedInstance(),
                    queue: nil
                ) { [weak self] notification in
                    self?.handleInterruption(notification)
                }
                #endif
                return true
            } catch {
                print("AudioCaptureEngine failed to start: \(error)")
                inputNode.removeTap(onBus: 0)
                deactivateRecordingSession()
                return false
            }
        }
        if started {
            Task { @MainActor [weak self] in
                self?.isCapturing = true
            }
        }
    }

    private func tearDown() {
        sessionQueue.sync {
            guard _isRunning else { return }

            if let observer = interruptionObserver {
                NotificationCenter.default.removeObserver(observer)
                interruptionObserver = nil
            }

            engine.inputNode.removeTap(onBus: 0)
            engine.stop()
            _isRunning = false

            deactivateRecordingSession()

            bufferLock.lock()
            writePos = 0
            filled = 0
            resampleAccumulator = 0.0
            lastResampleSample = 0
            hasLastResampleSample = false
            ringBuffer = [Float](repeating: 0, count: Self.frameSize)
            analysisBuf = [Float](repeating: 0, count: Self.frameSize)
            bufferLock.unlock()
        }
    }

    func stop() {
        tearDown()
        Task { @MainActor [weak self] in
            self?.isCapturing = false
        }
    }

    private func processBuffer(_ buffer: AVAudioPCMBuffer) {
        guard let channelData = buffer.floatChannelData else { return }
        let samples = channelData[0]
        let count = Int(buffer.frameLength)

        bufferLock.lock()
        defer { bufferLock.unlock() }

        let needsResample = abs(hardwareSampleRate - Self.sampleRate) > 1.0

        if needsResample {
            let step = hardwareSampleRate / Self.sampleRate
            var srcPos = resampleAccumulator
            while srcPos < Double(count - 1) {
                let interpolated: Float
                if srcPos < 0 {
                    guard hasLastResampleSample else { srcPos = 0; continue }
                    let frac = Float(srcPos + 1)
                    interpolated = lastResampleSample + frac * (samples[0] - lastResampleSample)
                } else {
                    let idx = Int(srcPos)
                    let frac = Float(srcPos - Double(idx))
                    interpolated = samples[idx] + frac * (samples[idx + 1] - samples[idx])
                }
                ringBuffer[writePos] = interpolated
                writePos = (writePos + 1) % Self.frameSize
                filled += 1
                srcPos += step
            }
            resampleAccumulator = srcPos - Double(count)
            lastResampleSample = samples[count - 1]
            hasLastResampleSample = true
        } else {
            for i in 0..<count {
                ringBuffer[writePos] = samples[i]
                writePos = (writePos + 1) % Self.frameSize
            }
            filled += count
        }

        if filled >= Self.frameSize {
            for i in 0..<Self.frameSize {
                analysisBuf[i] = ringBuffer[(writePos + i) % Self.frameSize]
            }
            onBuffer?(analysisBuf)
            filled -= Self.hopSize
        }
    }

    private func handleInterruption(_ notification: Notification) {
        guard let info = notification.userInfo,
              let typeValue = info[AVAudioSessionInterruptionTypeKey] as? UInt,
              let type = AVAudioSession.InterruptionType(rawValue: typeValue) else { return }

        if type == .began {
            let callback = onInterrupted
            stop()
            Task { @MainActor in
                callback?()
            }
        }
    }

    /// Deactivates the recording audio session and restores playback-only mode.
    private func deactivateRecordingSession() {
        #if !targetEnvironment(simulator)
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setActive(false, options: .notifyOthersOnDeactivation)
        } catch {
            print("AudioCaptureEngine: failed to deactivate audio session: \(error)")
        }
        do {
            try session.setCategory(.playback, mode: .default, options: [.defaultToSpeaker])
            try session.setActive(true)
        } catch {
            print("AudioCaptureEngine: failed to restore playback session: \(error)")
        }
        #endif
    }

    deinit {
        tearDown()
    }
}
