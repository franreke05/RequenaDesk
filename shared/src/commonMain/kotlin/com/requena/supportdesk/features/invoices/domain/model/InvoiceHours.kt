package com.requena.supportdesk.features.invoices.domain.model

/**
 * Rounds a task invoice line's TOTAL real minutes - already summed across every pending
 * billable unit selected for that task - up to the next quarter hour (15 min). Must be applied
 * exactly once per invoice line: sum every selected unit's real minutes first, then round the
 * total a single time. Never round a segment, session, or day individually - two 7 and 8 minute
 * sessions on the same task must produce a 15-minute line, not two 15-minute lines.
 */
fun roundUpToQuarterHour(totalMinutes: Int): Int {
    val minutes = totalMinutes.coerceAtLeast(0)
    if (minutes == 0) return 0
    return ((minutes + QUARTER_HOUR_MINUTES - 1) / QUARTER_HOUR_MINUTES) * QUARTER_HOUR_MINUTES
}

/** Converts already-rounded minutes (always a multiple of 15) into the decimal hours an invoice line needs. */
fun quarterHourMinutesToDecimalHours(roundedMinutes: Int): Double = roundedMinutes / MINUTES_PER_HOUR

private const val QUARTER_HOUR_MINUTES = 15
private const val MINUTES_PER_HOUR = 60.0
