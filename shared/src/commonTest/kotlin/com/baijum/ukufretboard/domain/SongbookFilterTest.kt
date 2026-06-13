package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.SongSortOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SongbookFilterTest {

    private data class Song(
        val title: String,
        val artist: String,
        val labels: List<String> = emptyList(),
        val updatedAt: Long = 0L,
        val createdAt: Long = 0L,
    )

    private fun apply(
        songs: List<Song>,
        query: String = "",
        label: String? = null,
        sortOrder: SongSortOrder = SongSortOrder.LAST_MODIFIED,
    ): List<Song> = SongbookFilter.apply(
        songs = songs,
        query = query,
        label = label,
        sortOrder = sortOrder,
        title = { it.title },
        artist = { it.artist },
        labels = { it.labels },
        updatedAt = { it.updatedAt },
        createdAt = { it.createdAt },
    )

    private val sampleSongs = listOf(
        Song("Somewhere Over The Rainbow", "Israel Kamakawiwoʻole", listOf("Hawaiian", "Classic"), updatedAt = 300, createdAt = 100),
        Song("Riptide", "Vance Joy", listOf("Pop"), updatedAt = 500, createdAt = 200),
        Song("I'm Yours", "Jason Mraz", listOf("Pop", "Chill"), updatedAt = 100, createdAt = 300),
        Song("Hey Soul Sister", "Train", listOf("Pop", "Classic"), updatedAt = 400, createdAt = 400),
        Song("Can't Help Falling In Love", "Elvis Presley", listOf("Classic"), updatedAt = 200, createdAt = 500),
    )

    // --- Empty input ---

    @Test
    fun emptyListReturnsEmpty() {
        val result = apply(emptyList())
        assertTrue(result.isEmpty())
    }

    // --- Query filtering ---

    @Test
    fun noQueryReturnsAllSongs() {
        val result = apply(sampleSongs, query = "")
        assertEquals(5, result.size)
    }

    @Test
    fun queryFiltersByTitleCaseInsensitive() {
        val result = apply(sampleSongs, query = "riptide")
        assertEquals(1, result.size)
        assertEquals("Riptide", result[0].title)
    }

    @Test
    fun queryFiltersByArtistCaseInsensitive() {
        val result = apply(sampleSongs, query = "jason")
        assertEquals(1, result.size)
        assertEquals("Jason Mraz", result[0].artist)
    }

    @Test
    fun queryMatchesPartialTitle() {
        val result = apply(sampleSongs, query = "soul")
        assertEquals(1, result.size)
        assertEquals("Hey Soul Sister", result[0].title)
    }

    @Test
    fun queryMatchesPartialArtist() {
        val result = apply(sampleSongs, query = "vanc")
        assertEquals(1, result.size)
        assertEquals("Vance Joy", result[0].artist)
    }

    @Test
    fun queryMatchesBothTitleAndArtist() {
        val result = apply(sampleSongs, query = "love")
        assertEquals(1, result.size)
        assertEquals("Can't Help Falling In Love", result[0].title)
    }

    @Test
    fun queryWithLeadingAndTrailingSpacesIsTrimmed() {
        val result = apply(sampleSongs, query = "  riptide  ")
        assertEquals(1, result.size)
        assertEquals("Riptide", result[0].title)
    }

    @Test
    fun queryWithNoMatchesReturnsEmpty() {
        val result = apply(sampleSongs, query = "nonexistent")
        assertTrue(result.isEmpty())
    }

    @Test
    fun queryWithMixedCaseMatches() {
        val result = apply(sampleSongs, query = "RiPtIdE")
        assertEquals(1, result.size)
        assertEquals("Riptide", result[0].title)
    }

    // --- Label filtering ---

    @Test
    fun nullLabelReturnsAllSongs() {
        val result = apply(sampleSongs, label = null)
        assertEquals(5, result.size)
    }

    @Test
    fun labelFilterIncludesMatchingSongs() {
        val result = apply(sampleSongs, label = "Pop")
        assertEquals(3, result.size)
        assertTrue(result.all { "Pop" in it.labels })
    }

    @Test
    fun labelFilterIsCaseInsensitive() {
        val result = apply(sampleSongs, label = "pop")
        assertEquals(3, result.size)
    }

    @Test
    fun labelFilterWithNoMatchReturnsEmpty() {
        val result = apply(sampleSongs, label = "Jazz")
        assertTrue(result.isEmpty())
    }

    @Test
    fun labelFilterCombinedWithQuery() {
        val result = apply(sampleSongs, query = "train", label = "Classic")
        assertEquals(1, result.size)
        assertEquals("Hey Soul Sister", result[0].title)
    }

    // --- Sort: LAST_MODIFIED ---

    @Test
    fun sortByLastModifiedDescending() {
        val result = apply(sampleSongs, sortOrder = SongSortOrder.LAST_MODIFIED)
        assertEquals(listOf(500L, 400L, 300L, 200L, 100L), result.map { it.updatedAt })
    }

    // --- Sort: DATE_ADDED ---

    @Test
    fun sortByDateAddedDescending() {
        val result = apply(sampleSongs, sortOrder = SongSortOrder.DATE_ADDED)
        assertEquals(listOf(500L, 400L, 300L, 200L, 100L), result.map { it.createdAt })
    }

    // --- Sort: TITLE ---

    @Test
    fun sortByTitleAscendingCaseInsensitive() {
        val result = apply(sampleSongs, sortOrder = SongSortOrder.TITLE)
        assertEquals(
            listOf(
                "Can't Help Falling In Love",
                "Hey Soul Sister",
                "I'm Yours",
                "Riptide",
                "Somewhere Over The Rainbow",
            ),
            result.map { it.title },
        )
    }

    // --- Sort: ARTIST ---

    @Test
    fun sortByArtistAscendingCaseInsensitive() {
        val result = apply(sampleSongs, sortOrder = SongSortOrder.ARTIST)
        assertEquals(
            listOf(
                "Elvis Presley",
                "Israel Kamakawiwoʻole",
                "Jason Mraz",
                "Train",
                "Vance Joy",
            ),
            result.map { it.artist },
        )
    }

    // --- Combined filter + sort ---

    @Test
    fun filterAndSortCombined() {
        val result = apply(sampleSongs, query = "pop", label = "Pop", sortOrder = SongSortOrder.TITLE)
        assertTrue(result.isEmpty())
    }

    @Test
    fun labelFilterWithSortByTitle() {
        val result = apply(sampleSongs, label = "Classic", sortOrder = SongSortOrder.TITLE)
        assertEquals(
            listOf(
                "Can't Help Falling In Love",
                "Hey Soul Sister",
                "Somewhere Over The Rainbow",
            ),
            result.map { it.title },
        )
    }

    @Test
    fun queryFilterWithSortByArtist() {
        val result = apply(sampleSongs, query = "i", sortOrder = SongSortOrder.ARTIST)
        val titles = result.map { it.title }
        assertTrue("Riptide" in titles)
        assertTrue("I'm Yours" in titles)
        assertTrue("Somewhere Over The Rainbow" in titles)
    }

    // --- Edge cases ---

    @Test
    fun songsWithEmptyTitleAndArtistHandledGracefully() {
        val songs = listOf(Song("", "", listOf("Tag"), updatedAt = 1, createdAt = 1))
        val result = apply(songs, query = "")
        assertEquals(1, result.size)
    }

    @Test
    fun songsWithEmptyLabelsListNotMatchedByLabel() {
        val songs = listOf(Song("Test", "Artist", emptyList(), updatedAt = 1, createdAt = 1))
        val result = apply(songs, label = "Any")
        assertTrue(result.isEmpty())
    }

    @Test
    fun stableSortPreservesOrderForEqualValues() {
        val songs = listOf(
            Song("Alpha", "Same", updatedAt = 100, createdAt = 100),
            Song("Alpha", "Same", updatedAt = 100, createdAt = 100),
        )
        val result = apply(songs, sortOrder = SongSortOrder.TITLE)
        assertEquals(2, result.size)
    }
}
