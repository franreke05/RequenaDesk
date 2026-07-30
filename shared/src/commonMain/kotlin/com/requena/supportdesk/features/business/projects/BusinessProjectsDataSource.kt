package com.requena.supportdesk.features.business.projects

import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.encodeURLParameter

interface BusinessProjectsDataSource {
    suspend fun workspace(
        query: String?,
        projectStatus: BusinessProjectStatus?,
        taskStatus: BusinessTaskStatus?,
        projectId: String?,
        customerId: String?,
        labelId: String?,
    ): BusinessProjectsWorkspace

    suspend fun createProject(input: CreateBusinessProjectInput): BusinessProject
    suspend fun updateProject(projectId: String, input: UpdateBusinessProjectInput): BusinessProject
    suspend fun transitionProject(projectId: String, action: String, expectedVersion: Int): BusinessProject
    suspend fun createTask(input: CreateBusinessTaskInput): BusinessTask
    suspend fun updateTask(taskId: String, input: UpdateBusinessTaskInput): BusinessTask
    suspend fun createLabel(input: CreateBusinessTaskLabelInput): BusinessTaskLabel
    suspend fun updateLabel(labelId: String, input: UpdateBusinessTaskLabelInput): BusinessTaskLabel
    suspend fun archiveLabel(labelId: String, expectedVersion: Int): BusinessTaskLabel
    suspend fun placeTask(taskId: String, input: BusinessTaskBoardPlacementInput): BusinessTaskBoardPlacement
    suspend fun removeTaskPlacement(taskId: String)
    suspend fun activeWorkSession(): BusinessWorkSession?
    suspend fun taskWorkSessions(taskId: String): List<BusinessWorkSession>
    suspend fun startWorkSession(taskId: String): BusinessWorkSession
    suspend fun workSessionAction(sessionId: String, action: String): BusinessWorkSession
}

class RemoteBusinessProjectsDataSource(
    private val httpClient: HttpClient,
) : BusinessProjectsDataSource {
    private val base get() = "${supportDeskBaseUrl()}/client/apps"

    override suspend fun workspace(
        query: String?,
        projectStatus: BusinessProjectStatus?,
        taskStatus: BusinessTaskStatus?,
        projectId: String?,
        customerId: String?,
        labelId: String?,
    ): BusinessProjectsWorkspace {
        val parameters = listOfNotNull(
            query?.takeIf(String::isNotBlank)?.let { "query=${it.encodeURLParameter()}" },
            projectStatus?.let { "projectStatus=${it.name}" },
            taskStatus?.let { "taskStatus=${it.name}" },
            projectId?.takeIf(String::isNotBlank)?.let { "projectId=${it.encodeURLParameter()}" },
            customerId?.takeIf(String::isNotBlank)?.let { "customerId=${it.encodeURLParameter()}" },
            labelId?.takeIf(String::isNotBlank)?.let { "labelId=${it.encodeURLParameter()}" },
        )
        val suffix = parameters.takeIf(List<String>::isNotEmpty)?.joinToString("&", prefix = "?").orEmpty()
        return httpClient.get("$base/projects/workspace$suffix").requireApiData()
    }

    override suspend fun createProject(input: CreateBusinessProjectInput): BusinessProject =
        httpClient.post("$base/projects") { setBody(jsonRequestBody(input)) }.requireApiData()

    override suspend fun updateProject(projectId: String, input: UpdateBusinessProjectInput): BusinessProject =
        httpClient.patch("$base/projects/$projectId") { setBody(jsonRequestBody(input)) }.requireApiData()

    override suspend fun transitionProject(projectId: String, action: String, expectedVersion: Int): BusinessProject =
        httpClient.post("$base/projects/$projectId/$action") {
            setBody(jsonRequestBody(ProjectTransitionInput(expectedVersion)))
        }.requireApiData()

    override suspend fun createTask(input: CreateBusinessTaskInput): BusinessTask =
        httpClient.post("$base/tasks") { setBody(jsonRequestBody(input)) }.requireApiData()

    override suspend fun updateTask(taskId: String, input: UpdateBusinessTaskInput): BusinessTask =
        httpClient.patch("$base/tasks/$taskId") { setBody(jsonRequestBody(input)) }.requireApiData()

    override suspend fun createLabel(input: CreateBusinessTaskLabelInput): BusinessTaskLabel =
        httpClient.post("$base/tasks/labels") { setBody(jsonRequestBody(input)) }.requireApiData()

    override suspend fun updateLabel(labelId: String, input: UpdateBusinessTaskLabelInput): BusinessTaskLabel =
        httpClient.patch("$base/tasks/labels/$labelId") { setBody(jsonRequestBody(input)) }.requireApiData()

    override suspend fun archiveLabel(labelId: String, expectedVersion: Int): BusinessTaskLabel =
        httpClient.post("$base/tasks/labels/$labelId/archive") {
            setBody(jsonRequestBody(ProjectTransitionInput(expectedVersion)))
        }.requireApiData()

    override suspend fun placeTask(taskId: String, input: BusinessTaskBoardPlacementInput): BusinessTaskBoardPlacement =
        httpClient.put("$base/task-board/placements/$taskId") { setBody(jsonRequestBody(input)) }.requireApiData()

    override suspend fun removeTaskPlacement(taskId: String) {
        httpClient.delete("$base/task-board/placements/$taskId").requireApiData<BusinessMutationResult>()
    }

    override suspend fun activeWorkSession(): BusinessWorkSession? =
        httpClient.get("$base/work-sessions/active").requireApiData()

    override suspend fun taskWorkSessions(taskId: String): List<BusinessWorkSession> =
        httpClient.get("$base/tasks/$taskId/work-sessions").requireApiData()

    override suspend fun startWorkSession(taskId: String): BusinessWorkSession =
        httpClient.post("$base/work-sessions/start") {
            setBody(jsonRequestBody(StartBusinessWorkSessionInput(taskId)))
        }.requireApiData()

    override suspend fun workSessionAction(sessionId: String, action: String): BusinessWorkSession =
        httpClient.post("$base/work-sessions/$sessionId/$action").requireApiData()
}
