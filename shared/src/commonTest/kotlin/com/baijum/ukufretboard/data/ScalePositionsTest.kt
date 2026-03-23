package com.baijum.ukufretboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScalePositionsTest {

    // Standard High-G tuning pitch classes
    private val highGTuning = listOf(7, 0, 4, 9)
    private val lowGTuning = listOf(7, 0, 4, 9) // same pitch classes, different octaves

    private val majorIntervals = listOf(0, 2, 4, 5, 7, 9, 11)
    private val pentatonicIntervals = listOf(0, 2, 4, 7, 9)

    // --- Basic generation ---

    @Test
    fun cMajorProducesMultiplePositions() {
        val positions = ScalePositions.generate(0, majorIntervals, highGTuning)
        assertTrue(positions.size >= 2, "Should produce multiple positions: ${positions.size}")
    }

    @Test
    fun positionsAreNamed() {
        val positions = ScalePositions.generate(0, majorIntervals, highGTuning)
        positions.forEachIndexed { i, pos ->
            assertEquals("Position ${i + 1}", pos.name)
        }
    }

    // --- Fret span ---

    @Test
    fun eachPositionSpansFiveFrets() {
        val positions = ScalePositions.generate(0, majorIntervals, highGTuning)
        for (pos in positions) {
            val span = pos.fretRange.last - pos.fretRange.first + 1
            assertEquals(5, span, "Position '${pos.name}' should span 5 frets, got $span")
        }
    }

    // --- Overlap ---

    @Test
    fun adjacentPositionsOverlapByOneFret() {
        val positions = ScalePositions.generate(0, majorIntervals, highGTuning)
        for (i in 0 until positions.size - 1) {
            val overlap = positions[i].fretRange.last
            val nextStart = positions[i + 1].fretRange.first
            assertEquals(overlap, nextStart,
                "Position ${i + 1} end (${overlap}) should equal position ${i + 2} start ($nextStart)")
        }
    }

    // --- All notes are scale notes ---

    @Test
    fun allNotesAreInScale() {
        val root = 0
        val scaleNotes = majorIntervals.map { (root + it) % 12 }.toSet()
        val positions = ScalePositions.generate(root, majorIntervals, highGTuning)
        for (pos in positions) {
            for ((stringIdx, frets) in pos.notes) {
                for (fret in frets) {
                    val pc = (highGTuning[stringIdx] + fret) % 12
                    assertTrue(pc in scaleNotes,
                        "Fret $fret on string $stringIdx produces pc $pc not in scale $scaleNotes")
                }
            }
        }
    }

    // --- Frets within range ---

    @Test
    fun allFretsAreWithinPositionRange() {
        val positions = ScalePositions.generate(0, majorIntervals, highGTuning)
        for (pos in positions) {
            for ((_, frets) in pos.notes) {
                for (fret in frets) {
                    assertTrue(fret in pos.fretRange,
                        "Fret $fret not in range ${pos.fretRange} for '${pos.name}'")
                }
            }
        }
    }

    // --- String indices are valid ---

    @Test
    fun stringIndicesAreValid() {
        val positions = ScalePositions.generate(0, majorIntervals, highGTuning)
        for (pos in positions) {
            for (stringIdx in pos.notes.keys) {
                assertTrue(stringIdx in 0..3, "String index $stringIdx out of range")
            }
        }
    }

    // --- Non-empty notes ---

    @Test
    fun eachPositionHasAtLeastOneNote() {
        val positions = ScalePositions.generate(0, majorIntervals, highGTuning)
        for (pos in positions) {
            assertTrue(pos.notes.isNotEmpty(), "Position '${pos.name}' should have notes")
        }
    }

    // --- Pentatonic scale ---

    @Test
    fun pentatonicScaleProducesPositions() {
        val positions = ScalePositions.generate(0, pentatonicIntervals, highGTuning)
        assertTrue(positions.isNotEmpty())
    }

    @Test
    fun pentatonicHasFewerNotesPerPosition() {
        val majorPositions = ScalePositions.generate(0, majorIntervals, highGTuning)
        val pentPositions = ScalePositions.generate(0, pentatonicIntervals, highGTuning)

        val majorTotal = majorPositions.sumOf { it.notes.values.sumOf { frets -> frets.size } }
        val pentTotal = pentPositions.sumOf { it.notes.values.sumOf { frets -> frets.size } }

        assertTrue(pentTotal < majorTotal,
            "Pentatonic ($pentTotal notes) should have fewer notes than major ($majorTotal)")
    }

    // --- Different roots ---

    @Test
    fun differentRootsProducePositions() {
        for (root in 0..11) {
            val positions = ScalePositions.generate(root, majorIntervals, highGTuning)
            assertTrue(positions.isNotEmpty(), "Root $root should produce positions")
        }
    }

    // --- All scale notes reachable ---

    @Test
    fun allScaleNotesReachableAcrossPositions() {
        val root = 0
        val scaleNotes = majorIntervals.map { (root + it) % 12 }.toSet()
        val positions = ScalePositions.generate(root, majorIntervals, highGTuning)

        val reachedPCs = mutableSetOf<Int>()
        for (pos in positions) {
            for ((stringIdx, frets) in pos.notes) {
                for (fret in frets) {
                    reachedPCs.add((highGTuning[stringIdx] + fret) % 12)
                }
            }
        }

        assertEquals(scaleNotes, reachedPCs,
            "All scale pitch classes should be reachable across positions")
    }
}
