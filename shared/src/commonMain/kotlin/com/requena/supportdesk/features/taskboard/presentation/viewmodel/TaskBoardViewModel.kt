package com.requena.supportdesk.features.taskboard.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.model.TaskBoardPlacement
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.taskboard.domain.usecase.GetTaskBoardUseCase
import com.requena.supportdesk.features.taskboard.domain.usecase.PlaceTaskOnBoardUseCase
import com.requena.supportdesk.features.taskboard.domain.usecase.RemoveTaskFromBoardUseCase
import com.requena.supportdesk.features.taskboard.presentation.effect.TaskBoardUiEffect
import com.requena.supportdesk.features.taskboard.presentation.event.TaskBoardUiEvent
import com.requena.supportdesk.features.taskboard.presentation.state.TaskBoardUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TaskBoardViewModel(
    private val getTaskBoardUseCase: GetTaskBoardUseCase,
    private val placeTaskOnBoardUseCase: PlaceTaskOnBoardUseCase,
    private val removeTaskFromBoardUseCase: RemoveTaskFromBoardUseCase,
) : BaseViewModel() {
    private val _state = MutableStateFlow(TaskBoardUiState())
    val state: StateFlow<TaskBoardUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<TaskBoardUiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<TaskBoardUiEffect> = _effects.asSharedFlow()
    private val mutationMutex = Mutex()
    private var rolloverJob: Job? = null

    init {
        load()
    }

    fun onEvent(event: TaskBoardUiEvent) {
        when (event) {
            TaskBoardUiEvent.Load,
            TaskBoardUiEvent.Refresh -> load()
            TaskBoardUiEvent.SelectToday -> selectDate(state.value.today)
            is TaskBoardUiEvent.SelectDate -> selectDate(event.date)
            is TaskBoardUiEvent.PlaceTask -> savePlacement(
                taskId = event.taskId,
                boardDate = event.boardDate,
                positionX = event.positionX,
                positionY = event.positionY,
            )
            is TaskBoardUiEvent.MoveTask -> {
                val placement = state.value.placementFor(event.taskId) ?: return
                savePlacement(
                    taskId = event.taskId,
                    boardDate = placement.boardDate,
                    positionX = event.positionX,
                    positionY = event.positionY,
                )
            }
            is TaskBoardUiEvent.RemoveTask -> removePlacement(event.taskId)
        }
    }

    private fun load() {
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getTaskBoardUseCase()) {
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is AppResult.Success -> {
                    val board = result.data
                    _state.update { current ->
                        current.copy(
                            today = board.today,
                            timeZone = board.timeZone,
                            selectedDate = current.selectedDate
                                .takeIf { it.isNotBlank() && it >= board.today }
                                ?: board.today,
                            placements = board.placements.filter { it.boardDate >= board.today },
                            savingTaskIds = emptySet(),
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                    scheduleMidnightRefresh(board.refreshAfterSeconds)
                }
            }
        }
    }

    private fun scheduleMidnightRefresh(afterSeconds: Long) {
        rolloverJob?.cancel()
        rolloverJob = viewModelScope.launch {
            delay(afterSeconds.coerceAtLeast(1) * 1_000L + 1_000L)
            load()
        }
    }

    private fun selectDate(date: String) {
        val current = state.value
        if (date.isBlank() || current.today.isBlank() || date < current.today) {
            _effects.tryEmit(TaskBoardUiEffect.ShowMessage("Los días anteriores ya no están disponibles."))
            return
        }
        _state.update { it.copy(selectedDate = date, errorMessage = null) }
    }

    private fun savePlacement(
        taskId: String,
        boardDate: String,
        positionX: Double,
        positionY: Double,
    ) {
        launch {
            mutationMutex.withLock {
                val before = state.value.placements
                val previous = before.firstOrNull { it.taskId == taskId }
                val optimistic = TaskBoardPlacement(
                    taskId = taskId,
                    boardDate = boardDate,
                    positionX = positionX.coerceIn(0.0, 1.0),
                    positionY = positionY.coerceIn(0.0, 1.0),
                    createdAt = previous?.createdAt.orEmpty(),
                    updatedAt = previous?.updatedAt.orEmpty(),
                )
                _state.update {
                    it.copy(
                        placements = before.filterNot { item -> item.taskId == taskId } + optimistic,
                        savingTaskIds = it.savingTaskIds + taskId,
                        errorMessage = null,
                    )
                }
                when (
                    val result = placeTaskOnBoardUseCase(
                        taskId = taskId,
                        boardDate = boardDate,
                        positionX = optimistic.positionX,
                        positionY = optimistic.positionY,
                    )
                ) {
                    is AppResult.Error -> {
                        _state.update {
                            it.copy(
                                placements = before,
                                savingTaskIds = it.savingTaskIds - taskId,
                                errorMessage = result.message,
                            )
                        }
                        _effects.emit(TaskBoardUiEffect.ShowMessage(result.message))
                    }
                    is AppResult.Success -> _state.update {
                        it.copy(
                            placements = it.placements.filterNot { item -> item.taskId == taskId } + result.data,
                            savingTaskIds = it.savingTaskIds - taskId,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    private fun removePlacement(taskId: String) {
        launch {
            mutationMutex.withLock {
                val before = state.value.placements
                if (before.none { it.taskId == taskId }) return@withLock
                _state.update {
                    it.copy(
                        placements = before.filterNot { item -> item.taskId == taskId },
                        savingTaskIds = it.savingTaskIds + taskId,
                        errorMessage = null,
                    )
                }
                when (val result = removeTaskFromBoardUseCase(taskId)) {
                    is AppResult.Error -> {
                        _state.update {
                            it.copy(
                                placements = before,
                                savingTaskIds = it.savingTaskIds - taskId,
                                errorMessage = result.message,
                            )
                        }
                        _effects.emit(TaskBoardUiEffect.ShowMessage(result.message))
                    }
                    is AppResult.Success -> _state.update {
                        it.copy(
                            savingTaskIds = it.savingTaskIds - taskId,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }
}
