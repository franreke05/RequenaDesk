package com.requena.supportdesk.features.business.projects

import com.requena.supportdesk.core.result.AppResult
import kotlinx.coroutines.CancellationException

class BusinessProjectsRepositoryImpl(
    private val dataSource: BusinessProjectsDataSource,
) : BusinessProjectsRepository {
    override suspend fun workspace(
        query: String?,
        projectStatus: BusinessProjectStatus?,
        taskStatus: BusinessTaskStatus?,
        projectId: String?,
        customerId: String?,
        labelId: String?,
    ) = request { dataSource.workspace(query, projectStatus, taskStatus, projectId, customerId, labelId) }

    override suspend fun createProject(input: CreateBusinessProjectInput) = request { dataSource.createProject(input) }
    override suspend fun updateProject(projectId: String, input: UpdateBusinessProjectInput) = request { dataSource.updateProject(projectId, input) }
    override suspend fun completeProject(projectId: String, expectedVersion: Int) = request { dataSource.transitionProject(projectId, "complete", expectedVersion) }
    override suspend fun reopenProject(projectId: String, expectedVersion: Int) = request { dataSource.transitionProject(projectId, "reopen", expectedVersion) }
    override suspend fun archiveProject(projectId: String, expectedVersion: Int) = request { dataSource.transitionProject(projectId, "archive", expectedVersion) }
    override suspend fun createTask(input: CreateBusinessTaskInput) = request { dataSource.createTask(input) }
    override suspend fun updateTask(taskId: String, input: UpdateBusinessTaskInput) = request { dataSource.updateTask(taskId, input) }
    override suspend fun createLabel(input: CreateBusinessTaskLabelInput) = request { dataSource.createLabel(input) }
    override suspend fun updateLabel(labelId: String, input: UpdateBusinessTaskLabelInput) = request { dataSource.updateLabel(labelId, input) }
    override suspend fun archiveLabel(labelId: String, expectedVersion: Int) = request { dataSource.archiveLabel(labelId, expectedVersion) }
    override suspend fun placeTask(taskId: String, input: BusinessTaskBoardPlacementInput) = request { dataSource.placeTask(taskId, input) }
    override suspend fun removeTaskPlacement(taskId: String) = request { dataSource.removeTaskPlacement(taskId) }
    override suspend fun activeWorkSession() = request { dataSource.activeWorkSession() }
    override suspend fun taskWorkSessions(taskId: String) = request { dataSource.taskWorkSessions(taskId) }
    override suspend fun startWorkSession(taskId: String) = request { dataSource.startWorkSession(taskId) }
    override suspend fun pauseWorkSession(sessionId: String) = request { dataSource.workSessionAction(sessionId, "pause") }
    override suspend fun resumeWorkSession(sessionId: String) = request { dataSource.workSessionAction(sessionId, "resume") }
    override suspend fun stopWorkSession(sessionId: String) = request { dataSource.workSessionAction(sessionId, "stop") }
    override suspend fun checkpointWorkSession(sessionId: String) = request { dataSource.workSessionAction(sessionId, "checkpoint") }

    private suspend fun <T> request(block: suspend () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        AppResult.Error(error.message ?: "No se pudo completar la operacion.", error)
    }
}
