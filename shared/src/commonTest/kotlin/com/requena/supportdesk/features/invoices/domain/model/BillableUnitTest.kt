package com.requena.supportdesk.features.invoices.domain.model

import com.requena.supportdesk.core.model.TaskLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BillableUnitTest {
    private fun log(
        id: String,
        taskId: String = "task-1",
        sessionId: String? = "session-1",
        workDate: String? = "2026-07-30",
        minutes: Int = 60,
        billable: Boolean = true,
        isHistoricMigrated: Boolean = false,
    ) = TaskLog(
        id = id,
        taskId = taskId,
        authorId = "user-admin",
        authorName = "Admin",
        minutes = minutes,
        workDate = workDate,
        note = "",
        billable = billable,
        createdAt = "2026-07-30T10:00:00Z",
        sessionId = sessionId,
        isHistoricMigrated = isHistoricMigrated,
    )

    @Test
    fun excludesInternalNonBillableLogsEntirely() {
        val logs = listOf(log(id = "log-1", billable = true), log(id = "log-2", billable = false))
        val units = billableUnitsFor(logs, billedLogIds = emptySet())
        assertEquals(1, units.size)
        assertEquals("log-1", units.single().logId)
    }

    @Test
    fun marksAlreadyBilledUnitsAndExcludesThemFromPending() {
        val logs = listOf(log(id = "log-1"), log(id = "log-2"))
        val units = billableUnitsFor(logs, billedLogIds = setOf("log-1"))
        assertEquals(UnitBillingState.BILLED, units.first { it.logId == "log-1" }.billingState)
        assertEquals(UnitBillingState.PENDING, units.first { it.logId == "log-2" }.billingState)
        assertEquals(listOf("log-2"), units.pending().map { it.logId })
    }

    @Test
    fun selectingATaskOnlyTakesItsPendingUnits() {
        val logs = listOf(
            log(id = "log-1", taskId = "task-1"),
            log(id = "log-2", taskId = "task-1"),
            log(id = "log-3", taskId = "task-2"),
        )
        val units = billableUnitsFor(logs, billedLogIds = setOf("log-2"))
        val selected = units.pendingForTask("task-1")
        assertEquals(listOf("log-1"), selected.map { it.logId })
    }

    @Test
    fun selectingADayOnlyTakesThatTaskAndDate() {
        val logs = listOf(
            log(id = "log-1", taskId = "task-1", workDate = "2026-07-30"),
            log(id = "log-2", taskId = "task-1", workDate = "2026-07-31"),
        )
        val units = billableUnitsFor(logs, billedLogIds = emptySet())
        val selected = units.pendingForTaskAndDay("task-1", "2026-07-30")
        assertEquals(listOf("log-1"), selected.map { it.logId })
    }

    @Test
    fun selectingASessionSpansBothLocalDaysItTouched() {
        // A session crossing midnight produces two units sharing the same sessionId.
        val logs = listOf(
            log(id = "log-1", sessionId = "session-x", workDate = "2026-07-30", minutes = 30),
            log(id = "log-2", sessionId = "session-x", workDate = "2026-07-31", minutes = 60),
        )
        val units = billableUnitsFor(logs, billedLogIds = emptySet())
        val selected = units.pendingForSession("session-x")
        assertEquals(setOf("log-1", "log-2"), selected.map { it.logId }.toSet())
        assertEquals(90, selected.sumOf { it.minutes })
    }

    @Test
    fun historicMigratedBucketHasNoWorkDateAndDoesNotMatchAnyDay() {
        val logs = listOf(log(id = "log-historic", workDate = null, minutes = 500, isHistoricMigrated = true))
        val units = billableUnitsFor(logs, billedLogIds = emptySet())
        val unit = units.single()
        assertTrue(unit.isHistoricMigrated)
        assertEquals(null, unit.workDate)
        // It's still selectable at the task level even though it can't be grouped under a day.
        assertEquals(listOf("log-historic"), units.pendingForTask("task-1").map { it.logId })
        assertEquals(emptyList(), units.pendingForTaskAndDay("task-1", "2026-07-30").map { it.logId })
    }

    @Test
    fun taskInvoiceLineSumsRealMinutesThenRoundsOnce() {
        val selected = listOf(
            BillableUnit("log-1", "task-1", "s1", "2026-07-30", minutes = 7, isHistoricMigrated = false, billingState = UnitBillingState.PENDING),
            BillableUnit("log-2", "task-1", "s2", "2026-07-30", minutes = 8, isHistoricMigrated = false, billingState = UnitBillingState.PENDING),
        )
        val line = buildTaskInvoiceLine("task-1", selected)
        assertEquals(15, line.realMinutes)
        assertEquals(15, line.roundedMinutes)
        assertEquals(0.25, line.decimalHours)
    }

    @Test
    fun taskInvoiceLineIgnoresUnitsFromOtherTasks() {
        val selected = listOf(
            BillableUnit("log-1", "task-1", "s1", "2026-07-30", minutes = 60, isHistoricMigrated = false, billingState = UnitBillingState.PENDING),
            BillableUnit("log-2", "task-2", "s2", "2026-07-30", minutes = 999, isHistoricMigrated = false, billingState = UnitBillingState.PENDING),
        )
        val line = buildTaskInvoiceLine("task-1", selected)
        assertEquals(60, line.realMinutes)
        assertEquals(60, line.roundedMinutes)
    }
}
