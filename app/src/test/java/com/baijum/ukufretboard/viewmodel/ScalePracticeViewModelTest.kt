package com.baijum.ukufretboard.viewmodel

import com.baijum.ukufretboard.data.FretPosition
import com.baijum.ukufretboard.data.PlayDirection
import com.baijum.ukufretboard.data.PracticeMode
import com.baijum.ukufretboard.data.ScaleCategory
import com.baijum.ukufretboard.data.ScalePracticeSettings
import com.baijum.ukufretboard.data.Scales
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScalePracticeViewModelTest {

    private lateinit var vm: ScalePracticeViewModel

    @Before
    fun setUp() {
        vm = ScalePracticeViewModel()
    }

    @Test
    fun initialStateDefaults() {
        val state = vm.uiState.value
        assertEquals(PracticeMode.PLAY_ALONG, state.mode)
        assertEquals(0, state.selectedRoot)
        assertEquals(ScalePracticeSettings.DEFAULT_BPM, state.bpm)
        assertEquals(PlayDirection.ASCENDING, state.direction)
        assertEquals(PlaybackState.STOPPED, state.playbackState)
        assertFalse(state.showFretboard)
    }

    @Test
    fun setModeUpdatesMode() {
        vm.setMode(PracticeMode.QUIZ)
        assertEquals(PracticeMode.QUIZ, vm.uiState.value.mode)
        vm.setMode(PracticeMode.EAR_TRAINING)
        assertEquals(PracticeMode.EAR_TRAINING, vm.uiState.value.mode)
    }

    @Test
    fun setRootCoercesToValidRange() {
        vm.setRoot(7)
        assertEquals(7, vm.uiState.value.selectedRoot)

        vm.setRoot(-1)
        assertEquals(0, vm.uiState.value.selectedRoot)

        vm.setRoot(15)
        assertEquals(11, vm.uiState.value.selectedRoot)
    }

    @Test
    fun setCategoryFiltersAvailableScales() {
        vm.setCategory(ScaleCategory.BLUES)
        val scales = vm.uiState.value.availableScales
        assertTrue(scales.isNotEmpty())
        assertTrue(scales.all { ScaleCategory.BLUES in it.categories })
    }

    @Test
    fun setCategoryNullShowsAllScales() {
        vm.setCategory(ScaleCategory.BLUES)
        vm.setCategory(null)
        assertEquals(Scales.ALL, vm.uiState.value.availableScales)
    }

    @Test
    fun setBpmCoercesToRange() {
        vm.setBpm(10)
        assertEquals(ScalePracticeSettings.MIN_BPM, vm.uiState.value.bpm)

        vm.setBpm(500)
        assertEquals(ScalePracticeSettings.MAX_BPM, vm.uiState.value.bpm)

        vm.setBpm(120)
        assertEquals(120, vm.uiState.value.bpm)
    }

    @Test
    fun toggleFretboardFlips() {
        assertFalse(vm.uiState.value.showFretboard)
        vm.toggleFretboard()
        assertTrue(vm.uiState.value.showFretboard)
        vm.toggleFretboard()
        assertFalse(vm.uiState.value.showFretboard)
    }

    @Test
    fun toggleLoopFlips() {
        assertFalse(vm.uiState.value.loopPlayback)
        vm.toggleLoop()
        assertTrue(vm.uiState.value.loopPlayback)
        vm.toggleLoop()
        assertFalse(vm.uiState.value.loopPlayback)
    }

    @Test
    fun quizScoringTracksCorrectAndStreak() {
        vm.generateQuizQuestion()
        val question = vm.uiState.value.quizQuestion
        assertNotNull(question)

        val correctIdx = question!!.correctIndex
        assertTrue(vm.submitQuizAnswer(correctIdx))
        assertEquals(1, vm.uiState.value.quizCorrect)
        assertEquals(1, vm.uiState.value.quizTotal)
        assertEquals(1, vm.uiState.value.quizStreak)

        vm.generateQuizQuestion()
        val wrongIdx = (vm.uiState.value.quizQuestion!!.correctIndex + 1) % 4
        assertFalse(vm.submitQuizAnswer(wrongIdx))
        assertEquals(1, vm.uiState.value.quizCorrect)
        assertEquals(2, vm.uiState.value.quizTotal)
        assertEquals(0, vm.uiState.value.quizStreak)
        assertEquals(1, vm.uiState.value.quizBestStreak)
    }

    @Test
    fun earTrainingScoringTracksCorrectAndStreak() {
        vm.generateEarQuestion()
        val question = vm.uiState.value.earQuestion
        assertNotNull(question)

        val correctIdx = question!!.correctIndex
        assertTrue(vm.submitEarAnswer(correctIdx))
        assertEquals(1, vm.uiState.value.earCorrect)
        assertEquals(1, vm.uiState.value.earTotal)
        assertEquals(1, vm.uiState.value.earStreak)

        vm.generateEarQuestion()
        val wrongIdx = (vm.uiState.value.earQuestion!!.correctIndex + 1) % 4
        assertFalse(vm.submitEarAnswer(wrongIdx))
        assertEquals(1, vm.uiState.value.earCorrect)
        assertEquals(2, vm.uiState.value.earTotal)
        assertEquals(0, vm.uiState.value.earStreak)
        assertEquals(1, vm.uiState.value.earBestStreak)
    }

    @Test
    fun restoreAndCurrentSettingsRoundTrip() {
        vm.setRoot(5)
        vm.setBpm(140)
        vm.setMode(PracticeMode.QUIZ)
        vm.toggleFretboard()

        val settings = vm.currentSettings()
        assertEquals(5, settings.lastRoot)
        assertEquals(140, settings.lastBpm)
        assertEquals(PracticeMode.QUIZ.ordinal, settings.lastMode)
        assertTrue(settings.showFretboard)

        val vm2 = ScalePracticeViewModel()
        vm2.restoreSettings(settings)
        val state2 = vm2.uiState.value
        assertEquals(5, state2.selectedRoot)
        assertEquals(140, state2.bpm)
        assertEquals(PracticeMode.QUIZ, state2.mode)
        assertTrue(state2.showFretboard)
    }

    @Test
    fun fretPositionRanges() {
        assertEquals(null, FretPosition.ALL.range())
        assertEquals(0..4, FretPosition.OPEN.range())
        assertEquals(4..8, FretPosition.MID.range())
        assertEquals(7..12, FretPosition.HIGH.range())
        assertEquals(7..18, FretPosition.HIGH.range(lastFret = 18))
    }
}
