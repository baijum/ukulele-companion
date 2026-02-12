package com.baijum.ukufretboard.domain

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import kotlin.math.max

data class NeuralPitchResult(
    val frequencyHz: Double,
    val confidence: Double,
)

/**
 * Runs intermittent ONNX inference for neural pitch supervision.
 *
 * This class is intentionally tolerant: if model output parsing fails or the
 * model produces unexpected tensor shapes, it returns null and the caller keeps
 * using the existing YIN-only pipeline.
 */
class NeuralPitchSupervisor(context: Context) : AutoCloseable {
    companion object {
        private const val MODEL_ASSET = "swift_f0_model.onnx"
        private const val MIN_FREQ_HZ = 46.875
        private const val MAX_FREQ_HZ = 2093.75
        private const val MIN_CONFIDENCE = 0.50
    }

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val inputName: String
    private var lastInferenceNanos: Long = 0L

    init {
        val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
        val options = OrtSession.SessionOptions().apply {
            setInterOpNumThreads(1)
            setIntraOpNumThreads(1)
        }
        session = env.createSession(bytes, options)
        inputName = session.inputNames.first()
    }

    /**
     * Approximate inference duration of the last successful run.
     */
    fun lastInferenceMs(): Double = lastInferenceNanos / 1_000_000.0

    fun estimate(samples44k: FloatArray): NeuralPitchResult? {
        val samples16k = AudioResampler.downsample44kTo16k(samples44k)
        if (samples16k.isEmpty()) return null

        val shape = longArrayOf(1L, samples16k.size.toLong())
        val start = System.nanoTime()

        try {
            OnnxTensor.createTensor(env, FloatBuffer.wrap(samples16k), shape).use { inputTensor ->
                session.run(mapOf(inputName to inputTensor)).use { outputs ->
                    lastInferenceNanos = System.nanoTime() - start
                    return extractMiddleEstimate(outputs)
                }
            }
        } catch (_: OrtException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        } catch (_: ClassCastException) {
            return null
        }
    }

    private fun extractMiddleEstimate(outputs: OrtSession.Result): NeuralPitchResult? {
        if (outputs.size() == 0) return null

        val pitchArray = extractFloatSeries(outputs[0].value) ?: return null
        if (pitchArray.isEmpty()) return null

        val confidenceArray = if (outputs.size() >= 2) {
            extractFloatSeries(outputs[1].value)
        } else {
            null
        }

        val idx = pitchArray.size / 2
        val freq = pitchArray[idx].toDouble()
        val conf = confidenceArray?.getOrNull(max(0, idx.coerceAtMost(confidenceArray.lastIndex)))?.toDouble() ?: 0.0

        if (freq !in MIN_FREQ_HZ..MAX_FREQ_HZ) return null
        if (conf < MIN_CONFIDENCE) return null

        return NeuralPitchResult(frequencyHz = freq, confidence = conf)
    }

    private fun extractFloatSeries(value: Any?): FloatArray? {
        return when (value) {
            is FloatArray -> value
            is Array<*> -> flattenArrayValue(value)
            else -> null
        }
    }

    private fun flattenArrayValue(array: Array<*>): FloatArray? {
        if (array.isEmpty()) return FloatArray(0)
        val first = array[0]
        return when (first) {
            is FloatArray -> {
                val total = array.filterIsInstance<FloatArray>().sumOf { it.size }
                val out = FloatArray(total)
                var pos = 0
                array.filterIsInstance<FloatArray>().forEach { row ->
                    row.copyInto(out, pos)
                    pos += row.size
                }
                out
            }
            is Array<*> -> {
                val list = mutableListOf<Float>()
                array.filterIsInstance<Array<*>>().forEach { nested ->
                    nested.filterIsInstance<Float>().forEach { list.add(it) }
                    nested.filterIsInstance<Double>().forEach { list.add(it.toFloat()) }
                }
                list.toFloatArray()
            }
            is Number -> array.mapNotNull { (it as? Number)?.toFloat() }.toFloatArray()
            else -> null
        }
    }

    override fun close() {
        session.close()
    }
}
