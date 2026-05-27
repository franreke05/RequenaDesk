package com.requena.supportdesk.features.tasks.domain.usecase

import com.requena.supportdesk.features.tasks.domain.model.TaskDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskLabelDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskTimeLogDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskUpdateInput
import com.requena.supportdesk.features.tasks.domain.repository.TasksRepository

class GetTaskLabelsUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke() = repository.getLabels()
}

class GetTasksUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke() = repository.getTasks()
}

class GetTaskLogsUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke() = repository.getTimeLogs()
}

class CreateTaskUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(input: TaskDraft) = repository.createTask(input)
}

class UpdateTaskUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(taskId: String, input: TaskUpdateInput) = repository.updateTask(taskId, input)
}

class DeleteTaskUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(taskId: String) = repository.deleteTask(taskId)
}

class SetTaskPinnedUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(taskId: String, pinned: Boolean) = repository.setTaskPinned(taskId, pinned)
}

class CreateTaskLabelUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(input: TaskLabelDraft) = repository.createLabel(input)
}

class UpdateTaskLabelUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(labelId: String, input: TaskLabelDraft) = repository.updateLabel(labelId, input)
}

class DeleteTaskLabelUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(labelId: String) = repository.deleteLabel(labelId)
}

class CreateTimeLogUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(input: TaskTimeLogDraft) = repository.createTimeLog(input)
}
