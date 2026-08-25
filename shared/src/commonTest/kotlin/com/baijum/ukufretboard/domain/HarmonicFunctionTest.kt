package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.ScaleType
import kotlin.test.Test
import kotlin.test.assertEquals

class HarmonicFunctionTest {

    // --- Major scale ---

    @Test
    fun majorIIsTonic() {
        assertEquals(HarmonicFunction.TONIC, harmonicFunction("I", ScaleType.MAJOR))
    }

    @Test
    fun majorIiiIsTonic() {
        assertEquals(HarmonicFunction.TONIC, harmonicFunction("iii", ScaleType.MAJOR))
    }

    @Test
    fun majorViIsTonic() {
        assertEquals(HarmonicFunction.TONIC, harmonicFunction("vi", ScaleType.MAJOR))
    }

    @Test
    fun majorIiIsSubdominant() {
        assertEquals(HarmonicFunction.SUBDOMINANT, harmonicFunction("ii", ScaleType.MAJOR))
    }

    @Test
    fun majorIVIsSubdominant() {
        assertEquals(HarmonicFunction.SUBDOMINANT, harmonicFunction("IV", ScaleType.MAJOR))
    }

    @Test
    fun majorVIsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("V", ScaleType.MAJOR))
    }

    @Test
    fun majorViiDimIsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("vii\u00B0", ScaleType.MAJOR))
    }

    // --- Minor scale ---

    @Test
    fun minorIIsTonic() {
        assertEquals(HarmonicFunction.TONIC, harmonicFunction("i", ScaleType.MINOR))
    }

    @Test
    fun minorIIIIsTonic() {
        assertEquals(HarmonicFunction.TONIC, harmonicFunction("III", ScaleType.MINOR))
    }

    @Test
    fun minorVIIsTonic() {
        assertEquals(HarmonicFunction.TONIC, harmonicFunction("VI", ScaleType.MINOR))
    }

    @Test
    fun minorIiDimIsSubdominant() {
        assertEquals(HarmonicFunction.SUBDOMINANT, harmonicFunction("ii\u00B0", ScaleType.MINOR))
    }

    @Test
    fun minorIvIsSubdominant() {
        assertEquals(HarmonicFunction.SUBDOMINANT, harmonicFunction("iv", ScaleType.MINOR))
    }

    @Test
    fun minorVIsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("V", ScaleType.MINOR))
    }

    @Test
    fun minorSmallVIsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("v", ScaleType.MINOR))
    }

    @Test
    fun minorVIIIsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("VII", ScaleType.MINOR))
    }

    // --- Dorian ---

    @Test
    fun dorianIIsTonic() {
        assertEquals(HarmonicFunction.TONIC, harmonicFunction("i", ScaleType.DORIAN))
    }

    @Test
    fun dorianIIIsSubdominant() {
        assertEquals(HarmonicFunction.SUBDOMINANT, harmonicFunction("ii", ScaleType.DORIAN))
    }

    @Test
    fun dorianVIsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("v", ScaleType.DORIAN))
    }

    @Test
    fun dorianVIIIsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("VII", ScaleType.DORIAN))
    }

    // --- Phrygian ---

    @Test
    fun phrygianIIsTonic() {
        assertEquals(HarmonicFunction.TONIC, harmonicFunction("i", ScaleType.PHRYGIAN))
    }

    @Test
    fun phrygianIIIsSubdominant() {
        assertEquals(HarmonicFunction.SUBDOMINANT, harmonicFunction("II", ScaleType.PHRYGIAN))
    }

    @Test
    fun phrygianVDimIsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("v\u00B0", ScaleType.PHRYGIAN))
    }

    // --- Lydian ---

    @Test
    fun lydianIIsTonic() {
        assertEquals(HarmonicFunction.TONIC, harmonicFunction("I", ScaleType.LYDIAN))
    }

    @Test
    fun lydianIIIsSubdominant() {
        assertEquals(HarmonicFunction.SUBDOMINANT, harmonicFunction("II", ScaleType.LYDIAN))
    }

    @Test
    fun lydianSharpIvDimIsSubdominant() {
        assertEquals(HarmonicFunction.SUBDOMINANT, harmonicFunction("#iv\u00B0", ScaleType.LYDIAN))
    }

    @Test
    fun lydianVIsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("V", ScaleType.LYDIAN))
    }

    // --- Mixolydian ---

    @Test
    fun mixolydianIIsTonic() {
        assertEquals(HarmonicFunction.TONIC, harmonicFunction("I", ScaleType.MIXOLYDIAN))
    }

    @Test
    fun mixolydianIiIsSubdominant() {
        assertEquals(HarmonicFunction.SUBDOMINANT, harmonicFunction("ii", ScaleType.MIXOLYDIAN))
    }

    @Test
    fun mixolydianIiiDimIsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("iii\u00B0", ScaleType.MIXOLYDIAN))
    }

    @Test
    fun mixolydianVIIIsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("VII", ScaleType.MIXOLYDIAN))
    }

    // --- Locrian ---

    @Test
    fun locrianIDimIsTonic() {
        assertEquals(HarmonicFunction.TONIC, harmonicFunction("i\u00B0", ScaleType.LOCRIAN))
    }

    @Test
    fun locrianIIIsSubdominant() {
        assertEquals(HarmonicFunction.SUBDOMINANT, harmonicFunction("II", ScaleType.LOCRIAN))
    }

    @Test
    fun locrianVIsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("V", ScaleType.LOCRIAN))
    }

    // --- Extended numeral stripping ---

    @Test
    fun extendedNumeralImaj7IsTonic() {
        assertEquals(HarmonicFunction.TONIC, harmonicFunction("Imaj7", ScaleType.MAJOR))
    }

    @Test
    fun extendedNumeralIim7IsSubdominant() {
        assertEquals(HarmonicFunction.SUBDOMINANT, harmonicFunction("iim7", ScaleType.MAJOR))
    }

    @Test
    fun extendedNumeralV7IsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("V7", ScaleType.MAJOR))
    }

    @Test
    fun extendedNumeralViisus4IsTonic() {
        // "vi" base → TONIC in major
        assertEquals(HarmonicFunction.TONIC, harmonicFunction("visus4", ScaleType.MAJOR))
    }

    // --- Chromatic / borrowed / secondary-dominant numerals (issue #604) ---

    @Test
    fun minorHalfDiminishedIiIsSubdominant() {
        // "iim7♭5" (Minor ii–V–I) → base "ii" → SUBDOMINANT, not TONIC.
        assertEquals(HarmonicFunction.SUBDOMINANT, harmonicFunction("iim7♭5", ScaleType.MINOR))
    }

    @Test
    fun majorSecondaryDominantII7IsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("II7", ScaleType.MAJOR))
    }

    @Test
    fun majorSecondaryDominantIII7IsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("III7", ScaleType.MAJOR))
    }

    @Test
    fun majorSecondaryDominantVI7IsDominant() {
        assertEquals(HarmonicFunction.DOMINANT, harmonicFunction("VI7", ScaleType.MAJOR))
    }

    @Test
    fun majorBorrowedFlatVIIsSubdominant() {
        assertEquals(HarmonicFunction.SUBDOMINANT, harmonicFunction("♭VI", ScaleType.MAJOR))
    }

    @Test
    fun majorBorrowedFlatVIIIsSubdominant() {
        assertEquals(HarmonicFunction.SUBDOMINANT, harmonicFunction("♭VII", ScaleType.MAJOR))
    }

    // --- Fallback ---

    @Test
    fun unknownNumeralFallsBackToTonic() {
        assertEquals(HarmonicFunction.TONIC, harmonicFunction("unknown", ScaleType.MAJOR))
    }
}
