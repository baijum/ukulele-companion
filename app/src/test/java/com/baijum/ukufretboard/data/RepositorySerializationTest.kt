package com.baijum.ukufretboard.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositorySerializationTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ── FavoritesRepository ──────────────────────────────────────────

    @Test
    fun favoritesRoundTrip() {
        val repo = FavoritesRepository(context)
        val voicing = FavoriteVoicing(
            rootPitchClass = 0,
            chordSymbol = "m7",
            frets = listOf(0, 2, 3, 3),
            addedAt = 1000L,
            folderIds = listOf("f1", "f2"),
        )
        repo.add(voicing)
        val loaded = repo.getAll()
        assertEquals(1, loaded.size)
        val v = loaded[0]
        assertEquals(0, v.rootPitchClass)
        assertEquals("m7", v.chordSymbol)
        assertEquals(listOf(0, 2, 3, 3), v.frets)
        assertEquals(1000L, v.addedAt)
        assertEquals(listOf("f1", "f2"), v.folderIds)
    }

    @Test
    fun favoritesEmptyFolderIds() {
        val repo = FavoritesRepository(context)
        val voicing = FavoriteVoicing(
            rootPitchClass = 7,
            chordSymbol = "",
            frets = listOf(0, 0, 0, 0),
            addedAt = 500L,
            folderIds = emptyList(),
        )
        repo.add(voicing)
        val loaded = repo.getAll().first()
        assertTrue(loaded.folderIds.isEmpty())
    }

    @Test
    fun favoritesImportUnionByKey() {
        val repo = FavoritesRepository(context)
        val existing = FavoriteVoicing(0, "maj", listOf(0, 0, 0, 3), 100L)
        repo.add(existing)
        val sameKey = FavoriteVoicing(0, "maj", listOf(0, 0, 0, 3), 200L, listOf("f1"))
        val newVoicing = FavoriteVoicing(4, "m", listOf(0, 4, 3, 2), 300L)
        repo.importAll(listOf(sameKey, newVoicing))
        val all = repo.getAll()
        assertEquals(2, all.size)
        val original = all.first { it.addedAt == 100L }
        assertTrue("Existing voicing should keep original folderIds", original.folderIds.isEmpty())
    }

    @Test
    fun favoritesFolderRoundTrip() {
        val repo = FavoritesRepository(context)
        val folder = FavoriteFolder(
            id = "folder-1",
            name = "Jazz Chords",
            createdAt = 2000L,
            voicingOrder = listOf("0|m7|0,2,3,3", "4|m|0,4,3,2"),
        )
        repo.saveFolder(folder)
        val loaded = repo.getAllFolders()
        assertEquals(1, loaded.size)
        assertEquals("folder-1", loaded[0].id)
        assertEquals("Jazz Chords", loaded[0].name)
        assertEquals(2000L, loaded[0].createdAt)
        assertEquals(2, loaded[0].voicingOrder.size)
    }

    @Test
    fun favoritesFolderImportUnionById() {
        val repo = FavoritesRepository(context)
        val existing = FavoriteFolder(id = "f1", name = "Existing", createdAt = 100L)
        repo.saveFolder(existing)
        val duplicate = FavoriteFolder(id = "f1", name = "Updated", createdAt = 200L)
        val newFolder = FavoriteFolder(id = "f2", name = "New", createdAt = 300L)
        repo.importFolders(listOf(duplicate, newFolder))
        val all = repo.getAllFolders()
        assertEquals(2, all.size)
        assertEquals("Existing", all.first { it.id == "f1" }.name)
    }

    @Test
    fun favoritesLegacyPipeMigration() {
        val prefs = context.getSharedPreferences("chord_favorites", 0)
        prefs.edit()
            .putString("0|m7|0,2,3,3", "0|m7|0,2,3,3|1000|f1;f2")
            .putString("4|m|0,4,3,2", "4|m|0,4,3,2|2000|")
            .apply()

        val repo = FavoritesRepository(context)
        val all = repo.getAll()
        assertEquals(2, all.size)
        val v1 = all.first { it.rootPitchClass == 0 }
        assertEquals("m7", v1.chordSymbol)
        assertEquals(listOf(0, 2, 3, 3), v1.frets)
        assertEquals(1000L, v1.addedAt)
        assertEquals(listOf("f1", "f2"), v1.folderIds)
        val v2 = all.first { it.rootPitchClass == 4 }
        assertTrue(v2.folderIds.isEmpty())

        assertTrue(
            "Legacy keys should be removed after migration",
            !prefs.contains("0|m7|0,2,3,3"),
        )
        assertTrue(
            "JSON key should exist after migration",
            prefs.contains("favorites_json"),
        )
    }

    @Test
    fun favoritesLegacyFolderPipeMigration() {
        val prefs = context.getSharedPreferences("favorite_folders", 0)
        prefs.edit()
            .putString("folder-1", "folder-1|||Jazz Chords|||2000|||0|m7|0,2,3,3;4|m|0,4,3,2")
            .apply()

        val repo = FavoritesRepository(context)
        val folders = repo.getAllFolders()
        assertEquals(1, folders.size)
        assertEquals("folder-1", folders[0].id)
        assertEquals("Jazz Chords", folders[0].name)
        assertEquals(2000L, folders[0].createdAt)
        assertEquals(2, folders[0].voicingOrder.size)

        assertTrue(
            "Legacy keys should be removed after migration",
            !prefs.contains("folder-1"),
        )
        assertTrue(
            "JSON key should exist after migration",
            prefs.contains("folders_json"),
        )
    }

    @Test
    fun favoritesCorruptJsonFallsBackToBackup() {
        val repo = FavoritesRepository(context)
        val voicing = FavoriteVoicing(0, "maj", listOf(0, 0, 0, 3), 100L)
        repo.add(voicing)
        assertEquals(1, repo.getAll().size)

        val prefs = context.getSharedPreferences("chord_favorites", 0)
        prefs.edit().putString("favorites_json", "CORRUPTED").apply()

        val recovered = repo.getAll()
        assertEquals("Backup key should provide recovery", 1, recovered.size)
        assertEquals(0, recovered[0].rootPitchClass)
    }

    // ── ChordSheetRepository ─────────────────────────────────────────

    @Test
    fun chordSheetRoundTrip() {
        val repo = ChordSheetRepository(context)
        val sheet = ChordSheet(
            id = "s1",
            title = "Somewhere Over the Rainbow",
            artist = "IZ",
            content = "[C]Somewhere [Em]over",
            key = "C",
            capo = 2,
            strumPatternName = "Island Strum",
            labels = listOf("Hawaiian", "Classic"),
            createdAt = 1000L,
            updatedAt = 2000L,
            subtitle = "From Oz",
            viewCount = 5,
            lastViewedAt = 1500L,
            totalViewTimeMs = 60000L,
        )
        repo.save(sheet)
        val loaded = repo.get("s1")
        assertNotNull(loaded)
        assertEquals("Somewhere Over the Rainbow", loaded!!.title)
        assertEquals("IZ", loaded.artist)
        assertEquals("[C]Somewhere [Em]over", loaded.content)
        assertEquals("C", loaded.key)
        assertEquals(2, loaded.capo)
        assertEquals(listOf("Hawaiian", "Classic"), loaded.labels)
        assertEquals("From Oz", loaded.subtitle)
        assertEquals(5, loaded.viewCount)
    }

    @Test
    fun chordSheetPipeInFields() {
        val repo = ChordSheetRepository(context)
        val sheet = ChordSheet(
            id = "pipe",
            title = "Title|With|Pipes",
            artist = "Artist|Name",
            content = "Content|||here",
            createdAt = 100L,
            updatedAt = 200L,
        )
        repo.save(sheet)
        val loaded = repo.get("pipe")
        assertNotNull(loaded)
        assertEquals("Title|With|Pipes", loaded!!.title)
        assertEquals("Artist|Name", loaded.artist)
        assertEquals("Content|||here", loaded.content)
    }

    @Test
    fun chordSheetLabelsWithSpecialChars() {
        val repo = ChordSheetRepository(context)
        val sheet = ChordSheet(
            id = "labels",
            title = "Test",
            content = "",
            labels = listOf("Rock, Pop", "Blues\\Jazz"),
            createdAt = 100L,
            updatedAt = 200L,
        )
        repo.save(sheet)
        val loaded = repo.get("labels")
        assertNotNull(loaded)
        assertEquals(listOf("Rock, Pop", "Blues\\Jazz"), loaded!!.labels)
    }

    @Test
    fun chordSheetImportLatestUpdatedAtWins() {
        val repo = ChordSheetRepository(context)
        val local = ChordSheet(id = "s1", title = "Old", content = "", createdAt = 100L, updatedAt = 200L)
        repo.save(local)
        val newerBackup = ChordSheet(id = "s1", title = "New", content = "", createdAt = 100L, updatedAt = 300L)
        val olderBackup = ChordSheet(id = "s1", title = "Ancient", content = "", createdAt = 100L, updatedAt = 100L)
        repo.importAll(listOf(newerBackup, olderBackup))
        val loaded = repo.get("s1")
        assertEquals("New", loaded!!.title)
    }

    // ── MelodyRepository ─────────────────────────────────────────────

    @Test
    fun melodyRoundTripWithAllNoteFields() {
        val repo = MelodyRepository(context)
        val melody = Melody(
            id = "m1",
            name = "Test Melody",
            notes = listOf(
                MelodyNote(pitchClass = 0, octave = 4, duration = NoteDuration.QUARTER, stringIndex = 2, fret = 3),
                MelodyNote(pitchClass = 7, octave = 5, duration = NoteDuration.EIGHTH, stringIndex = 0, fret = 0),
            ),
            bpm = 100,
            createdAt = 5000L,
        )
        repo.save(melody)
        val loaded = repo.get("m1")
        assertNotNull(loaded)
        assertEquals("Test Melody", loaded!!.name)
        assertEquals(2, loaded.notes.size)
        assertEquals(0, loaded.notes[0].pitchClass)
        assertEquals(2, loaded.notes[0].stringIndex)
        assertEquals(3, loaded.notes[0].fret)
        assertEquals(100, loaded.bpm)
    }

    @Test
    fun melodyNullFieldsRoundTrip() {
        val repo = MelodyRepository(context)
        val melody = Melody(
            id = "m2",
            name = "Rest Melody",
            notes = listOf(
                MelodyNote(pitchClass = null, octave = 4, duration = NoteDuration.HALF),
                MelodyNote(pitchClass = 5, octave = 3, duration = NoteDuration.WHOLE, stringIndex = null, fret = null),
            ),
            bpm = 80,
            createdAt = 6000L,
        )
        repo.save(melody)
        val loaded = repo.get("m2")
        assertNotNull(loaded)
        assertNull(loaded!!.notes[0].pitchClass)
        assertNull(loaded.notes[0].stringIndex)
        assertNull(loaded.notes[0].fret)
        assertNull(loaded.notes[1].stringIndex)
    }

    @Test
    fun melodyEmptyNotes() {
        val repo = MelodyRepository(context)
        val melody = Melody(id = "m3", name = "Empty", notes = emptyList(), bpm = 120, createdAt = 7000L)
        repo.save(melody)
        val loaded = repo.get("m3")
        assertNotNull(loaded)
        assertTrue(loaded!!.notes.isEmpty())
    }

    @Test
    fun melodyImportKeepsTheLocalCopyOnAnIdCollision() {
        val repo = MelodyRepository(context)
        val local = Melody(id = "m1", name = "Local", notes = emptyList(), bpm = 100, createdAt = 100L)
        repo.save(local)
        // Melody has no updatedAt and createdAt never changes after creation (#597),
        // so an import keeps the local copy on an ID collision instead of letting a
        // newer-looking createdAt in the backup overwrite local edits.
        val backup = Melody(id = "m1", name = "Backup", notes = emptyList(), bpm = 120, createdAt = 200L)
        repo.importAll(listOf(backup))
        assertEquals("Local", repo.get("m1")!!.name)
    }

    @Test
    fun melodyPipeInName() {
        val repo = MelodyRepository(context)
        val melody = Melody(id = "m4", name = "Name|With|Pipe", notes = emptyList(), bpm = 90, createdAt = 8000L)
        repo.save(melody)
        assertEquals("Name|With|Pipe", repo.get("m4")!!.name)
    }

    // ── SetlistRepository ────────────────────────────────────────────

    @Test
    fun setlistRoundTrip() {
        val repo = SetlistRepository(context)
        val setlist = Setlist(
            id = "sl1",
            name = "My Setlist",
            songIds = listOf("s1", "s2", "s3"),
            createdAt = 1000L,
            updatedAt = 2000L,
        )
        repo.save(setlist)
        val all = repo.getAll()
        assertEquals(1, all.size)
        assertEquals("My Setlist", all[0].name)
        assertEquals(listOf("s1", "s2", "s3"), all[0].songIds)
    }

    @Test
    fun setlistImportUpdatedAtWins() {
        val repo = SetlistRepository(context)
        val local = Setlist(id = "sl1", name = "Old", songIds = emptyList(), createdAt = 100L, updatedAt = 200L)
        repo.save(local)
        val newer = Setlist(id = "sl1", name = "New", songIds = listOf("a"), createdAt = 100L, updatedAt = 300L)
        repo.importAll(listOf(newer))
        assertEquals("New", repo.getAll().first { it.id == "sl1" }.name)
    }

    @Test
    fun setlistCorruptJsonReturnsEmpty() {
        val repo = SetlistRepository(context)
        val prefs = context.getSharedPreferences("setlists", 0)
        prefs.edit().putString("setlist_data", "not valid json").apply()
        assertTrue(repo.getAll().isEmpty())
    }

    // ── AchievementRepository ────────────────────────────────────────

    @Test
    fun achievementRoundTrip() {
        val repo = AchievementRepository(context)
        repo.unlock("streak_3")
        assertTrue(repo.isUnlocked("streak_3"))
        assertEquals(1, repo.unlockedCount())
    }

    @Test
    fun achievementImportUnionById() {
        val repo = AchievementRepository(context)
        repo.unlock("local_ach")
        repo.importAll(mapOf("local_ach" to 999L, "new_ach" to 888L))
        assertTrue(repo.isUnlocked("new_ach"))
        val all = repo.exportAll()
        assertEquals(2, all.size)
        assertTrue("Local timestamp preserved", all["local_ach"]!! != 999L)
    }

    @Test
    fun achievementCorruptJsonReturnsEmpty() {
        val repo = AchievementRepository(context)
        val prefs = context.getSharedPreferences("achievements", 0)
        prefs.edit().putString("unlocked", "broken{json").apply()
        assertTrue(repo.getUnlocked().isEmpty())
    }

    // ── PracticeTimerRepository ──────────────────────────────────────

    @Test
    fun practiceTimerExportImportMaxMerge() {
        val repo = PracticeTimerRepository(context)
        repo.recordSession(120_000) // 2 minutes
        val export = PracticeTimerExport(
            totalMinutes = 10,
            totalSessions = 5,
            longestSession = 8,
            lastSessionTime = 999L,
            dailyGoal = 30,
            dailyMinutes = mapOf("2025-01-01" to 15),
        )
        repo.importAll(export)
        assertEquals(10, repo.totalMinutes())
        assertEquals(5, repo.totalSessions())
        assertEquals(8, repo.longestSession())
        assertEquals(30, repo.dailyGoal())
    }

    @Test
    fun practiceTimerDefaultGoalNotOverwritten() {
        val repo = PracticeTimerRepository(context)
        repo.setDailyGoal(30)
        val export = PracticeTimerExport(dailyGoal = 15)
        repo.importAll(export)
        assertEquals("Default goal (15) should not overwrite custom goal", 30, repo.dailyGoal())
    }

    // ── CustomStrumPatternRepository ─────────────────────────────────

    @Test
    fun strumPatternRoundTrip() {
        val repo = CustomStrumPatternRepository(context)
        val pattern = CustomStrumPattern(
            id = "sp1",
            pattern = StrumPattern(
                name = "My Pattern",
                description = "Custom pattern",
                difficulty = Difficulty.BEGINNER,
                timeSignature = "3/4",
                beats = listOf(
                    StrumBeat(StrumDirection.DOWN, emphasis = true),
                    StrumBeat(StrumDirection.UP, emphasis = false),
                    StrumBeat(StrumDirection.CHUCK, emphasis = false),
                ),
                notation = "↓ ↑ X",
                suggestedBpm = 80..120,
            ),
            createdAt = 3000L,
        )
        repo.save(pattern)
        val loaded = repo.getAll()
        assertEquals(1, loaded.size)
        assertEquals("sp1", loaded[0].id)
        assertEquals("My Pattern", loaded[0].pattern.name)
        assertEquals("3/4", loaded[0].pattern.timeSignature)
        assertEquals(3, loaded[0].pattern.beats.size)
        assertEquals(StrumDirection.DOWN, loaded[0].pattern.beats[0].direction)
        assertTrue(loaded[0].pattern.beats[0].emphasis)
        assertEquals(StrumDirection.CHUCK, loaded[0].pattern.beats[2].direction)
    }

    @Test
    fun strumPatternMissingTimeSignatureDefaults() {
        val repo = CustomStrumPatternRepository(context)
        val prefs = context.getSharedPreferences("custom_strum_patterns", 0)
        prefs.edit().putString("sp-legacy", "sp-legacy|||Old Pat|||DOWN:true;UP:false|||9000").apply()
        val loaded = repo.getAll()
        assertEquals(1, loaded.size)
        assertEquals("4/4", loaded[0].pattern.timeSignature)
    }

    @Test
    fun strumPatternImportUnionById() {
        val repo = CustomStrumPatternRepository(context)
        val existing = CustomStrumPattern(
            id = "sp1",
            pattern = StrumPattern("Existing", "", Difficulty.BEGINNER, beats = listOf(StrumBeat(StrumDirection.DOWN)), notation = "D", suggestedBpm = 80..120),
            createdAt = 100L,
        )
        repo.save(existing)
        val duplicate = CustomStrumPattern(
            id = "sp1",
            pattern = StrumPattern("Duplicate", "", Difficulty.BEGINNER, beats = listOf(StrumBeat(StrumDirection.UP)), notation = "U", suggestedBpm = 80..120),
            createdAt = 200L,
        )
        repo.importAll(listOf(duplicate))
        assertEquals("Existing", repo.getAll().first().pattern.name)
    }

    // ── CustomFingerpickingPatternRepository ─────────────────────────

    @Test
    fun fingerpickingPatternRoundTrip() {
        val repo = CustomFingerpickingPatternRepository(context)
        val pattern = CustomFingerpickingPattern(
            id = "fp1",
            pattern = FingerpickingPattern(
                name = "Travis Pick",
                description = "Custom pattern",
                difficulty = Difficulty.INTERMEDIATE,
                timeSignature = "4/4",
                steps = listOf(
                    FingerpickStep(Finger.THUMB, 0, emphasis = true),
                    FingerpickStep(Finger.INDEX, 2, emphasis = false),
                    FingerpickStep(Finger.MIDDLE, 3, emphasis = false),
                ),
                notation = "T(G) I(E) M(A)",
                suggestedBpm = 60..100,
            ),
            createdAt = 4000L,
        )
        repo.save(pattern)
        val loaded = repo.getAll()
        assertEquals(1, loaded.size)
        assertEquals("fp1", loaded[0].id)
        assertEquals("Travis Pick", loaded[0].pattern.name)
        assertEquals(3, loaded[0].pattern.steps.size)
        assertEquals(Finger.THUMB, loaded[0].pattern.steps[0].finger)
        assertEquals(0, loaded[0].pattern.steps[0].stringIndex)
    }

    @Test
    fun fingerpickingLegacyIntegerTimeSignature() {
        val repo = CustomFingerpickingPatternRepository(context)
        val prefs = context.getSharedPreferences("custom_fingerpicking_patterns", 0)
        prefs.edit().putString("fp-leg", "fp-leg|||Legacy|||THUMB:0:true;INDEX:2:false|||5000|||3").apply()
        val loaded = repo.getAll()
        assertEquals(1, loaded.size)
        assertEquals("3/4", loaded[0].pattern.timeSignature)
    }

    @Test
    fun fingerpickingImportUnionById() {
        val repo = CustomFingerpickingPatternRepository(context)
        val existing = CustomFingerpickingPattern(
            id = "fp1",
            pattern = FingerpickingPattern("Existing", "", Difficulty.BEGINNER, steps = listOf(FingerpickStep(Finger.THUMB, 0)), notation = "T(G)", suggestedBpm = 60..100),
            createdAt = 100L,
        )
        repo.save(existing)
        val duplicate = CustomFingerpickingPattern(
            id = "fp1",
            pattern = FingerpickingPattern("Duplicate", "", Difficulty.BEGINNER, steps = listOf(FingerpickStep(Finger.INDEX, 2)), notation = "I(E)", suggestedBpm = 60..100),
            createdAt = 200L,
        )
        repo.importAll(listOf(duplicate))
        assertEquals("Existing", repo.getAll().first().pattern.name)
    }

    // ── Data-loss prevention ───────────────────────────────────────

    @Test
    fun setlistCorruptJsonFallsBackToBackup() {
        val repo = SetlistRepository(context)
        val setlist = Setlist(id = "s1", name = "Important", songIds = listOf("a"), createdAt = 100L, updatedAt = 200L)
        repo.save(setlist)
        assertEquals(1, repo.getAll().size)

        val prefs = context.getSharedPreferences("setlists", 0)
        prefs.edit().putString("setlist_data", "CORRUPTED").apply()

        val recovered = repo.getAll()
        assertEquals("Backup key should provide recovery", 1, recovered.size)
        assertEquals("Important", recovered[0].name)
    }

    @Test
    fun chordSheetCorruptJsonFallsBackToLegacyMigration() {
        val repo = ChordSheetRepository(context)
        val sheet = ChordSheet(id = "cs1", title = "My Song", artist = "", content = "[C]Hello", createdAt = 100L, updatedAt = 200L)
        repo.save(sheet)
        assertEquals(1, repo.getAll().size)

        val prefs = context.getSharedPreferences("chord_sheets", 0)
        prefs.edit().putString("sheets_json", "CORRUPTED").apply()

        val recovered = repo.getAll()
        assertEquals("Backup key should provide recovery", 1, recovered.size)
        assertEquals("My Song", recovered[0].title)
    }

    @Test
    fun saveAfterCorruptionPreservesBackup() {
        val repo = SetlistRepository(context)
        val original = Setlist(id = "s1", name = "Original", songIds = emptyList(), createdAt = 100L, updatedAt = 200L)
        repo.save(original)

        val prefs = context.getSharedPreferences("setlists", 0)
        prefs.edit().putString("setlist_data", "CORRUPTED").apply()

        val newItem = Setlist(id = "s2", name = "New", songIds = emptyList(), createdAt = 300L, updatedAt = 400L)
        repo.save(newItem)

        val all = repo.getAll()
        assertTrue("New item should be saved", all.any { it.id == "s2" })
        assertTrue("Original should be recovered from backup", all.any { it.id == "s1" })
    }
}
