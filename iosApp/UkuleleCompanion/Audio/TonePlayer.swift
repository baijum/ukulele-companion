import AVFoundation

/// Plays bundled WAV audio samples for reference tones, chords, and metronome clicks.
///
/// Supports polyphonic playback via a pool of `AVAudioPlayerNode` instances,
/// allowing strummed chords where multiple notes overlap with configurable delay.
final class TonePlayer {
    private var audioEngine: AVAudioEngine?
    private var playerNodes: [AVAudioPlayerNode] = []
    private var cachedBuffers: [String: AVAudioPCMBuffer] = [:]
    private let nodePoolSize = 8
    private var nextNodeIndex = 0

    /// Pitch class (0=C … 11=B) to WAV sample name.
    static let pitchClassToSample: [Int32: String] = [
        0: "uke_c", 1: "uke_csharp", 2: "uke_d", 3: "uke_dsharp",
        4: "uke_e", 5: "uke_f", 6: "uke_fsharp", 7: "uke_g",
        8: "uke_gsharp", 9: "uke_a", 10: "uke_asharp", 11: "uke_b",
    ]

    /// All available sample names (without .wav extension).
    static let noteSamples = [
        "uke_a", "uke_asharp", "uke_b", "uke_c", "uke_csharp",
        "uke_d", "uke_dsharp", "uke_e", "uke_f", "uke_fsharp",
        "uke_g", "uke_gsharp"
    ]

    static let clickSamples = ["click_high", "click_low"]

    init() {
        setupEngine()
    }

    private func setupEngine() {
        let engine = AVAudioEngine()
        let sampleFormat = AVAudioFormat(standardFormatWithSampleRate: 44100, channels: 1)!

        for _ in 0..<nodePoolSize {
            let player = AVAudioPlayerNode()
            engine.attach(player)
            engine.connect(player, to: engine.mainMixerNode, format: sampleFormat)
            playerNodes.append(player)
        }

        do {
            try engine.start()
            self.audioEngine = engine
        } catch {
            print("TonePlayer engine failed to start: \(error)")
        }
    }

    private func nextPlayer() -> AVAudioPlayerNode? {
        guard !playerNodes.isEmpty else { return nil }
        let player = playerNodes[nextNodeIndex % nodePoolSize]
        nextNodeIndex += 1
        return player
    }

    /// Plays the named WAV sample from the app bundle on a single player node.
    func play(_ sampleName: String) {
        guard let player = nextPlayer(), let engine = audioEngine else { return }

        if !engine.isRunning {
            try? engine.start()
        }

        if let buffer = loadBuffer(named: sampleName) {
            player.stop()
            player.scheduleBuffer(buffer, at: nil, options: .interrupts)
            player.play()
        }
    }

    /// Plays a single note by pitch class using the corresponding WAV sample.
    func playNote(pitchClass: Int32) {
        if let sample = Self.pitchClassToSample[pitchClass] {
            play(sample)
        }
    }

    /// Plays a chord as a strum: notes are triggered with a configurable delay
    /// between successive strings, using separate player nodes for polyphony.
    ///
    /// - Parameters:
    ///   - pitchClasses: Pitch classes in strum order (sorted by string direction).
    ///   - strumDelayMs: Delay between successive notes in milliseconds.
    ///   - volume: Playback volume (0…1). Currently informational; AVAudioEngine
    ///     volume could be applied per-node if needed.
    func playChord(pitchClasses: [Int32], strumDelayMs: Int = 50, volume: Float = 1.0) {
        guard let engine = audioEngine, !pitchClasses.isEmpty else { return }

        if !engine.isRunning {
            try? engine.start()
        }

        let vol = min(max(volume, 0), 1)

        for (index, pc) in pitchClasses.enumerated() {
            guard let sample = Self.pitchClassToSample[pc],
                  let buffer = loadBuffer(named: sample),
                  let player = nextPlayer() else { continue }

            player.volume = vol

            let delaySeconds = Double(index) * Double(strumDelayMs) / 1000.0

            if delaySeconds > 0 {
                nonisolated(unsafe) let weakSelf = self
                nonisolated(unsafe) let b = buffer
                let nodeIndex = (nextNodeIndex - 1) % nodePoolSize
                DispatchQueue.main.asyncAfter(deadline: .now() + delaySeconds) {
                    guard let engine = weakSelf.audioEngine, engine.isRunning else { return }
                    let p = weakSelf.playerNodes[nodeIndex]
                    p.stop()
                    p.scheduleBuffer(b, at: nil, options: .interrupts)
                    p.play()
                }
            } else {
                player.stop()
                player.scheduleBuffer(buffer, at: nil, options: .interrupts)
                player.play()
            }
        }
    }

    func stop() {
        for player in playerNodes {
            player.stop()
        }
    }

    private func loadBuffer(named name: String) -> AVAudioPCMBuffer? {
        if let cached = cachedBuffers[name] {
            return cached
        }

        guard let url = Bundle.main.url(forResource: name, withExtension: "wav") else {
            print("WAV file not found: \(name).wav")
            return nil
        }

        guard let file = try? AVAudioFile(forReading: url) else {
            print("Failed to read WAV file: \(name).wav")
            return nil
        }

        let frameCount = AVAudioFrameCount(file.length)
        guard let buffer = AVAudioPCMBuffer(pcmFormat: file.processingFormat, frameCapacity: frameCount) else {
            return nil
        }

        do {
            try file.read(into: buffer)
            cachedBuffers[name] = buffer
            return buffer
        } catch {
            print("Failed to read audio data: \(error)")
            return nil
        }
    }

    deinit {
        audioEngine?.stop()
    }
}
