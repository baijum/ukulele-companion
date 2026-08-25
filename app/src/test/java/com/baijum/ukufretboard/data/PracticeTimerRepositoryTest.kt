package com.baijum.ukufretboard.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PracticeTimerRepositoryTest {
    private lateinit var repo: PracticeTimerRepository

    @Before
    fun setUp() {
        val context: Application = ApplicationProvider.getApplicationContext()
        repo = PracticeTimerRepository(context)
    }

    @Test
    fun recordSessionConvertsMillisToMinutes() {
        repo.recordSession(25 * 60_000L)
        assertEquals(25, repo.totalMinutes())
        assertEquals(25, repo.todayMinutes())
        assertEquals(25, repo.longestSession())
        assertEquals(1, repo.totalSessions())
    }

    @Test
    fun recordSessionFloorsToAtLeastOneMinute() {
        // A span between 60s and 120s rounds down to 1 minute of integer
        // division; it must still count as a minute, never zero.
        repo.recordSession(90_000L)
        assertEquals(1, repo.totalMinutes())
        assertEquals(1, repo.longestSession())
    }

    @Test
    fun recordSessionCapsAnAbsurdSpanAtTheCeiling() {
        // The pre-fix defect banked ~24h (a rotation/background span). The
        // sanity ceiling must clamp it so it cannot pin longest_session (#601).
        val twentyFourHoursMs = 24 * 60 * 60_000L
        repo.recordSession(twentyFourHoursMs)
        val ceilingMinutes = 12 * 60
        assertEquals(ceilingMinutes, repo.totalMinutes())
        assertEquals(ceilingMinutes, repo.todayMinutes())
        assertEquals(ceilingMinutes, repo.longestSession())
    }

    @Test
    fun recordSessionAccumulatesAcrossSessions() {
        repo.recordSession(10 * 60_000L)
        repo.recordSession(5 * 60_000L)
        assertEquals(15, repo.totalMinutes())
        assertEquals(15, repo.todayMinutes())
        assertEquals(2, repo.totalSessions())
        // Longest tracks the single largest session, not the sum.
        assertEquals(10, repo.longestSession())
    }
}
