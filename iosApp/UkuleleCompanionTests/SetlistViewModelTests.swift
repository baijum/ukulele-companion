import XCTest
@testable import UkuleleCompanion

final class SetlistViewModelTests: XCTestCase {

    override func setUp() {
        super.setUp()
        UserDefaults.standard.removeObject(forKey: "setlists")
    }

    @MainActor
    func testDefaultEmpty() {
        let vm = SetlistViewModel()
        XCTAssertTrue(vm.setlists.isEmpty)
    }

    @MainActor
    func testCreate() {
        let vm = SetlistViewModel()
        vm.create(name: "Friday Gig")

        XCTAssertEqual(vm.setlists.count, 1)
        XCTAssertEqual(vm.setlists.first?.name, "Friday Gig")
        XCTAssertTrue(vm.setlists.first?.songIds.isEmpty ?? false)
    }

    @MainActor
    func testRename() {
        let vm = SetlistViewModel()
        vm.create(name: "Old Name")
        let id = vm.setlists.first!.id

        vm.rename(id: id, newName: "New Name")
        XCTAssertEqual(vm.setlists.first?.name, "New Name")
    }

    @MainActor
    func testDelete() {
        let vm = SetlistViewModel()
        vm.create(name: "To Delete")
        let id = vm.setlists.first!.id

        vm.delete(id: id)
        XCTAssertTrue(vm.setlists.isEmpty)
    }

    @MainActor
    func testAddSong() {
        let vm = SetlistViewModel()
        vm.create(name: "My Set")
        let id = vm.setlists.first!.id

        vm.addSong(setlistId: id, songId: "song-1")
        XCTAssertEqual(vm.setlists.first?.songIds, ["song-1"])
    }

    @MainActor
    func testAddDuplicateSong() {
        let vm = SetlistViewModel()
        vm.create(name: "My Set")
        let id = vm.setlists.first!.id

        vm.addSong(setlistId: id, songId: "song-1")
        vm.addSong(setlistId: id, songId: "song-1")
        XCTAssertEqual(vm.setlists.first?.songIds.count, 1)
    }

    @MainActor
    func testRemoveSong() {
        let vm = SetlistViewModel()
        vm.create(name: "My Set")
        let id = vm.setlists.first!.id

        vm.addSong(setlistId: id, songId: "song-1")
        vm.addSong(setlistId: id, songId: "song-2")
        vm.removeSong(setlistId: id, songId: "song-1")

        XCTAssertEqual(vm.setlists.first?.songIds, ["song-2"])
    }

    @MainActor
    func testMoveSong() {
        let vm = SetlistViewModel()
        vm.create(name: "My Set")
        let id = vm.setlists.first!.id

        vm.addSong(setlistId: id, songId: "a")
        vm.addSong(setlistId: id, songId: "b")
        vm.addSong(setlistId: id, songId: "c")

        vm.moveSong(setlistId: id, from: 2, to: 0)
        XCTAssertEqual(vm.setlists.first?.songIds, ["c", "a", "b"])
    }

    @MainActor
    func testExportImportRoundTrip() {
        let vm = SetlistViewModel()
        vm.create(name: "Gig 1")
        let id = vm.setlists.first!.id
        vm.addSong(setlistId: id, songId: "s1")
        vm.addSong(setlistId: id, songId: "s2")

        let exported = vm.exportData()

        UserDefaults.standard.removeObject(forKey: "setlists")

        let vm2 = SetlistViewModel()
        XCTAssertTrue(vm2.setlists.isEmpty)

        vm2.importData(exported)
        XCTAssertEqual(vm2.setlists.count, 1)
        XCTAssertEqual(vm2.setlists.first?.name, "Gig 1")
        XCTAssertEqual(vm2.setlists.first?.songIds, ["s1", "s2"])
    }
}
