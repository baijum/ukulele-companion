import Foundation

/// Storage for favourited voicings and the folders they are filed under.
///
/// This class predates `JSONStorageRepository` and keeps two stores of its own,
/// so it cannot inherit that class's `getAll`/`save`. Routing both of them
/// through the same backup-and-quarantine helper is what stops the rule from
/// being written out a second time and drifting — the drift between two
/// hand-written copies is what Android's #553 and #560 were.
final class FavoritesRepository {
    private let favoritesKey = "favorite_voicings"
    private let foldersKey = "favorite_folders"
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func getAll() -> [FavoriteVoicingData] {
        load(favoritesKey)
    }

    func save(_ favorites: [FavoriteVoicingData]) {
        persist(favorites, to: favoritesKey)
    }

    func getAllFolders() -> [FavoriteFolderData] {
        load(foldersKey)
    }

    func saveFolders(_ folders: [FavoriteFolderData]) {
        persist(folders, to: foldersKey)
    }

    /// Reads one of the two stores, falling back to its backup copy when the
    /// primary payload cannot be decoded. An empty list is the answer both to a
    /// store that was never written and to one whose copies are both
    /// unreadable; only the second leaves quarantined bytes behind.
    private func load<Element: Decodable>(_ key: String) -> [Element] {
        let parse: (Data) -> [Element]? = { try? JSONDecoder().decode([Element].self, from: $0) }
        switch defaults.readWithBackupFallback(key: key, tryParse: parse) {
        case let .loaded(items): return items
        case .absent, .unrecoverable: return []
        }
    }

    /// Writes one of the two stores, rotating the outgoing payload into its
    /// backup slot.
    private func persist<Element: Codable>(_ items: [Element], to key: String) {
        guard let data = try? JSONEncoder().encode(items) else { return }
        defaults.writeWithBackupRotation(key: key, raw: data) {
            (try? JSONDecoder().decode([Element].self, from: $0)) != nil
        }
    }

    func exportData(favorites: [FavoriteVoicingData], folders: [FavoriteFolderData]) -> (favorites: [[String: Any]], folders: [[String: Any]]) {
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

    func importData(favorites: [[String: Any]], folders: [[String: Any]],
                    existing: inout [FavoriteVoicingData], existingFolders: inout [FavoriteFolderData]) {
        for dict in favorites {
            guard let rpc = dict["rootPitchClass"] as? Int,
                  (0...11).contains(rpc),
                  let sym = dict["chordSymbol"] as? String,
                  let frets = dict["frets"] as? [Int],
                  let addedAt = dict["addedAt"] as? Double
            else { continue }
            let folderIds = dict["folderIds"] as? [String] ?? []
            let fav = FavoriteVoicingData(rootPitchClass: rpc, chordSymbol: sym,
                                          frets: frets, addedAt: addedAt, folderIds: folderIds)
            if !existing.contains(where: { $0.key == fav.key }) {
                existing.append(fav)
            }
        }
        for dict in folders {
            guard let id = dict["id"] as? String,
                  let name = dict["name"] as? String,
                  let createdAt = dict["createdAt"] as? Double
            else { continue }
            let order = dict["voicingOrder"] as? [String] ?? []
            if !existingFolders.contains(where: { $0.id == id }) {
                existingFolders.append(FavoriteFolderData(id: id, name: name,
                                                           createdAt: createdAt, voicingOrder: order))
            }
        }
        save(existing)
        saveFolders(existingFolders)
    }
}
