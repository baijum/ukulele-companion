import Foundation

final class ProgressionsRepository {
    private let userDefaultsKey = "custom_progressions"

    func getAll() -> [CustomProgression] {
        guard let data = UserDefaults.standard.data(forKey: userDefaultsKey),
              let decoded = try? JSONDecoder().decode([CustomProgression].self, from: data)
        else { return [] }
        return decoded
    }

    func save(_ progressions: [CustomProgression]) {
        guard let data = try? JSONEncoder().encode(progressions) else { return }
        UserDefaults.standard.set(data, forKey: userDefaultsKey)
    }

    func exportData(_ progressions: [CustomProgression]) -> [[String: Any]] {
        let encoder = JSONEncoder()
        return progressions.compactMap { prog in
            guard let data = try? encoder.encode(prog),
                  let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
            else { return nil }
            return dict
        }
    }

    func importData(_ incoming: [[String: Any]], into progressions: inout [CustomProgression]) {
        let decoder = JSONDecoder()
        let existingIds = Set(progressions.map { $0.id })
        for item in incoming {
            guard let jsonData = try? JSONSerialization.data(withJSONObject: item),
                  let progression = try? decoder.decode(CustomProgression.self, from: jsonData)
            else { continue }
            if !existingIds.contains(progression.id) {
                progressions.append(progression)
            }
        }
        save(progressions)
    }
}
