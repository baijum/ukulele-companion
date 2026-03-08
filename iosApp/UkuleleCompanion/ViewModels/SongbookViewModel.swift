import Foundation
import shared

struct StoredSong: Codable, Identifiable {
    let id: String
    var title: String
    var artist: String
    var content: String
    var key: String
    var capo: Int
    var strumPatternName: String
    var labels: [String]
    var createdAt: Double
    var updatedAt: Double
}

enum SongSortOrder: String, CaseIterable {
    case lastModified = "Last Modified"
    case title = "Title"
    case artist = "Artist"
}

@MainActor
final class SongbookViewModel: ObservableObject {
    @Published var songs: [StoredSong] = []
    @Published var searchQuery = ""
    @Published var sortOrder: SongSortOrder = .lastModified
    @Published var selectedLabels: Set<String> = []

    private let userDefaultsKey = "chord_sheets"

    init() {
        load()
    }

    var allLabels: [String] {
        let labels = songs.flatMap { $0.labels }
        return Array(Set(labels)).sorted()
    }

    var filteredSongs: [StoredSong] {
        var result = songs

        if !searchQuery.isEmpty {
            let q = searchQuery.lowercased()
            result = result.filter {
                $0.title.lowercased().contains(q) ||
                $0.artist.lowercased().contains(q)
            }
        }

        if !selectedLabels.isEmpty {
            result = result.filter { song in
                !selectedLabels.isDisjoint(with: Set(song.labels))
            }
        }

        switch sortOrder {
        case .lastModified:
            result.sort { $0.updatedAt > $1.updatedAt }
        case .title:
            result.sort { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
        case .artist:
            result.sort { $0.artist.localizedCaseInsensitiveCompare($1.artist) == .orderedAscending }
        }

        return result
    }

    func save(song: StoredSong) {
        if let idx = songs.firstIndex(where: { $0.id == song.id }) {
            var updated = song
            updated.updatedAt = Date().timeIntervalSince1970 * 1000
            songs[idx] = updated
        } else {
            songs.insert(song, at: 0)
        }
        persist()
    }

    func delete(id: String) {
        songs.removeAll { $0.id == id }
        persist()
    }

    func importChordPro(text: String) {
        let sheet = ChordProParser.shared.parse(input: text, defaultTitle: "Imported Song")
        let song = StoredSong(
            id: sheet.id,
            title: sheet.title,
            artist: sheet.artist,
            content: sheet.content,
            key: sheet.key,
            capo: Int(sheet.capo),
            strumPatternName: sheet.strumPatternName,
            labels: sheet.labels as! [String],
            createdAt: Double(sheet.createdAt),
            updatedAt: Double(sheet.updatedAt)
        )
        save(song: song)
    }

    func transpose(song: StoredSong, semitones: Int) -> StoredSong {
        let transposed = ChordSheetTranspose.shared.transpose(
            content: song.content,
            semitones: Int32(semitones)
        )
        var updated = song
        updated.content = transposed
        updated.updatedAt = Date().timeIntervalSince1970 * 1000
        return updated
    }

    func formattedDisplay(song: StoredSong) -> String {
        let sheet = toChordSheet(song)
        return ChordSheetFormatter.shared.formatChordsAboveLyrics(sheet: sheet)
    }

    func exportChordPro(song: StoredSong) -> String {
        let sheet = toChordSheet(song)
        return ChordProExporter.shared.export(sheet: sheet)
    }

    private func toChordSheet(_ song: StoredSong) -> ChordSheet {
        ChordSheet(
            id: song.id,
            title: song.title,
            artist: song.artist,
            content: song.content,
            key: song.key,
            capo: Int32(song.capo),
            strumPatternName: song.strumPatternName,
            labels: song.labels,
            createdAt: Int64(song.createdAt),
            updatedAt: Int64(song.updatedAt)
        )
    }

    func importData(_ incoming: [[String: Any]]) {
        let decoder = JSONDecoder()
        for item in incoming {
            guard let jsonData = try? JSONSerialization.data(withJSONObject: item),
                  let song = try? decoder.decode(StoredSong.self, from: jsonData)
            else { continue }
            if let idx = songs.firstIndex(where: { $0.id == song.id }) {
                // Keep the one with the latest updatedAt
                if song.updatedAt > songs[idx].updatedAt {
                    songs[idx] = song
                }
            } else {
                songs.append(song)
            }
        }
        persist()
    }

    func exportData() -> [[String: Any]] {
        let encoder = JSONEncoder()
        return songs.compactMap { song in
            guard let data = try? encoder.encode(song),
                  let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
            else { return nil }
            return dict
        }
    }

    private func load() {
        guard let data = UserDefaults.standard.data(forKey: userDefaultsKey),
              let decoded = try? JSONDecoder().decode([StoredSong].self, from: data)
        else { return }
        songs = decoded
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(songs) else { return }
        UserDefaults.standard.set(data, forKey: userDefaultsKey)
    }
}
