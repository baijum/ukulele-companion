package com.baijum.ukufretboard.data

import android.content.Context
import android.content.SharedPreferences
import com.baijum.ukufretboard.domain.ReviewPromptEligibility
import java.time.LocalDate

/**
 * SharedPreferences-backed storage for in-app review prompt state.
 *
 * Tracks:
 * - Distinct calendar days the app was opened ("active days")
 * - First launch date (for minimum time-since-install gate)
 * - Whether the Play review flow has already run to completion
 * - How many times the flow has been attempted
 * - When the flow was last attempted
 *
 * The rules themselves live in [ReviewPromptEligibility] so Android and iOS
 * cannot drift apart; this class only persists and reads back the state.
 */
class ReviewPromptRepository(
    context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun initFirstLaunch() {
        if (prefs.getLong(KEY_FIRST_LAUNCH, 0L) == 0L) {
            prefs.edit().putLong(KEY_FIRST_LAUNCH, System.currentTimeMillis()).apply()
        }
    }

    /**
     * Records today as an active usage day.
     * Idempotent per calendar day, and stops writing once the gate is satisfied
     * so the stored set cannot grow without bound.
     */
    fun recordActiveDay() {
        val days = activeDays()
        if (!ReviewPromptEligibility.shouldRecordActiveDay(days.size)) return
        val updated = days.toMutableSet()
        if (updated.add(todayKey())) {
            prefs.edit().putStringSet(KEY_ACTIVE_DAYS, updated).apply()
        }
    }

    fun activeDaysCount(): Int = activeDays().size

    fun hasReviewed(): Boolean = prefs.getBoolean(KEY_HAS_REVIEWED, false)

    fun promptCount(): Int = prefs.getInt(KEY_PROMPT_COUNT, 0)

    /**
     * Records that the Play review flow ran to completion.
     *
     * Play deliberately does not report whether the user actually reviewed, or
     * even whether the card was shown, so this only means "the flow completed".
     * Call it from the flow's completion listener — never before, or a failed
     * request would permanently lock the user out of ever being asked again.
     */
    fun recordReviewed() {
        prefs.edit().putBoolean(KEY_HAS_REVIEWED, true).apply()
    }

    /**
     * Records an attempt to launch the review flow.
     *
     * Called before the request so that a flow that fails to launch still burns
     * the cooldown, rather than retrying on every achievement unlock.
     */
    fun recordPromptShown() {
        prefs
            .edit()
            .putInt(KEY_PROMPT_COUNT, promptCount() + 1)
            .putLong(KEY_LAST_PROMPTED, System.currentTimeMillis())
            .apply()
    }

    /** Whether every gate in [ReviewPromptEligibility] passes for the stored state. */
    fun isEligible(): Boolean =
        ReviewPromptEligibility.isEligible(
            state = snapshot(),
            nowMillis = System.currentTimeMillis(),
        )

    private fun snapshot(): ReviewPromptEligibility.State =
        ReviewPromptEligibility.State(
            activeDayCount = activeDaysCount(),
            firstLaunchMillis = prefs.getLong(KEY_FIRST_LAUNCH, 0L),
            hasReviewed = hasReviewed(),
            promptCount = promptCount(),
            lastPromptedMillis = prefs.getLong(KEY_LAST_PROMPTED, 0L),
        )

    private fun activeDays(): Set<String> = prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet()) ?: emptySet()

    private fun todayKey(): String = LocalDate.now().toString()

    companion object {
        private const val PREFS_NAME = "review_prompt"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_ACTIVE_DAYS = "active_days"
        private const val KEY_HAS_REVIEWED = "has_reviewed"

        // Key kept as "dismiss_count" so installs that already recorded
        // dismissals under the old prompt keep their attempt budget.
        private const val KEY_PROMPT_COUNT = "dismiss_count"
        private const val KEY_LAST_PROMPTED = "last_prompted"
    }
}
