import Foundation
import shared

/// View model for the chord library browser.
@MainActor
final class ChordLibraryViewModel: ObservableObject {
    @Published var selectedRoot: Int32 = 0
    @Published var selectedCategory: ChordCategory = .triad
    @Published var selectedFormula: ChordFormula?
    @Published var voicings: [ChordVoicing] = []
    @Published var searchQuery: String = ""
    @Published var searchResults: [ChordSearchResult] = []

    private let tuning: [shared.UkuleleString] = {
        let t = UkuleleTuning.highG
        return (0..<4).map { i in
            shared.UkuleleString(
                name: t.stringNames[i] as! String,
                openPitchClass: (t.pitchClasses[i] as! NSNumber).int32Value,
                octave: (t.octaves[i] as! NSNumber).int32Value
            )
        }
    }()

    init() {
        let formulas = formulasForCategory(.triad)
        if let first = formulas.first {
            selectedFormula = first
            voicings = generateVoicings(root: 0, formula: first)
        }
    }

    var rootNoteNames: [String] {
        (0..<Int32(12)).map { Notes.shared.pitchClassToName(pitchClass: $0) }
    }

    func formulasForCategory(_ category: ChordCategory) -> [ChordFormula] {
        guard let list = ChordFormulas.shared.BY_CATEGORY[category] else { return [] }
        return list as! [ChordFormula]
    }

    var currentChordName: String {
        let rootName = Notes.shared.pitchClassToName(pitchClass: selectedRoot)
        let symbol = selectedFormula?.symbol ?? ""
        return rootName + symbol
    }

    func selectRoot(_ pitchClass: Int32) {
        selectedRoot = pitchClass
        regenerateVoicings()
    }

    func selectCategory(_ category: ChordCategory) {
        selectedCategory = category
        let formulas = formulasForCategory(category)
        selectedFormula = formulas.first
        regenerateVoicings()
    }

    func selectFormula(_ formula: ChordFormula) {
        selectedFormula = formula
        regenerateVoicings()
    }

    // MARK: - Chord Search

    func updateSearchQuery(_ query: String) {
        searchQuery = query
        searchResults = ChordNameParser.shared.suggestions(query: query) as [ChordSearchResult]
    }

    /// Navigates to the chord described by the search result.
    /// Returns the target inversion if the result is a slash chord, nil otherwise.
    func selectSearchResult(_ result: ChordSearchResult) -> ChordInfo.Inversion? {
        selectRoot(result.rootPitchClass)
        selectCategory(result.formula.category)
        selectFormula(result.formula)
        searchQuery = ""
        searchResults = []
        return result.inversion
    }

    private func regenerateVoicings() {
        guard let formula = selectedFormula else {
            voicings = []
            return
        }
        voicings = generateVoicings(root: selectedRoot, formula: formula)
    }

    private func generateVoicings(root: Int32, formula: ChordFormula) -> [ChordVoicing] {
        let result = VoicingGenerator.shared.generate(
            rootPitchClass: root,
            formula: formula,
            tuning: tuning,
            allowMutedStrings: false
        )
        return result as! [ChordVoicing]
    }
}
