import SwiftUI
import shared

struct LearningProgressView: View {
    @EnvironmentObject var learnVM: LearnViewModel
    @EnvironmentObject var practiceTimerVM: PracticeTimerViewModel
    @State private var showResetConfirmation = false

    var body: some View {
        let _ = learnVM.stateVersion // observe changes

        ScrollView {
            VStack(spacing: 20) {
                // Practice Time
                VStack(alignment: .leading, spacing: 8) {
                    Text("Practice Time")
                        .font(.headline)
                        .accessibilityAddTraits(.isHeader)
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Total: \(practiceTimerVM.totalTimeFormatted)")
                            Text("Sessions: \(practiceTimerVM.data.totalSessions)")
                            HStack(spacing: 4) {
                                Text("Today: \(practiceTimerVM.todayMinutes)m")
                                Text("/")
                                    .foregroundStyle(.secondary)
                                Text("\(practiceTimerVM.data.dailyGoal)m goal")
                                    .foregroundStyle(.secondary)
                            }
                            Text("Longest: \(practiceTimerVM.data.longestSession)m")
                        }
                        .font(.subheadline)
                        Spacer()
                        VStack {
                            ZStack {
                                Circle()
                                    .stroke(Color(.systemGray4), lineWidth: 6)
                                    .frame(width: 50, height: 50)
                                Circle()
                                    .trim(from: 0, to: min(Double(practiceTimerVM.dailyProgress), 1.0))
                                    .stroke(practiceTimerVM.dailyProgress >= 1.0 ? Color.green : Color.accentColor,
                                            style: StrokeStyle(lineWidth: 6, lineCap: .round))
                                    .frame(width: 50, height: 50)
                                    .rotationEffect(.degrees(-90))
                                Text("\(Int(min(practiceTimerVM.dailyProgress, 1.0) * 100))%")
                                    .font(.caption2.weight(.bold))
                            }
                            Text("Daily Goal")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                        .accessibilityCombined(label: "Daily Goal", value: "\(Int(practiceTimerVM.dailyProgress * 100)) percent")
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
                .padding(.horizontal)

                // Streak
                HStack(spacing: 24) {
                    statCard(title: "Current Streak", value: "\(learnVM.currentDayStreak())", icon: "flame.fill", color: .orange)
                    statCard(title: "Best Streak", value: "\(learnVM.bestDayStreak())", icon: "trophy.fill", color: .yellow)
                }
                .padding(.horizontal)

                // Lessons
                let totalLessons = (TheoryLessons.shared.ALL as! [TheoryLesson]).count
                let completedLessons = learnVM.completedLessonCount()
                let passedQuizzes = learnVM.passedQuizCount()

                VStack(alignment: .leading, spacing: 8) {
                    Text("Theory Lessons")
                        .font(.headline)
                        .accessibilityAddTraits(.isHeader)
                    HStack {
                        progressCircle(value: Double(completedLessons), total: Double(totalLessons), label: "Completed")
                        Spacer()
                        progressCircle(value: Double(passedQuizzes), total: Double(totalLessons), label: "Quizzes Passed")
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
                .padding(.horizontal)

                // Quiz Stats
                statsSection(title: "Theory Quiz", stats: learnVM.quizStats())

                // Interval Stats
                statsSection(title: "Interval Trainer", stats: learnVM.intervalStats())

                // Note Quiz
                statsSection(title: "Note Quiz", stats: learnVM.noteQuizStats())

                // Chord Ear Training
                statsSection(title: "Chord Ear Training", stats: learnVM.chordEarStats())

                // Scale Practice
                statsSection(title: "Scale Practice", stats: learnVM.scalePracticeStats())

                Button(role: .destructive) {
                    showResetConfirmation = true
                } label: {
                    Label("Reset All Progress", systemImage: "arrow.counterclockwise")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .padding(.horizontal)
                .padding(.top, 8)
            }
            .padding(.vertical)
        }
        .navigationTitle("Learning Progress")
        .confirmationDialog(
            "Reset All Progress?",
            isPresented: $showResetConfirmation,
            titleVisibility: .visible
        ) {
            Button("Reset", role: .destructive) {
                learnVM.clearAllProgress()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will permanently delete all your learning progress, streaks, and quiz statistics. This action cannot be undone.")
        }
    }

    private func statCard(title: String, value: String, icon: String, color: Color) -> some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title)
                .foregroundStyle(color)
            Text(value)
                .font(.title.weight(.bold))
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
        .accessibilityCombined(label: title, value: value)
    }

    private func progressCircle(value: Double, total: Double, label: String) -> some View {
        VStack(spacing: 4) {
            ZStack {
                Circle()
                    .stroke(Color(.systemGray4), lineWidth: 6)
                    .frame(width: 60, height: 60)
                Circle()
                    .trim(from: 0, to: total > 0 ? value / total : 0)
                    .stroke(Color.accentColor, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                    .frame(width: 60, height: 60)
                    .rotationEffect(.degrees(-90))
                Text("\(Int(value))/\(Int(total))")
                    .font(.caption2.weight(.medium))
            }
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .accessibilityCombined(label: label, value: "\(Int(value)) of \(Int(total))")
    }

    private func statsSection(title: String, stats: LearningStats) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
            HStack {
                VStack(alignment: .leading) {
                    Text("Total: \(stats.total)")
                    Text("Correct: \(stats.correct)")
                    Text("Best Streak: \(stats.bestStreak)")
                }
                .font(.subheadline)
                Spacer()
                if stats.total > 0 {
                    VStack {
                        ZStack {
                            Circle()
                                .stroke(Color(.systemGray4), lineWidth: 6)
                                .frame(width: 50, height: 50)
                            Circle()
                                .trim(from: 0, to: Double(stats.accuracyPercent) / 100)
                                .stroke(accuracyColor(stats.accuracyPercent), style: StrokeStyle(lineWidth: 6, lineCap: .round))
                                .frame(width: 50, height: 50)
                                .rotationEffect(.degrees(-90))
                            Text("\(stats.accuracyPercent)%")
                                .font(.caption2.weight(.bold))
                        }
                        Text("Accuracy")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    .accessibilityCombined(label: "Accuracy", value: "\(stats.accuracyPercent) percent")
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
        .padding(.horizontal)
    }

    private func accuracyColor(_ percent: Int) -> Color {
        if percent >= 80 { return .green }
        if percent >= 60 { return .yellow }
        return .orange
    }
}
