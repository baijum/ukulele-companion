package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Notes

/**
 * Represents a musical note identified by its pitch class.
 *
 * A pitch class is an integer from 0 to 11 that groups together all pitches
 * of the same name regardless of octave (e.g., C2, C3, and C4 all share
 * pitch class 0).
 *
 * @property pitchClass An integer from 0 to 11 (C = 0, C# = 1, ... B = 11).
 * @property name The human-readable name of the note (e.g., "C", "F#").
 */
data class Note(
    val pitchClass: Int,
    val name: String,
)

/**
 * Calculates the note produced when a ukulele string is fretted at a given position.
 *
 * On a fretboard, each fret raises the pitch by one semitone (half step).
 * The resulting pitch class wraps around after 12 semitones (one octave),
 * so the formula is:
 *
 *     resultPitchClass = (openStringPitchClass + fret) mod 12
 *
 * For example, the G string (pitch class 7) fretted at fret 5 produces
 * pitch class (7 + 5) % 12 = 0, which is C.
 *
 * @param openStringPitchClass The pitch class of the open (unfretted) string (0–11).
 * @param fret The fret number (0 = open string, 1–12 = fretted positions).
 * @return A [Note] representing the pitch at the given string/fret position.
 */
fun calculateNote(openStringPitchClass: Int, fret: Int): Note {
    val pitchClass = (openStringPitchClass + fret) % Notes.PITCH_CLASS_COUNT
    return Note(
        pitchClass = pitchClass,
        name = Notes.pitchClassToName(pitchClass),
    )
}
