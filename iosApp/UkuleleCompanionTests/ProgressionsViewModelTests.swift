import XCTest
@testable import UkuleleCompanion
import shared

final class ProgressionsViewModelTests: XCTestCase {

    override func setUp() {
        super.setUp()
        UserDefaults.standard.removeObject(forKey: "custom_progressions")
    }

    @MainActor
    func testDefaultState() {
        let vm = ProgressionsViewModel()
        XCTAssertEqual(vm.selectedRoot, 0)
        XCTAssertEqual(vm.selectedScaleType, .major)
        XCTAssertTrue(vm.customProgressions.isEmpty)
    }

    @MainActor
    func testRootNoteNames() {
        let vm = ProgressionsViewModel()
        let names = vm.rootNoteNames
        XCTAssertEqual(names.count, 12)
        XCTAssertEqual(names[0], "C")
        XCTAssertEqual(names[9], "A")
    }

    @MainActor
    func testAllScaleTypes() {
        let vm = ProgressionsViewModel()
        let types = vm.allScaleTypes
        XCTAssertTrue(types.contains(.major))
        XCTAssertTrue(types.contains(.minor))
        XCTAssertEqual(types.count, 7)
    }

    @MainActor
    func testPresetProgressions() {
        let vm = ProgressionsViewModel()
        let presets = vm.presetProgressions
        XCTAssertFalse(presets.isEmpty)
    }

    @MainActor
    func testDiatonicDegrees() {
        let vm = ProgressionsViewModel()
        let degrees = vm.diatonicDegrees
        XCTAssertEqual(degrees.count, 7)
    }

    @MainActor
    func testResolvedChordNameKeyOfC() {
        let vm = ProgressionsViewModel()
        vm.selectedRoot = 0
        vm.selectedScaleType = .major
        let degrees = vm.diatonicDegrees
        guard degrees.count >= 4 else {
            XCTFail("Expected at least 4 diatonic degrees")
            return
        }

        let first = vm.resolvedChordName(degree: degrees[0])
        XCTAssertTrue(first.hasPrefix("C"), "First degree in C major should start with C, got \(first)")

        let fourth = vm.resolvedChordName(degree: degrees[3])
        XCTAssertTrue(fourth.hasPrefix("F"), "Fourth degree in C major should start with F, got \(fourth)")
    }

    @MainActor
    func testCreateCustomProgression() {
        let vm = ProgressionsViewModel()
        vm.createCustom(name: "My Progression", description: "Test", selectedDegreeIndices: Set([0, 3, 4]))

        XCTAssertEqual(vm.customProgressions.count, 1)
        XCTAssertEqual(vm.customProgressions.first?.name, "My Progression")
        XCTAssertEqual(vm.customProgressions.first?.degreeIntervals.count, 3)
    }

    @MainActor
    func testDeleteCustomProgression() {
        let vm = ProgressionsViewModel()
        vm.createCustom(name: "To Delete", description: "", selectedDegreeIndices: Set([0, 1]))
        let id = vm.customProgressions.first!.id

        vm.deleteCustom(id: id)
        XCTAssertTrue(vm.customProgressions.isEmpty)
    }

    @MainActor
    func testExportImportRoundTrip() {
        let vm = ProgressionsViewModel()
        vm.createCustom(name: "Export Test", description: "desc", selectedDegreeIndices: Set([0, 2, 4]))
        let exported = vm.exportData()

        UserDefaults.standard.removeObject(forKey: "custom_progressions")

        let vm2 = ProgressionsViewModel()
        XCTAssertTrue(vm2.customProgressions.isEmpty)

        vm2.importData(exported)
        XCTAssertEqual(vm2.customProgressions.count, 1)
        XCTAssertEqual(vm2.customProgressions.first?.name, "Export Test")
    }
}
