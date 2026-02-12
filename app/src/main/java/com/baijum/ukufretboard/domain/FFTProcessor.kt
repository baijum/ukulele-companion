package com.baijum.ukufretboard.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure-Kotlin radix-2 Cooley–Tukey FFT implementation.
 *
 * Operates on buffers whose length is a power of two (e.g. 4096 samples
 * from [com.baijum.ukufretboard.audio.AudioCaptureEngine]).
 *
 * All operations are allocation-light and suitable for real-time audio
 * processing on the UI thread budget (~93 ms per frame at 44.1 kHz / 4096).
 */
object FFTProcessor {

    /**
     * Applies a Hanning window to [samples] to reduce spectral leakage.
     *
     * The Hanning (raised cosine) window tapers both ends of the buffer
     * to zero, which dramatically reduces side-lobe artefacts in the FFT
     * at the cost of a small reduction in frequency resolution.
     *
     * @param samples Normalised audio samples (−1.0 .. 1.0).
     * @return A new [FloatArray] with the window applied.
     */
    fun hanningWindow(samples: FloatArray): FloatArray {
        val n = samples.size
        val windowed = FloatArray(n)
        for (i in 0 until n) {
            val w = 0.5f * (1.0f - cos(2.0 * PI * i / (n - 1)).toFloat())
            windowed[i] = samples[i] * w
        }
        return windowed
    }

    /**
     * Computes an in-place radix-2 Cooley–Tukey FFT.
     *
     * On return, [real] and [imag] contain the complex frequency-domain
     * representation. The length of both arrays must be the same power of two.
     *
     * @param real Real part of the signal (overwritten with FFT result).
     * @param imag Imaginary part (should be all-zero for a real signal;
     *   overwritten with FFT result).
     */
    fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        require(n > 0 && n and (n - 1) == 0) { "FFT size must be a power of 2" }

        // --- Bit-reversal permutation ----------------------------------------
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit

            if (i < j) {
                // Swap real
                val tmpR = real[i]; real[i] = real[j]; real[j] = tmpR
                // Swap imag
                val tmpI = imag[i]; imag[i] = imag[j]; imag[j] = tmpI
            }
        }

        // --- Butterfly stages ------------------------------------------------
        var len = 2
        while (len <= n) {
            val halfLen = len / 2
            val angle = -2.0 * PI / len
            val wReal = cos(angle).toFloat()
            val wImag = sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var curR = 1.0f
                var curI = 0.0f

                for (k in 0 until halfLen) {
                    val evenIdx = i + k
                    val oddIdx = i + k + halfLen

                    // Twiddle factor × odd element
                    val tR = curR * real[oddIdx] - curI * imag[oddIdx]
                    val tI = curR * imag[oddIdx] + curI * real[oddIdx]

                    real[oddIdx] = real[evenIdx] - tR
                    imag[oddIdx] = imag[evenIdx] - tI
                    real[evenIdx] = real[evenIdx] + tR
                    imag[evenIdx] = imag[evenIdx] + tI

                    // Advance twiddle factor
                    val nextR = curR * wReal - curI * wImag
                    val nextI = curR * wImag + curI * wReal
                    curR = nextR
                    curI = nextI
                }
                i += len
            }
            len = len shl 1
        }
    }

    /**
     * Computes an in-place inverse FFT (IFFT).
     *
     * Uses the identity IFFT(X) = (1/N) * conj(FFT(conj(X))):
     * 1. Conjugate the input (negate imaginary parts).
     * 2. Apply the forward [fft].
     * 3. Conjugate the output and divide by N.
     *
     * On return, [real] and [imag] contain the time-domain signal.
     *
     * @param real Real part of the frequency-domain signal (overwritten).
     * @param imag Imaginary part (overwritten).
     */
    fun ifft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        require(n > 0 && n and (n - 1) == 0) { "IFFT size must be a power of 2" }

        // Conjugate input
        for (i in imag.indices) imag[i] = -imag[i]

        // Forward FFT
        fft(real, imag)

        // Conjugate output and normalise
        val invN = 1.0f / n
        for (i in 0 until n) {
            real[i] *= invN
            imag[i] = -imag[i] * invN
        }
    }

    /**
     * Computes the magnitude spectrum from a complex FFT result.
     *
     * Only the first N/2 bins are returned (the positive-frequency half),
     * since the input is real-valued and the spectrum is symmetric.
     *
     * @param real Real part of the FFT result.
     * @param imag Imaginary part of the FFT result.
     * @return A [FloatArray] of length N/2 containing magnitude values.
     */
    fun magnitudeSpectrum(real: FloatArray, imag: FloatArray): FloatArray {
        val halfN = real.size / 2
        val magnitudes = FloatArray(halfN)
        for (i in 0 until halfN) {
            magnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }
        return magnitudes
    }
}
