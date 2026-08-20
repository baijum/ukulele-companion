package com.baijum.ukufretboard.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.baijum.ukufretboard.data.AppSettings
import com.baijum.ukufretboard.data.ChordColorOption
import com.baijum.ukufretboard.data.SoundSettings
import com.baijum.ukufretboard.data.ThemeMode
import com.baijum.ukufretboard.data.TuningSettings
import com.baijum.ukufretboard.data.UkuleleTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [SettingsViewModel].
 *
 * `loadSettings()` runs in a property initializer, so every test seeds the
 * preferences *before* constructing the ViewModel, and reads back through a
 * second instance when it needs to prove something was persisted rather than
 * merely held in the StateFlow.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {
    private lateinit var app: Application
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun newViewModel() = SettingsViewModel(app)

    private fun storedJson(): String? = prefs.getString(KEY_SETTINGS, null)

    private fun backupJson(): String? = prefs.getString(KEY_BACKUP, null)

    private fun writeRaw(
        key: String,
        value: String,
    ) {
        prefs.edit().putString(key, value).commit()
    }

    // ── First run ────────────────────────────────────────────────────

    @Test
    fun defaultsAreUsedOnAFirstRunWithNoStoredData() {
        assertEquals(AppSettings(), newViewModel().settings.value)
    }

    @Test
    fun afirstRunDoesNotWriteAnythingUntilSomethingChanges() {
        newViewModel()
        assertNull("constructing the ViewModel should not persist defaults", storedJson())
    }

    // ── Section updates ──────────────────────────────────────────────

    @Test
    fun updateSoundPersistsAndLeavesOtherSectionsUntouched() {
        val vm = newViewModel()
        vm.updateSound { it.copy(enabled = false, volume = 0.3f) }

        assertFalse(vm.settings.value.sound.enabled)
        assertEquals(0.3f, vm.settings.value.sound.volume, 1e-6f)
        assertEquals("other sections must not move", AppSettings().display, vm.settings.value.display)
        assertEquals(AppSettings().tuner, vm.settings.value.tuner)
    }

    @Test
    fun everySectionUpdaterPersistsAcrossViewModelInstances() {
        val vm = newViewModel()
        vm.updateSound { it.copy(noteDurationMs = 900) }
        vm.updateDisplay { it.copy(themeMode = ThemeMode.HIGH_CONTRAST, chordColor = ChordColorOption.BLUE) }
        vm.updateTuning { it.copy(tuning = UkuleleTuning.BARITONE) }
        vm.updateFretboard { it.copy(leftHanded = true, lastFret = 18) }
        vm.updateScalePractice { it.copy(lastBpm = 140, lastScaleName = "Dorian") }
        vm.updateTuner { it.copy(spokenFeedback = true, a4Reference = 432f) }
        vm.updatePitchMonitor { it.copy(placeholder = true) }

        val reloaded = newViewModel().settings.value
        assertEquals(900, reloaded.sound.noteDurationMs)
        assertEquals(ThemeMode.HIGH_CONTRAST, reloaded.display.themeMode)
        assertEquals(ChordColorOption.BLUE, reloaded.display.chordColor)
        assertEquals(UkuleleTuning.BARITONE, reloaded.tuning.tuning)
        assertTrue(reloaded.fretboard.leftHanded)
        assertEquals(18, reloaded.fretboard.lastFret)
        assertEquals(140, reloaded.scalePractice.lastBpm)
        assertEquals("Dorian", reloaded.scalePractice.lastScaleName)
        assertTrue(reloaded.tuner.spokenFeedback)
        assertEquals(432f, reloaded.tuner.a4Reference, 1e-6f)
        assertTrue(reloaded.pitchMonitor.placeholder)
    }

    @Test
    fun theUpdaterReceivesTheCurrentSectionNotTheDefaultOne() {
        val vm = newViewModel()
        vm.updateSound { it.copy(volume = 0.5f) }
        vm.updateSound { it.copy(noteDurationMs = 800) }

        assertEquals("the second update must build on the first", 0.5f, vm.settings.value.sound.volume, 1e-6f)
        assertEquals(800, vm.settings.value.sound.noteDurationMs)
    }

    // ── One-shot flags ───────────────────────────────────────────────

    @Test
    fun completeOnboardingPersistsAndIsIdempotent() {
        val vm = newViewModel()
        vm.completeOnboarding()
        vm.completeOnboarding()

        assertTrue(vm.settings.value.onboardingCompleted)
        assertTrue(newViewModel().settings.value.onboardingCompleted)
    }

    @Test
    fun dismissExplorerTipsPersists() {
        newViewModel().dismissExplorerTips()
        assertTrue(newViewModel().settings.value.explorerTipsDismissed)
    }

    // ── Restore and export ───────────────────────────────────────────

    @Test
    fun replaceAllOverwritesEverySectionAndPersists() {
        val vm = newViewModel()
        vm.updateSound { it.copy(volume = 0.1f) }

        val restored =
            AppSettings(
                sound = SoundSettings(enabled = false, volume = 0.9f),
                tuning = TuningSettings(UkuleleTuning.LOW_G),
                onboardingCompleted = true,
            )
        vm.replaceAll(restored)

        assertEquals(restored, vm.settings.value)
        assertEquals("replaceAll must persist, not just publish", restored, newViewModel().settings.value)
    }

    @Test
    fun exportSettingsReflectsTheLatestUpdate() {
        val vm = newViewModel()
        vm.updateFretboard { it.copy(lastFret = 20) }
        assertEquals(20, vm.exportSettings().fretboard.lastFret)
        assertEquals(vm.settings.value, vm.exportSettings())
    }

    // ── Stored payload ───────────────────────────────────────────────

    @Test
    fun prefsFileAndKeyNameAreTheOnDiskContract() {
        newViewModel().updateSound { it.copy(enabled = false) }
        assertNotNull("settings must persist under settings_json in app_settings", storedJson())
    }

    @Test
    fun storedJsonUsesEnumNamesNotLabels() {
        val vm = newViewModel()
        vm.updateDisplay { it.copy(themeMode = ThemeMode.HIGH_CONTRAST) }
        vm.updateTuning { it.copy(tuning = UkuleleTuning.BARITONE) }

        val raw = storedJson().orEmpty()
        assertTrue("expected the enum name in $raw", raw.contains("HIGH_CONTRAST"))
        assertTrue("expected the enum name in $raw", raw.contains("BARITONE"))
        assertFalse("labels must not reach the disk", raw.contains("High Contrast"))
    }

    @Test
    fun unknownKeysInStoredJsonAreIgnored() {
        // Forward compatibility with a newer version, and with an iOS-authored backup.
        writeRaw(KEY_SETTINGS, """{"sound":{"enabled":false},"aFieldFromTheFuture":42}""")
        assertFalse(
            newViewModel()
                .settings.value.sound.enabled,
        )
    }

    @Test
    fun partialStoredJsonFillsMissingSectionsWithDefaults() {
        writeRaw(KEY_SETTINGS, """{"fretboard":{"lastFret":22}}""")
        val settings = newViewModel().settings.value

        assertEquals(22, settings.fretboard.lastFret)
        assertEquals(AppSettings().sound, settings.sound)
        assertEquals(AppSettings().display, settings.display)
    }

    // ── Corruption recovery (#555) ───────────────────────────────────

    @Test
    fun theFirstSaveSeedsTheBackupWithTheSamePayload() {
        newViewModel().updateDisplay { it.copy(themeMode = ThemeMode.DARK) }

        assertEquals(
            "with no earlier payload to rotate, the backup mirrors the primary",
            storedJson(),
            backupJson(),
        )
    }

    @Test
    fun aLaterSaveRotatesThePreviousPayloadIntoTheBackup() {
        val vm = newViewModel()
        vm.updateDisplay { it.copy(themeMode = ThemeMode.DARK) }
        val afterFirstSave = storedJson()
        vm.updateFretboard { it.copy(lastFret = 20) }

        assertEquals("the backup holds the payload the save replaced", afterFirstSave, backupJson())
        assertNotEquals(storedJson(), backupJson())
    }

    @Test
    fun aCorruptPrimaryRecoversTheLastGoodCopyFromTheBackup() {
        val vm = newViewModel()
        vm.updateDisplay { it.copy(themeMode = ThemeMode.DARK) }
        vm.updateTuning { it.copy(tuning = UkuleleTuning.BARITONE) }
        writeRaw(KEY_SETTINGS, "}} not json {{")

        val recovered = newViewModel().settings.value
        assertEquals("the user's theme must survive a bad write", ThemeMode.DARK, recovered.display.themeMode)
        // The backup lags one save, exactly as JsonListRepository's does, so the
        // most recent change is the only thing lost.
        assertEquals(UkuleleTuning.HIGH_G, recovered.tuning.tuning)
        assertNull("recovering must not quarantine anything", prefs.getString(KEY_QUARANTINE, null))
    }

    @Test
    fun accessibilitySettingsSurviveACorruptPrimary() {
        // The reason this matters more than the equivalent list-repository case:
        // a blind or visually impaired user silently losing spoken feedback and
        // precision mode has no way to tell why the app changed under them.
        val vm = newViewModel()
        vm.updateTuner { it.copy(spokenFeedback = true, precisionMode = true) }
        vm.updateSound { it.copy(enabled = false) }
        writeRaw(KEY_SETTINGS, """{"tuner":{"spokenFeedback":tr""")

        val recovered = newViewModel().settings.value
        assertTrue(recovered.tuner.spokenFeedback)
        assertTrue(recovered.tuner.precisionMode)
    }

    @Test
    fun aCorruptPrimaryIsNotPromotedIntoTheBackupByTheNextSave() {
        val vm = newViewModel()
        vm.updateDisplay { it.copy(themeMode = ThemeMode.DARK) }
        val lastGood = storedJson()
        writeRaw(KEY_SETTINGS, "}} not json {{")

        newViewModel().updateFretboard { it.copy(lastFret = 20) }

        assertEquals("the backup must not advance to an unparseable payload", lastGood, backupJson())
    }

    @Test
    fun aCorruptPrimaryWithNoBackupFallsBackToDefaults() {
        writeRaw(KEY_SETTINGS, "}} not json {{")
        assertEquals(AppSettings(), newViewModel().settings.value)
    }

    @Test
    fun aCorruptPrimaryAndBackupQuarantineTheRawPayload() {
        writeRaw(KEY_SETTINGS, "}} not json {{")
        writeRaw(KEY_BACKUP, "also not json")

        assertEquals(AppSettings(), newViewModel().settings.value)
        assertEquals(
            "the unreadable bytes are kept rather than dropped",
            "}} not json {{",
            prefs.getString(KEY_QUARANTINE, null),
        )
    }

    @Test
    fun aQuarantinedPayloadOutlivesTheSaveThatOverwritesThePrimary() {
        writeRaw(KEY_SETTINGS, "}} not json {{")
        writeRaw(KEY_BACKUP, "also not json")

        newViewModel().updateSound { it.copy(enabled = false) }

        assertFalse(
            newViewModel()
                .settings.value.sound.enabled,
        )
        assertEquals("}} not json {{", prefs.getString(KEY_QUARANTINE, null))
    }

    @Test
    fun theBackupSelfHealsOnTheSaveAfterBothCopiesWereCorrupt() {
        writeRaw(KEY_SETTINGS, "}} not json {{")
        writeRaw(KEY_BACKUP, "also not json")

        newViewModel().updateSound { it.copy(enabled = false) }
        val firstValidPayload = storedJson()
        assertNotEquals(
            "the save after a double corruption cannot promote the unparseable primary",
            firstValidPayload,
            backupJson(),
        )

        newViewModel().updateFretboard { it.copy(lastFret = 20) }

        assertEquals(
            "the next save rotates the recovered primary in, so the backup stops holding garbage",
            firstValidPayload,
            backupJson(),
        )
    }

    // ── Legacy migration ─────────────────────────────────────────────

    @Test
    fun legacyMigrationRunsWhenTheSoundEnabledKeyIsPresent() {
        prefs
            .edit()
            .putBoolean("sound_enabled", false)
            .putString("theme_mode", "DARK")
            .putString("tuning", "BARITONE")
            .commit()

        val settings = newViewModel().settings.value
        assertFalse(settings.sound.enabled)
        assertEquals(ThemeMode.DARK, settings.display.themeMode)
        assertEquals(UkuleleTuning.BARITONE, settings.tuning.tuning)
    }

    @Test
    fun legacyMigrationWritesSettingsJsonAndRemovesTheLegacyKeys() {
        prefs
            .edit()
            .putBoolean("sound_enabled", false)
            .putString("theme_mode", "DARK")
            .commit()
        newViewModel()

        assertNotNull("the migration should write the JSON payload", storedJson())
        assertNull("legacy keys should be consumed", prefs.getString("theme_mode", null))
        assertFalse("legacy keys should be consumed", prefs.contains("sound_enabled"))
    }

    @Test
    fun legacyMigrationDoesNotRunASecondTime() {
        prefs.edit().putBoolean("sound_enabled", false).commit()
        newViewModel()

        val afterMigration = storedJson()
        val second = newViewModel()
        assertEquals("the JSON payload should be reused as-is", afterMigration, storedJson())
        assertFalse(second.settings.value.sound.enabled)
    }

    @Test
    fun legacyMigrationMarksOnboardingAsAlreadyCompleted() {
        prefs.edit().putBoolean("sound_enabled", true).commit()
        val settings = newViewModel().settings.value

        assertTrue("an upgrading user must not be shown onboarding", settings.onboardingCompleted)
        assertTrue(settings.explorerTipsDismissed)
    }

    @Test
    fun legacyMigrationIsSkippedWhenSoundEnabledIsAbsentEvenThoughOtherLegacyKeysExist() {
        // Pinned weakness: migration is gated on one unrelated key. A legacy user
        // who customised only their theme, tuning or fretboard — and never toggled
        // sound — silently loses all of it on upgrade.
        prefs
            .edit()
            .putString("theme_mode", "DARK")
            .putString("tuning", "BARITONE")
            .putBoolean("left_handed", true)
            .commit()

        val settings = newViewModel().settings.value
        assertEquals(
            "today a theme-only legacy install is not migrated",
            ThemeMode.SYSTEM,
            settings.display.themeMode,
        )
        assertEquals(UkuleleTuning.HIGH_G, settings.tuning.tuning)
        assertFalse(settings.fretboard.leftHanded)
        assertTrue("and the orphaned keys are left behind", prefs.contains("theme_mode"))
    }

    @Test
    fun storedJsonWinsOverLegacyKeys() {
        prefs.edit().putBoolean("sound_enabled", false).commit()
        writeRaw(KEY_SETTINGS, """{"sound":{"enabled":true}}""")

        assertTrue(
            "the JSON payload is authoritative",
            newViewModel()
                .settings.value.sound.enabled,
        )
    }

    private companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_SETTINGS = "settings_json"
        const val KEY_BACKUP = "settings_json_backup"
        const val KEY_QUARANTINE = "settings_json_quarantine"
    }
}
