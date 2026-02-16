package com.baijum.ukufretboard.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TtsAnnouncementThrottler].
 *
 * These verify that the tuner's spoken feedback does not fire continuously
 * (the bug) while still announcing meaningful changes in note, status,
 * or deviation.
 */
class TtsAnnouncementThrottlerTest {

    private var fakeTimeMs = 10_000L
    private lateinit var throttler: TtsAnnouncementThrottler

    @Before
    fun setUp() {
        fakeTimeMs = 10_000L
        throttler = TtsAnnouncementThrottler(
            minIntervalMs = 2000L,
            inTuneIntervalMs = 3000L,
            centsBucketSize = 5,
            timeProvider = { fakeTimeMs },
        )
    }

    // -----------------------------------------------------------------------
    // Basic announcements
    // -----------------------------------------------------------------------

    @Test
    fun firstAnnouncementIsAlwaysAllowed() {
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.FLAT, -12.0, justTuned = false),
        )
    }

    @Test
    fun justTunedAlwaysSpeaks() {
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.IN_TUNE, 0.0, justTuned = true),
        )
    }

    @Test
    fun silentStatusIsNeverAnnounced() {
        assertFalse(
            throttler.shouldAnnounce("A4", TuningStatus.SILENT, 0.0, justTuned = false),
        )
    }

    // -----------------------------------------------------------------------
    // Duplicate suppression (core bug fix)
    // -----------------------------------------------------------------------

    @Test
    fun duplicateFlatIsSuppressed() {
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.FLAT, -12.0, justTuned = false),
        )
        fakeTimeMs += 3000L
        assertFalse(
            "Same note+status+cents should be suppressed even after interval",
            throttler.shouldAnnounce("A4", TuningStatus.FLAT, -12.0, justTuned = false),
        )
    }

    @Test
    fun duplicateSharpIsSuppressed() {
        assertTrue(
            throttler.shouldAnnounce("E4", TuningStatus.SHARP, 18.0, justTuned = false),
        )
        fakeTimeMs += 3000L
        assertFalse(
            "Same note+status+cents should be suppressed even after interval",
            throttler.shouldAnnounce("E4", TuningStatus.SHARP, 18.0, justTuned = false),
        )
    }

    @Test
    fun duplicateCloseIsSuppressed() {
        assertTrue(
            throttler.shouldAnnounce("C4", TuningStatus.CLOSE, 8.0, justTuned = false),
        )
        fakeTimeMs += 3000L
        assertFalse(
            "Same note+status+cents should be suppressed even after interval",
            throttler.shouldAnnounce("C4", TuningStatus.CLOSE, 8.0, justTuned = false),
        )
    }

    @Test
    fun duplicateInTuneIsSuppressed() {
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.IN_TUNE, 1.0, justTuned = false),
        )
        fakeTimeMs += 5000L
        assertFalse(
            "Same IN_TUNE announcement should be suppressed",
            throttler.shouldAnnounce("A4", TuningStatus.IN_TUNE, 1.0, justTuned = false),
        )
    }

    // -----------------------------------------------------------------------
    // Announcements allowed on meaningful change
    // -----------------------------------------------------------------------

    @Test
    fun differentNoteIsAnnounced() {
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.FLAT, -12.0, justTuned = false),
        )
        fakeTimeMs += 3000L
        assertTrue(
            "Different note should be announced",
            throttler.shouldAnnounce("E4", TuningStatus.FLAT, -12.0, justTuned = false),
        )
    }

    @Test
    fun differentStatusIsAnnounced() {
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.FLAT, -12.0, justTuned = false),
        )
        fakeTimeMs += 3000L
        assertTrue(
            "Different status should be announced",
            throttler.shouldAnnounce("A4", TuningStatus.CLOSE, -8.0, justTuned = false),
        )
    }

    @Test
    fun significantCentsChangeIsAnnounced() {
        // -12 cents → bucket -2, then -3 cents → bucket 0 (different bucket)
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.FLAT, -12.0, justTuned = false),
        )
        fakeTimeMs += 3000L
        assertTrue(
            "Cents shifting to a different bucket should be announced",
            throttler.shouldAnnounce("A4", TuningStatus.FLAT, -3.0, justTuned = false),
        )
    }

    @Test
    fun smallCentsChangeWithinBucketIsSuppressed() {
        // -11 cents → bucket -2, -12 cents → still bucket -2
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.FLAT, -11.0, justTuned = false),
        )
        fakeTimeMs += 3000L
        assertFalse(
            "Cents within same bucket should be suppressed",
            throttler.shouldAnnounce("A4", TuningStatus.FLAT, -12.0, justTuned = false),
        )
    }

    // -----------------------------------------------------------------------
    // Time-based throttling
    // -----------------------------------------------------------------------

    @Test
    fun announcementThrottledWithinMinInterval() {
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.FLAT, -12.0, justTuned = false),
        )
        fakeTimeMs += 1000L // less than 2000ms
        assertFalse(
            "Should be throttled within minIntervalMs even if note changes",
            throttler.shouldAnnounce("E4", TuningStatus.SHARP, 20.0, justTuned = false),
        )
    }

    @Test
    fun announcementAllowedAfterMinInterval() {
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.FLAT, -12.0, justTuned = false),
        )
        fakeTimeMs += 2000L
        assertTrue(
            "Should be allowed after minIntervalMs with different data",
            throttler.shouldAnnounce("E4", TuningStatus.SHARP, 20.0, justTuned = false),
        )
    }

    @Test
    fun inTuneUsesLongerInterval() {
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.IN_TUNE, 1.0, justTuned = false),
        )
        fakeTimeMs += 2500L // past minInterval but within inTuneInterval
        assertFalse(
            "IN_TUNE should use the longer inTuneIntervalMs",
            throttler.shouldAnnounce("A4", TuningStatus.IN_TUNE, -2.0, justTuned = false),
        )
    }

    @Test
    fun inTuneAllowedAfterLongerInterval() {
        // 1.0 cents → bucket 0
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.IN_TUNE, 1.0, justTuned = false),
        )
        fakeTimeMs += 3000L
        // -8.0 cents → bucket -1 (different from 0)
        assertTrue(
            "IN_TUNE should be allowed after inTuneIntervalMs with different bucket",
            throttler.shouldAnnounce("A4", TuningStatus.IN_TUNE, -8.0, justTuned = false),
        )
    }

    // -----------------------------------------------------------------------
    // justTuned bypasses throttle
    // -----------------------------------------------------------------------

    @Test
    fun justTunedBypassesTimeThrottle() {
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.FLAT, -12.0, justTuned = false),
        )
        fakeTimeMs += 100L // well within throttle window
        assertTrue(
            "justTuned should bypass all throttling",
            throttler.shouldAnnounce("A4", TuningStatus.IN_TUNE, 0.0, justTuned = true),
        )
    }

    @Test
    fun justTunedBypassesDuplicateSuppression() {
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.IN_TUNE, 0.0, justTuned = true),
        )
        fakeTimeMs += 100L
        assertTrue(
            "justTuned should bypass duplicate suppression",
            throttler.shouldAnnounce("A4", TuningStatus.IN_TUNE, 0.0, justTuned = true),
        )
    }

    // -----------------------------------------------------------------------
    // Reset
    // -----------------------------------------------------------------------

    @Test
    fun resetClearsState() {
        assertTrue(
            throttler.shouldAnnounce("A4", TuningStatus.FLAT, -12.0, justTuned = false),
        )
        throttler.reset()
        assertTrue(
            "After reset, the same announcement should be allowed again",
            throttler.shouldAnnounce("A4", TuningStatus.FLAT, -12.0, justTuned = false),
        )
    }
}
