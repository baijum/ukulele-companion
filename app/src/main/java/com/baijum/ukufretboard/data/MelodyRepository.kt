package com.baijum.ukufretboard.data

import android.content.Context
import android.util.Log
import com.baijum.ukufretboard.domain.mergeNewerWins

/**
 * Repository for persisting melodies using SharedPreferences.
 *
 * Stores the full list as a single JSON array under [KEY_MELODIES], which puts
 * melodies on the same backup-and-quarantine scheme as every other list store.
 * On first access, migrates from the legacy pipe-delimited per-entry format.
 */
class MelodyRepository(context: Context) : JsonListRepository<Melody>(
    context,
    PREFS_NAME,
    KEY_MELODIES,
    Melody.serializer(),
) {
    override fun entityId(item: Melody) = item.id
    override fun entityTimestamp(item: Melody) = item.createdAt

    /**
     * Newest first, the order this store has always answered in and the order
     * the saved-melody list is shown in.
     */
    override fun getAll(): List<Melody> =
        readOrElse(::migrateLegacyPipeEntries).sortedByDescending { it.createdAt }

    fun get(id: String): Melody? = getAll().firstOrNull { it.id == id }

    /**
     * Merges the given list of melodies into local storage using merge-by-ID.
     * Keeps the newer version (by createdAt) when a melody exists in both.
     */
    override fun importAll(items: List<Melody>) {
        persist(mergeNewerWins(getAll(), items, ::entityId, ::entityTimestamp))
    }

    /**
     * Folds the legacy one-melody-per-key entries into the JSON list.
     *
     * Unlike the other pipe-format migrations this one sweeps only the keys it
     * actually consumed. Under the per-key layout a melody whose string was
     * damaged cost only itself: nothing rewrote the whole file, so its bytes
     * sat under their own key indefinitely -- invisible, but recoverable
     * (#565). Removing them here because they failed to parse would turn this
     * fix into the loss it is meant to prevent, so they stay where they are and
     * the log line says so.
     */
    private fun migrateLegacyPipeEntries(): List<Melody> {
        val legacy = prefs.all.entries.filter { it.key !in ownKeys }
        if (legacy.isEmpty()) return emptyList()

        val migrated = mutableListOf<Melody>()
        val consumed = mutableListOf<String>()
        val unreadable = mutableListOf<String>()
        for ((key, value) in legacy) {
            val melody = deserializeLegacy(value as? String)
            if (melody == null) {
                unreadable += key
            } else {
                migrated += melody
                consumed += key
            }
        }
        if (unreadable.isNotEmpty()) {
            Log.e(TAG, "unreadable melody entries left in place: ${unreadable.joinToString()}")
        }

        // Persisted even when nothing was readable: it is what marks the
        // migration done, and without it every read would rescan and re-log.
        persist(migrated)
        val editor = prefs.edit()
        consumed.forEach { editor.remove(it) }
        editor.apply()
        return migrated
    }

    private fun deserializeLegacy(value: String?): Melody? {
        if (value == null) return null
        val parts = value.split(LEGACY_SEPARATOR)
        if (parts.size < 5) return null
        return try {
            val notes = if (parts[2].isBlank()) {
                emptyList()
            } else {
                parts[2].split(LEGACY_NOTE_LIST_SEPARATOR).map { noteStr ->
                    val fields = noteStr.split(LEGACY_NOTE_FIELD_SEPARATOR)
                    MelodyNote(
                        pitchClass = fields[0].takeIf { it != LEGACY_NULL_MARKER }?.toInt(),
                        octave = fields[1].toInt(),
                        duration = NoteDuration.valueOf(fields[2]),
                        stringIndex = fields.getOrNull(3)?.takeIf { it != LEGACY_NULL_MARKER }?.toInt(),
                        fret = fields.getOrNull(4)?.takeIf { it != LEGACY_NULL_MARKER }?.toInt(),
                    )
                }
            }
            Melody(
                id = parts[0],
                name = parts[1].replace("\\|", "|"),
                notes = notes,
                bpm = parts[3].toInt(),
                createdAt = parts[4].toLong(),
            )
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "melodies"
        private const val KEY_MELODIES = "melodies_json"
        private const val LEGACY_SEPARATOR = "|||"
        private const val LEGACY_NOTE_LIST_SEPARATOR = ";;"
        private const val LEGACY_NOTE_FIELD_SEPARATOR = ":"
        private const val LEGACY_NULL_MARKER = "_"
        private const val TAG = "MelodyRepository"
    }
}
