package com.baijum.ukufretboard.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuizGeneratorTest {

    @Test
    fun generateWithoutCategoryReturnsQuestion() {
        val q = QuizGenerator.generate()
        assertTrue(q.question.isNotEmpty())
        assertTrue(q.options.size in 3..4, "Should have 3-4 options: ${q.options.size}")
        assertTrue(q.correctIndex in q.options.indices)
        assertTrue(q.explanation.isNotEmpty())
    }

    @Test
    fun generateWithCategoryReturnsMatchingCategory() {
        for (cat in QuizGenerator.QuizCategory.entries) {
            val q = QuizGenerator.generate(cat)
            assertEquals(cat, q.category)
        }
    }

    @Test
    fun correctIndexPointsToValidOption() {
        repeat(20) {
            val q = QuizGenerator.generate()
            assertTrue(q.correctIndex in q.options.indices)
        }
    }

    @Test
    fun optionsHaveAtLeastThreeEntries() {
        repeat(20) {
            val q = QuizGenerator.generate()
            assertTrue(q.options.size in 3..4, "Options should have 3-4 entries: ${q.options.size}")
        }
    }

    @Test
    fun optionsAreNonEmpty() {
        repeat(20) {
            val q = QuizGenerator.generate()
            for (opt in q.options) {
                assertTrue(opt.isNotEmpty(), "Option should not be empty")
            }
        }
    }

    @Test
    fun allCategoriesCanBeGenerated() {
        val seen = mutableSetOf<QuizGenerator.QuizCategory>()
        // Generate enough questions to likely see all categories
        repeat(100) {
            seen.add(QuizGenerator.generate().category)
        }
        assertEquals(QuizGenerator.QuizCategory.entries.toSet(), seen,
            "All categories should appear: missing ${QuizGenerator.QuizCategory.entries.toSet() - seen}")
    }

    @Test
    fun repeatedCallsDoNotCrash() {
        repeat(50) {
            QuizGenerator.generate()
        }
    }

    @Test
    fun intervalsQuestionHasValidContent() {
        val q = QuizGenerator.generate(QuizGenerator.QuizCategory.INTERVALS)
        assertTrue(q.question.isNotEmpty())
        assertTrue(q.explanation.isNotEmpty())
    }

    @Test
    fun chordsQuestionHasValidContent() {
        val q = QuizGenerator.generate(QuizGenerator.QuizCategory.CHORDS)
        assertTrue(q.question.isNotEmpty())
    }

    @Test
    fun keysQuestionHasValidContent() {
        val q = QuizGenerator.generate(QuizGenerator.QuizCategory.KEYS)
        assertTrue(q.question.isNotEmpty())
    }

    @Test
    fun scalesQuestionHasValidContent() {
        val q = QuizGenerator.generate(QuizGenerator.QuizCategory.SCALES)
        assertTrue(q.question.isNotEmpty())
    }

    @Test
    fun progressionsQuestionHasValidContent() {
        val q = QuizGenerator.generate(QuizGenerator.QuizCategory.PROGRESSIONS)
        assertTrue(q.question.isNotEmpty())
    }
}
