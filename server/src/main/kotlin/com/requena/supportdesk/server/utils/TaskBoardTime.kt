package com.requena.supportdesk.server.utils

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

const val TASK_BOARD_TIME_ZONE = "Europe/Madrid"

data class TaskBoardDayBoundary(
    val today: LocalDate,
    val refreshAfterSeconds: Long,
)

fun madridTaskBoardDayBoundary(
    now: ZonedDateTime = ZonedDateTime.now(ZoneId.of(TASK_BOARD_TIME_ZONE)),
): TaskBoardDayBoundary {
    val zone = ZoneId.of(TASK_BOARD_TIME_ZONE)
    val madridNow = now.withZoneSameInstant(zone)
    return TaskBoardDayBoundary(
        today = madridNow.toLocalDate(),
        refreshAfterSeconds = Duration.between(
            madridNow,
            madridNow.toLocalDate().plusDays(1).atStartOfDay(zone),
        ).seconds.coerceAtLeast(1),
    )
}
