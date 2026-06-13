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
    fun recordQuizAnswer(category: QuizGenerator.QuizCategory, correct: Boolean) =
        recordAnswer("quiz", category.name, correct)

    /** Returns quiz stats for a category, or overall if null. */
    fun quizStats(category: QuizGenerator.QuizCategory? = null): LearningStats =
        stats("quiz", category?.name)

    // ── Interval Trainer ────────────────────────────────────────────

    /**
     * Records an interval trainer answer for the given level.
     *
     * Updates both per-level and overall stats.
     */
    fun recordIntervalAnswer(level: Int, correct: Boolean) =
        recordAnswer("interval", level.coerceIn(1, 4).toString(), correct)

    /** Returns interval trainer stats for a level (1–4), or overall if null. */
    fun intervalStats(level: Int? = null): LearningStats =
        stats("interval", level?.coerceIn(1, 4)?.toString())

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
    fun recordChordEarAnswer(level: Int, correct: Boolean) =
        recordAnswer("chord_ear", level.coerceIn(1, 4).toString(), correct)

    /** Returns chord ear training stats for a level (1–4), or overall if null. */
    fun chordEarStats(level: Int? = null): LearningStats =
        stats("chord_ear", level?.coerceIn(1, 4)?.toString())

    // ── Scale Practice ─────────────────────────────────────────────

    /**
     * Records a scale practice answer (quiz or ear training).
     *
     * @param mode "quiz" or "ear" to distinguish the two practice types.
     * @param correct Whether the answer was correct.
     */
    fun recordScalePracticeAnswer(mode: String, correct: Boolean) =
        recordAnswer("scale_practice", mode.lowercase(), correct)

    /** Returns scale practice stats for a mode ("quiz"/"ear"), or overall if null. */
    fun scalePracticeStats(mode: String? = null): LearningStats =
        stats("scale_practice", mode?.lowercase())

    // ── Generic record/stats helpers ─────────────────────────────────

    private fun recordAnswer(prefix: String, subcategory: String, correct: Boolean) {
        val editor = prefs.edit()

        editor.putInt("${prefix}_total_$subcategory", prefs.getInt("${prefix}_total_$subcategory", 0) + 1)
        editor.putInt("${prefix}_total_ALL", prefs.getInt("${prefix}_total_ALL", 0) + 1)

        if (correct) {
            editor.putInt("${prefix}_correct_$subcategory", prefs.getInt("${prefix}_correct_$subcategory", 0) + 1)
            editor.putInt("${prefix}_correct_ALL", prefs.getInt("${prefix}_correct_ALL", 0) + 1)
            val newStreak = prefs.getInt("${prefix}_streak_$subcategory", 0) + 1
            editor.putInt("${prefix}_streak_$subcategory", newStreak)
            if (newStreak > prefs.getInt("${prefix}_best_$subcategory", 0)) {
                editor.putInt("${prefix}_best_$subcategory", newStreak)
            }
            val newOverallStreak = prefs.getInt("${prefix}_streak_ALL", 0) + 1
            editor.putInt("${prefix}_streak_ALL", newOverallStreak)
            if (newOverallStreak > prefs.getInt("${prefix}_best_ALL", 0)) {
                editor.putInt("${prefix}_best_ALL", newOverallStreak)
            }
        } else {
            editor.putInt("${prefix}_streak_$subcategory", 0)
            editor.putInt("${prefix}_streak_ALL", 0)
        }

        applyActivityUpdate(editor)
    }

    private fun stats(prefix: String, subcategory: String?): LearningStats {
        val suffix = subcategory ?: "ALL"
        return LearningStats(
            total = prefs.getInt("${prefix}_total_$suffix", 0),
            correct = prefs.getInt("${prefix}_correct_$suffix", 0),
            bestStreak = prefs.getInt("${prefix}_best_$suffix", 0),
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

        // Note Quiz keys (single-bucket, not using generic helper)
        private const val KEY_NOTE_QUIZ_TOTAL = "note_quiz_total"
        private const val KEY_NOTE_QUIZ_CORRECT = "note_quiz_correct"
        private const val KEY_NOTE_QUIZ_STREAK = "note_quiz_streak"
        private const val KEY_NOTE_QUIZ_BEST = "note_quiz_best"

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
