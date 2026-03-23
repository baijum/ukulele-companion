package com.baijum.ukufretboard.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [FFTProcessor].
 *
 * Verifies FFT correctness using known signals (DC, pure sine) and
 * round-trip (FFT then IFFT) consistency.
 */
class FFTProcessorTest {

    companion object {
        private const val TOLERANCE = 1e-3f
    }

    private fun assertApproxEquals(expected: Float, actual: Float, tolerance: Float, message: String? = null) {
        assertTrue(abs(expected - actual) <= tolerance, message ?: "Expected $expected ± $tolerance but was $actual")
    }

    @Test
    fun fftOfDcSignalHasEnergyOnlyInBinZero() {
        val n = 256
        val real = FloatArray(n) { 1.0f }
        val imag = FloatArray(n)

        FFTProcessor.fft(real, imag)

        assertApproxEquals(n.toFloat(), real[0], TOLERANCE, "DC component should equal N")
        assertApproxEquals(0f, imag[0], TOLERANCE, "Imaginary DC should be 0")

        // All other bins should be near zero
        for (i in 1 until n) {
            assertTrue(
                abs(real[i]) < TOLERANCE,
                "Bin $i real should be near zero, was ${real[i]}",
            )
            assertTrue(
                abs(imag[i]) < TOLERANCE,
                "Bin $i imag should be near zero, was ${imag[i]}",
            )
        }
    }

    @Test
    fun fftOfPureSineHasPeakAtCorrectBin() {
        val n = 1024
        val binIndex = 10 // sine at frequency = binIndex * sampleRate / n
        val real = FloatArray(n)
        val imag = FloatArray(n)

        for (i in 0 until n) {
            real[i] = sin(2.0 * PI * binIndex * i / n).toFloat()
        }

        FFTProcessor.fft(real, imag)

        val magnitudes = FFTProcessor.magnitudeSpectrum(real, imag)

        // Peak should be at the bin index
        var maxBin = 0
        var maxVal = 0f
        for (i in magnitudes.indices) {
            if (magnitudes[i] > maxVal) {
                maxVal = magnitudes[i]
                maxBin = i
            }
        }
        assertEquals(binIndex, maxBin, "Peak should be at bin $binIndex")
    }

    @Test
    fun fftThenIfftRecoversOriginalSignal() {
        val n = 512
        val original = FloatArray(n)
        for (i in 0 until n) {
            original[i] = sin(2.0 * PI * 7 * i / n).toFloat() +
                0.5f * sin(2.0 * PI * 23 * i / n).toFloat()
        }

        val real = original.copyOf()
        val imag = FloatArray(n)

        FFTProcessor.fft(real, imag)
        FFTProcessor.ifft(real, imag)

        for (i in 0 until n) {
            assertApproxEquals(
                original[i],
                real[i],
                TOLERANCE,
                "Sample $i should be recovered after FFT→IFFT",
            )
        }
    }

    @Test
    fun magnitudeSpectrumHasCorrectLength() {
        val n = 256
        val real = FloatArray(n)
        val imag = FloatArray(n)
        val mag = FFTProcessor.magnitudeSpectrum(real, imag)
        assertEquals(n / 2, mag.size)
    }

    @Test
    fun hanningWindowTapersEnds() {
        val n = 256
        val samples = FloatArray(n) { 1.0f }
        val windowed = FFTProcessor.hanningWindow(samples)

        assertApproxEquals(0f, windowed[0], 0.01f, "First sample should be near zero")
        assertApproxEquals(0f, windowed[n - 1], 0.01f, "Last sample should be near zero")
        // Middle should be near 1.0
        assertTrue(windowed[n / 2] > 0.9f, "Middle sample should be near 1.0")
    }

    @Test
    fun twiddleCachingProducesSameResults() {
        // Run FFT twice with the same size to exercise twiddle caching
        val n = 256
        val real1 = FloatArray(n) { sin(2.0 * PI * 5 * it / n).toFloat() }
        val imag1 = FloatArray(n)
        FFTProcessor.fft(real1, imag1)

        val real2 = FloatArray(n) { sin(2.0 * PI * 5 * it / n).toFloat() }
        val imag2 = FloatArray(n)
        FFTProcessor.fft(real2, imag2)

        for (i in 0 until n) {
            assertApproxEquals(real1[i], real2[i], 1e-6f, "Real[$i] should match")
            assertApproxEquals(imag1[i], imag2[i], 1e-6f, "Imag[$i] should match")
        }
    }
}
