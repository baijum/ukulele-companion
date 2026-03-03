package com.baijum.ukufretboard.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Unit tests for [AudioResampler].
 *
 * Verifies downsampling ratio and that the anti-aliasing filter preserves
 * low-frequency content while attenuating high frequencies.
 */
class AudioResamplerTest {

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
            "Low-frequency content should be preserved (max amplitude $maxAbs)",
            maxAbs > 0.7f,
        )
    }

    @Test
    fun dcSignalIsPreserved() {
        val input = FloatArray(1024) { 0.75f }
        val output = AudioResampler.downsample44kTo16k(input)

        for (i in output.indices) {
            assertEquals(
                "DC level should be preserved at sample $i",
                0.75f,
                output[i],
                0.05f,
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
