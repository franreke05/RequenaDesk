package com.requena.supportdesk.features.invoices.domain.model

import com.requena.supportdesk.core.model.TaskLog

enum class UnitBillingState {
    PENDING,
    BILLED,
}

/**
 * The atomic, immutable thing a user can select for an invoice: one TaskLog row - a task's
 * real minutes on one local day, or (when [workDate] is null) the migrated "historic time
 * without a daily breakdown" bucket. A session that touches two local days already exists as
 * two separate units sharing the same [sessionId], so "select a whole session" simply means
 * selecting every unit with that id, and "select a day" means every unit for that task on that
 * date - never a fraction of one.
 */
data class BillableUnit(
    val logId: String,
    val taskId: String,
    val sessionId: String?,
    val workDate: String?,
    val minutes: Int,
    val isHistoricMigrated: Boolean,
    val billingState: UnitBillingState,
)

/** Only real, billable (non-internal) time can ever become a billable unit. */
fun billableUnitsFor(logs: List<TaskLog>, billedLogIds: Set<String>): List<BillableUnit> =
    logs.filter { it.billable }.map { log ->
        BillableUnit(
            logId = log.id,
            taskId = log.taskId,
            sessionId = log.sessionId,
            workDate = log.workDate,
            minutes = log.minutes,
            isHistoricMigrated = log.isHistoricMigrated,
            billingState = if (log.id in billedLogIds) UnitBillingState.BILLED else UnitBillingState.PENDING,
        )
    }

fun List<BillableUnit>.pending(): List<BillableUnit> = filter { it.billingState == UnitBillingState.PENDING }

/** Every pending unit for one task, used when the user selects "the whole task". */
fun List<BillableUnit>.pendingForTask(taskId: String): List<BillableUnit> =
    pending().filter { it.taskId == taskId }

/** Every pending unit for one task on one local day (workDate == null never matches a day). */
fun List<BillableUnit>.pendingForTaskAndDay(taskId: String, workDate: String): List<BillableUnit> =
    pending().filter { it.taskId == taskId && it.workDate == workDate }

/** Every pending unit belonging to the same session, regardless of which local day it fell on. */
fun List<BillableUnit>.pendingForSession(sessionId: String): List<BillableUnit> =
    pending().filter { it.sessionId == sessionId }

data class TaskInvoiceLine(
    val taskId: String,
    val realMinutes: Int,
    val roundedMinutes: Int,
) {
    val decimalHours: Double get() = quarterHourMinutesToDecimalHours(roundedMinutes)
}

/**
 * Builds the single invoice line for a task: sum the real minutes of every unit the user
 * selected for that task, then round the total once. Never call this per-day or per-session.
 */
fun buildTaskInvoiceLine(taskId: String, selectedUnits: List<BillableUnit>): TaskInvoiceLine {
    val realMinutes = selectedUnits.filter { it.taskId == taskId }.sumOf { it.minutes }
    return TaskInvoiceLine(taskId = taskId, realMinutes = realMinutes, roundedMinutes = roundUpToQuarterHour(realMinutes))
}
