import Foundation

final class SettingsRepository {
    private let defaults = UserDefaults(suiteName: "app_settings") ?? .standard

    func load() -> SettingsData {
        SettingsData(
            soundEnabled: defaults.object(forKey: "sound_enabled") as? Bool ?? true,
            volume: defaults.object(forKey: "volume") as? Float ?? 1.0,
            noteDurationMs: defaults.object(forKey: "note_duration_ms") as? Int ?? 600,
            strumDelayMs: defaults.object(forKey: "strum_delay_ms") as? Int ?? 50,
            playOnTap: defaults.bool(forKey: "play_on_tap"),
            noiseGateFiltering: defaults.object(forKey: "noise_gate_filtering") as? Float ?? 0.75,
            themeMode: defaults.string(forKey: "theme_mode") ?? "System",
            showTips: defaults.object(forKey: "show_tips") as? Bool ?? true,
            selectedTuning: defaults.string(forKey: "selected_tuning") ?? "High-G (Standard)",
            precisionMode: defaults.bool(forKey: "precision_mode"),
            autoAdvance: defaults.bool(forKey: "auto_advance"),
            a4Reference: defaults.object(forKey: "a4_reference") as? Float ?? 440.0,
            spokenFeedback: defaults.bool(forKey: "spoken_feedback"),
            autoStartTuner: defaults.bool(forKey: "auto_start_tuner"),
            leftHanded: defaults.bool(forKey: "left_handed"),
            showNoteNames: defaults.object(forKey: "show_note_names") as? Bool ?? true,
            allowMuted: defaults.bool(forKey: "allow_muted"),
            lastFret: defaults.object(forKey: "last_fret") as? Int ?? 12,
            showLearnTab: defaults.object(forKey: "show_learn_tab") as? Bool ?? true,
            showReferenceTab: defaults.object(forKey: "show_reference_tab") as? Bool ?? true,
            strumDown: defaults.object(forKey: "strum_down") as? Bool ?? true,
            appLanguage: defaults.string(forKey: "app_language") ?? "system",
            onboardingCompleted: defaults.bool(forKey: "onboarding_completed"),
            chordDisplayStyle: defaults.string(forKey: "chord_display_style") ?? "above",
            chordColor: defaults.string(forKey: "chord_color") ?? "theme",
            showChordDiagramRail: defaults.object(forKey: "show_chord_diagram_rail") as? Bool ?? true
        )
    }

    func save(_ data: SettingsData) {
        defaults.set(data.soundEnabled, forKey: "sound_enabled")
        defaults.set(data.volume, forKey: "volume")
        defaults.set(data.noteDurationMs, forKey: "note_duration_ms")
        defaults.set(data.strumDelayMs, forKey: "strum_delay_ms")
        defaults.set(data.playOnTap, forKey: "play_on_tap")
        defaults.set(data.noiseGateFiltering, forKey: "noise_gate_filtering")
        defaults.set(data.themeMode, forKey: "theme_mode")
        defaults.set(data.showTips, forKey: "show_tips")
        defaults.set(data.selectedTuning, forKey: "selected_tuning")
        defaults.set(data.precisionMode, forKey: "precision_mode")
        defaults.set(data.autoAdvance, forKey: "auto_advance")
        defaults.set(data.a4Reference, forKey: "a4_reference")
        defaults.set(data.spokenFeedback, forKey: "spoken_feedback")
        defaults.set(data.autoStartTuner, forKey: "auto_start_tuner")
        defaults.set(data.leftHanded, forKey: "left_handed")
        defaults.set(data.showNoteNames, forKey: "show_note_names")
        defaults.set(data.allowMuted, forKey: "allow_muted")
        defaults.set(data.lastFret, forKey: "last_fret")
        defaults.set(data.showLearnTab, forKey: "show_learn_tab")
        defaults.set(data.showReferenceTab, forKey: "show_reference_tab")
        defaults.set(data.strumDown, forKey: "strum_down")
        defaults.set(data.appLanguage, forKey: "app_language")
        defaults.set(data.onboardingCompleted, forKey: "onboarding_completed")
        defaults.set(data.chordDisplayStyle, forKey: "chord_display_style")
        defaults.set(data.chordColor, forKey: "chord_color")
        defaults.set(data.showChordDiagramRail, forKey: "show_chord_diagram_rail")
    }

    func exportSettings() -> [String: Any] {
        [
            "sound_enabled": defaults.object(forKey: "sound_enabled") as? Bool ?? true,
            "volume": defaults.object(forKey: "volume") as? Float ?? 1.0,
            "note_duration_ms": defaults.object(forKey: "note_duration_ms") as? Int ?? 600,
            "strum_delay_ms": defaults.object(forKey: "strum_delay_ms") as? Int ?? 50,
            "play_on_tap": defaults.bool(forKey: "play_on_tap"),
            "noise_gate_filtering": defaults.object(forKey: "noise_gate_filtering") as? Float ?? 0.75,
            "theme_mode": defaults.string(forKey: "theme_mode") ?? "System",
            "show_tips": defaults.object(forKey: "show_tips") as? Bool ?? true,
            "selected_tuning": defaults.string(forKey: "selected_tuning") ?? "High-G (Standard)",
            "precision_mode": defaults.bool(forKey: "precision_mode"),
            "auto_advance": defaults.bool(forKey: "auto_advance"),
            "a4_reference": defaults.object(forKey: "a4_reference") as? Float ?? 440.0,
            "spoken_feedback": defaults.bool(forKey: "spoken_feedback"),
            "auto_start_tuner": defaults.bool(forKey: "auto_start_tuner"),
            "left_handed": defaults.bool(forKey: "left_handed"),
            "show_note_names": defaults.object(forKey: "show_note_names") as? Bool ?? true,
            "allow_muted": defaults.bool(forKey: "allow_muted"),
            "last_fret": defaults.object(forKey: "last_fret") as? Int ?? 12,
            "show_learn_tab": defaults.object(forKey: "show_learn_tab") as? Bool ?? true,
            "show_reference_tab": defaults.object(forKey: "show_reference_tab") as? Bool ?? true,
            "strum_down": defaults.object(forKey: "strum_down") as? Bool ?? true,
            "app_language": defaults.string(forKey: "app_language") ?? "system",
            "onboarding_completed": defaults.bool(forKey: "onboarding_completed"),
            "chord_display_style": defaults.string(forKey: "chord_display_style") ?? "above",
            "chord_color": defaults.string(forKey: "chord_color") ?? "theme",
            "show_chord_diagram_rail": defaults.object(forKey: "show_chord_diagram_rail") as? Bool ?? true,
        ]
    }

    func importSettings(_ dict: [String: Any]) {
        if let v = dict["sound_enabled"] as? Bool { defaults.set(v, forKey: "sound_enabled") }
        if let v = dict["volume"] as? Float { defaults.set(v, forKey: "volume") }
        if let v = dict["note_duration_ms"] as? Int { defaults.set(v, forKey: "note_duration_ms") }
        if let v = dict["strum_delay_ms"] as? Int { defaults.set(v, forKey: "strum_delay_ms") }
        if let v = dict["play_on_tap"] as? Bool { defaults.set(v, forKey: "play_on_tap") }
        if let v = dict["noise_gate_filtering"] as? Float { defaults.set(v, forKey: "noise_gate_filtering") }
        if let v = dict["theme_mode"] as? String { defaults.set(v, forKey: "theme_mode") }
        if let v = dict["show_tips"] as? Bool { defaults.set(v, forKey: "show_tips") }
        if let v = dict["selected_tuning"] as? String { defaults.set(v, forKey: "selected_tuning") }
        if let v = dict["precision_mode"] as? Bool { defaults.set(v, forKey: "precision_mode") }
        if let v = dict["auto_advance"] as? Bool { defaults.set(v, forKey: "auto_advance") }
        if let v = dict["a4_reference"] as? Float { defaults.set(v, forKey: "a4_reference") }
        if let v = dict["spoken_feedback"] as? Bool { defaults.set(v, forKey: "spoken_feedback") }
        if let v = dict["auto_start_tuner"] as? Bool { defaults.set(v, forKey: "auto_start_tuner") }
        if let v = dict["left_handed"] as? Bool { defaults.set(v, forKey: "left_handed") }
        if let v = dict["show_note_names"] as? Bool { defaults.set(v, forKey: "show_note_names") }
        if let v = dict["allow_muted"] as? Bool { defaults.set(v, forKey: "allow_muted") }
        if let v = dict["last_fret"] as? Int { defaults.set(v, forKey: "last_fret") }
        if let v = dict["show_learn_tab"] as? Bool { defaults.set(v, forKey: "show_learn_tab") }
        if let v = dict["show_reference_tab"] as? Bool { defaults.set(v, forKey: "show_reference_tab") }
        if let v = dict["strum_down"] as? Bool { defaults.set(v, forKey: "strum_down") }
        if let v = dict["app_language"] as? String { defaults.set(v, forKey: "app_language") }
        if let v = dict["onboarding_completed"] as? Bool { defaults.set(v, forKey: "onboarding_completed") }
        if let v = dict["chord_display_style"] as? String { defaults.set(v, forKey: "chord_display_style") }
        if let v = dict["chord_color"] as? String { defaults.set(v, forKey: "chord_color") }
        if let v = dict["show_chord_diagram_rail"] as? Bool { defaults.set(v, forKey: "show_chord_diagram_rail") }
    }
}

struct SettingsData {
    var soundEnabled: Bool
    var volume: Float
    var noteDurationMs: Int
    var strumDelayMs: Int
    var playOnTap: Bool
    var noiseGateFiltering: Float
    var themeMode: String
    var showTips: Bool
    var selectedTuning: String
    var precisionMode: Bool
    var autoAdvance: Bool
    var a4Reference: Float
    var spokenFeedback: Bool
    var autoStartTuner: Bool
    var leftHanded: Bool
    var showNoteNames: Bool
    var allowMuted: Bool
    var lastFret: Int
    var showLearnTab: Bool
    var showReferenceTab: Bool
    var strumDown: Bool
    var appLanguage: String
    var onboardingCompleted: Bool
    var chordDisplayStyle: String
    var chordColor: String
    var showChordDiagramRail: Bool
}
