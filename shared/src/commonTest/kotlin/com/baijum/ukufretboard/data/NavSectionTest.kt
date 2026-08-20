package com.baijum.ukufretboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [NavSection], whose [NavSection.id] values are a persistence contract.
 *
 * The selected section is stored by id across configuration changes and process
 * death, so an id that changes meaning silently reopens the wrong screen for
 * every user who was last on it.
 */
class NavSectionTest {
    // ── fromId ───────────────────────────────────────────────────────

    @Test
    fun fromIdRoundTripsEveryEntry() {
        for (section in NavSection.entries) {
            assertEquals(section, NavSection.fromId(section.id), "fromId lost ${section.name}")
        }
    }

    @Test
    fun fromIdReturnsNullForUnknownIds() {
        assertNull(NavSection.fromId(-1), "-1 is not a section")
        assertNull(NavSection.fromId(32), "32 is past the last section")
        assertNull(NavSection.fromId(Int.MAX_VALUE), "Int.MAX_VALUE is not a section")
        assertNull(NavSection.fromId(Int.MIN_VALUE), "Int.MIN_VALUE is not a section")
    }

    @Test
    fun idTwentyFourIsRetiredAndResolvesToNull() {
        // Deliberate gap, not an oversight: the section that held id 24 was removed
        // and the id was never reused. FretboardScreen falls back to EXPLORER on a
        // null lookup, so a saved state pointing here reopens Explorer rather than
        // crashing. Reusing 24 would silently reopen the new screen instead.
        assertNull(NavSection.fromId(24), "id 24 is retired and must stay unassigned")
    }

    // ── Id allocation ────────────────────────────────────────────────

    @Test
    fun idsAreUniqueAcrossEntries() {
        val ids = NavSection.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate ids: $ids")
    }

    @Test
    fun idsCoverZeroThroughThirtyOneExceptTwentyFour() {
        val expected = ((0..31).toSet() - 24)
        assertEquals(expected, NavSection.entries.map { it.id }.toSet(), "id allocation drifted")
    }

    @Test
    fun entryDeclarationOrderMatchesAscendingId() {
        val ids = NavSection.entries.map { it.id }
        assertEquals(ids.sorted(), ids, "entries are declared out of id order: $ids")
    }

    @Test
    fun idsAreNonNegative() {
        for (section in NavSection.entries) {
            assertTrue(section.id >= 0, "${section.name} has a negative id ${section.id}")
        }
    }
}
