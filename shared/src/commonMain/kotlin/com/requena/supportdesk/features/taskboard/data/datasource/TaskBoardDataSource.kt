package com.requena.supportdesk.features.taskboard.data.datasource

import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.requireSuccess
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.taskboard.data.dto.TaskBoardPlacementDto
import com.requena.supportdesk.features.taskboard.data.dto.TaskBoardSnapshotDto
import com.requena.supportdesk.features.taskboard.data.dto.UpdateTaskBoardPlacementRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody

interface TaskBoardDataSource {
    suspend fun getBoard(): TaskBoardSnapshotDto
    suspend fun upsertPlacement(
        taskId: String,
        request: UpdateTaskBoardPlacementRequestDto,
    ): TaskBoardPlacementDto
    suspend fun deletePlacement(taskId: String)
}

class RemoteTaskBoardDataSource(
    private val httpClient: HttpClient,
) : TaskBoardDataSource {
    override suspend fun getBoard(): TaskBoardSnapshotDto =
        httpClient.get("${supportDeskBaseUrl()}/admin/task-board").requireApiData()

    override suspend fun upsertPlacement(
        taskId: String,
        request: UpdateTaskBoardPlacementRequestDto,
    ): TaskBoardPlacementDto =
        httpClient.put("${supportDeskBaseUrl()}/admin/task-board/placements/$taskId") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun deletePlacement(taskId: String) {
        httpClient.delete("${supportDeskBaseUrl()}/admin/task-board/placements/$taskId").requireSuccess()
    }
}
