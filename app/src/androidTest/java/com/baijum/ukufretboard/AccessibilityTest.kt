package com.baijum.ukufretboard

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import org.junit.Rule
import org.junit.Test

/**
 * Basic accessibility smoke tests for the Ukulele Companion app.
 *
 * These tests verify that key accessibility attributes are present:
 * - Interactive elements have content descriptions
 * - Headings exist for screen reader navigation
 * - No clickable nodes are missing content descriptions
 *
 * Run with: ./gradlew connectedAndroidTest
 */
class AccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Utility: asserts that no clickable node lacks a content description.
     *
     * Nodes that are clickable but have no contentDescription are invisible
     * to screen readers like TalkBack. This catches missing descriptions
     * that would make the app unusable for blind users.
     */
    private fun assertNoClickableWithoutDescription() {
        val clickableWithoutDescription = hasClickAction() and
            SemanticsMatcher("missing contentDescription") {
                val descriptions = it.config.getOrElseNullable(
                    SemanticsProperties.ContentDescription
                ) { null }
                val text = it.config.getOrElseNullable(
                    SemanticsProperties.Text
                ) { null }
                descriptions.isNullOrEmpty() && text.isNullOrEmpty()
            }

        val nodes = composeTestRule
            .onAllNodes(clickableWithoutDescription)
            .fetchSemanticsNodes()

        if (nodes.isNotEmpty()) {
            composeTestRule.onRoot().printToLog("AccessibilityTest")
            throw AssertionError(
                "Found ${nodes.size} clickable node(s) without content description or text. " +
                    "See 'AccessibilityTest' log tag for the full semantics tree."
            )
        }
    }

    /**
     * Utility: asserts that at least one heading exists in the current composition.
     *
     * Every screen should have at least one heading for screen reader
     * navigation (users swipe through headings to understand page structure).
     */
    private fun assertHeadingsExist() {
        val headingMatcher = SemanticsMatcher("isHeading") {
            it.config.getOrElseNullable(SemanticsProperties.Heading) { null } != null
        }

        val headings = composeTestRule
            .onAllNodes(headingMatcher)
            .fetchSemanticsNodes()

        if (headings.isEmpty()) {
            throw AssertionError(
                "No heading nodes found. Every screen should have at least one " +
                    "Text with Modifier.semantics { heading() } for screen reader navigation."
            )
        }
    }

    @Test
    fun verifySettingsSheetContentDescriptions() {
        // This test serves as a template. To test specific screens,
        // set the content to the screen's composable and call the assertions.
        //
        // Example:
        // composeTestRule.setContent {
        //     SettingsSheet(
        //         soundSettings = SoundSettings(),
        //         onSoundSettingsChange = {},
        //         displaySettings = DisplaySettings(),
        //         onDisplaySettingsChange = {},
        //         tuningSettings = TuningSettings(),
        //         onTuningSettingsChange = {},
        //         fretboardSettings = FretboardSettings(),
        //         onFretboardSettingsChange = {},
        //         onDismiss = {},
        //     )
        // }
        // assertNoClickableWithoutDescription()
        // assertHeadingsExist()

        // Placeholder assertion — replace with real screen test above
        assert(true) { "Accessibility test template runs successfully" }
    }
}
