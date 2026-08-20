import SwiftUI
import shared

struct AchievementDisplayInfo: Identifiable {
    let id: String
    let title: String
    let description: String
    let icon: String
    let category: AchievementCategorySwift
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

    var localizedLabel: String {
        switch self {
        case .practice: return String(localized: "achievement_category_practice")
        case .learning: return String(localized: "achievement_category_learning")
        case .ear: return String(localized: "achievement_category_ear")
        case .songs: return String(localized: "achievement_category_songs")
        case .chords: return String(localized: "achievement_category_chords")
        }
    }
}

private nonisolated(unsafe) let allAchievements: [AchievementDisplayInfo] = [
    // Practice
    AchievementDisplayInfo(id: "streak_3", title: String(localized: "achievement_streak_3_title"), description: String(localized: "achievement_streak_3_desc"), icon: "star.fill", category: .practice),
    AchievementDisplayInfo(id: "streak_7", title: String(localized: "achievement_streak_7_title"), description: String(localized: "achievement_streak_7_desc"), icon: "star.fill", category: .practice),
    AchievementDisplayInfo(id: "streak_30", title: String(localized: "achievement_streak_30_title"), description: String(localized: "achievement_streak_30_desc"), icon: "star.fill", category: .practice),
    AchievementDisplayInfo(id: "scale_first", title: String(localized: "achievement_scale_first_title"), description: String(localized: "achievement_scale_first_desc"), icon: "figure.walk", category: .practice),
    AchievementDisplayInfo(id: "scale_25", title: String(localized: "achievement_scale_25_title"), description: String(localized: "achievement_scale_25_desc"), icon: "figure.walk", category: .practice),

    // Learning
    AchievementDisplayInfo(id: "first_lesson", title: String(localized: "achievement_first_lesson_title"), description: String(localized: "achievement_first_lesson_desc"), icon: "book.fill", category: .learning),
    AchievementDisplayInfo(id: "half_lessons", title: String(localized: "achievement_half_lessons_title"), description: String(localized: "achievement_half_lessons_desc"), icon: "book.fill", category: .learning),
    AchievementDisplayInfo(id: "all_lessons", title: String(localized: "achievement_all_lessons_title"), description: String(localized: "achievement_all_lessons_desc"), icon: "book.fill", category: .learning),
    AchievementDisplayInfo(id: "quiz_10", title: String(localized: "achievement_quiz_10_title"), description: String(localized: "achievement_quiz_10_desc"), icon: "book.fill", category: .learning),
    AchievementDisplayInfo(id: "quiz_50", title: String(localized: "achievement_quiz_50_title"), description: String(localized: "achievement_quiz_50_desc"), icon: "book.fill", category: .learning),
    AchievementDisplayInfo(id: "quiz_100", title: String(localized: "achievement_quiz_100_title"), description: String(localized: "achievement_quiz_100_desc"), icon: "book.fill", category: .learning),
    AchievementDisplayInfo(id: "perfect_streak_5", title: String(localized: "achievement_perfect_streak_5_title"), description: String(localized: "achievement_perfect_streak_5_desc"), icon: "star.fill", category: .learning),
    AchievementDisplayInfo(id: "perfect_streak_10", title: String(localized: "achievement_perfect_streak_10_title"), description: String(localized: "achievement_perfect_streak_10_desc"), icon: "star.fill", category: .learning),

    // Ear
    AchievementDisplayInfo(id: "ear_first", title: String(localized: "achievement_ear_first_title"), description: String(localized: "achievement_ear_first_desc"), icon: "ear.fill", category: .ear),
    AchievementDisplayInfo(id: "ear_50", title: String(localized: "achievement_ear_50_title"), description: String(localized: "achievement_ear_50_desc"), icon: "ear.fill", category: .ear),
    AchievementDisplayInfo(id: "ear_accuracy_80", title: String(localized: "achievement_ear_accuracy_80_title"), description: String(localized: "achievement_ear_accuracy_80_desc"), icon: "ear.fill", category: .ear),

    // Songs
    AchievementDisplayInfo(id: "first_song", title: String(localized: "achievement_first_song_title"), description: String(localized: "achievement_first_song_desc"), icon: "music.note.list", category: .songs),
    AchievementDisplayInfo(id: "songs_5", title: String(localized: "achievement_songs_5_title"), description: String(localized: "achievement_songs_5_desc"), icon: "music.note.list", category: .songs),
    AchievementDisplayInfo(id: "songs_10", title: String(localized: "achievement_songs_10_title"), description: String(localized: "achievement_songs_10_desc"), icon: "music.note.list", category: .songs),

    // Chords
    AchievementDisplayInfo(id: "fav_5", title: String(localized: "achievement_fav_5_title"), description: String(localized: "achievement_fav_5_desc"), icon: "heart.fill", category: .chords),
    AchievementDisplayInfo(id: "fav_25", title: String(localized: "achievement_fav_25_title"), description: String(localized: "achievement_fav_25_desc"), icon: "heart.fill", category: .chords),
]

/// Displays achievement progress. Earning them is not this screen's job — the
/// app-wide `AchievementWatcher` driven from `ContentView` records unlocks, so
/// they happen whether or not the user ever opens this screen.
struct AchievementsView: View {
    @EnvironmentObject var learnVM: LearnViewModel
    @EnvironmentObject var favoritesVM: FavoritesViewModel
    @EnvironmentObject var songbookVM: SongbookViewModel

    var body: some View {
        let _ = learnVM.stateVersion
        let context = AchievementWatcher.context(
            learnVM: learnVM,
            songsCount: songbookVM.songs.count,
            favoritesCount: favoritesVM.favorites.count
        )
        let unlocked = learnVM.unlockedAchievementIds
        let earnedIds = Achievements.shared.earned(context: context) as? Set<String> ?? Set()

        ScrollView {
            VStack(spacing: 16) {
                // Summary
                let unlockedCount = allAchievements.filter { unlocked.contains($0.id) || earnedIds.contains($0.id) }.count
                Text(String(format: String(localized: "achievements_unlocked"), unlockedCount))
                    .font(.headline)
                    .padding()

                ForEach(AchievementCategorySwift.allCases, id: \.self) { category in
                    let categoryAchievements = allAchievements.filter { $0.category == category }
                    if !categoryAchievements.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Label(category.localizedLabel, systemImage: category.icon)
                                .font(.headline)
                                .padding(.horizontal)
                                .accessibilityAddTraits(.isHeader)

                            ForEach(categoryAchievements) { achievement in
                                let isUnlocked = unlocked.contains(achievement.id) || earnedIds.contains(achievement.id)
                                achievementRow(achievement, unlocked: isUnlocked)
                            }
                        }
                    }
                }
            }
            .padding(.vertical)
        }
        .navigationTitle(String(localized: "achievements_title"))
    }

    private func achievementRow(_ achievement: AchievementDisplayInfo, unlocked: Bool) -> some View {
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
}
