package com.baijum.ukufretboard.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.audio.AudioCaptureEngine
import com.baijum.ukufretboard.audio.MetronomeEngine
import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Progression
import com.baijum.ukufretboard.data.Progressions
import com.baijum.ukufretboard.data.ScaleType
import com.baijum.ukufretboard.data.VoicingGenerator
import com.baijum.ukufretboard.domain.AudioChordDetector
import com.baijum.ukufretboard.domain.ChordDetector
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.domain.PlayAlongScorer
import com.baijum.ukufretboard.viewmodel.UkuleleString

/**
 * Real-Time Play-Along with feedback view.
 *
 * Displays a chord progression with a metronome, captures audio from the
 * microphone, detects chords, and provides real-time feedback on whether
 * the user is playing the correct chord.
 *
 * @param progression The chord progression to play along with.
 * @param keyRoot The root pitch class of the selected key.
 * @param tuning Current ukulele tuning.
 * @param onPlayVoicing Callback to play a chord voicing.
 * @param onBack Callback to exit the view.
 */
@Composable
fun PlayAlongView(
    progression: Progression,
    keyRoot: Int,
    tuning: List<UkuleleString>,
    onPlayVoicing: ((ChordVoicing) -> Unit)? = null,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { MetronomeEngine() }
    val scorer = remember { PlayAlongScorer() }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasMicPermission = granted
    }

    // State
    var isPlaying by remember { mutableStateOf(false) }
    var bpm by remember { mutableFloatStateOf(80f) }
    var beatsPerChord by remember { mutableIntStateOf(4) }
    var currentChordIndex by remember { mutableIntStateOf(0) }
    var detectedChord by remember { mutableStateOf<String?>(null) }
    var detectionConfidence by remember { mutableFloatStateOf(0f) }
    var score by remember { mutableStateOf(PlayAlongScorer.PlayAlongScore()) }
    var sessionComplete by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    // Build chord names for the progression
    val chordNames = remember(progression, keyRoot) {
        progression.degrees.map { degree ->
            val chordRoot = (keyRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
            Notes.enharmonicForKey(chordRoot, keyRoot) + degree.quality
        }
    }

    // Clean up on dispose
    DisposableEffect(Unit) {
        onDispose {
            engine.stop()
            if (isListening) {
                AudioCaptureEngine.stop()
            }
        }
    }

    // Audio detection callback
    LaunchedEffect(isListening) {
        if (isListening && hasMicPermission) {
            AudioCaptureEngine.start(scope) { samples ->
                val result = AudioChordDetector.detect(samples)
                val chordFound = result.detection as? ChordDetector.DetectionResult.ChordFound
                if (chordFound != null && result.confidence > 0.3f) {
                    detectedChord = chordFound.result.name
                    detectionConfidence = result.confidence
                } else {
                    detectedChord = null
                    detectionConfidence = 0f
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.play_along_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${progression.name} — ${stringResource(R.string.play_along_subtitle)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Microphone permission
        if (!hasMicPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.play_along_mic_needed),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }) {
                        Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.cd_grant_permission))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.play_along_grant))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Chord timeline with current highlight
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            chordNames.forEachIndexed { index, name ->
                val isCurrent = index == currentChordIndex
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Current expected chord display
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.play_along_play),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
                Text(
                    text = chordNames.getOrElse(currentChordIndex) { "?" },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )

                if (isPlaying && detectedChord != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val expectedChord = chordNames.getOrElse(currentChordIndex) { "" }
                    val isCorrect = detectedChord?.contains(
                        expectedChord.take(2),
                        ignoreCase = true,
                    ) == true

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Close,
                            contentDescription = if (isCorrect) stringResource(R.string.cd_correct) else stringResource(R.string.cd_incorrect),
                            tint = if (isCorrect) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stringResource(R.string.play_along_heard)} ${detectedChord ?: "..."}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCorrect) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Score display (during/after play)
        if (score.totalBeats > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${score.accuracyPercent}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(stringResource(R.string.label_accuracy), style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = score.grade,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(stringResource(R.string.play_along_grade), style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${score.bestStreak}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(stringResource(R.string.label_best_streak), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // BPM + Tap Tempo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_bpm_value, bpm.toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(64.dp),
                    )
                    Slider(
                        value = bpm,
                        onValueChange = { bpm = it },
                        valueRange = 40f..180f,
                        modifier = Modifier.weight(1f),
                    )
                    TapTempoButton(
                        onBpmDetected = { bpm = it.toFloat() },
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                // Beats per chord
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(R.string.label_beats), style = MaterialTheme.typography.labelSmall)
                    listOf(2, 4, 8).forEach { beats ->
                        FilterChip(
                            selected = beatsPerChord == beats,
                            onClick = { beatsPerChord = beats },
                            label = { Text("$beats") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier.height(30.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Play / Stop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                engine.stop()
                                AudioCaptureEngine.stop()
                                isPlaying = false
                                isListening = false
                                score = scorer.getScore()
                                sessionComplete = true
                            } else {
                                scorer.reset()
                                sessionComplete = false
                                isPlaying = true
                                isListening = hasMicPermission

                                var lastChordPlayed = -1
                                engine.start(
                                    scope = scope,
                                    bpm = bpm.toInt(),
                                    beatsPerChord = beatsPerChord,
                                    chordCount = progression.degrees.size,
                                    loop = false,
                                    onBeat = { chordIdx, beat ->
                                        currentChordIndex = chordIdx
                                        // Score the previous beat
                                        val expected = chordNames.getOrElse(chordIdx) { "" }
                                        scorer.recordBeat(
                                            expectedChord = expected,
                                            detectedChord = detectedChord,
                                            confidence = detectionConfidence,
                                        )
                                        score = scorer.getScore()

                                        if (chordIdx != lastChordPlayed) {
                                            lastChordPlayed = chordIdx
                                            // Play voicing for reference
                                            val chordRoot = (keyRoot + progression.degrees[chordIdx].interval) %
                                                Notes.PITCH_CLASS_COUNT
                                            val formula = ChordFormulas.ALL
                                                .firstOrNull { it.symbol == progression.degrees[chordIdx].quality }
                                            if (formula != null && onPlayVoicing != null) {
                                                val voicing = VoicingGenerator.generate(chordRoot, formula, tuning)
                                                    .firstOrNull()
                                                if (voicing != null) onPlayVoicing(voicing)
                                            }
                                        }
                                    },
                                    onComplete = {
                                        isPlaying = false
                                        isListening = false
                                        AudioCaptureEngine.stop()
                                        score = scorer.getScore()
                                        sessionComplete = true
                                    },
                                )
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = if (isPlaying) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                shape = CircleShape,
                            ),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play Along",
                            tint = if (isPlaying) {
                                MaterialTheme.colorScheme.onError
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Setup screen for Play Along — select a key and progression before starting.
 */
@Composable
fun PlayAlongSetup(
    tuning: List<UkuleleString>,
    onPlayVoicing: ((ChordVoicing) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var selectedKey by remember { mutableIntStateOf(0) } // pitch class 0 = C
    var selectedProgression by remember { mutableStateOf<Progression?>(null) }
    var selectedScale by remember { mutableStateOf(ScaleType.MAJOR) }

    val progressions = remember(selectedScale) { Progressions.forScale(selectedScale) }

    if (selectedProgression != null) {
        PlayAlongView(
            progression = selectedProgression!!,
            keyRoot = selectedKey,
            tuning = tuning,
            onPlayVoicing = onPlayVoicing,
            onBack = { selectedProgression = null },
            modifier = modifier,
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.play_along_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.play_along_choose),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Key selection
            Text(
                text = stringResource(R.string.label_key),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val noteNames = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")
                noteNames.forEachIndexed { index, name ->
                    FilterChip(
                        selected = selectedKey == index,
                        onClick = { selectedKey = index },
                        label = { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scale type
            Text(
                text = stringResource(R.string.label_scale),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScaleType.entries.forEach { scale ->
                    FilterChip(
                        selected = selectedScale == scale,
                        onClick = { selectedScale = scale },
                        label = { Text(scale.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progression selection
            Text(
                text = stringResource(R.string.play_along_progression),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            progressions.forEach { progression ->
                Card(
                    onClick = { selectedProgression = progression },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = progression.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = progression.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // Show resolved chord names
                        val chords = progression.degrees.map { deg ->
                            val root = (selectedKey + deg.interval) % Notes.PITCH_CLASS_COUNT
                            Notes.enharmonicForKey(root, selectedKey) + deg.quality
                        }
                        Text(
                            text = chords.joinToString(" → "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
