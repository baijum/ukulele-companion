import Foundation
import Combine
import shared

@MainActor
final class ChordTransitionsViewModel: ObservableObject {
    @Published var chord1: String = "C"
    @Published var chord2: String = "G"
    @Published var bpm: Double = 80
    @Published var beatsPerChord: Int = 4
    @Published var isRunning = false
    @Published var transitionCount: Int = 0
    @Published var currentBeat: Int = 0
    @Published var currentChordIsFirst = true
    @Published var startTime: Date?
    @Published var elapsedSeconds: Int = 0
    @Published var playChordOnTransition = true

    let tonePlayer = TonePlayer()
    private var metronomeTimer: AnyCancellable?
    private var clockTimer: AnyCancellable?
    private var tapTimestamps: [Date] = []

    // Driven from settings so diagrams and playback honour the selected tuning
    // (#576) and the Allow Muted Strings setting (#593).
    private var tuning: [shared.UkuleleString] = UkuleleTuning.highG.asUkuleleStrings
    private var allowMutedStrings: Bool = false

    /// Applies the current tuning and muted-strings settings.
    func applySettings(tuning: [shared.UkuleleString], allowMuted: Bool) {
        self.tuning = tuning
        self.allowMutedStrings = allowMuted
    }

    var currentChord: String {
        currentChordIsFirst ? chord1 : chord2
    }

    var switchesPerMinute: Double {
        guard elapsedSeconds > 0, transitionCount > 0 else { return 0 }
        return Double(transitionCount) / (Double(elapsedSeconds) / 60.0)
    }

    func start() {
        guard !isRunning else { return }
        isRunning = true
        transitionCount = 0
        currentBeat = 0
        currentChordIsFirst = true
        startTime = Date()
        elapsedSeconds = 0

        if playChordOnTransition {
            playChord(name: currentChord)
        }

        clockTimer = Timer.publish(every: 1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self, let start = self.startTime else { return }
                self.elapsedSeconds = Int(Date().timeIntervalSince(start))
            }

        metronomeTimer = Timer.publish(every: 60.0 / bpm, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self else { return }
                self.currentBeat += 1
                if self.currentBeat == 1 {
                    self.tonePlayer.play("click_high")
                } else {
                    self.tonePlayer.play("click_low")
                }
                if self.currentBeat >= self.beatsPerChord {
                    self.currentBeat = 0
                    self.currentChordIsFirst.toggle()
                    self.transitionCount += 1
                    if self.playChordOnTransition {
                        self.playChord(name: self.currentChord)
                    }
                }
            }
    }

    func stop() {
        isRunning = false
        metronomeTimer?.cancel()
        metronomeTimer = nil
        clockTimer?.cancel()
        clockTimer = nil
        tonePlayer.stop()
    }

    func reset() {
        stop()
        transitionCount = 0
        currentBeat = 0
        currentChordIsFirst = true
        elapsedSeconds = 0
        startTime = nil
    }

    func tapTempo() {
        let now = Date()
        tapTimestamps.append(now)
        if tapTimestamps.count > 4 { tapTimestamps.removeFirst() }
        guard tapTimestamps.count >= 2 else { return }
        let intervals = zip(tapTimestamps.dropFirst(), tapTimestamps).map { $0.timeIntervalSince($1) }
        let avg = intervals.reduce(0, +) / Double(intervals.count)
        if avg > 0 {
            bpm = min(200, max(40, 60.0 / avg))
        }
    }

    func playChord(name: String) {
        guard let parsed = ChordNameParser.shared.parse(input: name) else { return }
        let rootPc = parsed.rootPitchClass
        let formula = parsed.formula

        let voicings = VoicingGenerator.shared.generate(
            rootPitchClass: Int32(rootPc),
            formula: formula,
            tuning: tuning,
            allowMutedStrings: allowMutedStrings
        ).asArray(of: ChordVoicing.self)

        if let voicing = voicings.first {
            let fretList = voicing.fretInts
            let pitchClasses = (0..<fretList.count).compactMap { i -> Int32? in
                let fret = fretList[i]
                guard fret >= 0 else { return nil }
                let openPc = tuning[i].openPitchClass
                return (openPc + Int32(fret)) % 12
            }
            tonePlayer.playChord(pitchClasses: pitchClasses, strumDelayMs: 40)
        }
    }

    func voicingForChord(name: String) -> ChordVoicing? {
        guard let parsed = ChordNameParser.shared.parse(input: name) else { return nil }
        let rootPc = parsed.rootPitchClass
        let formula = parsed.formula

        let voicings = VoicingGenerator.shared.generate(
            rootPitchClass: Int32(rootPc),
            formula: formula,
            tuning: tuning,
            allowMutedStrings: allowMutedStrings
        ).asArray(of: ChordVoicing.self)

        return voicings.first
    }
}
