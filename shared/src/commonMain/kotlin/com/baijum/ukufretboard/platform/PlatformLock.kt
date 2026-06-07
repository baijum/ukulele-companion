package com.baijum.ukufretboard.platform

expect class PlatformLock() {
    fun lock()
    fun unlock()
}

inline fun <T> PlatformLock.withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
