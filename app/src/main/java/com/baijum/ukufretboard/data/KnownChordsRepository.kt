package com.baijum.ukufretboard.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Repository for persisting the set of chord names the user explicitly
 * marks as "known" — independent of favorites.
 *
 * Stored as a single comma-separated string in SharedPreferences.
 */
class KnownChordsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns all known chord names.
     */
    fun getAll(): Set<String> {
        val raw = prefs.getString(KEY_CHORDS, null) ?: return emptySet()
        return raw.split(SEPARATOR).filter { it.isNotBlank() }.toSet()
    }

    /**
     * Adds a chord name to the known set.
     */
    fun add(chordName: String) {
        val current = getAll().toMutableSet()
        current.add(chordName)
        save(current)
    }

    /**
     * Removes a chord name from the known set.
     */
    fun remove(chordName: String) {
        val current = getAll().toMutableSet()
        current.remove(chordName)
        save(current)
    }

    /**
     * Checks if a chord name is in the known set.
     */
    fun contains(chordName: String): Boolean = chordName in getAll()

    /**
     * Replaces the entire known set with the given chords.
     */
    fun setAll(chords: Set<String>) {
        save(chords)
    }

    /**
     * Merges the given set into local storage (union merge).
     */
    fun importAll(chords: Set<String>) {
        val current = getAll().toMutableSet()
        current.addAll(chords)
        save(current)
    }

    /**
     * Returns the raw comma-separated string for backup.
     */
    fun exportRaw(): String = prefs.getString(KEY_CHORDS, "") ?: ""

    private fun save(chords: Set<String>) {
        prefs.edit().putString(KEY_CHORDS, chords.joinToString(SEPARATOR)).apply()
    }

    companion object {
        private const val PREFS_NAME = "known_chords"
        private const val KEY_CHORDS = "chord_names"
        private const val SEPARATOR = ","
    }
}
