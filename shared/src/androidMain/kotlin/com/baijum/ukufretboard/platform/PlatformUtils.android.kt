package com.baijum.ukufretboard.platform

import java.util.Calendar
import java.util.UUID

actual fun generateUuid(): String = UUID.randomUUID().toString()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

actual fun currentDayOfYear(): Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
