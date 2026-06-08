import XCTest
@testable import UkuleleCompanion

final class MelodyViewModelTests: XCTestCase {

    override func setUp() {
        super.setUp()
        UserDefaults.standard.removeObject(forKey: "melodies")
    }

    @MainActor
    func testInitialEmptyState() {
        let vm = MelodyViewModel()
        XCTAssertTrue(vm.notes.isEmpty)
        XCTAssertEqual(vm.selectedDuration, .quarter)
        XCTAssertEqual(vm.bpm, 120)
        XCTAssertEqual(vm.currentOctave, 4)
        XCTAssertFalse(vm.isPlaying)
        XCTAssertNil(vm.playingIndex)
        XCTAssertFalse(vm.isRecording)
        XCTAssertFalse(vm.hasUnsavedChanges)
    }

    @MainActor
    func testAddNoteAppendsAndMarksUnsaved() {
        let vm = MelodyViewModel()
        vm.addNote(pitchClass: 0, octave: 4)
        XCTAssertEqual(vm.notes.count, 1)
        XCTAssertEqual(vm.notes[0].pitchClass, 0)
        XCTAssertEqual(vm.notes[0].octave, 4)
        XCTAssertFalse(vm.notes[0].isRest)
        XCTAssertTrue(vm.hasUnsavedChanges)
    }

    @MainActor
    func testAddRestAppendsRestNote() {
        let vm = MelodyViewModel()
        vm.addRest()
        XCTAssertEqual(vm.notes.count, 1)
        XCTAssertTrue(vm.notes[0].isRest)
        XCTAssertNil(vm.notes[0].pitchClass)
        XCTAssertEqual(vm.notes[0].displayName, "Rest")
    }

    @MainActor
    func testDeleteNoteRemovesAtIndex() {
        let vm = MelodyViewModel()
        vm.addNote(pitchClass: 0, octave: 4)
        vm.addNote(pitchClass: 4, octave: 4)
        vm.addNote(pitchClass: 7, octave: 4)
        XCTAssertEqual(vm.notes.count, 3)

        vm.deleteNoteAt(index: 1)
        XCTAssertEqual(vm.notes.count, 2)
        XCTAssertEqual(vm.notes[0].pitchClass, 0)
        XCTAssertEqual(vm.notes[1].pitchClass, 7)
    }

    @MainActor
    func testClearAllResetsState() {
        let vm = MelodyViewModel()
        vm.addNote(pitchClass: 5, octave: 3)
        vm.save(name: "Test")
        vm.addNote(pitchClass: 7, octave: 3)

        vm.clearAll()
        XCTAssertTrue(vm.notes.isEmpty)
        XCTAssertNil(vm.loadedMelodyName)
        XCTAssertFalse(vm.hasUnsavedChanges)
    }

    @MainActor
    func testNoteDurationBeats() {
        XCTAssertEqual(NoteDuration.whole.beats, 4.0)
        XCTAssertEqual(NoteDuration.half.beats, 2.0)
        XCTAssertEqual(NoteDuration.quarter.beats, 1.0)
        XCTAssertEqual(NoteDuration.eighth.beats, 0.5)
        XCTAssertEqual(NoteDuration.sixteenth.beats, 0.25)
    }

    @MainActor
    func testSaveLoadRoundTrip() {
        let vm = MelodyViewModel()
        vm.addNote(pitchClass: 0, octave: 4)
        vm.addNote(pitchClass: 4, octave: 4)
        vm.bpm = 100
        vm.save(name: "My Melody")

        XCTAssertEqual(vm.savedMelodies.count, 1)
        XCTAssertEqual(vm.savedMelodies[0].name, "My Melody")
        XCTAssertEqual(vm.loadedMelodyName, "My Melody")
        XCTAssertFalse(vm.hasUnsavedChanges)

        let vm2 = MelodyViewModel()
        XCTAssertEqual(vm2.savedMelodies.count, 1)
        XCTAssertEqual(vm2.savedMelodies[0].notes.count, 2)
        XCTAssertEqual(vm2.savedMelodies[0].bpm, 100)
    }

    @MainActor
    func testExportImportRoundTrip() {
        let vm = MelodyViewModel()
        vm.addNote(pitchClass: 7, octave: 5)
        vm.save(name: "Export Test")
        let exported = vm.exportData()
        XCTAssertEqual(exported.count, 1)

        UserDefaults.standard.removeObject(forKey: "melodies")
        let vm2 = MelodyViewModel()
        XCTAssertTrue(vm2.savedMelodies.isEmpty)

        vm2.importData(exported)
        XCTAssertEqual(vm2.savedMelodies.count, 1)
        XCTAssertEqual(vm2.savedMelodies[0].name, "Export Test")
        XCTAssertEqual(vm2.savedMelodies[0].notes[0].pitchClass, 7)
    }

    @MainActor
    func testSelectNoteToggle() {
        let vm = MelodyViewModel()
        XCTAssertNil(vm.selectedNoteIndex)

        vm.selectNote(index: 2)
        XCTAssertEqual(vm.selectedNoteIndex, 2)

        vm.selectNote(index: 2)
        XCTAssertNil(vm.selectedNoteIndex, "Selecting the same index should deselect")

        vm.selectNote(index: 3)
        XCTAssertEqual(vm.selectedNoteIndex, 3)
    }

    @MainActor
    func testStepSequencerExpandAndShrink() {
        let vm = MelodyViewModel()
        XCTAssertEqual(vm.steps.count, MelodyViewModel.stepCount8)

        vm.expandSteps()
        XCTAssertEqual(vm.steps.count, MelodyViewModel.stepCount16)

        vm.expandSteps()
        XCTAssertEqual(vm.steps.count, MelodyViewModel.stepCount8, "Should shrink back to 8")
    }

    @MainActor
    func testDeleteMelody() {
        let vm = MelodyViewModel()
        vm.addNote(pitchClass: 0, octave: 4)
        vm.save(name: "To Delete")
        let id = vm.savedMelodies[0].id
        XCTAssertEqual(vm.savedMelodies.count, 1)

        vm.deleteMelody(id: id)
        XCTAssertTrue(vm.savedMelodies.isEmpty)
    }
}
