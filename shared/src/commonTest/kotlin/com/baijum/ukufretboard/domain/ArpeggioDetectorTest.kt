package com.baijum.ukufretboard.domain

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArpeggioDetectorTest {

    // --- Not enough notes ---

    @Test
    fun emptyBufferReturnsNull() {
        val detector = ArpeggioDetector()
        assertNull(detector.detect(1000L))
    }

    @Test
    fun singleNoteReturnsNull() {
        val detector = ArpeggioDetector()
        detector.addNote(1000L, 0) // C
        assertNull(detector.detect(1000L))
    }

    @Test
    fun twoNotesReturnsNull() {
        val detector = ArpeggioDetector()
        detector.addNote(1000L, 0) // C
        detector.addNote(1100L, 4) // E
        assertNull(detector.detect(1200L))
    }

    // --- Three notes form chord ---

    @Test
    fun threeNotesWithinWindowDetectsChord() {
        val detector = ArpeggioDetector(windowMs = 3000L)
        detector.addNote(1000L, 0)  // C
        detector.addNote(1500L, 4)  // E
        detector.addNote(2000L, 7)  // G
        val result = detector.detect(2500L)
        assertNotNull(result, "C-E-G should detect a chord")
    }

    @Test
    fun detectedChordHasValidName() {
        val detector = ArpeggioDetector(windowMs = 3000L)
        detector.addNote(1000L, 0)  // C
        detector.addNote(1500L, 4)  // E
        detector.addNote(2000L, 7)  // G
        val result = detector.detect(2500L)
        assertNotNull(result)
        assertTrue(result.result.name.isNotEmpty(), "Chord name should not be empty")
    }

    // --- Notes outside window ---

    @Test
    fun notesOutsideWindowAreIgnored() {
        val detector = ArpeggioDetector(windowMs = 1000L)
        detector.addNote(100L, 0)   // C — will be pruned
        detector.addNote(200L, 4)   // E — will be pruned
        detector.addNote(2000L, 7)  // G — only this remains
        assertNull(detector.detect(2000L), "Old notes should be pruned")
    }

    @Test
    fun notesJustInsideWindowAreKept() {
        val detector = ArpeggioDetector(windowMs = 3000L)
        detector.addNote(1000L, 0)  // C
        detector.addNote(2000L, 4)  // E
        detector.addNote(3000L, 7)  // G
        // At time 3999, the first note (1000) is still within 3000ms window
        val result = detector.detect(3999L)
        assertNotNull(result, "Notes within window should be kept")
    }

    // --- Clear ---

    @Test
    fun clearResetsBuffer() {
        val detector = ArpeggioDetector()
        detector.addNote(1000L, 0)
        detector.addNote(1500L, 4)
        detector.addNote(2000L, 7)
        detector.clear()
        assertNull(detector.detect(2500L))
    }

    // --- Consecutive duplicates ignored ---

    @Test
    fun consecutiveDuplicatesIgnored() {
        val detector = ArpeggioDetector()
        detector.addNote(1000L, 0) // C
        detector.addNote(1100L, 0) // C again — ignored
        detector.addNote(1200L, 0) // C again — ignored
        detector.addNote(1300L, 4) // E
        // Only 2 unique pitch classes: C, E
        assertNull(detector.detect(1500L))
    }

    @Test
    fun nonConsecutiveDuplicatesAreKept() {
        val detector = ArpeggioDetector()
        detector.addNote(1000L, 0) // C
        detector.addNote(1100L, 4) // E
        detector.addNote(1200L, 0) // C again — NOT consecutive duplicate, kept
        // Still only 2 unique pitch classes for chord detection
        assertNull(detector.detect(1300L))
    }

    // --- Minor chord detection ---

    @Test
    fun minorArpeggioDetected() {
        val detector = ArpeggioDetector(windowMs = 3000L)
        detector.addNote(1000L, 9)  // A
        detector.addNote(1500L, 0)  // C
        detector.addNote(2000L, 4)  // E
        val result = detector.detect(2500L)
        assertNotNull(result, "A-C-E should detect Am chord")
    }

    // --- Multiple arpeggios in sequence ---

    @Test
    fun clearBetweenArpeggiosWorks() {
        val detector = ArpeggioDetector(windowMs = 3000L)

        // First arpeggio
        detector.addNote(1000L, 0)
        detector.addNote(1500L, 4)
        detector.addNote(2000L, 7)
        assertNotNull(detector.detect(2500L))

        // Clear and second arpeggio
        detector.clear()
        detector.addNote(3000L, 9)  // A
        detector.addNote(3500L, 0)  // C
        detector.addNote(4000L, 4)  // E
        assertNotNull(detector.detect(4500L))
    }

    // --- Custom window ---

    @Test
    fun customWindowSizeWorks() {
        val detector = ArpeggioDetector(windowMs = 500L) // very short window
        detector.addNote(1000L, 0)
        detector.addNote(1200L, 4)
        detector.addNote(1400L, 7)
        val result = detector.detect(1400L)
        assertNotNull(result, "All notes within 500ms window")
    }

    @Test
    fun customWindowPrunesOldNotes() {
        val detector = ArpeggioDetector(windowMs = 500L)
        detector.addNote(1000L, 0)  // pruned at time 1600
        detector.addNote(1200L, 4)  // pruned at time 1800
        detector.addNote(1800L, 7)  // only this remains
        assertNull(detector.detect(1800L), "Only 1 note in window")
    }
}
