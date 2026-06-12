import Foundation

final class ScalePracticeRepository {
    private static let settingsKey = "scale_practice_settings"

    func loadSettings() -> [String: Any]? {
        UserDefaults.standard.dictionary(forKey: Self.settingsKey)
    }

    func saveSettings(_ dict: [String: Any]) {
        UserDefaults.standard.set(dict, forKey: Self.settingsKey)
    }
}
