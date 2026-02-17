package com.baijum.ukufretboard.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordParser
import com.baijum.ukufretboard.data.ChordProExporter
import com.baijum.ukufretboard.data.ChordProParser
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.domain.ChordSheetFormatter
import com.baijum.ukufretboard.domain.ChordSheetTranspose
import com.baijum.ukufretboard.domain.KeyDetector
import com.baijum.ukufretboard.viewmodel.SongbookViewModel

/**
 * Songbook tab showing a list of chord sheets, a viewer, and an editor.
 *
 * @param viewModel The [SongbookViewModel] managing chord sheets.
 * @param onChordTapped Callback when a chord name in a sheet is tapped.
 */
@Composable
fun SongbookTab(
    viewModel: SongbookViewModel,
    onChordTapped: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheets by viewModel.sheets.collectAsState()
    val currentSheet by viewModel.currentSheet.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()

    when {
        isEditing -> {
            SheetEditor(
                sheet = currentSheet,
                onSave = { title, artist, content ->
                    viewModel.saveSheet(title, artist, content)
                },
                onCancel = { viewModel.closeSheet() },
            )
        }
        currentSheet != null -> {
            SheetViewer(
                sheet = currentSheet!!,
                onBack = { viewModel.closeSheet() },
                onEdit = { viewModel.startEditing(currentSheet) },
                onDelete = {
                    viewModel.deleteSheet(currentSheet!!.id)
                    viewModel.closeSheet()
                },
                onChordTapped = onChordTapped,
            )
        }
        else -> {
            SheetList(
                sheets = sheets,
                onSheetTapped = { viewModel.openSheet(it) },
                onNewSheet = { viewModel.startEditing() },
                onImport = { content, filename ->
                    if (filename != null && ChordProParser.isChordProFile(filename)) {
                        viewModel.importChordPro(content, filename)
                    } else {
                        viewModel.importChordPro(content, filename)
                    }
                },
                modifier = modifier,
            )
        }
    }
}

/**
 * List of saved chord sheets.
 */
@Composable
private fun SheetList(
    sheets: List<ChordSheet>,
    onSheetTapped: (ChordSheet) -> Unit,
    onNewSheet: () -> Unit,
    onImport: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader()?.readText() ?: return@let
                inputStream.close()
                // Extract filename from URI for fallback title
                val filename = it.lastPathSegment
                onImport(content, filename)
            } catch (_: Exception) {
                // Import failed silently
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (sheets.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.songbook_empty_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.songbook_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sheets) { sheet ->
                    SheetCard(sheet = sheet, onClick = { onSheetTapped(sheet) })
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Import button
            SmallFloatingActionButton(
                onClick = {
                    importLauncher.launch(arrayOf("text/*", "*/*"))
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Icon(
                    Icons.Filled.FileOpen,
                    contentDescription = stringResource(R.string.cd_import_chordpro),
                    modifier = Modifier.size(20.dp),
                )
            }
            // New sheet button
            FloatingActionButton(
                onClick = onNewSheet,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_new_chord_sheet))
            }
        }
    }
}

@Composable
private fun SheetCard(sheet: ChordSheet, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = sheet.title.ifEmpty { stringResource(R.string.songbook_untitled) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (sheet.artist.isNotEmpty()) {
                Text(
                    text = sheet.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Viewer for a chord sheet with tappable chord names.
 */
@Composable
private fun SheetViewer(
    sheet: ChordSheet,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onChordTapped: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val chordSheetLabel = stringResource(R.string.songbook_chord_sheet)
    val exportChooserLabel = stringResource(R.string.songbook_export_chooser)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(
                text = sheet.title.ifEmpty { stringResource(R.string.songbook_untitled) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).semantics { heading() },
                textAlign = TextAlign.Center,
            )
            // Share / Export menu
            Box {
                var showShareMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showShareMenu = true }) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share))
                }
                DropdownMenu(
                    expanded = showShareMenu,
                    onDismissRequest = { showShareMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.songbook_share_as_text)) },
                        onClick = {
                            showShareMenu = false
                            val formatted = ChordSheetFormatter.formatChordsAboveLyrics(sheet)
                            ChordSheetFormatter.shareText(
                                context = context,
                                title = sheet.title.ifEmpty { chordSheetLabel },
                                text = formatted,
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.songbook_export_chordpro)) },
                        onClick = {
                            showShareMenu = false
                            val chordProText = ChordProExporter.export(sheet)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, chordProText)
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    ChordProExporter.suggestedFilename(sheet),
                                )
                            }
                            context.startActivity(
                                Intent.createChooser(intent, exportChooserLabel),
                            )
                        },
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.dialog_delete))
            }
        }

        if (sheet.artist.isNotEmpty()) {
            Text(
                text = sheet.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 48.dp, bottom = 8.dp),
            )
        }

        // Detect song key from chords
        val songChords = remember(sheet.content) { ChordParser.extractChords(sheet.content) }
        val detectedKey = remember(songChords) { KeyDetector.detectKey(songChords) }

        // Key display
        if (detectedKey != null) {
            Text(
                text = stringResource(R.string.songbook_key_prefix) + detectedKey.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
            )
        }

        // Transpose controls
        var transposeSemitones by remember { mutableStateOf(0) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.songbook_transpose),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = { transposeSemitones-- }) {
                Text("\u2212") // minus sign
            }
            Text(
                text = ChordSheetTranspose.semitoneLabel(transposeSemitones),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            OutlinedButton(onClick = { transposeSemitones++ }) {
                Text("+")
            }
            if (transposeSemitones != 0) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { transposeSemitones = 0 }) {
                    Text(stringResource(R.string.dialog_reset))
                }
            }
        }

        // Capo equivalent (shown when transposed)
        if (transposeSemitones != 0) {
            val capoFret = ((transposeSemitones % 12) + 12) % 12
            if (capoFret > 0) {
                Text(
                    text = stringResource(R.string.songbook_capo_hint, capoFret),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Content with tappable chords (transposed if needed)
        val displayContent = if (transposeSemitones != 0) {
            ChordSheetTranspose.transpose(sheet.content, transposeSemitones)
        } else {
            sheet.content
        }

        // Auto-scroll state
        var autoScrolling by remember { mutableStateOf(false) }
        var scrollSpeed by remember { mutableFloatStateOf(1f) } // 1x, 2x, 3x
        val scrollState = rememberScrollState()

        // Auto-scroll effect
        LaunchedEffect(autoScrolling, scrollSpeed) {
            if (autoScrolling) {
                while (autoScrolling) {
                    scrollState.animateScrollTo(
                        scrollState.value + scrollSpeed.toInt().coerceAtLeast(1),
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 16,
                            easing = androidx.compose.animation.core.LinearEasing,
                        ),
                    )
                    delay(16L) // ~60fps
                }
            }
        }

        // Pause auto-scroll when user manually scrolls
        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress && autoScrolling) {
                autoScrolling = false
            }
        }

        Box(modifier = Modifier.weight(1f)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            displayContent.lines().forEach { line ->
                val segments = ChordParser.parseLine(line)
                if (segments.isEmpty()) {
                    Text(
                        text = " ", // empty line
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                } else {
                    val chordColor = MaterialTheme.colorScheme.primary
                    val annotated = buildAnnotatedString {
                        segments.forEach { segment ->
                            when (segment) {
                                is ChordParser.TextSegment.PlainText -> {
                                    append(segment.text)
                                }
                                is ChordParser.TextSegment.Chord -> {
                                    val chordName = segment.name
                                    withLink(
                                        LinkAnnotation.Clickable(
                                            tag = chordName,
                                            styles = TextLinkStyles(
                                                style = SpanStyle(
                                                    color = chordColor,
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                            ),
                                            linkInteractionListener = {
                                                onChordTapped(chordName)
                                            },
                                        )
                                    ) {
                                        append(chordName)
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            }
        }

            // Auto-scroll controls overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Speed chips (visible when scrolling)
                if (autoScrolling) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.5f to "0.5x", 1f to "1x", 2f to "2x", 3f to "3x").forEach { (speed, label) ->
                            FilterChip(
                                selected = scrollSpeed == speed,
                                onClick = { scrollSpeed = speed },
                                label = { Text(label) },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Stop button (visible when scrolling) — resets to top
                    if (autoScrolling) {
                        FloatingActionButton(
                            onClick = {
                                autoScrolling = false
                            },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = stringResource(R.string.cd_stop_scroll),
                            )
                        }
                    }
                    // Play / Pause toggle
                    FloatingActionButton(
                        onClick = { autoScrolling = !autoScrolling },
                    ) {
                        Icon(
                            imageVector = if (autoScrolling) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (autoScrolling) stringResource(R.string.cd_pause_scroll) else stringResource(R.string.cd_auto_scroll),
                        )
                    }
                }
            }
        } // Box
    }
}

/**
 * Editor for creating/editing a chord sheet.
 */
@Composable
private fun SheetEditor(
    sheet: ChordSheet?,
    onSave: (title: String, artist: String, content: String) -> Unit,
    onCancel: () -> Unit,
) {
    var title by remember(sheet?.id) { mutableStateOf(sheet?.title ?: "") }
    var artist by remember(sheet?.id) { mutableStateOf(sheet?.artist ?: "") }
    var content by remember(sheet?.id) { mutableStateOf(sheet?.content ?: "") }

    Column(
        modifier = Modifier
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

        Text(
            text = stringResource(R.string.songbook_field_lyrics_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text(stringResource(R.string.songbook_field_lyrics)) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.dialog_cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { onSave(title, artist, content) },
                enabled = title.isNotBlank(),
            ) {
                Text(stringResource(R.string.dialog_save))
            }
        }
    }
}
