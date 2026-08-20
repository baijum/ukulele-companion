package com.baijum.ukufretboard.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [awaitEnterSuspending].
 *
 * The suspending wrapper exists so the audio path can wait for an in-flight DSP
 * frame without busy-spinning on the main thread (#462). These tests use real
 * time rather than a virtual clock — `kotlinx-coroutines-test` is not on the
 * classpath — so every case passes an explicit short timeout and the whole
 * class runs in well under a second.
 */
class FrameGateExtTest {
    private val shortTimeoutMs = 50L

    // ── Fast path ────────────────────────────────────────────────────

    @Test
    fun takesAFreeGateImmediately() =
        runBlocking {
            assertTrue("a free gate should be entered", FrameGate().awaitEnterSuspending(shortTimeoutMs))
        }

    @Test
    fun aZeroTimeoutStillTakesAFreeGate() =
        runBlocking {
            // The tryEnter() fast path runs before withTimeoutOrNull, so a zero
            // budget must not stop an uncontended entry.
            assertTrue(FrameGate().awaitEnterSuspending(timeoutMs = 0L))
        }

    @Test
    fun enteringHoldsTheGateAgainstTheNextTryEnter() =
        runBlocking {
            val gate = FrameGate()
            assertTrue(gate.awaitEnterSuspending(shortTimeoutMs))
            assertFalse("the gate should now be held", gate.tryEnter())
        }

    // ── Timeout ──────────────────────────────────────────────────────

    @Test
    fun returnsFalseWhenTheGateStaysBusy() =
        runBlocking {
            val gate = FrameGate()
            assertTrue(gate.tryEnter())

            val start = System.nanoTime()
            val entered = gate.awaitEnterSuspending(shortTimeoutMs)
            val elapsedMs = (System.nanoTime() - start) / 1_000_000

            assertFalse("a held gate must not be entered", entered)
            assertTrue("it should have waited out the timeout, took ${elapsedMs}ms", elapsedMs >= shortTimeoutMs)
            assertTrue("it must not fall back to the 500ms default, took ${elapsedMs}ms", elapsedMs < 400)
        }

    @Test
    fun aTimedOutAwaitDoesNotLeakAnEntry() =
        runBlocking {
            val gate = FrameGate()
            assertTrue(gate.tryEnter())
            assertFalse(gate.awaitEnterSuspending(shortTimeoutMs))

            gate.exit()
            assertTrue("the original holder's exit should free the gate", gate.tryEnter())
            assertFalse("and only one caller may hold it", gate.tryEnter())
        }

    // ── Handover ─────────────────────────────────────────────────────

    @Test
    fun succeedsOnceAnotherCoroutineExits() =
        runBlocking {
            val gate = FrameGate()
            assertTrue(gate.tryEnter())

            // The poller yields via delay(1), so the releasing coroutine gets to run
            // on the same event loop without needing a separate dispatcher.
            val waiter = async { gate.awaitEnterSuspending(timeoutMs = 2_000L) }
            launch {
                delay(20)
                gate.exit()
            }

            assertTrue("the waiter should take the gate once it is released", waiter.await())
        }

    @Test
    fun theGateIsReusableAcrossSuccessiveFrames() =
        runBlocking {
            val gate = FrameGate()
            repeat(3) { frame ->
                assertTrue("frame $frame should enter", gate.awaitEnterSuspending(shortTimeoutMs))
                gate.exit()
            }
            assertTrue("the gate should be free at the end", gate.tryEnter())
        }
}
