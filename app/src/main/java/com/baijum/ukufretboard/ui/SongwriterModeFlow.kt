package com.baijum.ukufretboard.ui

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordDegree
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Progressions
import com.baijum.ukufretboard.data.ScaleType
import com.baijum.ukufretboard.domain.ChordSheetFormatter
import com.baijum.ukufretboard.domain.ChordSheetTranspose
import com.baijum.ukufretboard.viewmodel.SongbookViewModel

private const val TOTAL_STEPS = 5

/**
 * Guided songwriting flow that walks the user through picking a key/scale,
 * building a progression, writing lyrics, transposing, and sharing.
 */
@Composable
fun SongwriterModeFlow(
    songbookViewModel: SongbookViewModel,
    onSaveProgression: (String, String, List<ChordDegree>, ScaleType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }

    var selectedRoot by rememberSaveable { mutableIntStateOf(0) }
    var selectedScale by rememberSaveable { mutableStateOf(ScaleType.MAJOR) }
    var chosenDegrees by rememberSaveable { mutableStateOf(listOf<ChordDegree>()) }
    var songTitle by rememberSaveable { mutableStateOf("") }
    var songContent by rememberSaveable { mutableStateOf("") }
    var transposeSemitones by rememberSaveable { mutableIntStateOf(0) }

    val context = LocalContext.current
    val songSavedMessage = stringResource(R.string.songwriter_song_saved)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.nav_songwriter_mode),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / TOTAL_STEPS },
            modifier = Modifier.fillMaxWidth(),
        )

        val stepLabel = "Step ${currentStep + 1} of $TOTAL_STEPS"
        Text(
            text = stepLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 4.dp, bottom = 12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            when (currentStep) {
                0 -> StepChooseKeyScale(
                    selectedRoot = selectedRoot,
                    onRootChange = {
                        selectedRoot = it
                        chosenDegrees = emptyList()
                        transposeSemitones = 0
                    },
                    selectedScale = selectedScale,
                    onScaleChange = {
                        selectedScale = it
                        chosenDegrees = emptyList()
                        transposeSemitones = 0
                    },
                )
                1 -> StepBuildProgression(
                    selectedRoot = selectedRoot,
                    selectedScale = selectedScale,
                    chosenDegrees = chosenDegrees,
                    onDegreesChange = { chosenDegrees = it },
                )
                2 -> StepAddLyrics(
                    songTitle = songTitle,
                    onTitleChange = { songTitle = it },
                    songContent = songContent,
                    onContentChange = { songContent = it },
                    selectedRoot = selectedRoot,
                    chosenDegrees = chosenDegrees,
                )
                3 -> StepTranspose(
                    content = songContent,
                    transposeSemitones = transposeSemitones,
                    onTransposeChange = { transposeSemitones = it },
                )
                4 -> StepReviewAndSave(
                    songTitle = songTitle,
                    songContent = songContent,
                    transposeSemitones = transposeSemitones,
                    selectedRoot = selectedRoot,
                    chosenDegrees = chosenDegrees,
                    selectedScale = selectedScale,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (currentStep > 0) {
                OutlinedButton(onClick = { currentStep-- }) {
                    Text(stringResource(R.string.onboarding_back))
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (currentStep < TOTAL_STEPS - 1) {
                Button(
                    onClick = { currentStep++ },
                    enabled = when (currentStep) {
                        1 -> chosenDegrees.size >= 2
                        else -> true
                    },
                ) {
                    Text(stringResource(R.string.onboarding_next))
                }
            } else {
                Button(
                    onClick = {
                        val finalContent = if (transposeSemitones != 0) {
                            ChordSheetTranspose.transpose(songContent, transposeSemitones)
                        } else {
                            songContent
                        }
                        val finalRoot = ((selectedRoot + transposeSemitones) % 12 + 12) % 12
                        val finalKey = "${Notes.pitchClassToName(finalRoot)} ${selectedScale.label}"
                        // Always create a NEW song here. Songwriter Mode is never
                        // editing an existing sheet, but saveSheet would infer that
                        // from a stale _currentSheet and overwrite the last-opened
                        // song in place (issue #575).
                        songbookViewModel.createSheet(
                            title = songTitle.ifBlank { "My Song" },
                            artist = "",
                            content = finalContent,
                            key = finalKey,
                        )
                        if (chosenDegrees.size >= 2) {
                            val keyName = Notes.pitchClassToName(selectedRoot)
                            onSaveProgression(
                                "$keyName ${selectedScale.name} song progression",
                                "Created in Songwriter Mode",
                                chosenDegrees,
                                selectedScale,
                            )
                        }
                        Toast.makeText(context, songSavedMessage, Toast.LENGTH_SHORT).show()
                        currentStep = 0
                        songTitle = ""
                        songContent = ""
                        chosenDegrees = emptyList()
                        transposeSemitones = 0
                    },
                    enabled = songTitle.isNotBlank() || songContent.isNotBlank(),
                ) {
                    Text(stringResource(R.string.dialog_save))
                }
            }
        }
    }
}

@Composable
private fun StepChooseKeyScale(
    selectedRoot: Int,
    onRootChange: (Int) -> Unit,
    selectedScale: ScaleType,
    onScaleChange: (ScaleType) -> Unit,
) {
    Text(
        text = "Choose a key and scale",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Pick the musical key and scale for your song. This determines which chords will be suggested.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.label_root),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (pc in 0 until Notes.PITCH_CLASS_COUNT) {
            val name = Notes.pitchClassToName(pc)
            FilterChip(
                selected = pc == selectedRoot,
                onClick = { onRootChange(pc) },
                label = { Text(name) },
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.label_scale),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ScaleType.entries.forEach { scale ->
            FilterChip(
                selected = scale == selectedScale,
                onClick = { onScaleChange(scale) },
                label = { Text(scale.label) },
            )
        }
    }
}

@Composable
private fun StepBuildProgression(
    selectedRoot: Int,
    selectedScale: ScaleType,
    chosenDegrees: List<ChordDegree>,
    onDegreesChange: (List<ChordDegree>) -> Unit,
) {
    Text(
        text = "Build a chord progression",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Tap the diatonic chords below to build your progression. These are the chords that naturally fit your chosen key.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(16.dp))

    val availableDegrees = Progressions.diatonicDegrees(selectedScale)

    Text(
        text = stringResource(R.string.progression_tap_chords),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        availableDegrees.forEach { degree ->
            val chordRoot = (selectedRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
            val chordName = Notes.enharmonicForKey(chordRoot, selectedRoot) + degree.quality
            FilterChip(
                selected = false,
                onClick = { onDegreesChange(chosenDegrees + degree) },
                label = { Text(chordName) },
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (chosenDegrees.isNotEmpty()) {
        Text(
            text = stringResource(R.string.progression_your_chords, chosenDegrees.size),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            chosenDegrees.forEachIndexed { index, degree ->
                val chordRoot = (selectedRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
                val chordName = Notes.enharmonicForKey(chordRoot, selectedRoot) + degree.quality
                FilterChip(
                    selected = true,
                    onClick = {
                        onDegreesChange(chosenDegrees.toMutableList().apply { removeAt(index) })
                    },
                    label = { Text(chordName) },
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(onClick = { onDegreesChange(emptyList()) }) {
            Text(stringResource(R.string.dialog_reset))
        }
    } else {
        Text(
            text = stringResource(R.string.progression_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (chosenDegrees.size == 1) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.progression_error_min),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun StepAddLyrics(
    songTitle: String,
    onTitleChange: (String) -> Unit,
    songContent: String,
    onContentChange: (String) -> Unit,
    selectedRoot: Int,
    chosenDegrees: List<ChordDegree>,
) {
    Text(
        text = "Add lyrics and chords",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Write your lyrics below. Tap the chord chips to insert [Chord] markers.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = songTitle,
        onValueChange = onTitleChange,
        label = { Text(stringResource(R.string.songbook_field_title)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (chosenDegrees.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            chosenDegrees.forEach { degree ->
                val chordRoot = (selectedRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
                val chordName = Notes.enharmonicForKey(chordRoot, selectedRoot) + degree.quality
                FilterChip(
                    selected = false,
                    onClick = { onContentChange(songContent + "[$chordName]") },
                    label = { Text(chordName) },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    OutlinedTextField(
        value = songContent,
        onValueChange = onContentChange,
        label = { Text(stringResource(R.string.songbook_field_lyrics)) },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
    )
}

@Composable
private fun StepTranspose(
    content: String,
    transposeSemitones: Int,
    onTransposeChange: (Int) -> Unit,
) {
    Text(
        text = "Transpose (optional)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Shift the key up or down if needed. The transposition will be applied when you save.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = { onTransposeChange(transposeSemitones - 1) }) {
            Text("\u2212")
        }
        Text(
            text = ChordSheetTranspose.semitoneLabel(transposeSemitones),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        OutlinedButton(onClick = { onTransposeChange(transposeSemitones + 1) }) {
            Text("+")
        }
        if (transposeSemitones != 0) {
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { onTransposeChange(0) }) {
                Text(stringResource(R.string.dialog_reset))
            }
        }
    }

    if (content.isNotEmpty() && transposeSemitones != 0) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Preview:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        val transposed = ChordSheetTranspose.transpose(content, transposeSemitones)
        Text(
            text = transposed,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepReviewAndSave(
    songTitle: String,
    songContent: String,
    transposeSemitones: Int,
    selectedRoot: Int,
    chosenDegrees: List<ChordDegree>,
    selectedScale: ScaleType,
) {
    Text(
        text = "Review and save",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Check your song below, then tap Save to add it to your songbook.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(16.dp))

    val keyName = Notes.pitchClassToName(selectedRoot)
    Text(
        text = "Key: $keyName ${selectedScale.label}",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )

    if (chosenDegrees.isNotEmpty()) {
        val chordNames = chosenDegrees.map { degree ->
            val chordRoot = (selectedRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
            Notes.enharmonicForKey(chordRoot, selectedRoot) + degree.quality
        }
        Text(
            text = "Progression: ${chordNames.joinToString(" – ")}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (transposeSemitones != 0) {
        Text(
            text = "Transpose: ${ChordSheetTranspose.semitoneLabel(transposeSemitones)} semitones",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.height(4.dp))
    }

    Text(
        text = songTitle.ifBlank { "Untitled" },
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(4.dp))

    val finalContent = if (transposeSemitones != 0) {
        ChordSheetTranspose.transpose(songContent, transposeSemitones)
    } else {
        songContent
    }
    Text(
        text = finalContent.ifEmpty { "(no lyrics yet)" },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
