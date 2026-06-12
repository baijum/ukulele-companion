package com.baijum.ukufretboard.data

import android.content.Context
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A user-created fingerpicking pattern with a unique identifier.
 *
 * @property id Unique identifier (UUID string).
 * @property pattern The fingerpicking pattern data.
 * @property createdAt Epoch millis when the pattern was created.
 */
@Serializable
data class CustomFingerpickingPattern(
    val id: String = UUID.randomUUID().toString(),
    val pattern: FingerpickingPattern,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Repository for persisting user-created fingerpicking patterns using SharedPreferences.
 *
 * Stores the full list as a single JSON array under [KEY_PATTERNS].
 * On first access, migrates from the legacy pipe-delimited per-entry format.
 */
class CustomFingerpickingPatternRepository(context: Context) : JsonListRepository<CustomFingerpickingPattern>(
    context,
    PREFS_NAME,
    KEY_PATTERNS,
    CustomFingerpickingPattern.serializer(),
) {
    override fun entityId(item: CustomFingerpickingPattern) = item.id
    override fun entityTimestamp(item: CustomFingerpickingPattern) = item.createdAt

    override fun getAll(): List<CustomFingerpickingPattern> {
        val raw = prefs.getString(KEY_PATTERNS, null)
        if (raw != null) {
            return tryParse(raw)
                ?: tryParse(prefs.getString(backupKey, null))
                ?: migrateLegacyPipeEntries()
        }
        return migrateLegacyPipeEntries()
    }

    private fun migrateLegacyPipeEntries(): List<CustomFingerpickingPattern> {
        val entries = prefs.all.entries
            .filter { it.key != KEY_PATTERNS }
            .mapNotNull { (_, value) -> deserializeLegacy(value as? String) }
            .sortedByDescending { it.createdAt }
        if (entries.isNotEmpty()) {
            persist(entries)
            val editor = prefs.edit()
            prefs.all.keys.filter { it != KEY_PATTERNS }.forEach { editor.remove(it) }
            editor.apply()
        }
        return entries
    }

    private fun deserializeLegacy(value: String?): CustomFingerpickingPattern? {
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
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "custom_fingerpicking_patterns"
        private const val KEY_PATTERNS = "patterns_json"
    }
}
