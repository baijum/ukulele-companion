package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.Notes
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransposePropertyTest {

    @Test
    fun transposePitchClassAlwaysReturnsValidRange() {
        runBlocking {
            checkAll(Arb.int(-1000..1000), Arb.int(-1000..1000)) { pitchClass, semitones ->
                val result = Transpose.transposePitchClass(pitchClass, semitones)
                assertTrue(
                    "Result $result should be in 0..11",
                    result in 0..11,
                )
            }
        }
    }

    @Test
    fun transposeByZeroIsIdentity() {
        runBlocking {
            checkAll(Arb.int(0..11)) { pitchClass ->
                assertEquals(pitchClass, Transpose.transposePitchClass(pitchClass, 0))
            }
        }
    }

    @Test
    fun transposeByTwelveIsIdentity() {
        runBlocking {
            checkAll(Arb.int(0..11)) { pitchClass ->
                assertEquals(pitchClass, Transpose.transposePitchClass(pitchClass, 12))
                assertEquals(pitchClass, Transpose.transposePitchClass(pitchClass, -12))
                assertEquals(pitchClass, Transpose.transposePitchClass(pitchClass, 24))
            }
        }
    }

    @Test
    fun transposeThenInverseIsIdentity() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.int(-48..48)) { pitchClass, semitones ->
                val transposed = Transpose.transposePitchClass(pitchClass, semitones)
                val roundTrip = Transpose.transposePitchClass(transposed, -semitones)
                assertEquals(pitchClass, roundTrip)
            }
        }
    }

    @Test
    fun transposeIsAssociative() {
        runBlocking {
            checkAll(
                Arb.int(0..11),
                Arb.int(-24..24),
                Arb.int(-24..24),
            ) { pitchClass, a, b ->
                val stepwise = Transpose.transposePitchClass(
                    Transpose.transposePitchClass(pitchClass, a), b
                )
                val combined = Transpose.transposePitchClass(pitchClass, a + b)
                assertEquals(stepwise, combined)
            }
        }
    }

    @Test
    fun transposeChordNamePreservesSymbol() {
        runBlocking {
            val symbols = listOf("", "m", "7", "m7", "maj7", "dim", "aug", "sus4", "sus2")
            checkAll(Arb.int(0..11), Arb.int(-24..24)) { rootPc, semitones ->
                for (symbol in symbols) {
                    val result = Transpose.transposeChordName(rootPc, symbol, semitones)
                    assertTrue(
                        "Chord name '$result' should end with '$symbol'",
                        result.endsWith(symbol),
                    )
                }
            }
        }
    }

    @Test
    fun transposeChordNameRootIsValid() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.int(-24..24)) { rootPc, semitones ->
                val result = Transpose.transposeChordName(rootPc, "", semitones)
                assertTrue(
                    "'$result' should be a valid note name",
                    result in Notes.NOTE_NAMES_STANDARD,
                )
            }
        }
    }

    @Test
    fun capoFretAlwaysReturnsValidRange() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.int(0..11)) { original, target ->
                val capo = Transpose.capoFret(original, target)
                assertTrue(
                    "Capo fret $capo should be in 0..11",
                    capo in 0..11,
                )
            }
        }
    }

    @Test
    fun capoFretZeroWhenKeysMatch() {
        runBlocking {
            checkAll(Arb.int(0..11)) { key ->
                assertEquals(0, Transpose.capoFret(key, key))
            }
        }
    }

    @Test
    fun capoFretRoundTrip() {
        runBlocking {
            checkAll(Arb.int(0..11), Arb.int(0..11)) { original, target ->
                val capo = Transpose.capoFret(original, target)
                val reconstructed = Transpose.transposePitchClass(original, capo)
                assertEquals(target, reconstructed)
            }
        }
    }
}
