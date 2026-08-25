import Foundation

/// Storage for the practice totals.
///
/// One object rather than a list, but with the same thing to lose: a payload
/// that fails to decode used to reset the totals to zero, and the next session
/// wrote that zero back over the only copy. It goes through the same
/// backup-and-quarantine helper as the list stores (#569).
final class PracticeTimerRepository {
    private let storageKey = "practice_timer"
    /// Default daily practice goal in minutes; matches `PracticeTimerData`'s
    /// default and Android's `DEFAULT_DAILY_GOAL`.
    private static let defaultDailyGoal = 15
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    /// Reads the totals, falling back to the backup copy when the primary
    /// payload cannot be decoded. Fresh totals are the answer both to a store
    /// that was never written and to one whose copies are both unreadable; only
    /// the second leaves quarantined bytes behind.
    func load() -> PracticeTimerData {
        let parse: (Data) -> PracticeTimerData? = {
            try? JSONDecoder().decode(PracticeTimerData.self, from: $0)
        }
        switch defaults.readWithBackupFallback(key: storageKey, tryParse: parse) {
        case let .loaded(stored): return stored
        case .absent, .unrecoverable: return PracticeTimerData()
        }
    }

    /// Writes the totals, rotating the outgoing payload into the backup slot.
    func save(_ data: PracticeTimerData) {
        guard let encoded = try? JSONEncoder().encode(data) else { return }
        defaults.writeWithBackupRotation(key: storageKey, raw: encoded) {
            (try? JSONDecoder().decode(PracticeTimerData.self, from: $0)) != nil
        }
    }

    func exportData(_ data: PracticeTimerData) -> [String: Any] {
        [
            "totalMinutes": data.totalMinutes,
            "totalSessions": data.totalSessions,
            "dailyMinutes": data.dailyMinutes,
            "longestSession": data.longestSession,
            "lastSessionTime": data.lastSessionTime,
            "dailyGoal": data.dailyGoal,
        ]
    }

    func importData(_ dict: [String: Any], into data: inout PracticeTimerData) {
        if let total = dict["totalMinutes"] as? Int {
            data.totalMinutes = max(data.totalMinutes, total)
        }
        if let sessions = dict["totalSessions"] as? Int {
            data.totalSessions = max(data.totalSessions, sessions)
        }
        if let longest = dict["longestSession"] as? Int {
            data.longestSession = max(data.longestSession, longest)
        }
        if let lastTime = dict["lastSessionTime"] as? Double {
            let normalized = lastTime > 100_000_000_000 ? lastTime / 1000.0 : lastTime
            data.lastSessionTime = max(data.lastSessionTime, normalized)
        }
        if let goal = dict["dailyGoal"] as? Int {
            // Mirror Android (PracticeTimerRepository.importAll): clamp the
            // incoming goal to the valid 5...120 range and apply it only when it
            // differs from the 15-minute default. The backup dictionary always
            // carries this key — an absent or default practice-timer section
            // decodes to a goal of 15 — so assigning it unconditionally would
            // silently overwrite a goal the user chose (#608).
            let incomingGoal = min(max(goal, 5), 120)
            if incomingGoal != Self.defaultDailyGoal {
                data.dailyGoal = incomingGoal
            }
        }
        if let daily = dict["dailyMinutes"] as? [String: Int] {
            for (key, value) in daily {
                data.dailyMinutes[key] = max(data.dailyMinutes[key] ?? 0, value)
            }
        }
        save(data)
    }
}
