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

    fun moveSong(setlistId: String, fromIndex: Int, toIndex: Int) {
        val setlist = _setlists.value.find { it.id == setlistId } ?: return
        val songs = setlist.songIds.toMutableList()
        if (fromIndex !in songs.indices || toIndex !in songs.indices) return
        val item = songs.removeAt(fromIndex)
        songs.add(toIndex, item)
        val updated = setlist.copy(
            songIds = songs,
            updatedAt = System.currentTimeMillis(),
        )
        repository.save(updated)
        if (_currentSetlist.value?.id == setlistId) _currentSetlist.value = updated
        refresh()
    }
}
