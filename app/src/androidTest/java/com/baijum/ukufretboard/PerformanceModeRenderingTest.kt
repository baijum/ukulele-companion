package com.baijum.ukufretboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import com.baijum.ukufretboard.data.ChordColorOption
import com.baijum.ukufretboard.data.ChordDisplayStyle
import com.baijum.ukufretboard.ui.songbook.PerformanceModeView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for fullscreen / performance mode rendering (issue #520).
 *
 * Fullscreen used to print `displayContent.lines()` verbatim, so the reader saw raw
 * `[Em]` markers instead of a chord sheet, and the whole song sat inside one clickable
 * node labelled "toggle controls" — unreadable under TalkBack.
 *
 * Run with: ./gradlew connectedAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.baijum.ukufretboard.PerformanceModeRenderingTest
 */
class PerformanceModeRenderingTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val song =
        """
        [Chorus]
        [Em]I wanna live, [C]I wanna give
        """.trimIndent()

    private var tapped: String? = null

    /**
     * Clicks [charIndex] characters into a monospace line. `performClick` hits the
     * node's centre, which for a chord row lands in the padding between chords.
     */
    private fun SemanticsNodeInteraction.clickCharacter(charIndex: Int) =
        performTouchInput {
            val charWidth =
                width / (
                    fetchSemanticsNode()
                        .config[SemanticsProperties.Text]
                        .first()
                        .length
                )
            click(Offset(charWidth * (charIndex + 0.5f), centerY))
        }

    private fun str(id: Int): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.getString(id)
    }

    private fun renderPerformanceMode(
        content: String = song,
        chordDisplayStyle: ChordDisplayStyle = ChordDisplayStyle.ABOVE,
    ) {
        tapped = null
        composeTestRule.setContent {
            MaterialTheme {
                PerformanceModeView(
                    displayContent = content,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                    onExit = {},
                    chordDisplayStyle = chordDisplayStyle,
                    chordColor = ChordColorOption.THEME,
                    onChordTap = { tapped = it },
                )
            }
        }
    }

    @Test
    fun rawChordProMarkupIsNotShown() {
        // The exact symptom from the report: "[Em]I wanna l[C]ive" on screen.
        renderPerformanceMode()

        composeTestRule.onNodeWithText("[Em]I wanna live, [C]I wanna give").assertDoesNotExist()
    }

    @Test
    fun lyricsAreShownWithoutTheirChordMarkers() {
        renderPerformanceMode()

        composeTestRule.onNodeWithText("I wanna live, I wanna give").assertIsDisplayed()
    }

    @Test
    fun chordNamesAreRenderedOnTheirOwnRowAboveTheLyrics() {
        renderPerformanceMode()

        // Chords sit at columns 0 and 14, so the row reads "Em" then padding then "C".
        composeTestRule.onNodeWithText("Em            C").assertIsDisplayed()
    }

    @Test
    fun sectionHeadingsAreStyledRatherThanPrintedAsBrackets() {
        renderPerformanceMode()

        composeTestRule.onNodeWithText("Chorus").assertIsDisplayed()
        composeTestRule.onNodeWithText("[Chorus]").assertDoesNotExist()
    }

    @Test
    fun tappingAChordReportsIt() {
        // #501's ask: chord lookup without leaving fullscreen.
        renderPerformanceMode()

        composeTestRule.onNodeWithText("Em            C").clickCharacter(0)

        assertEquals("Em", tapped)
    }

    @Test
    fun inlineStyleIsHonouredInFullscreen() {
        renderPerformanceMode(chordDisplayStyle = ChordDisplayStyle.INLINE)

        // Inline keeps the brackets, but as a tappable link rather than dead text —
        // and it must not also emit the separate chords-above row.
        composeTestRule.onNodeWithText("Em            C").assertDoesNotExist()
        composeTestRule.onNodeWithText("[Em]I wanna live, [C]I wanna give").clickCharacter(1)

        assertEquals("Em", tapped)
    }

    @Test
    fun theSongIsNotCollapsedIntoOneLabelledNode() {
        // The root Box used to be `clickable` with contentDescription "toggle controls".
        // clickable merges its descendants, so that label replaced the entire song for
        // a screen reader. The node carrying the lyrics must carry no such label.
        renderPerformanceMode()

        val lyricNode =
            composeTestRule
                .onNode(hasText("I wanna live", substring = true))
                .fetchSemanticsNode()

        assertNull(
            "the lyric node must not be merged into a labelled 'toggle controls' node",
            lyricNode.config.getOrNull(SemanticsProperties.ContentDescription),
        )
    }

    @Test
    fun tappingTheBackgroundStillTogglesTheControls() {
        renderPerformanceMode()

        val exitLabel = str(R.string.performance_mode_exit)
        composeTestRule.onNodeWithContentDescription(exitLabel).assertExists()

        composeTestRule.onRoot().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(exitLabel).assertDoesNotExist()
    }

    @Test
    fun tappingAChordDoesNotAlsoToggleTheControls() {
        // The chord link and the background tap gesture compete for the same event.
        renderPerformanceMode()

        val exitLabel = str(R.string.performance_mode_exit)
        composeTestRule.onNodeWithText("Em            C").clickCharacter(0)
        composeTestRule.waitForIdle()

        assertEquals("Em", tapped)
        composeTestRule.onNodeWithContentDescription(exitLabel).assertExists()
    }
}
