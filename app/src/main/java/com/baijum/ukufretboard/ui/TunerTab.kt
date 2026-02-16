package com.baijum.ukufretboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baijum.ukufretboard.audio.ToneGenerator
import com.baijum.ukufretboard.data.TunerSettings
import com.baijum.ukufretboard.data.UkuleleTuning
import com.baijum.ukufretboard.viewmodel.TunerViewModel
import com.baijum.ukufretboard.viewmodel.NeuralRuntimeStatus
import com.baijum.ukufretboard.viewmodel.TuningStatus
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Tuner tab composable — the main UI for the chromatic tuner.
 *
 * Layout (top → bottom):
 * 1. Tuning label (e.g. "Standard — G C E A")
 * 2. Detected note name + octave
 * 3. Semicircular needle meter (−50 … +50 cents)
 * 4. Plain-English guidance text
 * 5. String buttons (tap for reference tone)
 * 6. Start / Stop button
 *
 * Wraps content in [RequireMicPermission] so the permission flow is handled
 * transparently before the tuner UI appears.
 */
@Composable
fun TunerTab(
    viewModel: TunerViewModel,
    tuning: UkuleleTuning,
    leftHanded: Boolean,
    soundEnabled: Boolean,
    tunerSettings: TunerSettings = TunerSettings(),
) {
    val context = LocalContext.current

    // Keep ViewModel in sync with settings.
    viewModel.setTuning(tuning)
    viewModel.setTunerSettings(tunerSettings)
    viewModel.setApplicationContext(context)

    // Stop capture when leaving the tab.
    DisposableEffect(Unit) {
        onDispose { viewModel.stopTuning() }
    }

    RequireMicPermission {
        TunerContent(
            viewModel = viewModel,
            tuning = tuning,
            leftHanded = leftHanded,
            soundEnabled = soundEnabled,
            tunerSettings = tunerSettings,
        )
    }
}

// ---------------------------------------------------------------------------
// Content (shown after permission is granted)
// ---------------------------------------------------------------------------

@Composable
private fun TunerContent(
    viewModel: TunerViewModel,
    tuning: UkuleleTuning,
    leftHanded: Boolean,
    soundEnabled: Boolean,
    tunerSettings: TunerSettings = TunerSettings(),
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // --- Auto-start: begin listening when entering the tab ----------------
    if (tunerSettings.autoStart && !state.isListening) {
        LaunchedEffect(Unit) {
            viewModel.startTuning()
        }
    }

    // --- Haptic feedback on string tuned ----------------------------------
    // Track which strings were tuned to detect new completions.
    var previousTunedCount by remember { mutableStateOf(0) }
    val currentTunedCount = state.stringProgress.count { it }
    LaunchedEffect(currentTunedCount) {
        if (currentTunedCount > previousTunedCount && previousTunedCount >= 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previousTunedCount = currentTunedCount
    }

    // --- All strings tuned detection --------------------------------------
    val allTuned by remember {
        derivedStateOf { state.stringProgress.all { it } && state.isListening }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // --- Tuning label ---------------------------------------------------
        val a4Label = if (tunerSettings.a4Reference != TunerSettings.DEFAULT_A4_REFERENCE) {
            " (A4=${"%.1f".format(tunerSettings.a4Reference)} Hz)"
        } else ""
        Text(
            text = "${tuning.label} — ${tuning.stringNames.joinToString(" ")}$a4Label",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            SwiftF0StatusBadge(status = state.neuralRuntimeStatus)
            if (tunerSettings.precisionMode) {
                PrecisionModeBadge()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Detected note --------------------------------------------------
        val noteColor by animateColorAsState(
            targetValue = when (state.tuningStatus) {
                TuningStatus.IN_TUNE -> MaterialTheme.colorScheme.primary
                TuningStatus.CLOSE -> MaterialTheme.colorScheme.tertiary
                TuningStatus.SILENT -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.error
            },
            animationSpec = tween(durationMillis = 200),
            label = "noteColor",
        )

        // --- Confidence → alpha mapping -------------------------------------
        val confidenceAlpha by animateFloatAsState(
            targetValue = if (state.tuningStatus == TuningStatus.SILENT) 1f
            else (1.0f - state.confidence.toFloat() * 2f).coerceIn(0.4f, 1.0f),
            animationSpec = tween(durationMillis = 150),
            label = "confidenceAlpha",
        )

        Text(
            text = state.detectedNote ?: "—",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 64.sp,
            ),
            color = noteColor.copy(alpha = noteColor.alpha * confidenceAlpha),
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Detected note: ${state.detectedNote ?: "none"}"
            },
        )

        Spacer(modifier = Modifier.height(4.dp))

        // --- Needle meter (weighted to fill available space) ----------------
        val animatedCents by animateFloatAsState(
            targetValue = state.displayCentsDeviation.toFloat(),
            animationSpec = tween(durationMillis = 120),
            label = "needleCents",
        )

        val meterColorScheme = MaterialTheme.colorScheme

        val meterDescription = when (state.tuningStatus) {
            TuningStatus.SILENT -> "Tuning meter, no pitch detected"
            TuningStatus.IN_TUNE -> "Tuning meter, in tune"
            TuningStatus.CLOSE -> "Tuning meter, ${abs(state.centsDeviation).roundToInt()} cents ${if (state.centsDeviation < 0) "flat" else "sharp"}, almost in tune"
            TuningStatus.FLAT -> "Tuning meter, ${abs(state.centsDeviation).roundToInt()} cents flat"
            TuningStatus.SHARP -> "Tuning meter, ${abs(state.centsDeviation).roundToInt()} cents sharp"
        }

        val effectiveInTuneCents = if (tunerSettings.precisionMode) {
            TunerViewModel.PRECISION_IN_TUNE_CENTS
        } else {
            TunerViewModel.IN_TUNE_CENTS
        }

        NeedleMeter(
            cents = animatedCents,
            tuningStatus = state.tuningStatus,
            confidenceAlpha = confidenceAlpha,
            inTuneCents = effectiveInTuneCents,
            inTuneColor = meterColorScheme.primary,
            closeColor = meterColorScheme.tertiary,
            offColor = meterColorScheme.error,
            trackColor = meterColorScheme.outlineVariant,
            needleColor = meterColorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clearAndSetSemantics {
                    contentDescription = meterDescription
                },
        )

        // --- Numeric cents display ------------------------------------------
        val centsText = when (state.tuningStatus) {
            TuningStatus.SILENT -> ""
            TuningStatus.IN_TUNE -> "0 ¢"
            else -> {
                val rounded = state.centsDeviation.roundToInt()
                val sign = if (rounded > 0) "+" else ""
                "$sign$rounded ¢"
            }
        }

        Text(
            text = centsText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = noteColor.copy(alpha = noteColor.alpha * confidenceAlpha * 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.height(20.dp),
        )

        Spacer(modifier = Modifier.height(4.dp))

        // --- Guidance text --------------------------------------------------
        val guidanceText = when (state.tuningStatus) {
            TuningStatus.SILENT -> "Play a string…"
            TuningStatus.IN_TUNE -> "In Tune!"
            TuningStatus.CLOSE -> if (state.centsDeviation < 0) "Almost — tune up a tiny bit" else "Almost — tune down a tiny bit"
            TuningStatus.FLAT -> "Tune Up"
            TuningStatus.SHARP -> "Tune Down"
        }

        Text(
            text = guidanceText,
            style = MaterialTheme.typography.titleMedium,
            color = noteColor.copy(alpha = noteColor.alpha * confidenceAlpha),
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Assertive
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- All strings tuned celebration ----------------------------------
        AnimatedVisibility(
            visible = allTuned,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            ) + fadeIn(),
        ) {
            Text(
                text = "All strings tuned!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .semantics {
                        liveRegion = LiveRegionMode.Assertive
                    },
            )
        }

        if (!allTuned) {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // --- String buttons -------------------------------------------------
        val stringOrder = if (leftHanded) {
            tuning.stringNames.indices.reversed().toList()
        } else {
            tuning.stringNames.indices.toList()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            stringOrder.forEach { idx ->
                val isActive = state.targetString?.stringIndex == idx
                        && state.tuningStatus != TuningStatus.SILENT
                val isTuned = state.stringProgress[idx]
                val isAutoAdvanceTarget = tunerSettings.autoAdvance &&
                        state.autoAdvanceTarget == idx

                StringButton(
                    label = tuning.stringNames[idx],
                    isActive = isActive,
                    isTuned = isTuned,
                    isAutoAdvanceTarget = isAutoAdvanceTarget,
                    onClick = {
                        if (soundEnabled) {
                            scope.launch {
                                ToneGenerator.playNote(
                                    pitchClass = tuning.pitchClasses[idx],
                                    octave = tuning.octaves[idx],
                                )
                            }
                        }
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Start / Stop button --------------------------------------------
        if (state.isListening) {
            Button(
                onClick = { viewModel.stopTuning() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.Filled.MicOff, contentDescription = "Stop tuning")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop")
            }
        } else {
            Button(onClick = { viewModel.startTuning() }) {
                Icon(Icons.Filled.Mic, contentDescription = "Start tuning")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Tuning")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SwiftF0StatusBadge(
    status: NeuralRuntimeStatus,
    modifier: Modifier = Modifier,
) {
    val (label, bgColor, fgColor) = when (status) {
        NeuralRuntimeStatus.ACTIVE -> Triple(
            "SwiftF0 Active",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        NeuralRuntimeStatus.FALLBACK -> Triple(
            "SwiftF0 Fallback",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
    }

    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fgColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ---------------------------------------------------------------------------
// Needle meter (Canvas)
// ---------------------------------------------------------------------------

/**
 * A semicircular gauge that shows cents deviation from −50 to +50.
 *
 * The arc spans 180° (π radians). A needle rotates around the centre
 * of the arc's base. The centre zone (±5 cents) is highlighted in
 * [inTuneColor].
 */
@Composable
private fun NeedleMeter(
    cents: Float,
    tuningStatus: TuningStatus,
    confidenceAlpha: Float,
    inTuneCents: Double = TunerViewModel.IN_TUNE_CENTS,
    inTuneColor: Color,
    closeColor: Color,
    offColor: Color,
    trackColor: Color,
    needleColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height          // centre at bottom-centre (semicircle)
        val radius = size.height * 0.85f
        val trackStroke = 6.dp.toPx()

        // --- Background arc -------------------------------------------------
        drawArc(
            color = trackColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = trackStroke, cap = StrokeCap.Round),
        )

        // --- In-tune highlight zone -----------------------------------------
        val highlightSweep = (inTuneCents / 50.0 * 180.0).toFloat()
        drawArc(
            color = inTuneColor.copy(alpha = 0.25f),
            startAngle = 270f - highlightSweep,
            sweepAngle = highlightSweep * 2,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = trackStroke * 2.5f, cap = StrokeCap.Round),
        )

        // --- Tick marks -----------------------------------------------------
        val tickAngles = listOf(-50f, -25f, 0f, 25f, 50f)
        for (tickCents in tickAngles) {
            val angle = PI - (tickCents + 50f) / 100f * PI
            val innerR = radius - 12.dp.toPx()
            val outerR = radius + 12.dp.toPx()
            val isCentre = tickCents == 0f
            drawLine(
                color = if (isCentre) inTuneColor else trackColor,
                start = Offset(
                    cx + (innerR * cos(angle)).toFloat(),
                    cy - (innerR * sin(angle)).toFloat(),
                ),
                end = Offset(
                    cx + (outerR * cos(angle)).toFloat(),
                    cy - (outerR * sin(angle)).toFloat(),
                ),
                strokeWidth = if (isCentre) 3.dp.toPx() else 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // --- Needle ---------------------------------------------------------
        if (tuningStatus != TuningStatus.SILENT) {
            // Map cents (-50..+50) to angle (PI..0).  -50 = left, +50 = right.
            val clampedCents = cents.coerceIn(-50f, 50f)
            val needleAngle = PI - (clampedCents + 50f) / 100f * PI
            val needleLen = radius * 0.92f

            val needleEndX = cx + (needleLen * cos(needleAngle)).toFloat()
            val needleEndY = cy - (needleLen * sin(needleAngle)).toFloat()

            val baseNeedleCol = when (tuningStatus) {
                TuningStatus.IN_TUNE -> inTuneColor
                TuningStatus.CLOSE -> closeColor
                else -> offColor
            }
            // Modulate needle opacity by detection confidence.
            val needleCol = baseNeedleCol.copy(
                alpha = baseNeedleCol.alpha * confidenceAlpha,
            )

            drawLine(
                color = needleCol,
                start = Offset(cx, cy),
                end = Offset(needleEndX, needleEndY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )

            // Pivot dot
            drawCircle(
                color = needleCol,
                radius = 6.dp.toPx(),
                center = Offset(cx, cy),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// String button
// ---------------------------------------------------------------------------

/**
 * A tappable button representing one ukulele string.
 *
 * Shows the string name (e.g. "A") and overlays a checkmark when the
 * string has been successfully tuned.
 */
@Composable
private fun PrecisionModeBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = "Precision Mode",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StringButton(
    label: String,
    isActive: Boolean,
    isTuned: Boolean,
    isAutoAdvanceTarget: Boolean = false,
    onClick: () -> Unit,
) {
    val stateDesc = when {
        isTuned -> "tuned"
        isAutoAdvanceTarget -> "next string to tune"
        isActive -> "active"
        else -> "not tuned"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.semantics {
            contentDescription = "$label string, $stateDesc, tap for reference tone"
            stateDescription = stateDesc
        },
    ) {
        if (isActive) {
            FilledTonalButton(
                onClick = onClick,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
            ) {
                StringButtonContent(label, isTuned)
            }
        } else if (isAutoAdvanceTarget && !isTuned) {
            FilledTonalButton(
                onClick = onClick,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                StringButtonContent(label, isTuned)
            }
        } else {
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
            ) {
                StringButtonContent(label, isTuned)
            }
        }
    }
}

@Composable
private fun StringButtonContent(label: String, isTuned: Boolean) {
    if (isTuned) {
        // Animate the checkmark appearing with a bouncy scale-in.
        val checkScale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "checkScale",
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Icon(
                Icons.Filled.Check,
                contentDescription = "Tuned",
                modifier = Modifier.size((14.dp.value * checkScale).dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    } else {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
