package com.baijum.ukufretboard.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Repository for managing in-app review prompt state using SharedPreferences.
 *
 * Tracks:
 * - Distinct calendar days the app was opened ("active days")
 * - First launch date (for minimum time-since-install gate)
 * - Whether the Play review flow has already run to completion
 * - How many times the flow has been attempted
 * - When the flow was last attempted
 *
 * There is deliberately no "user said no" state: the Play In-App Review API
 * forbids asking the user an opinion question before the flow, so the app never
 * learns whether the user reviewed or declined. Attempts are capped instead.
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
        if (days.size >= MIN_ACTIVE_DAYS) return
        val today = todayKey()
        val updated = days.toMutableSet()
        if (updated.add(today)) {
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

    /**
     * Returns `true` when all eligibility gates pass:
     * - 5+ distinct active days
     * - 7+ days since first launch
     * - Flow has not already completed
     * - Fewer than 3 prior attempts
     * - 90+ days since last attempt (or never attempted)
     */
    fun isEligible(): Boolean {
        if (hasReviewed()) return false
        if (promptCount() >= MAX_PROMPTS) return false
        if (activeDaysCount() < MIN_ACTIVE_DAYS) return false

        val firstLaunch = prefs.getLong(KEY_FIRST_LAUNCH, 0L)
        if (firstLaunch == 0L) return false
        val daysSinceInstall =
            TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - firstLaunch,
            )
        if (daysSinceInstall < MIN_DAYS_SINCE_INSTALL) return false

        val lastPrompted = prefs.getLong(KEY_LAST_PROMPTED, 0L)
        if (lastPrompted > 0L) {
            val daysSincePrompt =
                TimeUnit.MILLISECONDS.toDays(
                    System.currentTimeMillis() - lastPrompted,
                )
            if (daysSincePrompt < COOLDOWN_DAYS) return false
        }

        return true
    }

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

        private const val MIN_ACTIVE_DAYS = 5
        private const val MIN_DAYS_SINCE_INSTALL = 7
        private const val MAX_PROMPTS = 3
        private const val COOLDOWN_DAYS = 90L
    }
}
