package com.baijum.ukufretboard.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.baijum.ukufretboard.data.FavoriteVoicing
import com.baijum.ukufretboard.data.FavoritesRepository
import com.baijum.ukufretboard.data.UkuleleTuning
import com.baijum.ukufretboard.domain.ChordVoicing
import com.baijum.ukufretboard.domain.UkuleleString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [FavoritesViewModel].
 *
 * Everything here hangs off [FavoriteVoicing.key] — it is the de-duplication
 * identity, the folder-membership handle, and the sort key for a folder's manual
 * order. The ViewModel resolves some lookups against its cached StateFlow and
 * others straight through the repository, so several tests below use a second
 * ViewModel instance to make that split visible.
 */
@RunWith(RobolectricTestRunner::class)
class FavoritesViewModelTest {
    private lateinit var app: Application
    private lateinit var vm: FavoritesViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        vm = FavoritesViewModel(app)
    }

    /** Seeds storage directly when a controlled addedAt is needed. */
    private fun seed(voicing: FavoriteVoicing) {
        FavoritesRepository(app).add(voicing)
    }

    private fun voicing(
        root: Int,
        symbol: String,
        frets: List<Int>,
        addedAt: Long,
    ) = FavoriteVoicing(
        rootPitchClass = root,
        chordSymbol = symbol,
        frets = frets,
        addedAt = addedAt,
    )

    private fun createFolderAndGetId(name: String): String {
        vm.createFolder(name)
        return vm.folders.value
            .single { it.name == name }
            .id
    }

    private fun tuningOf(tuning: UkuleleTuning) =
        tuning.pitchClasses.mapIndexed { i, pc ->
            UkuleleString(name = tuning.stringNames[i], openPitchClass = pc, octave = tuning.octaves[i])
        }

    // ── Add and remove ───────────────────────────────────────────────

    @Test
    fun addFavoriteAppearsInTheFavoritesFlow() {
        vm.addFavorite(0, "m7", listOf(0, 2, 3, 3))

        val stored = vm.favorites.value.single()
        assertEquals(0, stored.rootPitchClass)
        assertEquals("m7", stored.chordSymbol)
        assertEquals(listOf(0, 2, 3, 3), stored.frets)
        assertEquals("0|m7|0,2,3,3", stored.key)
    }

    @Test
    fun addingTheSameVoicingTwiceIsIgnored() {
        vm.addFavorite(0, "m7", listOf(0, 2, 3, 3))
        vm.addFavorite(0, "m7", listOf(0, 2, 3, 3))

        assertEquals("favorites de-duplicate by key", 1, vm.favorites.value.size)
    }

    @Test
    fun voicingsDifferingOnlyInFretsAreSeparateFavorites() {
        vm.addFavorite(0, "maj", listOf(0, 0, 0, 3))
        vm.addFavorite(0, "maj", listOf(5, 4, 3, 3))

        assertEquals(2, vm.favorites.value.size)
    }

    @Test
    fun theMostRecentlyAddedFavoriteComesFirst() {
        vm.addFavorite(0, "maj", listOf(0, 0, 0, 3))
        vm.addFavorite(9, "m", listOf(2, 0, 0, 0))
        vm.addFavorite(5, "maj", listOf(2, 0, 1, 0))

        assertEquals(listOf(5, 9, 0), vm.favorites.value.map { it.rootPitchClass })
    }

    @Test
    fun theFavoritesListIsOrderedByInsertionNotByAddedAt() {
        // add() prepends, so a voicing carrying an older addedAt — an imported one,
        // say — still lands at the top of the list. Only getOrderedVoicings()
        // consults addedAt, and then only for voicings with no manual order.
        seed(voicing(0, "maj", listOf(0, 0, 0, 3), addedAt = 3_000L))
        seed(voicing(9, "m", listOf(2, 0, 0, 0), addedAt = 1_000L))

        val fresh = FavoritesViewModel(app)
        assertEquals(listOf(9, 0), fresh.favorites.value.map { it.rootPitchClass })
    }

    @Test
    fun removeFavoriteByFieldsRemovesTheMatchingVoicing() {
        vm.addFavorite(0, "maj", listOf(0, 0, 0, 3))
        vm.addFavorite(9, "m", listOf(2, 0, 0, 0))

        vm.removeFavorite(0, "maj", listOf(0, 0, 0, 3))

        assertEquals(listOf(9), vm.favorites.value.map { it.rootPitchClass })
    }

    @Test
    fun removeFavoriteByFieldsIsANoOpWhenTheVoicingIsAbsent() {
        vm.addFavorite(0, "maj", listOf(0, 0, 0, 3))
        vm.removeFavorite(7, "sus4", listOf(0, 2, 3, 3))

        assertEquals(1, vm.favorites.value.size)
    }

    @Test
    fun removeFavoriteByInstanceRemovesIt() {
        vm.addFavorite(0, "maj", listOf(0, 0, 0, 3))
        vm.removeFavorite(vm.favorites.value.single())

        assertTrue(vm.favorites.value.isEmpty())
    }

    @Test
    fun favoritesSurviveANewViewModelOverTheSameStorage() {
        vm.addFavorite(0, "m7", listOf(0, 2, 3, 3))
        assertEquals(1, FavoritesViewModel(app).favorites.value.size)
    }

    // ── Lookups ──────────────────────────────────────────────────────

    @Test
    fun isFavoriteReportsMembership() {
        vm.addFavorite(0, "maj", listOf(0, 0, 0, 3))

        assertTrue(vm.isFavorite(0, "maj", listOf(0, 0, 0, 3)))
        assertFalse(vm.isFavorite(0, "maj", listOf(5, 4, 3, 3)))
        assertFalse(vm.isFavorite(7, "maj", listOf(0, 0, 0, 3)))
    }

    @Test
    fun isFavoriteReadsThroughToStorageWhileTheFlowStaysStale() {
        // isFavorite queries the repository, but removeFavorite(fields) and
        // getFolderIdsForVoicing resolve against the cached flow. A second
        // ViewModel writing to the same storage makes the split observable.
        val other = FavoritesViewModel(app)
        other.addFavorite(0, "maj", listOf(0, 0, 0, 3))

        assertTrue("isFavorite sees storage", vm.isFavorite(0, "maj", listOf(0, 0, 0, 3)))
        assertTrue("the cached flow has not refreshed", vm.favorites.value.isEmpty())
    }

    @Test
    fun getFolderIdsForVoicingReturnsEmptyForAnUnknownVoicing() {
        assertTrue(vm.getFolderIdsForVoicing(0, "maj", listOf(0, 0, 0, 3)).isEmpty())
    }

    @Test
    fun getFolderIdsForVoicingReturnsTheAssignedFolders() {
        val folder = createFolderAndGetId("Practice")
        vm.saveFavoriteToFolders(0, "maj", listOf(0, 0, 0, 3), listOf(folder))

        assertEquals(listOf(folder), vm.getFolderIdsForVoicing(0, "maj", listOf(0, 0, 0, 3)))
    }

    // ── Save to folders ──────────────────────────────────────────────

    @Test
    fun saveFavoriteToFoldersCreatesTheVoicingAndAssignsFolders() {
        val a = createFolderAndGetId("A")
        val b = createFolderAndGetId("B")

        vm.saveFavoriteToFolders(0, "maj", listOf(0, 0, 0, 3), listOf(a, b))

        val stored = vm.favorites.value.single()
        assertEquals("0|maj|0,0,0,3", stored.key)
        assertEquals(setOf(a, b), stored.folderIds.toSet())
    }

    @Test
    fun saveFavoriteToFoldersUpdatesFoldersOnAnExistingVoicing() {
        val a = createFolderAndGetId("A")
        val b = createFolderAndGetId("B")
        vm.addFavorite(0, "maj", listOf(0, 0, 0, 3))

        vm.saveFavoriteToFolders(0, "maj", listOf(0, 0, 0, 3), listOf(a))
        assertEquals(
            listOf(a),
            vm.favorites.value
                .single()
                .folderIds,
        )

        vm.saveFavoriteToFolders(0, "maj", listOf(0, 0, 0, 3), listOf(b))
        assertEquals(
            "reassigning must replace, not append",
            listOf(b),
            vm.favorites.value
                .single()
                .folderIds,
        )
    }

    @Test
    fun saveFavoriteToFoldersWithNoFoldersLeavesTheVoicingUnfiled() {
        vm.saveFavoriteToFolders(0, "maj", listOf(0, 0, 0, 3), emptyList())

        assertTrue(
            vm.favorites.value
                .single()
                .folderIds
                .isEmpty(),
        )
    }

    @Test
    fun saveFavoriteToFoldersDoesNotDuplicateAnExistingVoicing() {
        val folder = createFolderAndGetId("A")
        vm.addFavorite(0, "maj", listOf(0, 0, 0, 3))
        vm.saveFavoriteToFolders(0, "maj", listOf(0, 0, 0, 3), listOf(folder))

        assertEquals(1, vm.favorites.value.size)
    }

    @Test
    fun setFoldersReplacesTheAssignmentForAVoicing() {
        val a = createFolderAndGetId("A")
        vm.addFavorite(0, "maj", listOf(0, 0, 0, 3))

        vm.setFolders(vm.favorites.value.single(), listOf(a))
        assertEquals(
            listOf(a),
            vm.favorites.value
                .single()
                .folderIds,
        )

        vm.setFolders(vm.favorites.value.single(), emptyList())
        assertTrue(
            vm.favorites.value
                .single()
                .folderIds
                .isEmpty(),
        )
    }

    // ── Folders ──────────────────────────────────────────────────────

    @Test
    fun createRenameAndDeleteFolderRoundTripThroughTheFlow() {
        val id = createFolderAndGetId("Practice")
        assertEquals(listOf("Practice"), vm.folders.value.map { it.name })

        vm.renameFolder(id, "Gig set")
        assertEquals(listOf("Gig set"), vm.folders.value.map { it.name })

        vm.deleteFolder(id)
        assertTrue(vm.folders.value.isEmpty())
    }

    @Test
    fun renameFolderOfAnUnknownIdIsANoOp() {
        createFolderAndGetId("Practice")
        vm.renameFolder("no-such-id", "Ignored")

        assertEquals(listOf("Practice"), vm.folders.value.map { it.name })
    }

    @Test
    fun deleteFolderStripsItsIdFromEveryVoicing() {
        val doomed = createFolderAndGetId("Doomed")
        val kept = createFolderAndGetId("Kept")
        vm.saveFavoriteToFolders(0, "maj", listOf(0, 0, 0, 3), listOf(doomed, kept))
        vm.saveFavoriteToFolders(9, "m", listOf(2, 0, 0, 0), listOf(doomed))

        vm.deleteFolder(doomed)

        assertTrue(
            "no voicing may keep a dangling folder id",
            vm.favorites.value.none { doomed in it.folderIds },
        )
        assertEquals(
            listOf(kept),
            vm.favorites.value
                .single { it.rootPitchClass == 0 }
                .folderIds,
        )
    }

    @Test
    fun deleteFolderKeepsTheVoicingsThemselves() {
        val folder = createFolderAndGetId("Practice")
        vm.saveFavoriteToFolders(0, "maj", listOf(0, 0, 0, 3), listOf(folder))

        vm.deleteFolder(folder)

        assertEquals("deleting a folder must not delete its chords", 1, vm.favorites.value.size)
    }

    @Test
    fun foldersSurviveANewViewModelOverTheSameStorage() {
        createFolderAndGetId("Practice")
        assertEquals(listOf("Practice"), FavoritesViewModel(app).folders.value.map { it.name })
    }

    // ── Folder ordering ──────────────────────────────────────────────

    @Test
    fun getOrderedVoicingsReturnsEmptyForAnUnknownFolder() {
        assertTrue(vm.getOrderedVoicings("no-such-folder").isEmpty())
    }

    @Test
    fun getOrderedVoicingsReturnsOnlyTheFoldersOwnVoicings() {
        val a = createFolderAndGetId("A")
        val b = createFolderAndGetId("B")
        vm.saveFavoriteToFolders(0, "maj", listOf(0, 0, 0, 3), listOf(a))
        vm.saveFavoriteToFolders(9, "m", listOf(2, 0, 0, 0), listOf(b))

        assertEquals(listOf("0|maj|0,0,0,3"), vm.getOrderedVoicings(a).map { it.key })
    }

    @Test
    fun reorderInFolderDrivesGetOrderedVoicings() {
        val folder = createFolderAndGetId("Practice")
        vm.saveFavoriteToFolders(0, "maj", listOf(0, 0, 0, 3), listOf(folder))
        vm.saveFavoriteToFolders(9, "m", listOf(2, 0, 0, 0), listOf(folder))
        vm.saveFavoriteToFolders(5, "maj", listOf(2, 0, 1, 0), listOf(folder))

        val desired = listOf("9|m|2,0,0,0", "5|maj|2,0,1,0", "0|maj|0,0,0,3")
        vm.reorderInFolder(folder, desired)

        assertEquals(desired, vm.getOrderedVoicings(folder).map { it.key })
    }

    @Test
    fun getOrderedVoicingsPutsUnorderedVoicingsLastNewestFirst() {
        val folder = createFolderAndGetId("Practice")
        seed(voicing(0, "maj", listOf(0, 0, 0, 3), addedAt = 1_000L))
        seed(voicing(9, "m", listOf(2, 0, 0, 0), addedAt = 2_000L))
        seed(voicing(5, "maj", listOf(2, 0, 1, 0), addedAt = 3_000L))

        val fresh = FavoritesViewModel(app)
        fresh.favorites.value.forEach { fresh.setFolders(it, listOf(folder)) }
        fresh.reorderInFolder(folder, listOf("0|maj|0,0,0,3"))

        assertEquals(
            listOf("0|maj|0,0,0,3", "5|maj|2,0,1,0", "9|m|2,0,0,0"),
            fresh.getOrderedVoicings(folder).map { it.key },
        )
    }

    // ── Voicing conversion ───────────────────────────────────────────

    @Test
    fun toChordVoicingResolvesNotesForHighG() {
        vm.addFavorite(0, "maj", listOf(0, 0, 0, 3))
        val chord = vm.toChordVoicing(vm.favorites.value.single(), tuningOf(UkuleleTuning.HIGH_G))

        // G C E A with the A string at fret 3 sounds G C E C.
        assertEquals(listOf(7, 0, 4, 0), chord.notes.map { it?.pitchClass })
        assertEquals(listOf("G", "C", "E", "C"), chord.notes.map { it?.name })
    }

    @Test
    fun toChordVoicingResolvesNotesForLowG() {
        vm.addFavorite(0, "maj", listOf(0, 0, 0, 3))
        val chord = vm.toChordVoicing(vm.favorites.value.single(), tuningOf(UkuleleTuning.LOW_G))

        // Low-G drops the G string an octave but keeps every pitch class, and
        // toChordVoicing maps frets to pitch classes only. Asserted explicitly
        // rather than assumed, so that making the conversion octave-aware has to
        // come back through this test.
        assertEquals(listOf(7, 0, 4, 0), chord.notes.map { it?.pitchClass })
        assertEquals(listOf("G", "C", "E", "C"), chord.notes.map { it?.name })
    }

    @Test
    fun toChordVoicingResolvesNotesForBaritone() {
        // Baritone is the tuning whose pitch classes actually differ from High-G,
        // so it is the one that exercises a genuinely different mapping.
        vm.addFavorite(2, "maj", listOf(0, 0, 0, 0))
        val chord = vm.toChordVoicing(vm.favorites.value.single(), tuningOf(UkuleleTuning.BARITONE))

        assertEquals(listOf(2, 7, 11, 4), chord.notes.map { it?.pitchClass })
    }

    @Test
    fun toChordVoicingMapsMutedStringsToNullNotes() {
        vm.addFavorite(0, "maj", listOf(ChordVoicing.MUTED, 0, 0, 3))
        val chord = vm.toChordVoicing(vm.favorites.value.single(), tuningOf(UkuleleTuning.HIGH_G))

        assertNull("a muted string has no note", chord.notes.first())
        assertEquals(listOf(0, 4, 0), chord.notes.drop(1).map { it?.pitchClass })
    }

    @Test
    fun toChordVoicingReportsTheFrettedRangeIgnoringOpenStrings() {
        vm.addFavorite(0, "maj", listOf(5, 4, 3, 0))
        val chord = vm.toChordVoicing(vm.favorites.value.single(), tuningOf(UkuleleTuning.HIGH_G))

        assertEquals(3, chord.minFret)
        assertEquals(5, chord.maxFret)
    }

    @Test
    fun toChordVoicingReportsZeroFretRangeForAnAllOpenShape() {
        vm.addFavorite(7, "maj", listOf(0, 0, 0, 0))
        val chord = vm.toChordVoicing(vm.favorites.value.single(), tuningOf(UkuleleTuning.HIGH_G))

        assertEquals("an open shape has no fretted range", 0, chord.minFret)
        assertEquals(0, chord.maxFret)
    }

    @Test
    fun toChordVoicingPreservesTheOriginalFrets() {
        vm.addFavorite(0, "m7", listOf(0, 2, 3, 3))
        val chord = vm.toChordVoicing(vm.favorites.value.single(), tuningOf(UkuleleTuning.HIGH_G))

        assertEquals(listOf(0, 2, 3, 3), chord.frets)
    }
}
