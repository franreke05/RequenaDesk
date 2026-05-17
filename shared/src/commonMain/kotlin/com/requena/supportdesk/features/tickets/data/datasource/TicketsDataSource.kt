package com.requena.supportdesk.features.tickets.data.datasource

import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.network.AdminSessionContext
import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.requireSuccess
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.tickets.data.dto.CreateTicketMessageRequestDto
import com.requena.supportdesk.features.tickets.data.dto.CreateTicketRequestDto
import com.requena.supportdesk.features.tickets.data.dto.TicketDto
import com.requena.supportdesk.features.tickets.data.dto.TicketCloseAcceptanceRequestDto
import com.requena.supportdesk.features.tickets.data.dto.TicketSatisfactionRequestDto
import com.requena.supportdesk.features.tickets.data.dto.UpdateTicketPriorityRequestDto
import com.requena.supportdesk.features.tickets.data.dto.UpdateTicketStatusRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

interface TicketsDataSource {
    suspend fun getTickets(): List<TicketDto>
    suspend fun getTicket(id: String): TicketDto?
    suspend fun createTicket(request: CreateTicketRequestDto): TicketDto
    suspend fun replyTicket(ticketId: String, request: CreateTicketMessageRequestDto)
    suspend fun changeStatus(ticketId: String, request: UpdateTicketStatusRequestDto)
    suspend fun changePriority(ticketId: String, request: UpdateTicketPriorityRequestDto)
    suspend fun acceptClose(ticketId: String, request: TicketCloseAcceptanceRequestDto): TicketDto
    suspend fun rateTicket(ticketId: String, request: TicketSatisfactionRequestDto): TicketDto
}

class RemoteTicketsDataSource(
    private val httpClient: HttpClient,
) : TicketsDataSource {
    override suspend fun getTickets(): List<TicketDto> =
        httpClient.get("${supportDeskBaseUrl()}${ticketsPath()}").requireApiData()

    override suspend fun getTicket(id: String): TicketDto? =
        httpClient.get("${supportDeskBaseUrl()}${ticketsPath()}/$id").requireApiData()

    override suspend fun createTicket(request: CreateTicketRequestDto): TicketDto =
        httpClient.post("${supportDeskBaseUrl()}${ticketsPath()}") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun replyTicket(ticketId: String, request: CreateTicketMessageRequestDto) {
        httpClient.post("${supportDeskBaseUrl()}${ticketsPath()}/$ticketId/messages") {
            setBody(jsonRequestBody(request))
        }.requireSuccess()
    }

    override suspend fun changeStatus(ticketId: String, request: UpdateTicketStatusRequestDto) {
        httpClient.patch("${supportDeskBaseUrl()}/admin/tickets/$ticketId/status") {
            setBody(jsonRequestBody(request))
        }.requireSuccess()
    }

    override suspend fun changePriority(ticketId: String, request: UpdateTicketPriorityRequestDto) {
        httpClient.patch("${supportDeskBaseUrl()}/admin/tickets/$ticketId/priority") {
            setBody(jsonRequestBody(request))
        }.requireSuccess()
    }

    override suspend fun acceptClose(ticketId: String, request: TicketCloseAcceptanceRequestDto): TicketDto =
        httpClient.post("${supportDeskBaseUrl()}${ticketsPath()}/$ticketId/accept-close") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun rateTicket(ticketId: String, request: TicketSatisfactionRequestDto): TicketDto =
        httpClient.post("${supportDeskBaseUrl()}/client/tickets/$ticketId/satisfaction") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    private fun ticketsPath(): String =
        if (AdminSessionContext.currentUser()?.role == UserRole.CLIENT) "/client/tickets" else "/admin/tickets"
}
