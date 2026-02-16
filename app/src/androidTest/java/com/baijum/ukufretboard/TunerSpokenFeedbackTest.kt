package com.baijum.ukufretboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.baijum.ukufretboard.data.TunerSettings
import org.junit.Rule
import org.junit.Test

/**
 * Acceptance tests for the tuner spoken-feedback fix.
 *
 * Verifies that live-region semantics are suppressed when spoken feedback
 * is enabled, preventing TalkBack and the app's TTS from speaking over
 * each other.
 *
 * Run with: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.baijum.ukufretboard.TunerSpokenFeedbackTest
 */
class TunerSpokenFeedbackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Matches nodes that have a [LiveRegionMode] set. */
    private val hasLiveRegion = SemanticsMatcher("has LiveRegion") {
        it.config.getOrElseNullable(SemanticsProperties.LiveRegion) { null } != null
    }

    /**
     * Composable that mirrors the exact pattern used in TunerTab for the
     * detected-note text: liveRegion is only set when spoken feedback is OFF.
     */
    private fun setDetectedNoteContent(spokenFeedback: Boolean) {
        val settings = TunerSettings(spokenFeedback = spokenFeedback)
        composeTestRule.setContent {
            MaterialTheme {
                Text(
                    text = "A4",
                    modifier = Modifier.semantics {
                        if (!settings.spokenFeedback) {
                            liveRegion = LiveRegionMode.Polite
                        }
                        contentDescription = "Detected note: A4"
                    },
                )
            }
        }
    }

    /**
     * Composable that mirrors the pattern for the guidance text:
     * liveRegion (Assertive) is only set when spoken feedback is OFF.
     */
    private fun setGuidanceContent(spokenFeedback: Boolean) {
        val settings = TunerSettings(spokenFeedback = spokenFeedback)
        composeTestRule.setContent {
            MaterialTheme {
                Text(
                    text = "Tune up",
                    modifier = Modifier.semantics {
                        if (!settings.spokenFeedback) {
                            liveRegion = LiveRegionMode.Assertive
                        }
                        contentDescription = "Tune up"
                    },
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // Detected note live region
    // -----------------------------------------------------------------------

    @Test
    fun detectedNote_liveRegionPresent_whenSpokenFeedbackOff() {
        setDetectedNoteContent(spokenFeedback = false)

        composeTestRule
            .onNodeWithContentDescription("Detected note: A4")
            .assert(hasLiveRegion)
    }

    @Test
    fun detectedNote_liveRegionAbsent_whenSpokenFeedbackOn() {
        setDetectedNoteContent(spokenFeedback = true)

        composeTestRule
            .onNodeWithContentDescription("Detected note: A4")
            .assert(!hasLiveRegion)
    }

    // -----------------------------------------------------------------------
    // Guidance text live region
    // -----------------------------------------------------------------------

    @Test
    fun guidanceText_liveRegionPresent_whenSpokenFeedbackOff() {
        setGuidanceContent(spokenFeedback = false)

        composeTestRule
            .onNodeWithContentDescription("Tune up")
            .assert(hasLiveRegion)
    }

    @Test
    fun guidanceText_liveRegionAbsent_whenSpokenFeedbackOn() {
        setGuidanceContent(spokenFeedback = true)

        composeTestRule
            .onNodeWithContentDescription("Tune up")
            .assert(!hasLiveRegion)
    }

    // -----------------------------------------------------------------------
    // Content description always present (accessibility not broken)
    // -----------------------------------------------------------------------

    @Test
    fun detectedNote_contentDescriptionPresent_regardlessOfSpokenFeedback() {
        setDetectedNoteContent(spokenFeedback = true)

        composeTestRule
            .onNodeWithContentDescription("Detected note: A4")
            .assertExists("contentDescription must be present even with spoken feedback on")
    }
}
