package com.baijum.ukufretboard.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Unit tests for [PitchDetector] using synthetic sine waves.
 *
 * Each test generates a pure sine wave at a known frequency and verifies
 * that the YIN algorithm correctly detects it within a tolerance.
 */
class PitchDetectorTest {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val FRAME_SIZE = 4096
        /**
         * Maximum allowed frequency error in Hz.
         *
         * YIN with parabolic interpolation on a 4096-sample frame at 44.1 kHz
         * typically achieves ~0.5% accuracy. For 440 Hz that's ~2.2 Hz.
         * We use 3.0 Hz to account for edge effects in short test buffers.
         */
        private const val TOLERANCE_HZ = 3.0
        /** Amplitude for test signals (0..1). */
        private const val AMPLITUDE = 0.5f
    }

    /** Generates a pure sine wave at [frequencyHz]. */
    private fun sineWave(
        frequencyHz: Double,
        numSamples: Int = FRAME_SIZE,
        sampleRate: Int = SAMPLE_RATE,
        amplitude: Float = AMPLITUDE,
    ): FloatArray {
        val samples = FloatArray(numSamples)
        val omega = 2.0 * PI * frequencyHz / sampleRate
        for (i in 0 until numSamples) {
            samples[i] = (amplitude * sin(omega * i)).toFloat()
        }
        return samples
    }

    // --- Core pitch detection ------------------------------------------------

    @Test
    fun detectsA4_440Hz() {
        val samples = sineWave(440.0)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull("Should detect A4 (440 Hz)", result)
        assertEquals(440.0, result!!.frequencyHz, TOLERANCE_HZ)
    }

    @Test
    fun detectsC4_262Hz() {
        val samples = sineWave(261.63)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull("Should detect C4 (~262 Hz)", result)
        assertEquals(261.63, result!!.frequencyHz, TOLERANCE_HZ)
    }

    @Test
    fun detectsE4_330Hz() {
        val samples = sineWave(329.63)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull("Should detect E4 (~330 Hz)", result)
        assertEquals(329.63, result!!.frequencyHz, TOLERANCE_HZ)
    }

    @Test
    fun detectsG4_392Hz() {
        val samples = sineWave(392.0)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull("Should detect G4 (392 Hz)", result)
        assertEquals(392.0, result!!.frequencyHz, TOLERANCE_HZ)
    }

    @Test
    fun detectsD3_147Hz() {
        // Lowest standard ukulele string (Baritone)
        val samples = sineWave(146.83)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull("Should detect D3 (~147 Hz)", result)
        assertEquals(146.83, result!!.frequencyHz, TOLERANCE_HZ)
    }

    @Test
    fun detectsHighFrequency_880Hz() {
        val samples = sineWave(880.0)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull("Should detect A5 (880 Hz)", result)
        // Higher frequencies have less lag resolution → wider tolerance.
        assertEquals(880.0, result!!.frequencyHz, 5.0)
    }

    @Test
    fun detectsG3_196Hz() {
        // G3 = 196 Hz — Low-G ukulele tuning string
        val samples = sineWave(196.0)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull("Should detect G3 (196 Hz)", result)
        assertEquals(196.0, result!!.frequencyHz, TOLERANCE_HZ)
    }

    // --- Silence / noise rejection -------------------------------------------

    @Test
    fun rejectsSilence() {
        val samples = FloatArray(FRAME_SIZE) // all zeros
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNull("Should return null for silence", result)
    }

    @Test
    fun rejectsVeryQuietSignal() {
        // Below the silence threshold (RMS < 0.01)
        val samples = sineWave(440.0, amplitude = 0.005f)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNull("Should return null for very quiet signal", result)
    }

    // --- Confidence ----------------------------------------------------------

    @Test
    fun confidenceIsLowForCleanSine() {
        val samples = sineWave(440.0)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull(result)
        assertTrue(
            "Confidence (CMND dip) should be low for a clean sine wave, was ${result!!.confidence}",
            result.confidence < 0.1,
        )
    }

    // --- Continuity constraint -----------------------------------------------

    @Test
    fun continuityConstraintNarrowsSearch() {
        val samples = sineWave(440.0)
        // With a previous frequency close to the target, should still detect
        val result = PitchDetector.detect(
            samples,
            SAMPLE_RATE,
            previousFrequency = 435.0,
        )
        assertNotNull("Should detect with continuity constraint", result)
        assertEquals(440.0, result!!.frequencyHz, TOLERANCE_HZ)
    }

    @Test
    fun continuityFallsBackOnStringSwitch() {
        val samples = sineWave(261.63) // C4
        // Previous frequency far away (A4 = 440 Hz) — should fall back to full search
        val result = PitchDetector.detect(
            samples,
            SAMPLE_RATE,
            previousFrequency = 440.0,
        )
        assertNotNull("Should fall back to full range on string switch", result)
        assertEquals(261.63, result!!.frequencyHz, TOLERANCE_HZ)
    }

    // --- RMS utility ---------------------------------------------------------

    @Test
    fun rmsOfSilenceIsZero() {
        val samples = FloatArray(1024)
        assertEquals(0f, PitchDetector.rms(samples), 1e-6f)
    }

    @Test
    fun rmsOfFullScaleSine() {
        val samples = sineWave(440.0, numSamples = 1024, amplitude = 1.0f)
        val rms = PitchDetector.rms(samples)
        // RMS of a sine wave with amplitude A is A / sqrt(2) ≈ 0.707
        assertEquals(0.707f, rms, 0.02f)
    }

    // --- Edge cases ----------------------------------------------------------

    @Test
    fun handlesTinyBuffer() {
        val samples = FloatArray(2) { 0.5f }
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNull("Should return null for a buffer too small for YIN", result)
    }

    @Test
    fun handlesCustomThreshold() {
        val samples = sineWave(440.0)
        // Very strict threshold — clean sine should still pass
        val result = PitchDetector.detect(samples, SAMPLE_RATE, threshold = 0.05)
        assertNotNull("Clean sine should pass strict threshold", result)
        assertEquals(440.0, result!!.frequencyHz, TOLERANCE_HZ)
    }
}
