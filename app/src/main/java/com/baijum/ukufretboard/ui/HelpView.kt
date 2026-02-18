package com.baijum.ukufretboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R

/**
 * A single help entry with a feature title and its description.
 */
private data class HelpEntry(
    val title: String,
    val description: String,
)

/**
 * A group of related help entries under a section header.
 */
private data class HelpSection(
    val title: String,
    val entries: List<HelpEntry>,
)

/** Builds help content organised by the same section groupings as the navigation drawer. */
@Composable
private fun helpSections(): List<HelpSection> = listOf(
    HelpSection(
        title = stringResource(R.string.help_section_play),
        entries = listOf(
            HelpEntry(
                title = stringResource(R.string.help_explorer),
                description = stringResource(R.string.help_desc_explorer),
            ),
            HelpEntry(
                title = stringResource(R.string.help_tuner),
                description = stringResource(R.string.help_desc_tuner),
            ),
            HelpEntry(
                title = stringResource(R.string.help_pitch_monitor),
                description = stringResource(R.string.help_desc_pitch_monitor),
            ),
            HelpEntry(
                title = stringResource(R.string.help_chords),
                description = stringResource(R.string.help_desc_chords),
            ),
            HelpEntry(
                title = stringResource(R.string.help_favorites),
                description = stringResource(R.string.help_desc_favorites),
            ),
        ),
    ),
    HelpSection(
        title = stringResource(R.string.help_section_create),
        entries = listOf(
            HelpEntry(
                title = stringResource(R.string.help_songs),
                description = stringResource(R.string.help_desc_songs),
            ),
            HelpEntry(
                title = stringResource(R.string.help_melody_notepad),
                description = stringResource(R.string.help_desc_melody_notepad),
            ),
            HelpEntry(
                title = stringResource(R.string.help_patterns),
                description = stringResource(R.string.help_desc_patterns),
            ),
            HelpEntry(
                title = stringResource(R.string.help_progressions),
                description = stringResource(R.string.help_desc_progressions),
            ),
        ),
    ),
    HelpSection(
        title = stringResource(R.string.help_section_learn),
        entries = listOf(
            HelpEntry(
                title = stringResource(R.string.help_learn_theory),
                description = stringResource(R.string.help_desc_learn_theory),
            ),
            HelpEntry(
                title = stringResource(R.string.help_theory_quiz),
                description = stringResource(R.string.help_desc_theory_quiz),
            ),
            HelpEntry(
                title = stringResource(R.string.help_interval_trainer),
                description = stringResource(R.string.help_desc_interval_trainer),
            ),
            HelpEntry(
                title = stringResource(R.string.help_note_quiz),
                description = stringResource(R.string.help_desc_note_quiz),
            ),
            HelpEntry(
                title = stringResource(R.string.help_chord_ear),
                description = stringResource(R.string.help_desc_chord_ear),
            ),
            HelpEntry(
                title = stringResource(R.string.help_scale_practice),
                description = stringResource(R.string.help_desc_scale_practice),
            ),
            HelpEntry(
                title = stringResource(R.string.help_progress),
                description = stringResource(R.string.help_desc_progress),
            ),
            HelpEntry(
                title = stringResource(R.string.help_daily_challenge),
                description = stringResource(R.string.help_desc_daily_challenge),
            ),
            HelpEntry(
                title = stringResource(R.string.help_practice_routine),
                description = stringResource(R.string.help_desc_practice_routine),
            ),

            HelpEntry(
                title = stringResource(R.string.help_chord_transitions),
                description = stringResource(R.string.help_desc_chord_transitions),
            ),
            HelpEntry(
                title = stringResource(R.string.help_play_along),
                description = stringResource(R.string.help_desc_play_along),
            ),
            HelpEntry(
                title = stringResource(R.string.help_achievements),
                description = stringResource(R.string.help_desc_achievements),
            ),
        ),
    ),
    HelpSection(
        title = stringResource(R.string.help_section_reference),
        entries = listOf(
            HelpEntry(
                title = stringResource(R.string.help_capo_guide),
                description = stringResource(R.string.help_desc_capo_guide),
            ),
            HelpEntry(
                title = stringResource(R.string.help_circle_of_fifths),
                description = stringResource(R.string.help_desc_circle_of_fifths),
            ),
            HelpEntry(
                title = stringResource(R.string.help_chord_subs),
                description = stringResource(R.string.help_desc_chord_subs),
            ),
            HelpEntry(
                title = stringResource(R.string.help_chords_in_scale),
                description = stringResource(R.string.help_desc_chords_in_scale),
            ),
            HelpEntry(
                title = stringResource(R.string.help_fretboard_notes),
                description = stringResource(R.string.help_desc_fretboard_notes),
            ),
            HelpEntry(
                title = stringResource(R.string.help_glossary),
                description = stringResource(R.string.help_desc_glossary),
            ),
        ),
    ),
    HelpSection(
        title = stringResource(R.string.help_section_other),
        entries = listOf(
            HelpEntry(
                title = stringResource(R.string.help_settings),
                description = stringResource(R.string.help_desc_settings),
            ),
            HelpEntry(
                title = stringResource(R.string.help_sharing),
                description = stringResource(R.string.help_desc_sharing),
            ),
            HelpEntry(
                title = stringResource(R.string.help_full_screen),
                description = stringResource(R.string.help_desc_full_screen),
            ),
        ),
    ),
)

/**
 * Help page listing every feature in the app with expandable descriptions.
 *
 * Content is grouped into the same sections as the navigation drawer
 * (Play, Create, Learn, Reference) plus an "Other" section for settings
 * and miscellaneous features.
 */
@Composable
fun HelpView(
    modifier: Modifier = Modifier,
) {
    var expandedEntry by remember { mutableStateOf<String?>(null) }
    val sections = helpSections()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.help_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.help_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        sections.forEach { section ->
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 6.dp).semantics { heading() },
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    section.entries.forEachIndexed { index, entry ->
                        HelpItem(
                            entry = entry,
                            isExpanded = expandedEntry == "${section.title}/${entry.title}",
                            onToggle = {
                                val key = "${section.title}/${entry.title}"
                                expandedEntry = if (expandedEntry == key) null else key
                            },
                        )
                        if (index < section.entries.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * A single expandable help entry showing the feature title and its description.
 */
@Composable
private fun HelpItem(
    entry: HelpEntry,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = entry.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )

        AnimatedVisibility(visible = isExpanded) {
            Column {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!isExpanded) {
            Text(
                text = entry.description.take(60) +
                    if (entry.description.length > 60) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
