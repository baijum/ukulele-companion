package com.baijum.ukufretboard.ui.songbook

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.data.ChordColorOption
import com.baijum.ukufretboard.data.ChordDisplayStyle
import com.baijum.ukufretboard.data.ChordParser

private val CHORD_BLUE = Color(0xFF1565C0)
private val CHORD_GREEN = Color(0xFF2E7D32)
private val CHORD_ORANGE = Color(0xFFE65100)
private val CHORD_PURPLE = Color(0xFF6A1B9A)

/** Keeps the red option readable against the sheet rather than alarming. */
private const val RED_CHORD_ALPHA = 0.85f

/**
 * Renders one line of a ChordPro chord sheet: a section heading, a blank spacer,
 * or lyrics with their chords either on a row above or inline in `[C]` form.
 *
 * Shared by [SheetViewer] and [PerformanceModeView] so both show the same formatted
 * sheet. Fullscreen used to print each line verbatim, so the reader saw raw `[Em]`
 * markers and lost chord colouring, headings and tap targets (issue #520).
 */
@Composable
internal fun ChordSheetLine(
    line: String,
    isSectionHeading: Boolean,
    textStyle: TextStyle,
    chordDisplayStyle: ChordDisplayStyle,
    chordColor: ChordColorOption,
    onChordTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val segments = remember(line) { ChordParser.parseLine(line) }
    val resolvedChordColor = resolveChordColor(chordColor)
    val lyricStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface)

    Column(modifier = modifier) {
        if (isSectionHeading) {
            Text(
                text = line.trim().removePrefix("[").removeSuffix("]"),
                style =
                    textStyle.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                    ),
                modifier =
                    Modifier
                        .padding(top = 8.dp, bottom = 4.dp)
                        .semantics { heading() },
            )
            return@Column
        }

        if (segments.isEmpty()) {
            Text(text = " ", style = textStyle)
            return@Column
        }

        val hasChords = segments.any { it is ChordParser.TextSegment.Chord }

        when {
            hasChords && chordDisplayStyle == ChordDisplayStyle.INLINE -> {
                Text(
                    text = inlineChordLine(segments, resolvedChordColor, onChordTap),
                    style = lyricStyle,
                )
            }

            hasChords -> {
                Text(
                    text = chordRow(segments, resolvedChordColor, onChordTap),
                    style = textStyle,
                )
                Text(text = lyricsOf(segments), style = lyricStyle)
            }

            else -> {
                Text(text = lyricsOf(segments), style = lyricStyle)
            }
        }
    }
}

/** The lyrics with every `[C]` marker stripped out. */
private fun lyricsOf(segments: List<ChordParser.TextSegment>): String =
    segments
        .filterIsInstance<ChordParser.TextSegment.PlainText>()
        .joinToString("") { it.text }

/**
 * The chords-above-lyrics row: each chord padded out to the column of the syllable it
 * lands on, so it lines up with the monospace lyric line rendered underneath.
 */
private fun chordRow(
    segments: List<ChordParser.TextSegment>,
    color: Color,
    onChordTap: (String) -> Unit,
): AnnotatedString =
    buildAnnotatedString {
        var lyricColumn = 0
        var cursor = 0
        segments.forEach { segment ->
            when (segment) {
                is ChordParser.TextSegment.PlainText -> {
                    lyricColumn += segment.text.length
                }

                is ChordParser.TextSegment.Chord -> {
                    if (lyricColumn > cursor) {
                        append(" ".repeat(lyricColumn - cursor))
                        cursor = lyricColumn
                    }
                    withLink(chordLink(segment.name, color, onChordTap)) {
                        append(segment.name)
                    }
                    cursor += segment.name.length
                }
            }
        }
    }

/** The lyric line with the `[C]` markers kept in place, each one tappable. */
private fun inlineChordLine(
    segments: List<ChordParser.TextSegment>,
    color: Color,
    onChordTap: (String) -> Unit,
): AnnotatedString =
    buildAnnotatedString {
        segments.forEach { segment ->
            when (segment) {
                is ChordParser.TextSegment.PlainText -> {
                    append(segment.text)
                }

                is ChordParser.TextSegment.Chord -> {
                    withLink(chordLink(segment.name, color, onChordTap)) {
                        append("[${segment.name}]")
                    }
                }
            }
        }
    }

/**
 * Chord names are links rather than separate clickables: links keep the chord row a
 * single [Text], which is what preserves the column alignment with the lyrics.
 */
private fun chordLink(
    name: String,
    color: Color,
    onChordTap: (String) -> Unit,
): LinkAnnotation.Clickable =
    LinkAnnotation.Clickable(
        tag = name,
        styles =
            TextLinkStyles(
                style = SpanStyle(color = color, fontWeight = FontWeight.Bold),
            ),
        linkInteractionListener = { onChordTap(name) },
    )

/** The colour the user picked in Settings for chord names. */
@Composable
@ReadOnlyComposable
internal fun resolveChordColor(option: ChordColorOption): Color =
    when (option) {
        ChordColorOption.THEME -> MaterialTheme.colorScheme.primary
        ChordColorOption.RED -> MaterialTheme.colorScheme.error.copy(alpha = RED_CHORD_ALPHA)
        ChordColorOption.BLUE -> CHORD_BLUE
        ChordColorOption.GREEN -> CHORD_GREEN
        ChordColorOption.ORANGE -> CHORD_ORANGE
        ChordColorOption.PURPLE -> CHORD_PURPLE
    }
