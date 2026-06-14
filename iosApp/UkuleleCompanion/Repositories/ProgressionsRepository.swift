import Foundation

final class ProgressionsRepository: JSONStorageRepository<CustomProgression> {
    init() {
        super.init(key: "custom_progressions")
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
