import Foundation

final class CustomPatternsRepository {
    private let strumKey = "custom_strum_patterns"
    private let fingerpickKey = "custom_fingerpicking_patterns"

    // MARK: - Strum Patterns

    func getAllStrum() -> [CustomStrumPatternData] {
        guard let data = UserDefaults.standard.data(forKey: strumKey),
              let decoded = try? JSONDecoder().decode([CustomStrumPatternData].self, from: data)
        else { return [] }
        return decoded
    }

    func saveStrum(_ patterns: [CustomStrumPatternData]) {
        guard let data = try? JSONEncoder().encode(patterns) else { return }
        UserDefaults.standard.set(data, forKey: strumKey)
    }

    // MARK: - Fingerpicking Patterns

    func getAllFingerpicking() -> [CustomFingerpickingData] {
        guard let data = UserDefaults.standard.data(forKey: fingerpickKey),
              let decoded = try? JSONDecoder().decode([CustomFingerpickingData].self, from: data)
        else { return [] }
        return decoded
    }

    func saveFingerpicking(_ patterns: [CustomFingerpickingData]) {
        guard let data = try? JSONEncoder().encode(patterns) else { return }
        UserDefaults.standard.set(data, forKey: fingerpickKey)
    }

    // MARK: - Export/Import

    func exportStrumData(_ patterns: [CustomStrumPatternData]) -> [[String: Any]] {
        patterns.map { p in
            [
                "id": p.id,
                "name": p.name,
                "beats": p.beats.map { ["direction": $0.direction, "emphasis": $0.emphasis] },
                "createdAt": p.createdAt,
                "timeSignature": p.timeSignature,
            ]
        }
    }

    func importStrumData(_ items: [[String: Any]], into patterns: inout [CustomStrumPatternData]) {
        let existingIds = Set(patterns.map(\.id))
        for dict in items {
            guard let id = dict["id"] as? String,
                  !existingIds.contains(id),
                  let name = dict["name"] as? String,
                  let beatDicts = dict["beats"] as? [[String: Any]],
                  let rawCreatedAt = dict["createdAt"] as? Double
            else { continue }
            let createdAt = rawCreatedAt > 100_000_000_000 ? rawCreatedAt / 1000.0 : rawCreatedAt
            let timeSignature = dict["timeSignature"] as? String ?? "4/4"
            let beats = beatDicts.compactMap { bd -> StrumBeatData? in
                guard let dir = bd["direction"] as? String,
                      let emph = bd["emphasis"] as? Bool
                else { return nil }
                return StrumBeatData(direction: dir, emphasis: emph)
            }
            patterns.append(CustomStrumPatternData(
                id: id, name: name, beats: beats, createdAt: createdAt, timeSignature: timeSignature))
        }
        saveStrum(patterns)
    }

    func exportFingerpickData(_ patterns: [CustomFingerpickingData]) -> [[String: Any]] {
        patterns.map { p in
            [
                "id": p.id,
                "name": p.name,
                "steps": p.steps.map { ["finger": $0.finger, "stringIndex": $0.stringIndex, "emphasis": $0.emphasis] as [String: Any] },
                "createdAt": p.createdAt,
                "timeSignature": p.timeSignature,
            ]
        }
    }

    func importFingerpickData(_ items: [[String: Any]], into patterns: inout [CustomFingerpickingData]) {
        let existingIds = Set(patterns.map(\.id))
        for dict in items {
            guard let id = dict["id"] as? String,
                  !existingIds.contains(id),
                  let name = dict["name"] as? String,
                  let stepDicts = dict["steps"] as? [[String: Any]],
                  let rawCreatedAt = dict["createdAt"] as? Double
            else { continue }
            let createdAt = rawCreatedAt > 100_000_000_000 ? rawCreatedAt / 1000.0 : rawCreatedAt
            let timeSignature = dict["timeSignature"] as? String ?? "4/4"
            let steps = stepDicts.compactMap { sd -> FingerpickStepData? in
                guard let finger = sd["finger"] as? String,
                      let strIdx = sd["stringIndex"] as? Int,
                      (0...3).contains(strIdx),
                      let emph = sd["emphasis"] as? Bool
                else { return nil }
                return FingerpickStepData(finger: finger, stringIndex: strIdx, emphasis: emph)
            }
            guard !steps.isEmpty else { continue }
            patterns.append(CustomFingerpickingData(
                id: id, name: name, steps: steps, createdAt: createdAt, timeSignature: timeSignature))
        }
        saveFingerpicking(patterns)
    }
}
