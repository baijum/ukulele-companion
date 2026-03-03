package com.baijum.ukufretboard.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

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

    @Test
    fun fftOfDcSignalHasEnergyOnlyInBinZero() {
        val n = 256
        val real = FloatArray(n) { 1.0f }
        val imag = FloatArray(n)

        FFTProcessor.fft(real, imag)

        assertEquals("DC component should equal N", n.toFloat(), real[0], TOLERANCE)
        assertEquals("Imaginary DC should be 0", 0f, imag[0], TOLERANCE)

        // All other bins should be near zero
        for (i in 1 until n) {
            assertTrue(
                "Bin $i real should be near zero, was ${real[i]}",
                abs(real[i]) < TOLERANCE,
            )
            assertTrue(
                "Bin $i imag should be near zero, was ${imag[i]}",
                abs(imag[i]) < TOLERANCE,
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
        assertEquals("Peak should be at bin $binIndex", binIndex, maxBin)
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
            assertEquals(
                "Sample $i should be recovered after FFT→IFFT",
                original[i],
                real[i],
                TOLERANCE,
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

        assertEquals("First sample should be near zero", 0f, windowed[0], 0.01f)
        assertEquals("Last sample should be near zero", 0f, windowed[n - 1], 0.01f)
        // Middle should be near 1.0
        assertTrue("Middle sample should be near 1.0", windowed[n / 2] > 0.9f)
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
            assertEquals("Real[$i] should match", real1[i], real2[i], 1e-6f)
            assertEquals("Imag[$i] should match", imag1[i], imag2[i], 1e-6f)
        }
    }
}
