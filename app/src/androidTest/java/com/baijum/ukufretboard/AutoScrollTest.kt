package com.baijum.ukufretboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the song auto-scroll feature.
 *
 * Verifies that auto-scroll starts, stops, and does not immediately cancel
 * due to the programmatic scroll being mistaken for a user gesture.
 *
 * Run with: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.baijum.ukufretboard.AutoScrollTest
 */
class AutoScrollTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Renders a minimal scrollable column with auto-scroll controls that
     * mirror the pattern used in SheetViewer (SongbookTab.kt).
     */
    private fun setAutoScrollContent(): AutoScrollState {
        val state = AutoScrollState()
        composeTestRule.setContent {
            MaterialTheme {
                var autoScrolling by rememberSaveable { mutableStateOf(false) }
                val scrollSpeed by rememberSaveable { mutableStateOf(1f) }
                val scrollState = rememberScrollState()
                val programmaticScroll = remember { mutableStateOf(false) }

                LaunchedEffect(autoScrolling) {
                    state.autoScrolling = autoScrolling
                }

                LaunchedEffect(autoScrolling, scrollSpeed) {
                    if (autoScrolling) {
                        while (autoScrolling) {
                            programmaticScroll.value = true
                            try {
                                scrollState.animateScrollTo(
                                    scrollState.value + scrollSpeed.toInt().coerceAtLeast(1),
                                    animationSpec = androidx.compose.animation.core.tween(
                                        durationMillis = 16,
                                        easing = androidx.compose.animation.core.LinearEasing,
                                    ),
                                )
                            } finally {
                                programmaticScroll.value = false
                            }
                            delay(16L)
                        }
                    }
                }

                LaunchedEffect(scrollState.isScrollInProgress) {
                    if (scrollState.isScrollInProgress && autoScrolling && !programmaticScroll.value) {
                        autoScrolling = false
                    }
                }

                LaunchedEffect(scrollState.value) {
                    state.scrollValue = scrollState.value
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                ) {
                    repeat(100) { i ->
                        Text(
                            text = "Line $i of the song",
                            modifier = Modifier.height(40.dp),
                        )
                    }
                }

                androidx.compose.material3.FloatingActionButton(
                    onClick = { autoScrolling = !autoScrolling },
                    modifier = Modifier.semantics {
                        contentDescription = if (autoScrolling) "Pause scroll" else "Auto-scroll"
                    },
                ) {
                    Text(if (autoScrolling) "⏸" else "▶")
                }
            }
        }
        return state
    }

    @Test
    fun autoScroll_startsScrolling_whenPlayPressed() {
        val state = setAutoScrollContent()
        composeTestRule.waitForIdle()

        val initialScroll = state.scrollValue

        // Disable auto-advance so the infinite scroll loop doesn't make Compose perpetually busy
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithContentDescription("Auto-scroll").performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)

        assertTrue("autoScrolling should be true after pressing play", state.autoScrolling)
        assertTrue(
            "scroll position should advance (was $initialScroll, now ${state.scrollValue})",
            state.scrollValue > initialScroll,
        )
    }

    @Test
    fun autoScroll_stops_whenPausePressed() {
        val state = setAutoScrollContent()
        composeTestRule.waitForIdle()

        // Start auto-scroll with clock control
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithContentDescription("Auto-scroll").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)

        assertTrue("autoScrolling should be true", state.autoScrolling)

        // Pause auto-scroll
        composeTestRule.onNodeWithContentDescription("Pause scroll").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)

        assertFalse("autoScrolling should be false after pressing pause", state.autoScrolling)
    }

    @Test
    fun autoScroll_doesNotImmediatelyCancel() {
        val state = setAutoScrollContent()
        composeTestRule.waitForIdle()

        // Disable auto-advance so the infinite scroll loop doesn't make Compose perpetually busy
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithContentDescription("Auto-scroll").performClick()

        // The original bug caused auto-scroll to cancel within ~50ms.
        // Advance 500ms of frames and verify it's still running.
        composeTestRule.mainClock.advanceTimeBy(500)

        assertTrue(
            "autoScrolling should still be true (was cancelled prematurely)",
            state.autoScrolling,
        )
        assertTrue(
            "scroll position should be > 0 (was ${state.scrollValue})",
            state.scrollValue > 0,
        )
    }

    /** Mutable holder for observing state changes from outside the composition. */
    private class AutoScrollState {
        @Volatile var autoScrolling = false
        @Volatile var scrollValue = 0
    }
}
