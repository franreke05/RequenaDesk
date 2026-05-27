package com.requena.supportdesk.features.tickets.data.datasource

import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.network.AdminSessionContext
import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.NetworkLogger
import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.requireSuccess
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.tickets.data.dto.AddInternalCommentRequest
import com.requena.supportdesk.features.tickets.data.dto.AddTimeEntryRequest
import com.requena.supportdesk.features.tickets.data.dto.ChangeAssigneeRequest
import com.requena.supportdesk.features.tickets.data.dto.CreateTicketRequestDto
import com.requena.supportdesk.features.tickets.data.dto.TicketDto
import com.requena.supportdesk.features.tickets.data.dto.TicketCloseAcceptanceRequestDto
import com.requena.supportdesk.features.tickets.data.dto.TicketSatisfactionRequestDto
import com.requena.supportdesk.features.tickets.data.dto.TicketTimeEntryDto
import com.requena.supportdesk.features.tickets.data.dto.UpdateTicketPriorityRequestDto
import com.requena.supportdesk.features.tickets.data.dto.UpdateTicketStatusRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

interface TicketsDataSource {
    suspend fun getTickets(): List<TicketDto>
    suspend fun getTicket(id: String): TicketDto?
    suspend fun createTicket(request: CreateTicketRequestDto): TicketDto
    suspend fun changeStatus(ticketId: String, request: UpdateTicketStatusRequestDto)
    suspend fun changePriority(ticketId: String, request: UpdateTicketPriorityRequestDto)
    suspend fun acceptClose(ticketId: String, request: TicketCloseAcceptanceRequestDto): TicketDto
    suspend fun rateTicket(ticketId: String, request: TicketSatisfactionRequestDto): TicketDto
    suspend fun deleteTicket(ticketId: String)
    suspend fun addTimeEntry(ticketId: String, request: AddTimeEntryRequest): TicketTimeEntryDto
    suspend fun getTimeEntries(ticketId: String): List<TicketTimeEntryDto>
    suspend fun addInternalComment(ticketId: String, request: AddInternalCommentRequest)
    suspend fun changeAssignee(ticketId: String, request: ChangeAssigneeRequest): TicketDto
}

class RemoteTicketsDataSource(
    private val httpClient: HttpClient,
) : TicketsDataSource {
    override suspend fun getTickets(): List<TicketDto> {
        val url = "${supportDeskBaseUrl()}${ticketsPath()}"
        return try {
            NetworkLogger.addLog("[DEBUG] GET $url")
            httpClient.get(url).requireApiData()
        } catch (e: Exception) {
            NetworkLogger.addLog("[ERROR] getTickets failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun getTicket(id: String): TicketDto? {
        val url = "${supportDeskBaseUrl()}${ticketsPath()}/$id"
        return try {
            NetworkLogger.addLog("[DEBUG] GET $url")
            httpClient.get(url).requireApiData()
        } catch (e: Exception) {
            NetworkLogger.addLog("[ERROR] getTicket($id) failed: ${e.message} at $url")
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
            NetworkLogger.addLog("[ERROR] createTicket failed: ${e.message} at $url")
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
            NetworkLogger.addLog("[ERROR] changeStatus($ticketId) failed: ${e.message} at $url")
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
            NetworkLogger.addLog("[ERROR] changePriority($ticketId) failed: ${e.message} at $url")
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
            NetworkLogger.addLog("[ERROR] acceptClose($ticketId) failed: ${e.message} at $url")
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
            NetworkLogger.addLog("[ERROR] rateTicket($ticketId) failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun deleteTicket(ticketId: String) {
        val url = "${supportDeskBaseUrl()}/admin/tickets/$ticketId"
        try {
            println("[DEBUG] DELETE $url")
            httpClient.delete(url).requireSuccess()
        } catch (e: Exception) {
            NetworkLogger.addLog("[ERROR] deleteTicket($ticketId) failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun addTimeEntry(ticketId: String, request: AddTimeEntryRequest): TicketTimeEntryDto {
        val url = "${supportDeskBaseUrl()}/admin/tickets/$ticketId/time-entries"
        return try {
            NetworkLogger.addLog("[DEBUG] POST $url")
            httpClient.post(url) {
                setBody(jsonRequestBody(request))
            }.requireApiData()
        } catch (e: Exception) {
            NetworkLogger.addLog("[ERROR] addTimeEntry($ticketId) failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun getTimeEntries(ticketId: String): List<TicketTimeEntryDto> {
        val url = "${supportDeskBaseUrl()}/admin/tickets/$ticketId/time-entries"
        return try {
            NetworkLogger.addLog("[DEBUG] GET $url")
            httpClient.get(url).requireApiData()
        } catch (e: Exception) {
            NetworkLogger.addLog("[ERROR] getTimeEntries($ticketId) failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun addInternalComment(ticketId: String, request: AddInternalCommentRequest) {
        val url = "${supportDeskBaseUrl()}/admin/tickets/$ticketId/internal-comment"
        try {
            NetworkLogger.addLog("[DEBUG] POST $url")
            httpClient.post(url) {
                setBody(jsonRequestBody(request))
            }.requireSuccess()
        } catch (e: Exception) {
            NetworkLogger.addLog("[ERROR] addInternalComment($ticketId) failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun changeAssignee(ticketId: String, request: ChangeAssigneeRequest): TicketDto {
        val url = "${supportDeskBaseUrl()}/admin/tickets/$ticketId/assignee"
        return try {
            NetworkLogger.addLog("[DEBUG] PATCH $url")
            httpClient.patch(url) {
                setBody(jsonRequestBody(request))
            }.requireApiData()
        } catch (e: Exception) {
            NetworkLogger.addLog("[ERROR] changeAssignee($ticketId) failed: ${e.message} at $url")
            throw e
        }
    }

    private fun ticketsPath(): String =
        if (AdminSessionContext.currentUser()?.role == UserRole.CLIENT) "/client/tickets" else "/admin/tickets"
}
