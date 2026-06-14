import Foundation

/// Generic base class for UserDefaults-backed JSON array storage.
///
/// Provides `getAll()`, `save(_:)`, and `exportData(_:)` with a single
/// implementation. Subclasses only need to implement merge logic in
/// `importData(_:into:)`.
class JSONStorageRepository<T: Codable> {
    let userDefaultsKey: String
    private let defaults: UserDefaults

    init(key: String, defaults: UserDefaults = .standard) {
        self.userDefaultsKey = key
        self.defaults = defaults
    }

    func getAll() -> [T] {
        guard let data = defaults.data(forKey: userDefaultsKey),
              let decoded = try? JSONDecoder().decode([T].self, from: data)
        else { return [] }
        return decoded
    }

    func save(_ items: [T]) {
        guard let data = try? JSONEncoder().encode(items) else { return }
        defaults.set(data, forKey: userDefaultsKey)
    }

    func exportData(_ items: [T]) -> [[String: Any]] {
        let encoder = JSONEncoder()
        return items.compactMap { item in
            guard let data = try? encoder.encode(item),
                  let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
            else { return nil }
            return dict
        }
    }
}
