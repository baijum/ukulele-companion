import SwiftUI
import UniformTypeIdentifiers
import shared

enum BackupError: LocalizedError {
    case securityScopeDenied
    case fileReadFailed(Error)
    case invalidJSON
    case serializationFailed(Error)
    case noRecognizableData

    var errorDescription: String? {
        switch self {
        case .securityScopeDenied:
            return "Could not access the selected file."
        case .fileReadFailed(let error):
            return "Could not read file: \(error.localizedDescription)"
        case .invalidJSON:
            return "The file is not a valid backup."
        case .serializationFailed(let error):
            return "Could not create backup: \(error.localizedDescription)"
        case .noRecognizableData:
            return "The file does not contain any recognized backup data."
        }
    }
}

private enum BackupTimestamp {
    static func toMillis(_ seconds: Double) -> Int64 { Int64(seconds * 1000) }
    static func toSeconds(_ millis: Int64) -> Double { Double(millis) / 1000.0 }
    static func fromAny(_ value: Any?) -> Int64 {
        if let d = value as? Double { return Int64(d) }
        if let i = value as? Int { return Int64(i) }
        return 0
    }
}

@MainActor
final class BackupRestoreManager: ObservableObject {
    @Published var lastBackupDate: String?

    private let favoritesRepo: FavoritesRepository
    private let songbookRepo: SongbookRepository
    private let melodyRepo: MelodyRepository
    private let progressionsRepo: ProgressionsRepository
    private let learnRepo: LearnRepository
    private let practiceTimerRepo: PracticeTimerRepository
    private let customPatternsRepo: CustomPatternsRepository
    private let setlistRepo: SetlistRepository
    private let settingsRepo: SettingsRepository

    private let favoritesVM: FavoritesViewModel
    private let songbookVM: SongbookViewModel
    private let melodyVM: MelodyViewModel
    private let progressionsVM: ProgressionsViewModel
    private let learnVM: LearnViewModel
    private let practiceTimerVM: PracticeTimerViewModel
    private let customPatternsVM: CustomPatternsViewModel
    private let setlistVM: SetlistViewModel
    private let settingsVM: SettingsViewModel

    init(
        favoritesVM: FavoritesViewModel,
        songbookVM: SongbookViewModel,
        melodyVM: MelodyViewModel,
        progressionsVM: ProgressionsViewModel,
        learnVM: LearnViewModel,
        practiceTimerVM: PracticeTimerViewModel,
        customPatternsVM: CustomPatternsViewModel,
        setlistVM: SetlistViewModel,
        settingsVM: SettingsViewModel
    ) {
        self.favoritesVM = favoritesVM
        self.songbookVM = songbookVM
        self.melodyVM = melodyVM
        self.progressionsVM = progressionsVM
        self.learnVM = learnVM
        self.practiceTimerVM = practiceTimerVM
        self.customPatternsVM = customPatternsVM
        self.setlistVM = setlistVM
        self.settingsVM = settingsVM

        self.favoritesRepo = FavoritesRepository()
        self.songbookRepo = SongbookRepository()
        self.melodyRepo = MelodyRepository()
        self.progressionsRepo = ProgressionsRepository()
        self.learnRepo = LearnRepository()
        self.practiceTimerRepo = PracticeTimerRepository()
        self.customPatternsRepo = CustomPatternsRepository()
        self.setlistRepo = SetlistRepository()
        self.settingsRepo = SettingsRepository()
    }

    // MARK: - Export (via shared BackupCodec)

    func buildBackupData() throws -> Data {
        let (favs, folders) = favoritesVM.exportData()
        let learnData = learnVM.exportProgress()

        let backup = BackupData(
            version: BackupData.companion.CURRENT_VERSION,
            exportedAt: Int64(Date().timeIntervalSince1970 * 1000),
            favorites: FavoriteCoder.build(favs),
            favoriteFolders: FolderCoder.build(folders),
            chordSheets: ChordSheetCoder.build(songbookVM.exportData()),
            customProgressions: ProgressionCoder.build(progressionsVM.exportData()),
            customStrumPatterns: StrumPatternCoder.build(customPatternsVM.exportStrumData()),
            customFingerpickingPatterns: FingerpickPatternCoder.build(customPatternsVM.exportFingerpickData()),
            setlists: SetlistCoder.build(setlistVM.exportData()),
            melodies: MelodyCoder.build(melodyVM.exportData()),
            learningProgress: LearningProgressCoder.build(learnData),
            settings: SettingsCoder.build(settingsVM.exportSettings()),
            achievements: AchievementCoder.build(learnData),
            practiceTimer: PracticeTimerCoder.build(practiceTimerVM.exportData())
        )

        let jsonString = BackupCodec.shared.encode(data: backup)
        guard let data = jsonString.data(using: .utf8) else {
            throw BackupError.serializationFailed(
                NSError(domain: "BackupCodec", code: -1,
                        userInfo: [NSLocalizedDescriptionKey: "UTF-8 encoding failed"])
            )
        }
        return data
    }

    // MARK: - Import (via shared BackupCodec)

    func restoreFromURL(_ url: URL) throws {
        guard url.startAccessingSecurityScopedResource() else {
            throw BackupError.securityScopeDenied
        }
        defer { url.stopAccessingSecurityScopedResource() }

        let data: Data
        do {
            data = try Data(contentsOf: url)
        } catch {
            throw BackupError.fileReadFailed(error)
        }

        guard let jsonString = String(data: data, encoding: .utf8) else {
            throw BackupError.invalidJSON
        }

        let backup: BackupData
        do {
            backup = try BackupCodec.shared.decode(jsonString: jsonString)
        } catch {
            throw BackupError.invalidJSON
        }

        var importedAny = false

        // --- Favorites ---
        if !backup.favorites.isEmpty || !backup.favoriteFolders.isEmpty {
            favoritesVM.importData(
                favorites: FavoriteCoder.extract(backup.favorites),
                folders: FolderCoder.extract(backup.favoriteFolders)
            )
            importedAny = true
        }

        // --- Settings ---
        let settings = SettingsCoder.extract(backup.settings)
        if !settings.isEmpty {
            settingsVM.importSettings(settings)
            importedAny = true
        }

        // --- Chord sheets ---
        let sheets = ChordSheetCoder.extract(backup.chordSheets)
        if !sheets.isEmpty {
            songbookVM.importData(sheets)
            importedAny = true
        }

        // --- Melodies ---
        let melodies = MelodyCoder.extract(backup.melodies)
        if !melodies.isEmpty {
            melodyVM.importData(melodies)
            importedAny = true
        }

        // --- Custom progressions ---
        let progs = ProgressionCoder.extract(backup.customProgressions)
        if !progs.isEmpty {
            progressionsVM.importData(progs)
            importedAny = true
        }

        // --- Learning progress ---
        let entries = backup.learningProgress.entries as? [String: String] ?? [:]
        if !entries.isEmpty {
            learnVM.importProgress(LearningProgressCoder.extract(entries))
            importedAny = true
        }
        let achievements = backup.achievements as? [String: Any] ?? [:]
        if !achievements.isEmpty {
            let ids = Array(achievements.keys)
            learnVM.importProgress(["unlocked_achievements": ids])
            importedAny = true
        }

        // --- Practice timer ---
        let timer = PracticeTimerCoder.extract(backup.practiceTimer)
        if !timer.isEmpty {
            practiceTimerVM.importData(timer)
            importedAny = true
        }

        // --- Custom strum patterns ---
        let strumPatterns = StrumPatternCoder.extract(backup.customStrumPatterns)
        if !strumPatterns.isEmpty {
            customPatternsVM.importStrumData(strumPatterns)
            importedAny = true
        }

        // --- Custom fingerpicking patterns ---
        let fpPatterns = FingerpickPatternCoder.extract(backup.customFingerpickingPatterns)
        if !fpPatterns.isEmpty {
            customPatternsVM.importFingerpickData(fpPatterns)
            importedAny = true
        }

        // --- Setlists ---
        let setlists = SetlistCoder.extract(backup.setlists)
        if !setlists.isEmpty {
            setlistVM.importData(setlists)
            importedAny = true
        }

        if !importedAny {
            throw BackupError.noRecognizableData
        }
    }

    func recordBackupDate() {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        lastBackupDate = formatter.string(from: Date())
    }
}

// MARK: - Backup Coders (build KMP ↔ extract iOS dict)

private enum FavoriteCoder {
    static func build(_ favs: [[String: Any]]) -> [BackupFavorite] {
        favs.map { f in
            BackupFavorite(
                rootPitchClass: Int32(f["rootPitchClass"] as? Int ?? 0),
                chordSymbol: f["chordSymbol"] as? String ?? "",
                frets: (f["frets"] as? [Int] ?? []).map { KotlinInt(int: Int32($0)) },
                addedAt: BackupTimestamp.toMillis(f["addedAt"] as? Double ?? 0),
                folderId: nil,
                folderIds: f["folderIds"] as? [String] ?? []
            )
        }
    }

    static func extract(_ favs: [BackupFavorite]) -> [[String: Any]] {
        favs.map { f in
            var out: [String: Any] = [
                "rootPitchClass": Int(f.rootPitchClass),
                "chordSymbol": f.chordSymbol,
                "frets": f.frets.asInts,
                "addedAt": BackupTimestamp.toSeconds(f.addedAt),
            ]
            let folderIds = f.folderIds.asStrings
            if !folderIds.isEmpty {
                out["folderIds"] = folderIds
            } else if let folderId = f.folderId {
                out["folderIds"] = [folderId]
            } else {
                out["folderIds"] = [] as [String]
            }
            return out
        }
    }
}

private enum FolderCoder {
    static func build(_ folders: [[String: Any]]) -> [BackupFavoriteFolder] {
        folders.map { f in
            BackupFavoriteFolder(
                id: f["id"] as? String ?? UUID().uuidString,
                name: f["name"] as? String ?? "",
                createdAt: BackupTimestamp.toMillis(f["createdAt"] as? Double ?? 0),
                voicingOrder: f["voicingOrder"] as? [String] ?? []
            )
        }
    }

    static func extract(_ folders: [BackupFavoriteFolder]) -> [[String: Any]] {
        folders.map { [
            "id": $0.id, "name": $0.name,
            "createdAt": BackupTimestamp.toSeconds($0.createdAt),
            "voicingOrder": $0.voicingOrder.asStrings,
        ] as [String: Any] }
    }
}

private enum ChordSheetCoder {
    static func build(_ sheets: [[String: Any]]) -> [BackupChordSheet] {
        sheets.map { s in
            BackupChordSheet(
                id: s["id"] as? String ?? UUID().uuidString,
                title: s["title"] as? String ?? "",
                subtitle: s["subtitle"] as? String ?? "",
                artist: s["artist"] as? String ?? "",
                content: s["content"] as? String ?? "",
                key: s["key"] as? String ?? "",
                capo: Int32(s["capo"] as? Int ?? 0),
                strumPatternName: s["strumPatternName"] as? String ?? "",
                labels: s["labels"] as? [String] ?? [],
                createdAt: BackupTimestamp.fromAny(s["createdAt"]),
                updatedAt: BackupTimestamp.fromAny(s["updatedAt"]),
                viewCount: Int32(s["viewCount"] as? Int ?? 0),
                lastViewedAt: BackupTimestamp.fromAny(s["lastViewedAt"]),
                totalViewTimeMs: BackupTimestamp.fromAny(s["totalViewTimeMs"])
            )
        }
    }

    static func extract(_ sheets: [BackupChordSheet]) -> [[String: Any]] {
        sheets.map { s in
            [
                "id": s.id, "title": s.title, "subtitle": s.subtitle,
                "artist": s.artist, "content": s.content, "key": s.key,
                "capo": Int(s.capo),
                "strumPatternName": s.strumPatternName,
                "labels": s.labels.asStrings,
                "createdAt": Double(s.createdAt), "updatedAt": Double(s.updatedAt),
                "viewCount": Int(s.viewCount),
                "lastViewedAt": Double(s.lastViewedAt),
                "totalViewTimeMs": Double(s.totalViewTimeMs),
            ] as [String: Any]
        }
    }
}

private enum ProgressionCoder {
    static func build(_ progs: [[String: Any]]) -> [BackupProgression] {
        progs.compactMap { p -> BackupProgression? in
            guard let intervals = p["degreeIntervals"] as? [Int],
                  let qualities = p["degreeQualities"] as? [String],
                  let numerals = p["degreeNumerals"] as? [String] else { return nil }
            let count = min(intervals.count, qualities.count, numerals.count)
            let degrees = (0..<count).map { i in
                BackupChordDegree(interval: Int32(intervals[i]), quality: qualities[i], numeral: numerals[i])
            }
            guard !degrees.isEmpty else { return nil }
            return BackupProgression(
                id: p["id"] as? String ?? UUID().uuidString,
                name: p["name"] as? String ?? "",
                description: p["description"] as? String ?? "",
                scaleType: p["scaleType"] as? String ?? "MAJOR",
                degrees: degrees,
                createdAt: BackupTimestamp.toMillis(p["createdAt"] as? Double ?? Date().timeIntervalSince1970)
            )
        }
    }

    static func extract(_ progs: [BackupProgression]) -> [[String: Any]] {
        progs.map { p in
            let degrees = p.degrees.asArray(of: BackupChordDegree.self)
            return [
                "id": p.id, "name": p.name,
                "description": p.description_,
                "scaleType": p.scaleType,
                "degreeIntervals": degrees.map { Int($0.interval) },
                "degreeQualities": degrees.map { $0.quality },
                "degreeNumerals": degrees.map { $0.numeral },
                "createdAt": BackupTimestamp.toSeconds(p.createdAt),
            ] as [String: Any]
        }
    }
}

private enum StrumPatternCoder {
    static func build(_ patterns: [[String: Any]]) -> [BackupStrumPattern] {
        patterns.map { p in
            let beats = (p["beats"] as? [[String: Any]] ?? []).map { b in
                BackupStrumBeat(
                    direction: b["direction"] as? String ?? "DOWN",
                    emphasis: b["emphasis"] as? Bool ?? false
                )
            }
            return BackupStrumPattern(
                id: p["id"] as? String ?? UUID().uuidString,
                name: p["name"] as? String ?? "",
                beats: beats,
                createdAt: BackupTimestamp.toMillis(p["createdAt"] as? Double ?? 0),
                timeSignature: p["timeSignature"] as? String ?? "4/4"
            )
        }
    }

    static func extract(_ patterns: [BackupStrumPattern]) -> [[String: Any]] {
        patterns.map { p in
            let beats: [[String: Any]] = p.beats.asArray(of: BackupStrumBeat.self).map {
                ["direction": $0.direction, "emphasis": $0.emphasis] as [String: Any]
            }
            return [
                "id": p.id, "name": p.name, "beats": beats,
                "createdAt": BackupTimestamp.toSeconds(p.createdAt),
                "timeSignature": p.timeSignature,
            ] as [String: Any]
        }
    }
}

private enum FingerpickPatternCoder {
    private static let fingerToKMP: [String: String] = [
        "T": "THUMB", "I": "INDEX", "M": "MIDDLE", "R": "RING", "P": "PINKY",
    ]
    private static let fingerFromKMP: [String: String] = {
        Dictionary(uniqueKeysWithValues: fingerToKMP.map { ($0.value, $0.key) })
    }()

    static func build(_ patterns: [[String: Any]]) -> [BackupFingerpickingPattern] {
        patterns.map { p in
            let steps = (p["steps"] as? [[String: Any]] ?? []).map { s in
                let finger = s["finger"] as? String ?? "T"
                return BackupFingerpickStep(
                    finger: fingerToKMP[finger] ?? finger,
                    stringIndex: Int32(s["stringIndex"] as? Int ?? 0),
                    emphasis: s["emphasis"] as? Bool ?? false
                )
            }
            return BackupFingerpickingPattern(
                id: p["id"] as? String ?? UUID().uuidString,
                name: p["name"] as? String ?? "",
                steps: steps,
                createdAt: BackupTimestamp.toMillis(p["createdAt"] as? Double ?? 0),
                timeSignature: p["timeSignature"] as? String ?? "4/4"
            )
        }
    }

    static func extract(_ patterns: [BackupFingerpickingPattern]) -> [[String: Any]] {
        patterns.map { p in
            let steps: [[String: Any]] = p.steps.asArray(of: BackupFingerpickStep.self).map {
                [
                    "finger": fingerFromKMP[$0.finger] ?? $0.finger,
                    "stringIndex": Int($0.stringIndex),
                    "emphasis": $0.emphasis,
                ] as [String: Any]
            }
            return [
                "id": p.id, "name": p.name, "steps": steps,
                "createdAt": BackupTimestamp.toSeconds(p.createdAt),
                "timeSignature": p.timeSignature,
            ] as [String: Any]
        }
    }
}

private enum SetlistCoder {
    static func build(_ setlists: [[String: Any]]) -> [BackupSetlist] {
        setlists.map { sl in
            BackupSetlist(
                id: sl["id"] as? String ?? UUID().uuidString,
                name: sl["name"] as? String ?? "",
                songIds: sl["songIds"] as? [String] ?? [],
                createdAt: BackupTimestamp.fromAny(sl["createdAt"]),
                updatedAt: BackupTimestamp.fromAny(sl["updatedAt"])
            )
        }
    }

    static func extract(_ setlists: [BackupSetlist]) -> [[String: Any]] {
        setlists.map { [
            "id": $0.id, "name": $0.name,
            "songIds": $0.songIds.asStrings,
            "createdAt": Double($0.createdAt), "updatedAt": Double($0.updatedAt),
        ] as [String: Any] }
    }
}

private enum MelodyCoder {
    // Derived from NoteDuration.allCases so the glyph keys always match the
    // enum's real raw values. Hand-typed escapes drifted from the combining
    // sequences that half/sixteenth notes actually use, dropping those
    // melodies on cross-platform restore (see issue #600).
    private static let durationToKMP: [String: String] = Dictionary(
        uniqueKeysWithValues: NoteDuration.allCases.map { ($0.rawValue, kmpName($0)) }
    )
    private static let durationFromKMP: [String: String] = Dictionary(
        uniqueKeysWithValues: NoteDuration.allCases.map { (kmpName($0), $0.rawValue) }
    )

    private static func kmpName(_ duration: NoteDuration) -> String {
        switch duration {
        case .whole: "WHOLE"
        case .half: "HALF"
        case .quarter: "QUARTER"
        case .eighth: "EIGHTH"
        case .sixteenth: "SIXTEENTH"
        }
    }

    static func build(_ melodies: [[String: Any]]) -> [BackupMelody] {
        melodies.map { m in
            let notes: [BackupMelodyNote] = (m["notes"] as? [[String: Any]] ?? []).map { n in
                let dur = n["duration"] as? String ?? "QUARTER"
                return BackupMelodyNote(
                    pitchClass: (n["pitchClass"] as? Int).map { KotlinInt(int: Int32($0)) },
                    octave: Int32(n["octave"] as? Int ?? 4),
                    duration: durationToKMP[dur] ?? dur,
                    stringIndex: nil, fret: nil
                )
            }
            return BackupMelody(
                id: m["id"] as? String ?? UUID().uuidString,
                name: m["name"] as? String ?? "",
                notes: notes,
                bpm: Int32(m["bpm"] as? Int ?? 120),
                createdAt: BackupTimestamp.fromAny(m["createdAt"])
            )
        }
    }

    static func extract(_ melodies: [BackupMelody]) -> [[String: Any]] {
        melodies.map { m in
            let notes: [[String: Any]] = m.notes.asArray(of: BackupMelodyNote.self).map { n in
                var note: [String: Any] = [
                    "id": UUID().uuidString,
                    "octave": Int(n.octave),
                    "duration": durationFromKMP[n.duration] ?? n.duration,
                ]
                if let pc = n.pitchClass?.intValue {
                    note["pitchClass"] = Int(pc)
                    note["isRest"] = false
                } else {
                    note["isRest"] = true
                }
                return note
            }
            return [
                "id": m.id, "name": m.name, "notes": notes,
                "bpm": Int(m.bpm),
                "createdAt": Double(m.createdAt),
            ] as [String: Any]
        }
    }
}

private enum LearningProgressCoder {
    static func build(_ data: [String: Any]) -> BackupLearningProgress {
        var entries: [String: String] = [:]
        for (key, value) in data where key != "unlocked_achievements" {
            entries[key] = "\(value)"
        }
        return BackupLearningProgress(entries: entries)
    }

    static func extract(_ entries: [String: String]) -> [String: Any] {
        var out: [String: Any] = [:]
        for (key, value) in entries {
            if value == "true" { out[key] = true }
            else if value == "false" { out[key] = false }
            else if let intVal = Int(value) { out[key] = intVal }
            else { out[key] = value }
        }
        return out
    }
}

private enum AchievementCoder {
    static func build(_ data: [String: Any]) -> [String: KotlinLong] {
        guard let list = data["unlocked_achievements"] as? [String] else { return [:] }
        var map: [String: KotlinLong] = [:]
        for id in list { map[id] = KotlinLong(value: 0) }
        return map
    }
}

private enum PracticeTimerCoder {
    static func build(_ timer: [String: Any]) -> BackupPracticeTimer {
        let dailyMinutes = timer["dailyMinutes"] as? [String: Int] ?? [:]
        var kmpDaily: [String: KotlinInt] = [:]
        for (key, value) in dailyMinutes {
            kmpDaily[key] = KotlinInt(int: Int32(value))
        }
        return BackupPracticeTimer(
            totalMinutes: Int32(timer["totalMinutes"] as? Int ?? 0),
            totalSessions: Int32(timer["totalSessions"] as? Int ?? 0),
            longestSession: Int32(timer["longestSession"] as? Int ?? 0),
            lastSessionTime: BackupTimestamp.toMillis(timer["lastSessionTime"] as? Double ?? 0),
            dailyGoal: Int32(timer["dailyGoal"] as? Int ?? 15),
            dailyMinutes: kmpDaily
        )
    }

    static func extract(_ timer: BackupPracticeTimer) -> [String: Any] {
        var dailyMinutes: [String: Int] = [:]
        if let kmpDaily = timer.dailyMinutes as? [String: Any] {
            for (key, value) in kmpDaily {
                if let num = value as? NSNumber { dailyMinutes[key] = num.intValue }
            }
        }
        return [
            "totalMinutes": Int(timer.totalMinutes),
            "totalSessions": Int(timer.totalSessions),
            "longestSession": Int(timer.longestSession),
            "lastSessionTime": BackupTimestamp.toSeconds(timer.lastSessionTime),
            "dailyGoal": Int(timer.dailyGoal),
            "dailyMinutes": dailyMinutes,
        ]
    }
}

private enum SettingsCoder {
    private static let themeToKMP: [String: String] = [
        "Light": "LIGHT", "Dark": "DARK",
        "System": "SYSTEM", "High Contrast": "HIGH_CONTRAST",
    ]
    private static let themeFromKMP: [String: String] = {
        Dictionary(uniqueKeysWithValues: themeToKMP.map { ($0.value, $0.key) })
    }()
    private static let tuningToKMP: [String: String] = [
        "High-G (Standard)": "HIGH_G", "Low-G": "LOW_G",
        "Baritone (DGBE)": "BARITONE", "D-Tuning (ADF#B)": "D_TUNING",
        "Slack Key (GCEG)": "SLACK_KEY", "Open A (AC#EA)": "OPEN_A",
        "Low A (GCEa)": "LOW_A", "Half-Step Down": "HALF_STEP_DOWN",
    ]
    private static let tuningFromKMP: [String: String] = {
        Dictionary(uniqueKeysWithValues: tuningToKMP.map { ($0.value, $0.key) })
    }()

    static func build(_ settings: [String: Any]) -> BackupSettings {
        let themeLabel = settings["theme_mode"] as? String ?? "System"
        let tuningLabel = settings["selected_tuning"] as? String ?? "High-G (Standard)"
        return BackupSettings(
            soundEnabled: settings["sound_enabled"] as? Bool ?? true,
            volume: Float(settings["volume"] as? Double ?? 0.7),
            noteDurationMs: Int32(settings["note_duration_ms"] as? Int ?? 600),
            strumDelayMs: Int32(settings["strum_delay_ms"] as? Int ?? 50),
            strumDown: settings["strum_down"] as? Bool ?? true,
            playOnTap: settings["play_on_tap"] as? Bool ?? false,
            themeMode: themeToKMP[themeLabel] ?? "SYSTEM",
            showExplorerTips: settings["show_tips"] as? Bool ?? true,
            showLearnSection: settings["show_learn_tab"] as? Bool ?? true,
            showReferenceSection: settings["show_reference_tab"] as? Bool ?? true,
            tuning: tuningToKMP[tuningLabel] ?? "HIGH_G",
            leftHanded: settings["left_handed"] as? Bool ?? false,
            lastFret: Int32(settings["last_fret"] as? Int ?? 12),
            showNoteNames: settings["show_note_names"] as? Bool ?? true
        )
    }

    static func extract(_ settings: BackupSettings) -> [String: Any] {
        [
            "sound_enabled": settings.soundEnabled,
            "volume": Double(settings.volume),
            "note_duration_ms": Int(settings.noteDurationMs),
            "strum_delay_ms": Int(settings.strumDelayMs),
            "strum_down": settings.strumDown,
            "play_on_tap": settings.playOnTap,
            "theme_mode": themeFromKMP[settings.themeMode] ?? "System",
            "show_tips": settings.showExplorerTips,
            "show_learn_tab": settings.showLearnSection,
            "show_reference_tab": settings.showReferenceSection,
            "selected_tuning": tuningFromKMP[settings.tuning] ?? "High-G (Standard)",
            "left_handed": settings.leftHanded,
            "last_fret": Int(settings.lastFret),
            "show_note_names": settings.showNoteNames,
        ]
    }
}

struct BackupDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json] }

    let data: Data

    init(data: Data) {
        self.data = data
    }

    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data()
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}
