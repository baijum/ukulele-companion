package com.baijum.ukufretboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.baijum.ukufretboard.data.FavoriteFolder
import com.baijum.ukufretboard.data.FavoriteVoicing
import com.baijum.ukufretboard.data.FavoritesRepository
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.domain.Note
import com.baijum.ukufretboard.domain.UkuleleString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel managing the chord favorites list.
 *
 * Uses [AndroidViewModel] to access the application context for
 * SharedPreferences-based persistence.
 */
class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FavoritesRepository(application)

    private val _favorites = MutableStateFlow<List<FavoriteVoicing>>(emptyList())
    private val _folders = MutableStateFlow<List<FavoriteFolder>>(emptyList())

    /** Observable list of favorite voicings. */
    val favorites: StateFlow<List<FavoriteVoicing>> = _favorites.asStateFlow()

    /** Observable list of folders. */
    val folders: StateFlow<List<FavoriteFolder>> = _folders.asStateFlow()

    init {
        refresh()
    }

    /**
     * Adds a voicing to favorites (unfiled).
     */
    fun addFavorite(rootPitchClass: Int, chordSymbol: String, frets: List<Int>) {
        val voicing = FavoriteVoicing(
            rootPitchClass = rootPitchClass,
            chordSymbol = chordSymbol,
            frets = frets,
        )
        repository.add(voicing)
        refresh()
    }

    /**
     * Removes a voicing from favorites.
     */
    fun removeFavorite(favorite: FavoriteVoicing) {
        repository.remove(favorite)
        refresh()
    }

    /**
     * Removes a voicing identified by root, symbol, and frets from favorites.
     */
    fun removeFavorite(rootPitchClass: Int, chordSymbol: String, frets: List<Int>) {
        val key = "$rootPitchClass|$chordSymbol|${frets.joinToString(",")}"
        val existing = _favorites.value.firstOrNull { it.key == key }
        if (existing != null) {
            repository.remove(existing)
            refresh()
        }
    }

    /**
     * Checks if a voicing is in favorites.
     */
    fun isFavorite(rootPitchClass: Int, chordSymbol: String, frets: List<Int>): Boolean =
        repository.contains(rootPitchClass, chordSymbol, frets)

    /**
     * Returns the folder IDs for a voicing identified by root, symbol, and frets.
     */
    fun getFolderIdsForVoicing(rootPitchClass: Int, chordSymbol: String, frets: List<Int>): List<String> {
        val key = "$rootPitchClass|$chordSymbol|${frets.joinToString(",")}"
        return _favorites.value.firstOrNull { it.key == key }?.folderIds ?: emptyList()
    }

    /**
     * Saves a voicing to favorites with the given folder assignments.
     * Adds the voicing if not already present, then sets its folders.
     */
    fun saveFavoriteToFolders(
        rootPitchClass: Int,
        chordSymbol: String,
        frets: List<Int>,
        folderIds: List<String>,
    ) {
        val key = "$rootPitchClass|$chordSymbol|${frets.joinToString(",")}"
        val existing = _favorites.value.firstOrNull { it.key == key }
        if (existing == null) {
            // Add as new favorite, then set folders
            val voicing = FavoriteVoicing(
                rootPitchClass = rootPitchClass,
                chordSymbol = chordSymbol,
                frets = frets,
                folderIds = folderIds,
            )
            repository.add(voicing)
            // After add, set folders to update ordering
            val added = repository.getAll().firstOrNull { it.key == key }
            if (added != null) repository.setFolders(added, folderIds)
        } else {
            repository.setFolders(existing, folderIds)
        }
        refresh()
    }

    /**
     * Converts a [FavoriteVoicing] to a [ChordVoicing] for display and application.
     */
    fun toChordVoicing(favorite: FavoriteVoicing, tuning: List<UkuleleString>): ChordVoicing {
        val notes = favorite.frets.mapIndexed { i, fret ->
            if (fret == ChordVoicing.MUTED) null
            else {
                val pc = (tuning[i].openPitchClass + fret) % Notes.PITCH_CLASS_COUNT
                Note(pitchClass = pc, name = Notes.pitchClassToName(pc))
            }
        }
        val frettedPositions = favorite.frets.filter { it > 0 }
        return ChordVoicing(
            frets = favorite.frets,
            notes = notes,
            minFret = frettedPositions.minOrNull() ?: 0,
            maxFret = frettedPositions.maxOrNull() ?: 0,
        )
    }

    // ── Folder management ────────────────────────────────────────

    fun createFolder(name: String) {
        repository.saveFolder(FavoriteFolder(name = name))
        refresh()
    }

    fun renameFolder(folderId: String, newName: String) {
        repository.renameFolder(folderId, newName)
        refresh()
    }

    fun deleteFolder(folderId: String) {
        repository.deleteFolder(folderId)
        refresh()
    }

    /**
     * Updates the folder assignments for a voicing.
     */
    fun setFolders(favorite: FavoriteVoicing, folderIds: List<String>) {
        repository.setFolders(favorite, folderIds)
        refresh()
    }

    /**
     * Saves the reordered voicing keys for a specific folder.
     */
    fun reorderInFolder(folderId: String, orderedKeys: List<String>) {
        repository.reorderVoicingsInFolder(folderId, orderedKeys)
        refresh()
    }

    /**
     * Returns voicings in the given folder, sorted by the folder's voicing order.
     * Voicings not in the order list are appended at the end (newest first).
     */
    fun getOrderedVoicings(folderId: String): List<FavoriteVoicing> {
        val folder = _folders.value.firstOrNull { it.id == folderId } ?: return emptyList()
        val inFolder = _favorites.value.filter { folderId in it.folderIds }
        val orderMap = folder.voicingOrder.withIndex().associate { (idx, key) -> key to idx }
        return inFolder.sortedWith(compareBy<FavoriteVoicing> {
            orderMap[it.key] ?: Int.MAX_VALUE
        }.thenByDescending { it.addedAt })
    }

    private fun refresh() {
        _favorites.value = repository.getAll()
        _folders.value = repository.getAllFolders()
    }
}
