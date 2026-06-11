package com.baijum.ukufretboard.ui.songbook

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordParser
import com.baijum.ukufretboard.data.ChordProExporter
import com.baijum.ukufretboard.data.ChordSheet
import com.baijum.ukufretboard.data.VoicingGenerator
import com.baijum.ukufretboard.domain.ChordNameParser
import com.baijum.ukufretboard.domain.ChordSheetFormatter
import com.baijum.ukufretboard.domain.ChordSheetTranspose
import com.baijum.ukufretboard.domain.KeyDetector
import com.baijum.ukufretboard.domain.UkuleleString
import com.baijum.ukufretboard.ui.ShareUtils
import com.baijum.ukufretboard.ui.VerticalChordDiagram
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Viewer for a chord sheet with tappable chord names.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetViewer(
    sheet: ChordSheet,
    allLabels: Set<String>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onChordTapped: (String) -> Unit,
    onPlayChord: (String) -> Unit,
    onStartMetronome: (Int) -> Unit,
    tuning: List<UkuleleString>,
    leftHanded: Boolean,
    onStrumPatternChange: (String) -> Unit,
    onLabelsChange: (List<String>) -> Unit,
    onApplyTranspose: (Int) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val chordSheetLabel = stringResource(R.string.songbook_chord_sheet)
    val exportChooserLabel = stringResource(R.string.songbook_export_chooser)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var tappedChord by remember { mutableStateOf<String?>(null) }
    var performanceMode by rememberSaveable { mutableStateOf(false) }

    // Transpose controls
    var transposeSemitones by rememberSaveable { mutableStateOf(0) }

    val fontSizePrefs = context.getSharedPreferences("songbook_prefs", android.content.Context.MODE_PRIVATE)
    var songFontSize by rememberSaveable { mutableStateOf(fontSizePrefs.getFloat("song_font_size", 14f)) }

    val displayContent = if (transposeSemitones != 0) {
        ChordSheetTranspose.transpose(sheet.content, transposeSemitones)
    } else {
        sheet.content
    }

    val songTextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = songFontSize.sp,
    )

    if (performanceMode) {
        PerformanceModeView(
            displayContent = displayContent,
            textStyle = songTextStyle,
            onExit = { performanceMode = false },
        )
        return
    }

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
            IconButton(onClick = onDuplicate) {
                Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.songbook_duplicate_cd))
            }
            IconButton(onClick = { showShareSheet = true }) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
            }
            IconButton(onClick = { performanceMode = true }) {
                Icon(Icons.Filled.Fullscreen, contentDescription = stringResource(R.string.performance_mode))
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.dialog_delete))
            }
        }

        if (showDeleteDialog) {
            DeleteSongDialog(
                songName = sheet.title.ifEmpty { stringResource(R.string.songbook_untitled) },
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    showDeleteDialog = false
                    onDelete()
                },
            )
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

        // Strum pattern display
        StrumPatternRow(
            patternName = sheet.strumPatternName,
            onPatternChange = onStrumPatternChange,
        )

        // Labels display
        LabelDisplayRow(
            labels = sheet.labels,
            allLabels = allLabels,
            onLabelsChange = onLabelsChange,
        )

        // Song statistics
        if (sheet.viewCount > 0) {
            SongStatsRow(sheet = sheet)
        }

        // Transpose controls
        val transposeDownDesc = stringResource(R.string.cd_transpose_down)
        val transposeUpDesc = stringResource(R.string.cd_transpose_up)
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
            OutlinedButton(
                onClick = { transposeSemitones-- },
                modifier = Modifier.semantics { contentDescription = transposeDownDesc },
            ) {
                Text("\u2212")
            }
            Text(
                text = ChordSheetTranspose.semitoneLabel(transposeSemitones),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            OutlinedButton(
                onClick = { transposeSemitones++ },
                modifier = Modifier.semantics { contentDescription = transposeUpDesc },
            ) {
                Text("+")
            }
            if (transposeSemitones != 0) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { transposeSemitones = 0 }) {
                    Text(stringResource(R.string.dialog_reset))
                }
            }
        }

        // Capo equivalent and "Save in this key" (shown when transposed)
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

            val savedMsg = stringResource(R.string.songbook_saved_in_key)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                FilledTonalButton(
                    onClick = {
                        onApplyTranspose(transposeSemitones)
                        transposeSemitones = 0
                        Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text(stringResource(R.string.songbook_save_in_key))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Auto-scroll state
        var autoScrolling by rememberSaveable { mutableStateOf(false) }
        var scrollSpeed by rememberSaveable { mutableStateOf(1f) }
        val scrollState = rememberScrollState()
        val programmaticScroll = remember { mutableStateOf(false) }

        // Section navigation
        val sections = remember(displayContent) {
            ChordSheetFormatter.extractSections(displayContent)
        }
        val sectionOffsets = remember { mutableMapOf<Int, Int>() }
        val sectionScrollScope = rememberCoroutineScope()

        if (sections.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                sections.forEach { section ->
                    AssistChip(
                        onClick = {
                            sectionOffsets[section.lineIndex]?.let { offset ->
                                sectionScrollScope.launch {
                                    programmaticScroll.value = true
                                    try {
                                        scrollState.animateScrollTo(offset)
                                    } finally {
                                        programmaticScroll.value = false
                                    }
                                }
                            }
                        },
                        label = { Text(section.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }

        // Tempo / metronome integration
        val songTempo = remember(displayContent) {
            ChordSheetFormatter.extractTempo(displayContent)
        }
        if (songTempo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.songbook_tempo_label, songTempo),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = { onStartMetronome(songTempo) },
                ) {
                    Text(stringResource(R.string.songbook_start_metronome))
                }
            }
        }

        // Auto-scroll effect
        LaunchedEffect(autoScrolling, scrollSpeed) {
            if (autoScrolling) {
                while (autoScrolling) {
                    programmaticScroll.value = true
                    try {
                        scrollState.animateScrollTo(
                            scrollState.value + scrollSpeed.toInt().coerceAtLeast(1),
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 16,
                                easing = androidx.compose.animation.core.LinearEasing,
                            ),
                        )
                    } finally {
                        programmaticScroll.value = false
                    }
                    delay(16L)
                }
            }
        }

        // Pause auto-scroll when user manually scrolls (not programmatic)
        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress && autoScrolling && !programmaticScroll.value) {
                autoScrolling = false
            }
        }

        val sectionLineIndices = remember(sections) { sections.map { it.lineIndex }.toSet() }

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                displayContent.lines().forEachIndexed { lineIndex, line ->
                    val sectionModifier = if (lineIndex in sectionLineIndices) {
                        Modifier.onGloballyPositioned { coords ->
                            sectionOffsets[lineIndex] = coords.positionInParent().y.toInt()
                        }
                    } else {
                        Modifier
                    }
                    val segments = ChordParser.parseLine(line)
                    Column(modifier = sectionModifier) {
                        if (segments.isEmpty()) {
                            Text(
                                text = " ",
                                style = songTextStyle,
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
                                val chordAnnotated = buildAnnotatedString {
                                    var cursor = 0
                                    chordPositions.forEach { (pos, name) ->
                                        if (pos > cursor) {
                                            append(" ".repeat(pos - cursor))
                                            cursor = pos
                                        }
                                        withLink(
                                            LinkAnnotation.Clickable(
                                                tag = name,
                                                styles = TextLinkStyles(
                                                    style = SpanStyle(
                                                        color = chordColor,
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                ),
                                                linkInteractionListener = {
                                                    tappedChord = name
                                                },
                                            )
                                        ) {
                                            append(name)
                                        }
                                        cursor += name.length
                                    }
                                }

                                Text(
                                    text = chordAnnotated,
                                    style = songTextStyle,
                                )
                            }

                            Text(
                                text = lyricLine.toString(),
                                style = songTextStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                        }
                    }
                }
            }

            // Font size controls overlay
            val fontDecreaseDesc = stringResource(R.string.songbook_font_decrease)
            val fontIncreaseDesc = stringResource(R.string.songbook_font_increase)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        songFontSize = (songFontSize - 2f).coerceAtLeast(10f)
                        fontSizePrefs.edit().putFloat("song_font_size", songFontSize).apply()
                    },
                    modifier = Modifier.semantics { contentDescription = fontDecreaseDesc },
                ) {
                    Text("A−", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                SmallFloatingActionButton(
                    onClick = {
                        songFontSize = (songFontSize + 2f).coerceAtMost(32f)
                        fontSizePrefs.edit().putFloat("song_font_size", songFontSize).apply()
                    },
                    modifier = Modifier.semantics { contentDescription = fontIncreaseDesc },
                ) {
                    Text("A+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                if (autoScrolling) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.5f to "0.5x", 1f to "1x", 2f to "2x", 3f to "3x").forEach { (speed, label) ->
                            val speedDesc = stringResource(R.string.cd_scroll_speed, label)
                            FilterChip(
                                selected = scrollSpeed == speed,
                                onClick = { scrollSpeed = speed },
                                label = { Text(label) },
                                modifier = Modifier.semantics {
                                    contentDescription = speedDesc
                                    if (scrollSpeed == speed) stateDescription = "selected"
                                },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        val copiedMsg = stringResource(R.string.share_copied)
        if (showShareSheet) {
            val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val effectiveSheet = if (transposeSemitones != 0) {
                sheet.copy(content = displayContent)
            } else {
                sheet
            }
            ModalBottomSheet(
                onDismissRequest = { showShareSheet = false },
                sheetState = shareSheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                ) {
                    Text(
                        text = stringResource(R.string.share_sheet_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(bottom = 16.dp, start = 8.dp)
                            .semantics { heading() },
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.share_as_chordpro)) },
                        leadingContent = {
                            Icon(Icons.Filled.MusicNote, contentDescription = null)
                        },
                        modifier = Modifier.clickable(role = Role.Button) {
                            showShareSheet = false
                            val chordProText = ChordProExporter.export(effectiveSheet)
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
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.share_as_text)) },
                        leadingContent = {
                            Icon(Icons.Filled.Description, contentDescription = null)
                        },
                        modifier = Modifier.clickable(role = Role.Button) {
                            showShareSheet = false
                            val formatted = ChordSheetFormatter.formatChordsAboveLyrics(effectiveSheet)
                            ShareUtils.shareText(
                                context = context,
                                title = sheet.title.ifEmpty { chordSheetLabel },
                                text = formatted,
                            )
                        },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.share_copy_clipboard)) },
                        leadingContent = {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null)
                        },
                        modifier = Modifier.clickable(role = Role.Button) {
                            showShareSheet = false
                            val formatted = ChordSheetFormatter.formatChordsAboveLyrics(effectiveSheet)
                            ShareUtils.copyToClipboard(context, chordSheetLabel, formatted)
                            Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
                            view.announceForAccessibility(copiedMsg)
                        },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.share_as_pdf)) },
                        leadingContent = {
                            Icon(Icons.Filled.Description, contentDescription = null)
                        },
                        modifier = Modifier.clickable(role = Role.Button) {
                            showShareSheet = false
                            val formatted = ChordSheetFormatter.formatChordsAboveLyrics(effectiveSheet)
                            val title = effectiveSheet.title.ifEmpty { chordSheetLabel }
                            ShareUtils.sharePdf(context, title, formatted)
                        },
                    )
                }
            }
        }

        if (tappedChord != null) {
            val chordName = tappedChord!!
            val chordSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            val voicing = remember(chordName, tuning) {
                val parsed = ChordNameParser.parse(chordName) ?: return@remember null
                if (tuning.isEmpty()) return@remember null
                VoicingGenerator.generate(parsed.rootPitchClass, parsed.formula, tuning)
                    .firstOrNull()
            }

            val playChordDesc = stringResource(R.string.songbook_play_chord, chordName)
            val viewInLibraryDesc = stringResource(R.string.songbook_view_in_library, chordName)

            ModalBottomSheet(
                onDismissRequest = { tappedChord = null },
                sheetState = chordSheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = chordName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .semantics { heading() },
                    )

                    if (voicing != null) {
                        VerticalChordDiagram(
                            voicing = voicing,
                            onClick = {},
                            chordName = chordName,
                            leftHanded = leftHanded,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }

                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.songbook_play_chord, chordName)) },
                        leadingContent = {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        },
                        modifier = Modifier
                            .clickable(role = Role.Button) {
                                onPlayChord(chordName)
                            }
                            .semantics { contentDescription = playChordDesc },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.songbook_view_in_library, chordName)) },
                        leadingContent = {
                            Icon(Icons.Filled.MusicNote, contentDescription = null)
                        },
                        modifier = Modifier
                            .clickable(role = Role.Button) {
                                tappedChord = null
                                onChordTapped(chordName)
                            }
                            .semantics { contentDescription = viewInLibraryDesc },
                    )
                }
            }
        }
    }
}

/**
 * Displays labels on a song with a "+" button to add more (used in the viewer).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelDisplayRow(
    labels: List<String>,
    allLabels: Set<String>,
    onLabelsChange: (List<String>) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEach { label ->
            val labelDesc = stringResource(R.string.cd_label, label)
            AssistChip(
                onClick = {},
                label = {
                    Text(label, style = MaterialTheme.typography.labelSmall)
                },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                },
                modifier = Modifier
                    .height(28.dp)
                    .semantics { contentDescription = labelDesc },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
        AssistChip(
            onClick = { showAddDialog = true },
            label = {
                Text(
                    stringResource(R.string.songbook_add_label),
                    style = MaterialTheme.typography.labelSmall,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_add_label),
                    modifier = Modifier.size(14.dp),
                )
            },
            modifier = Modifier.height(28.dp),
        )
    }

    if (showAddDialog) {
        AddLabelDialog(
            currentLabels = labels,
            allLabels = allLabels,
            onAdd = { newLabel ->
                onLabelsChange(labels + newLabel)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun SongStatsRow(sheet: ChordSheet) {
    val lastViewed = if (sheet.lastViewedAt > 0) {
        val fmt = android.text.format.DateUtils.getRelativeTimeSpanString(
            sheet.lastViewedAt,
            System.currentTimeMillis(),
            android.text.format.DateUtils.MINUTE_IN_MILLIS,
        )
        fmt.toString()
    } else {
        null
    }

    val totalMinutes = (sheet.totalViewTimeMs / 60_000).toInt()
    val totalTimeLabel = when {
        totalMinutes < 1 -> stringResource(R.string.stats_time_under_minute)
        totalMinutes == 1 -> stringResource(R.string.stats_time_one_minute)
        totalMinutes < 60 -> stringResource(R.string.stats_time_minutes, totalMinutes)
        else -> {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            stringResource(R.string.stats_time_hours_minutes, hours, mins)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = pluralStringResource(R.plurals.stats_views, sheet.viewCount, sheet.viewCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (lastViewed != null) {
            Text(
                text = lastViewed,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = totalTimeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
