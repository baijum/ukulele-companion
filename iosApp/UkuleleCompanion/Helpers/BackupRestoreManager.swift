import SwiftUI
import UniformTypeIdentifiers

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

    func buildBackupData() -> Data {
        let (favs, folders) = favoritesVM.exportData()
        let settings = settingsVM.exportSettings()
        let backup: [String: Any] = [
            "version": 3,
            "timestamp": Date().timeIntervalSince1970,
            "favorites": favs,
            "folders": folders,
            "settings": settings,
            "chord_sheets": songbookVM.exportData(),
            "melodies": melodyVM.exportData(),
            "custom_progressions": progressionsVM.exportData(),
            "learn_progress": learnVM.exportProgress(),
            "practice_timer": practiceTimerVM.exportData(),
            "custom_strum_patterns": customPatternsVM.exportStrumData(),
            "custom_fingerpicking_patterns": customPatternsVM.exportFingerpickData(),
            "setlists": setlistVM.exportData(),
        ]
        return (try? JSONSerialization.data(withJSONObject: backup, options: .prettyPrinted)) ?? Data()
    }

    func restoreFromURL(_ url: URL) {
        guard url.startAccessingSecurityScopedResource() else { return }
        defer { url.stopAccessingSecurityScopedResource() }
        guard let data = try? Data(contentsOf: url),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else { return }

        if let favs = json["favorites"] as? [[String: Any]] {
            let folders = json["folders"] as? [[String: Any]] ?? []
            favoritesVM.importData(favorites: favs, folders: folders)
        }
        if let settings = json["settings"] as? [String: Any] {
            settingsVM.importSettings(settings)
        }
        if let songs = json["chord_sheets"] as? [[String: Any]] {
            songbookVM.importData(songs)
        }
        if let melodies = json["melodies"] as? [[String: Any]] {
            melodyVM.importData(melodies)
        }
        if let progressions = json["custom_progressions"] as? [[String: Any]] {
            progressionsVM.importData(progressions)
        }
        if let learnData = json["learn_progress"] as? [String: Any] {
            learnVM.importProgress(learnData)
        }
        if let timerData = json["practice_timer"] as? [String: Any] {
            practiceTimerVM.importData(timerData)
        }
        if let strumData = json["custom_strum_patterns"] as? [[String: Any]] {
            customPatternsVM.importStrumData(strumData)
        }
        if let fpData = json["custom_fingerpicking_patterns"] as? [[String: Any]] {
            customPatternsVM.importFingerpickData(fpData)
        }
        if let setlists = json["setlists"] as? [[String: Any]] {
            setlistVM.importData(setlists)
        }
    }

    func recordBackupDate() {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        lastBackupDate = formatter.string(from: Date())
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
