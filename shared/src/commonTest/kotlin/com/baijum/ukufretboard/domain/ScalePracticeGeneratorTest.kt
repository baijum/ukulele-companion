package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ScaleCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScalePracticeGeneratorTest {

    // --- generateQuizQuestion ---

    @Test
    fun quizQuestionHasValidStructure() {
        val q = ScalePracticeGenerator.generateQuizQuestion()
        assertTrue(q.question.isNotEmpty())
        assertEquals(4, q.options.size)
        assertTrue(q.correctIndex in 0..3)
        assertTrue(q.explanation.isNotEmpty())
    }

    @Test
    fun quizQuestionCorrectAnswerInOptions() {
        repeat(10) {
            val q = ScalePracticeGenerator.generateQuizQuestion()
            assertTrue(q.options[q.correctIndex].isNotEmpty())
        }
    }

    @Test
    fun quizQuestionWithCategoryFilter() {
        val q = ScalePracticeGenerator.generateQuizQuestion(ScaleCategory.BASIC)
        assertTrue(q.question.isNotEmpty())
        assertEquals(4, q.options.size)
    }

    @Test
    fun quizQuestionAllCategoriesWork() {
        for (cat in ScaleCategory.entries) {
            val q = ScalePracticeGenerator.generateQuizQuestion(cat)
            assertTrue(q.question.isNotEmpty(), "Category $cat should produce a question")
        }
    }

    @Test
    fun quizQuestionRepeatedCallsDoNotCrash() {
        repeat(50) {
            ScalePracticeGenerator.generateQuizQuestion()
        }
    }

    @Test
    fun quizQuestionOptionsAreNonEmpty() {
        repeat(10) {
            val q = ScalePracticeGenerator.generateQuizQuestion()
            for (opt in q.options) {
                assertTrue(opt.isNotEmpty())
            }
        }
    }

    // --- generateEarQuestion ---

    @Test
    fun earQuestionHasValidStructure() {
        val q = ScalePracticeGenerator.generateEarQuestion()
        assertTrue(q.root in 0..11)
        assertTrue(q.scale.name.isNotEmpty())
        assertEquals(4, q.options.size)
        assertTrue(q.correctIndex in 0..3)
    }

    @Test
    fun earQuestionCorrectAnswerIsScaleName() {
        repeat(10) {
            val q = ScalePracticeGenerator.generateEarQuestion()
            assertEquals(q.scale.name, q.options[q.correctIndex])
        }
    }

    @Test
    fun earQuestionWithCategoryFilter() {
        val q = ScalePracticeGenerator.generateEarQuestion(ScaleCategory.MODES)
        assertTrue(q.scale.name.isNotEmpty())
    }

    @Test
    fun earQuestionRepeatedCallsDoNotCrash() {
        repeat(30) {
            ScalePracticeGenerator.generateEarQuestion()
        }
    }
}
