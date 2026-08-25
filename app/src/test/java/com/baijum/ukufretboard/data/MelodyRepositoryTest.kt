package com.baijum.ukufretboard.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [MelodyRepository], the last list store to move onto the shared
 * JSON-plus-backup scheme (#565).
 *
 * The move is what these tests are about. Round-tripping a melody is covered by
 * `RepositorySerializationTest`; what is new here is the migration off the
 * one-melody-per-key layout, and the promise that it does not destroy the
 * entries it cannot read on the way past -- under the old layout a damaged
 * melody was invisible but still on disk, and a migration that swept the file
 * clean would have made this fix a loss.
 */
@RunWith(RobolectricTestRunner::class)
class MelodyRepositoryTest {
    private lateinit var prefs: SharedPreferences
    private lateinit var repo: MelodyRepository

    @Before
    fun setUp() {
        val app: Application = ApplicationProvider.getApplicationContext()
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        repo = MelodyRepository(app)
    }

    private fun melody(
        id: String,
        name: String = "Melody $id",
        createdAt: Long = 1_000L,
    ) = Melody(
        id = id,
        name = name,
        notes = listOf(MelodyNote(pitchClass = 0, octave = 4, duration = NoteDuration.QUARTER)),
        bpm = 120,
        createdAt = createdAt,
    )

    /** The pre-JSON on-disk format: one preference key per melody. */
    private fun legacyEntry(
        id: String,
        name: String,
        notes: String = "0:4:QUARTER:2:3",
        bpm: Int = 120,
        createdAt: Long = 1_000L,
    ) = listOf(id, name, notes, bpm.toString(), createdAt.toString()).joinToString("|||")

    private fun writeRaw(
        key: String,
        value: String,
    ) {
        prefs.edit().putString(key, value).commit()
    }

    // ── Legacy migration ─────────────────────────────────────────────

    @Test
    fun migratesLegacyPipeEntriesNewestFirst() {
        writeRaw("a", legacyEntry("a", "Oldest", createdAt = 1_000L))
        writeRaw("b", legacyEntry("b", "Newest", createdAt = 3_000L))
        writeRaw("c", legacyEntry("c", "Middle", createdAt = 2_000L))

        assertEquals(listOf("b", "c", "a"), repo.getAll().map { it.id })
    }

    @Test
    fun legacyMigrationPreservesEveryNoteField() {
        writeRaw(
            "a",
            legacyEntry("a", "Tune \\| Two", notes = "0:4:QUARTER:2:3;;_:3:HALF:_:_", bpm = 96),
        )

        val migrated = repo.getAll().single()
        assertEquals("Tune | Two", migrated.name)
        assertEquals(96, migrated.bpm)
        assertEquals(
            listOf(
                MelodyNote(pitchClass = 0, octave = 4, duration = NoteDuration.QUARTER, stringIndex = 2, fret = 3),
                MelodyNote(pitchClass = null, octave = 3, duration = NoteDuration.HALF),
            ),
            migrated.notes,
        )
    }

    @Test
    fun migratedEntriesLeaveTheirLegacyKeysBehind() {
        writeRaw("a", legacyEntry("a", "Migrated"))

        repo.getAll()

        assertNull("the consumed key should be swept", prefs.getString("a", null))
        assertEquals(listOf("a"), repo.getAll().map { it.id })
    }

    @Test
    fun anUnreadableLegacyEntryIsLeftOnDiskWhileTheRestMigrate() {
        // The whole point of #565: the damaged melody is still absent from the
        // list, but its bytes are recoverable rather than swept away by the
        // migration that moved its neighbours.
        writeRaw("bad", "truncated|||entry")
        writeRaw("good", legacyEntry("good", "Survivor"))

        assertEquals(listOf("good"), repo.getAll().map { it.id })
        assertEquals("truncated|||entry", prefs.getString("bad", null))
    }

    @Test
    fun aStoreOfNothingButUnreadableEntriesStillFinishesMigrating() {
        // Otherwise every read rescans the same broken entries and re-logs them.
        writeRaw("bad", "truncated|||entry")

        assertTrue(repo.getAll().isEmpty())
        assertEquals("[]", prefs.getString(KEY_MELODIES, null))
        assertEquals("truncated|||entry", prefs.getString("bad", null))
    }

    @Test
    fun anEmptyStoreWritesNothing() {
        assertTrue(repo.getAll().isEmpty())
        assertNull("a fresh install has nothing to migrate", prefs.getString(KEY_MELODIES, null))
    }

    // ── Corrupt data ─────────────────────────────────────────────────

    @Test
    fun corruptPrimaryFallsBackToTheBackupCopy() {
        repo.save(melody("a", name = "Recoverable"))
        repo.save(melody("b", name = "Rotates a into the backup", createdAt = 2_000L))
        writeRaw(KEY_MELODIES, "}} not json {{")

        assertEquals(listOf("a"), repo.getAll().map { it.id })
    }

    @Test
    fun corruptPrimaryAndBackupQuarantineTheUnreadableBytes() {
        writeRaw(KEY_MELODIES, "}} not json {{")
        writeRaw(BACKUP_KEY, "also broken")

        assertTrue("nothing is readable, so the store reads as empty", repo.getAll().isEmpty())
        assertEquals(
            "the primary's bytes should be preserved for a support request",
            "}} not json {{",
            prefs.getString(QUARANTINE_KEY, null),
        )
    }

    @Test
    fun quarantinedBytesAreNotOverwrittenByTheNextSave() {
        writeRaw(KEY_MELODIES, "}} not json {{")
        writeRaw(BACKUP_KEY, "also broken")
        repo.getAll()

        repo.save(melody("a", name = "Added after the loss"))

        assertEquals(listOf("a"), repo.getAll().map { it.id })
        assertEquals("}} not json {{", prefs.getString(QUARANTINE_KEY, null))
    }

    @Test
    fun anUnrecoverableStoreStillFallsThroughToTheLegacyMigration() {
        // The migration's own cleanup must leave the bytes just quarantined
        // alone -- they are not a leftover per-melody entry (#554).
        writeRaw(KEY_MELODIES, "}} not json {{")
        writeRaw(BACKUP_KEY, "also broken")
        writeRaw("a", legacyEntry("a", "Recovered from the legacy format"))

        assertEquals("Recovered from the legacy format", repo.getAll().single().name)
        assertEquals(
            "the migration cleanup should not sweep away the quarantine",
            "}} not json {{",
            prefs.getString(QUARANTINE_KEY, null),
        )
    }

    // ── CRUD ─────────────────────────────────────────────────────────

    @Test
    fun getReturnsTheSavedMelody() {
        repo.save(melody("a", name = "Findable"))
        assertEquals("Findable", repo.get("a")?.name)
        assertNull(repo.get("missing"))
    }

    @Test
    fun deleteRemovesOnlyTheNamedMelody() {
        repo.save(melody("a"))
        repo.save(melody("b", createdAt = 2_000L))

        repo.delete("a")

        assertEquals(listOf("b"), repo.getAll().map { it.id })
    }

    @Test
    fun importAllKeepsTheLocalCopyOfAnExistingIdAndAddsNewIds() {
        repo.save(melody("a", name = "Local", createdAt = 100L))

        repo.importAll(listOf(melody("a", name = "Backup", createdAt = 200L), melody("b", createdAt = 50L)))

        // Melody has no updatedAt and createdAt never changes after creation, so
        // an import must keep the local copy on an ID collision (#597) and add the
        // genuinely new IDs. A "keep newer by createdAt" policy would tie on every
        // real conflict and let the backup silently win.
        assertEquals("Local", repo.get("a")?.name)
        assertEquals(listOf("a", "b"), repo.getAll().map { it.id })
    }

    @Test
    fun importAllDoesNotOverwriteNewerLocalEditsWithAnOlderBackup() {
        // #597 repro: save M, snapshot it for a backup, edit M locally keeping the
        // same id and createdAt, then re-import the snapshot. The local edit must
        // survive -- under the old mergeNewerWins the tie on createdAt resolved to
        // the incoming (older) backup, silently discarding the edit.
        repo.save(melody("X", name = "Original", createdAt = 1_000L))
        val snapshot = repo.getAll() // the backup file

        val edited =
            melody("X", name = "Renamed after backup", createdAt = 1_000L)
                .copy(notes = melody("X").notes + MelodyNote(pitchClass = 7, octave = 4, duration = NoteDuration.HALF))
        repo.save(edited)

        repo.importAll(snapshot)

        val restored = repo.get("X")
        assertEquals("Renamed after backup", restored?.name)
        assertEquals(edited.notes, restored?.notes)
    }

    private companion object {
        const val PREFS_NAME = "melodies"
        const val KEY_MELODIES = "melodies_json"
        const val BACKUP_KEY = "melodies_json_backup"
        const val QUARANTINE_KEY = "melodies_json_quarantine"
    }
}
