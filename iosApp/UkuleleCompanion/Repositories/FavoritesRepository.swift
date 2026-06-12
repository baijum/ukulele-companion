import Foundation

final class FavoritesRepository {
    private let favoritesKey = "favorite_voicings"
    private let foldersKey = "favorite_folders"

    func getAll() -> [FavoriteVoicingData] {
        guard let data = UserDefaults.standard.data(forKey: favoritesKey),
              let decoded = try? JSONDecoder().decode([FavoriteVoicingData].self, from: data)
        else { return [] }
        return decoded
    }

    func save(_ favorites: [FavoriteVoicingData]) {
        guard let data = try? JSONEncoder().encode(favorites) else { return }
        UserDefaults.standard.set(data, forKey: favoritesKey)
    }

    func getAllFolders() -> [FavoriteFolderData] {
        guard let data = UserDefaults.standard.data(forKey: foldersKey),
              let decoded = try? JSONDecoder().decode([FavoriteFolderData].self, from: data)
        else { return [] }
        return decoded
    }

    func saveFolders(_ folders: [FavoriteFolderData]) {
        guard let data = try? JSONEncoder().encode(folders) else { return }
        UserDefaults.standard.set(data, forKey: foldersKey)
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
