package com.baijum.ukufretboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.ui.songbook.SheetEditor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for unsaved-edit survival across recreation (issue #573).
 *
 * The defect was that every editor field used plain [androidx.compose.runtime.remember],
 * so an activity recreation (rotation, system font/display size change, split-screen)
 * silently restored the last *saved* values and discarded everything typed.
 *
 * The test simulates recreation the same way Compose does it: the whole composition
 * under a [androidx.compose.runtime.saveable.SaveableStateHolder.SaveableStateProvider]
 * is disposed and re-provided with the same key, so only `rememberSaveable` state
 * survives — exactly what happens across a configuration change.
 *
 * Run with: ./gradlew connectedAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.baijum.ukufretboard.SheetEditorStateRestorationTest
 */
class SheetEditorStateRestorationTest {
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

    /** Toggling this disposes and re-creates the editor under the same saveable key. */
    private lateinit var editorVisible: androidx.compose.runtime.MutableState<Boolean>

    private fun str(id: Int): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.getString(id)
    }

    private fun renderEditor(sheet: ChordSheet?) {
        saved = null
        editorVisible = mutableStateOf(true)
        composeTestRule.setContent {
            MaterialTheme {
                val holder = rememberSaveableStateHolder()
                if (editorVisible.value) {
                    holder.SaveableStateProvider("sheet-editor") {
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
        }
    }

    private fun simulateRecreation() {
        composeTestRule.runOnUiThread { editorVisible.value = false }
        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread { editorVisible.value = true }
        composeTestRule.waitForIdle()
    }

    private fun tapSave() {
        composeTestRule.onNodeWithText(str(R.string.dialog_save)).performClick()
    }

    @Test
    fun typedTitleAndLyricsSurviveRecreation() {
        renderEditor(null)

        composeTestRule
            .onNodeWithText(str(R.string.songbook_field_title))
            .performTextInput("Rotation Test")
        composeTestRule
            .onNodeWithText(str(R.string.songbook_field_lyrics))
            .performTextInput("[C]First verse\nSecond line")

        simulateRecreation()

        composeTestRule.onNodeWithText("Rotation Test").assertExists()

        tapSave()
        assertEquals("Rotation Test", saved?.title)
        assertEquals("[C]First verse\nSecond line", saved?.content)
    }

    @Test
    fun editedLabelsSurviveRecreation() {
        renderEditor(null)

        composeTestRule
            .onNodeWithText(str(R.string.songbook_field_title))
            .performTextInput("Label Test")
        composeTestRule
            .onNodeWithText(str(R.string.songbook_label_hint))
            .performTextInput("folk")
        composeTestRule.onNodeWithContentDescription(str(R.string.cd_add_label)).performClick()

        simulateRecreation()

        // The added label chip must still be there after recreation.
        composeTestRule.onNodeWithText("folk").assertExists()

        tapSave()
        assertEquals(listOf("folk"), saved?.labels)
    }

    @Test
    fun editsOnAnExistingSongSurviveRecreation() {
        // The exact #573 repro shape: edit a saved song without saving.
        renderEditor(
            ChordSheet(
                title = "Heart of Gold",
                artist = "Neil Young",
                content = "[Em]I want to live",
                key = "G",
                capo = 2,
            ),
        )

        composeTestRule
            .onNodeWithText(str(R.string.songbook_field_lyrics))
            .performTextInput("\n[Am]More lyrics")

        simulateRecreation()

        tapSave()
        assertEquals("Heart of Gold", saved?.title)
        assertEquals("[Em]I want to live\n[Am]More lyrics", saved?.content)
    }
}
