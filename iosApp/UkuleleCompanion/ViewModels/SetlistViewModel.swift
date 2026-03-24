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

    private let userDefaultsKey = "setlists"

    init() {
        load()
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
        persist()
    }

    func rename(id: String, newName: String) {
        guard let idx = setlists.firstIndex(where: { $0.id == id }) else { return }
        setlists[idx].name = newName
        setlists[idx].updatedAt = Date().timeIntervalSince1970 * 1000
        persist()
    }

    func delete(id: String) {
        setlists.removeAll { $0.id == id }
        persist()
    }

    func addSong(setlistId: String, songId: String) {
        guard let idx = setlists.firstIndex(where: { $0.id == setlistId }) else { return }
        guard !setlists[idx].songIds.contains(songId) else { return }
        setlists[idx].songIds.append(songId)
        setlists[idx].updatedAt = Date().timeIntervalSince1970 * 1000
        persist()
    }

    func removeSong(setlistId: String, songId: String) {
        guard let idx = setlists.firstIndex(where: { $0.id == setlistId }) else { return }
        setlists[idx].songIds.removeAll { $0 == songId }
        setlists[idx].updatedAt = Date().timeIntervalSince1970 * 1000
        persist()
    }

    func moveSong(setlistId: String, from: Int, to: Int) {
        guard let idx = setlists.firstIndex(where: { $0.id == setlistId }) else { return }
        guard from >= 0, from < setlists[idx].songIds.count,
              to >= 0, to < setlists[idx].songIds.count else { return }
        let item = setlists[idx].songIds.remove(at: from)
        setlists[idx].songIds.insert(item, at: to)
        setlists[idx].updatedAt = Date().timeIntervalSince1970 * 1000
        persist()
    }

    func exportData() -> [[String: Any]] {
        let encoder = JSONEncoder()
        return setlists.compactMap { setlist in
            guard let data = try? encoder.encode(setlist),
                  let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
            else { return nil }
            return dict
        }
    }

    func importData(_ incoming: [[String: Any]]) {
        let decoder = JSONDecoder()
        for item in incoming {
            guard let jsonData = try? JSONSerialization.data(withJSONObject: item),
                  let setlist = try? decoder.decode(StoredSetlist.self, from: jsonData)
            else { continue }
            if let idx = setlists.firstIndex(where: { $0.id == setlist.id }) {
                if setlist.updatedAt > setlists[idx].updatedAt {
                    setlists[idx] = setlist
                }
            } else {
                setlists.append(setlist)
            }
        }
        persist()
    }

    private func load() {
        guard let data = UserDefaults.standard.data(forKey: userDefaultsKey),
              let decoded = try? JSONDecoder().decode([StoredSetlist].self, from: data)
        else { return }
        setlists = decoded
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(setlists) else { return }
        UserDefaults.standard.set(data, forKey: userDefaultsKey)
    }
}
