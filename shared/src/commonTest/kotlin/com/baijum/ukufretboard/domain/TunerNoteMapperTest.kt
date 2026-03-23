package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.UkuleleTuning
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [TunerNoteMapper].
 *
 * Verifies frequency-to-note mapping, string matching, and hysteresis
 * behaviour for the tuner pipeline.
 */
class TunerNoteMapperTest {

    companion object {
        private const val A4_HZ = 440.0
    }

    // --- mapFrequency --------------------------------------------------------

    @Test
    fun mapsA4Correctly() {
        val note = TunerNoteMapper.mapFrequency(440.0)
        assertNotNull(note)
        assertEquals("A", note.noteName)
        assertEquals(4, note.octave)
        assertEquals(9, note.pitchClass) // A = 9
        assertTrue(abs(note.centsDeviation) < 0.5, "Cents should be near zero")
    }

    @Test
    fun mapsC4Correctly() {
        // C4 = 261.63 Hz
        val note = TunerNoteMapper.mapFrequency(261.63)
        assertNotNull(note)
        assertEquals("C", note.noteName)
        assertEquals(4, note.octave)
        assertEquals(0, note.pitchClass)
    }

    @Test
    fun mapsE4Correctly() {
        // E4 = 329.63 Hz
        val note = TunerNoteMapper.mapFrequency(329.63)
        assertNotNull(note)
        assertEquals("E", note.noteName)
        assertEquals(4, note.octave)
        assertEquals(4, note.pitchClass)
    }

    @Test
    fun mapsG4Correctly() {
        // G4 = 392.00 Hz
        val note = TunerNoteMapper.mapFrequency(392.0)
        assertNotNull(note)
        assertEquals("G", note.noteName)
        assertEquals(4, note.octave)
        assertEquals(7, note.pitchClass)
    }

    @Test
    fun mapsD3Correctly() {
        // D3 = 146.83 Hz (Baritone lowest string)
        val note = TunerNoteMapper.mapFrequency(146.83)
        assertNotNull(note)
        assertEquals("D", note.noteName)
        assertEquals(3, note.octave)
        assertEquals(2, note.pitchClass)
    }

    @Test
    fun returnsNullForZeroFrequency() {
        assertNull(TunerNoteMapper.mapFrequency(0.0))
    }

    @Test
    fun returnsNullForNegativeFrequency() {
        assertNull(TunerNoteMapper.mapFrequency(-100.0))
    }

    @Test
    fun detectsSharpDeviation() {
        // 5 cents sharp of A4 = 440 * 2^(5/1200)
        val sharpA4 = 440.0 * 2.0.pow(5.0 / 1200.0)
        val note = TunerNoteMapper.mapFrequency(sharpA4)
        assertNotNull(note)
        assertEquals("A", note.noteName)
        assertTrue(note.centsDeviation > 4.0, "Should be sharp")
        assertTrue(abs(note.centsDeviation - 5.0) < 1.0, "Should be around 5 cents")
    }

    @Test
    fun detectsFlatDeviation() {
        // 10 cents flat of A4 = 440 * 2^(-10/1200)
        val flatA4 = 440.0 * 2.0.pow(-10.0 / 1200.0)
        val note = TunerNoteMapper.mapFrequency(flatA4)
        assertNotNull(note)
        assertEquals("A", note.noteName)
        assertTrue(note.centsDeviation < -9.0, "Should be flat")
    }

    @Test
    fun customA4Reference() {
        // With A4 = 442 Hz, playing 442 Hz should map to A4 with ~0 cents
        val note = TunerNoteMapper.mapFrequency(442.0, a4Reference = 442.0)
        assertNotNull(note)
        assertEquals("A", note.noteName)
        assertTrue(abs(note.centsDeviation) < 0.5, "Cents should be near zero")
    }

    // --- findNearestString ---------------------------------------------------

    @Test
    fun findsGStringInHighG() {
        val noteG4 = TunerNoteMapper.mapFrequency(392.0)!!
        val match = TunerNoteMapper.findNearestString(noteG4, UkuleleTuning.HIGH_G)
        assertEquals("G", match.stringName)
        assertEquals(0, match.stringIndex)
    }

    @Test
    fun findsCStringInHighG() {
        val noteC4 = TunerNoteMapper.mapFrequency(261.63)!!
        val match = TunerNoteMapper.findNearestString(noteC4, UkuleleTuning.HIGH_G)
        assertEquals("C", match.stringName)
        assertEquals(1, match.stringIndex)
    }

    @Test
    fun findsEStringInHighG() {
        val noteE4 = TunerNoteMapper.mapFrequency(329.63)!!
        val match = TunerNoteMapper.findNearestString(noteE4, UkuleleTuning.HIGH_G)
        assertEquals("E", match.stringName)
        assertEquals(2, match.stringIndex)
    }

    @Test
    fun findsAStringInHighG() {
        val noteA4 = TunerNoteMapper.mapFrequency(440.0)!!
        val match = TunerNoteMapper.findNearestString(noteA4, UkuleleTuning.HIGH_G)
        assertEquals("A", match.stringName)
        assertEquals(3, match.stringIndex)
    }

    @Test
    fun findsDStringInBaritone() {
        val noteD3 = TunerNoteMapper.mapFrequency(146.83)!!
        val match = TunerNoteMapper.findNearestString(noteD3, UkuleleTuning.BARITONE)
        assertEquals("D", match.stringName)
        assertEquals(0, match.stringIndex)
    }

    @Test
    fun findsLowGStringInLowG() {
        // Low G = G3 ≈ 196 Hz
        val noteG3 = TunerNoteMapper.mapFrequency(196.0)!!
        val match = TunerNoteMapper.findNearestString(noteG3, UkuleleTuning.LOW_G)
        assertEquals("g", match.stringName)
        assertEquals(0, match.stringIndex)
    }

    // --- findNearestStringWithHysteresis -------------------------------------

    @Test
    fun hysteresisKeepsPreviousString() {
        val noteInfo = TunerNoteMapper.mapFrequency(329.63)!! // Exactly E4

        val match = TunerNoteMapper.findNearestStringWithHysteresis(
            noteInfo = noteInfo,
            tuning = UkuleleTuning.HIGH_G,
            previousStringIndex = 2, // E string was previous
            switchHysteresisCents = 4.0,
        )
        assertEquals(2, match.stringIndex, "E string should stay locked")
    }

    @Test
    fun hysteresisSwitchesOnLargeDeviation() {
        // Playing C4 frequency — should switch to C string even if previous was E
        val noteInfo = TunerNoteMapper.mapFrequency(261.63)!!

        val match = TunerNoteMapper.findNearestStringWithHysteresis(
            noteInfo = noteInfo,
            tuning = UkuleleTuning.HIGH_G,
            previousStringIndex = 2, // E string was previous
            switchHysteresisCents = 4.0,
        )
        assertEquals(1, match.stringIndex, "Should switch to C string")
    }

    @Test
    fun hysteresisWorksWithNoPrevious() {
        val noteInfo = TunerNoteMapper.mapFrequency(440.0)!!

        val match = TunerNoteMapper.findNearestStringWithHysteresis(
            noteInfo = noteInfo,
            tuning = UkuleleTuning.HIGH_G,
            previousStringIndex = null,
            switchHysteresisCents = 4.0,
        )
        assertEquals("A", match.stringName)
        assertEquals(3, match.stringIndex)
    }

    // --- Cents from target in string match -----------------------------------

    @Test
    fun centsFromTargetNearZeroForExactPitch() {
        val noteA4 = TunerNoteMapper.mapFrequency(440.0)!!
        val match = TunerNoteMapper.findNearestString(noteA4, UkuleleTuning.HIGH_G)
        assertTrue(
            abs(match.centsFromTarget) < 1.0,
            "Cents from target should be near zero, was ${match.centsFromTarget}",
        )
    }

    @Test
    fun centsFromTargetPositiveForSharp() {
        // 10 cents sharp of A4
        val sharpA4 = 440.0 * 2.0.pow(10.0 / 1200.0)
        val note = TunerNoteMapper.mapFrequency(sharpA4)!!
        val match = TunerNoteMapper.findNearestString(note, UkuleleTuning.HIGH_G)
        assertTrue(match.centsFromTarget > 9.0, "Should be sharp (positive cents)")
    }

    @Test
    fun centsFromTargetNegativeForFlat() {
        // 15 cents flat of A4
        val flatA4 = 440.0 * 2.0.pow(-15.0 / 1200.0)
        val note = TunerNoteMapper.mapFrequency(flatA4)!!
        val match = TunerNoteMapper.findNearestString(note, UkuleleTuning.HIGH_G)
        assertTrue(match.centsFromTarget < -14.0, "Should be flat (negative cents)")
    }

    // --- Custom A4 reference with string matching ----------------------------

    @Test
    fun customA4AffectsStringMatching() {
        // With A4=442, playing 442 Hz should match A string with ~0 cents
        val note = TunerNoteMapper.mapFrequency(442.0, a4Reference = 442.0)!!
        val match = TunerNoteMapper.findNearestString(note, UkuleleTuning.HIGH_G, a4Reference = 442.0)
        assertEquals("A", match.stringName)
        assertTrue(abs(match.centsFromTarget) < 1.0, "Cents from target should be near zero")
    }
}
