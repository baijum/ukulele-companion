package com.baijum.ukufretboard.data

import android.content.Context
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

    /**
     * Every key this store writes into its own preferences file.
     *
     * The legacy migrations sweep the file clean once they have consumed it, on
     * the assumption that anything which is not the primary key is a leftover
     * per-entry record. Anything the store writes for itself has to be named
     * here or the sweep takes it too -- including the backup that [persist]
     * wrote moments earlier (#554).
     */
    protected val ownKeys: Set<String> get() = setOf(key, backupKey, quarantineKey)

    /**
     * Reads the list, falling back to the backup copy when the primary payload
     * cannot be parsed. See [readWithBackupFallback] for the rule.
     *
     * An empty list is the answer both to a store that was never written and to
     * one whose copies are both unreadable: with no legacy layout to migrate,
     * the two cases lead to the same place.
     */
    open fun getAll(): List<T> = readOrElse { emptyList() }

    /**
     * Reads the stored list, calling [fallback] when neither stored copy yields
     * one.
     *
     * Every subclass overrides [getAll] to reach a legacy on-disk format when
     * the JSON store has nothing to give. Handing that migration to this
     * function keeps them on the recovery path instead of replacing it: before
     * it existed each override went straight to its migration, so the
     * quarantine below was written but never reached (#564).
     *
     * [fallback] answers both "never written" and "written, now unreadable",
     * because a store with a legacy layout to try has the same thing to try in
     * either case. Only the second leaves quarantined bytes behind, and
     * [readWithBackupFallback] has already written them by the time [fallback]
     * runs -- which is why the migrations must leave [ownKeys] alone.
     */
    protected fun readOrElse(fallback: () -> List<T>): List<T> {
        val read =
            prefs.readWithBackupFallback(
                key = key,
                backupKey = backupKey,
                quarantineKey = quarantineKey,
                tag = TAG,
                tryParse = ::tryParse,
            )
        return when (read) {
            is BackupRead.Loaded -> read.value
            BackupRead.Absent, BackupRead.Unrecoverable -> fallback()
        }
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

    private companion object {
        const val TAG = "JsonListRepository"
    }
}
