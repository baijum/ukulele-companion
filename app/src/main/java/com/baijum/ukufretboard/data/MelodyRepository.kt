package com.baijum.ukufretboard.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Repository for persisting melodies using SharedPreferences.
 *
 * Each melody is serialized as a pipe-delimited string with individual notes
 * encoded as colon-delimited fields.
 */
class MelodyRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): List<Melody> {
        return prefs.all.entries
            .mapNotNull { (_, value) -> deserialize(value as? String) }
            .sortedByDescending { it.createdAt }
    }

    fun get(id: String): Melody? {
        val value = prefs.getString(id, null)
        return deserialize(value)
    }

    fun save(melody: Melody) {
        prefs.edit().putString(melody.id, serialize(melody)).apply()
    }

    fun delete(id: String) {
        prefs.edit().remove(id).apply()
    }

    /**
     * Merges the given list of melodies into local storage.
     * For each melody, if it doesn't exist locally it is added.
     * If it already exists, the newer version (by createdAt) wins.
     */
    fun importAll(melodies: List<Melody>) {
        val editor = prefs.edit()
        for (melody in melodies) {
            val existing = get(melody.id)
            if (existing == null || melody.createdAt > existing.createdAt) {
                editor.putString(melody.id, serialize(melody))
            }
        }
        editor.apply()
    }

    private fun serialize(melody: Melody): String {
        val notesPart = melody.notes.joinToString(NOTE_LIST_SEPARATOR) { note ->
            listOf(
                note.pitchClass?.toString() ?: NULL_MARKER,
                note.octave.toString(),
                note.duration.name,
                note.stringIndex?.toString() ?: NULL_MARKER,
                note.fret?.toString() ?: NULL_MARKER,
            ).joinToString(NOTE_FIELD_SEPARATOR)
        }
        return listOf(
            melody.id,
            melody.name.replace("|", "\\|"),
            notesPart,
            melody.bpm.toString(),
            melody.createdAt.toString(),
        ).joinToString(SEPARATOR)
    }

    private fun deserialize(value: String?): Melody? {
        if (value == null) return null
        val parts = value.split(SEPARATOR)
        if (parts.size < 5) return null
        return try {
            val notes = if (parts[2].isBlank()) {
                emptyList()
            } else {
                parts[2].split(NOTE_LIST_SEPARATOR).map { noteStr ->
                    val fields = noteStr.split(NOTE_FIELD_SEPARATOR)
                    MelodyNote(
                        pitchClass = fields[0].takeIf { it != NULL_MARKER }?.toInt(),
                        octave = fields[1].toInt(),
                        duration = NoteDuration.valueOf(fields[2]),
                        stringIndex = fields.getOrNull(3)?.takeIf { it != NULL_MARKER }?.toInt(),
                        fret = fields.getOrNull(4)?.takeIf { it != NULL_MARKER }?.toInt(),
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
        private const val SEPARATOR = "|||"
        private const val NOTE_LIST_SEPARATOR = ";;"
        private const val NOTE_FIELD_SEPARATOR = ":"
        private const val NULL_MARKER = "_"
    }
}
