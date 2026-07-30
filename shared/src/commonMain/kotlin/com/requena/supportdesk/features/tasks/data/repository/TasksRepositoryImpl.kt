package com.requena.supportdesk.features.tasks.data.repository

import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkSession
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.tasks.data.datasource.TasksDataSource
import com.requena.supportdesk.features.tasks.data.dto.CreateTaskLabelRequestDto
import com.requena.supportdesk.features.tasks.data.dto.CreateTaskRequestDto
import com.requena.supportdesk.features.tasks.data.dto.StartWorkSessionRequestDto
import com.requena.supportdesk.features.tasks.data.dto.UpdateTaskLabelRequestDto
import com.requena.supportdesk.features.tasks.data.dto.UpdateTaskRequestDto
import com.requena.supportdesk.features.tasks.data.mapper.TasksMapper
import com.requena.supportdesk.features.tasks.domain.model.TaskDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskLabelDraft
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

    override suspend fun getActiveWorkSession(): AppResult<WorkSession?> = runCatching {
        dataSource.getActiveWorkSession()?.let(TasksMapper::fromWorkSessionDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo recuperar el temporizador activo.", cause = it) },
    )

    override suspend fun getWorkSessionsForTask(taskId: String): AppResult<List<WorkSession>> = runCatching {
        dataSource.getWorkSessionsForTask(taskId).map(TasksMapper::fromWorkSessionDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo cargar el historial de sesiones.", cause = it) },
    )

    override suspend fun startWorkSession(taskId: String): AppResult<WorkSession> = runCatching {
        dataSource.startWorkSession(StartWorkSessionRequestDto(taskId)).let(TasksMapper::fromWorkSessionDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo iniciar el contador.", cause = it) },
    )

    override suspend fun pauseWorkSession(sessionId: String): AppResult<WorkSession> = runCatching {
        dataSource.pauseWorkSession(sessionId).let(TasksMapper::fromWorkSessionDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo pausar el contador.", cause = it) },
    )

    override suspend fun resumeWorkSession(sessionId: String): AppResult<WorkSession> = runCatching {
        dataSource.resumeWorkSession(sessionId).let(TasksMapper::fromWorkSessionDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo reanudar el contador.", cause = it) },
    )

    override suspend fun stopWorkSession(sessionId: String): AppResult<WorkSession> = runCatching {
        dataSource.stopWorkSession(sessionId).let(TasksMapper::fromWorkSessionDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo detener el contador.", cause = it) },
    )

    override suspend fun checkpointWorkSession(sessionId: String): AppResult<WorkSession> = runCatching {
        dataSource.checkpointWorkSession(sessionId).let(TasksMapper::fromWorkSessionDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo guardar el checkpoint.", cause = it) },
    )
}
