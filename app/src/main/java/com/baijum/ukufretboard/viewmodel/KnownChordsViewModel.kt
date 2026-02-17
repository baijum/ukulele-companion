package com.baijum.ukufretboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.baijum.ukufretboard.data.KnownChordsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel managing the set of chord names the user explicitly
 * marks as "known" — independent of favorites.
 */
class KnownChordsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KnownChordsRepository(application)

    private val _knownChords = MutableStateFlow<Set<String>>(emptySet())

    /** Observable set of explicitly marked known chord names. */
    val knownChords: StateFlow<Set<String>> = _knownChords.asStateFlow()

    init {
        refresh()
    }

    /** Adds a chord name to the known set. */
    fun addChord(name: String) {
        repository.add(name)
        refresh()
    }

    /** Removes a chord name from the known set. */
    fun removeChord(name: String) {
        repository.remove(name)
        refresh()
    }

    /** Toggles whether a chord is in the known set. */
    fun toggleChord(name: String) {
        if (repository.contains(name)) {
            repository.remove(name)
        } else {
            repository.add(name)
        }
        refresh()
    }

    private fun refresh() {
        _knownChords.value = repository.getAll()
    }
}
