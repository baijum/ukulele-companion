package com.baijum.ukufretboard.data

import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for [FavoriteVoicing.key] and [FavoriteFolder].
 *
 * [FavoriteVoicing.key] is the identity used for de-duplication when adding a
 * favorite, for merge-on-import, and for the per-folder display order. Both
 * `FavoritesRepository` and `FavoritesViewModel` compare voicings by this string,
 * so its exact shape is a cross-file contract rather than an implementation detail.
 */
class FavoriteVoicingTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private fun voicing(
        root: Int = 0,
        symbol: String = "m7",
        frets: List<Int> = listOf(0, 2, 3, 3),
    ) = FavoriteVoicing(rootPitchClass = root, chordSymbol = symbol, frets = frets)

    // ── Key shape ────────────────────────────────────────────────────

    @Test
    fun keyConcatenatesRootSymbolAndFrets() {
        assertEquals("0|m7|0,2,3,3", voicing().key, "key shape changed")
    }

    @Test
    fun keyKeepsAnEmptyChordSymbolAsAnEmptySegment() {
        assertEquals("7||0,2,3,2", voicing(root = 7, symbol = "", frets = listOf(0, 2, 3, 2)).key)
    }

    @Test
    fun mutedStringsAppearInTheKeyAsNegativeOne() {
        assertEquals("0|maj|-1,0,0,3", voicing(symbol = "maj", frets = listOf(-1, 0, 0, 3)).key)
    }

    @Test
    fun emptyFretsProduceATrailingSeparator() {
        assertEquals("5|sus2|", voicing(root = 5, symbol = "sus2", frets = emptyList()).key)
    }

    // ── Key identity ─────────────────────────────────────────────────

    @Test
    fun keyIgnoresAddedAtAndFolderIds() {
        val a = voicing().copy(addedAt = 1_000L, folderIds = emptyList())
        val b = voicing().copy(addedAt = 9_999_999L, folderIds = listOf("f1", "f2"))
        assertEquals(a.key, b.key, "key must depend only on root, symbol and frets")
    }

    @Test
    fun keyDistinguishesVoicingsThatDifferInAnyField() {
        runBlocking {
            checkAll(
                Arb.int(0..11),
                Arb.element("", "m", "m7", "sus2", "maj7"),
                Arb.list(Arb.int(-1..22), 1..4),
                Arb.int(0..11),
                Arb.element("", "m", "m7", "sus2", "maj7"),
                Arb.list(Arb.int(-1..22), 1..4),
            ) { rootA, symA, fretsA, rootB, symB, fretsB ->
                val a = voicing(rootA, symA, fretsA)
                val b = voicing(rootB, symB, fretsB)
                if (rootA == rootB && symA == symB && fretsA == fretsB) {
                    assertEquals(a.key, b.key, "identical voicings must share a key")
                } else {
                    assertNotEquals(a.key, b.key, "distinct voicings collided on ${a.key}")
                }
            }
        }
    }

    // ── Serialization ────────────────────────────────────────────────

    @Test
    fun keyIsNotWrittenToJson() {
        // @Transient keeps `key` derived. If it were persisted, every stored favorite
        // would carry a redundant field and a stale one could outlive its frets.
        val encoded = json.encodeToString(FavoriteVoicing.serializer(), voicing())
        assertFalse(encoded.contains("\"key\""), "key leaked into JSON: $encoded")
    }

    @Test
    fun keyIsRecomputedWhenDecodedFromJson() {
        val decoded =
            json.decodeFromString(
                FavoriteVoicing.serializer(),
                """{"rootPitchClass":9,"chordSymbol":"m","frets":[2,0,0,0],"addedAt":12}""",
            )
        assertEquals("9|m|2,0,0,0", decoded.key, "key was not derived on decode")
    }

    @Test
    fun decodingAPayloadContainingKeyNeedsIgnoreUnknownKeys() {
        // Pins why every favorites repository configures ignoreUnknownKeys.
        val raw = """{"rootPitchClass":0,"chordSymbol":"m7","frets":[0,2,3,3],"key":"stale"}"""
        val decoded = json.decodeFromString(FavoriteVoicing.serializer(), raw)
        assertEquals("0|m7|0,2,3,3", decoded.key, "a stored key must never win over the derived one")
    }

    @Test
    fun voicingRoundTripsThroughJson() {
        val original = voicing().copy(addedAt = 4_242L, folderIds = listOf("a", "b"))
        val decoded =
            json.decodeFromString(
                FavoriteVoicing.serializer(),
                json.encodeToString(FavoriteVoicing.serializer(), original),
            )
        assertEquals(original, decoded, "round trip lost data")
        assertEquals(original.key, decoded.key)
    }

    @Test
    fun addedAtDefaultsToTheCurrentTime() {
        assertTrue(voicing().addedAt > 0L, "addedAt should be stamped from the platform clock")
    }

    // ── Folders ──────────────────────────────────────────────────────

    @Test
    fun favoriteFolderGeneratesDistinctIdsByDefault() {
        assertNotEquals(FavoriteFolder(name = "A").id, FavoriteFolder(name = "A").id)
    }

    @Test
    fun favoriteFolderRoundTripsVoicingOrderThroughJson() {
        val folder =
            FavoriteFolder(
                id = "folder-1",
                name = "Practice",
                createdAt = 77L,
                voicingOrder = listOf("0|m7|0,2,3,3", "7||0,2,3,2"),
            )
        val decoded =
            json.decodeFromString(
                FavoriteFolder.serializer(),
                json.encodeToString(FavoriteFolder.serializer(), folder),
            )
        assertEquals(folder, decoded, "folder round trip lost data")
    }

    @Test
    fun folderVoicingOrderDefaultsToEmpty() {
        assertTrue(FavoriteFolder(name = "New").voicingOrder.isEmpty())
    }
}
