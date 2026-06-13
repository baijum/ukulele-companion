package com.baijum.ukufretboard.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayAlongScorerTest {

    @Test
    fun emptyScoreIsZero() {
        val scorer = PlayAlongScorer()
        val score = scorer.getScore()
        assertEquals(0, score.totalBeats)
        assertEquals(0, score.correctBeats)
        assertEquals(0f, score.accuracy)
        assertEquals(0, score.currentStreak)
        assertEquals(0, score.bestStreak)
    }

    @Test
    fun singleCorrectBeat() {
        val scorer = PlayAlongScorer()
        scorer.recordBeat("Am", "Am", 0.9f)
        val score = scorer.getScore()
        assertEquals(1, score.totalBeats)
        assertEquals(1, score.correctBeats)
        assertEquals(1f, score.accuracy)
        assertEquals(1, score.currentStreak)
        assertEquals(1, score.bestStreak)
    }

    @Test
    fun singleIncorrectBeat() {
        val scorer = PlayAlongScorer()
        scorer.recordBeat("Am", "C", 0.5f)
        val score = scorer.getScore()
        assertEquals(1, score.totalBeats)
        assertEquals(0, score.correctBeats)
        assertEquals(0f, score.accuracy)
        assertEquals(0, score.currentStreak)
    }

    @Test
    fun nullDetectionIsIncorrect() {
        val scorer = PlayAlongScorer()
        scorer.recordBeat("Am", null, 0.0f)
        val score = scorer.getScore()
        assertEquals(0, score.correctBeats)
    }

    @Test
    fun accuracyCalculation() {
        val scorer = PlayAlongScorer()
        scorer.recordBeat("Am", "Am", 0.9f)
        scorer.recordBeat("C", "C", 0.8f)
        scorer.recordBeat("G", "Am", 0.5f)
        scorer.recordBeat("F", "F", 0.7f)
        val score = scorer.getScore()
        assertEquals(4, score.totalBeats)
        assertEquals(3, score.correctBeats)
        assertEquals(0.75f, score.accuracy)
        assertEquals(75, score.accuracyPercent)
    }

    // --- Streak tracking ---

    @Test
    fun streakIncrementsOnCorrect() {
        val scorer = PlayAlongScorer()
        scorer.recordBeat("Am", "Am", 0.9f)
        scorer.recordBeat("C", "C", 0.9f)
        scorer.recordBeat("G", "G", 0.9f)
        assertEquals(3, scorer.getScore().currentStreak)
        assertEquals(3, scorer.getScore().bestStreak)
    }

    @Test
    fun streakResetsOnIncorrect() {
        val scorer = PlayAlongScorer()
        scorer.recordBeat("Am", "Am", 0.9f)
        scorer.recordBeat("C", "C", 0.9f)
        scorer.recordBeat("G", "Am", 0.5f) // wrong
        assertEquals(0, scorer.getScore().currentStreak)
        assertEquals(2, scorer.getScore().bestStreak)
    }

    @Test
    fun bestStreakTrackedAcrossSession() {
        val scorer = PlayAlongScorer()
        scorer.recordBeat("Am", "Am", 0.9f)
        scorer.recordBeat("C", "C", 0.9f)
        scorer.recordBeat("G", "G", 0.9f) // streak 3
        scorer.recordBeat("F", "Am", 0.5f) // break
        scorer.recordBeat("Am", "Am", 0.9f)
        scorer.recordBeat("C", "C", 0.9f) // streak 2
        assertEquals(2, scorer.getScore().currentStreak)
        assertEquals(3, scorer.getScore().bestStreak) // best was 3
    }

    // --- Grading ---

    @Test
    fun gradeS() {
        val scorer = PlayAlongScorer()
        repeat(10) { scorer.recordBeat("Am", "Am", 0.9f) }
        assertEquals("S", scorer.getScore().grade)
    }

    @Test
    fun gradeA() {
        val scorer = PlayAlongScorer()
        repeat(8) { scorer.recordBeat("Am", "Am", 0.9f) }
        repeat(2) { scorer.recordBeat("Am", "C", 0.5f) }
        assertEquals("A", scorer.getScore().grade) // 80%
    }

    @Test
    fun gradeB() {
        val scorer = PlayAlongScorer()
        repeat(7) { scorer.recordBeat("Am", "Am", 0.9f) }
        repeat(3) { scorer.recordBeat("Am", "C", 0.5f) }
        assertEquals("B", scorer.getScore().grade) // 70%
    }

    @Test
    fun gradeC() {
        val scorer = PlayAlongScorer()
        repeat(6) { scorer.recordBeat("Am", "Am", 0.9f) }
        repeat(4) { scorer.recordBeat("Am", "C", 0.5f) }
        assertEquals("C", scorer.getScore().grade) // 60%
    }

    @Test
    fun gradeD() {
        val scorer = PlayAlongScorer()
        repeat(5) { scorer.recordBeat("Am", "Am", 0.9f) }
        repeat(5) { scorer.recordBeat("Am", "C", 0.5f) }
        assertEquals("D", scorer.getScore().grade) // 50%
    }

    @Test
    fun gradeF() {
        val scorer = PlayAlongScorer()
        repeat(3) { scorer.recordBeat("Am", "Am", 0.9f) }
        repeat(7) { scorer.recordBeat("Am", "C", 0.5f) }
        assertEquals("F", scorer.getScore().grade) // 30%
    }

    // --- Reset ---

    @Test
    fun resetClearsEverything() {
        val scorer = PlayAlongScorer()
        scorer.recordBeat("Am", "Am", 0.9f)
        scorer.recordBeat("C", "C", 0.9f)
        scorer.reset()
        val score = scorer.getScore()
        assertEquals(0, score.totalBeats)
        assertEquals(0, score.correctBeats)
        assertEquals(0, score.currentStreak)
        assertEquals(0, score.bestStreak)
        assertTrue(score.beatResults.isEmpty())
    }

    // --- Beat results ---

    @Test
    fun beatResultsRecordedInOrder() {
        val scorer = PlayAlongScorer()
        scorer.recordBeat("Am", "Am", 0.9f)
        scorer.recordBeat("C", "G", 0.5f)
        val results = scorer.getScore().beatResults
        assertEquals(2, results.size)
        assertEquals("Am", results[0].expected)
        assertTrue(results[0].isCorrect)
        assertEquals("C", results[1].expected)
        assertEquals("G", results[1].detected)
        assertTrue(!results[1].isCorrect)
    }

    // --- Chord normalization ---

    @Test
    fun chordNormalizationMatchesSeventh() {
        // "Am7" should match "Am" due to normalization
        val scorer = PlayAlongScorer()
        scorer.recordBeat("Am7", "Am", 0.9f)
        assertEquals(1, scorer.getScore().correctBeats)
    }

    @Test
    fun chordNormalizationMatchesMaj() {
        val scorer = PlayAlongScorer()
        scorer.recordBeat("Cmaj7", "C", 0.9f)
        assertEquals(1, scorer.getScore().correctBeats)
    }

    @Test
    fun chordNormalizationDoesNotStripMajFromMinor() {
        val scorer = PlayAlongScorer()
        // "Amaj7" normalizes to "A" (strips "maj7"), "Am" stays "Am"
        // These are different chords — should NOT match.
        scorer.recordBeat("Amaj7", "Am", 0.9f)
        assertEquals(0, scorer.getScore().correctBeats)
    }

    @Test
    fun chordNormalizationMatchesNinth() {
        val scorer = PlayAlongScorer()
        scorer.recordBeat("C9", "C", 0.9f)
        assertEquals(1, scorer.getScore().correctBeats)
    }
}
