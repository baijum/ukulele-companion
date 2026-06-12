import Foundation

final class SetlistRepository {
    private let userDefaultsKey = "setlists"

    func getAll() -> [StoredSetlist] {
        guard let data = UserDefaults.standard.data(forKey: userDefaultsKey),
              let decoded = try? JSONDecoder().decode([StoredSetlist].self, from: data)
        else { return [] }
        return decoded
    }

    func save(_ setlists: [StoredSetlist]) {
        guard let data = try? JSONEncoder().encode(setlists) else { return }
        UserDefaults.standard.set(data, forKey: userDefaultsKey)
    }

    func exportData(_ setlists: [StoredSetlist]) -> [[String: Any]] {
        let encoder = JSONEncoder()
        return setlists.compactMap { setlist in
            guard let data = try? encoder.encode(setlist),
                  let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
            else { return nil }
            return dict
        }
    }

    func importData(_ incoming: [[String: Any]], into setlists: inout [StoredSetlist]) {
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
        save(setlists)
    }
}
