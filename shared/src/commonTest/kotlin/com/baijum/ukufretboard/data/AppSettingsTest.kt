package com.baijum.ukufretboard.data

import io.kotest.property.Arb
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [AppSettings] and its sections.
 *
 * Two things here are contracts rather than implementation details:
 *
 * 1. [UkuleleTuning]'s pitch classes and octaves drive `TunerNoteMapper`, the
 *    fretboard display and pattern playback. A wrong octave makes the tuner aim
 *    at the wrong frequency for that string.
 * 2. Every enum constant name is persisted verbatim in `settings_json`, so a
 *    rename silently resets that preference on every existing install.
 */
class AppSettingsTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    /** The app's own convention, from `TunerNoteMapper.targetFrequency`. */
    private fun midiOf(
        pitchClass: Int,
        octave: Int,
    ) = (octave + 1) * 12 + pitchClass

    private fun midiNotes(tuning: UkuleleTuning) =
        tuning.pitchClasses.indices.map { midiOf(tuning.pitchClasses[it], tuning.octaves[it]) }

    // ── Noise gate mapping ───────────────────────────────────────────

    @Test
    fun filteringToRmsMapsZeroToTheFloorAndOneToTheCeiling() {
        assertEquals(0.002f, SoundSettings.filteringToRms(0f), 1e-6f, "floor moved")
        assertEquals(0.053f, SoundSettings.filteringToRms(1f), 1e-6f, "ceiling moved")
    }

    @Test
    fun filteringToRmsClampsInputsOutsideZeroToOne() {
        assertEquals(0.002f, SoundSettings.filteringToRms(-5f), 1e-6f)
        assertEquals(0.053f, SoundSettings.filteringToRms(42f), 1e-6f)
        assertEquals(0.002f, SoundSettings.filteringToRms(Float.NEGATIVE_INFINITY), 1e-6f)
        assertEquals(0.053f, SoundSettings.filteringToRms(Float.POSITIVE_INFINITY), 1e-6f)
    }

    @Test
    fun filteringToRmsIsMonotonicAcrossTheSliderTravel() {
        var previous = SoundSettings.filteringToRms(0f)
        for (step in 1..100) {
            val current = SoundSettings.filteringToRms(step / 100f)
            assertTrue(current >= previous, "gate dipped at step $step: $current < $previous")
            previous = current
        }
    }

    @Test
    fun filteringToRmsStaysInsideTheGateBoundsForAnyFiniteInput() {
        runBlocking {
            checkAll(Arb.numericFloat(-1e6f, 1e6f)) { filtering ->
                val rms = SoundSettings.filteringToRms(filtering)
                assertTrue(rms in 0.002f..0.053f, "gate escaped its bounds: $filtering -> $rms")
            }
        }
    }

    @Test
    fun filteringToRmsPropagatesNaNStraightThroughTheClamp() {
        // Float.coerceIn returns NaN for NaN because both comparisons are false, so a
        // NaN slider value produces a NaN gate and every RMS comparison downstream
        // becomes false — the gate never opens and the tuner appears deaf. Pinned
        // rather than fixed: no code path currently writes NaN into the setting.
        assertTrue(SoundSettings.filteringToRms(Float.NaN).isNaN(), "NaN handling changed")
    }

    @Test
    fun defaultNoiseGateFilteringMapsInsideTheGateBounds() {
        val rms = SoundSettings.filteringToRms(SoundSettings.DEFAULT_NOISE_GATE_FILTERING)
        assertEquals(0.04025f, rms, 1e-6f, "the shipped default gate moved")
    }

    @Test
    fun noiseGateBoundsSpanTheFullSliderTravel() {
        assertEquals(0f, SoundSettings.MIN_NOISE_GATE_FILTERING)
        assertEquals(1f, SoundSettings.MAX_NOISE_GATE_FILTERING)
        assertTrue(
            SoundSettings.DEFAULT_NOISE_GATE_FILTERING in
                SoundSettings.MIN_NOISE_GATE_FILTERING..SoundSettings.MAX_NOISE_GATE_FILTERING,
            "the default sits outside its own bounds",
        )
    }

    // ── Tunings ──────────────────────────────────────────────────────

    @Test
    fun everyTuningHasFourStringsWithMatchingNameAndOctaveArity() {
        for (tuning in UkuleleTuning.entries) {
            assertEquals(4, tuning.pitchClasses.size, "${tuning.name} does not have 4 pitch classes")
            assertEquals(4, tuning.stringNames.size, "${tuning.name} does not have 4 string names")
            assertEquals(4, tuning.octaves.size, "${tuning.name} does not have 4 octaves")
        }
    }

    @Test
    fun everyTuningPitchClassIsInPitchClassRange() {
        for (tuning in UkuleleTuning.entries) {
            for (pitchClass in tuning.pitchClasses) {
                assertTrue(pitchClass in 0..11, "${tuning.name} has pitch class $pitchClass")
            }
        }
    }

    @Test
    fun everyTuningOctaveIsInAPlayableRange() {
        for (tuning in UkuleleTuning.entries) {
            for (octave in tuning.octaves) {
                assertTrue(octave in 2..5, "${tuning.name} has implausible octave $octave")
            }
        }
    }

    @Test
    fun everyTuningStringNameMatchesItsPitchClass() {
        // Accept either enharmonic spelling: HALF_STEP_DOWN names a string "D#"
        // while the standard spelling for that pitch class is "Eb". The string
        // names are display text, so only the pitch they denote has to agree.
        for (tuning in UkuleleTuning.entries) {
            for (i in 0 until 4) {
                val pitchClass = tuning.pitchClasses[i]
                val accepted =
                    setOf(
                        Notes.NOTE_NAMES_SHARP[pitchClass],
                        Notes.NOTE_NAMES_FLAT[pitchClass],
                        Notes.NOTE_NAMES_STANDARD[pitchClass],
                    ).map { it.uppercase() }
                val actual = tuning.stringNames[i].uppercase()
                assertTrue(
                    actual in accepted,
                    "${tuning.name} string $i is named $actual but sounds one of $accepted",
                )
            }
        }
    }

    @Test
    fun highGIsReentrantAndLowGIsNot() {
        assertTrue(UkuleleTuning.HIGH_G.isReentrant, "standard high-G tuning is re-entrant")
        assertFalse(UkuleleTuning.LOW_G.isReentrant, "low-G tuning ascends linearly")
    }

    @Test
    fun lowGDiffersFromHighGOnlyInTheOctaveOfTheGString() {
        assertEquals(UkuleleTuning.HIGH_G.pitchClasses, UkuleleTuning.LOW_G.pitchClasses)
        assertEquals(listOf(4, 4, 4, 4), UkuleleTuning.HIGH_G.octaves)
        assertEquals(listOf(3, 4, 4, 4), UkuleleTuning.LOW_G.octaves)
    }

    @Test
    fun halfStepDownIsExactlyOneSemitoneBelowHighGOnEveryString() {
        val standard = midiNotes(UkuleleTuning.HIGH_G)
        val lowered = midiNotes(UkuleleTuning.HALF_STEP_DOWN)
        for (i in standard.indices) {
            assertEquals(
                standard[i] - 1,
                lowered[i],
                "half-step-down string $i is not one semitone below high-G",
            )
        }
    }

    @Test
    fun lowAOnlyDropsTheAStringAnOctave() {
        val standard = midiNotes(UkuleleTuning.HIGH_G)
        val lowA = midiNotes(UkuleleTuning.LOW_A)
        assertEquals(standard.dropLast(1), lowA.dropLast(1), "only the A string should move")
        assertEquals(standard.last() - 12, lowA.last(), "the A string should drop a full octave")
    }

    @Test
    fun tuningEnumNamesAreTheOnDiskContract() {
        assertEquals(
            listOf(
                "HIGH_G",
                "LOW_G",
                "BARITONE",
                "D_TUNING",
                "SLACK_KEY",
                "OPEN_A",
                "LOW_A",
                "HALF_STEP_DOWN",
            ),
            UkuleleTuning.entries.map { it.name },
            "settings_json stores the tuning by name; renaming one resets it on upgrade",
        )
    }

    @Test
    fun tuningLabelsAreUniqueAndNonBlank() {
        val labels = UkuleleTuning.entries.map { it.label }
        assertEquals(labels.size, labels.toSet().size, "duplicate tuning labels: $labels")
        for (label in labels) {
            assertTrue(label.isNotBlank(), "a tuning has a blank label")
        }
    }

    @Test
    @Ignore // Enabled by "Fix: Correct the baritone B string octave".
    fun everyTuningStringMatchesItsScientificPitch() {
        // Expected MIDI numbers, spelled out from each tuning's documented note names.
        val expected =
            mapOf(
                UkuleleTuning.HIGH_G to listOf(67, 60, 64, 69), // G4 C4 E4 A4
                UkuleleTuning.LOW_G to listOf(55, 60, 64, 69), // G3 C4 E4 A4
                UkuleleTuning.BARITONE to listOf(50, 55, 59, 64), // D3 G3 B3 E4
                UkuleleTuning.D_TUNING to listOf(69, 62, 66, 71), // A4 D4 F#4 B4
                UkuleleTuning.SLACK_KEY to listOf(67, 60, 64, 67), // G4 C4 E4 G4
                UkuleleTuning.OPEN_A to listOf(69, 61, 64, 69), // A4 C#4 E4 A4
                UkuleleTuning.LOW_A to listOf(67, 60, 64, 57), // G4 C4 E4 A3
                UkuleleTuning.HALF_STEP_DOWN to listOf(66, 59, 63, 68), // F#4 B3 D#4 G#4
            )
        for (tuning in UkuleleTuning.entries) {
            assertEquals(expected.getValue(tuning), midiNotes(tuning), "${tuning.name} is mistuned")
        }
    }

    @Test
    @Ignore // Enabled by "Fix: Correct the baritone B string octave".
    fun baritoneIsALinearTuningNotAReentrantOne() {
        // Baritone DGBE ascends D3 G3 B3 E4 like a guitar's top four strings.
        assertFalse(UkuleleTuning.BARITONE.isReentrant, "baritone tuning ascends linearly")
    }

    // ── Serialization ────────────────────────────────────────────────

    @Test
    fun appSettingsRoundTripsThroughJson() {
        val original =
            AppSettings(
                sound = SoundSettings(enabled = false, volume = 0.4f, noiseGateFiltering = 0.25f),
                display = DisplaySettings(themeMode = ThemeMode.HIGH_CONTRAST),
                tuning = TuningSettings(UkuleleTuning.BARITONE),
                fretboard = FretboardSettings(leftHanded = true, lastFret = 18),
                tuner = TunerSettings(spokenFeedback = true, a4Reference = 432f),
                onboardingCompleted = true,
            )
        val decoded =
            json.decodeFromString(
                AppSettings.serializer(),
                json.encodeToString(AppSettings.serializer(), original),
            )
        assertEquals(original, decoded, "settings round trip lost data")
    }

    @Test
    fun appSettingsDecodesPartialJsonUsingSectionDefaults() {
        // This is what makes settings forward and backward compatible across
        // versions and across an iOS-authored backup.
        val decoded =
            json.decodeFromString(
                AppSettings.serializer(),
                """{"sound":{"enabled":false}}""",
            )
        assertFalse(decoded.sound.enabled, "the stored field should win")
        assertEquals(SoundSettings.DEFAULT_VOLUME, decoded.sound.volume, "volume should default")
        assertEquals(DisplaySettings(), decoded.display, "an absent section should default")
        assertEquals(TunerSettings(), decoded.tuner, "an absent section should default")
    }

    @Test
    fun appSettingsIgnoresUnknownKeysFromANewerVersion() {
        val decoded =
            json.decodeFromString(
                AppSettings.serializer(),
                """{"sound":{"enabled":true,"futureField":7},"somethingNew":true}""",
            )
        assertTrue(decoded.sound.enabled, "a newer payload should still decode")
    }

    @Test
    fun appSettingsJsonUsesEnumNamesNotLabels() {
        val encoded =
            json.encodeToString(
                AppSettings.serializer(),
                AppSettings(
                    display = DisplaySettings(themeMode = ThemeMode.HIGH_CONTRAST),
                    tuning = TuningSettings(UkuleleTuning.BARITONE),
                ),
            )
        assertTrue(encoded.contains("HIGH_CONTRAST"), "theme should persist by name: $encoded")
        assertTrue(encoded.contains("BARITONE"), "tuning should persist by name: $encoded")
        assertFalse(encoded.contains("High Contrast"), "labels must not reach the disk")
    }

    // ── Defaults ─────────────────────────────────────────────────────

    @Test
    fun defaultAppSettingsTreatsOnboardingAsIncomplete() {
        // Deliberately the opposite of LegacySettingsReader's defaults: a fresh
        // install has not seen onboarding, a migrating one has.
        val defaults = AppSettings()
        assertFalse(defaults.onboardingCompleted, "a fresh install must see onboarding")
        assertFalse(defaults.explorerTipsDismissed, "a fresh install must see explorer tips")
    }

    @Test
    fun defaultsSitInsideTheirOwnDeclaredBounds() {
        val defaults = AppSettings()
        assertTrue(defaults.sound.volume in SoundSettings.MIN_VOLUME..SoundSettings.MAX_VOLUME)
        assertTrue(
            defaults.sound.noteDurationMs in
                SoundSettings.MIN_NOTE_DURATION_MS..SoundSettings.MAX_NOTE_DURATION_MS,
        )
        assertTrue(
            defaults.sound.strumDelayMs in
                SoundSettings.MIN_STRUM_DELAY_MS..SoundSettings.MAX_STRUM_DELAY_MS,
        )
        assertTrue(
            defaults.fretboard.lastFret in
                FretboardSettings.MIN_LAST_FRET..FretboardSettings.MAX_LAST_FRET,
        )
        assertTrue(
            defaults.scalePractice.lastBpm in
                ScalePracticeSettings.MIN_BPM..ScalePracticeSettings.MAX_BPM,
        )
        assertTrue(
            defaults.tuner.a4Reference in
                TunerSettings.MIN_A4_REFERENCE..TunerSettings.MAX_A4_REFERENCE,
        )
    }

    @Test
    fun precisionInTuneZoneIsTighterThanTheStandardOne() {
        assertTrue(
            TunerSettings.PRECISION_IN_TUNE_CENTS < TunerSettings.STANDARD_IN_TUNE_CENTS,
            "precision mode must narrow the in-tune zone",
        )
    }
}
