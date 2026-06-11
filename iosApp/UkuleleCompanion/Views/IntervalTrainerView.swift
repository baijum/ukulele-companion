import SwiftUI
import shared

struct IntervalTrainerView: View {
    @EnvironmentObject var learnVM: LearnViewModel
    @State private var level: Int32 = 1
    @State private var direction: IntervalTrainer.IntervalDirection = .ascending
    @State private var currentQuestion: IntervalTrainer.IntervalQuestion? = nil
    @State private var selectedAnswer: Int? = nil
    @State private var showResult = false
    @State private var score = 0
    @State private var total = 0
    @State private var streak = 0
    @State private var bestStreak = 0
    @State private var isAudioMode = true

    private let tonePlayer = TonePlayer()

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Picker("Mode", selection: $isAudioMode) {
                    Text("Audio").tag(true)
                    Text("Visual").tag(false)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)
                .onChange(of: isAudioMode) { _ in nextQuestion() }

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

                if isAudioMode {
                    HStack {
                        Text("Direction")
                            .font(.headline)
                        Picker("Direction", selection: $direction) {
                            Text("Up").tag(IntervalTrainer.IntervalDirection.ascending)
                            Text("Down").tag(IntervalTrainer.IntervalDirection.descending)
                            Text("Harmonic").tag(IntervalTrainer.IntervalDirection.harmonic)
                        }
                        .pickerStyle(.segmented)
                    }
                    .padding(.horizontal)
                }

                HStack {
                    Text("Score: \(score)/\(total)")
                        .font(.headline)
                        .accessibilityLabel("Score: \(score) out of \(total)")
                    Spacer()
                    Button("New Question") {
                        nextQuestion()
                    }
                    .buttonStyle(.bordered)
                }
                .padding(.horizontal)

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
                    .padding(.horizontal)
                }

                allTimeStatsRow

                if let question = currentQuestion {
                    VStack(spacing: 12) {
                        if isAudioMode {
                            Button {
                                playInterval(question)
                            } label: {
                                Label("Play Interval", systemImage: "play.circle.fill")
                                    .font(.title2)
                            }
                            .buttonStyle(.borderedProminent)

                            Text("\(question.note1Name) -> ?")
                                .font(.title3)
                                .foregroundStyle(.secondary)
                                .accessibilityLabel("\(question.note1Name) to unknown")
                        } else {
                            HStack(spacing: 24) {
                                noteCircle(question.note1Name)
                                Image(systemName: "arrow.right")
                                    .font(.title2)
                                    .foregroundStyle(.secondary)
                                noteCircle(question.note2Name)
                            }
                            .padding(.vertical, 8)

                            Text("Semitones: \(abs(Int(question.note2PitchClass) - Int(question.note1PitchClass) + 12) % 12)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }

                        let options = question.options as! [String]
                        ForEach(Array(options.enumerated()), id: \.offset) { index, option in
                            Button {
                                if selectedAnswer == nil {
                                    selectedAnswer = index
                                    showResult = true
                                    total += 1
                                    let correct = index == Int(question.correctIndex)
                                    if correct {
                                        score += 1
                                        streak += 1
                                        if streak > bestStreak { bestStreak = streak }
                                        if streak % 5 == 0 && level < 4 {
                                            level += 1
                                        }
                                    } else {
                                        streak = 0
                                    }
                                    learnVM.recordIntervalAnswer(level: Int(level), correct: correct)
                                }
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

                        if showResult {
                            Text("Answer: \(question.correctAnswer)")
                                .font(.callout)
                                .foregroundStyle(.secondary)

                            Button("Next") {
                                nextQuestion()
                            }
                            .buttonStyle(.borderedProminent)
                        }
                    }
                    .padding(.horizontal)
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Interval Trainer")
        .onAppear {
            if currentQuestion == nil { nextQuestion() }
        }
    }

    private func nextQuestion() {
        currentQuestion = IntervalTrainer.shared.generateQuestion(level: level, direction: direction)
        selectedAnswer = nil
        showResult = false
    }

    private func playInterval(_ q: IntervalTrainer.IntervalQuestion) {
        switch q.direction {
        case .ascending:
            tonePlayer.playNote(pitchClass: q.note1PitchClass, octave: q.note1Octave)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                self.tonePlayer.playNote(pitchClass: q.note2PitchClass, octave: q.note2Octave)
            }
        case .descending:
            tonePlayer.playNote(pitchClass: q.note2PitchClass, octave: q.note2Octave)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                self.tonePlayer.playNote(pitchClass: q.note1PitchClass, octave: q.note1Octave)
            }
        case .harmonic:
            tonePlayer.playNote(pitchClass: q.note1PitchClass, octave: q.note1Octave)
            tonePlayer.playNote(pitchClass: q.note2PitchClass, octave: q.note2Octave)
        default:
            break
        }
    }

    private func optionBg(index: Int, correctIndex: Int) -> Color {
        guard showResult else { return Color(.systemGray6) }
        if index == correctIndex { return Color.green.opacity(0.2) }
        if index == selectedAnswer { return Color.red.opacity(0.2) }
        return Color(.systemGray6)
    }

    private func noteCircle(_ name: String) -> some View {
        Text(name)
            .font(.title2.bold())
            .frame(width: 64, height: 64)
            .background(Color.accentColor.opacity(0.15))
            .clipShape(Circle())
            .accessibilityLabel("Note: \(name)")
    }

    private var allTimeStatsRow: some View {
        let stats = learnVM.intervalStats(level: Int(level))
        return Group {
            if stats.total > 0 {
                HStack {
                    Label("\(stats.correct)/\(stats.total)", systemImage: "chart.bar.fill")
                        .font(.caption)
                    Text("\(stats.accuracyPercent)%")
                        .font(.caption.bold())
                    if stats.bestStreak > 0 {
                        Text("Best: \(stats.bestStreak)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                }
                .foregroundStyle(.secondary)
                .padding(.horizontal)
            }
        }
    }
}
