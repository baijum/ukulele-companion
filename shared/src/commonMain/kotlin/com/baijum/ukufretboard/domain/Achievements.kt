package com.baijum.ukufretboard.domain

/**
 * Snapshot of the user's current stats used to evaluate achievement conditions.
 */
data class AchievementContext(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val completedLessons: Int = 0,
    val totalLessons: Int = 0,
    val quizCorrect: Int = 0,
    val quizTotal: Int = 0,
    val quizBestStreak: Int = 0,
    val intervalTotal: Int = 0,
    val intervalCorrect: Int = 0,
    val chordEarTotal: Int = 0,
    val chordEarCorrect: Int = 0,
    val scalePracticeTotal: Int = 0,
    val songsCount: Int = 0,
    val favoritesCount: Int = 0,
)

/**
 * Categories of achievements for display grouping.
 */
enum class AchievementCategory(val label: String) {
    PRACTICE("Practice"),
    LEARNING("Learning"),
    EAR("Ear Training"),
    CHORDS("Chords"),
    SONGS("Songs"),
}

/**
 * Platform-neutral definition of a single achievement.
 *
 * Platform layers add display concerns (icons, localized strings)
 * keyed by [id].
 */
data class AchievementDef(
    val id: String,
    val category: AchievementCategory,
    val condition: (AchievementContext) -> Boolean,
)

/**
 * Single source of truth for all achievement definitions.
 *
 * Both Android and iOS map these IDs to platform-specific icons
 * and localized strings in their respective UI layers.
 */
object Achievements {

    val ALL: List<AchievementDef> = listOf(
        // ── Practice Milestones ──────────────────────────────────────
        AchievementDef("streak_3", AchievementCategory.PRACTICE) { it.currentStreak >= 3 },
        AchievementDef("streak_7", AchievementCategory.PRACTICE) { it.currentStreak >= 7 },
        AchievementDef("streak_30", AchievementCategory.PRACTICE) { it.currentStreak >= 30 },

        // ── Theory Learning ──────────────────────────────────────────
        AchievementDef("first_lesson", AchievementCategory.LEARNING) { it.completedLessons >= 1 },
        AchievementDef("half_lessons", AchievementCategory.LEARNING) {
            it.totalLessons > 0 && it.completedLessons >= it.totalLessons / 2
        },
        AchievementDef("all_lessons", AchievementCategory.LEARNING) {
            it.totalLessons > 0 && it.completedLessons >= it.totalLessons
        },

        // ── Quiz Achievements ────────────────────────────────────────
        AchievementDef("quiz_10", AchievementCategory.LEARNING) { it.quizCorrect >= 10 },
        AchievementDef("quiz_50", AchievementCategory.LEARNING) { it.quizCorrect >= 50 },
        AchievementDef("quiz_100", AchievementCategory.LEARNING) { it.quizCorrect >= 100 },
        AchievementDef("perfect_streak_5", AchievementCategory.LEARNING) { it.quizBestStreak >= 5 },
        AchievementDef("perfect_streak_10", AchievementCategory.LEARNING) { it.quizBestStreak >= 10 },

        // ── Ear Training ─────────────────────────────────────────────
        AchievementDef("ear_first", AchievementCategory.EAR) {
            it.intervalTotal >= 1 || it.chordEarTotal >= 1
        },
        AchievementDef("ear_50", AchievementCategory.EAR) {
            (it.intervalTotal + it.chordEarTotal) >= 50
        },
        AchievementDef("ear_accuracy_80", AchievementCategory.EAR) {
            val total = it.intervalTotal + it.chordEarTotal
            val correct = it.intervalCorrect + it.chordEarCorrect
            total >= 50 && correct * 100 / total >= 80
        },

        // ── Songbook ─────────────────────────────────────────────────
        AchievementDef("first_song", AchievementCategory.SONGS) { it.songsCount >= 1 },
        AchievementDef("songs_5", AchievementCategory.SONGS) { it.songsCount >= 5 },
        AchievementDef("songs_10", AchievementCategory.SONGS) { it.songsCount >= 10 },

        // ── Favorites ────────────────────────────────────────────────
        AchievementDef("fav_5", AchievementCategory.CHORDS) { it.favoritesCount >= 5 },
        AchievementDef("fav_25", AchievementCategory.CHORDS) { it.favoritesCount >= 25 },

        // ── Scale Practice ───────────────────────────────────────────
        AchievementDef("scale_first", AchievementCategory.PRACTICE) { it.scalePracticeTotal >= 1 },
        AchievementDef("scale_25", AchievementCategory.PRACTICE) { it.scalePracticeTotal >= 25 },
    )

    /** Returns the total number of achievements available. */
    fun totalCount(): Int = ALL.size

    /** Returns the set of achievement IDs whose conditions are met. */
    fun earned(context: AchievementContext): Set<String> =
        ALL.filter { it.condition(context) }.map { it.id }.toSet()

    /**
     * Returns achievement definitions that are newly earned (condition met
     * but not yet in [alreadyUnlocked]).
     */
    fun checkNewlyEarned(
        context: AchievementContext,
        alreadyUnlocked: Set<String>,
    ): List<AchievementDef> =
        ALL.filter { it.id !in alreadyUnlocked && it.condition(context) }
}
