package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.UkuleleTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

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
        assertEquals("A", note!!.noteName)
        assertEquals(4, note.octave)
        assertEquals(9, note.pitchClass) // A = 9
        assertTrue("Cents should be near zero", abs(note.centsDeviation) < 0.5)
    }

    @Test
    fun mapsC4Correctly() {
        // C4 = 261.63 Hz
        val note = TunerNoteMapper.mapFrequency(261.63)
        assertNotNull(note)
        assertEquals("C", note!!.noteName)
        assertEquals(4, note.octave)
        assertEquals(0, note.pitchClass)
    }

    @Test
    fun mapsE4Correctly() {
        // E4 = 329.63 Hz
        val note = TunerNoteMapper.mapFrequency(329.63)
        assertNotNull(note)
        assertEquals("E", note!!.noteName)
        assertEquals(4, note.octave)
        assertEquals(4, note.pitchClass)
    }

    @Test
    fun mapsG4Correctly() {
        // G4 = 392.00 Hz
        val note = TunerNoteMapper.mapFrequency(392.0)
        assertNotNull(note)
        assertEquals("G", note!!.noteName)
        assertEquals(4, note.octave)
        assertEquals(7, note.pitchClass)
    }

    @Test
    fun mapsD3Correctly() {
        // D3 = 146.83 Hz (Baritone lowest string)
        val note = TunerNoteMapper.mapFrequency(146.83)
        assertNotNull(note)
        assertEquals("D", note!!.noteName)
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
        val sharpA4 = 440.0 * Math.pow(2.0, 5.0 / 1200.0)
        val note = TunerNoteMapper.mapFrequency(sharpA4)
        assertNotNull(note)
        assertEquals("A", note!!.noteName)
        assertTrue("Should be sharp", note.centsDeviation > 4.0)
        assertTrue("Should be around 5 cents", abs(note.centsDeviation - 5.0) < 1.0)
    }

    @Test
    fun detectsFlatDeviation() {
        // 10 cents flat of A4 = 440 * 2^(-10/1200)
        val flatA4 = 440.0 * Math.pow(2.0, -10.0 / 1200.0)
        val note = TunerNoteMapper.mapFrequency(flatA4)
        assertNotNull(note)
        assertEquals("A", note!!.noteName)
        assertTrue("Should be flat", note.centsDeviation < -9.0)
    }

    @Test
    fun customA4Reference() {
        // With A4 = 442 Hz, playing 442 Hz should map to A4 with ~0 cents
        val note = TunerNoteMapper.mapFrequency(442.0, a4Reference = 442.0)
        assertNotNull(note)
        assertEquals("A", note!!.noteName)
        assertTrue("Cents should be near zero", abs(note.centsDeviation) < 0.5)
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
        // E4 (329.63 Hz) is close to both E string (329.63 Hz) and C string (261.63 Hz).
        // When exactly at E4, best match is E. With hysteresis and previous=C,
        // a small deviation toward C should keep C as the target.
        //
        // Use a frequency between C4 and E4 but closer to E — hysteresis
        // should keep the previous string if within tolerance.
        val noteInfo = TunerNoteMapper.mapFrequency(329.63)!! // Exactly E4

        val match = TunerNoteMapper.findNearestStringWithHysteresis(
            noteInfo = noteInfo,
            tuning = UkuleleTuning.HIGH_G,
            previousStringIndex = 2, // E string was previous
            switchHysteresisCents = 4.0,
        )
        assertEquals("E string should stay locked", 2, match.stringIndex)
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
        assertEquals("Should switch to C string", 1, match.stringIndex)
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
            "Cents from target should be near zero, was ${match.centsFromTarget}",
            abs(match.centsFromTarget) < 1.0,
        )
    }

    @Test
    fun centsFromTargetPositiveForSharp() {
        // 10 cents sharp of A4
        val sharpA4 = 440.0 * Math.pow(2.0, 10.0 / 1200.0)
        val note = TunerNoteMapper.mapFrequency(sharpA4)!!
        val match = TunerNoteMapper.findNearestString(note, UkuleleTuning.HIGH_G)
        assertTrue("Should be sharp (positive cents)", match.centsFromTarget > 9.0)
    }

    @Test
    fun centsFromTargetNegativeForFlat() {
        // 15 cents flat of A4
        val flatA4 = 440.0 * Math.pow(2.0, -15.0 / 1200.0)
        val note = TunerNoteMapper.mapFrequency(flatA4)!!
        val match = TunerNoteMapper.findNearestString(note, UkuleleTuning.HIGH_G)
        assertTrue("Should be flat (negative cents)", match.centsFromTarget < -14.0)
    }

    // --- Custom A4 reference with string matching ----------------------------

    @Test
    fun customA4AffectsStringMatching() {
        // With A4=442, playing 442 Hz should match A string with ~0 cents
        val note = TunerNoteMapper.mapFrequency(442.0, a4Reference = 442.0)!!
        val match = TunerNoteMapper.findNearestString(note, UkuleleTuning.HIGH_G, a4Reference = 442.0)
        assertEquals("A", match.stringName)
        assertTrue("Cents from target should be near zero", abs(match.centsFromTarget) < 1.0)
    }
}
