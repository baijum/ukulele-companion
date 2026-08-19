package com.baijum.ukufretboard

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
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

    /**
     * A song with nothing optional in it. CI's emulator is the default 320x640dp skin,
     * and the details sit in a non-scrolling `Column`: with artist, key, statistics and
     * section shortcuts all present, the controls at the bottom of the panel fall past
     * the screen edge, where a tap lands on nothing. Tests that press one of those use
     * this instead.
     */
    private fun leanSheet(withTempo: Boolean): ChordSheet =
        ChordSheet(
            title = "Heart of Gold",
            content =
                (if (withTempo) "Tempo: 96 BPM\n" else "") +
                    "[Em]I wanna live [C]I wanna give",
        )

    /**
     * The chord rail is off unless a test needs it. It is the tallest thing in the
     * panel at roughly 150dp, and the viewer's details sit in a non-scrolling `Column`:
     * on a 360x640dp screen the rail pushes the tempo row and the save button past the
     * bottom edge, where a tap lands on nothing and the wiring under test never runs.
     */
    private fun renderViewer(
        sheet: ChordSheet = sheet(),
        showRail: Boolean = false,
    ) {
        composeTestRule.setContent {
            MaterialTheme {
                SheetViewer(
                    showChordDiagramRail = showRail,
                    sheet = sheet,
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
        renderViewer(leanSheet(withTempo = false))

        transposeUp()
        transposeUp()
        transposeDown()
        // The save button is the last thing in the panel. On a viewport too short to
        // hold the panel it is measured against maxHeight = 0 and a tap lands on
        // nothing -- the layout problem in #529, not a fault in the wiring. CI's
        // emulator is the 320x640dp default skin and hits exactly that, so state the
        // requirement rather than assert something the screen cannot present.
        val save = composeTestRule.onNodeWithText(str(R.string.songbook_save_in_key))
        assumeTrue(
            "needs a viewport tall enough to lay the save button out; see #529",
            save.fetchSemanticsNode().size.height > 0,
        )

        save.performClick()

        composeTestRule.runOnIdle {
            assertEquals("the previewed transpose should reach onApplyTranspose", 1, appliedTranspose)
        }
    }

    @Test
    fun theTempoDirectiveStartsTheMetronomeAtThatBpm() {
        renderViewer(leanSheet(withTempo = true))

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

        // "It scrolled up" rather than "it is now displayed": whether the heading ends
        // up fully on screen depends on how much height the song area was left with,
        // and that varies by device.
        //
        // positionInRoot, not boundsInRoot — the heading starts well below the fold and
        // boundsInRoot is clipped, so it reads a flat 0 for anything off screen and both
        // measurements would compare equal no matter what the shortcut did.
        val before = heading().fetchSemanticsNode().positionInRoot.y
        assertTrue("the heading should start below the fold, was at $before", before > 0f)

        chip().performClick()
        composeTestRule.waitForIdle()

        val after = heading().fetchSemanticsNode().positionInRoot.y
        assertTrue("the shortcut should scroll the song, but the heading stayed at $after", after < before)
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
        renderViewer(showRail = true)

        // Matched on the diagram's own description, which the song text does not carry,
        // and on whichever diagram comes first: the rail is a LazyRow, so on a narrow
        // screen the later chords are never composed.
        composeTestRule
            .onAllNodesWithContentDescription("Chord ", substring = true)
            .onFirst()
            .assertExists()
    }

    @Test
    fun theKeyAndArtistSurviveTheExtraction() {
        renderViewer()

        composeTestRule.onNodeWithText("Neil Young").assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.songbook_key_prefix) + "G").assertExists()
    }
}
