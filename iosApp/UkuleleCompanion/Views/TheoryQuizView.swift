import SwiftUI
import shared

struct TheoryQuizView: View {
    @EnvironmentObject var learnVM: LearnViewModel
    @State private var selectedCategory: QuizGenerator.QuizCategory = .intervals
    @State private var currentQuestion: QuizGenerator.QuizQuestion? = nil
    @State private var selectedAnswer: Int? = nil
    @State private var showExplanation = false
    @State private var score = 0
    @State private var total = 0
    @State private var streak = 0
    @State private var bestStreak = 0
    @State private var useAllCategories = false

    // Blitz mode
    @State private var isBlitzMode = false
    @State private var blitzActive = false
    @State private var blitzTimeMs: Int = 60_000
    @State private var blitzScore = 0
    @State private var blitzTotal = 0
    @State private var blitzFinished = false
    @State private var blitzTimer: Timer?
    private static let blitzDurationMs = 60_000

    private let categories: [QuizGenerator.QuizCategory] = [
        .intervals, .chords, .keys, .scales, .progressions
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                modePicker
                categoryPicker
                if isBlitzMode {
                    blitzContent
                } else {
                    scoreBar
                    if let question = currentQuestion {
                        questionContent(question)
                    }
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Theory Quiz")
        .onAppear {
            if currentQuestion == nil { nextQuestion() }
        }
        .onDisappear {
            blitzTimer?.invalidate()
        }
    }

    private var modePicker: some View {
        Picker("Mode", selection: $isBlitzMode) {
            Text("Standard").tag(false)
            Text("Blitz").tag(true)
        }
        .pickerStyle(.segmented)
        .padding(.horizontal)
        .onChange(of: isBlitzMode) { _ in
            blitzTimer?.invalidate()
            blitzActive = false
            blitzFinished = false
            nextQuestion()
        }
    }

    private var categoryPicker: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                Button {
                    useAllCategories = true
                    nextQuestion()
                } label: {
                    Text("All")
                        .font(.subheadline)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(useAllCategories ? Color.accentColor : Color(.systemGray5))
                        .foregroundStyle(useAllCategories ? .white : .primary)
                        .cornerRadius(16)
                }
                .accessibilityAddTraits(useAllCategories ? .isSelected : [])

                ForEach(categories, id: \.name) { cat in
                    Button {
                        useAllCategories = false
                        selectedCategory = cat
                        nextQuestion()
                    } label: {
                        Text(cat.label)
                            .font(.subheadline)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(!useAllCategories && selectedCategory == cat ? Color.accentColor : Color(.systemGray5))
                            .foregroundStyle(!useAllCategories && selectedCategory == cat ? .white : .primary)
                            .cornerRadius(16)
                    }
                    .accessibilityAddTraits(!useAllCategories && selectedCategory == cat ? .isSelected : [])
                }
            }
            .padding(.horizontal)
        }
    }

    private var scoreBar: some View {
        VStack(spacing: 6) {
            HStack {
                Text("Score: \(score)/\(total)")
                    .font(.headline)
                    .accessibilityLabel("Score: \(score) out of \(total)")
                Spacer()
                Button("New Question") { nextQuestion() }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
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

            let stats = useAllCategories ? learnVM.quizStats(category: nil) : learnVM.quizStats(category: selectedCategory)
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
    }

    private func questionContent(_ question: QuizGenerator.QuizQuestion) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(question.question)
                .font(.title3.weight(.medium))

            let options = question.options.asStrings
            ForEach(Array(options.enumerated()), id: \.offset) { index, option in
                Button {
                    if selectedAnswer == nil {
                        selectedAnswer = index
                        showExplanation = true
                        total += 1
                        let correct = index == Int(question.correctIndex)
                        if correct {
                            score += 1
                            streak += 1
                            if streak > bestStreak { bestStreak = streak }
                        } else {
                            streak = 0
                        }
                        learnVM.recordQuizAnswer(category: question.category, correct: correct)
                    }
                } label: {
                    HStack {
                        Text(option)
                            .foregroundStyle(.primary)
                        Spacer()
                        if showExplanation && index == Int(question.correctIndex) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(.green)
                                .accessibilityHidden(true)
                        } else if showExplanation && index == selectedAnswer {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(.red)
                                .accessibilityHidden(true)
                        }
                    }
                    .padding()
                    .background(optionBackground(index: index, correctIndex: Int(question.correctIndex)))
                    .cornerRadius(8)
                }
                .accessibilityValue(showExplanation ? (index == Int(question.correctIndex) ? "Correct answer" : (index == selectedAnswer ? "Your answer, incorrect" : "")) : "")
            }

            if showExplanation {
                Text(question.explanation)
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(8)

                Button("Next Question") {
                    nextQuestion()
                }
                .buttonStyle(.borderedProminent)
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal)
    }

    // MARK: - Blitz Mode

    @ViewBuilder
    private var blitzContent: some View {
        if blitzFinished {
            blitzResultsCard
        } else if blitzActive {
            blitzTimerBar
            if let question = currentQuestion {
                blitzQuestionContent(question)
            }
        } else {
            startBlitzCard
        }
    }

    private var startBlitzCard: some View {
        VStack(spacing: 12) {
            Image(systemName: "bolt.fill")
                .font(.largeTitle)
                .foregroundStyle(.yellow)
            Text("60 seconds. +3s per correct answer.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Button("Start Blitz") { startBlitz() }
                .buttonStyle(.borderedProminent)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color(.systemGray6))
        .cornerRadius(12)
        .padding(.horizontal)
    }

    private var blitzTimerBar: some View {
        HStack {
            Image(systemName: "bolt.fill")
                .foregroundStyle(.yellow)
            ProgressView(value: Double(blitzTimeMs), total: Double(Self.blitzDurationMs))
                .tint(blitzTimeMs > 10_000 ? .accentColor : .red)
            Text(String(format: "%.1fs", Double(blitzTimeMs) / 1000.0))
                .font(.headline.monospacedDigit())
                .frame(width: 50)
            Text("Score: \(blitzScore)")
                .font(.headline)
        }
        .padding(.horizontal)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Blitz timer")
        .accessibilityValue("\(blitzTimeMs / 1000) seconds remaining, score \(blitzScore)")
    }

    private func blitzQuestionContent(_ question: QuizGenerator.QuizQuestion) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(question.question)
                .font(.title3.weight(.medium))

            let options = question.options.asStrings
            ForEach(Array(options.enumerated()), id: \.offset) { index, option in
                Button {
                    guard selectedAnswer == nil else { return }
                    selectedAnswer = index
                    blitzTotal += 1
                    let correct = index == Int(question.correctIndex)
                    if correct {
                        blitzScore += 1
                        blitzTimeMs = min(blitzTimeMs + 3_000, Self.blitzDurationMs)
                    }
                    learnVM.recordQuizAnswer(category: question.category, correct: correct)
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                        nextQuestion()
                    }
                } label: {
                    HStack {
                        Text(option).foregroundStyle(.primary)
                        Spacer()
                        if selectedAnswer == index {
                            Image(systemName: index == Int(question.correctIndex) ? "checkmark.circle.fill" : "xmark.circle.fill")
                                .foregroundStyle(index == Int(question.correctIndex) ? .green : .red)
                                .accessibilityHidden(true)
                        }
                    }
                    .padding()
                    .background(blitzOptionBg(index: index, correctIndex: Int(question.correctIndex)))
                    .cornerRadius(8)
                }
            }
        }
        .padding(.horizontal)
    }

    private var blitzResultsCard: some View {
        VStack(spacing: 12) {
            Image(systemName: "clock.badge.checkmark")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text("Time's up!")
                .font(.title2.bold())
            Text("Score: \(blitzScore) / \(blitzTotal)")
                .font(.headline)
            if blitzTotal > 0 {
                Text("Accuracy: \(blitzScore * 100 / blitzTotal)%")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Button("Play Again") { startBlitz() }
                .buttonStyle(.borderedProminent)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color(.systemGray6))
        .cornerRadius(12)
        .padding(.horizontal)
    }

    private func startBlitz() {
        blitzTimeMs = Self.blitzDurationMs
        blitzScore = 0
        blitzTotal = 0
        blitzFinished = false
        blitzActive = true
        nextQuestion()

        blitzTimer?.invalidate()
        blitzTimer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { _ in
            DispatchQueue.main.async {
                blitzTimeMs -= 100
                if blitzTimeMs <= 0 {
                    blitzTimeMs = 0
                    blitzActive = false
                    blitzFinished = true
                    blitzTimer?.invalidate()
                }
            }
        }
    }

    private func blitzOptionBg(index: Int, correctIndex: Int) -> Color {
        guard selectedAnswer != nil else { return Color(.systemGray6) }
        if index == correctIndex { return Color.green.opacity(0.2) }
        if index == selectedAnswer { return Color.red.opacity(0.2) }
        return Color(.systemGray6)
    }

    // MARK: - Standard Mode

    private func nextQuestion() {
        currentQuestion = QuizGenerator.shared.generate(category: useAllCategories ? nil : selectedCategory)
        selectedAnswer = nil
        showExplanation = false
    }

    private func optionBackground(index: Int, correctIndex: Int) -> Color {
        guard showExplanation else { return Color(.systemGray6) }
        if index == correctIndex { return Color.green.opacity(0.2) }
        if index == selectedAnswer { return Color.red.opacity(0.2) }
        return Color(.systemGray6)
    }
}
