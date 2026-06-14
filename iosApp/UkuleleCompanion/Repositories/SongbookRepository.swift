import Foundation

final class SongbookRepository: JSONStorageRepository<StoredSong> {
    init() {
        super.init(key: "chord_sheets")
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
