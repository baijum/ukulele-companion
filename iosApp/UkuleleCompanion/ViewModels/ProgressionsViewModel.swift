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
    @Published var editingCustomId: String?

    private let repository: ProgressionsRepository

    init(repository: ProgressionsRepository = ProgressionsRepository()) {
        self.repository = repository
        customProgressions = repository.getAll()
    }

    var rootNoteNames: [String] {
        (0..<Int32(12)).map { Notes.shared.pitchClassToName(pitchClass: $0) }
    }

    var allScaleTypes: [ScaleType] {
        [.major, .minor, .dorian, .phrygian, .lydian, .mixolydian, .locrian]
    }

    var presetProgressions: [Progression] {
        let list = Progressions.shared.forScale(scaleType: selectedScaleType)
        return list.asArray(of: Progression.self)
    }

    var diatonicDegrees: [ChordDegree] {
        let list = Progressions.shared.diatonicDegrees(scaleType: selectedScaleType)
        return list.asArray(of: ChordDegree.self)
    }

    var filteredCustomProgressions: [CustomProgression] {
        customProgressions.filter { $0.scaleType == selectedScaleType.name }
    }

    func diatonicDegreesForScale(_ scaleType: ScaleType) -> [ChordDegree] {
        let list = Progressions.shared.diatonicDegrees(scaleType: scaleType)
        return list.asArray(of: ChordDegree.self)
    }

    func toProgression(_ custom: CustomProgression) -> Progression {
        let degrees = zip(custom.degreeIntervals, zip(custom.degreeQualities, custom.degreeNumerals))
            .map { interval, qualityNumeral in
                let (quality, numeral) = qualityNumeral
                return ChordDegree(interval: Int32(interval), quality: quality, numeral: numeral)
            }
        let scaleType = allScaleTypes.first { $0.name == custom.scaleType } ?? .major
        return Progression(
            name: custom.name,
            description: custom.description,
            degrees: degrees,
            scaleType: scaleType
        )
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

    func shareTextForCustom(_ custom: CustomProgression) -> String {
        shareText(for: toProgression(custom))
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
        repository.save(customProgressions)
    }

    func createCustomFromDegrees(name: String, description: String, degrees: [ChordDegree], scaleType: ScaleType) {
        guard degrees.count >= 2 else { return }
        let custom = CustomProgression(
            id: UUID().uuidString,
            name: name,
            description: description.isEmpty ? "Custom progression" : description,
            degreeIntervals: degrees.map { Int($0.interval) },
            degreeQualities: degrees.map { $0.quality },
            degreeNumerals: degrees.map { $0.numeral },
            scaleType: scaleType.name
        )
        customProgressions.insert(custom, at: 0)
        repository.save(customProgressions)
    }

    func updateCustom(id: String, name: String, description: String, degrees: [ChordDegree], scaleType: ScaleType) {
        guard let index = customProgressions.firstIndex(where: { $0.id == id }) else { return }
        guard degrees.count >= 2 else { return }
        let updated = CustomProgression(
            id: id,
            name: name,
            description: description.isEmpty ? "Custom progression" : description,
            degreeIntervals: degrees.map { Int($0.interval) },
            degreeQualities: degrees.map { $0.quality },
            degreeNumerals: degrees.map { $0.numeral },
            scaleType: scaleType.name
        )
        customProgressions[index] = updated
        repository.save(customProgressions)
    }

    func deleteCustom(id: String) {
        customProgressions.removeAll { $0.id == id }
        repository.save(customProgressions)
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
        repository.importData(incoming, into: &customProgressions)
    }

    func exportData() -> [[String: Any]] {
        repository.exportData(customProgressions)
    }
}
