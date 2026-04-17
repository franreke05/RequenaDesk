package com.requena.supportdesk.features.tasks.domain.repository

import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.tasks.domain.model.TaskDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskLabelDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskTimeLogDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskUpdateInput

interface TasksRepository {
    suspend fun getLabels(): AppResult<List<TaskCategory>>
    suspend fun getTasks(): AppResult<List<WorkTask>>
    suspend fun getTimeLogs(): AppResult<List<TaskLog>>
    suspend fun createTask(input: TaskDraft): AppResult<WorkTask>
    suspend fun updateTask(taskId: String, input: TaskUpdateInput): AppResult<WorkTask>
    suspend fun deleteTask(taskId: String): AppResult<Unit>
    suspend fun createLabel(input: TaskLabelDraft): AppResult<TaskCategory>
    suspend fun updateLabel(labelId: String, input: TaskLabelDraft): AppResult<TaskCategory>
    suspend fun deleteLabel(labelId: String): AppResult<Unit>
    suspend fun createTimeLog(input: TaskTimeLogDraft): AppResult<TaskLog>
}
