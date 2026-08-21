package com.baijum.ukufretboard.data

import com.baijum.ukufretboard.platform.currentTimeMillis
import com.baijum.ukufretboard.platform.generateUuid
import kotlinx.serialization.Serializable

/**
 * Duration of a note relative to the beat.
 */
enum class NoteDuration(val label: String, val beats: Float) {
    WHOLE("Whole", 4f),
    HALF("Half", 2f),
    QUARTER("Quarter", 1f),
    EIGHTH("Eighth", 0.5f),
    SIXTEENTH("Sixteenth", 0.25f),
}

/**
 * A single note in a melody sequence.
 *
 * @property pitchClass The pitch class (0–11, where C=0). Null for a rest.
 * @property octave The octave (4 = middle C octave). Used for display.
 * @property duration The note duration.
 * @property stringIndex Which ukulele string this note is on (0–3). Null if unassigned.
 * @property fret Which fret this note is at. Null if unassigned.
 */
@Serializable
data class MelodyNote(
    val pitchClass: Int?,
    val octave: Int = 4,
    val duration: NoteDuration = NoteDuration.QUARTER,
    val stringIndex: Int? = null,
    val fret: Int? = null,
)

/**
 * A melody sequence with a name and tempo.
 *
 * @property id Unique identifier.
 * @property name Name of the melody.
 * @property notes The sequence of notes.
 * @property bpm Tempo in beats per minute.
 * @property createdAt Timestamp.
 */
@Serializable
data class Melody(
    val id: String = generateUuid(),
    val name: String,
    val notes: List<MelodyNote>,
    val bpm: Int = 120,
    val createdAt: Long = currentTimeMillis(),
)
