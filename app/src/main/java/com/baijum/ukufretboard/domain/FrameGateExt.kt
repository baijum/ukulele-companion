package com.baijum.ukufretboard.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val DEFAULT_TIMEOUT_MS = 500L

/**
 * Suspends until the gate is free, then enters. Safe to call from the main
 * thread — yields between attempts via [delay] instead of busy-spinning.
 *
 * Returns true if the gate was entered, false if the timeout elapsed.
 * The default 500ms timeout is generous for a single DSP frame (typically <50ms).
 */
suspend fun FrameGate.awaitEnterSuspending(timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
    if (tryEnter()) return true
    val result =
        withTimeoutOrNull(timeoutMs) {
            while (!tryEnter()) {
                delay(1)
            }
            true
        }
    return result == true
}
