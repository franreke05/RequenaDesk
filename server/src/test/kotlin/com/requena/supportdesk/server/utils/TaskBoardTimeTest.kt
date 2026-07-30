package com.requena.supportdesk.server.utils

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskBoardTimeTest {
    private val madrid = ZoneId.of(TASK_BOARD_TIME_ZONE)

    @Test
    fun `refresh waits until the next Madrid midnight`() {
        val boundary = madridTaskBoardDayBoundary(
            ZonedDateTime.of(2026, 7, 30, 23, 59, 30, 0, madrid),
        )

        assertEquals("2026-07-30", boundary.today.toString())
        assertEquals(30, boundary.refreshAfterSeconds)
    }

    @Test
    fun `Madrid daylight saving boundaries keep calendar-day expiration`() {
        val springForward = madridTaskBoardDayBoundary(
            ZonedDateTime.of(2026, 3, 29, 0, 0, 0, 0, madrid),
        )
        val autumnBack = madridTaskBoardDayBoundary(
            ZonedDateTime.of(2026, 10, 25, 0, 0, 0, 0, madrid),
        )

        assertEquals(23 * 60 * 60, springForward.refreshAfterSeconds)
        assertEquals(25 * 60 * 60, autumnBack.refreshAfterSeconds)
    }
}
