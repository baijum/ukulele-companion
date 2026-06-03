package com.baijum.ukufretboard.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository for tracking practice session durations using SharedPreferences.
 *
 * Records:
 * - Total practice time (lifetime minutes)
 * - Per-day practice time (today's minutes)
 * - Session count
 * - Longest session
 * - Daily goal progress
 */
class PracticeTimerRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Records the duration of a completed practice session.
     *
     * @param durationMs Duration in milliseconds.
     */
    fun recordSession(durationMs: Long) {
        val minutes = (durationMs / 60_000).toInt().coerceAtLeast(1)
        val today = todayKey()

        prefs.edit().apply {
            // Total lifetime minutes
            val totalMinutes = prefs.getInt(KEY_TOTAL_MINUTES, 0) + minutes
            putInt(KEY_TOTAL_MINUTES, totalMinutes)

            // Total sessions
            val totalSessions = prefs.getInt(KEY_TOTAL_SESSIONS, 0) + 1
            putInt(KEY_TOTAL_SESSIONS, totalSessions)

            // Today's minutes
            val todayMinutes = prefs.getInt("$KEY_DAY_MINUTES$today", 0) + minutes
            putInt("$KEY_DAY_MINUTES$today", todayMinutes)

            // Longest session
            val longestSession = prefs.getInt(KEY_LONGEST_SESSION, 0)
            if (minutes > longestSession) {
                putInt(KEY_LONGEST_SESSION, minutes)
            }

            // Last session timestamp
            putLong(KEY_LAST_SESSION, System.currentTimeMillis())

            apply()
        }
    }

    /** Returns total lifetime practice minutes. */
    fun totalMinutes(): Int = prefs.getInt(KEY_TOTAL_MINUTES, 0)

    /** Returns total number of practice sessions. */
    fun totalSessions(): Int = prefs.getInt(KEY_TOTAL_SESSIONS, 0)

    /** Returns today's practice minutes. */
    fun todayMinutes(): Int {
        val today = todayKey()
        return prefs.getInt("$KEY_DAY_MINUTES$today", 0)
    }

    /** Returns the longest session in minutes. */
    fun longestSession(): Int = prefs.getInt(KEY_LONGEST_SESSION, 0)

    /** Returns the last session timestamp, or 0. */
    fun lastSessionTime(): Long = prefs.getLong(KEY_LAST_SESSION, 0L)

    /** Returns the daily goal in minutes (default 15). */
    fun dailyGoal(): Int = prefs.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL)

    /** Sets the daily practice goal. */
    fun setDailyGoal(minutes: Int) {
        prefs.edit().putInt(KEY_DAILY_GOAL, minutes.coerceIn(5, 120)).apply()
    }

    /** Returns a summary of practice stats. */
    fun stats(): PracticeStats = PracticeStats(
        totalMinutes = totalMinutes(),
        totalSessions = totalSessions(),
        todayMinutes = todayMinutes(),
        longestSession = longestSession(),
        dailyGoal = dailyGoal(),
        lastSessionTime = lastSessionTime(),
    )

    /** Exports all practice timer data for backup. */
    fun exportAll(): PracticeTimerExport {
        val dailyMinutes = mutableMapOf<String, Int>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(KEY_DAY_MINUTES) && value is Int) {
                dailyMinutes[key.removePrefix(KEY_DAY_MINUTES)] = value
            }
        }
        return PracticeTimerExport(
            totalMinutes = totalMinutes(),
            totalSessions = totalSessions(),
            longestSession = longestSession(),
            lastSessionTime = lastSessionTime(),
            dailyGoal = dailyGoal(),
            dailyMinutes = dailyMinutes,
        )
    }

    /** Imports practice timer data from backup, using max-merge for cumulative stats. */
    fun importAll(data: PracticeTimerExport) {
        prefs.edit().apply {
            putInt(KEY_TOTAL_MINUTES, maxOf(totalMinutes(), data.totalMinutes))
            putInt(KEY_TOTAL_SESSIONS, maxOf(totalSessions(), data.totalSessions))
            putInt(KEY_LONGEST_SESSION, maxOf(longestSession(), data.longestSession))
            putLong(KEY_LAST_SESSION, maxOf(lastSessionTime(), data.lastSessionTime))
            val incomingGoal = data.dailyGoal.coerceIn(5, 120)
            if (incomingGoal != DEFAULT_DAILY_GOAL) {
                putInt(KEY_DAILY_GOAL, incomingGoal)
            }
            data.dailyMinutes.forEach { (day, minutes) ->
                val key = "$KEY_DAY_MINUTES$day"
                val existing = prefs.getInt(key, 0)
                putInt(key, maxOf(existing, minutes))
            }
            apply()
        }
    }

    /** Clears all practice time data. */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun todayKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    companion object {
        private const val PREFS_NAME = "practice_timer"
        private const val KEY_TOTAL_MINUTES = "total_minutes"
        private const val KEY_TOTAL_SESSIONS = "total_sessions"
        private const val KEY_DAY_MINUTES = "day_"
        private const val KEY_LONGEST_SESSION = "longest_session"
        private const val KEY_LAST_SESSION = "last_session"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val DEFAULT_DAILY_GOAL = 15
    }
}

/**
 * All practice timer data for backup export/import.
 */
data class PracticeTimerExport(
    val totalMinutes: Int = 0,
    val totalSessions: Int = 0,
    val longestSession: Int = 0,
    val lastSessionTime: Long = 0L,
    val dailyGoal: Int = 15,
    val dailyMinutes: Map<String, Int> = emptyMap(),
)

/**
 * Snapshot of practice time statistics.
 */
data class PracticeStats(
    val totalMinutes: Int = 0,
    val totalSessions: Int = 0,
    val todayMinutes: Int = 0,
    val longestSession: Int = 0,
    val dailyGoal: Int = 15,
    val lastSessionTime: Long = 0L,
) {
    /** Today's progress as a fraction (0.0–1.0+). */
    val dailyProgress: Float
        get() = if (dailyGoal > 0) todayMinutes.toFloat() / dailyGoal else 0f

    /** Formats total time as "Xh Ym". */
    val totalTimeFormatted: String
        get() {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }
}
