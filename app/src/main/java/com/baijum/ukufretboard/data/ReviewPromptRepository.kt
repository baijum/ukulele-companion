package com.baijum.ukufretboard.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Repository for managing in-app review prompt state using SharedPreferences.
 *
 * Tracks:
 * - Distinct calendar days with Play/Create section usage ("active days")
 * - First launch date (for minimum time-since-install gate)
 * - Whether the user has already submitted a review
 * - How many times the prompt has been dismissed
 * - When the prompt was last shown
 */
class ReviewPromptRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun initFirstLaunch() {
        if (prefs.getLong(KEY_FIRST_LAUNCH, 0L) == 0L) {
            prefs.edit().putLong(KEY_FIRST_LAUNCH, System.currentTimeMillis()).apply()
        }
    }

    /**
     * Records today as an active Play/Create usage day.
     * Idempotent per calendar day.
     */
    fun recordActiveDay() {
        val today = todayKey()
        val days = activeDays().toMutableSet()
        if (days.add(today)) {
            prefs.edit().putStringSet(KEY_ACTIVE_DAYS, days).apply()
        }
    }

    fun activeDaysCount(): Int = activeDays().size

    fun hasReviewed(): Boolean = prefs.getBoolean(KEY_HAS_REVIEWED, false)

    fun dismissCount(): Int = prefs.getInt(KEY_DISMISS_COUNT, 0)

    fun recordReviewed() {
        prefs.edit().putBoolean(KEY_HAS_REVIEWED, true).apply()
    }

    fun recordDismissal() {
        prefs.edit()
            .putInt(KEY_DISMISS_COUNT, dismissCount() + 1)
            .putLong(KEY_LAST_PROMPTED, System.currentTimeMillis())
            .apply()
    }

    fun recordPromptShown() {
        prefs.edit().putLong(KEY_LAST_PROMPTED, System.currentTimeMillis()).apply()
    }

    /**
     * Returns `true` when all eligibility gates pass:
     * - 5+ distinct active days
     * - 7+ days since first launch
     * - Not already reviewed
     * - Fewer than 3 dismissals
     * - 90+ days since last prompt (or never prompted)
     */
    fun isEligible(): Boolean {
        if (hasReviewed()) return false
        if (dismissCount() >= MAX_DISMISSALS) return false
        if (activeDaysCount() < MIN_ACTIVE_DAYS) return false

        val firstLaunch = prefs.getLong(KEY_FIRST_LAUNCH, 0L)
        if (firstLaunch == 0L) return false
        val daysSinceInstall = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - firstLaunch,
        )
        if (daysSinceInstall < MIN_DAYS_SINCE_INSTALL) return false

        val lastPrompted = prefs.getLong(KEY_LAST_PROMPTED, 0L)
        if (lastPrompted > 0L) {
            val daysSincePrompt = TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - lastPrompted,
            )
            if (daysSincePrompt < COOLDOWN_DAYS) return false
        }

        return true
    }

    private fun activeDays(): Set<String> =
        prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet()) ?: emptySet()

    private fun todayKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    companion object {
        private const val PREFS_NAME = "review_prompt"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_ACTIVE_DAYS = "active_days"
        private const val KEY_HAS_REVIEWED = "has_reviewed"
        private const val KEY_DISMISS_COUNT = "dismiss_count"
        private const val KEY_LAST_PROMPTED = "last_prompted"

        private const val MIN_ACTIVE_DAYS = 5
        private const val MIN_DAYS_SINCE_INSTALL = 7
        private const val MAX_DISMISSALS = 3
        private const val COOLDOWN_DAYS = 90L
    }
}
