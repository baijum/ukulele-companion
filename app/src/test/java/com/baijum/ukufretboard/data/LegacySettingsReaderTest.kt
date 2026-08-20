package com.baijum.ukufretboard.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [LegacySettingsReader], the one-shot migration from the pre-JSON
 * per-key preference layout.
 *
 * Every key name below is deliberately spelled out rather than read from the
 * reader. They are an on-disk contract with installs that predate `settings_json`,
 * and the duplicated list is what makes `removeLegacyKeysRemovesEveryKeyThatReadConsumes`
 * able to catch a key added to `read` but forgotten in the private `LEGACY_KEYS`.
 */
@RunWith(RobolectricTestRunner::class)
class LegacySettingsReaderTest {
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        val app: Application = ApplicationProvider.getApplicationContext()
        prefs = app.getSharedPreferences("legacy_settings_reader_test", Context.MODE_PRIVATE)
    }

    private fun put(build: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(build).commit()
    }

    /** Every key `read` consumes, each set to a non-default value. */
    private fun seedAllLegacyKeys() =
        put {
            putBoolean("sound_enabled", false)
            putFloat("sound_volume", 0.25f)
            putInt("note_duration_ms", 900)
            putInt("strum_delay_ms", 120)
            putBoolean("strum_down", false)
            putBoolean("play_on_tap", true)
            putFloat("pm_noise_gate_filtering", 0.4f)
            putString("theme_mode", "HIGH_CONTRAST")
            putBoolean("show_explorer_tips", false)
            putBoolean("show_learn_section", false)
            putBoolean("show_reference_section", false)
            putString("chord_display_style", "INLINE")
            putString("chord_color", "PURPLE")
            putBoolean("show_chord_diagram_rail", false)
            putString("tuning", "BARITONE")
            putBoolean("left_handed", true)
            putInt("last_fret", 18)
            putBoolean("show_note_names", false)
            putBoolean("allow_muted_strings", true)
            putInt("scale_practice_root", 9)
            putString("scale_practice_scale", "Dorian")
            putString("scale_practice_category", "Modes")
            putInt("scale_practice_bpm", 140)
            putInt("scale_practice_mode", 2)
            putBoolean("scale_practice_fretboard", true)
            putInt("scale_practice_direction", 1)
            putBoolean("scale_practice_loop", true)
            putInt("scale_practice_fret_position", 3)
            putBoolean("tuner_spoken_feedback", true)
            putBoolean("tuner_precision_mode", true)
            putFloat("tuner_a4_reference", 432f)
            putBoolean("tuner_auto_advance", true)
            putBoolean("tuner_auto_start", true)
            putBoolean("onboarding_completed", false)
            putBoolean("explorer_tips_dismissed", false)
        }

    // ── Defaults ─────────────────────────────────────────────────────

    @Test
    fun readOnEmptyPrefsReturnsThePerKeyDefaults() {
        val settings = LegacySettingsReader.read(prefs)

        assertEquals(SoundSettings(), settings.sound)
        assertEquals(FretboardSettings(), settings.fretboard)
        assertEquals(ScalePracticeSettings(), settings.scalePractice)
        assertEquals(TunerSettings(), settings.tuner)
    }

    @Test
    fun aMigratingInstallIsTreatedAsHavingSeenOnboarding() {
        // Deliberately the opposite of AppSettings()'s defaults: someone upgrading
        // from the legacy layout has already been through onboarding and the tips.
        val settings = LegacySettingsReader.read(prefs)

        assertTrue("a legacy install must not be shown onboarding again", settings.onboardingCompleted)
        assertTrue("a legacy install must not be shown explorer tips again", settings.explorerTipsDismissed)
        assertFalse("a fresh install still sees onboarding", AppSettings().onboardingCompleted)
    }

    @Test
    fun pitchMonitorSectionIsAlwaysTheDefault() {
        // The noise gate moved to SoundSettings; nothing legacy feeds this section.
        seedAllLegacyKeys()
        assertEquals(PitchMonitorSettings(), LegacySettingsReader.read(prefs).pitchMonitor)
    }

    // ── Key mapping ──────────────────────────────────────────────────

    @Test
    fun readMapsEverySoundKey() {
        seedAllLegacyKeys()
        val sound = LegacySettingsReader.read(prefs).sound

        assertFalse(sound.enabled)
        assertEquals(0.25f, sound.volume, 1e-6f)
        assertEquals(900, sound.noteDurationMs)
        assertEquals(120, sound.strumDelayMs)
        assertFalse(sound.strumDown)
        assertTrue(sound.playOnTap)
    }

    @Test
    fun noiseGateFilteringIsReadFromThePitchMonitorKey() {
        // The setting moved from the Pitch Monitor section to Sound, but the
        // legacy key name did not move with it.
        put { putFloat("pm_noise_gate_filtering", 0.4f) }
        assertEquals(0.4f, LegacySettingsReader.read(prefs).sound.noiseGateFiltering, 1e-6f)
    }

    @Test
    fun readMapsEveryDisplayKey() {
        seedAllLegacyKeys()
        val display = LegacySettingsReader.read(prefs).display

        assertEquals(ThemeMode.HIGH_CONTRAST, display.themeMode)
        assertFalse(display.showExplorerTips)
        assertFalse(display.showLearnSection)
        assertFalse(display.showReferenceSection)
        assertEquals(ChordDisplayStyle.INLINE, display.chordDisplayStyle)
        assertEquals(ChordColorOption.PURPLE, display.chordColor)
        assertFalse(display.showChordDiagramRail)
    }

    @Test
    fun readMapsTheTuningKey() {
        seedAllLegacyKeys()
        assertEquals(UkuleleTuning.BARITONE, LegacySettingsReader.read(prefs).tuning.tuning)
    }

    @Test
    fun readMapsEveryFretboardKey() {
        seedAllLegacyKeys()
        val fretboard = LegacySettingsReader.read(prefs).fretboard

        assertTrue(fretboard.leftHanded)
        assertEquals(18, fretboard.lastFret)
        assertFalse(fretboard.showNoteNames)
        assertTrue(fretboard.allowMutedStrings)
    }

    @Test
    fun readMapsEveryScalePracticeKey() {
        seedAllLegacyKeys()
        val scale = LegacySettingsReader.read(prefs).scalePractice

        assertEquals(9, scale.lastRoot)
        assertEquals("Dorian", scale.lastScaleName)
        assertEquals("Modes", scale.lastCategory)
        assertEquals(140, scale.lastBpm)
        assertEquals(2, scale.lastMode)
        assertTrue(scale.showFretboard)
        assertEquals(1, scale.lastDirection)
        assertTrue(scale.loopPlayback)
        assertEquals(3, scale.lastFretPosition)
    }

    @Test
    fun readMapsEveryTunerKey() {
        seedAllLegacyKeys()
        val tuner = LegacySettingsReader.read(prefs).tuner

        assertTrue(tuner.spokenFeedback)
        assertTrue(tuner.precisionMode)
        assertEquals(432f, tuner.a4Reference, 1e-6f)
        assertTrue(tuner.autoAdvance)
        assertTrue(tuner.autoStart)
    }

    @Test
    fun readMapsTheOnboardingFlags() {
        seedAllLegacyKeys()
        val settings = LegacySettingsReader.read(prefs)

        assertFalse(settings.onboardingCompleted)
        assertFalse(settings.explorerTipsDismissed)
    }

    // ── Enum parsing ─────────────────────────────────────────────────

    @Test
    fun enumsAreReadByNameNotByLabel() {
        put { putString("theme_mode", ThemeMode.HIGH_CONTRAST.label) }
        assertEquals(
            "the human-readable label must not be accepted as a stored value",
            ThemeMode.SYSTEM,
            LegacySettingsReader.read(prefs).display.themeMode,
        )
    }

    @Test
    fun unknownEnumNamesFallBackToTheSectionDefault() {
        put {
            putString("theme_mode", "NEON")
            putString("chord_display_style", "SIDEWAYS")
            putString("chord_color", "CHARTREUSE")
            putString("tuning", "KLINGON")
        }
        val settings = LegacySettingsReader.read(prefs)

        assertEquals(ThemeMode.SYSTEM, settings.display.themeMode)
        assertEquals(ChordDisplayStyle.ABOVE, settings.display.chordDisplayStyle)
        assertEquals(ChordColorOption.THEME, settings.display.chordColor)
        assertEquals(UkuleleTuning.HIGH_G, settings.tuning.tuning)
    }

    @Test
    fun blankEnumStringFallsBackToTheDefault() {
        put { putString("theme_mode", "") }
        assertEquals(ThemeMode.SYSTEM, LegacySettingsReader.read(prefs).display.themeMode)
    }

    @Test
    fun missingScaleNameFallsBackToMajor() {
        assertEquals("Major", LegacySettingsReader.read(prefs).scalePractice.lastScaleName)
        assertEquals("", LegacySettingsReader.read(prefs).scalePractice.lastCategory)
    }

    // ── Cleanup ──────────────────────────────────────────────────────

    @Test
    fun removeLegacyKeysRemovesEveryKeyThatReadConsumes() {
        // If `read` gains a key that `LEGACY_KEYS` does not list, that key
        // survives the migration and leaks stale data forever. The seed list
        // above is the independent copy that makes this detectable.
        seedAllLegacyKeys()
        assertTrue("the seed did not write anything", prefs.all.isNotEmpty())

        LegacySettingsReader.removeLegacyKeys(prefs)

        assertTrue(
            "these legacy keys survived the migration: ${prefs.all.keys}",
            prefs.all.isEmpty(),
        )
    }

    @Test
    fun removeLegacyKeysLeavesUnrelatedKeysIntact() {
        seedAllLegacyKeys()
        put { putString("settings_json", "{}") }

        LegacySettingsReader.removeLegacyKeys(prefs)

        assertEquals(setOf("settings_json"), prefs.all.keys)
    }

    @Test
    fun removeLegacyKeysOnEmptyPrefsIsANoOp() {
        LegacySettingsReader.removeLegacyKeys(prefs)
        assertTrue(prefs.all.isEmpty())
    }
}
