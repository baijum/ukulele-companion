package com.baijum.ukufretboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.ui.songbook.SheetEditor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the key and capo fields in the song editor (issue #518).
 *
 * The defect these guard against was a wiring one: the editor never passed the
 * key to the ViewModel, so every save silently cleared it. Unit tests cover the
 * ViewModel side; only a UI test can catch the callback dropping a field again.
 *
 * Run with: ./gradlew connectedAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.baijum.ukufretboard.SheetEditorKeyCapoTest
 */
class SheetEditorKeyCapoTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private data class SaveArgs(
        val title: String,
        val artist: String,
        val content: String,
        val key: String,
        val capo: Int,
        val strumPatternName: String,
        val labels: List<String>,
    )

    private var saved: SaveArgs? = null

    private fun str(
        id: Int,
        vararg args: Any,
    ): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.getString(id, *args)
    }

    private fun renderEditor(sheet: ChordSheet?) {
        saved = null
        composeTestRule.setContent {
            MaterialTheme {
                SheetEditor(
                    sheet = sheet,
                    allLabels = emptySet(),
                    onSave = { title, artist, content, key, capo, strumPatternName, labels ->
                        saved = SaveArgs(title, artist, content, key, capo, strumPatternName, labels)
                    },
                    onCancel = {},
                )
            }
        }
    }

    private fun existingSheet(): ChordSheet =
        ChordSheet(
            title = "Heart of Gold",
            artist = "Neil Young",
            content = "[Em]I want to live",
            key = "G",
            capo = 2,
        )

    private fun tapSave() {
        composeTestRule.onNodeWithText(str(R.string.dialog_save)).performClick()
    }

    @Test
    fun savingWithoutChangesPreservesKeyAndCapo() {
        // The exact #518 repro: open an imported song, change nothing, save.
        renderEditor(existingSheet())

        tapSave()

        assertEquals("G", saved?.key)
        assertEquals(2, saved?.capo)
    }

    @Test
    fun editorPrefillsStoredKeyAndCapo() {
        renderEditor(existingSheet())

        composeTestRule.onNodeWithText("G").assertExists()
        composeTestRule.onNodeWithText("2").assertExists()
    }

    @Test
    fun capoStepperIncrementsAndDecrements() {
        renderEditor(existingSheet())

        val increase = str(R.string.cd_increase_capo)
        val decrease = str(R.string.cd_decrease_capo)

        composeTestRule.onNodeWithContentDescription(increase).performClick()
        composeTestRule.onNodeWithText("3").assertExists()

        composeTestRule.onNodeWithContentDescription(decrease).performClick()
        composeTestRule.onNodeWithContentDescription(decrease).performClick()
        composeTestRule.onNodeWithText("1").assertExists()
    }

    @Test
    fun capoValueAnnouncesItsMeaningNotABareNumber() {
        // A bare "3" carries nothing once TalkBack focus leaves the stepper buttons.
        renderEditor(existingSheet())

        composeTestRule.onNodeWithContentDescription(str(R.string.songbook_capo_value, 2)).assertExists()
    }

    @Test
    fun capoZeroAnnouncesNoCapo() {
        renderEditor(existingSheet().copy(capo = 0))

        composeTestRule.onNodeWithContentDescription(str(R.string.capo_calc_no_capo)).assertExists()
    }

    @Test
    fun editedKeyReachesTheSaveCallback() {
        renderEditor(existingSheet())

        composeTestRule.onNodeWithText("G").performTextClearance()
        composeTestRule.onNodeWithText(str(R.string.songbook_field_key)).performTextInput("Bb Mixolydian")
        tapSave()

        // Free-form key text must survive verbatim — a picker would drop this value.
        assertEquals("Bb Mixolydian", saved?.key)
    }

    @Test
    fun newSongStartsWithNoKeyAndNoCapo() {
        renderEditor(null)

        composeTestRule.onNodeWithText(str(R.string.songbook_field_title)).performTextInput("Untitled")
        tapSave()

        assertEquals("", saved?.key)
        assertEquals(0, saved?.capo)
    }
}
