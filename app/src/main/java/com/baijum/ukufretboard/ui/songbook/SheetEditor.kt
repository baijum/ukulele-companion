package com.baijum.ukufretboard.ui.songbook

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordParser
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.ui.CapoStepper
import com.baijum.ukufretboard.ui.StringListSaver

/** Highest capo position offered by the editor stepper; matches the iOS 0...12 range. */
private const val MAX_CAPO_FRET = 12

/**
 * Editor for creating/editing a chord sheet with preview and chord insertion.
 */
@Composable
internal fun SheetEditor(
    sheet: ChordSheet?,
    allLabels: Set<String>,
    onSave: (
        title: String,
        artist: String,
        content: String,
        key: String,
        capo: Int,
        strumPatternName: String,
        labels: List<String>,
    ) -> Unit,
    onCancel: () -> Unit,
) {
    var title by rememberSaveable(sheet?.id) { mutableStateOf(sheet?.title ?: "") }
    var artist by rememberSaveable(sheet?.id) { mutableStateOf(sheet?.artist ?: "") }
    var content by rememberSaveable(sheet?.id) { mutableStateOf(sheet?.content ?: "") }
    var key by rememberSaveable(sheet?.id) { mutableStateOf(sheet?.key ?: "") }
    var capo by rememberSaveable(sheet?.id) { mutableIntStateOf(sheet?.capo ?: 0) }
    var strumPatternName by rememberSaveable(sheet?.id) { mutableStateOf(sheet?.strumPatternName ?: "") }
    // List<String> is not auto-saveable; use the reusable StringListSaver so unsaved
    // label edits survive activity recreation (rotation, font-size change, #573).
    var labels by rememberSaveable(sheet?.id, stateSaver = StringListSaver) {
        mutableStateOf(sheet?.labels ?: emptyList())
    }
    var showPreview by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasChanges =
        title != (sheet?.title ?: "") ||
            artist != (sheet?.artist ?: "") ||
            content != (sheet?.content ?: "") ||
            key != (sheet?.key ?: "") ||
            capo != (sheet?.capo ?: 0) ||
            strumPatternName != (sheet?.strumPatternName ?: "") ||
            labels != (sheet?.labels ?: emptyList<String>())

    val handleCancel = {
        if (hasChanges) {
            showDiscardDialog = true
        } else {
            onCancel()
        }
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDismiss = { showDiscardDialog = false },
            onDiscard = {
                showDiscardDialog = false
                onCancel()
            },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.songbook_field_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text(stringResource(R.string.songbook_field_artist)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        KeyAndCapoRow(
            key = key,
            capo = capo,
            onKeyChange = { key = it },
            onCapoChange = { capo = it },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Strum pattern picker
        StrumPatternRow(
            patternName = strumPatternName,
            onPatternChange = { strumPatternName = it },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Label editor
        LabelEditorSection(
            labels = labels,
            allLabels = allLabels,
            onLabelsChange = { labels = it },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Chord count and insert helper
        val detectedChords = remember(content) { ChordParser.extractChords(content) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.songbook_field_lyrics_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (detectedChords.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.songbook_chords_detected, detectedChords.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Chord insertion helper row
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val commonChords = listOf("C", "G", "Am", "F", "Em", "Dm", "D", "A", "E", "Bm")
            commonChords.forEach { chord ->
                val insertDesc = stringResource(R.string.songbook_insert_chord)
                val insertChordDesc = stringResource(R.string.cd_insert_chord, insertDesc, chord)
                FilterChip(
                    selected = false,
                    onClick = { content += "[$chord]" },
                    label = { Text(chord) },
                    modifier =
                        Modifier.clearAndSetSemantics {
                            contentDescription = insertChordDesc
                        },
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Edit / Preview toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            FilterChip(
                selected = !showPreview,
                onClick = { showPreview = false },
                label = { Text(stringResource(R.string.songbook_editor_edit)) },
            )
            Spacer(modifier = Modifier.width(4.dp))
            FilterChip(
                selected = showPreview,
                onClick = { showPreview = true },
                label = { Text(stringResource(R.string.songbook_editor_preview)) },
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (showPreview) {
            // Live preview of chords above lyrics
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
            ) {
                content.lines().forEach { line ->
                    val segments = ChordParser.parseLine(line)
                    if (segments.isEmpty()) {
                        Text(
                            text = " ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                    } else {
                        val chordColor = MaterialTheme.colorScheme.primary
                        val chordPositions = mutableListOf<Pair<Int, String>>()
                        val lyricLine = StringBuilder()
                        var hasChords = false

                        segments.forEach { segment ->
                            when (segment) {
                                is ChordParser.TextSegment.PlainText -> {
                                    lyricLine.append(segment.text)
                                }

                                is ChordParser.TextSegment.Chord -> {
                                    hasChords = true
                                    chordPositions.add(lyricLine.length to segment.name)
                                }
                            }
                        }

                        if (hasChords) {
                            val chordAnnotated =
                                buildAnnotatedString {
                                    var cursor = 0
                                    chordPositions.forEach { (pos, name) ->
                                        if (pos > cursor) {
                                            append(" ".repeat(pos - cursor))
                                            cursor = pos
                                        }
                                        withStyle(
                                            SpanStyle(
                                                color = chordColor,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        ) {
                                            append(name)
                                        }
                                        cursor += name.length
                                    }
                                }

                            Text(
                                text = chordAnnotated,
                                style =
                                    MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                    ),
                            )
                        }

                        Text(
                            text = lyricLine.toString(),
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.songbook_field_lyrics)) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { handleCancel() }) {
                Text(stringResource(R.string.dialog_cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { onSave(title, artist, content, key, capo, strumPatternName, labels) },
                enabled = title.isNotBlank(),
            ) {
                Text(stringResource(R.string.dialog_save))
            }
        }
    }
}

/**
 * Key text field plus capo stepper, mirroring the fields the iOS editor already has.
 *
 * The key is free text rather than a picker: the ChordPro `{key: ...}` directive is
 * free-form, so an imported "Bb Mixolydian" has to survive a round-trip through the
 * editor unchanged. A fixed picker would silently drop anything not on its list —
 * the same class of bug this screen is being fixed for.
 */
@Composable
private fun KeyAndCapoRow(
    key: String,
    capo: Int,
    onKeyChange: (String) -> Unit,
    onCapoChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = key,
            onValueChange = onKeyChange,
            label = { Text(stringResource(R.string.songbook_field_key)) },
            placeholder = { Text(stringResource(R.string.songbook_field_key_hint)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = stringResource(R.string.label_capo),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        CapoStepper(
            capo = capo,
            onCapoChange = onCapoChange,
            maxFret = MAX_CAPO_FRET,
        )
    }
}

/**
 * Label editor used inside the SheetEditor with InputChips and auto-suggest.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelEditorSection(
    labels: List<String>,
    allLabels: Set<String>,
    onLabelsChange: (List<String>) -> Unit,
) {
    var labelInput by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }

    val suggestions =
        remember(labelInput, allLabels, labels) {
            if (labelInput.isBlank()) {
                emptyList()
            } else {
                val query = labelInput.trim().lowercase()
                (allLabels - labels.toSet())
                    .filter { it.lowercase().contains(query) }
                    .take(5)
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.songbook_labels),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        if (labels.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                labels.forEach { label ->
                    val removeDesc = stringResource(R.string.cd_remove_label, label)
                    InputChip(
                        selected = false,
                        onClick = { onLabelsChange(labels - label) },
                        label = {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = removeDesc,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        modifier = Modifier.height(28.dp),
                        colors =
                            InputChipDefaults.inputChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                    )
                }
            }
        }

        Box {
            OutlinedTextField(
                value = labelInput,
                onValueChange = {
                    labelInput = it
                    showSuggestions = it.isNotBlank()
                },
                placeholder = { Text(stringResource(R.string.songbook_label_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (labelInput.isNotBlank()) {
                        IconButton(onClick = {
                            val trimmed = labelInput.trim()
                            if (trimmed.isNotEmpty() && trimmed !in labels) {
                                onLabelsChange(labels + trimmed)
                            }
                            labelInput = ""
                            showSuggestions = false
                        }) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(R.string.cd_add_label),
                            )
                        }
                    }
                },
            )
            DropdownMenu(
                expanded = showSuggestions && suggestions.isNotEmpty(),
                onDismissRequest = { showSuggestions = false },
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            if (suggestion !in labels) {
                                onLabelsChange(labels + suggestion)
                            }
                            labelInput = ""
                            showSuggestions = false
                        },
                    )
                }
            }
        }
    }
}
