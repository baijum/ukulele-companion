package com.baijum.ukufretboard.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FrameGateTest {

    @Test
    fun tryEnterSucceedsOnFreeGate() {
        val gate = FrameGate()
        assertTrue(gate.tryEnter())
    }

    @Test
    fun tryEnterFailsWhenAlreadyEntered() {
        val gate = FrameGate()
        assertTrue(gate.tryEnter())
        assertFalse(gate.tryEnter(), "second tryEnter should be rejected (drop)")
    }

    @Test
    fun exitReleasesGate() {
        val gate = FrameGate()
        gate.tryEnter()
        gate.exit()
        assertTrue(gate.tryEnter(), "tryEnter should succeed after exit")
    }

    @Test
    fun multipleEnterExitCycles() {
        val gate = FrameGate()
        repeat(100) {
            assertTrue(gate.tryEnter())
            assertFalse(gate.tryEnter())
            gate.exit()
        }
    }

    @Test
    fun isActiveReflectsState() {
        val gate = FrameGate()
        assertFalse(gate.isActive, "new gate should be inactive")
        gate.tryEnter()
        assertTrue(gate.isActive, "entered gate should be active")
        gate.exit()
        assertFalse(gate.isActive, "exited gate should be inactive")
    }
}
