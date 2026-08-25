import Foundation
import shared

/// Builds the achievement context and records whatever has been newly earned.
///
/// This deliberately lives outside any screen. The check used to run only while
/// the Achievements screen was open, so a user who never opened that screen
/// never unlocked anything — and so never became a candidate for the review
/// prompt either. `ContentView` runs it for the whole app instead, and the
/// Achievements screen only displays what has already been recorded.
@MainActor
enum AchievementWatcher {

    /// Assembles the progress snapshot the achievement rules are evaluated against.
    ///
    /// Song and favourite counts are passed in from the view models that own
    /// them, so the watcher and the Achievements screen always judge the same
    /// numbers.
    static func context(
        learnVM: LearnViewModel,
        songsCount: Int,
        favoritesCount: Int
    ) -> AchievementContext {
        let quizStats = learnVM.quizStats()
        let intervalStats = learnVM.intervalStats()
        let chordEarStats = learnVM.chordEarStats()
        let scaleStats = learnVM.scalePracticeStats()
        let totalLessons = TheoryLessons.shared.ALL.asArray(of: TheoryLesson.self).count

        // Counts are clamped rather than converted: these values originate from
        // `UserDefaults` (64-bit `Int`), and a value outside `Int32` — from a
        // corrupt store or a restored backup — would trap on the way into the
        // shared rules. This runs from `ContentView`'s `.task` on every launch,
        // so a trap here is a startup crash loop. Clamping to `Int32.max` also
        // stays on the safe side of every achievement threshold. Matches
        // `ReviewPromptManager.snapshot()`.
        return AchievementContext(
            currentStreak: Int32(clamping: learnVM.currentDayStreak()),
            bestStreak: Int32(clamping: learnVM.bestDayStreak()),
            completedLessons: Int32(clamping: learnVM.completedLessonCount()),
            totalLessons: Int32(clamping: totalLessons),
            quizCorrect: Int32(clamping: quizStats.correct),
            quizTotal: Int32(clamping: quizStats.total),
            quizBestStreak: Int32(clamping: quizStats.bestStreak),
            intervalTotal: Int32(clamping: intervalStats.total),
            intervalCorrect: Int32(clamping: intervalStats.correct),
            chordEarTotal: Int32(clamping: chordEarStats.total),
            chordEarCorrect: Int32(clamping: chordEarStats.correct),
            scalePracticeTotal: Int32(clamping: scaleStats.total),
            songsCount: Int32(clamping: songsCount),
            favoritesCount: Int32(clamping: favoritesCount)
        )
    }

    /// Unlocks every achievement that has been earned but not yet recorded.
    ///
    /// Returns `true` when something was unlocked, which is the moment of
    /// success the review prompt is allowed to follow.
    @discardableResult
    static func unlockNewlyEarned(
        learnVM: LearnViewModel,
        songsCount: Int,
        favoritesCount: Int
    ) -> Bool {
        let unlocked = learnVM.unlockedAchievementIds
        let newlyEarned = Achievements.shared.checkNewlyEarned(
            context: context(learnVM: learnVM, songsCount: songsCount, favoritesCount: favoritesCount),
            alreadyUnlocked: unlocked
        )
        guard !newlyEarned.isEmpty else { return false }
        for achievement in newlyEarned {
            learnVM.unlockAchievement(achievement.id)
        }
        return true
    }
}
