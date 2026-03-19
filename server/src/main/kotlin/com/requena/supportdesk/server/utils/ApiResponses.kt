package com.requena.supportdesk.server.utils

import com.requena.supportdesk.server.domain.model.ServerAttachmentSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientSnapshot
import com.requena.supportdesk.server.domain.model.ServerDashboardSnapshot
import com.requena.supportdesk.server.domain.model.ServerDeviceRegistration
import com.requena.supportdesk.server.domain.model.ServerSession
import com.requena.supportdesk.server.domain.model.ServerTicketSnapshot
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun successResponse(path: String, data: JsonElement) = buildJsonObject {
    put("status", "ok")
    put("path", path)
    put("data", data)
}

fun errorResponse(message: String) = buildJsonObject {
    put("status", "error")
    put("message", message)
}

fun messageJson(message: String) = buildJsonObject {
    put("message", message)
}

fun sessionJson(session: ServerSession) = buildJsonObject {
    put("role", session.role)
    put("accessToken", session.accessToken)
    put("refreshToken", session.refreshToken)
}

fun ticketsJson(tickets: List<ServerTicketSnapshot>) = buildJsonArray {
    tickets.forEach { add(ticketJson(it)) }
}

fun ticketJson(ticket: ServerTicketSnapshot) = buildJsonObject {
    put("id", ticket.id)
    put("ticketNumber", ticket.ticketNumber)
    put("subject", ticket.subject)
    put("description", ticket.description)
    put("category", ticket.category)
    put("affectedApp", ticket.affectedApp)
    put("platform", ticket.platform)
    put("appVersion", ticket.appVersion)
    put("clientReference", ticket.clientReference)
    put("status", ticket.status)
    put("priority", ticket.priority)
    put("waitingOn", ticket.waitingOn)
    put("resolutionSummary", ticket.resolutionSummary)
}

fun clientsJson(clients: List<ServerClientSnapshot>) = buildJsonArray {
    clients.forEach { add(clientJson(it)) }
}

fun clientJson(client: ServerClientSnapshot) = buildJsonObject {
    put("id", client.id)
    put("companyName", client.companyName)
    put("productName", client.productName)
    put("email", client.email)
    put("accountStatus", client.accountStatus)
    put("serviceTier", client.serviceTier)
    put("preferredContactChannel", client.preferredContactChannel)
    put("activeTicketCount", client.activeTicketCount)
}

fun dashboardJson(dashboard: ServerDashboardSnapshot) = buildJsonObject {
    put("openTickets", dashboard.openTickets)
    put("pendingClientTickets", dashboard.pendingClientTickets)
    put("resolvedToday", dashboard.resolvedToday)
    put("activeClients", dashboard.activeClients)
}

fun attachmentJson(attachment: ServerAttachmentSnapshot) = buildJsonObject {
    put("id", attachment.id)
    put("fileName", attachment.fileName)
    put("contentType", attachment.contentType)
}

fun deviceJson(device: ServerDeviceRegistration) = buildJsonObject {
    put("id", device.id)
    put("userId", device.userId)
    put("platform", device.platform)
}
