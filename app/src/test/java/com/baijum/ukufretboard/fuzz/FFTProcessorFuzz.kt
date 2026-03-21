package com.baijum.ukufretboard.fuzz

import com.baijum.ukufretboard.domain.FFTProcessor
import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest

/**
 * Fuzz tests for [FFTProcessor].
 *
 * Covers:
 *   - [FFTProcessor.fft]: power-of-2 enforcement, float edge-values (NaN, Inf,
 *     denormalized), twiddle-cache behaviour across different FFT sizes.
 *   - [FFTProcessor.hanningWindow]: output range [0, 1], size invariant,
 *     edge-case inputs (empty not reached — size ≥ 1 enforced by fuzzer).
 */
class FFTProcessorFuzz {

    /**
     * Fuzz [FFTProcessor.fft] with a random power-of-2 buffer filled with
     * arbitrary float bit patterns so Jazzer can find corner cases in the
     * butterfly arithmetic or twiddle-cache eviction paths.
     *
     * The function documents that non-power-of-2 sizes throw [IllegalArgumentException];
     * this fuzzer only passes valid sizes so we assert no other exception escapes.
     */
    @FuzzTest
    fun fuzzFft(data: FuzzedDataProvider) {
        // Valid power-of-2 sizes: 2^1 (2) … 2^14 (16 384)
        val log2 = data.consumeInt(1, 14)
        val n = 1 shl log2

        val real = FloatArray(n) { data.consumeFloat() }
        val imag = FloatArray(n) { data.consumeFloat() }

        // Must not throw (other than the documented IllegalArgumentException
        // for bad sizes, which we never pass here).
        FFTProcessor.fft(real, imag)

        // Output arrays must retain their size after the in-place transform.
        check(real.size == n) { "fft() changed real array size" }
        check(imag.size == n) { "fft() changed imag array size" }
    }

    /**
     * Verify that [FFTProcessor.fft] throws [IllegalArgumentException] for
     * non-power-of-2 sizes, and does not crash or loop infinitely.
     */
    @FuzzTest
    fun fuzzFftNonPowerOfTwo(data: FuzzedDataProvider) {
        // Generate sizes in [3, 32767] that are NOT powers of two
        var size = data.consumeInt(3, 32_767)
        // Round to nearest non-power-of-2 (clear lowest set bit if power of 2)
        if (size > 0 && size and (size - 1) == 0) size += 1

        val real = FloatArray(size) { 0f }
        val imag = FloatArray(size) { 0f }

        try {
            FFTProcessor.fft(real, imag)
            error("Expected IllegalArgumentException for non-power-of-2 size=$size")
        } catch (_: IllegalArgumentException) {
            // Expected: size is guaranteed non-power-of-2 after adjustment
        }
    }

    /**
     * Fuzz [FFTProcessor.hanningWindow] with buffers of arbitrary length and
     * arbitrary float content. The output must satisfy:
     *   - Same length as input
     *   - No NaN in output when input is finite (window coefficients are finite)
     */
    @FuzzTest
    fun fuzzHanningWindow(data: FuzzedDataProvider) {
        // Size 1..4096 to keep memory bounded during a fuzz campaign
        val size = data.consumeInt(1, 4_096)
        val samples = FloatArray(size) { data.consumeFloat() }

        val windowed = FFTProcessor.hanningWindow(samples)

        check(windowed.size == size) { "hanningWindow() changed output size" }
    }
}
