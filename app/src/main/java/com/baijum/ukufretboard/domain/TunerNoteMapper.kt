package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.audio.ToneGenerator
import com.baijum.ukufretboard.data.UkuleleTuning
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * Information about the musical note nearest to a detected frequency.
 *
 * @property noteName      Display name of the note (e.g. "A", "C#", "Bb").
 * @property octave        Octave number (e.g. 4 for A4 = 440 Hz).
 * @property pitchClass    Pitch class 0–11 (C = 0, C# = 1, … B = 11).
 * @property centsDeviation Deviation from the exact note in cents (−50 .. +50).
 *   Negative means flat, positive means sharp.
 * @property frequencyHz   The original detected frequency.
 */
data class NoteInfo(
    val noteName: String,
    val octave: Int,
    val pitchClass: Int,
    val centsDeviation: Double,
    val frequencyHz: Double,
)

/**
 * Describes which string in the current tuning is closest to the detected note.
 *
 * @property stringIndex  0-based index of the matched string (top to bottom).
 * @property stringName   Display name of the string (e.g. "G", "C", "E", "A").
 * @property centsFromTarget Cents deviation from that string's exact target pitch.
 */
data class StringMatch(
    val stringIndex: Int,
    val stringName: String,
    val centsFromTarget: Double,
)

/**
 * Maps detected frequencies to musical notes and ukulele strings.
 *
 * Uses the equal-temperament formula to convert a frequency to a MIDI note
 * number, then derives pitch class, octave, note name, and cents deviation.
 *
 * String matching uses the target frequencies from [UkuleleTuning] — the same
 * data that drives the rest of the app — so it automatically supports all
 * four tunings (High-G, Low-G, Baritone, D-Tuning).
 */
object TunerNoteMapper {

    /**
     * Converts a detected frequency to the nearest musical note.
     *
     * Uses standard note names (C, C#, D, Eb, E, F, F#, G, Ab, A, Bb, B).
     *
     * @param hz Frequency in Hertz (must be > 0).
     * @return A [NoteInfo] describing the nearest note, or `null` if [hz] ≤ 0.
     */
    fun mapFrequency(hz: Double): NoteInfo? {
        if (hz <= 0.0) return null

        // MIDI note number (fractional) relative to A4 = 69.
        val midiExact = 69.0 + 12.0 * log2(hz / 440.0)
        val midiRounded = midiExact.roundToInt()

        // Cents deviation from the nearest semitone.
        val cents = (midiExact - midiRounded) * 100.0

        // Pitch class and octave from the rounded MIDI note.
        val pitchClass = ((midiRounded % 12) + 12) % 12  // ensure positive
        val octave = (midiRounded / 12) - 1

        val noteName = com.baijum.ukufretboard.data.Notes.pitchClassToName(pitchClass)

        return NoteInfo(
            noteName = noteName,
            octave = octave,
            pitchClass = pitchClass,
            centsDeviation = cents,
            frequencyHz = hz,
        )
    }

    /**
     * Finds the ukulele string in [tuning] closest to the given [noteInfo].
     *
     * Compares the detected frequency against each string's exact target
     * frequency (computed from the tuning's pitch classes and octaves) and
     * returns the string with the smallest absolute cents difference.
     *
     * @return A [StringMatch] for the best-matching string.
     */
    fun findNearestString(
        noteInfo: NoteInfo,
        tuning: UkuleleTuning,
    ): StringMatch {
        var bestIndex = 0
        var bestCents = Double.MAX_VALUE

        for (i in tuning.pitchClasses.indices) {
            val targetHz = ToneGenerator.frequencyOf(
                tuning.pitchClasses[i],
                tuning.octaves[i],
            )
            val centsDiff = 1200.0 * log2(noteInfo.frequencyHz / targetHz)

            if (abs(centsDiff) < abs(bestCents)) {
                bestCents = centsDiff
                bestIndex = i
            }
        }

        return StringMatch(
            stringIndex = bestIndex,
            stringName = tuning.stringNames[bestIndex],
            centsFromTarget = bestCents,
        )
    }

    /**
     * Finds the nearest string with optional hysteresis to reduce string-switch
     * thrashing when two strings are very close in distance.
     *
     * If [previousStringIndex] is provided and it remains within
     * [switchHysteresisCents] of the current best match, the previous string is
     * kept. This stabilizes status guidance during borderline frames.
     */
    fun findNearestStringWithHysteresis(
        noteInfo: NoteInfo,
        tuning: UkuleleTuning,
        previousStringIndex: Int?,
        switchHysteresisCents: Double,
    ): StringMatch {
        val centsDiffs = tuning.pitchClasses.indices.map { i ->
            val targetHz = ToneGenerator.frequencyOf(
                tuning.pitchClasses[i],
                tuning.octaves[i],
            )
            1200.0 * log2(noteInfo.frequencyHz / targetHz)
        }

        var bestIndex = 0
        var bestAbs = Double.MAX_VALUE
        centsDiffs.forEachIndexed { idx, cents ->
            val absCents = abs(cents)
            if (absCents < bestAbs) {
                bestAbs = absCents
                bestIndex = idx
            }
        }

        val chosenIndex = if (previousStringIndex != null &&
            previousStringIndex in centsDiffs.indices
        ) {
            val previousAbs = abs(centsDiffs[previousStringIndex])
            if (previousAbs <= bestAbs + switchHysteresisCents) previousStringIndex
            else bestIndex
        } else {
            bestIndex
        }

        return StringMatch(
            stringIndex = chosenIndex,
            stringName = tuning.stringNames[chosenIndex],
            centsFromTarget = centsDiffs[chosenIndex],
        )
    }
}
