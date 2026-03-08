package com.baijum.ukufretboard.domain

import kotlin.math.log2

/**
 * Extracts a 12-bin chromagram (pitch-class energy profile) from an FFT
 * magnitude spectrum.
 *
 * A chromagram collapses the full frequency spectrum into 12 bins — one for
 * each pitch class in the chromatic scale (C, C#, D, … B). Each FFT bin's
 * energy is accumulated into the pitch class that its centre frequency maps to.
 *
 * This is the standard feature used in Music Information Retrieval (MIR) for
 * chord recognition: once you know which pitch classes are active, you can
 * match against chord interval formulas.
 */
object Chromagram {

    /** Reference frequency for C0 in Hz, used as the base for pitch-class mapping. */
    private const val C0_HZ = 16.3516f

    /**
     * Computes a 12-element chromagram from an FFT magnitude spectrum.
     *
     * @param magnitudes Magnitude spectrum from [FFTProcessor.magnitudeSpectrum]
     *   (length = fftSize / 2).
     * @param sampleRate Audio sample rate in Hz (e.g. 44100).
     * @param fftSize    Original FFT size (e.g. 4096). Used to compute the
     *   frequency of each bin: `freq = bin * sampleRate / fftSize`.
     * @param minFreq    Minimum frequency to consider (default ~C3 = 130 Hz).
     *   Bins below this are ignored to reduce low-frequency noise.
     * @param maxFreq    Maximum frequency to consider (default ~C6 = 1050 Hz).
     *   Bins above this are ignored to reduce overtone pollution.
     * @return A [FloatArray] of 12 elements, indexed by pitch class (C = 0,
     *   C# = 1, … B = 11). Values are normalised so the array sums to 1.0,
     *   or all zeros if the signal is silent.
     */
    fun compute(
        magnitudes: FloatArray,
        sampleRate: Int,
        fftSize: Int,
        minFreq: Float = 130.0f,
        maxFreq: Float = 1050.0f,
    ): FloatArray {
        val chroma = FloatArray(12)
        val freqPerBin = sampleRate.toFloat() / fftSize

        // Compute the bin range corresponding to [minFreq, maxFreq].
        val minBin = (minFreq / freqPerBin).toInt().coerceAtLeast(1)
        val maxBin = (maxFreq / freqPerBin).toInt().coerceAtMost(magnitudes.size - 1)

        for (bin in minBin..maxBin) {
            val freq = bin * freqPerBin
            if (freq < minFreq) continue

            // Map frequency → pitch class using equal temperament.
            // pitchClass = round(12 * log2(freq / C0)) mod 12
            val semitones = (12.0 * log2(freq.toDouble() / C0_HZ)).toFloat()
            val pitchClass = ((semitones.toInt() % 12) + 12) % 12

            // Accumulate squared magnitude for better energy representation.
            chroma[pitchClass] += magnitudes[bin] * magnitudes[bin]
        }

        // Normalise so values sum to 1.0 (or remain all-zero for silence).
        val total = chroma.sum()
        if (total > 0f) {
            for (i in chroma.indices) {
                chroma[i] /= total
            }
        }

        return chroma
    }
}
