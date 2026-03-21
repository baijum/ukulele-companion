package com.baijum.ukufretboard.fuzz

import com.baijum.ukufretboard.domain.ChordDetector
import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest

/**
 * Fuzz tests for [ChordDetector].
 *
 * [ChordDetector.detect] normalises all pitch class inputs via modular reduction
 * before any name lookup, so any integer (negative, > 11, extreme) is safe.
 * These fuzzers verify that invariant holds across all input domains.
 */
class ChordDetectorFuzz {

    /**
     * Fuzz with valid pitch classes (0–11).
     * Must always return a valid [ChordDetector.DetectionResult] — never throw.
     */
    @FuzzTest
    fun fuzzDetectValidPitchClasses(data: FuzzedDataProvider) {
        val count = data.consumeInt(0, 12)
        val pitchClasses = List(count) { data.consumeInt(0, 11) }
        ChordDetector.detect(pitchClasses)
    }

    /**
     * Fuzz with arbitrary integers, including values outside 0–11.
     * Modular normalisation in [ChordDetector.detect] means this must never throw.
     */
    @FuzzTest
    fun fuzzDetectArbitraryPitchClasses(data: FuzzedDataProvider) {
        val count = data.consumeInt(0, 20)
        val pitchClasses = List(count) { data.consumeInt() }
        ChordDetector.detect(pitchClasses)
    }
}
