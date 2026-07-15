package com.requena.supportdesk.core.time

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.localeWithLocaleIdentifier
import platform.Foundation.localTimeZone

actual fun currentIsoDate(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    formatter.locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
    formatter.timeZone = NSTimeZone.localTimeZone
    return formatter.stringFromDate(NSDate())
}
