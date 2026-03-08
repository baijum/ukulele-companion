import SwiftUI
import shared

struct NoteQuizView: View {
    @EnvironmentObject var learnVM: LearnViewModel
    @EnvironmentObject var settings: SettingsViewModel

    enum QuizMode: String, CaseIterable {
        case nameIt = "Name It"
        case findIt = "Find It"
    }

    @State private var mode: QuizMode = .nameIt
    @State private var difficulty: Int32 = 2
    @State private var score = 0
    @State private var total = 0
    @State private var streak = 0
    @State private var bestStreak = 0

    // Name It state
    @State private var nameQuestion: NoteQuizGenerator.NameItQuestion? = nil
    @State private var nameSelectedAnswer: Int? = nil
    @State private var nameShowResult = false

    // Find It state
    @State private var findQuestion: NoteQuizGenerator.FindItQuestion? = nil
    @State private var findSelectedAnswer: Int? = nil
    @State private var findShowResult = false

    private var currentTuning: UkuleleTuning {
        UkuleleTuning.entries.first { $0.label == settings.selectedTuning } ?? .highG
    }

    private var stringOpenNotes: [KotlinInt] {
        let t = currentTuning
        return (0..<4).map { KotlinInt(int: (t.pitchClasses[$0] as! NSNumber).int32Value) }
    }

    private var stringNames: [String] {
        let t = currentTuning
        let labels = ["4th", "3rd", "2nd", "1st"]
        return (0..<4).map { "\(t.stringNames[$0]) (\(labels[$0]))" }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Picker("Mode", selection: $mode) {
                    ForEach(QuizMode.allCases, id: \.self) { m in
                        Text(m.rawValue).tag(m)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)
                .onChange(of: mode) { _ in nextQuestion() }

                HStack {
                    Text("Difficulty")
                        .font(.headline)
                    Picker("Difficulty", selection: $difficulty) {
                        Text("Easy").tag(Int32(1))
                        Text("Medium").tag(Int32(2))
                        Text("Hard").tag(Int32(3))
                    }
                    .pickerStyle(.segmented)
                }
                .padding(.horizontal)

                VStack(spacing: 6) {
                    HStack {
                        Text("Score: \(score)/\(total)")
                            .font(.headline)
                            .accessibilityLabel("Score: \(score) out of \(total)")
                        Spacer()
                    }

                    if total > 0 {
                        HStack(spacing: 16) {
                            Label("\(score * 100 / total)%", systemImage: "percent")
                                .font(.caption)
                            Label("Streak: \(streak)", systemImage: "flame")
                                .font(.caption)
                            Label("Best: \(bestStreak)", systemImage: "star")
                                .font(.caption)
                            Spacer()
                        }
                        .foregroundStyle(.secondary)
                    }

                    let stats = learnVM.noteQuizStats()
                    if stats.total > 0 {
                        HStack {
                            Text("All-time: \(stats.correct)/\(stats.total) (\(stats.accuracyPercent)%)")
                                .font(.caption)
                            if stats.bestStreak > 0 {
                                Text("Best streak: \(stats.bestStreak)")
                                    .font(.caption)
                            }
                            Spacer()
                        }
                        .foregroundStyle(.secondary)
                    }
                }
                .padding(.horizontal)

                if mode == .nameIt {
                    nameItContent
                } else {
                    findItContent
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Note Quiz")
        .onAppear {
            if nameQuestion == nil && findQuestion == nil { nextQuestion() }
        }
        .onChange(of: settings.selectedTuning) { _ in
            score = 0
            total = 0
            nextQuestion()
        }
    }

    @ViewBuilder
    private var nameItContent: some View {
        if let q = nameQuestion {
            VStack(spacing: 12) {
                Text("What note is on String \(stringNames[Int(q.string)]), Fret \(q.fret)?")
                    .font(.title3.weight(.medium))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)

                let options = q.options as! [String]
                ForEach(Array(options.enumerated()), id: \.offset) { index, option in
                    Button {
                        guard nameSelectedAnswer == nil else { return }
                        nameSelectedAnswer = index
                        nameShowResult = true
                        total += 1
                        let correct = index == Int(q.correctIndex)
                        if correct {
                            score += 1
                            streak += 1
                            if streak > bestStreak { bestStreak = streak }
                        } else {
                            streak = 0
                        }
                        learnVM.recordNoteQuizAnswer(correct: correct)
                    } label: {
                        answerRow(option, index: index, correctIndex: Int(q.correctIndex),
                                  selectedAnswer: nameSelectedAnswer, showResult: nameShowResult)
                    }
                }

                if nameShowResult {
                    Button("Next") { nextQuestion() }
                        .buttonStyle(.borderedProminent)
                }
            }
            .padding(.horizontal)
        }
    }

    @ViewBuilder
    private var findItContent: some View {
        if let q = findQuestion {
            VStack(spacing: 12) {
                Text("Where is \(q.targetNote) on the fretboard?")
                    .font(.title3.weight(.medium))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)

                let options = q.options as! [String]
                ForEach(Array(options.enumerated()), id: \.offset) { index, option in
                    Button {
                        guard findSelectedAnswer == nil else { return }
                        findSelectedAnswer = index
                        findShowResult = true
                        total += 1
                        let correct = index == Int(q.correctIndex)
                        if correct {
                            score += 1
                            streak += 1
                            if streak > bestStreak { bestStreak = streak }
                        } else {
                            streak = 0
                        }
                        learnVM.recordNoteQuizAnswer(correct: correct)
                    } label: {
                        answerRow(option, index: index, correctIndex: Int(q.correctIndex),
                                  selectedAnswer: findSelectedAnswer, showResult: findShowResult)
                    }
                }

                if findShowResult {
                    Button("Next") { nextQuestion() }
                        .buttonStyle(.borderedProminent)
                }
            }
            .padding(.horizontal)
        }
    }

    private func answerRow(_ text: String, index: Int, correctIndex: Int,
                           selectedAnswer: Int?, showResult: Bool) -> some View {
        HStack {
            Text(text)
                .foregroundStyle(.primary)
            Spacer()
            if showResult && index == correctIndex {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(.green)
                    .accessibilityHidden(true)
            } else if showResult && index == selectedAnswer {
                Image(systemName: "xmark.circle.fill")
                    .foregroundStyle(.red)
                    .accessibilityHidden(true)
            }
        }
        .padding()
        .background(answerBg(index: index, correctIndex: correctIndex, selectedAnswer: selectedAnswer, showResult: showResult))
        .cornerRadius(8)
        .accessibilityValue(showResult ? (index == correctIndex ? "Correct answer" : (index == selectedAnswer ? "Your answer, incorrect" : "")) : "")
    }

    private func answerBg(index: Int, correctIndex: Int, selectedAnswer: Int?, showResult: Bool) -> Color {
        guard showResult else { return Color(.systemGray6) }
        if index == correctIndex { return Color.green.opacity(0.2) }
        if index == selectedAnswer { return Color.red.opacity(0.2) }
        return Color(.systemGray6)
    }

    private func nextQuestion() {
        if mode == .nameIt {
            nameQuestion = NoteQuizGenerator.shared.generateNameIt(
                difficulty: difficulty, stringOpenNotes: stringOpenNotes)
            nameSelectedAnswer = nil
            nameShowResult = false
        } else {
            findQuestion = NoteQuizGenerator.shared.generateFindIt(
                difficulty: difficulty, stringOpenNotes: stringOpenNotes, stringNames: stringNames)
            findSelectedAnswer = nil
            findShowResult = false
        }
    }
}
