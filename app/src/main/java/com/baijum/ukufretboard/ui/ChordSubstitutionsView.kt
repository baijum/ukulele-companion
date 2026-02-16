package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ChordSubstitutions
import com.baijum.ukufretboard.data.Notes
import com.baijum.ukufretboard.data.SubstitutionCategory

/**
 * Educational guide about chord substitution techniques.
 *
 * Displays 5 categories of substitutions with explanations and examples,
 * organized from simple (diatonic subs) to advanced (secondary dominants).
 */
@Composable
fun ChordSubstitutionsView(
    modifier: Modifier = Modifier,
) {
    var selectedKey by remember { mutableIntStateOf(0) } // C = 0 (default)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.chord_subs_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.chord_subs_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Key selector
        Text(
            text = stringResource(R.string.chord_subs_examples_in),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            (0..11).forEach { pc ->
                val name = Notes.pitchClassToName(pc)
                FilterChip(
                    selected = selectedKey == pc,
                    onClick = { selectedKey = pc },
                    label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display all categories
        ChordSubstitutions.CATEGORIES.forEachIndexed { index, category ->
            SubstitutionCategoryCard(category = category, transposition = selectedKey)
            if (index < ChordSubstitutions.CATEGORIES.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tip card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.chord_subs_practice_tip_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.chord_subs_practice_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/**
 * Transposes a chord name from C (pitchClass 0) to the target key.
 *
 * Handles names like "C", "Am", "G7", "Db7", "Fm", "Bdim".
 */
private fun transposeChordName(name: String, targetKey: Int): String {
    if (name == "\u2014" || name.isBlank()) return name // em-dash or empty
    // Extract root note (letter + optional # or b)
    val root = name.takeWhile { it.isLetter() || it == '#' || it == 'b' }
    val quality = name.removePrefix(root)
    val rootPc = Notes.NOTE_NAMES_SHARP.indexOf(root).takeIf { it >= 0 }
        ?: Notes.NOTE_NAMES_FLAT.indexOf(root).takeIf { it >= 0 }
        ?: return name // can't parse
    val transposedPc = (rootPc + targetKey) % 12
    val transposedName = Notes.pitchClassToName(transposedPc)
    return "$transposedName$quality"
}

/**
 * Simple transposition for the "exampleInC" field.
 *
 * Replaces chord names like C, Am, G7, Db7 with their transposed equivalents.
 */
private fun transposeExample(example: String, targetKey: Int): String {
    if (targetKey == 0) return example
    // Pattern: match chord-like tokens (letter + optional #/b + optional quality suffix)
    val chordPattern = Regex("""([A-G][#b]?)(m7?|7|dim|maj7|°|\+)?""")
    return chordPattern.replace(example) { match ->
        val root = match.groupValues[1]
        val quality = match.groupValues[2]
        val rootPc = Notes.NOTE_NAMES_SHARP.indexOf(root).takeIf { it >= 0 }
            ?: Notes.NOTE_NAMES_FLAT.indexOf(root).takeIf { it >= 0 }
            ?: return@replace match.value
        val transposedPc = (rootPc + targetKey) % 12
        "${Notes.pitchClassToName(transposedPc)}$quality"
    }
}

@Composable
private fun SubstitutionCategoryCard(category: SubstitutionCategory, transposition: Int = 0) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = category.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Substitution examples
            category.substitutions.forEachIndexed { index, sub ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = sub.original,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "\u2192",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = sub.substitute,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (sub.sharedNotes != "\u2014") {
                            Text(
                                text = stringResource(R.string.chord_subs_shared, transposeExample(sub.sharedNotes, transposition)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = transposeExample(sub.exampleInC, transposition),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < category.substitutions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}
