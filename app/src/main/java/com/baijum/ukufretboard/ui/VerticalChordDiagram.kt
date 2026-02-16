package com.baijum.ukufretboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baijum.ukufretboard.domain.ChordVoicing

/** Number of strings on a ukulele. */
private const val STRING_COUNT = 4

/** Minimum number of fret rows to match the standard chord diagram convention. */
private const val MIN_FRET_ROWS = 5

/** Spacing between adjacent strings (horizontal distance). */
private val STRING_SPACING = 26.dp

/** Spacing between adjacent fret lines (vertical distance). */
private val FRET_SPACING = 24.dp

/** Radius of fretted-position dots. */
private val DOT_RADIUS = 10.dp

/** Radius of open-string indicator circles. */
private val OPEN_CIRCLE_RADIUS = 6.dp

/** Thickness of the nut bar at the top when the diagram starts at fret 0. */
private val NUT_THICKNESS = 4.dp

/** Width reserved on the left for the fret-position label (e.g. "3fr"). */
private val POSITION_LABEL_WIDTH = 34.dp

/** Vertical space above the grid reserved for open-string circles. */
private val OPEN_CIRCLE_AREA = 18.dp

/** Horizontal padding on the right side of the canvas beyond the last string. */
private val RIGHT_PADDING = 12.dp

/**
 * A compact, tappable chord diagram card rendered in the traditional **vertical**
 * chord chart style: strings run vertically (G C E A, left-to-right) and frets
 * run horizontally.
 *
 * This composable is a drop-in replacement for [ChordDiagramPreview] with the
 * same parameter signature. It uses [MaterialTheme] colors for full dark-mode
 * support and renders the grid via a Compose [Canvas].
 *
 * @param voicing The [ChordVoicing] to display.
 * @param onClick Callback invoked when the card is tapped.
 * @param onLongClick Optional callback for long-press.
 * @param leftHanded When true, strings are mirrored (A E C G left-to-right).
 * @param inversionLabel Optional label (e.g. "Root", "1st Inv") shown below note names.
 * @param bassStringIndex Optional index (0–3) of the bass note string, drawn in a distinct color.
 * @param commonToneIndices Optional set of string indices highlighted as common tones.
 * @param capoFret Optional capo fret position; draws a horizontal bar across all strings.
 * @param soundingNotes Optional note-name override shown below the diagram (e.g. for capo).
 * @param isFavorite Whether this voicing is in the user's favorites (shows a filled heart).
 * @param onFavoriteClick Optional callback for the favorite heart icon. When null, no icon is shown.
 * @param modifier Optional [Modifier] for layout customization.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalChordDiagram(
    voicing: ChordVoicing,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    leftHanded: Boolean = false,
    inversionLabel: String? = null,
    bassStringIndex: Int? = null,
    commonToneIndices: Set<Int>? = null,
    capoFret: Int? = null,
    soundingNotes: String? = null,
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // Determine the fret range to display
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

    val chordDescription = voicing.toAccessibilityDescription(
        leftHanded = leftHanded,
        inversionLabel = inversionLabel,
        capoFret = capoFret,
        soundingNotes = soundingNotes,
    )

    Card(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics { contentDescription = chordDescription },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // The vertical chord diagram canvas
                VerticalChordCanvas(
                    voicing = voicing,
                    startFret = startFret,
                    fretRowCount = fretRowCount,
                    isAtNut = isAtNut,
                    leftHanded = leftHanded,
                    bassStringIndex = bassStringIndex,
                    commonToneIndices = commonToneIndices,
                    capoFret = capoFret,
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Note names below (show sounding notes if capo is active)
                Text(
                    text = soundingNotes ?: voicing.notes.joinToString(" ") { it?.name ?: "x" },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (soundingNotes != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                // Inversion label
                if (inversionLabel != null) {
                    Text(
                        text = inversionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Favorite heart icon at top-left inside the card
            if (onFavoriteClick != null) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(28.dp),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite
                        else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites"
                        else "Add to favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * Canvas that draws the vertical chord grid: nut, fret lines, string lines,
 * open-string circles, fretted dots (colored by role), and an optional capo bar.
 */
@Composable
private fun VerticalChordCanvas(
    voicing: ChordVoicing,
    startFret: Int,
    fretRowCount: Int,
    isAtNut: Boolean,
    leftHanded: Boolean,
    bassStringIndex: Int?,
    commonToneIndices: Set<Int>?,
    capoFret: Int?,
) {
    // Resolve theme colors in composable scope for use inside Canvas
    val stringColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val fretColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val nutColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val dotColor = MaterialTheme.colorScheme.primary
    val bassDotColor = MaterialTheme.colorScheme.tertiary
    val commonToneColor = MaterialTheme.colorScheme.secondary
    val openColor = MaterialTheme.colorScheme.outline
    val capoBarColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
    val posLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Canvas dimensions
    val canvasWidth = POSITION_LABEL_WIDTH + STRING_SPACING * (STRING_COUNT - 1) + RIGHT_PADDING
    val nutArea = if (isAtNut) NUT_THICKNESS else 0.dp
    val canvasHeight = OPEN_CIRCLE_AREA + nutArea + FRET_SPACING * fretRowCount

    Canvas(
        modifier = Modifier
            .size(width = canvasWidth, height = canvasHeight)
            .clearAndSetSemantics { },
    ) {
        val posLabelPx = POSITION_LABEL_WIDTH.toPx()
        val stringSpacePx = STRING_SPACING.toPx()
        val fretSpacePx = FRET_SPACING.toPx()
        val dotRadiusPx = DOT_RADIUS.toPx()
        val openRadiusPx = OPEN_CIRCLE_RADIUS.toPx()
        val nutPx = NUT_THICKNESS.toPx()
        val openAreaPx = OPEN_CIRCLE_AREA.toPx()

        // Y where the fret grid begins (below open circles and optional nut)
        val gridTop = openAreaPx + if (isAtNut) nutPx else 0f

        // X positions for each visual string slot (left to right)
        val slotX = (0 until STRING_COUNT).map { i -> posLabelPx + i * stringSpacePx }

        // Map logical string index to visual X position (mirrors for left-handed)
        fun stringX(stringIndex: Int): Float {
            val visualIndex = if (leftHanded) STRING_COUNT - 1 - stringIndex else stringIndex
            return slotX[visualIndex]
        }

        // Compute dot center Y for a given fret number
        fun dotY(fret: Int): Float {
            val relFret = fret - startFret
            return if (isAtNut) {
                // At nut: row between fret wire (f-1) and (f) holds fret f
                gridTop + (relFret - 0.5f) * fretSpacePx
            } else {
                // Not at nut: first row holds startFret
                gridTop + (relFret + 0.5f) * fretSpacePx
            }
        }

        // ── 1. Nut (thick horizontal bar at the top of the grid) ──
        if (isAtNut) {
            drawLine(
                color = nutColor,
                start = Offset(slotX.first(), gridTop),
                end = Offset(slotX.last(), gridTop),
                strokeWidth = nutPx,
            )
        }

        // ── 3. Fret lines (horizontal) ──
        for (row in 0..fretRowCount) {
            val y = gridTop + row * fretSpacePx
            drawLine(
                color = fretColor,
                start = Offset(slotX.first(), y),
                end = Offset(slotX.last(), y),
                strokeWidth = 1.5f,
            )
        }

        // ── 4. String lines (vertical) ──
        slotX.forEach { x ->
            drawLine(
                color = stringColor,
                start = Offset(x, gridTop),
                end = Offset(x, gridTop + fretRowCount * fretSpacePx),
                strokeWidth = 1.5f,
            )
        }

        // ── 5. Capo bar (horizontal across all strings at the capo fret) ──
        if (capoFret != null) {
            val relCapo = capoFret - startFret
            val capoVisible = if (isAtNut) relCapo in 1..fretRowCount
            else relCapo in 0 until fretRowCount
            if (capoVisible) {
                val capoY = dotY(capoFret)
                val barHeight = 6.dp.toPx()
                drawRoundRect(
                    color = capoBarColor,
                    topLeft = Offset(
                        slotX.first() - dotRadiusPx,
                        capoY - barHeight / 2,
                    ),
                    size = Size(
                        slotX.last() - slotX.first() + 2 * dotRadiusPx,
                        barHeight,
                    ),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }

        // ── 6. Open string circles, muted "x" marks, and fretted dots ──
        voicing.frets.forEachIndexed { stringIndex, fret ->
            val x = stringX(stringIndex)

            when {
                fret == ChordVoicing.MUTED -> {
                    // Muted string: draw "X" above the grid
                    val centerY = openAreaPx / 2
                    val xSize = openRadiusPx * 0.7f
                    val strokeW = 1.5.dp.toPx()
                    drawLine(
                        color = openColor,
                        start = Offset(x - xSize, centerY - xSize),
                        end = Offset(x + xSize, centerY + xSize),
                        strokeWidth = strokeW,
                    )
                    drawLine(
                        color = openColor,
                        start = Offset(x - xSize, centerY + xSize),
                        end = Offset(x + xSize, centerY - xSize),
                        strokeWidth = strokeW,
                    )
                }
                fret == 0 -> {
                    // Open string circle above the grid
                    val circleColor = if (commonToneIndices?.contains(stringIndex) == true) {
                        commonToneColor
                    } else {
                        openColor
                    }
                    drawCircle(
                        color = circleColor,
                        radius = openRadiusPx,
                        center = Offset(x, openAreaPx / 2),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
                capoFret == null || fret != capoFret -> {
                    // Fretted dot (skip if capo occupies this fret)
                    val y = dotY(fret)
                    val isCommon = commonToneIndices?.contains(stringIndex) == true
                    val isBass = bassStringIndex == stringIndex
                    val fillColor = when {
                        isCommon -> commonToneColor
                        isBass -> bassDotColor
                        else -> dotColor
                    }
                    drawCircle(
                        color = fillColor,
                        radius = dotRadiusPx,
                        center = Offset(x, y),
                    )
                }
            }
        }

        // ── 7. Position label ("3fr") when not at nut ──
        // Drawn last so it renders on top of any dots that might overlap
        if (!isAtNut) {
            drawPositionLabel(
                text = "${startFret}fr",
                x = posLabelPx / 2,
                y = gridTop + fretSpacePx / 2,
                color = posLabelColor,
            )
        }
    }
}

/**
 * Draws a position label (e.g. "3fr") using the native canvas.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPositionLabel(
    text: String,
    x: Float,
    y: Float,
    color: Color,
) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = color.toArgb()
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        drawText(text, x, y + paint.textSize / 3, paint)
    }
}

/**
 * Generates a text description of this chord voicing for screen reader accessibility.
 *
 * Example output: "C major chord: G string open, C string fret 3, E string fret 4, A string fret 3"
 */
fun ChordVoicing.toAccessibilityDescription(
    leftHanded: Boolean = false,
    inversionLabel: String? = null,
    capoFret: Int? = null,
    soundingNotes: String? = null,
): String {
    val stringNames = listOf("G", "C", "E", "A")
    val parts = mutableListOf<String>()

    // Note names or chord name summary
    val notesSummary = soundingNotes
        ?: notes.joinToString(" ") { it?.name ?: "x" }
    parts.add("Chord $notesSummary")

    // Per-string descriptions
    val indices = if (leftHanded) frets.indices.reversed() else frets.indices.toList()
    for (i in indices) {
        val name = stringNames.getOrElse(i) { "String $i" }
        val fret = frets[i]
        val desc = when (fret) {
            ChordVoicing.MUTED -> "$name string muted"
            0 -> "$name string open"
            else -> "$name string fret $fret"
        }
        parts.add(desc)
    }

    if (capoFret != null) {
        parts.add("capo at fret $capoFret")
    }

    if (inversionLabel != null) {
        parts.add(inversionLabel)
    }

    return parts.joinToString(", ")
}
