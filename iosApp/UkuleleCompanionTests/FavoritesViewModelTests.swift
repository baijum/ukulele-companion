import XCTest
@testable import UkuleleCompanion

final class FavoritesViewModelTests: XCTestCase {

    override func setUp() {
        super.setUp()
        UserDefaults.standard.removeObject(forKey: "favorite_voicings")
        UserDefaults.standard.removeObject(forKey: "favorite_folders")
    }

    @MainActor
    func testDefaultEmpty() {
        let vm = FavoritesViewModel()
        XCTAssertTrue(vm.favorites.isEmpty)
        XCTAssertTrue(vm.folders.isEmpty)
    }

    @MainActor
    func testAddFavorite() {
        let vm = FavoritesViewModel()
        vm.addFavorite(rootPitchClass: 0, chordSymbol: "", frets: [0, 0, 0, 3])

        XCTAssertEqual(vm.favorites.count, 1)
        XCTAssertEqual(vm.favorites.first?.rootPitchClass, 0)
        XCTAssertEqual(vm.favorites.first?.chordSymbol, "")
        XCTAssertEqual(vm.favorites.first?.frets, [0, 0, 0, 3])
    }

    @MainActor
    func testRemoveFavorite() {
        let vm = FavoritesViewModel()
        vm.addFavorite(rootPitchClass: 0, chordSymbol: "", frets: [0, 0, 0, 3])
        let key = vm.favorites.first!.key

        vm.removeFavorite(key: key)
        XCTAssertTrue(vm.favorites.isEmpty)
    }

    @MainActor
    func testIsFavorite() {
        let vm = FavoritesViewModel()
        XCTAssertFalse(vm.isFavorite(rootPitchClass: 0, chordSymbol: "", frets: [0, 0, 0, 3]))

        vm.addFavorite(rootPitchClass: 0, chordSymbol: "", frets: [0, 0, 0, 3])
        XCTAssertTrue(vm.isFavorite(rootPitchClass: 0, chordSymbol: "", frets: [0, 0, 0, 3]))
        XCTAssertFalse(vm.isFavorite(rootPitchClass: 0, chordSymbol: "m", frets: [0, 0, 0, 3]))
    }

    @MainActor
    func testDuplicateAdd() {
        let vm = FavoritesViewModel()
        vm.addFavorite(rootPitchClass: 0, chordSymbol: "", frets: [0, 0, 0, 3])
        vm.addFavorite(rootPitchClass: 0, chordSymbol: "", frets: [0, 0, 0, 3])

        XCTAssertEqual(vm.favorites.count, 1)
    }

    @MainActor
    func testCreateFolder() {
        let vm = FavoritesViewModel()
        vm.createFolder(name: "My Chords")

        XCTAssertEqual(vm.folders.count, 1)
        XCTAssertEqual(vm.folders.first?.name, "My Chords")
    }

    @MainActor
    func testRenameFolder() {
        let vm = FavoritesViewModel()
        vm.createFolder(name: "Old Name")
        let folderId = vm.folders.first!.id

        vm.renameFolder(id: folderId, name: "New Name")
        XCTAssertEqual(vm.folders.first?.name, "New Name")
    }

    @MainActor
    func testDeleteFolderRemovesFromFavorites() {
        let vm = FavoritesViewModel()
        vm.createFolder(name: "Folder")
        let folderId = vm.folders.first!.id

        vm.addFavorite(rootPitchClass: 0, chordSymbol: "", frets: [0, 0, 0, 3])
        let voicingKey = vm.favorites.first!.key
        vm.setFolders(voicingKey: voicingKey, folderIds: [folderId])
        XCTAssertTrue(vm.favorites.first!.folderIds.contains(folderId))

        vm.deleteFolder(id: folderId)
        XCTAssertTrue(vm.folders.isEmpty)
        XCTAssertFalse(vm.favorites.first!.folderIds.contains(folderId))
    }

    @MainActor
    func testFavoritesInFolder() {
        let vm = FavoritesViewModel()
        vm.createFolder(name: "Folder")
        let folderId = vm.folders.first!.id

        vm.addFavorite(rootPitchClass: 0, chordSymbol: "", frets: [0, 0, 0, 3])
        vm.addFavorite(rootPitchClass: 2, chordSymbol: "m", frets: [2, 2, 1, 0])
        let key1 = vm.favorites[0].key
        let key2 = vm.favorites[1].key

        vm.setFolders(voicingKey: key1, folderIds: [folderId])
        vm.setFolders(voicingKey: key2, folderIds: [folderId])

        let inFolder = vm.favoritesInFolder(folderId)
        XCTAssertEqual(inFolder.count, 2)
    }

    @MainActor
    func testReorderInFolder() {
        let vm = FavoritesViewModel()
        vm.createFolder(name: "Folder")
        let folderId = vm.folders.first!.id

        vm.addFavorite(rootPitchClass: 0, chordSymbol: "", frets: [0, 0, 0, 3])
        vm.addFavorite(rootPitchClass: 5, chordSymbol: "", frets: [2, 0, 1, 0])
        let key1 = vm.favorites[0].key
        let key2 = vm.favorites[1].key

        vm.setFolders(voicingKey: key1, folderIds: [folderId])
        vm.setFolders(voicingKey: key2, folderIds: [folderId])

        vm.reorderInFolder(folderId: folderId, orderedKeys: [key2, key1])

        let inFolder = vm.favoritesInFolder(folderId)
        XCTAssertEqual(inFolder.first?.key, key2)
        XCTAssertEqual(inFolder.last?.key, key1)
    }

    @MainActor
    func testExportImportRoundTrip() {
        let vm = FavoritesViewModel()
        vm.addFavorite(rootPitchClass: 0, chordSymbol: "", frets: [0, 0, 0, 3])
        vm.createFolder(name: "Test Folder")
        let exported = vm.exportData()

        UserDefaults.standard.removeObject(forKey: "favorite_voicings")
        UserDefaults.standard.removeObject(forKey: "favorite_folders")

        let vm2 = FavoritesViewModel()
        XCTAssertTrue(vm2.favorites.isEmpty)

        vm2.importData(favorites: exported.favorites, folders: exported.folders)
        XCTAssertEqual(vm2.favorites.count, 1)
        XCTAssertEqual(vm2.folders.count, 1)
        XCTAssertEqual(vm2.folders.first?.name, "Test Folder")
    }
}
