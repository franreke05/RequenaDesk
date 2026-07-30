package com.requena.supportdesk.features.business.projects

import com.requena.supportdesk.core.result.AppResult
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class BusinessProjectsViewModelTest {
    @Test
    fun refreshLoadsWorkspaceAndFailedDragRollsBack() = runBlocking {
        val repository = FakeBusinessProjectsRepository()
        val viewModel = BusinessProjectsViewModel(repository)
        viewModel.refresh()
        withTimeout(TIMEOUT) { viewModel.state.first { !it.isLoading && it.board != null } }
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        repository.failNextPlacement = true
        viewModel.placeTask("task-1", "2026-07-31", 0.8, 0.9)
        val restored = withTimeout(TIMEOUT) {
            viewModel.state.first { it.errorMessage != null && "task-1" !in it.savingIds }
        }

        assertEquals(0.1, restored.board?.placements?.single()?.positionX)
        assertEquals(0.2, restored.board?.placements?.single()?.positionY)
        assertEquals(
            BusinessProjectsEffect.ShowMessage("No se pudo guardar"),
            withTimeout(TIMEOUT) { effect.await() },
        )
        viewModel.clear()
    }

    @Test
    fun successfulPlacementReplacesTheOnlyDailyPosition() = runBlocking {
        val repository = FakeBusinessProjectsRepository()
        val viewModel = BusinessProjectsViewModel(repository)
        viewModel.refresh()
        withTimeout(TIMEOUT) { viewModel.state.first { !it.isLoading && it.board != null } }

        viewModel.placeTask("task-1", "2026-08-01", 0.6, 0.7)
        val saved = withTimeout(TIMEOUT) {
            viewModel.state.first { it.board?.placements?.singleOrNull()?.boardDate == "2026-08-01" && "task-1" !in it.savingIds }
        }
        assertEquals(1, saved.board?.placements?.size)
        assertEquals(0.6, saved.board?.placements?.single()?.positionX)
        viewModel.clear()
    }
}

private class FakeBusinessProjectsRepository : BusinessProjectsRepository {
    var failNextPlacement = false
    private val task = BusinessTask(
        id = "task-1",
        title = "Tarea",
        priority = BusinessTaskPriority.NORMAL,
        status = BusinessTaskStatus.TODO,
        version = 1,
        updatedAt = "2026-07-30T08:00:00Z",
    )
    private var placements = listOf(
        BusinessTaskBoardPlacement("task-1", "2026-07-30", 0.1, 0.2, "2026-07-30T08:00:00Z"),
    )

    override suspend fun workspace(
        query: String?,
        projectStatus: BusinessProjectStatus?,
        taskStatus: BusinessTaskStatus?,
        projectId: String?,
        customerId: String?,
        labelId: String?,
    ) = AppResult.Success(
        BusinessProjectsWorkspace(
            projects = emptyList(),
            tasks = listOf(task),
            labels = emptyList(),
            board = BusinessTaskBoard("2026-07-30", "Europe/Madrid", 86_400, listOf(task), placements),
        ),
    )

    override suspend fun placeTask(taskId: String, input: BusinessTaskBoardPlacementInput): AppResult<BusinessTaskBoardPlacement> {
        if (failNextPlacement) {
            failNextPlacement = false
            return AppResult.Error("No se pudo guardar")
        }
        val saved = BusinessTaskBoardPlacement(taskId, input.boardDate, input.positionX, input.positionY, "2026-07-30T09:00:00Z")
        placements = placements.filterNot { it.taskId == taskId } + saved
        return AppResult.Success(saved)
    }

    override suspend fun removeTaskPlacement(taskId: String): AppResult<Unit> {
        placements = placements.filterNot { it.taskId == taskId }
        return AppResult.Success(Unit)
    }

    override suspend fun createProject(input: CreateBusinessProjectInput) = unsupported<BusinessProject>()
    override suspend fun updateProject(projectId: String, input: UpdateBusinessProjectInput) = unsupported<BusinessProject>()
    override suspend fun completeProject(projectId: String, expectedVersion: Int) = unsupported<BusinessProject>()
    override suspend fun reopenProject(projectId: String, expectedVersion: Int) = unsupported<BusinessProject>()
    override suspend fun archiveProject(projectId: String, expectedVersion: Int) = unsupported<BusinessProject>()
    override suspend fun createTask(input: CreateBusinessTaskInput) = unsupported<BusinessTask>()
    override suspend fun updateTask(taskId: String, input: UpdateBusinessTaskInput) = unsupported<BusinessTask>()
    override suspend fun createLabel(input: CreateBusinessTaskLabelInput) = unsupported<BusinessTaskLabel>()
    override suspend fun updateLabel(labelId: String, input: UpdateBusinessTaskLabelInput) = unsupported<BusinessTaskLabel>()
    override suspend fun archiveLabel(labelId: String, expectedVersion: Int) = unsupported<BusinessTaskLabel>()
    override suspend fun activeWorkSession() = AppResult.Success(null)
    override suspend fun taskWorkSessions(taskId: String) = AppResult.Success(emptyList<BusinessWorkSession>())
    override suspend fun startWorkSession(taskId: String) = unsupported<BusinessWorkSession>()
    override suspend fun pauseWorkSession(sessionId: String) = unsupported<BusinessWorkSession>()
    override suspend fun resumeWorkSession(sessionId: String) = unsupported<BusinessWorkSession>()
    override suspend fun stopWorkSession(sessionId: String) = unsupported<BusinessWorkSession>()
    override suspend fun checkpointWorkSession(sessionId: String) = unsupported<BusinessWorkSession>()

    private fun <T> unsupported(): AppResult<T> = AppResult.Error("Unsupported")
}

private const val TIMEOUT = 5_000L
