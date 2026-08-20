package com.baijum.ukufretboard.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewPromptEligibilityTest {
    private val day = 86_400_000L

    /** An arbitrary "now" far enough from the epoch that subtracting days stays positive. */
    private val now = 1_700_000_000_000L

    /** A state that passes every gate, so each test can break exactly one thing. */
    private fun eligibleState(
        activeDayCount: Int = ReviewPromptEligibility.MIN_ACTIVE_DAYS,
        firstLaunchMillis: Long = now - 30 * day,
        hasReviewed: Boolean = false,
        promptCount: Int = 0,
        lastPromptedMillis: Long = 0L,
    ) = ReviewPromptEligibility.State(
        activeDayCount = activeDayCount,
        firstLaunchMillis = firstLaunchMillis,
        hasReviewed = hasReviewed,
        promptCount = promptCount,
        lastPromptedMillis = lastPromptedMillis,
    )

    @Test
    fun eligibleWhenAllGatesPass() {
        assertTrue(ReviewPromptEligibility.isEligible(eligibleState(), now))
    }

    // --- hasReviewed ---

    @Test
    fun notEligibleAfterReviewing() {
        assertFalse(ReviewPromptEligibility.isEligible(eligibleState(hasReviewed = true), now))
    }

    // --- prompt count ---

    @Test
    fun eligibleBelowPromptCap() {
        val state =
            eligibleState(
                promptCount = ReviewPromptEligibility.MAX_PROMPTS - 1,
                lastPromptedMillis = now - 200 * day,
            )
        assertTrue(ReviewPromptEligibility.isEligible(state, now))
    }

    @Test
    fun notEligibleAtPromptCap() {
        val state =
            eligibleState(
                promptCount = ReviewPromptEligibility.MAX_PROMPTS,
                lastPromptedMillis = now - 200 * day,
            )
        assertFalse(ReviewPromptEligibility.isEligible(state, now))
    }

    @Test
    fun notEligibleWhenPromptCountIsNegative() {
        // A negative count can only come from a corrupt store. Reading it as a
        // fresh budget would hand that install unlimited prompts, so it counts
        // as exhausted instead.
        val state = eligibleState(promptCount = -1)
        assertFalse(ReviewPromptEligibility.isEligible(state, now))
    }

    // --- active days ---

    @Test
    fun notEligibleOneActiveDayShort() {
        val state = eligibleState(activeDayCount = ReviewPromptEligibility.MIN_ACTIVE_DAYS - 1)
        assertFalse(ReviewPromptEligibility.isEligible(state, now))
    }

    @Test
    fun eligibleWithMoreActiveDaysThanRequired() {
        val state = eligibleState(activeDayCount = ReviewPromptEligibility.MIN_ACTIVE_DAYS + 10)
        assertTrue(ReviewPromptEligibility.isEligible(state, now))
    }

    // --- days since install ---

    @Test
    fun notEligibleWhenFirstLaunchNeverRecorded() {
        assertFalse(ReviewPromptEligibility.isEligible(eligibleState(firstLaunchMillis = 0L), now))
    }

    @Test
    fun notEligibleWhenFirstLaunchIsNegative() {
        assertFalse(ReviewPromptEligibility.isEligible(eligibleState(firstLaunchMillis = -1L), now))
    }

    @Test
    fun eligibleExactlyAtInstallThreshold() {
        val installed = now - ReviewPromptEligibility.MIN_DAYS_SINCE_INSTALL * day
        assertTrue(ReviewPromptEligibility.isEligible(eligibleState(firstLaunchMillis = installed), now))
    }

    @Test
    fun notEligibleOneMillisecondBeforeInstallThreshold() {
        val installed = now - ReviewPromptEligibility.MIN_DAYS_SINCE_INSTALL * day + 1
        assertFalse(ReviewPromptEligibility.isEligible(eligibleState(firstLaunchMillis = installed), now))
    }

    @Test
    fun notEligibleWhenClockMovedBackBeforeInstall() {
        val state = eligibleState(firstLaunchMillis = now + 30 * day)
        assertFalse(ReviewPromptEligibility.isEligible(state, now))
    }

    // --- cooldown ---

    @Test
    fun eligibleWhenNeverPrompted() {
        assertTrue(ReviewPromptEligibility.isEligible(eligibleState(lastPromptedMillis = 0L), now))
    }

    /**
     * A 0 timestamp means "never prompted", not "prompted at the epoch". With a
     * present-day clock the distinction is invisible because the epoch is
     * decades past the cooldown, so this pins it with a clock close to zero.
     */
    @Test
    fun treatsUnsetLastPromptedAsNeverRatherThanTheEpoch() {
        val earlyNow = 10 * day
        val state = eligibleState(firstLaunchMillis = 1L, lastPromptedMillis = 0L)
        assertTrue(ReviewPromptEligibility.isEligible(state, earlyNow))
    }

    @Test
    fun eligibleExactlyAtCooldownThreshold() {
        val state =
            eligibleState(
                promptCount = 1,
                lastPromptedMillis = now - ReviewPromptEligibility.COOLDOWN_DAYS * day,
            )
        assertTrue(ReviewPromptEligibility.isEligible(state, now))
    }

    @Test
    fun notEligibleOneMillisecondBeforeCooldownThreshold() {
        val state =
            eligibleState(
                promptCount = 1,
                lastPromptedMillis = now - ReviewPromptEligibility.COOLDOWN_DAYS * day + 1,
            )
        assertFalse(ReviewPromptEligibility.isEligible(state, now))
    }

    @Test
    fun notEligibleImmediatelyAfterPrompting() {
        val state = eligibleState(promptCount = 1, lastPromptedMillis = now)
        assertFalse(ReviewPromptEligibility.isEligible(state, now))
    }

    @Test
    fun notEligibleWhenClockMovedBackAfterPrompting() {
        val state = eligibleState(promptCount = 1, lastPromptedMillis = now + 10 * day)
        assertFalse(ReviewPromptEligibility.isEligible(state, now))
    }

    // --- active day recording bound ---

    @Test
    fun recordsActiveDayUntilThresholdIsReached() {
        for (count in 0 until ReviewPromptEligibility.MIN_ACTIVE_DAYS) {
            assertTrue(
                ReviewPromptEligibility.shouldRecordActiveDay(count),
                "should still record at $count active days",
            )
        }
    }

    @Test
    fun stopsRecordingActiveDaysOnceThresholdIsReached() {
        assertFalse(ReviewPromptEligibility.shouldRecordActiveDay(ReviewPromptEligibility.MIN_ACTIVE_DAYS))
        assertFalse(ReviewPromptEligibility.shouldRecordActiveDay(ReviewPromptEligibility.MIN_ACTIVE_DAYS + 1))
    }
}
