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

    private let repository: LearnRepository

    init(repository: LearnRepository = LearnRepository()) {
        self.repository = repository
    }

    // MARK: - Theory Lessons

    func markLessonCompleted(_ lessonId: String) {
        repository.setBool(true, forKey: "lesson_done_\(lessonId)")
        recordActivity()
        stateVersion += 1
    }

    func isLessonCompleted(_ lessonId: String) -> Bool {
        repository.bool(forKey: "lesson_done_\(lessonId)")
    }

    func markLessonQuizPassed(_ lessonId: String) {
        repository.setBool(true, forKey: "lesson_quiz_\(lessonId)")
        recordActivity()
        stateVersion += 1
    }

    func isLessonQuizPassed(_ lessonId: String) -> Bool {
        repository.bool(forKey: "lesson_quiz_\(lessonId)")
    }

    func completedLessonCount() -> Int {
        let all = TheoryLessons.shared.ALL.asArray(of: TheoryLesson.self)
        return all.filter { isLessonCompleted($0.id) }.count
    }

    func passedQuizCount() -> Int {
        let all = TheoryLessons.shared.ALL.asArray(of: TheoryLesson.self)
        return all.filter { isLessonQuizPassed($0.id) }.count
    }

    // MARK: - Theory Quiz

    func recordQuizAnswer(category: QuizGenerator.QuizCategory, correct: Bool) {
        let catKey = category.name
        repository.incrementInt("quiz_total_\(catKey)")
        repository.incrementInt("quiz_total_ALL")
        if correct {
            repository.incrementInt("quiz_correct_\(catKey)")
            repository.incrementInt("quiz_correct_ALL")
            let newStreak = repository.incrementInt("quiz_streak_\(catKey)")
            repository.updateBestStreak("quiz_best_\(catKey)", currentStreak: newStreak)
            let newOverallStreak = repository.incrementInt("quiz_streak_ALL")
            repository.updateBestStreak("quiz_best_ALL", currentStreak: newOverallStreak)
        } else {
            repository.setInteger(0, forKey: "quiz_streak_\(catKey)")
            repository.setInteger(0, forKey: "quiz_streak_ALL")
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
            total: repository.integer(forKey: totalKey),
            correct: repository.integer(forKey: correctKey),
            bestStreak: repository.integer(forKey: bestKey)
        )
    }

    // MARK: - Interval Trainer

    func recordIntervalAnswer(level: Int, correct: Bool) {
        let lvl = min(max(level, 1), 4)
        repository.incrementInt("interval_total_\(lvl)")
        repository.incrementInt("interval_total_ALL")
        if correct {
            repository.incrementInt("interval_correct_\(lvl)")
            repository.incrementInt("interval_correct_ALL")
            let newStreak = repository.incrementInt("interval_streak_\(lvl)")
            repository.updateBestStreak("interval_best_\(lvl)", currentStreak: newStreak)
            let newOverallStreak = repository.incrementInt("interval_streak_ALL")
            repository.updateBestStreak("interval_best_ALL", currentStreak: newOverallStreak)
        } else {
            repository.setInteger(0, forKey: "interval_streak_\(lvl)")
            repository.setInteger(0, forKey: "interval_streak_ALL")
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
            total: repository.integer(forKey: totalKey),
            correct: repository.integer(forKey: correctKey),
            bestStreak: repository.integer(forKey: bestKey)
        )
    }

    // MARK: - Note Quiz

    func recordNoteQuizAnswer(correct: Bool) {
        repository.incrementInt("note_quiz_total")
        if correct {
            repository.incrementInt("note_quiz_correct")
            let newStreak = repository.incrementInt("note_quiz_streak")
            repository.updateBestStreak("note_quiz_best", currentStreak: newStreak)
        } else {
            repository.setInteger(0, forKey: "note_quiz_streak")
        }
        recordActivity()
        stateVersion += 1
    }

    func noteQuizStats() -> LearningStats {
        LearningStats(
            total: repository.integer(forKey: "note_quiz_total"),
            correct: repository.integer(forKey: "note_quiz_correct"),
            bestStreak: repository.integer(forKey: "note_quiz_best")
        )
    }

    // MARK: - Chord Ear Training

    func recordChordEarAnswer(level: Int, correct: Bool) {
        let lvl = min(max(level, 1), 4)
        repository.incrementInt("chord_ear_total_\(lvl)")
        repository.incrementInt("chord_ear_total_ALL")
        if correct {
            repository.incrementInt("chord_ear_correct_\(lvl)")
            repository.incrementInt("chord_ear_correct_ALL")
            let newStreak = repository.incrementInt("chord_ear_streak_\(lvl)")
            repository.updateBestStreak("chord_ear_best_\(lvl)", currentStreak: newStreak)
            let newOverallStreak = repository.incrementInt("chord_ear_streak_ALL")
            repository.updateBestStreak("chord_ear_best_ALL", currentStreak: newOverallStreak)
        } else {
            repository.setInteger(0, forKey: "chord_ear_streak_\(lvl)")
            repository.setInteger(0, forKey: "chord_ear_streak_ALL")
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
            total: repository.integer(forKey: totalKey),
            correct: repository.integer(forKey: correctKey),
            bestStreak: repository.integer(forKey: bestKey)
        )
    }

    // MARK: - Scale Practice

    func recordScalePracticeAnswer(mode: String, correct: Bool) {
        let m = mode.lowercased()
        repository.incrementInt("scale_practice_total_\(m)")
        repository.incrementInt("scale_practice_total_ALL")
        if correct {
            repository.incrementInt("scale_practice_correct_\(m)")
            repository.incrementInt("scale_practice_correct_ALL")
            let newStreak = repository.incrementInt("scale_practice_streak_\(m)")
            repository.updateBestStreak("scale_practice_best_\(m)", currentStreak: newStreak)
            let newOverallStreak = repository.incrementInt("scale_practice_streak_ALL")
            repository.updateBestStreak("scale_practice_best_ALL", currentStreak: newOverallStreak)
        } else {
            repository.setInteger(0, forKey: "scale_practice_streak_\(m)")
            repository.setInteger(0, forKey: "scale_practice_streak_ALL")
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
            total: repository.integer(forKey: totalKey),
            correct: repository.integer(forKey: correctKey),
            bestStreak: repository.integer(forKey: bestKey)
        )
    }

    // MARK: - Daily Streak

    func recordActivity() {
        let today = todayString()
        let lastDate = repository.string(forKey: "last_activity_date")
        let currentStreak = repository.integer(forKey: "streak_days")

        let newStreak: Int
        if lastDate == today {
            newStreak = currentStreak
        } else if lastDate == yesterdayString() {
            newStreak = currentStreak + 1
        } else {
            newStreak = 1
        }

        let bestStreak = max(repository.integer(forKey: "best_streak_days"), newStreak)

        repository.setString(today, forKey: "last_activity_date")
        repository.setInteger(newStreak, forKey: "streak_days")
        repository.setInteger(bestStreak, forKey: "best_streak_days")
    }

    func currentDayStreak() -> Int {
        guard let lastDate = repository.string(forKey: "last_activity_date") else { return 0 }
        let today = todayString()
        if lastDate == today || lastDate == yesterdayString() {
            return repository.integer(forKey: "streak_days")
        }
        return 0
    }

    func bestDayStreak() -> Int {
        repository.integer(forKey: "best_streak_days")
    }

    // MARK: - Achievements

    var unlockedAchievementIds: Set<String> {
        get {
            Set(repository.stringArray(forKey: "unlocked_achievements") ?? [])
        }
        set {
            repository.setStringArray(Array(newValue), forKey: "unlocked_achievements")
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
        repository.clearAll()
        stateVersion += 1
    }

    // MARK: - Backup Export/Import

    func exportProgress() -> [String: Any] {
        let lessons = TheoryLessons.shared.ALL.asArray(of: TheoryLesson.self)
        let catNames = [
            QuizGenerator.QuizCategory.intervals,
            .chords, .keys, .scales, .progressions
        ].map { $0.name } + ["ALL"]

        return repository.exportProgress(
            lessonIds: lessons.map { $0.id },
            quizCategories: catNames,
            intervalSuffixes: ["1", "2", "3", "4", "ALL"],
            chordEarSuffixes: ["1", "2", "3", "4", "ALL"],
            scaleModes: ["quiz", "ear", "ALL"]
        )
    }

    func importProgress(_ data: [String: Any]) {
        repository.importProgress(data)
        stateVersion += 1
    }

    // MARK: - Helpers

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
