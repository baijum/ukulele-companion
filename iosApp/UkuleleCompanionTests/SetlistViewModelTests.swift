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

    // MARK: - Purging deleted songs (issue #594)

    @MainActor
    func testPurgeDeletedSongsStripsTheSongFromEverySetlist() {
        let vm = SetlistViewModel()
        vm.create(name: "First")
        vm.addSong(setlistId: vm.setlists.first { $0.name == "First" }!.id, songId: "a")
        vm.addSong(setlistId: vm.setlists.first { $0.name == "First" }!.id, songId: "b")
        vm.create(name: "Second")
        vm.addSong(setlistId: vm.setlists.first { $0.name == "Second" }!.id, songId: "b")
        vm.addSong(setlistId: vm.setlists.first { $0.name == "Second" }!.id, songId: "d")

        vm.purgeDeletedSongs(["b"])

        XCTAssertEqual(vm.setlists.first { $0.name == "First" }?.songIds, ["a"])
        XCTAssertEqual(vm.setlists.first { $0.name == "Second" }?.songIds, ["d"])
    }

    @MainActor
    func testPurgeDeletedSongsPersistsTheCleanup() {
        let vm = SetlistViewModel()
        vm.create(name: "Gig")
        let id = vm.setlists.first!.id
        vm.addSong(setlistId: id, songId: "a")
        vm.addSong(setlistId: id, songId: "b")

        vm.purgeDeletedSongs(["a"])

        let reloaded = SetlistViewModel()
        XCTAssertEqual(reloaded.setlists.first?.songIds, ["b"])
    }

    @MainActor
    func testPurgeDeletedSongsToleratesUnknownAndEmptyInputs() {
        let vm = SetlistViewModel()
        vm.create(name: "Gig")
        let id = vm.setlists.first!.id
        vm.addSong(setlistId: id, songId: "a")

        vm.purgeDeletedSongs([])
        vm.purgeDeletedSongs(["not-there"])

        XCTAssertEqual(vm.setlists.first?.songIds, ["a"])
    }

    @MainActor
    func testSongDeletionNotificationPurgesSetlists() {
        let setlists = SetlistViewModel()
        setlists.create(name: "Gig")
        let id = setlists.setlists.first!.id
        setlists.addSong(setlistId: id, songId: "song-1")

        NotificationCenter.default.post(
            name: .songsDeletedFromLibrary,
            object: nil,
            userInfo: [Notification.songsDeletedIds: ["song-1"]]
        )

        // The observer hops through the main queue and a @MainActor Task, so
        // wait for the published state rather than asserting immediately.
        let purged = expectation(for: NSPredicate { _, _ in
            setlists.setlists.first?.songIds.isEmpty ?? false
        }, evaluatedWith: nil)
        wait(for: [purged], timeout: 2)
    }
}
