package com.baijum.ukufretboard.data

import com.baijum.ukufretboard.data.sync.BackupData
import com.baijum.ukufretboard.data.sync.BackupFavorite
import com.baijum.ukufretboard.data.sync.BackupFavoriteFolder
import com.baijum.ukufretboard.data.sync.BackupLearningProgress
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
    }
}
