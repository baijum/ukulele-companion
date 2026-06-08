import XCTest
@testable import UkuleleCompanion

final class MetronomeViewModelTests: XCTestCase {

    @MainActor
    func testInitialStateDefaults() {
        let vm = MetronomeViewModel()
        XCTAssertEqual(vm.bpm, 100)
        XCTAssertEqual(vm.beatsPerMeasure, 4)
        XCTAssertEqual(vm.subdivision, 1)
        XCTAssertFalse(vm.isPlaying)
        XCTAssertFalse(vm.isCompound)
        XCTAssertEqual(vm.timeSignatureLabel, "4/4")
    }

    @MainActor
    func testInitialAccentPatternFirstBeatAccented() {
        let vm = MetronomeViewModel()
        XCTAssertEqual(vm.accentPattern.count, 4)
        XCTAssertEqual(vm.accentPattern[0], .accent)
        for i in 1..<vm.accentPattern.count {
            XCTAssertEqual(vm.accentPattern[i], .normal)
        }
    }

    @MainActor
    func testSetBpmCoercesRange() {
        let vm = MetronomeViewModel()
        vm.setBpm(10)
        XCTAssertEqual(vm.bpm, 30)

        vm.setBpm(500)
        XCTAssertEqual(vm.bpm, 300)

        vm.setBpm(120)
        XCTAssertEqual(vm.bpm, 120)
    }

    @MainActor
    func testSetTimeSignature68SetsCompoundMode() {
        let vm = MetronomeViewModel()
        vm.setTimeSignature("6/8")
        XCTAssertTrue(vm.isCompound)
        XCTAssertEqual(vm.beatsPerMeasure, 2)
        XCTAssertEqual(vm.subdivision, 3)
        XCTAssertEqual(vm.timeSignatureLabel, "6/8")
    }

    @MainActor
    func testSetTimeSignature128SetsCompoundMode() {
        let vm = MetronomeViewModel()
        vm.setTimeSignature("12/8")
        XCTAssertTrue(vm.isCompound)
        XCTAssertEqual(vm.beatsPerMeasure, 4)
        XCTAssertEqual(vm.subdivision, 3)
    }

    @MainActor
    func testSetTimeSignatureSimpleParsesBeats() {
        let vm = MetronomeViewModel()
        vm.setTimeSignature("3/4")
        XCTAssertFalse(vm.isCompound)
        XCTAssertEqual(vm.beatsPerMeasure, 3)
        XCTAssertEqual(vm.subdivision, 1)
        XCTAssertEqual(vm.timeSignatureLabel, "3/4")
    }

    @MainActor
    func testSetSubdivisionBlockedInCompoundMode() {
        let vm = MetronomeViewModel()
        vm.setTimeSignature("6/8")
        XCTAssertEqual(vm.subdivision, 3)
        vm.setSubdivision(2)
        XCTAssertEqual(vm.subdivision, 3, "Subdivision change should be blocked in compound mode")
    }

    @MainActor
    func testSetSubdivisionCoercesInSimpleMode() {
        let vm = MetronomeViewModel()
        vm.setSubdivision(0)
        XCTAssertEqual(vm.subdivision, 1)

        vm.setSubdivision(6)
        XCTAssertEqual(vm.subdivision, 4)

        vm.setSubdivision(2)
        XCTAssertEqual(vm.subdivision, 2)
    }

    @MainActor
    func testToggleBeatTypeCyclesAccentNormalMute() {
        let vm = MetronomeViewModel()
        XCTAssertEqual(vm.accentPattern[0], .accent)

        vm.toggleBeatType(0)
        XCTAssertEqual(vm.accentPattern[0], .normal)

        vm.toggleBeatType(0)
        XCTAssertEqual(vm.accentPattern[0], .mute)

        vm.toggleBeatType(0)
        XCTAssertEqual(vm.accentPattern[0], .accent)
    }

    @MainActor
    func testToggleBeatTypeIgnoresOutOfBounds() {
        let vm = MetronomeViewModel()
        let before = vm.accentPattern
        vm.toggleBeatType(-1)
        vm.toggleBeatType(99)
        XCTAssertEqual(vm.accentPattern, before)
    }

    @MainActor
    func testSetTimeSignatureResetsCompoundWhenSimple() {
        let vm = MetronomeViewModel()
        vm.setTimeSignature("6/8")
        XCTAssertTrue(vm.isCompound)

        vm.setTimeSignature("5/4")
        XCTAssertFalse(vm.isCompound)
        XCTAssertEqual(vm.beatsPerMeasure, 5)
        XCTAssertEqual(vm.subdivision, 1)
    }

    @MainActor
    func testStopResetsPlaybackState() {
        let vm = MetronomeViewModel()
        vm.start()
        XCTAssertTrue(vm.isPlaying)
        vm.stop()
        XCTAssertFalse(vm.isPlaying)
        XCTAssertEqual(vm.currentBeat, -1)
        XCTAssertEqual(vm.currentSubBeat, 0)
    }
}
