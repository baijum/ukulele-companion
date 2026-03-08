package com.baijum.ukufretboard.data

import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.domain.Note
import com.baijum.ukufretboard.domain.UkuleleString

/**
 * Generates all playable ukulele voicings for a given chord algorithmically.
 *
 * The algorithm:
 * 1. Computes the target pitch classes from the root and chord formula intervals.
 *    For chords with more notes than strings (e.g., 9th chords), generates subsets
 *    by dropping [ChordFormula.omittable] intervals.
 * 2. For each string, finds every fret (0–12) that produces one of the target
 *    pitch classes.
 * 3. Generates the cartesian product of valid frets across all 4 strings.
 * 4. Filters for playability: all required pitch classes must be present, and the
 *    fret span (ignoring open strings) must be at most [MAX_FRET_SPAN].
 * 5. Deduplicates, sorts by position and ease of play, and returns up to
 *    [MAX_VOICINGS] results.
 */
object VoicingGenerator {

    /** Maximum allowed span between the lowest and highest fretted (non-open) positions. */
    private const val MAX_FRET_SPAN = 4

    /** Maximum number of voicings to return per chord. */
    private const val MAX_VOICINGS = 10

    /** The highest fret to consider on the ukulele. */
    private const val LAST_FRET = 12

    /** Maximum number of muted strings allowed per voicing. */
    private const val MAX_MUTED_STRINGS = 1

    /** Maximum number of muted-string voicings to return (appended after full voicings). */
    private const val MAX_MUTED_VOICINGS = 5

    /**
     * Generates playable voicings for a chord defined by [rootPitchClass] and [formula],
     * using the given [tuning].
     *
     * @param rootPitchClass The pitch class (0–11) of the chord's root note.
     * @param formula The [ChordFormula] defining the chord's interval structure.
     * @param tuning The ukulele string tuning to generate voicings for.
     * @param allowMutedStrings When true, voicings with up to [MAX_MUTED_STRINGS]
     *   muted string(s) are included. Muted voicings are always sorted after full
     *   voicings. Defaults to false (all strings must be played).
     * @return A list of [ChordVoicing]s sorted by playability (lower positions first,
     *   smaller spans preferred, more open strings preferred, fewer muted strings preferred).
     */
    fun generate(
        rootPitchClass: Int,
        formula: ChordFormula,
        tuning: List<UkuleleString>,
        allowMutedStrings: Boolean = false,
    ): List<ChordVoicing> {
        val stringCount = tuning.size

        // Build the set of target pitch classes and the required subset
        val allTargetPCs = formula.intervals.map { interval ->
            (rootPitchClass + interval) % Notes.PITCH_CLASS_COUNT
        }.toSet()

        val requiredPCs = formula.intervals
            .subtract(formula.omittable)
            .map { interval -> (rootPitchClass + interval) % Notes.PITCH_CLASS_COUNT }
            .toSet()

        // Determine which pitch class sets to try.
        // For chords with ≤ stringCount notes, use the full set.
        // For chords with > stringCount notes, generate subsets by dropping omittable intervals.
        val pitchClassSets = if (allTargetPCs.size <= stringCount) {
            listOf(allTargetPCs)
        } else {
            buildSubsets(rootPitchClass, formula, stringCount)
        }

        val results = mutableSetOf<List<Int>>() // deduplicate by fret pattern

        for (targetPCs in pitchClassSets) {
            // For each string, find valid frets that produce a target pitch class
            val fretsPerString = tuning.map { string ->
                (0..LAST_FRET).filter { fret ->
                    (string.openPitchClass + fret) % Notes.PITCH_CLASS_COUNT in targetPCs
                }
            }

            // When muted strings are not allowed, skip if any string has no valid frets
            if (!allowMutedStrings && fretsPerString.any { it.isEmpty() }) continue

            // When muted strings are allowed, add MUTED as an option for each string
            // (including strings that have valid frets, to support intentional 3-string voicings)
            val fretsWithMuteOption = if (allowMutedStrings) {
                fretsPerString.map { frets ->
                    if (frets.isEmpty()) listOf(ChordVoicing.MUTED)
                    else frets + ChordVoicing.MUTED
                }
            } else {
                fretsPerString
            }

            // Cartesian product of valid frets across all strings
            cartesianProduct(fretsWithMuteOption) { combination ->
                if (combination in results) return@cartesianProduct

                val mutedCount = combination.count { it == ChordVoicing.MUTED }

                // Enforce muted string limits
                if (!allowMutedStrings && mutedCount > 0) return@cartesianProduct
                if (mutedCount > MAX_MUTED_STRINGS) return@cartesianProduct

                // Check that all required pitch classes are covered by non-muted strings
                val producedPCs = combination.mapIndexedTo(mutableSetOf()) { i, fret ->
                    if (fret == ChordVoicing.MUTED) return@mapIndexedTo -1 // sentinel, won't match
                    (tuning[i].openPitchClass + fret) % Notes.PITCH_CLASS_COUNT
                }
                producedPCs.remove(-1)
                if (!producedPCs.containsAll(requiredPCs)) return@cartesianProduct

                // Check playability: fret span of fretted (non-open, non-muted) positions
                val frettedPositions = combination.filter { it > 0 }
                if (frettedPositions.isNotEmpty()) {
                    val span = frettedPositions.max() - frettedPositions.min()
                    if (span > MAX_FRET_SPAN) return@cartesianProduct
                }

                results.add(combination.toList())
            }
        }

        // Convert to ChordVoicing objects and sort.
        // When muted strings are allowed, use separate pools so muted voicings
        // are guaranteed to appear (they'd otherwise be cut off by MAX_VOICINGS
        // since the comparator sorts them last).
        if (allowMutedStrings) {
            val fullFrets = results.filter { frets -> frets.none { it == ChordVoicing.MUTED } }
            val mutedFrets = results.filter { frets -> frets.any { it == ChordVoicing.MUTED } }

            val sortedFull = fullFrets
                .map { frets -> toVoicing(frets, tuning) }
                .sortedWith(voicingComparator())
                .take(MAX_VOICINGS)
            val sortedMuted = mutedFrets
                .map { frets -> toVoicing(frets, tuning) }
                .sortedWith(voicingComparator())
                .take(MAX_MUTED_VOICINGS)

            return sortedFull + sortedMuted
        }

        return results
            .map { frets -> toVoicing(frets, tuning) }
            .sortedWith(voicingComparator())
            .take(MAX_VOICINGS)
    }

    /**
     * Builds pitch class subsets for chords with more notes than strings.
     *
     * Drops each combination of omittable intervals to produce subsets that
     * fit within [stringCount] strings while retaining all required intervals.
     */
    private fun buildSubsets(
        rootPitchClass: Int,
        formula: ChordFormula,
        stringCount: Int,
    ): List<Set<Int>> {
        val required = formula.intervals - formula.omittable
        val omittableList = formula.omittable.toList()
        val results = mutableListOf<Set<Int>>()
        val needToDrop = formula.intervals.size - stringCount

        // Generate all combinations of omittable intervals to drop
        fun dropCombinations(remaining: List<Int>, toDrop: Int): List<Set<Int>> {
            if (toDrop == 0) return listOf(emptySet())
            if (remaining.size < toDrop) return emptyList()
            val combos = mutableListOf<Set<Int>>()
            for (i in remaining.indices) {
                val sub = dropCombinations(remaining.subList(i + 1, remaining.size), toDrop - 1)
                for (s in sub) {
                    combos.add(s + remaining[i])
                }
            }
            return combos
        }

        for (dropped in dropCombinations(omittableList, needToDrop)) {
            val kept = formula.intervals - dropped
            val pcs = kept.map { (rootPitchClass + it) % Notes.PITCH_CLASS_COUNT }.toSet()
            results.add(pcs)
        }

        // Also include the full set (in case doubling a note works)
        val allPCs = formula.intervals.map { (rootPitchClass + it) % Notes.PITCH_CLASS_COUNT }.toSet()
        if (allPCs !in results) results.add(allPCs)

        return results
    }

    /**
     * Iterates over the cartesian product of lists of frets without materializing
     * the full product in memory. Calls [action] for each combination.
     */
    private inline fun cartesianProduct(
        fretsPerString: List<List<Int>>,
        action: (List<Int>) -> Unit,
    ) {
        val sizes = fretsPerString.map { it.size }
        val indices = IntArray(fretsPerString.size)
        val total = sizes.fold(1L) { acc, s -> acc * s }

        for (i in 0 until total) {
            val combination = fretsPerString.mapIndexed { j, frets -> frets[indices[j]] }
            action(combination)

            // Increment indices (odometer-style)
            for (j in indices.indices.reversed()) {
                indices[j]++
                if (indices[j] < sizes[j]) break
                indices[j] = 0
            }
        }
    }

    /**
     * Converts a fret pattern to a [ChordVoicing] with computed notes and fret range.
     * Muted strings ([ChordVoicing.MUTED]) produce null notes.
     */
    private fun toVoicing(
        frets: List<Int>,
        tuning: List<UkuleleString>,
    ): ChordVoicing {
        val notes = frets.mapIndexed { i, fret ->
            if (fret == ChordVoicing.MUTED) null
            else {
                val pc = (tuning[i].openPitchClass + fret) % Notes.PITCH_CLASS_COUNT
                Note(pitchClass = pc, name = Notes.pitchClassToName(pc))
            }
        }
        val frettedPositions = frets.filter { it > 0 }
        return ChordVoicing(
            frets = frets,
            notes = notes,
            minFret = frettedPositions.minOrNull() ?: 0,
            maxFret = frettedPositions.maxOrNull() ?: 0,
        )
    }

    /**
     * Comparator that sorts voicings by playability:
     * 1. Fewer muted strings first (full voicings before partial)
     * 2. Lower position (minFret) first
     * 3. Smaller fret span preferred
     * 4. More open strings preferred (easier to play)
     */
    private fun voicingComparator(): Comparator<ChordVoicing> =
        compareBy<ChordVoicing> { voicing -> voicing.frets.count { it == ChordVoicing.MUTED } }
            .thenBy { it.minFret }
            .thenBy { it.maxFret - it.minFret }
            .thenByDescending { voicing -> voicing.frets.count { it == 0 } }
}
