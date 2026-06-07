package com.baijum.ukufretboard.platform

import java.util.concurrent.locks.ReentrantLock

actual class PlatformLock {
    private val lock = ReentrantLock()
    actual fun lock() = lock.lock()
    actual fun unlock() = lock.unlock()
}
