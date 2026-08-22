package com.baijum.ukufretboard.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [SetlistViewModel].
 *
 * The interesting part is the `currentSetlist` bookkeeping: every mutation has to
 * refresh the open setlist when it is the one being changed, and leave it alone
 * when it is not. A stale `currentSetlist` shows the user a setlist that no longer
 * matches what was saved.
 */
@RunWith(RobolectricTestRunner::class)
class SetlistViewModelTest {
    private lateinit var app: Application
    private lateinit var vm: SetlistViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        vm = SetlistViewModel(app)
    }

    private fun createAndGetId(name: String): String {
        vm.create(name)
        return vm.setlists.value
            .single { it.name == name }
            .id
    }

    private fun songIdsOf(id: String) =
        vm.setlists.value
            .single { it.id == id }
            .songIds

    // ── Create, rename, delete ───────────────────────────────────────

    @Test
    fun createAddsTheSetlistToTheFlow() {
        vm.create("Beach gig")
        assertEquals(listOf("Beach gig"), vm.setlists.value.map { it.name })
    }

    @Test
    fun newestSetlistsComeFirst() {
        vm.create("First")
        vm.create("Second")
        assertEquals(listOf("Second", "First"), vm.setlists.value.map { it.name })
    }

    @Test
    fun setlistsSurviveANewViewModelOverTheSameStorage() {
        vm.create("Persisted")
        assertEquals(listOf("Persisted"), SetlistViewModel(app).setlists.value.map { it.name })
    }

    @Test
    fun renameUpdatesTheStoredSetlist() {
        val id = createAndGetId("Old name")
        vm.rename(id, "New name")

        assertEquals(listOf("New name"), vm.setlists.value.map { it.name })
        assertEquals(
            "New name",
            SetlistViewModel(app)
                .setlists.value
                .single()
                .name,
        )
    }

    @Test
    fun renameOfAnUnknownIdIsANoOp() {
        createAndGetId("Kept")
        vm.rename("no-such-id", "Ignored")
        assertEquals(listOf("Kept"), vm.setlists.value.map { it.name })
    }

    @Test
    fun renameBumpsUpdatedAt() {
        val id = createAndGetId("Gig")
        val before =
            vm.setlists.value
                .single()
                .updatedAt
        vm.rename(id, "Gig v2")
        assertTrue(
            "updatedAt should not go backwards",
            vm.setlists.value
                .single()
                .updatedAt >= before,
        )
    }

    @Test
    fun deleteRemovesTheSetlist() {
        val id = createAndGetId("Doomed")
        createAndGetId("Kept")
        vm.delete(id)
        assertEquals(listOf("Kept"), vm.setlists.value.map { it.name })
    }

    @Test
    fun deleteOfAnUnknownIdLeavesTheListIntact() {
        createAndGetId("Kept")
        vm.delete("no-such-id")
        assertEquals(1, vm.setlists.value.size)
    }

    // ── Open setlist bookkeeping ─────────────────────────────────────

    @Test
    fun openThenCloseClearsTheCurrentSetlist() {
        val id = createAndGetId("Gig")
        vm.open(vm.setlists.value.single { it.id == id })
        assertNotNull(vm.currentSetlist.value)

        vm.close()
        assertNull(vm.currentSetlist.value)
    }

    @Test
    fun renameRefreshesTheOpenSetlistWhenItIsTheOneRenamed() {
        val id = createAndGetId("Old name")
        vm.open(vm.setlists.value.single { it.id == id })
        vm.rename(id, "New name")

        assertEquals("the open setlist must not go stale", "New name", vm.currentSetlist.value?.name)
    }

    @Test
    fun renameLeavesADifferentOpenSetlistAlone() {
        val open = createAndGetId("Open one")
        val other = createAndGetId("Other one")
        vm.open(vm.setlists.value.single { it.id == open })
        vm.rename(other, "Renamed")

        assertEquals("Open one", vm.currentSetlist.value?.name)
    }

    @Test
    fun deleteClosesTheSetlistWhenItWasTheOpenOne() {
        val id = createAndGetId("Doomed")
        vm.open(vm.setlists.value.single { it.id == id })
        vm.delete(id)

        assertNull("deleting the open setlist must close it", vm.currentSetlist.value)
    }

    @Test
    fun deleteLeavesADifferentOpenSetlistAlone() {
        val open = createAndGetId("Open one")
        val other = createAndGetId("Other one")
        vm.open(vm.setlists.value.single { it.id == open })
        vm.delete(other)

        assertEquals("Open one", vm.currentSetlist.value?.name)
    }

    // ── Songs ────────────────────────────────────────────────────────

    @Test
    fun addSongAppendsToTheEnd() {
        val id = createAndGetId("Gig")
        vm.addSong(id, "song-a")
        vm.addSong(id, "song-b")

        assertEquals(listOf("song-a", "song-b"), songIdsOf(id))
    }

    @Test
    fun addSongIgnoresADuplicateSongId() {
        val id = createAndGetId("Gig")
        vm.addSong(id, "song-a")
        vm.addSong(id, "song-a")

        assertEquals(listOf("song-a"), songIdsOf(id))
    }

    @Test
    fun addSongToAnUnknownSetlistIsANoOp() {
        val id = createAndGetId("Gig")
        vm.addSong("no-such-id", "song-a")
        assertTrue(songIdsOf(id).isEmpty())
    }

    @Test
    fun removeSongDropsTheSongAndKeepsTheRest() {
        val id = createAndGetId("Gig")
        listOf("a", "b", "c").forEach { vm.addSong(id, it) }
        vm.removeSong(id, "b")

        assertEquals(listOf("a", "c"), songIdsOf(id))
    }

    @Test
    fun removeSongOfAnAbsentSongIsANoOp() {
        val id = createAndGetId("Gig")
        vm.addSong(id, "a")
        vm.removeSong(id, "not-there")

        assertEquals(listOf("a"), songIdsOf(id))
    }

    @Test
    fun moveSongReordersTheSongList() {
        val id = createAndGetId("Gig")
        listOf("a", "b", "c").forEach { vm.addSong(id, it) }

        vm.moveSong(id, songId = "a", offset = +2)
        assertEquals(listOf("b", "c", "a"), songIdsOf(id))

        vm.moveSong(id, songId = "a", offset = -2)
        assertEquals(listOf("a", "b", "c"), songIdsOf(id))
    }

    @Test
    fun moveSongTargetsTheNamedSongNotAPosition() {
        // Regression for issue #572: the UI's display index can diverge from the
        // persisted order (e.g. a Songbook filter hides songs), so the move must
        // be keyed by ID.
        val id = createAndGetId("Gig")
        listOf("a", "b", "c").forEach { vm.addSong(id, it) }

        vm.moveSong(id, songId = "b", offset = -1)
        assertEquals(listOf("b", "a", "c"), songIdsOf(id))

        vm.moveSong(id, songId = "b", offset = +1)
        assertEquals(listOf("a", "b", "c"), songIdsOf(id))
    }

    @Test
    fun moveSongIgnoresOutOfRangeMoves() {
        val id = createAndGetId("Gig")
        listOf("a", "b").forEach { vm.addSong(id, it) }

        vm.moveSong(id, songId = "a", offset = -1)
        vm.moveSong(id, songId = "b", offset = +1)
        vm.moveSong(id, songId = "not-there", offset = 0)
        vm.moveSong(id, songId = "not-there", offset = +1)
        vm.moveSong(id, songId = "a", offset = 0)

        assertEquals(listOf("a", "b"), songIdsOf(id))
    }

    @Test
    fun moveSongOnAnEmptySetlistIsANoOp() {
        val id = createAndGetId("Gig")
        vm.moveSong(id, songId = "a", offset = +1)
        assertTrue(songIdsOf(id).isEmpty())
    }

    // ── Purging deleted songs (issue #594) ───────────────────────

    @Test
    fun purgeDeletedSongsStripsTheSongFromEverySetlist() {
        val first = createAndGetId("First")
        val second = createAndGetId("Second")
        listOf("a", "b", "c").forEach { vm.addSong(first, it) }
        listOf("b", "d").forEach { vm.addSong(second, it) }

        vm.purgeDeletedSongs(listOf("b"))

        assertEquals(listOf("a", "c"), songIdsOf(first))
        assertEquals(listOf("d"), songIdsOf(second))
    }

    @Test
    fun purgeDeletedSongsKeepsTheOpenSetlistInSync() {
        val id = createAndGetId("Gig")
        listOf("a", "b").forEach { vm.addSong(id, it) }
        vm.open(vm.setlists.value.single { it.id == id })

        vm.purgeDeletedSongs(listOf("a"))

        assertEquals(listOf("b"), vm.currentSetlist.value?.songIds)
    }

    @Test
    fun purgeDeletedSongsToleratesUnknownAndEmptyInputs() {
        val id = createAndGetId("Gig")
        listOf("a", "b").forEach { vm.addSong(id, it) }

        vm.purgeDeletedSongs(emptyList())
        vm.purgeDeletedSongs(listOf("not-there"))

        assertEquals(listOf("a", "b"), songIdsOf(id))
    }

    @Test
    fun songMutationsKeepTheOpenSetlistInSync() {
        val id = createAndGetId("Gig")
        vm.open(vm.setlists.value.single { it.id == id })

        vm.addSong(id, "a")
        assertEquals(listOf("a"), vm.currentSetlist.value?.songIds)

        vm.addSong(id, "b")
        vm.moveSong(id, songId = "a", offset = +1)
        assertEquals(listOf("b", "a"), vm.currentSetlist.value?.songIds)

        vm.removeSong(id, "b")
        assertEquals(listOf("a"), vm.currentSetlist.value?.songIds)
    }

    @Test
    fun songMutationsOnAnotherSetlistLeaveTheOpenOneAlone() {
        val open = createAndGetId("Open one")
        val other = createAndGetId("Other one")
        vm.open(vm.setlists.value.single { it.id == open })

        vm.addSong(other, "a")
        assertTrue(
            "the open setlist should be untouched",
            vm.currentSetlist.value
                ?.songIds
                ?.isEmpty() == true,
        )
    }
}
