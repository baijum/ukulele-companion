package com.baijum.ukufretboard.ui.songbook

/**
 * Accumulates a fractional auto-scroll speed (pixels per tick) and hands back
 * whole-pixel steps, carrying the sub-pixel remainder to later ticks.
 *
 * `scrollState.animateScrollTo` only accepts an integer pixel offset, so naively
 * calling `speed.toInt().coerceAtLeast(1)` truncated every fractional speed up to
 * at least one pixel per tick — making 0.5x indistinguishable from 1x and every
 * half-step in Performance mode inert (see issue #583). Accumulating the remainder
 * makes 0.5x scroll one pixel every other tick and 1.5x three pixels every two
 * ticks, on average.
 *
 * A fresh instance must be created whenever the speed changes or auto-scroll
 * restarts so the carried remainder does not leak across sessions. Both call sites
 * create it inside a `LaunchedEffect` keyed on the speed, which does exactly that.
 */
class AutoScrollAccumulator {
    private var remainder = 0f

    /**
     * Advance by [speed] pixels and return the whole pixels to scroll this tick
     * (0 when the accumulated fraction has not yet reached a full pixel). Negative
     * or zero speeds contribute nothing.
     */
    fun nextDelta(speed: Float): Int {
        if (speed <= 0f) return 0
        remainder += speed
        val delta = remainder.toInt()
        remainder -= delta
        return delta
    }
}
