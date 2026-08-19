package com.baijum.ukufretboard

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.domain.UkuleleString
import com.baijum.ukufretboard.ui.songbook.SheetViewer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the collapsible song details panel (issue #501).
 *
 * The reporter's complaint is vertical: subtitle, artist, key, strum pattern, labels
 * and the chord diagram rail push the lyrics off the bottom of the screen while
 * playing. So the load-bearing assertion here is not "the toggle exists" but
 * [collapsingLiftsTheLyricsUpTheScreen] — the lyrics must actually move.
 *
 * Run with: ./gradlew connectedAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.baijum.ukufretboard.SheetViewerCollapsibleDetailsTest
 */
class SheetViewerCollapsibleDetailsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val highG =
        listOf(
            UkuleleString(name = "G", openPitchClass = 7),
            UkuleleString(name = "C", openPitchClass = 0),
            UkuleleString(name = "E", openPitchClass = 4),
            UkuleleString(name = "A", openPitchClass = 9),
        )

    private fun context(): Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun str(id: Int): String = context().getString(id)

    /**
     * The collapsed flag is persisted, so a test that leaves it set would start every
     * later song-viewer test in the compact layout.
     */
    private fun clearPersistedState() {
        context()
            .getSharedPreferences("songbook_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("song_details_collapsed")
            .commit()
    }

    @Before
    fun resetPrefs() = clearPersistedState()

    @After
    fun restorePrefs() = clearPersistedState()

    private fun sheet(): ChordSheet =
        ChordSheet(
            title = "Heart of Gold",
            subtitle = "From Harvest",
            artist = "Neil Young",
            content = "{start_of_verse: Verse 1}\n[Em]I wanna live [C]I wanna give",
            key = "G",
            capo = 2,
            labels = listOf("folk"),
        )

    private fun renderViewer(sheet: ChordSheet = sheet()) {
        composeTestRule.setContent {
            MaterialTheme {
                SheetViewer(
                    sheet = sheet,
                    allLabels = emptySet(),
                    onBack = {},
                    onEdit = {},
                    onDelete = {},
                    onDuplicate = {},
                    onChordTapped = {},
                    onPlayChord = {},
                    onStartMetronome = {},
                    tuning = highG,
                    leftHanded = false,
                    onStrumPatternChange = {},
                    onLabelsChange = {},
                    onApplyTranspose = {},
                )
            }
        }
    }

    private fun toggle(id: Int) = composeTestRule.onNodeWithContentDescription(str(id))

    private fun collapse() = toggle(R.string.songbook_collapse_details).performClick()

    private fun expand() = toggle(R.string.songbook_expand_details).performClick()

    private fun lyricTop(): Float =
        composeTestRule
            .onNodeWithText("I wanna live", substring = true)
            .fetchSemanticsNode()
            .boundsInRoot
            .top

    @Test
    fun theDetailsShowByDefault() {
        renderViewer()

        composeTestRule.onNodeWithText("Neil Young").assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.songbook_transpose)).assertIsDisplayed()
    }

    @Test
    fun collapsingHidesTheDetails() {
        renderViewer()
        collapse()

        composeTestRule.onNodeWithText("Neil Young").assertDoesNotExist()
        composeTestRule.onNodeWithText("From Harvest").assertDoesNotExist()
        composeTestRule.onNodeWithText(str(R.string.songbook_transpose)).assertDoesNotExist()
        composeTestRule.onNodeWithText(str(R.string.songbook_key_prefix) + "G").assertDoesNotExist()
    }

    @Test
    fun collapsingKeepsTheLyricsReadable() {
        renderViewer()
        collapse()

        composeTestRule.onNodeWithText("I wanna live", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Heart of Gold").assertIsDisplayed()
    }

    /**
     * The whole point of #501: hiding the details has to hand the reclaimed height to
     * the song. Asserting only that the metadata disappeared would still pass if the
     * panel collapsed into an equally tall blank gap.
     */
    @Test
    fun collapsingLiftsTheLyricsUpTheScreen() {
        renderViewer()
        val expandedTop = lyricTop()

        collapse()
        val collapsedTop = lyricTop()

        assertTrue(
            "lyrics should move up when the details collapse, but went from $expandedTop to $collapsedTop",
            collapsedTop < expandedTop - 100f,
        )
    }

    @Test
    fun expandingBringsTheDetailsBack() {
        renderViewer()
        collapse()
        expand()

        composeTestRule.onNodeWithText("Neil Young").assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.songbook_transpose)).assertIsDisplayed()
    }

    @Test
    fun theToggleAnnouncesWhichWayItGoes() {
        renderViewer()

        toggle(R.string.songbook_collapse_details).assertExists()
        toggle(R.string.songbook_expand_details).assertDoesNotExist()

        collapse()

        toggle(R.string.songbook_expand_details).assertExists()
        toggle(R.string.songbook_collapse_details).assertDoesNotExist()
    }

    /** A screen-reader user drives this by touch too — 48dp is the Android minimum. */
    @Test
    fun theToggleIsABigEnoughTouchTarget() {
        renderViewer()

        toggle(R.string.songbook_collapse_details).assertHeightIsAtLeast(48.dp)
    }

    /** "Ideally the collapsed state could remain active" — it outlives the screen. */
    @Test
    fun theCollapsedStateIsRemembered() {
        renderViewer()
        collapse()

        composeTestRule.runOnIdle {
            val stored =
                context()
                    .getSharedPreferences("songbook_prefs", Context.MODE_PRIVATE)
                    .getBoolean("song_details_collapsed", false)
            assertTrue("collapsing should persist so the next song opens compact too", stored)
        }
    }
}
