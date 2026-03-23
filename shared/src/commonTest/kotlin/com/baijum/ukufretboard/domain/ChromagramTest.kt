package com.baijum.ukufretboard.domain

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChromagramTest {

    private val sampleRate = 44100
    private val fftSize = 4096

    /**
     * Build a magnitude spectrum with a peak at a given frequency.
     * Spreads energy across a few bins around the target to account for
     * FFT bin-center rounding in the chromagram's log2 pitch mapping.
     */
    private fun singlePeakMagnitudes(freqHz: Float): FloatArray {
        val mags = FloatArray(fftSize / 2)
        val freqPerBin = sampleRate.toFloat() / fftSize
        val centerBin = (freqHz / freqPerBin).toInt()
        // Place energy in a small cluster so the chromagram accumulates
        // into the correct pitch class even if the center bin rounds down.
        for (b in (centerBin - 1)..(centerBin + 1)) {
            if (b in mags.indices) mags[b] = 1.0f
        }
        return mags
    }

    /** Build a magnitude spectrum with peaks at multiple frequencies. */
    private fun multiPeakMagnitudes(vararg freqs: Float): FloatArray {
        val mags = FloatArray(fftSize / 2)
        val freqPerBin = sampleRate.toFloat() / fftSize
        for (freq in freqs) {
            val centerBin = (freq / freqPerBin).toInt()
            for (b in (centerBin - 1)..(centerBin + 1)) {
                if (b in mags.indices) mags[b] = 1.0f
            }
        }
        return mags
    }

    // --- Returns 12 bins ---

    @Test
    fun returnsExactly12Bins() {
        val chroma = Chromagram.compute(FloatArray(fftSize / 2), sampleRate, fftSize)
        assertEquals(12, chroma.size)
    }

    // --- Silent input ---

    @Test
    fun silentInputReturnsAllZeros() {
        val chroma = Chromagram.compute(FloatArray(fftSize / 2), sampleRate, fftSize)
        assertTrue(chroma.all { it == 0f }, "Silent input should produce all zeros")
    }

    // --- All bins non-negative ---

    @Test
    fun allBinsAreNonNegative() {
        val mags = singlePeakMagnitudes(440f)
        val chroma = Chromagram.compute(mags, sampleRate, fftSize)
        for (i in chroma.indices) {
            assertTrue(chroma[i] >= 0f, "Bin $i should be non-negative: ${chroma[i]}")
        }
    }

    // --- Normalization sums to 1 ---

    @Test
    fun nonSilentChromaSumsToOne() {
        val mags = singlePeakMagnitudes(440f) // A4
        val chroma = Chromagram.compute(mags, sampleRate, fftSize)
        val sum = chroma.sum()
        assertTrue(sum > 0.99f && sum < 1.01f, "Chroma should sum to ~1.0: $sum")
    }

    // --- Pitch class mapping (within ±1 due to FFT bin-center rounding) ---

    @Test
    fun a440MapsNearPitchClass9() {
        val mags = singlePeakMagnitudes(440f)
        val chroma = Chromagram.compute(mags, sampleRate, fftSize)
        val maxIdx = chroma.indices.maxByOrNull { chroma[it] } ?: -1
        // FFT bin centers may not perfectly align with A4, so ±1 is acceptable
        assertTrue(maxIdx in listOf(8, 9, 10),
            "A4 should map near pitch class 9 (A), got $maxIdx")
    }

    @Test
    fun c4MapsNearPitchClass0() {
        val mags = singlePeakMagnitudes(261.63f)
        val chroma = Chromagram.compute(mags, sampleRate, fftSize)
        val maxIdx = chroma.indices.maxByOrNull { chroma[it] } ?: -1
        assertTrue(maxIdx in listOf(11, 0, 1),
            "C4 should map near pitch class 0 (C), got $maxIdx")
    }

    @Test
    fun e4MapsNearPitchClass4() {
        val mags = singlePeakMagnitudes(329.63f)
        val chroma = Chromagram.compute(mags, sampleRate, fftSize)
        val maxIdx = chroma.indices.maxByOrNull { chroma[it] } ?: -1
        assertTrue(maxIdx in listOf(3, 4, 5),
            "E4 should map near pitch class 4 (E), got $maxIdx")
    }

    // --- Multiple peaks produce energy in multiple bins ---

    @Test
    fun multipleFrequenciesProduceMultipleActiveBins() {
        val mags = multiPeakMagnitudes(261.63f, 329.63f, 392.0f) // C4, E4, G4
        val chroma = Chromagram.compute(mags, sampleRate, fftSize)
        // Should have energy in at least 3 bins
        val activeBins = chroma.count { it > 0.05f }
        assertTrue(activeBins >= 3,
            "C major chord should activate at least 3 pitch classes, got $activeBins")
    }

    // --- Different sample rates produce non-zero output ---

    @Test
    fun differentSampleRateProducesNonZeroOutput() {
        val sr = 22050
        val mags = FloatArray(fftSize / 2)
        val freqPerBin = sr.toFloat() / fftSize
        val bin = (440f / freqPerBin).toInt()
        for (b in (bin - 1)..(bin + 1)) {
            if (b in mags.indices) mags[b] = 1.0f
        }
        val chroma = Chromagram.compute(mags, sr, fftSize)
        assertTrue(chroma.any { it > 0f }, "Should produce non-zero output at 22050 Hz")
    }

    // --- Frequencies outside range are excluded ---

    @Test
    fun lowFrequencyExcluded() {
        // A very low frequency below minFreq (130 Hz) should not contribute
        val mags = singlePeakMagnitudes(50f)
        val chroma = Chromagram.compute(mags, sampleRate, fftSize)
        assertTrue(chroma.all { it == 0f }, "Sub-130Hz should be excluded")
    }

    @Test
    fun highFrequencyExcluded() {
        // A very high frequency above maxFreq (1050 Hz) should not contribute
        val mags = singlePeakMagnitudes(2000f)
        val chroma = Chromagram.compute(mags, sampleRate, fftSize)
        assertTrue(chroma.all { it == 0f }, "Above-1050Hz should be excluded")
    }
}
