package com.baijum.ukufretboard.domain

import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PitchDetectorPropertyTest {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 4096
    }

    private fun generateSine(frequency: Double, sampleRate: Int, length: Int): FloatArray {
        return FloatArray(length) { i ->
            (0.8 * sin(2.0 * PI * frequency * i / sampleRate)).toFloat()
        }
    }

    @Test
    fun silentBufferReturnsNull() {
        runBlocking {
            checkAll(Arb.element(listOf(256, 512, 1024, 2048, 4096))) { size ->
                val silent = FloatArray(size)
                assertNull(PitchDetector.detect(silent, SAMPLE_RATE))
            }
        }
    }

    @Test
    fun veryQuietBufferReturnsNull() {
        runBlocking {
            checkAll(Arb.double(0.0..0.005)) { amplitude ->
                val quiet = FloatArray(BUFFER_SIZE) {
                    (amplitude * sin(2.0 * PI * 440.0 * it / SAMPLE_RATE)).toFloat()
                }
                assertNull(PitchDetector.detect(quiet, SAMPLE_RATE))
            }
        }
    }

    @Test
    fun pureSineDetectsCorrectFrequency() {
        runBlocking {
            val ukuleleFrequencies = listOf(
                261.63, // C4
                293.66, // D4
                329.63, // E4
                349.23, // F4
                392.00, // G4
                440.00, // A4
                493.88, // B4
            )
            checkAll(Arb.element(ukuleleFrequencies)) { freq ->
                val samples = generateSine(freq, SAMPLE_RATE, BUFFER_SIZE)
                val result = PitchDetector.detect(samples, SAMPLE_RATE)
                assertNotNull(result, "Should detect pitch for ${freq}Hz")
                val error = abs(result.frequencyHz - freq)
                assertTrue(
                    error / freq < 0.02,
                    "Detected ${result.frequencyHz}Hz should be within 2% of ${freq}Hz (error=$error)",
                )
            }
        }
    }

    @Test
    fun detectedFrequencyIsInValidRange() {
        runBlocking {
            val frequencies = listOf(100.0, 200.0, 300.0, 440.0, 600.0, 800.0, 1000.0)
            checkAll(Arb.element(frequencies)) { freq ->
                val samples = generateSine(freq, SAMPLE_RATE, BUFFER_SIZE)
                val result = PitchDetector.detect(samples, SAMPLE_RATE)
                if (result != null) {
                    assertTrue(
                        result.frequencyHz >= 65.0,
                        "Frequency ${result.frequencyHz} should be >= 65Hz",
                    )
                    assertTrue(
                        result.frequencyHz <= 1100.0,
                        "Frequency ${result.frequencyHz} should be <= 1100Hz",
                    )
                }
            }
        }
    }

    @Test
    fun confidenceIsNonNegative() {
        runBlocking {
            val frequencies = listOf(220.0, 330.0, 440.0, 660.0, 880.0)
            checkAll(Arb.element(frequencies)) { freq ->
                val samples = generateSine(freq, SAMPLE_RATE, BUFFER_SIZE)
                val result = PitchDetector.detect(samples, SAMPLE_RATE)
                if (result != null) {
                    assertTrue(
                        result.confidence >= 0.0,
                        "Confidence ${result.confidence} should be >= 0",
                    )
                }
            }
        }
    }

    @Test
    fun rmsOfSilenceIsZero() {
        val silent = FloatArray(1024)
        assertTrue(PitchDetector.rms(silent) == 0f)
    }

    @Test
    fun rmsIsNonNegative() {
        runBlocking {
            checkAll(100, Arb.int(10..1000)) { size ->
                val samples = FloatArray(size) { (Random.nextDouble() * 2 - 1).toFloat() }
                assertTrue(PitchDetector.rms(samples) >= 0f)
            }
        }
    }

    @Test
    fun tinyBufferReturnsNull() {
        runBlocking {
            checkAll(Arb.element(listOf(1, 2, 3))) { size ->
                val samples = FloatArray(size) { 0.5f }
                assertNull(PitchDetector.detect(samples, SAMPLE_RATE))
            }
        }
    }

    @Test
    fun previousFrequencyDoesNotCauseCrash() {
        runBlocking {
            val samples = generateSine(440.0, SAMPLE_RATE, BUFFER_SIZE)
            checkAll(Arb.double(-1000.0..5000.0)) { prevFreq ->
                PitchDetector.detect(samples, SAMPLE_RATE, previousFrequency = prevFreq)
            }
        }
    }
}
