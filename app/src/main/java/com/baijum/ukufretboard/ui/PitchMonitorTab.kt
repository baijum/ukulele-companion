package com.baijum.ukufretboard.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.viewmodel.PitchMonitorViewModel
import com.baijum.ukufretboard.viewmodel.PitchPoint
import kotlinx.coroutines.android.awaitFrame

// =============================================================================
// Color palette (matching the screenshot: dark background, gold grid, blue trace)
// =============================================================================

/** Dark navy background for the pitch monitor canvas. */
private val CanvasBackground = Color(0xFF1A2030)

/** Muted gold/yellow for standard note grid lines. */
private val GridLineColor = Color(0xFF8B8B3A)

/** Reddish highlight for C-note grid lines (octave markers). */
private val CLineColor = Color(0xFFAA4444)

/** Bright blue for the pitch trace line. */
private val PitchTraceColor = Color(0xFF4499FF)

/** Label text color (matching the gold theme). */
private val LabelColor = Color(0xFFAAAA60)

/** Warm amber glow for active chromagram pitch lanes. */
private val ChromaGlowColor = Color(0xFFFFAA33)

// =============================================================================
// Note range: C3 (MIDI 48) to C6 (MIDI 84) = 36 semitones
// =============================================================================

/** Lowest displayed MIDI note (C3 = 48). */
private const val MIN_MIDI = 48

/** Highest displayed MIDI note (C6 = 84). */
private const val MAX_MIDI = 84

/** Time window visible on the canvas in milliseconds (~8 seconds). */
private const val TIME_WINDOW_MS = 8_000L

/** Width of the left margin reserved for note labels, in pixels. */
private const val LABEL_MARGIN_PX = 100f

// =============================================================================
// Note names for the Y-axis labels (one per semitone from C3 to C6)
// =============================================================================

/**
 * Note name for each MIDI number in the display range.
 * Natural notes get their full name; sharps/flats are left blank in the label
 * column but still have grid lines.
 */
private val NOTE_LABELS = mapOf(
    48 to "C3", 50 to "D3", 52 to "E3", 53 to "F3",
    55 to "G3", 57 to "A3", 59 to "B3",
    60 to "C4", 62 to "D4", 64 to "E4", 65 to "F4",
    67 to "G4", 69 to "A4", 71 to "B4",
    72 to "C5", 74 to "D5", 76 to "E5", 77 to "F5",
    79 to "G5", 81 to "A5", 83 to "B5",
    84 to "C6",
)

/** MIDI numbers that are C notes (used for red highlighting). */
private val C_NOTES = setOf(48, 60, 72, 84)

// =============================================================================
// Top-level composable
// =============================================================================

/**
 * Pitch Monitor tab — real-time scrolling pitch visualization with chord detection.
 *
 * Layout (top → bottom):
 * 1. Detected chord name (large, prominent)
 * 2. Current note name (smaller)
 * 3. Scrolling pitch canvas (takes most of the screen)
 * 4. Start / Stop button
 *
 * Wraps content in [RequireMicPermission] and stops capture on leave.
 */
@Composable
fun PitchMonitorTab(
    viewModel: PitchMonitorViewModel,
) {
    val context = LocalContext.current
    viewModel.setApplicationContext(context)

    // Stop capture when navigating away.
    DisposableEffect(Unit) {
        onDispose { viewModel.stopListening() }
    }

    RequireMicPermission {
        PitchMonitorContent(viewModel = viewModel)
    }
}

// =============================================================================
// Content (shown after permission is granted)
// =============================================================================

@Composable
private fun PitchMonitorContent(
    viewModel: PitchMonitorViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    // Drive continuous recomposition for smooth scrolling when listening.
    var frameTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.isListening) {
        while (state.isListening) {
            awaitFrame()
            frameTime = System.currentTimeMillis()
        }
    }

    val detectedChordDesc = stringResource(
        R.string.pitch_monitor_detected_chord,
        state.detectedChord ?: stringResource(R.string.label_none),
    )
    val recentNotesDesc = stringResource(
        R.string.pitch_monitor_current_note,
        state.recentNotes.lastOrNull() ?: stringResource(R.string.label_none),
    )
    val pitchVizDesc = stringResource(R.string.pitch_monitor_visualization)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // --- Note history (left) + Chord name (right) -----------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: horizontally scrolling recent notes
            val scrollState = rememberScrollState()
            LaunchedEffect(state.recentNotes.size) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = recentNotesDesc
                    },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.recentNotes.forEachIndexed { index, note ->
                    val isLatest = index == state.recentNotes.lastIndex
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isLatest) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (isLatest) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: detected chord name
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedContent(
                    targetState = state.detectedChord,
                    label = "chord",
                ) { chord ->
                    Text(
                        text = chord ?: "—",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (chord != null) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                            contentDescription = detectedChordDesc
                        },
                    )
                }
                if (state.isArpeggioChord && state.detectedChord != null) {
                    Text(
                        text = stringResource(R.string.pitch_monitor_arpeggio_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Scrolling pitch canvas -----------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    color = CanvasBackground,
                    shape = RoundedCornerShape(8.dp),
                ),
        ) {
            PitchCanvas(
                pitchHistory = state.pitchHistory,
                currentTimeMs = frameTime,
                chromaEnergy = state.chromaEnergy,
                modifier = Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {
                        contentDescription = pitchVizDesc
                    },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Start / Stop button --------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            if (state.isListening) {
                Button(
                    onClick = { viewModel.stopListening() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Filled.MicOff, contentDescription = stringResource(R.string.cd_stop_listening))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_stop))
                }
            } else {
                Button(onClick = { viewModel.startListening() }) {
                    Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.cd_start_listening))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pitch_monitor_start))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// =============================================================================
// Scrolling pitch canvas (Canvas composable)
// =============================================================================

/**
 * A scrolling piano-roll style pitch visualization drawn with Compose [Canvas].
 *
 * - Y-axis: MIDI notes from [MIN_MIDI] (bottom) to [MAX_MIDI] (top).
 * - X-axis: Time, with the right edge = [currentTimeMs] and the left edge
 *   = [currentTimeMs] - [TIME_WINDOW_MS].
 * - Grid: Horizontal lines for each semitone (gold for normal, red for C notes).
 * - Trace: Blue line connecting consecutive [PitchPoint]s.
 */
@Composable
private fun PitchCanvas(
    pitchHistory: List<PitchPoint>,
    currentTimeMs: Long,
    chromaEnergy: FloatArray,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val canvasW = size.width
        val canvasH = size.height
        val plotLeft = LABEL_MARGIN_PX
        val plotWidth = canvasW - plotLeft
        val plotTop = 20f
        val plotBottom = canvasH - 16f
        val plotHeight = plotBottom - plotTop

        // --- Draw grid lines and labels -------------------------------------
        drawNoteGrid(
            plotLeft = plotLeft,
            plotWidth = plotWidth,
            plotTop = plotTop,
            plotBottom = plotBottom,
            plotHeight = plotHeight,
        )

        // --- Draw chromagram glow on active pitch lanes ---------------------
        drawChromaGlow(
            chromaEnergy = chromaEnergy,
            plotLeft = plotLeft,
            plotWidth = plotWidth,
            plotTop = plotTop,
            plotBottom = plotBottom,
            plotHeight = plotHeight,
        )

        // --- Draw pitch trace -----------------------------------------------
        drawPitchTrace(
            pitchHistory = pitchHistory,
            currentTimeMs = currentTimeMs,
            plotLeft = plotLeft,
            plotWidth = plotWidth,
            plotTop = plotTop,
            plotHeight = plotHeight,
        )
    }
}

/**
 * Draws the horizontal note grid lines and left-margin labels.
 */
private fun DrawScope.drawNoteGrid(
    plotLeft: Float,
    plotWidth: Float,
    plotTop: Float,
    plotBottom: Float,
    plotHeight: Float,
) {
    val midiRange = MAX_MIDI - MIN_MIDI // 36 semitones

    for (midi in MIN_MIDI..MAX_MIDI) {
        // Y position: higher MIDI = higher on screen (lower Y value)
        val fraction = (midi - MIN_MIDI).toFloat() / midiRange
        val y = plotBottom - fraction * plotHeight

        val isCNote = midi in C_NOTES
        val isNatural = NOTE_LABELS.containsKey(midi)

        // Grid line
        drawLine(
            color = if (isCNote) CLineColor else GridLineColor,
            start = Offset(plotLeft, y),
            end = Offset(plotLeft + plotWidth, y),
            strokeWidth = if (isCNote) 2f else if (isNatural) 1.2f else 0.6f,
        )

        // Label (natural notes only)
        val label = NOTE_LABELS[midi]
        if (label != null) {
            drawContext.canvas.nativeCanvas.drawText(
                label,
                8f,
                y + 5f, // slight vertical offset for centering
                android.graphics.Paint().apply {
                    color = if (isCNote) {
                        android.graphics.Color.argb(255, 170, 68, 68)
                    } else {
                        android.graphics.Color.argb(255, 170, 170, 96)
                    }
                    textSize = 28f
                    isAntiAlias = true
                },
            )
        }
    }
}

/**
 * Draws semi-transparent glowing bands on swimlanes whose pitch class has
 * energy in the chromagram. Intensity is proportional to chromagram energy.
 */
private fun DrawScope.drawChromaGlow(
    chromaEnergy: FloatArray,
    plotLeft: Float,
    plotWidth: Float,
    plotTop: Float,
    plotBottom: Float,
    plotHeight: Float,
) {
    val midiRange = MAX_MIDI - MIN_MIDI
    val bandHeight = plotHeight / midiRange

    for (midi in MIN_MIDI..MAX_MIDI) {
        val pitchClass = midi % 12
        val energy = chromaEnergy.getOrElse(pitchClass) { 0f }
        if (energy <= 0.05f) continue

        val fraction = (midi - MIN_MIDI).toFloat() / midiRange
        val y = plotBottom - fraction * plotHeight
        val alpha = (energy * 2.5f).coerceAtMost(0.4f)

        drawRect(
            color = ChromaGlowColor.copy(alpha = alpha),
            topLeft = Offset(plotLeft, y - bandHeight / 2f),
            size = Size(plotWidth, bandHeight),
        )
    }
}

/**
 * Draws the blue pitch trace line connecting consecutive non-null pitch points.
 */
private fun DrawScope.drawPitchTrace(
    pitchHistory: List<PitchPoint>,
    currentTimeMs: Long,
    plotLeft: Float,
    plotWidth: Float,
    plotTop: Float,
    plotHeight: Float,
) {
    val timeStart = currentTimeMs - TIME_WINDOW_MS
    val midiRange = (MAX_MIDI - MIN_MIDI).toFloat()

    // Filter to points within the visible time window
    val visiblePoints = pitchHistory.filter { it.timestampMs >= timeStart }
    if (visiblePoints.isEmpty()) return

    // Build segments of consecutive non-null points
    val path = Path()
    var pathStarted = false

    for (point in visiblePoints) {
        if (point.midiNote == null) {
            // Break in the trace (silence)
            if (pathStarted) {
                drawPath(
                    path = path,
                    color = PitchTraceColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round),
                )
                path.reset()
                pathStarted = false
            }
            continue
        }
        val midi = point.midiNote

        // Map timestamp → X
        val timeFraction = (point.timestampMs - timeStart).toFloat() / TIME_WINDOW_MS
        val x = plotLeft + timeFraction * plotWidth

        // Map MIDI note → Y (clamped to display range)
        val clampedMidi = midi.coerceIn(MIN_MIDI.toFloat(), MAX_MIDI.toFloat())
        val midiFraction = (clampedMidi - MIN_MIDI) / midiRange
        val y = (plotTop + plotHeight) - midiFraction * plotHeight

        if (!pathStarted) {
            path.moveTo(x, y)
            pathStarted = true
        } else {
            path.lineTo(x, y)
        }
    }

    // Draw the final segment
    if (pathStarted) {
        drawPath(
            path = path,
            color = PitchTraceColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )
    }

    // Draw a bright dot at the current position (last non-null point)
    visiblePoints.lastOrNull { it.midiNote != null }?.let { last ->
        val midi = last.midiNote ?: return@let
        val timeFraction = (last.timestampMs - timeStart).toFloat() / TIME_WINDOW_MS
        val x = plotLeft + timeFraction * plotWidth
        val clampedMidi = midi.coerceIn(MIN_MIDI.toFloat(), MAX_MIDI.toFloat())
        val midiFraction = (clampedMidi - MIN_MIDI) / midiRange
        val y = (plotTop + plotHeight) - midiFraction * plotHeight

        drawCircle(
            color = PitchTraceColor,
            radius = 6f,
            center = Offset(x, y),
        )
    }
}
