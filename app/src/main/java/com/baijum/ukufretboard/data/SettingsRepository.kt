package com.baijum.ukufretboard.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.serialization.json.Json

/**
 * Persists [AppSettings] as a single JSON string in SharedPreferences.
 *
 * Settings are the one store the user cannot re-create by hand, so the payload
 * is protected the same way [JsonListRepository] protects songs and favourites:
 * a rotating backup copy, and a quarantine slot for a payload that can no longer
 * be read. A one-time migration converts legacy individual-key storage on first
 * access.
 */
class SettingsRepository(
    context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    /**
     * Reads the settings, falling back to the backup copy when the primary
     * payload cannot be parsed.
     *
     * When neither copy is readable the raw primary is moved to a quarantine key
     * instead of being overwritten by the next save, so the bytes survive for a
     * support request rather than disappearing silently.
     */
    fun load(): AppSettings {
        val raw = prefs.getString(KEY_SETTINGS, null)
        if (raw != null) {
            tryParse(raw)?.let { return it }

            tryParse(prefs.getString(KEY_SETTINGS_BACKUP, null))?.let { recovered ->
                Log.w(TAG, "$KEY_SETTINGS unparseable; recovered from $KEY_SETTINGS_BACKUP")
                return recovered
            }

            Log.e(TAG, "$KEY_SETTINGS and its backup are both unparseable; quarantining")
            prefs.edit().putString(KEY_SETTINGS_QUARANTINE, raw).apply()
            return AppSettings()
        }

        if (!prefs.contains(KEY_LEGACY_SOUND_ENABLED)) return AppSettings()
        return migrateLegacySettings()
    }

    /**
     * Writes the settings, rotating the outgoing payload into the backup slot.
     *
     * See [writeWithBackupRotation] for the rule the backup follows.
     */
    fun save(settings: AppSettings) {
        prefs.writeWithBackupRotation(
            key = KEY_SETTINGS,
            backupKey = KEY_SETTINGS_BACKUP,
            raw = json.encodeToString(AppSettings.serializer(), settings),
            isReadable = { tryParse(it) != null },
        )
    }

    private fun tryParse(raw: String?): AppSettings? {
        if (raw == null) return null
        return try {
            json.decodeFromString(AppSettings.serializer(), raw)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * One-time migration: reads old individual-key settings, constructs
     * [AppSettings], saves as JSON, and removes the legacy keys.
     */
    private fun migrateLegacySettings(): AppSettings {
        val settings = LegacySettingsReader.read(prefs)
        save(settings)
        LegacySettingsReader.removeLegacyKeys(prefs)
        return settings
    }

    private companion object {
        const val TAG = "SettingsRepository"
        const val PREFS_NAME = "app_settings"
        const val KEY_SETTINGS = "settings_json"
        const val KEY_SETTINGS_BACKUP = "settings_json_backup"
        const val KEY_SETTINGS_QUARANTINE = "settings_json_quarantine"
        const val KEY_LEGACY_SOUND_ENABLED = "sound_enabled"
    }
}
