package com.baijum.ukufretboard.viewmodel

import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Scale
import com.baijum.ukufretboard.data.Scales
import com.baijum.ukufretboard.data.TuningSettings
import com.baijum.ukufretboard.data.UkuleleTuning
import com.baijum.ukufretboard.domain.ChordDetector
import com.baijum.ukufretboard.domain.ChordVoicing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FretboardViewModelTest {

    private lateinit var vm: FretboardViewModel

    @Before
    fun setUp() {
        vm = FretboardViewModel()
    }

    // ── Initial state ────────────────────────────────────────────────

    @Test
    fun initialStateHasNoSelectionsAndShowsNoteNames() {
        val state = vm.uiState.value
        state.selections.values.forEach { assertNull(it) }
        assertTrue(state.showNoteNames)
        assertEquals(0, state.capoFret)
        assertEquals(FretboardViewModel.LAST_FRET, state.lastFret)
        assertTrue(state.detectionResult is ChordDetector.DetectionResult.NoSelection)
    }

    @Test
    fun initialTuningIsStandardHighG() {
        assertEquals(4, vm.tuning.size)
        assertEquals("G", vm.tuning[0].name)
        assertEquals(7, vm.tuning[0].openPitchClass)
        assertEquals("C", vm.tuning[1].name)
        assertEquals(0, vm.tuning[1].openPitchClass)
        assertEquals("E", vm.tuning[2].name)
        assertEquals(4, vm.tuning[2].openPitchClass)
        assertEquals("A", vm.tuning[3].name)
        assertEquals(9, vm.tuning[3].openPitchClass)
    }

    // ── toggleFret ───────────────────────────────────────────────────

    @Test
    fun toggleFretSelectsAndDeselects() {
        vm.toggleFret(0, 3)
        assertEquals(3, vm.uiState.value.selections[0])

        vm.toggleFret(0, 3)
        assertNull(vm.uiState.value.selections[0])
    }

    @Test
    fun toggleFretReplacesExistingSelection() {
        vm.toggleFret(1, 2)
        assertEquals(2, vm.uiState.value.selections[1])

        vm.toggleFret(1, 5)
        assertEquals(5, vm.uiState.value.selections[1])
    }

    @Test
    fun toggleFretBlockedByCapo() {
        vm.setCapoFret(3)
        vm.toggleFret(0, 2)
        assertNull("Fret behind capo should be blocked", vm.uiState.value.selections[0])

        vm.toggleFret(0, 3)
        assertNull("Fret at capo should be blocked", vm.uiState.value.selections[0])

        vm.toggleFret(0, 0)
        assertEquals("Open string allowed with capo", 0, vm.uiState.value.selections[0])

        vm.toggleFret(0, 0)
        vm.toggleFret(0, 4)
        assertEquals("Fret beyond capo allowed", 4, vm.uiState.value.selections[0])
    }

    @Test
    fun toggleFretTriggersChordDetection() {
        vm.toggleFret(0, 0)
        vm.toggleFret(1, 0)
        vm.toggleFret(2, 0)
        vm.toggleFret(3, 0)
        val result = vm.uiState.value.detectionResult
        assertTrue("All open strings should detect a chord", result is ChordDetector.DetectionResult.ChordFound)
    }

    // ── clearAll ─────────────────────────────────────────────────────

    @Test
    fun clearAllResetsSelectionsPreservesNoteNames() {
        vm.setShowNoteNames(false)
        vm.toggleFret(0, 3)
        vm.toggleFret(2, 5)
        vm.clearAll()
        val state = vm.uiState.value
        state.selections.values.forEach { assertNull(it) }
        assertFalse("showNoteNames should be preserved", state.showNoteNames)
    }

    // ── setCapoFret ──────────────────────────────────────────────────

    @Test
    fun setCapoFretClearsSelectionsBehindCapo() {
        vm.toggleFret(0, 2)
        vm.toggleFret(1, 5)
        vm.setCapoFret(3)
        assertNull("Fret 2 behind capo 3 should be cleared", vm.uiState.value.selections[0])
        assertEquals("Fret 5 beyond capo 3 should remain", 5, vm.uiState.value.selections[1])
        assertEquals(3, vm.uiState.value.capoFret)
    }

    @Test
    fun setCapoFretCoercesToLastFret() {
        vm.setCapoFret(99)
        assertEquals(vm.uiState.value.lastFret, vm.uiState.value.capoFret)
    }

    // ── setLastFret ──────────────────────────────────────────────────

    @Test
    fun setLastFretCoercesRange() {
        vm.setLastFret(5)
        assertEquals(FretboardViewModel.LAST_FRET, vm.uiState.value.lastFret)

        vm.setLastFret(30)
        assertEquals(FretboardViewModel.MAX_LAST_FRET, vm.uiState.value.lastFret)
    }

    @Test
    fun setLastFretClearsSelectionsAndCoercesCapo() {
        vm.setCapoFret(5)
        vm.setLastFret(22)
        vm.toggleFret(0, 20)
        assertEquals(20, vm.uiState.value.selections[0])

        vm.setLastFret(15)
        assertNull("Fret 20 beyond new last fret 15 should be cleared", vm.uiState.value.selections[0])
        assertEquals(5, vm.uiState.value.capoFret)
    }

    // ── getNoteAt ────────────────────────────────────────────────────

    @Test
    fun getNoteAtComputesPitchClassCorrectly() {
        val noteG0 = vm.getNoteAt(0, 0)
        assertEquals(7, noteG0.pitchClass)

        val noteA1 = vm.getNoteAt(0, 2)
        assertEquals(9, noteA1.pitchClass)

        val noteC0 = vm.getNoteAt(1, 0)
        assertEquals(0, noteC0.pitchClass)
    }

    @Test
    fun getNoteAtAccountsForCapo() {
        vm.setCapoFret(2)
        val note = vm.getNoteAt(1, 0)
        assertEquals("C string open + capo 2 = D (pitch class 2)", 2, note.pitchClass)
    }

    // ── fingerPositions ──────────────────────────────────────────────

    @Test
    fun fingerPositionsFormatsCorrectly() {
        assertEquals("x - x - x - x", vm.uiState.value.fingerPositions)

        vm.toggleFret(0, 0)
        vm.toggleFret(1, 0)
        vm.toggleFret(2, 0)
        vm.toggleFret(3, 3)
        assertEquals("0 - 0 - 0 - 3", vm.uiState.value.fingerPositions)
    }

    // ── Scale overlay ────────────────────────────────────────────────

    @Test
    fun toggleScaleOverlayTogglesEnabled() {
        assertFalse(vm.uiState.value.scaleOverlay.enabled)
        vm.toggleScaleOverlay()
        assertTrue(vm.uiState.value.scaleOverlay.enabled)
        vm.toggleScaleOverlay()
        assertFalse(vm.uiState.value.scaleOverlay.enabled)
    }

    @Test
    fun setScaleComputesScaleNotesAndEnables() {
        val major = Scales.ALL.first { it.name == "Major" }
        vm.setScaleRoot(0)
        vm.setScale(major)
        val overlay = vm.uiState.value.scaleOverlay
        assertTrue("Setting a scale should enable the overlay", overlay.enabled)
        assertEquals(major.intervals.size, overlay.scaleNotes.size)
        assertTrue("C major should contain pitch class 0 (C)", 0 in overlay.scaleNotes)
    }

    // ── buildTuning ──────────────────────────────────────────────────

    @Test
    fun buildTuningHighGAndLowGDiffer() {
        val highG = FretboardViewModel.buildTuning(UkuleleTuning.HIGH_G)
        val lowG = FretboardViewModel.buildTuning(UkuleleTuning.LOW_G)
        assertEquals(4, highG.size)
        assertEquals(4, lowG.size)
        assertEquals(highG[0].openPitchClass, lowG[0].openPitchClass)
        assertTrue(
            "Low-G should have a lower octave on the G string",
            lowG[0].octave < highG[0].octave,
        )
    }

    @Test
    fun setTuningSettingsUpdatesTuningAndState() {
        vm.setTuningSettings(TuningSettings(tuning = UkuleleTuning.LOW_G))
        val state = vm.uiState.value
        assertEquals(4, state.tuning.size)
        assertEquals(UkuleleTuning.LOW_G.octaves[0], state.tuning[0].octave)
    }

    // ── applyVoicing honours the capo (#574) ─────────────────────────

    /** The open C-major shape (G-C-E-A tuning): G0, C0, E0, C on A-string. */
    private fun cMajorVoicing(): ChordVoicing =
        ChordVoicing(
            frets = listOf(0, 0, 0, 3),
            notes = listOf(null, null, null, null),
            minFret = 0,
            maxFret = 3,
        )

    private val majorFormula = ChordFormulas.ALL.first { it.quality == "Major" }

    @Test
    fun applyVoicingWithoutCapoUsesLibraryName() {
        vm.applyVoicing(cMajorVoicing(), rootPitchClass = 0, formula = majorFormula)
        val result = vm.uiState.value.detectionResult
        assertTrue(result is ChordDetector.DetectionResult.ChordFound)
        val chord = (result as ChordDetector.DetectionResult.ChordFound).result
        assertEquals("No capo: library C shape stays C", "C", chord.name)
        assertEquals(0, chord.root.pitchClass)
        assertEquals(
            "Notes are C-E-G",
            setOf(0, 4, 7),
            chord.notes.map { it.pitchClass }.toSet(),
        )
    }

    @Test
    fun applyVoicingShiftsNameAndNotesByCapo() {
        vm.setCapoFret(2)
        vm.applyVoicing(cMajorVoicing(), rootPitchClass = 0, formula = majorFormula)

        val result = vm.uiState.value.detectionResult
        assertTrue(result is ChordDetector.DetectionResult.ChordFound)
        val chord = (result as ChordDetector.DetectionResult.ChordFound).result

        // Capo 2 raises the C shape by two semitones: it sounds D major.
        assertEquals("Capo 2 + library C shape sounds D", "D", chord.name)
        assertEquals(2, chord.root.pitchClass)
        assertEquals(
            "Notes are the sounding D-F#-A, not C-E-G",
            setOf(2, 6, 9),
            chord.notes.map { it.pitchClass }.toSet(),
        )
    }

    @Test
    fun applyVoicingWithCapoMatchesManualTap() {
        // Library path: apply the C shape with a capo at fret 2.
        vm.setCapoFret(2)
        vm.applyVoicing(cMajorVoicing(), rootPitchClass = 0, formula = majorFormula)
        val applied = vm.uiState.value.detectionResult
        assertTrue(applied is ChordDetector.DetectionResult.ChordFound)
        val appliedChord = (applied as ChordDetector.DetectionResult.ChordFound).result

        // Manual path: tap the identical frets with the same capo.
        // clearAll() resets the capo, so re-apply it before tapping.
        vm.clearAll()
        vm.setCapoFret(2)
        vm.toggleFret(0, 0)
        vm.toggleFret(1, 0)
        vm.toggleFret(2, 0)
        vm.toggleFret(3, 3)
        val tapped = vm.uiState.value.detectionResult
        assertTrue(tapped is ChordDetector.DetectionResult.ChordFound)
        val tappedChord = (tapped as ChordDetector.DetectionResult.ChordFound).result

        assertEquals("Applied name matches manual-tap name", tappedChord.name, appliedChord.name)
        assertEquals(
            "Applied notes match manual-tap notes",
            tappedChord.notes.map { it.pitchClass }.toSet(),
            appliedChord.notes.map { it.pitchClass }.toSet(),
        )
    }

    @Test
    fun applyVoicingHonoursCapoInLowGTuning() {
        vm.setTuningSettings(TuningSettings(tuning = UkuleleTuning.LOW_G))
        vm.setCapoFret(2)
        vm.applyVoicing(cMajorVoicing(), rootPitchClass = 0, formula = majorFormula)

        val result = vm.uiState.value.detectionResult
        assertTrue(result is ChordDetector.DetectionResult.ChordFound)
        val chord = (result as ChordDetector.DetectionResult.ChordFound).result

        // Pitch classes are tuning-independent, so Low-G sounds D major too.
        assertEquals("Low-G capo 2 + C shape sounds D", "D", chord.name)
        assertEquals(
            setOf(2, 6, 9),
            chord.notes.map { it.pitchClass }.toSet(),
        )
    }
}
