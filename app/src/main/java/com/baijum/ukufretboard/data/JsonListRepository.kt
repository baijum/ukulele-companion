package com.baijum.ukufretboard.data

import android.content.Context
import android.util.Log
import com.baijum.ukufretboard.domain.mergeKeepExisting
import com.baijum.ukufretboard.domain.mergeNewerWins
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Base class for repositories that persist a list of entities as a single JSON array
 * in SharedPreferences. Provides standard CRUD and merge-by-ID import.
 *
 * Subclasses only need to specify the entity's ID and timestamp accessors.
 */
abstract class JsonListRepository<T>(
    context: Context,
    prefsName: String,
    private val key: String,
    private val serializer: KSerializer<T>,
) {
    protected val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    protected val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    abstract fun entityId(item: T): String
    abstract fun entityTimestamp(item: T): Long

    protected val backupKey get() = "${key}_backup"
    private val quarantineKey get() = "${key}_quarantine"

    open fun getAll(): List<T> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        tryParse(raw)?.let { return it }

        val backupRaw = prefs.getString(backupKey, null)
        tryParse(backupRaw)?.let { return it }

        Log.e("JsonListRepository", "Both primary and backup keys unparseable for '$key'; quarantining data")
        prefs.edit().putString(quarantineKey, raw).apply()
        return emptyList()
    }

    protected fun tryParse(raw: String?): List<T>? {
        if (raw == null) return null
        return try {
            json.decodeFromString(ListSerializer(serializer), raw)
        } catch (_: Exception) {
            null
        }
    }

    fun save(item: T) {
        val items = getAll().toMutableList()
        val index = items.indexOfFirst { entityId(it) == entityId(item) }
        if (index >= 0) items[index] = item else items.add(0, item)
        persist(items)
    }

    fun delete(id: String) {
        persist(getAll().filter { entityId(it) != id })
    }

    open fun importAll(items: List<T>) {
        val merged = mergeKeepExisting(
            existing = getAll(),
            incoming = items,
            idOf = ::entityId,
        )
        persist(merged)
    }

    /**
     * Writes the list, rotating the outgoing payload into the backup slot.
     *
     * See [writeWithBackupRotation] for the rule the backup follows.
     */
    protected fun persist(items: List<T>) {
        prefs.writeWithBackupRotation(
            key = key,
            backupKey = backupKey,
            raw = json.encodeToString(ListSerializer(serializer), items),
            isReadable = { tryParse(it) != null },
        )
    }
}
