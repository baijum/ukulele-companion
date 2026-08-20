package com.baijum.ukufretboard.viewmodel

import com.baijum.ukufretboard.data.LearningStats
import com.baijum.ukufretboard.domain.AchievementContext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [toAchievementContext].
 *
 * This adapter is pure field-shuffling between two flat types that happen to
 * declare their counters in different orders — `AchievementContext` has
 * `quizCorrect` before `quizTotal` but `intervalTotal` before `intervalCorrect`,
 * and `LearningStats` is built as `(total, correct, bestStreak)`. A transposed
 * pair here would silently unlock the wrong achievements, so every field is
 * seeded with its own distinct value and asserted individually.
 */
class AchievementContextAdapterTest {
    private fun sentinelState() =
        LearningProgressState(
            completedLessons = 2,
            totalLessons = 3,
            passedLessonQuizzes = 5,
            quizStatsOverall = LearningStats(total = 7, correct = 11, bestStreak = 13),
            intervalStatsOverall = LearningStats(total = 17, correct = 19, bestStreak = 23),
            noteQuizStats = LearningStats(total = 29, correct = 31, bestStreak = 37),
            chordEarStatsOverall = LearningStats(total = 41, correct = 43, bestStreak = 47),
            scalePracticeStatsOverall = LearningStats(total = 53, correct = 59, bestStreak = 61),
            currentDayStreak = 67,
            bestDayStreak = 71,
        )

    // ── Field mapping ────────────────────────────────────────────────

    @Test
    fun everyLearningProgressFieldMapsToItsAchievementCounterpart() {
        val context = sentinelState().toAchievementContext()

        assertEquals("currentStreak", 67, context.currentStreak)
        assertEquals("bestStreak", 71, context.bestStreak)
        assertEquals("completedLessons", 2, context.completedLessons)
        assertEquals("totalLessons", 3, context.totalLessons)
        assertEquals("quizCorrect", 11, context.quizCorrect)
        assertEquals("quizTotal", 7, context.quizTotal)
        assertEquals("quizBestStreak", 13, context.quizBestStreak)
        assertEquals("intervalTotal", 17, context.intervalTotal)
        assertEquals("intervalCorrect", 19, context.intervalCorrect)
        assertEquals("chordEarTotal", 41, context.chordEarTotal)
        assertEquals("chordEarCorrect", 43, context.chordEarCorrect)
        assertEquals("scalePracticeTotal", 53, context.scalePracticeTotal)
    }

    @Test
    fun quizCorrectAndQuizTotalAreNotTransposed() {
        // The two types declare this pair in opposite orders; this is the one
        // mapping a careless edit is most likely to swap.
        val context =
            LearningProgressState(
                quizStatsOverall = LearningStats(total = 100, correct = 1, bestStreak = 0),
            ).toAchievementContext()

        assertEquals("quizTotal must come from LearningStats.total", 100, context.quizTotal)
        assertEquals("quizCorrect must come from LearningStats.correct", 1, context.quizCorrect)
    }

    @Test
    fun intervalTotalAndIntervalCorrectAreNotTransposed() {
        val context =
            LearningProgressState(
                intervalStatsOverall = LearningStats(total = 100, correct = 1, bestStreak = 0),
            ).toAchievementContext()

        assertEquals(100, context.intervalTotal)
        assertEquals(1, context.intervalCorrect)
    }

    @Test
    fun chordEarTotalAndChordEarCorrectAreNotTransposed() {
        val context =
            LearningProgressState(
                chordEarStatsOverall = LearningStats(total = 100, correct = 1, bestStreak = 0),
            ).toAchievementContext()

        assertEquals(100, context.chordEarTotal)
        assertEquals(1, context.chordEarCorrect)
    }

    // ── Counts supplied by the caller ────────────────────────────────

    @Test
    fun songAndFavoriteCountsDefaultToZero() {
        val context = sentinelState().toAchievementContext()
        assertEquals("songs live outside the learning repository", 0, context.songsCount)
        assertEquals("favorites live outside the learning repository", 0, context.favoritesCount)
    }

    @Test
    fun explicitSongAndFavoriteCountsAreForwarded() {
        val context = sentinelState().toAchievementContext(songsCount = 73, favoritesCount = 79)
        assertEquals(73, context.songsCount)
        assertEquals(79, context.favoritesCount)
    }

    // ── Deliberate omissions ─────────────────────────────────────────

    @Test
    fun aDefaultStateMapsToADefaultContext() {
        assertEquals(AchievementContext(), LearningProgressState().toAchievementContext())
    }

    @Test
    fun noteQuizAndPassedLessonQuizzesAreDeliberatelyNotMapped() {
        // No achievement is keyed on either, so a state that differs only in
        // these fields must produce an identical context.
        val withoutExtras = LearningProgressState(currentDayStreak = 4)
        val withExtras =
            withoutExtras.copy(
                passedLessonQuizzes = 99,
                noteQuizStats = LearningStats(total = 99, correct = 99, bestStreak = 99),
            )
        assertEquals(withoutExtras.toAchievementContext(), withExtras.toAchievementContext())
    }

    @Test
    fun perCategoryStatsAreDeliberatelyNotMapped() {
        val withoutBreakdown =
            LearningProgressState(
                intervalStatsOverall = LearningStats(total = 10, correct = 5, bestStreak = 2),
            )
        val withBreakdown =
            withoutBreakdown.copy(
                intervalStatsByLevel = mapOf(1 to LearningStats(total = 10, correct = 5, bestStreak = 2)),
                scalePracticeStatsByMode = mapOf("QUIZ" to LearningStats(total = 3, correct = 3, bestStreak = 3)),
            )
        assertEquals(withoutBreakdown.toAchievementContext(), withBreakdown.toAchievementContext())
    }
}
