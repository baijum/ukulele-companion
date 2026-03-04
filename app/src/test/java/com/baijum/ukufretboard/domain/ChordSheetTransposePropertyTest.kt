package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Notes
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordSheetTransposePropertyTest {

    private val sampleChordSheets = listOf(
        "[C] [Am] [F] [G]",
        "Verse:\n[Em]Hello [G]world\n[C]Goodbye [D]moon",
        "[C#m7]Jazz [Bbmaj7]chords [F#]here",
        "No chords in this line",
        "[C] simple [C] repeated",
        "[Ab]Flat [Eb]keys [Bb]only",
    )

    @Test
    fun transposeByZeroIsIdentity() {
        runBlocking {
            checkAll(Arb.element(sampleChordSheets)) { content ->
                assertEquals(content, ChordSheetTranspose.transpose(content, 0))
            }
        }
    }

    @Test
    fun transposeByTwelveIsIdentity() {
        runBlocking {
            checkAll(Arb.element(sampleChordSheets)) { content ->
                assertEquals(content, ChordSheetTranspose.transpose(content, 12))
                assertEquals(content, ChordSheetTranspose.transpose(content, -12))
            }
        }
    }

    @Test
    fun doubleTransposeRoundTrip() {
        runBlocking {
            checkAll(Arb.element(sampleChordSheets), Arb.int(-11..11)) { content, semitones ->
                val transposed = ChordSheetTranspose.transpose(content, semitones)
                val roundTrip = ChordSheetTranspose.transpose(transposed, -semitones)
                assertEquals(
                    "Transpose +$semitones then -$semitones should round-trip",
                    content,
                    roundTrip,
                )
            }
        }
    }

    @Test
    fun transposePreservesNonChordText() {
        runBlocking {
            val plainTexts = listOf(
                "Just plain text",
                "Verse 1:\nLa la la",
                "",
                "  spaces  ",
                "12345 numbers",
            )
            checkAll(Arb.element(plainTexts), Arb.int(-11..11)) { text, semitones ->
                assertEquals(
                    "Plain text should be unchanged",
                    text,
                    ChordSheetTranspose.transpose(text, semitones),
                )
            }
        }
    }

    @Test
    fun transposePreservesQualitySuffix() {
        runBlocking {
            val suffixes = listOf("m", "7", "m7", "maj7", "dim", "aug", "sus4", "sus2", "add9")
            checkAll(
                Arb.element(Notes.NOTE_NAMES_SHARP),
                Arb.element(suffixes),
                Arb.int(1..11),
            ) { root, suffix, semitones ->
                val content = "[$root$suffix]"
                val transposed = ChordSheetTranspose.transpose(content, semitones)
                assertTrue(
                    "Transposed '$transposed' should still contain suffix '$suffix'",
                    transposed.contains("$suffix]"),
                )
            }
        }
    }

    @Test
    fun transposeNeverCrashesOnArbitraryContent() {
        runBlocking {
            checkAll(Arb.string(0..200), Arb.int(-100..100)) { content, semitones ->
                ChordSheetTranspose.transpose(content, semitones)
            }
        }
    }

    @Test
    fun transposedChordsHaveValidRootNames() {
        runBlocking {
            val validRoots = (Notes.NOTE_NAMES_SHARP + Notes.NOTE_NAMES_FLAT +
                Notes.NOTE_NAMES_STANDARD).toSet()
            checkAll(
                Arb.element(Notes.NOTE_NAMES_SHARP),
                Arb.int(1..11),
            ) { root, semitones ->
                val content = "[$root]"
                val transposed = ChordSheetTranspose.transpose(content, semitones)
                val newRoot = transposed.removePrefix("[").removeSuffix("]")
                assertTrue(
                    "'$newRoot' should be a valid note name",
                    newRoot in validRoots,
                )
            }
        }
    }

    @Test
    fun semitoneLabelFormat() {
        runBlocking {
            checkAll(Arb.int(-24..24)) { semitones ->
                val label = ChordSheetTranspose.semitoneLabel(semitones)
                if (semitones > 0) {
                    assertTrue("Positive should start with +", label.startsWith("+"))
                }
                assertTrue(
                    "Label should contain the number",
                    label.contains(semitones.toString().trimStart('-')),
                )
            }
        }
    }
}
