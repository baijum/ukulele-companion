package com.baijum.ukufretboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DailyChallengeGeneratorTest {

    // --- Returns 3 challenges ---

    @Test
    fun returnsExactlyThreeChallenges() {
        val challenges = DailyChallengeGenerator.generateForDate(2026, 100)
        assertEquals(3, challenges.size)
    }

    // --- Determinism ---

    @Test
    fun sameDateReturnsSameChallenges() {
        val first = DailyChallengeGenerator.generateForDate(2026, 100)
        val second = DailyChallengeGenerator.generateForDate(2026, 100)
        assertEquals(first, second)
    }

    @Test
    fun differentDaysReturnDifferentChallenges() {
        val day1 = DailyChallengeGenerator.generateForDate(2026, 1)
        val day2 = DailyChallengeGenerator.generateForDate(2026, 2)
        assertNotEquals(day1, day2)
    }

    @Test
    fun differentYearsReturnDifferentChallenges() {
        val y1 = DailyChallengeGenerator.generateForDate(2025, 100)
        val y2 = DailyChallengeGenerator.generateForDate(2026, 100)
        assertNotEquals(y1, y2)
    }

    // --- Challenge validity ---

    @Test
    fun eachChallengeHasNonEmptyTitle() {
        val challenges = DailyChallengeGenerator.generateForDate(2026, 50)
        for (c in challenges) {
            assertTrue(c.title.isNotEmpty(), "Challenge should have title")
        }
    }

    @Test
    fun eachChallengeHasNonEmptyDescription() {
        val challenges = DailyChallengeGenerator.generateForDate(2026, 50)
        for (c in challenges) {
            assertTrue(c.description.isNotEmpty(), "Challenge should have description")
        }
    }

    @Test
    fun eachChallengeHasPositiveTargetCount() {
        val challenges = DailyChallengeGenerator.generateForDate(2026, 50)
        for (c in challenges) {
            assertTrue(c.targetCount > 0, "Target count should be positive: ${c.targetCount}")
        }
    }

    @Test
    fun eachChallengeHasValidType() {
        val challenges = DailyChallengeGenerator.generateForDate(2026, 50)
        for (c in challenges) {
            assertTrue(c.type in DailyChallengeGenerator.ChallengeType.entries)
        }
    }

    // --- Three distinct types per day ---

    @Test
    fun threeChallengesHaveDistinctTypes() {
        val challenges = DailyChallengeGenerator.generateForDate(2026, 50)
        val types = challenges.map { it.type }.toSet()
        assertEquals(3, types.size, "Should have 3 distinct types: $types")
    }

    // --- Challenge type coverage over many days ---

    @Test
    fun allChallengeTypesAppearOverManyDays() {
        val seenTypes = mutableSetOf<DailyChallengeGenerator.ChallengeType>()
        for (day in 1..60) {
            val challenges = DailyChallengeGenerator.generateForDate(2026, day)
            seenTypes.addAll(challenges.map { it.type })
        }
        assertEquals(
            DailyChallengeGenerator.ChallengeType.entries.toSet(),
            seenTypes,
            "All challenge types should appear over 60 days"
        )
    }

    // --- today() ---

    @Test
    fun todayReturnsThreeChallenges() {
        val challenges = DailyChallengeGenerator.today()
        assertEquals(3, challenges.size)
    }

    // --- Repeated calls are stable ---

    @Test
    fun repeatedCallsDoNotCrash() {
        for (day in 1..30) {
            DailyChallengeGenerator.generateForDate(2026, day)
        }
    }
}
