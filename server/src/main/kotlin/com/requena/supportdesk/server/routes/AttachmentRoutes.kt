package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.utils.attachmentJson
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.attachmentRoutes(service: SupportDeskService) {
    route("/attachments") {
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, errorResponse("Missing attachment id"))
            val attachment = service.attachment(id) ?: return@get call.respond(HttpStatusCode.NotFound, errorResponse("Attachment not found"))
            call.respond(successResponse("/attachments/$id", attachmentJson(attachment)))
        }
    }
}
