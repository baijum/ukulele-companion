package com.baijum.ukufretboard.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for the [JsonListRepository] base class.
 *
 * This is the corrupt-data recovery path shared by every list repository:
 * an unparseable primary falls back to the backup copy, and when both are
 * unreadable the raw primary is quarantined instead of being thrown away.
 *
 * Worth knowing while reading these: all five production subclasses override
 * `getAll()`, so the base implementation below — including the entire quarantine
 * branch — is currently unreachable in the shipping app. `persist()` is shared by
 * all of them, though, so the rotation tests below cover live behaviour. These
 * tests also pin the remaining weakness noted inline so that changing it is a
 * deliberate act.
 */
@RunWith(RobolectricTestRunner::class)
class JsonListRepositoryTest {
    @Serializable
    private data class TestEntity(
        val id: String,
        val name: String,
        val updatedAt: Long = 0L,
    )

    private class TestRepository(
        context: Context,
    ) : JsonListRepository<TestEntity>(
            context,
            PREFS_NAME,
            DATA_KEY,
            TestEntity.serializer(),
        ) {
        override fun entityId(item: TestEntity) = item.id

        override fun entityTimestamp(item: TestEntity) = item.updatedAt
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var repo: TestRepository

    @Before
    fun setUp() {
        val app: Application = ApplicationProvider.getApplicationContext()
        // A dedicated prefs file: the production repositories scan prefs.all for
        // legacy entries, so scratch keys must never share a file with real data.
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        repo = TestRepository(app)
    }

    private fun entity(
        id: String,
        name: String = id,
        updatedAt: Long = 0L,
    ) = TestEntity(id = id, name = name, updatedAt = updatedAt)

    private fun writeRaw(
        key: String,
        value: String,
    ) {
        prefs.edit().putString(key, value).commit()
    }

    // ── Save, delete, read ───────────────────────────────────────────

    @Test
    fun getAllReturnsEmptyWhenNothingWasPersisted() {
        assertTrue("a fresh repository should be empty", repo.getAll().isEmpty())
    }

    @Test
    fun saveInsertsNewItemsAtTheFrontOfTheList() {
        repo.save(entity("a"))
        repo.save(entity("b"))
        repo.save(entity("c"))
        assertEquals(listOf("c", "b", "a"), repo.getAll().map { it.id })
    }

    @Test
    fun saveReplacesAnExistingItemInPlaceWithoutReordering() {
        repo.save(entity("a"))
        repo.save(entity("b"))
        repo.save(entity("a", name = "renamed"))

        val all = repo.getAll()
        assertEquals("order should be preserved on update", listOf("b", "a"), all.map { it.id })
        assertEquals("renamed", all.single { it.id == "a" }.name)
    }

    @Test
    fun deleteRemovesOnlyTheMatchingId() {
        repo.save(entity("a"))
        repo.save(entity("b"))
        repo.delete("a")
        assertEquals(listOf("b"), repo.getAll().map { it.id })
    }

    @Test
    fun deleteOfAnUnknownIdLeavesTheListIntact() {
        repo.save(entity("a"))
        repo.delete("nope")
        assertEquals(listOf("a"), repo.getAll().map { it.id })
    }

    @Test
    fun savedItemsSurviveANewRepositoryInstance() {
        repo.save(entity("a", name = "kept"))
        val fresh = TestRepository(ApplicationProvider.getApplicationContext())
        assertEquals("kept", fresh.getAll().single().name)
    }

    // ── Import merge ─────────────────────────────────────────────────

    @Test
    fun importAllKeepsTheExistingCopyOfADuplicateId() {
        repo.save(entity("a", name = "local"))
        repo.importAll(listOf(entity("a", name = "incoming")))
        assertEquals("local data is authoritative", "local", repo.getAll().single().name)
    }

    @Test
    fun importAllAppendsUnknownIdsAfterExistingOnes() {
        repo.save(entity("a"))
        repo.importAll(listOf(entity("a"), entity("b"), entity("c")))
        assertEquals(listOf("a", "b", "c"), repo.getAll().map { it.id })
    }

    @Test
    fun importAllOfAnEmptyListPreservesExistingItems() {
        repo.save(entity("a"))
        repo.importAll(emptyList())
        assertEquals(listOf("a"), repo.getAll().map { it.id })
    }

    @Test
    fun importAllIntoAnEmptyRepositoryAddsEverything() {
        repo.importAll(listOf(entity("a"), entity("b")))
        assertEquals(listOf("a", "b"), repo.getAll().map { it.id })
    }

    // ── Backup rotation ──────────────────────────────────────────────

    @Test
    fun firstPersistSeedsTheBackupWithTheSameRawWhenNoPreviousValueExists() {
        repo.save(entity("a"))
        assertEquals(
            "the first write has no previous good copy, so it backs up itself",
            prefs.getString(DATA_KEY, null),
            prefs.getString(BACKUP_KEY, null),
        )
    }

    @Test
    fun backupLagsExactlyOnePersistBehindThePrimary() {
        repo.save(entity("a"))
        val afterFirst = prefs.getString(DATA_KEY, null)
        repo.save(entity("b"))

        assertEquals("backup should hold the previous primary", afterFirst, prefs.getString(BACKUP_KEY, null))
        assertTrue("primary should hold both items", repo.getAll().map { it.id }.containsAll(listOf("a", "b")))
    }

    @Test
    fun backupAndQuarantineKeyNamesAreDerivedFromTheDataKey() {
        // On-disk contract: renaming these orphans the recovery copies of every install.
        repo.save(entity("a"))
        writeRaw(DATA_KEY, "{{ not json")
        writeRaw(BACKUP_KEY, "{{ also not json")
        repo.getAll()

        assertNotNull("expected a key named $BACKUP_KEY", prefs.getString(BACKUP_KEY, null))
        assertNotNull("expected a key named $QUARANTINE_KEY", prefs.getString(QUARANTINE_KEY, null))
    }

    // ── Corrupt-data recovery ────────────────────────────────────────

    @Test
    fun getAllFallsBackToTheBackupWhenThePrimaryIsUnparseable() {
        repo.save(entity("a", name = "good"))
        writeRaw(DATA_KEY, "]]not json[[")

        assertEquals("the backup copy should be served", "good", repo.getAll().single().name)
        assertNull("a recoverable read must not quarantine", prefs.getString(QUARANTINE_KEY, null))
    }

    @Test
    fun getAllQuarantinesTheRawPrimaryWhenBothCopiesAreUnparseable() {
        val corrupt = """[{"id":"a","name":"truncated"""
        writeRaw(DATA_KEY, corrupt)
        writeRaw(BACKUP_KEY, "also broken")

        assertTrue("an unreadable store reads as empty", repo.getAll().isEmpty())
        assertEquals(
            "the raw primary should be preserved for recovery",
            corrupt,
            prefs.getString(QUARANTINE_KEY, null),
        )
    }

    @Test
    fun getAllReturnsEmptyAndWritesNoQuarantineWhenNothingWasPersisted() {
        assertTrue(repo.getAll().isEmpty())
        assertNull("an absent store is not a corrupt one", prefs.getString(QUARANTINE_KEY, null))
    }

    @Test
    fun anEmptyStringPrimaryIsTreatedAsCorruptRatherThanEmpty() {
        writeRaw(DATA_KEY, "")
        assertTrue(repo.getAll().isEmpty())
        assertEquals("", prefs.getString(QUARANTINE_KEY, null))
    }

    @Test
    fun aSecondCorruptionOverwritesTheFirstQuarantinedPayload() {
        // Pinned weakness: the quarantine is a single slot with no timestamp or
        // suffix, so a second unrecoverable read destroys the payload the
        // mechanism exists to preserve. Fixing this means a new key scheme.
        writeRaw(DATA_KEY, "first corruption")
        writeRaw(BACKUP_KEY, "broken")
        repo.getAll()
        assertEquals("first corruption", prefs.getString(QUARANTINE_KEY, null))

        writeRaw(DATA_KEY, "second corruption")
        repo.getAll()
        assertEquals(
            "today the newer payload replaces the older one",
            "second corruption",
            prefs.getString(QUARANTINE_KEY, null),
        )
    }

    @Test
    fun persistLeavesTheBackupAloneWhenThePrimaryIsUnparseable() {
        // The backup only advances to a payload that parses. Promoting a corrupt
        // primary would destroy the last good copy — the one thing it exists to hold.
        repo.save(entity("a", name = "good"))
        val lastGood = prefs.getString(BACKUP_KEY, null)
        writeRaw(DATA_KEY, "corrupt")
        repo.save(entity("b"))

        assertEquals("the corrupt primary must not be promoted", lastGood, prefs.getString(BACKUP_KEY, null))
    }

    @Test
    fun aSaveOverACorruptPrimaryLeavesTheStoreRecoverableAfterTheNextCorruption() {
        repo.save(entity("a", name = "good"))
        writeRaw(DATA_KEY, "corrupt")
        repo.save(entity("b"))

        // Second corruption: the surviving backup is what keeps the data alive.
        writeRaw(DATA_KEY, "corrupt again")

        assertEquals("the backup should still serve the last good copy", listOf("a"), repo.getAll().map { it.id })
        assertNull("a recoverable read must not quarantine", prefs.getString(QUARANTINE_KEY, null))
    }

    @Test
    fun persistReseedsTheBackupWhenNeitherStoredCopyIsParseable() {
        writeRaw(DATA_KEY, "corrupt")
        writeRaw(BACKUP_KEY, "also corrupt")
        repo.save(entity("a"))

        assertEquals(
            "with nothing worth keeping, the backup is re-seeded with the new payload",
            prefs.getString(DATA_KEY, null),
            prefs.getString(BACKUP_KEY, null),
        )
    }

    private companion object {
        const val PREFS_NAME = "json_list_repo_test"
        const val DATA_KEY = "items"
        const val BACKUP_KEY = "items_backup"
        const val QUARANTINE_KEY = "items_quarantine"
    }
}
