package com.requena.supportdesk.features.tasks.data.repository

import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.tasks.data.datasource.TasksDataSource
import com.requena.supportdesk.features.tasks.data.dto.CreateTaskLabelRequestDto
import com.requena.supportdesk.features.tasks.data.dto.CreateTaskRequestDto
import com.requena.supportdesk.features.tasks.data.dto.CreateTimeLogRequestDto
import com.requena.supportdesk.features.tasks.data.dto.UpdateTaskLabelRequestDto
import com.requena.supportdesk.features.tasks.data.dto.UpdateTaskRequestDto
import com.requena.supportdesk.features.tasks.data.mapper.TasksMapper
import com.requena.supportdesk.features.tasks.domain.model.TaskDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskLabelDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskTimeLogDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskUpdateInput
import com.requena.supportdesk.features.tasks.domain.repository.TasksRepository

class TasksRepositoryImpl(
    private val dataSource: TasksDataSource,
) : TasksRepository {
    override suspend fun getLabels(): AppResult<List<TaskCategory>> = runCatching {
        dataSource.getLabels().map(TasksMapper::fromLabelDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudieron cargar las etiquetas.", cause = it) },
    )

    override suspend fun getTasks(): AppResult<List<WorkTask>> = runCatching {
        dataSource.getTasks().map(TasksMapper::fromTaskDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudieron cargar las tareas.", cause = it) },
    )

    override suspend fun getTimeLogs(): AppResult<List<TaskLog>> = runCatching {
        dataSource.getTimeLogs().map(TasksMapper::fromTaskLogDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudieron cargar los registros de tiempo.", cause = it) },
    )

    override suspend fun createTask(input: TaskDraft): AppResult<WorkTask> = runCatching {
        dataSource.createTask(
            CreateTaskRequestDto(
                title = input.title,
                description = input.description,
                clientId = input.clientId,
                labelId = input.categoryId,
                dueDate = input.dueDate,
            ),
        ).let(TasksMapper::fromTaskDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo crear la tarea.", cause = it) },
    )

    override suspend fun updateTask(taskId: String, input: TaskUpdateInput): AppResult<WorkTask> = runCatching {
        dataSource.updateTask(
            taskId = taskId,
            request = UpdateTaskRequestDto(
                title = input.title,
                description = input.description,
                clientId = input.clientId ?: "",
                labelId = input.categoryId,
                dueDate = input.dueDate ?: "",
                completed = input.completed,
                status = input.status,
            ),
        ).let(TasksMapper::fromTaskDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo actualizar la tarea.", cause = it) },
    )

    override suspend fun deleteTask(taskId: String): AppResult<Unit> = runCatching {
        dataSource.deleteTask(taskId)
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo borrar la tarea.", cause = it) },
    )

    override suspend fun createLabel(input: TaskLabelDraft): AppResult<TaskCategory> = runCatching {
        dataSource.createLabel(
            CreateTaskLabelRequestDto(
                name = input.name,
                colorHex = input.colorHex,
                ownerAdminId = input.ownerAdminId,
            ),
        ).let(TasksMapper::fromLabelDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo crear la etiqueta.", cause = it) },
    )

    override suspend fun updateLabel(labelId: String, input: TaskLabelDraft): AppResult<TaskCategory> = runCatching {
        dataSource.updateLabel(
            labelId = labelId,
            request = UpdateTaskLabelRequestDto(
                name = input.name,
                colorHex = input.colorHex,
            ),
        ).let(TasksMapper::fromLabelDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo actualizar la etiqueta.", cause = it) },
    )

    override suspend fun deleteLabel(labelId: String): AppResult<Unit> = runCatching {
        dataSource.deleteLabel(labelId)
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo borrar la etiqueta.", cause = it) },
    )

    override suspend fun createTimeLog(input: TaskTimeLogDraft): AppResult<TaskLog> = runCatching {
        dataSource.createTimeLog(
            CreateTimeLogRequestDto(
                taskId = input.taskId,
                authorId = input.authorId,
                workDate = input.workDate,
                minutes = input.minutes,
                seconds = input.seconds,
                note = input.note,
                billable = input.billable,
            ),
        ).let(TasksMapper::fromTaskLogDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo registrar el tiempo.", cause = it) },
    )
}
