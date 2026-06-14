package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.platform.PlatformLock
import com.baijum.ukufretboard.platform.withLock
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
    private const val DEFAULT_SAMPLE_RATE = 44100

    private const val MIN_PITCH_CLASSES = 3

    private val lock = PlatformLock()
    private var cachedFftSize = 0
    private var cachedReal = FloatArray(0)
    private var cachedImag = FloatArray(0)
    private var cachedMagnitudes = FloatArray(0)
    private var cachedChroma = FloatArray(12)
    private var cachedWeightedChroma = FloatArray(12)

    private fun ensureBuffers(fftSize: Int) {
        if (fftSize != cachedFftSize) {
            cachedFftSize = fftSize
            cachedReal = FloatArray(fftSize)
            cachedImag = FloatArray(fftSize)
            cachedMagnitudes = FloatArray(fftSize / 2)
        } else {
            cachedImag.fill(0f)
        }
    }

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
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        threshold: Float = DEFAULT_THRESHOLD,
        preferredRootPitchClass: Int? = null,
        preferredRootWeight: Float = 1.15f,
    ): AudioChordResult =
        lock.withLock {
            ensureBuffers(samples.size)

            // Step 1: Windowed FFT — write Hanning window directly into cachedReal
            FFTProcessor.hanningWindowInto(samples, cachedReal)
            FFTProcessor.fft(cachedReal, cachedImag)

            // Step 2: Magnitude spectrum into pre-allocated buffer
            FFTProcessor.magnitudeSpectrumInto(cachedReal, cachedImag, cachedMagnitudes)

            // Step 3: Chromagram
            Chromagram.computeInto(
                magnitudes = cachedMagnitudes,
                sampleRate = sampleRate,
                fftSize = samples.size,
                out = cachedChroma,
            )

            // Optional guidance: bias the chroma toward a stable root hint
            cachedChroma.copyInto(cachedWeightedChroma)
            if (preferredRootPitchClass != null && preferredRootPitchClass in 0..11) {
                cachedWeightedChroma[preferredRootPitchClass] =
                    cachedWeightedChroma[preferredRootPitchClass] * max(1.0f, preferredRootWeight)
            }

            // Step 4: Threshold — find active pitch classes
            val maxEnergy = cachedWeightedChroma.max()
            if (maxEnergy <= 0f) {
                return@withLock AudioChordResult(
                    detection = ChordDetector.DetectionResult.NoSelection,
                    confidence = 0f,
                    activePitchClasses = emptySet(),
                    chromagram = cachedChroma.copyOf(),
                )
            }

            val cutoff = threshold * maxEnergy
            val activePitchClasses = mutableSetOf<Int>()
            var activeEnergy = 0f

            for (i in cachedWeightedChroma.indices) {
                if (cachedWeightedChroma[i] >= cutoff) {
                    activePitchClasses.add(i)
                    activeEnergy += cachedWeightedChroma[i]
                }
            }

            // Step 5: Match against chord formulas (reuses existing ChordDetector)
            val detection =
                if (activePitchClasses.size >= MIN_PITCH_CLASSES) {
                    ChordDetector.detect(activePitchClasses.toList())
                } else {
                    ChordDetector.DetectionResult.NoSelection
                }

            val confidence = activeEnergy / cachedWeightedChroma.sum().coerceAtLeast(1e-6f)

            AudioChordResult(
                detection = detection,
                confidence = confidence,
                activePitchClasses = activePitchClasses,
                chromagram = cachedChroma.copyOf(),
            )
        }
}
