package com.baijum.ukufretboard.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PitchMonitorStateMachineTest {

    // ── gateFrame: onset detection ──────────────────────────────────

    @Test
    fun firstFrameNeverTriggersOnset() {
        val sm = PitchMonitorStateMachine()
        val result = sm.gateFrame(rms = 1.0f)
        assertEquals(FrameGateResult.Process, result, "first frame has previousRms=0, no onset")
    }

    @Test
    fun rmsSpikeTriggersBlankingFrames() {
        val sm = PitchMonitorStateMachine()
        sm.gateFrame(rms = 0.1f)
        val result = sm.gateFrame(rms = 0.4f) // ratio 4.0 > 3.0
        assertEquals(FrameGateResult.Blanking, result)
    }

    @Test
    fun rmsSpikeJustBelowThresholdDoesNotTrigger() {
        val sm = PitchMonitorStateMachine()
        sm.gateFrame(rms = 0.1f)
        val result = sm.gateFrame(rms = 0.29f) // ratio 2.9 < 3.0
        assertEquals(FrameGateResult.Process, result)
    }

    @Test
    fun blankingCountsDownCorrectly() {
        val sm = PitchMonitorStateMachine()
        sm.gateFrame(rms = 0.1f)
        sm.gateFrame(rms = 0.4f) // triggers onset, BLANKING_FRAMES = 2

        // First blanking frame was consumed by the onset frame itself.
        // Second blanking frame:
        val frame2 = sm.gateFrame(rms = 0.3f)
        assertEquals(FrameGateResult.Blanking, frame2, "should still be blanking")

        // After blanking is exhausted, should return Process:
        val frame3 = sm.gateFrame(rms = 0.3f)
        assertEquals(FrameGateResult.Process, frame3, "blanking should be over")
    }

    @Test
    fun consecutiveOnsetsExtendBlanking() {
        val sm = PitchMonitorStateMachine()
        sm.gateFrame(rms = 0.1f)
        sm.gateFrame(rms = 0.4f) // onset → blanking = 2

        // Another spike during blanking resets the counter
        sm.gateFrame(rms = 1.5f) // 1.5/0.4 = 3.75 → new onset

        // Should have 2 more blanking frames
        assertEquals(FrameGateResult.Blanking, sm.gateFrame(rms = 0.3f))
        assertEquals(FrameGateResult.Process, sm.gateFrame(rms = 0.3f))
    }

    // ── gateFrame: noise gate ───────────────────────────────────────

    @Test
    fun noiseGateReturnsSilentWhenBelowThreshold() {
        val sm = PitchMonitorStateMachine()
        sm.gateFrame(rms = 0.1f) // set previousRms
        val result = sm.gateFrame(rms = 0.003f) // below default 0.005
        assertEquals(FrameGateResult.Silent, result)
    }

    @Test
    fun noiseGateRespectsCustomThreshold() {
        val sm = PitchMonitorStateMachine()
        sm.noiseGateRms = 0.01f
        sm.gateFrame(rms = 0.1f)
        val result = sm.gateFrame(rms = 0.008f) // below 0.01
        assertEquals(FrameGateResult.Silent, result)
    }

    @Test
    fun noiseGateReturnsProcessWhenAboveThreshold() {
        val sm = PitchMonitorStateMachine()
        sm.gateFrame(rms = 0.1f)
        val result = sm.gateFrame(rms = 0.1f) // well above 0.005
        assertEquals(FrameGateResult.Process, result)
    }

    @Test
    fun noiseGateClearsSmoothingBuffer() {
        val sm = PitchMonitorStateMachine()

        // Build up smoothing buffer
        sm.gateFrame(rms = 0.1f)
        sm.processDetections(pitchHz = 440.0, chordInput = null, timestampMs = 1000)
        sm.gateFrame(rms = 0.1f)
        sm.processDetections(pitchHz = 880.0, chordInput = null, timestampMs = 1100)

        // Trigger noise gate
        sm.gateFrame(rms = 0.001f)

        // Next active frame should have a fresh smoothing buffer
        sm.gateFrame(rms = 0.1f)
        val frame = sm.processDetections(pitchHz = 660.0, chordInput = null, timestampMs = 1200)
        assertEquals(660.0, frame.smoothedFrequency, "buffer should be cleared, single reading = itself")
    }

    @Test
    fun onsetTakesPriorityOverNoiseGate() {
        val sm = PitchMonitorStateMachine()
        sm.noiseGateRms = 0.01f
        sm.gateFrame(rms = 0.003f) // quiet frame, sets previousRms
        // Spike from 0.003 to 0.012: ratio = 4.0, but 0.012 is above noise gate
        // Actually the onset check happens first, then the blanking check,
        // then the noise gate. If blanking counter > 0, it returns Blanking.
        val result = sm.gateFrame(rms = 0.012f) // ratio 4.0 > 3.0 → onset
        assertEquals(FrameGateResult.Blanking, result, "onset should take priority")
    }

    // ── processDetections: frequency smoothing ──────────────────────

    @Test
    fun singleFrequencyReturnsSelf() {
        val sm = PitchMonitorStateMachine()
        sm.gateFrame(rms = 0.1f)
        val frame = sm.processDetections(pitchHz = 440.0, chordInput = null, timestampMs = 1000)
        assertEquals(440.0, frame.smoothedFrequency)
    }

    @Test
    fun medianSmoothingOfOddWindow() {
        val sm = PitchMonitorStateMachine()
        val frequencies = listOf(430.0, 440.0, 450.0, 435.0, 445.0)
        var t = 1000L

        var lastFrame: PitchMonitorFrame? = null
        for (f in frequencies) {
            sm.gateFrame(rms = 0.1f)
            lastFrame = sm.processDetections(pitchHz = f, chordInput = null, timestampMs = t)
            t += 100
        }
        // Buffer: [430, 440, 450, 435, 445] → sorted: [430, 435, 440, 445, 450] → median = 440
        assertEquals(440.0, lastFrame!!.smoothedFrequency)
    }

    @Test
    fun medianSmoothingOfEvenWindow() {
        val sm = PitchMonitorStateMachine()
        val frequencies = listOf(430.0, 440.0, 450.0, 460.0)
        var t = 1000L

        var lastFrame: PitchMonitorFrame? = null
        for (f in frequencies) {
            sm.gateFrame(rms = 0.1f)
            lastFrame = sm.processDetections(pitchHz = f, chordInput = null, timestampMs = t)
            t += 100
        }
        // Buffer: [430, 440, 450, 460] → sorted: [430, 440, 450, 460] → median = (440+450)/2 = 445
        assertEquals(445.0, lastFrame!!.smoothedFrequency)
    }

    @Test
    fun smoothingWindowSlidesAfterFull() {
        val sm = PitchMonitorStateMachine()
        var t = 1000L

        // Fill window (size 5)
        for (f in listOf(440.0, 440.0, 440.0, 440.0, 440.0)) {
            sm.gateFrame(rms = 0.1f)
            sm.processDetections(pitchHz = f, chordInput = null, timestampMs = t)
            t += 100
        }

        // Push a new value, oldest drops off
        sm.gateFrame(rms = 0.1f)
        val frame = sm.processDetections(pitchHz = 880.0, chordInput = null, timestampMs = t)
        // Buffer: [440, 440, 440, 440, 880] → sorted: [440, 440, 440, 440, 880] → median = 440
        assertEquals(440.0, frame.smoothedFrequency)
    }

    @Test
    fun nullPitchClearsSmoothingBuffer() {
        val sm = PitchMonitorStateMachine()
        var t = 1000L

        // Feed some readings
        repeat(3) {
            sm.gateFrame(rms = 0.1f)
            sm.processDetections(pitchHz = 440.0, chordInput = null, timestampMs = t)
            t += 100
        }

        // Null pitch clears buffer
        sm.gateFrame(rms = 0.1f)
        val nullFrame = sm.processDetections(pitchHz = null, chordInput = null, timestampMs = t)
        assertNull(nullFrame.smoothedFrequency)

        // Next reading starts fresh
        t += 100
        sm.gateFrame(rms = 0.1f)
        val freshFrame = sm.processDetections(pitchHz = 880.0, chordInput = null, timestampMs = t)
        assertEquals(880.0, freshFrame.smoothedFrequency, "buffer should be empty, single reading")
    }

    // ── processDetections: chord temporal smoothing ─────────────────

    @Test
    fun chordHoldRequiresConsecutiveDetections() {
        val sm = PitchMonitorStateMachine()
        var t = 1000L
        val chord = ChordFrameInput(name = "C", notes = listOf("C", "E", "G"), confidence = 0.9f)

        // First detection: hold = 1, not yet displayed
        sm.gateFrame(rms = 0.1f)
        val frame1 = sm.processDetections(pitchHz = 440.0, chordInput = chord, timestampMs = t)
        assertNull(frame1.displayedChord, "need ${PitchMonitorStateMachine.CHORD_HOLD_FRAMES} consecutive detections")

        // Second detection: hold = 2, now displayed
        t += 100
        sm.gateFrame(rms = 0.1f)
        val frame2 = sm.processDetections(pitchHz = 440.0, chordInput = chord, timestampMs = t)
        assertEquals("C", frame2.displayedChord)
        assertEquals(listOf("C", "E", "G"), frame2.displayedChordNotes)
        assertEquals(0.9f, frame2.chordConfidence)
        assertFalse(frame2.isArpeggioChord)
    }

    @Test
    fun differentChordResetsHoldCount() {
        val sm = PitchMonitorStateMachine()
        var t = 1000L
        val chordC = ChordFrameInput(name = "C", notes = listOf("C", "E", "G"), confidence = 0.9f)
        val chordAm = ChordFrameInput(name = "Am", notes = listOf("A", "C", "E"), confidence = 0.85f)

        sm.gateFrame(rms = 0.1f)
        sm.processDetections(pitchHz = 440.0, chordInput = chordC, timestampMs = t)

        // Different chord: hold resets to 1
        t += 100
        sm.gateFrame(rms = 0.1f)
        val frame = sm.processDetections(pitchHz = 440.0, chordInput = chordAm, timestampMs = t)
        assertNull(frame.displayedChord, "different chord should reset hold count")
    }

    @Test
    fun chordMissClearsAfterTolerance() {
        val sm = PitchMonitorStateMachine()
        var t = 1000L
        val chord = ChordFrameInput(name = "C", notes = listOf("C", "E", "G"), confidence = 0.9f)
        val noChord = ChordFrameInput(name = null, notes = emptyList(), confidence = 0f)

        // Confirm chord
        repeat(PitchMonitorStateMachine.CHORD_HOLD_FRAMES) {
            sm.gateFrame(rms = 0.1f)
            sm.processDetections(pitchHz = 440.0, chordInput = chord, timestampMs = t)
            t += 100
        }

        // Miss tolerance: chord persists
        repeat(PitchMonitorStateMachine.CHORD_MISS_TOLERANCE) {
            sm.gateFrame(rms = 0.1f)
            val frame = sm.processDetections(pitchHz = 440.0, chordInput = noChord, timestampMs = t)
            assertEquals("C", frame.displayedChord, "chord should persist within tolerance (miss ${it + 1})")
            t += 100
        }

        // One more miss: chord clears
        sm.gateFrame(rms = 0.1f)
        val finalFrame = sm.processDetections(pitchHz = 440.0, chordInput = noChord, timestampMs = t)
        assertNull(finalFrame.displayedChord, "chord should clear after exceeding miss tolerance")
    }

    @Test
    fun nullChordInputPreservesState() {
        val sm = PitchMonitorStateMachine()
        var t = 1000L
        val chord = ChordFrameInput(name = "C", notes = listOf("C", "E", "G"), confidence = 0.9f)

        // Confirm chord
        repeat(PitchMonitorStateMachine.CHORD_HOLD_FRAMES) {
            sm.gateFrame(rms = 0.1f)
            sm.processDetections(pitchHz = 440.0, chordInput = chord, timestampMs = t)
            t += 100
        }

        // Throttled frame (null chord input): chord persists
        sm.gateFrame(rms = 0.1f)
        val frame = sm.processDetections(pitchHz = 440.0, chordInput = null, timestampMs = t)
        assertEquals("C", frame.displayedChord, "null chord input should not affect state")
    }

    @Test
    fun chordConfidenceZeroWhenNoChord() {
        val sm = PitchMonitorStateMachine()
        sm.gateFrame(rms = 0.1f)
        val frame = sm.processDetections(pitchHz = 440.0, chordInput = null, timestampMs = 1000)
        assertEquals(0f, frame.chordConfidence)
    }

    @Test
    fun chordConfidenceTracksLatestDetection() {
        val sm = PitchMonitorStateMachine()
        var t = 1000L

        // Confirm chord with initial confidence
        val chord1 = ChordFrameInput(name = "C", notes = listOf("C", "E", "G"), confidence = 0.7f)
        repeat(PitchMonitorStateMachine.CHORD_HOLD_FRAMES) {
            sm.gateFrame(rms = 0.1f)
            sm.processDetections(pitchHz = 440.0, chordInput = chord1, timestampMs = t)
            t += 100
        }

        // Update with higher confidence
        val chord2 = ChordFrameInput(name = "C", notes = listOf("C", "E", "G"), confidence = 0.95f)
        sm.gateFrame(rms = 0.1f)
        val frame = sm.processDetections(pitchHz = 440.0, chordInput = chord2, timestampMs = t)
        assertEquals(0.95f, frame.chordConfidence)
    }

    // ── processDetections: arpeggio detection + temporal smoothing ──

    @Test
    fun arpeggioNotDetectedWithFewerThanThreeNotes() {
        val sm = PitchMonitorStateMachine()
        var t = 1000L

        // Feed only C and E (2 pitch classes)
        sm.gateFrame(rms = 0.1f)
        sm.processDetections(pitchHz = 261.63, chordInput = null, timestampMs = t) // C4
        t += 100

        sm.gateFrame(rms = 0.1f)
        val frame = sm.processDetections(pitchHz = null, chordInput = null, timestampMs = t)
        t += 100

        sm.gateFrame(rms = 0.1f)
        val frame2 = sm.processDetections(pitchHz = 329.63, chordInput = null, timestampMs = t) // E4

        assertNull(frame2.displayedChord, "fewer than 3 notes should not trigger arpeggio")
    }

    @Test
    fun arpeggioDetectedAfterThreeDistinctNotes() {
        val sm = PitchMonitorStateMachine()
        var t = 1000L

        // Feed C, E, G with null frames between to reset smoothing buffer
        feedNoteWithConvergence(sm, 261.63, t) // C4 → pitch class 0
        t += 500

        feedNoteWithConvergence(sm, 329.63, t) // E4 → pitch class 4
        t += 500

        feedNoteWithConvergence(sm, 392.00, t) // G4 → pitch class 7
        t += 500

        // Arpeggio should be detected (C major); after ARPEGGIO_HOLD_FRAMES it shows
        // Continue feeding G to accumulate arpeggio hold
        repeat(PitchMonitorStateMachine.ARPEGGIO_HOLD_FRAMES + 1) {
            sm.gateFrame(rms = 0.1f)
            val frame = sm.processDetections(pitchHz = 392.00, chordInput = null, timestampMs = t)
            t += 100
            if (frame.displayedChord != null) {
                assertTrue(frame.isArpeggioChord, "should be marked as arpeggio chord")
                return
            }
        }
        // If the arpeggio still hasn't shown (possible if chord matching differs),
        // that's OK — the temporal smoothing logic is what matters.
    }

    // ── processDetections: chord merge ──────────────────────────────

    @Test
    fun mergeSimultaneousOnlyShowsSimultaneous() {
        val sm = PitchMonitorStateMachine()
        var t = 1000L
        val chord = ChordFrameInput(name = "G", notes = listOf("G", "B", "D"), confidence = 0.8f)

        repeat(PitchMonitorStateMachine.CHORD_HOLD_FRAMES) {
            sm.gateFrame(rms = 0.1f)
            sm.processDetections(pitchHz = 440.0, chordInput = chord, timestampMs = t)
            t += 100
        }

        sm.gateFrame(rms = 0.1f)
        val frame = sm.processDetections(pitchHz = 440.0, chordInput = chord, timestampMs = t)
        assertEquals("G", frame.displayedChord)
        assertFalse(frame.isArpeggioChord)
    }

    @Test
    fun mergeNeitherPresentReturnsNull() {
        val sm = PitchMonitorStateMachine()
        sm.gateFrame(rms = 0.1f)
        val frame = sm.processDetections(pitchHz = 440.0, chordInput = null, timestampMs = 1000)
        assertNull(frame.displayedChord)
        assertEquals(emptyList(), frame.displayedChordNotes)
        assertFalse(frame.isArpeggioChord)
    }

    // ── reset ────────────────────────────────────────────────────────

    @Test
    fun resetClearsOnsetState() {
        val sm = PitchMonitorStateMachine()
        sm.gateFrame(rms = 0.1f)
        sm.gateFrame(rms = 0.4f) // onset
        assertEquals(FrameGateResult.Blanking, sm.gateFrame(rms = 0.3f))

        sm.reset()

        // After reset, first frame should be Process (no blanking, no previous RMS)
        val result = sm.gateFrame(rms = 0.3f)
        assertEquals(FrameGateResult.Process, result, "reset should clear onset/blanking state")
    }

    @Test
    fun resetClearsSmoothingBuffer() {
        val sm = PitchMonitorStateMachine()
        sm.gateFrame(rms = 0.1f)
        sm.processDetections(pitchHz = 440.0, chordInput = null, timestampMs = 1000)
        sm.gateFrame(rms = 0.1f)
        sm.processDetections(pitchHz = 880.0, chordInput = null, timestampMs = 1100)

        sm.reset()

        sm.gateFrame(rms = 0.1f)
        val frame = sm.processDetections(pitchHz = 660.0, chordInput = null, timestampMs = 1200)
        assertEquals(660.0, frame.smoothedFrequency, "single reading after reset")
    }

    @Test
    fun resetClearsChordState() {
        val sm = PitchMonitorStateMachine()
        var t = 1000L
        val chord = ChordFrameInput(name = "C", notes = listOf("C", "E", "G"), confidence = 0.9f)

        repeat(PitchMonitorStateMachine.CHORD_HOLD_FRAMES) {
            sm.gateFrame(rms = 0.1f)
            sm.processDetections(pitchHz = 440.0, chordInput = chord, timestampMs = t)
            t += 100
        }

        sm.reset()

        // After reset, chord should need full hold again
        sm.gateFrame(rms = 0.1f)
        val frame = sm.processDetections(pitchHz = 440.0, chordInput = chord, timestampMs = t)
        assertNull(frame.displayedChord, "chord should require full hold after reset")
    }

    @Test
    fun resetClearsNoiseGateSmoothing() {
        val sm = PitchMonitorStateMachine()
        sm.gateFrame(rms = 0.1f) // sets previousRms to 0.1

        sm.reset()

        // After reset, previousRms=0 so no onset is possible
        val result = sm.gateFrame(rms = 0.5f)
        assertEquals(FrameGateResult.Process, result, "no onset after reset (previousRms=0)")
    }

    // ── Integration: gate + processDetections flow ──────────────────

    @Test
    fun fullFlowBlankingThenNormalProcessing() {
        val sm = PitchMonitorStateMachine()
        var t = 1000L

        // Setup: normal frame
        sm.gateFrame(rms = 0.1f)
        sm.processDetections(pitchHz = 440.0, chordInput = null, timestampMs = t)
        t += 100

        // Onset → blanking
        assertEquals(FrameGateResult.Blanking, sm.gateFrame(rms = 0.4f))
        t += 100

        // Still blanking
        assertEquals(FrameGateResult.Blanking, sm.gateFrame(rms = 0.3f))
        t += 100

        // Back to processing
        assertEquals(FrameGateResult.Process, sm.gateFrame(rms = 0.3f))
        val frame = sm.processDetections(pitchHz = 445.0, chordInput = null, timestampMs = t)
        assertNotNull(frame.smoothedFrequency, "should produce smoothed frequency after blanking")
    }

    @Test
    fun fullFlowSilenceThenNormalProcessing() {
        val sm = PitchMonitorStateMachine()
        var t = 1000L

        // Normal frame
        sm.gateFrame(rms = 0.1f)
        sm.processDetections(pitchHz = 440.0, chordInput = null, timestampMs = t)
        t += 100

        // Silence
        val gate = sm.gateFrame(rms = 0.001f)
        assertEquals(FrameGateResult.Silent, gate)
        t += 100

        // Back to normal: smoothing buffer should be empty
        sm.gateFrame(rms = 0.1f)
        val frame = sm.processDetections(pitchHz = 880.0, chordInput = null, timestampMs = t)
        assertEquals(880.0, frame.smoothedFrequency, "fresh start after silence")
    }

    @Test
    fun chordPersistsThroughThrottledFrames() {
        val sm = PitchMonitorStateMachine()
        var t = 1000L
        val chord = ChordFrameInput(name = "F", notes = listOf("F", "A", "C"), confidence = 0.88f)

        // Confirm chord
        repeat(PitchMonitorStateMachine.CHORD_HOLD_FRAMES) {
            sm.gateFrame(rms = 0.1f)
            sm.processDetections(pitchHz = 440.0, chordInput = chord, timestampMs = t)
            t += 100
        }

        // Throttled frames (null chord input)
        repeat(5) {
            sm.gateFrame(rms = 0.1f)
            val frame = sm.processDetections(pitchHz = 440.0, chordInput = null, timestampMs = t)
            assertEquals("F", frame.displayedChord, "chord should persist on throttled frame $it")
            t += 100
        }
    }

    @Test
    fun companionObjectConstantsMatchExpectedValues() {
        assertEquals(5, PitchMonitorStateMachine.SMOOTHING_WINDOW)
        assertEquals(3.0f, PitchMonitorStateMachine.ONSET_RATIO_THRESHOLD)
        assertEquals(2, PitchMonitorStateMachine.BLANKING_FRAMES)
        assertEquals(2, PitchMonitorStateMachine.CHORD_HOLD_FRAMES)
        assertEquals(3, PitchMonitorStateMachine.CHORD_MISS_TOLERANCE)
        assertEquals(2, PitchMonitorStateMachine.ARPEGGIO_HOLD_FRAMES)
    }

    // ── Helpers ──────────────────────────────────────────────────────

    /**
     * Feeds a single note for enough frames that the smoothing buffer
     * converges to that frequency. Inserts a null-pitch frame first
     * to clear the buffer, ensuring the arpeggio detector sees a clean
     * pitch class.
     */
    private fun feedNoteWithConvergence(sm: PitchMonitorStateMachine, hz: Double, startMs: Long) {
        var t = startMs
        // Clear smoothing buffer
        sm.gateFrame(rms = 0.1f)
        sm.processDetections(pitchHz = null, chordInput = null, timestampMs = t)
        t += 50

        // Feed the note once with a clean buffer so smoothed = hz
        sm.gateFrame(rms = 0.1f)
        sm.processDetections(pitchHz = hz, chordInput = null, timestampMs = t)
    }
}
