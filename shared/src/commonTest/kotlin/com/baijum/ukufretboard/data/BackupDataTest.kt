package com.baijum.ukufretboard.data

import com.baijum.ukufretboard.data.sync.BackupChordDegree
import com.baijum.ukufretboard.data.sync.BackupChordSheet
import com.baijum.ukufretboard.data.sync.BackupData
import com.baijum.ukufretboard.data.sync.BackupFavorite
import com.baijum.ukufretboard.data.sync.BackupFavoriteFolder
import com.baijum.ukufretboard.data.sync.BackupLearningProgress
import com.baijum.ukufretboard.data.sync.BackupMelody
import com.baijum.ukufretboard.data.sync.BackupMelodyNote
import com.baijum.ukufretboard.data.sync.BackupPracticeTimer
import com.baijum.ukufretboard.data.sync.BackupProgression
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

    /**
     * Verifies that a JSON string representing a normalized iOS backup
     * (with KMP field names and structure) round-trips correctly through
     * the KMP [BackupData] deserializer. This tests the end-to-end path
     * that Android's `normalizeIosFormat()` produces.
     */
    @Test
    fun normalizedIosFormatDeserializesCorrectly() {
        val iosNormalized = """
        {
            "version": 3,
            "exportedAt": 1717430400000,
            "favorites": [
                {
                    "rootPitchClass": 0,
                    "chordSymbol": "",
                    "frets": [0, 0, 0, 3],
                    "addedAt": 1717430400000,
                    "folderIds": ["f1"]
                }
            ],
            "favoriteFolders": [
                {
                    "id": "f1",
                    "name": "Jazz",
                    "createdAt": 1717430400000
                }
            ],
            "chordSheets": [
                {
                    "id": "cs-1",
                    "title": "Test Song",
                    "content": "[C]Hello [G]World",
                    "createdAt": 1717430400000,
                    "updatedAt": 1717430400000
                }
            ],
            "customProgressions": [
                {
                    "id": "cp-1",
                    "name": "I-IV-V",
                    "description": "Basic",
                    "scaleType": "MAJOR",
                    "degrees": [
                        {"interval": 0, "quality": "major", "numeral": "I"},
                        {"interval": 5, "quality": "major", "numeral": "IV"},
                        {"interval": 7, "quality": "major", "numeral": "V"}
                    ],
                    "createdAt": 1717430400000
                }
            ],
            "melodies": [
                {
                    "id": "m-1",
                    "name": "Test Melody",
                    "notes": [
                        {"pitchClass": 0, "octave": 4, "duration": "QUARTER"},
                        {"pitchClass": 4, "octave": 4, "duration": "HALF"},
                        {"octave": 4, "duration": "EIGHTH"}
                    ],
                    "bpm": 120,
                    "createdAt": 1717430400000
                }
            ],
            "setlists": [
                {
                    "id": "sl-1",
                    "name": "Gig Set",
                    "songIds": ["cs-1"],
                    "createdAt": 1717430400000,
                    "updatedAt": 1717430400000
                }
            ],
            "learningProgress": {
                "entries": {
                    "lesson_done_chords": "true",
                    "quiz_total_ALL": "42",
                    "streak_days": "5"
                }
            },
            "settings": {
                "soundEnabled": true,
                "volume": 0.8,
                "noteDurationMs": 500,
                "strumDelayMs": 40,
                "strumDown": true,
                "playOnTap": false,
                "themeMode": "DARK",
                "showExplorerTips": true,
                "showLearnSection": true,
                "showReferenceSection": true,
                "tuning": "LOW_G",
                "leftHanded": true,
                "lastFret": 15,
                "showNoteNames": false
            },
            "achievements": {
                "first_chord": 1717430400000,
                "ten_songs": 1717430400000
            },
            "practiceTimer": {
                "totalMinutes": 60,
                "totalSessions": 10,
                "longestSession": 20,
                "lastSessionTime": 1717430400000,
                "dailyGoal": 15,
                "dailyMinutes": {"2026-06-01": 25}
            },
            "knownChords": []
        }
        """.trimIndent()

        val decoded = json.decodeFromString<BackupData>(iosNormalized)

        assertEquals(3, decoded.version)
        assertEquals(1717430400000L, decoded.exportedAt)

        assertEquals(1, decoded.favorites.size)
        assertEquals(0, decoded.favorites[0].rootPitchClass)
        assertEquals(listOf("f1"), decoded.favorites[0].folderIds)

        assertEquals(1, decoded.favoriteFolders.size)
        assertEquals("Jazz", decoded.favoriteFolders[0].name)

        assertEquals(1, decoded.chordSheets.size)
        assertEquals("Test Song", decoded.chordSheets[0].title)

        assertEquals(1, decoded.customProgressions.size)
        assertEquals("I-IV-V", decoded.customProgressions[0].name)
        assertEquals(3, decoded.customProgressions[0].degrees.size)
        assertEquals(0, decoded.customProgressions[0].degrees[0].interval)
        assertEquals("major", decoded.customProgressions[0].degrees[0].quality)
        assertEquals("I", decoded.customProgressions[0].degrees[0].numeral)

        assertEquals(1, decoded.melodies.size)
        assertEquals(3, decoded.melodies[0].notes.size)
        assertEquals("QUARTER", decoded.melodies[0].notes[0].duration)
        assertEquals("HALF", decoded.melodies[0].notes[1].duration)
        assertEquals(null, decoded.melodies[0].notes[2].pitchClass)

        assertEquals(1, decoded.setlists.size)
        assertEquals("Gig Set", decoded.setlists[0].name)

        assertEquals("true", decoded.learningProgress.entries["lesson_done_chords"])
        assertEquals("42", decoded.learningProgress.entries["quiz_total_ALL"])

        assertTrue(decoded.settings.leftHanded)
        assertEquals("DARK", decoded.settings.themeMode)
        assertEquals("LOW_G", decoded.settings.tuning)
        assertEquals(15, decoded.settings.lastFret)

        assertEquals(2, decoded.achievements.size)
        assertEquals(1717430400000L, decoded.achievements["first_chord"])

        assertEquals(60, decoded.practiceTimer.totalMinutes)
        assertEquals(10, decoded.practiceTimer.totalSessions)
        assertEquals(25, decoded.practiceTimer.dailyMinutes["2026-06-01"])
    }
}
