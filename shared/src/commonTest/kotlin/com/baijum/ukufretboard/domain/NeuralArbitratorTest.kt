package com.baijum.ukufretboard.domain

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NeuralArbitratorTest {

    // ── shouldRunInference (frame-interval gating) ──────────────────

    @Test
    fun shouldRunInferenceReturnsTrueEveryIntervalFrames() {
        val arb = NeuralArbitrator()
        val results = (1..20).map { arb.shouldRunInference() }
        val trueIndices = results.mapIndexedNotNull { i, v -> if (v) i else null }
        assertEquals(
            listOf(4, 9, 14, 19),
            trueIndices,
            "shouldRunInference should be true at frames 5, 10, 15, 20 (0-indexed 4,9,14,19)",
        )
    }

    @Test
    fun shouldRunInferenceFirstFrameIsNotInference() {
        val arb = NeuralArbitrator()
        assertFalse(arb.shouldRunInference(), "frame 1 should not trigger inference")
    }

    // ── Result TTL aging ────────────────────────────────────────────

    @Test
    fun currentResultReturnsNullInitially() {
        val arb = NeuralArbitrator()
        assertNull(arb.currentResult())
    }

    @Test
    fun currentResultReturnsResultWithinTTL() {
        val arb = NeuralArbitrator()
        val result = NeuralPitchResult(frequencyHz = 440.0, confidence = 0.9)

        advanceToInferenceFrame(arb)
        arb.onInferenceResult(result)

        assertNotNull(arb.currentResult())
        assertEquals(440.0, arb.currentResult()!!.frequencyHz)
    }

    @Test
    fun currentResultExpiresAfterTTLFrames() {
        val arb = NeuralArbitrator()
        val result = NeuralPitchResult(frequencyHz = 440.0, confidence = 0.9)

        advanceToInferenceFrame(arb)
        arb.onInferenceResult(result)

        // Only non-inference frames age the result. Advance enough frames
        // (skipping inference frames that return true) to exceed the TTL.
        var aged = 0
        while (aged <= NeuralArbitrator.RESULT_TTL_FRAMES) {
            assertNotNull(arb.currentResult(), "result should still be valid at age $aged")
            val isInference = arb.shouldRunInference()
            if (!isInference) aged++
        }
        assertNull(arb.currentResult(), "result should be expired after TTL")
    }

    // ── Failure threshold ───────────────────────────────────────────

    @Test
    fun isDisabledByFailuresAfterThreshold() {
        val arb = NeuralArbitrator()
        assertFalse(arb.isDisabledByFailures)

        repeat(NeuralArbitrator.FAILURE_THRESHOLD - 1) {
            arb.onInferenceFailure()
            assertFalse(arb.isDisabledByFailures, "should not be disabled after ${it + 1} failures")
        }

        arb.onInferenceFailure()
        assertTrue(arb.isDisabledByFailures, "should be disabled after ${NeuralArbitrator.FAILURE_THRESHOLD} failures")
    }

    @Test
    fun successfulInferenceResetsFailureCount() {
        val arb = NeuralArbitrator()
        repeat(NeuralArbitrator.FAILURE_THRESHOLD - 1) { arb.onInferenceFailure() }
        assertFalse(arb.isDisabledByFailures)

        arb.onInferenceResult(NeuralPitchResult(frequencyHz = 440.0, confidence = 0.9))
        assertFalse(arb.isDisabledByFailures)

        repeat(NeuralArbitrator.FAILURE_THRESHOLD - 1) { arb.onInferenceFailure() }
        assertFalse(arb.isDisabledByFailures, "counter should have been reset by successful result")
    }

    @Test
    fun onInferenceFailureReturnsTrueAtThreshold() {
        val arb = NeuralArbitrator()
        repeat(NeuralArbitrator.FAILURE_THRESHOLD - 1) {
            assertFalse(arb.onInferenceFailure())
        }
        assertTrue(arb.onInferenceFailure())
    }

    // ── Consistency window ──────────────────────────────────────────

    @Test
    fun arbitrateRejectsNeuralBeforeConsistencyWindow() {
        val arb = NeuralArbitrator()
        val yin = PitchResult(frequencyHz = 440.0, confidence = 0.15)
        val neural = NeuralPitchResult(frequencyHz = 660.0, confidence = 0.95)

        advanceToInferenceFrame(arb)
        arb.onInferenceResult(neural)

        val decision = arb.arbitrate(yin, neural)
        assertEquals(PitchSource.YIN, decision.source)
        assertEquals("neural_inconsistent", decision.reason)
    }

    @Test
    fun arbitrateAcceptsNeuralAfterConsistencyWindow() {
        val arb = NeuralArbitrator()
        val yin = PitchResult(frequencyHz = 440.0, confidence = 0.2)
        val neural = NeuralPitchResult(frequencyHz = 660.0, confidence = 0.95)

        repeat(NeuralArbitrator.CONSISTENCY_FRAMES) {
            advanceToInferenceFrame(arb)
            arb.onInferenceResult(neural)
        }

        val decision = arb.arbitrate(yin, neural)
        assertEquals(PitchSource.NEURAL, decision.source)
        assertEquals("strong_disagreement", decision.reason)
    }

    @Test
    fun consistencyResetsByDivergentReading() {
        val arb = NeuralArbitrator()
        val yin = PitchResult(frequencyHz = 440.0, confidence = 0.2)
        val neural1 = NeuralPitchResult(frequencyHz = 660.0, confidence = 0.95)
        val neural2 = NeuralPitchResult(frequencyHz = 330.0, confidence = 0.95)

        advanceToInferenceFrame(arb)
        arb.onInferenceResult(neural1)

        advanceToInferenceFrame(arb)
        arb.onInferenceResult(neural2)

        val decision = arb.arbitrate(yin, neural2)
        assertEquals(PitchSource.YIN, decision.source, "divergent reading should reset consistency")
    }

    // ── Octave correction ───────────────────────────────────────────

    @Test
    fun arbitrateAppliesOctaveCorrectionWhenConfident() {
        val arb = NeuralArbitrator()
        val yin = PitchResult(frequencyHz = 440.0, confidence = 0.15)
        val neural = NeuralPitchResult(frequencyHz = 880.0, confidence = 0.90)

        buildConsistency(arb, neural)

        val decision = arb.arbitrate(yin, neural)
        assertEquals(PitchSource.NEURAL, decision.source)
        assertEquals("octave_correction", decision.reason)
        assertEquals(880.0, decision.frequencyHz)
        assertEquals(0.15, decision.confidence, "confidence should come from YIN")
    }

    @Test
    fun arbitrateSkipsOctaveCorrectionWhenNeuralConfidenceLow() {
        val arb = NeuralArbitrator()
        val yin = PitchResult(frequencyHz = 440.0, confidence = 0.15)
        val neural = NeuralPitchResult(frequencyHz = 880.0, confidence = 0.80)

        buildConsistency(arb, neural)

        val decision = arb.arbitrate(yin, neural)
        assertEquals(PitchSource.YIN, decision.source, "neural conf 0.80 < 0.85 threshold")
    }

    @Test
    fun arbitrateSkipsOctaveCorrectionWhenYinConfidenceTooLow() {
        val arb = NeuralArbitrator()
        val yin = PitchResult(frequencyHz = 440.0, confidence = 0.10)
        val neural = NeuralPitchResult(frequencyHz = 880.0, confidence = 0.90)

        buildConsistency(arb, neural)

        val decision = arb.arbitrate(yin, neural)
        assertEquals(PitchSource.YIN, decision.source, "yin conf 0.10 < 0.12 threshold")
    }

    @Test
    fun arbitrateHandlesDoubleOctave() {
        val arb = NeuralArbitrator()
        val yin = PitchResult(frequencyHz = 220.0, confidence = 0.15)
        val neural = NeuralPitchResult(frequencyHz = 880.0, confidence = 0.90)

        buildConsistency(arb, neural)

        val decision = arb.arbitrate(yin, neural)
        assertEquals("octave_correction", decision.reason)
        assertEquals(880.0, decision.frequencyHz)
    }

    // ── Strong disagreement override ────────────────────────────────

    @Test
    fun arbitrateOverridesOnStrongDisagreement() {
        val arb = NeuralArbitrator()
        val yin = PitchResult(frequencyHz = 440.0, confidence = 0.2)
        val neural = NeuralPitchResult(frequencyHz = 660.0, confidence = 0.95)

        buildConsistency(arb, neural)

        val decision = arb.arbitrate(yin, neural)
        assertEquals(PitchSource.NEURAL, decision.source)
        assertEquals("strong_disagreement", decision.reason)
        assertEquals(660.0, decision.frequencyHz)
        assertEquals(0.2, decision.confidence, "confidence should come from YIN")
    }

    @Test
    fun arbitrateKeepsYinWhenNeuralConfidenceBelowStrongThreshold() {
        val arb = NeuralArbitrator()
        val yin = PitchResult(frequencyHz = 440.0, confidence = 0.2)
        val neural = NeuralPitchResult(frequencyHz = 660.0, confidence = 0.90)

        buildConsistency(arb, neural)

        val decision = arb.arbitrate(yin, neural)
        assertEquals(PitchSource.YIN, decision.source, "neural conf 0.90 < 0.93 threshold")
        assertEquals("keep_yin", decision.reason)
    }

    @Test
    fun arbitrateKeepsYinWhenYinConfidenceBelowStrongThreshold() {
        val arb = NeuralArbitrator()
        val yin = PitchResult(frequencyHz = 440.0, confidence = 0.14)
        val neural = NeuralPitchResult(frequencyHz = 660.0, confidence = 0.95)

        buildConsistency(arb, neural)

        val decision = arb.arbitrate(yin, neural)
        assertEquals(PitchSource.YIN, decision.source, "yin conf 0.14 < 0.16 threshold")
    }

    // ── Small gap (no override needed) ──────────────────────────────

    @Test
    fun arbitrateKeepsYinWhenGapIsSmall() {
        val arb = NeuralArbitrator()
        val yin = PitchResult(frequencyHz = 440.0, confidence = 0.15)
        val detuned = 440.0 * 2.0.pow(1.0 / 12.0)
        val neural = NeuralPitchResult(frequencyHz = detuned, confidence = 0.95)

        buildConsistency(arb, neural)

        val decision = arb.arbitrate(yin, neural)
        assertEquals(PitchSource.YIN, decision.source)
        assertEquals("small_gap", decision.reason)
    }

    // ── No neural result ────────────────────────────────────────────

    @Test
    fun arbitrateReturnsYinWhenNoNeural() {
        val arb = NeuralArbitrator()
        val yin = PitchResult(frequencyHz = 440.0, confidence = 0.15)

        val decision = arb.arbitrate(yin, null)
        assertEquals(PitchSource.YIN, decision.source)
        assertEquals("no_neural", decision.reason)
        assertEquals(440.0, decision.frequencyHz)
        assertFalse(decision.overrideApplied)
    }

    // ── Reset ────────────────────────────────────────────────────────

    @Test
    fun resetClearsAllState() {
        val arb = NeuralArbitrator()
        val neural = NeuralPitchResult(frequencyHz = 440.0, confidence = 0.9)

        repeat(10) { arb.shouldRunInference() }
        advanceToInferenceFrame(arb)
        arb.onInferenceResult(neural)
        repeat(5) { arb.onInferenceFailure() }

        arb.reset()

        assertNull(arb.currentResult(), "result should be null after reset")
        assertFalse(arb.isDisabledByFailures, "failures should be cleared after reset")

        val firstInference = (1..NeuralArbitrator.SUPERVISOR_INTERVAL).map {
            arb.shouldRunInference()
        }
        assertEquals(
            NeuralArbitrator.SUPERVISOR_INTERVAL - 1,
            firstInference.count { !it },
            "frame counter should restart from 0 after reset",
        )
    }

    // ── semitoneDistance ─────────────────────────────────────────────

    @Test
    fun semitoneDistanceUnisonIsZero() {
        assertEquals(0.0, NeuralArbitrator.semitoneDistance(440.0, 440.0), 0.001)
    }

    @Test
    fun semitoneDistanceOctaveIsTwelve() {
        assertEquals(12.0, NeuralArbitrator.semitoneDistance(440.0, 880.0), 0.01)
        assertEquals(12.0, NeuralArbitrator.semitoneDistance(880.0, 440.0), 0.01)
    }

    @Test
    fun semitoneDistanceTwoOctavesIsTwentyFour() {
        assertEquals(24.0, NeuralArbitrator.semitoneDistance(220.0, 880.0), 0.01)
    }

    @Test
    fun semitoneDistancePerfectFifthIsSeven() {
        val a4 = 440.0
        val e5 = a4 * 2.0.pow(7.0 / 12.0)
        assertEquals(7.0, NeuralArbitrator.semitoneDistance(a4, e5), 0.01)
    }

    @Test
    fun semitoneDistanceZeroOrNegativeReturnsMaxValue() {
        assertEquals(Double.MAX_VALUE, NeuralArbitrator.semitoneDistance(0.0, 440.0), 0.0)
        assertEquals(Double.MAX_VALUE, NeuralArbitrator.semitoneDistance(440.0, 0.0), 0.0)
        assertEquals(Double.MAX_VALUE, NeuralArbitrator.semitoneDistance(-1.0, 440.0), 0.0)
    }

    @Test
    fun semitoneDistanceIsSymmetric() {
        val d1 = NeuralArbitrator.semitoneDistance(440.0, 523.25)
        val d2 = NeuralArbitrator.semitoneDistance(523.25, 440.0)
        assertEquals(d1, d2, 0.001)
    }

    // ── isOctaveRelation ────────────────────────────────────────────

    @Test
    fun isOctaveRelationTrueForExactOctave() {
        assertTrue(NeuralArbitrator.isOctaveRelation(440.0, 880.0))
        assertTrue(NeuralArbitrator.isOctaveRelation(880.0, 440.0))
    }

    @Test
    fun isOctaveRelationTrueForDoubleOctave() {
        assertTrue(NeuralArbitrator.isOctaveRelation(220.0, 880.0))
        assertTrue(NeuralArbitrator.isOctaveRelation(880.0, 220.0))
    }

    @Test
    fun isOctaveRelationTrueWithSlightDetuning() {
        val slightlySharp = 880.0 * 2.0.pow(0.8 / 12.0)
        assertTrue(NeuralArbitrator.isOctaveRelation(440.0, slightlySharp))
    }

    @Test
    fun isOctaveRelationFalseForUnison() {
        assertFalse(NeuralArbitrator.isOctaveRelation(440.0, 440.0))
    }

    @Test
    fun isOctaveRelationFalseForFifth() {
        val e5 = 440.0 * 2.0.pow(7.0 / 12.0)
        assertFalse(NeuralArbitrator.isOctaveRelation(440.0, e5))
    }

    @Test
    fun isOctaveRelationFalseForZeroOrNegative() {
        assertFalse(NeuralArbitrator.isOctaveRelation(0.0, 440.0))
        assertFalse(NeuralArbitrator.isOctaveRelation(440.0, -1.0))
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun advanceToInferenceFrame(arb: NeuralArbitrator) {
        while (!arb.shouldRunInference()) { /* advance */ }
    }

    private fun buildConsistency(arb: NeuralArbitrator, neural: NeuralPitchResult) {
        repeat(NeuralArbitrator.CONSISTENCY_FRAMES) {
            advanceToInferenceFrame(arb)
            arb.onInferenceResult(neural)
        }
    }
}
