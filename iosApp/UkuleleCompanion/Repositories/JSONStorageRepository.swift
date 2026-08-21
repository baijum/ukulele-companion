import Foundation

/// Generic base class for UserDefaults-backed JSON array storage.
///
/// Provides `getAll()`, `save(_:)`, and `exportData(_:)` with a single
/// implementation. Subclasses only need to implement merge logic in
/// `importData(_:into:)`.
///
/// The whole list is one blob per key, so a payload that fails to decode is
/// all-or-nothing: one bad byte in `chord_sheets` loses every song rather than
/// one. That is why reads and writes go through the backup-and-quarantine rule
/// in ``UserDefaults/readWithBackupFallback(key:tryParse:)`` and
/// ``UserDefaults/writeWithBackupRotation(key:raw:isReadable:)`` rather than
/// decoding with `try?` and answering with an empty array (#569).
class JSONStorageRepository<T: Codable> {
    let userDefaultsKey: String
    private let defaults: UserDefaults

    init(key: String, defaults: UserDefaults = .standard) {
        self.userDefaultsKey = key
        self.defaults = defaults
    }

    /// Reads the list, falling back to the backup copy when the primary payload
    /// cannot be decoded.
    ///
    /// An empty list is the answer both to a store that was never written and to
    /// one whose copies are both unreadable: with no legacy layout to migrate,
    /// the two cases lead to the same place. They differ on disk — the second
    /// has left the unreadable bytes in the quarantine slot.
    func getAll() -> [T] {
        switch defaults.readWithBackupFallback(key: userDefaultsKey, tryParse: Self.tryParse) {
        case let .loaded(items): return items
        case .absent, .unrecoverable: return []
        }
    }

    /// Writes the list, rotating the outgoing payload into the backup slot.
    func save(_ items: [T]) {
        guard let data = try? JSONEncoder().encode(items) else { return }
        defaults.writeWithBackupRotation(key: userDefaultsKey, raw: data) {
            Self.tryParse($0) != nil
        }
    }

    private static func tryParse(_ data: Data) -> [T]? {
        try? JSONDecoder().decode([T].self, from: data)
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
