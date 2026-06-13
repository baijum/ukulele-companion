import Foundation
import shared

@MainActor
final class SettingsViewModel: ObservableObject {
    // Sound
    @Published var soundEnabled: Bool = true
    @Published var volume: Float = 1.0
    @Published var noteDurationMs: Int = 600
    @Published var strumDelayMs: Int = 50
    @Published var playOnTap: Bool = false
    @Published var noiseGateFiltering: Float = 0.75

    // Display
    @Published var themeMode: String = "System"
    @Published var showTips: Bool = true

    // Tuning
    @Published var selectedTuning: String = "High-G (Standard)"

    // Tuner
    @Published var precisionMode: Bool = false
    @Published var autoAdvance: Bool = false
    @Published var a4Reference: Float = 440.0
    @Published var spokenFeedback: Bool = false
    @Published var autoStartTuner: Bool = false

    // Fretboard
    @Published var leftHanded: Bool = false
    @Published var showNoteNames: Bool = true
    @Published var allowMuted: Bool = false
    @Published var lastFret: Int = 12

    // Navigation
    @Published var showLearnTab: Bool = true
    @Published var showReferenceTab: Bool = true

    // Playback
    @Published var strumDown: Bool = true

    // Language
    @Published var appLanguage: String = "system"

    // Onboarding
    @Published var onboardingCompleted: Bool = false

    // Chord display
    @Published var chordDisplayStyle: String = "above"
    @Published var chordColor: String = "theme"

    private let repository: SettingsRepository

    init(repository: SettingsRepository = SettingsRepository()) {
        self.repository = repository
        load()
    }

    func load() {
        let data = repository.load()
        soundEnabled = data.soundEnabled
        volume = data.volume
        noteDurationMs = data.noteDurationMs
        strumDelayMs = data.strumDelayMs
        playOnTap = data.playOnTap
        noiseGateFiltering = data.noiseGateFiltering
        themeMode = data.themeMode
        showTips = data.showTips
        selectedTuning = data.selectedTuning
        precisionMode = data.precisionMode
        autoAdvance = data.autoAdvance
        a4Reference = data.a4Reference
        spokenFeedback = data.spokenFeedback
        autoStartTuner = data.autoStartTuner
        leftHanded = data.leftHanded
        showNoteNames = data.showNoteNames
        allowMuted = data.allowMuted
        lastFret = data.lastFret
        showLearnTab = data.showLearnTab
        showReferenceTab = data.showReferenceTab
        strumDown = data.strumDown
        appLanguage = data.appLanguage
        onboardingCompleted = data.onboardingCompleted
        chordDisplayStyle = data.chordDisplayStyle
        chordColor = data.chordColor
    }

    func save() {
        repository.save(SettingsData(
            soundEnabled: soundEnabled,
            volume: volume,
            noteDurationMs: noteDurationMs,
            strumDelayMs: strumDelayMs,
            playOnTap: playOnTap,
            noiseGateFiltering: noiseGateFiltering,
            themeMode: themeMode,
            showTips: showTips,
            selectedTuning: selectedTuning,
            precisionMode: precisionMode,
            autoAdvance: autoAdvance,
            a4Reference: a4Reference,
            spokenFeedback: spokenFeedback,
            autoStartTuner: autoStartTuner,
            leftHanded: leftHanded,
            showNoteNames: showNoteNames,
            allowMuted: allowMuted,
            lastFret: lastFret,
            showLearnTab: showLearnTab,
            showReferenceTab: showReferenceTab,
            strumDown: strumDown,
            appLanguage: appLanguage,
            onboardingCompleted: onboardingCompleted,
            chordDisplayStyle: chordDisplayStyle,
            chordColor: chordColor
        ))
    }

    func applyLanguage() {
        if appLanguage == "system" {
            UserDefaults.standard.removeObject(forKey: "AppleLanguages")
        } else {
            UserDefaults.standard.set([appLanguage], forKey: "AppleLanguages")
        }
    }

    // MARK: - Export/Import for Backup

    func exportSettings() -> [String: Any] {
        repository.exportSettings()
    }

    func importSettings(_ dict: [String: Any]) {
        repository.importSettings(dict)
        load()
    }
}
