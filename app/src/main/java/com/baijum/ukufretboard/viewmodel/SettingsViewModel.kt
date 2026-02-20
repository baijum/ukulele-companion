package com.baijum.ukufretboard.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.baijum.ukufretboard.data.AppSettings
import com.baijum.ukufretboard.data.DisplaySettings
import com.baijum.ukufretboard.data.FretboardSettings

import com.baijum.ukufretboard.data.PitchMonitorSettings
import com.baijum.ukufretboard.data.ScalePracticeSettings
import com.baijum.ukufretboard.data.SoundSettings
import com.baijum.ukufretboard.data.ThemeMode
import com.baijum.ukufretboard.data.TunerSettings
import com.baijum.ukufretboard.data.TuningSettings
import com.baijum.ukufretboard.data.UkuleleTuning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel that manages all application settings.
 *
 * Provides a single [StateFlow] of [AppSettings] and section-specific update
 * methods. Designed to be shared across all screens so that settings changes
 * are immediately reflected everywhere.
 *
 * Settings are persisted to SharedPreferences and restored on app restart.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())

    /** Observable stream of the current application settings. */
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /**
     * Updates the sound settings by applying a transformation function.
     *
     * Example usage:
     * ```
     * updateSound { it.copy(enabled = false) }
     * updateSound { it.copy(strumDelayMs = 100) }
     * ```
     *
     * @param transform A function that receives the current [SoundSettings]
     *   and returns the updated [SoundSettings].
     */
    fun updateSound(transform: (SoundSettings) -> SoundSettings) {
        _settings.update { current ->
            current.copy(sound = transform(current.sound)).also { saveSettings(it) }
        }
    }

    /**
     * Updates the display settings by applying a transformation function.
     */
    fun updateDisplay(transform: (DisplaySettings) -> DisplaySettings) {
        _settings.update { current ->
            current.copy(display = transform(current.display)).also { saveSettings(it) }
        }
    }

    /**
     * Updates the tuning settings by applying a transformation function.
     */
    fun updateTuning(transform: (TuningSettings) -> TuningSettings) {
        _settings.update { current ->
            current.copy(tuning = transform(current.tuning)).also { saveSettings(it) }
        }
    }

    /**
     * Updates the fretboard settings by applying a transformation function.
     */
    fun updateFretboard(transform: (FretboardSettings) -> FretboardSettings) {
        _settings.update { current ->
            current.copy(fretboard = transform(current.fretboard)).also { saveSettings(it) }
        }
    }


    /**
     * Updates the scale practice settings by applying a transformation function.
     */
    fun updateScalePractice(transform: (ScalePracticeSettings) -> ScalePracticeSettings) {
        _settings.update { current ->
            current.copy(scalePractice = transform(current.scalePractice)).also { saveSettings(it) }
        }
    }

    /**
     * Updates the tuner settings by applying a transformation function.
     */
    fun updateTuner(transform: (TunerSettings) -> TunerSettings) {
        _settings.update { current ->
            current.copy(tuner = transform(current.tuner)).also { saveSettings(it) }
        }
    }

    /**
     * Updates the pitch monitor settings by applying a transformation function.
     */
    fun updatePitchMonitor(transform: (PitchMonitorSettings) -> PitchMonitorSettings) {
        _settings.update { current ->
            current.copy(pitchMonitor = transform(current.pitchMonitor)).also { saveSettings(it) }
        }
    }

    /**
     * Marks onboarding as completed so the wizard is not shown again.
     */
    fun completeOnboarding() {
        _settings.update { current ->
            current.copy(onboardingCompleted = true).also { saveSettings(it) }
        }
    }

    /**
     * Dismisses the Explorer tips card so it is not shown again.
     */
    fun dismissExplorerTips() {
        _settings.update { current ->
            current.copy(explorerTipsDismissed = true).also { saveSettings(it) }
        }
    }

    /**
     * Replaces all settings with the given [AppSettings].
     * Used for sync/restore operations.
     */
    fun replaceAll(newSettings: AppSettings) {
        _settings.value = newSettings
        saveSettings(newSettings)
    }

    /**
     * Returns a snapshot of the current settings for export.
     */
    fun exportSettings(): AppSettings = _settings.value

    // ── Persistence ─────────────────────────────────────────────────────

    private fun saveSettings(s: AppSettings) {
        prefs.edit()
            // Sound
            .putBoolean(KEY_SOUND_ENABLED, s.sound.enabled)
            .putFloat(KEY_VOLUME, s.sound.volume)
            .putInt(KEY_NOTE_DURATION, s.sound.noteDurationMs)
            .putInt(KEY_STRUM_DELAY, s.sound.strumDelayMs)
            .putBoolean(KEY_STRUM_DOWN, s.sound.strumDown)
            .putBoolean(KEY_PLAY_ON_TAP, s.sound.playOnTap)
            // Display
            .putString(KEY_THEME_MODE, s.display.themeMode.name)
            .putBoolean(KEY_SHOW_EXPLORER_TIPS, s.display.showExplorerTips)
            .putBoolean(KEY_SHOW_LEARN_SECTION, s.display.showLearnSection)
            .putBoolean(KEY_SHOW_REFERENCE_SECTION, s.display.showReferenceSection)
            // Tuning
            .putString(KEY_TUNING, s.tuning.tuning.name)
            // Fretboard
            .putBoolean(KEY_LEFT_HANDED, s.fretboard.leftHanded)
            .putInt(KEY_LAST_FRET, s.fretboard.lastFret)
            .putBoolean(KEY_SHOW_NOTE_NAMES, s.fretboard.showNoteNames)
            .putBoolean(KEY_ALLOW_MUTED_STRINGS, s.fretboard.allowMutedStrings)

            // Scale Practice
            .putInt(KEY_SCALE_PRACTICE_ROOT, s.scalePractice.lastRoot)
            .putString(KEY_SCALE_PRACTICE_SCALE, s.scalePractice.lastScaleName)
            .putString(KEY_SCALE_PRACTICE_CATEGORY, s.scalePractice.lastCategory)
            .putInt(KEY_SCALE_PRACTICE_BPM, s.scalePractice.lastBpm)
            .putInt(KEY_SCALE_PRACTICE_MODE, s.scalePractice.lastMode)
            .putBoolean(KEY_SCALE_PRACTICE_FRETBOARD, s.scalePractice.showFretboard)
            // Tuner
            .putBoolean(KEY_TUNER_SPOKEN_FEEDBACK, s.tuner.spokenFeedback)
            .putBoolean(KEY_TUNER_PRECISION_MODE, s.tuner.precisionMode)
            .putFloat(KEY_TUNER_A4_REFERENCE, s.tuner.a4Reference)
            .putBoolean(KEY_TUNER_AUTO_ADVANCE, s.tuner.autoAdvance)
            .putBoolean(KEY_TUNER_AUTO_START, s.tuner.autoStart)
            // Pitch Monitor
            .putFloat(KEY_PM_NOISE_GATE_SENSITIVITY, s.pitchMonitor.noiseGateSensitivity)
            // Onboarding
            .putBoolean(KEY_ONBOARDING_COMPLETED, s.onboardingCompleted)
            .putBoolean(KEY_EXPLORER_TIPS_DISMISSED, s.explorerTipsDismissed)
            .apply()
    }

    private fun loadSettings(): AppSettings {
        if (!prefs.contains(KEY_SOUND_ENABLED)) return AppSettings()

        return AppSettings(
            sound = SoundSettings(
                enabled = prefs.getBoolean(KEY_SOUND_ENABLED, true),
                volume = prefs.getFloat(KEY_VOLUME, SoundSettings.DEFAULT_VOLUME),
                noteDurationMs = prefs.getInt(KEY_NOTE_DURATION, SoundSettings.DEFAULT_NOTE_DURATION_MS),
                strumDelayMs = prefs.getInt(KEY_STRUM_DELAY, SoundSettings.DEFAULT_STRUM_DELAY_MS),
                strumDown = prefs.getBoolean(KEY_STRUM_DOWN, true),
                playOnTap = prefs.getBoolean(KEY_PLAY_ON_TAP, false),
            ),
            display = DisplaySettings(
                themeMode = try {
                    ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)!!)
                } catch (_: Exception) {
                    ThemeMode.SYSTEM
                },
                showExplorerTips = prefs.getBoolean(KEY_SHOW_EXPLORER_TIPS, true),
                showLearnSection = prefs.getBoolean(KEY_SHOW_LEARN_SECTION, true),
                showReferenceSection = prefs.getBoolean(KEY_SHOW_REFERENCE_SECTION, true),
            ),
            tuning = TuningSettings(
                tuning = try {
                    UkuleleTuning.valueOf(prefs.getString(KEY_TUNING, UkuleleTuning.HIGH_G.name)!!)
                } catch (_: Exception) {
                    UkuleleTuning.HIGH_G
                },
            ),
            fretboard = FretboardSettings(
                leftHanded = prefs.getBoolean(KEY_LEFT_HANDED, false),
                lastFret = prefs.getInt(KEY_LAST_FRET, FretboardSettings.DEFAULT_LAST_FRET),
                showNoteNames = prefs.getBoolean(KEY_SHOW_NOTE_NAMES, true),
                allowMutedStrings = prefs.getBoolean(KEY_ALLOW_MUTED_STRINGS, false),
            ),

            scalePractice = ScalePracticeSettings(
                lastRoot = prefs.getInt(KEY_SCALE_PRACTICE_ROOT, 0),
                lastScaleName = prefs.getString(KEY_SCALE_PRACTICE_SCALE, "Major") ?: "Major",
                lastCategory = prefs.getString(KEY_SCALE_PRACTICE_CATEGORY, "") ?: "",
                lastBpm = prefs.getInt(KEY_SCALE_PRACTICE_BPM, ScalePracticeSettings.DEFAULT_BPM),
                lastMode = prefs.getInt(KEY_SCALE_PRACTICE_MODE, 0),
                showFretboard = prefs.getBoolean(KEY_SCALE_PRACTICE_FRETBOARD, false),
            ),
            tuner = TunerSettings(
                spokenFeedback = prefs.getBoolean(KEY_TUNER_SPOKEN_FEEDBACK, false),
                precisionMode = prefs.getBoolean(KEY_TUNER_PRECISION_MODE, false),
                a4Reference = prefs.getFloat(KEY_TUNER_A4_REFERENCE, TunerSettings.DEFAULT_A4_REFERENCE),
                autoAdvance = prefs.getBoolean(KEY_TUNER_AUTO_ADVANCE, false),
                autoStart = prefs.getBoolean(KEY_TUNER_AUTO_START, false),
            ),
            pitchMonitor = PitchMonitorSettings(
                noiseGateSensitivity = prefs.getFloat(KEY_PM_NOISE_GATE_SENSITIVITY, PitchMonitorSettings.DEFAULT_SENSITIVITY),
            ),
            onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, true),
            explorerTipsDismissed = prefs.getBoolean(KEY_EXPLORER_TIPS_DISMISSED, true),
        )
    }

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VOLUME = "sound_volume"
        private const val KEY_NOTE_DURATION = "note_duration_ms"
        private const val KEY_STRUM_DELAY = "strum_delay_ms"
        private const val KEY_STRUM_DOWN = "strum_down"
        private const val KEY_PLAY_ON_TAP = "play_on_tap"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_TUNING = "tuning"
        private const val KEY_LEFT_HANDED = "left_handed"
        private const val KEY_LAST_FRET = "last_fret"
        private const val KEY_SHOW_NOTE_NAMES = "show_note_names"
        private const val KEY_ALLOW_MUTED_STRINGS = "allow_muted_strings"

        private const val KEY_SCALE_PRACTICE_ROOT = "scale_practice_root"
        private const val KEY_SCALE_PRACTICE_SCALE = "scale_practice_scale"
        private const val KEY_SCALE_PRACTICE_CATEGORY = "scale_practice_category"
        private const val KEY_SCALE_PRACTICE_BPM = "scale_practice_bpm"
        private const val KEY_SCALE_PRACTICE_MODE = "scale_practice_mode"
        private const val KEY_SCALE_PRACTICE_FRETBOARD = "scale_practice_fretboard"
        private const val KEY_TUNER_SPOKEN_FEEDBACK = "tuner_spoken_feedback"
        private const val KEY_TUNER_PRECISION_MODE = "tuner_precision_mode"
        private const val KEY_TUNER_A4_REFERENCE = "tuner_a4_reference"
        private const val KEY_TUNER_AUTO_ADVANCE = "tuner_auto_advance"
        private const val KEY_TUNER_AUTO_START = "tuner_auto_start"
        private const val KEY_PM_NOISE_GATE_SENSITIVITY = "pm_noise_gate_sensitivity"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_EXPLORER_TIPS_DISMISSED = "explorer_tips_dismissed"
        private const val KEY_SHOW_EXPLORER_TIPS = "show_explorer_tips"
        private const val KEY_SHOW_LEARN_SECTION = "show_learn_section"
        private const val KEY_SHOW_REFERENCE_SECTION = "show_reference_section"
    }
}
