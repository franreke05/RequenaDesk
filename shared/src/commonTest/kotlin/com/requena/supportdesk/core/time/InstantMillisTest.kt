package com.requena.supportdesk.core.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InstantMillisTest {
    @Test
    fun epochInstantParsesToZero() {
        assertEquals(0L, parseIsoInstantToEpochMillis("1970-01-01T00:00:00Z"))
    }

    @Test
    fun parsesWithoutFractionalSecondsAndLandsOnAnExactDayBoundary() {
        val midnight = parseIsoInstantToEpochMillis("2025-07-30T00:00:00Z")!!
        // Any UTC midnight must be an exact multiple of a day in milliseconds.
        assertEquals(0L, midnight % 86_400_000L)
        val sameDayLater = parseIsoInstantToEpochMillis("2025-07-30T21:35:30Z")!!
        assertEquals((21 * 3600 + 35 * 60 + 30) * 1000L, sameDayLater - midnight)
    }

    @Test
    fun parsesWithFractionalSecondsOfVaryingLength() {
        val base = parseIsoInstantToEpochMillis("2026-07-30T21:35:30Z")!!
        assertEquals(base + 123, parseIsoInstantToEpochMillis("2026-07-30T21:35:30.123Z"))
        assertEquals(base + 123, parseIsoInstantToEpochMillis("2026-07-30T21:35:30.123456Z"))
        assertEquals(base + 100, parseIsoInstantToEpochMillis("2026-07-30T21:35:30.1Z"))
    }

    @Test
    fun crossesMidnightCorrectly() {
        val start = parseIsoInstantToEpochMillis("2026-07-30T23:30:00Z")!!
        val end = parseIsoInstantToEpochMillis("2026-07-31T01:00:00Z")!!
        assertEquals(90 * 60 * 1000L, end - start)
    }

    @Test
    fun handlesLeapYearFebruary() {
        val leapDay = parseIsoInstantToEpochMillis("2024-02-29T00:00:00Z")!!
        val nextDay = parseIsoInstantToEpochMillis("2024-03-01T00:00:00Z")!!
        assertEquals(86_400_000L, nextDay - leapDay)
    }

    @Test
    fun returnsNullForMalformedInput() {
        assertNull(parseIsoInstantToEpochMillis("not-a-date"))
        assertNull(parseIsoInstantToEpochMillis(""))
    }
}
