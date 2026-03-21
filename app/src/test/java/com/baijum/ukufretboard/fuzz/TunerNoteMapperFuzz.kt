package com.baijum.ukufretboard.fuzz

import com.baijum.ukufretboard.domain.TunerNoteMapper
import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest

/**
 * Fuzz tests for [TunerNoteMapper].
 *
 * [TunerNoteMapper.mapFrequency] uses log2 arithmetic and modular pitch-class
 * math. These fuzzers verify robustness against:
 *   - Negative, zero, and subnormal [hz] values (documented to return null for ≤ 0)
 *   - Extreme [hz] values (Double.MAX_VALUE, Double.MIN_VALUE, Infinity, NaN)
 *   - Non-standard [a4Reference] tunings (e.g., 415 Hz, 466 Hz, 0, negative, Inf)
 *   - Combination of extreme hz and extreme a4Reference
 */
class TunerNoteMapperFuzz {

    /**
     * Fuzz with arbitrary [Double] pairs for hz and a4Reference.
     * Invariants verified:
     *   - Never throws for any Double input
     *   - Returns null when hz ≤ 0
     *   - When result is non-null, pitchClass is in 0–11
     */
    @FuzzTest
    fun fuzzMapFrequency(data: FuzzedDataProvider) {
        val hz = data.consumeDouble()
        val a4Reference = data.consumeDouble()

        val result = TunerNoteMapper.mapFrequency(hz, a4Reference)

        if (hz <= 0.0) {
            check(result == null) { "mapFrequency must return null for hz=$hz" }
        }
        if (result != null) {
            check(result.pitchClass in 0..11) {
                "pitchClass ${result.pitchClass} out of range for hz=$hz, a4=$a4Reference"
            }
        }
    }

    /**
     * Fuzz with a realistic [hz] range (20 Hz – 20 kHz) and the standard A4
     * reference (440 Hz). This exercises the common operational path and verifies
     * that all audible frequencies produce a valid [NoteInfo].
     */
    @FuzzTest
    fun fuzzMapFrequencyAudibleRange(data: FuzzedDataProvider) {
        val hz = data.consumeRegularDouble(20.0, 20_000.0)

        val result = TunerNoteMapper.mapFrequency(hz)

        // All positive finite frequencies must return a non-null result
        check(result != null) { "mapFrequency returned null for valid hz=$hz" }
        check(result.pitchClass in 0..11) {
            "pitchClass ${result.pitchClass} out of range for hz=$hz"
        }
    }
}
