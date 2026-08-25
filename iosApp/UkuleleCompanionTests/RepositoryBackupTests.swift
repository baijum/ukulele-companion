import XCTest
@testable import UkuleleCompanion

/// What each UserDefaults-backed store does when its stored payload cannot be
/// decoded.
///
/// Every one of these stores used to answer that question the same way: `try?`
/// turned the failure into `nil`, the store reported empty, and the first edit
/// the user made wrote a one-element list over everything they had (#569). The
/// list stores keep the whole list in one blob per key, so that was all-or-
/// nothing — one bad byte in `chord_sheets` lost every song.
///
/// One test per store, because routing them onto the shared rule changes what
/// each of them does on disk, and because the Android side of this work found
/// that a recovery contract can exist while nothing actually reaches it (#564).
final class RepositoryBackupTests: XCTestCase {
    /// Every primary key that now carries a backup and a quarantine slot.
    private static let storeKeys = [
        "chord_sheets",
        "setlists",
        "melodies",
        "custom_progressions",
        "custom_strum_patterns",
        "custom_fingerpicking_patterns",
        "favorite_voicings",
        "favorite_folders",
        "practice_timer",
    ]

    private let defaults = UserDefaults.standard

    override func setUp() {
        super.setUp()
        clearStores()
    }

    override func tearDown() {
        clearStores()
        super.tearDown()
    }

    private func clearStores() {
        for key in Self.storeKeys {
            for slot in [key, UserDefaults.backupKey(for: key), UserDefaults.quarantineKey(for: key)] {
                defaults.removeObject(forKey: slot)
            }
        }
    }

    // MARK: - Helpers

    private static let corrupt = Data("NOT JSON".utf8)
    private static let alsoCorrupt = Data("ALSO NOT JSON".utf8)

    /// Leaves both stored copies of `key` unreadable.
    private func corruptBothCopies(_ key: String) {
        defaults.set(Self.corrupt, forKey: key)
        defaults.set(Self.alsoCorrupt, forKey: UserDefaults.backupKey(for: key))
    }

    /// Asserts that `key` holds the bytes the read was supposed to set aside.
    private func assertQuarantined(_ key: String, line: UInt = #line) {
        XCTAssertEqual(
            defaults.data(forKey: UserDefaults.quarantineKey(for: key)), Self.corrupt,
            "\(key) should have left its unreadable bytes in quarantine",
            line: line
        )
    }

    private func makeSong(id: String, title: String) -> StoredSong {
        StoredSong(
            id: id, title: title, artist: "", content: "[C]Hello",
            key: "C", capo: 0, strumPatternName: "", labels: [],
            createdAt: 1000, updatedAt: 1000,
            viewCount: 0, lastViewedAt: 0, totalViewTimeMs: 0
        )
    }

    // MARK: - Both copies unreadable

    func testSongbookQuarantinesUnreadableCopies() {
        corruptBothCopies("chord_sheets")

        XCTAssertTrue(SongbookRepository().getAll().isEmpty)
        assertQuarantined("chord_sheets")
    }

    func testSetlistQuarantinesUnreadableCopies() {
        corruptBothCopies("setlists")

        XCTAssertTrue(SetlistRepository().getAll().isEmpty)
        assertQuarantined("setlists")
    }

    func testMelodyQuarantinesUnreadableCopies() {
        corruptBothCopies("melodies")

        XCTAssertTrue(MelodyRepository().getAll().isEmpty)
        assertQuarantined("melodies")
    }

    func testProgressionsQuarantinesUnreadableCopies() {
        corruptBothCopies("custom_progressions")

        XCTAssertTrue(ProgressionsRepository().getAll().isEmpty)
        assertQuarantined("custom_progressions")
    }

    func testStrumPatternsQuarantineUnreadableCopies() {
        corruptBothCopies("custom_strum_patterns")

        XCTAssertTrue(CustomPatternsRepository().getAllStrum().isEmpty)
        assertQuarantined("custom_strum_patterns")
    }

    func testFingerpickingPatternsQuarantineUnreadableCopies() {
        corruptBothCopies("custom_fingerpicking_patterns")

        XCTAssertTrue(CustomPatternsRepository().getAllFingerpicking().isEmpty)
        assertQuarantined("custom_fingerpicking_patterns")
    }

    func testFavoriteVoicingsQuarantineUnreadableCopies() {
        corruptBothCopies("favorite_voicings")

        XCTAssertTrue(FavoritesRepository().getAll().isEmpty)
        assertQuarantined("favorite_voicings")
    }

    func testFavoriteFoldersQuarantineUnreadableCopies() {
        corruptBothCopies("favorite_folders")

        XCTAssertTrue(FavoritesRepository().getAllFolders().isEmpty)
        assertQuarantined("favorite_folders")
    }

    /// One object rather than a list, and the loss is the practice totals
    /// rather than a song list, but the shape is the same.
    func testPracticeTimerQuarantinesUnreadableCopies() {
        corruptBothCopies("practice_timer")

        XCTAssertEqual(PracticeTimerRepository().load().totalMinutes, 0)
        assertQuarantined("practice_timer")
    }

    /// A store nobody has written to is not a store that lost anything.
    func testUntouchedStoreQuarantinesNothing() {
        XCTAssertTrue(SongbookRepository().getAll().isEmpty)
        XCTAssertNil(defaults.data(forKey: UserDefaults.quarantineKey(for: "chord_sheets")))
    }

    // MARK: - Recovery from the backup copy

    func testCorruptPrimaryRecoversFromBackup() {
        let repository = SongbookRepository()
        repository.save([makeSong(id: "song-1", title: "Amazing Grace")])

        defaults.set(Self.corrupt, forKey: "chord_sheets")

        let recovered = repository.getAll()
        XCTAssertEqual(recovered.map(\.title), ["Amazing Grace"])
        XCTAssertNil(
            defaults.data(forKey: UserDefaults.quarantineKey(for: "chord_sheets")),
            "a payload the backup could answer for has not been lost"
        )
    }

    /// The sequence from #569: the list reads as empty, the user adds a song,
    /// and the save takes the rest with it. The backup is what stops step three.
    func testAddingASongAfterCorruptionDoesNotDestroyTheOthers() {
        let repository = SongbookRepository()
        repository.save([
            makeSong(id: "song-1", title: "Amazing Grace"),
            makeSong(id: "song-2", title: "Blackbird"),
        ])

        defaults.set(Self.corrupt, forKey: "chord_sheets")

        var songs = repository.getAll()
        songs.append(makeSong(id: "song-3", title: "Hallelujah"))
        repository.save(songs)

        XCTAssertEqual(
            SongbookRepository().getAll().map(\.title).sorted(),
            ["Amazing Grace", "Blackbird", "Hallelujah"]
        )
    }

    func testPracticeTimerRecoversTotalsFromBackup() {
        let repository = PracticeTimerRepository()
        var data = PracticeTimerData()
        data.totalMinutes = 120
        repository.save(data)

        defaults.set(Self.corrupt, forKey: "practice_timer")

        XCTAssertEqual(repository.load().totalMinutes, 120)
    }

    // MARK: - Importing the daily practice goal

    /// A backup with no practice-timer data (or one made on a device still at the
    /// default) carries the 15-minute default goal. Importing it must not clobber
    /// a goal the user chose (#608). Mirrors Android's guard in
    /// `PracticeTimerRepository.importAll`.
    func testImportDefaultGoalLeavesUserGoalUntouched() {
        let repository = PracticeTimerRepository()
        var data = PracticeTimerData()
        data.dailyGoal = 60

        repository.importData(["dailyGoal": 15], into: &data)

        XCTAssertEqual(data.dailyGoal, 60)
        XCTAssertEqual(repository.load().dailyGoal, 60)
    }

    /// A non-default goal from a backup still overrides the current goal.
    func testImportNonDefaultGoalOverridesCurrent() {
        let repository = PracticeTimerRepository()
        var data = PracticeTimerData()
        data.dailyGoal = 60

        repository.importData(["dailyGoal": 30], into: &data)

        XCTAssertEqual(data.dailyGoal, 30)
        XCTAssertEqual(repository.load().dailyGoal, 30)
    }

    /// A hand-edited or corrupt backup cannot push the goal outside the 5...120
    /// range the app's own setter enforces.
    func testImportClampsOutOfRangeGoal() {
        let repository = PracticeTimerRepository()
        var data = PracticeTimerData()
        data.dailyGoal = 60

        repository.importData(["dailyGoal": 500], into: &data)
        XCTAssertEqual(data.dailyGoal, 120)

        repository.importData(["dailyGoal": 1], into: &data)
        XCTAssertEqual(data.dailyGoal, 5)
    }

    func testFavoritesRecoverFromBackup() {
        let repository = FavoritesRepository()
        repository.save([
            FavoriteVoicingData(rootPitchClass: 0, chordSymbol: "maj",
                                frets: [0, 0, 0, 3], addedAt: 1000, folderIds: []),
        ])

        defaults.set(Self.corrupt, forKey: "favorite_voicings")

        XCTAssertEqual(repository.getAll().map(\.chordSymbol), ["maj"])
    }

    // MARK: - What the backup is allowed to advance to

    /// The backup only ever advances to a payload that still decodes. Promoting
    /// a corrupt primary would destroy the last good copy — the single thing the
    /// backup exists to hold (#553, #560).
    func testSaveDoesNotPromoteACorruptPrimaryIntoTheBackup() {
        let repository = SongbookRepository()
        repository.save([makeSong(id: "song-1", title: "Amazing Grace")])
        let lastGood = defaults.data(forKey: UserDefaults.backupKey(for: "chord_sheets"))
        XCTAssertNotNil(lastGood, "the first save should have seeded the backup")

        defaults.set(Self.corrupt, forKey: "chord_sheets")
        repository.save([makeSong(id: "song-2", title: "Blackbird")])

        XCTAssertEqual(defaults.data(forKey: UserDefaults.backupKey(for: "chord_sheets")), lastGood)
    }

    /// With neither stored copy readable there is nothing worth preserving, so
    /// the backup is re-seeded and the next corruption again has somewhere to
    /// fall back to.
    func testSaveReseedsAnUnreadableBackup() {
        let repository = SongbookRepository()
        corruptBothCopies("chord_sheets")

        repository.save([makeSong(id: "song-1", title: "Amazing Grace")])

        defaults.set(Self.corrupt, forKey: "chord_sheets")
        XCTAssertEqual(repository.getAll().map(\.title), ["Amazing Grace"])
    }

    /// An ordinary save rotates the previous payload into the backup rather than
    /// leaving the backup pinned to whatever seeded it.
    func testSaveRotatesThePreviousPayloadIntoTheBackup() {
        let repository = SongbookRepository()
        repository.save([makeSong(id: "song-1", title: "Amazing Grace")])
        repository.save([makeSong(id: "song-2", title: "Blackbird")])

        defaults.set(Self.corrupt, forKey: "chord_sheets")

        XCTAssertEqual(repository.getAll().map(\.title), ["Amazing Grace"])
    }

    // MARK: - Settings round trip

    /// A non-default playback volume must survive the full settings backup round
    /// trip. The value crosses two `[String: Any]` boundaries — export → build,
    /// then extract → import — and `as?` does no numeric conversion between two
    /// Swift-native numeric types boxed in `Any`, so a Float/Double mismatch on
    /// either hop silently drops the value and the app reverts to its default
    /// (#599). Both ends must agree on `Double`.
    func testVolumeSurvivesSettingsBackupRoundTrip() {
        // SettingsRepository stores settings in its own suite, not `.standard`.
        let suite = UserDefaults(suiteName: "app_settings") ?? .standard
        let saved = suite.object(forKey: "volume")
        defer {
            if let saved { suite.set(saved, forKey: "volume") } else { suite.removeObject(forKey: "volume") }
        }

        // A volume the user chose — distinct from every default in the pipeline
        // (export 1.0, build 0.7, restore 1.0) so a dropped value cannot masquerade.
        suite.set(Float(0.2), forKey: "volume")

        let repository = SettingsRepository()
        let exported = repository.exportSettings()
        let roundTripped = SettingsCoder.extract(SettingsCoder.build(exported))

        // Clear the stored value so only the import can restore it.
        suite.removeObject(forKey: "volume")
        repository.importSettings(roundTripped)

        XCTAssertEqual(
            repository.load().volume, 0.2, accuracy: 0.0001,
            "playback volume must survive export → build → extract → import"
        )
    }
}
