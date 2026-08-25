import Foundation
import AVFoundation
import os
import shared

enum NoteDuration: String, CaseIterable, Codable {
    case whole = "𝅝"
    case half = "𝅗𝅥"
    case quarter = "♩"
    case eighth = "♪"
    case sixteenth = "𝅘𝅥𝅯"

    var beats: Double {
        switch self {
        case .whole: 4.0
        case .half: 2.0
        case .quarter: 1.0
        case .eighth: 0.5
        case .sixteenth: 0.25
        }
    }

    var label: String {
        switch self {
        case .whole: "Whole"
        case .half: "Half"
        case .quarter: "Quarter"
        case .eighth: "Eighth"
        case .sixteenth: "16th"
        }
    }
}

struct MelodyNoteData: Codable, Identifiable {
    let id: String
    let pitchClass: Int?
    let octave: Int
    let duration: NoteDuration
    let isRest: Bool

    var displayName: String {
        if isRest { return "Rest" }
        guard let pc = pitchClass else { return "?" }
        return Notes.shared.pitchClassToName(pitchClass: Int32(pc)) + "\(octave)"
    }
}

struct MelodyData: Codable, Identifiable {
    let id: String
    var name: String
    var notes: [MelodyNoteData]
    var bpm: Int
    var createdAt: Double

    func sanitized() -> MelodyData {
        var copy = self
        copy.notes = notes.map { note in
            MelodyNoteData(
                id: note.id,
                pitchClass: note.pitchClass.flatMap { (0...11).contains($0) ? $0 : nil },
                octave: min(max(note.octave, 3), 6),
                duration: note.duration,
                isRest: note.isRest
            )
        }
        return copy
    }
}

enum MelodyInputMode {
    case tap, record
}

@MainActor
final class MelodyViewModel: ObservableObject {
    @Published var notes: [MelodyNoteData] = []
    @Published var selectedDuration: NoteDuration = .quarter
    @Published var bpm: Int = 120
    @Published var currentOctave: Int = 4
    @Published var isPlaying = false
    @Published var playingIndex: Int? = nil
    @Published var inputMode: MelodyInputMode = .tap
    @Published var isRecording = false
    @Published var detectedNote: String? = nil
    @Published var savedMelodies: [MelodyData] = []
    @Published var loadedMelodyName: String? = nil
    @Published var stabilizationProgress: Float = 0
    @Published var lastAddedFeedback: String? = nil
    @Published var hasUnsavedChanges: Bool = false
    @Published var selectedNoteIndex: Int? = nil

    static let stepCount8 = 8
    static let stepCount16 = 16

    @Published var isStepSequencerMode: Bool = false
    @Published var steps: [MelodyNoteData?] = Array(repeating: nil, count: stepCount8)
    @Published var stepLooping: Bool = true

    private var loadedMelodyId: String? = nil
    private static let stabilizationThreshold = 3

    private let repository: MelodyRepository
    private let tonePlayer = TonePlayer()
    private var playbackTask: Task<Void, Never>?
    private let audioEngine = AudioCaptureEngine()
    private nonisolated(unsafe) let frameGate = FrameGate()
    private nonisolated(unsafe) let audioProcessingQueue = DispatchQueue(
        label: "com.baijum.ukufretboard.melody.dsp", qos: .userInitiated
    )
    private var stableCount = 0
    private var lastDetectedPitchClass: Int? = nil
    private var lastDetectedOctave: Int? = nil
    private var awaitingSilence = false
    private nonisolated(unsafe) var previousFrequency: KotlinDouble? = nil

    init(repository: MelodyRepository = MelodyRepository()) {
        self.repository = repository
        savedMelodies = repository.getAll()
    }

    var noteNames: [String] {
        Notes.shared.NOTE_NAMES_SHARP.asStrings
    }

    func addNote(pitchClass: Int, octave: Int) {
        let note = MelodyNoteData(
            id: UUID().uuidString,
            pitchClass: pitchClass,
            octave: octave,
            duration: selectedDuration,
            isRest: false
        )
        notes.append(note)
        hasUnsavedChanges = true
        showFeedback(note.displayName)
    }

    func addRest() {
        let rest = MelodyNoteData(
            id: UUID().uuidString,
            pitchClass: nil,
            octave: currentOctave,
            duration: selectedDuration,
            isRest: true
        )
        notes.append(rest)
        hasUnsavedChanges = true
    }

    func deleteNote(at indices: IndexSet) {
        notes.remove(atOffsets: indices)
        hasUnsavedChanges = true
        selectedNoteIndex = nil
    }

    func deleteNoteAt(index: Int) {
        guard index >= 0 && index < notes.count else { return }
        notes.remove(at: index)
        hasUnsavedChanges = true
        selectedNoteIndex = nil
    }

    func selectNote(index: Int?) {
        selectedNoteIndex = (selectedNoteIndex == index) ? nil : index
    }

    func clearAll() {
        notes.removeAll()
        loadedMelodyName = nil
        loadedMelodyId = nil
        hasUnsavedChanges = false
        selectedNoteIndex = nil
    }

    // MARK: - Step Sequencer

    func toggleStepSequencerMode() {
        isStepSequencerMode.toggle()
    }

    func setStep(index: Int, pitchClass: Int) {
        guard index >= 0 && index < steps.count else { return }
        let note = MelodyNoteData(
            id: UUID().uuidString,
            pitchClass: pitchClass,
            octave: currentOctave,
            duration: selectedDuration,
            isRest: false
        )
        steps[index] = note
        hasUnsavedChanges = true
        tonePlayer.playNote(pitchClass: Int32(pitchClass))
    }

    func clearStep(index: Int) {
        guard index >= 0 && index < steps.count else { return }
        steps[index] = nil
        hasUnsavedChanges = true
    }

    func expandSteps() {
        if steps.count >= Self.stepCount16 {
            steps = Array(steps.prefix(Self.stepCount8))
        } else {
            steps += Array(repeating: nil as MelodyNoteData?, count: Self.stepCount16 - steps.count)
        }
    }

    func playSteps() {
        let filled = steps.contains { $0 != nil }
        guard filled else { return }
        isPlaying = true
        playingIndex = 0

        playbackTask = Task { @MainActor [weak self] in
            guard let self else { return }
            repeat {
                let snapshot = self.steps
                for i in 0..<snapshot.count {
                    if Task.isCancelled { break }
                    guard self.isPlaying else { break }
                    self.playingIndex = i
                    if let note = snapshot[i], let pc = note.pitchClass {
                        self.tonePlayer.playNote(pitchClass: Int32(pc))
                    }
                    let duration = (snapshot[i]?.duration ?? self.selectedDuration).beats
                    let ms = duration * 60_000.0 / Double(self.bpm)
                    try? await Task.sleep(nanoseconds: UInt64(ms * 1_000_000))
                }
            } while self.isPlaying && self.stepLooping && !Task.isCancelled
            self.isPlaying = false
            self.playingIndex = nil
        }
    }

    func clearAllSteps() {
        for i in 0..<steps.count {
            steps[i] = nil
        }
        hasUnsavedChanges = true
    }

    private func showFeedback(_ text: String) {
        lastAddedFeedback = "Added \(text)"
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) { [weak self] in
            if self?.lastAddedFeedback == "Added \(text)" {
                self?.lastAddedFeedback = nil
            }
        }
    }

    func playMelody() {
        guard !notes.isEmpty else { return }
        isPlaying = true
        playingIndex = 0

        playbackTask = Task { [weak self] in
            guard let self else { return }
            let beatsPerSecond = Double(self.bpm) / 60.0

            for (index, note) in self.notes.enumerated() {
                if Task.isCancelled { break }
                await MainActor.run { self.playingIndex = index }

                if !note.isRest, let pc = note.pitchClass {
                    let sampleNames = [
                        "uke_c", "uke_csharp", "uke_d", "uke_dsharp",
                        "uke_e", "uke_f", "uke_fsharp", "uke_g",
                        "uke_gsharp", "uke_a", "uke_asharp", "uke_b"
                    ]
                    self.tonePlayer.play(sampleNames[pc])
                }

                let duration = note.duration.beats / beatsPerSecond
                try? await Task.sleep(nanoseconds: UInt64(duration * 1_000_000_000))
            }

            await MainActor.run {
                self.isPlaying = false
                self.playingIndex = nil
            }
        }
    }

    func stopPlayback() {
        playbackTask?.cancel()
        playbackTask = nil
        tonePlayer.stop()
        isPlaying = false
        playingIndex = nil
    }

    func startRecording() {
        Task { @MainActor in
            let granted: Bool
            if #available(iOS 17.0, *) {
                granted = await AVAudioApplication.requestRecordPermission()
            } else {
                granted = await withCheckedContinuation { continuation in
                    AVAudioSession.sharedInstance().requestRecordPermission { g in
                        continuation.resume(returning: g)
                    }
                }
            }
            if granted {
                beginCapture()
            }
        }
    }

    private func beginCapture() {
        stableCount = 0
        lastDetectedPitchClass = nil
        lastDetectedOctave = nil
        awaitingSilence = false
        previousFrequency = nil

        audioEngine.onBuffer = { [weak self] samples in
            guard let self else { return }
            guard self.frameGate.tryEnter() else { return }
            self.audioProcessingQueue.async {
                let floatArray = KotlinFloatArray(size: Int32(samples.count))
                for i in 0..<samples.count {
                    floatArray.set(index: Int32(i), value: samples[i])
                }

                let prevFreq = self.previousFrequency

                let result = PitchDetector.shared.detect(
                    samples: floatArray,
                    sampleRate: Int32(AudioCaptureEngine.sampleRate),
                    threshold: 0.15,
                    previousFrequency: prevFreq
                )

                Task { @MainActor [weak self] in
                    guard let self else { return }
                    defer { self.frameGate.exit() }

                    guard let result else {
                        self.detectedNote = nil
                        self.stableCount = 0
                        self.lastDetectedPitchClass = nil
                        self.lastDetectedOctave = nil
                        self.stabilizationProgress = 0
                        self.previousFrequency = nil
                        self.awaitingSilence = false
                        return
                    }

                    self.previousFrequency = KotlinDouble(value: result.frequencyHz)

                    let noteInfo = TunerNoteMapper.shared.mapFrequency(
                        hz: result.frequencyHz,
                        a4Reference: 440.0
                    )

                    guard let noteInfo else { return }

                    let pc = Int(noteInfo.pitchClass)
                    let oct = Int(noteInfo.octave)

                    self.detectedNote = noteInfo.noteName + "\(oct)"

                    if pc == self.lastDetectedPitchClass && oct == self.lastDetectedOctave {
                        self.stableCount += 1
                    } else {
                        self.lastDetectedPitchClass = pc
                        self.lastDetectedOctave = oct
                        self.stableCount = 1
                    }

                    self.stabilizationProgress = Float(self.stableCount) / Float(Self.stabilizationThreshold)

                    if self.stableCount >= Self.stabilizationThreshold {
                        if !self.awaitingSilence {
                            self.addNote(pitchClass: pc, octave: oct)
                            self.awaitingSilence = true
                        }
                        self.stableCount = 0
                        self.lastDetectedPitchClass = nil
                        self.lastDetectedOctave = nil
                        self.detectedNote = nil
                        self.stabilizationProgress = 0
                        self.previousFrequency = nil
                    }
                }
            }
        }

        audioEngine.onInterrupted = { [weak self] in self?.stopRecording() }
        audioEngine.start()
        isRecording = true
    }

    func stopRecording() {
        isRecording = false
        audioEngine.stop()
        audioEngine.onBuffer = nil
        detectedNote = nil
        stableCount = 0
        lastDetectedPitchClass = nil
        lastDetectedOctave = nil
        awaitingSilence = false
        previousFrequency = nil
    }

    func save(name: String) {
        let id = loadedMelodyId ?? UUID().uuidString
        let createdAt = savedMelodies.first(where: { $0.id == id })?.createdAt
            ?? Date().timeIntervalSince1970 * 1000
        let melody = MelodyData(id: id, name: name, notes: notes, bpm: bpm, createdAt: createdAt)
        if let idx = savedMelodies.firstIndex(where: { $0.id == id }) {
            savedMelodies[idx] = melody
        } else {
            savedMelodies.insert(melody, at: 0)
        }
        loadedMelodyName = name
        loadedMelodyId = id
        hasUnsavedChanges = false
        repository.save(savedMelodies)
    }

    func load(melody: MelodyData) {
        notes = melody.notes
        bpm = melody.bpm
        loadedMelodyName = melody.name
        loadedMelodyId = melody.id
    }

    func renameMelody(id: String, newName: String) {
        guard let idx = savedMelodies.firstIndex(where: { $0.id == id }) else { return }
        savedMelodies[idx].name = newName
        if loadedMelodyId == id {
            loadedMelodyName = newName
        }
        repository.save(savedMelodies)
    }

    func deleteMelody(id: String) {
        savedMelodies.removeAll { $0.id == id }
        repository.save(savedMelodies)
    }

    func importData(_ incoming: [[String: Any]]) {
        repository.importData(incoming, into: &savedMelodies)
    }

    func exportData() -> [[String: Any]] {
        repository.exportData(savedMelodies)
    }
}
