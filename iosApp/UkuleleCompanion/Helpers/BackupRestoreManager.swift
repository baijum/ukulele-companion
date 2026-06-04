import SwiftUI
import UniformTypeIdentifiers

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
    }

    // MARK: - Export (KMP-compatible format)

    func buildBackupData() throws -> Data {
        let (favs, folders) = favoritesVM.exportData()
        let learnData = learnVM.exportProgress()

        let backup: [String: Any] = [
            "version": 3,
            "exportedAt": Int64(Date().timeIntervalSince1970 * 1000),
            "favorites": convertFavoritesToKMP(favs),
            "favoriteFolders": convertFoldersToKMP(folders),
            "chordSheets": songbookVM.exportData(),
            "customProgressions": convertProgressionsToKMP(progressionsVM.exportData()),
            "customStrumPatterns": customPatternsVM.exportStrumData(),
            "customFingerpickingPatterns": convertFingerpickToKMP(customPatternsVM.exportFingerpickData()),
            "setlists": setlistVM.exportData(),
            "melodies": convertMelodiesToKMP(melodyVM.exportData()),
            "learningProgress": convertLearnProgressToKMP(learnData),
            "settings": convertSettingsToKMP(settingsVM.exportSettings()),
            "achievements": extractAchievementsForKMP(learnData),
            "practiceTimer": practiceTimerVM.exportData(),
            "knownChords": [] as [String],
        ]
        do {
            return try JSONSerialization.data(withJSONObject: backup, options: .prettyPrinted)
        } catch {
            throw BackupError.serializationFailed(error)
        }
    }

    // MARK: - Import (accepts KMP/Android format and old iOS format)

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

        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw BackupError.invalidJSON
        }

        let isKMPFormat = json["exportedAt"] != nil
        var importedAny = false

        // --- Favorites ---
        let favsKey = isKMPFormat ? "favorites" : "favorites"
        if let favs = json[favsKey] as? [[String: Any]] {
            let foldersKey = isKMPFormat ? "favoriteFolders" : "folders"
            let folders = json[foldersKey] as? [[String: Any]] ?? []
            if isKMPFormat {
                favoritesVM.importData(
                    favorites: convertFavoritesFromKMP(favs),
                    folders: convertFoldersFromKMP(folders)
                )
            } else {
                favoritesVM.importData(favorites: favs, folders: folders)
            }
            importedAny = true
        }

        // --- Settings ---
        if let settings = json["settings"] as? [String: Any] {
            if isKMPFormat {
                settingsVM.importSettings(convertSettingsFromKMP(settings))
            } else {
                settingsVM.importSettings(settings)
            }
            importedAny = true
        }

        // --- Chord sheets ---
        let sheetsKey = isKMPFormat ? "chordSheets" : "chord_sheets"
        if let songs = json[sheetsKey] as? [[String: Any]] {
            songbookVM.importData(songs)
            importedAny = true
        }

        // --- Melodies ---
        if let melodies = json["melodies"] as? [[String: Any]] {
            if isKMPFormat {
                melodyVM.importData(convertMelodiesFromKMP(melodies))
            } else {
                melodyVM.importData(melodies)
            }
            importedAny = true
        }

        // --- Custom progressions ---
        let progsKey = isKMPFormat ? "customProgressions" : "custom_progressions"
        if let progressions = json[progsKey] as? [[String: Any]] {
            if isKMPFormat {
                progressionsVM.importData(convertProgressionsFromKMP(progressions))
            } else {
                progressionsVM.importData(progressions)
            }
            importedAny = true
        }

        // --- Learning progress ---
        if isKMPFormat {
            if let lp = json["learningProgress"] as? [String: Any],
               let entries = lp["entries"] as? [String: Any] {
                learnVM.importProgress(convertLearnProgressFromKMP(entries))
                importedAny = true
            }
            if let achievements = json["achievements"] as? [String: Any] {
                let ids = Array(achievements.keys)
                if !ids.isEmpty {
                    learnVM.importProgress(["unlocked_achievements": ids])
                    importedAny = true
                }
            }
        } else {
            if let learnData = json["learn_progress"] as? [String: Any] {
                learnVM.importProgress(learnData)
                importedAny = true
            }
        }

        // --- Practice timer ---
        let timerKey = isKMPFormat ? "practiceTimer" : "practice_timer"
        if let timerData = json[timerKey] as? [String: Any] {
            practiceTimerVM.importData(timerData)
            importedAny = true
        }

        // --- Custom strum patterns ---
        let strumKey = isKMPFormat ? "customStrumPatterns" : "custom_strum_patterns"
        if let strumData = json[strumKey] as? [[String: Any]] {
            customPatternsVM.importStrumData(strumData)
            importedAny = true
        }

        // --- Custom fingerpicking patterns ---
        let fpKey = isKMPFormat ? "customFingerpickingPatterns" : "custom_fingerpicking_patterns"
        if let fpData = json[fpKey] as? [[String: Any]] {
            if isKMPFormat {
                customPatternsVM.importFingerpickData(convertFingerpickFromKMP(fpData))
            } else {
                customPatternsVM.importFingerpickData(fpData)
            }
            importedAny = true
        }

        // --- Setlists ---
        if let setlists = json["setlists"] as? [[String: Any]] {
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

    // MARK: - Export Converters (iOS VM format → KMP)

    private func convertFavoritesToKMP(_ favs: [[String: Any]]) -> [[String: Any]] {
        favs.map { f in
            var out = f
            if let addedAt = f["addedAt"] as? Double {
                out["addedAt"] = Int64(addedAt * 1000)
            }
            return out
        }
    }

    private func convertFoldersToKMP(_ folders: [[String: Any]]) -> [[String: Any]] {
        folders.map { f in
            var out = f
            if let createdAt = f["createdAt"] as? Double {
                out["createdAt"] = Int64(createdAt * 1000)
            }
            return out
        }
    }

    private func convertProgressionsToKMP(_ progs: [[String: Any]]) -> [[String: Any]] {
        progs.map { p in
            let createdAt: Any = {
                if let ts = p["createdAt"] as? Double {
                    return Int64(ts * 1000)
                } else if let ts = p["createdAt"] {
                    return ts
                }
                return Int64(Date().timeIntervalSince1970 * 1000)
            }()
            var out: [String: Any] = [
                "id": p["id"] ?? UUID().uuidString,
                "name": p["name"] ?? "",
                "description": p["description"] ?? "",
                "scaleType": p["scaleType"] ?? "MAJOR",
                "createdAt": createdAt,
            ]
            if let intervals = p["degreeIntervals"] as? [Int],
               let qualities = p["degreeQualities"] as? [String],
               let numerals = p["degreeNumerals"] as? [String] {
                let count = min(intervals.count, qualities.count, numerals.count)
                var degrees: [[String: Any]] = []
                for i in 0..<count {
                    degrees.append([
                        "interval": intervals[i],
                        "quality": qualities[i],
                        "numeral": numerals[i],
                    ])
                }
                out["degrees"] = degrees
            }
            return out
        }
    }

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

    private func convertFingerpickToKMP(_ patterns: [[String: Any]]) -> [[String: Any]] {
        patterns.map { p in
            var out = p
            if let steps = p["steps"] as? [[String: Any]] {
                out["steps"] = steps.map { step in
                    var s = step
                    if let finger = step["finger"] as? String {
                        s["finger"] = Self.fingerToKMP[finger] ?? finger
                    }
                    return s
                }
            }
            return out
        }
    }

    private func convertMelodiesToKMP(_ melodies: [[String: Any]]) -> [[String: Any]] {
        melodies.map { melody in
            var out = melody
            if let createdAt = melody["createdAt"] as? Double {
                out["createdAt"] = Int64(createdAt)
            }
            if let notes = melody["notes"] as? [[String: Any]] {
                out["notes"] = notes.map { note in
                    var n: [String: Any] = [
                        "octave": note["octave"] ?? 4,
                    ]
                    if let pc = note["pitchClass"] as? Int {
                        n["pitchClass"] = pc
                    }
                    if let dur = note["duration"] as? String {
                        n["duration"] = Self.durationToKMP[dur] ?? "QUARTER"
                    }
                    return n
                }
            }
            return out
        }
    }

    private func convertLearnProgressToKMP(_ data: [String: Any]) -> [String: Any] {
        var entries: [String: String] = [:]
        for (key, value) in data where key != "unlocked_achievements" {
            entries[key] = "\(value)"
        }
        return ["entries": entries]
    }

    private func extractAchievementsForKMP(_ data: [String: Any]) -> [String: Int64] {
        guard let list = data["unlocked_achievements"] as? [String] else {
            return [:]
        }
        var map: [String: Int64] = [:]
        for id in list {
            map[id] = 0
        }
        return map
    }

    private static let settingsToKMP: [String: String] = [
        "sound_enabled": "soundEnabled",
        "volume": "volume",
        "note_duration_ms": "noteDurationMs",
        "strum_delay_ms": "strumDelayMs",
        "strum_down": "strumDown",
        "play_on_tap": "playOnTap",
        "show_tips": "showExplorerTips",
        "show_learn_tab": "showLearnSection",
        "show_reference_tab": "showReferenceSection",
        "left_handed": "leftHanded",
        "show_note_names": "showNoteNames",
        "last_fret": "lastFret",
    ]

    private static let settingsFromKMP: [String: String] = {
        Dictionary(uniqueKeysWithValues: settingsToKMP.map { ($0.value, $0.key) })
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

    private func convertSettingsToKMP(_ settings: [String: Any]) -> [String: Any] {
        var out: [String: Any] = [:]
        for (key, value) in settings {
            if key == "theme_mode" {
                let label = value as? String ?? "System"
                out["themeMode"] = Self.themeToKMP[label] ?? "SYSTEM"
            } else if key == "selected_tuning" {
                let label = value as? String ?? "High-G (Standard)"
                out["tuning"] = Self.tuningToKMP[label] ?? "HIGH_G"
            } else if let kmpKey = Self.settingsToKMP[key] {
                out[kmpKey] = value
            }
        }
        return out
    }

    // MARK: - Import Converters (KMP → iOS VM format)

    private func convertFavoritesFromKMP(_ favs: [[String: Any]]) -> [[String: Any]] {
        favs.map { f in
            var out = f
            if let addedAt = f["addedAt"] as? Int64 {
                out["addedAt"] = Double(addedAt) / 1000.0
            } else if let addedAt = f["addedAt"] as? Int {
                out["addedAt"] = Double(addedAt) / 1000.0
            }
            return out
        }
    }

    private func convertFoldersFromKMP(_ folders: [[String: Any]]) -> [[String: Any]] {
        folders.map { f in
            var out = f
            if let createdAt = f["createdAt"] as? Int64 {
                out["createdAt"] = Double(createdAt) / 1000.0
            } else if let createdAt = f["createdAt"] as? Int {
                out["createdAt"] = Double(createdAt) / 1000.0
            }
            return out
        }
    }

    private func convertProgressionsFromKMP(_ progs: [[String: Any]]) -> [[String: Any]] {
        progs.compactMap { p in
            guard let degrees = p["degrees"] as? [[String: Any]] else { return nil }
            var out: [String: Any] = [
                "id": p["id"] ?? UUID().uuidString,
                "name": p["name"] ?? "",
                "description": p["description"] ?? "",
                "scaleType": p["scaleType"] ?? "MAJOR",
                "degreeIntervals": degrees.map { $0["interval"] as? Int ?? 0 },
                "degreeQualities": degrees.map { $0["quality"] as? String ?? "" },
                "degreeNumerals": degrees.map { $0["numeral"] as? String ?? "" },
            ]
            if p["createdAt"] != nil {
                out["createdAt"] = p["createdAt"]
            }
            return out
        }
    }

    private func convertMelodiesFromKMP(_ melodies: [[String: Any]]) -> [[String: Any]] {
        melodies.map { melody in
            var out = melody
            if let notes = melody["notes"] as? [[String: Any]] {
                out["notes"] = notes.map { note in
                    var n = note
                    if let dur = note["duration"] as? String,
                       let iosDur = Self.durationFromKMP[dur] {
                        n["duration"] = iosDur
                    }
                    if n["id"] == nil {
                        n["id"] = UUID().uuidString
                    }
                    if n["isRest"] == nil {
                        n["isRest"] = (note["pitchClass"] == nil)
                    }
                    n.removeValue(forKey: "stringIndex")
                    n.removeValue(forKey: "fret")
                    return n
                }
            }
            return out
        }
    }

    private func convertFingerpickFromKMP(_ patterns: [[String: Any]]) -> [[String: Any]] {
        patterns.map { p in
            var out = p
            if let steps = p["steps"] as? [[String: Any]] {
                out["steps"] = steps.map { step in
                    var s = step
                    if let finger = step["finger"] as? String {
                        s["finger"] = Self.fingerFromKMP[finger] ?? finger
                    }
                    return s
                }
            }
            return out
        }
    }

    private func convertLearnProgressFromKMP(_ entries: [String: Any]) -> [String: Any] {
        var out: [String: Any] = [:]
        for (key, value) in entries {
            let str = value as? String ?? "\(value)"
            if str == "true" {
                out[key] = true
            } else if str == "false" {
                out[key] = false
            } else if let intVal = Int(str) {
                out[key] = intVal
            } else {
                out[key] = str
            }
        }
        return out
    }

    private func convertSettingsFromKMP(_ settings: [String: Any]) -> [String: Any] {
        var out: [String: Any] = [:]
        for (key, value) in settings {
            if key == "themeMode" {
                let enumName = value as? String ?? "SYSTEM"
                out["theme_mode"] = Self.themeFromKMP[enumName] ?? "System"
            } else if key == "tuning" {
                let enumName = value as? String ?? "HIGH_G"
                out["selected_tuning"] = Self.tuningFromKMP[enumName] ?? "High-G (Standard)"
            } else if let iosKey = Self.settingsFromKMP[key] {
                out[iosKey] = value
            }
        }
        return out
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
