package com.baijum.ukufretboard.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.data.SongSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SongbookViewModelTest {

    private lateinit var vm: SongbookViewModel

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        vm = SongbookViewModel(app)
    }

    private fun saveSong(title: String, artist: String = "", labels: List<String> = emptyList()) {
        vm.startEditing()
        vm.saveSheet(title = title, artist = artist, content = "test content", labels = labels)
        vm.closeSheet()
    }

    // ── Initial state ────────────────────────────────────────────────

    @Test
    fun initialStateIsEmpty() {
        assertTrue(vm.sheets.value.isEmpty())
        assertNull(vm.currentSheet.value)
        assertFalse(vm.isEditing.value)
        assertEquals("", vm.searchQuery.value)
        assertEquals(SongSortOrder.LAST_MODIFIED, vm.sortOrder.value)
    }

    // ── Save and list ────────────────────────────────────────────────

    @Test
    fun saveSheetAddsToList() {
        saveSong("Somewhere Over the Rainbow")
        assertEquals(1, vm.sheets.value.size)
        assertEquals("Somewhere Over the Rainbow", vm.sheets.value[0].title)
    }

    @Test
    fun saveSheetUpdatesExistingSheet() {
        saveSong("Draft")
        val sheet = vm.sheets.value[0]
        vm.openSheet(sheet)
        vm.saveSheet(title = "Final Title", artist = "Artist", content = "updated")
        vm.closeSheet()
        assertEquals(1, vm.sheets.value.size)
        assertEquals("Final Title", vm.sheets.value[0].title)
    }

    // ── Search filtering ─────────────────────────────────────────────

    @Test
    fun searchFiltersByTitleAndArtist() {
        saveSong("Riptide", artist = "Vance Joy")
        saveSong("Hey Soul Sister", artist = "Train")
        saveSong("Hallelujah", artist = "Leonard Cohen")

        vm.setSearchQuery("ript")
        assertEquals(1, vm.sheets.value.size)
        assertEquals("Riptide", vm.sheets.value[0].title)

        vm.setSearchQuery("train")
        assertEquals(1, vm.sheets.value.size)
        assertEquals("Hey Soul Sister", vm.sheets.value[0].title)

        vm.setSearchQuery("")
        assertEquals(3, vm.sheets.value.size)
    }

    // ── Sort order ───────────────────────────────────────────────────

    @Test
    fun sortByTitleOrdersAlphabetically() {
        saveSong("Cherry")
        saveSong("Apple")
        saveSong("Banana")

        vm.setSortOrder(SongSortOrder.TITLE)
        val titles = vm.sheets.value.map { it.title }
        assertEquals(listOf("Apple", "Banana", "Cherry"), titles)
    }

    // ── Label filtering ──────────────────────────────────────────────

    @Test
    fun toggleLabelFilterShowsMatchingSongs() {
        saveSong("Song A", labels = listOf("Jazz"))
        saveSong("Song B", labels = listOf("Blues"))
        saveSong("Song C", labels = listOf("Jazz", "Blues"))

        vm.toggleLabelFilter("Jazz")
        val filtered = vm.sheets.value.map { it.title }.toSet()
        assertEquals(setOf("Song A", "Song C"), filtered)

        vm.clearLabelFilter()
        assertEquals(3, vm.sheets.value.size)
    }

    // ── Editing lifecycle ────────────────────────────────────────────

    @Test
    fun startEditingNewSheetSetsEmptySheet() {
        vm.startEditing()
        assertTrue(vm.isEditing.value)
        assertNotNull(vm.currentSheet.value)
        assertEquals("", vm.currentSheet.value!!.title)
    }

    @Test
    fun startEditingExistingSheetPreservesContent() {
        saveSong("Existing Song")
        val sheet = vm.sheets.value[0]
        vm.startEditing(sheet)
        assertTrue(vm.isEditing.value)
        assertEquals("Existing Song", vm.currentSheet.value!!.title)
    }

    // ── Delete ───────────────────────────────────────────────────────

    @Test
    fun deleteSheetRemovesFromList() {
        saveSong("To Delete")
        assertEquals(1, vm.sheets.value.size)
        val id = vm.sheets.value[0].id
        vm.deleteSheet(id)
        assertTrue(vm.sheets.value.isEmpty())
    }

    // ── Selection mode ───────────────────────────────────────────────

    @Test
    fun selectionModeToggleAndClear() {
        saveSong("Song 1")
        saveSong("Song 2")
        val id1 = vm.sheets.value[0].id
        val id2 = vm.sheets.value[1].id

        vm.enterSelectionMode(id1)
        assertTrue(vm.isSelectionMode.value)
        assertEquals(setOf(id1), vm.selectedIds.value)

        vm.toggleSelection(id2)
        assertEquals(setOf(id1, id2), vm.selectedIds.value)

        vm.toggleSelection(id1)
        assertEquals(setOf(id2), vm.selectedIds.value)

        vm.clearSelection()
        assertFalse(vm.isSelectionMode.value)
        assertTrue(vm.selectedIds.value.isEmpty())
    }

    @Test
    fun selectAllSelectsAllVisibleSheets() {
        saveSong("Song 1")
        saveSong("Song 2")
        saveSong("Song 3")

        vm.enterSelectionMode(vm.sheets.value[0].id)
        vm.selectAll()
        assertEquals(3, vm.selectedIds.value.size)
    }

    // ── Duplicate ────────────────────────────────────────────────────

    @Test
    fun duplicateSheetCreatesCopy() {
        saveSong("Original", artist = "Artist")
        val original = vm.sheets.value[0]
        vm.duplicateSheet(original)
        assertEquals(2, vm.sheets.value.size)
        assertTrue(vm.sheets.value.any { it.title == "Original (Copy)" })
    }

    // ── Import (issue #500) ──────────────────────────────────────────

    @Test
    fun importChordProUsesTitleAndArtistFromDirectives() {
        vm.importChordPro(
            "{title:Heart of Gold}\n{artist:Neil Young}\n[Em]I wanna live",
            "Heart-of-Gold.pro",
        )
        val sheet = vm.currentSheet.value!!
        assertEquals("Heart of Gold", sheet.title)
        assertEquals("Neil Young", sheet.artist)
    }

    @Test
    fun importChordProFallsBackToFilenameWhenNoTitleDirective() {
        vm.importChordPro("{artist:Neil Young}\n[Em]I wanna live", "Heart_of_Gold.pro")
        assertEquals("Heart of Gold", vm.currentSheet.value!!.title)
    }

    @Test
    fun importStripsSafDocumentIdPrefixFromTitle() {
        vm.importPlainText("[Am]Hello", "primary:Download/My Song.txt")
        assertEquals("My Song", vm.currentSheet.value!!.title)
    }

    @Test
    fun importNeverUsesAContentUriAsTitle() {
        vm.importPlainText("[Am]Hello", "content://com.microsoft.skydrive.content.metadata/items/42")
        val title = vm.currentSheet.value!!.title
        assertFalse(title.contains("content://"))
        assertEquals("42", title)
    }

    @Test
    fun importWithoutFilenameUsesDefaultTitle() {
        vm.importPlainText("[Am]Hello", null)
        assertEquals("Imported Song", vm.currentSheet.value!!.title)
    }

    @Test
    fun importWithBlankFilenameUsesDefaultTitle() {
        vm.importPlainText("[Am]Hello", "  ")
        assertEquals("Imported Song", vm.currentSheet.value!!.title)
    }

    // ── Key and capo persistence (issue #518) ────────────────────────

    @Test
    fun saveSheetPreservesStoredKeyWhenCallerOmitsIt() {
        // The regression from #518: saveSheet defaulted key to "" and applied it
        // unconditionally, so any save from a caller unaware of the field wiped an
        // imported {key: ...} directive.
        vm.startEditing()
        vm.saveSheet(title = "Heart of Gold", artist = "Neil Young", content = "[G]Body", key = "G")
        vm.closeSheet()

        val stored = vm.sheets.value.single()
        vm.startEditing(stored)
        vm.saveSheet(title = stored.title, artist = stored.artist, content = stored.content)
        vm.closeSheet()

        val reloaded = vm.sheets.value.single()
        assertEquals("G", reloaded.key)
    }

    @Test
    fun saveSheetPreservesStoredCapoWhenCallerOmitsIt() {
        vm.startEditing()
        vm.saveSheet(title = "Capo Song", artist = "", content = "[C]Body", capo = 3)
        vm.closeSheet()

        val stored = vm.sheets.value.single()
        vm.startEditing(stored)
        vm.saveSheet(title = stored.title, artist = stored.artist, content = stored.content)
        vm.closeSheet()

        val reloaded = vm.sheets.value.single()
        assertEquals(3, reloaded.capo)
    }

    @Test
    fun saveSheetClearsKeyWhenExplicitlyEmptied() {
        // "Not supplied" preserves; an explicit "" is a deliberate clear and must apply,
        // otherwise the new editor field could never be emptied.
        vm.startEditing()
        vm.saveSheet(title = "Song", artist = "", content = "[G]Body", key = "G", capo = 2)
        vm.closeSheet()

        val stored = vm.sheets.value.single()
        vm.startEditing(stored)
        vm.saveSheet(title = stored.title, artist = "", content = stored.content, key = "", capo = 0)
        vm.closeSheet()

        val after = vm.sheets.value.single()
        assertEquals("", after.key)
        assertEquals(0, after.capo)
    }

    @Test
    fun saveSheetStoresKeyAndCapoFromTheEditor() {
        vm.startEditing()
        vm.saveSheet(title = "New Song", artist = "", content = "[D]Body", key = "D", capo = 5)
        vm.closeSheet()

        val stored = vm.sheets.value.single()
        assertEquals("D", stored.key)
        assertEquals(5, stored.capo)
    }

    @Test
    fun applyTransposeMovesTheStoredKeyWithTheContent() {
        vm.startEditing()
        vm.saveSheet(title = "Transpose Me", artist = "", content = "[G]Hello [C]world", key = "G")
        vm.closeSheet()

        vm.openSheet(vm.sheets.value.single())
        vm.applyTranspose(2)

        val after = vm.sheets.value.single()
        assertEquals("A", after.key)
        val transposedContent = after.content
        assertTrue("content should be transposed", transposedContent.contains("[A]"))
    }

    @Test
    fun applyTransposeLeavesAnAbsentKeyAbsent() {
        vm.startEditing()
        vm.saveSheet(title = "No Key", artist = "", content = "[G]Hello")
        vm.closeSheet()

        vm.openSheet(vm.sheets.value.single())
        vm.applyTranspose(2)

        val reloaded = vm.sheets.value.single()
        assertEquals("", reloaded.key)
    }
}
