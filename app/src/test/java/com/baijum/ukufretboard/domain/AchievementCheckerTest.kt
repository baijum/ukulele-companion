package com.baijum.ukufretboard.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementCheckerTest {

    @Test
    fun totalCountMatchesDefinedAchievements() {
        assertEquals(AchievementChecker.ALL.size, AchievementChecker.totalCount())
    }

    @Test
    fun allIdsAreUnique() {
        val ids = AchievementChecker.ALL.map { it.id }
        assertEquals("Achievement IDs must be unique", ids.size, ids.toSet().size)
    }

    // ── Practice streak achievements ─────────────────────────────────

    @Test
    fun streak3UnlocksAtThreeDays() {
        val ctx2 = AchievementContext(currentStreak = 2)
        val ctx3 = AchievementContext(currentStreak = 3)
        val ach = AchievementChecker.ALL.first { it.id == "streak_3" }
        assertFalse(ach.condition(ctx2))
        assertTrue(ach.condition(ctx3))
    }

    @Test
    fun streak7UnlocksAtSevenDays() {
        val ach = AchievementChecker.ALL.first { it.id == "streak_7" }
        assertFalse(ach.condition(AchievementContext(currentStreak = 6)))
        assertTrue(ach.condition(AchievementContext(currentStreak = 7)))
    }

    @Test
    fun streak30UnlocksAtThirtyDays() {
        val ach = AchievementChecker.ALL.first { it.id == "streak_30" }
        assertFalse(ach.condition(AchievementContext(currentStreak = 29)))
        assertTrue(ach.condition(AchievementContext(currentStreak = 30)))
    }

    // ── Theory lesson achievements ───────────────────────────────────

    @Test
    fun firstLessonUnlocksAtOne() {
        val ach = AchievementChecker.ALL.first { it.id == "first_lesson" }
        assertFalse(ach.condition(AchievementContext(completedLessons = 0)))
        assertTrue(ach.condition(AchievementContext(completedLessons = 1)))
    }

    @Test
    fun halfLessonsRequiresHalf() {
        val ach = AchievementChecker.ALL.first { it.id == "half_lessons" }
        assertFalse(ach.condition(AchievementContext(completedLessons = 4, totalLessons = 10)))
        assertTrue(ach.condition(AchievementContext(completedLessons = 5, totalLessons = 10)))
    }

    @Test
    fun halfLessonsDoesNotUnlockWithZeroTotal() {
        val ach = AchievementChecker.ALL.first { it.id == "half_lessons" }
        assertFalse(ach.condition(AchievementContext(completedLessons = 0, totalLessons = 0)))
    }

    @Test
    fun allLessonsRequiresAll() {
        val ach = AchievementChecker.ALL.first { it.id == "all_lessons" }
        assertFalse(ach.condition(AchievementContext(completedLessons = 9, totalLessons = 10)))
        assertTrue(ach.condition(AchievementContext(completedLessons = 10, totalLessons = 10)))
    }

    // ── Quiz achievements ────────────────────────────────────────────

    @Test
    fun quizMilestonesRequireCorrectCount() {
        val q10 = AchievementChecker.ALL.first { it.id == "quiz_10" }
        val q50 = AchievementChecker.ALL.first { it.id == "quiz_50" }
        val q100 = AchievementChecker.ALL.first { it.id == "quiz_100" }
        assertFalse(q10.condition(AchievementContext(quizCorrect = 9)))
        assertTrue(q10.condition(AchievementContext(quizCorrect = 10)))
        assertFalse(q50.condition(AchievementContext(quizCorrect = 49)))
        assertTrue(q50.condition(AchievementContext(quizCorrect = 50)))
        assertFalse(q100.condition(AchievementContext(quizCorrect = 99)))
        assertTrue(q100.condition(AchievementContext(quizCorrect = 100)))
    }

    @Test
    fun quizStreakAchievements() {
        val s5 = AchievementChecker.ALL.first { it.id == "perfect_streak_5" }
        val s10 = AchievementChecker.ALL.first { it.id == "perfect_streak_10" }
        assertFalse(s5.condition(AchievementContext(quizBestStreak = 4)))
        assertTrue(s5.condition(AchievementContext(quizBestStreak = 5)))
        assertFalse(s10.condition(AchievementContext(quizBestStreak = 9)))
        assertTrue(s10.condition(AchievementContext(quizBestStreak = 10)))
    }

    // ── Ear training achievements ────────────────────────────────────

    @Test
    fun earFirstUnlocksWithEitherType() {
        val ach = AchievementChecker.ALL.first { it.id == "ear_first" }
        assertFalse(ach.condition(AchievementContext(intervalTotal = 0, chordEarTotal = 0)))
        assertTrue(ach.condition(AchievementContext(intervalTotal = 1, chordEarTotal = 0)))
        assertTrue(ach.condition(AchievementContext(intervalTotal = 0, chordEarTotal = 1)))
    }

    @Test
    fun ear50CombinesBothTypes() {
        val ach = AchievementChecker.ALL.first { it.id == "ear_50" }
        assertFalse(ach.condition(AchievementContext(intervalTotal = 25, chordEarTotal = 24)))
        assertTrue(ach.condition(AchievementContext(intervalTotal = 25, chordEarTotal = 25)))
    }

    @Test
    fun earAccuracy80RequiresMinimumAndThreshold() {
        val ach = AchievementChecker.ALL.first { it.id == "ear_accuracy_80" }
        assertFalse("Needs 50+ total", ach.condition(
            AchievementContext(intervalTotal = 10, intervalCorrect = 10, chordEarTotal = 0, chordEarCorrect = 0),
        ))
        assertFalse("79% not enough", ach.condition(
            AchievementContext(intervalTotal = 50, intervalCorrect = 39, chordEarTotal = 0, chordEarCorrect = 0),
        ))
        assertTrue("80% with 50 total", ach.condition(
            AchievementContext(intervalTotal = 30, intervalCorrect = 24, chordEarTotal = 20, chordEarCorrect = 16),
        ))
    }

    // ── Songbook & favorites ─────────────────────────────────────────

    @Test
    fun songbookMilestones() {
        val s1 = AchievementChecker.ALL.first { it.id == "first_song" }
        val s5 = AchievementChecker.ALL.first { it.id == "songs_5" }
        val s10 = AchievementChecker.ALL.first { it.id == "songs_10" }
        assertFalse(s1.condition(AchievementContext(songsCount = 0)))
        assertTrue(s1.condition(AchievementContext(songsCount = 1)))
        assertTrue(s5.condition(AchievementContext(songsCount = 5)))
        assertTrue(s10.condition(AchievementContext(songsCount = 10)))
    }

    @Test
    fun favoriteMilestones() {
        val f5 = AchievementChecker.ALL.first { it.id == "fav_5" }
        val f25 = AchievementChecker.ALL.first { it.id == "fav_25" }
        assertFalse(f5.condition(AchievementContext(favoritesCount = 4)))
        assertTrue(f5.condition(AchievementContext(favoritesCount = 5)))
        assertTrue(f25.condition(AchievementContext(favoritesCount = 25)))
    }

    // ── Scale practice ───────────────────────────────────────────────

    @Test
    fun scalePracticeMilestones() {
        val s1 = AchievementChecker.ALL.first { it.id == "scale_first" }
        val s25 = AchievementChecker.ALL.first { it.id == "scale_25" }
        assertFalse(s1.condition(AchievementContext(scalePracticeTotal = 0)))
        assertTrue(s1.condition(AchievementContext(scalePracticeTotal = 1)))
        assertTrue(s25.condition(AchievementContext(scalePracticeTotal = 25)))
    }

    // ── checkNewlyEarned ─────────────────────────────────────────────

    @Test
    fun checkNewlyEarnedExcludesAlreadyUnlocked() {
        val ctx = AchievementContext(currentStreak = 7, completedLessons = 1, totalLessons = 10)
        val alreadyUnlocked = setOf("streak_3", "first_lesson")
        val newly = AchievementChecker.checkNewlyEarned(ctx, alreadyUnlocked)
        assertTrue("streak_7 should be newly earned", newly.any { it.id == "streak_7" })
        assertFalse("streak_3 already unlocked", newly.any { it.id == "streak_3" })
        assertFalse("first_lesson already unlocked", newly.any { it.id == "first_lesson" })
    }

    @Test
    fun checkNewlyEarnedReturnsEmptyWhenNothingNew() {
        val ctx = AchievementContext()
        val newly = AchievementChecker.checkNewlyEarned(ctx, emptySet())
        assertTrue("No conditions met, nothing newly earned", newly.isEmpty())
    }

    @Test
    fun checkNewlyEarnedReturnsMultipleSimultaneously() {
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
        val newly = AchievementChecker.checkNewlyEarned(ctx, emptySet())
        assertEquals("All achievements should be newly earned", AchievementChecker.totalCount(), newly.size)
    }
}
