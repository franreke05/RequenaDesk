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
    override suspend fun getTickets(): List<TicketDto> {
        val url = "${supportDeskBaseUrl()}${ticketsPath()}"
        return try {
            println("[DEBUG] GET $url")
            httpClient.get(url).requireApiData()
        } catch (e: Exception) {
            println("[ERROR] getTickets failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun getTicket(id: String): TicketDto? {
        val url = "${supportDeskBaseUrl()}${ticketsPath()}/$id"
        return try {
            println("[DEBUG] GET $url")
            httpClient.get(url).requireApiData()
        } catch (e: Exception) {
            println("[ERROR] getTicket($id) failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun createTicket(request: CreateTicketRequestDto): TicketDto {
        val url = "${supportDeskBaseUrl()}${ticketsPath()}"
        return try {
            println("[DEBUG] POST $url with subject: ${request.subject}")
            httpClient.post(url) {
                setBody(jsonRequestBody(request))
            }.requireApiData()
        } catch (e: Exception) {
            println("[ERROR] createTicket failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun replyTicket(ticketId: String, request: CreateTicketMessageRequestDto) {
        val url = "${supportDeskBaseUrl()}${ticketsPath()}/$ticketId/messages"
        try {
            println("[DEBUG] POST $url")
            httpClient.post(url) {
                setBody(jsonRequestBody(request))
            }.requireSuccess()
        } catch (e: Exception) {
            println("[ERROR] replyTicket($ticketId) failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun changeStatus(ticketId: String, request: UpdateTicketStatusRequestDto) {
        val url = "${supportDeskBaseUrl()}/admin/tickets/$ticketId/status"
        try {
            println("[DEBUG] PATCH $url")
            httpClient.patch(url) {
                setBody(jsonRequestBody(request))
            }.requireSuccess()
        } catch (e: Exception) {
            println("[ERROR] changeStatus($ticketId) failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun changePriority(ticketId: String, request: UpdateTicketPriorityRequestDto) {
        val url = "${supportDeskBaseUrl()}/admin/tickets/$ticketId/priority"
        try {
            println("[DEBUG] PATCH $url")
            httpClient.patch(url) {
                setBody(jsonRequestBody(request))
            }.requireSuccess()
        } catch (e: Exception) {
            println("[ERROR] changePriority($ticketId) failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun acceptClose(ticketId: String, request: TicketCloseAcceptanceRequestDto): TicketDto {
        val url = "${supportDeskBaseUrl()}${ticketsPath()}/$ticketId/accept-close"
        return try {
            println("[DEBUG] POST $url")
            httpClient.post(url) {
                setBody(jsonRequestBody(request))
            }.requireApiData()
        } catch (e: Exception) {
            println("[ERROR] acceptClose($ticketId) failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun rateTicket(ticketId: String, request: TicketSatisfactionRequestDto): TicketDto {
        val url = "${supportDeskBaseUrl()}/client/tickets/$ticketId/satisfaction"
        return try {
            println("[DEBUG] POST $url")
            httpClient.post(url) {
                setBody(jsonRequestBody(request))
            }.requireApiData()
        } catch (e: Exception) {
            println("[ERROR] rateTicket($ticketId) failed: ${e.message} at $url")
            throw e
        }
    }

    private fun ticketsPath(): String =
        if (AdminSessionContext.currentUser()?.role == UserRole.CLIENT) "/client/tickets" else "/admin/tickets"
}
