package com.requena.supportdesk.core.time

/** Device clock, in epoch milliseconds - used only to tick the active timer's visual display. */
expect fun currentEpochMillis(): Long

/**
 * Parses a UTC ISO-8601 instant (e.g. "2026-07-30T21:30:00.123456Z", as emitted by the server's
 * `DateTimeFormatter.ISO_INSTANT`/`Instant.toString()`) into epoch milliseconds, or null if it
 * cannot be parsed. Pure Kotlin (no platform date APIs) so it behaves identically on every
 * target, including iOS where this cannot be verified by compiling on this machine.
 */
fun parseIsoInstantToEpochMillis(iso: String): Long? = runCatching {
    val trimmed = iso.trim().removeSuffix("Z")
    val tIndex = trimmed.indexOf('T')
    require(tIndex > 0) { "Missing 'T' separator" }
    val datePart = trimmed.substring(0, tIndex)
    val timePart = trimmed.substring(tIndex + 1)

    val dateSegments = datePart.split("-")
    require(dateSegments.size == 3) { "Malformed date part" }
    val year = dateSegments[0].toInt()
    val month = dateSegments[1].toInt()
    val day = dateSegments[2].toInt()

    val dotIndex = timePart.indexOf('.')
    val timeMain = if (dotIndex >= 0) timePart.substring(0, dotIndex) else timePart
    val fraction = if (dotIndex >= 0) timePart.substring(dotIndex + 1) else ""
    val timeSegments = timeMain.split(":")
    require(timeSegments.size == 3) { "Malformed time part" }
    val hour = timeSegments[0].toInt()
    val minute = timeSegments[1].toInt()
    val second = timeSegments[2].toInt()
    val millis = fraction.take(3).padEnd(3, '0').let { if (it.isBlank()) 0 else it.toInt() }

    val days = daysFromCivil(year, month, day)
    days * 86_400_000L + hour * 3_600_000L + minute * 60_000L + second * 1000L + millis
}.getOrNull()

/**
 * Days since the epoch (1970-01-01) for a proleptic-Gregorian civil date. Howard Hinnant's
 * well-known `days_from_civil` algorithm - see http://howardhinnant.github.io/date_algorithms.html.
 */
private fun daysFromCivil(y: Int, m: Int, d: Int): Long {
    val year = (if (m <= 2) y - 1 else y).toLong()
    val era = (if (year >= 0) year else year - 399) / 400
    val yoe = year - era * 400
    val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097L + doe - 719468L
}
