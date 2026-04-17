package com.requena.supportdesk.features.tasks.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.common.SupportDeskSeed
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.tasks.domain.model.TaskDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskLabelDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskTimeLogDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskUpdateInput
import com.requena.supportdesk.features.tasks.domain.usecase.CreateTaskLabelUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.CreateTaskUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.CreateTimeLogUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.DeleteTaskLabelUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.DeleteTaskUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetTaskLabelsUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetTaskLogsUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetTasksUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.UpdateTaskLabelUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.UpdateTaskUseCase
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TasksViewModel(
    private val getTaskLabelsUseCase: GetTaskLabelsUseCase,
    private val getTasksUseCase: GetTasksUseCase,
    private val getTaskLogsUseCase: GetTaskLogsUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val createTaskLabelUseCase: CreateTaskLabelUseCase,
    private val updateTaskLabelUseCase: UpdateTaskLabelUseCase,
    private val deleteTaskLabelUseCase: DeleteTaskLabelUseCase,
    private val createTimeLogUseCase: CreateTimeLogUseCase,
) : BaseViewModel() {
    private val _state = MutableStateFlow(TasksUiState())
    val state: StateFlow<TasksUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var categories = emptyList<TaskCategory>()
    private var tasks = emptyList<WorkTask>()
    private var logs = emptyList<TaskLog>()

    init {
        onEvent(TasksUiEvent.Load)
    }

    fun onEvent(event: TasksUiEvent) {
        when (event) {
            TasksUiEvent.Load -> loadWorkspace()
            is TasksUiEvent.SelectDay -> publish(selectedDay = event.dayIsoDate)
            is TasksUiEvent.SelectTask -> publish(selectedTaskId = event.taskId)
            is TasksUiEvent.SelectCategory -> publish(selectedCategoryId = event.categoryId)
            is TasksUiEvent.SelectClientFilter -> publish(selectedClientFilterId = event.clientId)
            is TasksUiEvent.SelectDashboardClient -> publish(selectedDashboardClientId = event.clientId)
            is TasksUiEvent.CreateCategory -> createLabel(event.name, event.colorHex)
            is TasksUiEvent.UpdateLabel -> updateLabel(event.labelId, event.name, event.colorHex)
            is TasksUiEvent.DeleteLabel -> deleteLabel(event.labelId)
            is TasksUiEvent.CreateTask -> createTask(event)
            is TasksUiEvent.UpdateTaskClient -> updateTaskClient(event.taskId, event.clientId)
            is TasksUiEvent.UpdateTask -> updateTask(event)
            is TasksUiEvent.DeleteTask -> deleteTask(event.taskId)
            is TasksUiEvent.ToggleTaskCompletion -> toggleCompletion(event.taskId)
            is TasksUiEvent.StartTimer -> startTimer(event.taskId)
            TasksUiEvent.PauseTimer -> pauseTimer()
            TasksUiEvent.StopTimer -> stopTimer()
        }
    }

    private fun loadWorkspace(
        selectedDay: String? = state.value.selectedDay,
        selectedTaskId: String? = state.value.selectedTaskId,
        selectedCategoryId: String? = state.value.selectedCategoryId,
        selectedClientFilterId: String? = state.value.selectedClientFilterId,
        selectedDashboardClientId: String? = state.value.selectedDashboardClientId,
        activeTaskId: String? = state.value.activeTaskId,
        activeTaskSeconds: Int = state.value.activeTaskSeconds,
        isTimerRunning: Boolean = state.value.isTimerRunning,
        statusMessage: String? = null,
    ) {
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, statusMessage = statusMessage) }

            val labelsResult = getTaskLabelsUseCase()
            if (labelsResult is AppResult.Error) {
                handleWorkspaceError(labelsResult.message)
                return@launch
            }

            val tasksResult = getTasksUseCase()
            if (tasksResult is AppResult.Error) {
                handleWorkspaceError(tasksResult.message)
                return@launch
            }

            val logsResult = getTaskLogsUseCase()
            if (logsResult is AppResult.Error) {
                handleWorkspaceError(logsResult.message)
                return@launch
            }

            categories = (labelsResult as AppResult.Success).data
            tasks = (tasksResult as AppResult.Success).data
            logs = (logsResult as AppResult.Success).data

            publish(
                selectedDay = selectedDay,
                selectedTaskId = selectedTaskId,
                selectedCategoryId = selectedCategoryId,
                selectedClientFilterId = selectedClientFilterId,
                selectedDashboardClientId = selectedDashboardClientId,
                activeTaskId = activeTaskId,
                activeTaskSeconds = activeTaskSeconds,
                isTimerRunning = isTimerRunning,
                isLoading = false,
                errorMessage = null,
                statusMessage = statusMessage,
            )
        }
    }

    private fun createLabel(name: String, colorHex: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        launch {
            when (val result = createTaskLabelUseCase(TaskLabelDraft(cleanName, normalizeHex(colorHex)))) {
                is AppResult.Error -> handleWorkspaceError(result.message)
                is AppResult.Success -> loadWorkspace(
                    selectedCategoryId = result.data.id,
                    statusMessage = "Etiqueta creada",
                )
            }
        }
    }

    private fun updateLabel(labelId: String, name: String, colorHex: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        launch {
            when (val result = updateTaskLabelUseCase(labelId, TaskLabelDraft(cleanName, normalizeHex(colorHex)))) {
                is AppResult.Error -> handleWorkspaceError(result.message)
                is AppResult.Success -> loadWorkspace(
                    selectedCategoryId = result.data.id,
                    statusMessage = "Etiqueta actualizada",
                )
            }
        }
    }

    private fun deleteLabel(labelId: String) {
        launch {
            when (val result = deleteTaskLabelUseCase(labelId)) {
                is AppResult.Error -> handleWorkspaceError(result.message)
                is AppResult.Success -> loadWorkspace(
                    selectedCategoryId = state.value.selectedCategoryId.takeUnless { it == labelId },
                    statusMessage = "Etiqueta borrada",
                )
            }
        }
    }

    private fun createTask(event: TasksUiEvent.CreateTask) {
        val title = event.title.trim()
        if (title.isBlank() || event.categoryId.isBlank()) return
        launch {
            when (
                val result = createTaskUseCase(
                    TaskDraft(
                        title = title,
                        description = event.description.trim(),
                        clientId = event.clientId?.takeIf { it.isNotBlank() },
                        categoryId = event.categoryId,
                    ),
                )
            ) {
                is AppResult.Error -> handleWorkspaceError(result.message)
                is AppResult.Success -> loadWorkspace(
                    selectedTaskId = result.data.id,
                    selectedCategoryId = result.data.categoryId,
                    selectedClientFilterId = result.data.clientId,
                    selectedDashboardClientId = result.data.clientId ?: state.value.selectedDashboardClientId,
                    statusMessage = "Tarea creada",
                )
            }
        }
    }

    private fun updateTaskClient(taskId: String, clientId: String?) {
        val current = tasks.firstOrNull { it.id == taskId } ?: return
        executeTaskUpdate(
            taskId = taskId,
            input = TaskUpdateInput(
                title = current.title,
                description = current.description,
                clientId = clientId?.takeIf { it.isNotBlank() },
                categoryId = current.categoryId,
                completed = current.completed,
            ),
            successMessage = "Cliente asociado actualizado",
        )
    }

    private fun updateTask(event: TasksUiEvent.UpdateTask) {
        val current = tasks.firstOrNull { it.id == event.taskId } ?: return
        val cleanTitle = event.title.trim()
        if (cleanTitle.isBlank() || event.categoryId.isBlank()) return
        executeTaskUpdate(
            taskId = event.taskId,
            input = TaskUpdateInput(
                title = cleanTitle,
                description = event.description.trim(),
                clientId = current.clientId,
                categoryId = event.categoryId,
                completed = current.completed,
            ),
            successMessage = "Tarea actualizada",
        )
    }

    private fun deleteTask(taskId: String) {
        val fallbackSelection = nextSelectionAfterDelete(taskId)
        launch {
            when (val result = deleteTaskUseCase(taskId)) {
                is AppResult.Error -> handleWorkspaceError(result.message)
                is AppResult.Success -> loadWorkspace(
                    selectedTaskId = fallbackSelection,
                    statusMessage = "Tarea borrada",
                )
            }
        }
    }

    private fun toggleCompletion(taskId: String) {
        val current = tasks.firstOrNull { it.id == taskId } ?: return
        executeTaskUpdate(
            taskId = taskId,
            input = TaskUpdateInput(
                title = current.title,
                description = current.description,
                clientId = current.clientId,
                categoryId = current.categoryId,
                completed = !current.completed,
            ),
            successMessage = "Estado de tarea actualizado",
        )
    }

    private fun executeTaskUpdate(
        taskId: String,
        input: TaskUpdateInput,
        successMessage: String,
    ) {
        launch {
            when (val result = updateTaskUseCase(taskId, input)) {
                is AppResult.Error -> handleWorkspaceError(result.message)
                is AppResult.Success -> loadWorkspace(
                    selectedTaskId = result.data.id,
                    selectedCategoryId = result.data.categoryId,
                    selectedClientFilterId = result.data.clientId,
                    selectedDashboardClientId = result.data.clientId ?: state.value.selectedDashboardClientId,
                    statusMessage = successMessage,
                )
            }
        }
    }

    private fun startTimer(taskId: String) {
        val current = state.value
        if (current.isTimerRunning && current.activeTaskId == taskId) return
        if (current.isTimerRunning && current.activeTaskId != taskId) {
            pauseTimer()
        }
        val resumeSeconds = if (current.activeTaskId == taskId) current.activeTaskSeconds else 0
        _state.update {
            it.copy(
                activeTaskId = taskId,
                selectedTaskId = taskId,
                activeTaskSeconds = resumeSeconds,
                isTimerRunning = true,
                statusMessage = "Contador en marcha",
            )
        }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var seconds = state.value.activeTaskSeconds
            while (true) {
                delay(1000)
                seconds += 1
                _state.update { currentState -> currentState.copy(activeTaskSeconds = seconds) }
            }
        }
    }

    private fun pauseTimer() {
        val current = state.value
        if (!current.isTimerRunning) return
        timerJob?.cancel()
        timerJob = null
        publish(
            activeTaskId = current.activeTaskId,
            activeTaskSeconds = current.activeTaskSeconds,
            isTimerRunning = false,
            statusMessage = "Contador en pausa",
        )
    }

    private fun stopTimer() {
        val current = state.value
        val taskId = current.activeTaskId ?: return
        timerJob?.cancel()
        timerJob = null

        val minutesToAdd = when {
            current.activeTaskSeconds <= 0 -> 0
            current.activeTaskSeconds < 60 -> 1
            else -> current.activeTaskSeconds / 60
        }

        if (minutesToAdd <= 0) {
            publish(
                activeTaskId = null,
                activeTaskSeconds = 0,
                isTimerRunning = false,
                statusMessage = "Contador cancelado",
            )
            return
        }

        val workDate = current.selectedDay ?: DEFAULT_DAY
        launch {
            when (
                val result = createTimeLogUseCase(
                    TaskTimeLogDraft(
                        taskId = taskId,
                        authorId = SupportDeskSeed.adminUser.id,
                        workDate = workDate,
                        minutes = minutesToAdd,
                        note = "Registro desde contador",
                        billable = tasks.firstOrNull { it.id == taskId }?.clientId != null,
                    ),
                )
            ) {
                is AppResult.Error -> {
                    handleWorkspaceError(result.message)
                    publish(
                        activeTaskId = null,
                        activeTaskSeconds = 0,
                        isTimerRunning = false,
                        statusMessage = result.message,
                    )
                }
                is AppResult.Success -> loadWorkspace(
                    selectedDay = workDate,
                    selectedTaskId = taskId,
                    activeTaskId = null,
                    activeTaskSeconds = 0,
                    isTimerRunning = false,
                    statusMessage = "Contador detenido",
                )
            }
        }
    }

    private fun publish(
        selectedDay: String? = state.value.selectedDay,
        selectedTaskId: String? = state.value.selectedTaskId,
        selectedCategoryId: String? = state.value.selectedCategoryId,
        selectedClientFilterId: String? = state.value.selectedClientFilterId,
        selectedDashboardClientId: String? = state.value.selectedDashboardClientId,
        activeTaskId: String? = state.value.activeTaskId,
        activeTaskSeconds: Int = state.value.activeTaskSeconds,
        isTimerRunning: Boolean = state.value.isTimerRunning,
        isLoading: Boolean = state.value.isLoading,
        errorMessage: String? = state.value.errorMessage,
        statusMessage: String? = state.value.statusMessage,
    ) {
        val resolvedDay = selectedDay ?: logs.maxByOrNull { it.workDate }?.workDate ?: DEFAULT_DAY
        val resolvedSelectedTaskId = when {
            selectedTaskId == null && tasks.isNotEmpty() -> tasks.first().id
            selectedTaskId != null && tasks.any { it.id == selectedTaskId } -> selectedTaskId
            else -> tasks.firstOrNull()?.id
        }
        val resolvedSelectedCategoryId = when {
            selectedCategoryId == null -> null
            categories.any { it.id == selectedCategoryId } -> selectedCategoryId
            else -> null
        }
        val resolvedActiveTaskId = activeTaskId?.takeIf { id -> tasks.any { it.id == id } }

        _state.update {
            it.copy(
                categories = categories,
                tasks = tasks,
                logs = logs,
                selectedDay = resolvedDay,
                selectedTaskId = resolvedSelectedTaskId,
                selectedCategoryId = resolvedSelectedCategoryId,
                selectedClientFilterId = selectedClientFilterId,
                selectedDashboardClientId = selectedDashboardClientId,
                activeTaskId = resolvedActiveTaskId,
                activeTaskSeconds = if (resolvedActiveTaskId == null) 0 else activeTaskSeconds,
                isTimerRunning = resolvedActiveTaskId != null && isTimerRunning,
                isLoading = isLoading,
                errorMessage = errorMessage,
                statusMessage = statusMessage,
            )
        }
    }

    private fun handleWorkspaceError(message: String) {
        _state.update {
            it.copy(
                isLoading = false,
                errorMessage = message,
                statusMessage = message,
            )
        }
    }

    private fun nextSelectionAfterDelete(taskId: String): String? {
        val currentTasks = state.value.tasks
        val index = currentTasks.indexOfFirst { it.id == taskId }
        if (index < 0) return state.value.selectedTaskId
        return currentTasks.getOrNull(index + 1)?.id ?: currentTasks.getOrNull(index - 1)?.id
    }

    override fun clear() {
        timerJob?.cancel()
        super.clear()
    }

    private fun normalizeHex(raw: String): String {
        val trimmed = raw.trim().removePrefix("#")
        val normalized = trimmed.take(6).padStart(6, '0').uppercase()
        return "#$normalized"
    }

    private companion object {
        const val DEFAULT_DAY = "2026-04-16"
    }
}
