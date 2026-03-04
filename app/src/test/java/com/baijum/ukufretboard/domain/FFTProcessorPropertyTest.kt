package com.baijum.ukufretboard.domain

import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.float
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class FFTProcessorPropertyTest {

    companion object {
        private const val TOLERANCE = 1e-2f
    }

    @Test
    fun fftThenIfftRoundTripForRandomSignals() {
        runBlocking {
            val sizes = listOf(64, 128, 256, 512)
            checkAll(100, Arb.element(sizes)) { n ->
                val original = FloatArray(n) { (Math.random() * 2 - 1).toFloat() }
                val real = original.copyOf()
                val imag = FloatArray(n)

                FFTProcessor.fft(real, imag)
                FFTProcessor.ifft(real, imag)

                for (i in 0 until n) {
                    assertTrue(
                        "Sample $i: expected ${original[i]}, got ${real[i]}",
                        abs(real[i] - original[i]) < TOLERANCE,
                    )
                }
            }
        }
    }

    @Test
    fun fftOfSilenceIsAllZero() {
        runBlocking {
            val sizes = listOf(64, 128, 256, 512)
            checkAll(Arb.element(sizes)) { n ->
                val real = FloatArray(n)
                val imag = FloatArray(n)

                FFTProcessor.fft(real, imag)

                for (i in 0 until n) {
                    assertTrue(
                        "Bin $i real should be ~0, was ${real[i]}",
                        abs(real[i]) < 1e-6f,
                    )
                    assertTrue(
                        "Bin $i imag should be ~0, was ${imag[i]}",
                        abs(imag[i]) < 1e-6f,
                    )
                }
            }
        }
    }

    @Test
    fun fftPeakMatchesSineFrequencyBin() {
        runBlocking {
            val n = 512
            checkAll(Arb.element((1..n / 2 - 1).toList())) { bin ->
                val real = FloatArray(n) { sin(2.0 * PI * bin * it / n).toFloat() }
                val imag = FloatArray(n)

                FFTProcessor.fft(real, imag)
                val mag = FFTProcessor.magnitudeSpectrum(real, imag)

                var maxBin = 0
                var maxVal = 0f
                for (i in mag.indices) {
                    if (mag[i] > maxVal) {
                        maxVal = mag[i]
                        maxBin = i
                    }
                }
                assertEquals("Peak should be at bin $bin", bin, maxBin)
            }
        }
    }

    @Test
    fun magnitudeSpectrumLengthIsHalfInput() {
        runBlocking {
            val sizes = listOf(64, 128, 256, 512, 1024)
            checkAll(Arb.element(sizes)) { n ->
                val real = FloatArray(n)
                val imag = FloatArray(n)
                val mag = FFTProcessor.magnitudeSpectrum(real, imag)
                assertEquals(n / 2, mag.size)
            }
        }
    }

    @Test
    fun magnitudeSpectrumValuesAreNonNegative() {
        runBlocking {
            checkAll(100, Arb.element(listOf(64, 128, 256))) { n ->
                val real = FloatArray(n) { (Math.random() * 2 - 1).toFloat() }
                val imag = FloatArray(n)

                FFTProcessor.fft(real, imag)
                val mag = FFTProcessor.magnitudeSpectrum(real, imag)

                for (i in mag.indices) {
                    assertTrue("Magnitude[$i] should be >= 0, was ${mag[i]}", mag[i] >= 0f)
                }
            }
        }
    }

    @Test
    fun hanningWindowPreservesLength() {
        runBlocking {
            val sizes = listOf(64, 128, 256, 512, 1024)
            checkAll(Arb.element(sizes)) { n ->
                val samples = FloatArray(n) { 1.0f }
                val windowed = FFTProcessor.hanningWindow(samples)
                assertEquals(n, windowed.size)
            }
        }
    }

    @Test
    fun hanningWindowDoesNotAmplify() {
        runBlocking {
            checkAll(100, Arb.float(-1f..1f)) { value ->
                if (!value.isNaN() && !value.isInfinite()) {
                    val samples = FloatArray(128) { value }
                    val windowed = FFTProcessor.hanningWindow(samples)
                    for (i in windowed.indices) {
                        assertTrue(
                            "Windowed[$i]=${windowed[i]} should not exceed abs(input)=${abs(value)}",
                            abs(windowed[i]) <= abs(value) + 1e-6f,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun fftRejectsNonPowerOfTwo() {
        val badSizes = listOf(3, 5, 6, 7, 9, 10, 12, 15, 100)
        for (n in badSizes) {
            try {
                FFTProcessor.fft(FloatArray(n), FloatArray(n))
                assertTrue("Should have thrown for size $n", false)
            } catch (_: IllegalArgumentException) {
                // expected
            }
        }
    }
}
