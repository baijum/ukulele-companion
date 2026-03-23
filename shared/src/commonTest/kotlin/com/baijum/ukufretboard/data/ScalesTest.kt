package com.baijum.ukufretboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScalesTest {

    // --- ALL list ---

    @Test
    fun allIsNonEmpty() {
        assertTrue(Scales.ALL.isNotEmpty())
    }

    @Test
    fun allScalesHaveNonEmptyNames() {
        for (scale in Scales.ALL) {
            assertTrue(scale.name.isNotEmpty(), "Scale should have a name")
        }
    }

    @Test
    fun allScalesHaveNonEmptyIntervals() {
        for (scale in Scales.ALL) {
            assertTrue(scale.intervals.isNotEmpty(), "Scale '${scale.name}' should have intervals")
        }
    }

    @Test
    fun allIntervalsStartWithZero() {
        for (scale in Scales.ALL) {
            assertEquals(0, scale.intervals.first(),
                "Scale '${scale.name}' intervals should start with 0")
        }
    }

    @Test
    fun allIntervalsInRange() {
        for (scale in Scales.ALL) {
            for (interval in scale.intervals) {
                assertTrue(interval in 0..11,
                    "Scale '${scale.name}' has interval $interval out of range")
            }
        }
    }

    @Test
    fun intervalsAreStrictlyAscending() {
        for (scale in Scales.ALL) {
            for (i in 1 until scale.intervals.size) {
                assertTrue(scale.intervals[i] > scale.intervals[i - 1],
                    "Scale '${scale.name}' intervals not ascending at index $i")
            }
        }
    }

    @Test
    fun allScalesHaveAtLeastOneCategory() {
        for (scale in Scales.ALL) {
            assertTrue(scale.categories.isNotEmpty(),
                "Scale '${scale.name}' should have at least one category")
        }
    }

    @Test
    fun noEmptyDuplicateNames() {
        val names = Scales.ALL.map { it.name }
        assertEquals(names.size, names.toSet().size, "Duplicate scale names found")
    }

    // --- forCategory() ---

    @Test
    fun forCategoryNullReturnsAll() {
        assertEquals(Scales.ALL.size, Scales.forCategory(null).size)
    }

    @Test
    fun forCategoryReturnsNonEmptyForEachCategory() {
        for (category in ScaleCategory.entries) {
            val scales = Scales.forCategory(category)
            assertTrue(scales.isNotEmpty(), "Category ${category.label} should have scales")
        }
    }

    @Test
    fun forCategoryOnlyReturnsMatchingScales() {
        for (category in ScaleCategory.entries) {
            val scales = Scales.forCategory(category)
            for (scale in scales) {
                assertTrue(category in scale.categories,
                    "Scale '${scale.name}' returned for $category but doesn't have it")
            }
        }
    }

    // --- scaleNotes() ---

    @Test
    fun cMajorScaleNotesAreCorrect() {
        val majorScale = Scales.ALL.first { it.name == "Major" }
        val notes = Scales.scaleNotes(0, majorScale)
        assertEquals(setOf(0, 2, 4, 5, 7, 9, 11), notes)
    }

    @Test
    fun gMajorScaleNotesAreTransposed() {
        val majorScale = Scales.ALL.first { it.name == "Major" }
        val notes = Scales.scaleNotes(7, majorScale)
        // G major: G A B C D E F# → pc 7,9,11,0,2,4,6
        assertEquals(setOf(7, 9, 11, 0, 2, 4, 6), notes)
    }

    @Test
    fun scaleNotesCountMatchesIntervalCount() {
        for (scale in Scales.ALL) {
            val notes = Scales.scaleNotes(0, scale)
            assertEquals(scale.intervals.size, notes.size,
                "Scale '${scale.name}' note count should match interval count")
        }
    }

    @Test
    fun scaleNotesAllInRange() {
        for (scale in Scales.ALL) {
            for (root in 0..11) {
                val notes = Scales.scaleNotes(root, scale)
                for (note in notes) {
                    assertTrue(note in 0..11, "Note $note out of range for '${scale.name}' root $root")
                }
            }
        }
    }

    @Test
    fun chromaticScaleHasTwelveNotes() {
        val chromatic = Scales.ALL.first { it.name == "Chromatic" }
        val notes = Scales.scaleNotes(0, chromatic)
        assertEquals(12, notes.size)
        assertEquals((0..11).toSet(), notes)
    }

    // --- Known scales ---

    @Test
    fun majorScaleHasSevenNotes() {
        val major = Scales.ALL.first { it.name == "Major" }
        assertEquals(7, major.intervals.size)
    }

    @Test
    fun pentatonicMajorHasFiveNotes() {
        val pent = Scales.ALL.first { it.name == "Pentatonic Major" }
        assertEquals(5, pent.intervals.size)
    }

    @Test
    fun bluesHasSixNotes() {
        val blues = Scales.ALL.first { it.name == "Blues" }
        assertEquals(6, blues.intervals.size)
    }

    @Test
    fun wholeToneHasSixNotes() {
        val wholeTone = Scales.ALL.first { it.name == "Whole Tone" }
        assertEquals(6, wholeTone.intervals.size)
        // All intervals are even (whole steps)
        assertTrue(wholeTone.intervals.all { it % 2 == 0 })
    }
}
