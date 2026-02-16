package com.baijum.ukufretboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.domain.Note
import com.baijum.ukufretboard.viewmodel.FretboardViewModel
import com.baijum.ukufretboard.viewmodel.UkuleleString

/** Default width of each fret cell — large enough for comfortable touch targets. */
internal val CELL_WIDTH = 48.dp

/** Default height of each fret cell — one per string row. */
internal val CELL_HEIGHT = 48.dp

/** Width of the fixed string label column on the left. */
internal val FRETBOARD_LABEL_WIDTH = 36.dp

/** Height of the fret number row at the top. */
private val FRET_NUMBER_HEIGHT = 24.dp

/** Height of the fret marker row (position dots). */
private val MARKER_HEIGHT = 16.dp

/** Size of the dot indicator for fret position markers (5, 7, 10, 12). */
private val MARKER_DOT_SIZE = 6.dp

/** Size of the filled circle for a selected fret position. */
private val SELECTED_DOT_SIZE = 36.dp

/** Size of the outlined circle for an open string indicator. */
private val OPEN_STRING_DOT_SIZE = 28.dp

/**
 * Warm wood-tone background for the fretboard area in light theme.
 * In dark theme, the surfaceVariant token from the color scheme is used instead.
 */
private val FretboardBackgroundLight = Color(0xFFF5E6D3)
private val FretboardBackgroundDark = Color(0xFF3A322D)

/** Fret numbers where traditional single-dot position markers appear. */
private val SINGLE_MARKER_FRETS = setOf(3, 5, 7, 10, 15, 17, 19)

/** Fret numbers where double-dot position markers appear (octave positions). */
private val DOUBLE_MARKER_FRETS = setOf(12)

/**
 * Interactive ukulele fretboard rendered as a horizontally scrollable grid.
 *
 * Layout structure:
 * - Fixed left column: string labels (G, C, E, A)
 * - Scrollable right section: fret numbers, position markers, and fret cells
 *
 * Each cell in the grid corresponds to a string/fret intersection and can be
 * tapped to toggle note selection for chord detection.
 *
 * @param tuning The current ukulele tuning (list of strings with their open pitch classes).
 * @param selections Map of string index to selected fret (null = no selection on that string).
 * @param showNoteNames Whether to display note names inside fret cells.
 * @param onFretTap Callback invoked when a fret cell is tapped, with (stringIndex, fret).
 * @param getNoteAt Function to compute the [Note] at a given string/fret position.
 * @param cellWidth Override the fret cell width (defaults to [CELL_WIDTH]).
 * @param cellHeight Override the fret cell height (defaults to [CELL_HEIGHT]).
 * @param scrollable Whether the fretboard scrolls horizontally (defaults to true).
 *   Set to false in full-screen landscape mode where cells are sized to fit.
 * @param modifier Optional [Modifier] for the root layout.
 */
@Composable
fun FretboardView(
    tuning: List<UkuleleString>,
    selections: Map<Int, Int?>,
    showNoteNames: Boolean,
    onFretTap: (stringIndex: Int, fret: Int) -> Unit,
    getNoteAt: (stringIndex: Int, fret: Int) -> Note,
    leftHanded: Boolean = false,
    scaleNotes: Set<Int> = emptySet(),
    scaleRoot: Int? = null,
    scalePositionFretRange: IntRange? = null,
    capoFret: Int = 0,
    lastFret: Int = FretboardViewModel.LAST_FRET,
    cellWidth: Dp = CELL_WIDTH,
    cellHeight: Dp = CELL_HEIGHT,
    scrollable: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val fretRange = if (leftHanded) {
        (lastFret downTo FretboardViewModel.OPEN_STRING_FRET).toList()
    } else {
        (FretboardViewModel.OPEN_STRING_FRET..lastFret).toList()
    }

    Row(modifier = modifier.padding(start = 4.dp)) {
        // Fixed string labels column (does not scroll)
        Column {
            // Spacer aligned with fret numbers row
            Spacer(modifier = Modifier.height(FRET_NUMBER_HEIGHT))
            // Spacer aligned with marker row
            Spacer(modifier = Modifier.height(MARKER_HEIGHT))
            // One label per string (reversed: A, E, C, G top-to-bottom
            // to match the player's perspective looking down at the fretboard)
            tuning.indices.reversed().forEach { i ->
                Box(
                    modifier = Modifier
                        .width(FRETBOARD_LABEL_WIDTH)
                        .height(cellHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tuning[i].name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Fretboard area — scrollable or fixed depending on mode
        val fretboardModifier = if (scrollable) {
            Modifier.horizontalScroll(scrollState)
        } else {
            Modifier
        }
        Column(
            modifier = fretboardModifier
                .background(
                    if (isSystemInDarkTheme()) FretboardBackgroundDark else FretboardBackgroundLight,
                    RoundedCornerShape(8.dp),
                )
                .padding(end = 4.dp),
        ) {
            // Fret numbers row
            FretNumbersRow(fretRange = fretRange, capoFret = capoFret, cellWidth = cellWidth)

            // Position markers row (dots at frets 5, 7, 10, 12)
            FretMarkersRow(fretRange = fretRange, cellWidth = cellWidth)

            // String rows with fret cells (reversed: A, E, C, G top-to-bottom)
            tuning.indices.reversed().forEach { stringIndex ->
                Row {
                    fretRange.forEach { fret ->
                        val note = getNoteAt(stringIndex, fret)
                        val isSelected = selections[stringIndex] == fret
                        val inScale = note.pitchClass in scaleNotes &&
                            (scalePositionFretRange == null || fret in scalePositionFretRange)
                        val isScaleRoot = scaleRoot != null && note.pitchClass == scaleRoot
                        val blocked = capoFret > 0 && fret in 1..capoFret

                        // Build accessibility description
                        val stringName = tuning[stringIndex].name
                        val cellDesc = buildString {
                            append("$stringName string, ")
                            if (fret == FretboardViewModel.OPEN_STRING_FRET) {
                                append("open")
                            } else {
                                append("fret $fret")
                            }
                            append(", ${note.name}")
                            if (isSelected) append(", selected")
                            if (inScale) append(", in scale")
                            if (isScaleRoot) append(", scale root")
                            if (blocked) append(", blocked by capo")
                        }

                        FretCell(
                            note = note,
                            isSelected = isSelected,
                            isOpenString = fret == FretboardViewModel.OPEN_STRING_FRET,
                            showNoteNames = showNoteNames,
                            onClick = { onFretTap(stringIndex, fret) },
                            isNutOnLeft = !leftHanded,
                            isInScale = inScale,
                            isScaleRoot = isScaleRoot,
                            isCapoFret = capoFret > 0 && fret == capoFret,
                            isBlockedByCapo = blocked,
                            modifier = Modifier
                                .size(cellWidth, cellHeight)
                                .semantics {
                                    contentDescription = cellDesc
                                    role = Role.Button
                                    if (isSelected) selected = true
                                    if (blocked) stateDescription = "Blocked by capo"
                                },
                        )
                    }
                }
            }
        }
    }
}

/** Accent color for the capo fret number badge. */
private val CapoBadgeColor = Color(0xFFD4A24C)

/**
 * Row of fret numbers (0–12) displayed above the fretboard grid.
 * The capo fret number is highlighted with a badge.
 */
@Composable
private fun FretNumbersRow(fretRange: List<Int>, capoFret: Int = 0, cellWidth: Dp = CELL_WIDTH) {
    Row {
        fretRange.forEach { fret ->
            val isCapo = capoFret > 0 && fret == capoFret
            val isBlocked = capoFret > 0 && fret in 1 until capoFret
            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .height(FRET_NUMBER_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                if (isCapo) {
                    Box(
                        modifier = Modifier
                            .background(CapoBadgeColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = fret.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                } else {
                    Text(
                        text = fret.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isBlocked)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Row of position markers — small dots at traditional fretboard positions.
 *
 * Single dots appear at frets 5, 7, and 10.
 * A double dot appears at fret 12 (the octave).
 */
@Composable
private fun FretMarkersRow(fretRange: List<Int>, cellWidth: Dp = CELL_WIDTH) {
    val markerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Row {
        fretRange.forEach { fret ->
            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .height(MARKER_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    fret in SINGLE_MARKER_FRETS -> {
                        Box(
                            modifier = Modifier
                                .size(MARKER_DOT_SIZE)
                                .background(markerColor, CircleShape),
                        )
                    }
                    fret in DOUBLE_MARKER_FRETS -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(MARKER_DOT_SIZE)
                                    .background(markerColor, CircleShape),
                            )
                            Box(
                                modifier = Modifier
                                    .size(MARKER_DOT_SIZE)
                                    .background(markerColor, CircleShape),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single tappable cell on the fretboard grid.
 *
 * Visual states:
 * - **Selected**: large filled circle in primary color, with optional note name.
 * - **Open string (fret 0, unselected)**: outlined circle indicating the open string.
 * - **Unselected fretted position**: subtle note name text (if enabled) or empty.
 *
 * Draws the string (horizontal line) and fret wire (vertical line) behind the dot
 * to create the fretboard grid appearance. The fret-0 right edge is drawn thicker
 * to represent the nut.
 *
 * @param note The [Note] at this string/fret position.
 * @param isSelected Whether this fret is currently selected on its string.
 * @param isOpenString Whether this is fret 0 (open string position).
 * @param showNoteNames Whether to display the note name text.
 * @param onClick Callback invoked when this cell is tapped.
 * @param modifier Optional [Modifier] for sizing.
 */
/** Size of the scale note indicator dot. */
private val SCALE_DOT_SIZE = 20.dp

@Composable
private fun FretCell(
    note: Note,
    isSelected: Boolean,
    isOpenString: Boolean,
    showNoteNames: Boolean,
    onClick: () -> Unit,
    isNutOnLeft: Boolean = true,
    isInScale: Boolean = false,
    isScaleRoot: Boolean = false,
    isCapoFret: Boolean = false,
    isBlockedByCapo: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val outlineColor = MaterialTheme.colorScheme.outline
    val stringColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val fretColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val nutColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val scaleColor = Color(0xFF2196F3).copy(alpha = 0.3f) // light blue
    val scaleRootColor = Color(0xFF1565C0).copy(alpha = 0.5f) // darker blue
    val capoBarColor = CapoBadgeColor
    val blockedOverlay = if (isSystemInDarkTheme()) {
        FretboardBackgroundDark.copy(alpha = 0.45f)
    } else {
        FretboardBackgroundLight.copy(alpha = 0.45f)
    }

    Box(
        modifier = modifier
            .then(
                if (isBlockedByCapo) Modifier else Modifier.clickable(onClick = onClick)
            )
            .drawBehind {
                // Draw string — horizontal line through the vertical center
                drawLine(
                    color = stringColor,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2f,
                )
                if (isOpenString) {
                    // Nut: thick line on the side toward the fretted notes
                    val nutX = if (isNutOnLeft) size.width else 0f
                    drawLine(
                        color = nutColor,
                        start = Offset(nutX, 0f),
                        end = Offset(nutX, size.height),
                        strokeWidth = 4f,
                    )
                } else {
                    // Regular fret wire on right edge
                    drawLine(
                        color = fretColor,
                        start = Offset(size.width, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.5f,
                    )
                }
                // Capo bar — thick golden bar across the cell center
                if (isCapoFret) {
                    val barHeight = 8f
                    drawRoundRect(
                        color = capoBarColor,
                        topLeft = Offset(0f, (size.height - barHeight) / 2),
                        size = Size(size.width, barHeight),
                        cornerRadius = CornerRadius(barHeight / 2),
                    )
                }
                // Dim blocked cells behind the capo
                if (isBlockedByCapo && !isCapoFret) {
                    drawRect(color = blockedOverlay)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Blocked cells show no content (only the string/fret wire and optional capo bar)
        if (!isBlockedByCapo) {
            when {
                isSelected -> {
                    // Filled circle for selected fret position
                    Box(
                        modifier = Modifier
                            .size(SELECTED_DOT_SIZE)
                            .background(primaryColor, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (showNoteNames) {
                            Text(
                                text = note.name,
                                color = onPrimaryColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                isOpenString -> {
                    // Outlined circle for open string
                    Box(
                        modifier = Modifier
                            .size(OPEN_STRING_DOT_SIZE)
                            .border(2.dp, outlineColor, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (showNoteNames) {
                            Text(
                                text = note.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = outlineColor,
                            )
                        }
                    }
                }
                else -> {
                    if (isInScale && !isOpenString) {
                        // Scale overlay dot for unselected positions
                        Box(
                            modifier = Modifier
                                .size(SCALE_DOT_SIZE)
                                .background(
                                    if (isScaleRoot) scaleRootColor else scaleColor,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (showNoteNames) {
                                Text(
                                    text = note.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                )
                            }
                        }
                    } else if (showNoteNames) {
                        // Subtle note name for unselected fretted positions
                        Text(
                            text = note.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = outlineColor.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}
