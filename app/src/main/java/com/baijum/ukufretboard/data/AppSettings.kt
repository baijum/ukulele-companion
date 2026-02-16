package com.baijum.ukufretboard.data

/**
 * Settings for sound playback (strum, note duration, etc.).
 *
 * @property enabled Master toggle — when false, no sound is produced.
 * @property noteDurationMs How long each note rings, in milliseconds (300–1200).
 * @property strumDelayMs Time between successive string plucks in a strum (0–150).
 *   0 means all notes play simultaneously; higher values produce a slower strum.
 * @property strumDown When true, strings are strummed top-to-bottom (G→C→E→A).
 *   When false, the strum order is reversed (A→E→C→G).
 * @property playOnTap When true, a single note is played each time a fret is tapped.
 */
data class SoundSettings(
    val enabled: Boolean = true,
    val volume: Float = DEFAULT_VOLUME,
    val noteDurationMs: Int = DEFAULT_NOTE_DURATION_MS,
    val strumDelayMs: Int = DEFAULT_STRUM_DELAY_MS,
    val strumDown: Boolean = true,
    val playOnTap: Boolean = false,
) {
    companion object {
        const val MIN_VOLUME = 0f
        const val MAX_VOLUME = 1f
        const val DEFAULT_VOLUME = 1f

        const val MIN_NOTE_DURATION_MS = 300
        const val MAX_NOTE_DURATION_MS = 1200
        const val DEFAULT_NOTE_DURATION_MS = 600

        const val MIN_STRUM_DELAY_MS = 0
        const val MAX_STRUM_DELAY_MS = 150
        const val DEFAULT_STRUM_DELAY_MS = 50
    }
}

/**
 * Theme mode for the app.
 */
enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System"),
    HIGH_CONTRAST("High Contrast"),
}

/**
 * Settings for display preferences (theme).
 *
 * @property themeMode The app color theme: Light, Dark, or follow the System setting.
 */
data class DisplaySettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

/**
 * Available ukulele tunings.
 *
 * @property label Human-readable name for the tuning.
 * @property stringNames Display names for each string (top to bottom).
 * @property pitchClasses Pitch class (0–11) of each open string.
 * @property octaves Octave number of each open string.
 */
enum class UkuleleTuning(
    val label: String,
    val stringNames: List<String>,
    val pitchClasses: List<Int>,
    val octaves: List<Int>,
) {
    HIGH_G("High-G (Standard)", listOf("G","C","E","A"), listOf(7,0,4,9), listOf(4,4,4,4)),
    LOW_G("Low-G", listOf("g","C","E","A"), listOf(7,0,4,9), listOf(3,4,4,4)),
    BARITONE("Baritone (DGBE)", listOf("D","G","B","E"), listOf(2,7,11,4), listOf(3,3,4,4)),
    D_TUNING("D-Tuning (ADF#B)", listOf("A","D","F#","B"), listOf(9,2,6,11), listOf(4,4,4,4)),
    SLACK_KEY("Slack Key (GCEG)", listOf("G","C","E","G"), listOf(7,0,4,7), listOf(4,4,4,4)),
    OPEN_A("Open A (AC#EA)", listOf("A","C#","E","A"), listOf(9,1,4,9), listOf(4,4,4,4)),
    LOW_A("Low A (GCEa)", listOf("G","C","E","a"), listOf(7,0,4,9), listOf(4,4,4,3)),
    HALF_STEP_DOWN("Half-Step Down", listOf("F#","B","D#","G#"), listOf(6,11,3,8), listOf(4,3,4,4));

    /**
     * Whether this tuning is re-entrant (string pitches are not monotonically ascending).
     *
     * In re-entrant tuning the first string is higher than the second, which limits
     * the variety of chord inversions because the second string (C4 in High-G)
     * is almost always the lowest-pitched note.
     */
    val isReentrant: Boolean
        get() {
            for (i in 0 until octaves.size - 1) {
                val pitchA = octaves[i] * 12 + pitchClasses[i]
                val pitchB = octaves[i + 1] * 12 + pitchClasses[i + 1]
                if (pitchA > pitchB) return true
            }
            return false
        }
}

/**
 * Settings for ukulele tuning.
 *
 * @property tuning The selected tuning variant.
 */
data class TuningSettings(
    val tuning: UkuleleTuning = UkuleleTuning.HIGH_G,
)

/**
 * Settings for fretboard display preferences.
 *
 * @property leftHanded When true, the fretboard is mirrored horizontally
 *   so the nut appears on the right (matching a left-handed player's view).
 * @property lastFret The highest fret shown on the fretboard (12–22, default 12).
 * @property showNoteNames When true, note names are displayed on fretboard cells.
 * @property allowMutedStrings When true, the chord library may include voicings
 *   where one string is muted (not played). Disabled by default since standard
 *   ukulele playing uses all 4 strings for most chords.
 */
data class FretboardSettings(
    val leftHanded: Boolean = false,
    val lastFret: Int = DEFAULT_LAST_FRET,
    val showNoteNames: Boolean = true,
    val allowMutedStrings: Boolean = false,
) {
    companion object {
        const val MIN_LAST_FRET = 12
        const val MAX_LAST_FRET = 22
        const val DEFAULT_LAST_FRET = 12
    }
}

/**
 * Settings for daily chord notification.
 *
 * @property chordOfDayEnabled Whether to show a daily chord notification.
 */
data class NotificationSettings(
    val chordOfDayEnabled: Boolean = false,
)

/**
 * Persisted preferences for the Scale Practice screen.
 *
 * @property lastRoot Last selected root pitch class (0–11).
 * @property lastScaleName Last selected scale name (matches [Scale.name]).
 * @property lastCategory Last selected category filter name (matches [ScaleCategory.name]), or empty for "All".
 * @property lastBpm Last BPM setting for Play Along mode (40–200).
 * @property lastMode Last selected practice mode index (0 = Play Along, 1 = Quiz, 2 = Ear Training).
 * @property showFretboard Whether to show the compact fretboard in Play Along mode.
 */
data class ScalePracticeSettings(
    val lastRoot: Int = 0,
    val lastScaleName: String = "Major",
    val lastCategory: String = "",
    val lastBpm: Int = DEFAULT_BPM,
    val lastMode: Int = 0,
    val showFretboard: Boolean = false,
) {
    companion object {
        const val MIN_BPM = 40
        const val MAX_BPM = 200
        const val DEFAULT_BPM = 80
    }
}

/**
 * Settings for the tuner feature.
 *
 * @property spokenFeedback When true, the tuner uses text-to-speech to announce
 *   the detected note and tuning status (e.g. "A, 5 cents sharp"). Designed
 *   for blind and visually impaired musicians.
 * @property precisionMode When true, the in-tune zone is tightened from ±6 cents
 *   to ±2 cents for advanced players who need higher accuracy.
 * @property a4Reference The reference frequency for A4 in Hz (default 440.0).
 *   Adjustable for playing with instruments tuned to a different concert pitch.
 * @property autoAdvance When true, the tuner automatically advances to the next
 *   untuned string once the current string is marked as tuned.
 * @property autoStart When true, the tuner begins listening automatically when
 *   the tuner tab is opened, removing the need to tap "Start Tuning".
 */
data class TunerSettings(
    val spokenFeedback: Boolean = false,
    val precisionMode: Boolean = false,
    val a4Reference: Float = DEFAULT_A4_REFERENCE,
    val autoAdvance: Boolean = false,
    val autoStart: Boolean = false,
) {
    companion object {
        const val DEFAULT_A4_REFERENCE = 440.0f
        const val MIN_A4_REFERENCE = 415.0f
        const val MAX_A4_REFERENCE = 465.0f

        /** Standard (beginner-friendly) in-tune threshold in cents. */
        const val STANDARD_IN_TUNE_CENTS = 6.0
        /** Precision in-tune threshold in cents. */
        const val PRECISION_IN_TUNE_CENTS = 2.0
    }
}

/**
 * Top-level container for all app settings, organized by section.
 *
 * Each section is a nested data class with its own defaults.
 * New sections are added here as the app grows.
 */
data class AppSettings(
    val sound: SoundSettings = SoundSettings(),
    val display: DisplaySettings = DisplaySettings(),
    val tuning: TuningSettings = TuningSettings(),
    val fretboard: FretboardSettings = FretboardSettings(),
    val notification: NotificationSettings = NotificationSettings(),
    val scalePractice: ScalePracticeSettings = ScalePracticeSettings(),
    val tuner: TunerSettings = TunerSettings(),
)
