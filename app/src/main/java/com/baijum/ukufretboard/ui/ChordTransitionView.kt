package com.baijum.ukufretboard.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.audio.MetronomeEngine
import com.baijum.ukufretboard.data.ChordFormulas
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.VoicingGenerator
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.domain.Note
import com.baijum.ukufretboard.viewmodel.FretboardViewModel
import com.baijum.ukufretboard.viewmodel.UkuleleString

/**
 * Chord Transition Trainer — practise switching between two chords with a metronome.
 *
 * Displays two chord diagrams side by side, highlights the active chord on
 * each beat, and counts successful transitions. The user practises switching
 * between the two chords in time with the metronome.
 *
 * @param tuning Current ukulele tuning.
 * @param lastFret The highest fret shown.
 * @param leftHanded Whether to mirror for left-handed play.
 * @param onPlayVoicing Callback to play a chord voicing.
 */
@Composable
fun ChordTransitionView(
    tuning: List<UkuleleString> = FretboardViewModel.STANDARD_TUNING,
    lastFret: Int = 12,
    leftHanded: Boolean = false,
    onPlayVoicing: ((ChordVoicing) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val engine = remember { MetronomeEngine() }

    // Chord selection state
    var rootA by remember { mutableIntStateOf(0) } // C
    var formulaIndexA by remember { mutableIntStateOf(0) } // Major
    var rootB by remember { mutableIntStateOf(7) } // G
    var formulaIndexB by remember { mutableIntStateOf(0) } // Major

    // Playback state
    var isPlaying by remember { mutableStateOf(false) }
    var bpm by remember { mutableFloatStateOf(60f) }
    var beatsPerChord by remember { mutableIntStateOf(4) }
    var currentChord by remember { mutableIntStateOf(0) } // 0 = A, 1 = B
    var transitionCount by remember { mutableIntStateOf(0) }
    var totalBeats by remember { mutableIntStateOf(0) }
    var sessionStartTime by remember { mutableLongStateOf(0L) }

    // Simple formulas for the selector
    val commonFormulas = remember {
        listOf(
            ChordFormulas.ALL.first { it.symbol == "" },       // Major
            ChordFormulas.ALL.first { it.symbol == "m" },      // Minor
            ChordFormulas.ALL.first { it.symbol == "7" },      // Dominant 7th
            ChordFormulas.ALL.first { it.symbol == "m7" },     // Minor 7th
        )
    }

    // Generate voicings
    val voicingA = remember(rootA, formulaIndexA, tuning) {
        val formula = commonFormulas[formulaIndexA]
        VoicingGenerator.generate(rootA, formula, tuning).firstOrNull()
    }
    val voicingB = remember(rootB, formulaIndexB, tuning) {
        val formula = commonFormulas[formulaIndexB]
        VoicingGenerator.generate(rootB, formula, tuning).firstOrNull()
    }

    val chordNameA = Notes.pitchClassToName(rootA) + commonFormulas[formulaIndexA].symbol
    val chordNameB = Notes.pitchClassToName(rootB) + commonFormulas[formulaIndexB].symbol

    // Clean up metronome on dispose
    DisposableEffect(Unit) {
        onDispose { engine.stop() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Chord Transition Trainer",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Practice switching between two chords in time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Chord A selector ─────────────────────────────────────────
        ChordSelector(
            label = "Chord A",
            selectedRoot = rootA,
            selectedFormulaIndex = formulaIndexA,
            formulas = commonFormulas,
            isActive = currentChord == 0 && isPlaying,
            onRootChange = { rootA = it },
            onFormulaChange = { formulaIndexA = it },
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Swap button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(onClick = {
                val tempRoot = rootA
                val tempFormula = formulaIndexA
                rootA = rootB
                formulaIndexA = formulaIndexB
                rootB = tempRoot
                formulaIndexB = tempFormula
            }) {
                Icon(
                    Icons.Filled.SwapHoriz,
                    contentDescription = "Swap chords",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // ── Chord B selector ─────────────────────────────────────────
        ChordSelector(
            label = "Chord B",
            selectedRoot = rootB,
            selectedFormulaIndex = formulaIndexB,
            formulas = commonFormulas,
            isActive = currentChord == 1 && isPlaying,
            onRootChange = { rootB = it },
            onFormulaChange = { formulaIndexB = it },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Chord diagrams side by side ──────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = chordNameA,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (currentChord == 0 && isPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (voicingA != null) {
                    VerticalChordDiagram(
                        voicing = voicingA,
                        onClick = {
                            onPlayVoicing?.invoke(voicingA)
                        },
                        leftHanded = leftHanded,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = chordNameB,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (currentChord == 1 && isPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (voicingB != null) {
                    VerticalChordDiagram(
                        voicing = voicingB,
                        onClick = {
                            onPlayVoicing?.invoke(voicingB)
                        },
                        leftHanded = leftHanded,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Session stats ────────────────────────────────────────────
        if (isPlaying || transitionCount > 0) {
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
                            text = "$transitionCount",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Switches",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val elapsed = if (sessionStartTime > 0) {
                            (System.currentTimeMillis() - sessionStartTime) / 1000
                        } else 0
                        Text(
                            text = "${elapsed}s",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val elapsed = if (sessionStartTime > 0) {
                            (System.currentTimeMillis() - sessionStartTime) / 1000
                        } else 0
                        val switchesPerMin = if (elapsed > 0) {
                            (transitionCount * 60 / elapsed).toInt()
                        } else 0
                        Text(
                            text = "$switchesPerMin",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Per min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Controls card ────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                        valueRange = 30f..180f,
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

                // Play / Stop + Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Reset
                    IconButton(
                        onClick = {
                            engine.stop()
                            isPlaying = false
                            transitionCount = 0
                            totalBeats = 0
                            currentChord = 0
                            sessionStartTime = 0L
                        },
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Play / Stop
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                engine.stop()
                                isPlaying = false
                            } else {
                                isPlaying = true
                                transitionCount = 0
                                totalBeats = 0
                                currentChord = 0
                                sessionStartTime = System.currentTimeMillis()

                                var lastChordPlayed = -1
                                engine.start(
                                    scope = scope,
                                    bpm = bpm.toInt(),
                                    beatsPerChord = beatsPerChord,
                                    chordCount = 2,
                                    loop = true,
                                    onBeat = { chordIdx, beat ->
                                        currentChord = chordIdx
                                        totalBeats++
                                        if (chordIdx != lastChordPlayed) {
                                            lastChordPlayed = chordIdx
                                            transitionCount++
                                            // Play the chord voicing
                                            val voicing = if (chordIdx == 0) voicingA else voicingB
                                            if (voicing != null && onPlayVoicing != null) {
                                                onPlayVoicing(voicing)
                                            }
                                        }
                                    },
                                )
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
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
                            contentDescription = if (isPlaying) "Stop" else "Start",
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

/**
 * Root note and quality selector for a chord.
 */
@Composable
private fun ChordSelector(
    label: String,
    selectedRoot: Int,
    selectedFormulaIndex: Int,
    formulas: List<com.baijum.ukufretboard.data.ChordFormula>,
    isActive: Boolean,
    onRootChange: (Int) -> Unit,
    onFormulaChange: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Root note selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Notes.NOTE_NAMES_STANDARD.forEachIndexed { index, name ->
                    FilterChip(
                        selected = selectedRoot == index,
                        onClick = { onRootChange(index) },
                        label = {
                            Text(
                                name,
                                fontWeight = if (selectedRoot == index) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quality selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                formulas.forEachIndexed { index, formula ->
                    FilterChip(
                        selected = selectedFormulaIndex == index,
                        onClick = { onFormulaChange(index) },
                        label = { Text(formula.quality) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    )
                }
            }
        }
    }
}
