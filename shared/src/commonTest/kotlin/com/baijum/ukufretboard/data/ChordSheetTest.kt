package com.baijum.ukufretboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ChordSheetTest {

    private fun sheet() = ChordSheet(
        id = "test-id",
        title = "Test Song",
        content = "[C]Hello",
        createdAt = 1000L,
        updatedAt = 2000L,
    )

    @Test
    fun statsFieldsDefaultToZero() {
        val s = sheet()
        assertEquals(0, s.viewCount)
        assertEquals(0L, s.lastViewedAt)
        assertEquals(0L, s.totalViewTimeMs)
    }

    @Test
    fun copyUpdatesStatsWhilePreservingOtherFields() {
        val original = sheet()
        val updated = original.copy(
            viewCount = 3,
            lastViewedAt = 5000L,
            totalViewTimeMs = 120000L,
        )
        assertEquals(3, updated.viewCount)
        assertEquals(5000L, updated.lastViewedAt)
        assertEquals(120000L, updated.totalViewTimeMs)
        assertEquals(original.id, updated.id)
        assertEquals(original.title, updated.title)
        assertEquals(original.content, updated.content)
        assertEquals(original.createdAt, updated.createdAt)
    }

    @Test
    fun copyStatsDoesNotAlterUpdatedAt() {
        val original = sheet()
        val updated = original.copy(viewCount = 10, totalViewTimeMs = 60000L)
        assertEquals(original.updatedAt, updated.updatedAt)
    }
}
