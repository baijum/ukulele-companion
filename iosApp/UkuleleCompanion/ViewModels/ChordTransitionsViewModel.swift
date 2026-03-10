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
        let rootName = String(name.prefix(while: { $0.isLetter || $0 == "#" || $0 == "b" }))
        let quality = String(name.dropFirst(rootName.count))
        let noteMap: [String: Int32] = [
            "C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3,
            "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7, "G#": 8,
            "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11
        ]
        let rootPc = noteMap[rootName] ?? 0

        let tuning = (0..<4).map { i -> shared.UkuleleString in
            let t = UkuleleTuning.highG
            return shared.UkuleleString(
                name: t.stringNames[i] as! String,
                openPitchClass: (t.pitchClasses[i] as! NSNumber).int32Value,
                octave: (t.octaves[i] as! NSNumber).int32Value
            )
        }

        let formulas = ChordFormulas.shared.ALL as! [ChordFormula]
        let formula = formulas.first { $0.symbol == quality } ?? formulas.first { $0.symbol == "" }!
        let voicings = VoicingGenerator.shared.generate(
            rootPitchClass: rootPc,
            formula: formula,
            tuning: tuning,
            allowMutedStrings: false
        ) as! [ChordVoicing]

        if let voicing = voicings.first {
            let fretList = voicing.fretInts
            let pitchClasses = (0..<fretList.count).compactMap { i -> Int32? in
                let fret = fretList[i]
                guard fret >= 0 else { return nil }
                let openPc = (UkuleleTuning.highG.pitchClasses[i] as! NSNumber).int32Value
                return (openPc + Int32(fret)) % 12
            }
            tonePlayer.playChord(pitchClasses: pitchClasses, strumDelayMs: 40)
        }
    }

    func voicingForChord(name: String) -> ChordVoicing? {
        let rootName = String(name.prefix(while: { $0.isLetter || $0 == "#" || $0 == "b" }))
        let quality = String(name.dropFirst(rootName.count))
        let noteMap: [String: Int32] = [
            "C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3,
            "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7, "G#": 8,
            "Ab": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11
        ]
        let rootPc = noteMap[rootName] ?? 0

        let tuning = (0..<4).map { i -> shared.UkuleleString in
            let t = UkuleleTuning.highG
            return shared.UkuleleString(
                name: t.stringNames[i] as! String,
                openPitchClass: (t.pitchClasses[i] as! NSNumber).int32Value,
                octave: (t.octaves[i] as! NSNumber).int32Value
            )
        }

        let formulas = ChordFormulas.shared.ALL as! [ChordFormula]
        let formula = formulas.first { $0.symbol == quality } ?? formulas.first { $0.symbol == "" }!
        let voicings = VoicingGenerator.shared.generate(
            rootPitchClass: rootPc,
            formula: formula,
            tuning: tuning,
            allowMutedStrings: false
        ) as! [ChordVoicing]

        return voicings.first
    }
}
