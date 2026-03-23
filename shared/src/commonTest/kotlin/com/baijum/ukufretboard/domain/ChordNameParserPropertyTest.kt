package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChordNameParserPropertyTest {

    private val allRootNames = (Notes.NOTE_NAMES_SHARP + Notes.NOTE_NAMES_FLAT).distinct()
    private val allSymbols = ChordFormulas.ALL.map { it.symbol }

    @Test
    fun parseNeverCrashesOnArbitraryStrings() {
        runBlocking {
            checkAll(Arb.string(0..100)) { input ->
                ChordNameParser.parse(input)
            }
        }
    }

    @Test
    fun suggestionsNeverCrashOnArbitraryStrings() {
        runBlocking {
            checkAll(Arb.string(0..100)) { input ->
                ChordNameParser.suggestions(input)
            }
        }
    }

    @Test
    fun parseKnownChordsAlwaysSucceeds() {
        runBlocking {
            checkAll(
                Arb.element(allRootNames),
                Arb.element(allSymbols),
            ) { root, symbol ->
                val input = "$root$symbol"
                val result = ChordNameParser.parse(input)
                assertNotNull(result, "Should parse '$input'")
            }
        }
    }

    @Test
    fun parsedRootPitchClassIsInValidRange() {
        runBlocking {
            checkAll(
                Arb.element(allRootNames),
                Arb.element(allSymbols),
            ) { root, symbol ->
                val result = ChordNameParser.parse("$root$symbol") ?: return@checkAll
                assertTrue(
                    result.rootPitchClass in 0..11,
                    "rootPitchClass ${result.rootPitchClass} should be in 0..11",
                )
            }
        }
    }

    @Test
    fun parseResultDisplayNameContainsFormulaSymbol() {
        runBlocking {
            checkAll(
                Arb.element(allRootNames),
                Arb.element(allSymbols),
            ) { root, symbol ->
                val result = ChordNameParser.parse("$root$symbol") ?: return@checkAll
                assertTrue(
                    result.displayName.contains(symbol),
                    "displayName '${result.displayName}' should contain symbol '$symbol'",
                )
            }
        }
    }

    @Test
    fun parseEmptyAndBlankReturnsNull() {
        runBlocking {
            checkAll(Arb.int(0..20)) { spaces ->
                val input = " ".repeat(spaces)
                assertEquals(null, ChordNameParser.parse(input))
            }
        }
    }

    @Test
    fun suggestionsForRootReturnAllFormulas() {
        runBlocking {
            checkAll(Arb.element(allRootNames)) { root ->
                val results = ChordNameParser.suggestions(root)
                assertTrue(
                    results.size > 1,
                    "Root-only query '$root' should return multiple suggestions, got ${results.size}",
                )
            }
        }
    }

    @Test
    fun suggestionsNeverReturnDuplicateSymbols() {
        runBlocking {
            checkAll(Arb.element(allRootNames)) { root ->
                val results = ChordNameParser.suggestions(root)
                val symbols = results.map { it.formula.symbol }
                assertEquals(
                    symbols.size,
                    symbols.distinct().size,
                    "Suggestions for '$root' should have unique symbols",
                )
            }
        }
    }

    @Test
    fun parseCaseInsensitiveRoot() {
        runBlocking {
            val roots = listOf("c", "d", "e", "f", "g", "a", "b")
            checkAll(Arb.element(roots)) { lowerRoot ->
                val lower = ChordNameParser.parse(lowerRoot)
                val upper = ChordNameParser.parse(lowerRoot.uppercase())
                assertNotNull(lower, "Should parse lowercase root '$lowerRoot'")
                assertNotNull(upper, "Should parse uppercase root '${lowerRoot.uppercase()}'")
                assertEquals(
                    lower.rootPitchClass,
                    upper.rootPitchClass,
                    "Case should not affect pitch class",
                )
            }
        }
    }
}
