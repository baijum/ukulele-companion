package com.baijum.ukufretboard.data

import android.app.Application
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
 * What each list store does when neither stored copy of its payload can be
 * read.
 *
 * Every store used to answer that question for itself, and every one of them
 * answered it by throwing the bytes away. The recovery contract they were meant
 * to follow lived in `JsonListRepository.getAll()`, which each subclass
 * overrode, so the quarantine slot was written by code nothing called (#564).
 * One test per store, because routing them onto the shared rule changes what
 * each of them does on disk.
 */
@RunWith(RobolectricTestRunner::class)
class UnreadablePayloadQuarantineTest {
    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    /** Leaves both stored copies of [key] unreadable. */
    private fun corruptBothCopies(
        prefsName: String,
        key: String,
    ): SharedPreferences {
        val prefs = context.getSharedPreferences(prefsName, 0)
        prefs
            .edit()
            .putString(key, CORRUPT)
            .putString("${key}_backup", "ALSO UNREADABLE")
            .commit()
        return prefs
    }

    @Test
    fun chordSheetUnreadableCopiesAreQuarantined() {
        val prefs = corruptBothCopies("chord_sheets", "sheets_json")

        assertTrue(ChordSheetRepository(context).getAll().isEmpty())
        assertEquals(CORRUPT, prefs.getString("sheets_json_quarantine", null))
    }

    @Test
    fun setlistUnreadableCopiesAreQuarantined() {
        val prefs = corruptBothCopies("setlists", "setlist_data")

        assertTrue(SetlistRepository(context).getAll().isEmpty())
        assertEquals(CORRUPT, prefs.getString("setlist_data_quarantine", null))
    }

    @Test
    fun strumPatternUnreadableCopiesAreQuarantined() {
        val prefs = corruptBothCopies("custom_strum_patterns", "patterns_json")

        assertTrue(CustomStrumPatternRepository(context).getAll().isEmpty())
        assertEquals(CORRUPT, prefs.getString("patterns_json_quarantine", null))
    }

    @Test
    fun fingerpickingPatternUnreadableCopiesAreQuarantined() {
        val prefs = corruptBothCopies("custom_fingerpicking_patterns", "patterns_json")

        assertTrue(CustomFingerpickingPatternRepository(context).getAll().isEmpty())
        assertEquals(CORRUPT, prefs.getString("patterns_json_quarantine", null))
    }

    @Test
    fun favoritesUnreadableCopiesAreQuarantined() {
        val prefs = corruptBothCopies("chord_favorites", "favorites_json")

        assertTrue(FavoritesRepository(context).getAll().isEmpty())
        assertEquals(CORRUPT, prefs.getString("favorites_json_quarantine", null))
    }

    @Test
    fun favoriteFoldersUnreadableCopiesAreQuarantined() {
        val prefs = corruptBothCopies("favorite_folders", "folders_json")

        assertTrue(FavoritesRepository(context).getAllFolders().isEmpty())
        assertEquals(CORRUPT, prefs.getString("folders_json_quarantine", null))
    }

    @Test
    fun quarantinedBytesOutliveTheSaveThatFollowsTheLoss() {
        val prefs = corruptBothCopies("chord_sheets", "sheets_json")
        val repo = ChordSheetRepository(context)
        repo.getAll()

        repo.save(
            ChordSheet(
                id = "cs1",
                title = "Added after the loss",
                artist = "",
                content = "",
                createdAt = 1L,
                updatedAt = 2L,
            ),
        )

        assertEquals(listOf("cs1"), repo.getAll().map { it.id })
        assertEquals(
            "the save overwrites both copies, so only the quarantine still holds the original",
            CORRUPT,
            prefs.getString("sheets_json_quarantine", null),
        )
    }

    @Test
    fun setlistLegacyOrgJsonIsPreferredOverTheBackup() {
        // The setlist store consults its legacy org.json format before the
        // backup, so a primary that kotlinx cannot read but org.json can is
        // migrated in place rather than rolled back to an older copy. org.json
        // accepts unquoted keys and single-quoted strings; kotlinx does not.
        val legacy = "[{id:'s1',name:'Legacy',songIds:['a'],createdAt:100,updatedAt:200}]"
        val backup = """[{"id":"s2","name":"FromBackup","songIds":[],"createdAt":300,"updatedAt":400}]"""
        val prefs = context.getSharedPreferences("setlists", 0)
        prefs
            .edit()
            .putString("setlist_data", legacy)
            .putString("setlist_data_backup", backup)
            .commit()

        assertEquals("Legacy", SetlistRepository(context).getAll().single().name)
        assertNull(
            "a migrated primary is not a loss, so nothing is quarantined",
            prefs.getString("setlist_data_quarantine", null),
        )
    }

    private companion object {
        /** Stands in for a payload that neither parser can make sense of. */
        const val CORRUPT = "}} not json {{"
    }
}
