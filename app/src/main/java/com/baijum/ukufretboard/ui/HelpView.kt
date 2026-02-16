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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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

/** All help content organised by the same section groupings as the navigation drawer. */
private val HELP_SECTIONS = listOf(
    HelpSection(
        title = "Play",
        entries = listOf(
            HelpEntry(
                title = "Explorer",
                description = "The interactive fretboard where you tap frets to select notes. " +
                    "The app automatically detects what chord you are playing and displays " +
                    "its name, quality, constituent notes, intervals, formula, suggested " +
                    "fingering, and difficulty rating.\n\n" +
                    "Use the Play button to hear how the chord sounds and the Share button " +
                    "to export the chord diagram as an image.\n\n" +
                    "Toggle the Scale overlay to highlight scale notes on the fretboard, " +
                    "or tap the expand icon for a full-screen landscape view.",
            ),
            HelpEntry(
                title = "Tuner",
                description = "A chromatic tuner that uses your device's microphone to detect " +
                    "the pitch of a string as you pluck it. The display shows the closest " +
                    "note name and a visual indicator of whether you are sharp (too high) " +
                    "or flat (too low). Tune each string until the indicator is centred.\n\n" +
                    "Microphone permission is required for this feature.",
            ),
            HelpEntry(
                title = "Pitch Monitor",
                description = "A real-time pitch visualisation tool that continuously listens " +
                    "through your device's microphone and displays the detected pitch on a " +
                    "scrolling graph.\n\n" +
                    "Useful for monitoring your intonation while playing or singing along " +
                    "with the ukulele. Microphone permission is required.",
            ),
            HelpEntry(
                title = "Chords",
                description = "A comprehensive chord library. Select a root note (C, C#, D, ...), " +
                    "a category (Triad, Seventh, Suspended, Extended), and a specific chord " +
                    "formula to browse all playable voicings on the ukulele.\n\n" +
                    "Tap a voicing card to load it onto the Explorer fretboard. Use the " +
                    "heart icon to save it to Favorites. Long-press a card to share it as " +
                    "an image.\n\n" +
                    "Additional tools: filter by inversion, compare inversions side-by-side, " +
                    "capo calculator, and capo visualiser.",
            ),
            HelpEntry(
                title = "Favorites",
                description = "Your saved chord voicings. Tap the heart icon on any chord in " +
                    "the Chords library to add it here.\n\n" +
                    "Organise voicings into folders for quick access. Long-press a card to " +
                    "share the chord diagram as an image. Tap a card to load it onto the " +
                    "Explorer fretboard.",
            ),
            HelpEntry(
                title = "Song Finder",
                description = "Discover songs you can already play based on the chords in your " +
                    "Favorites. The app matches your known chords against a built-in song " +
                    "database.\n\n" +
                    "Songs are split into two lists: ones you can play right now using only " +
                    "chords you know, and ones that are almost playable (missing just 1\u20132 " +
                    "chords). Tap a missing chord to jump to its voicings in the library.",
            ),
        ),
    ),
    HelpSection(
        title = "Create",
        entries = listOf(
            HelpEntry(
                title = "Songs",
                description = "A personal songbook for writing and storing chord sheets. " +
                    "Type lyrics with chords in bracket notation, e.g. [Am] and [G], and " +
                    "the app formats them with chords above the lyrics.\n\n" +
                    "Features include:\n" +
                    "\u2022 Auto-scroll \u2014 hands-free scrolling at adjustable speed " +
                    "(0.5x, 1x, 2x, or 3x) with pause and stop controls.\n" +
                    "\u2022 Transpose \u2014 shift all chords up or down by semitones, with " +
                    "an equivalent capo fret suggestion displayed automatically.\n" +
                    "\u2022 Key detection \u2014 the app analyses the chords and displays " +
                    "the detected musical key of the song.\n" +
                    "\u2022 ChordPro import \u2014 import .chopro, .cho, and .chordpro files " +
                    "from your device.\n" +
                    "\u2022 ChordPro export \u2014 export any song to ChordPro format for use " +
                    "in other apps.\n" +
                    "\u2022 Share as text \u2014 send chord sheets via any messaging app.",
            ),
            HelpEntry(
                title = "Melody Notepad",
                description = "A simple tool for composing melodies. Tap notes on a keyboard " +
                    "to build a sequence. Play back your melody to hear how it sounds.\n\n" +
                    "Useful for jotting down melodic ideas before you forget them.",
            ),
            HelpEntry(
                title = "Patterns",
                description = "Browse and create strumming and fingerpicking patterns. " +
                    "Each pattern shows the rhythm with down-strokes, up-strokes, and " +
                    "rests. You can also create your own custom patterns.\n\n" +
                    "Great for practising rhythm and learning new strumming styles.",
            ),
            HelpEntry(
                title = "Progressions",
                description = "A library of common chord progressions (e.g. I\u2013IV\u2013V, " +
                    "I\u2013V\u2013vi\u2013IV) in every key. Select a progression to see " +
                    "the actual chord names and voicings.\n\n" +
                    "Features include voice leading visualisation, sequential playback, " +
                    "tap tempo for setting BPM by tapping rhythmically, " +
                    "and the ability to create your own custom progressions.",
            ),
        ),
    ),
    HelpSection(
        title = "Learn",
        entries = listOf(
            HelpEntry(
                title = "Learn Theory",
                description = "Structured music theory lessons covering fundamentals like " +
                    "intervals, scales, chord construction, keys, and more.\n\n" +
                    "Work through lessons at your own pace. Your progress is tracked " +
                    "automatically.",
            ),
            HelpEntry(
                title = "Theory Quiz",
                description = "Test your knowledge of music theory concepts with multiple-choice " +
                    "questions. Topics include intervals, chord types, scale degrees, " +
                    "and key signatures.\n\n" +
                    "A great way to reinforce what you have learned in the theory lessons.",
            ),
            HelpEntry(
                title = "Interval Trainer",
                description = "Ear training for musical intervals. Listen to two notes played " +
                    "in sequence and identify the interval between them (e.g. minor 3rd, " +
                    "perfect 5th).\n\n" +
                    "Developing interval recognition is one of the most valuable ear " +
                    "training skills for any musician.",
            ),
            HelpEntry(
                title = "Note Quiz",
                description = "A timed quiz that challenges you to identify notes on the " +
                    "ukulele fretboard. The app highlights a position and you select the " +
                    "correct note name.\n\n" +
                    "Helps you memorise the fretboard layout so you can find notes quickly " +
                    "while playing.",
            ),
            HelpEntry(
                title = "Chord Ear Training",
                description = "Listen to a chord and identify its type (major, minor, " +
                    "seventh, etc.) by ear. The app plays a chord and you choose from " +
                    "multiple options.\n\n" +
                    "Trains your ear to recognise chord qualities, which is essential for " +
                    "playing by ear and transcribing songs.",
            ),
            HelpEntry(
                title = "Scale Practice",
                description = "Practice playing scales on the fretboard with visual guidance. " +
                    "Select a root note and scale type to see the scale pattern " +
                    "highlighted on the fretboard.\n\n" +
                    "Builds familiarity with scale shapes and helps develop finger dexterity " +
                    "across the neck.",
            ),
            HelpEntry(
                title = "Progress",
                description = "A dashboard showing your learning statistics across all training " +
                    "activities: theory lessons completed, quiz scores, interval trainer " +
                    "accuracy, practice time, and more.\n\n" +
                    "Includes a practice timer that automatically tracks your session " +
                    "duration. View today's minutes, total time, longest session, and " +
                    "progress towards your daily goal.",
            ),
            HelpEntry(
                title = "Daily Challenge",
                description = "Three fresh challenges generated every day to keep your " +
                    "practice varied and engaging. Challenges may include learning a new " +
                    "chord, taking a theory quiz, or practising a specific song.\n\n" +
                    "Each challenge links directly to the relevant section of the app. " +
                    "A new tip of the day is also shown for quick inspiration.",
            ),
            HelpEntry(
                title = "Practice Routine",
                description = "A guided practice session builder. Set your available time " +
                    "(5\u201360 minutes), skill level (beginner, intermediate, advanced), " +
                    "and focus areas (chords, scales, ear training, theory, songs, SRS).\n\n" +
                    "The app generates a structured routine with warm-up, focused exercises, " +
                    "and cool-down. Follow along step by step, with each step linking to " +
                    "the relevant feature in the app.",
            ),
            HelpEntry(
                title = "SRS Review",
                description = "Spaced Repetition System for memorising chord voicings. " +
                    "Add any chord voicing to your review deck, and the app schedules " +
                    "reviews at increasing intervals based on how well you remember them.\n\n" +
                    "During review, you see the chord name and try to recall the fingering " +
                    "before revealing it. Rate your recall (Again, Hard, Good, Easy) and " +
                    "the algorithm adjusts the next review date accordingly. Based on the " +
                    "proven SM-2 algorithm.",
            ),
            HelpEntry(
                title = "Chord Transitions",
                description = "Visualise how your fingers move between two chords with an " +
                    "animated fretboard diagram. Select any two chords to see each finger " +
                    "slide, lift, or place onto its new position.\n\n" +
                    "Adjustable animation speed and a step-by-step mode let you study " +
                    "transitions at your own pace. A description of each finger movement " +
                    "is shown alongside the animation.",
            ),
            HelpEntry(
                title = "Play Along",
                description = "Real-time play-along practice with audio chord detection. " +
                    "Choose a key and a chord progression, then play your ukulele while " +
                    "the app listens through the microphone.\n\n" +
                    "The app detects which chord you are playing and shows live feedback " +
                    "\u2014 a green check for correct chords and a red X for mismatches. " +
                    "A metronome keeps time, and your performance is scored with accuracy " +
                    "percentage, letter grade, and streak tracking.\n\n" +
                    "Adjust BPM with a slider or tap tempo, and choose 2, 4, or 8 beats " +
                    "per chord. Microphone permission is required.",
            ),
            HelpEntry(
                title = "Achievements",
                description = "A gallery of milestones to unlock as you use the app. " +
                    "Achievements are earned by reaching goals such as completing lessons, " +
                    "building quiz streaks, learning chords, saving favourites, and " +
                    "practising regularly.\n\n" +
                    "View your progress towards each achievement and filter by category " +
                    "(Learning, Practice, Collection, Mastery). Locked achievements are " +
                    "shown dimmed with their unlock criteria visible.",
            ),
        ),
    ),
    HelpSection(
        title = "Reference",
        entries = listOf(
            HelpEntry(
                title = "Capo Guide",
                description = "An educational guide to using a capo on the ukulele. Explains " +
                    "how a capo changes the effective key and provides a transposition " +
                    "reference chart.\n\n" +
                    "Useful when you want to play a song in a different key without " +
                    "learning new chord shapes.",
            ),
            HelpEntry(
                title = "Circle of Fifths",
                description = "An interactive circle of fifths diagram \u2014 one of the most " +
                    "important tools in music theory. Shows the relationship between all " +
                    "12 keys and their relative majors and minors.\n\n" +
                    "Tap any chord on the circle to jump directly to its voicings in the " +
                    "Chord library.",
            ),
            HelpEntry(
                title = "Chord Substitutions",
                description = "A reference guide for chord substitution options. Shows which " +
                    "chords can replace others in a progression for a different harmonic " +
                    "colour.\n\n" +
                    "Common substitutions include relative major/minor swaps, tritone " +
                    "substitutions, and secondary dominants.",
            ),
            HelpEntry(
                title = "Chords in Scale",
                description = "Select a scale and see all the chords that naturally belong to " +
                    "it (diatonic chords). For example, the C major scale contains " +
                    "C, Dm, Em, F, G, Am, and Bdim.\n\n" +
                    "Helpful for songwriting and understanding why certain chords sound " +
                    "good together.",
            ),
            HelpEntry(
                title = "Fretboard Notes",
                description = "A visual map showing every note on the ukulele fretboard. " +
                    "Use it as a quick reference to find any note at any position.\n\n" +
                    "Pair this with the Note Quiz to test and build your fretboard " +
                    "knowledge.",
            ),
            HelpEntry(
                title = "Glossary",
                description = "A searchable dictionary of music theory and ukulele terms. " +
                    "Tap any term to expand its definition and see an example.\n\n" +
                    "Look up unfamiliar terms you encounter while using the app or " +
                    "learning music theory.",
            ),
        ),
    ),
    HelpSection(
        title = "Other",
        entries = listOf(
            HelpEntry(
                title = "Settings",
                description = "Configure app-wide preferences: enable or disable sound " +
                    "playback, adjust display options, change tuning, switch to " +
                    "left-handed fretboard orientation, manage notifications, and " +
                    "back up or restore your data to a local file.",
            ),
            HelpEntry(
                title = "Sharing Chords",
                description = "Share any chord as an image suitable for messaging apps like " +
                    "WhatsApp. In the Explorer, tap the share icon next to the Play button " +
                    "when a chord is detected. In the Chords library or Favorites, " +
                    "long-press a voicing card.\n\n" +
                    "A preview appears in a bottom sheet. Tap \"Share as Image\" to open " +
                    "the Android share sheet.",
            ),
            HelpEntry(
                title = "Full Screen Mode",
                description = "Tap the expand icon in the Explorer tab to open a full-screen " +
                    "landscape fretboard. This gives you more room to see and tap frets, " +
                    "especially useful on smaller screens.\n\n" +
                    "Press the back button or the exit icon to return to the normal view.",
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Help",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Tap any feature to learn more about how it works.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        HELP_SECTIONS.forEach { section ->
            // Section header
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
