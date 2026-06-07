package com.baijum.ukufretboard.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for the tensor parsing and middle-estimate extraction logic in
 * [NeuralPitchSupervisor]. These exercise the companion functions that
 * convert raw ONNX output values into [NeuralPitchResult].
 */
class NeuralPitchSupervisorTest {

    // ── extractFloatSeries ───────────────────────────────────────────

    @Test
    fun extractFloatSeriesFromDirectFloatArray() {
        val input = floatArrayOf(440f, 441f, 442f)
        val result = NeuralPitchSupervisor.extractFloatSeries(input)
        assertArrayEquals(input, result, 0.001f)
    }

    @Test
    fun extractFloatSeriesFromNestedFloatArrays() {
        val input: Array<Any> = arrayOf(
            floatArrayOf(100f, 200f),
            floatArrayOf(300f),
        )
        val result = NeuralPitchSupervisor.extractFloatSeries(input)
        assertNotNull(result)
        assertArrayEquals(floatArrayOf(100f, 200f, 300f), result!!, 0.001f)
    }

    @Test
    fun extractFloatSeriesFromNumberArray() {
        @Suppress("USELESS_CAST")
        val input: Array<Any> = arrayOf(440f as Number, 441f as Number, 442f as Number)
        val result = NeuralPitchSupervisor.extractFloatSeries(input)
        assertNotNull(result)
        assertArrayEquals(floatArrayOf(440f, 441f, 442f), result!!, 0.001f)
    }

    @Test
    fun extractFloatSeriesReturnsNullForUnsupportedType() {
        assertNull(NeuralPitchSupervisor.extractFloatSeries("not a tensor"))
        assertNull(NeuralPitchSupervisor.extractFloatSeries(42))
        assertNull(NeuralPitchSupervisor.extractFloatSeries(null))
    }

    @Test
    fun extractFloatSeriesEmptyArrayReturnsEmpty() {
        val input: Array<Any> = emptyArray()
        val result = NeuralPitchSupervisor.extractFloatSeries(input)
        assertNotNull(result)
        assertEquals(0, result!!.size)
    }

    // ── flattenArrayValue ────────────────────────────────────────────

    @Test
    fun flattenArrayValueDoublyNestedWithDoubles() {
        val inner1: Array<Any> = arrayOf(440.0, 441.0)
        val inner2: Array<Any> = arrayOf(442.0)
        val input: Array<Any> = arrayOf(inner1, inner2)
        val result = NeuralPitchSupervisor.flattenArrayValue(input)
        assertNotNull(result)
        assertEquals(3, result!!.size)
        assertEquals(440f, result[0], 0.01f)
        assertEquals(441f, result[1], 0.01f)
        assertEquals(442f, result[2], 0.01f)
    }

    @Test
    fun flattenArrayValueReturnsNullForUnknownElementType() {
        val input: Array<Any> = arrayOf("string1", "string2")
        assertNull(NeuralPitchSupervisor.flattenArrayValue(input))
    }

    // ── parseMiddleEstimate ──────────────────────────────────────────

    @Test
    fun parseMiddleEstimatePicksMiddleElement() {
        val pitches = floatArrayOf(200f, 440f, 600f)
        val confidences = floatArrayOf(0.7f, 0.95f, 0.8f)
        val result = NeuralPitchSupervisor.parseMiddleEstimate(pitches, confidences)
        assertNotNull(result)
        assertEquals(440.0, result!!.frequencyHz, 0.01)
        assertEquals(0.95, result.confidence, 0.01)
    }

    @Test
    fun parseMiddleEstimateReturnsNullForEmptyPitch() {
        assertNull(NeuralPitchSupervisor.parseMiddleEstimate(FloatArray(0), null))
    }

    @Test
    fun parseMiddleEstimateRejectsFrequencyBelowRange() {
        val pitches = floatArrayOf(10f)
        val confidences = floatArrayOf(0.9f)
        assertNull(NeuralPitchSupervisor.parseMiddleEstimate(pitches, confidences))
    }

    @Test
    fun parseMiddleEstimateRejectsFrequencyAboveRange() {
        val pitches = floatArrayOf(3000f)
        val confidences = floatArrayOf(0.9f)
        assertNull(NeuralPitchSupervisor.parseMiddleEstimate(pitches, confidences))
    }

    @Test
    fun parseMiddleEstimateRejectsLowConfidence() {
        val pitches = floatArrayOf(440f)
        val confidences = floatArrayOf(0.3f)
        assertNull(NeuralPitchSupervisor.parseMiddleEstimate(pitches, confidences))
    }

    @Test
    fun parseMiddleEstimateReturnsNullWhenConfidenceArrayMissing() {
        val pitches = floatArrayOf(440f)
        val result = NeuralPitchSupervisor.parseMiddleEstimate(pitches, null)
        assertNull("0.0 default confidence is below MIN_CONFIDENCE", result)
    }

    @Test
    fun parseMiddleEstimateAcceptsBoundaryFrequencies() {
        val lowBound = NeuralPitchSupervisor.MIN_FREQ_HZ.toFloat()
        val highBound = NeuralPitchSupervisor.MAX_FREQ_HZ.toFloat()
        val conf = floatArrayOf(0.9f)

        val lowResult = NeuralPitchSupervisor.parseMiddleEstimate(floatArrayOf(lowBound), conf)
        assertNotNull("MIN_FREQ_HZ should be accepted", lowResult)
        assertEquals(lowBound.toDouble(), lowResult!!.frequencyHz, 0.01)

        val highResult = NeuralPitchSupervisor.parseMiddleEstimate(floatArrayOf(highBound), conf)
        assertNotNull("MAX_FREQ_HZ should be accepted", highResult)
        assertEquals(highBound.toDouble(), highResult!!.frequencyHz, 0.01)
    }

    @Test
    fun parseMiddleEstimateHandlesMismatchedArrayLengths() {
        val pitches = floatArrayOf(200f, 440f, 600f, 800f, 1000f)
        val confidences = floatArrayOf(0.9f, 0.95f)
        val result = NeuralPitchSupervisor.parseMiddleEstimate(pitches, confidences)
        assertNotNull(result)
        assertEquals(600.0, result!!.frequencyHz, 0.01)
        assertEquals(0.95, result.confidence, 0.01)
    }
}
