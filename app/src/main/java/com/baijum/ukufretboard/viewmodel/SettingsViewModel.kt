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
import com.baijum.ukufretboard.data.TunerSettings
import com.baijum.ukufretboard.data.TuningSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

/**
 * ViewModel that manages all application settings.
 *
 * Provides a single [StateFlow] of [AppSettings] and section-specific update
 * methods. Designed to be shared across all screens so that settings changes
 * are immediately reflected everywhere.
 *
 * Settings are persisted as a single JSON string in SharedPreferences.
 * A one-time migration converts legacy individual-key storage on first access.
 */
class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

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
        prefs
            .edit()
            .putString(KEY_SETTINGS, json.encodeToString(AppSettings.serializer(), s))
            .apply()
    }

    private fun loadSettings(): AppSettings {
        val raw = prefs.getString(KEY_SETTINGS, null)
        if (raw != null) {
            return try {
                json.decodeFromString(AppSettings.serializer(), raw)
            } catch (_: Exception) {
                AppSettings()
            }
        }

        if (!prefs.contains(KEY_LEGACY_SOUND_ENABLED)) return AppSettings()
        return migrateLegacySettings()
    }

    /**
     * One-time migration: reads old individual-key settings, constructs
     * AppSettings, saves as JSON, and removes the legacy keys.
     */
    private fun migrateLegacySettings(): AppSettings {
        val settings = LegacySettingsReader.read(prefs)
        saveSettings(settings)
        LegacySettingsReader.removeLegacyKeys(prefs)
        return settings
    }

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_SETTINGS = "settings_json"
        private const val KEY_LEGACY_SOUND_ENABLED = "sound_enabled"
    }
}
