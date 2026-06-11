import Foundation
import shared
import Combine

@MainActor
final class ScalePracticeViewModel: ObservableObject {
    enum PracticeMode: String, CaseIterable {
        case playAlong = "Play Along"
        case quiz = "Quiz"
        case earTraining = "Ear Training"
    }

    enum PlayDirection: String, CaseIterable {
        case ascending = "Ascending"
        case descending = "Descending"
        case both = "Both"
    }

    enum FretPosition: String, CaseIterable {
        case all = "All"
        case open = "Open"
        case mid = "Mid"
        case high = "High"

        func range(lastFret: Int) -> ClosedRange<Int> {
            switch self {
            case .all: return 0...lastFret
            case .open: return 0...4
            case .mid: return 5...9
            case .high: return 10...lastFret
            }
        }
    }

    @Published var mode: PracticeMode = .quiz
    @Published var selectedRoot: Int32 = 0
    @Published var selectedCategory: ScaleCategory? = nil
    @Published var selectedScale: Scale? = nil
    @Published var bpm: Double = 120
    @Published var isPlaying = false
    @Published var playDirection: PlayDirection = .ascending
    @Published var loopPlayback = false
    @Published var fretPosition: FretPosition = .all
    @Published var showFretboard = false

    // Quiz state
    @Published var currentQuizQuestion: ScalePracticeGenerator.ScaleQuizQuestion? = nil
    @Published var quizSelectedAnswer: Int? = nil
    @Published var quizShowResult = false

    // Ear training state
    @Published var currentEarQuestion: ScalePracticeGenerator.EarTrainingQuestion? = nil
    @Published var earSelectedAnswer: Int? = nil
    @Published var earShowResult = false

    // Play along state
    @Published var currentNoteIndex: Int = -1

    private let tonePlayer = TonePlayer()
    private var playTimer: AnyCancellable?
    private let defaults = UserDefaults.standard
    private static let settingsKey = "scale_practice_settings"

    init() {
        loadSettings()
    }

    var rootNoteNames: [String] {
        (0..<Int32(12)).map { Notes.shared.pitchClassToName(pitchClass: $0) }
    }

    var availableScales: [Scale] {
        let list = Scales.shared.forCategory(category: selectedCategory)
        return list.asArray(of: Scale.self)
    }

    var scaleNotes: [Int] {
        guard let scale = selectedScale else { return [] }
        let noteSet = Scales.shared.scaleNotes(root: selectedRoot, scale: scale)
        let pitchClasses = noteSet.compactMap { ($0 as? KotlinInt)?.intValue }
        var ordered: [Int] = []
        for interval in scale.intervals.asArray(of: KotlinInt.self) {
            let pc = (Int(selectedRoot) + interval.intValue) % 12
            ordered.append(pc)
        }
        return ordered
    }

    func generateQuizQuestion() {
        currentQuizQuestion = ScalePracticeGenerator.shared.generateQuizQuestion(category: selectedCategory)
        quizSelectedAnswer = nil
        quizShowResult = false
    }

    func answerQuiz(_ index: Int, learnVM: LearnViewModel) {
        quizSelectedAnswer = index
        quizShowResult = true
        let correct = index == Int(currentQuizQuestion?.correctIndex ?? -1)
        learnVM.recordScalePracticeAnswer(mode: "quiz", correct: correct)
    }

    func generateEarQuestion() {
        currentEarQuestion = ScalePracticeGenerator.shared.generateEarQuestion(category: selectedCategory)
        earSelectedAnswer = nil
        earShowResult = false
    }

    func answerEar(_ index: Int, learnVM: LearnViewModel) {
        earSelectedAnswer = index
        earShowResult = true
        let correct = index == Int(currentEarQuestion?.correctIndex ?? -1)
        learnVM.recordScalePracticeAnswer(mode: "ear", correct: correct)
    }

    func playEarScale() {
        guard let question = currentEarQuestion else { return }
        let scale = question.scale
        let root = Int(question.root)
        let intervals = scale.intervals.asArray(of: KotlinInt.self)
        let notes = intervals.map { (root + $0.intValue) % 12 }
        playNoteSequence(notes)
    }

    func startPlayAlong() {
        guard let scale = selectedScale else { return }
        isPlaying = true
        currentNoteIndex = -1
        let intervals = scale.intervals.asArray(of: KotlinInt.self)
        var notes = intervals.map { (Int(selectedRoot) + $0.intValue) % 12 }

        switch playDirection {
        case .ascending:
            break
        case .descending:
            notes.reverse()
        case .both:
            notes = notes + notes.reversed().dropFirst()
        }

        playNoteSequence(notes)
    }

    func stopPlayAlong() {
        isPlaying = false
        currentNoteIndex = -1
        playTimer?.cancel()
        playTimer = nil
        tonePlayer.stop()
    }

    private func playNoteSequence(_ notes: [Int]) {
        var index = 0
        let interval = 60.0 / bpm

        playTimer?.cancel()
        playTimer = Timer.publish(every: interval, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self else { return }
                if index < notes.count {
                    self.currentNoteIndex = index
                    let pc = notes[index]
                    self.tonePlayer.playNote(pitchClass: Int32(pc))
                    index += 1
                } else if self.loopPlayback {
                    index = 0
                } else {
                    self.stopPlayAlong()
                }
            }
    }

    func saveSettings() {
        let dict: [String: Any] = [
            "root": selectedRoot,
            "bpm": bpm,
            "direction": playDirection.rawValue,
            "loop": loopPlayback,
            "fret_position": fretPosition.rawValue
        ]
        defaults.set(dict, forKey: Self.settingsKey)
    }

    private func loadSettings() {
        guard let dict = defaults.dictionary(forKey: Self.settingsKey) else { return }
        if let v = dict["root"] as? Int32 { selectedRoot = v }
        if let v = dict["bpm"] as? Double { bpm = v }
        if let v = dict["direction"] as? String { playDirection = PlayDirection(rawValue: v) ?? .ascending }
        if let v = dict["loop"] as? Bool { loopPlayback = v }
        if let v = dict["fret_position"] as? String { fretPosition = FretPosition(rawValue: v) ?? .all }
    }
}
