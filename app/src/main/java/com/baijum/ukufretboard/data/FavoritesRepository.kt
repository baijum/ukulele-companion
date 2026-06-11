package com.baijum.ukufretboard.data

import android.content.Context
import android.content.SharedPreferences
import com.baijum.ukufretboard.domain.mergeKeepExisting

/**
 * Repository for persisting favorite voicings using SharedPreferences.
 *
 * Each favorite is stored as a key-value pair where the key is derived from
 * the voicing's root, symbol, and frets, and the value is a serialized string.
 *
 * This is intentionally simple (no Room, no DataStore) to avoid adding
 * dependencies for what is essentially a small list.
 */
class FavoritesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns all saved favorites, sorted by time added (newest first).
     */
    fun getAll(): List<FavoriteVoicing> {
        return prefs.all.entries
            .mapNotNull { (_, value) -> deserialize(value as? String) }
            .sortedByDescending { it.addedAt }
    }

    /**
     * Adds a voicing to favorites. No-op if already saved.
     */
    fun add(voicing: FavoriteVoicing) {
        if (!contains(voicing)) {
            prefs.edit().putString(voicing.key, serialize(voicing)).apply()
        }
    }

    /**
     * Removes a voicing from favorites. Also removes its key from any folder ordering.
     */
    fun remove(voicing: FavoriteVoicing) {
        prefs.edit().remove(voicing.key).apply()
        // Clean up ordering in all folders that referenced this voicing
        getAllFolders().forEach { folder ->
            if (voicing.key in folder.voicingOrder) {
                val updated = folder.copy(voicingOrder = folder.voicingOrder - voicing.key)
                saveFolder(updated)
            }
        }
    }

    /**
     * Checks if a voicing is already in favorites.
     */
    fun contains(voicing: FavoriteVoicing): Boolean =
        prefs.contains(voicing.key)

    /**
     * Checks if a voicing with the given root, symbol, and frets is in favorites.
     */
    fun contains(rootPitchClass: Int, chordSymbol: String, frets: List<Int>): Boolean {
        val key = "$rootPitchClass|$chordSymbol|${frets.joinToString(",")}"
        return prefs.contains(key)
    }

    /**
     * Updates the folder assignments for a voicing and keeps folder ordering in sync.
     */
    fun setFolders(voicing: FavoriteVoicing, folderIds: List<String>) {
        val oldFolderIds = voicing.folderIds.toSet()
        val newFolderIds = folderIds.toSet()
        val updated = voicing.copy(folderIds = folderIds)
        prefs.edit().putString(updated.key, serialize(updated)).apply()

        // Append voicing key to newly added folders' ordering
        val addedTo = newFolderIds - oldFolderIds
        val removedFrom = oldFolderIds - newFolderIds
        val allFolders = getAllFolders()
        allFolders.forEach { folder ->
            when (folder.id) {
                in addedTo -> {
                    if (voicing.key !in folder.voicingOrder) {
                        saveFolder(folder.copy(voicingOrder = folder.voicingOrder + voicing.key))
                    }
                }
                in removedFrom -> {
                    if (voicing.key in folder.voicingOrder) {
                        saveFolder(folder.copy(voicingOrder = folder.voicingOrder - voicing.key))
                    }
                }
            }
        }
    }

    // ── Folder management ───────────────────────────────────────────

    private val folderPrefs: SharedPreferences =
        context.getSharedPreferences(FOLDER_PREFS_NAME, Context.MODE_PRIVATE)

    fun getAllFolders(): List<FavoriteFolder> {
        return folderPrefs.all.entries
            .mapNotNull { (_, value) -> deserializeFolder(value as? String) }
            .sortedBy { it.name }
    }

    fun saveFolder(folder: FavoriteFolder) {
        folderPrefs.edit().putString(folder.id, serializeFolder(folder)).apply()
    }

    fun renameFolder(folderId: String, newName: String) {
        val folder = getAllFolders().firstOrNull { it.id == folderId } ?: return
        saveFolder(folder.copy(name = newName))
    }

    fun deleteFolder(folderId: String) {
        folderPrefs.edit().remove(folderId).apply()
        // Remove this folder ID from all voicings that reference it
        getAll().filter { folderId in it.folderIds }.forEach { voicing ->
            val updated = voicing.copy(folderIds = voicing.folderIds - folderId)
            prefs.edit().putString(updated.key, serialize(updated)).apply()
        }
    }

    /**
     * Overwrites the voicing ordering for a folder.
     */
    fun reorderVoicingsInFolder(folderId: String, orderedKeys: List<String>) {
        val folder = getAllFolders().firstOrNull { it.id == folderId } ?: return
        saveFolder(folder.copy(voicingOrder = orderedKeys))
    }

    // ── Folder serialization ────────────────────────────────────────

    private fun serializeFolder(folder: FavoriteFolder): String {
        val orderStr = folder.voicingOrder.joinToString(";")
        return "${folder.id}|||${folder.name}|||${folder.createdAt}|||$orderStr"
    }

    private fun deserializeFolder(value: String?): FavoriteFolder? {
        if (value == null) return null
        val parts = value.split("|||")
        if (parts.size < 3) return null
        return try {
            val order = if (parts.size >= 4 && parts[3].isNotEmpty()) {
                parts[3].split(";")
            } else {
                emptyList()
            }
            FavoriteFolder(
                id = parts[0],
                name = parts[1],
                createdAt = parts[2].toLong(),
                voicingOrder = order,
            )
        } catch (_: Exception) { null }
    }

    // ── Voicing serialization ───────────────────────────────────────

    private fun serialize(voicing: FavoriteVoicing): String {
        val folderStr = voicing.folderIds.joinToString(";")
        return "${voicing.rootPitchClass}|${voicing.chordSymbol}|${voicing.frets.joinToString(",")}|${voicing.addedAt}|$folderStr"
    }

    private fun deserialize(value: String?): FavoriteVoicing? {
        if (value == null) return null
        val parts = value.split("|")
        if (parts.size < 4) return null
        return try {
            // Backward compat: old format had single folderId, new format has semicolon-joined list
            val folderField = parts.getOrNull(4)?.ifEmpty { null }
            val folderIds = if (folderField != null) {
                if (";" in folderField) {
                    folderField.split(";")
                } else {
                    listOf(folderField) // single old-format folderId
                }
            } else {
                emptyList()
            }
            FavoriteVoicing(
                rootPitchClass = parts[0].toInt(),
                chordSymbol = parts[1],
                frets = parts[2].split(",").map { it.toInt() },
                addedAt = parts[3].toLong(),
                folderIds = folderIds,
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Merges the given list of favorites into local storage using merge-by-ID.
     * Keeps the newer version (by addedAt) when a voicing exists in both.
     */
    fun importAll(favorites: List<FavoriteVoicing>) {
        val merged = mergeKeepExisting(
            existing = getAll(),
            incoming = favorites,
            idOf = { it.key },
        )
        val editor = prefs.edit()
        for (voicing in merged) {
            editor.putString(voicing.key, serialize(voicing))
        }
        editor.apply()
    }

    /**
     * Merges the given list of folders into local storage.
     * Only adds folders that are not already present.
     */
    fun importFolders(folders: List<FavoriteFolder>) {
        val merged = mergeKeepExisting(
            existing = getAllFolders(),
            incoming = folders,
            idOf = { it.id },
        )
        val editor = folderPrefs.edit()
        for (folder in merged) {
            editor.putString(folder.id, serializeFolder(folder))
        }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "chord_favorites"
        private const val FOLDER_PREFS_NAME = "favorite_folders"
    }
}
