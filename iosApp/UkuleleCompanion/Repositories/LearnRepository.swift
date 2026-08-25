import Foundation

final class LearnRepository {
    private let defaults: UserDefaults

    init() {
        self.defaults = UserDefaults(suiteName: "learn_progress") ?? .standard
    }

    // MARK: - Primitive accessors

    func bool(forKey key: String) -> Bool {
        defaults.bool(forKey: key)
    }

    func setBool(_ value: Bool, forKey key: String) {
        defaults.set(value, forKey: key)
    }

    func integer(forKey key: String) -> Int {
        defaults.integer(forKey: key)
    }

    func setInteger(_ value: Int, forKey key: String) {
        defaults.set(value, forKey: key)
    }

    func string(forKey key: String) -> String? {
        defaults.string(forKey: key)
    }

    func setString(_ value: String, forKey key: String) {
        defaults.set(value, forKey: key)
    }

    func stringArray(forKey key: String) -> [String]? {
        defaults.stringArray(forKey: key)
    }

    func setStringArray(_ value: [String], forKey key: String) {
        defaults.set(value, forKey: key)
    }

    @discardableResult
    func incrementInt(_ key: String) -> Int {
        let newValue = defaults.integer(forKey: key) + 1
        defaults.set(newValue, forKey: key)
        return newValue
    }

    func updateBestStreak(_ bestKey: String, currentStreak: Int) {
        let best = defaults.integer(forKey: bestKey)
        if currentStreak > best {
            defaults.set(currentStreak, forKey: bestKey)
        }
    }

    func clearAll() {
        // Preserve unlocked achievements: they live in this same suite but are
        // not "learning progress". Android keeps trophies across Reset All
        // Progress (achievements live in a separate store there), so read the
        // list, clear the suite, then write it back to match. See issue #607.
        let preservedAchievements = defaults.stringArray(forKey: "unlocked_achievements")
        let allKeys = defaults.dictionaryRepresentation().keys
        for key in allKeys {
            defaults.removeObject(forKey: key)
        }
        if let preservedAchievements, !preservedAchievements.isEmpty {
            defaults.set(preservedAchievements, forKey: "unlocked_achievements")
        }
    }

    // MARK: - Export/Import

    func exportProgress(lessonIds: [String], quizCategories: [String],
                        intervalSuffixes: [String], chordEarSuffixes: [String],
                        scaleModes: [String]) -> [String: Any] {
        var result: [String: Any] = [:]

        for lessonId in lessonIds {
            let doneKey = "lesson_done_\(lessonId)"
            let quizKey = "lesson_quiz_\(lessonId)"
            if defaults.bool(forKey: doneKey) { result[doneKey] = true }
            if defaults.bool(forKey: quizKey) { result[quizKey] = true }
        }

        for cat in quizCategories {
            for prefix in ["quiz_total_", "quiz_correct_", "quiz_streak_", "quiz_best_"] {
                let key = "\(prefix)\(cat)"
                let val = defaults.integer(forKey: key)
                if val != 0 { result[key] = val }
            }
        }

        for suffix in intervalSuffixes {
            for prefix in ["interval_total_", "interval_correct_", "interval_streak_", "interval_best_"] {
                let key = "\(prefix)\(suffix)"
                let val = defaults.integer(forKey: key)
                if val != 0 { result[key] = val }
            }
        }

        for key in ["note_quiz_total", "note_quiz_correct", "note_quiz_streak", "note_quiz_best"] {
            let val = defaults.integer(forKey: key)
            if val != 0 { result[key] = val }
        }

        for suffix in chordEarSuffixes {
            for prefix in ["chord_ear_total_", "chord_ear_correct_", "chord_ear_streak_", "chord_ear_best_"] {
                let key = "\(prefix)\(suffix)"
                let val = defaults.integer(forKey: key)
                if val != 0 { result[key] = val }
            }
        }

        for mode in scaleModes {
            for prefix in ["scale_practice_total_", "scale_practice_correct_", "scale_practice_streak_", "scale_practice_best_"] {
                let key = "\(prefix)\(mode)"
                let val = defaults.integer(forKey: key)
                if val != 0 { result[key] = val }
            }
        }

        if let lastDate = defaults.string(forKey: "last_activity_date") {
            result["last_activity_date"] = lastDate
        }
        let streakDays = defaults.integer(forKey: "streak_days")
        if streakDays != 0 { result["streak_days"] = streakDays }
        let bestStreakDays = defaults.integer(forKey: "best_streak_days")
        if bestStreakDays != 0 { result["best_streak_days"] = bestStreakDays }

        if let achievements = defaults.stringArray(forKey: "unlocked_achievements"), !achievements.isEmpty {
            result["unlocked_achievements"] = achievements
        }

        return result
    }

    func importProgress(_ data: [String: Any]) {
        for (key, value) in data {
            if key == "unlocked_achievements" {
                if let incoming = value as? [String] {
                    let existing = Set(defaults.stringArray(forKey: "unlocked_achievements") ?? [])
                    let merged = existing.union(incoming)
                    defaults.set(Array(merged), forKey: "unlocked_achievements")
                }
            } else if key == "last_activity_date" {
                if let incoming = value as? String {
                    let existing = defaults.string(forKey: "last_activity_date")
                    if existing == nil || incoming > existing! {
                        defaults.set(incoming, forKey: "last_activity_date")
                    }
                }
            } else if let incoming = value as? Bool {
                if incoming { defaults.set(true, forKey: key) }
            } else if let incoming = value as? Int {
                let existing = defaults.integer(forKey: key)
                if incoming > existing {
                    defaults.set(incoming, forKey: key)
                }
            }
        }
    }
}
