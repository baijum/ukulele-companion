import Foundation

final class SongbookRepository {
    private let userDefaultsKey = "chord_sheets"

    func getAll() -> [StoredSong] {
        guard let data = UserDefaults.standard.data(forKey: userDefaultsKey),
              let decoded = try? JSONDecoder().decode([StoredSong].self, from: data)
        else { return [] }
        return decoded
    }

    func save(_ songs: [StoredSong]) {
        guard let data = try? JSONEncoder().encode(songs) else { return }
        UserDefaults.standard.set(data, forKey: userDefaultsKey)
    }

    func exportData(_ songs: [StoredSong]) -> [[String: Any]] {
        let encoder = JSONEncoder()
        return songs.compactMap { song in
            guard let data = try? encoder.encode(song),
                  let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
            else { return nil }
            return dict
        }
    }

    func importData(_ incoming: [[String: Any]], into songs: inout [StoredSong]) {
        let decoder = JSONDecoder()
        for item in incoming {
            guard let jsonData = try? JSONSerialization.data(withJSONObject: item),
                  let song = try? decoder.decode(StoredSong.self, from: jsonData)
            else { continue }
            if let idx = songs.firstIndex(where: { $0.id == song.id }) {
                if song.updatedAt > songs[idx].updatedAt {
                    songs[idx] = song
                }
            } else {
                songs.append(song)
            }
        }
        save(songs)
    }
}
