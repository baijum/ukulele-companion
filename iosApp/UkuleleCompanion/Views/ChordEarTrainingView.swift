import SwiftUI
import shared

struct ChordEarTrainingView: View {
    @EnvironmentObject var learnVM: LearnViewModel
    @State private var level: Int32 = 1
    @State private var currentQuestion: ChordEarTrainer.ChordEarQuestion? = nil
    @State private var selectedAnswer: Int? = nil
    @State private var showResult = false
    @State private var score = 0
    @State private var total = 0

    private let tonePlayer = TonePlayer()
    private static let sampleNames = [
        "uke_a", "uke_asharp", "uke_b", "uke_c", "uke_csharp",
        "uke_d", "uke_dsharp", "uke_e", "uke_f", "uke_fsharp",
        "uke_g", "uke_gsharp"
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                HStack {
                    Text("Level")
                        .font(.headline)
                    Picker("Level", selection: $level) {
                        ForEach(Int32(1)...Int32(4), id: \.self) { l in
                            Text("\(l)").tag(l)
                        }
                    }
                    .pickerStyle(.segmented)
                }
                .padding(.horizontal)

                HStack {
                    Text("Score: \(score)/\(total)")
                        .font(.headline)
                        .accessibilityLabel("Score: \(score) out of \(total)")
                    Spacer()
                    Button("New Question") { nextQuestion() }
                        .buttonStyle(.bordered)
                }
                .padding(.horizontal)

                if let question = currentQuestion {
                    VStack(spacing: 12) {
                        Button {
                            playChord(question)
                        } label: {
                            Label("Play Chord", systemImage: "play.circle.fill")
                                .font(.title2)
                        }
                        .buttonStyle(.borderedProminent)

                        Text("Root: \(question.rootName)")
                            .font(.title3)
                            .foregroundStyle(.secondary)

                        Text("What quality is this chord?")
                            .font(.subheadline)

                        if let options = question.options as? [String] {
                            ForEach(Array(options.enumerated()), id: \.offset) { index, option in
                                Button {
                                    guard selectedAnswer == nil else { return }
                                    selectedAnswer = index
                                    showResult = true
                                    total += 1
                                    let correct = index == Int(question.correctIndex)
                                    if correct { score += 1 }
                                    learnVM.recordChordEarAnswer(level: Int(level), correct: correct)
                                } label: {
                                    HStack {
                                        Text(option)
                                            .foregroundStyle(.primary)
                                        Spacer()
                                        if showResult && index == Int(question.correctIndex) {
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
                                    .background(optionBg(index: index, correctIndex: Int(question.correctIndex)))
                                    .cornerRadius(8)
                                }
                                .accessibilityValue(showResult ? (index == Int(question.correctIndex) ? "Correct answer" : (index == selectedAnswer ? "Your answer, incorrect" : "")) : "")
                            }
                        }

                        if showResult {
                            Text("Answer: \(question.correctAnswer)")
                                .font(.callout)
                                .foregroundStyle(.secondary)

                            Button("Next") { nextQuestion() }
                                .buttonStyle(.borderedProminent)
                        }
                    }
                    .padding(.horizontal)
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Chord Ear Training")
        .onAppear {
            if currentQuestion == nil { nextQuestion() }
        }
    }

    private func nextQuestion() {
        currentQuestion = ChordEarTrainer.shared.generateQuestion(level: level)
        selectedAnswer = nil
        showResult = false
    }

    private func playChord(_ q: ChordEarTrainer.ChordEarQuestion) {
        let notes = q.notes as! [KotlinPair<KotlinInt, KotlinInt>]
        let pitchClasses = notes.map { $0.first!.int32Value }
        tonePlayer.playChord(pitchClasses: pitchClasses, strumDelayMs: 40)
    }

    private func optionBg(index: Int, correctIndex: Int) -> Color {
        guard showResult else { return Color(.systemGray6) }
        if index == correctIndex { return Color.green.opacity(0.2) }
        if index == selectedAnswer { return Color.red.opacity(0.2) }
        return Color(.systemGray6)
    }
}
