package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * A reusable "Tap Tempo" button that detects BPM from rhythmic taps.
 *
 * Users tap the button in time with the desired tempo. After at least 2 taps,
 * the average BPM is calculated from the inter-tap intervals and reported via
 * [onBpmDetected]. Taps older than [resetTimeoutMs] are discarded so that
 * pausing and resuming produces a clean reading.
 *
 * @param onBpmDetected Called with the detected BPM (clamped to 30–300) after each tap.
 * @param modifier Optional [Modifier] for layout.
 * @param resetTimeoutMs Taps older than this are discarded (default 3 seconds).
 */
@Composable
fun TapTempoButton(
    onBpmDetected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    resetTimeoutMs: Long = 3_000L,
) {
    val tapTimes = remember { mutableStateListOf<Long>() }
    var lastDetectedBpm by remember { mutableLongStateOf(0L) }

    // Clear stale taps automatically after timeout
    LaunchedEffect(tapTimes.size) {
        if (tapTimes.isNotEmpty()) {
            delay(resetTimeoutMs)
            tapTimes.clear()
        }
    }

    FilledTonalButton(
        onClick = {
            val now = System.currentTimeMillis()

            // Remove taps older than the timeout
            tapTimes.removeAll { now - it > resetTimeoutMs }
            tapTimes.add(now)

            if (tapTimes.size >= 2) {
                val intervals = tapTimes.zipWithNext { a, b -> b - a }
                val avgIntervalMs = intervals.average()
                val bpm = (60_000.0 / avgIntervalMs)
                    .roundToInt()
                    .coerceIn(30, 300)
                lastDetectedBpm = bpm.toLong()
                onBpmDetected(bpm)
            }
        },
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.tap_tempo_label),
                fontWeight = FontWeight.Bold,
            )
            if (lastDetectedBpm > 0 && tapTimes.size >= 2) {
                Text(
                    text = "$lastDetectedBpm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}
