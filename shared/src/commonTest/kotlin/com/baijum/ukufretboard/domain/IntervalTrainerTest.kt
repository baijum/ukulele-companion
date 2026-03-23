package com.baijum.ukufretboard.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntervalTrainerTest {

    @Test
    fun level1OnlySimpleIntervals() {
        val allowed = setOf(5, 7, 12) // P4, P5, Octave
        repeat(20) {
            val q = IntervalTrainer.generateQuestion(1)
            assertTrue(q.intervalSemitones in allowed,
                "Level 1 interval ${q.intervalSemitones} not in $allowed")
        }
    }

    @Test
    fun level2AddsThirds() {
        val allowed = setOf(3, 4, 5, 7, 12)
        repeat(20) {
            val q = IntervalTrainer.generateQuestion(2)
            assertTrue(q.intervalSemitones in allowed,
                "Level 2 interval ${q.intervalSemitones} not in $allowed")
        }
    }

    @Test
    fun level3AddsSecondsAndSevenths() {
        val allowed = setOf(1, 2, 3, 4, 5, 7, 10, 11)
        repeat(20) {
            val q = IntervalTrainer.generateQuestion(3)
            assertTrue(q.intervalSemitones in allowed,
                "Level 3 interval ${q.intervalSemitones} not in $allowed")
        }
    }

    @Test
    fun level4AllIntervals() {
        val allowed = (1..11).toSet()
        repeat(20) {
            val q = IntervalTrainer.generateQuestion(4)
            assertTrue(q.intervalSemitones in allowed,
                "Level 4 interval ${q.intervalSemitones} not in $allowed")
        }
    }

    @Test
    fun questionHasAtLeastThreeOptions() {
        for (level in 1..4) {
            val q = IntervalTrainer.generateQuestion(level)
            // Level 1 has only 3 intervals (P4, P5, Octave), so 3 options possible
            assertTrue(q.options.size in 3..4, "Level $level should have 3-4 options: ${q.options.size}")
        }
    }

    @Test
    fun correctIndexIsValid() {
        repeat(20) {
            val q = IntervalTrainer.generateQuestion((1..4).random())
            assertTrue(q.correctIndex in 0..3)
            assertEquals(q.correctAnswer, q.options[q.correctIndex])
        }
    }

    @Test
    fun noteNamesAreNonEmpty() {
        val q = IntervalTrainer.generateQuestion(1)
        assertTrue(q.note1Name.isNotEmpty())
        assertTrue(q.note2Name.isNotEmpty())
    }

    @Test
    fun pitchClassesAreValid() {
        repeat(20) {
            val q = IntervalTrainer.generateQuestion((1..4).random())
            assertTrue(q.note1PitchClass in 0..11)
            assertTrue(q.note2PitchClass in 0..11)
        }
    }

    @Test
    fun octaveIntervalCorrectAnswer() {
        // Keep generating level 1 until we get an octave
        var found = false
        repeat(100) {
            val q = IntervalTrainer.generateQuestion(1)
            if (q.intervalSemitones == 12) {
                assertEquals("Octave", q.correctAnswer)
                found = true
            }
        }
        // It's probabilistic but 100 tries at level 1 (3 options) should hit octave
        assertTrue(found, "Should find an octave question in 100 tries")
    }

    @Test
    fun directionParameterIsPreserved() {
        for (dir in IntervalTrainer.IntervalDirection.entries) {
            val q = IntervalTrainer.generateQuestion(1, dir)
            assertEquals(dir, q.direction)
        }
    }

    @Test
    fun levelClampedToValidRange() {
        // Level 0 should clamp to 1, level 5 to 4 — both should not crash
        val q0 = IntervalTrainer.generateQuestion(0)
        assertTrue(q0.options.size >= 3)
        val q5 = IntervalTrainer.generateQuestion(5)
        assertTrue(q5.options.size >= 3)
    }

    @Test
    fun fretPositionsAreNonNegative() {
        repeat(20) {
            val q = IntervalTrainer.generateQuestion((1..4).random())
            assertTrue(q.note1Fret >= 0)
            assertTrue(q.note2Fret >= 0)
        }
    }
}
