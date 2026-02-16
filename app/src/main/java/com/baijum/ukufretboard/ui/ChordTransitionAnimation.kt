package com.baijum.ukufretboard.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.baijum.ukufretboard.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baijum.ukufretboard.domain.ChordInfo
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.domain.FingerTransitionCalculator
import com.baijum.ukufretboard.domain.FingerTransitionCalculator.MovementType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated chord transition that shows finger movements between two voicings.
 *
 * Draws a fretboard grid with animated dots that smoothly move from the
 * source voicing positions to the target voicing positions. Finger
 * movements are colour-coded by type.
 *
 * @param fromVoicing The source chord voicing.
 * @param toVoicing The target chord voicing.
 * @param showFingerNumbers Whether to display finger numbers inside dots.
 */
@Composable
fun ChordTransitionAnimation(
    fromVoicing: ChordVoicing,
    toVoicing: ChordVoicing,
    showFingerNumbers: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    // Animation state
    val progress = remember { Animatable(0f) }
    var isAnimating by remember { mutableStateOf(false) }
    var animationSpeed by remember { mutableFloatStateOf(1f) }
    val scope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main) }

    // Step-by-step state
    var currentStep by remember { mutableIntStateOf(-1) } // -1 = show full from, movements.size = show full to

    // Calculate transitions
    val movements = remember(fromVoicing, toVoicing) {
        FingerTransitionCalculator.calculateTransition(fromVoicing.frets, toVoicing.frets)
    }

    val fromFingering = remember(fromVoicing) { ChordInfo.suggestFingering(fromVoicing.frets) }
    val toFingering = remember(toVoicing) { ChordInfo.suggestFingering(toVoicing.frets) }

    // Animation controller
    LaunchedEffect(isAnimating, animationSpeed) {
        if (isAnimating) {
            // Animate from 0 to 1
            progress.snapTo(0f)
            delay((500 / animationSpeed).toLong()) // Hold on from chord
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (1000 / animationSpeed).toInt(),
                    easing = EaseInOutCubic,
                ),
            )
            delay((500 / animationSpeed).toLong()) // Hold on to chord
            isAnimating = false
        }
    }

    // Colours from theme
    val stayColor = MaterialTheme.colorScheme.primary
    val slideColor = MaterialTheme.colorScheme.tertiary
    val moveColor = MaterialTheme.colorScheme.secondary
    val liftColor = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
    val placeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val nutColor = MaterialTheme.colorScheme.onSurface
    val textColor = MaterialTheme.colorScheme.onPrimary

    // Layout constants
    val stringCount = 4
    val minFret = minOf(
        fromVoicing.frets.filter { it > 0 }.minOrNull() ?: 0,
        toVoicing.frets.filter { it > 0 }.minOrNull() ?: 0,
    )
    val maxFret = maxOf(fromVoicing.maxFret, toVoicing.maxFret)
    val startFret = if (maxFret <= 4) 0 else (minFret - 1).coerceAtLeast(0)
    val fretRows = (maxFret - startFret + 1).coerceAtLeast(4)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Animated fretboard canvas
        val density = LocalDensity.current
        val canvasWidth = with(density) { (stringCount * 28 + 30).dp.toPx() }
        val canvasHeight = with(density) { (fretRows * 26 + 30).dp.toPx() }
        val stringSpacing = with(density) { 28.dp.toPx() }
        val fretSpacing = with(density) { 26.dp.toPx() }
        val leftMargin = with(density) { 24.dp.toPx() }
        val topMargin = with(density) { 20.dp.toPx() }
        val dotRadius = with(density) { 10.dp.toPx() }

        Canvas(
            modifier = Modifier
                .size(
                    width = (stringCount * 28 + 30).dp,
                    height = (fretRows * 26 + 30).dp,
                ),
        ) {
            // Draw fret lines
            for (i in 0..fretRows) {
                val y = topMargin + i * fretSpacing
                drawLine(
                    color = gridColor,
                    start = Offset(leftMargin, y),
                    end = Offset(leftMargin + (stringCount - 1) * stringSpacing, y),
                    strokeWidth = if (i == 0 && startFret == 0) 4f else 1f,
                )
            }

            // Draw string lines
            for (i in 0 until stringCount) {
                val x = leftMargin + i * stringSpacing
                drawLine(
                    color = gridColor,
                    start = Offset(x, topMargin),
                    end = Offset(x, topMargin + fretRows * fretSpacing),
                    strokeWidth = 1f,
                )
            }

            // Draw animated finger dots
            val animProgress = progress.value

            for (stringIdx in 0 until stringCount) {
                val fromFret = fromVoicing.frets[stringIdx]
                val toFret = toVoicing.frets[stringIdx]
                val fromFinger = fromFingering[stringIdx]
                val toFinger = toFingering[stringIdx]

                // Determine the movement for this string's finger
                val movement = movements.firstOrNull { m ->
                    (m.from?.stringIndex == stringIdx) || (m.to?.stringIndex == stringIdx)
                }

                val dotColor = when (movement?.type) {
                    MovementType.STAY -> stayColor
                    MovementType.SLIDE -> slideColor
                    MovementType.MOVE -> moveColor
                    MovementType.LIFT -> liftColor
                    MovementType.PLACE -> placeColor
                    else -> stayColor
                }

                // Calculate interpolated position
                val fromX = leftMargin + stringIdx * stringSpacing
                val toX = leftMargin + stringIdx * stringSpacing

                if (fromFret > 0 || toFret > 0) {
                    val fromY = if (fromFret > 0) {
                        topMargin + (fromFret - startFret - 0.5f) * fretSpacing
                    } else {
                        topMargin - dotRadius // above the nut
                    }
                    val toY = if (toFret > 0) {
                        topMargin + (toFret - startFret - 0.5f) * fretSpacing
                    } else {
                        topMargin - dotRadius
                    }

                    val currentX = fromX + (toX - fromX) * animProgress
                    val currentY = fromY + (toY - fromY) * animProgress
                    val currentAlpha = when (movement?.type) {
                        MovementType.LIFT -> 1f - animProgress
                        MovementType.PLACE -> animProgress
                        else -> 1f
                    }

                    if (currentAlpha > 0.1f) {
                        // Draw dot
                        drawCircle(
                            color = dotColor.copy(alpha = currentAlpha),
                            radius = dotRadius,
                            center = Offset(currentX, currentY),
                        )

                        // Draw finger number
                        if (showFingerNumbers) {
                            val finger = if (animProgress < 0.5f) fromFinger else toFinger
                            if (finger > 0) {
                                drawFingerNumber(
                                    textMeasurer = textMeasurer,
                                    finger = finger,
                                    center = Offset(currentX, currentY),
                                    color = textColor,
                                )
                            }
                        }
                    }
                } else if (fromFret == 0 && toFret == 0) {
                    // Both open — draw open string circle
                    drawCircle(
                        color = gridColor,
                        radius = dotRadius * 0.6f,
                        center = Offset(fromX, topMargin - dotRadius),
                        style = Stroke(width = 2f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Movement descriptions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.transition_anim_finger_movements),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (movements.isEmpty()) {
                    Text(
                        text = stringResource(R.string.transition_anim_no_movement),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    movements.forEach { movement ->
                        val color = when (movement.type) {
                            MovementType.STAY -> stayColor
                            MovementType.SLIDE -> slideColor
                            MovementType.MOVE -> moveColor
                            MovementType.LIFT -> liftColor
                            MovementType.PLACE -> placeColor
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(color = color)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = FingerTransitionCalculator.describeMovement(movement),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Speed control
            Text(
                text = stringResource(R.string.transition_anim_speed),
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = animationSpeed,
                onValueChange = { animationSpeed = it },
                valueRange = 0.25f..3f,
                modifier = Modifier.width(120.dp),
            )
            Text(
                text = "${String.format("%.1f", animationSpeed)}x",
                style = MaterialTheme.typography.labelSmall,
            )
        }

        // Play button
        FilledTonalButton(
            onClick = { isAnimating = true },
            enabled = !isAnimating,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.cd_animate_transition),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.transition_anim_animate))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Step-by-step controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (currentStep > -1) currentStep--
                    scope.launch {
                        progress.snapTo(
                            when {
                                currentStep < 0 -> 0f
                                currentStep >= movements.size -> 1f
                                else -> (currentStep + 1).toFloat() / (movements.size + 1)
                            },
                        )
                    }
                },
                enabled = currentStep > -1,
            ) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.cd_prev_step))
            }
            Text(
                text = when {
                    currentStep < 0 -> stringResource(R.string.transition_anim_start_shape)
                    currentStep >= movements.size -> stringResource(R.string.transition_anim_end_shape)
                    else -> stringResource(R.string.transition_anim_step, currentStep + 1, movements.size)
                },
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(100.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(
                onClick = {
                    if (currentStep < movements.size) currentStep++
                    scope.launch {
                        progress.snapTo(
                            when {
                                currentStep < 0 -> 0f
                                currentStep >= movements.size -> 1f
                                else -> (currentStep + 1).toFloat() / (movements.size + 1)
                            },
                        )
                    }
                },
                enabled = currentStep < movements.size,
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.cd_next_step))
            }
        }
    }
}

/**
 * Draws a finger number at the given centre point on the canvas.
 */
private fun DrawScope.drawFingerNumber(
    textMeasurer: TextMeasurer,
    finger: Int,
    center: Offset,
    color: Color,
) {
    val text = finger.toString()
    val style = TextStyle(
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
    )
    val measured = textMeasurer.measure(text, style)
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            center.x - measured.size.width / 2f,
            center.y - measured.size.height / 2f,
        ),
    )
}
