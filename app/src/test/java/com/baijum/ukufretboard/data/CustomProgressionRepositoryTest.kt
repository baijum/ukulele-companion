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
 * Tests for [CustomProgressionRepository].
 *
 * This is the one repository left out of `RepositorySerializationTest`, and the
 * only one whose `getAll()` override falls through to a legacy pipe-format
 * migration rather than to the base class's quarantine path. Both branches are
 * covered here.
 */
@RunWith(RobolectricTestRunner::class)
class CustomProgressionRepositoryTest {
    private lateinit var prefs: SharedPreferences
    private lateinit var repo: CustomProgressionRepository

    @Before
    fun setUp() {
        val app: Application = ApplicationProvider.getApplicationContext()
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        repo = CustomProgressionRepository(app)
    }

    private fun degrees() =
        listOf(
            ChordDegree(interval = 0, quality = "", numeral = "I"),
            ChordDegree(interval = 5, quality = "", numeral = "IV"),
            ChordDegree(interval = 7, quality = "", numeral = "V"),
        )

    private fun progression(
        id: String,
        name: String = "Pattern $id",
        createdAt: Long = 1_000L,
        scaleType: ScaleType = ScaleType.MAJOR,
    ) = CustomProgression(
        id = id,
        progression =
            Progression(
                name = name,
                description = "Description of $id",
                degrees = degrees(),
                scaleType = scaleType,
            ),
        createdAt = createdAt,
    )

    /** The pre-JSON on-disk format: one preference key per progression. */
    private fun legacyEntry(
        id: String,
        name: String,
        description: String = "legacy description",
        scaleType: String = "MAJOR",
        createdAt: Long = 1_000L,
    ) = listOf(id, name, description, scaleType, DEGREE_SPEC, createdAt.toString()).joinToString("|||")

    private fun writeRaw(
        key: String,
        value: String,
    ) {
        prefs.edit().putString(key, value).commit()
    }

    // ── CRUD ─────────────────────────────────────────────────────────

    @Test
    fun roundTripsAProgressionWithAllFields() {
        repo.save(progression("a", name = "Doo-wop", createdAt = 4_242L, scaleType = ScaleType.MINOR))

        val stored = repo.getAll().single()
        assertEquals("a", stored.id)
        assertEquals("Doo-wop", stored.progression.name)
        assertEquals("Description of a", stored.progression.description)
        assertEquals(ScaleType.MINOR, stored.progression.scaleType)
        assertEquals(degrees(), stored.progression.degrees)
        assertEquals(4_242L, stored.createdAt)
    }

    @Test
    fun savePrependsNewProgressionsAndUpdatesExistingOnesInPlace() {
        repo.save(progression("a"))
        repo.save(progression("b"))
        assertEquals(listOf("b", "a"), repo.getAll().map { it.id })

        repo.save(progression("a", name = "Renamed"))
        assertEquals("order should survive an update", listOf("b", "a"), repo.getAll().map { it.id })
        assertEquals(
            "Renamed",
            repo
                .getAll()
                .single { it.id == "a" }
                .progression.name,
        )
    }

    @Test
    fun deleteRemovesById() {
        repo.save(progression("a"))
        repo.save(progression("b"))
        repo.delete("a")
        assertEquals(listOf("b"), repo.getAll().map { it.id })
    }

    @Test
    fun savedProgressionsSurviveANewRepositoryInstance() {
        repo.save(progression("a", name = "Persisted"))
        val fresh = CustomProgressionRepository(ApplicationProvider.getApplicationContext())
        assertEquals(
            "Persisted",
            fresh
                .getAll()
                .single()
                .progression.name,
        )
    }

    @Test
    fun prefsFileAndKeyNamesAreTheOnDiskContract() {
        repo.save(progression("a"))
        assertTrue(
            "progressions must persist under progressions_json in custom_progressions",
            prefs.getString(KEY_PROGRESSIONS, null)?.contains("\"a\"") == true,
        )
    }

    // ── Import merge ─────────────────────────────────────────────────

    @Test
    fun importAllKeepsTheLocalCopyOfADuplicateId() {
        repo.save(progression("a", name = "Local"))
        repo.importAll(listOf(progression("a", name = "Incoming")))
        assertEquals(
            "Local",
            repo
                .getAll()
                .single()
                .progression.name,
        )
    }

    @Test
    fun importAllAddsUnknownIds() {
        repo.save(progression("a"))
        repo.importAll(listOf(progression("a"), progression("b")))
        assertEquals(listOf("a", "b"), repo.getAll().map { it.id })
    }

    // ── Corrupt data ─────────────────────────────────────────────────

    @Test
    fun corruptPrimaryFallsBackToTheBackupCopy() {
        repo.save(progression("a", name = "Recoverable"))
        writeRaw(KEY_PROGRESSIONS, "}} not json {{")
        assertEquals(
            "Recoverable",
            repo
                .getAll()
                .single()
                .progression.name,
        )
    }

    @Test
    fun corruptPrimaryAndBackupWithNoLegacyEntriesReturnsEmptyWithoutQuarantining() {
        // The getAll() override falls through to the legacy migration instead of
        // the base class's quarantine branch, so an unrecoverable store here is
        // silently dropped rather than preserved. Pinned, not fixed: adding
        // quarantine means reconciling the two recovery strategies.
        writeRaw(KEY_PROGRESSIONS, "}} not json {{")
        writeRaw(BACKUP_KEY, "also broken")

        assertTrue(repo.getAll().isEmpty())
        assertNull(
            "this repository never reaches the quarantine branch",
            prefs.getString(QUARANTINE_KEY, null),
        )
    }

    // ── Legacy migration ─────────────────────────────────────────────

    @Test
    fun migratesLegacyPipeEntriesNewestFirst() {
        writeRaw("old_a", legacyEntry("a", "Oldest", createdAt = 1_000L))
        writeRaw("old_b", legacyEntry("b", "Newest", createdAt = 3_000L))
        writeRaw("old_c", legacyEntry("c", "Middle", createdAt = 2_000L))

        assertEquals(listOf("b", "c", "a"), repo.getAll().map { it.id })
    }

    @Test
    fun legacyMigrationPreservesEveryField() {
        writeRaw(
            "old_a",
            legacyEntry("a", "Andalusian", description = "Flamenco staple", scaleType = "MINOR"),
        )

        val migrated = repo.getAll().single()
        assertEquals("a", migrated.id)
        assertEquals("Andalusian", migrated.progression.name)
        assertEquals("Flamenco staple", migrated.progression.description)
        assertEquals(ScaleType.MINOR, migrated.progression.scaleType)
        assertEquals(1_000L, migrated.createdAt)
        assertEquals(listOf(0, 5, 7), migrated.progression.degrees.map { it.interval })
        assertEquals(listOf("I", "IV", "V"), migrated.progression.degrees.map { it.numeral })
    }

    @Test
    fun legacyMigrationRemovesTheLegacyKeysAndWritesJson() {
        writeRaw("old_a", legacyEntry("a", "Migrated"))
        repo.getAll()

        assertNull("the legacy key should be consumed", prefs.getString("old_a", null))
        assertTrue(
            "the migrated data should land in the JSON key",
            prefs.getString(KEY_PROGRESSIONS, null)?.contains("Migrated") == true,
        )
    }

    @Test
    fun migratedDataSurvivesACorruptionBeforeTheNextSave() {
        // The cleanup sweeps out every key the store does not own, and the
        // backup persist() writes during the migration is one of the store's
        // own. Deleting it would leave the migrated data with no recovery copy
        // until the user's next save (#554).
        writeRaw("old_a", legacyEntry("a", "Migrated"))
        repo.getAll()
        writeRaw(KEY_PROGRESSIONS, "}} not json {{")

        assertEquals(
            "the migration's own backup should still be there to recover from",
            "Migrated",
            repo
                .getAll()
                .single()
                .progression.name,
        )
    }

    @Test
    fun legacyMigrationRunsOnlyOnce() {
        writeRaw("old_a", legacyEntry("a", "Migrated"))
        assertEquals(1, repo.getAll().size)
        assertEquals("a second read reuses the JSON store", 1, repo.getAll().size)
    }

    @Test
    fun legacyEntriesWithTooFewFieldsAreSkippedWithoutLosingTheRest() {
        writeRaw("old_bad", "a|||only|||three")
        writeRaw("old_good", legacyEntry("b", "Survivor"))

        assertEquals(listOf("b"), repo.getAll().map { it.id })
    }

    @Test
    fun legacyEntriesWithAnUnknownScaleTypeAreSkipped() {
        writeRaw("old_bad", legacyEntry("a", "Bad", scaleType = "KLINGON"))
        writeRaw("old_good", legacyEntry("b", "Survivor"))

        assertEquals(listOf("b"), repo.getAll().map { it.id })
    }

    @Test
    fun legacyEntriesWithMalformedDegreesAreSkipped() {
        writeRaw("old_bad", "a|||Bad|||Desc|||MAJOR|||not:a:number|||1000")
        writeRaw("old_good", legacyEntry("b", "Survivor"))

        assertEquals(listOf("b"), repo.getAll().map { it.id })
    }

    @Test
    fun legacyEntriesWithANonNumericTimestampAreSkipped() {
        writeRaw("old_bad", "a|||Name|||Desc|||MAJOR|||0:_:I|||yesterday")
        writeRaw("old_good", legacyEntry("b", "Survivor"))

        assertEquals(listOf("b"), repo.getAll().map { it.id })
    }

    @Test
    fun legacyMigrationUnescapesPipesInNameAndDescription() {
        writeRaw("old_a", legacyEntry("a", "Verse \\| Chorus", description = "A \\| B"))

        val migrated = repo.getAll().single()
        assertEquals("Verse | Chorus", migrated.progression.name)
        assertEquals("A | B", migrated.progression.description)
    }

    private companion object {
        const val DEGREE_SPEC = "0:_:I;5:_:IV;7:_:V"
        const val PREFS_NAME = "custom_progressions"
        const val KEY_PROGRESSIONS = "progressions_json"
        const val BACKUP_KEY = "progressions_json_backup"
        const val QUARANTINE_KEY = "progressions_json_quarantine"
    }
}
