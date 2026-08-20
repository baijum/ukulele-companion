package com.baijum.ukufretboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [NoteDuration], [MelodyNote] and [Melody].
 *
 * `MelodyRepository` persists each note as a pipe-delimited record containing
 * `duration.name`, so the enum constant names are an on-disk contract: renaming
 * one drops every note of that duration from every saved melody.
 */
class MelodyNoteTest {
    // ── NoteDuration arithmetic ──────────────────────────────────────

    @Test
    fun beatsHalveFromWholeToSixteenth() {
        assertEquals(4f, NoteDuration.WHOLE.beats)
        assertEquals(2f, NoteDuration.HALF.beats)
        assertEquals(1f, NoteDuration.QUARTER.beats)
        assertEquals(0.5f, NoteDuration.EIGHTH.beats)
        assertEquals(0.25f, NoteDuration.SIXTEENTH.beats)
    }

    @Test
    fun eachDurationIsExactlyTwiceTheNextShorterOne() {
        val ordered =
            listOf(
                NoteDuration.WHOLE,
                NoteDuration.HALF,
                NoteDuration.QUARTER,
                NoteDuration.EIGHTH,
                NoteDuration.SIXTEENTH,
            )
        for (i in 0 until ordered.size - 1) {
            assertEquals(
                ordered[i].beats,
                ordered[i + 1].beats * 2f,
                "${ordered[i].name} is not twice ${ordered[i + 1].name}",
            )
        }
    }

    @Test
    fun wholeEqualsSixteenSixteenthsAndFourQuarters() {
        assertEquals(NoteDuration.WHOLE.beats, NoteDuration.SIXTEENTH.beats * 16f)
        assertEquals(NoteDuration.WHOLE.beats, NoteDuration.QUARTER.beats * 4f)
    }

    @Test
    fun quarterIsOneBeatSoBeatsConvertDirectlyToMilliseconds() {
        // Playback multiplies beats by the per-beat duration at the melody's tempo.
        val msPerBeat = 60_000f / 120
        assertEquals(500f, NoteDuration.QUARTER.beats * msPerBeat, "a quarter at 120bpm is 500ms")
        assertEquals(2000f, NoteDuration.WHOLE.beats * msPerBeat, "a whole at 120bpm is 2000ms")
    }

    @Test
    fun everyDurationHasPositiveBeats() {
        for (duration in NoteDuration.entries) {
            assertTrue(duration.beats > 0f, "${duration.name} has non-positive beats")
        }
    }

    // ── On-disk contract ─────────────────────────────────────────────

    @Test
    fun durationNamesAreTheOnDiskContract() {
        assertEquals(
            listOf("WHOLE", "HALF", "QUARTER", "EIGHTH", "SIXTEENTH"),
            NoteDuration.entries.map { it.name },
            "MelodyRepository persists duration.name; renaming one loses saved notes",
        )
    }

    @Test
    fun durationLabelsAreUniqueAndNonBlank() {
        val labels = NoteDuration.entries.map { it.label }
        assertEquals(labels.size, labels.toSet().size, "duplicate labels: $labels")
        for (label in labels) {
            assertTrue(label.isNotBlank(), "a duration has a blank label")
        }
    }

    // ── MelodyNote ───────────────────────────────────────────────────

    @Test
    fun melodyNoteDefaultsToAnUnassignedQuarterInOctaveFour() {
        val note = MelodyNote(pitchClass = 0)
        assertEquals(4, note.octave)
        assertEquals(NoteDuration.QUARTER, note.duration)
        assertNull(note.stringIndex, "a fresh note is not assigned to a string")
        assertNull(note.fret, "a fresh note is not assigned to a fret")
    }

    @Test
    fun aRestIsAMelodyNoteWithNullPitchClass() {
        assertNull(MelodyNote(pitchClass = null).pitchClass, "a rest carries no pitch")
    }

    @Test
    fun notesWithTheSameFieldsAreEqual() {
        assertEquals(
            MelodyNote(pitchClass = 7, octave = 3, duration = NoteDuration.HALF),
            MelodyNote(pitchClass = 7, octave = 3, duration = NoteDuration.HALF),
        )
    }

    @Test
    fun octaveIsPartOfNoteIdentity() {
        assertNotEquals(MelodyNote(pitchClass = 0, octave = 3), MelodyNote(pitchClass = 0, octave = 4))
    }

    // ── Melody ───────────────────────────────────────────────────────

    @Test
    fun melodyGeneratesDistinctIdsByDefault() {
        assertNotEquals(
            Melody(name = "A", notes = emptyList()).id,
            Melody(name = "A", notes = emptyList()).id,
        )
    }

    @Test
    fun melodyDefaultsToOneHundredTwentyBpm() {
        assertEquals(120, Melody(name = "A", notes = emptyList()).bpm)
    }

    @Test
    fun melodyStampsCreatedAtFromThePlatformClock() {
        assertTrue(Melody(name = "A", notes = emptyList()).createdAt > 0L)
    }

    @Test
    fun melodyPreservesNoteOrder() {
        val notes =
            listOf(
                MelodyNote(pitchClass = 0),
                MelodyNote(pitchClass = null),
                MelodyNote(pitchClass = 7, duration = NoteDuration.EIGHTH),
            )
        assertEquals(notes, Melody(name = "Tune", notes = notes).notes, "note order must be stable")
    }
}
