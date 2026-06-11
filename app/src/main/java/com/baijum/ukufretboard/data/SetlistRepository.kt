package com.baijum.ukufretboard.data

import android.content.Context
import com.baijum.ukufretboard.domain.mergeNewerWins
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository for persisting setlists using SharedPreferences.
 *
 * Stores the full list as a single JSON array under [KEY_SETLISTS].
 * Migrates from the legacy org.json format to kotlinx.serialization on first access.
 */
class SetlistRepository(context: Context) : JsonListRepository<Setlist>(
    context,
    PREFS_NAME,
    KEY_SETLISTS,
    Setlist.serializer(),
) {
    override fun entityId(item: Setlist) = item.id
    override fun entityTimestamp(item: Setlist) = item.updatedAt

    override fun importAll(items: List<Setlist>) {
        val merged = mergeNewerWins(getAll(), items, ::entityId, ::entityTimestamp)
        persist(merged)
    }

    override fun getAll(): List<Setlist> {
        val raw = prefs.getString(KEY_SETLISTS, null) ?: return emptyList()
        return try {
            json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(Setlist.serializer()),
                raw,
            )
        } catch (_: Exception) {
            migrateLegacyOrgJson(raw)
        }
    }

    private fun migrateLegacyOrgJson(raw: String): List<Setlist> {
        return try {
            val array = JSONArray(raw)
            val items = (0 until array.length()).mapNotNull { i ->
                deserializeLegacy(array.getJSONObject(i))
            }
            if (items.isNotEmpty()) persist(items)
            items
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun deserializeLegacy(obj: JSONObject): Setlist? =
        try {
            val songIds = obj.getJSONArray("songIds")
            Setlist(
                id = obj.getString("id"),
                name = obj.getString("name"),
                songIds = (0 until songIds.length()).map { songIds.getString(it) },
                createdAt = obj.getLong("createdAt"),
                updatedAt = obj.getLong("updatedAt"),
            )
        } catch (_: Exception) {
            null
        }

    companion object {
        private const val PREFS_NAME = "setlists"
        private const val KEY_SETLISTS = "setlist_data"
    }
}
