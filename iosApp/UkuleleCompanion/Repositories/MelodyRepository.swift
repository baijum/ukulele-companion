import Foundation

final class MelodyRepository {
    private let userDefaultsKey = "melodies"

    func getAll() -> [MelodyData] {
        guard let data = UserDefaults.standard.data(forKey: userDefaultsKey),
              let decoded = try? JSONDecoder().decode([MelodyData].self, from: data)
        else { return [] }
        return decoded
    }

    func save(_ melodies: [MelodyData]) {
        guard let data = try? JSONEncoder().encode(melodies) else { return }
        UserDefaults.standard.set(data, forKey: userDefaultsKey)
    }

    func exportData(_ melodies: [MelodyData]) -> [[String: Any]] {
        let encoder = JSONEncoder()
        return melodies.compactMap { melody in
            guard let data = try? encoder.encode(melody),
                  let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
            else { return nil }
            return dict
        }
    }

    func importData(_ incoming: [[String: Any]], into melodies: inout [MelodyData]) {
        let decoder = JSONDecoder()
        for item in incoming {
            guard let jsonData = try? JSONSerialization.data(withJSONObject: item),
                  let decoded = try? decoder.decode(MelodyData.self, from: jsonData)
            else { continue }
            let melody = decoded.sanitized()
            if let idx = melodies.firstIndex(where: { $0.id == melody.id }) {
                if melody.createdAt > melodies[idx].createdAt {
                    melodies[idx] = melody
                }
            } else {
                melodies.append(melody)
            }
        }
        save(melodies)
    }
}
