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

    private let repository: PracticeTimerRepository

    init(repository: PracticeTimerRepository = PracticeTimerRepository()) {
        self.repository = repository
        data = repository.load()
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
        repository.save(data)
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
        repository.save(data)
    }

    // MARK: - Export/Import for Backup

    func exportData() -> [String: Any] {
        repository.exportData(data)
    }

    func importData(_ dict: [String: Any]) {
        repository.importData(dict, into: &data)
    }

    private func todayKey() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter.string(from: Date())
    }
}
