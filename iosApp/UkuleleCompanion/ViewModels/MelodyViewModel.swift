import Foundation
import AVFoundation
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
    private var loadedMelodyId: String? = nil
    private static let stabilizationThreshold = 3

    private let userDefaultsKey = "melodies"
    private let tonePlayer = TonePlayer()
    private var playbackTask: Task<Void, Never>?
    private let audioEngine = AudioCaptureEngine()
    private var stableCount = 0
    private var lastDetectedPitchClass: Int? = nil

    init() {
        loadMelodies()
    }

    var noteNames: [String] {
        Notes.shared.NOTE_NAMES_SHARP as! [String]
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
        isRecording = true
        stableCount = 0
        lastDetectedPitchClass = nil

        audioEngine.onBuffer = { [weak self] samples in
            Task { @MainActor in
            guard let self else { return }
            let floatArray = KotlinFloatArray(size: Int32(samples.count))
            for i in 0..<samples.count {
                floatArray.set(index: Int32(i), value: samples[i])
            }

            let result = PitchDetector.shared.detect(
                samples: floatArray,
                sampleRate: Int32(AudioCaptureEngine.sampleRate),
                threshold: 0.15,
                previousFrequency: nil
            )

            guard let result, result.confidence > 0.8 else {
            Task { @MainActor in
                self.detectedNote = nil
                self.stableCount = 0
                self.lastDetectedPitchClass = nil
                self.stabilizationProgress = 0
            }
            return
            }

            let noteInfo = TunerNoteMapper.shared.mapFrequency(
                hz: result.frequencyHz,
                a4Reference: 440.0
            )

            guard let noteInfo else { return }

            let pc = Int(noteInfo.pitchClass)
            let oct = Int(noteInfo.octave)

            Task { @MainActor in
                self.detectedNote = noteInfo.noteName + "\(oct)"

                if pc == self.lastDetectedPitchClass {
                    self.stableCount += 1
                } else {
                    self.lastDetectedPitchClass = pc
                    self.stableCount = 1
                }

                self.stabilizationProgress = Float(self.stableCount) / Float(Self.stabilizationThreshold)

                if self.stableCount >= Self.stabilizationThreshold {
                    self.addNote(pitchClass: pc, octave: oct)
                    self.stableCount = 0
                    self.lastDetectedPitchClass = nil
                    self.detectedNote = nil
                    self.stabilizationProgress = 0
                }
            }
            }
        }

        audioEngine.start()
    }

    func stopRecording() {
        isRecording = false
        audioEngine.stop()
        audioEngine.onBuffer = nil
        detectedNote = nil
        stableCount = 0
        lastDetectedPitchClass = nil
    }

    func save(name: String) {
        let id = UUID().uuidString
        let melody = MelodyData(
            id: id,
            name: name,
            notes: notes,
            bpm: bpm,
            createdAt: Date().timeIntervalSince1970 * 1000
        )
        savedMelodies.insert(melody, at: 0)
        loadedMelodyName = name
        loadedMelodyId = id
        hasUnsavedChanges = false
        persistMelodies()
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
        persistMelodies()
    }

    func deleteMelody(id: String) {
        savedMelodies.removeAll { $0.id == id }
        persistMelodies()
    }

    func importData(_ incoming: [[String: Any]]) {
        let decoder = JSONDecoder()
        for item in incoming {
            guard let jsonData = try? JSONSerialization.data(withJSONObject: item),
                  let melody = try? decoder.decode(MelodyData.self, from: jsonData)
            else { continue }
            if let idx = savedMelodies.firstIndex(where: { $0.id == melody.id }) {
                // Keep the one with the latest createdAt
                if melody.createdAt > savedMelodies[idx].createdAt {
                    savedMelodies[idx] = melody
                }
            } else {
                savedMelodies.append(melody)
            }
        }
        persistMelodies()
    }

    func exportData() -> [[String: Any]] {
        let encoder = JSONEncoder()
        return savedMelodies.compactMap { melody in
            guard let data = try? encoder.encode(melody),
                  let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
            else { return nil }
            return dict
        }
    }

    private func loadMelodies() {
        guard let data = UserDefaults.standard.data(forKey: userDefaultsKey),
              let decoded = try? JSONDecoder().decode([MelodyData].self, from: data)
        else { return }
        savedMelodies = decoded
    }

    private func persistMelodies() {
        guard let data = try? JSONEncoder().encode(savedMelodies) else { return }
        UserDefaults.standard.set(data, forKey: userDefaultsKey)
    }
}
