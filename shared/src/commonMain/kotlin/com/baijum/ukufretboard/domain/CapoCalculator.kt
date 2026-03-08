package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordFormula
import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Progression
import com.baijum.ukufretboard.data.VoicingGenerator

/**
 * Capo Calculator — finds the best capo position to simplify a chord or progression.
 *
 * For each capo position (0–11), transposes the chord(s) down by that many
 * semitones, generates voicings for the transposed shapes, and scores them
 * by playability. Results are sorted from easiest to hardest.
 *
 * **Scoring heuristic:**
 * - More open strings = easier (each open string scores +3)
 * - Lower average fret position = easier
 * - Smaller fret span = easier
 * - Matching a "friendly" open chord shape (C, G, Am, etc.) = big bonus
 */
object CapoCalculator {

    /**
     * A single capo position result for a chord.
     *
     * @property capoFret The fret to place the capo (0 = no capo).
     * @property shapeName The chord shape name played behind the capo (e.g., "Am").
     * @property soundingName The actual sounding chord name (e.g., "Cm").
     * @property bestVoicing The easiest voicing for this capo position.
     * @property score Playability score (higher = easier).
     */
    data class SingleChordResult(
        val capoFret: Int,
        val shapeName: String,
        val soundingName: String,
        val bestVoicing: ChordVoicing,
        val score: Int,
    )

    /**
     * A single capo position result for a full progression.
     *
     * @property capoFret The fret to place the capo (0 = no capo).
     * @property chordResults Per-chord results within the progression.
     * @property totalScore Sum of per-chord scores (higher = easier).
     */
    data class ProgressionResult(
        val capoFret: Int,
        val chordResults: List<SingleChordResult>,
        val totalScore: Int,
    )

    /**
     * Well-known "friendly" open chord shapes that get a bonus.
     * These are shapes beginners learn first and find easiest.
     */
    private val FRIENDLY_SHAPES = setOf(
        "" /* Major */, "m", "7",
    )
    private val FRIENDLY_ROOTS = setOf(
        0 /* C */, 2 /* D */, 5 /* F */, 7 /* G */, 9 /* A */,
    )

    /**
     * Finds the best capo positions for a single chord.
     *
     * @param rootPitchClass The pitch class of the chord's root (0–11).
     * @param formula The chord's formula (intervals + quality).
     * @param tuning Current ukulele tuning.
     * @return List of results sorted by score (best first), one per capo position.
     */
    fun forSingleChord(
        rootPitchClass: Int,
        formula: ChordFormula,
        tuning: List<UkuleleString>,
    ): List<SingleChordResult> {
        val results = mutableListOf<SingleChordResult>()

        for (capo in 0..11) {
            // Transpose root DOWN by capo semitones to get the shape
            val shapeRoot = ((rootPitchClass - capo) % Notes.PITCH_CLASS_COUNT +
                Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT

            // Generate voicings for the shape chord
            val voicings = VoicingGenerator.generate(shapeRoot, formula, tuning)
            if (voicings.isEmpty()) continue

            val best = voicings.first() // Already sorted by playability
            val score = scoreVoicing(best, shapeRoot, formula.symbol)

            val shapeName = Notes.pitchClassToName(shapeRoot) + formula.symbol
            val soundingName = Notes.pitchClassToName(rootPitchClass) + formula.symbol

            results.add(
                SingleChordResult(
                    capoFret = capo,
                    shapeName = shapeName,
                    soundingName = soundingName,
                    bestVoicing = best,
                    score = score,
                )
            )
        }

        return results.sortedByDescending { it.score }
    }

    /**
     * Finds the best capo positions for a full chord progression.
     *
     * @param progression The chord progression.
     * @param keyRoot The pitch class of the key root (0–11).
     * @param tuning Current ukulele tuning.
     * @return List of results sorted by total score (best first).
     */
    fun forProgression(
        progression: Progression,
        keyRoot: Int,
        tuning: List<UkuleleString>,
    ): List<ProgressionResult> {
        val results = mutableListOf<ProgressionResult>()

        for (capo in 0..11) {
            val chordResults = mutableListOf<SingleChordResult>()
            var allValid = true

            for (degree in progression.degrees) {
                val soundingRoot = (keyRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
                val shapeRoot = ((soundingRoot - capo) % Notes.PITCH_CLASS_COUNT +
                    Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT

                // Find the formula for this quality
                val formula = ChordFormulas.ALL.firstOrNull { it.symbol == degree.quality }
                if (formula == null) { allValid = false; break }

                val voicings = VoicingGenerator.generate(shapeRoot, formula, tuning)
                if (voicings.isEmpty()) { allValid = false; break }

                val best = voicings.first()
                val score = scoreVoicing(best, shapeRoot, formula.symbol)

                val shapeName = Notes.pitchClassToName(shapeRoot) + degree.quality
                val soundingName = Notes.pitchClassToName(soundingRoot) + degree.quality

                chordResults.add(
                    SingleChordResult(
                        capoFret = capo,
                        shapeName = shapeName,
                        soundingName = soundingName,
                        bestVoicing = best,
                        score = score,
                    )
                )
            }

            if (!allValid) continue

            results.add(
                ProgressionResult(
                    capoFret = capo,
                    chordResults = chordResults,
                    totalScore = chordResults.sumOf { it.score },
                )
            )
        }

        return results.sortedByDescending { it.totalScore }
    }

    /**
     * Scores a voicing for ease of play. Higher = easier.
     */
    private fun scoreVoicing(voicing: ChordVoicing, shapeRoot: Int, symbol: String): Int {
        var score = 0

        // Open strings bonus (+3 each)
        val openCount = voicing.frets.count { it == 0 }
        score += openCount * 3

        // Low position bonus (frets near the nut are easier)
        val avgFret = voicing.frets.filter { it > 0 }.let { fretted ->
            if (fretted.isEmpty()) 0.0 else fretted.average()
        }
        score += maxOf(0, (10 - avgFret).toInt())

        // Small span bonus
        val span = voicing.maxFret - voicing.minFret
        score += maxOf(0, 5 - span)

        // "Friendly" shape bonus — well-known beginner chords
        if (symbol in FRIENDLY_SHAPES && shapeRoot in FRIENDLY_ROOTS) {
            score += 5
        }

        // All open strings: maximum bonus
        if (voicing.frets.all { it == 0 }) {
            score += 3
        }

        return score
    }
}
