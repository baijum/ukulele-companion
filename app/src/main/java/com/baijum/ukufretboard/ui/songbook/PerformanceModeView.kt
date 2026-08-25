package com.baijum.ukufretboard.ui.songbook

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordColorOption
import com.baijum.ukufretboard.data.ChordDisplayStyle
import com.baijum.ukufretboard.domain.ChordSheetFormatter
import com.baijum.ukufretboard.ui.LocalReduceMotion
import com.baijum.ukufretboard.ui.rememberTouchExplorationEnabled
import kotlinx.coroutines.delay

@Composable
internal fun PerformanceModeView(
    displayContent: String,
    textStyle: TextStyle,
    onExit: () -> Unit,
    chordDisplayStyle: ChordDisplayStyle = ChordDisplayStyle.ABOVE,
    chordColor: ChordColorOption = ChordColorOption.THEME,
    onChordTap: (String) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    var autoScrolling by remember { mutableStateOf(false) }
    var scrollSpeed by remember { mutableFloatStateOf(1f) }
    val programmaticScroll = remember { mutableStateOf(false) }
    val touchExploration = rememberTouchExplorationEnabled()
    var controlsHidden by remember { mutableStateOf(false) }
    // A screen-reader user cannot perform the tap-anywhere gesture that brings the
    // controls back, so for them the controls simply stay put.
    val showControls = touchExploration || !controlsHidden
    val reduceMotion = LocalReduceMotion.current

    val sectionLineIndices =
        remember(displayContent) {
            ChordSheetFormatter.extractSections(displayContent).map { it.lineIndex }.toSet()
        }

    LaunchedEffect(autoScrolling, scrollSpeed) {
        if (autoScrolling) {
            // Accumulate the fractional remainder so fractional speeds (e.g. 0.5x,
            // 1.5x) scroll less than one pixel per tick on average instead of being
            // truncated up to a whole pixel. A fresh accumulator resets the remainder
            // on every scrollSpeed change or restart because this effect is keyed on
            // autoScrolling + scrollSpeed.
            val accumulator = AutoScrollAccumulator()
            while (autoScrolling) {
                val delta = accumulator.nextDelta(scrollSpeed)
                if (delta > 0) {
                    programmaticScroll.value = true
                    try {
                        scrollState.animateScrollTo(
                            scrollState.value + delta,
                            animationSpec =
                                androidx.compose.animation.core.tween(
                                    durationMillis = 16,
                                    easing = androidx.compose.animation.core.LinearEasing,
                                ),
                        )
                    } finally {
                        programmaticScroll.value = false
                    }
                    if (scrollState.value >= scrollState.maxValue) {
                        autoScrolling = false
                    }
                } else {
                    // No pixel to move this tick; keep the ~16ms cadence so a
                    // sub-1px/tick speed doesn't busy-loop.
                    delay(16L)
                }
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                // A `clickable` here would merge the whole song into one accessibility
                // node and label it "toggle controls", leaving the lyrics unreadable to
                // TalkBack. `pointerInput` adds no semantics, so every line stays
                // individually traversable. Chord links sit deeper in the tree and
                // consume their taps first, so tapping a chord does not also toggle.
                .pointerInput(Unit) {
                    detectTapGestures { controlsHidden = !controlsHidden }
                },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            displayContent.lines().forEachIndexed { lineIndex, line ->
                if (line.isBlank()) {
                    Spacer(modifier = Modifier.height(textStyle.fontSize.value.dp))
                } else {
                    ChordSheetLine(
                        line = line,
                        isSectionHeading = lineIndex in sectionLineIndices,
                        textStyle = textStyle,
                        chordDisplayStyle = chordDisplayStyle,
                        chordColor = chordColor,
                        onChordTap = onChordTap,
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showControls,
            enter =
                if (reduceMotion) {
                    androidx.compose.animation.EnterTransition.None
                } else {
                    androidx.compose.animation
                        .fadeIn()
                },
            exit =
                if (reduceMotion) {
                    androidx.compose.animation.ExitTransition.None
                } else {
                    androidx.compose.animation
                        .fadeOut()
                },
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val scrollLabel =
                    if (autoScrolling) {
                        stringResource(
                            R.string.performance_scroll_pause,
                        )
                    } else {
                        stringResource(R.string.performance_scroll_start)
                    }
                FilledTonalButton(
                    onClick = { autoScrolling = !autoScrolling },
                ) {
                    Icon(
                        imageVector = if (autoScrolling) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = scrollLabel,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(scrollLabel)
                }
                if (autoScrolling) {
                    val decreaseSpeedDesc = stringResource(R.string.performance_decrease_speed)
                    FilledTonalButton(
                        onClick = { scrollSpeed = (scrollSpeed - 0.5f).coerceAtLeast(0.5f) },
                        modifier = Modifier.semantics { contentDescription = decreaseSpeedDesc },
                    ) { Text("−") }
                    Text(
                        stringResource(R.string.performance_scroll_speed, scrollSpeed.toString()),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    val increaseSpeedDesc = stringResource(R.string.performance_increase_speed)
                    FilledTonalButton(
                        onClick = { scrollSpeed = (scrollSpeed + 0.5f).coerceAtMost(5f) },
                        modifier = Modifier.semantics { contentDescription = increaseSpeedDesc },
                    ) { Text("+") }
                }
                IconButton(onClick = onExit) {
                    Icon(
                        Icons.Filled.FullscreenExit,
                        contentDescription = stringResource(R.string.performance_mode_exit),
                    )
                }
            }
        }
    }
}
