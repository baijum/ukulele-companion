package com.baijum.ukufretboard.viewmodel

import com.baijum.ukufretboard.data.ChordCategory
import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.UkuleleTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChordLibraryViewModelTest {

    private lateinit var vm: ChordLibraryViewModel

    @Before
    fun setUp() {
        vm = ChordLibraryViewModel()
    }

    @Test
    fun initialStateAutoSelectsFirstTriadFormula() {
        val state = vm.uiState.value
        assertEquals(0, state.selectedRoot)
        assertEquals(ChordCategory.TRIAD, state.selectedCategory)
        assertNotNull(state.selectedFormula)
        assertEquals(
            ChordFormulas.BY_CATEGORY[ChordCategory.TRIAD]?.first(),
            state.selectedFormula,
        )
        assertTrue("Should generate voicings for default selection", state.voicings.isNotEmpty())
    }

    @Test
    fun selectRootChangesRootAndRegeneratesVoicings() {
        vm.selectRoot(7)
        val state = vm.uiState.value
        assertEquals(7, state.selectedRoot)
        assertTrue(state.voicings.isNotEmpty())
    }

    @Test
    fun selectCategoryAutoSelectsFirstFormula() {
        vm.selectCategory(ChordCategory.SEVENTH)
        val state = vm.uiState.value
        assertEquals(ChordCategory.SEVENTH, state.selectedCategory)
        assertEquals(
            ChordFormulas.BY_CATEGORY[ChordCategory.SEVENTH]?.first(),
            state.selectedFormula,
        )
    }

    @Test
    fun selectFormulaRegeneratesVoicings() {
        val formulas = ChordFormulas.BY_CATEGORY[ChordCategory.TRIAD]!!
        val minorFormula = formulas.first { it.symbol == "m" }
        vm.selectFormula(minorFormula)
        assertEquals(minorFormula, vm.uiState.value.selectedFormula)
        assertTrue(vm.uiState.value.voicings.isNotEmpty())
    }

    @Test
    fun transposeUpWrapsAroundCorrectly() {
        vm.selectRoot(0)
        vm.transpose(1)
        assertEquals(1, vm.uiState.value.selectedRoot)

        vm.selectRoot(11)
        vm.transpose(1)
        assertEquals(0, vm.uiState.value.selectedRoot)
    }

    @Test
    fun transposeDownWrapsAroundCorrectly() {
        vm.selectRoot(0)
        vm.transpose(-1)
        assertEquals(11, vm.uiState.value.selectedRoot)
    }

    @Test
    fun currentChordNameCombinesRootAndSymbol() {
        vm.selectRoot(0)
        assertEquals("C", vm.currentChordName().substringBefore("m").takeIf { it == "C" } ?: vm.currentChordName())

        val minorFormula = ChordFormulas.BY_CATEGORY[ChordCategory.TRIAD]!!.first { it.symbol == "m" }
        vm.selectRoot(9)
        vm.selectFormula(minorFormula)
        assertEquals("Am", vm.currentChordName())
    }

    @Test
    fun searchQueryReturnsResults() {
        vm.updateSearchQuery("Am")
        assertTrue(vm.searchResults.value.isNotEmpty())
        assertEquals("Am", vm.searchQuery.value)
    }

    @Test
    fun selectSearchResultUpdatesState() {
        vm.updateSearchQuery("Dm7")
        val results = vm.searchResults.value
        if (results.isNotEmpty()) {
            vm.selectSearchResult(results[0])
            assertEquals("", vm.searchQuery.value)
            assertTrue(vm.searchResults.value.isEmpty())
        }
    }

    @Test
    fun setTuningRegeneratesVoicings() {
        val lowG = FretboardViewModel.buildTuning(UkuleleTuning.LOW_G)
        vm.setTuning(lowG)
        assertTrue(vm.uiState.value.voicings.isNotEmpty())
    }
}
