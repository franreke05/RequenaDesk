package com.requena.supportdesk.features.business.projects

import com.requena.supportdesk.core.result.AppResult

interface BusinessProjectsRepository {
    suspend fun workspace(
        query: String? = null,
        projectStatus: BusinessProjectStatus? = null,
        taskStatus: BusinessTaskStatus? = null,
        projectId: String? = null,
        customerId: String? = null,
        labelId: String? = null,
    ): AppResult<BusinessProjectsWorkspace>

    suspend fun createProject(input: CreateBusinessProjectInput): AppResult<BusinessProject>
    suspend fun updateProject(projectId: String, input: UpdateBusinessProjectInput): AppResult<BusinessProject>
    suspend fun completeProject(projectId: String, expectedVersion: Int): AppResult<BusinessProject>
    suspend fun reopenProject(projectId: String, expectedVersion: Int): AppResult<BusinessProject>
    suspend fun archiveProject(projectId: String, expectedVersion: Int): AppResult<BusinessProject>

    suspend fun createTask(input: CreateBusinessTaskInput): AppResult<BusinessTask>
    suspend fun updateTask(taskId: String, input: UpdateBusinessTaskInput): AppResult<BusinessTask>
    suspend fun createLabel(input: CreateBusinessTaskLabelInput): AppResult<BusinessTaskLabel>
    suspend fun updateLabel(labelId: String, input: UpdateBusinessTaskLabelInput): AppResult<BusinessTaskLabel>
    suspend fun archiveLabel(labelId: String, expectedVersion: Int): AppResult<BusinessTaskLabel>

    suspend fun placeTask(taskId: String, input: BusinessTaskBoardPlacementInput): AppResult<BusinessTaskBoardPlacement>
    suspend fun removeTaskPlacement(taskId: String): AppResult<Unit>

    suspend fun activeWorkSession(): AppResult<BusinessWorkSession?>
    suspend fun taskWorkSessions(taskId: String): AppResult<List<BusinessWorkSession>>
    suspend fun startWorkSession(taskId: String): AppResult<BusinessWorkSession>
    suspend fun pauseWorkSession(sessionId: String): AppResult<BusinessWorkSession>
    suspend fun resumeWorkSession(sessionId: String): AppResult<BusinessWorkSession>
    suspend fun stopWorkSession(sessionId: String): AppResult<BusinessWorkSession>
    suspend fun checkpointWorkSession(sessionId: String): AppResult<BusinessWorkSession>
}
