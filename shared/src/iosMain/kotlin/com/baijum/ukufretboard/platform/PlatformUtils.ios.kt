package com.baijum.ukufretboard.platform

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDayOfYear
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

actual fun generateUuid(): String = NSUUID().UUIDString()

actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun currentYear(): Int {
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(NSCalendarUnitYear, fromDate = NSDate())
    return components.year.toInt()
}

actual fun currentDayOfYear(): Int {
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(NSCalendarUnitDayOfYear, fromDate = NSDate())
    return components.day.toInt()
}
