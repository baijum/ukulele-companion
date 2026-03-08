import Foundation
import shared

struct LearningStats {
    let total: Int
    let correct: Int
    let bestStreak: Int

    var accuracyPercent: Int {
        total > 0 ? (correct * 100 / total) : 0
    }
}

@MainActor
final class LearnViewModel: ObservableObject {
    @Published var stateVersion: Int = 0

    private let defaults: UserDefaults

    init() {
        self.defaults = UserDefaults(suiteName: "learn_progress") ?? .standard
    }

    // MARK: - Theory Lessons

    func markLessonCompleted(_ lessonId: String) {
        defaults.set(true, forKey: "lesson_done_\(lessonId)")
        recordActivity()
        stateVersion += 1
    }

    func isLessonCompleted(_ lessonId: String) -> Bool {
        defaults.bool(forKey: "lesson_done_\(lessonId)")
    }

    func markLessonQuizPassed(_ lessonId: String) {
        defaults.set(true, forKey: "lesson_quiz_\(lessonId)")
        recordActivity()
        stateVersion += 1
    }

    func isLessonQuizPassed(_ lessonId: String) -> Bool {
        defaults.bool(forKey: "lesson_quiz_\(lessonId)")
    }

    func completedLessonCount() -> Int {
        let all = TheoryLessons.shared.ALL as! [TheoryLesson]
        return all.filter { isLessonCompleted($0.id) }.count
    }

    func passedQuizCount() -> Int {
        let all = TheoryLessons.shared.ALL as! [TheoryLesson]
        return all.filter { isLessonQuizPassed($0.id) }.count
    }

    // MARK: - Theory Quiz

    func recordQuizAnswer(category: QuizGenerator.QuizCategory, correct: Bool) {
        let catKey = category.name
        incrementInt("quiz_total_\(catKey)")
        incrementInt("quiz_total_ALL")
        if correct {
            incrementInt("quiz_correct_\(catKey)")
            incrementInt("quiz_correct_ALL")
            let newStreak = incrementInt("quiz_streak_\(catKey)")
            updateBestStreak("quiz_best_\(catKey)", currentStreak: newStreak)
            let newOverallStreak = incrementInt("quiz_streak_ALL")
            updateBestStreak("quiz_best_ALL", currentStreak: newOverallStreak)
        } else {
            defaults.set(0, forKey: "quiz_streak_\(catKey)")
            defaults.set(0, forKey: "quiz_streak_ALL")
        }
        recordActivity()
        stateVersion += 1
    }

    func quizStats(category: QuizGenerator.QuizCategory? = nil) -> LearningStats {
        let suffix = category?.name ?? "ALL"
        let totalKey = category != nil ? "quiz_total_\(suffix)" : "quiz_total_ALL"
        let correctKey = category != nil ? "quiz_correct_\(suffix)" : "quiz_correct_ALL"
        let bestKey = category != nil ? "quiz_best_\(suffix)" : "quiz_best_ALL"
        return LearningStats(
            total: defaults.integer(forKey: totalKey),
            correct: defaults.integer(forKey: correctKey),
            bestStreak: defaults.integer(forKey: bestKey)
        )
    }

    // MARK: - Interval Trainer

    func recordIntervalAnswer(level: Int, correct: Bool) {
        let lvl = min(max(level, 1), 4)
        incrementInt("interval_total_\(lvl)")
        incrementInt("interval_total_ALL")
        if correct {
            incrementInt("interval_correct_\(lvl)")
            incrementInt("interval_correct_ALL")
            let newStreak = incrementInt("interval_streak_\(lvl)")
            updateBestStreak("interval_best_\(lvl)", currentStreak: newStreak)
            let newOverallStreak = incrementInt("interval_streak_ALL")
            updateBestStreak("interval_best_ALL", currentStreak: newOverallStreak)
        } else {
            defaults.set(0, forKey: "interval_streak_\(lvl)")
            defaults.set(0, forKey: "interval_streak_ALL")
        }
        recordActivity()
        stateVersion += 1
    }

    func intervalStats(level: Int? = nil) -> LearningStats {
        let suffix = level.map { String(min(max($0, 1), 4)) } ?? "ALL"
        let totalKey = level != nil ? "interval_total_\(suffix)" : "interval_total_ALL"
        let correctKey = level != nil ? "interval_correct_\(suffix)" : "interval_correct_ALL"
        let bestKey = level != nil ? "interval_best_\(suffix)" : "interval_best_ALL"
        return LearningStats(
            total: defaults.integer(forKey: totalKey),
            correct: defaults.integer(forKey: correctKey),
            bestStreak: defaults.integer(forKey: bestKey)
        )
    }

    // MARK: - Note Quiz

    func recordNoteQuizAnswer(correct: Bool) {
        incrementInt("note_quiz_total")
        if correct {
            incrementInt("note_quiz_correct")
            let newStreak = incrementInt("note_quiz_streak")
            updateBestStreak("note_quiz_best", currentStreak: newStreak)
        } else {
            defaults.set(0, forKey: "note_quiz_streak")
        }
        recordActivity()
        stateVersion += 1
    }

    func noteQuizStats() -> LearningStats {
        LearningStats(
            total: defaults.integer(forKey: "note_quiz_total"),
            correct: defaults.integer(forKey: "note_quiz_correct"),
            bestStreak: defaults.integer(forKey: "note_quiz_best")
        )
    }

    // MARK: - Chord Ear Training

    func recordChordEarAnswer(level: Int, correct: Bool) {
        let lvl = min(max(level, 1), 4)
        incrementInt("chord_ear_total_\(lvl)")
        incrementInt("chord_ear_total_ALL")
        if correct {
            incrementInt("chord_ear_correct_\(lvl)")
            incrementInt("chord_ear_correct_ALL")
            let newStreak = incrementInt("chord_ear_streak_\(lvl)")
            updateBestStreak("chord_ear_best_\(lvl)", currentStreak: newStreak)
            let newOverallStreak = incrementInt("chord_ear_streak_ALL")
            updateBestStreak("chord_ear_best_ALL", currentStreak: newOverallStreak)
        } else {
            defaults.set(0, forKey: "chord_ear_streak_\(lvl)")
            defaults.set(0, forKey: "chord_ear_streak_ALL")
        }
        recordActivity()
        stateVersion += 1
    }

    func chordEarStats(level: Int? = nil) -> LearningStats {
        let suffix = level.map { String(min(max($0, 1), 4)) } ?? "ALL"
        let totalKey = level != nil ? "chord_ear_total_\(suffix)" : "chord_ear_total_ALL"
        let correctKey = level != nil ? "chord_ear_correct_\(suffix)" : "chord_ear_correct_ALL"
        let bestKey = level != nil ? "chord_ear_best_\(suffix)" : "chord_ear_best_ALL"
        return LearningStats(
            total: defaults.integer(forKey: totalKey),
            correct: defaults.integer(forKey: correctKey),
            bestStreak: defaults.integer(forKey: bestKey)
        )
    }

    // MARK: - Scale Practice

    func recordScalePracticeAnswer(mode: String, correct: Bool) {
        let m = mode.lowercased()
        incrementInt("scale_practice_total_\(m)")
        incrementInt("scale_practice_total_ALL")
        if correct {
            incrementInt("scale_practice_correct_\(m)")
            incrementInt("scale_practice_correct_ALL")
            let newStreak = incrementInt("scale_practice_streak_\(m)")
            updateBestStreak("scale_practice_best_\(m)", currentStreak: newStreak)
            let newOverallStreak = incrementInt("scale_practice_streak_ALL")
            updateBestStreak("scale_practice_best_ALL", currentStreak: newOverallStreak)
        } else {
            defaults.set(0, forKey: "scale_practice_streak_\(m)")
            defaults.set(0, forKey: "scale_practice_streak_ALL")
        }
        recordActivity()
        stateVersion += 1
    }

    func scalePracticeStats(mode: String? = nil) -> LearningStats {
        let suffix = mode?.lowercased() ?? "ALL"
        let totalKey = mode != nil ? "scale_practice_total_\(suffix)" : "scale_practice_total_ALL"
        let correctKey = mode != nil ? "scale_practice_correct_\(suffix)" : "scale_practice_correct_ALL"
        let bestKey = mode != nil ? "scale_practice_best_\(suffix)" : "scale_practice_best_ALL"
        return LearningStats(
            total: defaults.integer(forKey: totalKey),
            correct: defaults.integer(forKey: correctKey),
            bestStreak: defaults.integer(forKey: bestKey)
        )
    }

    // MARK: - Daily Streak

    func recordActivity() {
        let today = todayString()
        let lastDate = defaults.string(forKey: "last_activity_date")
        let currentStreak = defaults.integer(forKey: "streak_days")

        let newStreak: Int
        if lastDate == today {
            newStreak = currentStreak
        } else if lastDate == yesterdayString() {
            newStreak = currentStreak + 1
        } else {
            newStreak = 1
        }

        let bestStreak = max(defaults.integer(forKey: "best_streak_days"), newStreak)

        defaults.set(today, forKey: "last_activity_date")
        defaults.set(newStreak, forKey: "streak_days")
        defaults.set(bestStreak, forKey: "best_streak_days")
    }

    func currentDayStreak() -> Int {
        guard let lastDate = defaults.string(forKey: "last_activity_date") else { return 0 }
        let today = todayString()
        if lastDate == today || lastDate == yesterdayString() {
            return defaults.integer(forKey: "streak_days")
        }
        return 0
    }

    func bestDayStreak() -> Int {
        defaults.integer(forKey: "best_streak_days")
    }

    // MARK: - Achievements

    var unlockedAchievementIds: Set<String> {
        get {
            Set(defaults.stringArray(forKey: "unlocked_achievements") ?? [])
        }
        set {
            defaults.set(Array(newValue), forKey: "unlocked_achievements")
            stateVersion += 1
        }
    }

    func unlockAchievement(_ id: String) {
        var ids = unlockedAchievementIds
        ids.insert(id)
        unlockedAchievementIds = ids
    }

    // MARK: - Reset

    func clearAllProgress() {
        let allKeys = defaults.dictionaryRepresentation().keys
        for key in allKeys {
            defaults.removeObject(forKey: key)
        }
        stateVersion += 1
    }

    // MARK: - Backup Export/Import

    func exportProgress() -> [String: Any] {
        var result: [String: Any] = [:]

        // Theory lessons
        let lessons = TheoryLessons.shared.ALL as! [TheoryLesson]
        for lesson in lessons {
            let doneKey = "lesson_done_\(lesson.id)"
            let quizKey = "lesson_quiz_\(lesson.id)"
            if defaults.bool(forKey: doneKey) { result[doneKey] = true }
            if defaults.bool(forKey: quizKey) { result[quizKey] = true }
        }

        // Quiz stats
        let catNames = [
            QuizGenerator.QuizCategory.intervals,
            .chords, .keys, .scales, .progressions
        ].map { $0.name } + ["ALL"]
        for cat in catNames {
            for prefix in ["quiz_total_", "quiz_correct_", "quiz_streak_", "quiz_best_"] {
                let key = "\(prefix)\(cat)"
                let val = defaults.integer(forKey: key)
                if val != 0 { result[key] = val }
            }
        }

        // Interval stats
        let intervalSuffixes = ["1", "2", "3", "4", "ALL"]
        for suffix in intervalSuffixes {
            for prefix in ["interval_total_", "interval_correct_", "interval_streak_", "interval_best_"] {
                let key = "\(prefix)\(suffix)"
                let val = defaults.integer(forKey: key)
                if val != 0 { result[key] = val }
            }
        }

        // Note quiz stats
        for key in ["note_quiz_total", "note_quiz_correct", "note_quiz_streak", "note_quiz_best"] {
            let val = defaults.integer(forKey: key)
            if val != 0 { result[key] = val }
        }

        // Chord ear training stats
        let chordEarSuffixes = ["1", "2", "3", "4", "ALL"]
        for suffix in chordEarSuffixes {
            for prefix in ["chord_ear_total_", "chord_ear_correct_", "chord_ear_streak_", "chord_ear_best_"] {
                let key = "\(prefix)\(suffix)"
                let val = defaults.integer(forKey: key)
                if val != 0 { result[key] = val }
            }
        }

        // Scale practice stats
        let scaleModes = ["quiz", "ear", "ALL"]
        for mode in scaleModes {
            for prefix in ["scale_practice_total_", "scale_practice_correct_", "scale_practice_streak_", "scale_practice_best_"] {
                let key = "\(prefix)\(mode)"
                let val = defaults.integer(forKey: key)
                if val != 0 { result[key] = val }
            }
        }

        // Daily streak
        if let lastDate = defaults.string(forKey: "last_activity_date") {
            result["last_activity_date"] = lastDate
        }
        let streakDays = defaults.integer(forKey: "streak_days")
        if streakDays != 0 { result["streak_days"] = streakDays }
        let bestStreakDays = defaults.integer(forKey: "best_streak_days")
        if bestStreakDays != 0 { result["best_streak_days"] = bestStreakDays }

        // Achievements
        if let achievements = defaults.stringArray(forKey: "unlocked_achievements"), !achievements.isEmpty {
            result["unlocked_achievements"] = achievements
        }

        return result
    }

    func importProgress(_ data: [String: Any]) {
        for (key, value) in data {
            if key == "unlocked_achievements" {
                // Union merge for achievements
                if let incoming = value as? [String] {
                    let existing = Set(defaults.stringArray(forKey: "unlocked_achievements") ?? [])
                    let merged = existing.union(incoming)
                    defaults.set(Array(merged), forKey: "unlocked_achievements")
                }
            } else if key == "last_activity_date" {
                // Keep the more recent date
                if let incoming = value as? String {
                    let existing = defaults.string(forKey: "last_activity_date")
                    if existing == nil || incoming > existing! {
                        defaults.set(incoming, forKey: "last_activity_date")
                    }
                }
            } else if let incoming = value as? Bool {
                // lesson_done_*, lesson_quiz_*: OR merge (once done, stays done)
                if incoming { defaults.set(true, forKey: key) }
            } else if let incoming = value as? Int {
                // For stats: use max() so we never lose progress
                let existing = defaults.integer(forKey: key)
                if incoming > existing {
                    defaults.set(incoming, forKey: key)
                }
            }
        }
        stateVersion += 1
    }

    // MARK: - Helpers

    @discardableResult
    private func incrementInt(_ key: String) -> Int {
        let newValue = defaults.integer(forKey: key) + 1
        defaults.set(newValue, forKey: key)
        return newValue
    }

    private func updateBestStreak(_ bestKey: String, currentStreak: Int) {
        let best = defaults.integer(forKey: bestKey)
        if currentStreak > best {
            defaults.set(currentStreak, forKey: bestKey)
        }
    }

    private func todayString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter.string(from: Date())
    }

    private func yesterdayString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        let yesterday = Calendar.current.date(byAdding: .day, value: -1, to: Date())!
        return formatter.string(from: yesterday)
    }
}
