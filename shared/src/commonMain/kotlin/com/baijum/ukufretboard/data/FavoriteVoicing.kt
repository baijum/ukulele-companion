package com.baijum.ukufretboard.data

import com.baijum.ukufretboard.platform.currentTimeMillis
import com.baijum.ukufretboard.platform.generateUuid

/**
 * A saved chord voicing in the favorites list.
 *
 * @property rootPitchClass The pitch class (0–11) of the chord root.
 * @property chordSymbol The chord quality symbol (e.g., "m7", "sus2", "").
 * @property frets The fret positions for each string (e.g., [0, 0, 0, 3]).
 * @property addedAt Timestamp when the voicing was saved.
 * @property folderIds IDs of the folders this voicing belongs to. Empty = unfiled.
 */
data class FavoriteVoicing(
    val rootPitchClass: Int,
    val chordSymbol: String,
    val frets: List<Int>,
    val addedAt: Long = currentTimeMillis(),
    val folderIds: List<String> = emptyList(),
) {
    /**
     * A unique key for deduplication: root + symbol + frets.
     */
    val key: String get() = "$rootPitchClass|$chordSymbol|${frets.joinToString(",")}"
}

/**
 * A folder for organizing favorite voicings.
 *
 * @property id Unique identifier.
 * @property name Display name of the folder.
 * @property createdAt Timestamp when the folder was created.
 * @property voicingOrder Ordered list of voicing keys defining display order within the folder.
 */
data class FavoriteFolder(
    val id: String = generateUuid(),
    val name: String,
    val createdAt: Long = currentTimeMillis(),
    val voicingOrder: List<String> = emptyList(),
)
