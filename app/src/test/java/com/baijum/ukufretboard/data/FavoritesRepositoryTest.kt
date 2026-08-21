package com.baijum.ukufretboard.data

import android.app.Application
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What [FavoritesRepository] writes to disk when one of its two stores has a
 * corrupt primary copy.
 *
 * This class predates [JsonListRepository] and keeps its own storage, so the
 * write side was never carried along when the rotation rule was fixed there
 * (#553) and extracted into [writeWithBackupRotation] (#561). It rotated the
 * outgoing payload into the backup unconditionally, which promoted corrupt
 * bytes over the last readable copy — on nothing more dangerous than starring a
 * chord (#567). Both of its stores are covered, because they are two separate
 * preference files with two separate write paths.
 */
@RunWith(RobolectricTestRunner::class)
class FavoritesRepositoryTest {
    private lateinit var context: Application
    private lateinit var prefs: SharedPreferences
    private lateinit var folderPrefs: SharedPreferences
    private lateinit var repo: FavoritesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences(PREFS_NAME, 0)
        folderPrefs = context.getSharedPreferences(FOLDER_PREFS_NAME, 0)
        repo = FavoritesRepository(context)
    }

    private fun voicing(
        root: Int,
        symbol: String = "maj",
        frets: List<Int> = listOf(0, 0, 0, 3),
    ) = FavoriteVoicing(rootPitchClass = root, chordSymbol = symbol, frets = frets, addedAt = 1L)

    private fun writeRaw(
        prefs: SharedPreferences,
        key: String,
        value: String,
    ) {
        prefs.edit().putString(key, value).commit()
    }

    // ── Voicings ─────────────────────────────────────────────────────

    @Test
    fun starringAChordOverACorruptPrimaryLeavesTheLastGoodBackupInPlace() {
        repo.add(voicing(0))
        val lastGood = prefs.getString(BACKUP_KEY_FAVORITES, null)
        writeRaw(prefs, KEY_FAVORITES, CORRUPT)

        repo.add(voicing(5))

        assertEquals(
            "the corrupt primary must not be promoted over the last readable copy",
            lastGood,
            prefs.getString(BACKUP_KEY_FAVORITES, null),
        )
    }

    @Test
    fun aStarredChordOverACorruptPrimaryLeavesFavoritesRecoverableAfterTheNextCorruption() {
        repo.add(voicing(0))
        writeRaw(prefs, KEY_FAVORITES, CORRUPT)
        repo.add(voicing(5))

        // Second corruption: the surviving backup is what keeps the data alive.
        writeRaw(prefs, KEY_FAVORITES, "corrupt again")

        assertEquals(
            "the backup should still serve the last good copy",
            listOf(voicing(0).key),
            repo.getAll().map { it.key },
        )
        assertNull(
            "a recoverable read must not quarantine",
            prefs.getString(QUARANTINE_KEY_FAVORITES, null),
        )
    }

    @Test
    fun voicingBackupLagsExactlyOnePersistBehindThePrimary() {
        repo.add(voicing(0))
        val afterFirst = prefs.getString(KEY_FAVORITES, null)
        repo.add(voicing(5))

        assertEquals(
            "the backup should hold the previous primary",
            afterFirst,
            prefs.getString(BACKUP_KEY_FAVORITES, null),
        )
    }

    @Test
    fun persistReseedsTheVoicingBackupWhenNeitherStoredCopyIsReadable() {
        writeRaw(prefs, KEY_FAVORITES, CORRUPT)
        writeRaw(prefs, BACKUP_KEY_FAVORITES, "also corrupt")

        repo.add(voicing(0))

        assertEquals(
            "with nothing worth keeping, the backup is re-seeded with the new payload",
            prefs.getString(KEY_FAVORITES, null),
            prefs.getString(BACKUP_KEY_FAVORITES, null),
        )
    }

    // ── Folders ──────────────────────────────────────────────────────

    @Test
    fun savingAFolderOverACorruptPrimaryLeavesTheLastGoodBackupInPlace() {
        repo.saveFolder(FavoriteFolder(id = "f1", name = "Jazz", createdAt = 1L))
        val lastGood = folderPrefs.getString(BACKUP_KEY_FOLDERS, null)
        writeRaw(folderPrefs, KEY_FOLDERS, CORRUPT)

        repo.saveFolder(FavoriteFolder(id = "f2", name = "Blues", createdAt = 2L))

        assertEquals(
            "the corrupt primary must not be promoted over the last readable copy",
            lastGood,
            folderPrefs.getString(BACKUP_KEY_FOLDERS, null),
        )
    }

    @Test
    fun aSavedFolderOverACorruptPrimaryLeavesFoldersRecoverableAfterTheNextCorruption() {
        repo.saveFolder(FavoriteFolder(id = "f1", name = "Jazz", createdAt = 1L))
        writeRaw(folderPrefs, KEY_FOLDERS, CORRUPT)
        repo.saveFolder(FavoriteFolder(id = "f2", name = "Blues", createdAt = 2L))

        writeRaw(folderPrefs, KEY_FOLDERS, "corrupt again")

        assertEquals(
            "the backup should still serve the last good copy",
            listOf("f1"),
            repo.getAllFolders().map { it.id },
        )
        assertNull(
            "a recoverable read must not quarantine",
            folderPrefs.getString(QUARANTINE_KEY_FOLDERS, null),
        )
    }

    @Test
    fun persistReseedsTheFolderBackupWhenNeitherStoredCopyIsReadable() {
        writeRaw(folderPrefs, KEY_FOLDERS, CORRUPT)
        writeRaw(folderPrefs, BACKUP_KEY_FOLDERS, "also corrupt")

        repo.saveFolder(FavoriteFolder(id = "f1", name = "Jazz", createdAt = 1L))

        assertEquals(
            "with nothing worth keeping, the backup is re-seeded with the new payload",
            folderPrefs.getString(KEY_FOLDERS, null),
            folderPrefs.getString(BACKUP_KEY_FOLDERS, null),
        )
    }

    private companion object {
        const val PREFS_NAME = "chord_favorites"
        const val KEY_FAVORITES = "favorites_json"
        const val BACKUP_KEY_FAVORITES = "favorites_json_backup"
        const val QUARANTINE_KEY_FAVORITES = "favorites_json_quarantine"
        const val FOLDER_PREFS_NAME = "favorite_folders"
        const val KEY_FOLDERS = "folders_json"
        const val BACKUP_KEY_FOLDERS = "folders_json_backup"
        const val QUARANTINE_KEY_FOLDERS = "folders_json_quarantine"

        /** Stands in for a payload the parser cannot make sense of. */
        const val CORRUPT = "}} not json {{"
    }
}
