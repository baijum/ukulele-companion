package com.baijum.ukufretboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.domain.UkuleleString
import com.baijum.ukufretboard.ui.songbook.SheetViewer
import org.junit.Rule
import org.junit.Test

/**
 * Fullscreen reached through the real viewer (issue #520, and the chord-lookup half
 * of #501).
 *
 * [PerformanceModeRenderingTest] covers the rendering in isolation; this covers the
 * wiring — that [SheetViewer] hands its display settings down, and that the chord
 * detail sheet is emitted alongside fullscreen rather than only in the normal viewer,
 * where the early `return` used to leave it unreachable.
 *
 * Run with: ./gradlew connectedAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.baijum.ukufretboard.SheetViewerPerformanceModeTest
 */
class SheetViewerPerformanceModeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val highG =
        listOf(
            UkuleleString(name = "G", openPitchClass = 7),
            UkuleleString(name = "C", openPitchClass = 0),
            UkuleleString(name = "E", openPitchClass = 4),
            UkuleleString(name = "A", openPitchClass = 9),
        )

    private fun str(
        id: Int,
        vararg args: Any,
    ): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.getString(id, *args)
    }

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

    private fun renderViewer() {
        composeTestRule.setContent {
            MaterialTheme {
                SheetViewer(
                    sheet =
                        ChordSheet(
                            title = "Heart of Gold",
                            artist = "Neil Young",
                            content = "[Em]I wanna live",
                        ),
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
                    // The diagram rail repeats every chord name; switching it off keeps
                    // "Em" unambiguous so these tests address the sheet itself.
                    showChordDiagramRail = false,
                )
            }
        }
    }

    private fun enterFullscreen() {
        composeTestRule.onNodeWithContentDescription(str(R.string.performance_mode)).performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun fullscreenShowsTheParsedSheetNotTheMarkup() {
        renderViewer()
        enterFullscreen()

        composeTestRule.onNodeWithText("[Em]I wanna live").assertDoesNotExist()
        composeTestRule.onNodeWithText("I wanna live").assertExists()
        composeTestRule.onNodeWithText("Em").assertExists()
    }

    @Test
    fun tappingAChordInFullscreenOpensTheChordSheet() {
        // The detail sheet lives after SheetViewer's early `return` for performance
        // mode, so without an explicit emission there the tap does nothing.
        renderViewer()
        enterFullscreen()

        composeTestRule.onNodeWithText("Em").clickCharacter(0)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(str(R.string.songbook_play_chord, "Em")).assertExists()
    }

    @Test
    fun theNormalViewerStillRendersTheParsedSheet() {
        // Guards the extraction itself: SheetViewer now delegates its per-line
        // rendering to the same composable fullscreen uses.
        renderViewer()

        composeTestRule.onNodeWithText("I wanna live").assertExists()
        composeTestRule.onNodeWithText("Em").assertExists()
    }

    @Test
    fun tappingAChordInTheNormalViewerStillOpensTheChordSheet() {
        renderViewer()

        composeTestRule.onNodeWithText("Em").clickCharacter(0)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(str(R.string.songbook_play_chord, "Em")).assertExists()
    }
}
