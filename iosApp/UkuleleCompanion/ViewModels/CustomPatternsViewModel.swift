import Foundation

// MARK: - Custom Strum Pattern

struct StrumBeatData: Codable {
    var direction: String // DOWN, UP, CHUCK, MISS, PAUSE
    var emphasis: Bool
}

struct CustomStrumPatternData: Codable, Identifiable {
    let id: String
    var name: String
    var beats: [StrumBeatData]
    let createdAt: Double
    var timeSignature: String
}

// MARK: - Custom Fingerpicking Pattern

struct FingerpickStepData: Codable {
    var finger: String // T, I, M, R
    var stringIndex: Int // 0=G, 1=C, 2=E, 3=A
    var emphasis: Bool
}

struct CustomFingerpickingData: Codable, Identifiable {
    let id: String
    var name: String
    var steps: [FingerpickStepData]
    let createdAt: Double
    var timeSignature: String
}

// MARK: - ViewModel

@MainActor
final class CustomPatternsViewModel: ObservableObject {
    @Published var strumPatterns: [CustomStrumPatternData] = []
    @Published var fingerpickingPatterns: [CustomFingerpickingData] = []

    private let strumKey = "custom_strum_patterns"
    private let fingerpickKey = "custom_fingerpicking_patterns"

    init() {
        loadStrum()
        loadFingerpicking()
    }

    // MARK: - Strum Patterns

    func saveStrumPattern(_ pattern: CustomStrumPatternData) {
        if let idx = strumPatterns.firstIndex(where: { $0.id == pattern.id }) {
            strumPatterns[idx] = pattern
        } else {
            strumPatterns.insert(pattern, at: 0)
        }
        persistStrum()
    }

    func deleteStrumPattern(id: String) {
        strumPatterns.removeAll { $0.id == id }
        persistStrum()
    }

    // MARK: - Fingerpicking Patterns

    func saveFingerpickingPattern(_ pattern: CustomFingerpickingData) {
        if let idx = fingerpickingPatterns.firstIndex(where: { $0.id == pattern.id }) {
            fingerpickingPatterns[idx] = pattern
        } else {
            fingerpickingPatterns.insert(pattern, at: 0)
        }
        persistFingerpicking()
    }

    func deleteFingerpickingPattern(id: String) {
        fingerpickingPatterns.removeAll { $0.id == id }
        persistFingerpicking()
    }

    // MARK: - Persistence

    private func persistStrum() {
        guard let data = try? JSONEncoder().encode(strumPatterns) else { return }
        UserDefaults.standard.set(data, forKey: strumKey)
    }

    private func loadStrum() {
        guard let data = UserDefaults.standard.data(forKey: strumKey),
              let decoded = try? JSONDecoder().decode([CustomStrumPatternData].self, from: data)
        else { return }
        strumPatterns = decoded
    }

    private func persistFingerpicking() {
        guard let data = try? JSONEncoder().encode(fingerpickingPatterns) else { return }
        UserDefaults.standard.set(data, forKey: fingerpickKey)
    }

    private func loadFingerpicking() {
        guard let data = UserDefaults.standard.data(forKey: fingerpickKey),
              let decoded = try? JSONDecoder().decode([CustomFingerpickingData].self, from: data)
        else { return }
        fingerpickingPatterns = decoded
    }

    // MARK: - Export/Import for Backup

    func exportStrumData() -> [[String: Any]] {
        strumPatterns.map { p in
            [
                "id": p.id,
                "name": p.name,
                "beats": p.beats.map { ["direction": $0.direction, "emphasis": $0.emphasis] },
                "createdAt": p.createdAt,
                "timeSignature": p.timeSignature,
            ]
        }
    }

    func importStrumData(_ items: [[String: Any]]) {
        let existingIds = Set(strumPatterns.map(\.id))
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
            strumPatterns.append(CustomStrumPatternData(
                id: id, name: name, beats: beats, createdAt: createdAt, timeSignature: timeSignature))
        }
        persistStrum()
    }

    func exportFingerpickData() -> [[String: Any]] {
        fingerpickingPatterns.map { p in
            [
                "id": p.id,
                "name": p.name,
                "steps": p.steps.map { ["finger": $0.finger, "stringIndex": $0.stringIndex, "emphasis": $0.emphasis] as [String: Any] },
                "createdAt": p.createdAt,
                "timeSignature": p.timeSignature,
            ]
        }
    }

    func importFingerpickData(_ items: [[String: Any]]) {
        let existingIds = Set(fingerpickingPatterns.map(\.id))
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
            fingerpickingPatterns.append(CustomFingerpickingData(
                id: id, name: name, steps: steps, createdAt: createdAt, timeSignature: timeSignature))
        }
        persistFingerpicking()
    }
}
