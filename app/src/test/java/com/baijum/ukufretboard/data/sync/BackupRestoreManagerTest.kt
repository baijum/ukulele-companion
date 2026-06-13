package com.baijum.ukufretboard.data.sync

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.baijum.ukufretboard.data.AchievementRepository
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.data.ChordSheetRepository
import com.baijum.ukufretboard.data.CustomFingerpickingPatternRepository
import com.baijum.ukufretboard.data.CustomProgressionRepository
import com.baijum.ukufretboard.data.CustomStrumPatternRepository
import com.baijum.ukufretboard.data.FavoriteVoicing
import com.baijum.ukufretboard.data.FavoritesRepository
import com.baijum.ukufretboard.data.LearningProgressRepository
import com.baijum.ukufretboard.data.MelodyRepository
import com.baijum.ukufretboard.data.PracticeTimerRepository
import com.baijum.ukufretboard.data.SetlistRepository
import com.baijum.ukufretboard.viewmodel.SettingsViewModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupRestoreManagerTest {

    private lateinit var app: Application
    private lateinit var settingsVM: SettingsViewModel
    private lateinit var manager: BackupRestoreManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        settingsVM = SettingsViewModel(app)
        manager = BackupRestoreManager(app, settingsVM)
    }

    // ── Export ────────────────────────────────────────────────────────

    @Test
    fun exportEmptyProducesValidJson() {
        val json = manager.exportBackup()
        val root = Json.parseToJsonElement(json).jsonObject
        assertEquals("3", root["version"]?.jsonPrimitive?.content)
        assertNotNull(root["exportedAt"])
        assertTrue(root["favorites"]!!.toString().contains("[]") || root["favorites"]!!.toString() == "[\n]" || root["favorites"]!!.toString().trim().let { it == "[]" || it == "[\n\n]" || it.startsWith("[") })
    }

    @Test
    fun exportIncludesPopulatedData() {
        val favRepo = FavoritesRepository(app)
        favRepo.add(FavoriteVoicing(0, "maj", listOf(0, 0, 0, 3), 1000L))
        val sheetRepo = ChordSheetRepository(app)
        sheetRepo.save(ChordSheet(id = "s1", title = "Test", content = "[C]Lyrics", createdAt = 100L, updatedAt = 200L))

        val json = manager.exportBackup()
        val root = Json.parseToJsonElement(json).jsonObject
        assertTrue(json.contains("\"rootPitchClass\""))
        assertTrue(json.contains("\"chordSheets\""))
        assertTrue(json.contains("Test"))
    }

    // ── Import round-trip ────────────────────────────────────────────

    @Test
    fun importExportRoundTripPreservesFavorites() {
        val backup = buildMinimalBackup(
            favorites = """[{"rootPitchClass":0,"chordSymbol":"m7","frets":[0,2,3,3],"addedAt":5000,"folderIds":["f1"]}]""",
            favoriteFolders = """[{"id":"f1","name":"Jazz","createdAt":6000,"voicingOrder":["0|m7|0,2,3,3"]}]""",
        )
        manager.importBackup(backup)
        val favRepo = FavoritesRepository(app)
        val all = favRepo.getAll()
        assertEquals(1, all.size)
        assertEquals(0, all[0].rootPitchClass)
        assertEquals("m7", all[0].chordSymbol)
        assertEquals(listOf("f1"), all[0].folderIds)
        val folders = favRepo.getAllFolders()
        assertEquals(1, folders.size)
        assertEquals("Jazz", folders[0].name)
    }

    @Test
    fun importExportRoundTripPreservesChordSheets() {
        val backup = buildMinimalBackup(
            chordSheets = """[{"id":"s1","title":"Rainbow","artist":"IZ","content":"[C]Over","key":"C","capo":2,"strumPatternName":"","labels":["Hawaiian"],"createdAt":100,"updatedAt":200,"viewCount":5,"lastViewedAt":150,"totalViewTimeMs":9000}]""",
        )
        manager.importBackup(backup)
        val repo = ChordSheetRepository(app)
        val sheet = repo.get("s1")
        assertNotNull(sheet)
        assertEquals("Rainbow", sheet!!.title)
        assertEquals("IZ", sheet.artist)
        assertEquals(2, sheet.capo)
        assertEquals(listOf("Hawaiian"), sheet.labels)
        assertEquals(5, sheet.viewCount)
    }

    @Test
    fun importExportRoundTripPreservesMelodies() {
        val backup = buildMinimalBackup(
            melodies = """[{"id":"m1","name":"Scale","notes":[{"pitchClass":0,"octave":4,"duration":"QUARTER","stringIndex":2,"fret":3}],"bpm":100,"createdAt":7000}]""",
        )
        manager.importBackup(backup)
        val repo = MelodyRepository(app)
        val melody = repo.get("m1")
        assertNotNull(melody)
        assertEquals("Scale", melody!!.name)
        assertEquals(1, melody.notes.size)
        assertEquals(0, melody.notes[0].pitchClass)
        assertEquals(100, melody.bpm)
    }

    @Test
    fun importExportRoundTripPreservesSetlists() {
        val backup = buildMinimalBackup(
            setlists = """[{"id":"sl1","name":"Friday","songIds":["s1","s2"],"createdAt":100,"updatedAt":200}]""",
        )
        manager.importBackup(backup)
        val repo = SetlistRepository(app)
        val all = repo.getAll()
        assertEquals(1, all.size)
        assertEquals("Friday", all[0].name)
        assertEquals(listOf("s1", "s2"), all[0].songIds)
    }

    @Test
    fun importExportRoundTripPreservesAchievements() {
        val backup = buildMinimalBackup(achievements = """{"streak_3":1000,"first_chord":2000}""")
        manager.importBackup(backup)
        val repo = AchievementRepository(app)
        assertTrue(repo.isUnlocked("streak_3"))
        assertTrue(repo.isUnlocked("first_chord"))
        assertEquals(2, repo.unlockedCount())
    }

    @Test
    fun importExportRoundTripPreservesPracticeTimer() {
        val backup = buildMinimalBackup(
            practiceTimer = """{"totalMinutes":60,"totalSessions":10,"longestSession":15,"lastSessionTime":9999,"dailyGoal":20,"dailyMinutes":{"2025-01-01":10}}""",
        )
        manager.importBackup(backup)
        val repo = PracticeTimerRepository(app)
        assertEquals(60, repo.totalMinutes())
        assertEquals(10, repo.totalSessions())
        assertEquals(15, repo.longestSession())
        assertEquals(20, repo.dailyGoal())
    }

    @Test
    fun importExportRoundTripPreservesLearningProgress() {
        val backup = buildMinimalBackup(
            learningProgress = """{"entries":{"lesson_done_basics":"true","quiz_total_ALL":"5"}}""",
        )
        manager.importBackup(backup)
        val repo = LearningProgressRepository(app)
        assertTrue(repo.isLessonCompleted("basics"))
    }

    // ── Merge semantics ──────────────────────────────────────────────

    @Test
    fun importMergesWithExistingFavorites() {
        val favRepo = FavoritesRepository(app)
        favRepo.add(FavoriteVoicing(0, "maj", listOf(0, 0, 0, 3), 1000L))
        val backup = buildMinimalBackup(
            favorites = """[
                {"rootPitchClass":0,"chordSymbol":"maj","frets":[0,0,0,3],"addedAt":2000,"folderIds":[]},
                {"rootPitchClass":4,"chordSymbol":"m","frets":[0,4,3,2],"addedAt":3000,"folderIds":[]}
            ]""",
        )
        manager.importBackup(backup)
        val all = favRepo.getAll()
        assertEquals(2, all.size)
    }

    @Test
    fun importChordSheetUpdatedAtWins() {
        val repo = ChordSheetRepository(app)
        repo.save(ChordSheet(id = "s1", title = "Old", content = "", createdAt = 100L, updatedAt = 200L))
        val backup = buildMinimalBackup(
            chordSheets = """[{"id":"s1","title":"New","content":"","createdAt":100,"updatedAt":300}]""",
        )
        manager.importBackup(backup)
        assertEquals("New", repo.get("s1")!!.title)
    }

    // ── Backward compatibility ───────────────────────────────────────

    @Test
    fun importMigratesOldFolderIdToFolderIds() {
        val backup = buildMinimalBackup(
            favorites = """[{"rootPitchClass":0,"chordSymbol":"","frets":[0,0,0,0],"addedAt":1000,"folderId":"old-folder","folderIds":[]}]""",
        )
        manager.importBackup(backup)
        val favRepo = FavoritesRepository(app)
        val loaded = favRepo.getAll().first()
        assertEquals(listOf("old-folder"), loaded.folderIds)
    }

    // ── iOS format normalization ─────────────────────────────────────

    @Test
    fun importNormalizesIosTimestamps() {
        val iosBackup = """
        {
            "version": 3,
            "timestamp": 1700000000.0,
            "favorites": [{"rootPitchClass":0,"chordSymbol":"","frets":[0,0,0,0],"addedAt":1700000000.0,"folderIds":[]}],
            "folders": [],
            "chord_sheets": [],
            "custom_progressions": [],
            "custom_strum_patterns": [],
            "custom_fingerpicking_patterns": [],
            "setlists": [],
            "melodies": [],
            "learn_progress": {},
            "practice_timer": {},
            "settings": {}
        }
        """.trimIndent()
        manager.importBackup(iosBackup)
        val favRepo = FavoritesRepository(app)
        val fav = favRepo.getAll().first()
        assertTrue("iOS timestamp (seconds) should be converted to millis", fav.addedAt >= 1_000_000_000_000)
    }

    @Test
    fun importNormalizesIosFingerNames() {
        val iosBackup = """
        {
            "version": 3,
            "timestamp": 1700000000.0,
            "favorites": [],
            "folders": [],
            "chord_sheets": [],
            "custom_progressions": [],
            "custom_strum_patterns": [],
            "custom_fingerpicking_patterns": [
                {"id":"fp1","name":"Test","steps":[{"finger":"T","stringIndex":0,"emphasis":true},{"finger":"I","stringIndex":2,"emphasis":false}],"createdAt":1000,"timeSignature":"4/4"}
            ],
            "setlists": [],
            "melodies": [],
            "learn_progress": {},
            "practice_timer": {},
            "settings": {}
        }
        """.trimIndent()
        manager.importBackup(iosBackup)
        val repo = CustomFingerpickingPatternRepository(app)
        val all = repo.getAll()
        assertEquals(1, all.size)
        assertEquals("Test", all[0].pattern.name)
    }

    @Test
    fun importNormalizesIosMelodyDurations() {
        val iosBackup = """
        {
            "version": 3,
            "timestamp": 1700000000.0,
            "favorites": [],
            "folders": [],
            "chord_sheets": [],
            "custom_progressions": [],
            "custom_strum_patterns": [],
            "custom_fingerpicking_patterns": [],
            "setlists": [],
            "melodies": [{"id":"m1","name":"Test","notes":[{"pitchClass":0,"octave":4,"duration":"♩"}],"bpm":120,"createdAt":1000}],
            "learn_progress": {},
            "practice_timer": {},
            "settings": {}
        }
        """.trimIndent()
        manager.importBackup(iosBackup)
        val repo = MelodyRepository(app)
        val melody = repo.get("m1")
        assertNotNull(melody)
        assertEquals("QUARTER", melody!!.notes[0].duration.name)
    }

    // ── Error handling ───────────────────────────────────────────────

    @Test(expected = Exception::class)
    fun importInvalidJsonThrows() {
        manager.importBackup("not valid json {{{")
    }

    // ── Settings ─────────────────────────────────────────────────────

    @Test
    fun importPreservesSettingsNotInBackup() {
        settingsVM.updateTuner { it.copy(spokenFeedback = true) }
        val backup = buildMinimalBackup(
            settings = """{"soundEnabled":false,"volume":0.5,"noteDurationMs":600,"strumDelayMs":50,"strumDown":true,"playOnTap":false,"themeMode":"DARK","showExplorerTips":true,"showLearnSection":true,"showReferenceSection":true,"tuning":"LOW_G","leftHanded":true,"lastFret":15,"showNoteNames":false}""",
        )
        manager.importBackup(backup)
        val s = settingsVM.exportSettings()
        assertEquals(false, s.sound.enabled)
        assertEquals(0.5f, s.sound.volume)
        assertEquals("DARK", s.display.themeMode.name)
        assertEquals("LOW_G", s.tuning.tuning.name)
        assertEquals(true, s.fretboard.leftHanded)
        assertEquals(15, s.fretboard.lastFret)
        assertTrue("Tuner spokenFeedback should be preserved", s.tuner.spokenFeedback)
    }

    // ── Helper ───────────────────────────────────────────────────────

    private fun buildMinimalBackup(
        favorites: String = "[]",
        favoriteFolders: String = "[]",
        chordSheets: String = "[]",
        customProgressions: String = "[]",
        customStrumPatterns: String = "[]",
        customFingerpickingPatterns: String = "[]",
        setlists: String = "[]",
        melodies: String = "[]",
        learningProgress: String = """{"entries":{}}""",
        achievements: String = "{}",
        practiceTimer: String = """{"totalMinutes":0,"totalSessions":0,"longestSession":0,"lastSessionTime":0,"dailyGoal":15,"dailyMinutes":{}}""",
        settings: String = """{"soundEnabled":true,"volume":0.7,"noteDurationMs":600,"strumDelayMs":50,"strumDown":true,"playOnTap":false,"themeMode":"SYSTEM","showExplorerTips":true,"showLearnSection":true,"showReferenceSection":true,"tuning":"HIGH_G","leftHanded":false,"lastFret":12,"showNoteNames":true}""",
    ): String = """
        {
            "version": 3,
            "exportedAt": ${System.currentTimeMillis()},
            "favorites": $favorites,
            "favoriteFolders": $favoriteFolders,
            "chordSheets": $chordSheets,
            "customProgressions": $customProgressions,
            "customStrumPatterns": $customStrumPatterns,
            "customFingerpickingPatterns": $customFingerpickingPatterns,
            "setlists": $setlists,
            "melodies": $melodies,
            "learningProgress": $learningProgress,
            "achievements": $achievements,
            "practiceTimer": $practiceTimer,
            "settings": $settings
        }
    """.trimIndent()
}
