package com.baijum.ukufretboard.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.SrsCard
import com.baijum.ukufretboard.data.SrsCardRepository
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.domain.SrsScheduler

/**
 * SRS Practice view — reviews chord voicings using spaced repetition.
 *
 * Shows due cards one at a time. The user sees the chord name, tries to
 * recall the voicing, then reveals the diagram and grades their recall.
 * The SM-2 algorithm schedules the next review based on the grade.
 *
 * @param repository The SRS card repository for persistence.
 * @param leftHanded Whether to mirror chord diagrams.
 * @param onPlayVoicing Callback to play a voicing's audio.
 */
@Composable
fun SrsPracticeView(
    repository: SrsCardRepository,
    leftHanded: Boolean = false,
    onPlayVoicing: ((ChordVoicing) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var dueCards by remember { mutableStateOf(repository.getDueCards()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }
    var sessionReviewed by remember { mutableIntStateOf(0) }
    val totalCards = remember { repository.totalCount() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.srs_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.srs_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Stats summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
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
                        text = "${dueCards.size}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.srs_due),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$sessionReviewed",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.srs_reviewed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalCards",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.srs_total),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (dueCards.isEmpty()) {
            // No cards due
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (totalCards == 0) {
                        Text(
                            text = stringResource(R.string.srs_no_cards),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.srs_no_cards_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.srs_all_caught_up),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (sessionReviewed > 0) {
                                stringResource(
                                    R.string.srs_session_done,
                                    sessionReviewed,
                                    if (sessionReviewed == 1) stringResource(R.string.srs_card_singular) else stringResource(R.string.srs_card_plural),
                                )
                            } else {
                                stringResource(R.string.srs_no_due)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            val card = dueCards.getOrNull(currentIndex)
            if (card != null) {
                // Review card
                ReviewCard(
                    card = card,
                    showAnswer = showAnswer,
                    leftHanded = leftHanded,
                    onShowAnswer = { showAnswer = true },
                    onGrade = { grade ->
                        repository.recordReview(card.id, grade)
                        sessionReviewed++
                        showAnswer = false

                        // Move to next card or refresh the list
                        if (currentIndex < dueCards.lastIndex) {
                            currentIndex++
                        } else {
                            dueCards = repository.getDueCards()
                            currentIndex = 0
                        }
                    },
                    onPlayVoicing = onPlayVoicing,
                    remainingCount = dueCards.size - currentIndex,
                )
            }
        }
    }
}

/**
 * A single review card showing the chord name with reveal-and-grade functionality.
 */
@Composable
private fun ReviewCard(
    card: SrsCard,
    showAnswer: Boolean,
    leftHanded: Boolean,
    onShowAnswer: () -> Unit,
    onGrade: (SrsScheduler.Grade) -> Unit,
    onPlayVoicing: ((ChordVoicing) -> Unit)?,
    remainingCount: Int,
) {
    // Card front: chord name
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
                text = "Card ${remainingCount} remaining",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = card.chordName,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            Text(
                text = stringResource(R.string.srs_recall_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!showAnswer) {
                // Show answer button
                Button(
                    onClick = onShowAnswer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.srs_show_voicing))
                }
            } else {
                // Reveal the chord diagram
                val voicing = ChordVoicing(
                    frets = card.frets,
                    notes = emptyList(), // Diagram doesn't need notes
                    minFret = card.frets.filter { it > 0 }.minOrNull() ?: 0,
                    maxFret = card.frets.maxOrNull() ?: 0,
                )
                VerticalChordDiagram(
                    voicing = voicing,
                    onClick = { onPlayVoicing?.invoke(voicing) },
                    leftHanded = leftHanded,
                )

                Spacer(modifier = Modifier.height(8.dp))

                val fretsString = card.frets.joinToString(" ")
                Text(
                    text = stringResource(R.string.srs_frets, fretsString),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Grade buttons
                Text(
                    text = stringResource(R.string.srs_how_well),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SrsScheduler.Grade.entries.forEach { grade ->
                        val colors = when (grade) {
                            SrsScheduler.Grade.AGAIN -> ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            )
                            SrsScheduler.Grade.HARD -> ButtonDefaults.outlinedButtonColors()
                            SrsScheduler.Grade.GOOD -> ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            )
                            SrsScheduler.Grade.EASY -> ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary,
                            )
                        }
                        if (grade == SrsScheduler.Grade.HARD) {
                            OutlinedButton(
                                onClick = { onGrade(grade) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(grade.label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onGrade(grade) },
                                modifier = Modifier.weight(1f),
                                colors = colors,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(grade.label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
