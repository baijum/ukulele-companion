package com.baijum.ukufretboard.viewmodel

import com.baijum.ukufretboard.data.SoundSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression tests for issue #581: the step-sequencer grid is scratch state
 * that is never persisted with the melody, so grid edits must NOT mark the
 * document as having unsaved changes. Real note edits still must.
 */
class MelodyViewModelTest {
    private lateinit var vm: MelodyViewModel

    @Before
    fun setUp() {
        vm = MelodyViewModel()
        // Disable sound so playNote() does not launch a coroutine on the
        // (unavailable) Main dispatcher during the unit test.
        vm.setSoundSettings(SoundSettings(enabled = false))
    }

    @Test
    fun setStepDoesNotMarkDocumentDirty() {
        assertFalse(vm.uiState.value.hasUnsavedChanges)

        vm.setStep(index = 0, pitchClass = 0)

        val state = vm.uiState.value
        assertNotNull("Step should be written to the scratch grid", state.steps[0])
        assertFalse(
            "Editing the scratch-pad grid must not flag unsaved changes",
            state.hasUnsavedChanges,
        )
    }

    @Test
    fun clearStepDoesNotMarkDocumentDirty() {
        vm.setStep(index = 0, pitchClass = 0)

        vm.clearStep(index = 0)

        val state = vm.uiState.value
        assertNull("Step should be cleared", state.steps[0])
        assertFalse(
            "Clearing a scratch-pad step must not flag unsaved changes",
            state.hasUnsavedChanges,
        )
    }

    @Test
    fun addNoteStillMarksDocumentDirty() {
        assertFalse(vm.uiState.value.hasUnsavedChanges)

        vm.addNote(pitchClass = 0, octave = 4)

        val state = vm.uiState.value
        assertTrue("A real note edit must still flag unsaved changes", state.hasUnsavedChanges)
        assertTrue(state.notes.isNotEmpty())
    }
}
