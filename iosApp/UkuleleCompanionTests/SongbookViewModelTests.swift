import XCTest
@testable import UkuleleCompanion

final class SongbookViewModelTests: XCTestCase {

    override func setUp() {
        super.setUp()
        UserDefaults.standard.removeObject(forKey: "chord_sheets")
    }

    private func makeSong(
        id: String = UUID().uuidString,
        title: String = "Test Song",
        artist: String = "Test Artist",
        content: String = "[C]Hello [G]World",
        key: String = "C",
        labels: [String] = [],
        createdAt: Double = 1000.0,
        updatedAt: Double = 1000.0
    ) -> StoredSong {
        StoredSong(
            id: id, title: title, artist: artist, content: content,
            key: key, capo: 0, strumPatternName: "", labels: labels,
            createdAt: createdAt, updatedAt: updatedAt
        )
    }

    @MainActor
    func testDefaultEmpty() {
        let vm = SongbookViewModel()
        XCTAssertTrue(vm.songs.isEmpty)
        XCTAssertTrue(vm.filteredSongs.isEmpty)
    }

    @MainActor
    func testSaveSong() {
        let vm = SongbookViewModel()
        let song = makeSong(title: "Amazing Grace")
        vm.save(song: song)

        XCTAssertEqual(vm.songs.count, 1)
        XCTAssertEqual(vm.songs.first?.title, "Amazing Grace")
    }

    @MainActor
    func testUpdateExistingSong() {
        let vm = SongbookViewModel()
        let song = makeSong(id: "song-1", title: "Original")
        vm.save(song: song)

        var updated = song
        updated.title = "Updated Title"
        vm.save(song: updated)

        XCTAssertEqual(vm.songs.count, 1)
        XCTAssertEqual(vm.songs.first?.title, "Updated Title")
    }

    @MainActor
    func testDeleteSong() {
        let vm = SongbookViewModel()
        let song = makeSong(id: "song-1")
        vm.save(song: song)

        vm.delete(id: "song-1")
        XCTAssertTrue(vm.songs.isEmpty)
    }

    @MainActor
    func testSearchFilter() {
        let vm = SongbookViewModel()
        vm.save(song: makeSong(title: "Amazing Grace", artist: "Traditional"))
        vm.save(song: makeSong(title: "Somewhere Over the Rainbow", artist: "IZ"))

        vm.searchQuery = "amazing"
        XCTAssertEqual(vm.filteredSongs.count, 1)
        XCTAssertEqual(vm.filteredSongs.first?.title, "Amazing Grace")

        vm.searchQuery = "iz"
        XCTAssertEqual(vm.filteredSongs.count, 1)
        XCTAssertEqual(vm.filteredSongs.first?.artist, "IZ")

        vm.searchQuery = ""
        XCTAssertEqual(vm.filteredSongs.count, 2)
    }

    @MainActor
    func testLabelFilter() {
        let vm = SongbookViewModel()
        vm.save(song: makeSong(title: "Song A", labels: ["worship"]))
        vm.save(song: makeSong(title: "Song B", labels: ["folk"]))
        vm.save(song: makeSong(title: "Song C", labels: ["worship", "folk"]))

        vm.selectedLabels = ["worship"]
        XCTAssertEqual(vm.filteredSongs.count, 2)

        vm.selectedLabels = ["folk"]
        XCTAssertEqual(vm.filteredSongs.count, 2)

        vm.selectedLabels = []
        XCTAssertEqual(vm.filteredSongs.count, 3)
    }

    @MainActor
    func testSortByTitle() {
        let vm = SongbookViewModel()
        vm.save(song: makeSong(title: "Zebra Song"))
        vm.save(song: makeSong(title: "Apple Song"))

        vm.sortOrder = .title
        XCTAssertEqual(vm.filteredSongs.first?.title, "Apple Song")
        XCTAssertEqual(vm.filteredSongs.last?.title, "Zebra Song")
    }

    @MainActor
    func testSortByArtist() {
        let vm = SongbookViewModel()
        vm.save(song: makeSong(title: "Song 1", artist: "Zoe"))
        vm.save(song: makeSong(title: "Song 2", artist: "Alice"))

        vm.sortOrder = .artist
        XCTAssertEqual(vm.filteredSongs.first?.artist, "Alice")
        XCTAssertEqual(vm.filteredSongs.last?.artist, "Zoe")
    }

    @MainActor
    func testAllLabels() {
        let vm = SongbookViewModel()
        vm.save(song: makeSong(title: "A", labels: ["folk", "worship"]))
        vm.save(song: makeSong(title: "B", labels: ["pop"]))
        vm.save(song: makeSong(title: "C", labels: ["folk"]))

        let labels = vm.allLabels
        XCTAssertEqual(labels.count, 3)
        XCTAssertTrue(labels.contains("folk"))
        XCTAssertTrue(labels.contains("worship"))
        XCTAssertTrue(labels.contains("pop"))
    }

    @MainActor
    func testExportImportRoundTrip() {
        let vm = SongbookViewModel()
        vm.save(song: makeSong(id: "s-1", title: "My Song", content: "[Am]test"))
        let exported = vm.exportData()

        UserDefaults.standard.removeObject(forKey: "chord_sheets")

        let vm2 = SongbookViewModel()
        XCTAssertTrue(vm2.songs.isEmpty)

        vm2.importData(exported)
        XCTAssertEqual(vm2.songs.count, 1)
        XCTAssertEqual(vm2.songs.first?.title, "My Song")
        XCTAssertEqual(vm2.songs.first?.content, "[Am]test")
    }
}
