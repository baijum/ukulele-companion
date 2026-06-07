package com.baijum.ukufretboard.platform

import platform.Foundation.NSRecursiveLock

actual class PlatformLock {
    private val lock = NSRecursiveLock()
    actual fun lock() = lock.lock()
    actual fun unlock() = lock.unlock()
}
