package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordFormulas
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class ChordDetectorPropertyTest {

    @Test
    fun detectEmptyListReturnsNoSelection() {
        val result = ChordDetector.detect(emptyList())
        assertTrue(result is ChordDetector.DetectionResult.NoSelection)
    }

    @Test
    fun detectSingleNoteReturnsSingleNote() {
        runBlocking {
            checkAll(Arb.int(0..11)) { pc ->
                val result = ChordDetector.detect(listOf(pc))
                assertTrue(
                    result is ChordDetector.DetectionResult.SingleNote,
                    "Single pitch class $pc should yield SingleNote, got $result",
                )
            }
        }
    }

    @Test
    fun detectTwoNotesReturnsInterval() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.int(0..11)) { a, b ->
                if (a != b) {
                    val result = ChordDetector.detect(listOf(a, b))
                    assertTrue(
                        result is ChordDetector.DetectionResult.Interval,
                        "Two distinct pitch classes should yield Interval, got $result",
                    )
                }
            }
        }
    }

    @Test
    fun detectDuplicatesAreTreatedAsSinglePitchClass() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.int(2..10)) { pc, count ->
                val duplicated = List(count) { pc }
                val result = ChordDetector.detect(duplicated)
                assertTrue(
                    result is ChordDetector.DetectionResult.SingleNote,
                    "Duplicated single pitch class should yield SingleNote, got $result",
                )
            }
        }
    }

    @Test
    fun detectNeverCrashesOnArbitraryPitchClassLists() {
        runBlocking {
            checkAll(Arb.list(Arb.int(0..11), 0..12)) { pitchClasses ->
                ChordDetector.detect(pitchClasses)
            }
        }
    }

    @Test
    fun detectKnownFormulaAlwaysFindsChord() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.element(ChordFormulas.ALL)) { root, formula ->
                val pitchClasses = formula.intervals.map { (root + it) % 12 }
                val result = ChordDetector.detect(pitchClasses)
                assertTrue(
                    result is ChordDetector.DetectionResult.ChordFound,
                    "Root $root + ${formula.symbol} should yield ChordFound, got $result",
                )
            }
        }
    }

    @Test
    fun detectedChordRootIsInInputPitchClasses() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.element(ChordFormulas.ALL)) { root, formula ->
                val pitchClasses = formula.intervals.map { (root + it) % 12 }
                val result = ChordDetector.detect(pitchClasses)
                if (result is ChordDetector.DetectionResult.ChordFound) {
                    assertTrue(
                        result.result.root.pitchClass in pitchClasses,
                        "Root ${result.result.root.pitchClass} should be in input $pitchClasses",
                    )
                }
            }
        }
    }

    @Test
    fun detectedChordNotesMatchInputPitchClasses() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.element(ChordFormulas.ALL)) { root, formula ->
                val pitchClasses = formula.intervals.map { (root + it) % 12 }
                val result = ChordDetector.detect(pitchClasses)
                if (result is ChordDetector.DetectionResult.ChordFound) {
                    val detectedPcs = result.result.notes.map { it.pitchClass }.toSet()
                    val inputPcs = pitchClasses.toSet()
                    assertTrue(
                        detectedPcs == inputPcs,
                        "Detected pitch classes $detectedPcs should equal input $inputPcs",
                    )
                }
            }
        }
    }

    @Test
    fun resultTypeIsExhaustiveForAnyInputSize() {
        runBlocking {
            checkAll(Arb.list(Arb.int(0..11), 0..8)) { pitchClasses ->
                val result = ChordDetector.detect(pitchClasses)
                val uniqueCount = pitchClasses.distinct().size
                when {
                    uniqueCount == 0 -> assertTrue(result is ChordDetector.DetectionResult.NoSelection)
                    uniqueCount == 1 -> assertTrue(result is ChordDetector.DetectionResult.SingleNote)
                    uniqueCount == 2 -> assertTrue(result is ChordDetector.DetectionResult.Interval)
                    else -> assertTrue(
                        result is ChordDetector.DetectionResult.ChordFound ||
                            result is ChordDetector.DetectionResult.NoMatch,
                    )
                }
            }
        }
    }
}
