package com.baijum.ukufretboard.fuzz

import com.baijum.ukufretboard.domain.ChordNameParser
import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest

/**
 * Fuzz tests for [ChordNameParser].
 *
 * Targets the two public entry points that accept arbitrary user-supplied
 * strings from the chord search UI. Neither method should ever throw an
 * unexpected exception or hang regardless of the input.
 */
class ChordNameParserFuzz {

    /**
     * Fuzz [ChordNameParser.parse] with arbitrary byte sequences converted to
     * UTF-8 strings. The function must return either a valid [ChordSearchResult]
     * or null — it must never throw.
     */
    @FuzzTest
    fun fuzzParse(data: FuzzedDataProvider) {
        val input = data.consumeRemainingAsString()
        // Must not throw for any string input
        ChordNameParser.parse(input)
    }

    /**
     * Fuzz [ChordNameParser.suggestions] with arbitrary strings.
     * The function must return a (possibly empty) list and must never throw.
     */
    @FuzzTest
    fun fuzzSuggestions(data: FuzzedDataProvider) {
        val input = data.consumeRemainingAsString()
        ChordNameParser.suggestions(input)
    }
}
