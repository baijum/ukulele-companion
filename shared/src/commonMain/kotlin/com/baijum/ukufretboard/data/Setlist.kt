package com.baijum.ukufretboard.data

import com.baijum.ukufretboard.platform.currentTimeMillis
import com.baijum.ukufretboard.platform.generateUuid
import kotlinx.serialization.Serializable

/**
 * An ordered collection of songs for gigs or practice sessions.
 *
 * @property id Unique identifier.
 * @property name Display name (e.g. "Friday Gig", "Beginner Songs").
 * @property songIds Ordered list of [ChordSheet.id] references.
 * @property createdAt Timestamp when created.
 * @property updatedAt Timestamp when last modified.
 */
@Serializable
data class Setlist(
    val id: String = generateUuid(),
    val name: String,
    val songIds: List<String> = emptyList(),
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
)
