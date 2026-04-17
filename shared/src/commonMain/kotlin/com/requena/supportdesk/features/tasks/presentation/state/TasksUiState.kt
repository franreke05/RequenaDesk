package com.requena.supportdesk.features.tasks.presentation.state

import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkTask

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

    val filteredTasks: List<WorkTask>
        get() = tasks.filter { task ->
            (selectedCategoryId == null || task.categoryId == selectedCategoryId) &&
                (selectedClientFilterId == null || task.clientId == selectedClientFilterId)
        }

    val dashboardClientTasks: List<WorkTask>
        get() = tasks.filter { task ->
            selectedDashboardClientId == null || task.clientId == selectedDashboardClientId
        }
}
