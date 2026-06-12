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
            favorites: buildFavorites(favs),
            favoriteFolders: buildFolders(folders),
            chordSheets: buildChordSheets(songbookVM.exportData()),
            customProgressions: buildProgressions(progressionsVM.exportData()),
            customStrumPatterns: buildStrumPatterns(customPatternsVM.exportStrumData()),
            customFingerpickingPatterns: buildFingerpickPatterns(customPatternsVM.exportFingerpickData()),
            setlists: buildSetlists(setlistVM.exportData()),
            melodies: buildMelodies(melodyVM.exportData()),
            learningProgress: buildLearningProgress(learnData),
            settings: buildSettings(settingsVM.exportSettings()),
            achievements: buildAchievements(learnData),
            practiceTimer: buildPracticeTimer(practiceTimerVM.exportData()),
            knownChords: []
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
                favorites: extractFavorites(backup.favorites),
                folders: extractFolders(backup.favoriteFolders)
            )
            importedAny = true
        }

        // --- Settings ---
        let settings = extractSettings(backup.settings)
        if !settings.isEmpty {
            settingsVM.importSettings(settings)
            importedAny = true
        }

        // --- Chord sheets ---
        let sheets = extractChordSheets(backup.chordSheets)
        if !sheets.isEmpty {
            songbookVM.importData(sheets)
            importedAny = true
        }

        // --- Melodies ---
        let melodies = extractMelodies(backup.melodies)
        if !melodies.isEmpty {
            melodyVM.importData(melodies)
            importedAny = true
        }

        // --- Custom progressions ---
        let progs = extractProgressions(backup.customProgressions)
        if !progs.isEmpty {
            progressionsVM.importData(progs)
            importedAny = true
        }

        // --- Learning progress ---
        let entries = backup.learningProgress.entries as? [String: String] ?? [:]
        if !entries.isEmpty {
            learnVM.importProgress(extractLearnProgress(entries))
            importedAny = true
        }
        let achievements = backup.achievements as? [String: Any] ?? [:]
        if !achievements.isEmpty {
            let ids = Array(achievements.keys)
            learnVM.importProgress(["unlocked_achievements": ids])
            importedAny = true
        }

        // --- Practice timer ---
        let timer = extractPracticeTimer(backup.practiceTimer)
        if !timer.isEmpty {
            practiceTimerVM.importData(timer)
            importedAny = true
        }

        // --- Custom strum patterns ---
        let strumPatterns = extractStrumPatterns(backup.customStrumPatterns)
        if !strumPatterns.isEmpty {
            customPatternsVM.importStrumData(strumPatterns)
            importedAny = true
        }

        // --- Custom fingerpicking patterns ---
        let fpPatterns = extractFingerpickPatterns(backup.customFingerpickingPatterns)
        if !fpPatterns.isEmpty {
            customPatternsVM.importFingerpickData(fpPatterns)
            importedAny = true
        }

        // --- Setlists ---
        let setlists = extractSetlists(backup.setlists)
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

    // MARK: - Build KMP objects from iOS VM data (export)

    private func buildFavorites(_ favs: [[String: Any]]) -> [BackupFavorite] {
        favs.map { f in
            let addedAt = f["addedAt"] as? Double ?? 0
            return BackupFavorite(
                rootPitchClass: Int32(f["rootPitchClass"] as? Int ?? 0),
                chordSymbol: f["chordSymbol"] as? String ?? "",
                frets: (f["frets"] as? [Int] ?? []).map { KotlinInt(int: Int32($0)) },
                addedAt: Int64(addedAt * 1000),
                folderId: nil,
                folderIds: f["folderIds"] as? [String] ?? []
            )
        }
    }

    private func buildFolders(_ folders: [[String: Any]]) -> [BackupFavoriteFolder] {
        folders.map { f in
            let createdAt = f["createdAt"] as? Double ?? 0
            return BackupFavoriteFolder(
                id: f["id"] as? String ?? UUID().uuidString,
                name: f["name"] as? String ?? "",
                createdAt: Int64(createdAt * 1000),
                voicingOrder: f["voicingOrder"] as? [String] ?? []
            )
        }
    }

    private func buildChordSheets(_ sheets: [[String: Any]]) -> [BackupChordSheet] {
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
                createdAt: millisFromAny(s["createdAt"]),
                updatedAt: millisFromAny(s["updatedAt"]),
                viewCount: Int32(s["viewCount"] as? Int ?? 0),
                lastViewedAt: millisFromAny(s["lastViewedAt"]),
                totalViewTimeMs: millisFromAny(s["totalViewTimeMs"])
            )
        }
    }

    private func buildProgressions(_ progs: [[String: Any]]) -> [BackupProgression] {
        progs.compactMap { p -> BackupProgression? in
            var degrees: [BackupChordDegree] = []
            if let intervals = p["degreeIntervals"] as? [Int],
               let qualities = p["degreeQualities"] as? [String],
               let numerals = p["degreeNumerals"] as? [String] {
                let count = min(intervals.count, qualities.count, numerals.count)
                for i in 0..<count {
                    degrees.append(BackupChordDegree(
                        interval: Int32(intervals[i]),
                        quality: qualities[i],
                        numeral: numerals[i]
                    ))
                }
            }
            guard !degrees.isEmpty else { return nil }
            let createdAt = p["createdAt"] as? Double ?? Date().timeIntervalSince1970
            return BackupProgression(
                id: p["id"] as? String ?? UUID().uuidString,
                name: p["name"] as? String ?? "",
                description: p["description"] as? String ?? "",
                scaleType: p["scaleType"] as? String ?? "MAJOR",
                degrees: degrees,
                createdAt: Int64(createdAt * 1000)
            )
        }
    }

    private func buildStrumPatterns(_ patterns: [[String: Any]]) -> [BackupStrumPattern] {
        patterns.map { p in
            let beats: [BackupStrumBeat] = (p["beats"] as? [[String: Any]] ?? []).map { b in
                BackupStrumBeat(
                    direction: b["direction"] as? String ?? "DOWN",
                    emphasis: b["emphasis"] as? Bool ?? false
                )
            }
            let createdAt = p["createdAt"] as? Double ?? 0
            return BackupStrumPattern(
                id: p["id"] as? String ?? UUID().uuidString,
                name: p["name"] as? String ?? "",
                beats: beats,
                createdAt: Int64(createdAt * 1000),
                timeSignature: p["timeSignature"] as? String ?? "4/4"
            )
        }
    }

    private func buildFingerpickPatterns(_ patterns: [[String: Any]]) -> [BackupFingerpickingPattern] {
        patterns.map { p in
            let steps: [BackupFingerpickStep] = (p["steps"] as? [[String: Any]] ?? []).map { s in
                let finger = s["finger"] as? String ?? "T"
                return BackupFingerpickStep(
                    finger: Self.fingerToKMP[finger] ?? finger,
                    stringIndex: Int32(s["stringIndex"] as? Int ?? 0),
                    emphasis: s["emphasis"] as? Bool ?? false
                )
            }
            let createdAt = p["createdAt"] as? Double ?? 0
            return BackupFingerpickingPattern(
                id: p["id"] as? String ?? UUID().uuidString,
                name: p["name"] as? String ?? "",
                steps: steps,
                createdAt: Int64(createdAt * 1000),
                timeSignature: p["timeSignature"] as? String ?? "4/4"
            )
        }
    }

    private func buildSetlists(_ setlists: [[String: Any]]) -> [BackupSetlist] {
        setlists.map { sl in
            BackupSetlist(
                id: sl["id"] as? String ?? UUID().uuidString,
                name: sl["name"] as? String ?? "",
                songIds: sl["songIds"] as? [String] ?? [],
                createdAt: millisFromAny(sl["createdAt"]),
                updatedAt: millisFromAny(sl["updatedAt"])
            )
        }
    }

    private func buildMelodies(_ melodies: [[String: Any]]) -> [BackupMelody] {
        melodies.map { m in
            let notes: [BackupMelodyNote] = (m["notes"] as? [[String: Any]] ?? []).map { n in
                let pc = n["pitchClass"] as? Int
                let dur = n["duration"] as? String ?? "QUARTER"
                return BackupMelodyNote(
                    pitchClass: pc.map { KotlinInt(int: Int32($0)) },
                    octave: Int32(n["octave"] as? Int ?? 4),
                    duration: Self.durationToKMP[dur] ?? dur,
                    stringIndex: nil,
                    fret: nil
                )
            }
            let createdAt = m["createdAt"] as? Double ?? 0
            return BackupMelody(
                id: m["id"] as? String ?? UUID().uuidString,
                name: m["name"] as? String ?? "",
                notes: notes,
                bpm: Int32(m["bpm"] as? Int ?? 120),
                createdAt: Int64(createdAt * 1000)
            )
        }
    }

    private func buildLearningProgress(_ data: [String: Any]) -> BackupLearningProgress {
        var entries: [String: String] = [:]
        for (key, value) in data where key != "unlocked_achievements" {
            entries[key] = "\(value)"
        }
        return BackupLearningProgress(entries: entries)
    }

    private func buildAchievements(_ data: [String: Any]) -> [String: KotlinLong] {
        guard let list = data["unlocked_achievements"] as? [String] else { return [:] }
        var map: [String: KotlinLong] = [:]
        for id in list {
            map[id] = KotlinLong(value: 0)
        }
        return map
    }

    private func buildPracticeTimer(_ timer: [String: Any]) -> BackupPracticeTimer {
        let lastSessionTime = timer["lastSessionTime"] as? Double ?? 0
        let dailyMinutes = timer["dailyMinutes"] as? [String: Int] ?? [:]
        var kmpDaily: [String: KotlinInt] = [:]
        for (key, value) in dailyMinutes {
            kmpDaily[key] = KotlinInt(int: Int32(value))
        }
        return BackupPracticeTimer(
            totalMinutes: Int32(timer["totalMinutes"] as? Int ?? 0),
            totalSessions: Int32(timer["totalSessions"] as? Int ?? 0),
            longestSession: Int32(timer["longestSession"] as? Int ?? 0),
            lastSessionTime: Int64(lastSessionTime * 1000),
            dailyGoal: Int32(timer["dailyGoal"] as? Int ?? 15),
            dailyMinutes: kmpDaily
        )
    }

    private func buildSettings(_ settings: [String: Any]) -> BackupSettings {
        let themeLabel = settings["theme_mode"] as? String ?? "System"
        let tuningLabel = settings["selected_tuning"] as? String ?? "High-G (Standard)"
        return BackupSettings(
            soundEnabled: settings["sound_enabled"] as? Bool ?? true,
            volume: Float(settings["volume"] as? Double ?? 0.7),
            noteDurationMs: Int32(settings["note_duration_ms"] as? Int ?? 600),
            strumDelayMs: Int32(settings["strum_delay_ms"] as? Int ?? 50),
            strumDown: settings["strum_down"] as? Bool ?? true,
            playOnTap: settings["play_on_tap"] as? Bool ?? false,
            themeMode: Self.themeToKMP[themeLabel] ?? "SYSTEM",
            showExplorerTips: settings["show_tips"] as? Bool ?? true,
            showLearnSection: settings["show_learn_tab"] as? Bool ?? true,
            showReferenceSection: settings["show_reference_tab"] as? Bool ?? true,
            tuning: Self.tuningToKMP[tuningLabel] ?? "HIGH_G",
            leftHanded: settings["left_handed"] as? Bool ?? false,
            lastFret: Int32(settings["last_fret"] as? Int ?? 12),
            showNoteNames: settings["show_note_names"] as? Bool ?? true
        )
    }

    // MARK: - Extract iOS VM data from KMP objects (import)

    private func extractFavorites(_ favs: [BackupFavorite]) -> [[String: Any]] {
        favs.map { f in
            var out: [String: Any] = [
                "rootPitchClass": Int(f.rootPitchClass),
                "chordSymbol": f.chordSymbol,
                "frets": f.frets.asInts,
                "addedAt": Double(f.addedAt) / 1000.0,
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

    private func extractFolders(_ folders: [BackupFavoriteFolder]) -> [[String: Any]] {
        folders.map { f in
            [
                "id": f.id,
                "name": f.name,
                "createdAt": Double(f.createdAt) / 1000.0,
                "voicingOrder": f.voicingOrder.asStrings,
            ] as [String: Any]
        }
    }

    private func extractChordSheets(_ sheets: [BackupChordSheet]) -> [[String: Any]] {
        sheets.map { s in
            [
                "id": s.id,
                "title": s.title,
                "subtitle": s.subtitle,
                "artist": s.artist,
                "content": s.content,
                "key": s.key,
                "capo": Int(s.capo),
                "strumPatternName": s.strumPatternName,
                "labels": s.labels.asStrings,
                "createdAt": Double(s.createdAt),
                "updatedAt": Double(s.updatedAt),
                "viewCount": Int(s.viewCount),
                "lastViewedAt": Double(s.lastViewedAt),
                "totalViewTimeMs": Double(s.totalViewTimeMs),
            ] as [String: Any]
        }
    }

    private func extractProgressions(_ progs: [BackupProgression]) -> [[String: Any]] {
        progs.map { p in
            let degrees = p.degrees.asArray(of: BackupChordDegree.self)
            return [
                "id": p.id,
                "name": p.name,
                "description": p.description_,
                "scaleType": p.scaleType,
                "degreeIntervals": degrees.map { Int($0.interval) },
                "degreeQualities": degrees.map { $0.quality },
                "degreeNumerals": degrees.map { $0.numeral },
                "createdAt": Double(p.createdAt) / 1000.0,
            ] as [String: Any]
        }
    }

    private func extractMelodies(_ melodies: [BackupMelody]) -> [[String: Any]] {
        melodies.map { m in
            let notes: [[String: Any]] = m.notes.asArray(of: BackupMelodyNote.self).map { n in
                var note: [String: Any] = [
                    "id": UUID().uuidString,
                    "octave": Int(n.octave),
                    "duration": Self.durationFromKMP[n.duration] ?? n.duration,
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
                "id": m.id,
                "name": m.name,
                "notes": notes,
                "bpm": Int(m.bpm),
                "createdAt": Double(m.createdAt) / 1000.0,
            ] as [String: Any]
        }
    }

    private func extractStrumPatterns(_ patterns: [BackupStrumPattern]) -> [[String: Any]] {
        patterns.map { p in
            let beats: [[String: Any]] = p.beats.asArray(of: BackupStrumBeat.self).map { b in
                ["direction": b.direction, "emphasis": b.emphasis] as [String: Any]
            }
            return [
                "id": p.id,
                "name": p.name,
                "beats": beats,
                "createdAt": Double(p.createdAt) / 1000.0,
                "timeSignature": p.timeSignature,
            ] as [String: Any]
        }
    }

    private func extractFingerpickPatterns(_ patterns: [BackupFingerpickingPattern]) -> [[String: Any]] {
        patterns.map { p in
            let steps: [[String: Any]] = p.steps.asArray(of: BackupFingerpickStep.self).map { s in
                [
                    "finger": Self.fingerFromKMP[s.finger] ?? s.finger,
                    "stringIndex": Int(s.stringIndex),
                    "emphasis": s.emphasis,
                ] as [String: Any]
            }
            return [
                "id": p.id,
                "name": p.name,
                "steps": steps,
                "createdAt": Double(p.createdAt) / 1000.0,
                "timeSignature": p.timeSignature,
            ] as [String: Any]
        }
    }

    private func extractSetlists(_ setlists: [BackupSetlist]) -> [[String: Any]] {
        setlists.map { sl in
            [
                "id": sl.id,
                "name": sl.name,
                "songIds": sl.songIds.asStrings,
                "createdAt": Double(sl.createdAt),
                "updatedAt": Double(sl.updatedAt),
            ] as [String: Any]
        }
    }

    private func extractLearnProgress(_ entries: [String: String]) -> [String: Any] {
        var out: [String: Any] = [:]
        for (key, value) in entries {
            if value == "true" {
                out[key] = true
            } else if value == "false" {
                out[key] = false
            } else if let intVal = Int(value) {
                out[key] = intVal
            } else {
                out[key] = value
            }
        }
        return out
    }

    private func extractPracticeTimer(_ timer: BackupPracticeTimer) -> [String: Any] {
        var dailyMinutes: [String: Int] = [:]
        if let kmpDaily = timer.dailyMinutes as? [String: Any] {
            for (key, value) in kmpDaily {
                if let num = value as? NSNumber {
                    dailyMinutes[key] = num.intValue
                }
            }
        }
        return [
            "totalMinutes": Int(timer.totalMinutes),
            "totalSessions": Int(timer.totalSessions),
            "longestSession": Int(timer.longestSession),
            "lastSessionTime": Double(timer.lastSessionTime) / 1000.0,
            "dailyGoal": Int(timer.dailyGoal),
            "dailyMinutes": dailyMinutes,
        ]
    }

    private func extractSettings(_ settings: BackupSettings) -> [String: Any] {
        [
            "sound_enabled": settings.soundEnabled,
            "volume": Double(settings.volume),
            "note_duration_ms": Int(settings.noteDurationMs),
            "strum_delay_ms": Int(settings.strumDelayMs),
            "strum_down": settings.strumDown,
            "play_on_tap": settings.playOnTap,
            "theme_mode": Self.themeFromKMP[settings.themeMode] ?? "System",
            "show_tips": settings.showExplorerTips,
            "show_learn_tab": settings.showLearnSection,
            "show_reference_tab": settings.showReferenceSection,
            "selected_tuning": Self.tuningFromKMP[settings.tuning] ?? "High-G (Standard)",
            "left_handed": settings.leftHanded,
            "last_fret": Int(settings.lastFret),
            "show_note_names": settings.showNoteNames,
        ]
    }

    // MARK: - Helpers

    private func millisFromAny(_ value: Any?) -> Int64 {
        if let d = value as? Double {
            return Int64(d)
        } else if let i = value as? Int {
            return Int64(i)
        }
        return 0
    }

    // MARK: - Domain mappings (iOS ↔ KMP)

    private static let durationToKMP: [String: String] = [
        "\u{1D15D}": "WHOLE",
        "\u{1D15E}": "HALF",
        "\u{2669}": "QUARTER",
        "\u{266A}": "EIGHTH",
        "\u{1D163}": "SIXTEENTH",
    ]

    private static let durationFromKMP: [String: String] = {
        Dictionary(uniqueKeysWithValues: durationToKMP.map { ($0.value, $0.key) })
    }()

    private static let fingerToKMP: [String: String] = [
        "T": "THUMB", "I": "INDEX", "M": "MIDDLE", "R": "RING", "P": "PINKY",
    ]

    private static let fingerFromKMP: [String: String] = {
        Dictionary(uniqueKeysWithValues: fingerToKMP.map { ($0.value, $0.key) })
    }()

    private static let tuningToKMP: [String: String] = [
        "High-G (Standard)": "HIGH_G",
        "Low-G": "LOW_G",
        "Baritone (DGBE)": "BARITONE",
        "D-Tuning (ADF#B)": "D_TUNING",
        "Slack Key (GCEG)": "SLACK_KEY",
        "Open A (AC#EA)": "OPEN_A",
        "Low A (GCEa)": "LOW_A",
        "Half-Step Down": "HALF_STEP_DOWN",
    ]

    private static let tuningFromKMP: [String: String] = {
        Dictionary(uniqueKeysWithValues: tuningToKMP.map { ($0.value, $0.key) })
    }()

    private static let themeToKMP: [String: String] = [
        "Light": "LIGHT",
        "Dark": "DARK",
        "System": "SYSTEM",
        "High Contrast": "HIGH_CONTRAST",
    ]

    private static let themeFromKMP: [String: String] = {
        Dictionary(uniqueKeysWithValues: themeToKMP.map { ($0.value, $0.key) })
    }()
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
