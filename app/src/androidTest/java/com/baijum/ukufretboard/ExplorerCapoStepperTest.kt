package com.baijum.ukufretboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.baijum.ukufretboard.ui.navigation.CapoSelector
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the Explorer's capo stepper (issue #523).
 *
 * The Explorer had its own copy of the stepper that predated the song editor's:
 * it announced nothing after a press, read the value as a bare number, and boxed
 * it in a fixed 32.dp width that clips longer translations of "Off". Both screens
 * now share one [com.baijum.ukufretboard.ui.CapoStepper].
 *
 * Run with: ./gradlew connectedAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.baijum.ukufretboard.ExplorerCapoStepperTest
 */
class ExplorerCapoStepperTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var capo = 0

    private fun str(
        id: Int,
        vararg args: Any,
    ): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.getString(id, *args)
    }

    private fun renderSelector(
        initialCapo: Int = 0,
        lastFret: Int = 12,
    ) {
        capo = initialCapo
        composeTestRule.setContent {
            MaterialTheme {
                var value by remember { mutableIntStateOf(initialCapo) }
                CapoSelector(
                    capoFret = value,
                    lastFret = lastFret,
                    onCapoChange = {
                        value = it
                        capo = it
                    },
                )
            }
        }
    }

    private fun increase() = composeTestRule.onNodeWithContentDescription(str(R.string.cd_increase_capo))

    private fun decrease() = composeTestRule.onNodeWithContentDescription(str(R.string.cd_decrease_capo))

    private fun valueNode(capoFret: Int) =
        composeTestRule.onNodeWithContentDescription(
            if (capoFret == 0) {
                str(R.string.capo_calc_no_capo)
            } else {
                str(R.string.songbook_capo_value, capoFret)
            },
        )

    @Test
    fun theValueIsALiveRegionSoChangesAreSpoken() {
        // The blocking defect: focus stays on the +/- button after a press, so with
        // no live region TalkBack says nothing and the user has to swipe away and
        // back to learn the new value.
        renderSelector(initialCapo = 3)

        val node = valueNode(3).fetchSemanticsNode()

        assertEquals(
            LiveRegionMode.Polite,
            node.config.getOrNull(SemanticsProperties.LiveRegion),
        )
    }

    @Test
    fun theValueAnnouncesItsMeaningNotABareNumber() {
        renderSelector(initialCapo = 3)

        valueNode(3).assertIsDisplayed()
    }

    @Test
    fun zeroAnnouncesNoCapo() {
        renderSelector(initialCapo = 0)

        valueNode(0).assertIsDisplayed()
    }

    @Test
    fun theValueIsNotBoxedIntoAFixedWidth() {
        // 32.dp fitted "Off" and no more; "Désactivé", "Выкл." and any two-digit
        // fret at a large font scale overflowed it.
        renderSelector(initialCapo = 0)

        valueNode(0).assertWidthIsAtLeast(40.dp)
    }

    @Test
    fun steppingUpdatesTheValueAndReportsIt() {
        renderSelector(initialCapo = 3)

        increase().performClick()
        composeTestRule.waitForIdle()
        valueNode(4).assertIsDisplayed()
        assertEquals(4, capo)

        decrease().performClick()
        decrease().performClick()
        composeTestRule.waitForIdle()
        valueNode(2).assertIsDisplayed()
        assertEquals(2, capo)
    }

    @Test
    fun decreaseIsDisabledAtZero() {
        renderSelector(initialCapo = 0)

        decrease().assertIsNotEnabled()
        increase().assertIsEnabled()
    }

    @Test
    fun increaseIsDisabledAtTheLastFret() {
        renderSelector(initialCapo = 5, lastFret = 5)

        increase().assertIsNotEnabled()
        decrease().assertIsEnabled()
    }

    @Test
    fun theStepperRespectsTheCallersFretBound() {
        // The Explorer passes the user's own last-fret setting; the song editor caps
        // at 12. A shared component has to honour whichever it is given.
        renderSelector(initialCapo = 6, lastFret = 7)

        increase().performClick()
        composeTestRule.waitForIdle()

        assertEquals(7, capo)
        increase().assertIsNotEnabled()
    }
}
