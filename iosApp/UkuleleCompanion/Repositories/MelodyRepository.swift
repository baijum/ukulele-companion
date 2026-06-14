import Foundation

final class MelodyRepository: JSONStorageRepository<MelodyData> {
    init() {
        super.init(key: "melodies")
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
