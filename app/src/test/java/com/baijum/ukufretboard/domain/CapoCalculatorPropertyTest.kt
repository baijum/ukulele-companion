package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.viewmodel.UkuleleString
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class CapoCalculatorPropertyTest {

    private val standardTuning = listOf(
        UkuleleString(name = "G", openPitchClass = 7),
        UkuleleString(name = "C", openPitchClass = 0),
        UkuleleString(name = "E", openPitchClass = 4),
        UkuleleString(name = "A", openPitchClass = 9),
    )

    private val triads = ChordFormulas.ALL.filter { it.intervals.size <= 4 }

    @Test
    fun capoPositionsAreInValidRange() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.element(triads)) { root, formula ->
                val results = CapoCalculator.forSingleChord(root, formula, standardTuning)
                for (result in results) {
                    assertTrue(
                        "Capo fret ${result.capoFret} should be in 0..11",
                        result.capoFret in 0..11,
                    )
                }
            }
        }
    }

    @Test
    fun resultsAreSortedByScoreDescending() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.element(triads)) { root, formula ->
                val results = CapoCalculator.forSingleChord(root, formula, standardTuning)
                for (i in 1 until results.size) {
                    assertTrue(
                        "Results should be sorted by score descending",
                        results[i - 1].score >= results[i].score,
                    )
                }
            }
        }
    }

    @Test
    fun soundingNameMatchesRequestedChord() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.element(triads)) { root, formula ->
                val results = CapoCalculator.forSingleChord(root, formula, standardTuning)
                for (result in results) {
                    assertTrue(
                        "Sounding name '${result.soundingName}' should end with '${formula.symbol}'",
                        result.soundingName.endsWith(formula.symbol),
                    )
                }
            }
        }
    }

    @Test
    fun neverCrashesForAnyRootAndFormula() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.element(ChordFormulas.ALL)) { root, formula ->
                CapoCalculator.forSingleChord(root, formula, standardTuning)
            }
        }
    }

    @Test
    fun voicingFretsAreInValidRange() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.element(triads)) { root, formula ->
                val results = CapoCalculator.forSingleChord(root, formula, standardTuning)
                for (result in results) {
                    for (fret in result.bestVoicing.frets) {
                        assertTrue(
                            "Fret $fret should be >= -1 (muted) and <= 15",
                            fret in -1..15,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun capoZeroAlwaysPresent() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.element(triads)) { root, formula ->
                val results = CapoCalculator.forSingleChord(root, formula, standardTuning)
                if (results.isNotEmpty()) {
                    assertTrue(
                        "Capo 0 (no capo) should be among results",
                        results.any { it.capoFret == 0 },
                    )
                }
            }
        }
    }
}
