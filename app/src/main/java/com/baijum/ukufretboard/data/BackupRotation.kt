package com.baijum.ukufretboard.data

import android.content.SharedPreferences

/**
 * Writes [raw] to [key] and rotates the outgoing payload into [backupKey].
 *
 * The backup only ever advances to a payload that [isReadable] accepts.
 * Promoting a copy that can no longer be read would destroy the last good one,
 * which is the single thing the backup exists to hold: once the primary is
 * corrupt, the backup is what the next read recovers from. When neither stored
 * copy is readable there is nothing worth preserving, so the backup is re-seeded
 * with [raw] and the next corruption again has somewhere to fall back to.
 *
 * Both stores that need this — [SettingsRepository] and [JsonListRepository] —
 * differ only in how they encode and parse their payload, so the rule itself
 * lives here rather than being written out once per store. It was written out
 * twice before, and the two copies drifted: one re-seeded on whether the backup
 * key existed, the other on whether it parsed (#553, #560).
 *
 * [isReadable] is called at most twice and never on the common path where the
 * primary parses, so a store whose payload is expensive to parse pays for the
 * check only when something has already gone wrong.
 */
internal fun SharedPreferences.writeWithBackupRotation(
    key: String,
    backupKey: String,
    raw: String,
    isReadable: (String) -> Boolean,
) {
    val lastGood = getString(key, null)?.takeIf(isReadable)
    val editor = edit().putString(key, raw)
    if (lastGood != null) {
        editor.putString(backupKey, lastGood)
    } else if (!holdsReadable(backupKey, isReadable)) {
        editor.putString(backupKey, raw)
    }
    editor.apply()
}

/**
 * True when [key] holds a payload that [isReadable] accepts. A missing key is
 * not readable, so an absent backup is re-seeded the same way a corrupt one is.
 */
private fun SharedPreferences.holdsReadable(
    key: String,
    isReadable: (String) -> Boolean,
): Boolean = getString(key, null)?.let(isReadable) == true
