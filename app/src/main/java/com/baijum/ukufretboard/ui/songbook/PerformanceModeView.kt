package com.baijum.ukufretboard.ui.songbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box

@Composable
internal fun PerformanceModeView(
    displayContent: String,
    textStyle: TextStyle,
    onExit: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var autoScrolling by remember { mutableStateOf(false) }
    var scrollSpeed by remember { mutableFloatStateOf(1f) }
    val programmaticScroll = remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

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
                if (scrollState.value >= scrollState.maxValue) {
                    autoScrolling = false
                }
            }
        }
    }

    val toggleControlsLabel = stringResource(R.string.performance_mode_toggle_controls)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .semantics { contentDescription = toggleControlsLabel }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { showControls = !showControls },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            displayContent.lines().forEach { line ->
                if (line.isBlank()) {
                    Spacer(modifier = Modifier.height(textStyle.fontSize.value.dp))
                } else {
                    Text(
                        text = line,
                        style = textStyle,
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showControls,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val scrollLabel = if (autoScrolling) stringResource(R.string.performance_scroll_pause) else stringResource(R.string.performance_scroll_start)
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
                    ) { Text("\u2212") }
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
                    Icon(Icons.Filled.FullscreenExit, contentDescription = stringResource(R.string.performance_mode_exit))
                }
            }
        }
    }
}
