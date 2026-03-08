import Foundation

struct PracticeTimerData: Codable {
    var totalMinutes: Int = 0
    var totalSessions: Int = 0
    var dailyMinutes: [String: Int] = [:]
    var longestSession: Int = 0
    var lastSessionTime: Double = 0
    var dailyGoal: Int = 15
}

@MainActor
final class PracticeTimerViewModel: ObservableObject {
    @Published var data = PracticeTimerData()

    private let storageKey = "practice_timer"

    init() {
        load()
    }

    func recordSession(durationSeconds: Int) {
        let minutes = max(durationSeconds / 60, 1)
        let today = todayKey()

        data.totalMinutes += minutes
        data.totalSessions += 1
        data.dailyMinutes[today, default: 0] += minutes
        if minutes > data.longestSession {
            data.longestSession = minutes
        }
        data.lastSessionTime = Date().timeIntervalSince1970
        save()
    }

    var todayMinutes: Int {
        data.dailyMinutes[todayKey()] ?? 0
    }

    var dailyProgress: Float {
        data.dailyGoal > 0 ? Float(todayMinutes) / Float(data.dailyGoal) : 0
    }

    var totalTimeFormatted: String {
        let hours = data.totalMinutes / 60
        let mins = data.totalMinutes % 60
        return hours > 0 ? "\(hours)h \(mins)m" : "\(mins)m"
    }

    func setDailyGoal(_ minutes: Int) {
        data.dailyGoal = min(max(minutes, 5), 120)
        save()
    }

    // MARK: - Persistence

    private func save() {
        guard let encoded = try? JSONEncoder().encode(data) else { return }
        UserDefaults.standard.set(encoded, forKey: storageKey)
    }

    private func load() {
        guard let stored = UserDefaults.standard.data(forKey: storageKey),
              let decoded = try? JSONDecoder().decode(PracticeTimerData.self, from: stored)
        else { return }
        data = decoded
    }

    private func todayKey() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter.string(from: Date())
    }

    // MARK: - Export/Import for Backup

    func exportData() -> [String: Any] {
        [
            "totalMinutes": data.totalMinutes,
            "totalSessions": data.totalSessions,
            "dailyMinutes": data.dailyMinutes,
            "longestSession": data.longestSession,
            "lastSessionTime": data.lastSessionTime,
            "dailyGoal": data.dailyGoal,
        ]
    }

    func importData(_ dict: [String: Any]) {
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
            data.lastSessionTime = max(data.lastSessionTime, lastTime)
        }
        if let goal = dict["dailyGoal"] as? Int {
            data.dailyGoal = goal
        }
        if let daily = dict["dailyMinutes"] as? [String: Int] {
            for (key, value) in daily {
                data.dailyMinutes[key] = max(data.dailyMinutes[key] ?? 0, value)
            }
        }
        save()
    }
}
