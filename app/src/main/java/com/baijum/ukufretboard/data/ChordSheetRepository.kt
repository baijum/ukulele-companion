package com.baijum.ukufretboard.data

import android.content.Context
import com.baijum.ukufretboard.domain.mergeNewerWins

/**
 * Repository for persisting chord sheets using SharedPreferences.
 *
 * Stores the full list as a single JSON array under [KEY_SHEETS].
 * On first access, migrates from the legacy pipe-delimited per-entry format.
 */
class ChordSheetRepository(context: Context) : JsonListRepository<ChordSheet>(
    context,
    PREFS_NAME,
    KEY_SHEETS,
    ChordSheet.serializer(),
) {
    override fun entityId(item: ChordSheet) = item.id
    override fun entityTimestamp(item: ChordSheet) = item.updatedAt

    override fun importAll(items: List<ChordSheet>) {
        val merged = mergeNewerWins(getAll(), items, ::entityId, ::entityTimestamp)
        persist(merged)
    }

    override fun getAll(): List<ChordSheet> {
        val raw = prefs.getString(KEY_SHEETS, null)
        if (raw != null) {
            return tryParse(raw)
                ?: tryParse(prefs.getString(backupKey, null))
                ?: migrateLegacyPipeEntries()
        }
        return migrateLegacyPipeEntries()
    }

    /**
     * Returns the set of all distinct labels used across every saved chord sheet.
     */
    fun getAllLabels(): Set<String> =
        getAll().flatMap { it.labels }.toSortedSet(String.CASE_INSENSITIVE_ORDER)

    fun get(id: String): ChordSheet? = getAll().firstOrNull { it.id == id }

    private fun migrateLegacyPipeEntries(): List<ChordSheet> {
        val entries = prefs.all.entries
            .filter { it.key !in ownKeys }
            .mapNotNull { (_, value) -> deserializeLegacy(value as? String) }
            .sortedByDescending { it.updatedAt }
        if (entries.isNotEmpty()) {
            persist(entries)
            val editor = prefs.edit()
            prefs.all.keys.filter { it !in ownKeys }.forEach { editor.remove(it) }
            editor.apply()
        }
        return entries
    }

    private fun deserializeLegacy(value: String?): ChordSheet? {
        if (value == null) return null
        val parts = value.split(LEGACY_SEPARATOR)
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
                labels = deserializeLegacyLabels(parts.getOrNull(9)),
                subtitle = parts.getOrNull(10)?.replace("\\|", "|") ?: "",
                viewCount = parts.getOrNull(11)?.toIntOrNull() ?: 0,
                lastViewedAt = parts.getOrNull(12)?.toLongOrNull() ?: 0L,
                totalViewTimeMs = parts.getOrNull(13)?.toLongOrNull() ?: 0L,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun deserializeLegacyLabels(raw: String?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (ch in raw) {
            if (escaped) {
                current.append(ch)
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else if (ch == ',') {
                result.add(current.toString())
                current.clear()
            } else {
                current.append(ch)
            }
        }
        result.add(current.toString())
        return result.filter { it.isNotEmpty() }
    }

    companion object {
        private const val PREFS_NAME = "chord_sheets"
        private const val KEY_SHEETS = "sheets_json"
        private const val LEGACY_SEPARATOR = "|||"
    }
}
