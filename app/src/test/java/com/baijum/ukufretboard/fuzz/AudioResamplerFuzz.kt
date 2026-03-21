package com.baijum.ukufretboard.fuzz

import com.baijum.ukufretboard.domain.AudioResampler
import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest

/**
 * Fuzz tests for [AudioResampler].
 *
 * [AudioResampler.downsample44kTo16k] is in the hot path of the neural pitch
 * pipeline, converting 44.1 kHz audio frames to 16 kHz for the SwiftF0 model.
 * These fuzzers verify robustness against:
 *   - Empty input (documented to return an empty array)
 *   - Single-sample input (edge case for the moving-average boundary logic)
 *   - Arbitrary float bit patterns (NaN, Inf, denormalized, extreme values)
 *   - Very large buffers (memory safety)
 */
class AudioResamplerFuzz {

    /**
     * Fuzz with fully-arbitrary float buffers (arbitrary size, arbitrary values).
     * Invariants verified:
     *   - Never throws
     *   - Output is non-null
     *   - Empty input → empty output
     */
    @FuzzTest
    fun fuzzDownsample(data: FuzzedDataProvider) {
        val remaining = data.consumeRemainingAsBytes()
        val samples = FloatArray(remaining.size / 4)
        for (i in samples.indices) {
            val offset = i * 4
            val bits = ((remaining[offset].toInt() and 0xFF) shl 24) or
                       ((remaining[offset + 1].toInt() and 0xFF) shl 16) or
                       ((remaining[offset + 2].toInt() and 0xFF) shl 8) or
                       (remaining[offset + 3].toInt() and 0xFF)
            samples[i] = Float.fromBits(bits)
        }

        val output = AudioResampler.downsample44kTo16k(samples)

        if (samples.isEmpty()) {
            check(output.isEmpty()) { "Empty input must produce empty output" }
        } else {
            check(output.isNotEmpty()) { "Non-empty input must produce non-empty output" }
        }
    }

    /**
     * Fuzz with a fixed-length buffer (4096 samples — one audio frame from
     * [AudioCaptureEngine]) filled with arbitrary floats. This exercises the
     * normal operational path most heavily.
     */
    @FuzzTest
    fun fuzzDownsampleOneFrame(data: FuzzedDataProvider) {
        val samples = FloatArray(4_096) { data.consumeFloat() }
        val output = AudioResampler.downsample44kTo16k(samples)

        // The expected output length for 4096 @ 44.1 kHz → 16 kHz (±1 for rounding)
        val expectedLen = (4_096 / (44_100.0 / 16_000.0)).toInt().coerceAtLeast(1)
        check(output.size in (expectedLen - 1)..(expectedLen + 1)) {
            "Unexpected output length: got ${output.size}, expected ~$expectedLen"
        }
    }
}
