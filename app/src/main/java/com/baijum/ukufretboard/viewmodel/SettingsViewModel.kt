package com.baijum.ukufretboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.baijum.ukufretboard.data.AppSettings
import com.baijum.ukufretboard.data.DisplaySettings
import com.baijum.ukufretboard.data.FretboardSettings
import com.baijum.ukufretboard.data.PitchMonitorSettings
import com.baijum.ukufretboard.data.ScalePracticeSettings
import com.baijum.ukufretboard.data.SettingsRepository
import com.baijum.ukufretboard.data.SoundSettings
import com.baijum.ukufretboard.data.TunerSettings
import com.baijum.ukufretboard.data.TuningSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

/**
 * ViewModel that manages all application settings.
 *
 * Provides a single [StateFlow] of [AppSettings] and section-specific update
 * methods. Designed to be shared across all screens so that settings changes
 * are immediately reflected everywhere.
 *
 * Persistence — including the backup/quarantine protection and the one-time
 * migration from legacy individual-key storage — lives in [SettingsRepository].
 */
class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    private val _settings = MutableStateFlow(repository.load())

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
        saveSettings(
            _settings.updateAndGet { current ->
                current.copy(sound = transform(current.sound))
            },
        )
    }

    /**
     * Updates the display settings by applying a transformation function.
     */
    fun updateDisplay(transform: (DisplaySettings) -> DisplaySettings) {
        saveSettings(
            _settings.updateAndGet { current ->
                current.copy(display = transform(current.display))
            },
        )
    }

    /**
     * Updates the tuning settings by applying a transformation function.
     */
    fun updateTuning(transform: (TuningSettings) -> TuningSettings) {
        saveSettings(
            _settings.updateAndGet { current ->
                current.copy(tuning = transform(current.tuning))
            },
        )
    }

    /**
     * Updates the fretboard settings by applying a transformation function.
     */
    fun updateFretboard(transform: (FretboardSettings) -> FretboardSettings) {
        saveSettings(
            _settings.updateAndGet { current ->
                current.copy(fretboard = transform(current.fretboard))
            },
        )
    }

    /**
     * Updates the scale practice settings by applying a transformation function.
     */
    fun updateScalePractice(transform: (ScalePracticeSettings) -> ScalePracticeSettings) {
        saveSettings(
            _settings.updateAndGet { current ->
                current.copy(scalePractice = transform(current.scalePractice))
            },
        )
    }

    /**
     * Updates the tuner settings by applying a transformation function.
     */
    fun updateTuner(transform: (TunerSettings) -> TunerSettings) {
        saveSettings(
            _settings.updateAndGet { current ->
                current.copy(tuner = transform(current.tuner))
            },
        )
    }

    /**
     * Updates the pitch monitor settings by applying a transformation function.
     */
    fun updatePitchMonitor(transform: (PitchMonitorSettings) -> PitchMonitorSettings) {
        saveSettings(
            _settings.updateAndGet { current ->
                current.copy(pitchMonitor = transform(current.pitchMonitor))
            },
        )
    }

    /**
     * Marks onboarding as completed so the wizard is not shown again.
     */
    fun completeOnboarding() {
        saveSettings(_settings.updateAndGet { it.copy(onboardingCompleted = true) })
    }

    /**
     * Dismisses the Explorer tips card so it is not shown again.
     */
    fun dismissExplorerTips() {
        saveSettings(_settings.updateAndGet { it.copy(explorerTipsDismissed = true) })
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

    /**
     * Persists [s] via the repository.
     *
     * Always called on the value [MutableStateFlow.updateAndGet] actually
     * published, never from inside the update lambda: that lambda is a
     * compare-and-set body and re-runs on contention, so a save placed there
     * would write candidate states that lost the race. Because
     * [SettingsRepository.save] rotates the outgoing payload into the backup,
     * one such stray write costs both the primary and the last good copy.
     */
    private fun saveSettings(s: AppSettings) = repository.save(s)
}
