import XCTest
@testable import UkuleleCompanion
import shared

final class FretboardViewModelTests: XCTestCase {

    /// Flatten the [Int: Int?] double-optional into a plain Int?.
    @MainActor
    private func selectedFret(_ vm: FretboardViewModel, string: Int) -> Int? {
        vm.selections[string] ?? nil
    }

    @MainActor
    func testInitialState() {
        let vm = FretboardViewModel()
        XCTAssertEqual(vm.selections.count, 4)
        for i in 0..<4 {
            XCTAssertNil(selectedFret(vm, string: i), "String \(i) should start with no selection")
        }
        XCTAssertTrue(vm.showNoteNames)
        XCTAssertEqual(vm.capoFret, 0)
    }

    @MainActor
    func testInitialDetectionIsNoSelection() {
        let vm = FretboardViewModel()
        if case .noSelection = vm.detectionResult {
            // expected
        } else {
            XCTFail("Expected .noSelection, got \(vm.detectionResult)")
        }
    }

    @MainActor
    func testToggleFretSelectsAndDeselects() {
        let vm = FretboardViewModel()
        vm.soundEnabled = false

        vm.toggleFret(stringIndex: 0, fret: 3)
        XCTAssertEqual(selectedFret(vm, string: 0), 3)

        vm.toggleFret(stringIndex: 0, fret: 3)
        XCTAssertNil(selectedFret(vm, string: 0))
    }

    @MainActor
    func testToggleFretReplacesPreviousSelection() {
        let vm = FretboardViewModel()
        vm.soundEnabled = false

        vm.toggleFret(stringIndex: 1, fret: 2)
        XCTAssertEqual(selectedFret(vm, string: 1), 2)

        vm.toggleFret(stringIndex: 1, fret: 5)
        XCTAssertEqual(selectedFret(vm, string: 1), 5)
    }

    @MainActor
    func testCapoBlocksFretsBehind() {
        let vm = FretboardViewModel()
        vm.soundEnabled = false
        vm.setCapoFret(3)

        vm.toggleFret(stringIndex: 0, fret: 2)
        XCTAssertNil(selectedFret(vm, string: 0), "Fret 2 should be blocked by capo at fret 3")

        vm.toggleFret(stringIndex: 0, fret: 3)
        XCTAssertNil(selectedFret(vm, string: 0), "Fret 3 (equal to capo) should be blocked")

        vm.toggleFret(stringIndex: 0, fret: 4)
        XCTAssertEqual(selectedFret(vm, string: 0), 4, "Fret 4 should be allowed past capo 3")
    }

    @MainActor
    func testSetCapoFretClearsBehindCapo() {
        let vm = FretboardViewModel()
        vm.soundEnabled = false

        vm.toggleFret(stringIndex: 0, fret: 2)
        vm.toggleFret(stringIndex: 1, fret: 5)
        XCTAssertEqual(selectedFret(vm, string: 0), 2)
        XCTAssertEqual(selectedFret(vm, string: 1), 5)

        vm.setCapoFret(3)
        XCTAssertNil(selectedFret(vm, string: 0), "Fret 2 should be cleared when capo set to 3")
        XCTAssertEqual(selectedFret(vm, string: 1), 5, "Fret 5 should remain past capo 3")
    }

    @MainActor
    func testClearAllResetsSelections() {
        let vm = FretboardViewModel()
        vm.soundEnabled = false

        vm.toggleFret(stringIndex: 0, fret: 1)
        vm.toggleFret(stringIndex: 2, fret: 3)
        vm.clearAll()

        for i in 0..<4 {
            XCTAssertNil(selectedFret(vm, string: i), "String \(i) should be nil after clearAll")
        }
        if case .noSelection = vm.detectionResult {
            // expected
        } else {
            XCTFail("Expected .noSelection after clearAll")
        }
    }

    @MainActor
    func testGetNoteAtOpenStrings() {
        let vm = FretboardViewModel()
        let (pc0, _) = vm.getNoteAt(stringIndex: 0, fret: 0)
        let (pc1, _) = vm.getNoteAt(stringIndex: 1, fret: 0)
        let (pc2, _) = vm.getNoteAt(stringIndex: 2, fret: 0)
        let (pc3, _) = vm.getNoteAt(stringIndex: 3, fret: 0)

        XCTAssertEqual(pc0, 7, "String 0 open should be G")
        XCTAssertEqual(pc1, 0, "String 1 open should be C")
        XCTAssertEqual(pc2, 4, "String 2 open should be E")
        XCTAssertEqual(pc3, 9, "String 3 open should be A")
    }

    @MainActor
    func testGetNoteAtWithCapo() {
        let vm = FretboardViewModel()
        vm.setCapoFret(2)
        let (pc, _) = vm.getNoteAt(stringIndex: 1, fret: 0)
        XCTAssertEqual(pc, 2, "String 1 fret 0 with capo 2 should give D (pitch class 2)")
    }

    @MainActor
    func testScaleOverlayToggle() {
        let vm = FretboardViewModel()
        XCTAssertFalse(vm.scaleOverlay.enabled)
        vm.toggleScaleOverlay()
        XCTAssertTrue(vm.scaleOverlay.enabled)
        vm.toggleScaleOverlay()
        XCTAssertFalse(vm.scaleOverlay.enabled)
    }

    @MainActor
    func testSetScalePopulatesNotes() {
        let vm = FretboardViewModel()
        let allScales = Scales.shared.forCategory(category: nil) as! [Scale]
        guard let major = allScales.first(where: { $0.name == "Major" }) else {
            XCTFail("Major scale not found")
            return
        }
        vm.setScale(major)
        XCTAssertTrue(vm.scaleOverlay.enabled)
        XCTAssertFalse(vm.scaleOverlay.scaleNotes.isEmpty)
        XCTAssertTrue(vm.scaleOverlay.scaleNotes.contains(0), "C Major scale should contain C (0)")
    }

    @MainActor
    func testChordDetectionTriggered() {
        let vm = FretboardViewModel()
        vm.soundEnabled = false

        vm.toggleFret(stringIndex: 0, fret: 0)
        vm.toggleFret(stringIndex: 1, fret: 0)
        vm.toggleFret(stringIndex: 2, fret: 0)
        vm.toggleFret(stringIndex: 3, fret: 3)

        if case .noSelection = vm.detectionResult {
            XCTFail("Should have a detection result after selecting frets")
        }
    }

    @MainActor
    func testSetCapoFretClampsRange() {
        let vm = FretboardViewModel()
        vm.setCapoFret(-1)
        XCTAssertEqual(vm.capoFret, 0)

        vm.setCapoFret(20)
        XCTAssertEqual(vm.capoFret, vm.fretCount - 1)
    }
}
