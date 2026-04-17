package com.requena.supportdesk.server.utils

import com.requena.supportdesk.server.domain.model.ServerAttachmentSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientSnapshot
import com.requena.supportdesk.server.domain.model.ServerDailyMinutesSnapshot
import com.requena.supportdesk.server.domain.model.ServerDashboardSnapshot
import com.requena.supportdesk.server.domain.model.ServerDeviceRegistration
import com.requena.supportdesk.server.domain.model.ServerSession
import com.requena.supportdesk.server.domain.model.ServerTaskLabelSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskSnapshot
import com.requena.supportdesk.server.domain.model.ServerTicketSnapshot
import com.requena.supportdesk.server.domain.model.ServerTimeLogSnapshot
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@PublishedApi
internal val responseJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

suspend fun ApplicationCall.respondJson(
    status: HttpStatusCode = HttpStatusCode.OK,
    body: JsonElement,
) {
    respondText(
        text = responseJson.encodeToString(JsonElement.serializer(), body),
        contentType = ContentType.Application.Json,
        status = status,
    )
}

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
    put("userId", session.userId)
    put("name", session.name)
    put("email", session.email)
    put("role", session.role)
    put("clientId", session.clientId)
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
    put("contactName", client.contactName)
    put("email", client.email)
    put("accountStatus", client.accountStatus)
    put("serviceTier", client.serviceTier)
    put("preferredContactChannel", client.preferredContactChannel)
    put("activeTicketCount", client.activeTicketCount)
    put("openTasksCount", client.openTasksCount)
    put("monthlyLoggedMinutes", client.monthlyLoggedMinutes)
}

fun dashboardJson(dashboard: ServerDashboardSnapshot) = buildJsonObject {
    put("openTickets", dashboard.openTickets)
    put("pendingClientTickets", dashboard.pendingClientTickets)
    put("resolvedToday", dashboard.resolvedToday)
    put("activeClients", dashboard.activeClients)
    put("monthLabel", dashboard.monthLabel)
    put("totalMinutes", dashboard.totalMinutes)
    put("billableMinutes", dashboard.billableMinutes)
    put("selectedClientId", dashboard.selectedClientId)
    put("selectedClientMinutes", dashboard.selectedClientMinutes)
    put("selectedClientBillableMinutes", dashboard.selectedClientBillableMinutes)
    put("dailyMinutes", dailyMinutesJson(dashboard.dailyMinutes))
    put("availableTasks", tasksJson(dashboard.availableTasks))
}

fun labelsJson(labels: List<ServerTaskLabelSnapshot>) = buildJsonArray {
    labels.forEach { add(labelJson(it)) }
}

fun labelJson(label: ServerTaskLabelSnapshot) = buildJsonObject {
    put("id", label.id)
    put("name", label.name)
    put("colorHex", label.colorHex)
    put("tasksCount", label.tasksCount)
}

fun tasksJson(tasks: List<ServerTaskSnapshot>) = buildJsonArray {
    tasks.forEach { add(taskJson(it)) }
}

fun taskJson(task: ServerTaskSnapshot) = buildJsonObject {
    put("id", task.id)
    put("title", task.title)
    put("description", task.description)
    put("clientId", task.clientId)
    put("clientName", task.clientName)
    put("labelId", task.labelId)
    put("labelName", task.labelName)
    put("labelColorHex", task.labelColorHex)
    put("dueDate", task.dueDate)
    put("completed", task.completed)
    put("loggedMinutes", task.loggedMinutes)
    put("loggedSeconds", task.loggedSeconds)
    put("createdAt", task.createdAt)
    put("updatedAt", task.updatedAt)
}

fun timeLogsJson(logs: List<ServerTimeLogSnapshot>) = buildJsonArray {
    logs.forEach { add(timeLogJson(it)) }
}

fun timeLogJson(log: ServerTimeLogSnapshot) = buildJsonObject {
    put("id", log.id)
    put("taskId", log.taskId)
    put("clientId", log.clientId)
    put("authorId", log.authorId)
    put("authorName", log.authorName)
    put("minutes", log.minutes)
    put("seconds", log.seconds)
    put("workDate", log.workDate)
    put("note", log.note)
    put("billable", log.billable)
    put("createdAt", log.createdAt)
}

fun dailyMinutesJson(entries: List<ServerDailyMinutesSnapshot>) = buildJsonArray {
    entries.forEach { entry ->
        add(
            buildJsonObject {
                put("workDate", entry.workDate)
                put("minutes", entry.minutes)
            },
        )
    }
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
