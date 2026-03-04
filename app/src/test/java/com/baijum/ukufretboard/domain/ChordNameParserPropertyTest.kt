package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
                assertNotNull("Should parse '$input'", result)
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
                    "rootPitchClass ${result.rootPitchClass} should be in 0..11",
                    result.rootPitchClass in 0..11,
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
                    "displayName '${result.displayName}' should contain symbol '$symbol'",
                    result.displayName.contains(symbol),
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
                    "Root-only query '$root' should return multiple suggestions, got ${results.size}",
                    results.size > 1,
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
                    "Suggestions for '$root' should have unique symbols",
                    symbols.size,
                    symbols.distinct().size,
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
                assertNotNull("Should parse lowercase root '$lowerRoot'", lower)
                assertNotNull("Should parse uppercase root '${lowerRoot.uppercase()}'", upper)
                assertEquals(
                    "Case should not affect pitch class",
                    lower!!.rootPitchClass,
                    upper!!.rootPitchClass,
                )
            }
        }
    }
}
