package com.baijum.ukufretboard.ui.songbook

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression tests for issue #583: fractional auto-scroll speeds were truncated
 * with `toInt().coerceAtLeast(1)`, making 0.5x identical to 1x and every
 * Performance-mode half-step inert. The accumulator must carry the sub-pixel
 * remainder instead.
 */
class AutoScrollAccumulatorTest {
    @Test
    fun `0_5x yields one pixel every other tick`() {
        val acc = AutoScrollAccumulator()
        val deltas = (1..6).map { acc.nextDelta(0.5f) }
        // 0.5, 1.0, 1.5, 2.0, 2.5, 3.0 -> 0,1,0,1,0,1
        assertEquals(listOf(0, 1, 0, 1, 0, 1), deltas)
        assertEquals("three pixels over six ticks", 3, deltas.sum())
    }

    @Test
    fun `1_5x yields three pixels per two ticks`() {
        val acc = AutoScrollAccumulator()
        val deltas = (1..4).map { acc.nextDelta(1.5f) }
        // 1.5, 3.0, 4.5, 6.0 -> 1,2,1,2
        assertEquals(listOf(1, 2, 1, 2), deltas)
        assertEquals("six pixels over four ticks", 6, deltas.sum())
    }

    @Test
    fun `1x yields one pixel every tick`() {
        val acc = AutoScrollAccumulator()
        repeat(5) { assertEquals(1, acc.nextDelta(1f)) }
    }

    @Test
    fun `whole speeds move exactly that many pixels each tick`() {
        val acc = AutoScrollAccumulator()
        assertEquals(2, acc.nextDelta(2f))
        assertEquals(3, acc.nextDelta(3f))
    }

    @Test
    fun `zero and negative speeds never scroll`() {
        val acc = AutoScrollAccumulator()
        assertEquals(0, acc.nextDelta(0f))
        assertEquals(0, acc.nextDelta(-1f))
    }

    @Test
    fun `0_5x accumulated across many ticks averages half a pixel`() {
        val acc = AutoScrollAccumulator()
        val total = (1..100).sumOf { acc.nextDelta(0.5f) }
        assertEquals(50, total)
    }
}
