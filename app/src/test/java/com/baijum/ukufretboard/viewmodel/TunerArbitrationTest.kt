package com.baijum.ukufretboard.viewmodel

import com.baijum.ukufretboard.domain.NeuralArbitrator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the pitch arbitration helper functions.
 *
 * Semitone distance and octave detection logic now live in
 * [NeuralArbitrator] in the shared KMP module. This test class
 * exercises them from the Android side and verifies that the
 * [TunerViewModel] tuning-status thresholds remain correct.
 */
class TunerArbitrationTest {

    // ── semitoneDistance ──────────────────────────────────────────────

    @Test
    fun semitoneDistanceUnisonIsZero() {
        assertEquals(0.0, NeuralArbitrator.semitoneDistance(440.0, 440.0), 0.001)
    }

    @Test
    fun semitoneDistanceOctaveIsTwelve() {
        assertEquals(12.0, NeuralArbitrator.semitoneDistance(440.0, 880.0), 0.01)
        assertEquals(12.0, NeuralArbitrator.semitoneDistance(880.0, 440.0), 0.01)
    }

    @Test
    fun semitoneDistanceTwoOctavesIsTwentyFour() {
        assertEquals(24.0, NeuralArbitrator.semitoneDistance(220.0, 880.0), 0.01)
    }

    @Test
    fun semitoneDistancePerfectFifthIsSeven() {
        val a4 = 440.0
        val e5 = a4 * Math.pow(2.0, 7.0 / 12.0)
        assertEquals(7.0, NeuralArbitrator.semitoneDistance(a4, e5), 0.01)
    }

    @Test
    fun semitoneDistanceZeroOrNegativeReturnsMaxValue() {
        assertEquals(Double.MAX_VALUE, NeuralArbitrator.semitoneDistance(0.0, 440.0), 0.0)
        assertEquals(Double.MAX_VALUE, NeuralArbitrator.semitoneDistance(440.0, 0.0), 0.0)
        assertEquals(Double.MAX_VALUE, NeuralArbitrator.semitoneDistance(-1.0, 440.0), 0.0)
        assertEquals(Double.MAX_VALUE, NeuralArbitrator.semitoneDistance(440.0, -1.0), 0.0)
    }

    @Test
    fun semitoneDistanceIsSymmetric() {
        val d1 = NeuralArbitrator.semitoneDistance(440.0, 523.25)
        val d2 = NeuralArbitrator.semitoneDistance(523.25, 440.0)
        assertEquals(d1, d2, 0.001)
    }

    // ── isOctaveRelation ─────────────────────────────────────────────

    @Test
    fun isOctaveRelationTrueForExactOctave() {
        assertTrue(NeuralArbitrator.isOctaveRelation(440.0, 880.0))
        assertTrue(NeuralArbitrator.isOctaveRelation(880.0, 440.0))
    }

    @Test
    fun isOctaveRelationTrueForDoubleOctave() {
        assertTrue(NeuralArbitrator.isOctaveRelation(220.0, 880.0))
        assertTrue(NeuralArbitrator.isOctaveRelation(880.0, 220.0))
    }

    @Test
    fun isOctaveRelationTrueWithSlightDetuning() {
        val slightlySharp = 880.0 * Math.pow(2.0, 0.8 / 12.0)
        assertTrue(NeuralArbitrator.isOctaveRelation(440.0, slightlySharp))
    }

    @Test
    fun isOctaveRelationFalseForUnison() {
        assertFalse(NeuralArbitrator.isOctaveRelation(440.0, 440.0))
    }

    @Test
    fun isOctaveRelationFalseForFifth() {
        val e5 = 440.0 * Math.pow(2.0, 7.0 / 12.0)
        assertFalse(NeuralArbitrator.isOctaveRelation(440.0, e5))
    }

    @Test
    fun isOctaveRelationFalseForZeroOrNegative() {
        assertFalse(NeuralArbitrator.isOctaveRelation(0.0, 440.0))
        assertFalse(NeuralArbitrator.isOctaveRelation(440.0, -1.0))
    }

    // ── Tuning status thresholds ─────────────────────────────────────

    @Test
    fun inTuneCentsThresholdValues() {
        assertEquals(6.0, TunerViewModel.IN_TUNE_CENTS, 0.0)
        assertEquals(2.0, TunerViewModel.PRECISION_IN_TUNE_CENTS, 0.0)
        assertEquals(15.0, TunerViewModel.CLOSE_CENTS, 0.0)
    }
}
