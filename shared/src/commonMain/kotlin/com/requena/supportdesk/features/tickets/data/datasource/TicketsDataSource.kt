package com.requena.supportdesk.features.tickets.data.datasource

import com.requena.supportdesk.core.network.ApiEnvelope
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.tickets.data.dto.CreateTicketMessageRequestDto
import com.requena.supportdesk.features.tickets.data.dto.CreateTicketRequestDto
import com.requena.supportdesk.features.tickets.data.dto.TicketDto
import com.requena.supportdesk.features.tickets.data.dto.UpdateTicketPriorityRequestDto
import com.requena.supportdesk.features.tickets.data.dto.UpdateTicketStatusRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

interface TicketsDataSource {
    suspend fun getTickets(): List<TicketDto>
    suspend fun getTicket(id: String): TicketDto?
    suspend fun createTicket(request: CreateTicketRequestDto): TicketDto
    suspend fun replyTicket(ticketId: String, request: CreateTicketMessageRequestDto)
    suspend fun changeStatus(ticketId: String, request: UpdateTicketStatusRequestDto)
    suspend fun changePriority(ticketId: String, request: UpdateTicketPriorityRequestDto)
}

class RemoteTicketsDataSource(
    private val httpClient: HttpClient,
) : TicketsDataSource {
    override suspend fun getTickets(): List<TicketDto> =
        httpClient.get("${supportDeskBaseUrl()}/tickets").body<ApiEnvelope<List<TicketDto>>>().data

    override suspend fun getTicket(id: String): TicketDto? =
        httpClient.get("${supportDeskBaseUrl()}/tickets/$id").body<ApiEnvelope<TicketDto>>().data

    override suspend fun createTicket(request: CreateTicketRequestDto): TicketDto =
        httpClient.post("${supportDeskBaseUrl()}/tickets") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<ApiEnvelope<TicketDto>>().data

    override suspend fun replyTicket(ticketId: String, request: CreateTicketMessageRequestDto) {
        httpClient.post("${supportDeskBaseUrl()}/tickets/$ticketId/messages") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun changeStatus(ticketId: String, request: UpdateTicketStatusRequestDto) {
        httpClient.patch("${supportDeskBaseUrl()}/tickets/$ticketId/status") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun changePriority(ticketId: String, request: UpdateTicketPriorityRequestDto) {
        httpClient.patch("${supportDeskBaseUrl()}/tickets/$ticketId/priority") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
