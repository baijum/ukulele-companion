import Foundation
import shared

struct StoredSetlist: Codable, Identifiable {
    let id: String
    var name: String
    var songIds: [String]
    var createdAt: Double
    var updatedAt: Double
}

@MainActor
final class SetlistViewModel: ObservableObject {
    @Published var setlists: [StoredSetlist] = []

    private let repository: SetlistRepository
    // `nonisolated(unsafe)` so deinit can remove the observer; the token is only
    // touched from init and deinit, which never overlap.
    private nonisolated(unsafe) var songsDeletedObserver: NSObjectProtocol?

    init(
        repository: SetlistRepository = SetlistRepository(),
        songbookRepository: SongbookRepository = SongbookRepository()
    ) {
        self.repository = repository
        setlists = repository.getAll()
        // One-time reconciliation at launch: strip any songIds entry that no
        // longer resolves in the song library. A dead ID (a song deleted in an
        // earlier version, or one a restored backup's import skipped) would
        // otherwise persist forever and offset every future delete/reorder,
        // since the rendered list — built with compactMap — is shorter than the
        // raw songIds array (issue #602). Nothing else reconciles at launch: the
        // #594 observer only fires on live deletions.
        reconcileWithLibrary(validSongIds: Set(songbookRepository.getAll().map { $0.id }))
        // When a song is deleted from the Songbook, drop it from every setlist
        // right away so dead IDs cannot resurface in a later save (issue #594).
        songsDeletedObserver = NotificationCenter.default.addObserver(
            forName: .songsDeletedFromLibrary,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            let ids = notification.userInfo?[Notification.songsDeletedIds] as? [String] ?? []
            Task { @MainActor [weak self] in
                self?.purgeDeletedSongs(ids)
            }
        }
    }

    deinit {
        if let observer = songsDeletedObserver {
            NotificationCenter.default.removeObserver(observer)
        }
    }

    func create(name: String) {
        let now = Date().timeIntervalSince1970 * 1000
        let setlist = StoredSetlist(
            id: UUID().uuidString,
            name: name,
            songIds: [],
            createdAt: now,
            updatedAt: now
        )
        setlists.insert(setlist, at: 0)
        repository.save(setlists)
    }

    func rename(id: String, newName: String) {
        guard let idx = setlists.firstIndex(where: { $0.id == id }) else { return }
        setlists[idx].name = newName
        setlists[idx].updatedAt = Date().timeIntervalSince1970 * 1000
        repository.save(setlists)
    }

    func delete(id: String) {
        setlists.removeAll { $0.id == id }
        repository.save(setlists)
    }

    func addSong(setlistId: String, songId: String) {
        guard let idx = setlists.firstIndex(where: { $0.id == setlistId }) else { return }
        guard !setlists[idx].songIds.contains(songId) else { return }
        setlists[idx].songIds.append(songId)
        setlists[idx].updatedAt = Date().timeIntervalSince1970 * 1000
        repository.save(setlists)
    }

    func removeSong(setlistId: String, songId: String) {
        guard let idx = setlists.firstIndex(where: { $0.id == setlistId }) else { return }
        setlists[idx].songIds.removeAll { $0 == songId }
        setlists[idx].updatedAt = Date().timeIntervalSince1970 * 1000
        repository.save(setlists)
    }

    /// Moves `songId` by `offset` positions within the persisted song order
    /// (`offset` of -1 moves it up one, +1 down one).
    ///
    /// Keyed by song ID rather than by display index so it cannot be desynced
    /// from the stored `songIds` array — e.g. when an unresolvable ID makes the
    /// rendered list (built with `compactMap`) shorter than the raw array and
    /// the visible offsets no longer match the persisted ones (issue #602).
    func moveSong(setlistId: String, songId: String, offset: Int) {
        guard offset != 0 else { return }
        guard let idx = setlists.firstIndex(where: { $0.id == setlistId }) else { return }
        guard let currentIndex = setlists[idx].songIds.firstIndex(of: songId) else { return }
        let targetIndex = currentIndex + offset
        guard targetIndex >= 0, targetIndex < setlists[idx].songIds.count else { return }
        let item = setlists[idx].songIds.remove(at: currentIndex)
        setlists[idx].songIds.insert(item, at: targetIndex)
        setlists[idx].updatedAt = Date().timeIntervalSince1970 * 1000
        repository.save(setlists)
    }

    /// Drops every `songIds` entry not present in `validSongIds`. Persisted and
    /// in-memory setlists are updated in place; only setlists that actually held
    /// a dead ID are rewritten, so clean setlists keep their `updatedAt`.
    private func reconcileWithLibrary(validSongIds: Set<String>) {
        var changed = false
        for idx in setlists.indices
        where setlists[idx].songIds.contains(where: { !validSongIds.contains($0) }) {
            setlists[idx].songIds.removeAll { !validSongIds.contains($0) }
            setlists[idx].updatedAt = Date().timeIntervalSince1970 * 1000
            changed = true
        }
        if changed {
            repository.save(setlists)
        }
    }

    /// Strips deleted library songs out of every setlist — persisted and in
    /// memory — so a dead ID can neither resurface in a later save nor silently
    /// vanish from the rendered list (issue #594).
    func purgeDeletedSongs(_ deletedSongIds: [String]) {
        guard !deletedSongIds.isEmpty else { return }
        let deleted = Set(deletedSongIds)
        for idx in setlists.indices
        where setlists[idx].songIds.contains(where: deleted.contains) {
            setlists[idx].songIds.removeAll { deleted.contains($0) }
            setlists[idx].updatedAt = Date().timeIntervalSince1970 * 1000
        }
        repository.save(setlists)
    }

    func exportData() -> [[String: Any]] {
        repository.exportData(setlists)
    }

    func importData(_ incoming: [[String: Any]]) {
        repository.importData(incoming, into: &setlists)
    }
}
