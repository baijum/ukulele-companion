package com.baijum.ukufretboard.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.baijum.ukufretboard.data.ChordDegree
import com.baijum.ukufretboard.data.ScaleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [CustomProgressionViewModel].
 */
@RunWith(RobolectricTestRunner::class)
class CustomProgressionViewModelTest {
    private lateinit var app: Application
    private lateinit var vm: CustomProgressionViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        vm = CustomProgressionViewModel(app)
    }

    private fun degrees(vararg numerals: String) =
        numerals.mapIndexed { index, numeral ->
            ChordDegree(interval = index, quality = "", numeral = numeral)
        }

    private fun createAndGetId(
        name: String,
        description: String = "A description",
        scaleType: ScaleType = ScaleType.MAJOR,
    ): String {
        vm.create(name, description, degrees("I", "IV", "V"), scaleType)
        return vm.progressions.value
            .single { it.progression.name == name }
            .id
    }

    // ── Create ───────────────────────────────────────────────────────

    @Test
    fun createStoresTheProgressionAndExposesIt() {
        vm.create("Doo-wop", "Fifties staple", degrees("I", "vi", "IV", "V"), ScaleType.MAJOR)

        val stored = vm.progressions.value.single()
        assertEquals("Doo-wop", stored.progression.name)
        assertEquals("Fifties staple", stored.progression.description)
        assertEquals(ScaleType.MAJOR, stored.progression.scaleType)
        assertEquals(listOf("I", "vi", "IV", "V"), stored.progression.degrees.map { it.numeral })
    }

    @Test
    fun createFallsBackToADefaultDescriptionWhenBlank() {
        vm.create("Unnamed", "", degrees("I"), ScaleType.MAJOR)
        assertEquals(
            "Custom progression",
            vm.progressions.value
                .single()
                .progression.description,
        )
    }

    @Test
    fun createTreatsAWhitespaceOnlyDescriptionAsBlank() {
        // ifBlank, not ifEmpty: a description of spaces would otherwise ship as-is.
        vm.create("Unnamed", "   \t  ", degrees("I"), ScaleType.MAJOR)
        assertEquals(
            "Custom progression",
            vm.progressions.value
                .single()
                .progression.description,
        )
    }

    @Test
    fun createGeneratesDistinctIdsForIdenticalProgressions() {
        vm.create("Same", "Same", degrees("I"), ScaleType.MAJOR)
        vm.create("Same", "Same", degrees("I"), ScaleType.MAJOR)

        val ids = vm.progressions.value.map { it.id }
        assertEquals("identical progressions must still be distinct rows", 2, ids.toSet().size)
    }

    @Test
    fun newestProgressionsComeFirst() {
        createAndGetId("First")
        createAndGetId("Second")
        assertEquals(listOf("Second", "First"), vm.progressions.value.map { it.progression.name })
    }

    @Test
    fun emptyDegreeListsAreStoredAsIs() {
        // The ViewModel performs no validation; pinned so that adding it is deliberate.
        vm.create("Empty", "No degrees", emptyList(), ScaleType.MAJOR)
        assertTrue(
            vm.progressions.value
                .single()
                .progression.degrees
                .isEmpty(),
        )
    }

    // ── Update ───────────────────────────────────────────────────────

    @Test
    fun updatePreservesTheIdAndCreatedAt() {
        val id = createAndGetId("Original")
        val createdAt =
            vm.progressions.value
                .single()
                .createdAt

        vm.update(id, "Renamed", "New description", degrees("ii", "V", "I"), ScaleType.MINOR)

        val updated = vm.progressions.value.single()
        assertEquals("the identity must survive an edit", id, updated.id)
        assertEquals("createdAt must survive an edit", createdAt, updated.createdAt)
        assertEquals("Renamed", updated.progression.name)
        assertEquals("New description", updated.progression.description)
        assertEquals(listOf("ii", "V", "I"), updated.progression.degrees.map { it.numeral })
    }

    @Test
    fun updateCanChangeTheScaleType() {
        val id = createAndGetId("Modal", scaleType = ScaleType.MAJOR)
        vm.update(id, "Modal", "Now dorian", degrees("i", "IV"), ScaleType.DORIAN)

        assertEquals(
            ScaleType.DORIAN,
            vm.progressions.value
                .single()
                .progression.scaleType,
        )
    }

    @Test
    fun updateFallsBackToADefaultDescriptionWhenBlank() {
        val id = createAndGetId("Named")
        vm.update(id, "Named", "  ", degrees("I"), ScaleType.MAJOR)

        assertEquals(
            "Custom progression",
            vm.progressions.value
                .single()
                .progression.description,
        )
    }

    @Test
    fun updateOfAnUnknownIdIsANoOp() {
        createAndGetId("Kept", description = "Untouched")
        vm.update("no-such-id", "Ghost", "Ghost", degrees("I"), ScaleType.MAJOR)

        val all = vm.progressions.value
        assertEquals(1, all.size)
        assertEquals("Kept", all.single().progression.name)
        assertEquals("Untouched", all.single().progression.description)
    }

    @Test
    fun updateLeavesOtherProgressionsAlone() {
        val target = createAndGetId("Target")
        createAndGetId("Bystander")

        vm.update(target, "Edited", "Edited", degrees("I"), ScaleType.MAJOR)

        assertEquals(
            setOf("Edited", "Bystander"),
            vm.progressions.value
                .map { it.progression.name }
                .toSet(),
        )
    }

    // ── Delete ───────────────────────────────────────────────────────

    @Test
    fun deleteRemovesTheProgression() {
        val id = createAndGetId("Doomed")
        createAndGetId("Kept")
        vm.delete(id)

        assertEquals(listOf("Kept"), vm.progressions.value.map { it.progression.name })
    }

    @Test
    fun deleteOfAnUnknownIdLeavesTheListIntact() {
        createAndGetId("Kept")
        vm.delete("no-such-id")
        assertEquals(1, vm.progressions.value.size)
    }

    // ── Persistence ──────────────────────────────────────────────────

    @Test
    fun progressionsSurviveANewViewModelOverTheSameStorage() {
        createAndGetId("Persisted")
        assertEquals(
            listOf("Persisted"),
            CustomProgressionViewModel(app).progressions.value.map { it.progression.name },
        )
    }

    @Test
    fun deletionsSurviveANewViewModelOverTheSameStorage() {
        val id = createAndGetId("Doomed")
        vm.delete(id)
        assertTrue(CustomProgressionViewModel(app).progressions.value.isEmpty())
    }
}
