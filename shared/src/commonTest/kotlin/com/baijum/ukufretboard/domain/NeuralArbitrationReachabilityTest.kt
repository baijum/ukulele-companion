package com.baijum.ukufretboard.domain

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Reachability tests for the neural arbitrator's YIN-confidence gates.
 *
 * Regression coverage for #603: the "strong disagreement" override once
 * required `yinResult.confidence >= 0.16`, but [PitchDetector] can never emit a
 * confidence that high — [PitchResult.confidence] is the CMND dip value, and by
 * construction it is always strictly below the detector's threshold
 * ([PitchDetector.DEFAULT_THRESHOLD] = 0.15). The gate was therefore dead code.
 *
 * Unlike [NeuralArbitratorTest], these tests never hand-build a [PitchResult].
 * They feed **real synthesised audio** through [PitchDetector.detect] and use
 * the confidence it actually produces, so an out-of-range constant makes the
 * suite fail instead of silently passing.
 */
class NeuralArbitrationReachabilityTest {
    private companion object {
        const val SAMPLE_RATE = 44100
        const val FRAME_SIZE = 4096
        const val TARGET_HZ = 196.0 // G3 — a low string, where octave latching bites.
        const val AMPLITUDE = 0.5f
    }

    /**
     * A sine plus deterministic broadband noise. Increasing [noiseAmplitude]
     * degrades periodicity, which raises the CMND dip value (YIN confidence)
     * toward — but never past — the detector threshold.
     */
    private fun noisySine(
        frequencyHz: Double,
        noiseAmplitude: Float,
        seed: Int,
    ): FloatArray {
        val random = Random(seed)
        val omega = 2.0 * PI * frequencyHz / SAMPLE_RATE
        return FloatArray(FRAME_SIZE) { i ->
            val tone = AMPLITUDE * sin(omega * i).toFloat()
            val noise = (random.nextFloat() * 2f - 1f) * noiseAmplitude
            tone + noise
        }
    }

    /**
     * Detects [TARGET_HZ] across a fine, deterministic sweep of noise levels
     * and returns every real [PitchResult] the detector emitted. The sweep
     * spans clean sine (confidence ≈ 0) up to noise levels that push the CMND
     * dip close to the threshold. Shared by every test so the "reachable"
     * assertion and the override case always agree on the same signals.
     */
    private fun detections(): List<PitchResult> {
        val results = mutableListOf<PitchResult>()
        var noise = 0.0f
        while (noise <= 0.6f) {
            val samples = noisySine(TARGET_HZ, noise, seed = noise.toRawBits())
            PitchDetector.detect(samples, SAMPLE_RATE)?.let { results += it }
            noise += 0.005f
        }
        return results
    }

    private val strongGate: Double
        get() = PitchDetector.DEFAULT_THRESHOLD * NeuralArbitrator.YIN_CONFIDENCE_STRONG_FRACTION

    @Test
    fun realDetectorConfidenceStaysBelowThreshold() {
        val results = detections()
        assertTrue(results.isNotEmpty(), "expected at least one real detection across the sweep")
        // The core structural invariant: confidence is a sub-threshold CMND dip.
        // Any arbitration gate at or above the threshold is therefore dead code.
        results.forEach { r ->
            assertTrue(
                r.confidence >= 0.0 && r.confidence < PitchDetector.DEFAULT_THRESHOLD,
                "real YIN confidence must lie in [0, ${PitchDetector.DEFAULT_THRESHOLD}) but was ${r.confidence}",
            )
        }
    }

    @Test
    fun strongDisagreementGateIsReachableByRealAudio() {
        // A gate expressed as a fraction of the threshold necessarily sits
        // inside the reachable [0, threshold) band. Guard against a future edit
        // that pushes it back out (the #603 regression).
        assertTrue(
            strongGate < PitchDetector.DEFAULT_THRESHOLD,
            "strong-disagreement gate $strongGate must be below the threshold to ever fire",
        )

        val maxConfidence = detections().maxOfOrNull { it.confidence }
        assertNotNull(maxConfidence, "expected at least one real detection")
        assertTrue(
            maxConfidence >= strongGate,
            "real audio should be able to reach the strong-disagreement gate " +
                "($strongGate) but the highest real confidence was $maxConfidence",
        )
    }

    @Test
    fun realAudioDrivesStrongDisagreementOverride() {
        // The highest-confidence real detection clears the strong-disagreement
        // gate. With the buggy 0.16 constant no real signal could, and this
        // search would come up empty — exactly the signal #603 asks for.
        val yin = detections().filter { it.confidence >= strongGate }.maxByOrNull { it.confidence }
        assertNotNull(yin, "no real detection reached the strong-disagreement gate $strongGate")

        // Neural reports a fundamental ~5 semitones below the YIN pitch — a
        // strong, non-octave disagreement — with high confidence.
        val neuralHz = yin.frequencyHz / 2.0.pow(5.0 / 12.0)
        val neural = NeuralPitchResult(frequencyHz = neuralHz, confidence = 0.95)

        val arb = NeuralArbitrator()
        // Prime the consistency window so the override is eligible.
        repeat(NeuralArbitrator.CONSISTENCY_FRAMES + 1) {
            advanceToInferenceFrame(arb)
            arb.onInferenceResult(neural)
        }

        val decision = arb.arbitrate(yin, neural)
        assertTrue(
            decision.source == PitchSource.NEURAL && decision.reason == "strong_disagreement",
            "expected neural strong-disagreement override, got ${decision.source}/${decision.reason}",
        )
        assertTrue(decision.frequencyHz == neuralHz, "override should adopt the neural frequency")
    }

    private fun advanceToInferenceFrame(arb: NeuralArbitrator) {
        while (!arb.shouldRunInference()) {
            // advance frame counter to the next inference tick
        }
    }
}
