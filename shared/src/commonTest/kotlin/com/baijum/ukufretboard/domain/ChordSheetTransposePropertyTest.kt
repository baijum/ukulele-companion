package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Notes
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChordSheetTransposePropertyTest {

    private val sampleChordSheets = listOf(
        "[C] [Am] [F] [G]",
        "Verse:\n[Em]Hello [G]world\n[C]Goodbye [D]moon",
        "[C#m7]Jazz [Bbmaj7]chords [F#]here",
        "No chords in this line",
        "[C] simple [C] repeated",
        "[Ab]Flat [Eb]keys [Bb]only",
        "[D/F#]Slash [C/G]chords [Am7/G]here",
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
                    content,
                    roundTrip,
                    "Transpose +$semitones then -$semitones should round-trip",
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
                    text,
                    ChordSheetTranspose.transpose(text, semitones),
                    "Plain text should be unchanged",
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
                    transposed.contains("$suffix]"),
                    "Transposed '$transposed' should still contain suffix '$suffix'",
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
                    newRoot in validRoots,
                    "'$newRoot' should be a valid note name",
                )
            }
        }
    }

    @Test
    fun transposeSimpleSlashChord() {
        assertEquals("[D/A]", ChordSheetTranspose.transpose("[C/G]", 2))
    }

    @Test
    fun transposeQualityAndSlashChord() {
        assertEquals("[Cm7/Bb]", ChordSheetTranspose.transpose("[Am7/G]", 3))
    }

    @Test
    fun transposeSharpBassSlashChord() {
        assertEquals("[E/Ab]", ChordSheetTranspose.transpose("[D/F#]", 2))
    }

    @Test
    fun transposeFlatBassSlashChord() {
        assertEquals("[A/E]", ChordSheetTranspose.transpose("[Ab/Eb]", 1))
    }

    @Test
    fun transposeNonSlashChordUnchanged() {
        assertEquals("[Dmaj7]", ChordSheetTranspose.transpose("[Cmaj7]", 2))
    }

    @Test
    fun slashChordRoundTrip() {
        val content = "[C/G]"
        val transposed = ChordSheetTranspose.transpose(content, 5)
        assertEquals("[F/C]", transposed)
        assertEquals(content, ChordSheetTranspose.transpose(transposed, -5))
    }

    @Test
    fun sectionLabelUntouchedByTranspose() {
        assertEquals("[Chorus]", ChordSheetTranspose.transpose("[Chorus]", 2))
    }

    @Test
    fun codaUntouchedByTranspose() {
        assertEquals("[Coda]", ChordSheetTranspose.transpose("[Coda]", 2))
    }

    @Test
    fun endingUntouchedByTranspose() {
        assertEquals("[Ending]", ChordSheetTranspose.transpose("[Ending]", 2))
    }

    @Test
    fun breakUntouchedByTranspose() {
        assertEquals("[Break]", ChordSheetTranspose.transpose("[Break]", 2))
    }

    @Test
    fun fineUntouchedByTranspose() {
        assertEquals("[Fine]", ChordSheetTranspose.transpose("[Fine]", 1))
    }

    @Test
    fun bassLabelUntouchedByTranspose() {
        assertEquals("[Bass]", ChordSheetTranspose.transpose("[Bass]", 2))
    }

    @Test
    fun drumSoloUntouchedByTranspose() {
        assertEquals("[Drum Solo]", ChordSheetTranspose.transpose("[Drum Solo]", 2))
    }

    @Test
    fun guitarSoloUntouchedByTranspose() {
        assertEquals("[Guitar Solo]", ChordSheetTranspose.transpose("[Guitar Solo]", 2))
    }

    @Test
    fun sectionLabelsMixedWithChords() {
        val input = "[Verse]\n[C]Twinkle [G]twinkle\n\n[Coda]\n[F]How I [C]wonder"
        val result = ChordSheetTranspose.transpose(input, 2)
        assertEquals("[Verse]\n[D]Twinkle [A]twinkle\n\n[Coda]\n[G]How I [D]wonder", result)
    }

    @Test
    fun unknownBassPreservedOnTranspose() {
        assertEquals("[D/X]", ChordSheetTranspose.transpose("[C/X]", 2))
    }

    @Test
    fun semitoneLabelFormat() {
        runBlocking {
            checkAll(Arb.int(-24..24)) { semitones ->
                val label = ChordSheetTranspose.semitoneLabel(semitones)
                if (semitones > 0) {
                    assertTrue(label.startsWith("+"), "Positive should start with +")
                }
                assertTrue(
                    label.contains(semitones.toString().trimStart('-')),
                    "Label should contain the number",
                )
            }
        }
    }
}
