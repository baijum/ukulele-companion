package com.baijum.ukufretboard.ui.patterns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.audio.PatternPlayer
import com.baijum.ukufretboard.data.CustomFingerpickingPattern
import com.baijum.ukufretboard.data.CustomFingerpickingPatternRepository
import com.baijum.ukufretboard.data.CustomStrumPattern
import com.baijum.ukufretboard.data.CustomStrumPatternRepository
import com.baijum.ukufretboard.data.FingerpickingPatterns
import com.baijum.ukufretboard.data.StrumPatterns
import com.baijum.ukufretboard.data.UkuleleTuning

private const val TAB_STRUMMING = 0
private const val TAB_FINGERPICKING = 1

internal val TIME_SIGNATURES = listOf("2/2", "2/4", "3/4", "4/4", "5/4", "7/4", "6/8", "7/8", "9/8", "11/8", "12/8")

internal fun defaultBeatCount(timeSignature: String): Int {
    val parts = timeSignature.split("/")
    val numerator = parts[0].toIntOrNull() ?: 4
    val denominator = parts.getOrNull(1)?.toIntOrNull() ?: 4
    return when (denominator) {
        2 -> numerator * 2
        4 -> numerator * 2
        8 -> numerator
        else -> numerator * 2
    }
}

/**
 * Tab showing a reference list of common ukulele strumming and fingerpicking patterns.
 *
 * A toggle at the top switches between Strumming and Fingerpicking views.
 * Users can create custom patterns via the FAB on either tab.
 */
@Composable
fun StrumPatternsTab(
    tuning: UkuleleTuning,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copySuffix = stringResource(R.string.progressions_copy_suffix)
    val strumRepo = remember { CustomStrumPatternRepository(context) }
    val fingerpickRepo = remember { CustomFingerpickingPatternRepository(context) }
    var customStrumPatterns by remember { mutableStateOf(strumRepo.getAll()) }
    var customFingerpickPatterns by remember { mutableStateOf(fingerpickRepo.getAll()) }
    var selectedTab by remember { mutableIntStateOf(TAB_STRUMMING) }
    var showCreateStrumSheet by remember { mutableStateOf(false) }
    var showCreateFingerpickSheet by remember { mutableStateOf(false) }
    var editingStrumPattern by remember { mutableStateOf<CustomStrumPattern?>(null) }
    var editingFingerpickPattern by remember { mutableStateOf<CustomFingerpickingPattern?>(null) }
    var deletingStrumPattern by remember { mutableStateOf<CustomStrumPattern?>(null) }
    var deletingFingerpickPattern by remember { mutableStateOf<CustomFingerpickingPattern?>(null) }

    val patternPlayer = remember { PatternPlayer() }
    val isPlaying by patternPlayer.isPlaying.collectAsState()
    val activeBeatIndex by patternPlayer.currentIndex.collectAsState()
    var playingPatternName by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { patternPlayer.stop() }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                FilterChip(
                    selected = selectedTab == TAB_STRUMMING,
                    onClick = { selectedTab = TAB_STRUMMING },
                    label = { Text(stringResource(R.string.patterns_strumming)) },
                )
                FilterChip(
                    selected = selectedTab == TAB_FINGERPICKING,
                    onClick = { selectedTab = TAB_FINGERPICKING },
                    label = { Text(stringResource(R.string.patterns_fingerpicking)) },
                )
            }

            when (selectedTab) {
                TAB_STRUMMING -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                ) {
                    if (customStrumPatterns.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.patterns_my),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                        items(customStrumPatterns, key = { it.id }) { custom ->
                            StrumPatternCard(
                                pattern = custom.pattern,
                                isPlaying = isPlaying && playingPatternName == custom.pattern.name,
                                activeBeatIndex = if (isPlaying && playingPatternName == custom.pattern.name) activeBeatIndex else -1,
                                onPlay = { bpm ->
                                    playingPatternName = custom.pattern.name
                                    patternPlayer.playStrum(scope, custom.pattern, bpm, tuning)
                                },
                                onStop = { patternPlayer.stop(); playingPatternName = null },
                                onDuplicate = {
                                    strumRepo.save(CustomStrumPattern(pattern = custom.pattern.copy(name = custom.pattern.name + copySuffix)))
                                    customStrumPatterns = strumRepo.getAll()
                                },
                                onEdit = { editingStrumPattern = custom },
                                onDelete = { deletingStrumPattern = custom },
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.label_presets),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                    }
                    items(StrumPatterns.ALL) { pattern ->
                        StrumPatternCard(
                            pattern = pattern,
                            isPlaying = isPlaying && playingPatternName == pattern.name,
                            activeBeatIndex = if (isPlaying && playingPatternName == pattern.name) activeBeatIndex else -1,
                            onPlay = { bpm ->
                                playingPatternName = pattern.name
                                patternPlayer.playStrum(scope, pattern, bpm, tuning)
                            },
                            onStop = { patternPlayer.stop(); playingPatternName = null },
                            onDuplicate = {
                                strumRepo.save(CustomStrumPattern(pattern = pattern.copy(name = pattern.name + copySuffix)))
                                customStrumPatterns = strumRepo.getAll()
                            },
                        )
                    }
                }
                TAB_FINGERPICKING -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                ) {
                    if (customFingerpickPatterns.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.patterns_my),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                        items(customFingerpickPatterns, key = { it.id }) { custom ->
                            FingerpickingPatternCard(
                                pattern = custom.pattern,
                                isPlaying = isPlaying && playingPatternName == custom.pattern.name,
                                activeStepIndex = if (isPlaying && playingPatternName == custom.pattern.name) activeBeatIndex else -1,
                                onPlay = { bpm ->
                                    playingPatternName = custom.pattern.name
                                    patternPlayer.playFingerpick(scope, custom.pattern, bpm, tuning)
                                },
                                onStop = { patternPlayer.stop(); playingPatternName = null },
                                onDuplicate = {
                                    fingerpickRepo.save(CustomFingerpickingPattern(pattern = custom.pattern.copy(name = custom.pattern.name + copySuffix)))
                                    customFingerpickPatterns = fingerpickRepo.getAll()
                                },
                                onEdit = { editingFingerpickPattern = custom },
                                onDelete = { deletingFingerpickPattern = custom },
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.label_presets),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                    }
                    items(FingerpickingPatterns.ALL) { pattern ->
                        FingerpickingPatternCard(
                            pattern = pattern,
                            isPlaying = isPlaying && playingPatternName == pattern.name,
                            activeStepIndex = if (isPlaying && playingPatternName == pattern.name) activeBeatIndex else -1,
                            onPlay = { bpm ->
                                playingPatternName = pattern.name
                                patternPlayer.playFingerpick(scope, pattern, bpm, tuning)
                            },
                            onStop = { patternPlayer.stop(); playingPatternName = null },
                            onDuplicate = {
                                fingerpickRepo.save(CustomFingerpickingPattern(pattern = pattern.copy(name = pattern.name + copySuffix)))
                                customFingerpickPatterns = fingerpickRepo.getAll()
                            },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                when (selectedTab) {
                    TAB_STRUMMING -> showCreateStrumSheet = true
                    TAB_FINGERPICKING -> showCreateFingerpickSheet = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_create_pattern))
        }
    }

    if (showCreateStrumSheet) {
        CreateStrumPatternSheet(
            onDismiss = { showCreateStrumSheet = false },
            onSave = { pattern ->
                strumRepo.save(CustomStrumPattern(pattern = pattern))
                customStrumPatterns = strumRepo.getAll()
                showCreateStrumSheet = false
            },
        )
    }

    if (showCreateFingerpickSheet) {
        CreateFingerpickingPatternSheet(
            onDismiss = { showCreateFingerpickSheet = false },
            onSave = { pattern ->
                fingerpickRepo.save(CustomFingerpickingPattern(pattern = pattern))
                customFingerpickPatterns = fingerpickRepo.getAll()
                showCreateFingerpickSheet = false
            },
        )
    }

    val editingStrum = editingStrumPattern
    if (editingStrum != null) {
        CreateStrumPatternSheet(
            initialPattern = editingStrum.pattern,
            onDismiss = { editingStrumPattern = null },
            onSave = { pattern ->
                strumRepo.save(
                    CustomStrumPattern(
                        id = editingStrum.id,
                        pattern = pattern,
                        createdAt = editingStrum.createdAt,
                    ),
                )
                customStrumPatterns = strumRepo.getAll()
                editingStrumPattern = null
            },
        )
    }

    val editingFingerpick = editingFingerpickPattern
    if (editingFingerpick != null) {
        CreateFingerpickingPatternSheet(
            initialPattern = editingFingerpick.pattern,
            onDismiss = { editingFingerpickPattern = null },
            onSave = { pattern ->
                fingerpickRepo.save(
                    CustomFingerpickingPattern(
                        id = editingFingerpick.id,
                        pattern = pattern,
                        createdAt = editingFingerpick.createdAt,
                    ),
                )
                customFingerpickPatterns = fingerpickRepo.getAll()
                editingFingerpickPattern = null
            },
        )
    }

    deletingStrumPattern?.let { custom ->
        DeletePatternDialog(
            patternName = custom.pattern.name,
            onDismiss = { deletingStrumPattern = null },
            onConfirm = {
                patternPlayer.stop(); playingPatternName = null
                strumRepo.delete(custom.id)
                customStrumPatterns = strumRepo.getAll()
                deletingStrumPattern = null
            },
        )
    }

    deletingFingerpickPattern?.let { custom ->
        DeletePatternDialog(
            patternName = custom.pattern.name,
            onDismiss = { deletingFingerpickPattern = null },
            onConfirm = {
                patternPlayer.stop(); playingPatternName = null
                fingerpickRepo.delete(custom.id)
                customFingerpickPatterns = fingerpickRepo.getAll()
                deletingFingerpickPattern = null
            },
        )
    }
}

@Composable
private fun DeletePatternDialog(
    patternName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.pattern_delete_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = { Text(stringResource(R.string.pattern_delete_message, patternName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.dialog_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}
