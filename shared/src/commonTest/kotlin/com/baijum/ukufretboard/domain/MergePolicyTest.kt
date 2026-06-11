package com.baijum.ukufretboard.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MergePolicyTest {

    private data class Item(val id: String, val timestamp: Long, val value: String)

    private fun merge(existing: List<Item>, incoming: List<Item>) = mergeNewerWins(
        existing = existing,
        incoming = incoming,
        idOf = { it.id },
        timestampOf = { it.timestamp },
    )

    @Test
    fun mergingEmptyListsReturnsEmpty() {
        val result = merge(emptyList(), emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun mergingIntoEmptyExistingReturnsAllIncoming() {
        val incoming = listOf(Item("a", 100, "x"), Item("b", 200, "y"))
        val result = merge(emptyList(), incoming)
        assertEquals(2, result.size)
        assertTrue(result.containsAll(incoming))
    }

    @Test
    fun mergingEmptyIncomingReturnsExisting() {
        val existing = listOf(Item("a", 100, "x"), Item("b", 200, "y"))
        val result = merge(existing, emptyList())
        assertEquals(2, result.size)
        assertTrue(result.containsAll(existing))
    }

    @Test
    fun newerIncomingWins() {
        val existing = listOf(Item("a", 100, "old"))
        val incoming = listOf(Item("a", 200, "new"))
        val result = merge(existing, incoming)
        assertEquals(1, result.size)
        assertEquals("new", result.first().value)
        assertEquals(200, result.first().timestamp)
    }

    @Test
    fun olderIncomingIsKept() {
        val existing = listOf(Item("a", 200, "existing"))
        val incoming = listOf(Item("a", 100, "old"))
        val result = merge(existing, incoming)
        assertEquals(1, result.size)
        assertEquals("existing", result.first().value)
        assertEquals(200, result.first().timestamp)
    }

    @Test
    fun newItemsAreAdded() {
        val existing = listOf(Item("a", 100, "x"))
        val incoming = listOf(Item("b", 200, "y"))
        val result = merge(existing, incoming)
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "a" && it.value == "x" })
        assertTrue(result.any { it.id == "b" && it.value == "y" })
    }

    @Test
    fun equalTimestampsIncomingWins() {
        val existing = listOf(Item("a", 100, "old"))
        val incoming = listOf(Item("a", 100, "incoming"))
        val result = merge(existing, incoming)
        assertEquals(1, result.size)
        assertEquals("incoming", result.first().value)
    }

    @Test
    fun mixedScenarioMergesCorrectly() {
        val existing = listOf(
            Item("a", 300, "a-existing"),
            Item("b", 100, "b-existing"),
            Item("c", 200, "c-existing"),
        )
        val incoming = listOf(
            Item("a", 100, "a-old"),
            Item("b", 200, "b-newer"),
            Item("d", 400, "d-new"),
        )
        val result = merge(existing, incoming)
        assertEquals(4, result.size)
        val byId = result.associateBy { it.id }
        assertEquals("a-existing", byId["a"]!!.value)
        assertEquals("b-newer", byId["b"]!!.value)
        assertEquals("c-existing", byId["c"]!!.value)
        assertEquals("d-new", byId["d"]!!.value)
    }
}
