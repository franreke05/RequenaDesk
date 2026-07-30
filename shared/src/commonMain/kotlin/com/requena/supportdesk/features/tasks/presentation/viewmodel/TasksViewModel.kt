package com.requena.supportdesk.features.tasks.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.network.AdminSessionContext
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkSession
import com.requena.supportdesk.core.model.WorkSessionStatus
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.core.time.currentEpochMillis
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.core.time.isPastIsoDate
import com.requena.supportdesk.core.time.parseIsoInstantToEpochMillis
import com.requena.supportdesk.features.tasks.domain.model.TaskDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskLabelDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskUpdateInput
import com.requena.supportdesk.features.tasks.domain.model.derivedTaskStepDescription
import com.requena.supportdesk.features.tasks.domain.model.derivedTaskStepTitle
import com.requena.supportdesk.features.tasks.domain.model.parseDerivedTaskSteps
import com.requena.supportdesk.features.tasks.domain.usecase.CheckpointWorkSessionUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.CreateTaskLabelUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.CreateTaskUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.DeleteTaskLabelUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.DeleteTaskUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetActiveWorkSessionUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetTaskLabelsUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetTaskLogsUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetTasksUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetWorkSessionsForTaskUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.PauseWorkSessionUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.ResumeWorkSessionUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.StartWorkSessionUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.StopWorkSessionUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.UpdateTaskLabelUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.UpdateTaskUseCase
import com.requena.supportdesk.features.tasks.presentation.effect.TasksUiEffect
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

private const val CHECKPOINT_INTERVAL_MILLIS = 20 * 60 * 1000L

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
    private val getActiveWorkSessionUseCase: GetActiveWorkSessionUseCase,
    private val startWorkSessionUseCase: StartWorkSessionUseCase,
    private val pauseWorkSessionUseCase: PauseWorkSessionUseCase,
    private val resumeWorkSessionUseCase: ResumeWorkSessionUseCase,
    private val stopWorkSessionUseCase: StopWorkSessionUseCase,
    private val checkpointWorkSessionUseCase: CheckpointWorkSessionUseCase,
    private val getWorkSessionsForTaskUseCase: GetWorkSessionsForTaskUseCase,
) : BaseViewModel() {
    private val _state = MutableStateFlow(TasksUiState())
    val state: StateFlow<TasksUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<TasksUiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<TasksUiEffect> = _effects.asSharedFlow()

    private var tickJob: Job? = null
    private var checkpointJob: Job? = null
    private val timerMutex = Mutex()
    private var categories = emptyList<TaskCategory>()
    private var tasks = emptyList<WorkTask>()
    private var logs = emptyList<TaskLog>()

    // Serializes every entry point that ends in loadWorkspace() (Load, create/update/delete
    // task or label, stopTimer's time-log write). Two jobs: (1) the canonical double-submit
    // guard (e.g. rapid double-taps on "Anadir tarea" used to both reach the network - the
    // daily task-limit check read stale state from before either had completed); (2)
    // categories/tasks/logs above are plain vars written by loadWorkspace() and read by
    // publish(), with no coordination between the ~8 independent coroutines that used to be
    // able to call loadWorkspace() concurrently - serializing here closes both at once.
    private val workspaceMutex = Mutex()

    init {
        onEvent(TasksUiEvent.Load)
        guardedLaunch(timerMutex) { recoverActiveSession() }
    }

    fun onEvent(event: TasksUiEvent) {
        when (event) {
            TasksUiEvent.Load -> guardedLaunch(workspaceMutex) { loadWorkspace() }
            is TasksUiEvent.SelectDay -> selectDay(event.dayIsoDate)
            is TasksUiEvent.SelectTask -> publish(selectedTaskId = event.taskId)
            is TasksUiEvent.SelectCategory -> publish(selectedCategoryId = event.categoryId)
            is TasksUiEvent.SelectClientFilter -> publish(selectedClientFilterId = event.clientId)
            is TasksUiEvent.SelectDashboardClient -> publish(selectedDashboardClientId = event.clientId)
            is TasksUiEvent.ToggleDashboardCategoryFilter -> toggleDashboardCategoryFilter(event.categoryId)
            TasksUiEvent.ClearDashboardFilters -> publish(
                selectedDashboardClientId = null,
                selectedDashboardCategoryIds = emptySet(),
            )
            is TasksUiEvent.CreateCategory -> createLabel(event.name, event.colorHex)
            is TasksUiEvent.UpdateLabel -> updateLabel(event.labelId, event.name, event.colorHex)
            is TasksUiEvent.DeleteLabel -> deleteLabel(event.labelId)
            is TasksUiEvent.CreateTask -> createTask(event)
            is TasksUiEvent.CreateTaskSteps -> createTaskSteps(event)
            is TasksUiEvent.UpdateTaskClient -> updateTaskClient(event.taskId, event.clientId)
            is TasksUiEvent.UpdateTask -> updateTask(event)
            is TasksUiEvent.DeleteTask -> deleteTask(event.taskId)
            is TasksUiEvent.ToggleTaskCompletion -> toggleCompletion(event.taskId)
            is TasksUiEvent.StartTimer -> startOrResumeTimer(event.taskId)
            TasksUiEvent.PauseTimer -> pauseTimer()
            TasksUiEvent.StopTimer -> stopTimer()
            is TasksUiEvent.LoadTaskSessions -> loadTaskSessions(event.taskId)
        }
    }

    private fun loadTaskSessions(taskId: String) {
        launch {
            when (val result = getWorkSessionsForTaskUseCase(taskId)) {
                is AppResult.Error -> handleWorkspaceError(result.message)
                is AppResult.Success -> _state.update {
                    it.copy(taskSessions = result.data, taskSessionsForTaskId = taskId)
                }
            }
        }
    }

    // suspend, not self-launching: every caller now runs it inside its own guardedLaunch
    // (workspaceMutex) block so the mutex covers the full create/update-then-refresh
    // sequence, not just the initial mutation - a plain `launch{}` here would let the mutex
    // unlock (in the caller's `finally`) before this function's own network calls finished.
    private suspend fun loadWorkspace(
        selectedDay: String? = state.value.selectedDay,
        selectedTaskId: String? = state.value.selectedTaskId,
        selectedCategoryId: String? = state.value.selectedCategoryId,
        selectedClientFilterId: String? = state.value.selectedClientFilterId,
        selectedDashboardClientId: String? = state.value.selectedDashboardClientId,
        selectedDashboardCategoryIds: Set<String> = state.value.selectedDashboardCategoryIds,
        activeSession: WorkSession? = state.value.activeSession,
        activeTaskId: String? = state.value.activeTaskId,
        activeTaskSeconds: Int = state.value.activeTaskSeconds,
        isTimerRunning: Boolean = state.value.isTimerRunning,
        statusMessage: String? = null,
    ) {
        _state.update { it.copy(isLoading = true, errorMessage = null, statusMessage = statusMessage) }

        val labelsResult = getTaskLabelsUseCase()
        if (labelsResult is AppResult.Error) {
            handleWorkspaceError(labelsResult.message)
            return
        }

        val tasksResult = getTasksUseCase()
        if (tasksResult is AppResult.Error) {
            handleWorkspaceError(tasksResult.message)
            return
        }

        val logsResult = getTaskLogsUseCase()
        if (logsResult is AppResult.Error) {
            handleWorkspaceError(logsResult.message)
            return
        }

        categories = (labelsResult as AppResult.Success).data
        tasks = (tasksResult as AppResult.Success).data
        logs = (logsResult as AppResult.Success).data

        if (statusMessage != null) {
            _effects.emit(TasksUiEffect.ShowMessage(statusMessage))
        }

        publish(
            selectedDay = selectedDay,
            selectedTaskId = selectedTaskId,
            selectedCategoryId = selectedCategoryId,
            selectedClientFilterId = selectedClientFilterId,
            selectedDashboardClientId = selectedDashboardClientId,
            selectedDashboardCategoryIds = selectedDashboardCategoryIds,
            activeSession = activeSession,
            activeTaskId = activeTaskId,
            activeTaskSeconds = activeTaskSeconds,
            isTimerRunning = isTimerRunning,
            isLoading = false,
            errorMessage = null,
            statusMessage = statusMessage,
        )
    }

    private fun createLabel(name: String, colorHex: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val ownerAdminId = AdminSessionContext.currentUserId()
        if (ownerAdminId == null) {
            handleWorkspaceError("No se encontro una sesion de administrador activa.")
            return
        }
        guardedLaunch(workspaceMutex) {
            when (val result = createTaskLabelUseCase(TaskLabelDraft(cleanName, normalizeHex(colorHex), ownerAdminId = ownerAdminId))) {
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
        val ownerAdminId = AdminSessionContext.currentUserId()
        if (ownerAdminId == null) {
            handleWorkspaceError("No se encontro una sesion de administrador activa.")
            return
        }
        guardedLaunch(workspaceMutex) {
            when (val result = updateTaskLabelUseCase(labelId, TaskLabelDraft(cleanName, normalizeHex(colorHex), ownerAdminId = ownerAdminId))) {
                is AppResult.Error -> handleWorkspaceError(result.message)
                is AppResult.Success -> loadWorkspace(
                    selectedCategoryId = result.data.id,
                    statusMessage = "Etiqueta actualizada",
                )
            }
        }
    }

    private fun deleteLabel(labelId: String) {
        guardedLaunch(workspaceMutex) {
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
        val dueDate = event.dueDate?.trim()?.takeIf { it.isNotBlank() }?.also { plannedDate ->
            if (isPastIsoDate(plannedDate)) {
                handleWorkspaceError("No puedes programar tareas para dias anteriores.")
                return
            }
        }
        guardedLaunch(workspaceMutex) {
            // isLoading=true here is the actual fix: ClientTasksScreen's "Anadir" button is
            // already correctly wired to `enabled = !state.isLoading`, it just never received
            // a true value during the in-flight window before - the button was clickable for
            // the entire round-trip, letting two fast taps both reach createTaskUseCase().
            _state.update { it.copy(isLoading = true, errorMessage = null, lastCreatedTaskId = null) }
            when (
                val result = createTaskUseCase(
                    TaskDraft(
                        title = title,
                        description = event.description.trim(),
                        clientId = event.clientId?.takeIf { it.isNotBlank() },
                        categoryId = event.categoryId,
                        dueDate = dueDate,
                    ),
                )
            ) {
                is AppResult.Error -> handleWorkspaceError(result.message)
                is AppResult.Success -> {
                    _state.update { it.copy(lastCreatedTaskId = result.data.id) }
                    loadWorkspace(
                        selectedTaskId = result.data.id,
                        selectedCategoryId = result.data.categoryId,
                        selectedClientFilterId = result.data.clientId,
                        selectedDashboardClientId = result.data.clientId ?: state.value.selectedDashboardClientId,
                        statusMessage = "Tarea creada",
                    )
                }
            }
        }
    }

    private fun createTaskSteps(event: TasksUiEvent.CreateTaskSteps) {
        val parent = tasks.firstOrNull { it.id == event.parentTaskId }
        if (parent == null) {
            handleWorkspaceError("No se encontró la tarea que quieres dividir.")
            return
        }
        if (parent.completed) {
            handleWorkspaceError("Reabre la tarea antes de crear nuevos pasos.")
            return
        }
        val steps = parseDerivedTaskSteps(event.steps)
        if (steps.isEmpty()) {
            handleWorkspaceError("Escribe al menos un paso.")
            return
        }

        guardedLaunch(workspaceMutex) {
            _state.update {
                it.copy(
                    isLoading = true,
                    isCreatingTaskSteps = true,
                    errorMessage = null,
                    statusMessage = null,
                )
            }
            val createdTasks = mutableListOf<WorkTask>()
            var failureMessage: String? = null
            for (step in steps) {
                when (
                    val result = createTaskUseCase(
                        TaskDraft(
                            title = derivedTaskStepTitle(parent.title, step),
                            description = derivedTaskStepDescription(parent.title),
                            clientId = parent.clientId,
                            categoryId = parent.categoryId,
                            dueDate = parent.dueDate?.takeUnless(::isPastIsoDate),
                        ),
                    )
                ) {
                    is AppResult.Error -> {
                        failureMessage = result.message
                        break
                    }
                    is AppResult.Success -> createdTasks += result.data
                }
            }

            if (createdTasks.isEmpty()) {
                _state.update { it.copy(isCreatingTaskSteps = false) }
                handleWorkspaceError(failureMessage ?: "No se pudieron crear los pasos.")
                return@guardedLaunch
            }

            _state.update {
                it.copy(
                    isCreatingTaskSteps = false,
                    taskStepsCreationVersion = it.taskStepsCreationVersion + 1,
                )
            }
            val lastCreated = createdTasks.last()
            val status = if (failureMessage == null) {
                if (createdTasks.size == 1) "1 paso creado" else "${createdTasks.size} pasos creados"
            } else {
                "Se crearon ${createdTasks.size} de ${steps.size} pasos"
            }
            loadWorkspace(
                selectedTaskId = lastCreated.id,
                selectedCategoryId = parent.categoryId,
                selectedClientFilterId = parent.clientId,
                selectedDashboardClientId = parent.clientId ?: state.value.selectedDashboardClientId,
                statusMessage = status,
            )
            if (failureMessage != null) {
                val partialFailure = "$status. No se pudo continuar: $failureMessage"
                _state.update { it.copy(errorMessage = partialFailure, statusMessage = partialFailure) }
                _effects.emit(TasksUiEffect.ShowMessage(partialFailure))
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
                dueDate = current.dueDate,
                completed = current.completed,
            ),
            successMessage = "Cliente asociado actualizado",
        )
    }

    private fun updateTask(event: TasksUiEvent.UpdateTask) {
        val current = tasks.firstOrNull { it.id == event.taskId } ?: return
        val cleanTitle = event.title.trim()
        if (cleanTitle.isBlank() || event.categoryId.isBlank()) return
        val dueDate = event.dueDate?.trim()?.takeIf { it.isNotBlank() }?.also { plannedDate ->
            if (plannedDate != current.dueDate && isPastIsoDate(plannedDate)) {
                handleWorkspaceError("No puedes programar tareas para dias anteriores.")
                return
            }
        }
        executeTaskUpdate(
            taskId = event.taskId,
            input = TaskUpdateInput(
                title = cleanTitle,
                description = event.description.trim(),
                clientId = event.clientId,
                categoryId = event.categoryId,
                dueDate = dueDate,
                completed = current.completed,
            ),
            successMessage = "Tarea actualizada",
        )
    }

    private fun deleteTask(taskId: String) {
        val fallbackSelection = nextSelectionAfterDelete(taskId)
        guardedLaunch(workspaceMutex) {
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
                dueDate = current.dueDate,
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
        guardedLaunch(workspaceMutex) {
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

    // Entry point for TasksUiEvent.StartTimer. Decides start-vs-resume from the current
    // persisted session rather than exposing a separate "resume" event, so the existing
    // Iniciar/Pausar/Detener button wiring in AdminDashboardScreen/MobileWorkspaceApp needs no
    // changes. A second timer on a different task is rejected (not auto-paused) per spec.
    private fun startOrResumeTimer(taskId: String) {
        val current = state.value
        if (!current.canTrackSelectedDay) {
            publish(statusMessage = "Solo puedes registrar horas en el dia actual.")
            return
        }
        val activeSession = current.activeSession
        if (activeSession != null) {
            if (activeSession.taskId == taskId) {
                if (activeSession.status == WorkSessionStatus.PAUSED) {
                    resumeSession(activeSession.id)
                }
                return
            }
            val activeTaskTitle = tasks.firstOrNull { it.id == activeSession.taskId }?.title ?: "otra tarea"
            publish(statusMessage = "Hay un temporizador activo en '$activeTaskTitle'. Detenlo o pausalo antes de iniciar uno nuevo.")
            return
        }
        guardedLaunch(timerMutex) {
            when (val result = startWorkSessionUseCase(taskId)) {
                is AppResult.Error -> handleWorkspaceError(result.message)
                is AppResult.Success -> applySession(result.data, selectedTaskId = taskId, statusMessage = "Contador en marcha")
            }
        }
    }

    private fun resumeSession(sessionId: String) {
        guardedLaunch(timerMutex) {
            when (val result = resumeWorkSessionUseCase(sessionId)) {
                is AppResult.Error -> handleWorkspaceError(result.message)
                is AppResult.Success -> applySession(result.data, statusMessage = "Contador en marcha")
            }
        }
    }

    private fun pauseTimer() {
        val sessionId = state.value.activeSession?.takeIf { it.status == WorkSessionStatus.ACTIVE }?.id ?: return
        guardedLaunch(timerMutex) {
            when (val result = pauseWorkSessionUseCase(sessionId)) {
                is AppResult.Error -> handleWorkspaceError(result.message)
                is AppResult.Success -> applySession(result.data, statusMessage = "Contador en pausa")
            }
        }
    }

    private fun stopTimer() {
        val sessionId = state.value.activeSession?.id ?: return
        guardedLaunch(timerMutex) {
            when (val result = stopWorkSessionUseCase(sessionId)) {
                is AppResult.Error -> handleWorkspaceError(result.message)
                is AppResult.Success -> {
                    restartTickingFor(result.data)
                    _state.update {
                        it.copy(activeSession = null, activeTaskId = null, activeTaskSeconds = 0, isTimerRunning = false)
                    }
                    // Stopping just changed the task's accumulated total and its time_logs
                    // history server-side, so the cached tasks/logs need a real refresh.
                    guardedLaunch(workspaceMutex) {
                        loadWorkspace(
                            activeSession = null,
                            activeTaskId = null,
                            activeTaskSeconds = 0,
                            isTimerRunning = false,
                            statusMessage = "Contador detenido",
                        )
                    }
                }
            }
        }
    }

    private suspend fun recoverActiveSession() {
        val result = getActiveWorkSessionUseCase()
        if (result !is AppResult.Success) return
        val session = result.data ?: return
        applySession(session)
    }

    private suspend fun applySession(session: WorkSession, selectedTaskId: String? = null, statusMessage: String? = null) {
        restartTickingFor(session)
        _state.update {
            it.copy(
                activeSession = session,
                activeTaskId = session.taskId,
                activeTaskSeconds = elapsedSecondsFor(session),
                isTimerRunning = session.status == WorkSessionStatus.ACTIVE,
                selectedTaskId = selectedTaskId ?: it.selectedTaskId,
                statusMessage = statusMessage ?: it.statusMessage,
            )
        }
        if (statusMessage != null) {
            _effects.emit(TasksUiEffect.ShowMessage(statusMessage))
        }
    }

    /**
     * The visible counter is always recomputed from the session's persisted timestamps
     * (closedSegmentsSeconds + elapsed-since-openSegmentStartedAt), never incremented in
     * place - so a missed tick, a recomposition, or a fresh recovery after restart can never
     * duplicate or drift the displayed duration.
     */
    private fun elapsedSecondsFor(session: WorkSession): Int {
        val openStartMillis = session.openSegmentStartedAt?.let(::parseIsoInstantToEpochMillis)
        val tickingSeconds = if (session.status == WorkSessionStatus.ACTIVE && openStartMillis != null) {
            ((currentEpochMillis() - openStartMillis) / 1000).coerceAtLeast(0)
        } else {
            0L
        }
        return (session.closedSegmentsSeconds + tickingSeconds).toInt()
    }

    private fun restartTickingFor(session: WorkSession) {
        tickJob?.cancel()
        checkpointJob?.cancel()
        tickJob = null
        checkpointJob = null
        if (session.status != WorkSessionStatus.ACTIVE) return
        tickJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { current ->
                    val latest = current.activeSession
                    if (latest == null || latest.id != session.id) current
                    else current.copy(activeTaskSeconds = elapsedSecondsFor(latest))
                }
            }
        }
        checkpointJob = viewModelScope.launch {
            while (true) {
                delay(CHECKPOINT_INTERVAL_MILLIS)
                // Idempotent heartbeat: only refreshes last_checkpoint_at server-side, never
                // creates a row or touches duration - safe to retry/repeat indefinitely.
                runCatching { checkpointWorkSessionUseCase(session.id) }
            }
        }
    }

    private fun publish(
        selectedDay: String? = state.value.selectedDay,
        selectedTaskId: String? = state.value.selectedTaskId,
        selectedCategoryId: String? = state.value.selectedCategoryId,
        selectedClientFilterId: String? = state.value.selectedClientFilterId,
        selectedDashboardClientId: String? = state.value.selectedDashboardClientId,
        selectedDashboardCategoryIds: Set<String> = state.value.selectedDashboardCategoryIds,
        activeSession: WorkSession? = state.value.activeSession,
        activeTaskId: String? = state.value.activeTaskId,
        activeTaskSeconds: Int = state.value.activeTaskSeconds,
        isTimerRunning: Boolean = state.value.isTimerRunning,
        isLoading: Boolean = state.value.isLoading,
        errorMessage: String? = state.value.errorMessage,
        statusMessage: String? = state.value.statusMessage,
    ) {
        val todayIsoDate = currentIsoDate()
        val resolvedDay = selectedDay ?: todayIsoDate
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
        val resolvedActiveSession = activeSession?.takeIf { it.taskId == resolvedActiveTaskId }
        val resolvedDashboardCategoryIds = selectedDashboardCategoryIds
            .filterTo(mutableSetOf()) { id -> categories.any { it.id == id } }

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
                selectedDashboardCategoryIds = resolvedDashboardCategoryIds,
                activeSession = resolvedActiveSession,
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
        launch { _effects.emit(TasksUiEffect.ShowMessage(message)) }
    }

    private fun nextSelectionAfterDelete(taskId: String): String? {
        val currentTasks = state.value.tasks
        val index = currentTasks.indexOfFirst { it.id == taskId }
        if (index < 0) return state.value.selectedTaskId
        return currentTasks.getOrNull(index + 1)?.id ?: currentTasks.getOrNull(index - 1)?.id
    }

    private fun toggleDashboardCategoryFilter(categoryId: String) {
        val current = state.value.selectedDashboardCategoryIds
        val updated = if (categoryId in current) current - categoryId else current + categoryId
        publish(selectedDashboardCategoryIds = updated)
    }

    private fun selectDay(dayIsoDate: String) {
        if (isPastIsoDate(dayIsoDate)) {
            publish(statusMessage = "Los dias anteriores estan bloqueados.")
            return
        }
        if (state.value.isTimerRunning && dayIsoDate != currentIsoDate()) {
            publish(statusMessage = "Deten el contador antes de cambiar a una fecha futura.")
            return
        }
        publish(
            selectedDay = dayIsoDate,
            statusMessage = if (dayIsoDate == currentIsoDate()) {
                "Registro abierto para hoy."
            } else {
                "Planificando tareas para $dayIsoDate."
            },
        )
    }

    override fun clear() {
        tickJob?.cancel()
        checkpointJob?.cancel()
        super.clear()
    }

    private fun normalizeHex(raw: String): String {
        val trimmed = raw.trim().removePrefix("#")
        val normalized = trimmed.take(6).padStart(6, '0').uppercase()
        return "#$normalized"
    }

}
