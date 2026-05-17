package com.requena.supportdesk.core.time

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.localeWithLocaleIdentifier

actual fun currentIsoDate(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    formatter.locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
    return formatter.stringFromDate(NSDate())
}

actual fun currentIsoDateTime(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    formatter.locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
    return formatter.stringFromDate(NSDate())
}
