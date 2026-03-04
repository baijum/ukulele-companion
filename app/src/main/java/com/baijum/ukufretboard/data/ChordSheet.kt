package com.baijum.ukufretboard.data

import java.util.UUID

/**
 * A chord sheet containing lyrics with inline chord markers.
 *
 * Chord names are embedded in the content using square brackets, e.g.,
 * `"Some[C]where over the [Em]rainbow"`.
 *
 * @property id Unique identifier.
 * @property title Song title.
 * @property artist Artist name (optional).
 * @property content Raw text with `[ChordName]` markers.
 * @property key Original key from ChordPro `{key}` directive (optional).
 * @property capo Capo position from ChordPro `{capo}` directive (optional).
 * @property strumPatternName Name of the associated strumming pattern (optional).
 *   Matches names from [StrumPatterns.ALL] or custom pattern names.
 * @property labels User-defined labels for categorization and filtering.
 * @property createdAt Timestamp when created.
 * @property updatedAt Timestamp when last modified.
 */
data class ChordSheet(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val artist: String = "",
    val content: String,
    val key: String = "",
    val capo: Int = 0,
    val strumPatternName: String = "",
    val labels: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
