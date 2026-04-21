package com.requena.supportdesk.features.tasks.data.datasource

import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.requireSuccess
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.tasks.data.dto.CreateTaskLabelRequestDto
import com.requena.supportdesk.features.tasks.data.dto.CreateTaskRequestDto
import com.requena.supportdesk.features.tasks.data.dto.CreateTimeLogRequestDto
import com.requena.supportdesk.features.tasks.data.dto.TaskDto
import com.requena.supportdesk.features.tasks.data.dto.TaskLabelDto
import com.requena.supportdesk.features.tasks.data.dto.TaskLogDto
import com.requena.supportdesk.features.tasks.data.dto.UpdateTaskLabelRequestDto
import com.requena.supportdesk.features.tasks.data.dto.UpdateTaskRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

interface TasksDataSource {
    suspend fun getLabels(): List<TaskLabelDto>
    suspend fun getTasks(): List<TaskDto>
    suspend fun getTimeLogs(): List<TaskLogDto>
    suspend fun createTask(request: CreateTaskRequestDto): TaskDto
    suspend fun updateTask(taskId: String, request: UpdateTaskRequestDto): TaskDto
    suspend fun deleteTask(taskId: String)
    suspend fun createLabel(request: CreateTaskLabelRequestDto): TaskLabelDto
    suspend fun updateLabel(labelId: String, request: UpdateTaskLabelRequestDto): TaskLabelDto
    suspend fun deleteLabel(labelId: String)
    suspend fun createTimeLog(request: CreateTimeLogRequestDto): TaskLogDto
}

class RemoteTasksDataSource(
    private val httpClient: HttpClient,
) : TasksDataSource {
    override suspend fun getLabels(): List<TaskLabelDto> =
        httpClient.get("${supportDeskBaseUrl()}/admin/labels").requireApiData()

    override suspend fun getTasks(): List<TaskDto> =
        httpClient.get("${supportDeskBaseUrl()}/admin/tasks").requireApiData()

    override suspend fun getTimeLogs(): List<TaskLogDto> =
        httpClient.get("${supportDeskBaseUrl()}/admin/time-logs").requireApiData()

    override suspend fun createTask(request: CreateTaskRequestDto): TaskDto =
        httpClient.post("${supportDeskBaseUrl()}/admin/tasks") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun updateTask(taskId: String, request: UpdateTaskRequestDto): TaskDto =
        httpClient.patch("${supportDeskBaseUrl()}/admin/tasks/$taskId") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun deleteTask(taskId: String) {
        httpClient.delete("${supportDeskBaseUrl()}/admin/tasks/$taskId").requireSuccess()
    }

    override suspend fun createLabel(request: CreateTaskLabelRequestDto): TaskLabelDto =
        httpClient.post("${supportDeskBaseUrl()}/admin/labels") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun updateLabel(labelId: String, request: UpdateTaskLabelRequestDto): TaskLabelDto =
        httpClient.patch("${supportDeskBaseUrl()}/admin/labels/$labelId") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun deleteLabel(labelId: String) {
        httpClient.delete("${supportDeskBaseUrl()}/admin/labels/$labelId").requireSuccess()
    }

    override suspend fun createTimeLog(request: CreateTimeLogRequestDto): TaskLogDto =
        httpClient.post("${supportDeskBaseUrl()}/admin/time-logs") {
            setBody(jsonRequestBody(request))
        }.requireApiData()
}
