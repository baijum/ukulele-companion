package com.baijum.ukufretboard.data.sync

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.baijum.ukufretboard.data.AchievementRepository
import com.baijum.ukufretboard.data.ChordSheetRepository
import com.baijum.ukufretboard.data.CustomFingerpickingPatternRepository
import com.baijum.ukufretboard.data.CustomProgressionRepository
import com.baijum.ukufretboard.data.CustomStrumPatternRepository
import com.baijum.ukufretboard.data.FavoritesRepository
import com.baijum.ukufretboard.data.LearningProgressRepository
import com.baijum.ukufretboard.data.MelodyRepository
import com.baijum.ukufretboard.data.NoteDuration
import com.baijum.ukufretboard.data.PracticeTimerRepository
import com.baijum.ukufretboard.data.SetlistRepository
import com.baijum.ukufretboard.viewmodel.SettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for iOS backup format normalization in [BackupRestoreManager].
 *
 * iOS backups use different JSON keys (snake_case), timestamps in epoch
 * seconds, single-char finger abbreviations, Unicode note durations, and
 * separate arrays for progression degree fields. These tests verify that
 * all conversion paths produce valid KMP-format data after import.
 */
@RunWith(RobolectricTestRunner::class)
class IosBackupNormalizationTest {

    private lateinit var app: Application
    private lateinit var manager: BackupRestoreManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        manager = BackupRestoreManager(app, SettingsViewModel(app))
    }

    @Test
    fun kmpFormatPassesThroughUnchanged() {
        val kmpBackup = """
        {
            "version": 3,
            "exportedAt": 1700000000000,
            "favorites": [{"rootPitchClass":7,"chordSymbol":"","frets":[0,0,0,0],"addedAt":1700000000000,"folderIds":[]}],
            "favoriteFolders": [],
            "chordSheets": [],
            "customProgressions": [],
            "customStrumPatterns": [],
            "customFingerpickingPatterns": [],
            "setlists": [],
            "melodies": [],
            "learningProgress": {"entries":{}},
            "achievements": {},
            "practiceTimer": {"totalMinutes":0,"totalSessions":0,"longestSession":0,"lastSessionTime":0,"dailyGoal":15,"dailyMinutes":{}},
            "settings": {"soundEnabled":true,"volume":0.7,"noteDurationMs":600,"strumDelayMs":50,"strumDown":true,"playOnTap":false,"themeMode":"SYSTEM","showExplorerTips":true,"showLearnSection":true,"showReferenceSection":true,"tuning":"HIGH_G","leftHanded":false,"lastFret":12,"showNoteNames":true}
        }
        """.trimIndent()
        manager.importBackup(kmpBackup)
        val fav = FavoritesRepository(app).getAll().first()
        assertEquals(1700000000000L, fav.addedAt)
    }

    @Test
    fun iosChordSheetsKeyMapped() {
        val iosBackup = buildIosBackup(
            chordSheets = """[{"id":"cs1","title":"Aloha Oe","artist":"","content":"[C]Aloha","key":"C","capo":0,"strumPatternName":"","labels":[],"createdAt":100,"updatedAt":200}]""",
            chordSheetsKey = "chord_sheets",
        )
        manager.importBackup(iosBackup)
        val sheet = ChordSheetRepository(app).get("cs1")
        assertNotNull(sheet)
        assertEquals("Aloha Oe", sheet!!.title)
    }

    @Test
    fun iosFoldersKeyMappedToFavoriteFolders() {
        val iosBackup = buildIosBackup(
            folders = """[{"id":"f1","name":"Hawaiian","createdAt":1700000000.0,"voicingOrder":[]}]""",
        )
        manager.importBackup(iosBackup)
        val folders = FavoritesRepository(app).getAllFolders()
        assertEquals(1, folders.size)
        assertEquals("Hawaiian", folders[0].name)
        assertEquals(1700000000000L, folders[0].createdAt)
    }

    @Test
    fun iosProgressionsNormalized() {
        val iosBackup = buildIosBackup(
            progressions = """[{
                "id": "p1",
                "name": "I-IV-V-I",
                "description": "Classic",
                "scaleType": "MAJOR",
                "degreeIntervals": [0, 5, 7],
                "degreeQualities": ["", "", ""],
                "degreeNumerals": ["I", "IV", "V"],
                "createdAt": 5000
            }]""",
        )
        manager.importBackup(iosBackup)
        val repo = CustomProgressionRepository(app)
        val all = repo.getAll()
        assertEquals(1, all.size)
        assertEquals("I-IV-V-I", all[0].progression.name)
        assertEquals(3, all[0].progression.degrees.size)
        assertEquals(0, all[0].progression.degrees[0].interval)
        assertEquals("IV", all[0].progression.degrees[1].numeral)
    }

    @Test
    fun iosStrumPatternsKeyMapped() {
        val iosBackup = buildIosBackup(
            strumPatterns = """[{"id":"sp1","name":"Island","beats":[{"direction":"DOWN","emphasis":true},{"direction":"UP","emphasis":false}],"createdAt":1000,"timeSignature":"4/4"}]""",
            strumPatternsKey = "custom_strum_patterns",
        )
        manager.importBackup(iosBackup)
        val repo = CustomStrumPatternRepository(app)
        val all = repo.getAll()
        assertEquals(1, all.size)
        assertEquals("Island", all[0].pattern.name)
    }

    @Test
    fun iosLearnProgressWithAchievements() {
        val iosBackup = buildIosBackup(
            learnProgress = """{
                "lesson_done_basics": "true",
                "quiz_total_ALL": "10",
                "quiz_correct_ALL": "8",
                "unlocked_achievements": ["first_chord", "streak_7"]
            }""",
        )
        manager.importBackup(iosBackup)
        val learnRepo = LearningProgressRepository(app)
        assertTrue(learnRepo.isLessonCompleted("basics"))
        val achRepo = AchievementRepository(app)
        assertTrue(achRepo.isUnlocked("first_chord"))
        assertTrue(achRepo.isUnlocked("streak_7"))
        assertEquals(2, achRepo.unlockedCount())
    }

    @Test
    fun iosPracticeTimerKeyMapped() {
        val iosBackup = buildIosBackup(
            practiceTimer = """{"totalMinutes":30,"totalSessions":5,"longestSession":12,"lastSessionTime":9000,"dailyGoal":20,"dailyMinutes":{"2025-06-01":15}}""",
            practiceTimerKey = "practice_timer",
        )
        manager.importBackup(iosBackup)
        val repo = PracticeTimerRepository(app)
        assertEquals(30, repo.totalMinutes())
        assertEquals(5, repo.totalSessions())
        assertEquals(20, repo.dailyGoal())
    }

    @Test
    fun iosSettingsKeyAndValueMapping() {
        val iosBackup = buildIosBackup(
            settings = """{
                "sound_enabled": false,
                "volume": 0.5,
                "note_duration_ms": 400,
                "strum_delay_ms": 80,
                "strum_down": false,
                "play_on_tap": true,
                "theme_mode": "High Contrast",
                "selected_tuning": "Low-G",
                "show_tips": false,
                "show_learn_tab": false,
                "show_reference_tab": false,
                "left_handed": true,
                "show_note_names": false,
                "last_fret": 15
            }""",
        )
        manager.importBackup(iosBackup)
        val settingsVM = SettingsViewModel(app)
        val s = settingsVM.exportSettings()
        assertEquals(false, s.sound.enabled)
        assertEquals(0.5f, s.sound.volume)
        assertEquals(400, s.sound.noteDurationMs)
        assertEquals(80, s.sound.strumDelayMs)
        assertEquals(false, s.sound.strumDown)
        assertEquals(true, s.sound.playOnTap)
        assertEquals("HIGH_CONTRAST", s.display.themeMode.name)
        assertEquals("LOW_G", s.tuning.tuning.name)
        assertEquals(true, s.fretboard.leftHanded)
        assertEquals(false, s.fretboard.showNoteNames)
        assertEquals(15, s.fretboard.lastFret)
    }

    @Test
    fun iosAllDurationSymbolsMapped() {
        val iosBackup = buildIosBackup(
            melodies = """[{"id":"m1","name":"Durations","notes":[
                {"pitchClass":0,"octave":4,"duration":"\uD834\uDD5D"},
                {"pitchClass":2,"octave":4,"duration":"\uD834\uDD5E"},
                {"pitchClass":4,"octave":4,"duration":"\u2669"},
                {"pitchClass":5,"octave":4,"duration":"\u266A"},
                {"pitchClass":7,"octave":4,"duration":"\uD834\uDD63"}
            ],"bpm":100,"createdAt":1000}]""",
        )
        manager.importBackup(iosBackup)
        val melody = MelodyRepository(app).get("m1")
        assertNotNull(melody)
        assertEquals(5, melody!!.notes.size)
        assertEquals(NoteDuration.WHOLE, melody.notes[0].duration)
        assertEquals(NoteDuration.HALF, melody.notes[1].duration)
        assertEquals(NoteDuration.QUARTER, melody.notes[2].duration)
        assertEquals(NoteDuration.EIGHTH, melody.notes[3].duration)
        assertEquals(NoteDuration.SIXTEENTH, melody.notes[4].duration)
    }

    @Test
    fun iosMelodyNullPitchClassPreserved() {
        val iosBackup = buildIosBackup(
            melodies = """[{"id":"m2","name":"Rest","notes":[
                {"octave":4,"duration":"\u2669"}
            ],"bpm":80,"createdAt":2000}]""",
        )
        manager.importBackup(iosBackup)
        val melody = MelodyRepository(app).get("m2")
        assertNotNull(melody)
        assertNull("Rest note should have null pitchClass", melody!!.notes[0].pitchClass)
    }

    @Test
    fun iosAllTuningsMapped() {
        val tuningMap = mapOf(
            "High-G (Standard)" to "HIGH_G",
            "Low-G" to "LOW_G",
            "Baritone (DGBE)" to "BARITONE",
            "D-Tuning (ADF#B)" to "D_TUNING",
            "Slack Key (GCEG)" to "SLACK_KEY",
            "Open A (AC#EA)" to "OPEN_A",
            "Low A (GCEa)" to "LOW_A",
            "Half-Step Down" to "HALF_STEP_DOWN",
        )
        for ((iosLabel, kmpName) in tuningMap) {
            val iosBackup = buildIosBackup(
                settings = """{"selected_tuning": "$iosLabel"}""",
            )
            manager.importBackup(iosBackup)
            val vm = SettingsViewModel(app)
            assertEquals(
                "iOS tuning '$iosLabel' should map to $kmpName",
                kmpName,
                vm.exportSettings().tuning.tuning.name,
            )
        }
    }

    @Test
    fun iosAllThemesMapped() {
        val themeMap = mapOf(
            "Light" to "LIGHT",
            "Dark" to "DARK",
            "System" to "SYSTEM",
            "High Contrast" to "HIGH_CONTRAST",
        )
        for ((iosLabel, kmpName) in themeMap) {
            val iosBackup = buildIosBackup(
                settings = """{"theme_mode": "$iosLabel"}""",
            )
            manager.importBackup(iosBackup)
            val vm = SettingsViewModel(app)
            assertEquals(
                "iOS theme '$iosLabel' should map to $kmpName",
                kmpName,
                vm.exportSettings().display.themeMode.name,
            )
        }
    }

    @Test
    fun kmpFormatFractionalTimestampsNormalized() {
        val kmpBackup = """
        {
            "version": 3,
            "exportedAt": 1700000000000,
            "favorites": [],
            "favoriteFolders": [],
            "chordSheets": [],
            "customProgressions": [],
            "customStrumPatterns": [
                {"id":"sp1","name":"Island","beats":[{"direction":"DOWN","emphasis":true}],"createdAt":1700000000.123,"timeSignature":"4/4"}
            ],
            "customFingerpickingPatterns": [
                {"id":"fp1","name":"Travis","steps":[{"finger":"THUMB","stringIndex":3,"emphasis":false}],"createdAt":1700000000.456,"timeSignature":"4/4"}
            ],
            "setlists": [
                {"id":"sl1","name":"Gig","songIds":[],"createdAt":1700000000000.789,"updatedAt":1700000000000.999}
            ],
            "melodies": [],
            "learningProgress": {"entries":{}},
            "achievements": {},
            "practiceTimer": {"totalMinutes":10,"totalSessions":2,"longestSession":8,"lastSessionTime":1700000000.321,"dailyGoal":15,"dailyMinutes":{}},
            "settings": {"soundEnabled":true,"volume":0.7,"noteDurationMs":600,"strumDelayMs":50,"strumDown":true,"playOnTap":false,"themeMode":"SYSTEM","showExplorerTips":true,"showLearnSection":true,"showReferenceSection":true,"tuning":"HIGH_G","leftHanded":false,"lastFret":12,"showNoteNames":true}
        }
        """.trimIndent()
        manager.importBackup(kmpBackup)

        val strumRepo = CustomStrumPatternRepository(app)
        val strumAll = strumRepo.getAll()
        assertEquals(1, strumAll.size)
        assertTrue(
            "Strum createdAt (seconds) should be converted to millis",
            strumAll[0].createdAt >= 1_000_000_000_000,
        )

        val fpRepo = CustomFingerpickingPatternRepository(app)
        val fpAll = fpRepo.getAll()
        assertEquals(1, fpAll.size)
        assertTrue(
            "Fingerpick createdAt (seconds) should be converted to millis",
            fpAll[0].createdAt >= 1_000_000_000_000,
        )

        val setlistRepo = SetlistRepository(app)
        val setlists = setlistRepo.getAll()
        assertEquals(1, setlists.size)
        assertEquals("Gig", setlists[0].name)
        assertEquals(1700000000000L, setlists[0].createdAt)
        assertEquals(1700000000000L, setlists[0].updatedAt)

        val practiceRepo = PracticeTimerRepository(app)
        assertTrue(
            "Practice lastSessionTime (seconds) should be converted to millis",
            practiceRepo.lastSessionTime() >= 1_000_000_000_000,
        )
    }

    // ── Helper ───────────────────────────────────────────────────────

    private fun buildIosBackup(
        favorites: String = "[]",
        folders: String? = null,
        chordSheets: String = "[]",
        chordSheetsKey: String = "chord_sheets",
        progressions: String = "[]",
        strumPatterns: String = "[]",
        strumPatternsKey: String = "custom_strum_patterns",
        fingerpickingPatterns: String = "[]",
        melodies: String = "[]",
        learnProgress: String = "{}",
        practiceTimer: String = "{}",
        practiceTimerKey: String = "practice_timer",
        settings: String = "{}",
    ): String {
        val foldersEntry = if (folders != null) {
            """"folders": $folders,"""
        } else {
            ""
        }
        return """
        {
            "version": 3,
            "timestamp": 1700000000.0,
            "favorites": $favorites,
            $foldersEntry
            "$chordSheetsKey": $chordSheets,
            "custom_progressions": $progressions,
            "$strumPatternsKey": $strumPatterns,
            "custom_fingerpicking_patterns": $fingerpickingPatterns,
            "setlists": [],
            "melodies": $melodies,
            "learn_progress": $learnProgress,
            "$practiceTimerKey": $practiceTimer,
            "settings": $settings
        }
        """.trimIndent()
    }
}
