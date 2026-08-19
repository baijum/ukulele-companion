package com.baijum.ukufretboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.baijum.ukufretboard.data.ChordParser
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.domain.KeyDetector
import com.baijum.ukufretboard.domain.UkuleleString
import com.baijum.ukufretboard.ui.songbook.SheetViewer
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the key, capo and subtitle displayed above a song (issues #518, #519).
 *
 * A stored key is authoritative and must win over the KeyDetector guess — showing
 * the guess instead is what made the file in #500 read "Key:E minor" for a song
 * declaring {key:G}. The key must also track a transpose preview, or it advertises
 * a key the chords are not in.
 *
 * Run with: ./gradlew connectedAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.baijum.ukufretboard.SheetViewerKeyDisplayTest
 */
class SheetViewerKeyDisplayTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val highG =
        listOf(
            UkuleleString(name = "G", openPitchClass = 7),
            UkuleleString(name = "C", openPitchClass = 0),
            UkuleleString(name = "E", openPitchClass = 4),
            UkuleleString(name = "A", openPitchClass = 9),
        )

    private fun str(id: Int): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.getString(id)
    }

    /** A song whose chords imply E minor, but which declares G — the #500 case. */
    private fun sheetDeclaringG(): ChordSheet =
        ChordSheet(
            title = "Heart of Gold",
            subtitle = "From Harvest",
            artist = "Neil Young",
            content = "[Em]I want to live [Em]I want to give",
            key = "G",
            capo = 2,
        )

    private fun renderViewer(sheet: ChordSheet) {
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

    @Test
    fun storedKeyWinsOverTheDetectorGuess() {
        renderViewer(sheetDeclaringG())

        composeTestRule.onNodeWithText(str(R.string.songbook_key_prefix) + "G").assertExists()
    }

    @Test
    fun detectorFillsInWhenTheSongDeclaresNoKey() {
        renderViewer(sheetDeclaringG().copy(key = ""))

        // With no declared key the detector's own guess is shown, whatever it is.
        val chords = ChordParser.extractChords("[Em]I want to live [Em]I want to give")
        val detected = KeyDetector.detectKey(chords)
        assertNotNull("the fixture should produce a detectable key", detected)
        val expected = str(R.string.songbook_key_prefix) + detected!!.displayName
        composeTestRule.onNodeWithText(expected).assertExists()
    }

    @Test
    fun capoIsDisplayedWhenSet() {
        renderViewer(sheetDeclaringG())

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeTestRule.onNodeWithText(context.getString(R.string.songbook_capo_value, 2)).assertExists()
    }

    @Test
    fun capoIsHiddenWhenZero() {
        renderViewer(sheetDeclaringG().copy(capo = 0))

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeTestRule.onNodeWithText(context.getString(R.string.songbook_capo_value, 0)).assertDoesNotExist()
    }

    @Test
    fun subtitleIsDisplayedWhenSet() {
        renderViewer(sheetDeclaringG())

        composeTestRule.onNodeWithText("From Harvest").assertExists()
    }

    @Test
    fun transposePreviewMovesTheDisplayedKey() {
        // Issue #519: the label used to be keyed off sheet.content, so it went stale
        // while the preview transposed displayContent underneath it.
        renderViewer(sheetDeclaringG())

        composeTestRule.onNodeWithContentDescription(str(R.string.cd_transpose_up)).performClick()

        composeTestRule.onNodeWithText(str(R.string.songbook_key_prefix) + "Ab").assertExists()
    }
}
