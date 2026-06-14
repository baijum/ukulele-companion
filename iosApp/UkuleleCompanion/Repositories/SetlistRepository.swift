import Foundation

final class SetlistRepository: JSONStorageRepository<StoredSetlist> {
    init() {
        super.init(key: "setlists")
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
