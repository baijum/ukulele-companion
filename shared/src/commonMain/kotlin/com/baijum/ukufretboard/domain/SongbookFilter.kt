package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.SongSortOrder

object SongbookFilter {

    fun <T> apply(
        songs: List<T>,
        query: String,
        label: String?,
        sortOrder: SongSortOrder,
        title: (T) -> String,
        artist: (T) -> String,
        labels: (T) -> List<String>,
        updatedAt: (T) -> Long,
        createdAt: (T) -> Long,
    ): List<T> {
        val normalizedQuery = query.trim().lowercase()

        var filtered = songs

        if (normalizedQuery.isNotEmpty()) {
            filtered = filtered.filter { song ->
                title(song).lowercase().contains(normalizedQuery) ||
                    artist(song).lowercase().contains(normalizedQuery)
            }
        }

        if (label != null) {
            filtered = filtered.filter { song ->
                labels(song).any { it.equals(label, ignoreCase = true) }
            }
        }

        return when (sortOrder) {
            SongSortOrder.LAST_MODIFIED -> filtered.sortedByDescending { updatedAt(it) }
            SongSortOrder.DATE_ADDED -> filtered.sortedByDescending { createdAt(it) }
            SongSortOrder.TITLE -> filtered.sortedBy { title(it).lowercase() }
            SongSortOrder.ARTIST -> filtered.sortedBy { artist(it).lowercase() }
        }
    }
}
