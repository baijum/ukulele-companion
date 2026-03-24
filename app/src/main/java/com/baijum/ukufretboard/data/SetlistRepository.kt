package com.baijum.ukufretboard.data

import android.content.Context
import android.content.SharedPreferences
import com.baijum.ukufretboard.data.Setlist
import org.json.JSONArray
import org.json.JSONObject

class SetlistRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): List<Setlist> {
        val json = prefs.getString(KEY_SETLISTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { deserialize(array.getJSONObject(it)) }
                .sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(setlist: Setlist) {
        val all = getAll().toMutableList()
        val idx = all.indexOfFirst { it.id == setlist.id }
        if (idx >= 0) {
            all[idx] = setlist
        } else {
            all.add(0, setlist)
        }
        persist(all)
    }

    fun delete(id: String) {
        val all = getAll().toMutableList()
        all.removeAll { it.id == id }
        persist(all)
    }

    fun importAll(setlists: List<Setlist>) {
        val all = getAll().toMutableList()
        for (setlist in setlists) {
            val idx = all.indexOfFirst { it.id == setlist.id }
            if (idx >= 0) {
                if (setlist.updatedAt > all[idx].updatedAt) {
                    all[idx] = setlist
                }
            } else {
                all.add(setlist)
            }
        }
        persist(all)
    }

    private fun persist(setlists: List<Setlist>) {
        val array = JSONArray()
        setlists.forEach { array.put(serialize(it)) }
        prefs.edit().putString(KEY_SETLISTS, array.toString()).apply()
    }

    private fun serialize(setlist: Setlist): JSONObject =
        JSONObject().apply {
            put("id", setlist.id)
            put("name", setlist.name)
            put("songIds", JSONArray(setlist.songIds))
            put("createdAt", setlist.createdAt)
            put("updatedAt", setlist.updatedAt)
        }

    private fun deserialize(obj: JSONObject): Setlist? =
        try {
            val songIds = obj.getJSONArray("songIds")
            Setlist(
                id = obj.getString("id"),
                name = obj.getString("name"),
                songIds = (0 until songIds.length()).map { songIds.getString(it) },
                createdAt = obj.getLong("createdAt"),
                updatedAt = obj.getLong("updatedAt"),
            )
        } catch (e: Exception) {
            null
        }

    companion object {
        private const val PREFS_NAME = "setlists"
        private const val KEY_SETLISTS = "setlist_data"
    }
}
