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

    private let favoritesKey = "favorite_voicings"
    private let foldersKey = "favorite_folders"

    init() {
        loadFavorites()
        loadFolders()
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
        saveFavorites()
    }

    func removeFavorite(key: String) {
        favorites.removeAll { $0.key == key }
        // Clean up folder orderings
        for i in folders.indices {
            folders[i].voicingOrder.removeAll { $0 == key }
        }
        saveFavorites()
        saveFolders()
    }

    func isFavorite(rootPitchClass: Int, chordSymbol: String, frets: [Int]) -> Bool {
        let key = "\(rootPitchClass)|\(chordSymbol)|\(frets.map(String.init).joined(separator: ","))"
        return favorites.contains { $0.key == key }
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
        saveFolders()
    }

    func renameFolder(id: String, name: String) {
        guard let idx = folders.firstIndex(where: { $0.id == id }) else { return }
        folders[idx].name = name
        saveFolders()
    }

    func deleteFolder(id: String) {
        folders.removeAll { $0.id == id }
        for i in favorites.indices {
            favorites[i].folderIds.removeAll { $0 == id }
        }
        saveFolders()
        saveFavorites()
    }

    func setFolders(voicingKey: String, folderIds: [String]) {
        guard let idx = favorites.firstIndex(where: { $0.key == voicingKey }) else { return }
        let oldIds = Set(favorites[idx].folderIds)
        let newIds = Set(folderIds)
        favorites[idx].folderIds = folderIds

        // Add to newly assigned folders
        for folderId in newIds.subtracting(oldIds) {
            if let fi = folders.firstIndex(where: { $0.id == folderId }),
               !folders[fi].voicingOrder.contains(voicingKey) {
                folders[fi].voicingOrder.append(voicingKey)
            }
        }
        // Remove from unassigned folders
        for folderId in oldIds.subtracting(newIds) {
            if let fi = folders.firstIndex(where: { $0.id == folderId }) {
                folders[fi].voicingOrder.removeAll { $0 == voicingKey }
            }
        }

        saveFavorites()
        saveFolders()
    }

    func reorderInFolder(folderId: String, orderedKeys: [String]) {
        guard let idx = folders.firstIndex(where: { $0.id == folderId }) else { return }
        folders[idx].voicingOrder = orderedKeys
        saveFolders()
    }

    func favoritesInFolder(_ folderId: String) -> [FavoriteVoicingData] {
        guard let folder = folders.first(where: { $0.id == folderId }) else { return [] }
        let inFolder = favorites.filter { $0.folderIds.contains(folderId) }
        // Order by folder's voicingOrder
        let keyOrder = folder.voicingOrder
        return inFolder.sorted { a, b in
            let ai = keyOrder.firstIndex(of: a.key) ?? Int.max
            let bi = keyOrder.firstIndex(of: b.key) ?? Int.max
            return ai < bi
        }
    }

    // MARK: - Persistence

    private func saveFavorites() {
        guard let data = try? JSONEncoder().encode(favorites) else { return }
        UserDefaults.standard.set(data, forKey: favoritesKey)
    }

    private func loadFavorites() {
        guard let data = UserDefaults.standard.data(forKey: favoritesKey),
              let decoded = try? JSONDecoder().decode([FavoriteVoicingData].self, from: data)
        else { return }
        favorites = decoded
    }

    private func saveFolders() {
        guard let data = try? JSONEncoder().encode(folders) else { return }
        UserDefaults.standard.set(data, forKey: foldersKey)
    }

    private func loadFolders() {
        guard let data = UserDefaults.standard.data(forKey: foldersKey),
              let decoded = try? JSONDecoder().decode([FavoriteFolderData].self, from: data)
        else { return }
        folders = decoded
    }

    // MARK: - Export/Import for Backup

    func exportData() -> (favorites: [[String: Any]], folders: [[String: Any]]) {
        let favDicts: [[String: Any]] = favorites.map { f in
            ["rootPitchClass": f.rootPitchClass, "chordSymbol": f.chordSymbol,
             "frets": f.frets, "addedAt": f.addedAt, "folderIds": f.folderIds]
        }
        let folderDicts: [[String: Any]] = folders.map { f in
            ["id": f.id, "name": f.name, "createdAt": f.createdAt,
             "voicingOrder": f.voicingOrder]
        }
        return (favDicts, folderDicts)
    }

    func importData(favorites: [[String: Any]], folders: [[String: Any]]) {
        for dict in favorites {
            guard let rpc = dict["rootPitchClass"] as? Int,
                  let sym = dict["chordSymbol"] as? String,
                  let frets = dict["frets"] as? [Int],
                  let addedAt = dict["addedAt"] as? Double
            else { continue }
            let folderIds = dict["folderIds"] as? [String] ?? []
            let fav = FavoriteVoicingData(rootPitchClass: rpc, chordSymbol: sym,
                                          frets: frets, addedAt: addedAt, folderIds: folderIds)
            if !self.favorites.contains(where: { $0.key == fav.key }) {
                self.favorites.append(fav)
            }
        }
        for dict in folders {
            guard let id = dict["id"] as? String,
                  let name = dict["name"] as? String,
                  let createdAt = dict["createdAt"] as? Double
            else { continue }
            let order = dict["voicingOrder"] as? [String] ?? []
            if !self.folders.contains(where: { $0.id == id }) {
                self.folders.append(FavoriteFolderData(id: id, name: name,
                                                       createdAt: createdAt, voicingOrder: order))
            }
        }
        saveFavorites()
        saveFolders()
    }
}
