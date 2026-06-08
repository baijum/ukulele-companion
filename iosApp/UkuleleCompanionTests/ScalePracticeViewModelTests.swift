import XCTest
@testable import UkuleleCompanion
import shared

final class ScalePracticeViewModelTests: XCTestCase {

    override func setUp() {
        super.setUp()
        UserDefaults.standard.removeObject(forKey: "scale_practice_settings")
    }

    @MainActor
    func testInitialDefaults() {
        let vm = ScalePracticeViewModel()
        XCTAssertEqual(vm.mode, .quiz)
        XCTAssertEqual(vm.selectedRoot, 0)
        XCTAssertNil(vm.selectedCategory)
        XCTAssertNil(vm.selectedScale)
        XCTAssertEqual(vm.bpm, 120)
        XCTAssertFalse(vm.isPlaying)
        XCTAssertEqual(vm.playDirection, .ascending)
        XCTAssertFalse(vm.loopPlayback)
        XCTAssertEqual(vm.fretPosition, .all)
        XCTAssertFalse(vm.showFretboard)
    }

    @MainActor
    func testModeSwitching() {
        let vm = ScalePracticeViewModel()
        vm.mode = .playAlong
        XCTAssertEqual(vm.mode, .playAlong)

        vm.mode = .earTraining
        XCTAssertEqual(vm.mode, .earTraining)

        vm.mode = .quiz
        XCTAssertEqual(vm.mode, .quiz)
    }

    @MainActor
    func testRootNoteNamesReturns12Notes() {
        let vm = ScalePracticeViewModel()
        let names = vm.rootNoteNames
        XCTAssertEqual(names.count, 12)
        XCTAssertEqual(names[0], "C")
    }

    @MainActor
    func testAvailableScalesNoCategory() {
        let vm = ScalePracticeViewModel()
        vm.selectedCategory = nil
        let scales = vm.availableScales
        XCTAssertFalse(scales.isEmpty, "All scales should be returned when category is nil")
    }

    @MainActor
    func testToggleLoopFlipsBothWays() {
        let vm = ScalePracticeViewModel()
        XCTAssertFalse(vm.loopPlayback)
        vm.loopPlayback = true
        XCTAssertTrue(vm.loopPlayback)
        vm.loopPlayback = false
        XCTAssertFalse(vm.loopPlayback)
    }

    @MainActor
    func testToggleShowFretboard() {
        let vm = ScalePracticeViewModel()
        XCTAssertFalse(vm.showFretboard)
        vm.showFretboard = true
        XCTAssertTrue(vm.showFretboard)
    }

    @MainActor
    func testSettingsRoundTrip() {
        let vm = ScalePracticeViewModel()
        vm.selectedRoot = 7
        vm.bpm = 80
        vm.playDirection = .descending
        vm.loopPlayback = true
        vm.fretPosition = .mid
        vm.saveSettings()

        let vm2 = ScalePracticeViewModel()
        XCTAssertEqual(vm2.selectedRoot, 7)
        XCTAssertEqual(vm2.bpm, 80, accuracy: 0.1)
        XCTAssertEqual(vm2.playDirection, .descending)
        XCTAssertTrue(vm2.loopPlayback)
        XCTAssertEqual(vm2.fretPosition, .mid)
    }

    @MainActor
    func testFretPositionRangeAll() {
        let range = ScalePracticeViewModel.FretPosition.all.range(lastFret: 12)
        XCTAssertEqual(range, 0...12)
    }

    @MainActor
    func testFretPositionRangeOpen() {
        let range = ScalePracticeViewModel.FretPosition.open.range(lastFret: 12)
        XCTAssertEqual(range, 0...4)
    }

    @MainActor
    func testFretPositionRangeMid() {
        let range = ScalePracticeViewModel.FretPosition.mid.range(lastFret: 12)
        XCTAssertEqual(range, 5...9)
    }

    @MainActor
    func testFretPositionRangeHigh() {
        let range = ScalePracticeViewModel.FretPosition.high.range(lastFret: 15)
        XCTAssertEqual(range, 10...15)
    }

    @MainActor
    func testScaleNotesForMajorScale() {
        let vm = ScalePracticeViewModel()
        let scales = vm.availableScales
        let major = scales.first { $0.name == "Major" }
        guard let majorScale = major else {
            XCTFail("Major scale should exist in available scales")
            return
        }
        vm.selectedScale = majorScale
        vm.selectedRoot = 0  // C Major
        let notes = vm.scaleNotes
        XCTAssertFalse(notes.isEmpty, "Scale notes should not be empty for C Major")
        XCTAssertEqual(notes[0], 0, "First note of C Major should be C (pitch class 0)")
    }
}
