package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.domain.PracticeRoutineGenerator.FocusArea
import com.baijum.ukufretboard.domain.PracticeRoutineGenerator.SkillLevel
import com.baijum.ukufretboard.domain.PracticeRoutineGenerator.StepType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PracticeRoutineGeneratorTest {

    @Test
    fun defaultRoutineIsNonEmpty() {
        val routine = PracticeRoutineGenerator.generate()
        assertTrue(routine.steps.isNotEmpty())
        assertTrue(routine.title.isNotEmpty())
    }

    @Test
    fun routineStartsWithWarmUp() {
        val routine = PracticeRoutineGenerator.generate()
        assertEquals(StepType.WARM_UP, routine.steps.first().type)
    }

    @Test
    fun routineEndsWithFreePlay() {
        val routine = PracticeRoutineGenerator.generate()
        assertEquals(StepType.FREE_PLAY, routine.steps.last().type)
    }

    @Test
    fun routineTotalMinutesMatchesStepSum() {
        val routine = PracticeRoutineGenerator.generate(durationMinutes = 20)
        assertEquals(routine.steps.sumOf { it.durationMinutes }, routine.totalMinutes)
    }

    @Test
    fun allStepsHaveNonEmptyTitleAndDescription() {
        val routine = PracticeRoutineGenerator.generate()
        for (step in routine.steps) {
            assertTrue(step.title.isNotEmpty(), "Step should have title")
            assertTrue(step.description.isNotEmpty(), "Step should have description")
        }
    }

    @Test
    fun allStepsHavePositiveDuration() {
        val routine = PracticeRoutineGenerator.generate()
        for (step in routine.steps) {
            assertTrue(step.durationMinutes > 0, "Step '${step.title}' should have positive duration")
        }
    }

    @Test
    fun titleIncludesDurationAndLevel() {
        val routine = PracticeRoutineGenerator.generate(durationMinutes = 30, skillLevel = SkillLevel.ADVANCED)
        assertTrue(routine.title.contains("30"), "Title should include duration")
        assertTrue(routine.title.contains("Advanced"), "Title should include skill level")
    }

    // --- Skill levels ---

    @Test
    fun allSkillLevelsProduceRoutines() {
        for (level in SkillLevel.entries) {
            val routine = PracticeRoutineGenerator.generate(skillLevel = level)
            assertTrue(routine.steps.size >= 3, "$level should produce at least warm-up + exercise + cool-down")
        }
    }

    // --- Focus areas ---

    @Test
    fun emptyFocusAreasStillProducesRoutine() {
        val routine = PracticeRoutineGenerator.generate(focusAreas = emptySet())
        // Should still have warm-up and cool-down at minimum
        assertTrue(routine.steps.size >= 2)
    }

    @Test
    fun singleFocusAreaProducesRoutine() {
        for (area in FocusArea.entries) {
            val routine = PracticeRoutineGenerator.generate(focusAreas = setOf(area))
            assertTrue(routine.steps.isNotEmpty(), "Focus $area should produce a routine")
        }
    }

    // --- Duration ---

    @Test
    fun shortRoutineHasFewerSteps() {
        val short = PracticeRoutineGenerator.generate(durationMinutes = 10)
        val long = PracticeRoutineGenerator.generate(durationMinutes = 30)
        assertTrue(short.steps.size <= long.steps.size,
            "Short routine (${short.steps.size} steps) should have <= steps than long (${long.steps.size})")
    }

    // --- Step types are valid ---

    @Test
    fun allStepTypesAreFromEnum() {
        val routine = PracticeRoutineGenerator.generate()
        for (step in routine.steps) {
            assertTrue(step.type in StepType.entries, "Unknown step type: ${step.type}")
        }
    }

    @Test
    fun repeatedCallsDoNotCrash() {
        repeat(20) {
            PracticeRoutineGenerator.generate(
                durationMinutes = listOf(10, 15, 20, 30).random(),
                skillLevel = SkillLevel.entries.random(),
            )
        }
    }
}
