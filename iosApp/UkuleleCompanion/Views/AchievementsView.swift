import SwiftUI
import shared

struct AchievementDef: Identifiable {
    let id: String
    let title: String
    let description: String
    let icon: String
    let category: AchievementCategorySwift
    let condition: (AchievementContextSwift) -> Bool
}

enum AchievementCategorySwift: String, CaseIterable {
    case practice = "Practice"
    case learning = "Learning"
    case ear = "Ear Training"
    case songs = "Songs"
    case chords = "Chords"

    var icon: String {
        switch self {
        case .practice: return "star.fill"
        case .learning: return "book.fill"
        case .ear: return "ear.fill"
        case .songs: return "music.note.list"
        case .chords: return "heart.fill"
        }
    }
}

struct AchievementContextSwift {
    let currentStreak: Int
    let bestStreak: Int
    let completedLessons: Int
    let totalLessons: Int
    let quizCorrect: Int
    let quizBestStreak: Int
    let intervalTotal: Int
    let intervalCorrect: Int
    let chordEarTotal: Int
    let chordEarCorrect: Int
    let scalePracticeTotal: Int
    let songsCount: Int
    let favoritesCount: Int
}

private nonisolated(unsafe) let allAchievements: [AchievementDef] = [
    // Practice
    AchievementDef(id: "streak_3", title: "Getting Started", description: "Maintain a 3-day practice streak", icon: "star.fill", category: .practice) { $0.currentStreak >= 3 },
    AchievementDef(id: "streak_7", title: "Week Warrior", description: "Maintain a 7-day practice streak", icon: "star.fill", category: .practice) { $0.currentStreak >= 7 },
    AchievementDef(id: "streak_30", title: "Monthly Master", description: "Maintain a 30-day practice streak", icon: "star.fill", category: .practice) { $0.currentStreak >= 30 },
    AchievementDef(id: "scale_first", title: "Scale Runner", description: "Complete your first scale practice session", icon: "figure.walk", category: .practice) { $0.scalePracticeTotal >= 1 },
    AchievementDef(id: "scale_25", title: "Scale Explorer", description: "Complete 25 scale practice exercises", icon: "figure.walk", category: .practice) { $0.scalePracticeTotal >= 25 },

    // Learning
    AchievementDef(id: "first_lesson", title: "First Steps", description: "Complete your first theory lesson", icon: "book.fill", category: .learning) { $0.completedLessons >= 1 },
    AchievementDef(id: "half_lessons", title: "Halfway There", description: "Complete half of all theory lessons", icon: "book.fill", category: .learning) { $0.totalLessons > 0 && $0.completedLessons >= $0.totalLessons / 2 },
    AchievementDef(id: "all_lessons", title: "Scholar", description: "Complete all theory lessons", icon: "book.fill", category: .learning) { $0.totalLessons > 0 && $0.completedLessons >= $0.totalLessons },
    AchievementDef(id: "quiz_10", title: "Quiz Starter", description: "Answer 10 quiz questions correctly", icon: "book.fill", category: .learning) { $0.quizCorrect >= 10 },
    AchievementDef(id: "quiz_50", title: "Quiz Whiz", description: "Answer 50 quiz questions correctly", icon: "book.fill", category: .learning) { $0.quizCorrect >= 50 },
    AchievementDef(id: "quiz_100", title: "Quiz Master", description: "Answer 100 quiz questions correctly", icon: "book.fill", category: .learning) { $0.quizCorrect >= 100 },
    AchievementDef(id: "perfect_streak_5", title: "Sharp Mind", description: "Get 5 quiz questions correct in a row", icon: "star.fill", category: .learning) { $0.quizBestStreak >= 5 },
    AchievementDef(id: "perfect_streak_10", title: "Perfect Score", description: "Get 10 quiz questions correct in a row", icon: "star.fill", category: .learning) { $0.quizBestStreak >= 10 },

    // Ear
    AchievementDef(id: "ear_first", title: "Tuning In", description: "Complete your first ear training exercise", icon: "ear.fill", category: .ear) { $0.intervalTotal >= 1 || $0.chordEarTotal >= 1 },
    AchievementDef(id: "ear_50", title: "Good Ear", description: "Complete 50 ear training exercises", icon: "ear.fill", category: .ear) { ($0.intervalTotal + $0.chordEarTotal) >= 50 },
    AchievementDef(id: "ear_accuracy_80", title: "Golden Ear", description: "Achieve 80% accuracy in ear training (50+ exercises)", icon: "ear.fill", category: .ear) {
        let total = $0.intervalTotal + $0.chordEarTotal
        let correct = $0.intervalCorrect + $0.chordEarCorrect
        return total >= 50 && correct * 100 / total >= 80
    },

    // Songs
    AchievementDef(id: "first_song", title: "Songwriter", description: "Create your first song in the songbook", icon: "music.note.list", category: .songs) { $0.songsCount >= 1 },
    AchievementDef(id: "songs_5", title: "Setlist Builder", description: "Create 5 songs in the songbook", icon: "music.note.list", category: .songs) { $0.songsCount >= 5 },
    AchievementDef(id: "songs_10", title: "Prolific Writer", description: "Create 10 songs in the songbook", icon: "music.note.list", category: .songs) { $0.songsCount >= 10 },

    // Chords
    AchievementDef(id: "fav_5", title: "Collector", description: "Save 5 favorite chord voicings", icon: "heart.fill", category: .chords) { $0.favoritesCount >= 5 },
    AchievementDef(id: "fav_25", title: "Chord Hoarder", description: "Save 25 favorite chord voicings", icon: "heart.fill", category: .chords) { $0.favoritesCount >= 25 },
]

struct AchievementsView: View {
    @EnvironmentObject var learnVM: LearnViewModel
    @State private var showReviewPrompt = false
    @State private var reviewAchievementTitle = ""

    var body: some View {
        let _ = learnVM.stateVersion
        let context = buildContext()
        let unlocked = learnVM.unlockedAchievementIds

        ScrollView {
            VStack(spacing: 16) {
                // Summary
                let unlockedCount = allAchievements.filter { unlocked.contains($0.id) || $0.condition(context) }.count
                Text("\(unlockedCount) / \(allAchievements.count) Achievements")
                    .font(.headline)
                    .padding()

                ForEach(AchievementCategorySwift.allCases, id: \.self) { category in
                    let categoryAchievements = allAchievements.filter { $0.category == category }
                    if !categoryAchievements.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Label(category.rawValue, systemImage: category.icon)
                                .font(.headline)
                                .padding(.horizontal)
                                .accessibilityAddTraits(.isHeader)

                            ForEach(categoryAchievements) { achievement in
                                let isUnlocked = unlocked.contains(achievement.id) || achievement.condition(context)
                                achievementRow(achievement, unlocked: isUnlocked)
                            }
                        }
                    }
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Achievements")
        .task(id: learnVM.stateVersion) {
            checkNewAchievements(context: context, unlocked: unlocked)
        }
        .alert("Enjoying Ukulele Companion?", isPresented: $showReviewPrompt) {
            Button("Yes, I love it!") {
                ReviewPromptManager.shared.recordReviewed()
                ReviewPromptManager.shared.requestReview()
            }
            Button("Not really", role: .cancel) {
                ReviewPromptManager.shared.recordDismissal()
            }
        } message: {
            Text("You just earned \(reviewAchievementTitle)!")
        }
    }

    private func achievementRow(_ achievement: AchievementDef, unlocked: Bool) -> some View {
        HStack(spacing: 12) {
            Image(systemName: achievement.icon)
                .font(.title2)
                .foregroundStyle(unlocked ? Color.accentColor : Color.gray)
                .frame(width: 40)

            VStack(alignment: .leading, spacing: 2) {
                Text(achievement.title)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(unlocked ? .primary : .secondary)
                Text(achievement.description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            if unlocked {
                Image(systemName: "checkmark.seal.fill")
                    .foregroundStyle(.green)
            } else {
                Image(systemName: "lock.fill")
                    .foregroundStyle(.gray)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(10)
        .opacity(unlocked ? 1.0 : 0.7)
        .padding(.horizontal)
        .accessibilityCombined(label: achievement.title, value: "\(achievement.description). \(unlocked ? "Unlocked" : "Locked")")
    }

    private func buildContext() -> AchievementContextSwift {
        let totalLessons = (TheoryLessons.shared.ALL as! [TheoryLesson]).count
        let quizStats = learnVM.quizStats()
        let intervalStats = learnVM.intervalStats()
        let chordEarStats = learnVM.chordEarStats()
        let scaleStats = learnVM.scalePracticeStats()

        // Count songs from chord_sheets UserDefaults
        let songsData = UserDefaults.standard.data(forKey: "chord_sheets")
        var songsCount = 0
        if let data = songsData,
           let arr = try? JSONDecoder().decode([AnyCodable].self, from: data) {
            songsCount = arr.count
        }

        // Count favorites
        let favData = UserDefaults.standard.data(forKey: "favorite_voicings")
        var favCount = 0
        if let data = favData,
           let arr = try? JSONDecoder().decode([AnyCodable].self, from: data) {
            favCount = arr.count
        }

        return AchievementContextSwift(
            currentStreak: learnVM.currentDayStreak(),
            bestStreak: learnVM.bestDayStreak(),
            completedLessons: learnVM.completedLessonCount(),
            totalLessons: totalLessons,
            quizCorrect: quizStats.correct,
            quizBestStreak: quizStats.bestStreak,
            intervalTotal: intervalStats.total,
            intervalCorrect: intervalStats.correct,
            chordEarTotal: chordEarStats.total,
            chordEarCorrect: chordEarStats.correct,
            scalePracticeTotal: scaleStats.total,
            songsCount: songsCount,
            favoritesCount: favCount
        )
    }

    private func checkNewAchievements(context: AchievementContextSwift, unlocked: Set<String>) {
        var firstNewTitle: String?
        for achievement in allAchievements {
            if !unlocked.contains(achievement.id) && achievement.condition(context) {
                learnVM.unlockAchievement(achievement.id)
                if firstNewTitle == nil { firstNewTitle = achievement.title }
            }
        }
        if let title = firstNewTitle, ReviewPromptManager.shared.isEligible() {
            ReviewPromptManager.shared.recordPromptShown()
            reviewAchievementTitle = title
            showReviewPrompt = true
        }
    }
}

private struct AnyCodable: Codable {
    init(from decoder: Decoder) throws {
        _ = try decoder.singleValueContainer()
    }
    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(true)
    }
}
