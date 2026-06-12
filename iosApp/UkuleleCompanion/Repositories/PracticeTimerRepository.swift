import Foundation

final class PracticeTimerRepository {
    private let storageKey = "practice_timer"

    func load() -> PracticeTimerData {
        guard let stored = UserDefaults.standard.data(forKey: storageKey),
              let decoded = try? JSONDecoder().decode(PracticeTimerData.self, from: stored)
        else { return PracticeTimerData() }
        return decoded
    }

    func save(_ data: PracticeTimerData) {
        guard let encoded = try? JSONEncoder().encode(data) else { return }
        UserDefaults.standard.set(encoded, forKey: storageKey)
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
            data.dailyGoal = goal
        }
        if let daily = dict["dailyMinutes"] as? [String: Int] {
            for (key, value) in daily {
                data.dailyMinutes[key] = max(data.dailyMinutes[key] ?? 0, value)
            }
        }
        save(data)
    }
}
