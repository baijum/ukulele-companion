package com.baijum.ukufretboard.data

import android.content.Context
import android.content.SharedPreferences
import com.baijum.ukufretboard.domain.QuizGenerator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Repository for persisting Learn section progress using SharedPreferences.
 *
 * Tracks:
 * - Theory Lesson completion and mini quiz results
 * - Theory Quiz scores, accuracy, and streaks per category
 * - Interval Trainer scores, accuracy, and streaks per level
 * - Daily learning activity streak
 */
class LearningProgressRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Theory Lessons ──────────────────────────────────────────────

    /** Marks a lesson as completed (read). */
    fun markLessonCompleted(lessonId: String) {
        prefs.edit().putBoolean("$KEY_LESSON_COMPLETED$lessonId", true).apply()
        recordActivity()
    }

    /** Returns whether a lesson has been completed. */
    fun isLessonCompleted(lessonId: String): Boolean =
        prefs.getBoolean("$KEY_LESSON_COMPLETED$lessonId", false)

    /** Marks a lesson's mini quiz as passed. */
    fun markLessonQuizPassed(lessonId: String) {
        prefs.edit().putBoolean("$KEY_LESSON_QUIZ$lessonId", true).apply()
        recordActivity()
    }

    /** Returns whether a lesson's mini quiz has been passed. */
    fun isLessonQuizPassed(lessonId: String): Boolean =
        prefs.getBoolean("$KEY_LESSON_QUIZ$lessonId", false)

    /** Returns the count of completed lessons. */
    fun completedLessonCount(): Int =
        TheoryLessons.ALL.count { isLessonCompleted(it.id) }

    /** Returns the count of passed lesson quizzes. */
    fun passedQuizCount(): Int =
        TheoryLessons.ALL.count { isLessonQuizPassed(it.id) }

    // ── Theory Quiz ─────────────────────────────────────────────────

    /**
     * Records a quiz answer for the given category.
     *
     * Updates both per-category and overall stats.
     */
    fun recordQuizAnswer(category: QuizGenerator.QuizCategory, correct: Boolean) {
        val catKey = category.name
        val editor = prefs.edit()

        editor.putInt("$KEY_QUIZ_TOTAL$catKey", prefs.getInt("$KEY_QUIZ_TOTAL$catKey", 0) + 1)
        editor.putInt(KEY_QUIZ_TOTAL_ALL, prefs.getInt(KEY_QUIZ_TOTAL_ALL, 0) + 1)

        if (correct) {
            editor.putInt("$KEY_QUIZ_CORRECT$catKey", prefs.getInt("$KEY_QUIZ_CORRECT$catKey", 0) + 1)
            editor.putInt(KEY_QUIZ_CORRECT_ALL, prefs.getInt(KEY_QUIZ_CORRECT_ALL, 0) + 1)
            val newStreak = prefs.getInt("$KEY_QUIZ_STREAK$catKey", 0) + 1
            editor.putInt("$KEY_QUIZ_STREAK$catKey", newStreak)
            if (newStreak > prefs.getInt("$KEY_QUIZ_BEST$catKey", 0)) {
                editor.putInt("$KEY_QUIZ_BEST$catKey", newStreak)
            }
            val newOverallStreak = prefs.getInt(KEY_QUIZ_STREAK_ALL, 0) + 1
            editor.putInt(KEY_QUIZ_STREAK_ALL, newOverallStreak)
            if (newOverallStreak > prefs.getInt(KEY_QUIZ_BEST_ALL, 0)) {
                editor.putInt(KEY_QUIZ_BEST_ALL, newOverallStreak)
            }
        } else {
            editor.putInt("$KEY_QUIZ_STREAK$catKey", 0)
            editor.putInt(KEY_QUIZ_STREAK_ALL, 0)
        }

        applyActivityUpdate(editor)
    }

    /** Returns quiz stats for a category, or overall if null. */
    fun quizStats(category: QuizGenerator.QuizCategory? = null): LearningStats {
        val suffix = category?.name ?: "ALL"
        val totalKey = if (category != null) "$KEY_QUIZ_TOTAL${suffix}" else KEY_QUIZ_TOTAL_ALL
        val correctKey = if (category != null) "$KEY_QUIZ_CORRECT${suffix}" else KEY_QUIZ_CORRECT_ALL
        val bestKey = if (category != null) "$KEY_QUIZ_BEST${suffix}" else KEY_QUIZ_BEST_ALL
        return LearningStats(
            total = prefs.getInt(totalKey, 0),
            correct = prefs.getInt(correctKey, 0),
            bestStreak = prefs.getInt(bestKey, 0),
        )
    }

    // ── Interval Trainer ────────────────────────────────────────────

    /**
     * Records an interval trainer answer for the given level.
     *
     * Updates both per-level and overall stats.
     */
    fun recordIntervalAnswer(level: Int, correct: Boolean) {
        val lvl = level.coerceIn(1, 4)
        val editor = prefs.edit()

        editor.putInt("$KEY_INTERVAL_TOTAL$lvl", prefs.getInt("$KEY_INTERVAL_TOTAL$lvl", 0) + 1)
        editor.putInt(KEY_INTERVAL_TOTAL_ALL, prefs.getInt(KEY_INTERVAL_TOTAL_ALL, 0) + 1)

        if (correct) {
            editor.putInt("$KEY_INTERVAL_CORRECT$lvl", prefs.getInt("$KEY_INTERVAL_CORRECT$lvl", 0) + 1)
            editor.putInt(KEY_INTERVAL_CORRECT_ALL, prefs.getInt(KEY_INTERVAL_CORRECT_ALL, 0) + 1)
            val newStreak = prefs.getInt("$KEY_INTERVAL_STREAK$lvl", 0) + 1
            editor.putInt("$KEY_INTERVAL_STREAK$lvl", newStreak)
            if (newStreak > prefs.getInt("$KEY_INTERVAL_BEST$lvl", 0)) {
                editor.putInt("$KEY_INTERVAL_BEST$lvl", newStreak)
            }
            val newOverallStreak = prefs.getInt(KEY_INTERVAL_STREAK_ALL, 0) + 1
            editor.putInt(KEY_INTERVAL_STREAK_ALL, newOverallStreak)
            if (newOverallStreak > prefs.getInt(KEY_INTERVAL_BEST_ALL, 0)) {
                editor.putInt(KEY_INTERVAL_BEST_ALL, newOverallStreak)
            }
        } else {
            editor.putInt("$KEY_INTERVAL_STREAK$lvl", 0)
            editor.putInt(KEY_INTERVAL_STREAK_ALL, 0)
        }

        applyActivityUpdate(editor)
    }

    /** Returns interval trainer stats for a level (1–4), or overall if null. */
    fun intervalStats(level: Int? = null): LearningStats {
        val suffix = level?.coerceIn(1, 4)?.toString() ?: "ALL"
        val totalKey = if (level != null) "$KEY_INTERVAL_TOTAL${suffix}" else KEY_INTERVAL_TOTAL_ALL
        val correctKey = if (level != null) "$KEY_INTERVAL_CORRECT${suffix}" else KEY_INTERVAL_CORRECT_ALL
        val bestKey = if (level != null) "$KEY_INTERVAL_BEST${suffix}" else KEY_INTERVAL_BEST_ALL
        return LearningStats(
            total = prefs.getInt(totalKey, 0),
            correct = prefs.getInt(correctKey, 0),
            bestStreak = prefs.getInt(bestKey, 0),
        )
    }

    // ── Note Quiz ────────────────────────────────────────────────────

    /** Records a note quiz answer. Updates overall stats. */
    fun recordNoteQuizAnswer(correct: Boolean) {
        val editor = prefs.edit()

        editor.putInt(KEY_NOTE_QUIZ_TOTAL, prefs.getInt(KEY_NOTE_QUIZ_TOTAL, 0) + 1)

        if (correct) {
            editor.putInt(KEY_NOTE_QUIZ_CORRECT, prefs.getInt(KEY_NOTE_QUIZ_CORRECT, 0) + 1)
            val newStreak = prefs.getInt(KEY_NOTE_QUIZ_STREAK, 0) + 1
            editor.putInt(KEY_NOTE_QUIZ_STREAK, newStreak)
            if (newStreak > prefs.getInt(KEY_NOTE_QUIZ_BEST, 0)) {
                editor.putInt(KEY_NOTE_QUIZ_BEST, newStreak)
            }
        } else {
            editor.putInt(KEY_NOTE_QUIZ_STREAK, 0)
        }

        applyActivityUpdate(editor)
    }

    /** Returns note quiz stats. */
    fun noteQuizStats(): LearningStats = LearningStats(
        total = prefs.getInt(KEY_NOTE_QUIZ_TOTAL, 0),
        correct = prefs.getInt(KEY_NOTE_QUIZ_CORRECT, 0),
        bestStreak = prefs.getInt(KEY_NOTE_QUIZ_BEST, 0),
    )

    // ── Chord Ear Training ──────────────────────────────────────────

    /**
     * Records a chord ear training answer for the given level.
     *
     * Updates both per-level and overall stats.
     */
    fun recordChordEarAnswer(level: Int, correct: Boolean) {
        val lvl = level.coerceIn(1, 4)
        val editor = prefs.edit()

        editor.putInt("$KEY_CHORD_EAR_TOTAL$lvl", prefs.getInt("$KEY_CHORD_EAR_TOTAL$lvl", 0) + 1)
        editor.putInt(KEY_CHORD_EAR_TOTAL_ALL, prefs.getInt(KEY_CHORD_EAR_TOTAL_ALL, 0) + 1)

        if (correct) {
            editor.putInt("$KEY_CHORD_EAR_CORRECT$lvl", prefs.getInt("$KEY_CHORD_EAR_CORRECT$lvl", 0) + 1)
            editor.putInt(KEY_CHORD_EAR_CORRECT_ALL, prefs.getInt(KEY_CHORD_EAR_CORRECT_ALL, 0) + 1)
            val newStreak = prefs.getInt("$KEY_CHORD_EAR_STREAK$lvl", 0) + 1
            editor.putInt("$KEY_CHORD_EAR_STREAK$lvl", newStreak)
            if (newStreak > prefs.getInt("$KEY_CHORD_EAR_BEST$lvl", 0)) {
                editor.putInt("$KEY_CHORD_EAR_BEST$lvl", newStreak)
            }
            val newOverallStreak = prefs.getInt(KEY_CHORD_EAR_STREAK_ALL, 0) + 1
            editor.putInt(KEY_CHORD_EAR_STREAK_ALL, newOverallStreak)
            if (newOverallStreak > prefs.getInt(KEY_CHORD_EAR_BEST_ALL, 0)) {
                editor.putInt(KEY_CHORD_EAR_BEST_ALL, newOverallStreak)
            }
        } else {
            editor.putInt("$KEY_CHORD_EAR_STREAK$lvl", 0)
            editor.putInt(KEY_CHORD_EAR_STREAK_ALL, 0)
        }

        applyActivityUpdate(editor)
    }

    /** Returns chord ear training stats for a level (1–4), or overall if null. */
    fun chordEarStats(level: Int? = null): LearningStats {
        val suffix = level?.coerceIn(1, 4)?.toString() ?: "ALL"
        val totalKey = if (level != null) "$KEY_CHORD_EAR_TOTAL${suffix}" else KEY_CHORD_EAR_TOTAL_ALL
        val correctKey = if (level != null) "$KEY_CHORD_EAR_CORRECT${suffix}" else KEY_CHORD_EAR_CORRECT_ALL
        val bestKey = if (level != null) "$KEY_CHORD_EAR_BEST${suffix}" else KEY_CHORD_EAR_BEST_ALL
        return LearningStats(
            total = prefs.getInt(totalKey, 0),
            correct = prefs.getInt(correctKey, 0),
            bestStreak = prefs.getInt(bestKey, 0),
        )
    }

    // ── Scale Practice ─────────────────────────────────────────────

    /**
     * Records a scale practice answer (quiz or ear training).
     *
     * @param mode "quiz" or "ear" to distinguish the two practice types.
     * @param correct Whether the answer was correct.
     */
    fun recordScalePracticeAnswer(mode: String, correct: Boolean) {
        val m = mode.lowercase()
        val editor = prefs.edit()

        editor.putInt("$KEY_SCALE_PRACTICE_TOTAL$m", prefs.getInt("$KEY_SCALE_PRACTICE_TOTAL$m", 0) + 1)
        editor.putInt(KEY_SCALE_PRACTICE_TOTAL_ALL, prefs.getInt(KEY_SCALE_PRACTICE_TOTAL_ALL, 0) + 1)

        if (correct) {
            editor.putInt("$KEY_SCALE_PRACTICE_CORRECT$m", prefs.getInt("$KEY_SCALE_PRACTICE_CORRECT$m", 0) + 1)
            editor.putInt(KEY_SCALE_PRACTICE_CORRECT_ALL, prefs.getInt(KEY_SCALE_PRACTICE_CORRECT_ALL, 0) + 1)
            val newStreak = prefs.getInt("$KEY_SCALE_PRACTICE_STREAK$m", 0) + 1
            editor.putInt("$KEY_SCALE_PRACTICE_STREAK$m", newStreak)
            if (newStreak > prefs.getInt("$KEY_SCALE_PRACTICE_BEST$m", 0)) {
                editor.putInt("$KEY_SCALE_PRACTICE_BEST$m", newStreak)
            }
            val newOverallStreak = prefs.getInt(KEY_SCALE_PRACTICE_STREAK_ALL, 0) + 1
            editor.putInt(KEY_SCALE_PRACTICE_STREAK_ALL, newOverallStreak)
            if (newOverallStreak > prefs.getInt(KEY_SCALE_PRACTICE_BEST_ALL, 0)) {
                editor.putInt(KEY_SCALE_PRACTICE_BEST_ALL, newOverallStreak)
            }
        } else {
            editor.putInt("$KEY_SCALE_PRACTICE_STREAK$m", 0)
            editor.putInt(KEY_SCALE_PRACTICE_STREAK_ALL, 0)
        }

        applyActivityUpdate(editor)
    }

    /** Returns scale practice stats for a mode ("quiz"/"ear"), or overall if null. */
    fun scalePracticeStats(mode: String? = null): LearningStats {
        val suffix = mode?.lowercase() ?: "ALL"
        val totalKey = if (mode != null) "$KEY_SCALE_PRACTICE_TOTAL${suffix}" else KEY_SCALE_PRACTICE_TOTAL_ALL
        val correctKey = if (mode != null) "$KEY_SCALE_PRACTICE_CORRECT${suffix}" else KEY_SCALE_PRACTICE_CORRECT_ALL
        val bestKey = if (mode != null) "$KEY_SCALE_PRACTICE_BEST${suffix}" else KEY_SCALE_PRACTICE_BEST_ALL
        return LearningStats(
            total = prefs.getInt(totalKey, 0),
            correct = prefs.getInt(correctKey, 0),
            bestStreak = prefs.getInt(bestKey, 0),
        )
    }

    // ── Daily Streak ────────────────────────────────────────────────

    /** Records a learning activity for today. Updates the daily streak. */
    fun recordActivity() {
        applyActivityUpdate(prefs.edit())
    }

    private fun applyActivityUpdate(editor: SharedPreferences.Editor) {
        val today = todayString()
        val lastDate = prefs.getString(KEY_LAST_ACTIVITY, null)
        val currentStreak = prefs.getInt(KEY_STREAK_DAYS, 0)

        val newStreak = when (lastDate) {
            today -> currentStreak
            yesterdayString() -> currentStreak + 1
            else -> 1
        }

        val bestStreak = maxOf(prefs.getInt(KEY_BEST_STREAK_DAYS, 0), newStreak)

        editor
            .putString(KEY_LAST_ACTIVITY, today)
            .putInt(KEY_STREAK_DAYS, newStreak)
            .putInt(KEY_BEST_STREAK_DAYS, bestStreak)
            .apply()
    }

    /** Returns the current consecutive-day learning streak. */
    fun currentDayStreak(): Int {
        val lastDate = prefs.getString(KEY_LAST_ACTIVITY, null) ?: return 0
        val today = todayString()
        return when (lastDate) {
            today -> prefs.getInt(KEY_STREAK_DAYS, 0)
            yesterdayString() -> prefs.getInt(KEY_STREAK_DAYS, 0)
            else -> 0
        }
    }

    /** Returns the best consecutive-day learning streak ever achieved. */
    fun bestDayStreak(): Int = prefs.getInt(KEY_BEST_STREAK_DAYS, 0)

    // ── Reset ───────────────────────────────────────────────────────

    /** Clears all learning progress. */
    fun clearAllProgress() {
        prefs.edit().clear().apply()
    }

    /**
     * Exports all learning progress as a map of SharedPreferences entries.
     * Values are converted to strings for JSON serialization.
     */
    fun exportAll(): Map<String, String> {
        return prefs.all.mapValues { (_, value) -> value.toString() }
    }

    /**
     * Imports learning progress from a backup, merging with existing data
     * so that restoring never loses progress:
     * - Booleans (lesson flags): OR — once completed, stays completed.
     * - Integers (counters, streaks, bests): max(existing, imported).
     * - `last_activity_date`: keep the more recent date.
     * - Other strings: keep existing if present, else use imported.
     */
    fun importAll(entries: Map<String, String>) {
        val editor = prefs.edit()
        for ((key, value) in entries) {
            when {
                value == "true" || value == "false" -> {
                    val incoming = value.toBoolean()
                    if (incoming) {
                        editor.putBoolean(key, true)
                    }
                }
                value.toIntOrNull() != null -> {
                    val incoming = value.toInt()
                    val existing = prefs.getInt(key, 0)
                    editor.putInt(key, maxOf(existing, incoming))
                }
                key == KEY_LAST_ACTIVITY -> {
                    val existing = prefs.getString(key, null)
                    if (existing == null || value > existing) {
                        editor.putString(key, value)
                    }
                }
                else -> {
                    if (!prefs.contains(key)) {
                        editor.putString(key, value)
                    }
                }
            }
        }
        editor.apply()
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun yesterdayString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    companion object {
        private const val PREFS_NAME = "learn_section_progress"

        // Lesson keys
        private const val KEY_LESSON_COMPLETED = "lesson_done_"
        private const val KEY_LESSON_QUIZ = "lesson_quiz_"

        // Quiz keys
        private const val KEY_QUIZ_TOTAL = "quiz_total_"
        private const val KEY_QUIZ_CORRECT = "quiz_correct_"
        private const val KEY_QUIZ_STREAK = "quiz_streak_"
        private const val KEY_QUIZ_BEST = "quiz_best_"
        private const val KEY_QUIZ_TOTAL_ALL = "quiz_total_ALL"
        private const val KEY_QUIZ_CORRECT_ALL = "quiz_correct_ALL"
        private const val KEY_QUIZ_STREAK_ALL = "quiz_streak_ALL"
        private const val KEY_QUIZ_BEST_ALL = "quiz_best_ALL"

        // Interval keys
        private const val KEY_INTERVAL_TOTAL = "interval_total_"
        private const val KEY_INTERVAL_CORRECT = "interval_correct_"
        private const val KEY_INTERVAL_STREAK = "interval_streak_"
        private const val KEY_INTERVAL_BEST = "interval_best_"
        private const val KEY_INTERVAL_TOTAL_ALL = "interval_total_ALL"
        private const val KEY_INTERVAL_CORRECT_ALL = "interval_correct_ALL"
        private const val KEY_INTERVAL_STREAK_ALL = "interval_streak_ALL"
        private const val KEY_INTERVAL_BEST_ALL = "interval_best_ALL"

        // Note Quiz keys
        private const val KEY_NOTE_QUIZ_TOTAL = "note_quiz_total"
        private const val KEY_NOTE_QUIZ_CORRECT = "note_quiz_correct"
        private const val KEY_NOTE_QUIZ_STREAK = "note_quiz_streak"
        private const val KEY_NOTE_QUIZ_BEST = "note_quiz_best"

        // Chord Ear Training keys
        private const val KEY_CHORD_EAR_TOTAL = "chord_ear_total_"
        private const val KEY_CHORD_EAR_CORRECT = "chord_ear_correct_"
        private const val KEY_CHORD_EAR_STREAK = "chord_ear_streak_"
        private const val KEY_CHORD_EAR_BEST = "chord_ear_best_"
        private const val KEY_CHORD_EAR_TOTAL_ALL = "chord_ear_total_ALL"
        private const val KEY_CHORD_EAR_CORRECT_ALL = "chord_ear_correct_ALL"
        private const val KEY_CHORD_EAR_STREAK_ALL = "chord_ear_streak_ALL"
        private const val KEY_CHORD_EAR_BEST_ALL = "chord_ear_best_ALL"

        // Scale Practice keys
        private const val KEY_SCALE_PRACTICE_TOTAL = "scale_practice_total_"
        private const val KEY_SCALE_PRACTICE_CORRECT = "scale_practice_correct_"
        private const val KEY_SCALE_PRACTICE_STREAK = "scale_practice_streak_"
        private const val KEY_SCALE_PRACTICE_BEST = "scale_practice_best_"
        private const val KEY_SCALE_PRACTICE_TOTAL_ALL = "scale_practice_total_ALL"
        private const val KEY_SCALE_PRACTICE_CORRECT_ALL = "scale_practice_correct_ALL"
        private const val KEY_SCALE_PRACTICE_STREAK_ALL = "scale_practice_streak_ALL"
        private const val KEY_SCALE_PRACTICE_BEST_ALL = "scale_practice_best_ALL"

        // Streak keys
        private const val KEY_LAST_ACTIVITY = "last_activity_date"
        private const val KEY_STREAK_DAYS = "streak_days"
        private const val KEY_BEST_STREAK_DAYS = "best_streak_days"
    }
}

/**
 * Statistics snapshot for quiz or interval trainer performance.
 *
 * @property total Total questions attempted.
 * @property correct Total correct answers.
 * @property bestStreak Best consecutive correct streak.
 */
data class LearningStats(
    val total: Int,
    val correct: Int,
    val bestStreak: Int,
) {
    /** Accuracy as a percentage (0–100), or 0 if no attempts. */
    val accuracyPercent: Int
        get() = if (total > 0) (correct * 100 / total) else 0
}
