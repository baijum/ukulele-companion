package com.baijum.ukufretboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.baijum.ukufretboard.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.domain.ChordInfo
import com.baijum.ukufretboard.domain.ChordVoicing

/** Number of strings on a ukulele. */
private const val STRING_COUNT = 4

/** Minimum number of fret rows to match the standard chord diagram convention. */
private const val MIN_FRET_ROWS = 5

/** Color used for the fretted-position dots (green matching the reference). */
private val DOT_COLOR = Color(0xFF2E7D32)

/** Open-string pitch classes for standard GCEA tuning. */
private val STANDARD_OPEN_PITCH_CLASSES = listOf(7, 0, 4, 9)

/** Number of pitch classes in the chromatic scale. */
private const val PITCH_CLASS_COUNT = 12

/**
 * A shareable chord diagram rendered in the traditional vertical chord chart style.
 *
 * Strings run vertically (G C E A left-to-right), frets run horizontally.
 * Features a thick nut bar, open-string circles, green dots with white finger
 * numbers, fret position indicator, note names, and fret number labels below.
 *
 * This composable is designed for image export/sharing and uses a white background
 * that looks clean on messaging apps like WhatsApp.
 *
 * @param voicing The [ChordVoicing] to render.
 * @param chordName Display name for the chord (e.g., "Am7", "C").
 * @param inversionLabel Optional inversion label (e.g., "1st Inv").
 * @param modifier Optional [Modifier] for layout customization.
 */
@Composable
fun ShareableChordCard(
    voicing: ChordVoicing,
    chordName: String,
    inversionLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    // Determine fret range
    val startFret: Int
    val fretRowCount: Int

    if (voicing.maxFret == 0) {
        startFret = 0
        fretRowCount = MIN_FRET_ROWS
    } else if (voicing.maxFret <= MIN_FRET_ROWS) {
        startFret = 0
        fretRowCount = MIN_FRET_ROWS
    } else {
        startFret = maxOf(1, voicing.minFret)
        fretRowCount = maxOf(MIN_FRET_ROWS, voicing.maxFret - startFret + 1)
    }
    val isAtNut = startFret == 0

    // Compute finger numbers
    val fingering = ChordInfo.suggestFingering(voicing.frets)

    // Compute per-string note names from tuning + fret positions
    val perStringNotes = voicing.frets.mapIndexed { index, fret ->
        if (fret == ChordVoicing.MUTED) "x"
        else {
            val pitchClass = (STANDARD_OPEN_PITCH_CLASSES[index] + fret) % PITCH_CLASS_COUNT
            Notes.pitchClassToName(pitchClass)
        }
    }

    Column(
        modifier = modifier
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Chord name header
        Text(
            text = chordName,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // The chord diagram canvas (includes per-string note names and fret numbers)
        ShareableChordCanvas(
            voicing = voicing,
            fingering = fingering,
            startFret = startFret,
            fretRowCount = fretRowCount,
            isAtNut = isAtNut,
            perStringNotes = perStringNotes,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Inversion label
        if (inversionLabel != null) {
            Text(
                text = inversionLabel,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = Color.Gray,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // App attribution
        Text(
            text = stringResource(R.string.share_diagram_attribution),
            fontSize = 10.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Canvas-based chord diagram drawn in traditional vertical orientation.
 */
@Composable
private fun ShareableChordCanvas(
    voicing: ChordVoicing,
    fingering: List<Int>,
    startFret: Int,
    fretRowCount: Int,
    isAtNut: Boolean,
    perStringNotes: List<String>,
) {
    // Sizing constants (in dp, converted to px inside DrawScope)
    val stringSpacing = 36.dp
    val fretSpacing = 36.dp
    val openCircleRadius = 8.dp
    val dotRadius = 13.dp
    val nutThickness = 5.dp
    val positionLabelWidth = 48.dp

    // Total canvas size (extra space at bottom for note names + fret numbers)
    val canvasWidth = positionLabelWidth + stringSpacing * (STRING_COUNT - 1) + 24.dp
    val openCircleArea = 24.dp
    val nutArea = if (isAtNut) nutThickness else 0.dp
    val bottomLabelArea = 40.dp // space for note names + fret numbers
    val canvasHeight = openCircleArea + nutArea + fretSpacing * fretRowCount + bottomLabelArea

    Canvas(
        modifier = Modifier.size(width = canvasWidth, height = canvasHeight),
    ) {
        val posLabelPx = positionLabelWidth.toPx()
        val stringSpacePx = stringSpacing.toPx()
        val fretSpacePx = fretSpacing.toPx()
        val openRadiusPx = openCircleRadius.toPx()
        val dotRadiusPx = dotRadius.toPx()
        val nutPx = nutThickness.toPx()
        val openAreaPx = openCircleArea.toPx()

        // Y offset where the fret grid starts (below open circles and nut)
        val gridTop = openAreaPx + if (isAtNut) nutPx else 0f

        // X positions for each string
        val stringXPositions = (0 until STRING_COUNT).map { i ->
            posLabelPx + i * stringSpacePx
        }

        // Draw nut (thick bar at the top)
        if (isAtNut) {
            drawLine(
                color = Color.Black,
                start = Offset(stringXPositions.first(), gridTop),
                end = Offset(stringXPositions.last(), gridTop),
                strokeWidth = nutPx,
            )
        }

        // Draw fret lines (horizontal)
        for (row in 0..fretRowCount) {
            val y = gridTop + row * fretSpacePx
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(stringXPositions.first(), y),
                end = Offset(stringXPositions.last(), y),
                strokeWidth = 1.5f,
            )
        }

        // Draw string lines (vertical)
        stringXPositions.forEach { x ->
            drawLine(
                color = Color.Black.copy(alpha = 0.7f),
                start = Offset(x, gridTop),
                end = Offset(x, gridTop + fretRowCount * fretSpacePx),
                strokeWidth = 1.5f,
            )
        }

        // Draw open string circles, muted "x" marks, and fretted dots
        voicing.frets.forEachIndexed { stringIndex, fret ->
            val x = stringXPositions[stringIndex]

            when {
                fret == ChordVoicing.MUTED -> {
                    // Muted string: draw "X" above the grid
                    val centerY = openAreaPx / 2
                    val xSize = openRadiusPx * 0.7f
                    drawLine(
                        color = Color.Black,
                        start = Offset(x - xSize, centerY - xSize),
                        end = Offset(x + xSize, centerY + xSize),
                        strokeWidth = 2f,
                    )
                    drawLine(
                        color = Color.Black,
                        start = Offset(x - xSize, centerY + xSize),
                        end = Offset(x + xSize, centerY - xSize),
                        strokeWidth = 2f,
                    )
                }
                fret == 0 -> {
                    // Open string circle above the nut
                    drawCircle(
                        color = Color.Black,
                        radius = openRadiusPx,
                        center = Offset(x, openAreaPx / 2),
                        style = Stroke(width = 2f),
                    )
                }
                else -> {
                    // Fretted position: draw filled dot with finger number
                    val relFret = fret - startFret
                    val y = if (isAtNut) {
                        gridTop + (relFret - 0.5f) * fretSpacePx
                    } else {
                        gridTop + (relFret + 0.5f) * fretSpacePx
                    }

                    // Green filled dot
                    drawCircle(
                        color = DOT_COLOR,
                        radius = dotRadiusPx,
                        center = Offset(x, y),
                    )

                    // White finger number inside the dot
                    val finger = fingering[stringIndex]
                    if (finger > 0) {
                        drawFingerNumber(x, y, finger, dotRadiusPx)
                    }
                }
            }
        }

        // Draw position indicator (e.g., "7fr") after dots so it renders on top
        if (!isAtNut) {
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 12.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText(
                    "${startFret}fr",
                    posLabelPx / 2,
                    gridTop + fretSpacePx / 2 + paint.textSize / 3,
                    paint,
                )
            }
        }

        // Draw per-string note names and fret numbers below the grid
        val gridBottom = gridTop + fretRowCount * fretSpacePx
        val noteNameY = gridBottom + 14.sp.toPx()
        val fretNumberY = noteNameY + 14.sp.toPx()

        drawContext.canvas.nativeCanvas.apply {
            val notePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = 13.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            val fretPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 12.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }

            stringXPositions.forEachIndexed { index, x ->
                // Note name (e.g., "G", "C", "E", "C") or "x" for muted
                drawText(perStringNotes[index], x, noteNameY, notePaint)
                // Fret number (e.g., "0", "0", "0", "3") or "x" for muted
                val fretLabel = if (voicing.frets[index] == ChordVoicing.MUTED) "x"
                    else "${voicing.frets[index]}"
                drawText(fretLabel, x, fretNumberY, fretPaint)
            }
        }
    }
}

/**
 * Draws a white finger number centered inside a dot.
 */
private fun DrawScope.drawFingerNumber(
    cx: Float,
    cy: Float,
    finger: Int,
    dotRadius: Float,
) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = dotRadius * 1.2f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        // Vertically center the text
        val textBounds = android.graphics.Rect()
        paint.getTextBounds("$finger", 0, 1, textBounds)
        val textY = cy + textBounds.height() / 2f
        drawText("$finger", cx, textY, paint)
    }
}
