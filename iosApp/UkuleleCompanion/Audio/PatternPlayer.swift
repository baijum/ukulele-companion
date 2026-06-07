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
    private var currentBeats: [StrumBeat] = []
    private var currentSteps: [FingerpickStep] = []
    private var playbackIndex = 0

    private let emphasisVolume: Float = 1.0
    private let normalVolume: Float = 0.7
    private let strumDelayMs = 30

    private var openPitchClasses: [Int32] {
        let t = UkuleleTuning.highG
        return (0..<4).map { (t.pitchClasses[$0] as NSNumber).int32Value }
    }

    func playStrum(pattern: StrumPattern, bpm: Int) {
        stop()
        currentBeats = pattern.beats as [StrumBeat]
        guard !currentBeats.isEmpty else { return }

        let beatInterval = 60.0 / Double(max(30, min(bpm, 300)))
        playbackIndex = 0
        isPlaying = true

        timer = Timer.scheduledTimer(withTimeInterval: beatInterval, repeats: true) { [weak self] _ in
            Task { @MainActor in
                guard let self, self.isPlaying else { return }
                let beat = self.currentBeats[self.playbackIndex]
                self.currentIndex = self.playbackIndex
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

                self.playbackIndex = (self.playbackIndex + 1) % self.currentBeats.count
            }
        }
    }

    func playFingerpick(pattern: FingerpickingPattern, bpm: Int) {
        stop()
        currentSteps = pattern.steps as [FingerpickStep]
        guard !currentSteps.isEmpty else { return }

        let stepInterval = 60.0 / Double(max(30, min(bpm, 300)))
        playbackIndex = 0
        isPlaying = true

        timer = Timer.scheduledTimer(withTimeInterval: stepInterval, repeats: true) { [weak self] _ in
            Task { @MainActor in
                guard let self, self.isPlaying else { return }
                let step = self.currentSteps[self.playbackIndex]
                self.currentIndex = self.playbackIndex

                let pc = self.openPitchClasses[Int(step.stringIndex)]
                self.tonePlayer.playNote(pitchClass: pc)

                self.playbackIndex = (self.playbackIndex + 1) % self.currentSteps.count
            }
        }
    }

    func stop() {
        timer?.invalidate()
        timer = nil
        isPlaying = false
        currentIndex = -1
        playbackIndex = 0
        currentBeats = []
        currentSteps = []
    }

}
