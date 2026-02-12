package com.baijum.ukufretboard.domain

/**
 * Utility to downsample mono audio from 44.1 kHz to 16 kHz.
 *
 * SwiftF0 expects 16 kHz input. Linear interpolation is sufficient for the
 * current supervisor cadence and ukulele pitch range.
 */
object AudioResampler {
    private const val SOURCE_RATE = 44100
    private const val TARGET_RATE = 16000
    private const val SOURCE_TO_TARGET_RATIO = SOURCE_RATE.toDouble() / TARGET_RATE

    fun downsample44kTo16k(input: FloatArray): FloatArray {
        if (input.isEmpty()) return FloatArray(0)

        val outputLength = (input.size / SOURCE_TO_TARGET_RATIO).toInt().coerceAtLeast(1)
        val output = FloatArray(outputLength)

        for (i in 0 until outputLength) {
            val sourcePos = i * SOURCE_TO_TARGET_RATIO
            val baseIdx = sourcePos.toInt().coerceIn(0, input.lastIndex)
            val nextIdx = (baseIdx + 1).coerceAtMost(input.lastIndex)
            val frac = (sourcePos - baseIdx).toFloat()
            output[i] = input[baseIdx] * (1f - frac) + input[nextIdx] * frac
        }

        return output
    }
}
