package com.baijum.ukufretboard.data

import com.baijum.ukufretboard.data.sync.BackupChordSheet
import com.baijum.ukufretboard.data.sync.BackupData
import com.baijum.ukufretboard.data.sync.BackupFavorite
import com.baijum.ukufretboard.data.sync.BackupFavoriteFolder
import com.baijum.ukufretboard.data.sync.BackupLearningProgress
import com.baijum.ukufretboard.data.sync.BackupPracticeTimer
import com.baijum.ukufretboard.data.sync.BackupSetlist
import com.baijum.ukufretboard.data.sync.BackupSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun currentVersionIs3() {
        assertEquals(3, BackupData.CURRENT_VERSION)
    }

    @Test
    fun defaultBackupDataHasCorrectVersion() {
        val data = BackupData()
        assertEquals(3, data.version)
    }

    @Test
    fun emptyBackupSerializesAndDeserializes() {
        val data = BackupData(exportedAt = 12345L)
        val jsonStr = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(jsonStr)
        assertEquals(data.version, decoded.version)
        assertEquals(data.exportedAt, decoded.exportedAt)
        assertTrue(decoded.favorites.isEmpty())
        assertTrue(decoded.chordSheets.isEmpty())
        assertTrue(decoded.knownChords.isEmpty())
    }

    @Test
    fun backupWithFavoritesRoundTrips() {
        val fav = BackupFavorite(
            rootPitchClass = 0,
            chordSymbol = "",
            frets = listOf(0, 0, 0, 3),
            addedAt = 1000L,
        )
        val data = BackupData(
            exportedAt = 12345L,
            favorites = listOf(fav),
        )
        val jsonStr = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(jsonStr)
        assertEquals(1, decoded.favorites.size)
        assertEquals(0, decoded.favorites[0].rootPitchClass)
        assertEquals(listOf(0, 0, 0, 3), decoded.favorites[0].frets)
    }

    @Test
    fun backupWithFoldersRoundTrips() {
        val folder = BackupFavoriteFolder(
            id = "folder-1",
            name = "My Folder",
            createdAt = 2000L,
        )
        val data = BackupData(
            exportedAt = 12345L,
            favoriteFolders = listOf(folder),
        )
        val jsonStr = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(jsonStr)
        assertEquals(1, decoded.favoriteFolders.size)
        assertEquals("My Folder", decoded.favoriteFolders[0].name)
    }

    @Test
    fun backupWithKnownChordsRoundTrips() {
        val data = BackupData(
            exportedAt = 12345L,
            knownChords = listOf("C", "Am", "F", "G"),
        )
        val jsonStr = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(jsonStr)
        assertEquals(listOf("C", "Am", "F", "G"), decoded.knownChords)
    }

    @Test
    fun defaultListsAreEmpty() {
        val data = BackupData()
        assertTrue(data.favorites.isEmpty())
        assertTrue(data.favoriteFolders.isEmpty())
        assertTrue(data.chordSheets.isEmpty())
        assertTrue(data.customProgressions.isEmpty())
        assertTrue(data.customStrumPatterns.isEmpty())
        assertTrue(data.customFingerpickingPatterns.isEmpty())
        assertTrue(data.melodies.isEmpty())
        assertTrue(data.knownChords.isEmpty())
        assertTrue(data.setlists.isEmpty())
        assertTrue(data.achievements.isEmpty())
    }

    @Test
    fun backupWithSetlistsRoundTrips() {
        val setlist = BackupSetlist(
            id = "sl-1",
            name = "Friday Gig",
            songIds = listOf("song-1", "song-2", "song-3"),
            createdAt = 5000L,
            updatedAt = 6000L,
        )
        val data = BackupData(
            exportedAt = 12345L,
            setlists = listOf(setlist),
        )
        val jsonStr = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(jsonStr)
        assertEquals(1, decoded.setlists.size)
        assertEquals("sl-1", decoded.setlists[0].id)
        assertEquals("Friday Gig", decoded.setlists[0].name)
        assertEquals(listOf("song-1", "song-2", "song-3"), decoded.setlists[0].songIds)
        assertEquals(5000L, decoded.setlists[0].createdAt)
        assertEquals(6000L, decoded.setlists[0].updatedAt)
    }

    @Test
    fun backupChordSheetViewStatsRoundTrip() {
        val sheet = BackupChordSheet(
            id = "cs-1",
            title = "Test Song",
            content = "[C]Hello",
            createdAt = 1000L,
            updatedAt = 2000L,
            viewCount = 5,
            lastViewedAt = 12345L,
            totalViewTimeMs = 60000L,
        )
        val data = BackupData(
            exportedAt = 12345L,
            chordSheets = listOf(sheet),
        )
        val jsonStr = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(jsonStr)
        assertEquals(1, decoded.chordSheets.size)
        assertEquals(5, decoded.chordSheets[0].viewCount)
        assertEquals(12345L, decoded.chordSheets[0].lastViewedAt)
        assertEquals(60000L, decoded.chordSheets[0].totalViewTimeMs)
    }

    @Test
    fun backupChordSheetViewStatsDefaultToZero() {
        val sheet = BackupChordSheet(
            id = "cs-2",
            title = "No Stats",
            content = "[Am]Lyrics",
            createdAt = 1000L,
            updatedAt = 2000L,
        )
        assertEquals(0, sheet.viewCount)
        assertEquals(0L, sheet.lastViewedAt)
        assertEquals(0L, sheet.totalViewTimeMs)
    }

    @Test
    fun backwardCompatMissingFieldsGetDefaults() {
        val oldJson = """
            {
                "version": 3,
                "exportedAt": 12345,
                "chordSheets": [{
                    "id": "cs-old",
                    "title": "Old Song",
                    "content": "[C]Test",
                    "createdAt": 1000,
                    "updatedAt": 2000
                }]
            }
        """.trimIndent()
        val decoded = json.decodeFromString<BackupData>(oldJson)
        assertEquals(1, decoded.chordSheets.size)
        assertEquals(0, decoded.chordSheets[0].viewCount)
        assertEquals(0L, decoded.chordSheets[0].lastViewedAt)
        assertEquals(0L, decoded.chordSheets[0].totalViewTimeMs)
        assertTrue(decoded.setlists.isEmpty())
        assertTrue(decoded.achievements.isEmpty())
        assertEquals(BackupPracticeTimer(), decoded.practiceTimer)
    }

    @Test
    fun backupWithAchievementsRoundTrips() {
        val achievements = mapOf(
            "first_chord" to 1000L,
            "ten_songs" to 2000L,
        )
        val data = BackupData(
            exportedAt = 12345L,
            achievements = achievements,
        )
        val jsonStr = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(jsonStr)
        assertEquals(2, decoded.achievements.size)
        assertEquals(1000L, decoded.achievements["first_chord"])
        assertEquals(2000L, decoded.achievements["ten_songs"])
    }

    @Test
    fun backupWithPracticeTimerRoundTrips() {
        val timer = BackupPracticeTimer(
            totalMinutes = 120,
            totalSessions = 15,
            longestSession = 30,
            lastSessionTime = 99999L,
            dailyGoal = 20,
            dailyMinutes = mapOf("2026-06-01" to 25, "2026-06-02" to 15),
        )
        val data = BackupData(
            exportedAt = 12345L,
            practiceTimer = timer,
        )
        val jsonStr = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(jsonStr)
        assertEquals(120, decoded.practiceTimer.totalMinutes)
        assertEquals(15, decoded.practiceTimer.totalSessions)
        assertEquals(30, decoded.practiceTimer.longestSession)
        assertEquals(99999L, decoded.practiceTimer.lastSessionTime)
        assertEquals(20, decoded.practiceTimer.dailyGoal)
        assertEquals(2, decoded.practiceTimer.dailyMinutes.size)
        assertEquals(25, decoded.practiceTimer.dailyMinutes["2026-06-01"])
    }

    @Test
    fun defaultAchievementsAndPracticeTimerAreEmpty() {
        val data = BackupData()
        assertTrue(data.achievements.isEmpty())
        assertEquals(0, data.practiceTimer.totalMinutes)
        assertEquals(0, data.practiceTimer.totalSessions)
        assertTrue(data.practiceTimer.dailyMinutes.isEmpty())
    }
}
