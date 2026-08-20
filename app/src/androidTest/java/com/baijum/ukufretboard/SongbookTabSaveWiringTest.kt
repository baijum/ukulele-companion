package com.baijum.ukufretboard

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.baijum.ukufretboard.ui.songbook.SongbookTab
import com.baijum.ukufretboard.viewmodel.SongbookViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end wiring test for saving a song from the songbook tab (issue #518).
 *
 * SheetEditorKeyCapoTest covers the editor handing key and capo to its callback.
 * This covers the other half — SongbookTab's lambda forwarding them to the
 * ViewModel — which is the line that actually dropped the key: it called
 * saveSheet without a key argument, and the parameter defaulted to "".
 *
 * Run with: ./gradlew connectedAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.baijum.ukufretboard.SongbookTabSaveWiringTest
 */
class SongbookTabSaveWiringTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: SongbookViewModel

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = SongbookViewModel(app)
        // The ViewModel is backed by real SharedPreferences on the device, so start
        // from a known-empty state rather than whatever a previous test left behind.
        viewModel.sheets.value.forEach { viewModel.deleteSheet(it.id) }
    }

    private fun str(id: Int): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.getString(id)
    }

    private fun renderTab() {
        composeTestRule.setContent {
            MaterialTheme {
                SongbookTab(viewModel = viewModel, onChordTapped = {})
            }
        }
    }

    private fun seedSongWithKeyG() {
        viewModel.startEditing()
        viewModel.saveSheet(
            title = "Heart of Gold",
            artist = "Neil Young",
            content = "[Em]I want to live",
            key = "G",
            capo = 2,
        )
        viewModel.closeSheet()
    }

    @Test
    fun aKeyEditedInTheEditorReachesTheViewModel() {
        // This is the assertion that actually exercises SongbookTab's lambda. A save
        // with no changes cannot: saveSheet preserves omitted fields by design, so
        // dropping the argument again would still leave the stored key intact.
        seedSongWithKeyG()
        viewModel.startEditing(viewModel.sheets.value.single())
        renderTab()

        composeTestRule.onNodeWithText("G").performTextClearance()
        composeTestRule.onNodeWithText(str(R.string.songbook_field_key)).performTextInput("D")
        composeTestRule.onNodeWithText(str(R.string.dialog_save)).performClick()
        composeTestRule.waitForIdle()

        val stored = viewModel.sheets.value.single()
        assertEquals("D", stored.key)
    }

    @Test
    fun editingAndSavingWithoutChangesKeepsKeyAndCapo() {
        // The user-facing #518 repro. Double-protected now (the lambda passes the
        // values and saveSheet would preserve them anyway), but it is the behaviour
        // the issue was filed about, so it gets an explicit guard.
        seedSongWithKeyG()
        viewModel.startEditing(viewModel.sheets.value.single())
        renderTab()

        composeTestRule.onNodeWithText(str(R.string.dialog_save)).performClick()
        composeTestRule.waitForIdle()

        val stored = viewModel.sheets.value.single()
        assertEquals("G", stored.key)
        assertEquals(2, stored.capo)
    }
}
