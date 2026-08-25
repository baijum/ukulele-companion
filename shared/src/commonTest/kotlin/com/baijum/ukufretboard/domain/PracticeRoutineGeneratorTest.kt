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
    fun titleIncludesRealisedDurationAndLevel() {
        val routine = PracticeRoutineGenerator.generate(durationMinutes = 30, skillLevel = SkillLevel.ADVANCED)
        // Title must reflect the realised total, never the requested duration,
        // so the header can never overstate the routine (#609).
        assertTrue(
            routine.title.contains("${routine.totalMinutes}"),
            "Title '${routine.title}' should include realised total ${routine.totalMinutes}",
        )
        assertTrue(routine.title.contains("Advanced"), "Title should include skill level")
    }

    @Test
    fun singleFocusAreaFillsRequestedDuration() {
        // Regression for #609: selecting one focus area used to drain the type
        // pool, so a "15-Minute" routine delivered only ~7 minutes. The pool
        // now refills, so the routine fills the requested time and the title
        // matches the realised total.
        val routine =
            PracticeRoutineGenerator.generate(
                durationMinutes = 15,
                skillLevel = SkillLevel.BEGINNER,
                focusAreas = setOf(FocusArea.CHORDS),
            )

        // The title must exactly match the realised total (no overstating).
        assertEquals(
            routine.steps.sumOf { it.durationMinutes },
            routine.totalMinutes,
            "totalMinutes should equal the sum of step durations",
        )
        assertTrue(
            routine.title.contains("${routine.totalMinutes}"),
            "Title '${routine.title}' should advertise the realised ${routine.totalMinutes} minutes",
        )

        // The routine must fill close to the requested 15 minutes, not the old
        // 7-minute (warm-up + one drill + cool-down) result.
        assertTrue(
            routine.totalMinutes >= 14,
            "Single focus area at 15 min should fill the duration, got ${routine.totalMinutes}",
        )

        // The focused exercises (everything but warm-up and cool-down) should
        // all be chord drills, repeating to fill the time.
        val exercises =
            routine.steps.filter {
                it.type != StepType.WARM_UP && it.type != StepType.FREE_PLAY
            }
        assertTrue(
            exercises.size > 1,
            "Pool should refill so exercises repeat, got ${exercises.size}",
        )
        assertTrue(
            exercises.all { it.type == StepType.CHORD_DRILL },
            "Only Chords was selected, so every exercise should be a chord drill",
        )
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
        assertTrue(
            short.steps.size <= long.steps.size,
            "Short routine (${short.steps.size} steps) should have <= steps than long (${long.steps.size})",
        )
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
