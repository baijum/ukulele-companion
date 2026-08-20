package com.baijum.ukufretboard.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.baijum.ukufretboard.domain.ReviewPromptEligibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * Storage-side tests for [ReviewPromptRepository].
 *
 * The gate arithmetic is covered by `ReviewPromptEligibilityTest` in `:shared`.
 * What is left here is the part only Android can get wrong: which preference key
 * each piece of state is written to, and whether the snapshot handed to the
 * shared rules wires every field to the right gate.
 *
 * The key names are deliberately repeated in this file rather than read from the
 * repository. They are an on-disk contract with every existing install — renaming
 * one silently orphans that install's data, and `dismiss_count` in particular is
 * a legacy name kept on purpose. A test that hard-codes them fails loudly when
 * they change, which is the point.
 */
@RunWith(RobolectricTestRunner::class)
class ReviewPromptRepositoryTest {
    private lateinit var prefs: SharedPreferences
    private lateinit var repo: ReviewPromptRepository

    private val day = 86_400_000L

    @Before
    fun setUp() {
        val context: Application = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences("review_prompt", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repo = ReviewPromptRepository(context)
    }

    /** Seeds a stored state that passes every gate, so a test can break one thing. */
    private fun seedEligible() {
        val now = System.currentTimeMillis()
        prefs
            .edit()
            .putLong("first_launch", now - 30 * day)
            .putStringSet("active_days", (1..ReviewPromptEligibility.MIN_ACTIVE_DAYS).map { "2020-01-0$it" }.toSet())
            .putBoolean("has_reviewed", false)
            .putInt("dismiss_count", 0)
            .putLong("last_prompted", 0L)
            .commit()
    }

    // --- first launch ---

    @Test
    fun initFirstLaunchStampsTheClockOnce() {
        repo.initFirstLaunch()
        val stamped = prefs.getLong("first_launch", 0L)
        assertTrue("expected a first_launch timestamp", stamped > 0L)
    }

    @Test
    fun initFirstLaunchDoesNotOverwriteAnExistingStamp() {
        val original = System.currentTimeMillis() - 100 * day
        prefs.edit().putLong("first_launch", original).commit()

        repo.initFirstLaunch()

        // Overwriting would restart the time-since-install gate on every launch,
        // so the user could never become eligible.
        assertEquals(original, prefs.getLong("first_launch", 0L))
    }

    // --- active days ---

    @Test
    fun recordActiveDayIsIdempotentWithinOneCalendarDay() {
        repo.recordActiveDay()
        repo.recordActiveDay()
        repo.recordActiveDay()

        assertEquals(1, repo.activeDaysCount())
        assertEquals(setOf(LocalDate.now().toString()), prefs.getStringSet("active_days", emptySet()))
    }

    @Test
    fun recordActiveDayStopsWritingOnceTheGateIsSatisfied() {
        val banked = (1..ReviewPromptEligibility.MIN_ACTIVE_DAYS).map { "2020-01-0$it" }.toSet()
        prefs.edit().putStringSet("active_days", banked).commit()

        repo.recordActiveDay()

        // Today is not added: the set is capped so it cannot grow without bound.
        assertEquals(banked, prefs.getStringSet("active_days", emptySet()))
    }

    // --- attempt bookkeeping ---

    @Test
    fun recordPromptShownIncrementsTheCountAndStampsTheClock() {
        repo.recordPromptShown()
        assertEquals(1, repo.promptCount())

        repo.recordPromptShown()
        assertEquals(2, repo.promptCount())
        assertTrue("expected a last_prompted timestamp", prefs.getLong("last_prompted", 0L) > 0L)
    }

    @Test
    fun promptCountReadsTheLegacyDismissCountKey() {
        // Renaming this key would hand every existing install a fresh budget of
        // three prompts, which is exactly what the cap exists to prevent.
        prefs.edit().putInt("dismiss_count", 2).commit()
        assertEquals(2, repo.promptCount())
    }

    @Test
    fun recordReviewedLatchesTheFlag() {
        assertFalse(repo.hasReviewed())
        repo.recordReviewed()
        assertTrue(repo.hasReviewed())
    }

    // --- snapshot wiring ---
    //
    // Each of these breaks exactly one stored value and asserts the decision
    // flips, which is what proves that value reaches the gate it belongs to.

    @Test
    fun eligibleWhenEveryStoredValuePasses() {
        seedEligible()
        assertTrue(repo.isEligible())
    }

    @Test
    fun notEligibleWhenHasReviewedIsStored() {
        seedEligible()
        prefs.edit().putBoolean("has_reviewed", true).commit()
        assertFalse(repo.isEligible())
    }

    @Test
    fun notEligibleWhenStoredPromptCountHitsTheCap() {
        seedEligible()
        prefs.edit().putInt("dismiss_count", ReviewPromptEligibility.MAX_PROMPTS).commit()
        assertFalse(repo.isEligible())
    }

    @Test
    fun notEligibleWhenTooFewActiveDaysAreStored() {
        seedEligible()
        prefs.edit().putStringSet("active_days", setOf("2020-01-01")).commit()
        assertFalse(repo.isEligible())
    }

    @Test
    fun notEligibleWhenFirstLaunchWasNeverStored() {
        seedEligible()
        prefs.edit().remove("first_launch").commit()
        assertFalse(repo.isEligible())
    }

    @Test
    fun notEligibleWhenInstalledTooRecently() {
        seedEligible()
        prefs.edit().putLong("first_launch", System.currentTimeMillis() - day).commit()
        assertFalse(repo.isEligible())
    }

    @Test
    fun notEligibleWhileTheCooldownFromTheStoredPromptIsRunning() {
        seedEligible()
        prefs
            .edit()
            .putInt("dismiss_count", 1)
            .putLong("last_prompted", System.currentTimeMillis() - day)
            .commit()
        assertFalse(repo.isEligible())
    }

    @Test
    fun eligibleAgainOnceTheStoredCooldownHasElapsed() {
        seedEligible()
        val elapsed = (ReviewPromptEligibility.COOLDOWN_DAYS + 1) * day
        prefs
            .edit()
            .putInt("dismiss_count", 1)
            .putLong("last_prompted", System.currentTimeMillis() - elapsed)
            .commit()
        assertTrue(repo.isEligible())
    }
}
