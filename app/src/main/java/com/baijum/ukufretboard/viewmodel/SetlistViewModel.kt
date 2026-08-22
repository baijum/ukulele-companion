package com.baijum.ukufretboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.baijum.ukufretboard.data.Setlist
import com.baijum.ukufretboard.data.SetlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SetlistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SetlistRepository(application)

    private val _setlists = MutableStateFlow<List<Setlist>>(emptyList())
    val setlists: StateFlow<List<Setlist>> = _setlists.asStateFlow()

    private val _currentSetlist = MutableStateFlow<Setlist?>(null)
    val currentSetlist: StateFlow<Setlist?> = _currentSetlist.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _setlists.value = repository.getAll()
    }

    fun create(name: String) {
        val setlist = Setlist(name = name)
        repository.save(setlist)
        refresh()
    }

    fun rename(id: String, newName: String) {
        val setlist = _setlists.value.find { it.id == id } ?: return
        val updated = setlist.copy(
            name = newName,
            updatedAt = System.currentTimeMillis(),
        )
        repository.save(updated)
        if (_currentSetlist.value?.id == id) _currentSetlist.value = updated
        refresh()
    }

    fun delete(id: String) {
        repository.delete(id)
        if (_currentSetlist.value?.id == id) _currentSetlist.value = null
        refresh()
    }

    fun open(setlist: Setlist) {
        _currentSetlist.value = setlist
    }

    fun close() {
        _currentSetlist.value = null
    }

    fun addSong(setlistId: String, songId: String) {
        val setlist = _setlists.value.find { it.id == setlistId } ?: return
        if (songId in setlist.songIds) return
        val updated = setlist.copy(
            songIds = setlist.songIds + songId,
            updatedAt = System.currentTimeMillis(),
        )
        repository.save(updated)
        if (_currentSetlist.value?.id == setlistId) _currentSetlist.value = updated
        refresh()
    }

    fun removeSong(setlistId: String, songId: String) {
        val setlist = _setlists.value.find { it.id == setlistId } ?: return
        val updated = setlist.copy(
            songIds = setlist.songIds.filter { it != songId },
            updatedAt = System.currentTimeMillis(),
        )
        repository.save(updated)
        if (_currentSetlist.value?.id == setlistId) _currentSetlist.value = updated
        refresh()
    }

    /**
     * Moves [songId] by [offset] positions within the persisted song order
     * ([offset] of -1 moves it up one, +1 down one).
     *
     * The move is keyed by song ID rather than by display index so it cannot be
     * desynced from the stored `songIds` array — e.g. when a Songbook search
     * filter hides some songs and the visible list's indices no longer match the
     * persisted ones (issue #572).
     */
    fun moveSong(setlistId: String, songId: String, offset: Int) {
        if (offset == 0) return
        val setlist = _setlists.value.find { it.id == setlistId } ?: return
        val songs = setlist.songIds.toMutableList()
        val currentIndex = songs.indexOf(songId)
        if (currentIndex < 0) return
        val targetIndex = currentIndex + offset
        if (targetIndex !in songs.indices) return
        val item = songs.removeAt(currentIndex)
        songs.add(targetIndex, item)
        val updated = setlist.copy(
            songIds = songs,
            updatedAt = System.currentTimeMillis(),
        )
        repository.save(updated)
        if (_currentSetlist.value?.id == setlistId) {
            _currentSetlist.value = updated
        }
        refresh()
    }

    /**
     * Strips deleted library songs out of every setlist — persisted and in
     * memory, including the open one — so a dead ID can neither resurface in a
     * later save nor silently vanish from the rendered list (issue #594).
     */
    fun purgeDeletedSongs(deletedSongIds: Collection<String>) {
        if (deletedSongIds.isEmpty()) return
        var updatedCurrent: Setlist? = null
        _setlists.value = _setlists.value.map { setlist ->
            if (setlist.songIds.none { it in deletedSongIds }) {
                setlist
            } else {
                val updated = setlist.copy(
                    songIds = setlist.songIds.filterNot { it in deletedSongIds },
                    updatedAt = System.currentTimeMillis(),
                )
                repository.save(updated)
                if (_currentSetlist.value?.id == updated.id) {
                    updatedCurrent = updated
                }
                updated
            }
        }
        updatedCurrent?.let { _currentSetlist.value = it }
    }
}
