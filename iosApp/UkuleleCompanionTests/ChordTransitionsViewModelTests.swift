import XCTest
@testable import UkuleleCompanion

final class ChordTransitionsViewModelTests: XCTestCase {

    @MainActor
    func testInitialState() {
        let vm = ChordTransitionsViewModel()
        XCTAssertEqual(vm.chord1, "C")
        XCTAssertEqual(vm.chord2, "G")
        XCTAssertEqual(vm.bpm, 80)
        XCTAssertEqual(vm.beatsPerChord, 4)
        XCTAssertFalse(vm.isRunning)
        XCTAssertEqual(vm.transitionCount, 0)
        XCTAssertEqual(vm.currentBeat, 0)
        XCTAssertTrue(vm.currentChordIsFirst)
        XCTAssertTrue(vm.playChordOnTransition)
    }

    @MainActor
    func testCurrentChordReflectsToggle() {
        let vm = ChordTransitionsViewModel()
        XCTAssertEqual(vm.currentChord, "C")

        vm.currentChordIsFirst = false
        XCTAssertEqual(vm.currentChord, "G")

        vm.currentChordIsFirst = true
        XCTAssertEqual(vm.currentChord, "C")
    }

    @MainActor
    func testChordPairSelection() {
        let vm = ChordTransitionsViewModel()
        vm.chord1 = "Am"
        vm.chord2 = "Dm"
        XCTAssertEqual(vm.chord1, "Am")
        XCTAssertEqual(vm.chord2, "Dm")
        XCTAssertEqual(vm.currentChord, "Am")
    }

    @MainActor
    func testSwitchesPerMinuteCalculation() {
        let vm = ChordTransitionsViewModel()
        vm.transitionCount = 10
        vm.elapsedSeconds = 30
        XCTAssertEqual(vm.switchesPerMinute, 20.0, accuracy: 0.01)
    }

    @MainActor
    func testSwitchesPerMinuteZeroWhenNoElapsed() {
        let vm = ChordTransitionsViewModel()
        vm.transitionCount = 5
        vm.elapsedSeconds = 0
        XCTAssertEqual(vm.switchesPerMinute, 0)
    }

    @MainActor
    func testSwitchesPerMinuteZeroWhenNoTransitions() {
        let vm = ChordTransitionsViewModel()
        vm.transitionCount = 0
        vm.elapsedSeconds = 30
        XCTAssertEqual(vm.switchesPerMinute, 0)
    }

    @MainActor
    func testResetClearsState() {
        let vm = ChordTransitionsViewModel()
        vm.transitionCount = 5
        vm.currentBeat = 3
        vm.currentChordIsFirst = false
        vm.elapsedSeconds = 60
        vm.startTime = Date()

        vm.reset()
        XCTAssertFalse(vm.isRunning)
        XCTAssertEqual(vm.transitionCount, 0)
        XCTAssertEqual(vm.currentBeat, 0)
        XCTAssertTrue(vm.currentChordIsFirst)
        XCTAssertEqual(vm.elapsedSeconds, 0)
        XCTAssertNil(vm.startTime)
    }

    @MainActor
    func testStopSetsNotRunning() {
        let vm = ChordTransitionsViewModel()
        vm.start()
        XCTAssertTrue(vm.isRunning)
        vm.stop()
        XCTAssertFalse(vm.isRunning)
    }

    @MainActor
    func testVoicingForChordReturnsVoicing() {
        let vm = ChordTransitionsViewModel()
        let voicing = vm.voicingForChord(name: "C")
        XCTAssertNotNil(voicing, "Should find a voicing for C major")
    }

    @MainActor
    func testVoicingForMinorChord() {
        let vm = ChordTransitionsViewModel()
        let voicing = vm.voicingForChord(name: "Am")
        XCTAssertNotNil(voicing, "Should find a voicing for Am")
    }
}
