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

    /**
     * Unlike the other stores this one consults its legacy format *before* the
     * backup: a primary that kotlinx cannot read may still be readable
     * org.json, in which case migrating it in place beats recovering whatever
     * older copy the backup happens to hold.
     *
     * Only once that has failed does the shared rule take over. It re-reads the
     * primary and re-parses it, which is wasted work -- but only on a store
     * that is already broken, and it is what makes the quarantine happen.
     */
    override fun getAll(): List<Setlist> {
        val raw = prefs.getString(KEY_SETLISTS, null) ?: return emptyList()
        tryParse(raw)?.let { return it }
        migrateLegacyOrgJson(raw)?.let { return it }
        return readOrElse { emptyList() }
    }

    private fun migrateLegacyOrgJson(raw: String): List<Setlist>? {
        return try {
            val array = JSONArray(raw)
            val items = (0 until array.length()).mapNotNull { i ->
                deserializeLegacy(array.getJSONObject(i))
            }
            if (items.isNotEmpty()) { persist(items); items } else null
        } catch (_: Exception) {
            null
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
