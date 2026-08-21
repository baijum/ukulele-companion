import Foundation

/// The outcome of reading a payload that is protected by a backup slot.
///
/// The three cases are kept apart because callers genuinely treat them
/// differently: a store with nothing written yet is untouched, whereas one
/// whose copies are both unreadable has already lost its data and can only
/// start over. Today every iOS caller answers both with the same default, but
/// the distinction is what a caller with a legacy layout to migrate would need,
/// and it is the difference the quarantine slot and its log line record.
enum BackupRead<T> {
    /// A payload was read, from either the primary slot or the backup.
    case loaded(T)

    /// Nothing has ever been written to the primary slot.
    case absent

    /// Both copies were unreadable. The primary's raw bytes have been quarantined.
    case unrecoverable
}

extension UserDefaults {
    /// Where the backup copy of `key` lives.
    ///
    /// Derived rather than passed in, so the two halves of the scheme cannot be
    /// pointed at different slots and no store has to remember its own naming.
    static func backupKey(for key: String) -> String { "\(key)_backup" }

    /// Where the unreadable bytes of `key` are set aside.
    static func quarantineKey(for key: String) -> String { "\(key)_quarantine" }

    /// Writes `raw` to `key` and rotates the outgoing payload into its backup slot.
    ///
    /// The backup only ever advances to a payload that `isReadable` accepts.
    /// Promoting a copy that can no longer be read would destroy the last good
    /// one, which is the single thing the backup exists to hold: once the
    /// primary is corrupt, the backup is what the next read recovers from. When
    /// neither stored copy is readable there is nothing worth preserving, so the
    /// backup is re-seeded with `raw` and the next corruption again has
    /// somewhere to fall back to.
    ///
    /// The backup is written before the primary is overwritten, so a process
    /// killed mid-rotation leaves the last good copy behind rather than losing
    /// it along with the write. `isReadable` is called at most twice and never
    /// on the common path where the primary parses, so a store whose payload is
    /// expensive to decode pays for the check only when something has already
    /// gone wrong.
    ///
    /// This is the write half of the scheme whose read half is
    /// ``readWithBackupFallback(key:tryParse:)``; the two are meant to be
    /// changed together, which is why neither is written out per store. On
    /// Android the same rule was written out twice and the two copies drifted
    /// apart (#553, #560).
    func writeWithBackupRotation(
        key: String,
        raw: Data,
        isReadable: (Data) -> Bool
    ) {
        let backup = Self.backupKey(for: key)
        if let lastGood = data(forKey: key).flatMap({ isReadable($0) ? $0 : nil }) {
            set(lastGood, forKey: backup)
        } else if data(forKey: backup).map(isReadable) != true {
            // A missing backup is not readable, so an absent one is re-seeded
            // the same way a corrupt one is.
            set(raw, forKey: backup)
        }
        set(raw, forKey: key)
    }

    /// Reads `key`, falling back to its backup slot when the primary payload
    /// cannot be decoded by `tryParse`.
    ///
    /// When neither copy is readable the primary's raw bytes are moved to the
    /// quarantine slot rather than left to be overwritten by the next save, so
    /// they survive for a support request instead of disappearing silently.
    /// Quarantining is the only write this function performs.
    ///
    /// The silent-empty read this replaces was the whole bug: a store that
    /// failed to decode was indistinguishable from one that had never been
    /// written, and the first edit the user made wrote a one-element list over
    /// everything they had (#569).
    func readWithBackupFallback<T>(
        key: String,
        tryParse: (Data) -> T?
    ) -> BackupRead<T> {
        guard let raw = data(forKey: key) else { return .absent }

        if let parsed = tryParse(raw) { return .loaded(parsed) }

        if let recovered = data(forKey: Self.backupKey(for: key)).flatMap(tryParse) {
            print("Storage: \(key) unreadable; recovered from backup")
            return .loaded(recovered)
        }

        print("Storage: \(key) and its backup are both unreadable; quarantining")
        set(raw, forKey: Self.quarantineKey(for: key))
        return .unrecoverable
    }
}
