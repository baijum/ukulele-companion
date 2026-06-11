package com.baijum.ukufretboard.viewmodel

import com.baijum.ukufretboard.domain.AchievementContext

/**
 * Builds an [AchievementContext] from a [LearningProgressState] snapshot
 * plus counts that live outside the learning-progress repository.
 */
fun LearningProgressState.toAchievementContext(
    songsCount: Int = 0,
    favoritesCount: Int = 0,
): AchievementContext = AchievementContext(
    currentStreak = currentDayStreak,
    bestStreak = bestDayStreak,
    completedLessons = completedLessons,
    totalLessons = totalLessons,
    quizCorrect = quizStatsOverall.correct,
    quizTotal = quizStatsOverall.total,
    quizBestStreak = quizStatsOverall.bestStreak,
    intervalTotal = intervalStatsOverall.total,
    intervalCorrect = intervalStatsOverall.correct,
    chordEarTotal = chordEarStatsOverall.total,
    chordEarCorrect = chordEarStatsOverall.correct,
    scalePracticeTotal = scalePracticeStatsOverall.total,
    songsCount = songsCount,
    favoritesCount = favoritesCount,
)
