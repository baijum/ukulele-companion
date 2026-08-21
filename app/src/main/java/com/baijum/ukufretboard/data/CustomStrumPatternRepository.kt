package com.baijum.ukufretboard.data

import android.content.Context
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A user-created strumming pattern with a unique identifier.
 *
 * @property id Unique identifier (UUID string).
 * @property pattern The strumming pattern data.
 * @property createdAt Epoch millis when the pattern was created.
 */
@Serializable
data class CustomStrumPattern(
    val id: String = UUID.randomUUID().toString(),
    val pattern: StrumPattern,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Repository for persisting user-created strumming patterns using SharedPreferences.
 *
 * Stores the full list as a single JSON array under [KEY_PATTERNS].
 * On first access, migrates from the legacy pipe-delimited per-entry format.
 */
class CustomStrumPatternRepository(context: Context) : JsonListRepository<CustomStrumPattern>(
    context,
    PREFS_NAME,
    KEY_PATTERNS,
    CustomStrumPattern.serializer(),
) {
    override fun entityId(item: CustomStrumPattern) = item.id
    override fun entityTimestamp(item: CustomStrumPattern) = item.createdAt

    override fun getAll(): List<CustomStrumPattern> {
        val raw = prefs.getString(KEY_PATTERNS, null)
        if (raw != null) {
            return tryParse(raw)
                ?: tryParse(prefs.getString(backupKey, null))
                ?: migrateLegacyPipeEntries()
        }
        return migrateLegacyPipeEntries()
    }

    private fun migrateLegacyPipeEntries(): List<CustomStrumPattern> {
        val entries = prefs.all.entries
            .filter { it.key !in ownKeys }
            .mapNotNull { (_, value) -> deserializeLegacy(value as? String) }
            .sortedByDescending { it.createdAt }
        if (entries.isNotEmpty()) {
            persist(entries)
            val editor = prefs.edit()
            prefs.all.keys.filter { it !in ownKeys }.forEach { editor.remove(it) }
            editor.apply()
        }
        return entries
    }

    private fun deserializeLegacy(value: String?): CustomStrumPattern? {
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
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "custom_strum_patterns"
        private const val KEY_PATTERNS = "patterns_json"
    }
}
