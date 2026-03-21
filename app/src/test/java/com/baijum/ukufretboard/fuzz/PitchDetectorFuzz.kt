package com.baijum.ukufretboard.fuzz

import com.baijum.ukufretboard.domain.PitchDetector
import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest

/**
 * Fuzz tests for [PitchDetector].
 *
 * The YIN pitch-detection algorithm processes real-time audio buffers at
 * ~43 frames/second. This fuzzer verifies that [PitchDetector.detect] is
 * robust against:
 *   - Arbitrary float values (NaN, Infinity, denormalized, extreme magnitudes)
 *   - Buffers of any size (empty, 1-sample, millions of samples)
 *   - Unusual sample-rate values
 *   - Non-null previousFrequency values that trigger the constrained lag search
 */
class PitchDetectorFuzz {

    /**
     * Fuzz [PitchDetector.detect] with a varied sample array and realistic
     * sample rates (8 kHz – 192 kHz). Uses [FuzzedDataProvider] so Jazzer
     * can structure the byte stream into typed fields for better coverage.
     */
    @FuzzTest
    fun fuzzDetect(data: FuzzedDataProvider) {
        // Consume a sample rate in a realistic range so the lag bounds are sensible
        val sampleRate = data.consumeInt(8_000, 192_000)

        // A previous frequency hint (optional); null for ~half the runs
        val usePrevFreq = data.consumeBoolean()
        val previousFreq: Double? = if (usePrevFreq) data.consumeDouble() else null

        // The threshold drives the CMND dip search — fuzz it across valid range
        val threshold = data.consumeDouble()

        // Consume the rest as raw floats for the sample buffer
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

        // Must not throw for any combination of inputs
        PitchDetector.detect(samples, sampleRate, threshold, previousFreq)
    }

    /**
     * Focused variant that feeds pure-float buffers of power-of-2 sizes to
     * exercise the inner YIN difference-function loop more deeply.
     */
    @FuzzTest
    fun fuzzDetectPowerOfTwoBuffers(data: FuzzedDataProvider) {
        // Pick a power-of-2 buffer size (64 … 16384)
        val log2size = data.consumeInt(6, 14)
        val size = 1 shl log2size
        val samples = FloatArray(size) { data.consumeFloat() }

        PitchDetector.detect(samples, sampleRate = 44_100)
    }
}
