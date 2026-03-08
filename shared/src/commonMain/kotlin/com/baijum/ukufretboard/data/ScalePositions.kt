package com.baijum.ukufretboard.data

/**
 * A fretboard position for practicing a scale in a limited fret span.
 *
 * @property name Display name (e.g., "Position 1").
 * @property fretRange The fret range covered by this position.
 * @property notes The scale notes (pitch classes) playable within this range,
 *   organized as a map of stringIndex -> list of frets.
 */
data class ScalePosition(
    val name: String,
    val fretRange: IntRange,
    val notes: Map<Int, List<Int>>,
)

/**
 * Generates fretboard positions for a given scale and tuning.
 *
 * Positions cover 4-5 fret spans, grouping scale notes into manageable chunks
 * so players can practice one area at a time instead of seeing all notes.
 */
object ScalePositions {

    /** Number of strings on a ukulele. */
    private const val NUM_STRINGS = 4

    /** Maximum fret on the ukulele. */
    private const val MAX_FRET = 12

    /** Width of each position in frets. */
    private const val POSITION_SPAN = 5

    /**
     * Generates scale positions for the given scale root and intervals.
     *
     * @param root The root pitch class (0–11).
     * @param intervals The scale intervals (e.g., [0, 2, 4, 5, 7, 9, 11] for major).
     * @param tuningPitchClasses The open pitch class of each string (e.g., [7, 0, 4, 9]).
     * @return A list of [ScalePosition]s covering the fretboard.
     */
    fun generate(
        root: Int,
        intervals: List<Int>,
        tuningPitchClasses: List<Int>,
    ): List<ScalePosition> {
        // Compute the set of pitch classes in this scale
        val scaleNotes = intervals.map { (root + it) % Notes.PITCH_CLASS_COUNT }.toSet()

        val positions = mutableListOf<ScalePosition>()
        var startFret = 0
        var positionNumber = 1

        while (startFret <= MAX_FRET - POSITION_SPAN + 1) {
            val fretRange = startFret..(startFret + POSITION_SPAN - 1)
            val notesInPosition = mutableMapOf<Int, List<Int>>()

            for (stringIndex in 0 until NUM_STRINGS.coerceAtMost(tuningPitchClasses.size)) {
                val openPitch = tuningPitchClasses[stringIndex]
                val frets = fretRange.filter { fret ->
                    val pitchClass = (openPitch + fret) % Notes.PITCH_CLASS_COUNT
                    pitchClass in scaleNotes
                }
                if (frets.isNotEmpty()) {
                    notesInPosition[stringIndex] = frets
                }
            }

            if (notesInPosition.isNotEmpty()) {
                positions.add(
                    ScalePosition(
                        name = "Position $positionNumber",
                        fretRange = fretRange,
                        notes = notesInPosition,
                    ),
                )
                positionNumber++
            }

            startFret += POSITION_SPAN - 1 // Overlap by 1 fret
        }

        return positions
    }
}
