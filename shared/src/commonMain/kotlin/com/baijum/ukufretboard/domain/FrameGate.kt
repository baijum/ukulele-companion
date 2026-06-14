package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.platform.PlatformLock

/**
 * Gate for audio frame processing. Only one frame can be in-flight at
 * a time; additional frames are dropped ([tryEnter] returns false).
 *
 * Usage patterns:
 * - **Drop-if-busy** (audio callback): `if (!gate.tryEnter()) return`
 * - **Non-blocking drain** (start/stop in coroutines): use [awaitEnterSuspending] extension
 *
 * Thread-safe: all access is serialized through [PlatformLock].
 */
class FrameGate {
    private val lock = PlatformLock()
    private var processing = false

    /** Try to enter the gate. Returns false if a frame is already in-flight. */
    fun tryEnter(): Boolean {
        lock.lock()
        try {
            if (processing) return false
            processing = true
            return true
        } finally {
            lock.unlock()
        }
    }

    /** Release the gate after frame processing completes. */
    fun exit() {
        lock.lock()
        try {
            processing = false
        } finally {
            lock.unlock()
        }
    }

    /** Returns true if a frame is currently being processed. */
    val isActive: Boolean
        get() {
            lock.lock()
            try {
                return processing
            } finally {
                lock.unlock()
            }
        }
}
