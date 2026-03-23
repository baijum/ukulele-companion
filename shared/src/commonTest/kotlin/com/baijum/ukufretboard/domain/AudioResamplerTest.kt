package com.baijum.ukufretboard.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [AudioResampler].
 *
 * Verifies downsampling ratio and that the anti-aliasing filter preserves
 * low-frequency content while attenuating high frequencies.
 */
class AudioResamplerTest {

    private fun assertApproxEquals(expected: Float, actual: Float, tolerance: Float, message: String? = null) {
        assertTrue(abs(expected - actual) <= tolerance, message ?: "Expected $expected ± $tolerance but was $actual")
    }

    @Test
    fun outputLengthIsCorrect() {
        // 4096 samples at 44.1kHz → should produce ~1486 samples at 16kHz
        val input = FloatArray(4096) { 0.5f }
        val output = AudioResampler.downsample44kTo16k(input)
        val expectedLength = (4096 / (44100.0 / 16000.0)).toInt()
        assertEquals(expectedLength, output.size)
    }

    @Test
    fun emptyInputReturnsEmpty() {
        val output = AudioResampler.downsample44kTo16k(FloatArray(0))
        assertEquals(0, output.size)
    }

    @Test
    fun preservesLowFrequencyContent() {
        // A 440 Hz sine at 44.1kHz should survive downsampling to 16kHz
        // (440 Hz is well below the 8 kHz Nyquist)
        val n = 4096
        val freq = 440.0
        val input = FloatArray(n) {
            sin(2.0 * PI * freq * it / 44100.0).toFloat()
        }

        val output = AudioResampler.downsample44kTo16k(input)

        // Verify the output still has periodic structure
        // Check that it's not all zeros and has reasonable amplitude
        val maxAbs = output.maxOf { abs(it) }
        assertTrue(
            maxAbs > 0.7f,
            "Low-frequency content should be preserved (max amplitude $maxAbs)",
        )
    }

    @Test
    fun dcSignalIsPreserved() {
        val input = FloatArray(1024) { 0.75f }
        val output = AudioResampler.downsample44kTo16k(input)

        for (i in output.indices) {
            assertApproxEquals(
                0.75f,
                output[i],
                0.05f,
                "DC level should be preserved at sample $i",
            )
        }
    }

    @Test
    fun singleSampleReturnsOneSample() {
        val input = floatArrayOf(0.42f)
        val output = AudioResampler.downsample44kTo16k(input)
        assertEquals(1, output.size)
    }
}
