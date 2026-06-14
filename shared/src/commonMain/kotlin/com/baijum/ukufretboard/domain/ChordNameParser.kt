package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ChordFormula
import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes

/**
 * Result of parsing or searching a chord name.
 *
 * @property displayName The canonical display name (e.g., "Cm/Eb").
 * @property rootPitchClass Pitch class of the root note (0–11).
 * @property formula The matched [ChordFormula].
 * @property quality Human-readable quality (e.g., "Minor 7th").
 * @property bassPitchClass Pitch class of the slash bass note, or null for root position.
 * @property inversion The inversion implied by the bass note, or null.
 */
data class ChordSearchResult(
    val displayName: String,
    val rootPitchClass: Int,
    val formula: ChordFormula,
    val quality: String,
    val bassPitchClass: Int? = null,
    val inversion: ChordInfo.Inversion? = null,
)

/**
 * Parses chord name strings and generates search suggestions.
 *
 * Handles canonical symbols (e.g., "m7"), aliases (e.g., "-7", "M7", "ø7"),
 * slash chords (e.g., "C/E"), and both sharp/flat note names.
 *
 * This is pure Kotlin with no Android dependencies.
 */
object ChordNameParser {

    private val ROOT_REGEX = Regex("^([A-Ga-g][#b]?)")

    /**
     * All note name variants mapped to their pitch class, covering both
     * sharp and flat spellings. Lowercase variants are included for
     * case-insensitive root matching.
     */
    private val NOTE_NAME_TO_PITCH_CLASS: Map<String, Int> = buildMap {
        Notes.NOTE_NAMES_SHARP.forEachIndexed { pc, name -> put(name, pc) }
        Notes.NOTE_NAMES_FLAT.forEachIndexed { pc, name -> put(name, pc) }
    }

    /**
     * All distinct root note names that can start a chord (e.g., "C", "C#", "Db").
     * Sorted longest-first so "C#" matches before "C".
     */
    private val ROOT_NAMES: List<String> =
        NOTE_NAME_TO_PITCH_CLASS.keys.sortedByDescending { it.length }

    /**
     * Parses a complete chord name into a [ChordSearchResult].
     *
     * Supports formats like "C", "Cm", "C#m7", "Bbmaj7", "C/E", "Cm/Eb",
     * "CM", "C-7", "Cø7", etc.
     *
     * @return The parsed result, or null if the input cannot be recognized.
     */
    fun parse(input: String): ChordSearchResult? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val (rootPc, rootName, remainder) = extractRoot(trimmed) ?: return null

        // Check for slash chord: "quality/bass"
        val slashIndex = remainder.indexOf('/')
        val qualityPart: String
        val bassPart: String?

        if (slashIndex >= 0) {
            qualityPart = remainder.substring(0, slashIndex)
            bassPart = remainder.substring(slashIndex + 1)
        } else {
            qualityPart = remainder
            bassPart = null
        }

        val formula = matchFormula(qualityPart) ?: return null

        val canonicalRoot = Notes.pitchClassToName(rootPc)

        if (bassPart != null) {
            val bassPc = resolveNoteName(bassPart) ?: return null
            val interval = (bassPc - rootPc + Notes.PITCH_CLASS_COUNT) % Notes.PITCH_CLASS_COUNT
            if (interval !in formula.intervals || interval == 0) return null
            val inversion = ChordInfo.inversionForInterval(interval, formula)
            val bassName = Notes.pitchClassToName(bassPc)
            return ChordSearchResult(
                displayName = "$canonicalRoot${formula.symbol}/$bassName",
                rootPitchClass = rootPc,
                formula = formula,
                quality = formula.quality,
                bassPitchClass = bassPc,
                inversion = inversion,
            )
        }

        return ChordSearchResult(
            displayName = "$canonicalRoot${formula.symbol}",
            rootPitchClass = rootPc,
            formula = formula,
            quality = formula.quality,
        )
    }

    /**
     * Generates chord search suggestions for a partial query.
     *
     * Returns matching chords ordered by: exact symbol matches first,
     * then alias matches. Within each group, formulas follow the
     * [ChordFormulas.ALL] ordering (triads first, then extended).
     */
    fun suggestions(query: String): List<ChordSearchResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val (rootPc, rootName, remainder) = extractRoot(trimmed)
            ?: return emptyList()

        val canonicalRoot = Notes.pitchClassToName(rootPc)

        val slashIndex = remainder.indexOf('/')
        if (slashIndex >= 0) {
            return slashChordSuggestions(
                rootPc, canonicalRoot,
                remainder.substring(0, slashIndex),
                remainder.substring(slashIndex + 1),
            )
        }

        val results = mutableListOf<ChordSearchResult>()

        for (formula in ChordFormulas.ALL) {
            if (matchesPrefix(formula.symbol, remainder)) {
                results.add(
                    ChordSearchResult(
                        displayName = "$canonicalRoot${formula.symbol}",
                        rootPitchClass = rootPc,
                        formula = formula,
                        quality = formula.quality,
                    )
                )
            } else if (aliasMatchesPrefix(formula, remainder)) {
                results.add(
                    ChordSearchResult(
                        displayName = "$canonicalRoot${formula.symbol}",
                        rootPitchClass = rootPc,
                        formula = formula,
                        quality = formula.quality,
                    )
                )
            }
        }

        return results.distinctBy { it.formula.symbol }
    }

    // ── Private helpers ─────────────────────────────────────────────────

    private data class RootExtraction(
        val pitchClass: Int,
        val name: String,
        val remainder: String,
    )

    private fun extractRoot(input: String): RootExtraction? {
        val match = ROOT_REGEX.find(input) ?: return null
        val rawRoot = match.groupValues[1]
        val normalizedRoot = rawRoot.replaceFirstChar { it.uppercaseChar() }
        val pc = resolveNoteName(normalizedRoot) ?: return null
        return RootExtraction(pc, normalizedRoot, input.substring(match.range.last + 1))
    }

    private fun resolveNoteName(name: String): Int? {
        val normalized = name.replaceFirstChar { it.uppercaseChar() }
        return NOTE_NAME_TO_PITCH_CLASS[normalized]
    }

    /**
     * Finds a [ChordFormula] whose symbol or aliases exactly match [quality].
     * Symbol matching is case-sensitive; alias matching is exact as stored.
     */
    private fun matchFormula(quality: String): ChordFormula? {
        // Exact symbol match first
        ChordFormulas.ALL.firstOrNull { it.symbol == quality }?.let { return it }
        // Exact alias match
        return ChordFormulas.ALL.firstOrNull { formula ->
            formula.aliases.any { it == quality }
        }
    }

    /**
     * Case-sensitive prefix match for symbols.
     * Empty query matches everything (shows all chords for a root).
     */
    private fun matchesPrefix(symbol: String, query: String): Boolean {
        if (query.isEmpty()) return true
        return symbol.startsWith(query)
    }

    /**
     * Checks if any alias of [formula] starts with [query] (exact case).
     */
    private fun aliasMatchesPrefix(formula: ChordFormula, query: String): Boolean {
        if (query.isEmpty()) return false
        return formula.aliases.any { it.startsWith(query) }
    }

    /**
     * Generates slash chord suggestions for a given root + quality + partial bass.
     */
    private fun slashChordSuggestions(
        rootPc: Int,
        canonicalRoot: String,
        qualityPart: String,
        bassQuery: String,
    ): List<ChordSearchResult> {
        val formula = if (qualityPart.isEmpty()) {
            matchFormula("")
        } else {
            matchFormula(qualityPart)
        } ?: return emptyList()

        val results = mutableListOf<ChordSearchResult>()

        // Non-root intervals that can be in the bass
        val bassIntervals = formula.intervals.filter { it != 0 }

        for (interval in bassIntervals) {
            val bassPc = (rootPc + interval) % Notes.PITCH_CLASS_COUNT
            val bassName = Notes.pitchClassToName(bassPc)
            val bassNameSharp = Notes.NOTE_NAMES_SHARP[bassPc]
            val bassNameFlat = Notes.NOTE_NAMES_FLAT[bassPc]

            val matchesBass = bassQuery.isEmpty() ||
                bassName.startsWith(bassQuery, ignoreCase = true) ||
                bassNameSharp.startsWith(bassQuery, ignoreCase = true) ||
                bassNameFlat.startsWith(bassQuery, ignoreCase = true)

            if (matchesBass) {
                val inversion = ChordInfo.inversionForInterval(interval, formula)
                results.add(
                    ChordSearchResult(
                        displayName = "$canonicalRoot${formula.symbol}/$bassName",
                        rootPitchClass = rootPc,
                        formula = formula,
                        quality = formula.quality,
                        bassPitchClass = bassPc,
                        inversion = inversion,
                    )
                )
            }
        }

        return results
    }
}
