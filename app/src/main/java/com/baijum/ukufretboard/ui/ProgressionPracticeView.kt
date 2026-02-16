package com.baijum.ukufretboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.audio.MetronomeEngine
import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.Progression
import com.baijum.ukufretboard.data.VoicingGenerator
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.domain.Note
import com.baijum.ukufretboard.viewmodel.FretboardViewModel
import com.baijum.ukufretboard.viewmodel.UkuleleString

/**
 * Full-screen practice view for a chord progression.
 *
 * Shows the progression chords in a timeline, a fretboard diagram that
 * updates to the current chord, a voicing selector with left/right arrows,
 * and playback controls (BPM, beats per chord, loop, play/stop).
 *
 * Accessed from the Progressions tab via a "Practice" action on each card,
 * following the same content-replacement pattern as VoiceLeadingView.
 *
 * @param progression The chord progression to practice.
 * @param keyRoot The root pitch class of the selected key.
 * @param tuning Current ukulele tuning for voicing generation and fretboard.
 * @param lastFret The highest fret shown on the fretboard (from user settings).
 * @param leftHanded Whether to mirror diagrams for left-handed players.
 * @param onPlayVoicing Callback to play a single chord voicing (null if sound disabled).
 * @param onBack Callback to exit the practice view.
 * @param modifier Optional modifier.
 */
@Composable
fun ProgressionPracticeView(
    progression: Progression,
    keyRoot: Int,
    tuning: List<UkuleleString>,
    lastFret: Int = 12,
    leftHanded: Boolean = false,
    onPlayVoicing: ((ChordVoicing) -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val engine = remember { MetronomeEngine() }

    // Playback state
    var isPlaying by remember { mutableStateOf(false) }
    var bpm by remember { mutableFloatStateOf(100f) }
    var beatsPerChord by remember { mutableIntStateOf(4) }
    var loop by remember { mutableStateOf(true) }
    var currentChordIndex by remember { mutableIntStateOf(0) }

    // All voicings per chord degree (list of lists)
    val allVoicings: List<List<ChordVoicing>> = remember(progression, keyRoot, tuning) {
        progression.degrees.map { degree ->
            val chordRoot = (keyRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
            val formula = ChordFormulas.ALL.firstOrNull { it.symbol == degree.quality }
            if (formula != null) {
                VoicingGenerator.generate(chordRoot, formula, tuning)
            } else {
                emptyList()
            }
        }
    }

    // Selected voicing index per chord position (defaults to 0)
    val selectedVoicingIndex = remember { mutableStateMapOf<Int, Int>() }

    // Helper to get the selected voicing for a chord index
    fun voicingFor(chordIdx: Int): ChordVoicing? {
        val voicings = allVoicings.getOrNull(chordIdx) ?: return null
        if (voicings.isEmpty()) return null
        val idx = (selectedVoicingIndex[chordIdx] ?: 0).coerceIn(0, voicings.lastIndex)
        return voicings[idx]
    }

    // Current voicing for the displayed chord
    val currentVoicing = voicingFor(currentChordIndex)
    val currentVoicingCount = allVoicings.getOrNull(currentChordIndex)?.size ?: 0
    val currentVoicingIdx = (selectedVoicingIndex[currentChordIndex] ?: 0)
        .coerceIn(0, (currentVoicingCount - 1).coerceAtLeast(0))

    // Build selections map for FretboardView from the current voicing
    // Muted strings (MUTED = -1) map to null (unplayed)
    val selections: Map<Int, Int?> = if (currentVoicing != null) {
        currentVoicing.frets.mapIndexed { stringIdx, fret ->
            stringIdx to (if (fret == ChordVoicing.MUTED) null else fret)
        }.toMap()
    } else {
        tuning.indices.associateWith { null }
    }

    // getNoteAt for the fretboard
    val getNoteAt: (Int, Int) -> Note = { stringIndex, fret ->
        val pc = (tuning[stringIndex].openPitchClass + fret) % Notes.PITCH_CLASS_COUNT
        Note(pitchClass = pc, name = Notes.pitchClassToName(pc))
    }

    // Clean up metronome on dispose
    DisposableEffect(Unit) {
        onDispose {
            engine.stop()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp),
    ) {
        // ── Header: Back + progression name ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    engine.stop()
                    isPlaying = false
                    onBack()
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Practice",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = progression.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            // Key label
            Text(
                text = "Key: ${Notes.enharmonicForKey(keyRoot, keyRoot)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Chord timeline ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            progression.degrees.forEachIndexed { index, degree ->
                val chordRoot = (keyRoot + degree.interval) % Notes.PITCH_CLASS_COUNT
                val chordName = Notes.enharmonicForKey(chordRoot, keyRoot) + degree.quality
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
                        .clickable(enabled = !isPlaying) {
                            currentChordIndex = index
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = chordName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text(
                            text = degree.numeral,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Fretboard ──
        FretboardView(
            tuning = tuning,
            selections = selections,
            showNoteNames = true,
            onFretTap = { _, _ -> }, // read-only
            getNoteAt = getNoteAt,
            leftHanded = leftHanded,
            lastFret = lastFret,
            cellWidth = 48.dp,
            cellHeight = 42.dp,
            scrollable = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Voicing selector ──
        if (currentVoicingCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(
                    onClick = {
                        val cur = selectedVoicingIndex[currentChordIndex] ?: 0
                        val newIdx = if (cur > 0) cur - 1 else currentVoicingCount - 1
                        selectedVoicingIndex[currentChordIndex] = newIdx
                    },
                    enabled = currentVoicingCount > 1,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous voicing",
                    )
                }

                Text(
                    text = "Voicing ${currentVoicingIdx + 1} of $currentVoicingCount",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(140.dp),
                )

                IconButton(
                    onClick = {
                        val cur = selectedVoicingIndex[currentChordIndex] ?: 0
                        val newIdx = if (cur < currentVoicingCount - 1) cur + 1 else 0
                        selectedVoicingIndex[currentChordIndex] = newIdx
                    },
                    enabled = currentVoicingCount > 1,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next voicing",
                    )
                }
            }
        } else {
            Text(
                text = "No voicing available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Controls card ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // BPM slider + Tap Tempo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "BPM: ${bpm.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(64.dp),
                    )
                    Slider(
                        value = bpm,
                        onValueChange = { bpm = it },
                        valueRange = 40f..220f,
                        steps = 0,
                        modifier = Modifier.weight(1f),
                    )
                    TapTempoButton(
                        onBpmDetected = { detected -> bpm = detected.toFloat() },
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Beats per chord
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Beats:",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    listOf(1, 2, 4, 8).forEach { beats ->
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

                // Loop + Play / Stop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Loop toggle
                    FilterChip(
                        selected = loop,
                        onClick = { loop = !loop },
                        label = { Text("Loop") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Toggle loop",
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.height(30.dp),
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Play / Stop
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                engine.stop()
                                isPlaying = false
                            } else {
                                isPlaying = true
                                var lastChordPlayed = -1
                                engine.start(
                                    scope = scope,
                                    bpm = bpm.toInt(),
                                    beatsPerChord = beatsPerChord,
                                    chordCount = progression.degrees.size,
                                    loop = loop,
                                    onBeat = { chordIdx, _ ->
                                        currentChordIndex = chordIdx
                                        if (chordIdx != lastChordPlayed) {
                                            lastChordPlayed = chordIdx
                                            val voicing = voicingFor(chordIdx)
                                            if (voicing != null && onPlayVoicing != null) {
                                                onPlayVoicing(voicing)
                                            }
                                        }
                                    },
                                    onComplete = {
                                        isPlaying = false
                                    },
                                )
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
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
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            tint = if (isPlaying) {
                                MaterialTheme.colorScheme.onError
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
