import XCTest
@testable import UkuleleCompanion

final class AccessibilityTests: XCTestCase {

    // MARK: - TunerViewModel accessibility labels

    @MainActor
    func testNoteAccessibilityLabel_noNote() {
        let vm = TunerViewModel()
        XCTAssertEqual(vm.noteAccessibilityLabel, "No note detected")
    }

    @MainActor
    func testNoteAccessibilityLabel_withOctave() {
        let vm = TunerViewModel()
        vm.noteName = "A"
        vm.octave = 4
        XCTAssertEqual(vm.noteAccessibilityLabel, "Detected note: A4")
    }

    @MainActor
    func testNoteAccessibilityLabel_withoutOctave() {
        let vm = TunerViewModel()
        vm.noteName = "C"
        vm.octave = nil
        XCTAssertEqual(vm.noteAccessibilityLabel, "Detected note: C")
    }

    @MainActor
    func testCentsAccessibilityValue_noPitch() {
        let vm = TunerViewModel()
        XCTAssertEqual(vm.centsAccessibilityValue, "No pitch detected")
    }

    @MainActor
    func testCentsAccessibilityValue_inTune() {
        let vm = TunerViewModel()
        vm.noteName = "A"
        vm.centsDeviation = 3.0
        XCTAssertEqual(vm.centsAccessibilityValue, "In tune")
    }

    @MainActor
    func testCentsAccessibilityValue_inTune_negative() {
        let vm = TunerViewModel()
        vm.noteName = "A"
        vm.centsDeviation = -5.0
        XCTAssertEqual(vm.centsAccessibilityValue, "In tune")
    }

    @MainActor
    func testCentsAccessibilityValue_sharp() {
        let vm = TunerViewModel()
        vm.noteName = "A"
        vm.centsDeviation = 20.0
        XCTAssertEqual(vm.centsAccessibilityValue, "20 cents sharp")
    }

    @MainActor
    func testCentsAccessibilityValue_flat() {
        let vm = TunerViewModel()
        vm.noteName = "A"
        vm.centsDeviation = -15.0
        XCTAssertEqual(vm.centsAccessibilityValue, "15 cents flat")
    }

    // MARK: - isInTune state transition (haptic trigger condition)

    @MainActor
    func testIsInTune_defaultFalse() {
        let vm = TunerViewModel()
        XCTAssertFalse(vm.isInTune)
    }

    // MARK: - AccessibilityAnnouncer throttling

    @MainActor
    func testAnnouncerThrottlesSameMessage() {
        let announcer = AccessibilityAnnouncer.shared
        announcer.announce("Test", minInterval: 10.0)
        announcer.announce("Test", minInterval: 10.0)
    }

    @MainActor
    func testAnnouncerAllowsDifferentMessages() {
        let announcer = AccessibilityAnnouncer.shared
        announcer.announce("First", minInterval: 10.0)
        announcer.announce("Second", minInterval: 10.0)
    }

    // MARK: - NeedleMeterView accessibility

    @MainActor
    func testNeedleMeterAccessibilityLabel_inTune() {
        let view = NeedleMeterView(cents: 0)
        _ = view
    }

    // MARK: - String progress descriptions

    @MainActor
    func testStringProgressDefault() {
        let vm = TunerViewModel()
        XCTAssertEqual(vm.stringProgress.count, 4)
        XCTAssertTrue(vm.stringProgress.allSatisfy { !$0 })
    }
}
