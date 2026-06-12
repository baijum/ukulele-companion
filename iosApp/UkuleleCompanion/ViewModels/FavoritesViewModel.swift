import Foundation
import shared

struct FavoriteVoicingData: Codable, Identifiable {
    let rootPitchClass: Int
    let chordSymbol: String
    let frets: [Int]
    let addedAt: Double
    var folderIds: [String]

    var id: String { key }
    var key: String { "\(rootPitchClass)|\(chordSymbol)|\(frets.map(String.init).joined(separator: ","))" }

    var chordName: String {
        Notes.shared.pitchClassToName(pitchClass: Int32(rootPitchClass)) + chordSymbol
    }
}

struct FavoriteFolderData: Codable, Identifiable {
    let id: String
    var name: String
    let createdAt: Double
    var voicingOrder: [String]
}

@MainActor
final class FavoritesViewModel: ObservableObject {
    @Published var favorites: [FavoriteVoicingData] = []
    @Published var folders: [FavoriteFolderData] = []

    private let repository: FavoritesRepository

    init(repository: FavoritesRepository = FavoritesRepository()) {
        self.repository = repository
        favorites = repository.getAll()
        folders = repository.getAllFolders()
    }

    // MARK: - Favorites

    func addFavorite(rootPitchClass: Int, chordSymbol: String, frets: [Int]) {
        let fav = FavoriteVoicingData(
            rootPitchClass: rootPitchClass,
            chordSymbol: chordSymbol,
            frets: frets,
            addedAt: Date().timeIntervalSince1970,
            folderIds: []
        )
        guard !favorites.contains(where: { $0.key == fav.key }) else { return }
        favorites.insert(fav, at: 0)
        repository.save(favorites)
    }

    func removeFavorite(key: String) {
        favorites.removeAll { $0.key == key }
        for i in folders.indices {
            folders[i].voicingOrder.removeAll { $0 == key }
        }
        repository.save(favorites)
        repository.saveFolders(folders)
    }

    func removeFavorite(rootPitchClass: Int, chordSymbol: String, frets: [Int]) {
        let key = "\(rootPitchClass)|\(chordSymbol)|\(frets.map(String.init).joined(separator: ","))"
        removeFavorite(key: key)
    }

    func isFavorite(rootPitchClass: Int, chordSymbol: String, frets: [Int]) -> Bool {
        let key = "\(rootPitchClass)|\(chordSymbol)|\(frets.map(String.init).joined(separator: ","))"
        return favorites.contains { $0.key == key }
    }

    func folderIdsForVoicing(rootPitchClass: Int, chordSymbol: String, frets: [Int]) -> [String] {
        let key = "\(rootPitchClass)|\(chordSymbol)|\(frets.map(String.init).joined(separator: ","))"
        return favorites.first { $0.key == key }?.folderIds ?? []
    }

    func saveFavoriteToFolders(rootPitchClass: Int, chordSymbol: String, frets: [Int], folderIds: [String]) {
        let key = "\(rootPitchClass)|\(chordSymbol)|\(frets.map(String.init).joined(separator: ","))"
        if let idx = favorites.firstIndex(where: { $0.key == key }) {
            let oldIds = Set(favorites[idx].folderIds)
            let newIds = Set(folderIds)
            favorites[idx].folderIds = folderIds

            for folderId in newIds.subtracting(oldIds) {
                if let fi = folders.firstIndex(where: { $0.id == folderId }),
                   !folders[fi].voicingOrder.contains(key) {
                    folders[fi].voicingOrder.append(key)
                }
            }
            for folderId in oldIds.subtracting(newIds) {
                if let fi = folders.firstIndex(where: { $0.id == folderId }) {
                    folders[fi].voicingOrder.removeAll { $0 == key }
                }
            }
        } else {
            let fav = FavoriteVoicingData(
                rootPitchClass: rootPitchClass,
                chordSymbol: chordSymbol,
                frets: frets,
                addedAt: Date().timeIntervalSince1970,
                folderIds: folderIds
            )
            favorites.insert(fav, at: 0)

            for folderId in folderIds {
                if let fi = folders.firstIndex(where: { $0.id == folderId }),
                   !folders[fi].voicingOrder.contains(key) {
                    folders[fi].voicingOrder.append(key)
                }
            }
        }
        repository.save(favorites)
        repository.saveFolders(folders)
    }

    // MARK: - Folders

    func createFolder(name: String) {
        let folder = FavoriteFolderData(
            id: UUID().uuidString,
            name: name,
            createdAt: Date().timeIntervalSince1970,
            voicingOrder: []
        )
        folders.append(folder)
        repository.saveFolders(folders)
    }

    func renameFolder(id: String, name: String) {
        guard let idx = folders.firstIndex(where: { $0.id == id }) else { return }
        folders[idx].name = name
        repository.saveFolders(folders)
    }

    func deleteFolder(id: String) {
        folders.removeAll { $0.id == id }
        for i in favorites.indices {
            favorites[i].folderIds.removeAll { $0 == id }
        }
        repository.saveFolders(folders)
        repository.save(favorites)
    }

    func setFolders(voicingKey: String, folderIds: [String]) {
        guard let idx = favorites.firstIndex(where: { $0.key == voicingKey }) else { return }
        let oldIds = Set(favorites[idx].folderIds)
        let newIds = Set(folderIds)
        favorites[idx].folderIds = folderIds

        for folderId in newIds.subtracting(oldIds) {
            if let fi = folders.firstIndex(where: { $0.id == folderId }),
               !folders[fi].voicingOrder.contains(voicingKey) {
                folders[fi].voicingOrder.append(voicingKey)
            }
        }
        for folderId in oldIds.subtracting(newIds) {
            if let fi = folders.firstIndex(where: { $0.id == folderId }) {
                folders[fi].voicingOrder.removeAll { $0 == voicingKey }
            }
        }

        repository.save(favorites)
        repository.saveFolders(folders)
    }

    func reorderInFolder(folderId: String, orderedKeys: [String]) {
        guard let idx = folders.firstIndex(where: { $0.id == folderId }) else { return }
        folders[idx].voicingOrder = orderedKeys
        repository.saveFolders(folders)
    }

    func favoritesInFolder(_ folderId: String) -> [FavoriteVoicingData] {
        guard let folder = folders.first(where: { $0.id == folderId }) else { return [] }
        let inFolder = favorites.filter { $0.folderIds.contains(folderId) }
        let keyOrder = folder.voicingOrder
        return inFolder.sorted { a, b in
            let ai = keyOrder.firstIndex(of: a.key) ?? Int.max
            let bi = keyOrder.firstIndex(of: b.key) ?? Int.max
            return ai < bi
        }
    }

    // MARK: - Export/Import for Backup

    func exportData() -> (favorites: [[String: Any]], folders: [[String: Any]]) {
        repository.exportData(favorites: favorites, folders: folders)
    }

    func importData(favorites: [[String: Any]], folders: [[String: Any]]) {
        repository.importData(favorites: favorites, folders: folders,
                              existing: &self.favorites, existingFolders: &self.folders)
    }
}
