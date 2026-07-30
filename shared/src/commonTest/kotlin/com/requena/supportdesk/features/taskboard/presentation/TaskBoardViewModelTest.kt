package com.requena.supportdesk.features.taskboard.presentation

import com.requena.supportdesk.core.model.TaskBoardPlacement
import com.requena.supportdesk.core.model.TaskBoardSnapshot
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.taskboard.domain.repository.TaskBoardRepository
import com.requena.supportdesk.features.taskboard.domain.usecase.GetTaskBoardUseCase
import com.requena.supportdesk.features.taskboard.domain.usecase.PlaceTaskOnBoardUseCase
import com.requena.supportdesk.features.taskboard.domain.usecase.RemoveTaskFromBoardUseCase
import com.requena.supportdesk.features.taskboard.presentation.effect.TaskBoardUiEffect
import com.requena.supportdesk.features.taskboard.presentation.event.TaskBoardUiEvent
import com.requena.supportdesk.features.taskboard.presentation.viewmodel.TaskBoardViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskBoardViewModelTest {
    @Test
    fun `place and remove use optimistic state and persist the final placement`() = runBlocking {
        val repository = FakeTaskBoardRepository()
        val viewModel = viewModel(repository)
        withTimeout(TIMEOUT) { viewModel.state.first { !it.isLoading && it.today.isNotBlank() } }

        viewModel.onEvent(TaskBoardUiEvent.PlaceTask("task-1", "2026-07-31", 0.3, 0.7))
        val placed = withTimeout(TIMEOUT) {
            viewModel.state.first {
                it.placementFor("task-1")?.positionX == 0.3 && "task-1" !in it.savingTaskIds
            }
        }
        assertEquals("2026-07-31", placed.placementFor("task-1")?.boardDate)
        assertEquals(0.7, repository.placements.single().positionY)

        viewModel.onEvent(TaskBoardUiEvent.RemoveTask("task-1"))
        val removed = withTimeout(TIMEOUT) {
            viewModel.state.first { it.placementFor("task-1") == null && "task-1" !in it.savingTaskIds }
        }
        assertNull(removed.errorMessage)
        assertTrue(repository.placements.isEmpty())
        viewModel.clear()
    }

    @Test
    fun `failed movement restores the persisted coordinates and emits an error`() = runBlocking {
        val original = placement("task-1", 0.1, 0.2)
        val repository = FakeTaskBoardRepository(mutableListOf(original)).apply {
            failNextUpsert = true
        }
        val viewModel = viewModel(repository)
        withTimeout(TIMEOUT) { viewModel.state.first { it.placementFor("task-1") != null && !it.isLoading } }
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        viewModel.onEvent(TaskBoardUiEvent.MoveTask("task-1", 0.8, 0.9))
        val restored = withTimeout(TIMEOUT) {
            viewModel.state.first { it.errorMessage != null && "task-1" !in it.savingTaskIds }
        }

        assertEquals(0.1, restored.placementFor("task-1")?.positionX)
        assertEquals(0.2, restored.placementFor("task-1")?.positionY)
        assertEquals(
            TaskBoardUiEffect.ShowMessage("No se pudo guardar"),
            withTimeout(TIMEOUT) { effect.await() },
        )
        viewModel.clear()
    }

    private fun viewModel(repository: TaskBoardRepository) = TaskBoardViewModel(
        getTaskBoardUseCase = GetTaskBoardUseCase(repository),
        placeTaskOnBoardUseCase = PlaceTaskOnBoardUseCase(repository),
        removeTaskFromBoardUseCase = RemoveTaskFromBoardUseCase(repository),
    )
}

private class FakeTaskBoardRepository(
    val placements: MutableList<TaskBoardPlacement> = mutableListOf(),
) : TaskBoardRepository {
    var failNextUpsert = false

    override suspend fun getBoard(): AppResult<TaskBoardSnapshot> =
        AppResult.Success(
            TaskBoardSnapshot(
                today = "2026-07-30",
                timeZone = "Europe/Madrid",
                refreshAfterSeconds = 86_400,
                placements = placements.toList(),
            ),
        )

    override suspend fun upsertPlacement(
        taskId: String,
        boardDate: String,
        positionX: Double,
        positionY: Double,
    ): AppResult<TaskBoardPlacement> {
        if (failNextUpsert) {
            failNextUpsert = false
            return AppResult.Error("No se pudo guardar")
        }
        val saved = placement(taskId, positionX, positionY, boardDate)
        placements.removeAll { it.taskId == taskId }
        placements += saved
        return AppResult.Success(saved)
    }

    override suspend fun deletePlacement(taskId: String): AppResult<Unit> {
        placements.removeAll { it.taskId == taskId }
        return AppResult.Success(Unit)
    }
}

private fun placement(
    taskId: String,
    x: Double,
    y: Double,
    date: String = "2026-07-30",
) = TaskBoardPlacement(
    taskId = taskId,
    boardDate = date,
    positionX = x,
    positionY = y,
    createdAt = "2026-07-30T08:00:00Z",
    updatedAt = "2026-07-30T08:00:00Z",
)

private const val TIMEOUT = 5_000L
