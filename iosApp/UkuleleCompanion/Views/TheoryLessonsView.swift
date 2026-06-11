import SwiftUI
import shared

struct TheoryLessonsView: View {
    @EnvironmentObject var learnVM: LearnViewModel

    var body: some View {
        let modules = TheoryLessons.shared.MODULES.asStrings
        let byModule = TheoryLessons.shared.byModule() as? [String: [TheoryLesson]] ?? [:]

        List {
            ForEach(modules, id: \.self) { module in
                Section(module) {
                    if let lessons = byModule[module] {
                        ForEach(lessons, id: \.id) { lesson in
                            NavigationLink {
                                TheoryLessonDetailView(lesson: lesson)
                            } label: {
                                HStack {
                                    Text(lesson.title)
                                    Spacer()
                                    if learnVM.isLessonQuizPassed(lesson.id) {
                                        Image(systemName: "checkmark.seal.fill")
                                            .foregroundStyle(.green)
                                            .font(.caption)
                                            .accessibilityLabel("Quiz passed")
                                    } else if learnVM.isLessonCompleted(lesson.id) {
                                        Image(systemName: "checkmark")
                                            .foregroundStyle(.blue)
                                            .font(.caption)
                                            .accessibilityLabel("Lesson completed")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Theory Lessons")
    }
}

private struct TheoryLessonDetailView: View {
    let lesson: TheoryLesson
    @EnvironmentObject var learnVM: LearnViewModel
    @State private var selectedAnswer: Int? = nil
    @State private var showResult = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(lesson.content)
                    .font(.body)

                if let keyPoints = lesson.keyPoints as? [String], !keyPoints.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Key Points")
                            .font(.headline)
                            .accessibilityAddTraits(.isHeader)
                        ForEach(keyPoints, id: \.self) { point in
                            HStack(alignment: .top) {
                                Text("•")
                                Text(point)
                            }
                            .font(.subheadline)
                        }
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                }

                Divider()

                Text("Mini Quiz")
                    .font(.headline)
                    .accessibilityAddTraits(.isHeader)

                Text(lesson.quizQuestion)
                    .font(.subheadline)

                if let options = lesson.quizOptions as? [String] {
                    ForEach(Array(options.enumerated()), id: \.offset) { index, option in
                        Button {
                            if !showResult {
                                selectedAnswer = index
                                showResult = true
                                let correct = index == Int(lesson.quizCorrectIndex)
                                if correct {
                                    learnVM.markLessonQuizPassed(lesson.id)
                                }
                            }
                        } label: {
                            HStack {
                                Text(option)
                                    .foregroundStyle(.primary)
                                Spacer()
                                if showResult && index == Int(lesson.quizCorrectIndex) {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundStyle(.green)
                                        .accessibilityLabel("Correct answer")
                                } else if showResult && index == selectedAnswer {
                                    Image(systemName: "xmark.circle.fill")
                                        .foregroundStyle(.red)
                                        .accessibilityLabel("Incorrect answer")
                                }
                            }
                            .padding()
                            .background(quizOptionBackground(index: index))
                            .cornerRadius(8)
                        }
                    }
                }

                if showResult {
                    Text(lesson.quizExplanation)
                        .font(.callout)
                        .foregroundStyle(.secondary)
                        .padding()
                        .background(Color(.systemGray6))
                        .cornerRadius(8)
                }
            }
            .padding()
        }
        .navigationTitle(lesson.title)
        .onAppear {
            learnVM.markLessonCompleted(lesson.id)
        }
    }

    private func quizOptionBackground(index: Int) -> Color {
        guard showResult else { return Color(.systemGray6) }
        if index == Int(lesson.quizCorrectIndex) {
            return Color.green.opacity(0.2)
        }
        if index == selectedAnswer {
            return Color.red.opacity(0.2)
        }
        return Color(.systemGray6)
    }
}
