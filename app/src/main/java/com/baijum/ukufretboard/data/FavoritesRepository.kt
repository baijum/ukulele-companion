package com.baijum.ukufretboard.data

import android.content.Context
import android.content.SharedPreferences
import com.baijum.ukufretboard.domain.mergeKeepExisting
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Repository for persisting favorite voicings using SharedPreferences.
 *
 * Stores favorites as a single JSON array under [KEY_FAVORITES] and folders
 * as a single JSON array under [KEY_FOLDERS]. On first access, transparently
 * migrates from the legacy per-entry pipe-delimited format.
 */
class FavoritesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val folderPrefs: SharedPreferences =
        context.getSharedPreferences(FOLDER_PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ── Voicing CRUD ─────────────────────────────────────────────────

    /**
     * Returns all saved favorites, sorted by time added (newest first).
     */
    fun getAll(): List<FavoriteVoicing> {
        val raw = prefs.getString(KEY_FAVORITES, null)
        if (raw != null) {
            return tryParseVoicings(raw)
                ?: tryParseVoicings(prefs.getString(BACKUP_KEY_FAVORITES, null))
                ?: migrateLegacyVoicings()
        }
        return migrateLegacyVoicings()
    }

    /**
     * Adds a voicing to favorites. No-op if already saved.
     */
    fun add(voicing: FavoriteVoicing) {
        if (!contains(voicing)) {
            val items = getAll().toMutableList()
            items.add(0, voicing)
            persistVoicings(items)
        }
    }

    /**
     * Removes a voicing from favorites. Also removes its key from any folder ordering.
     */
    fun remove(voicing: FavoriteVoicing) {
        persistVoicings(getAll().filter { it.key != voicing.key })
        getAllFolders().forEach { folder ->
            if (voicing.key in folder.voicingOrder) {
                val updated = folder.copy(voicingOrder = folder.voicingOrder - voicing.key)
                saveFolderInternal(updated, getAllFolders().map { if (it.id == updated.id) updated else it })
            }
        }
    }

    /**
     * Checks if a voicing is already in favorites.
     */
    fun contains(voicing: FavoriteVoicing): Boolean =
        getAll().any { it.key == voicing.key }

    /**
     * Checks if a voicing with the given root, symbol, and frets is in favorites.
     */
    fun contains(rootPitchClass: Int, chordSymbol: String, frets: List<Int>): Boolean {
        val key = "$rootPitchClass|$chordSymbol|${frets.joinToString(",")}"
        return getAll().any { it.key == key }
    }

    /**
     * Updates the folder assignments for a voicing and keeps folder ordering in sync.
     */
    fun setFolders(voicing: FavoriteVoicing, folderIds: List<String>) {
        val oldFolderIds = voicing.folderIds.toSet()
        val newFolderIds = folderIds.toSet()
        val updated = voicing.copy(folderIds = folderIds)
        val items = getAll().toMutableList()
        val index = items.indexOfFirst { it.key == updated.key }
        if (index >= 0) items[index] = updated
        persistVoicings(items)

        val addedTo = newFolderIds - oldFolderIds
        val removedFrom = oldFolderIds - newFolderIds
        val allFolders = getAllFolders().toMutableList()
        var foldersChanged = false
        allFolders.forEachIndexed { i, folder ->
            when (folder.id) {
                in addedTo -> {
                    if (voicing.key !in folder.voicingOrder) {
                        allFolders[i] = folder.copy(voicingOrder = folder.voicingOrder + voicing.key)
                        foldersChanged = true
                    }
                }
                in removedFrom -> {
                    if (voicing.key in folder.voicingOrder) {
                        allFolders[i] = folder.copy(voicingOrder = folder.voicingOrder - voicing.key)
                        foldersChanged = true
                    }
                }
            }
        }
        if (foldersChanged) persistFolders(allFolders)
    }

    // ── Folder management ───────────────────────────────────────────

    fun getAllFolders(): List<FavoriteFolder> {
        val raw = folderPrefs.getString(KEY_FOLDERS, null)
        if (raw != null) {
            return tryParseFolders(raw)
                ?: tryParseFolders(folderPrefs.getString(BACKUP_KEY_FOLDERS, null))
                ?: migrateLegacyFolders()
        }
        return migrateLegacyFolders()
    }

    fun saveFolder(folder: FavoriteFolder) {
        val items = getAllFolders().toMutableList()
        val index = items.indexOfFirst { it.id == folder.id }
        if (index >= 0) items[index] = folder else items.add(folder)
        persistFolders(items)
    }

    fun renameFolder(folderId: String, newName: String) {
        val folders = getAllFolders().toMutableList()
        val index = folders.indexOfFirst { it.id == folderId }
        if (index < 0) return
        folders[index] = folders[index].copy(name = newName)
        persistFolders(folders)
    }

    fun deleteFolder(folderId: String) {
        persistFolders(getAllFolders().filter { it.id != folderId })
        val voicings = getAll()
        val updated = voicings.map { v ->
            if (folderId in v.folderIds) v.copy(folderIds = v.folderIds - folderId) else v
        }
        if (updated != voicings) persistVoicings(updated)
    }

    /**
     * Overwrites the voicing ordering for a folder.
     */
    fun reorderVoicingsInFolder(folderId: String, orderedKeys: List<String>) {
        val folders = getAllFolders().toMutableList()
        val index = folders.indexOfFirst { it.id == folderId }
        if (index < 0) return
        folders[index] = folders[index].copy(voicingOrder = orderedKeys)
        persistFolders(folders)
    }

    // ── Import (backup/restore) ─────────────────────────────────────

    /**
     * Merges the given list of favorites into local storage using merge-by-ID.
     * Keeps the existing version when a voicing exists in both.
     */
    fun importAll(favorites: List<FavoriteVoicing>) {
        val merged = mergeKeepExisting(
            existing = getAll(),
            incoming = favorites,
            idOf = { it.key },
        )
        persistVoicings(merged)
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
        persistFolders(merged)
    }

    // ── JSON persistence ────────────────────────────────────────────

    private fun persistVoicings(items: List<FavoriteVoicing>) {
        val raw = json.encodeToString(ListSerializer(FavoriteVoicing.serializer()), items)
        val previousGood = prefs.getString(KEY_FAVORITES, null)
        prefs.edit()
            .putString(BACKUP_KEY_FAVORITES, previousGood ?: raw)
            .putString(KEY_FAVORITES, raw)
            .apply()
    }

    private fun persistFolders(items: List<FavoriteFolder>) {
        val raw = json.encodeToString(ListSerializer(FavoriteFolder.serializer()), items)
        val previousGood = folderPrefs.getString(KEY_FOLDERS, null)
        folderPrefs.edit()
            .putString(BACKUP_KEY_FOLDERS, previousGood ?: raw)
            .putString(KEY_FOLDERS, raw)
            .apply()
    }

    private fun tryParseVoicings(raw: String?): List<FavoriteVoicing>? {
        if (raw == null) return null
        return try {
            json.decodeFromString(ListSerializer(FavoriteVoicing.serializer()), raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun tryParseFolders(raw: String?): List<FavoriteFolder>? {
        if (raw == null) return null
        return try {
            json.decodeFromString(ListSerializer(FavoriteFolder.serializer()), raw)
        } catch (_: Exception) {
            null
        }
    }

    // ── Legacy pipe-delimited migration ─────────────────────────────

    private fun migrateLegacyVoicings(): List<FavoriteVoicing> {
        val entries = prefs.all.entries
            .filter { it.key != KEY_FAVORITES && it.key != BACKUP_KEY_FAVORITES }
            .mapNotNull { (_, value) -> deserializeLegacyVoicing(value as? String) }
            .sortedByDescending { it.addedAt }
        if (entries.isNotEmpty()) {
            persistVoicings(entries)
            val editor = prefs.edit()
            prefs.all.keys
                .filter { it != KEY_FAVORITES && it != BACKUP_KEY_FAVORITES }
                .forEach { editor.remove(it) }
            editor.apply()
        }
        return entries
    }

    private fun migrateLegacyFolders(): List<FavoriteFolder> {
        val entries = folderPrefs.all.entries
            .filter { it.key != KEY_FOLDERS && it.key != BACKUP_KEY_FOLDERS }
            .mapNotNull { (_, value) -> deserializeLegacyFolder(value as? String) }
            .sortedBy { it.name }
        if (entries.isNotEmpty()) {
            persistFolders(entries)
            val editor = folderPrefs.edit()
            folderPrefs.all.keys
                .filter { it != KEY_FOLDERS && it != BACKUP_KEY_FOLDERS }
                .forEach { editor.remove(it) }
            editor.apply()
        }
        return entries
    }

    private fun deserializeLegacyVoicing(value: String?): FavoriteVoicing? {
        if (value == null) return null
        val parts = value.split("|")
        if (parts.size < 4) return null
        return try {
            val folderField = parts.getOrNull(4)?.ifEmpty { null }
            val folderIds = if (folderField != null) {
                if (";" in folderField) {
                    folderField.split(";")
                } else {
                    listOf(folderField)
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

    private fun deserializeLegacyFolder(value: String?): FavoriteFolder? {
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
        } catch (_: Exception) {
            null
        }
    }

    private fun saveFolderInternal(folder: FavoriteFolder, allFolders: List<FavoriteFolder>) {
        persistFolders(allFolders)
    }

    companion object {
        private const val PREFS_NAME = "chord_favorites"
        private const val KEY_FAVORITES = "favorites_json"
        private const val BACKUP_KEY_FAVORITES = "favorites_json_backup"
        private const val FOLDER_PREFS_NAME = "favorite_folders"
        private const val KEY_FOLDERS = "folders_json"
        private const val BACKUP_KEY_FOLDERS = "folders_json_backup"
    }
}
