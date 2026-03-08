import XCTest
@testable import UkuleleCompanion
import shared

final class ChordLibraryViewModelTests: XCTestCase {

    @MainActor
    func testDefaultSelection() {
        let vm = ChordLibraryViewModel()
        XCTAssertEqual(vm.selectedRoot, 0)
        XCTAssertEqual(vm.selectedCategory, .triad)
        XCTAssertNotNil(vm.selectedFormula)
        XCTAssertFalse(vm.voicings.isEmpty)
    }

    @MainActor
    func testRootNoteNames() {
        let vm = ChordLibraryViewModel()
        let names = vm.rootNoteNames
        XCTAssertEqual(names.count, 12)
        XCTAssertEqual(names[0], "C")
        XCTAssertEqual(names[7], "G")
    }

    @MainActor
    func testSelectRoot() {
        let vm = ChordLibraryViewModel()
        XCTAssertEqual(vm.selectedRoot, 0)

        vm.selectRoot(7)
        XCTAssertEqual(vm.selectedRoot, 7)
        XCTAssertFalse(vm.voicings.isEmpty)
        XCTAssertTrue(vm.currentChordName.hasPrefix("G"), "Chord name should reflect new root G, got \(vm.currentChordName)")
    }

    @MainActor
    func testSelectCategory() {
        let vm = ChordLibraryViewModel()

        vm.selectCategory(.seventh)
        XCTAssertEqual(vm.selectedCategory, .seventh)
        XCTAssertNotNil(vm.selectedFormula)
    }

    @MainActor
    func testSelectFormula() {
        let vm = ChordLibraryViewModel()
        let formulas = vm.formulasForCategory(.triad)
        guard formulas.count >= 2 else {
            XCTFail("Expected at least 2 triad formulas")
            return
        }

        vm.selectFormula(formulas[1])
        XCTAssertEqual(vm.selectedFormula?.symbol, formulas[1].symbol)
        XCTAssertFalse(vm.voicings.isEmpty)
    }

    @MainActor
    func testCurrentChordName() {
        let vm = ChordLibraryViewModel()
        vm.selectRoot(0)
        let name = vm.currentChordName
        XCTAssertTrue(name.hasPrefix("C"), "Chord name should start with C, got \(name)")
    }

    @MainActor
    func testSearchQuery() {
        let vm = ChordLibraryViewModel()
        vm.updateSearchQuery("Am")

        XCTAssertFalse(vm.searchResults.isEmpty, "Searching 'Am' should return results")
        let hasAm = vm.searchResults.contains { $0.displayName.contains("Am") }
        XCTAssertTrue(hasAm, "Results should contain Am")
    }

    @MainActor
    func testSearchQueryClear() {
        let vm = ChordLibraryViewModel()
        vm.updateSearchQuery("C")
        XCTAssertFalse(vm.searchResults.isEmpty)

        vm.updateSearchQuery("")
        XCTAssertTrue(vm.searchResults.isEmpty)
    }

    @MainActor
    func testSelectSearchResult() {
        let vm = ChordLibraryViewModel()
        vm.updateSearchQuery("G")
        guard let result = vm.searchResults.first else {
            XCTFail("Expected at least one search result for 'G'")
            return
        }

        _ = vm.selectSearchResult(result)
        XCTAssertTrue(vm.searchQuery.isEmpty, "Search query should clear after selection")
        XCTAssertTrue(vm.searchResults.isEmpty, "Search results should clear after selection")
        XCTAssertFalse(vm.voicings.isEmpty, "Voicings should regenerate for selected chord")
    }

    @MainActor
    func testFormulasForCategory() {
        let vm = ChordLibraryViewModel()

        let triads = vm.formulasForCategory(.triad)
        XCTAssertFalse(triads.isEmpty, "Should have triad formulas")

        let sevenths = vm.formulasForCategory(.seventh)
        XCTAssertFalse(sevenths.isEmpty, "Should have seventh formulas")
    }
}
