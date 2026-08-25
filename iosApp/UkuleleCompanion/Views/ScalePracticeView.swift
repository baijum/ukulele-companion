import SwiftUI
import shared

struct ScalePracticeView: View {
    @StateObject private var viewModel = ScalePracticeViewModel()
    @EnvironmentObject var learnVM: LearnViewModel
    @EnvironmentObject var settings: SettingsViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Picker("Mode", selection: $viewModel.mode) {
                    ForEach(ScalePracticeViewModel.PracticeMode.allCases, id: \.self) { m in
                        Text(m.rawValue).tag(m)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)

                switch viewModel.mode {
                case .playAlong:
                    playAlongContent
                case .quiz:
                    quizContent
                case .earTraining:
                    earTrainingContent
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Scale Practice")
    }

    private var categoryFilterRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                categoryChip(label: "All", isSelected: viewModel.selectedCategory == nil) {
                    viewModel.selectedCategory = nil
                }
                ForEach(ScaleCategory.entries, id: \.name) { cat in
                    categoryChip(label: cat.label, isSelected: viewModel.selectedCategory == cat) {
                        viewModel.selectedCategory = cat
                    }
                }
            }
            .padding(.horizontal)
        }
    }

    private func categoryChip(label: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.caption)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(isSelected ? Color.accentColor : Color(.systemGray5))
                .foregroundStyle(isSelected ? .white : .primary)
                .clipShape(Capsule())
        }
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    @ViewBuilder
    private var playAlongContent: some View {
        VStack(spacing: 12) {
            categoryFilterRow

            HStack {
                Text("Root")
                    .font(.headline)
                Picker("Root", selection: $viewModel.selectedRoot) {
                    ForEach(0..<12, id: \.self) { i in
                        Text(viewModel.rootNoteNames[i]).tag(Int32(i))
                    }
                }
            }
            .padding(.horizontal)

            HStack {
                Text("Scale")
                    .font(.headline)
                Picker("Scale", selection: $viewModel.selectedScale) {
                    Text("Select...").tag(nil as Scale?)
                    ForEach(viewModel.availableScales, id: \.name) { scale in
                        Text(scale.name).tag(scale as Scale?)
                    }
                }
            }
            .padding(.horizontal)

            HStack {
                Text("BPM: \(Int(viewModel.bpm))")
                    .font(.headline)
                Slider(value: $viewModel.bpm, in: 40...200, step: 5)
            }
            .padding(.horizontal)

            Picker("Direction", selection: $viewModel.playDirection) {
                ForEach(ScalePracticeViewModel.PlayDirection.allCases, id: \.self) { d in
                    Text(d.rawValue).tag(d)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal)

            Toggle("Show Fretboard", isOn: $viewModel.showFretboard)
                .padding(.horizontal)

            if viewModel.showFretboard {
                Picker("Fret Position", selection: $viewModel.fretPosition) {
                    ForEach(ScalePracticeViewModel.FretPosition.allCases, id: \.self) { pos in
                        Text(pos.rawValue).tag(pos)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)
            }

            Toggle("Loop", isOn: $viewModel.loopPlayback)
                .padding(.horizontal)

            if viewModel.isPlaying {
                Button("Stop") { viewModel.stopPlayAlong() }
                    .buttonStyle(.borderedProminent)
                    .tint(.red)
            } else {
                Button("Play Scale") {
                    viewModel.saveSettings()
                    viewModel.startPlayAlong()
                }
                .buttonStyle(.borderedProminent)
                .disabled(viewModel.selectedScale == nil)
            }

            if !viewModel.scaleNotes.isEmpty {
                HStack(spacing: 4) {
                    ForEach(Array(viewModel.scaleNotes.enumerated()), id: \.offset) { i, pc in
                        Text(Notes.shared.pitchClassToName(pitchClass: Int32(pc)))
                            .font(.caption)
                            .padding(6)
                            .background(i == viewModel.currentNoteIndex ? Color.accentColor : Color(.systemGray5))
                            .foregroundStyle(i == viewModel.currentNoteIndex ? .white : .primary)
                            .cornerRadius(4)
                            .accessibilityAddTraits(i == viewModel.currentNoteIndex ? .isSelected : [])
                    }
                }
                .padding(.horizontal)

                if viewModel.showFretboard {
                    scaleFretboardView
                }
            }
        }
    }

    @ViewBuilder
    private var scaleFretboardView: some View {
        let scaleSet = Set(viewModel.scaleNotes)
        let currentPC = viewModel.currentNoteIndex >= 0 && viewModel.currentNoteIndex < viewModel.scaleNotes.count
            ? viewModel.scaleNotes[viewModel.currentNoteIndex] : -1
        let fretRange = viewModel.fretPosition.range(lastFret: settings.lastFret)
        let tuning: [Int] = [7, 0, 4, 9] // G C E A (High-G)

        VStack(spacing: 0) {
            ForEach(Array((0..<4).reversed()), id: \.self) { stringIdx in
                HStack(spacing: 0) {
                    ForEach(Array(fretRange), id: \.self) { fret in
                        let pc = (tuning[stringIdx] + fret) % 12
                        let isScale = scaleSet.contains(pc)
                        let isCurrent = pc == currentPC
                        let isRoot = pc == Int(viewModel.selectedRoot)
                        ZStack {
                            Rectangle()
                                .stroke(Color.secondary.opacity(0.3), lineWidth: 0.5)
                            if isScale {
                                let dotColor = isCurrent
                                    ? Color.accentColor
                                    : (isRoot ? Color.orange : Color.blue.opacity(0.6))
                                Circle()
                                    .fill(dotColor)
                                    .frame(width: 14, height: 14)
                            }
                        }
                        .frame(width: 28, height: 22)
                    }
                }
            }
        }
        .padding(.horizontal)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Scale fretboard diagram")
    }

    @ViewBuilder
    private var quizContent: some View {
        VStack(spacing: 12) {
            if let q = viewModel.currentQuizQuestion {
                Text(q.question)
                    .font(.title3.weight(.medium))
                    .padding(.horizontal)

                if let options = q.options as? [String] {
                    ForEach(Array(options.enumerated()), id: \.offset) { index, option in
                        Button {
                            if !viewModel.quizShowResult {
                                viewModel.answerQuiz(index, learnVM: learnVM)
                            }
                        } label: {
                            HStack {
                                Text(option).foregroundStyle(.primary)
                                Spacer()
                                if viewModel.quizShowResult && index == Int(q.correctIndex) {
                                    Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
                                        .accessibilityHidden(true)
                                } else if viewModel.quizShowResult && index == viewModel.quizSelectedAnswer {
                                    Image(systemName: "xmark.circle.fill").foregroundStyle(.red)
                                        .accessibilityHidden(true)
                                }
                            }
                            .padding()
                            .background(quizBg(index: index, correctIndex: Int(q.correctIndex)))
                            .cornerRadius(8)
                        }
                        .accessibilityValue(answerAccessibilityValue(
                            showResult: viewModel.quizShowResult,
                            index: index,
                            correctIndex: Int(q.correctIndex),
                            selectedAnswer: viewModel.quizSelectedAnswer
                        ))
                    }
                    .padding(.horizontal)
                }

                if viewModel.quizShowResult {
                    Text(q.explanation)
                        .font(.callout)
                        .foregroundStyle(.secondary)
                        .padding()

                    Button("Next Question") { viewModel.generateQuizQuestion() }
                        .buttonStyle(.borderedProminent)
                }
            } else {
                Button("Start Quiz") { viewModel.generateQuizQuestion() }
                    .buttonStyle(.borderedProminent)
            }
        }
    }

    @ViewBuilder
    private var earTrainingContent: some View {
        VStack(spacing: 12) {
            if let q = viewModel.currentEarQuestion {
                Button {
                    viewModel.playEarScale()
                } label: {
                    Label("Play Scale", systemImage: "play.circle.fill")
                        .font(.title2)
                }
                .buttonStyle(.borderedProminent)

                Text("Which scale is this?")
                    .font(.title3)

                if let options = q.options as? [String] {
                    ForEach(Array(options.enumerated()), id: \.offset) { index, option in
                        Button {
                            if !viewModel.earShowResult {
                                viewModel.answerEar(index, learnVM: learnVM)
                            }
                        } label: {
                            HStack {
                                Text(option).foregroundStyle(.primary)
                                Spacer()
                                if viewModel.earShowResult && index == Int(q.correctIndex) {
                                    Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
                                        .accessibilityHidden(true)
                                } else if viewModel.earShowResult && index == viewModel.earSelectedAnswer {
                                    Image(systemName: "xmark.circle.fill").foregroundStyle(.red)
                                        .accessibilityHidden(true)
                                }
                            }
                            .padding()
                            .background(earBg(index: index, correctIndex: Int(q.correctIndex)))
                            .cornerRadius(8)
                        }
                        .accessibilityValue(answerAccessibilityValue(
                            showResult: viewModel.earShowResult,
                            index: index,
                            correctIndex: Int(q.correctIndex),
                            selectedAnswer: viewModel.earSelectedAnswer
                        ))
                    }
                    .padding(.horizontal)
                }

                if viewModel.earShowResult {
                    Button("Next") { viewModel.generateEarQuestion() }
                        .buttonStyle(.borderedProminent)
                }
            } else {
                Button("Start Ear Training") { viewModel.generateEarQuestion() }
                    .buttonStyle(.borderedProminent)
            }
        }
    }

    private func answerAccessibilityValue(
        showResult: Bool,
        index: Int,
        correctIndex: Int,
        selectedAnswer: Int?
    ) -> String {
        guard showResult else { return "" }
        if index == correctIndex { return "Correct answer" }
        return index == selectedAnswer ? "Your answer, incorrect" : ""
    }

    private func quizBg(index: Int, correctIndex: Int) -> Color {
        guard viewModel.quizShowResult else { return Color(.systemGray6) }
        if index == correctIndex { return Color.green.opacity(0.2) }
        if index == viewModel.quizSelectedAnswer { return Color.red.opacity(0.2) }
        return Color(.systemGray6)
    }

    private func earBg(index: Int, correctIndex: Int) -> Color {
        guard viewModel.earShowResult else { return Color(.systemGray6) }
        if index == correctIndex { return Color.green.opacity(0.2) }
        if index == viewModel.earSelectedAnswer { return Color.red.opacity(0.2) }
        return Color(.systemGray6)
    }
}
