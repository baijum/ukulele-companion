package com.baijum.ukufretboard.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Repository for persisting chord sheets using SharedPreferences.
 *
 * Each sheet is serialized as a pipe-delimited string.
 */
class ChordSheetRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): List<ChordSheet> {
        return prefs.all.entries
            .mapNotNull { (_, value) -> deserialize(value as? String) }
            .sortedByDescending { it.updatedAt }
    }

    fun get(id: String): ChordSheet? {
        val value = prefs.getString(id, null)
        return deserialize(value)
    }

    fun save(sheet: ChordSheet) {
        prefs.edit().putString(sheet.id, serialize(sheet)).apply()
    }

    fun delete(id: String) {
        prefs.edit().remove(id).apply()
    }

    private fun serialize(sheet: ChordSheet): String =
        listOf(
            sheet.id,
            sheet.title.replace("|", "\\|"),
            sheet.artist.replace("|", "\\|"),
            sheet.content.replace("|", "\\|"),
            sheet.createdAt.toString(),
            sheet.updatedAt.toString(),
            sheet.key.replace("|", "\\|"),
            sheet.capo.toString(),
            sheet.strumPatternName.replace("|", "\\|"),
        ).joinToString(SEPARATOR)

    private fun deserialize(value: String?): ChordSheet? {
        if (value == null) return null
        val parts = value.split(SEPARATOR)
        if (parts.size < 6) return null
        return try {
            ChordSheet(
                id = parts[0],
                title = parts[1].replace("\\|", "|"),
                artist = parts[2].replace("\\|", "|"),
                content = parts[3].replace("\\|", "|"),
                createdAt = parts[4].toLong(),
                updatedAt = parts[5].toLong(),
                key = parts.getOrNull(6)?.replace("\\|", "|") ?: "",
                capo = parts.getOrNull(7)?.toIntOrNull() ?: 0,
                strumPatternName = parts.getOrNull(8)?.replace("\\|", "|") ?: "",
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Merges the given list of chord sheets into local storage.
     * For each sheet, if it doesn't exist locally it is added.
     * If it already exists, the version with the latest [ChordSheet.updatedAt] wins.
     */
    fun importAll(sheets: List<ChordSheet>) {
        val editor = prefs.edit()
        for (sheet in sheets) {
            val existing = get(sheet.id)
            if (existing == null || sheet.updatedAt > existing.updatedAt) {
                editor.putString(sheet.id, serialize(sheet))
            }
        }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "chord_sheets"
        private const val SEPARATOR = "|||"
    }
}
