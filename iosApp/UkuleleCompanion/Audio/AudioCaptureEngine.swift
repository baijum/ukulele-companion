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

        // Create the format we want: mono Float32 at 44.1kHz
        guard let desiredFormat = AVAudioFormat(
            commonFormat: .pcmFormatFloat32,
            sampleRate: Self.sampleRate,
            channels: 1,
            interleaved: false
        ) else { return }

        // Install a tap - if hardware format differs, AVAudioEngine will convert
        let tapFormat = inputFormat.sampleRate == Self.sampleRate && inputFormat.channelCount == 1
            ? inputFormat
            : desiredFormat

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
        ringBuffer = [Float](repeating: 0, count: Self.frameSize)
        Task { @MainActor in
            self.isCapturing = false
        }
    }

    private func processBuffer(_ buffer: AVAudioPCMBuffer) {
        guard let channelData = buffer.floatChannelData else { return }
        let samples = channelData[0]
        let count = Int(buffer.frameLength)

        for i in 0..<count {
            ringBuffer[writePos] = samples[i]
            writePos = (writePos + 1) % Self.frameSize
        }
        filled += count

        if filled >= Self.frameSize {
            // Linearise the ring buffer
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
