package com.baijum.ukufretboard.ui

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.viewmodel.SetlistViewModel
import com.baijum.ukufretboard.viewmodel.SongbookViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI wiring tests for [SetlistTab]'s detail view (issue #572).
 *
 * The reorder buttons must pass the tapped song's ID to the view model — not a
 * display index — so reordering stays correct even when the visible list would
 * be a filtered subset of the library. These tests render the real composables
 * under Robolectric, drive the ▲/▼ buttons, and assert against the persisted
 * order.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SetlistTabTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var setlistViewModel: SetlistViewModel
    private lateinit var songbookViewModel: SongbookViewModel

    private lateinit var setlistId: String

    /** Songs render in this order when the test starts. */
    private val titles = listOf("Alpha", "Bravo", "Charlie")

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        setlistViewModel = SetlistViewModel(application)
        songbookViewModel = SongbookViewModel(application)

        titles.forEach { title ->
            songbookViewModel.startEditing()
            songbookViewModel.saveSheet(title = title, artist = "", content = "content")
            songbookViewModel.closeSheet()
        }
        val songs =
            songbookViewModel.allSheets.value
                .sortedBy { it.title }
        assertEquals(titles, songs.map { it.title })

        setlistViewModel.create("Gig")
        setlistId =
            setlistViewModel.setlists.value
                .single { it.name == "Gig" }
                .id
        songs.forEach { setlistViewModel.addSong(setlistId, it.id) }
        setlistViewModel.open(setlistViewModel.setlists.value.single { it.id == setlistId })

        composeTestRule.setContent {
            SetlistTab(
                setlistViewModel = setlistViewModel,
                songbookViewModel = songbookViewModel,
            )
        }
        composeTestRule.waitForIdle()
    }

    /** The setlist's persisted order, as song titles. */
    private fun persistedOrder(): List<String> {
        val titleById =
            songbookViewModel.allSheets.value
                .associate { it.id to it.title }
        return setlistViewModel.setlists.value
            .single { it.id == setlistId }
            .songIds
            .mapNotNull { titleById[it] }
    }

    /**
     * Clicks the ▲/▼ button of the row showing [songTitle].
     *
     * Every row's move button shares one contentDescription, and range-dependent
     * buttons are omitted (no ▲ on the first row, no ▼ on the last), so the
     * button's index within its group has to be derived from the row position.
     */
    private fun clickMoveOn(
        songTitle: String,
        up: Boolean,
    ) {
        val description =
            applicationGetString(
                if (up) R.string.cd_setlist_move_up else R.string.cd_setlist_move_down,
            )
        val rowIndex = titles.indexOf(songTitle)
        // Up buttons start at the second row; down buttons start at the first.
        val buttonIndex = if (up) rowIndex - 1 else rowIndex
        composeTestRule
            .onAllNodesWithContentDescription(description)[buttonIndex]
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun applicationGetString(resId: Int): String =
        ApplicationProvider.getApplicationContext<Application>().getString(resId)

    @Test
    fun moveUpMovesTheTappedSongNotTheIndexedOne() {
        composeTestRule.onNodeWithText("Bravo").assertExists()

        clickMoveOn("Bravo", up = true)

        assertEquals(listOf("Bravo"), persistedOrder().take(1))
        composeTestRule.waitForIdle()
    }

    @Test
    fun moveDownMovesTheTappedSongNotTheIndexedOne() {
        composeTestRule.onNodeWithText("Alpha").assertExists()

        clickMoveOn("Alpha", up = false)

        assertEquals(listOf("Bravo"), persistedOrder().take(1))
        composeTestRule.waitForIdle()
    }

    @Test
    fun removingASongUsesItsId() {
        composeTestRule.onNodeWithText("Bravo").assertExists()

        composeTestRule
            .onAllNodesWithContentDescription(
                applicationGetString(R.string.cd_setlist_remove_song),
            )[1]
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("Alpha", "Charlie"), persistedOrder())
    }
}
