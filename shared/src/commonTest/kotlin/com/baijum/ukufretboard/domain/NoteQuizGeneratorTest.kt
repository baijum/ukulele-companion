package com.baijum.ukufretboard.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoteQuizGeneratorTest {

    // --- generateNameIt ---

    @Test
    fun nameItReturnsValidQuestion() {
        val q = NoteQuizGenerator.generateNameIt()
        assertTrue(q.string in 0..3)
        assertTrue(q.pitchClass in 0..11)
        assertTrue(q.correctNote.isNotEmpty())
        assertEquals(4, q.options.size)
        assertTrue(q.correctIndex in 0..3)
    }

    @Test
    fun nameItCorrectAnswerIsInOptions() {
        repeat(10) {
            val q = NoteQuizGenerator.generateNameIt()
            assertEquals(q.correctNote, q.options[q.correctIndex])
        }
    }

    @Test
    fun nameItEasyMaxFret5() {
        repeat(20) {
            val q = NoteQuizGenerator.generateNameIt(difficulty = 1)
            assertTrue(q.fret in 0..5, "Easy fret should be 0-5: ${q.fret}")
        }
    }

    @Test
    fun nameItMediumMaxFret9() {
        repeat(20) {
            val q = NoteQuizGenerator.generateNameIt(difficulty = 2)
            assertTrue(q.fret in 0..9, "Medium fret should be 0-9: ${q.fret}")
        }
    }

    @Test
    fun nameItHardMaxFret12() {
        repeat(20) {
            val q = NoteQuizGenerator.generateNameIt(difficulty = 3)
            assertTrue(q.fret in 0..12, "Hard fret should be 0-12: ${q.fret}")
        }
    }

    @Test
    fun nameItPitchClassMatchesFret() {
        repeat(20) {
            val q = NoteQuizGenerator.generateNameIt()
            val openNotes = listOf(7, 0, 4, 9) // standard tuning
            val expected = (openNotes[q.string] + q.fret) % 12
            assertEquals(expected, q.pitchClass)
        }
    }

    @Test
    fun nameItOptionsAreDistinct() {
        repeat(10) {
            val q = NoteQuizGenerator.generateNameIt()
            assertEquals(q.options.size, q.options.toSet().size, "Options should be distinct")
        }
    }

    // --- generateFindIt ---

    @Test
    fun findItReturnsValidQuestion() {
        val q = NoteQuizGenerator.generateFindIt()
        assertTrue(q.targetNote.isNotEmpty())
        assertTrue(q.targetPitchClass in 0..11)
        assertTrue(q.correctString in 0..3)
        assertEquals(4, q.options.size)
        assertTrue(q.correctIndex in 0..3)
    }

    @Test
    fun findItCorrectAnswerIsInOptions() {
        repeat(10) {
            val q = NoteQuizGenerator.generateFindIt()
            assertTrue(q.options[q.correctIndex].contains("fret"))
        }
    }

    @Test
    fun findItTargetPitchClassMatchesPosition() {
        repeat(20) {
            val q = NoteQuizGenerator.generateFindIt()
            val openNotes = listOf(7, 0, 4, 9)
            val expected = (openNotes[q.correctString] + q.correctFret) % 12
            assertEquals(expected, q.targetPitchClass)
        }
    }

    @Test
    fun findItOptionsContainStringAndFret() {
        val q = NoteQuizGenerator.generateFindIt()
        for (opt in q.options) {
            assertTrue(opt.contains("string"), "Option should contain 'string': $opt")
            assertTrue(opt.contains("fret"), "Option should contain 'fret': $opt")
        }
    }

    @Test
    fun findItRepeatedCallsDoNotCrash() {
        repeat(30) {
            NoteQuizGenerator.generateFindIt()
        }
    }
}
