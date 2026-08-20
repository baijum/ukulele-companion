package com.baijum.ukufretboard.domain

/**
 * Decides when the app may ask the platform store for a review.
 *
 * Both stores hand the app a single lever — "request the sheet now" — and then
 * decide for themselves whether anything is shown. Neither reports back what the
 * user did. So the only thing the app controls is *when* it asks, which is what
 * this object encodes: a pure decision over a state snapshot and a clock reading.
 *
 * The rules deliberately err towards asking rarely. Guidance from both stores is
 * to ask after a moment of success (here, an achievement unlock) and never to
 * nag; the caps below are the app's interpretation of that.
 *
 * There is deliberately no "user said no" state: both stores forbid preceding
 * the sheet with a custom opinion prompt, so the app never learns whether the
 * user reviewed or declined. Attempts are capped instead.
 *
 * Storage stays on the platform side ([com.baijum.ukufretboard.data] on Android,
 * `ReviewPromptManager` on iOS); only the decision lives here. Timestamps are
 * epoch milliseconds — iOS persists seconds and converts at this boundary.
 */
object ReviewPromptEligibility {
    /** Distinct calendar days the app must have been opened on. */
    const val MIN_ACTIVE_DAYS = 5

    /** Days that must have passed since first launch. */
    const val MIN_DAYS_SINCE_INSTALL = 7

    /** How many times the review flow may ever be requested. */
    const val MAX_PROMPTS = 3

    /** Days that must pass between two requests. */
    const val COOLDOWN_DAYS = 90

    private const val MILLIS_PER_DAY = 86_400_000L

    /**
     * A snapshot of everything persisted about the review prompt.
     *
     * @property activeDayCount Distinct calendar days the app has been opened on.
     * @property firstLaunchMillis When the app first ran, or 0 if never recorded.
     * @property hasReviewed Whether the flow has already run to completion. Only
     *   Android can observe this; iOS keeps reading it so installs that latched
     *   it under the old prompt are never asked again.
     * @property promptCount How many times the flow has been requested.
     * @property lastPromptedMillis When the flow was last requested, or 0 if never.
     */
    data class State(
        val activeDayCount: Int,
        val firstLaunchMillis: Long,
        val hasReviewed: Boolean,
        val promptCount: Int,
        val lastPromptedMillis: Long,
    )

    /**
     * Returns `true` when every gate passes:
     * - [MIN_ACTIVE_DAYS]+ distinct active days
     * - [MIN_DAYS_SINCE_INSTALL]+ days since first launch
     * - the flow has not already completed
     * - fewer than [MAX_PROMPTS] prior requests
     * - [COOLDOWN_DAYS]+ days since the last request, or never requested
     *
     * A clock that has moved backwards produces a negative elapsed span, which
     * fails the gate rather than passing it — the conservative direction. A
     * negative prompt count is nonsense rather than a fresh budget, so it is
     * treated as exhausted for the same reason.
     */
    fun isEligible(
        state: State,
        nowMillis: Long,
    ): Boolean {
        if (state.hasReviewed) return false
        if (state.promptCount !in 0 until MAX_PROMPTS) return false
        if (state.activeDayCount < MIN_ACTIVE_DAYS) return false

        if (state.firstLaunchMillis <= 0L) return false
        if (daysBetween(state.firstLaunchMillis, nowMillis) < MIN_DAYS_SINCE_INSTALL) return false

        if (state.lastPromptedMillis > 0L &&
            daysBetween(state.lastPromptedMillis, nowMillis) < COOLDOWN_DAYS
        ) {
            return false
        }

        return true
    }

    /**
     * Returns `true` while today is still worth recording as an active day.
     *
     * Once [MIN_ACTIVE_DAYS] have been banked the gate can never fail again, so
     * callers stop writing and the stored set cannot grow without bound.
     */
    fun shouldRecordActiveDay(activeDayCount: Int): Boolean = activeDayCount < MIN_ACTIVE_DAYS

    /** Whole days from [startMillis] to [endMillis], truncated towards zero. */
    private fun daysBetween(
        startMillis: Long,
        endMillis: Long,
    ): Long = (endMillis - startMillis) / MILLIS_PER_DAY
}
