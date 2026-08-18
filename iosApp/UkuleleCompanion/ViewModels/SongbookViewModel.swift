import Foundation
import shared

struct StoredSong: Codable, Identifiable {
    let id: String
    var title: String
    var subtitle: String
    var artist: String
    var content: String
    var key: String
    var capo: Int
    var strumPatternName: String
    var labels: [String]
    var createdAt: Double
    var updatedAt: Double
    var viewCount: Int
    var lastViewedAt: Double
    var totalViewTimeMs: Double

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        title = try container.decode(String.self, forKey: .title)
        subtitle = try container.decodeIfPresent(String.self, forKey: .subtitle) ?? ""
        artist = try container.decode(String.self, forKey: .artist)
        content = try container.decode(String.self, forKey: .content)
        key = try container.decode(String.self, forKey: .key)
        capo = try container.decode(Int.self, forKey: .capo)
        strumPatternName = try container.decode(String.self, forKey: .strumPatternName)
        labels = try container.decode([String].self, forKey: .labels)
        createdAt = try container.decode(Double.self, forKey: .createdAt)
        updatedAt = try container.decode(Double.self, forKey: .updatedAt)
        viewCount = try container.decodeIfPresent(Int.self, forKey: .viewCount) ?? 0
        lastViewedAt = try container.decodeIfPresent(Double.self, forKey: .lastViewedAt) ?? 0
        totalViewTimeMs = try container.decodeIfPresent(Double.self, forKey: .totalViewTimeMs) ?? 0
    }

    init(id: String, title: String, subtitle: String = "", artist: String, content: String,
         key: String, capo: Int, strumPatternName: String, labels: [String],
         createdAt: Double, updatedAt: Double,
         viewCount: Int = 0, lastViewedAt: Double = 0, totalViewTimeMs: Double = 0) {
        self.id = id
        self.title = title
        self.subtitle = subtitle
        self.artist = artist
        self.content = content
        self.key = key
        self.capo = capo
        self.strumPatternName = strumPatternName
        self.labels = labels
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.viewCount = viewCount
        self.lastViewedAt = lastViewedAt
        self.totalViewTimeMs = totalViewTimeMs
    }
}

enum SongSortOrder: String, CaseIterable {
    case lastModified = "Last Modified"
    case dateAdded = "Date Added"
    case title = "Title"
    case artist = "Artist"
}

@MainActor
final class SongbookViewModel: ObservableObject {
    @Published var songs: [StoredSong] = []
    @Published var searchQuery = ""
    @Published var sortOrder: SongSortOrder = .lastModified
    @Published var selectedLabels: Set<String> = []

    private let repository: SongbookRepository

    init(repository: SongbookRepository = SongbookRepository()) {
        self.repository = repository
        songs = repository.getAll()
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
                selectedLabels.isSubset(of: Set(song.labels))
            }
        }

        switch sortOrder {
        case .lastModified:
            result.sort { $0.updatedAt > $1.updatedAt }
        case .dateAdded:
            result.sort { $0.createdAt > $1.createdAt }
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
        repository.save(songs)
    }

    func duplicate(song: StoredSong) -> StoredSong {
        let now = Date().timeIntervalSince1970 * 1000
        let copy = StoredSong(
            id: UUID().uuidString,
            title: "\(song.title) (Copy)",
            subtitle: song.subtitle,
            artist: song.artist,
            content: song.content,
            key: song.key,
            capo: song.capo,
            strumPatternName: song.strumPatternName,
            labels: song.labels,
            createdAt: now,
            updatedAt: now
        )
        songs.insert(copy, at: 0)
        repository.save(songs)
        return copy
    }

    func delete(id: String) {
        songs.removeAll { $0.id == id }
        repository.save(songs)
    }

    func updateLabels(id: String, labels: [String]) {
        guard let idx = songs.firstIndex(where: { $0.id == id }) else { return }
        songs[idx].labels = labels
        songs[idx].updatedAt = Date().timeIntervalSince1970 * 1000
        repository.save(songs)
    }

    func updateStrumPattern(id: String, patternName: String) {
        guard let idx = songs.firstIndex(where: { $0.id == id }) else { return }
        songs[idx].strumPatternName = patternName
        songs[idx].updatedAt = Date().timeIntervalSince1970 * 1000
        repository.save(songs)
    }

    func recordView(id: String) {
        guard let idx = songs.firstIndex(where: { $0.id == id }) else { return }
        songs[idx].viewCount += 1
        songs[idx].lastViewedAt = Date().timeIntervalSince1970 * 1000
        repository.save(songs)
    }

    func recordViewTime(id: String, elapsedMs: Double) {
        guard elapsedMs > 0,
              let idx = songs.firstIndex(where: { $0.id == id }) else { return }
        songs[idx].totalViewTimeMs += elapsedMs
        repository.save(songs)
    }

    /// Derives a song title from an imported file name.
    ///
    /// Strips any path and document-ID prefix before dropping the extension, so
    /// a URI or document ID can never surface as the title (issue #500).
    ///
    /// - Parameter filename: The resolved file name, or `nil` when unavailable.
    /// - Returns: A display title, falling back to `"Imported Song"`.
    nonisolated static func titleFromFilename(_ filename: String?) -> String {
        guard let filename else { return defaultImportTitle }
        let base = filename
            .components(separatedBy: "/").last?
            .components(separatedBy: ":").last?
            .replacingOccurrences(of: "_", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let withoutExtension = (base as NSString).deletingPathExtension
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return withoutExtension.isEmpty ? defaultImportTitle : withoutExtension
    }

    private nonisolated static let defaultImportTitle = "Imported Song"

    func importPlainText(content: String, filename: String?) {
        let now = Date().timeIntervalSince1970 * 1000
        let song = StoredSong(
            id: UUID().uuidString,
            title: Self.titleFromFilename(filename),
            artist: "",
            content: content.trimmingCharacters(in: .whitespacesAndNewlines),
            key: "",
            capo: 0,
            strumPatternName: "",
            labels: [],
            createdAt: now,
            updatedAt: now
        )
        save(song: song)
    }

    /// Imports a song from ChordPro text.
    ///
    /// - Parameters:
    ///   - text: The raw ChordPro content.
    ///   - filename: Optional file name used as a fallback title when the
    ///     content carries no `{title}` directive.
    func importChordPro(text: String, filename: String? = nil) {
        let sheet = ChordProParser.shared.parse(
            input: text,
            defaultTitle: Self.titleFromFilename(filename)
        )
        let song = StoredSong(
            id: sheet.id,
            title: sheet.title,
            subtitle: sheet.subtitle,
            artist: sheet.artist,
            content: sheet.content,
            key: sheet.key,
            capo: Int(sheet.capo),
            strumPatternName: sheet.strumPatternName,
            labels: sheet.labels.asStrings,
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
            subtitle: song.subtitle,
            artist: song.artist,
            content: song.content,
            key: song.key,
            capo: Int32(song.capo),
            strumPatternName: song.strumPatternName,
            labels: song.labels,
            createdAt: Int64(song.createdAt),
            updatedAt: Int64(song.updatedAt),
            viewCount: Int32(song.viewCount),
            lastViewedAt: Int64(song.lastViewedAt),
            totalViewTimeMs: Int64(song.totalViewTimeMs)
        )
    }

    func importData(_ incoming: [[String: Any]]) {
        repository.importData(incoming, into: &songs)
    }

    func exportData() -> [[String: Any]] {
        repository.exportData(songs)
    }
}
