package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Notes

/**
 * Generates fretboard note identification exercises.
 *
 * Two modes:
 * - **Name It**: A string and fret are shown; the user picks the note name.
 * - **Find It**: A note name is shown; the user picks the correct string+fret position.
 *
 * Difficulty controls the fret range:
 * - Easy: frets 0–5
 * - Medium: frets 0–9
 * - Hard: frets 0–12
 */
object NoteQuizGenerator {

    /** Quiz mode. */
    enum class Mode { NAME_IT, FIND_IT }

    /** Open-string pitch classes for standard high-G tuning: G, C, E, A. */
    private val STANDARD_STRING_OPEN_NOTES = listOf(7, 0, 4, 9)

    /** String display names in standard tuning order. */
    private val STANDARD_STRING_NAMES = listOf("G", "C", "E", "A")

    /** Max fret by difficulty level (1=Easy, 2=Medium, 3=Hard). */
    private val DIFFICULTY_MAX_FRET = mapOf(1 to 5, 2 to 9, 3 to 12)

    /**
     * A "Name It" question: identify the note at a given position.
     *
     * @property string String index (0–3).
     * @property fret Fret number.
     * @property pitchClass The correct pitch class (0–11).
     * @property correctNote The correct note name.
     * @property options Four note-name options.
     * @property correctIndex Index of the correct answer in [options].
     */
    data class NameItQuestion(
        val string: Int,
        val fret: Int,
        val pitchClass: Int,
        val correctNote: String,
        val options: List<String>,
        val correctIndex: Int,
    )

    /**
     * A "Find It" question: find the position of a given note.
     *
     * @property targetNote The note name to find.
     * @property targetPitchClass Pitch class of the target note.
     * @property correctString String index of the correct answer.
     * @property correctFret Fret of the correct answer.
     * @property options Four (stringName, fret) label options.
     * @property correctIndex Index of the correct answer in [options].
     */
    data class FindItQuestion(
        val targetNote: String,
        val targetPitchClass: Int,
        val correctString: Int,
        val correctFret: Int,
        val options: List<String>,
        val correctIndex: Int,
    )

    /**
     * Generates a "Name It" question at the given difficulty.
     *
     * @param difficulty 1=Easy, 2=Medium, 3=Hard.
     */
    fun generateNameIt(
        difficulty: Int = 2,
        stringOpenNotes: List<Int> = STANDARD_STRING_OPEN_NOTES,
    ): NameItQuestion {
        val openNotes = List(4) { index ->
            stringOpenNotes.getOrElse(index) { STANDARD_STRING_OPEN_NOTES[index] }
        }
        val maxFret = DIFFICULTY_MAX_FRET[difficulty.coerceIn(1, 3)] ?: 12
        val string = (0..3).random()
        val fret = (0..maxFret).random()
        val pitchClass = (openNotes[string] + fret) % 12
        val correctNote = Notes.pitchClassToName(pitchClass)

        // 3 wrong answers: other note names not equal to the correct one
        val allNotes = (0..11).map { Notes.pitchClassToName(it) }.distinct()
        val wrong = allNotes.filter { it != correctNote }.shuffled().take(3)

        val options = (wrong + correctNote).shuffled()
        val correctIndex = options.indexOf(correctNote)

        return NameItQuestion(
            string = string,
            fret = fret,
            pitchClass = pitchClass,
            correctNote = correctNote,
            options = options,
            correctIndex = correctIndex,
        )
    }

    /**
     * Generates a "Find It" question at the given difficulty.
     *
     * @param difficulty 1=Easy, 2=Medium, 3=Hard.
     */
    fun generateFindIt(
        difficulty: Int = 2,
        stringOpenNotes: List<Int> = STANDARD_STRING_OPEN_NOTES,
        stringNames: List<String> = STANDARD_STRING_NAMES,
    ): FindItQuestion {
        val openNotes = List(4) { index ->
            stringOpenNotes.getOrElse(index) { STANDARD_STRING_OPEN_NOTES[index] }
        }
        val displayStringNames = List(4) { index ->
            stringNames.getOrElse(index) { STANDARD_STRING_NAMES[index] }
        }
        val maxFret = DIFFICULTY_MAX_FRET[difficulty.coerceIn(1, 3)] ?: 12

        // Pick a random target position
        val correctString = (0..3).random()
        val correctFret = (0..maxFret).random()
        val targetPc = (openNotes[correctString] + correctFret) % 12
        val targetNote = Notes.pitchClassToName(targetPc)

        val correctLabel = "${displayStringNames[correctString]} string, fret $correctFret"

        // Generate 3 wrong positions (different from the correct one)
        val wrongPositions = mutableSetOf<String>()
        while (wrongPositions.size < 3) {
            val s = (0..3).random()
            val f = (0..maxFret).random()
            val pc = (openNotes[s] + f) % 12
            // Must not be the same position and must not be the same note
            if ((s != correctString || f != correctFret) && pc != targetPc) {
                wrongPositions.add("${displayStringNames[s]} string, fret $f")
            }
        }

        val options = (wrongPositions.toList() + correctLabel).shuffled()
        val correctIndex = options.indexOf(correctLabel)

        return FindItQuestion(
            targetNote = targetNote,
            targetPitchClass = targetPc,
            correctString = correctString,
            correctFret = correctFret,
            options = options,
            correctIndex = correctIndex,
        )
    }
}
