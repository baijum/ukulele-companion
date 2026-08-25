import XCTest
@testable import UkuleleCompanion

final class SetlistViewModelTests: XCTestCase {

    override func setUp() {
        super.setUp()
        clearStores()
    }

    override func tearDown() {
        clearStores()
        super.tearDown()
    }

    private func clearStores() {
        for key in ["setlists", "chord_sheets"] {
            UserDefaults.standard.removeObject(forKey: key)
            UserDefaults.standard.removeObject(forKey: "\(key)_backup")
            UserDefaults.standard.removeObject(forKey: "\(key)_quarantine")
        }
    }

    private func seedLibrary(ids: [String]) {
        let songs = ids.map { id in
            StoredSong(
                id: id, title: id, artist: "", content: "", key: "C",
                capo: 0, strumPatternName: "", labels: [], createdAt: 0, updatedAt: 0
            )
        }
        SongbookRepository().save(songs)
    }

    private func seedSetlist(id: String, name: String, songIds: [String]) {
        let setlist = StoredSetlist(
            id: id, name: name, songIds: songIds, createdAt: 0, updatedAt: 0
        )
        SetlistRepository().save([setlist])
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

        // Keyed by song ID + offset, not a raw index: move "c" up two places.
        vm.moveSong(setlistId: id, songId: "c", offset: -2)
        XCTAssertEqual(vm.setlists.first?.songIds, ["c", "a", "b"])
    }

    @MainActor
    func testMoveSongWithUnknownIdIsNoOp() {
        let vm = SetlistViewModel()
        vm.create(name: "My Set")
        guard let id = vm.setlists.first?.id else { return XCTFail("no setlist") }
        vm.addSong(setlistId: id, songId: "a")
        vm.addSong(setlistId: id, songId: "b")

        vm.moveSong(setlistId: id, songId: "missing", offset: 1)
        vm.moveSong(setlistId: id, songId: "a", offset: 0)
        XCTAssertEqual(vm.setlists.first?.songIds, ["a", "b"])
    }

    // MARK: - Launch reconciliation (issue #602)

    @MainActor
    func testInitDropsSongIdsAbsentFromLibrary() {
        // A setlist persisted with a leading ID (S1) that no longer resolves in
        // the library — a legacy dead ID, or one a restored backup's import
        // skipped. It should render two rows (S2, S3), so reconciliation must
        // strip S1 rather than let it offset every edit.
        seedLibrary(ids: ["S2", "S3"])
        seedSetlist(id: "set-1", name: "Legacy", songIds: ["S1", "S2", "S3"])

        let vm = SetlistViewModel()

        XCTAssertEqual(vm.setlists.first?.songIds, ["S2", "S3"])
    }

    @MainActor
    func testInitReconciliationPersistsAcrossReload() {
        seedLibrary(ids: ["S2", "S3"])
        seedSetlist(id: "set-1", name: "Legacy", songIds: ["S1", "S2", "S3"])

        _ = SetlistViewModel() // reconciles and saves
        let reloaded = SetlistViewModel()

        XCTAssertEqual(reloaded.setlists.first?.songIds, ["S2", "S3"])
    }

    @MainActor
    func testInitLeavesResolvableSetlistUntouched() {
        seedLibrary(ids: ["S1", "S2", "S3"])
        seedSetlist(id: "set-1", name: "Clean", songIds: ["S1", "S2", "S3"])

        let vm = SetlistViewModel()

        XCTAssertEqual(vm.setlists.first?.songIds, ["S1", "S2", "S3"])
        // A setlist without dead IDs must not be rewritten (updatedAt preserved).
        XCTAssertEqual(vm.setlists.first?.updatedAt, 0)
    }

    @MainActor
    func testDeleteAfterReconciliationTargetsCorrectRenderedRow() {
        // Pre-fix: songIds[0] = S1 (dead), so deleting rendered row 0 (S2) read
        // S1 and left S2 in place. After reconciliation songIds == rendered
        // order, so removing the rendered row's ID hits the right song.
        seedLibrary(ids: ["S2", "S3"])
        seedSetlist(id: "set-1", name: "Legacy", songIds: ["S1", "S2", "S3"])

        let vm = SetlistViewModel()
        guard let renderedFirst = vm.setlists.first?.songIds.first else {
            return XCTFail("expected reconciled rows")
        }
        XCTAssertEqual(renderedFirst, "S2")

        vm.removeSong(setlistId: "set-1", songId: renderedFirst)
        XCTAssertEqual(vm.setlists.first?.songIds, ["S3"])
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
