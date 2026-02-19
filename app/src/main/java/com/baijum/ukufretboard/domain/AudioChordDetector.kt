package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.audio.AudioCaptureEngine
import kotlin.math.max

/**
 * Detects chords from live audio by combining [FFTProcessor], [Chromagram],
 * and the existing [ChordDetector] formula-matching engine.
 *
 * Pipeline:
 * ```
 * FloatArray (audio samples)
 *     → FFTProcessor (Hanning window + FFT + magnitude spectrum)
 *     → Chromagram (12-bin pitch-class energy)
 *     → Threshold (select active pitch classes)
 *     → ChordDetector.detect() (interval-based formula matching)
 * ```
 *
 * This bridges the gap between raw audio and the existing chord detection
 * system, which was originally designed for fret-selection input.
 */
object AudioChordDetector {

    /**
     * Result of audio-based chord detection.
     *
     * @property detection The chord detection result from [ChordDetector].
     * @property confidence Ratio of energy in matched pitch classes vs. total
     *   chromagram energy (0.0 .. 1.0). Higher values indicate a cleaner match.
     * @property activePitchClasses The pitch classes that exceeded the energy
     *   threshold, used for debugging and visualization.
     */
    data class AudioChordResult(
        val detection: ChordDetector.DetectionResult,
        val confidence: Float,
        val activePitchClasses: Set<Int>,
        val chromagram: FloatArray,
    )

    /**
     * Default energy threshold as a fraction of the maximum chromagram bin.
     *
     * A pitch class is considered "active" if its energy exceeds
     * `threshold * maxBinEnergy`. Lower values are more sensitive (detect
     * quieter notes) but may pick up overtones as false positives.
     */
    private const val DEFAULT_THRESHOLD = 0.28f

    /**
     * Minimum number of active pitch classes required to attempt chord matching.
     * A chord requires at least 3 distinct pitch classes.
     */
    private const val MIN_PITCH_CLASSES = 3

    /**
     * Detects a chord from raw audio samples.
     *
     * @param samples Normalised audio samples (−1.0 .. 1.0) from
     *   [AudioCaptureEngine]. Length must be a power of two (e.g. 4096).
     * @param sampleRate Sample rate in Hz (e.g. 44100).
     * @param threshold Energy threshold as a fraction of the max bin
     *   (default [DEFAULT_THRESHOLD]).
     * @return An [AudioChordResult] with the detection result, confidence,
     *   and active pitch classes.
     */
    fun detect(
        samples: FloatArray,
        sampleRate: Int = AudioCaptureEngine.SAMPLE_RATE,
        threshold: Float = DEFAULT_THRESHOLD,
        preferredRootPitchClass: Int? = null,
        preferredRootWeight: Float = 1.15f,
    ): AudioChordResult {
        // Step 1: Windowed FFT
        val windowed = FFTProcessor.hanningWindow(samples)
        val real = windowed.copyOf()
        val imag = FloatArray(windowed.size)
        FFTProcessor.fft(real, imag)

        // Step 2: Magnitude spectrum
        val magnitudes = FFTProcessor.magnitudeSpectrum(real, imag)

        // Step 3: Chromagram
        val chroma = Chromagram.compute(
            magnitudes = magnitudes,
            sampleRate = sampleRate,
            fftSize = samples.size,
        )

        // Optional guidance: bias the chroma toward a stable root hint from
        // external pitch estimation (e.g., neural supervisor in Pitch Monitor).
        val weightedChroma = chroma.copyOf()
        if (preferredRootPitchClass != null && preferredRootPitchClass in 0..11) {
            weightedChroma[preferredRootPitchClass] =
                weightedChroma[preferredRootPitchClass] * max(1.0f, preferredRootWeight)
        }

        // Step 4: Threshold — find active pitch classes
        val maxEnergy = weightedChroma.max()
        if (maxEnergy <= 0f) {
            return AudioChordResult(
                detection = ChordDetector.DetectionResult.NoSelection,
                confidence = 0f,
                activePitchClasses = emptySet(),
                chromagram = FloatArray(12),
            )
        }

        val cutoff = threshold * maxEnergy
        val activePitchClasses = mutableSetOf<Int>()
        var activeEnergy = 0f

        for (i in weightedChroma.indices) {
            if (weightedChroma[i] >= cutoff) {
                activePitchClasses.add(i)
                activeEnergy += weightedChroma[i]
            }
        }

        // Step 5: Match against chord formulas (reuses existing ChordDetector)
        val detection = if (activePitchClasses.size >= MIN_PITCH_CLASSES) {
            ChordDetector.detect(activePitchClasses.toList())
        } else {
            ChordDetector.detect(activePitchClasses.toList())
        }

        // Confidence: fraction of total energy captured by the active bins
        val confidence = activeEnergy / weightedChroma.sum().coerceAtLeast(1e-6f)

        return AudioChordResult(
            detection = detection,
            confidence = confidence,
            activePitchClasses = activePitchClasses,
            chromagram = chroma,
        )
    }
}
