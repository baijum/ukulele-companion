import SwiftUI
import UniformTypeIdentifiers
import shared

struct SettingsView: View {
    @StateObject private var viewModel = SettingsViewModel()
    @StateObject private var favoritesVM = FavoritesViewModel()
    @StateObject private var songbookVM = SongbookViewModel()
    @StateObject private var melodyVM = MelodyViewModel()
    @StateObject private var progressionsVM = ProgressionsViewModel()
    @StateObject private var learnVM = LearnViewModel()
    @StateObject private var practiceTimerVM = PracticeTimerViewModel()
    @StateObject private var customPatternsVM = CustomPatternsViewModel()
    @State private var backupManager: BackupRestoreManager?
    @State private var backupDocument: BackupDocument?
    @State private var showExporter = false
    @State private var showImporter = false
    @State private var showRestoreAlert = false
    @State private var showLanguageRestart = false

    private let tuningOptions = [
        "High-G (Standard)", "Low-G", "Baritone (DGBE)", "D-Tuning (ADF#B)",
        "Slack Key (GCEG)", "Open A (AC#EA)", "Low A (GCEa)", "Half-Step Down"
    ]
    private let themeModes = ["Light", "Dark", "System", "High Contrast"]

    private let languageOptions: [(code: String, name: String)] = [
        ("system", "System"),
        ("en", "English"),
        ("es", "Español"),
        ("fr", "Français"),
        ("pt", "Português"),
        ("de", "Deutsch"),
        ("ja", "日本語"),
        ("zh-Hans", "简体中文"),
        ("ko", "한국어"),
        ("hi", "हिन्दी"),
        ("ar", "العربية"),
        ("ru", "Русский"),
        ("it", "Italiano"),
        ("id", "Bahasa Indonesia"),
        ("tr", "Türkçe"),
        ("nl", "Nederlands"),
        ("pl", "Polski"),
    ]

    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Sound").accessibilityAddTraits(.isHeader)) {
                    Toggle("Sound Enabled", isOn: $viewModel.soundEnabled)
                        .onChange(of: viewModel.soundEnabled) { _ in viewModel.save() }

                    VStack(alignment: .leading) {
                        HStack {
                            Text("Volume")
                            Spacer()
                            Text("\(Int(viewModel.volume * 100))%")
                                .foregroundStyle(.secondary)
                        }
                        Slider(value: $viewModel.volume, in: 0...1)
                            .disabled(!viewModel.soundEnabled)
                            .onChange(of: viewModel.volume) { _ in viewModel.save() }
                    }

                    Stepper("Note Duration: \(viewModel.noteDurationMs)ms",
                            value: $viewModel.noteDurationMs, in: 300...1200, step: 50)
                        .disabled(!viewModel.soundEnabled)
                        .onChange(of: viewModel.noteDurationMs) { _ in viewModel.save() }

                    Stepper("Strum Delay: \(viewModel.strumDelayMs)ms",
                            value: $viewModel.strumDelayMs, in: 0...150, step: 10)
                        .disabled(!viewModel.soundEnabled)
                        .onChange(of: viewModel.strumDelayMs) { _ in viewModel.save() }

                    Toggle("Play on Tap", isOn: $viewModel.playOnTap)
                        .disabled(!viewModel.soundEnabled)
                        .onChange(of: viewModel.playOnTap) { _ in viewModel.save() }

                    Toggle("Strum Down", isOn: $viewModel.strumDown)
                        .disabled(!viewModel.soundEnabled)
                        .onChange(of: viewModel.strumDown) { _ in viewModel.save() }

                    VStack(alignment: .leading) {
                        HStack {
                            Text("Noise Gate")
                            Spacer()
                            Text("\(Int(viewModel.noiseGateFiltering * 100))%")
                                .foregroundStyle(.secondary)
                        }
                        Slider(value: $viewModel.noiseGateFiltering, in: 0...1)
                            .onChange(of: viewModel.noiseGateFiltering) { _ in viewModel.save() }
                            .accessibilityValue("\(Int(viewModel.noiseGateFiltering * 100)) percent")
                    }
                }

                Section(header: Text("Display").accessibilityAddTraits(.isHeader)) {
                    Picker("Theme", selection: $viewModel.themeMode) {
                        ForEach(themeModes, id: \.self) { mode in
                            Text(mode).tag(mode)
                        }
                    }
                    .onChange(of: viewModel.themeMode) { _ in viewModel.save() }

                    Toggle("Show Tips", isOn: $viewModel.showTips)
                        .onChange(of: viewModel.showTips) { _ in viewModel.save() }

                    Toggle("Show Learn Tab", isOn: $viewModel.showLearnTab)
                        .onChange(of: viewModel.showLearnTab) { _ in viewModel.save() }

                    Toggle("Show Reference Tab", isOn: $viewModel.showReferenceTab)
                        .onChange(of: viewModel.showReferenceTab) { _ in viewModel.save() }
                }

                Section(header: Text("Language").accessibilityAddTraits(.isHeader)) {
                    Picker("App Language", selection: $viewModel.appLanguage) {
                        ForEach(languageOptions, id: \.code) { lang in
                            Text(lang.name).tag(lang.code)
                        }
                    }
                    .onChange(of: viewModel.appLanguage) { _ in
                        viewModel.save()
                        viewModel.applyLanguage()
                        showLanguageRestart = true
                    }
                }

                Section(header: Text("Tuning").accessibilityAddTraits(.isHeader)) {
                    Picker("Preset", selection: $viewModel.selectedTuning) {
                        ForEach(tuningOptions, id: \.self) { t in
                            Text(t).tag(t)
                        }
                    }
                    .onChange(of: viewModel.selectedTuning) { _ in viewModel.save() }
                }

                Section(header: Text("Tuner").accessibilityAddTraits(.isHeader)) {
                    Toggle("Auto-Start", isOn: $viewModel.autoStartTuner)
                        .onChange(of: viewModel.autoStartTuner) { _ in viewModel.save() }

                    Text("Start listening automatically when the tuner tab is opened")
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    Toggle("Precision Mode", isOn: $viewModel.precisionMode)
                        .onChange(of: viewModel.precisionMode) { _ in viewModel.save() }

                    Toggle("Auto-Advance", isOn: $viewModel.autoAdvance)
                        .onChange(of: viewModel.autoAdvance) { _ in viewModel.save() }

                    Toggle("Spoken Feedback", isOn: $viewModel.spokenFeedback)
                        .onChange(of: viewModel.spokenFeedback) { _ in viewModel.save() }

                    VStack(alignment: .leading) {
                        HStack {
                            Text("A4 Reference")
                            Spacer()
                            Text(String(format: "%.1f Hz", viewModel.a4Reference))
                                .foregroundStyle(.secondary)
                        }
                        Slider(value: $viewModel.a4Reference, in: 415...465)
                            .onChange(of: viewModel.a4Reference) { _ in viewModel.save() }
                    }
                }

                Section(header: Text("Fretboard").accessibilityAddTraits(.isHeader)) {
                    Toggle("Left-Handed", isOn: $viewModel.leftHanded)
                        .onChange(of: viewModel.leftHanded) { _ in viewModel.save() }

                    Toggle("Show Note Names", isOn: $viewModel.showNoteNames)
                        .onChange(of: viewModel.showNoteNames) { _ in viewModel.save() }

                    Toggle("Allow Muted Strings", isOn: $viewModel.allowMuted)
                        .onChange(of: viewModel.allowMuted) { _ in viewModel.save() }

                    Stepper("Last Fret: \(viewModel.lastFret)",
                            value: $viewModel.lastFret, in: 12...22)
                        .onChange(of: viewModel.lastFret) { _ in viewModel.save() }
                }

                Section(header: Text("Backup & Restore").accessibilityAddTraits(.isHeader)) {
                    Button("Export Backup") {
                        ensureBackupManager()
                        backupDocument = BackupDocument(data: backupManager!.buildBackupData())
                        showExporter = true
                    }
                    Button("Restore from Backup") { showRestoreAlert = true }
                    if let date = backupManager?.lastBackupDate {
                        Text("Last backup: \(date)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Section(header: Text("About").accessibilityAddTraits(.isHeader)) {
                    HStack {
                        Text("Ukulele Companion")
                            .fontWeight(.bold)
                        Spacer()
                        Text(appVersion)
                            .foregroundStyle(.secondary)
                    }

                    Text("Your all-in-one ukulele companion for chords, scales, music theory, and composition. Free, offline, no ads, no tracking.")
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    NavigationLink("Help") {
                        HelpView()
                    }
                    Link(destination: URL(string: "https://baijum.github.io/ukulele-companion")!) {
                        Label("Website", systemImage: "globe")
                    }
                    .accessibilityHint("Opens in Safari")
                    Link(destination: URL(string: "https://archive.org/details/ukulele-book")!) {
                        Label("Free Ukulele Book", systemImage: "book")
                    }
                    .accessibilityHint("Opens in Safari")
                    Link(destination: URL(string: "https://www.youtube.com/playlist?list=PL4GycHdD--uonaifRHrBvNU7ym5jAVs8c")!) {
                        Label("Feature Guide Videos", systemImage: "play.rectangle")
                    }
                    .accessibilityHint("Opens YouTube in Safari")
                }

                Section(header: Text("Credits").accessibilityAddTraits(.isHeader)) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Audio Samples")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        Text("\"Ukelele single notes, close-mic\" by stomachache (Freesound.org)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    VStack(alignment: .leading, spacing: 4) {
                        Text("License")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        Text("CC BY 3.0")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
        }
        .fileExporter(isPresented: $showExporter,
                      document: backupDocument,
                      contentType: .json,
                      defaultFilename: "ukulele_companion_backup.json") { result in
            if case .success = result {
                backupManager?.recordBackupDate()
            }
            backupDocument = nil
        }
        .fileImporter(isPresented: $showImporter,
                      allowedContentTypes: [.json]) { result in
            if case .success(let url) = result {
                getBackupManager().restoreFromURL(url)
            }
        }
        .alert("Language Changed", isPresented: $showLanguageRestart) {
            Button("OK") {}
        } message: {
            Text("Please restart the app for the language change to take full effect.")
        }
        .alert("Restore Backup?", isPresented: $showRestoreAlert) {
            Button("Restore") {
                ensureBackupManager()
                showImporter = true
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will merge the backup data with your current data. Existing items will not be overwritten.")
        }
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }

    private func ensureBackupManager() {
        if backupManager == nil {
            backupManager = BackupRestoreManager(
                favoritesVM: favoritesVM,
                songbookVM: songbookVM,
                melodyVM: melodyVM,
                progressionsVM: progressionsVM,
                learnVM: learnVM,
                practiceTimerVM: practiceTimerVM,
                customPatternsVM: customPatternsVM,
                settingsVM: viewModel
            )
        }
    }

    private func getBackupManager() -> BackupRestoreManager {
        ensureBackupManager()
        return backupManager!
    }
}
