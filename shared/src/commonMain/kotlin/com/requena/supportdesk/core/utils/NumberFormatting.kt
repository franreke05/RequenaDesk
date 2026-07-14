package com.requena.supportdesk.core.utils

import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.round

// java.util.Formatter-backed String.format is JVM-only; this stays commonMain-safe for iOS.
fun Double.toFixedString(decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val scaled = round(this * factor).toLong()
    val whole = scaled.absoluteValue / factor.toLong()
    val fraction = (scaled.absoluteValue % factor.toLong()).toString().padStart(decimals, '0')
    val sign = if (scaled < 0) "-" else ""
    return "$sign$whole.$fraction"
}
