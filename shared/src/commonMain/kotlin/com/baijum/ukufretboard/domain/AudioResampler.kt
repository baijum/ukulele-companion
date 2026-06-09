package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.platform.PlatformLock
import com.baijum.ukufretboard.platform.withLock

/**
 * Utility to downsample mono audio from 44.1 kHz to 16 kHz.
 *
 * SwiftF0 expects 16 kHz input. A simple moving-average low-pass filter is
 * applied before linear interpolation to attenuate frequencies above the
 * 8 kHz Nyquist limit of the 16 kHz target rate, preventing aliased energy
 * from corrupting the neural model's input.
 */
object AudioResampler {
    private const val SOURCE_RATE = 44100
    private const val TARGET_RATE = 16000
    private const val SOURCE_TO_TARGET_RATIO = SOURCE_RATE.toDouble() / TARGET_RATE

    /**
     * Moving-average kernel half-width. A 5-tap moving average
     * (kernel = [1,1,1,1,1]/5) has its first null at 44100/5 = 8820 Hz,
     * providing ~14 dB attenuation at 8 kHz — sufficient for the neural
     * model's frequency range (46–2094 Hz fundamentals).
     */
    private const val MA_HALF = 2  // 5-tap: indices -2..+2

    private val lock = PlatformLock()

    // Cached work buffers, reused across calls to avoid per-frame allocation.
    // Guarded by [lock] — all access goes through [downsample44kTo16k].
    private var cachedInputSize = 0
    private var cachedFiltered = FloatArray(0)
    private var cachedOutput = FloatArray(0)

    fun downsample44kTo16k(input: FloatArray): FloatArray {
        if (input.isEmpty()) return FloatArray(0)

        return lock.withLock {
            val n = input.size
            if (n != cachedInputSize) {
                cachedInputSize = n
                cachedFiltered = FloatArray(n)
                val outLen = (n / SOURCE_TO_TARGET_RATIO).toInt().coerceAtLeast(1)
                cachedOutput = FloatArray(outLen)
            }

            applyMovingAverageInto(input, cachedFiltered)
            val filtered = cachedFiltered
            val output = cachedOutput

            for (i in output.indices) {
                val sourcePos = i * SOURCE_TO_TARGET_RATIO
                val baseIdx = sourcePos.toInt().coerceIn(0, filtered.lastIndex)
                val nextIdx = (baseIdx + 1).coerceAtMost(filtered.lastIndex)
                val frac = (sourcePos - baseIdx).toFloat()
                output[i] = filtered[baseIdx] * (1f - frac) + filtered[nextIdx] * frac
            }

            output
        }
    }

    /**
     * Applies a simple 5-tap moving average low-pass filter, writing results
     * into the pre-allocated [out] buffer.
     */
    private fun applyMovingAverageInto(input: FloatArray, out: FloatArray) {
        val n = input.size
        if (n <= MA_HALF * 2) {
            input.copyInto(out, 0, 0, n)
            return
        }

        for (i in 0 until n) {
            val lo = (i - MA_HALF).coerceAtLeast(0)
            val hi = (i + MA_HALF).coerceAtMost(n - 1)
            var sum = 0f
            for (j in lo..hi) sum += input[j]
            out[i] = sum / (hi - lo + 1)
        }
    }
}
