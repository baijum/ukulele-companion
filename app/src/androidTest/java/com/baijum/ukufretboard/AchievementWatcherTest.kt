package com.baijum.ukufretboard

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.baijum.ukufretboard.domain.AchievementDef
import com.baijum.ukufretboard.ui.navigation.AchievementWatcher
import com.baijum.ukufretboard.viewmodel.LearningProgressViewModel
import com.baijum.ukufretboard.viewmodel.SongbookViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the app-wide achievement watcher (issue #543).
 *
 * Achievements used to be checked only while the Achievements screen was open,
 * so a user who never opened that screen never unlocked anything — and never
 * became a candidate for the review prompt. The watcher now runs at the
 * composition root, which is what these render it on its own to verify.
 *
 * Run with: ./gradlew connectedAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.baijum.ukufretboard.AchievementWatcherTest
 */
class AchievementWatcherTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var application: Application
    private lateinit var progressViewModel: LearningProgressViewModel
    private lateinit var songbookViewModel: SongbookViewModel

    @Before
    fun setUp() {
        application =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .applicationContext as Application
        application
            .getSharedPreferences("learn_section_progress", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        progressViewModel = LearningProgressViewModel(application)
        songbookViewModel = SongbookViewModel(application)
    }

    /** Renders the watcher alone and returns everything it reported. */
    private fun renderWatcher(
        favoritesCount: Int = 0,
        unlockedAchievementIds: Set<String> = emptySet(),
    ): List<AchievementDef> {
        val reported = mutableListOf<AchievementDef>()
        composeTestRule.setContent {
            AchievementWatcher(
                learningProgressViewModel = progressViewModel,
                songbookViewModel = songbookViewModel,
                favoritesCount = favoritesCount,
                unlockedAchievementIds = unlockedAchievementIds,
                onNewlyEarned = { reported += it },
            )
        }
        composeTestRule.waitForIdle()
        return reported
    }

    @Test
    fun reportsNothingFromAStandingStart() {
        assertTrue(renderWatcher().isEmpty())
    }

    @Test
    fun reportsALessonAchievementWithoutTheAchievementsScreen() {
        progressViewModel.markLessonCompleted("notes_12")

        val reported = renderWatcher()

        // The Achievements screen was never composed — this is the whole point
        // of #543.
        assertEquals(listOf("first_lesson"), reported.map { it.id })
    }

    @Test
    fun reportsNothingForAnAchievementAlreadyUnlocked() {
        progressViewModel.markLessonCompleted("notes_12")

        val reported = renderWatcher(unlockedAchievementIds = setOf("first_lesson"))

        assertTrue(reported.isEmpty())
    }

    @Test
    fun favoritesCountReachesTheFavoritesAchievement() {
        val reported = renderWatcher(favoritesCount = 5)

        assertEquals(listOf("fav_5"), reported.map { it.id })
    }

    @Test
    fun reportsEachNewlyEarnedAchievementOnlyOnce() {
        progressViewModel.markLessonCompleted("notes_12")

        val reported = renderWatcher(favoritesCount = 5)

        // Recomposition must not re-report: the caller unlocks and prompts from
        // this callback, so a repeat would burn the review prompt's budget.
        assertEquals(reported.size, reported.map { it.id }.distinct().size)
        assertEquals(setOf("first_lesson", "fav_5"), reported.map { it.id }.toSet())
    }
}
