package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.attachmentJson
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.requireAdminIdentity
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.attachmentRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/attachments") {
        get("/{id}") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@get
            val id = call.parameters["id"] ?: return@get call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing attachment id"))
            val attachment = service.attachment(id, ownerAdminId = identity.userId)
                ?: return@get call.respondJson(HttpStatusCode.NotFound, errorResponse("Attachment not found"))
            call.respondJson(body = successResponse("/attachments/$id", attachmentJson(attachment)))
        }
    }
}
