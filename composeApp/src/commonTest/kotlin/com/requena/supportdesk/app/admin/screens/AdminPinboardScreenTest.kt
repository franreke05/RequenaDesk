package com.requena.supportdesk.app.admin.screens

import com.requena.supportdesk.core.model.TaskBoardPlacement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminPinboardScreenTest {
    @Test
    fun `week starts on Monday and crosses month boundaries`() {
        val thursday = BoardDate.parse("2026-07-30")

        assertEquals("2026-07-27", thursday.startOfWeek().iso)
        assertEquals("2026-08-06", thursday.plusDays(7).iso)
        assertEquals("2026-07-23", thursday.plusDays(-7).iso)
    }

    @Test
    fun `calendar always contains six complete weeks`() {
        val cells = calendarCells(BoardMonth(2026, 8))

        assertEquals(42, cells.size)
        assertEquals("2026-07-27", cells.first().iso)
        assertEquals("2026-09-06", cells.last().iso)
    }

    @Test
    fun `new task chooses a free board slot`() {
        val occupied = listOf(
            placement("task-1", 0.04, 0.04),
            placement("task-2", 0.38, 0.04),
        )

        val next = nextAvailablePosition(occupied)

        assertEquals(0.72, next.first)
        assertEquals(0.04, next.second)
        assertTrue(occupied.none { it.positionX == next.first && it.positionY == next.second })
    }

    @Test
    fun `normalized coordinate preserves relative position after resizing`() {
        val normalized = normalizedPosition(pixel = 420f, trackSizePx = 1_000f)

        assertEquals(0.42, normalized, absoluteTolerance = 0.000001)
        assertEquals(210f, pixelPosition(normalized, trackSizePx = 500f))
        assertEquals(1.0, normalizedPosition(pixel = 800f, trackSizePx = 500f))
    }
}

private fun placement(taskId: String, x: Double, y: Double) = TaskBoardPlacement(
    taskId = taskId,
    boardDate = "2026-07-30",
    positionX = x,
    positionY = y,
    createdAt = "",
    updatedAt = "",
)
