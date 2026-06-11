package com.baijum.ukufretboard.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AchievementsTest {

    private fun findDef(id: String): AchievementDef =
        Achievements.ALL.first { it.id == id }

    // ── Catalog integrity ───────────────────────────────────────────

    @Test
    fun totalCountMatchesList() {
        assertEquals(Achievements.ALL.size, Achievements.totalCount())
    }

    @Test
    fun allIdsAreUnique() {
        val ids = Achievements.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Achievement IDs must be unique")
    }

    @Test
    fun catalogHas21Achievements() {
        assertEquals(21, Achievements.totalCount())
    }

    // ── Practice streak ─────────────────────────────────────────────

    @Test
    fun streak3BelowThreshold() {
        assertFalse(findDef("streak_3").condition(AchievementContext(currentStreak = 2)))
    }

    @Test
    fun streak3AtThreshold() {
        assertTrue(findDef("streak_3").condition(AchievementContext(currentStreak = 3)))
    }

    @Test
    fun streak7BelowThreshold() {
        assertFalse(findDef("streak_7").condition(AchievementContext(currentStreak = 6)))
    }

    @Test
    fun streak7AtThreshold() {
        assertTrue(findDef("streak_7").condition(AchievementContext(currentStreak = 7)))
    }

    @Test
    fun streak30BelowThreshold() {
        assertFalse(findDef("streak_30").condition(AchievementContext(currentStreak = 29)))
    }

    @Test
    fun streak30AtThreshold() {
        assertTrue(findDef("streak_30").condition(AchievementContext(currentStreak = 30)))
    }

    // ── Theory lessons ──────────────────────────────────────────────

    @Test
    fun firstLessonNotEarnedAtZero() {
        assertFalse(findDef("first_lesson").condition(AchievementContext(completedLessons = 0)))
    }

    @Test
    fun firstLessonEarnedAtOne() {
        assertTrue(findDef("first_lesson").condition(AchievementContext(completedLessons = 1)))
    }

    @Test
    fun halfLessonsBelowThreshold() {
        assertFalse(findDef("half_lessons").condition(
            AchievementContext(completedLessons = 4, totalLessons = 10),
        ))
    }

    @Test
    fun halfLessonsAtThreshold() {
        assertTrue(findDef("half_lessons").condition(
            AchievementContext(completedLessons = 5, totalLessons = 10),
        ))
    }

    @Test
    fun halfLessonsNotEarnedWithZeroTotal() {
        assertFalse(findDef("half_lessons").condition(
            AchievementContext(completedLessons = 0, totalLessons = 0),
        ))
    }

    @Test
    fun allLessonsBelowThreshold() {
        assertFalse(findDef("all_lessons").condition(
            AchievementContext(completedLessons = 9, totalLessons = 10),
        ))
    }

    @Test
    fun allLessonsAtThreshold() {
        assertTrue(findDef("all_lessons").condition(
            AchievementContext(completedLessons = 10, totalLessons = 10),
        ))
    }

    @Test
    fun allLessonsNotEarnedWithZeroTotal() {
        assertFalse(findDef("all_lessons").condition(
            AchievementContext(completedLessons = 0, totalLessons = 0),
        ))
    }

    // ── Quiz achievements ───────────────────────────────────────────

    @Test
    fun quiz10BelowThreshold() {
        assertFalse(findDef("quiz_10").condition(AchievementContext(quizCorrect = 9)))
    }

    @Test
    fun quiz10AtThreshold() {
        assertTrue(findDef("quiz_10").condition(AchievementContext(quizCorrect = 10)))
    }

    @Test
    fun quiz50BelowThreshold() {
        assertFalse(findDef("quiz_50").condition(AchievementContext(quizCorrect = 49)))
    }

    @Test
    fun quiz50AtThreshold() {
        assertTrue(findDef("quiz_50").condition(AchievementContext(quizCorrect = 50)))
    }

    @Test
    fun quiz100BelowThreshold() {
        assertFalse(findDef("quiz_100").condition(AchievementContext(quizCorrect = 99)))
    }

    @Test
    fun quiz100AtThreshold() {
        assertTrue(findDef("quiz_100").condition(AchievementContext(quizCorrect = 100)))
    }

    @Test
    fun perfectStreak5BelowThreshold() {
        assertFalse(findDef("perfect_streak_5").condition(AchievementContext(quizBestStreak = 4)))
    }

    @Test
    fun perfectStreak5AtThreshold() {
        assertTrue(findDef("perfect_streak_5").condition(AchievementContext(quizBestStreak = 5)))
    }

    @Test
    fun perfectStreak10BelowThreshold() {
        assertFalse(findDef("perfect_streak_10").condition(AchievementContext(quizBestStreak = 9)))
    }

    @Test
    fun perfectStreak10AtThreshold() {
        assertTrue(findDef("perfect_streak_10").condition(AchievementContext(quizBestStreak = 10)))
    }

    // ── Ear training ────────────────────────────────────────────────

    @Test
    fun earFirstNotEarnedWithZero() {
        assertFalse(findDef("ear_first").condition(
            AchievementContext(intervalTotal = 0, chordEarTotal = 0),
        ))
    }

    @Test
    fun earFirstEarnedWithInterval() {
        assertTrue(findDef("ear_first").condition(
            AchievementContext(intervalTotal = 1, chordEarTotal = 0),
        ))
    }

    @Test
    fun earFirstEarnedWithChordEar() {
        assertTrue(findDef("ear_first").condition(
            AchievementContext(intervalTotal = 0, chordEarTotal = 1),
        ))
    }

    @Test
    fun ear50BelowThreshold() {
        assertFalse(findDef("ear_50").condition(
            AchievementContext(intervalTotal = 25, chordEarTotal = 24),
        ))
    }

    @Test
    fun ear50AtThreshold() {
        assertTrue(findDef("ear_50").condition(
            AchievementContext(intervalTotal = 25, chordEarTotal = 25),
        ))
    }

    @Test
    fun earAccuracy80NeedsFiftyTotal() {
        assertFalse(findDef("ear_accuracy_80").condition(
            AchievementContext(intervalTotal = 10, intervalCorrect = 10),
        ))
    }

    @Test
    fun earAccuracy80Below80Percent() {
        assertFalse(findDef("ear_accuracy_80").condition(
            AchievementContext(intervalTotal = 50, intervalCorrect = 39),
        ))
    }

    @Test
    fun earAccuracy80AtExactly80Percent() {
        assertTrue(findDef("ear_accuracy_80").condition(
            AchievementContext(
                intervalTotal = 30, intervalCorrect = 24,
                chordEarTotal = 20, chordEarCorrect = 16,
            ),
        ))
    }

    // ── Songbook ────────────────────────────────────────────────────

    @Test
    fun firstSongNotEarnedAtZero() {
        assertFalse(findDef("first_song").condition(AchievementContext(songsCount = 0)))
    }

    @Test
    fun firstSongEarnedAtOne() {
        assertTrue(findDef("first_song").condition(AchievementContext(songsCount = 1)))
    }

    @Test
    fun songs5BelowThreshold() {
        assertFalse(findDef("songs_5").condition(AchievementContext(songsCount = 4)))
    }

    @Test
    fun songs5AtThreshold() {
        assertTrue(findDef("songs_5").condition(AchievementContext(songsCount = 5)))
    }

    @Test
    fun songs10BelowThreshold() {
        assertFalse(findDef("songs_10").condition(AchievementContext(songsCount = 9)))
    }

    @Test
    fun songs10AtThreshold() {
        assertTrue(findDef("songs_10").condition(AchievementContext(songsCount = 10)))
    }

    // ── Favorites ───────────────────────────────────────────────────

    @Test
    fun fav5BelowThreshold() {
        assertFalse(findDef("fav_5").condition(AchievementContext(favoritesCount = 4)))
    }

    @Test
    fun fav5AtThreshold() {
        assertTrue(findDef("fav_5").condition(AchievementContext(favoritesCount = 5)))
    }

    @Test
    fun fav25BelowThreshold() {
        assertFalse(findDef("fav_25").condition(AchievementContext(favoritesCount = 24)))
    }

    @Test
    fun fav25AtThreshold() {
        assertTrue(findDef("fav_25").condition(AchievementContext(favoritesCount = 25)))
    }

    // ── Scale practice ──────────────────────────────────────────────

    @Test
    fun scaleFirstNotEarnedAtZero() {
        assertFalse(findDef("scale_first").condition(AchievementContext(scalePracticeTotal = 0)))
    }

    @Test
    fun scaleFirstEarnedAtOne() {
        assertTrue(findDef("scale_first").condition(AchievementContext(scalePracticeTotal = 1)))
    }

    @Test
    fun scale25BelowThreshold() {
        assertFalse(findDef("scale_25").condition(AchievementContext(scalePracticeTotal = 24)))
    }

    @Test
    fun scale25AtThreshold() {
        assertTrue(findDef("scale_25").condition(AchievementContext(scalePracticeTotal = 25)))
    }

    // ── earned() ────────────────────────────────────────────────────

    @Test
    fun earnedReturnsEmptyForDefaultContext() {
        assertTrue(Achievements.earned(AchievementContext()).isEmpty())
    }

    @Test
    fun earnedReturnsAllWhenAllConditionsMet() {
        val ctx = AchievementContext(
            currentStreak = 30,
            completedLessons = 10,
            totalLessons = 10,
            quizCorrect = 100,
            quizBestStreak = 10,
            intervalTotal = 30,
            intervalCorrect = 25,
            chordEarTotal = 20,
            chordEarCorrect = 16,
            scalePracticeTotal = 25,
            songsCount = 10,
            favoritesCount = 25,
        )
        assertEquals(Achievements.totalCount(), Achievements.earned(ctx).size)
    }

    // ── checkNewlyEarned() ──────────────────────────────────────────

    @Test
    fun checkNewlyEarnedExcludesAlreadyUnlocked() {
        val ctx = AchievementContext(currentStreak = 7, completedLessons = 1, totalLessons = 10)
        val alreadyUnlocked = setOf("streak_3", "first_lesson")
        val newly = Achievements.checkNewlyEarned(ctx, alreadyUnlocked)
        assertTrue(newly.any { it.id == "streak_7" })
        assertFalse(newly.any { it.id == "streak_3" })
        assertFalse(newly.any { it.id == "first_lesson" })
    }

    @Test
    fun checkNewlyEarnedReturnsEmptyWhenNothingNew() {
        assertTrue(Achievements.checkNewlyEarned(AchievementContext(), emptySet()).isEmpty())
    }

    @Test
    fun checkNewlyEarnedReturnsAllWhenNoneUnlocked() {
        val ctx = AchievementContext(
            currentStreak = 30,
            completedLessons = 10,
            totalLessons = 10,
            quizCorrect = 100,
            quizBestStreak = 10,
            intervalTotal = 30,
            intervalCorrect = 25,
            chordEarTotal = 20,
            chordEarCorrect = 16,
            scalePracticeTotal = 25,
            songsCount = 10,
            favoritesCount = 25,
        )
        assertEquals(Achievements.totalCount(), Achievements.checkNewlyEarned(ctx, emptySet()).size)
    }
}
