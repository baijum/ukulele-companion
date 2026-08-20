package com.baijum.ukufretboard.data

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [IntRangeSerializer].
 *
 * The two field names it emits are an on-disk contract: every stored custom strum
 * and fingerpicking pattern persists its `suggestedBpm` through this serializer,
 * so renaming a field orphans that data on every existing install.
 */
class IntRangeSerializerTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private fun encode(range: IntRange) = json.encodeToString(IntRangeSerializer, range)

    private fun decode(raw: String) = json.decodeFromString(IntRangeSerializer, raw)

    // ── Wire format ──────────────────────────────────────────────────

    @Test
    fun serializesToStartAndEndInclusiveFields() {
        assertEquals("""{"start":80,"endInclusive":120}""", encode(80..120), "wire format changed")
    }

    @Test
    fun decodesTheDocumentedWireFormat() {
        val decoded = decode("""{"start":80,"endInclusive":120}""")
        assertEquals(80, decoded.first)
        assertEquals(120, decoded.last)
    }

    @Test
    fun fieldOrderInTheInputDoesNotMatter() {
        val decoded = decode("""{"endInclusive":120,"start":80}""")
        assertEquals(80, decoded.first)
        assertEquals(120, decoded.last)
    }

    // ── Round trips ──────────────────────────────────────────────────

    @Test
    fun roundTripPreservesFirstAndLast() {
        val decoded = decode(encode(40..200))
        assertEquals(40, decoded.first)
        assertEquals(200, decoded.last)
    }

    @Test
    fun roundTripsASingleValueRange() {
        val decoded = decode(encode(5..5))
        assertEquals(5, decoded.first)
        assertEquals(5, decoded.last)
    }

    @Test
    fun roundTripsNegativeAndCrossZeroRanges() {
        val negative = decode(encode(-9..-3))
        assertEquals(-9, negative.first)
        assertEquals(-3, negative.last)

        val crossZero = decode(encode(-4..4))
        assertEquals(-4, crossZero.first)
        assertEquals(4, crossZero.last)
    }

    @Test
    fun roundTripsTheExtremeIntBounds() {
        val decoded = decode(encode(Int.MIN_VALUE..Int.MAX_VALUE))
        assertEquals(Int.MIN_VALUE, decoded.first)
        assertEquals(Int.MAX_VALUE, decoded.last)
    }

    @Test
    fun emptyRangePreservesItsOriginalBounds() {
        // Assert the bounds, not the range: every empty IntRange compares equal
        // (5..1 == 3..1), so assertEquals on the range would pass even if the
        // serializer round-tripped completely different numbers.
        val decoded = decode(encode(5..1))
        assertEquals(5, decoded.first, "start was not preserved")
        assertEquals(1, decoded.last, "endInclusive was not preserved")
    }

    // ── Malformed input ──────────────────────────────────────────────

    @Test
    fun missingEndInclusiveDecodesAsZeroRatherThanThrowing() {
        // The hand-rolled decodeStructure loop performs no required-field check,
        // so a truncated payload silently yields a range ending at 0 instead of
        // failing. Pinned so that adding validation is a deliberate change.
        val decoded = decode("""{"start":5}""")
        assertEquals(5, decoded.first)
        assertEquals(0, decoded.last, "missing endInclusive should default to 0 today")
    }

    @Test
    fun missingBothFieldsDecodesAsTheEmptyZeroRange() {
        val decoded = decode("{}")
        assertEquals(0, decoded.first)
        assertEquals(0, decoded.last)
    }

    // ── Use through a real holder ────────────────────────────────────

    @Test
    fun suggestedBpmRoundTripsInsideAStrumPattern() {
        val pattern =
            StrumPattern(
                name = "Island",
                description = "The island strum",
                difficulty = Difficulty.BEGINNER,
                beats = listOf(StrumBeat(StrumDirection.DOWN), StrumBeat(StrumDirection.UP)),
                notation = "D U",
                suggestedBpm = 90..110,
            )
        val decoded =
            json.decodeFromString(
                StrumPattern.serializer(),
                json.encodeToString(StrumPattern.serializer(), pattern),
            )
        assertEquals(90, decoded.suggestedBpm.first, "suggestedBpm start lost")
        assertEquals(110, decoded.suggestedBpm.last, "suggestedBpm end lost")
    }
}
