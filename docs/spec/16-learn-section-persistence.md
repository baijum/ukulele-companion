# Feature: Learn Section Persistence & Progress Tracking

**Status: PROPOSED**

## Summary

Add data persistence and a progress dashboard to the Learn section (Theory Lessons, Theory Quiz, Interval Trainer). Currently all scores, streaks, and lesson state reset when the app restarts. This feature saves user progress via SharedPreferences and introduces a visual progress dashboard to encourage continued learning.

## Motivation

- All quiz scores, streaks, and interval trainer stats are lost on app restart — users have no sense of long-term progress
- Leading music education apps (Solfy, Tomus, Yousician) prominently feature progress tracking; its absence makes the app feel like a toy rather than a learning tool
- The app already uses SharedPreferences for Favorites and Chord Sheets — the persistence pattern is well-established
- Completion tracking for Theory Lessons was proposed in `docs/spec/15-music-theory-learning.md` (Feature C) but never implemented
- Progress visibility is a key driver of habit formation and return visits

## Scope

### In Scope

- Persist Theory Quiz scores (correct/total, accuracy %, current streak, best streak) per category and overall
- Persist Interval Trainer scores (correct/total, accuracy %, streak, best streak) per difficulty level
- Track Theory Lesson completion (which lessons have been read, which mini quizzes answered correctly)
- A "Learning Progress" dashboard accessible from the navigation drawer or Learn section header
- Visual progress indicators on the lesson list (completion checkmarks, module progress bars)
- Reset option to clear all learning progress

### Out of Scope

- Cloud sync or account-based persistence (local SharedPreferences only)
- Social features (leaderboards, sharing progress)
- Analytics or telemetry sent externally
- Detailed per-question history logging

---

## Design

### 1. Data to Persist

#### Theory Lessons

| Key | Type | Description |
|-----|------|-------------|
| `lesson_completed_{lessonId}` | Boolean | Whether the lesson has been read |
| `lesson_quiz_passed_{lessonId}` | Boolean | Whether the mini quiz was answered correctly |

#### Theory Quiz

| Key | Type | Description |
|-----|------|-------------|
| `quiz_total_{category}` | Int | Total questions attempted |
| `quiz_correct_{category}` | Int | Total correct answers |
| `quiz_best_streak_{category}` | Int | Best consecutive correct streak |
| `quiz_total_all` | Int | Total questions across all categories |
| `quiz_correct_all` | Int | Total correct across all categories |
| `quiz_best_streak_all` | Int | Best streak across all categories |

#### Interval Trainer

| Key | Type | Description |
|-----|------|-------------|
| `interval_total_{level}` | Int | Total questions at this level |
| `interval_correct_{level}` | Int | Correct answers at this level |
| `interval_best_streak_{level}` | Int | Best streak at this level |
| `interval_total_all` | Int | Total across all levels |
| `interval_correct_all` | Int | Correct across all levels |
| `interval_best_streak_all` | Int | Best streak across all levels |

#### General

| Key | Type | Description |
|-----|------|-------------|
| `learn_first_activity_date` | Long | Timestamp of first learning activity |
| `learn_last_activity_date` | Long | Timestamp of most recent activity |
| `learn_days_active` | Int | Number of distinct days with activity |
| `learn_current_streak_days` | Int | Consecutive days with at least one activity |
| `learn_best_streak_days` | Int | Best consecutive day streak |

### 2. Repository Pattern

```kotlin
class LearningProgressRepository(context: Context) {
    private val prefs = context.getSharedPreferences(
        "learning_progress", Context.MODE_PRIVATE
    )

    // Theory Lessons
    fun markLessonCompleted(lessonId: String)
    fun isLessonCompleted(lessonId: String): Boolean
    fun markLessonQuizPassed(lessonId: String)
    fun isLessonQuizPassed(lessonId: String): Boolean
    fun completedLessonCount(): Int
    fun totalLessonCount(): Int

    // Theory Quiz
    fun recordQuizAnswer(category: QuizCategory, correct: Boolean)
    fun quizStats(category: QuizCategory?): QuizStats
    fun quizBestStreak(category: QuizCategory?): Int

    // Interval Trainer
    fun recordIntervalAnswer(level: Int, correct: Boolean)
    fun intervalStats(level: Int?): IntervalStats
    fun intervalBestStreak(level: Int?): Int

    // Daily Streak
    fun recordActivity()
    fun currentDayStreak(): Int
    fun bestDayStreak(): Int

    // Reset
    fun clearAllProgress()
}

data class QuizStats(
    val total: Int,
    val correct: Int,
    val accuracy: Float,  // 0.0–1.0
    val bestStreak: Int,
)

data class IntervalStats(
    val total: Int,
    val correct: Int,
    val accuracy: Float,
    val bestStreak: Int,
)
```

### 3. Progress Dashboard UI

A "Learning Progress" screen showing:

- **Overall summary card**: Total questions answered, overall accuracy, days active, current day streak
- **Theory Lessons progress**: Module-by-module progress bars (e.g., "Notes & Pitch: 2/2 complete"), overall completion percentage
- **Theory Quiz stats**: Per-category accuracy breakdown (bar chart or horizontal bars), best streaks
- **Interval Trainer stats**: Per-level accuracy breakdown, best streaks, current unlocked level
- **Reset button** at the bottom with confirmation dialog

### 4. Visual Indicators on Existing Screens

#### Theory Lessons List
- Checkmark icon on completed lessons
- "Quiz passed" badge on lessons where the mini quiz was answered correctly
- Module header shows "2/3 complete" count
- Overall progress bar at the top of the lesson list

#### Theory Quiz
- "All-time" stats section alongside the current session stats
- Best streak shown with a trophy/star icon
- Per-category accuracy displayed on the category filter chips

#### Interval Trainer
- "All-time" stats alongside current session stats
- Best streak display
- Per-level accuracy shown on difficulty filter chips

---

## Technical Approach

### Files

- **New:** `data/LearningProgressRepository.kt` — SharedPreferences persistence for all Learn section data
- **Modify:** `ui/TheoryLessonsView.kt` — Add completion checkmarks, module progress bars, persist quiz results
- **Modify:** `ui/TheoryQuizView.kt` — Persist scores, show all-time stats alongside session stats
- **Modify:** `ui/IntervalTrainerView.kt` — Persist scores, show all-time stats
- **New:** `ui/LearningProgressView.kt` — Progress dashboard screen
- **Modify:** `ui/FretboardScreen.kt` — Add "Learning Progress" navigation item or header in Learn section

### Implementation Notes

- Follow the same SharedPreferences pattern used by `FavoritesRepository` and `ChordSheetRepository`
- Daily streak calculation: store `learn_last_activity_date` as epoch millis, compare calendar day to determine if streak continues
- Use `remember` + `LaunchedEffect` in Compose to load persisted stats without blocking the UI
- Progress dashboard should use `LazyColumn` for scrollability

### Migration

- First launch after update: all progress starts at zero (clean slate)
- No migration needed from previous versions since no learning data was previously stored

---

## Effort Estimate

- **Complexity**: Low–Medium
- **Estimated time**: 2–3 days
- **APK size impact**: Negligible

## Dependencies

- None — all existing features remain functional without this; this is purely additive

## Testing Strategy

- Verify scores persist across app restart (force stop and relaunch)
- Verify daily streak increments correctly and resets after a missed day
- Verify completion checkmarks appear on lessons after reading
- Verify reset clears all data and returns to initial state
- Verify progress dashboard shows accurate aggregates
