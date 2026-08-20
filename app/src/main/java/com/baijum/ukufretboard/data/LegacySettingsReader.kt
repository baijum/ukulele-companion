package com.baijum.ukufretboard.data

import android.content.SharedPreferences

/**
 * Reads legacy individual-key SharedPreferences and constructs an [AppSettings].
 * Used once during migration to JSON-based storage.
 */
internal object LegacySettingsReader {
    fun read(prefs: SharedPreferences): AppSettings =
        AppSettings(
            sound =
                SoundSettings(
                    enabled = prefs.getBoolean("sound_enabled", true),
                    volume = prefs.getFloat("sound_volume", SoundSettings.DEFAULT_VOLUME),
                    noteDurationMs = prefs.getInt("note_duration_ms", SoundSettings.DEFAULT_NOTE_DURATION_MS),
                    strumDelayMs = prefs.getInt("strum_delay_ms", SoundSettings.DEFAULT_STRUM_DELAY_MS),
                    strumDown = prefs.getBoolean("strum_down", true),
                    playOnTap = prefs.getBoolean("play_on_tap", false),
                    noiseGateFiltering =
                        prefs.getFloat(
                            "pm_noise_gate_filtering",
                            SoundSettings.DEFAULT_NOISE_GATE_FILTERING,
                        ),
                ),
            display =
                DisplaySettings(
                    themeMode =
                        enumOrDefault(
                            prefs.getString("theme_mode", null),
                            ThemeMode.SYSTEM,
                        ),
                    showExplorerTips = prefs.getBoolean("show_explorer_tips", true),
                    showLearnSection = prefs.getBoolean("show_learn_section", true),
                    showReferenceSection = prefs.getBoolean("show_reference_section", true),
                    chordDisplayStyle =
                        enumOrDefault(
                            prefs.getString("chord_display_style", null),
                            ChordDisplayStyle.ABOVE,
                        ),
                    chordColor =
                        enumOrDefault(
                            prefs.getString("chord_color", null),
                            ChordColorOption.THEME,
                        ),
                    showChordDiagramRail = prefs.getBoolean("show_chord_diagram_rail", true),
                ),
            tuning =
                TuningSettings(
                    tuning =
                        enumOrDefault(
                            prefs.getString("tuning", null),
                            UkuleleTuning.HIGH_G,
                        ),
                ),
            fretboard =
                FretboardSettings(
                    leftHanded = prefs.getBoolean("left_handed", false),
                    lastFret = prefs.getInt("last_fret", FretboardSettings.DEFAULT_LAST_FRET),
                    showNoteNames = prefs.getBoolean("show_note_names", true),
                    allowMutedStrings = prefs.getBoolean("allow_muted_strings", false),
                ),
            scalePractice =
                ScalePracticeSettings(
                    lastRoot = prefs.getInt("scale_practice_root", 0),
                    lastScaleName = prefs.getString("scale_practice_scale", "Major") ?: "Major",
                    lastCategory = prefs.getString("scale_practice_category", "") ?: "",
                    lastBpm = prefs.getInt("scale_practice_bpm", ScalePracticeSettings.DEFAULT_BPM),
                    lastMode = prefs.getInt("scale_practice_mode", 0),
                    showFretboard = prefs.getBoolean("scale_practice_fretboard", false),
                    lastDirection = prefs.getInt("scale_practice_direction", 0),
                    loopPlayback = prefs.getBoolean("scale_practice_loop", false),
                    lastFretPosition = prefs.getInt("scale_practice_fret_position", 0),
                ),
            tuner =
                TunerSettings(
                    spokenFeedback = prefs.getBoolean("tuner_spoken_feedback", false),
                    precisionMode = prefs.getBoolean("tuner_precision_mode", false),
                    a4Reference = prefs.getFloat("tuner_a4_reference", TunerSettings.DEFAULT_A4_REFERENCE),
                    autoAdvance = prefs.getBoolean("tuner_auto_advance", false),
                    autoStart = prefs.getBoolean("tuner_auto_start", false),
                ),
            pitchMonitor = PitchMonitorSettings(),
            onboardingCompleted = prefs.getBoolean("onboarding_completed", true),
            explorerTipsDismissed = prefs.getBoolean("explorer_tips_dismissed", true),
        )

    fun removeLegacyKeys(prefs: SharedPreferences) {
        prefs.edit().apply {
            LEGACY_KEYS.forEach { remove(it) }
            apply()
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(
        value: String?,
        default: T,
    ): T =
        value?.let {
            try {
                enumValueOf<T>(it)
            } catch (_: Exception) {
                default
            }
        } ?: default

    private val LEGACY_KEYS =
        listOf(
            "sound_enabled",
            "sound_volume",
            "note_duration_ms",
            "strum_delay_ms",
            "strum_down",
            "play_on_tap",
            "pm_noise_gate_filtering",
            "theme_mode",
            "show_explorer_tips",
            "show_learn_section",
            "show_reference_section",
            "chord_display_style",
            "chord_color",
            "show_chord_diagram_rail",
            "tuning",
            "left_handed",
            "last_fret",
            "show_note_names",
            "allow_muted_strings",
            "scale_practice_root",
            "scale_practice_scale",
            "scale_practice_category",
            "scale_practice_bpm",
            "scale_practice_mode",
            "scale_practice_fretboard",
            "scale_practice_direction",
            "scale_practice_loop",
            "scale_practice_fret_position",
            "tuner_spoken_feedback",
            "tuner_precision_mode",
            "tuner_a4_reference",
            "tuner_auto_advance",
            "tuner_auto_start",
            "onboarding_completed",
            "explorer_tips_dismissed",
        )
}
