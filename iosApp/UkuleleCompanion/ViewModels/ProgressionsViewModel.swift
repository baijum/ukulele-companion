import Foundation
import shared

struct CustomProgression: Codable, Identifiable {
    let id: String
    let name: String
    let description: String
    let degreeIntervals: [Int]
    let degreeQualities: [String]
    let degreeNumerals: [String]
    let scaleType: String
}

@MainActor
final class ProgressionsViewModel: ObservableObject {
    @Published var selectedRoot: Int32 = 0
    @Published var selectedScaleType: ScaleType = .major
    @Published var customProgressions: [CustomProgression] = []
    @Published var showingCreateSheet = false

    private let userDefaultsKey = "custom_progressions"

    init() {
        loadCustomProgressions()
    }

    var rootNoteNames: [String] {
        (0..<Int32(12)).map { Notes.shared.pitchClassToName(pitchClass: $0) }
    }

    var allScaleTypes: [ScaleType] {
        [.major, .minor, .dorian, .phrygian, .lydian, .mixolydian, .locrian]
    }

    var presetProgressions: [Progression] {
        let list = Progressions.shared.forScale(scaleType: selectedScaleType)
        return list as! [Progression]
    }

    var diatonicDegrees: [ChordDegree] {
        let list = Progressions.shared.diatonicDegrees(scaleType: selectedScaleType)
        return list as! [ChordDegree]
    }

    func resolvedChordName(degree: ChordDegree) -> String {
        let pitchClass = (Int(selectedRoot) + Int(degree.interval)) % 12
        let name = Notes.shared.enharmonicForKey(
            pitchClass: Int32(pitchClass),
            keyRoot: KotlinInt(int: selectedRoot),
            isMinor: selectedScaleType == .minor
        )
        return name + degree.quality
    }

    func shareText(for progression: Progression) -> String {
        ChordSheetFormatter.shared.formatProgression(
            progression: progression,
            keyRoot: selectedRoot
        )
    }

    func createCustom(name: String, description: String, selectedDegreeIndices: Set<Int>) {
        let degrees = diatonicDegrees
        let selected = selectedDegreeIndices.sorted().compactMap { i -> ChordDegree? in
            guard i < degrees.count else { return nil }
            return degrees[i]
        }
        guard !selected.isEmpty else { return }

        let custom = CustomProgression(
            id: UUID().uuidString,
            name: name,
            description: description,
            degreeIntervals: selected.map { Int($0.interval) },
            degreeQualities: selected.map { $0.quality },
            degreeNumerals: selected.map { $0.numeral },
            scaleType: selectedScaleType.name
        )
        customProgressions.insert(custom, at: 0)
        saveCustomProgressions()
    }

    func deleteCustom(id: String) {
        customProgressions.removeAll { $0.id == id }
        saveCustomProgressions()
    }

    func resolvedChordNameForCustomDegree(interval: Int, quality: String) -> String {
        let pitchClass = (Int(selectedRoot) + interval) % 12
        let name = Notes.shared.enharmonicForKey(
            pitchClass: Int32(pitchClass),
            keyRoot: KotlinInt(int: selectedRoot),
            isMinor: selectedScaleType == .minor
        )
        return name + quality
    }

    func importData(_ incoming: [[String: Any]]) {
        let decoder = JSONDecoder()
        let existingIds = Set(customProgressions.map { $0.id })
        for item in incoming {
            guard let jsonData = try? JSONSerialization.data(withJSONObject: item),
                  let progression = try? decoder.decode(CustomProgression.self, from: jsonData)
            else { continue }
            if !existingIds.contains(progression.id) {
                customProgressions.append(progression)
            }
        }
        saveCustomProgressions()
    }

    func exportData() -> [[String: Any]] {
        let encoder = JSONEncoder()
        return customProgressions.compactMap { prog in
            guard let data = try? encoder.encode(prog),
                  let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
            else { return nil }
            return dict
        }
    }

    private func loadCustomProgressions() {
        guard let data = UserDefaults.standard.data(forKey: userDefaultsKey),
              let decoded = try? JSONDecoder().decode([CustomProgression].self, from: data)
        else { return }
        customProgressions = decoded
    }

    private func saveCustomProgressions() {
        guard let data = try? JSONEncoder().encode(customProgressions) else { return }
        UserDefaults.standard.set(data, forKey: userDefaultsKey)
    }
}
