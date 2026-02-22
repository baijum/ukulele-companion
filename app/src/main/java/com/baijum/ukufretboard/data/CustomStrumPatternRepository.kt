package com.baijum.ukufretboard.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * A user-created strumming pattern with a unique identifier.
 *
 * @property id Unique identifier (UUID string).
 * @property pattern The strumming pattern data.
 * @property createdAt Epoch millis when the pattern was created.
 */
data class CustomStrumPattern(
    val id: String = UUID.randomUUID().toString(),
    val pattern: StrumPattern,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Repository for persisting user-created strumming patterns using SharedPreferences.
 *
 * Serialization format:
 * ```
 * id|||name|||beats|||createdAt|||timeSignature
 * ```
 * Where each beat is: `direction:emphasis` (e.g., "DOWN:true;UP:false;PAUSE:false").
 * The `timeSignature` field was added later; older entries with only 4 fields default to "4/4".
 */
class CustomStrumPatternRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns all custom patterns, sorted by creation time (newest first).
     */
    fun getAll(): List<CustomStrumPattern> {
        return prefs.all.entries
            .mapNotNull { (_, value) -> deserialize(value as? String) }
            .sortedByDescending { it.createdAt }
    }

    /**
     * Saves a custom pattern. Overwrites if the same ID exists.
     */
    fun save(custom: CustomStrumPattern) {
        prefs.edit().putString(custom.id, serialize(custom)).apply()
    }

    /**
     * Deletes a custom pattern by ID.
     */
    fun delete(id: String) {
        prefs.edit().remove(id).apply()
    }

    private fun serialize(custom: CustomStrumPattern): String {
        val p = custom.pattern
        val beatsStr = p.beats.joinToString(";") { b ->
            "${b.direction.name}:${b.emphasis}"
        }
        val safeName = p.name.replace("|", "\\|")
        return "${custom.id}|||$safeName|||$beatsStr|||${custom.createdAt}|||${p.timeSignature}"
    }

    private fun deserialize(value: String?): CustomStrumPattern? {
        if (value == null) return null
        val parts = value.split("|||")
        if (parts.size < 4) return null
        return try {
            val id = parts[0]
            val name = parts[1].replace("\\|", "|")
            val beats = parts[2].split(";").map { beatStr ->
                val bp = beatStr.split(":")
                StrumBeat(
                    direction = StrumDirection.valueOf(bp[0]),
                    emphasis = bp[1].toBoolean(),
                )
            }
            val createdAt = parts[3].toLong()
            val timeSignature = if (parts.size >= 5) parts[4] else "4/4"
            val notation = beats.joinToString(" ") { b ->
                if (b.emphasis) b.direction.symbol.uppercase()
                else b.direction.symbol
            }
            CustomStrumPattern(
                id = id,
                pattern = StrumPattern(
                    name = name,
                    description = "Custom pattern",
                    difficulty = Difficulty.BEGINNER,
                    timeSignature = timeSignature,
                    beats = beats,
                    notation = notation,
                    suggestedBpm = 80..120,
                ),
                createdAt = createdAt,
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Merges the given list of patterns into local storage.
     * Only adds entries that are not already present (by ID).
     */
    fun importAll(items: List<CustomStrumPattern>) {
        val editor = prefs.edit()
        for (item in items) {
            if (!prefs.contains(item.id)) {
                editor.putString(item.id, serialize(item))
            }
        }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "custom_strum_patterns"
    }
}
