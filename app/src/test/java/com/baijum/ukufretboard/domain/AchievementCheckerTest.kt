package com.baijum.ukufretboard.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Android-side smoke tests that the shared [Achievements] catalog is
 * accessible from the app module. Comprehensive boundary tests live in
 * the shared module's `AchievementsTest`.
 */
class AchievementCheckerTest {

    @Test
    fun totalCountMatchesDefinedAchievements() {
        assertEquals(Achievements.ALL.size, Achievements.totalCount())
    }

    @Test
    fun allIdsAreUnique() {
        val ids = Achievements.ALL.map { it.id }
        assertEquals("Achievement IDs must be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun checkNewlyEarnedExcludesAlreadyUnlocked() {
        val ctx = AchievementContext(currentStreak = 7, completedLessons = 1, totalLessons = 10)
        val alreadyUnlocked = setOf("streak_3", "first_lesson")
        val newly = Achievements.checkNewlyEarned(ctx, alreadyUnlocked)
        assertTrue("streak_7 should be newly earned", newly.any { it.id == "streak_7" })
        assertFalse("streak_3 already unlocked", newly.any { it.id == "streak_3" })
        assertFalse("first_lesson already unlocked", newly.any { it.id == "first_lesson" })
    }

    @Test
    fun checkNewlyEarnedReturnsEmptyWhenNothingNew() {
        val ctx = AchievementContext()
        val newly = Achievements.checkNewlyEarned(ctx, emptySet())
        assertTrue("No conditions met, nothing newly earned", newly.isEmpty())
    }

    @Test
    fun checkNewlyEarnedReturnsAllSimultaneously() {
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
        val newly = Achievements.checkNewlyEarned(ctx, emptySet())
        assertEquals("All achievements should be newly earned", Achievements.totalCount(), newly.size)
    }
}
