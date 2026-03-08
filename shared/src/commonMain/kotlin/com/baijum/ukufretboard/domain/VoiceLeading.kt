package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Progression
import com.baijum.ukufretboard.data.VoicingGenerator
import kotlin.math.abs

/**
 * Algorithms for computing optimal voice leading paths through chord progressions.
 *
 * Voice leading minimizes total finger movement between consecutive chord
 * voicings, producing smoother transitions and easier chord changes.
 * Uses dynamic programming to find the globally optimal path through
 * all possible voicings for each chord in a progression.
 */
object VoiceLeading {

    /**
     * A single step in a voice leading path.
     *
     * @property chordName Display name of the chord (e.g., "C", "Am").
     * @property voicing The optimal voicing selected for this step.
     * @property numeral Roman numeral label (e.g., "I", "vi").
     */
    data class Step(
        val chordName: String,
        val voicing: ChordVoicing,
        val numeral: String,
    )

    /**
     * Information about the transition between two consecutive voicings.
     *
     * @property commonToneIndices String indices (0–3) where the fret stays the same.
     * @property totalDistance Sum of absolute fret differences across all 4 strings.
     * @property movements Per-string signed movement (positive = higher fret).
     */
    data class TransitionInfo(
        val commonToneIndices: Set<Int>,
        val totalDistance: Int,
        val movements: List<Int>,
    )

    /**
     * A complete voice leading path through a chord progression.
     *
     * @property progressionName Name of the progression.
     * @property keyRoot Pitch class of the key root.
     * @property steps The voicing selected for each chord.
     * @property transitions Transition info between consecutive steps (size = steps.size - 1).
     * @property totalDistance Sum of all transition distances.
     */
    data class Path(
        val progressionName: String,
        val keyRoot: Int,
        val steps: List<Step>,
        val transitions: List<TransitionInfo>,
        val totalDistance: Int,
    )

    /**
     * Computes voice leading distance between two voicings.
     *
     * Distance is the sum of absolute fret differences across all strings,
     * directly corresponding to total physical finger movement on the fretboard.
     *
     * @return Total fret distance (0 = identical voicings).
     */
    fun distance(v1: ChordVoicing, v2: ChordVoicing): Int =
        v1.frets.zip(v2.frets).sumOf { (f1, f2) -> abs(f1 - f2) }

    /**
     * Computes transition information between two consecutive voicings.
     *
     * Identifies common tones (strings where the fret stays the same),
     * total movement distance, and per-string signed movement.
     */
    fun computeTransition(v1: ChordVoicing, v2: ChordVoicing): TransitionInfo {
        val movements = v1.frets.zip(v2.frets).map { (f1, f2) -> f2 - f1 }
        val commonTones = movements.indices.filter { movements[it] == 0 }.toSet()
        return TransitionInfo(commonTones, movements.sumOf { abs(it) }, movements)
    }

    /**
     * Computes the optimal voice leading path through a chord progression
     * using dynamic programming.
     *
     * For each chord in the progression, all playable voicings are generated.
     * The algorithm then finds the sequence of voicings that minimizes total
     * finger movement across the entire progression. The first chord is biased
     * toward lower (more comfortable) fret positions.
     *
     * Complexity: O(n × k²) where n = number of chords (typically 3–8)
     * and k = max voicings per chord (≤ 10). This is trivially fast.
     *
     * @param progression The chord progression.
     * @param keyRoot Pitch class (0–11) of the key.
     * @param tuning Current ukulele tuning.
     * @return The optimal [Path], or null if any chord has no playable voicings.
     */
    fun computeOptimalPath(
        progression: Progression,
        keyRoot: Int,
        tuning: List<UkuleleString>,
    ): Path? {
        val n = progression.degrees.size
        if (n == 0) return null

        // Generate all voicings for each chord
        val chordVoicings = progression.degrees.map { degree ->
            val chordRoot = (keyRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
            val formula = ChordFormulas.ALL.firstOrNull { it.symbol == degree.quality }
                ?: return null
            val voicings = VoicingGenerator.generate(chordRoot, formula, tuning)
            if (voicings.isEmpty()) return null
            voicings
        }

        // Single chord: just pick the most comfortable voicing (already sorted)
        if (n == 1) {
            val degree = progression.degrees[0]
            val chordRoot = (keyRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
            val step = Step(
                chordName = Notes.enharmonicForKey(chordRoot, keyRoot) + degree.quality,
                voicing = chordVoicings[0][0],
                numeral = degree.numeral,
            )
            return Path(progression.name, keyRoot, listOf(step), emptyList(), 0)
        }

        // DP shortest path through the voicing graph.
        // dpPrev[j] = minimum total distance to reach voicing j at the previous chord.
        var dpPrev = IntArray(chordVoicings[0].size) { j ->
            // Starting bias: prefer low-fret, easy voicings
            chordVoicings[0][j].minFret
        }
        // backtrack[i][j] = which voicing index at chord i-1 leads to voicing j at chord i
        val backtrack = Array(n) { IntArray(0) }
        backtrack[0] = IntArray(chordVoicings[0].size) { -1 }

        for (i in 1 until n) {
            val curr = chordVoicings[i]
            val prev = chordVoicings[i - 1]
            val dpNext = IntArray(curr.size)
            val prevIdx = IntArray(curr.size)

            for (j in curr.indices) {
                var bestDist = Int.MAX_VALUE
                var bestK = 0
                for (k in prev.indices) {
                    val d = dpPrev[k] + distance(prev[k], curr[j])
                    if (d < bestDist) {
                        bestDist = d
                        bestK = k
                    }
                }
                dpNext[j] = bestDist
                prevIdx[j] = bestK
            }

            backtrack[i] = prevIdx
            dpPrev = dpNext
        }

        // Backtrack to find optimal voicing index at each chord
        val indices = IntArray(n)
        indices[n - 1] = dpPrev.indices.minByOrNull { dpPrev[it] } ?: 0
        for (i in n - 2 downTo 0) {
            indices[i] = backtrack[i + 1][indices[i + 1]]
        }

        // Build result
        val steps = indices.mapIndexed { i, vi ->
            val degree = progression.degrees[i]
            val chordRoot = (keyRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
            Step(
                chordName = Notes.enharmonicForKey(chordRoot, keyRoot) + degree.quality,
                voicing = chordVoicings[i][vi],
                numeral = degree.numeral,
            )
        }

        val transitions = (0 until n - 1).map { i ->
            computeTransition(steps[i].voicing, steps[i + 1].voicing)
        }

        return Path(
            progressionName = progression.name,
            keyRoot = keyRoot,
            steps = steps,
            transitions = transitions,
            totalDistance = transitions.sumOf { it.totalDistance },
        )
    }
}
