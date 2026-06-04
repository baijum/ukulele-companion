package com.baijum.ukufretboard.domain

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoteTest {

    @Test
    fun calculateNoteWithNegativeFretReturnsValidPitchClass() {
        // C string (pc 0) at fret -1 should wrap to 11 (B), not -1
        val note = calculateNote(0, -1)
        assertEquals(11, note.pitchClass)
        assertEquals("B", note.name)
    }

    @Test
    fun calculateNoteWithZeroReturnsOpenString() {
        // G string (pc 7) at fret 0 = G
        val note = calculateNote(7, 0)
        assertEquals(7, note.pitchClass)
        assertEquals("G", note.name)
    }

    @Test
    fun calculateNoteWrapsAtOctave() {
        // C string (pc 0) at fret 12 wraps back to C
        val note = calculateNote(0, 12)
        assertEquals(0, note.pitchClass)
    }

    @Test
    fun calculateNoteAlwaysReturnsValidRange() {
        runBlocking {
            checkAll(Arb.int(-100..100), Arb.int(-100..100)) { openPC, fret ->
                val note = calculateNote(openPC, fret)
                assertTrue(
                    note.pitchClass in 0..11,
                    "pitchClass ${note.pitchClass} out of range for openPC=$openPC, fret=$fret",
                )
            }
        }
    }
}
