package com.baijum.ukufretboard.domain

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioChordDetectorTest {

    private val sampleRate = 44100
    private val fftSize = 4096

    /** Generate a sine wave at a given frequency. */
    private fun sineWave(freqHz: Float, size: Int = fftSize): FloatArray {
        return FloatArray(size) { i ->
            sin(2.0 * PI * freqHz * i / sampleRate).toFloat()
        }
    }

    /** Generate a sum of sine waves (chord). */
    private fun chordSignal(vararg freqs: Float, size: Int = fftSize): FloatArray {
        val signal = FloatArray(size)
        for (freq in freqs) {
            for (i in signal.indices) {
                signal[i] += sin(2.0 * PI * freq * i / sampleRate).toFloat()
            }
        }
        // Normalize
        val max = signal.maxOrNull() ?: 1f
        if (max > 0f) {
            for (i in signal.indices) signal[i] /= max
        }
        return signal
    }

    // --- Silent input ---

    @Test
    fun silentAudioReturnsNoSelection() {
        val result = AudioChordDetector.detect(FloatArray(fftSize), sampleRate)
        assertEquals(
            ChordDetector.DetectionResult.NoSelection,
            result.detection
        )
        assertEquals(0f, result.confidence)
        assertTrue(result.activePitchClasses.isEmpty())
    }

    // --- Single note ---

    @Test
    fun singleNoteDetectsSomething() {
        val samples = sineWave(440f) // A4
        val result = AudioChordDetector.detect(samples, sampleRate)
        // Single note can't form a chord, but should not crash
        assertTrue(result.confidence >= 0f)
    }

    // --- Confidence ---

    @Test
    fun confidenceIsInRange() {
        val samples = chordSignal(261.63f, 329.63f, 392.0f) // C major
        val result = AudioChordDetector.detect(samples, sampleRate)
        assertTrue(result.confidence in 0f..1f, "Confidence should be 0-1: ${result.confidence}")
    }

    // --- Chromagram returned ---

    @Test
    fun chromagramHas12Bins() {
        val samples = sineWave(440f)
        val result = AudioChordDetector.detect(samples, sampleRate)
        assertEquals(12, result.chromagram.size)
    }

    // --- Active pitch classes ---

    @Test
    fun activePitchClassesAreValid() {
        val samples = chordSignal(261.63f, 329.63f, 392.0f)
        val result = AudioChordDetector.detect(samples, sampleRate)
        for (pc in result.activePitchClasses) {
            assertTrue(pc in 0..11, "Pitch class $pc out of range")
        }
    }

    // --- Threshold affects sensitivity ---

    @Test
    fun lowerThresholdDetectsMorePitchClasses() {
        val samples = chordSignal(261.63f, 329.63f, 392.0f) // C major
        val lowThresh = AudioChordDetector.detect(samples, sampleRate, threshold = 0.1f)
        val highThresh = AudioChordDetector.detect(samples, sampleRate, threshold = 0.5f)
        assertTrue(
            lowThresh.activePitchClasses.size >= highThresh.activePitchClasses.size,
            "Lower threshold should detect >= pitch classes"
        )
    }

    // --- Preferred root weight ---

    @Test
    fun preferredRootDoesNotCrash() {
        val samples = chordSignal(261.63f, 329.63f, 392.0f)
        val result = AudioChordDetector.detect(
            samples, sampleRate,
            preferredRootPitchClass = 0, // C
            preferredRootWeight = 1.5f,
        )
        assertTrue(result.confidence >= 0f)
    }

    // --- Repeated calls ---

    @Test
    fun repeatedCallsDoNotCrash() {
        val samples = chordSignal(261.63f, 329.63f, 392.0f)
        repeat(10) {
            AudioChordDetector.detect(samples, sampleRate)
        }
    }

    // --- Detection result types ---

    @Test
    fun detectionResultIsValidType() {
        val samples = chordSignal(261.63f, 329.63f, 392.0f)
        val result = AudioChordDetector.detect(samples, sampleRate)
        assertTrue(
            result.detection is ChordDetector.DetectionResult.NoSelection ||
                result.detection is ChordDetector.DetectionResult.SingleNote ||
                result.detection is ChordDetector.DetectionResult.Interval ||
                result.detection is ChordDetector.DetectionResult.ChordFound ||
                result.detection is ChordDetector.DetectionResult.NoMatch,
            "Should be a valid DetectionResult type"
        )
    }
}
