package com.baijum.ukufretboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetlistTest {

    @Test
    fun defaultSongIdsIsEmpty() {
        val setlist = Setlist(name = "My Setlist")
        assertTrue(setlist.songIds.isEmpty())
    }

    @Test
    fun defaultIdIsGenerated() {
        val setlist = Setlist(name = "My Setlist")
        assertTrue(setlist.id.isNotEmpty(), "Default id should be non-empty")
    }

    @Test
    fun twoSetlistsGetDistinctIds() {
        val a = Setlist(name = "A")
        val b = Setlist(name = "B")
        assertTrue(a.id != b.id, "Two setlists should have different generated IDs")
    }

    @Test
    fun copyPreservesOtherFields() {
        val original = Setlist(
            id = "sl-1",
            name = "Gig Night",
            songIds = listOf("s1", "s2"),
            createdAt = 1000L,
            updatedAt = 2000L,
        )
        val updated = original.copy(songIds = listOf("s1", "s2", "s3"))
        assertEquals("sl-1", updated.id)
        assertEquals("Gig Night", updated.name)
        assertEquals(listOf("s1", "s2", "s3"), updated.songIds)
        assertEquals(1000L, updated.createdAt)
        assertEquals(2000L, updated.updatedAt)
    }
}
