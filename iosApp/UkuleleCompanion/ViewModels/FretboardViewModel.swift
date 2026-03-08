import Foundation
import shared

/// View model managing fretboard state, chord detection, scale overlay, and capo.
@MainActor
final class FretboardViewModel: ObservableObject {
    static let fretCount = 13 // 0 (open) through 12
    static let stringCount = 4

    @Published var selections: [Int: Int?] = [0: nil, 1: nil, 2: nil, 3: nil]
    @Published var showNoteNames: Bool = true
    @Published private(set) var detectionResult: DetectionResultWrapper = .noSelection

    // MARK: - Capo

    @Published var capoFret: Int = 0

    // MARK: - Scale Overlay

    @Published var scaleOverlay = ScaleOverlayState()

    // MARK: - Sound Settings (set from SettingsViewModel)

    var soundEnabled: Bool = true
    var volume: Float = 1.0
    var noteDurationMs: Int = 600
    var strumDelayMs: Int = 50
    var strumDown: Bool = true
    var playOnTap: Bool = false

    let tonePlayer = TonePlayer()

    var tuning: [shared.UkuleleString] = {
        let t = UkuleleTuning.highG
        return (0..<4).map { i in
            shared.UkuleleString(
                name: t.stringNames[i] as! String,
                openPitchClass: (t.pitchClasses[i] as! NSNumber).int32Value,
                octave: (t.octaves[i] as! NSNumber).int32Value
            )
        }
    }()

    /// Applies current settings from a SettingsViewModel.
    func applySoundSettings(from settings: SettingsViewModel) {
        soundEnabled = settings.soundEnabled
        volume = settings.volume
        noteDurationMs = settings.noteDurationMs
        strumDelayMs = settings.strumDelayMs
        strumDown = settings.strumDown
        playOnTap = settings.playOnTap
    }

    // MARK: - Fret Interaction

    func toggleFret(stringIndex: Int, fret: Int) {
        if capoFret > 0 && fret >= 1 && fret <= capoFret {
            return
        }

        let wasSelected = selections[stringIndex] == fret
        if selections[stringIndex] == fret {
            selections[stringIndex] = nil
        } else {
            selections[stringIndex] = fret
        }
        runDetection()

        if !wasSelected, selections[stringIndex] != nil, soundEnabled {
            playNoteAt(stringIndex: stringIndex, fret: fret)
        }
    }

    func clearAll() {
        selections = [0: nil, 1: nil, 2: nil, 3: nil]
        detectionResult = .noSelection
    }

    func applyVoicing(_ voicing: ChordVoicing, rootPitchClass: Int32? = nil, formula: ChordFormula? = nil) {
        let frets = voicing.frets
        for i in 0..<Self.stringCount {
            let fret = (frets[i] as! NSNumber).intValue
            selections[i] = fret < 0 ? nil : fret
        }

        if let root = rootPitchClass, let formula = formula {
            let rootName = Notes.shared.pitchClassToName(pitchClass: root)
            let chordNotes = collectNotes()
            detectionResult = .chordFound(
                name: rootName + formula.symbol,
                quality: formula.quality,
                rootName: rootName,
                rootPitchClass: root,
                notes: chordNotes,
                alternates: [],
                matchedFormula: formula
            )
        } else {
            runDetection()
        }
    }

    // MARK: - Note Info

    func getNoteAt(stringIndex: Int, fret: Int) -> (pitchClass: Int32, name: String) {
        let openPC = tuning[stringIndex].openPitchClass
        let effectiveFret = Int32(fret) + Int32(capoFret)
        let pc = (openPC + effectiveFret) % Int32(Notes.shared.PITCH_CLASS_COUNT)

        if scaleOverlay.enabled, scaleOverlay.scale != nil {
            let isMinor = scaleOverlay.scale!.intervals.count > 2
                && (scaleOverlay.scale!.intervals[2] as! NSNumber).intValue == 3
            let name = Notes.shared.enharmonicForKey(
                pitchClass: pc,
                keyRoot: KotlinInt(int: scaleOverlay.root),
                isMinor: isMinor
            )
            return (pc, name)
        }

        let note = NoteKt.calculateNote(openStringPitchClass: openPC, fret: effectiveFret)
        return (note.pitchClass, note.name)
    }

    // MARK: - Chord Playback

    func playChord() {
        guard soundEnabled else { return }

        let sorted = selections.sorted(by: { $0.key < $1.key })
        let ordered = strumDown ? sorted : sorted.reversed()

        let pitchClasses: [Int32] = ordered.compactMap { (stringIndex, maybeFret) in
            guard let fret = maybeFret else { return nil }
            let openPC = tuning[stringIndex].openPitchClass
            return (openPC + Int32(fret) + Int32(capoFret)) % 12
        }

        tonePlayer.playChord(
            pitchClasses: pitchClasses,
            strumDelayMs: strumDelayMs,
            volume: volume
        )
    }

    /// Plays a voicing (array of frets per string) as a strummed chord.
    func playVoicing(frets: [Int]) {
        guard soundEnabled else { return }

        let indices = strumDown ? Array(0..<Self.stringCount) : Array((0..<Self.stringCount).reversed())
        let pitchClasses: [Int32] = indices.compactMap { stringIndex in
            let fret = frets[stringIndex]
            guard fret >= 0 else { return nil }
            let openPC = tuning[stringIndex].openPitchClass
            return (openPC + Int32(fret) + Int32(capoFret)) % 12
        }

        tonePlayer.playChord(
            pitchClasses: pitchClasses,
            strumDelayMs: strumDelayMs,
            volume: volume
        )
    }

    /// Plays a sequence of voicings with a pause between each.
    func playVoicingsSequentially(voicings: [[Int]], pauseMs: Int? = nil) {
        guard soundEnabled else { return }
        let pause = pauseMs ?? (noteDurationMs + 200)

        for (index, frets) in voicings.enumerated() {
            let delay = Double(index) * Double(pause) / 1000.0
            DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
                self?.playVoicing(frets: frets)
            }
        }
    }

    func playNote(pitchClass: Int32) {
        guard soundEnabled else { return }
        tonePlayer.playNote(pitchClass: pitchClass)
    }

    // MARK: - Capo

    func setCapoFret(_ fret: Int) {
        let newCapo = max(0, min(fret, Self.fretCount - 1))
        capoFret = newCapo

        for (stringIndex, sel) in selections {
            if let s = sel, newCapo > 0 && s >= 1 && s <= newCapo {
                selections[stringIndex] = nil
            }
        }
        runDetection()
    }

    // MARK: - Scale Overlay

    func toggleScaleOverlay() {
        scaleOverlay.enabled.toggle()
    }

    func setScaleRoot(_ root: Int32) {
        scaleOverlay.root = root
        if let scale = scaleOverlay.scale {
            let notes = Scales.shared.scaleNotes(root: root, scale: scale)
            scaleOverlay.scaleNotes = Set((notes as! [NSNumber]).map { $0.int32Value })
        }
    }

    func setScale(_ scale: Scale) {
        let notes = Scales.shared.scaleNotes(root: scaleOverlay.root, scale: scale)
        scaleOverlay.scale = scale
        scaleOverlay.scaleNotes = Set((notes as! [NSNumber]).map { $0.int32Value })
        scaleOverlay.enabled = true
        scaleOverlay.positionFretRange = nil
    }

    func setScalePositionRange(_ range: ClosedRange<Int>?) {
        scaleOverlay.positionFretRange = range
    }

    // MARK: - Private

    private func playNoteAt(stringIndex: Int, fret: Int) {
        let openPC = tuning[stringIndex].openPitchClass
        let pc = (openPC + Int32(fret) + Int32(capoFret)) % 12
        tonePlayer.playNote(pitchClass: pc)
    }

    private func collectPitchClasses() -> [Int32] {
        selections.sorted(by: { $0.key < $1.key }).compactMap { (stringIndex, fret) in
            guard let f = fret else { return nil }
            let openPC = tuning[stringIndex].openPitchClass
            return (openPC + Int32(f) + Int32(capoFret)) % 12
        }
    }

    private func collectNotes() -> [(pitchClass: Int32, name: String)] {
        selections.sorted(by: { $0.key < $1.key }).compactMap { (stringIndex, fret) in
            guard let f = fret else { return nil }
            let openPC = tuning[stringIndex].openPitchClass
            let pc = (openPC + Int32(f) + Int32(capoFret)) % 12
            return (pc, Notes.shared.pitchClassToName(pitchClass: pc))
        }
    }

    private func runDetection() {
        let pitchClasses = collectPitchClasses()
        let kotlinList = pitchClasses.map { KotlinInt(int: $0) }

        let result = ChordDetector.shared.detect(pitchClasses: kotlinList)

        if result is ChordDetector.DetectionResultNoSelection {
            detectionResult = .noSelection
        } else if let single = result as? ChordDetector.DetectionResultSingleNote {
            detectionResult = .singleNote(name: single.note.name, pitchClass: single.note.pitchClass)
        } else if let interval = result as? ChordDetector.DetectionResultInterval {
            let notes: [(pitchClass: Int32, name: String)] = interval.notes.map { n in
                let note = n as! Note
                return (note.pitchClass, note.name)
            }
            detectionResult = .interval(notes: notes)
        } else if let found = result as? ChordDetector.DetectionResultChordFound {
            let r = found.result
            let chordNotes: [(pitchClass: Int32, name: String)] = r.notes.map { n in
                let note = n as! Note
                return (note.pitchClass, note.name)
            }
            let alts: [String] = r.alternates.map { a in
                let alt = a as! AlternateChord
                return alt.name
            }
            detectionResult = .chordFound(
                name: r.name,
                quality: r.quality,
                rootName: r.root.name,
                rootPitchClass: r.root.pitchClass,
                notes: chordNotes,
                alternates: alts,
                matchedFormula: r.matchedFormula
            )
        } else if let noMatch = result as? ChordDetector.DetectionResultNoMatch {
            let notes: [(pitchClass: Int32, name: String)] = noMatch.notes.map { n in
                let note = n as! Note
                return (note.pitchClass, note.name)
            }
            detectionResult = .noMatch(notes: notes)
        }
    }
}

// MARK: - Scale Overlay State

struct ScaleOverlayState {
    var enabled: Bool = false
    var root: Int32 = 0
    var scale: Scale? = nil
    var scaleNotes: Set<Int32> = []
    var positionFretRange: ClosedRange<Int>? = nil
}

/// Swift-friendly wrapper for ChordDetector.DetectionResult.
enum DetectionResultWrapper {
    case noSelection
    case singleNote(name: String, pitchClass: Int32)
    case interval(notes: [(pitchClass: Int32, name: String)])
    case chordFound(name: String, quality: String, rootName: String,
                    rootPitchClass: Int32,
                    notes: [(pitchClass: Int32, name: String)],
                    alternates: [String],
                    matchedFormula: ChordFormula?)
    case noMatch(notes: [(pitchClass: Int32, name: String)])
}
