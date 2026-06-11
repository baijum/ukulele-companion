package com.baijum.ukufretboard.domain

import kotlin.math.abs
import kotlin.math.log2

/**
 * Result of the YIN-vs-neural pitch arbitration.
 *
 * [frequencyHz] and [confidence] represent the final pitch to use.
 * When neural overrides YIN, [frequencyHz] comes from neural while
 * [confidence] is always the YIN confidence (since YIN confidence is
 * a well-calibrated quality metric).
 */
data class ArbitrationResult(
    val frequencyHz: Double,
    val confidence: Double,
    val source: PitchSource,
    val reason: String,
) {
    val overrideApplied: Boolean get() = source == PitchSource.NEURAL
}

enum class PitchSource { YIN, NEURAL }

/**
 * Encapsulates the neural-vs-YIN pitch arbitration state machine.
 *
 * Both the Android and iOS tuners run a primary YIN pitch detector on
 * every frame and a heavier neural (ONNX) estimator at a lower cadence.
 * This class owns the frame-interval gating, result-TTL aging, failure
 * tracking, consistency window, and the final arbitration decision. It
 * contains **no platform dependencies** — audio capture, ONNX calls,
 * and UI updates remain in the platform ViewModels.
 *
 * Typical per-frame flow:
 * ```
 * if (arbitrator.shouldRunInference()) {
 *     val estimate = supervisor.estimate(samples)   // platform
 *     if (estimate != null) arbitrator.onInferenceResult(estimate)
 *     else arbitrator.onInferenceFailure()
 * }
 * val neuralResult = arbitrator.currentResult()
 * val decision = arbitrator.arbitrate(yinResult, neuralResult)
 * ```
 */
class NeuralArbitrator {

    companion object {
        /** Run neural inference every N audio frames. */
        const val SUPERVISOR_INTERVAL = 5

        /** Neural result expires after this many frames without refresh. */
        const val RESULT_TTL_FRAMES = 10

        /** Consecutive inference failures before disabling neural. */
        const val FAILURE_THRESHOLD = 15

        /** Ignore YIN-vs-neural gaps smaller than this (semitones). */
        const val ARBITRATION_IGNORE_SEMITONES = 1.5

        /** Strong-disagreement threshold for non-octave correction (semitones). */
        const val ARBITRATION_STRONG_SEMITONES = 2.5

        /** Required consecutive similar neural readings before override. */
        const val CONSISTENCY_FRAMES = 2

        fun semitoneDistance(aHz: Double, bHz: Double): Double {
            if (aHz <= 0.0 || bHz <= 0.0) return Double.MAX_VALUE
            return abs(12.0 * log2(aHz / bHz))
        }

        fun isOctaveRelation(aHz: Double, bHz: Double): Boolean {
            if (aHz <= 0.0 || bHz <= 0.0) return false
            val semitones = semitoneDistance(aHz, bHz)
            return abs(semitones - 12.0) <= 1.0 || abs(semitones - 24.0) <= 1.0
        }
    }

    // --- State ---------------------------------------------------------------

    private var frameCounter = 0
    private var lastResult: NeuralPitchResult? = null
    private var resultAgeFrames = Int.MAX_VALUE
    private var consecutiveFailures = 0
    private var lastFrequencyForConsistency: Double? = null
    private var consistencyFrames = 0

    /** True when [FAILURE_THRESHOLD] consecutive failures have occurred. */
    val isDisabledByFailures: Boolean
        get() = consecutiveFailures >= FAILURE_THRESHOLD

    // --- Frame-interval gating -----------------------------------------------

    /**
     * Advances the frame counter and returns `true` every [SUPERVISOR_INTERVAL]
     * frames, signalling the caller to run neural inference.
     *
     * On non-inference frames the neural result TTL is aged automatically.
     */
    fun shouldRunInference(): Boolean {
        frameCounter++
        if (frameCounter % SUPERVISOR_INTERVAL == 0) {
            return true
        }
        if (resultAgeFrames < Int.MAX_VALUE) {
            resultAgeFrames++
        }
        return false
    }

    // --- Inference result callbacks ------------------------------------------

    /**
     * Records a successful neural inference result.
     * Resets the failure counter and updates the consistency window.
     */
    fun onInferenceResult(result: NeuralPitchResult) {
        lastResult = result
        resultAgeFrames = 0
        consecutiveFailures = 0
        updateConsistency(result.frequencyHz)
    }

    /**
     * Records a failed neural inference (supervisor returned null).
     * Resets the consistency window.
     *
     * @return `true` if the failure threshold has been reached and neural
     *         should be marked as disabled/fallback.
     */
    fun onInferenceFailure(): Boolean {
        lastResult = null
        resultAgeFrames = 0
        consecutiveFailures++
        consistencyFrames = 0
        lastFrequencyForConsistency = null
        return isDisabledByFailures
    }

    // --- Result access -------------------------------------------------------

    /**
     * Returns the most recent neural result if it is still within the
     * [RESULT_TTL_FRAMES] window, or `null` if stale / unavailable.
     */
    fun currentResult(): NeuralPitchResult? {
        return if (resultAgeFrames <= RESULT_TTL_FRAMES) lastResult else null
    }

    // --- Arbitration ---------------------------------------------------------

    /**
     * Decides the final pitch from the YIN result and an optional neural result.
     *
     * Decision cascade:
     * 1. No neural result → use YIN.
     * 2. Gap ≤ [ARBITRATION_IGNORE_SEMITONES] → use YIN (close enough).
     * 3. Neural not yet consistent → use YIN.
     * 4. Octave relation with neural confidence ≥ 0.85 → correct to neural Hz.
     * 5. Strong disagreement (≥ [ARBITRATION_STRONG_SEMITONES]) with neural
     *    confidence ≥ 0.93 → override with neural Hz.
     * 6. Otherwise → use YIN.
     */
    fun arbitrate(
        yinResult: PitchResult,
        neuralResult: NeuralPitchResult?,
    ): ArbitrationResult {
        if (neuralResult == null) {
            return ArbitrationResult(
                frequencyHz = yinResult.frequencyHz,
                confidence = yinResult.confidence,
                source = PitchSource.YIN,
                reason = "no_neural",
            )
        }

        val semitoneGap = semitoneDistance(yinResult.frequencyHz, neuralResult.frequencyHz)
        if (semitoneGap <= ARBITRATION_IGNORE_SEMITONES) {
            return ArbitrationResult(
                frequencyHz = yinResult.frequencyHz,
                confidence = yinResult.confidence,
                source = PitchSource.YIN,
                reason = "small_gap",
            )
        }

        if (consistencyFrames < CONSISTENCY_FRAMES) {
            return ArbitrationResult(
                frequencyHz = yinResult.frequencyHz,
                confidence = yinResult.confidence,
                source = PitchSource.YIN,
                reason = "neural_inconsistent",
            )
        }

        if (isOctaveRelation(yinResult.frequencyHz, neuralResult.frequencyHz) &&
            neuralResult.confidence >= 0.85 &&
            yinResult.confidence >= 0.12
        ) {
            return ArbitrationResult(
                frequencyHz = neuralResult.frequencyHz,
                confidence = yinResult.confidence,
                source = PitchSource.NEURAL,
                reason = "octave_correction",
            )
        }

        return if (semitoneGap >= ARBITRATION_STRONG_SEMITONES &&
            neuralResult.confidence >= 0.93 &&
            yinResult.confidence >= 0.16
        ) {
            ArbitrationResult(
                frequencyHz = neuralResult.frequencyHz,
                confidence = yinResult.confidence,
                source = PitchSource.NEURAL,
                reason = "strong_disagreement",
            )
        } else {
            ArbitrationResult(
                frequencyHz = yinResult.frequencyHz,
                confidence = yinResult.confidence,
                source = PitchSource.YIN,
                reason = "keep_yin",
            )
        }
    }

    // --- Reset ---------------------------------------------------------------

    /** Resets all state. Call when the tuner starts or restarts. */
    fun reset() {
        frameCounter = 0
        lastResult = null
        resultAgeFrames = Int.MAX_VALUE
        consecutiveFailures = 0
        lastFrequencyForConsistency = null
        consistencyFrames = 0
    }

    // --- Internal ------------------------------------------------------------

    private fun updateConsistency(neuralFrequencyHz: Double) {
        if (neuralFrequencyHz <= 0.0) {
            consistencyFrames = 0
            lastFrequencyForConsistency = null
            return
        }

        val previous = lastFrequencyForConsistency
        consistencyFrames = if (previous != null &&
            semitoneDistance(previous, neuralFrequencyHz) <= 0.5
        ) {
            consistencyFrames + 1
        } else {
            1
        }
        lastFrequencyForConsistency = neuralFrequencyHz
    }
}
