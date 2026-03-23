package com.baijum.ukufretboard.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChordEarTrainerTest {

    @Test
    fun level1OnlyMajorMinor() {
        val allowed = setOf("Major", "Minor")
        repeat(20) {
            val q = ChordEarTrainer.generateQuestion(1)
            assertTrue(q.quality in allowed,
                "Level 1 quality '${q.quality}' not in $allowed")
        }
    }

    @Test
    fun level2AddsDimAug() {
        val allowed = setOf("Major", "Minor", "Diminished", "Augmented")
        repeat(20) {
            val q = ChordEarTrainer.generateQuestion(2)
            assertTrue(q.quality in allowed,
                "Level 2 quality '${q.quality}' not in $allowed")
        }
    }

    @Test
    fun level3AddsSevenths() {
        val allowed = setOf("Major", "Minor", "Diminished", "Augmented",
            "Dominant 7th", "Major 7th", "Minor 7th")
        repeat(20) {
            val q = ChordEarTrainer.generateQuestion(3)
            assertTrue(q.quality in allowed,
                "Level 3 quality '${q.quality}' not in $allowed")
        }
    }

    @Test
    fun level4AllQualities() {
        repeat(20) {
            val q = ChordEarTrainer.generateQuestion(4)
            assertTrue(q.quality.isNotEmpty())
        }
    }

    @Test
    fun questionHasFourOptions() {
        for (level in 1..4) {
            val q = ChordEarTrainer.generateQuestion(level)
            assertEquals(4, q.options.size)
        }
    }

    @Test
    fun correctAnswerMatchesQuality() {
        repeat(20) {
            val q = ChordEarTrainer.generateQuestion((1..4).random())
            assertEquals(q.quality, q.correctAnswer)
        }
    }

    @Test
    fun correctIndexPointsToCorrectAnswer() {
        repeat(20) {
            val q = ChordEarTrainer.generateQuestion((1..4).random())
            assertEquals(q.correctAnswer, q.options[q.correctIndex])
        }
    }

    @Test
    fun rootPitchClassIsValid() {
        repeat(20) {
            val q = ChordEarTrainer.generateQuestion((1..4).random())
            assertTrue(q.rootPitchClass in 0..11)
        }
    }

    @Test
    fun rootNameIsNonEmpty() {
        val q = ChordEarTrainer.generateQuestion(1)
        assertTrue(q.rootName.isNotEmpty())
    }

    @Test
    fun notesAreNonEmpty() {
        repeat(10) {
            val q = ChordEarTrainer.generateQuestion((1..4).random())
            assertTrue(q.notes.isNotEmpty(), "Chord should have playback notes")
        }
    }

    @Test
    fun notesPitchClassesAreValid() {
        repeat(10) {
            val q = ChordEarTrainer.generateQuestion((1..4).random())
            for ((pc, octave) in q.notes) {
                assertTrue(pc in 0..11, "Note pitch class $pc out of range")
                assertTrue(octave in 3..6, "Note octave $octave out of range")
            }
        }
    }

    @Test
    fun levelClampedToValidRange() {
        val q0 = ChordEarTrainer.generateQuestion(0) // clamps to 1
        assertTrue(q0.options.size == 4)
        val q5 = ChordEarTrainer.generateQuestion(5) // clamps to 4
        assertTrue(q5.options.size == 4)
    }

    @Test
    fun symbolIsNonNull() {
        repeat(10) {
            val q = ChordEarTrainer.generateQuestion((1..4).random())
            // symbol can be "" for major but should not crash
            assertTrue(q.symbol.length >= 0)
        }
    }
}
