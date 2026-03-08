import XCTest
@testable import UkuleleCompanion

final class CustomPatternsViewModelTests: XCTestCase {

    override func setUp() {
        super.setUp()
        UserDefaults.standard.removeObject(forKey: "custom_strum_patterns")
        UserDefaults.standard.removeObject(forKey: "custom_fingerpicking_patterns")
    }

    @MainActor
    func testDefaultEmpty() {
        let vm = CustomPatternsViewModel()
        XCTAssertTrue(vm.strumPatterns.isEmpty)
        XCTAssertTrue(vm.fingerpickingPatterns.isEmpty)
    }

    @MainActor
    func testSaveStrumPattern() {
        let vm = CustomPatternsViewModel()
        let pattern = CustomStrumPatternData(
            id: "test-1",
            name: "Island Strum",
            beats: [
                StrumBeatData(direction: "DOWN", emphasis: true),
                StrumBeatData(direction: "DOWN", emphasis: false),
                StrumBeatData(direction: "UP", emphasis: false),
                StrumBeatData(direction: "UP", emphasis: false),
            ],
            createdAt: Date().timeIntervalSince1970,
            timeSignature: "4/4"
        )
        vm.saveStrumPattern(pattern)

        XCTAssertEqual(vm.strumPatterns.count, 1)
        XCTAssertEqual(vm.strumPatterns.first?.name, "Island Strum")
        XCTAssertEqual(vm.strumPatterns.first?.beats.count, 4)
    }

    @MainActor
    func testUpdateStrumPattern() {
        let vm = CustomPatternsViewModel()
        let pattern = CustomStrumPatternData(
            id: "test-1", name: "Pattern A",
            beats: [StrumBeatData(direction: "DOWN", emphasis: true)],
            createdAt: Date().timeIntervalSince1970, timeSignature: "4/4"
        )
        vm.saveStrumPattern(pattern)

        var updated = pattern
        updated.name = "Pattern B"
        vm.saveStrumPattern(updated)

        XCTAssertEqual(vm.strumPatterns.count, 1)
        XCTAssertEqual(vm.strumPatterns.first?.name, "Pattern B")
    }

    @MainActor
    func testDeleteStrumPattern() {
        let vm = CustomPatternsViewModel()
        let pattern = CustomStrumPatternData(
            id: "test-1", name: "Strum",
            beats: [StrumBeatData(direction: "DOWN", emphasis: true)],
            createdAt: Date().timeIntervalSince1970, timeSignature: "4/4"
        )
        vm.saveStrumPattern(pattern)

        vm.deleteStrumPattern(id: "test-1")
        XCTAssertTrue(vm.strumPatterns.isEmpty)
    }

    @MainActor
    func testSaveFingerpickingPattern() {
        let vm = CustomPatternsViewModel()
        let pattern = CustomFingerpickingData(
            id: "fp-1",
            name: "Travis Pick",
            steps: [
                FingerpickStepData(finger: "T", stringIndex: 3, emphasis: true),
                FingerpickStepData(finger: "I", stringIndex: 2, emphasis: false),
                FingerpickStepData(finger: "M", stringIndex: 1, emphasis: false),
            ],
            createdAt: Date().timeIntervalSince1970,
            timeSignature: "4/4"
        )
        vm.saveFingerpickingPattern(pattern)

        XCTAssertEqual(vm.fingerpickingPatterns.count, 1)
        XCTAssertEqual(vm.fingerpickingPatterns.first?.name, "Travis Pick")
    }

    @MainActor
    func testDeleteFingerpickingPattern() {
        let vm = CustomPatternsViewModel()
        let pattern = CustomFingerpickingData(
            id: "fp-1", name: "Pick",
            steps: [FingerpickStepData(finger: "T", stringIndex: 0, emphasis: false)],
            createdAt: Date().timeIntervalSince1970, timeSignature: "4/4"
        )
        vm.saveFingerpickingPattern(pattern)

        vm.deleteFingerpickingPattern(id: "fp-1")
        XCTAssertTrue(vm.fingerpickingPatterns.isEmpty)
    }

    @MainActor
    func testImportStrumDataDedup() {
        let vm = CustomPatternsViewModel()
        let pattern = CustomStrumPatternData(
            id: "test-1", name: "Existing",
            beats: [StrumBeatData(direction: "DOWN", emphasis: true)],
            createdAt: Date().timeIntervalSince1970, timeSignature: "4/4"
        )
        vm.saveStrumPattern(pattern)

        let importItems: [[String: Any]] = [
            ["id": "test-1", "name": "Duplicate", "beats": [["direction": "UP", "emphasis": false]], "createdAt": 0.0],
            ["id": "test-2", "name": "New Pattern", "beats": [["direction": "DOWN", "emphasis": true]], "createdAt": 1.0],
        ]
        vm.importStrumData(importItems)

        XCTAssertEqual(vm.strumPatterns.count, 2)
        XCTAssertEqual(vm.strumPatterns.first?.name, "Existing")
    }

    @MainActor
    func testImportFingerpickDataDedup() {
        let vm = CustomPatternsViewModel()
        let pattern = CustomFingerpickingData(
            id: "fp-1", name: "Existing",
            steps: [FingerpickStepData(finger: "T", stringIndex: 0, emphasis: false)],
            createdAt: Date().timeIntervalSince1970, timeSignature: "4/4"
        )
        vm.saveFingerpickingPattern(pattern)

        let importItems: [[String: Any]] = [
            ["id": "fp-1", "name": "Dup", "steps": [["finger": "I", "stringIndex": 1, "emphasis": true] as [String: Any]], "createdAt": 0.0],
            ["id": "fp-2", "name": "New", "steps": [["finger": "M", "stringIndex": 2, "emphasis": false] as [String: Any]], "createdAt": 1.0],
        ]
        vm.importFingerpickData(importItems)

        XCTAssertEqual(vm.fingerpickingPatterns.count, 2)
        XCTAssertEqual(vm.fingerpickingPatterns.first?.name, "Existing")
    }

    @MainActor
    func testExportRoundTrip() {
        let vm = CustomPatternsViewModel()
        let strum = CustomStrumPatternData(
            id: "s-1", name: "Strum A",
            beats: [StrumBeatData(direction: "DOWN", emphasis: true)],
            createdAt: 100.0, timeSignature: "4/4"
        )
        let pick = CustomFingerpickingData(
            id: "f-1", name: "Pick A",
            steps: [FingerpickStepData(finger: "T", stringIndex: 3, emphasis: true)],
            createdAt: 200.0, timeSignature: "3/4"
        )
        vm.saveStrumPattern(strum)
        vm.saveFingerpickingPattern(pick)

        let exportedStrum = vm.exportStrumData()
        let exportedPick = vm.exportFingerpickData()

        UserDefaults.standard.removeObject(forKey: "custom_strum_patterns")
        UserDefaults.standard.removeObject(forKey: "custom_fingerpicking_patterns")

        let vm2 = CustomPatternsViewModel()
        vm2.importStrumData(exportedStrum)
        vm2.importFingerpickData(exportedPick)

        XCTAssertEqual(vm2.strumPatterns.count, 1)
        XCTAssertEqual(vm2.strumPatterns.first?.name, "Strum A")
        XCTAssertEqual(vm2.fingerpickingPatterns.count, 1)
        XCTAssertEqual(vm2.fingerpickingPatterns.first?.name, "Pick A")
        XCTAssertEqual(vm2.fingerpickingPatterns.first?.timeSignature, "3/4")
    }
}
