package com.baijum.ukufretboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataIntegrityTest {

    // ── Progressions ──

    @Test
    fun majorProgressionsAreNonEmpty() {
        assertTrue(Progressions.MAJOR_PROGRESSIONS.isNotEmpty())
    }

    @Test
    fun allProgressionsHaveNonEmptyNameAndDescription() {
        val all = ScaleType.entries.flatMap { Progressions.forScale(it) }
        for (prog in all) {
            assertTrue(prog.name.isNotEmpty(), "Progression should have a name")
            assertTrue(prog.description.isNotEmpty(), "Progression '${prog.name}' should have description")
        }
    }

    @Test
    fun allProgressionsHaveAtLeastTwoDegrees() {
        val all = ScaleType.entries.flatMap { Progressions.forScale(it) }
        for (prog in all) {
            assertTrue(prog.degrees.size >= 2,
                "Progression '${prog.name}' should have >= 2 degrees: ${prog.degrees.size}")
        }
    }

    @Test
    fun allDegreeIntervalsInRange() {
        val all = ScaleType.entries.flatMap { Progressions.forScale(it) }
        for (prog in all) {
            for (deg in prog.degrees) {
                assertTrue(deg.interval in 0..11,
                    "Degree interval ${deg.interval} out of range in '${prog.name}'")
            }
        }
    }

    @Test
    fun forScaleReturnsNonEmptyForEachType() {
        for (st in ScaleType.entries) {
            assertTrue(Progressions.forScale(st).isNotEmpty(),
                "ScaleType $st should have progressions")
        }
    }

    @Test
    fun diatonicDegreesReturnAtLeastSevenForMajor() {
        val degrees = Progressions.diatonicDegrees(ScaleType.MAJOR)
        assertTrue(degrees.size >= 7, "Major should have >= 7 diatonic degrees")
    }

    @Test
    fun diatonicDegreesNonEmptyForAllScaleTypes() {
        for (st in ScaleType.entries) {
            val degrees = Progressions.diatonicDegrees(st)
            assertTrue(degrees.isNotEmpty(), "ScaleType $st should have diatonic degrees")
        }
    }

    @Test
    fun noProgressionDuplicateNamesWithinScaleType() {
        for (st in ScaleType.entries) {
            val names = Progressions.forScale(st).map { it.name }
            assertEquals(names.size, names.toSet().size,
                "Duplicate progression names in $st: ${names.groupBy { it }.filter { it.value.size > 1 }.keys}")
        }
    }

    // ── ChordFormulas ──

    @Test
    fun chordFormulasAllIsNonEmpty() {
        assertTrue(ChordFormulas.ALL.isNotEmpty())
    }

    @Test
    fun allFormulasHaveNonEmptyQuality() {
        for (f in ChordFormulas.ALL) {
            assertTrue(f.quality.isNotEmpty(), "Formula should have quality")
        }
    }

    @Test
    fun allFormulasContainRoot() {
        for (f in ChordFormulas.ALL) {
            assertTrue(0 in f.intervals, "Formula '${f.quality}' should contain root (0)")
        }
    }

    @Test
    fun allIntervalsInRange() {
        for (f in ChordFormulas.ALL) {
            for (interval in f.intervals) {
                assertTrue(interval in 0..11,
                    "Interval $interval out of range in '${f.quality}'")
            }
        }
    }

    @Test
    fun byCategoryCoversAllCategories() {
        for (cat in ChordCategory.entries) {
            assertTrue(cat in ChordFormulas.BY_CATEGORY,
                "Category $cat missing from BY_CATEGORY")
            assertTrue(ChordFormulas.BY_CATEGORY[cat]!!.isNotEmpty(),
                "Category $cat should have formulas")
        }
    }

    @Test
    fun noFormulaDuplicateSymbols() {
        val symbols = ChordFormulas.ALL.map { it.symbol }
        assertEquals(symbols.size, symbols.toSet().size, "Duplicate formula symbols found")
    }

    // ── TheoryLessons ──

    @Test
    fun theoryLessonsAreNonEmpty() {
        assertTrue(TheoryLessons.ALL.isNotEmpty())
    }

    @Test
    fun allLessonsHaveNonEmptyFields() {
        for (lesson in TheoryLessons.ALL) {
            assertTrue(lesson.id.isNotEmpty(), "Lesson should have id")
            assertTrue(lesson.title.isNotEmpty(), "Lesson should have title")
            assertTrue(lesson.content.isNotEmpty(), "Lesson '${lesson.title}' should have content")
            assertTrue(lesson.keyPoints.isNotEmpty(), "Lesson '${lesson.title}' should have key points")
            assertTrue(lesson.quizQuestion.isNotEmpty(), "Lesson '${lesson.title}' should have quiz")
        }
    }

    @Test
    fun allLessonsHaveValidQuizIndex() {
        for (lesson in TheoryLessons.ALL) {
            assertTrue(lesson.quizCorrectIndex in lesson.quizOptions.indices,
                "Lesson '${lesson.title}' quizCorrectIndex ${lesson.quizCorrectIndex} out of bounds (${lesson.quizOptions.size} options)")
        }
    }

    @Test
    fun modulesAreNonEmpty() {
        assertTrue(TheoryLessons.MODULES.isNotEmpty())
        for (module in TheoryLessons.MODULES) {
            assertTrue(module.isNotEmpty())
        }
    }

    @Test
    fun byModuleCoversAllLessons() {
        val grouped = TheoryLessons.byModule()
        val totalGrouped = grouped.values.sumOf { it.size }
        assertEquals(TheoryLessons.ALL.size, totalGrouped,
            "byModule() should cover all lessons")
    }

    @Test
    fun noLessonDuplicateIds() {
        val ids = TheoryLessons.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Duplicate lesson IDs found")
    }

    // ── StrumPatterns ──

    @Test
    fun strumPatternsAreNonEmpty() {
        assertTrue(StrumPatterns.ALL.isNotEmpty())
    }

    @Test
    fun allStrumPatternsHaveNonEmptyFields() {
        for (p in StrumPatterns.ALL) {
            assertTrue(p.name.isNotEmpty(), "Pattern should have name")
            assertTrue(p.beats.isNotEmpty(), "Pattern '${p.name}' should have beats")
        }
    }

    @Test
    fun allStrumPatternsHaveValidBpmRange() {
        for (p in StrumPatterns.ALL) {
            assertTrue(p.suggestedBpm.first > 0, "Min BPM should be > 0 for '${p.name}'")
            assertTrue(p.suggestedBpm.last >= p.suggestedBpm.first,
                "Max BPM should be >= min for '${p.name}'")
        }
    }

    @Test
    fun allStrumPatternsHaveStandardTimeSignature() {
        val validTimeSigs = setOf("2/4", "3/4", "4/4", "6/8", "12/8")
        for (p in StrumPatterns.ALL) {
            assertTrue(p.timeSignature in validTimeSigs,
                "Pattern '${p.name}' has unexpected time sig: ${p.timeSignature}")
        }
    }

    // ── FingerpickingPatterns ──

    @Test
    fun fingerpickingPatternsAreNonEmpty() {
        assertTrue(FingerpickingPatterns.ALL.isNotEmpty())
    }

    @Test
    fun allFingerpickingStepsHaveValidStringIndex() {
        for (p in FingerpickingPatterns.ALL) {
            for (step in p.steps) {
                assertTrue(step.stringIndex in 0..3,
                    "Step string index ${step.stringIndex} out of range in '${p.name}'")
            }
        }
    }

    @Test
    fun allFingerpickingStepsHaveValidFinger() {
        for (p in FingerpickingPatterns.ALL) {
            for (step in p.steps) {
                assertTrue(step.finger in Finger.entries,
                    "Step finger ${step.finger} not valid in '${p.name}'")
            }
        }
    }

    // ── Glossary ──

    @Test
    fun glossaryIsNonEmpty() {
        assertTrue(Glossary.ALL.isNotEmpty())
    }

    @Test
    fun allGlossaryEntriesHaveNonEmptyTermAndDefinition() {
        for (entry in Glossary.ALL) {
            assertTrue(entry.term.isNotEmpty(), "Glossary entry should have term")
            assertTrue(entry.definition.isNotEmpty(),
                "Glossary entry '${entry.term}' should have definition")
        }
    }

    @Test
    fun glossaryIsRoughlySortedByFirstLetter() {
        val terms = Glossary.ALL.map { it.term }
        // Verify first letters are non-decreasing (case-insensitive)
        for (i in 1 until terms.size) {
            val prevFirst = terms[i - 1].first().lowercaseChar()
            val currFirst = terms[i].first().lowercaseChar()
            assertTrue(currFirst >= prevFirst,
                "Glossary out of order: '${terms[i - 1]}' before '${terms[i]}'")
        }
    }

    // ── ExplorerTips ──

    @Test
    fun explorerTipsAreNonEmpty() {
        // ChordResultView picks a tip with `currentTimeMillis() % ALL.size`.
        // An empty catalog is a divide-by-zero crash on the Explorer tab.
        assertTrue(ExplorerTips.ALL.isNotEmpty(), "ExplorerTips must never be empty")
    }

    @Test
    fun allExplorerTipsAreNonBlank() {
        for (tip in ExplorerTips.ALL) {
            assertTrue(tip.isNotBlank(), "ExplorerTips contains a blank tip")
        }
    }

    @Test
    fun allExplorerTipsAreTrimmed() {
        for (tip in ExplorerTips.ALL) {
            assertEquals(tip.trim(), tip, "Tip has stray whitespace: '$tip'")
        }
    }

    @Test
    fun explorerTipsHaveNoDuplicates() {
        val tips = ExplorerTips.ALL
        assertEquals(tips.size, tips.toSet().size, "ExplorerTips contains duplicates")
    }

    @Test
    fun explorerTipsFitTheDidYouKnowCard() {
        // The card is a fixed-height surface on the Explorer tab; an essay
        // overflows it and a fragment looks like a rendering bug.
        for (tip in ExplorerTips.ALL) {
            assertTrue(tip.length in 40..200, "Tip length ${tip.length} is out of range: '$tip'")
        }
    }

    @Test
    fun explorerTipsEndWithSentencePunctuation() {
        for (tip in ExplorerTips.ALL) {
            assertTrue(tip.last() in ".!?", "Tip is not a complete sentence: '$tip'")
        }
    }

    @Test
    fun tipIndexArithmeticStaysInBoundsForAnyTimestamp() {
        val timestamps = listOf(0L, 1L, 999L, 1_700_000_000_000L, Long.MAX_VALUE)
        for (millis in timestamps) {
            val index = (millis % ExplorerTips.ALL.size).toInt()
            assertTrue(index in ExplorerTips.ALL.indices, "Index $index out of bounds for $millis")
        }
    }
}
