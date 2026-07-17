package com.requena.supportdesk.features.programs.data.datasource

import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.programs.data.dto.ClientProgramBillingPreviewDto
import com.requena.supportdesk.features.programs.data.dto.ClientProgramRequestDto
import com.requena.supportdesk.features.programs.data.dto.ClientProgramsOverviewDto
import com.requena.supportdesk.features.programs.data.dto.CreateProgramRequestsRequestDto
import com.requena.supportdesk.features.programs.data.dto.DecideProgramRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

interface ProgramsDataSource {
    suspend fun getClientPrograms(): ClientProgramsOverviewDto
    suspend fun createProgramRequests(request: CreateProgramRequestsRequestDto): List<ClientProgramRequestDto>
    suspend fun getAdminProgramRequests(): List<ClientProgramRequestDto>
    suspend fun approveProgramRequest(requestId: String, request: DecideProgramRequestDto): ClientProgramRequestDto
    suspend fun rejectProgramRequest(requestId: String, request: DecideProgramRequestDto): ClientProgramRequestDto
    suspend fun getBillingPreview(clientId: String, period: String): ClientProgramBillingPreviewDto
}

class RemoteProgramsDataSource(
    private val httpClient: HttpClient,
) : ProgramsDataSource {
    override suspend fun getClientPrograms(): ClientProgramsOverviewDto =
        httpClient.get("${supportDeskBaseUrl()}/client/programs").requireApiData()

    override suspend fun createProgramRequests(request: CreateProgramRequestsRequestDto): List<ClientProgramRequestDto> =
        httpClient.post("${supportDeskBaseUrl()}/client/program-requests") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun getAdminProgramRequests(): List<ClientProgramRequestDto> =
        httpClient.get("${supportDeskBaseUrl()}/admin/program-requests").requireApiData()

    override suspend fun approveProgramRequest(
        requestId: String,
        request: DecideProgramRequestDto,
    ): ClientProgramRequestDto =
        httpClient.post("${supportDeskBaseUrl()}/admin/program-requests/$requestId/approve") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun rejectProgramRequest(
        requestId: String,
        request: DecideProgramRequestDto,
    ): ClientProgramRequestDto =
        httpClient.post("${supportDeskBaseUrl()}/admin/program-requests/$requestId/reject") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun getBillingPreview(clientId: String, period: String): ClientProgramBillingPreviewDto =
        httpClient.get("${supportDeskBaseUrl()}/admin/clients/$clientId/billing-preview?period=$period").requireApiData()
}
