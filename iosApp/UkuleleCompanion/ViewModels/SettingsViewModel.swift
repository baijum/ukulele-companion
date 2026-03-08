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

    private let defaults = UserDefaults(suiteName: "app_settings") ?? .standard

    init() {
        load()
    }

    func load() {
        soundEnabled = defaults.object(forKey: "sound_enabled") as? Bool ?? true
        volume = defaults.object(forKey: "volume") as? Float ?? 1.0
        noteDurationMs = defaults.object(forKey: "note_duration_ms") as? Int ?? 600
        strumDelayMs = defaults.object(forKey: "strum_delay_ms") as? Int ?? 50
        playOnTap = defaults.bool(forKey: "play_on_tap")
        noiseGateFiltering = defaults.object(forKey: "noise_gate_filtering") as? Float ?? 0.75

        themeMode = defaults.string(forKey: "theme_mode") ?? "System"
        showTips = defaults.object(forKey: "show_tips") as? Bool ?? true

        selectedTuning = defaults.string(forKey: "selected_tuning") ?? "High-G (Standard)"

        precisionMode = defaults.bool(forKey: "precision_mode")
        autoAdvance = defaults.bool(forKey: "auto_advance")
        a4Reference = defaults.object(forKey: "a4_reference") as? Float ?? 440.0
        spokenFeedback = defaults.bool(forKey: "spoken_feedback")
        autoStartTuner = defaults.bool(forKey: "auto_start_tuner")

        leftHanded = defaults.bool(forKey: "left_handed")
        showNoteNames = defaults.object(forKey: "show_note_names") as? Bool ?? true
        allowMuted = defaults.bool(forKey: "allow_muted")
        lastFret = defaults.object(forKey: "last_fret") as? Int ?? 12

        showLearnTab = defaults.object(forKey: "show_learn_tab") as? Bool ?? true
        showReferenceTab = defaults.object(forKey: "show_reference_tab") as? Bool ?? true
        strumDown = defaults.object(forKey: "strum_down") as? Bool ?? true

        appLanguage = defaults.string(forKey: "app_language") ?? "system"
        onboardingCompleted = defaults.bool(forKey: "onboarding_completed")
    }

    func save() {
        defaults.set(soundEnabled, forKey: "sound_enabled")
        defaults.set(volume, forKey: "volume")
        defaults.set(noteDurationMs, forKey: "note_duration_ms")
        defaults.set(strumDelayMs, forKey: "strum_delay_ms")
        defaults.set(playOnTap, forKey: "play_on_tap")
        defaults.set(noiseGateFiltering, forKey: "noise_gate_filtering")
        defaults.set(themeMode, forKey: "theme_mode")
        defaults.set(showTips, forKey: "show_tips")
        defaults.set(selectedTuning, forKey: "selected_tuning")
        defaults.set(precisionMode, forKey: "precision_mode")
        defaults.set(autoAdvance, forKey: "auto_advance")
        defaults.set(a4Reference, forKey: "a4_reference")
        defaults.set(spokenFeedback, forKey: "spoken_feedback")
        defaults.set(autoStartTuner, forKey: "auto_start_tuner")
        defaults.set(leftHanded, forKey: "left_handed")
        defaults.set(showNoteNames, forKey: "show_note_names")
        defaults.set(allowMuted, forKey: "allow_muted")
        defaults.set(lastFret, forKey: "last_fret")
        defaults.set(showLearnTab, forKey: "show_learn_tab")
        defaults.set(showReferenceTab, forKey: "show_reference_tab")
        defaults.set(strumDown, forKey: "strum_down")
        defaults.set(appLanguage, forKey: "app_language")
        defaults.set(onboardingCompleted, forKey: "onboarding_completed")
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
        [
            "sound_enabled": soundEnabled,
            "volume": volume,
            "note_duration_ms": noteDurationMs,
            "strum_delay_ms": strumDelayMs,
            "play_on_tap": playOnTap,
            "noise_gate_filtering": noiseGateFiltering,
            "theme_mode": themeMode,
            "show_tips": showTips,
            "selected_tuning": selectedTuning,
            "precision_mode": precisionMode,
            "auto_advance": autoAdvance,
            "a4_reference": a4Reference,
            "spoken_feedback": spokenFeedback,
            "auto_start_tuner": autoStartTuner,
            "left_handed": leftHanded,
            "show_note_names": showNoteNames,
            "allow_muted": allowMuted,
            "last_fret": lastFret,
            "show_learn_tab": showLearnTab,
            "show_reference_tab": showReferenceTab,
            "strum_down": strumDown,
            "app_language": appLanguage,
        ]
    }

    func importSettings(_ dict: [String: Any]) {
        if let v = dict["sound_enabled"] as? Bool { soundEnabled = v }
        if let v = dict["volume"] as? Float { volume = v }
        if let v = dict["note_duration_ms"] as? Int { noteDurationMs = v }
        if let v = dict["strum_delay_ms"] as? Int { strumDelayMs = v }
        if let v = dict["play_on_tap"] as? Bool { playOnTap = v }
        if let v = dict["noise_gate_filtering"] as? Float { noiseGateFiltering = v }
        if let v = dict["theme_mode"] as? String { themeMode = v }
        if let v = dict["show_tips"] as? Bool { showTips = v }
        if let v = dict["selected_tuning"] as? String { selectedTuning = v }
        if let v = dict["precision_mode"] as? Bool { precisionMode = v }
        if let v = dict["auto_advance"] as? Bool { autoAdvance = v }
        if let v = dict["a4_reference"] as? Float { a4Reference = v }
        if let v = dict["spoken_feedback"] as? Bool { spokenFeedback = v }
        if let v = dict["auto_start_tuner"] as? Bool { autoStartTuner = v }
        if let v = dict["left_handed"] as? Bool { leftHanded = v }
        if let v = dict["show_note_names"] as? Bool { showNoteNames = v }
        if let v = dict["allow_muted"] as? Bool { allowMuted = v }
        if let v = dict["last_fret"] as? Int { lastFret = v }
        if let v = dict["show_learn_tab"] as? Bool { showLearnTab = v }
        if let v = dict["show_reference_tab"] as? Bool { showReferenceTab = v }
        if let v = dict["strum_down"] as? Bool { strumDown = v }
        if let v = dict["app_language"] as? String { appLanguage = v }
        save()
    }
}
