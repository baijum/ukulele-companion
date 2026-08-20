package com.baijum.ukufretboard

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
 * [collapsingPutsTheLyricsBelowTheToggle] — the lyrics must actually end up on screen.
 *
 * Every assertion has to hold on a short screen as well as a tall one. The viewer is
 * a plain `Column`, so a details block taller than the viewport leaves the children
 * after it measured against `maxHeight = 0`: anything phrased as "this moved up by N
 * pixels" reads 0 on a small device and proves nothing.
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

    /** Flipped to false and back to force a fresh [SheetViewer] onto the screen. */
    private var mounted by mutableStateOf(true)

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
    fun resetState() {
        clearPersistedState()
        mounted = true
    }

    @After
    fun restorePrefs() = clearPersistedState()

    private fun sheet(): ChordSheet =
        ChordSheet(
            title = "Heart of Gold",
            subtitle = "From Harvest",
            artist = "Neil Young",
            content = "[Verse 1]\n[Em]I wanna live [C]I wanna give",
            key = "G",
            capo = 2,
            labels = listOf("folk"),
        )

    private fun renderViewer(sheet: ChordSheet = sheet()) {
        composeTestRule.setContent {
            MaterialTheme {
                if (mounted) {
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
                } else {
                    Text(UNMOUNTED)
                }
            }
        }
    }

    /** Tears the viewer off the screen and builds a new one, as reopening a song does. */
    private fun remountViewer() {
        composeTestRule.runOnIdle { mounted = false }
        composeTestRule.onNodeWithText(UNMOUNTED).assertExists()
        composeTestRule.runOnIdle { mounted = true }
        composeTestRule.waitForIdle()
    }

    private fun toggle(id: Int) = composeTestRule.onNodeWithContentDescription(str(id))

    private fun collapse() = toggle(R.string.songbook_collapse_details).performClick()

    private fun expand() = toggle(R.string.songbook_expand_details).performClick()

    private fun lyricNode() = composeTestRule.onNodeWithText("I wanna live", substring = true)

    @Test
    fun theDetailsShowByDefault() {
        renderViewer()

        composeTestRule.onNodeWithText("Neil Young").assertIsDisplayed()
        // Only assertExists: on a short screen the transpose row is below the fold.
        composeTestRule.onNodeWithText(str(R.string.songbook_transpose)).assertExists()
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

        lyricNode().assertIsDisplayed()
        // assertExists, not assertIsDisplayed: the toolbar's six icon buttons total
        // 288dp, so below roughly 320dp of width the title is squeezed to zero and
        // stops being "displayed". That is the pre-existing crowding this panel had
        // to route around, not something collapsing changes.
        composeTestRule.onNodeWithText("Heart of Gold").assertExists()
    }

    /**
     * The whole point of #501: hiding the details has to hand the reclaimed height to
     * the song. Both halves are needed — the displayed check fails on a short screen
     * where the details had starved the lyrics of any height at all, and the gap check
     * fails on a tall one, where the lyrics are visible either way but sit far down the
     * screen until the details go away.
     */
    @Test
    fun collapsingPutsTheLyricsBelowTheToggle() {
        renderViewer()
        collapse()

        lyricNode().assertIsDisplayed()

        val toggleBottom = toggle(R.string.songbook_expand_details).fetchSemanticsNode().boundsInRoot.bottom
        val lyricTop = lyricNode().fetchSemanticsNode().boundsInRoot.top
        // One section heading ("Verse 1") and one chord row sit between the two.
        val allowance = with(composeTestRule.density) { 80.dp.toPx() }

        assertTrue(
            "the song should start just below the toggle, but sat ${lyricTop - toggleBottom}px lower",
            lyricTop - toggleBottom in 0f..allowance,
        )
    }

    @Test
    fun expandingBringsTheDetailsBack() {
        renderViewer()
        collapse()
        expand()

        composeTestRule.onNodeWithText("Neil Young").assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.songbook_transpose)).assertExists()
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

    /**
     * A screen-reader user drives this by touch too — 48dp is the Android minimum.
     *
     * This also pins the toggle above the details rather than below them: a handle
     * placed after a details block taller than the viewport measures 0dp high.
     */
    @Test
    fun theToggleIsABigEnoughTouchTarget() {
        renderViewer()

        toggle(R.string.songbook_collapse_details).assertHeightIsAtLeast(48.dp)
    }

    /** "Ideally the collapsed state could remain active" — it outlives the screen. */
    @Test
    fun theCollapsedStateIsWrittenToPreferences() {
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

    /** And a viewer built from scratch reads it back, which is what "the next song" means. */
    @Test
    fun aFreshViewerOpensCollapsed() {
        renderViewer()
        collapse()

        remountViewer()

        composeTestRule.onNodeWithText("Neil Young").assertDoesNotExist()
        toggle(R.string.songbook_expand_details).assertExists()
    }

    private companion object {
        const val UNMOUNTED = "viewer torn down"
    }
}
