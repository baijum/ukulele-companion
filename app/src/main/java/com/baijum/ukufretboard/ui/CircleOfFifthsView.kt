package com.baijum.ukufretboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.data.KeySignatures
import com.baijum.ukufretboard.data.Notes
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Interactive Circle of Fifths view with key signature details.
 *
 * Displays all 12 major keys in a circular arrangement. Tapping a key
 * shows its key signature, relative minor, and diatonic chords in a
 * detail panel below.
 *
 * @param onChordTapped Optional callback when a diatonic chord is tapped.
 */
@Composable
fun CircleOfFifthsView(
    onChordTapped: ((rootPitchClass: Int, quality: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var selectedKey by remember { mutableStateOf<Int?>(0) } // Start with C selected

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Circle of Fifths",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 4.dp)
                .semantics { heading() },
        )
        Text(
            text = "Tap a key to see its signature and chords. Adjacent keys are closely related.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // Circle diagram
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val circleOrder = KeySignatures.CIRCLE_ORDER
            val selectedKeyName = selectedKey?.let { Notes.enharmonicForKey(it, it) }
            val circleDesc = if (selectedKeyName != null) {
                "Circle of Fifths diagram, $selectedKeyName selected. Tap to select a different key."
            } else {
                "Circle of Fifths diagram. Tap to select a key."
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {
                        contentDescription = circleDesc
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            val dist = sqrt(dx * dx + dy * dy)
                            val outerRadius = size.width * 0.42f
                            val innerRadius = size.width * 0.26f

                            if (dist <= outerRadius) {
                                // Determine which segment was tapped
                                var angle = atan2(dy, dx).toDouble()
                                // Rotate so 0° is at top (subtract 90°)
                                angle += PI / 2
                                if (angle < 0) angle += 2 * PI
                                val segmentIndex = ((angle / (2 * PI)) * 12).toInt() % 12
                                selectedKey = if (dist >= innerRadius) {
                                    // Outer ring — major key
                                    circleOrder[segmentIndex]
                                } else {
                                    // Inner ring — relative minor
                                    val majorPc = circleOrder[segmentIndex]
                                    (majorPc + 9) % 12 // relative minor root
                                }
                            }
                        }
                    },
            ) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val outerRadius = size.width * 0.42f
                val innerRadius = size.width * 0.26f
                val labelOuterRadius = size.width * 0.35f
                val labelInnerRadius = size.width * 0.20f

                // Draw outer ring segments
                drawCircle(
                    color = surfaceColor,
                    radius = outerRadius,
                    center = Offset(centerX, centerY),
                )
                drawCircle(
                    color = onSurfaceVariantColor.copy(alpha = 0.2f),
                    radius = outerRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 2f),
                )

                // Draw inner circle
                drawCircle(
                    color = surfaceColor.copy(alpha = 0.7f),
                    radius = innerRadius,
                    center = Offset(centerX, centerY),
                )
                drawCircle(
                    color = onSurfaceVariantColor.copy(alpha = 0.2f),
                    radius = innerRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.5f),
                )

                // Draw segment lines
                for (i in 0 until 12) {
                    val lineAngle = -PI / 2 + (2 * PI * i / 12) - (PI / 12)
                    val startX = centerX + (innerRadius * cos(lineAngle)).toFloat()
                    val startY = centerY + (innerRadius * sin(lineAngle)).toFloat()
                    val endX = centerX + (outerRadius * cos(lineAngle)).toFloat()
                    val endY = centerY + (outerRadius * sin(lineAngle)).toFloat()
                    drawLine(
                        color = onSurfaceVariantColor.copy(alpha = 0.15f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 1f,
                    )
                }

                // Highlight selected key segment
                selectedKey?.let { selPc ->
                    val selIndex = circleOrder.indexOf(selPc)
                    val relMinor = (selPc + 9) % 12
                    val relMinorIndex = circleOrder.indexOf(relMinor)
                    // Check if user tapped on the minor side
                    val majorIndex = if (selIndex >= 0) selIndex else {
                        // selectedKey is a minor root; find its relative major
                        val relMajor = (selPc + 3) % 12
                        circleOrder.indexOf(relMajor)
                    }
                    if (majorIndex >= 0) {
                        drawSegmentHighlight(
                            centerX, centerY, outerRadius, innerRadius,
                            majorIndex, primaryColor.copy(alpha = 0.25f),
                        )
                    }
                }

                // Draw key labels
                for (i in 0 until 12) {
                    val pc = circleOrder[i]
                    val angle = -PI / 2 + (2 * PI * i / 12)

                    // Major key label (outer) — use key-aware spelling
                    val majorName = Notes.enharmonicForKey(pc, pc)
                    val isSelected = pc == selectedKey || (selectedKey != null && (selectedKey!! + 3) % 12 == pc)
                    drawKeyLabel(
                        centerX, centerY, labelOuterRadius.toDouble(), angle,
                        majorName, onSurfaceColor,
                        isSelected, primaryColor, size.width * 0.045f,
                    )

                    // Minor key label (inner)
                    val minorPc = (pc + 9) % 12
                    val minorName = Notes.enharmonicForKey(minorPc, minorPc, isMinor = true) + "m"
                    val isMinorSelected = minorPc == selectedKey
                    drawKeyLabel(
                        centerX, centerY, labelInnerRadius.toDouble(), angle,
                        minorName, onSurfaceVariantColor,
                        isMinorSelected, secondaryColor, size.width * 0.032f,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detail panel for selected key
        selectedKey?.let { pitchClass ->
            KeyDetailPanel(
                pitchClass = pitchClass,
                onChordTapped = onChordTapped,
            )
        }
    }
}

/**
 * Draws a highlighted segment arc on the circle.
 */
private fun DrawScope.drawSegmentHighlight(
    cx: Float,
    cy: Float,
    outerR: Float,
    innerR: Float,
    segmentIndex: Int,
    color: Color,
) {
    val segAngle = 360f / 12f
    val startAngle = -90f + segmentIndex * segAngle - segAngle / 2f

    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = segAngle,
        useCenter = true,
        topLeft = Offset(cx - outerR, cy - outerR),
        size = androidx.compose.ui.geometry.Size(outerR * 2, outerR * 2),
    )
}

/**
 * Draws a key name label at the given position on the circle.
 */
private fun DrawScope.drawKeyLabel(
    cx: Float,
    cy: Float,
    radius: Double,
    angle: Double,
    text: String,
    color: Color,
    isSelected: Boolean,
    selectedColor: Color,
    textSize: Float,
) {
    val x = cx + (radius * cos(angle)).toFloat()
    val y = cy + (radius * sin(angle)).toFloat()

    drawContext.canvas.nativeCanvas.drawText(
        text,
        x,
        y + textSize / 3f, // vertical center offset
        android.graphics.Paint().apply {
            this.textSize = textSize
            this.textAlign = android.graphics.Paint.Align.CENTER
            this.color = if (isSelected) {
                android.graphics.Color.rgb(
                    (selectedColor.red * 255).toInt(),
                    (selectedColor.green * 255).toInt(),
                    (selectedColor.blue * 255).toInt(),
                )
            } else {
                android.graphics.Color.rgb(
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt(),
                )
            }
            this.isFakeBoldText = isSelected
            this.isAntiAlias = true
        },
    )
}

/**
 * Detail panel showing key signature information and diatonic chords.
 */
@Composable
private fun KeyDetailPanel(
    pitchClass: Int,
    onChordTapped: ((Int, String) -> Unit)?,
) {
    // Check if this pitch class is a major key or minor key
    val keySig = KeySignatures.forKey(pitchClass)

    // If we have a direct major key match
    if (keySig != null) {
        MajorKeyDetail(keySig, onChordTapped)
    } else {
        // This might be selected from the inner ring (minor key)
        // Find its relative major
        val relativeMajor = (pitchClass + 3) % 12
        val majorKeySig = KeySignatures.forKey(relativeMajor)
        if (majorKeySig != null) {
            MinorKeyDetail(pitchClass, majorKeySig, onChordTapped)
        }
    }
}

@Composable
private fun MajorKeyDetail(
    keySig: com.baijum.ukufretboard.data.KeySignature,
    onChordTapped: ((Int, String) -> Unit)?,
) {
    val keyName = Notes.enharmonicForKey(keySig.pitchClass, keySig.pitchClass)
    val relMinorName = Notes.enharmonicForKey(keySig.relativeMinorPitchClass, keySig.relativeMinorPitchClass, isMinor = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(
                text = "$keyName Major",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Key signature
            Text(
                text = "Key Signature",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = KeySignatures.formatSignature(keySig),
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Relative minor
            Text(
                text = "Relative Minor",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${relMinorName} minor",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Diatonic chords
            Text(
                text = "Diatonic Chords",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))

            val diatonicChords = KeySignatures.diatonicChordsForMajor(keySig.pitchClass)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                diatonicChords.forEach { (numeral, chordName) ->
                    SuggestionChip(
                        onClick = {
                            if (onChordTapped != null) {
                                val root = chordName.takeWhile { it.isLetter() || it == '#' || it == 'b' }
                                val quality = chordName.removePrefix(root)
                                val rootPc = Notes.NOTE_NAMES_SHARP.indexOf(root).takeIf { it >= 0 }
                                    ?: Notes.NOTE_NAMES_FLAT.indexOf(root).takeIf { it >= 0 }
                                    ?: return@SuggestionChip
                                onChordTapped(rootPc, quality)
                            }
                        },
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = numeral,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = chordName,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Closely related keys
            val circleIndex = KeySignatures.CIRCLE_ORDER.indexOf(keySig.pitchClass)
            if (circleIndex >= 0) {
                Text(
                    text = "Closely Related Keys",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                val prevKey = KeySignatures.CIRCLE_ORDER[(circleIndex - 1 + 12) % 12]
                val nextKey = KeySignatures.CIRCLE_ORDER[(circleIndex + 1) % 12]
                Text(
                    text = "${Notes.enharmonicForKey(prevKey, prevKey)} major, " +
                        "${Notes.enharmonicForKey(nextKey, nextKey)} major, " +
                        "${relMinorName} minor",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun MinorKeyDetail(
    minorPitchClass: Int,
    majorKeySig: com.baijum.ukufretboard.data.KeySignature,
    onChordTapped: ((Int, String) -> Unit)?,
) {
    val minorName = Notes.enharmonicForKey(minorPitchClass, minorPitchClass, isMinor = true)
    val majorName = Notes.enharmonicForKey(majorKeySig.pitchClass, majorKeySig.pitchClass)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(
                text = "$minorName Minor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Key Signature",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${KeySignatures.formatSignature(majorKeySig)} (same as $majorName major)",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Relative Major",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "$majorName major",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Diatonic chords for natural minor
            Text(
                text = "Diatonic Chords",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))

            val diatonicChords = KeySignatures.diatonicChordsForMinor(minorPitchClass)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                diatonicChords.forEach { (numeral, chordName) ->
                    SuggestionChip(
                        onClick = {
                            if (onChordTapped != null) {
                                val root = chordName.takeWhile { it.isLetter() || it == '#' || it == 'b' }
                                val quality = chordName.removePrefix(root)
                                val rootPc = Notes.NOTE_NAMES_SHARP.indexOf(root).takeIf { it >= 0 }
                                    ?: Notes.NOTE_NAMES_FLAT.indexOf(root).takeIf { it >= 0 }
                                    ?: return@SuggestionChip
                                onChordTapped(rootPc, quality)
                            }
                        },
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = numeral,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = chordName,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Closely related keys
            val (prevKey, nextKey) = KeySignatures.closelyRelatedKeys(majorKeySig.pitchClass)
            Text(
                text = "Closely Related Keys",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${Notes.enharmonicForKey(prevKey, prevKey)} major, " +
                    "${Notes.enharmonicForKey(nextKey, nextKey)} major",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
