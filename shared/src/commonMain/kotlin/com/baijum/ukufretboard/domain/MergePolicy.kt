package com.baijum.ukufretboard.domain

/**
 * Merge two lists by ID, keeping the newer version when duplicates exist.
 * Used by repositories with updatedAt timestamps (Setlist, ChordSheet).
 */
fun <T> mergeNewerWins(
    existing: List<T>,
    incoming: List<T>,
    idOf: (T) -> String,
    timestampOf: (T) -> Long,
): List<T> {
    val merged = existing.associateBy(idOf).toMutableMap()
    for (item in incoming) {
        val id = idOf(item)
        val current = merged[id]
        if (current == null || timestampOf(item) >= timestampOf(current)) {
            merged[id] = item
        }
    }
    return merged.values.toList()
}

/**
 * Merge two lists by ID, keeping the existing version when duplicates exist.
 * Only adds items from [incoming] that don't already exist. Used by repositories
 * that treat local data as authoritative (Favorites, CustomPatterns, Progressions).
 */
fun <T> mergeKeepExisting(
    existing: List<T>,
    incoming: List<T>,
    idOf: (T) -> String,
): List<T> {
    val existingIds = existing.map(idOf).toSet()
    return existing + incoming.filter { idOf(it) !in existingIds }
}
