package com.baijum.ukufretboard

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.domain.UkuleleString
import com.baijum.ukufretboard.ui.songbook.SheetViewer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Regression tests for the details panel's wiring (issue #501).
 *
 * #501 moved subtitle, key, capo, transpose, section shortcuts, tempo, statistics and
 * the chord rail out of `SheetViewer` and into `SongDetailsPanel`, turning direct state
 * mutation into callbacks — `transposeSemitones--` became `onTransposeChange(n - 1)`,
 * and the section-scroll and metronome lambdas crossed a file boundary. Silently
 * dropping one of those wires would leave a control that renders but does nothing, so
 * each is exercised here through the real [SheetViewer].
 *
 * Run with: ./gradlew connectedAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.baijum.ukufretboard.SongDetailsPanelWiringTest
 */
class SongDetailsPanelWiringTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val highG =
        listOf(
            UkuleleString(name = "G", openPitchClass = 7),
            UkuleleString(name = "C", openPitchClass = 0),
            UkuleleString(name = "E", openPitchClass = 4),
            UkuleleString(name = "A", openPitchClass = 9),
        )

    private var appliedTranspose: Int? = null
    private var startedTempo: Int? = null

    private fun context(): Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun str(
        id: Int,
        vararg args: Any,
    ): String = context().getString(id, *args)

    /** The panel starts collapsed if an earlier test left the flag set. */
    private fun clearPersistedState() {
        context()
            .getSharedPreferences("songbook_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("song_details_collapsed")
            .commit()
    }

    @Before
    fun resetState() {
        clearPersistedState()
        appliedTranspose = null
        startedTempo = null
    }

    @After
    fun restorePrefs() = clearPersistedState()

    /** Carries a tempo directive, several sections, and a viewing history. */
    private fun sheet(): ChordSheet =
        ChordSheet(
            title = "Heart of Gold",
            artist = "Neil Young",
            content =
                "Tempo: 96 BPM\n" +
                    "[Verse 1]\n" +
                    "[Em]I wanna live [C]I wanna give\n" +
                    // Padding so the chorus heading starts below the fold and the
                    // section shortcut has somewhere to scroll to.
                    "[G]filler line\n".repeat(40) +
                    "[Chorus]\n" +
                    "[Am]Keep me searching for a heart of gold",
            key = "G",
            viewCount = 4,
            lastViewedAt = 1_755_000_000_000L,
            totalViewTimeMs = 180_000L,
        )

    private fun renderViewer() {
        composeTestRule.setContent {
            MaterialTheme {
                SheetViewer(
                    sheet = sheet(),
                    allLabels = emptySet(),
                    onBack = {},
                    onEdit = {},
                    onDelete = {},
                    onDuplicate = {},
                    onChordTapped = {},
                    onPlayChord = {},
                    onStartMetronome = { startedTempo = it },
                    tuning = highG,
                    leftHanded = false,
                    onStrumPatternChange = {},
                    onLabelsChange = {},
                    onApplyTranspose = { appliedTranspose = it },
                )
            }
        }
    }

    private fun tapAction(id: Int) = composeTestRule.onNodeWithContentDescription(str(id)).performClick()

    private fun transposeUp() = tapAction(R.string.cd_transpose_up)

    private fun transposeDown() = tapAction(R.string.cd_transpose_down)

    @Test
    fun transposingUpRewritesTheChords() {
        renderViewer()

        transposeUp()
        transposeUp()

        // Em + 2 = F#m. The name lands twice, once in the chord rail and once in the
        // song, so this only asserts it is there; the key label is the precise half.
        composeTestRule.onAllNodesWithText("F#m", substring = true).onFirst().assertExists()
        composeTestRule.onNodeWithText(str(R.string.songbook_key_prefix) + "A").assertExists()
    }

    @Test
    fun transposingDownRewritesTheChords() {
        renderViewer()

        transposeDown()

        // Em - 1 spells flat, Ebm, while the key label spells sharp, F#. That split is
        // the app's existing behaviour, not something this panel decides.
        composeTestRule.onAllNodesWithText("Ebm", substring = true).onFirst().assertExists()
        composeTestRule.onNodeWithText(str(R.string.songbook_key_prefix) + "F#").assertExists()
    }

    @Test
    fun resetReturnsToTheOriginalKey() {
        renderViewer()

        transposeUp()
        transposeUp()
        composeTestRule.onNodeWithText(str(R.string.dialog_reset)).performClick()

        composeTestRule.onNodeWithText(str(R.string.songbook_key_prefix) + "G").assertExists()
        // The reset control only exists while a transpose is previewed.
        composeTestRule.onNodeWithText(str(R.string.dialog_reset)).assertDoesNotExist()
    }

    @Test
    fun theCapoHintAppearsOnlyWhileTransposed() {
        renderViewer()

        composeTestRule.onNodeWithText("Capo", substring = true).assertDoesNotExist()

        transposeUp()
        transposeUp()

        composeTestRule.onNodeWithText(str(R.string.songbook_capo_hint, 2)).assertExists()
    }

    /** The save button has to report the previewed amount, not zero or a stale value. */
    @Test
    fun savingInTheNewKeyReportsTheTransposedAmount() {
        renderViewer()

        transposeUp()
        transposeUp()
        transposeDown()
        composeTestRule.onNodeWithText(str(R.string.songbook_save_in_key)).performClick()

        composeTestRule.runOnIdle {
            assertEquals("the previewed transpose should reach onApplyTranspose", 1, appliedTranspose)
        }
    }

    @Test
    fun theTempoDirectiveStartsTheMetronomeAtThatBpm() {
        renderViewer()

        // Matches twice: the tempo row, and the raw "Tempo: 96 BPM" line the song body
        // still shows. The button below is unique.
        composeTestRule.onAllNodesWithText(str(R.string.songbook_tempo_label, 96)).onFirst().assertExists()
        composeTestRule.onNodeWithText(str(R.string.songbook_start_metronome)).performClick()

        composeTestRule.runOnIdle {
            assertEquals("the song's tempo should reach onStartMetronome", 96, startedTempo)
        }
    }

    /**
     * Tapping a section shortcut has to scroll the song to that section. The chip and
     * the heading carry the same label, so they are told apart by the click action —
     * only the chip has one.
     */
    @Test
    fun aSectionShortcutScrollsTheSongToThatSection() {
        renderViewer()

        val chip = { composeTestRule.onNode(hasText("Chorus") and hasClickAction()) }
        val heading = { composeTestRule.onNode(hasText("Chorus") and hasClickAction().not()) }

        heading().assertIsNotDisplayed()

        chip().performClick()
        composeTestRule.waitForIdle()

        heading().assertIsDisplayed()
    }

    @Test
    fun theStatisticsRowShowsAViewingHistory() {
        renderViewer()

        val views =
            context().resources.getQuantityString(R.plurals.stats_views, 4, 4)
        composeTestRule.onNodeWithText(views).assertExists()
        composeTestRule.onNodeWithText(str(R.string.stats_time_minutes, 3)).assertExists()
    }

    @Test
    fun theChordRailRendersTheSongsChords() {
        renderViewer()

        // Matched on the diagram's own description, which the song text does not carry.
        composeTestRule.onNodeWithContentDescription("Chord A C E A", substring = true).assertExists()
    }

    @Test
    fun theKeyAndArtistSurviveTheExtraction() {
        renderViewer()

        composeTestRule.onNodeWithText("Neil Young").assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.songbook_key_prefix) + "G").assertExists()
    }
}
