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

    init(repository: SetlistRepository = SetlistRepository()) {
        self.repository = repository
        setlists = repository.getAll()
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

    func moveSong(setlistId: String, from: Int, to: Int) {
        guard let idx = setlists.firstIndex(where: { $0.id == setlistId }) else { return }
        guard from >= 0, from < setlists[idx].songIds.count,
              to >= 0, to < setlists[idx].songIds.count else { return }
        let item = setlists[idx].songIds.remove(at: from)
        setlists[idx].songIds.insert(item, at: to)
        setlists[idx].updatedAt = Date().timeIntervalSince1970 * 1000
        repository.save(setlists)
    }

    func exportData() -> [[String: Any]] {
        repository.exportData(setlists)
    }

    func importData(_ incoming: [[String: Any]]) {
        repository.importData(incoming, into: &setlists)
    }
}
