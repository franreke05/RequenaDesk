package com.requena.supportdesk.features.business.projects

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.result.AppResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class BusinessProjectsUiState(
    val projects: List<BusinessProject> = emptyList(),
    val tasks: List<BusinessTask> = emptyList(),
    val labels: List<BusinessTaskLabel> = emptyList(),
    val customers: List<BusinessCommercialCustomerReference> = emptyList(),
    val board: BusinessTaskBoard? = null,
    val activeWorkSession: BusinessWorkSession? = null,
    val selectedDate: String = "",
    val selectedProjectId: String? = null,
    val query: String = "",
    val projectStatus: BusinessProjectStatus? = null,
    val taskStatus: BusinessTaskStatus? = null,
    val customerId: String? = null,
    val labelId: String? = null,
    val savingIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val placementsForSelectedDate: List<BusinessTaskBoardPlacement>
        get() = board?.placements.orEmpty().filter { it.boardDate == selectedDate }

    val pendingTasks: List<BusinessTask>
        get() {
            val planned = board?.placements.orEmpty().mapTo(mutableSetOf()) { it.taskId }
            return tasks.filter { it.id !in planned && it.status !in setOf(BusinessTaskStatus.DONE, BusinessTaskStatus.ARCHIVED) }
        }
}

sealed interface BusinessProjectsEffect {
    data class ShowMessage(val message: String) : BusinessProjectsEffect
}

class BusinessProjectsViewModel(
    private val repository: BusinessProjectsRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(BusinessProjectsUiState())
    val state: StateFlow<BusinessProjectsUiState> = _state.asStateFlow()
    private val _effects = MutableSharedFlow<BusinessProjectsEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<BusinessProjectsEffect> = _effects.asSharedFlow()
    private val boardMutex = Mutex()

    fun refresh() = launch {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        val current = state.value
        when (
            val result = repository.workspace(
                query = current.query.takeIf(String::isNotBlank),
                projectStatus = current.projectStatus,
                taskStatus = current.taskStatus,
                projectId = current.selectedProjectId,
                customerId = current.customerId,
                labelId = current.labelId,
            )
        ) {
            is AppResult.Error -> fail(result.message)
            is AppResult.Success -> {
                val workspace = result.data
                _state.update {
                    it.copy(
                        projects = workspace.projects,
                        tasks = workspace.tasks,
                        labels = workspace.labels,
                        customers = workspace.customers,
                        board = workspace.board,
                        activeWorkSession = workspace.activeWorkSession,
                        selectedDate = it.selectedDate
                            .takeIf { date -> date.isNotBlank() && date >= workspace.board.today }
                            ?: workspace.board.today,
                        isLoading = false,
                        savingIds = emptySet(),
                    )
                }
            }
        }
    }

    fun setFilters(
        query: String = state.value.query,
        projectStatus: BusinessProjectStatus? = state.value.projectStatus,
        taskStatus: BusinessTaskStatus? = state.value.taskStatus,
        projectId: String? = state.value.selectedProjectId,
        customerId: String? = state.value.customerId,
        labelId: String? = state.value.labelId,
    ) {
        _state.update {
            it.copy(
                query = query,
                projectStatus = projectStatus,
                taskStatus = taskStatus,
                selectedProjectId = projectId,
                customerId = customerId,
                labelId = labelId,
            )
        }
        refresh()
    }

    fun selectDate(date: String) {
        val today = state.value.board?.today.orEmpty()
        if (date.isBlank() || today.isBlank() || date < today) {
            _effects.tryEmit(BusinessProjectsEffect.ShowMessage("Los dias pasados no se pueden planificar."))
            return
        }
        _state.update { it.copy(selectedDate = date) }
    }

    fun createProject(input: CreateBusinessProjectInput) = save {
        BusinessProjectsRules.validate(input)
        repository.createProject(input).mapSuccess { created ->
            _state.update { it.copy(projects = listOf(created) + it.projects) }
            message("Proyecto creado")
        }
    }

    fun updateProject(projectId: String, input: UpdateBusinessProjectInput) = save {
        BusinessProjectsRules.validate(input)
        repository.updateProject(projectId, input).mapSuccess(::replaceProject)
    }

    fun completeProject(project: BusinessProject) = projectTransition(project, repository::completeProject, "Proyecto completado")
    fun reopenProject(project: BusinessProject) = projectTransition(project, repository::reopenProject, "Proyecto reabierto")
    fun archiveProject(project: BusinessProject) = projectTransition(project, repository::archiveProject, "Proyecto archivado")

    fun createTask(input: CreateBusinessTaskInput) = save {
        BusinessProjectsRules.validate(input)
        repository.createTask(input).mapSuccess { created ->
            _state.update { it.copy(tasks = listOf(created) + it.tasks, board = it.board?.copy(tasks = listOf(created) + it.board.tasks)) }
            message("Tarea creada")
        }
    }

    fun updateTask(taskId: String, input: UpdateBusinessTaskInput) = save {
        BusinessProjectsRules.validate(input)
        repository.updateTask(taskId, input).mapSuccess(::replaceTask)
    }

    fun createLabel(input: CreateBusinessTaskLabelInput) = save {
        BusinessProjectsRules.validate(input)
        repository.createLabel(input).mapSuccess { created ->
            _state.update { it.copy(labels = it.labels + created) }
        }
    }

    fun updateLabel(labelId: String, input: UpdateBusinessTaskLabelInput) = save {
        BusinessProjectsRules.validate(input)
        repository.updateLabel(labelId, input).mapSuccess { updated ->
            _state.update { current ->
                current.copy(
                    labels = current.labels.map { if (it.id == updated.id) updated else it },
                    tasks = current.tasks.map { task ->
                        task.copy(labels = task.labels.map { if (it.id == updated.id) updated else it })
                    },
                )
            }
        }
    }

    fun archiveLabel(label: BusinessTaskLabel) = save {
        repository.archiveLabel(label.id, label.version).mapSuccess { archived ->
            _state.update { it.copy(labels = it.labels.map { value -> if (value.id == archived.id) archived else value }) }
        }
    }

    fun placeTask(taskId: String, boardDate: String, positionX: Double, positionY: Double) = launch {
        boardMutex.withLock {
            val input = BusinessTaskBoardPlacementInput(boardDate, positionX.coerceIn(0.0, 1.0), positionY.coerceIn(0.0, 1.0))
            val before = state.value.board?.placements.orEmpty()
            val old = before.firstOrNull { it.taskId == taskId }
            val optimistic = BusinessTaskBoardPlacement(taskId, boardDate, input.positionX, input.positionY, old?.updatedAt.orEmpty())
            _state.update { current ->
                current.copy(
                    board = current.board?.copy(placements = before.filterNot { it.taskId == taskId } + optimistic),
                    savingIds = current.savingIds + taskId,
                    errorMessage = null,
                )
            }
            when (val result = repository.placeTask(taskId, input)) {
                is AppResult.Error -> {
                    _state.update { it.copy(board = it.board?.copy(placements = before), savingIds = it.savingIds - taskId, errorMessage = result.message) }
                    message(result.message)
                }
                is AppResult.Success -> _state.update {
                    it.copy(
                        board = it.board?.copy(placements = it.board.placements.filterNot { item -> item.taskId == taskId } + result.data),
                        savingIds = it.savingIds - taskId,
                    )
                }
            }
        }
    }

    fun removeTaskFromBoard(taskId: String) = launch {
        boardMutex.withLock {
            val before = state.value.board?.placements.orEmpty()
            _state.update { it.copy(board = it.board?.copy(placements = before.filterNot { value -> value.taskId == taskId }), savingIds = it.savingIds + taskId) }
            when (val result = repository.removeTaskPlacement(taskId)) {
                is AppResult.Error -> {
                    _state.update { it.copy(board = it.board?.copy(placements = before), savingIds = it.savingIds - taskId, errorMessage = result.message) }
                    message(result.message)
                }
                is AppResult.Success -> _state.update { it.copy(savingIds = it.savingIds - taskId) }
            }
        }
    }

    fun startTimer(taskId: String) = timer { repository.startWorkSession(taskId) }
    fun pauseTimer() = state.value.activeWorkSession?.let { session -> timer { repository.pauseWorkSession(session.id) } }
    fun resumeTimer() = state.value.activeWorkSession?.let { session -> timer { repository.resumeWorkSession(session.id) } }
    fun stopTimer() = state.value.activeWorkSession?.let { session ->
        timer(clearFinished = true) { repository.stopWorkSession(session.id) }
    }
    fun checkpointTimer() = state.value.activeWorkSession?.let { session -> timer { repository.checkpointWorkSession(session.id) } }

    private fun timer(
        clearFinished: Boolean = false,
        operation: suspend () -> AppResult<BusinessWorkSession>,
    ) = save {
        operation().mapSuccess { session ->
            _state.update { it.copy(activeWorkSession = if (clearFinished) null else session) }
            if (clearFinished) refresh()
        }
    }

    private fun projectTransition(
        project: BusinessProject,
        operation: suspend (String, Int) -> AppResult<BusinessProject>,
        confirmation: String,
    ) = save {
        operation(project.id, project.version).mapSuccess {
            replaceProject(it)
            message(confirmation)
        }
    }

    private fun replaceProject(updated: BusinessProject) {
        _state.update { it.copy(projects = it.projects.map { value -> if (value.id == updated.id) updated else value }) }
    }

    private fun replaceTask(updated: BusinessTask) {
        _state.update { current ->
            val updatedTasks = current.tasks.map { if (it.id == updated.id) updated else it }
            current.copy(
                tasks = updatedTasks,
                board = current.board?.copy(
                    tasks = current.board.tasks.map { if (it.id == updated.id) updated else it },
                    placements = if (updated.status in setOf(BusinessTaskStatus.DONE, BusinessTaskStatus.ARCHIVED)) {
                        current.board.placements.filterNot { it.taskId == updated.id }
                    } else {
                        current.board.placements
                    },
                ),
            )
        }
    }

    private fun save(block: suspend () -> AppResult<Unit>) {
        if (state.value.isSaving) return
        launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                when (val result = block()) {
                    is AppResult.Error -> fail(result.message)
                    is AppResult.Success -> _state.update { it.copy(isSaving = false) }
                }
            } catch (error: BusinessProjectsValidationException) {
                fail(error.message.orEmpty())
            }
        }
    }

    private suspend fun <T> AppResult<T>.mapSuccess(block: suspend (T) -> Unit): AppResult<Unit> = when (this) {
        is AppResult.Error -> this
        is AppResult.Success -> {
            block(data)
            AppResult.Success(Unit)
        }
    }

    private suspend fun fail(message: String) {
        _state.update { it.copy(isLoading = false, isSaving = false, errorMessage = message) }
        message(message)
    }

    private suspend fun message(value: String) {
        _effects.emit(BusinessProjectsEffect.ShowMessage(value))
    }
}
