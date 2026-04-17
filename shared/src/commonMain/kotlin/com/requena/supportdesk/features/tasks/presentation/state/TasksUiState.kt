package com.requena.supportdesk.features.tasks.presentation.state

import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.core.time.isFutureIsoDate
import com.requena.supportdesk.core.time.isPastIsoDate
import com.requena.supportdesk.core.time.isTodayIsoDate

data class TasksUiState(
    val categories: List<TaskCategory> = emptyList(),
    val tasks: List<WorkTask> = emptyList(),
    val logs: List<TaskLog> = emptyList(),
    val selectedDay: String? = null,
    val selectedTaskId: String? = null,
    val selectedCategoryId: String? = null,
    val selectedClientFilterId: String? = null,
    val selectedDashboardClientId: String? = null,
    val activeTaskId: String? = null,
    val activeTaskSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
) {
    val todayIsoDate: String
        get() = currentIsoDate()

    val activeTask: WorkTask?
        get() = tasks.firstOrNull { it.id == activeTaskId }

    val selectedTask: WorkTask?
        get() = tasks.firstOrNull { it.id == selectedTaskId } ?: activeTask

    val selectedCategory: TaskCategory?
        get() = categories.firstOrNull { it.id == selectedCategoryId }

    val selectedDayLogs: List<TaskLog>
        get() {
            val day = selectedDay ?: return emptyList()
            return logs.filter { it.workDate == day }
        }

    val selectedDayMinutes: Int
        get() = selectedDayLogs.sumOf { it.minutes }

    val selectedDaySeconds: Int
        get() = selectedDayLogs.sumOf { it.seconds }

    val selectedDayIsToday: Boolean
        get() = selectedDay?.let(::isTodayIsoDate) == true

    val selectedDayIsPast: Boolean
        get() = selectedDay?.let(::isPastIsoDate) == true

    val selectedDayIsFuture: Boolean
        get() = selectedDay?.let(::isFutureIsoDate) == true

    val canTrackSelectedDay: Boolean
        get() = selectedDayIsToday

    val filteredTasks: List<WorkTask>
        get() = tasks.filter { task ->
            (selectedCategoryId == null || task.categoryId == selectedCategoryId) &&
                (selectedClientFilterId == null || task.clientId == selectedClientFilterId)
        }

    val dashboardClientTasks: List<WorkTask>
        get() = tasks.filter { task ->
            selectedDashboardClientId == null || task.clientId == selectedDashboardClientId
        }

    fun trackedSecondsFor(task: WorkTask): Int =
        task.loggedSeconds + if (activeTaskId == task.id) activeTaskSeconds else 0

    val selectedTaskTrackedSeconds: Int
        get() = selectedTask?.let(::trackedSecondsFor) ?: activeTaskSeconds
}
