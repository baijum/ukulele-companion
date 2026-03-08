import Foundation
@preconcurrency import shared

/// Plays strumming and fingerpicking patterns using TonePlayer on a timer.
///
/// Only one pattern plays at a time — starting a new pattern stops any
/// previous playback. Open strings of High-G tuning are used as the demo chord.
@MainActor
final class PatternPlayer: ObservableObject {
    @Published var isPlaying = false
    @Published var currentIndex = -1

    private var timer: Timer?
    private let tonePlayer = TonePlayer()

    private let emphasisVolume: Float = 1.0
    private let normalVolume: Float = 0.7
    private let strumDelayMs = 30

    private var openPitchClasses: [Int32] {
        let t = UkuleleTuning.highG
        return (0..<4).map { (t.pitchClasses[$0] as NSNumber).int32Value }
    }

    func playStrum(pattern: StrumPattern, bpm: Int) {
        stop()
        let beats = pattern.beats as [StrumBeat]
        guard !beats.isEmpty else { return }

        let beatInterval = 60.0 / Double(max(30, min(bpm, 300)))
        var index = 0
        isPlaying = true

        timer = Timer.scheduledTimer(withTimeInterval: beatInterval, repeats: true) { [weak self] _ in
            Task { @MainActor in
                guard let self, self.isPlaying else { return }
                let beat = beats[index]
                self.currentIndex = index
                let vol = beat.emphasis ? self.emphasisVolume : self.normalVolume

                switch beat.direction {
                case .down:
                    self.tonePlayer.playChord(pitchClasses: self.openPitchClasses, strumDelayMs: self.strumDelayMs, volume: vol)
                case .up:
                    self.tonePlayer.playChord(pitchClasses: self.openPitchClasses.reversed(), strumDelayMs: self.strumDelayMs, volume: vol)
                case .chuck:
                    self.tonePlayer.playChord(pitchClasses: self.openPitchClasses, strumDelayMs: self.strumDelayMs, volume: vol * 0.5)
                default:
                    break
                }

                index = (index + 1) % beats.count
            }
        }
    }

    func playFingerpick(pattern: FingerpickingPattern, bpm: Int) {
        stop()
        let steps = pattern.steps as [FingerpickStep]
        guard !steps.isEmpty else { return }

        let stepInterval = 60.0 / Double(max(30, min(bpm, 300)))
        var index = 0
        isPlaying = true

        timer = Timer.scheduledTimer(withTimeInterval: stepInterval, repeats: true) { [weak self] _ in
            Task { @MainActor in
                guard let self, self.isPlaying else { return }
                let step = steps[index]
                self.currentIndex = index

                let pc = self.openPitchClasses[Int(step.stringIndex)]
                self.tonePlayer.playNote(pitchClass: pc)

                index = (index + 1) % steps.count
            }
        }
    }

    func stop() {
        timer?.invalidate()
        timer = nil
        isPlaying = false
        currentIndex = -1
    }

}
