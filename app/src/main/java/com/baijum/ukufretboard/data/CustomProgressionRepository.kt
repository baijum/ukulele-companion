package com.baijum.ukufretboard.data

import android.content.Context
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Persisted custom progression with a unique identifier.
 *
 * Wraps a [Progression] with metadata for storage and management.
 *
 * @property id Unique identifier (UUID string).
 * @property progression The chord progression data.
 * @property createdAt Epoch millis when the progression was created.
 */
@Serializable
data class CustomProgression(
    val id: String = UUID.randomUUID().toString(),
    val progression: Progression,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Repository for persisting user-created chord progressions using SharedPreferences.
 *
 * Stores the full list as a single JSON array under [KEY_PROGRESSIONS].
 * On first access, migrates from the legacy pipe-delimited per-entry format.
 */
class CustomProgressionRepository(context: Context) : JsonListRepository<CustomProgression>(
    context,
    PREFS_NAME,
    KEY_PROGRESSIONS,
    CustomProgression.serializer(),
) {
    override fun entityId(item: CustomProgression) = item.id
    override fun entityTimestamp(item: CustomProgression) = item.createdAt

    override fun getAll(): List<CustomProgression> = readOrElse(::migrateLegacyPipeEntries)

    private fun migrateLegacyPipeEntries(): List<CustomProgression> {
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

    private fun deserializeLegacy(value: String?): CustomProgression? {
        if (value == null) return null
        val parts = value.split("|||")
        if (parts.size < 6) return null
        return try {
            val id = parts[0]
            val name = parts[1].replace("\\|", "|")
            val description = parts[2].replace("\\|", "|")
            val scaleType = ScaleType.valueOf(parts[3])
            val degrees = parts[4].split(";").map { degreeStr ->
                val dp = degreeStr.split(":")
                ChordDegree(
                    interval = dp[0].toInt(),
                    quality = dp[1],
                    numeral = dp[2],
                )
            }
            val createdAt = parts[5].toLong()
            CustomProgression(
                id = id,
                progression = Progression(
                    name = name,
                    description = description,
                    degrees = degrees,
                    scaleType = scaleType,
                ),
                createdAt = createdAt,
            )
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "custom_progressions"
        private const val KEY_PROGRESSIONS = "progressions_json"
    }
}
