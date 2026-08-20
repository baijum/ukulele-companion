package com.baijum.ukufretboard.data

import android.content.SharedPreferences
import android.util.Log

/**
 * The outcome of reading a payload that is protected by a backup slot.
 *
 * The three cases are kept apart because callers genuinely treat them
 * differently: a store with nothing written yet may still have a legacy layout
 * to migrate, whereas one whose copies are both unreadable has already lost its
 * data and can only start over.
 */
internal sealed interface BackupRead<out T> {
    /** A payload was read, from either the primary slot or the backup. */
    data class Loaded<T>(
        val value: T,
    ) : BackupRead<T>

    /** Nothing has ever been written to the primary slot. */
    data object Absent : BackupRead<Nothing>

    /** Both copies were unreadable. The primary's raw bytes have been quarantined. */
    data object Unrecoverable : BackupRead<Nothing>
}

/**
 * Reads [key], falling back to [backupKey] when the primary payload cannot be
 * parsed by [tryParse].
 *
 * When neither copy is readable the primary's raw bytes are moved to
 * [quarantineKey] rather than left to be overwritten by the next save, so they
 * survive for a support request instead of disappearing silently. Quarantining
 * is the only write this function performs.
 *
 * This is the read half of the scheme whose write half is
 * [writeWithBackupRotation]; the two are meant to be changed together, which is
 * why neither is written out per store. See that function for why the rule
 * lives in one place.
 */
internal fun <T> SharedPreferences.readWithBackupFallback(
    key: String,
    backupKey: String,
    quarantineKey: String,
    tag: String,
    tryParse: (String?) -> T?,
): BackupRead<T> {
    val raw = getString(key, null) ?: return BackupRead.Absent

    tryParse(raw)?.let { return BackupRead.Loaded(it) }

    tryParse(getString(backupKey, null))?.let { recovered ->
        Log.w(tag, "$key unparseable; recovered from $backupKey")
        return BackupRead.Loaded(recovered)
    }

    Log.e(tag, "$key and its backup are both unparseable; quarantining")
    edit().putString(quarantineKey, raw).apply()
    return BackupRead.Unrecoverable
}
