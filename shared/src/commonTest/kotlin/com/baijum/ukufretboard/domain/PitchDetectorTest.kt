package com.baijum.ukufretboard.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    private fun assertApproxEquals(expected: Double, actual: Double, tolerance: Double, message: String? = null) {
        assertTrue(abs(expected - actual) <= tolerance, message ?: "Expected $expected ± $tolerance but was $actual")
    }

    private fun assertApproxEquals(expected: Float, actual: Float, tolerance: Float, message: String? = null) {
        assertTrue(abs(expected - actual) <= tolerance, message ?: "Expected $expected ± $tolerance but was $actual")
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
        assertNotNull(result, "Should detect A4 (440 Hz)")
        assertApproxEquals(440.0, result.frequencyHz, TOLERANCE_HZ)
    }

    @Test
    fun detectsC4_262Hz() {
        val samples = sineWave(261.63)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull(result, "Should detect C4 (~262 Hz)")
        assertApproxEquals(261.63, result.frequencyHz, TOLERANCE_HZ)
    }

    @Test
    fun detectsE4_330Hz() {
        val samples = sineWave(329.63)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull(result, "Should detect E4 (~330 Hz)")
        assertApproxEquals(329.63, result.frequencyHz, TOLERANCE_HZ)
    }

    @Test
    fun detectsG4_392Hz() {
        val samples = sineWave(392.0)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull(result, "Should detect G4 (392 Hz)")
        assertApproxEquals(392.0, result.frequencyHz, TOLERANCE_HZ)
    }

    @Test
    fun detectsD3_147Hz() {
        // Lowest standard ukulele string (Baritone)
        val samples = sineWave(146.83)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull(result, "Should detect D3 (~147 Hz)")
        assertApproxEquals(146.83, result.frequencyHz, TOLERANCE_HZ)
    }

    @Test
    fun detectsHighFrequency_880Hz() {
        val samples = sineWave(880.0)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull(result, "Should detect A5 (880 Hz)")
        // Higher frequencies have less lag resolution → wider tolerance.
        assertApproxEquals(880.0, result.frequencyHz, 5.0)
    }

    @Test
    fun detectsG3_196Hz() {
        // G3 = 196 Hz — Low-G ukulele tuning string
        val samples = sineWave(196.0)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull(result, "Should detect G3 (196 Hz)")
        assertApproxEquals(196.0, result.frequencyHz, TOLERANCE_HZ)
    }

    // --- Silence / noise rejection -------------------------------------------

    @Test
    fun rejectsSilence() {
        val samples = FloatArray(FRAME_SIZE) // all zeros
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNull(result, "Should return null for silence")
    }

    @Test
    fun rejectsVeryQuietSignal() {
        // Below the silence threshold (RMS < 0.01)
        val samples = sineWave(440.0, amplitude = 0.005f)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNull(result, "Should return null for very quiet signal")
    }

    // --- Confidence ----------------------------------------------------------

    @Test
    fun confidenceIsLowForCleanSine() {
        val samples = sineWave(440.0)
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNotNull(result)
        assertTrue(
            result.confidence < 0.1,
            "Confidence (CMND dip) should be low for a clean sine wave, was ${result.confidence}",
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
        assertNotNull(result, "Should detect with continuity constraint")
        assertApproxEquals(440.0, result.frequencyHz, TOLERANCE_HZ)
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
        assertNotNull(result, "Should fall back to full range on string switch")
        assertApproxEquals(261.63, result.frequencyHz, TOLERANCE_HZ)
    }

    // --- RMS utility ---------------------------------------------------------

    @Test
    fun rmsOfSilenceIsZero() {
        val samples = FloatArray(1024)
        assertApproxEquals(0f, PitchDetector.rms(samples), 1e-6f)
    }

    @Test
    fun rmsOfFullScaleSine() {
        val samples = sineWave(440.0, numSamples = 1024, amplitude = 1.0f)
        val rms = PitchDetector.rms(samples)
        // RMS of a sine wave with amplitude A is A / sqrt(2) ≈ 0.707
        assertApproxEquals(0.707f, rms, 0.02f)
    }

    // --- Edge cases ----------------------------------------------------------

    @Test
    fun handlesTinyBuffer() {
        val samples = FloatArray(2) { 0.5f }
        val result = PitchDetector.detect(samples, SAMPLE_RATE)
        assertNull(result, "Should return null for a buffer too small for YIN")
    }

    @Test
    fun handlesCustomThreshold() {
        val samples = sineWave(440.0)
        // Very strict threshold — clean sine should still pass
        val result = PitchDetector.detect(samples, SAMPLE_RATE, threshold = 0.05)
        assertNotNull(result, "Clean sine should pass strict threshold")
        assertApproxEquals(440.0, result.frequencyHz, TOLERANCE_HZ)
    }
}
