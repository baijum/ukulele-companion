package com.baijum.ukufretboard.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LearningProgressImportTest {

    private lateinit var repo: LearningProgressRepository

    @Before
    fun setUp() {
        val context: Application = ApplicationProvider.getApplicationContext()
        repo = LearningProgressRepository(context)
    }

    @Test
    fun intMaxMergePreservesHigherLocal() {
        repo.importAll(mapOf("quiz_total_ALL" to "800"))
        repo.importAll(mapOf("quiz_total_ALL" to "90"))
        val exported = repo.exportAll()
        assertEquals("800", exported["quiz_total_ALL"])
    }

    @Test
    fun intMaxMergeAcceptsHigherImport() {
        repo.importAll(mapOf("quiz_total_ALL" to "800"))
        repo.importAll(mapOf("quiz_total_ALL" to "900"))
        val exported = repo.exportAll()
        assertEquals("900", exported["quiz_total_ALL"])
    }

    @Test
    fun booleanOrNeverRevertsTrue() {
        repo.importAll(mapOf("lesson_done_chords" to "true"))
        repo.importAll(mapOf("lesson_done_chords" to "false"))
        val exported = repo.exportAll()
        assertEquals("true", exported["lesson_done_chords"])
    }

    @Test
    fun booleanTrueImported() {
        repo.importAll(mapOf("lesson_done_scales" to "true"))
        val exported = repo.exportAll()
        assertEquals("true", exported["lesson_done_scales"])
    }

    @Test
    fun dateKeepsNewerLocal() {
        repo.importAll(mapOf("last_activity_date" to "2026-06-10"))
        repo.importAll(mapOf("last_activity_date" to "2025-01-01"))
        val exported = repo.exportAll()
        assertEquals("2026-06-10", exported["last_activity_date"])
    }

    @Test
    fun dateUpdatesWhenImportIsNewer() {
        repo.importAll(mapOf("last_activity_date" to "2026-06-10"))
        repo.importAll(mapOf("last_activity_date" to "2026-07-01"))
        val exported = repo.exportAll()
        assertEquals("2026-07-01", exported["last_activity_date"])
    }

    @Test
    fun stringNotOverwritten() {
        repo.importAll(mapOf("custom_key" to "original"))
        repo.importAll(mapOf("custom_key" to "overwrite"))
        val exported = repo.exportAll()
        assertEquals("original", exported["custom_key"])
    }

    @Test
    fun bestStreakMaxMerged() {
        repo.importAll(mapOf("best_streak_days" to "45"))
        repo.importAll(mapOf("best_streak_days" to "10"))
        val exported = repo.exportAll()
        assertEquals("45", exported["best_streak_days"])
    }

    @Test
    fun streakDaysMaxMerged() {
        repo.importAll(mapOf("streak_days" to "12"))
        repo.importAll(mapOf("streak_days" to "5"))
        val exported = repo.exportAll()
        assertEquals("12", exported["streak_days"])
    }

    @Test
    fun booleanFalseNotWrittenToEmptyPrefs() {
        repo.importAll(mapOf("lesson_done_new" to "false"))
        val exported = repo.exportAll()
        assertFalse(exported.containsKey("lesson_done_new"))
    }
}
