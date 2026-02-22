package com.baijum.ukufretboard.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * A user-created fingerpicking pattern with a unique identifier.
 *
 * @property id Unique identifier (UUID string).
 * @property pattern The fingerpicking pattern data.
 * @property createdAt Epoch millis when the pattern was created.
 */
data class CustomFingerpickingPattern(
    val id: String = UUID.randomUUID().toString(),
    val pattern: FingerpickingPattern,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Repository for persisting user-created fingerpicking patterns using SharedPreferences.
 *
 * Serialization format:
 * ```
 * id|||name|||steps|||createdAt|||timeSignature
 * ```
 * Where each step is: `finger:stringIndex:emphasis` (e.g., "THUMB:0:true;INDEX:2:false").
 * Field 5 (`timeSignature`) was originally stored as an integer (`beatsPerMeasure`).
 * Older entries with a bare integer (e.g., "3") are converted to "3/4"; missing field defaults to "4/4".
 */
class CustomFingerpickingPatternRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns all custom patterns, sorted by creation time (newest first).
     */
    fun getAll(): List<CustomFingerpickingPattern> {
        return prefs.all.entries
            .mapNotNull { (_, value) -> deserialize(value as? String) }
            .sortedByDescending { it.createdAt }
    }

    /**
     * Saves a custom pattern. Overwrites if the same ID exists.
     */
    fun save(custom: CustomFingerpickingPattern) {
        prefs.edit().putString(custom.id, serialize(custom)).apply()
    }

    /**
     * Deletes a custom pattern by ID.
     */
    fun delete(id: String) {
        prefs.edit().remove(id).apply()
    }

    private fun serialize(custom: CustomFingerpickingPattern): String {
        val p = custom.pattern
        val stepsStr = p.steps.joinToString(";") { s ->
            "${s.finger.name}:${s.stringIndex}:${s.emphasis}"
        }
        val safeName = p.name.replace("|", "\\|")
        return "${custom.id}|||$safeName|||$stepsStr|||${custom.createdAt}|||${p.timeSignature}"
    }

    private fun deserialize(value: String?): CustomFingerpickingPattern? {
        if (value == null) return null
        val parts = value.split("|||")
        if (parts.size < 4) return null
        return try {
            val id = parts[0]
            val name = parts[1].replace("\\|", "|")
            val steps = parts[2].split(";").map { stepStr ->
                val sp = stepStr.split(":")
                FingerpickStep(
                    finger = Finger.valueOf(sp[0]),
                    stringIndex = sp[1].toInt(),
                    emphasis = sp[2].toBoolean(),
                )
            }
            val createdAt = parts[3].toLong()
            val timeSignature = if (parts.size >= 5) {
                val raw = parts[4]
                if (raw.contains("/")) raw else "${raw}/4"
            } else "4/4"
            val notation = steps.joinToString(" ") { s ->
                val stringName = FingerpickingPatterns.STRING_NAMES[s.stringIndex]
                "${s.finger.label}($stringName)"
            }
            CustomFingerpickingPattern(
                id = id,
                pattern = FingerpickingPattern(
                    name = name,
                    description = "Custom pattern",
                    difficulty = Difficulty.BEGINNER,
                    timeSignature = timeSignature,
                    steps = steps,
                    notation = notation,
                    suggestedBpm = 60..100,
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
    fun importAll(items: List<CustomFingerpickingPattern>) {
        val editor = prefs.edit()
        for (item in items) {
            if (!prefs.contains(item.id)) {
                editor.putString(item.id, serialize(item))
            }
        }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "custom_fingerpicking_patterns"
    }
}
