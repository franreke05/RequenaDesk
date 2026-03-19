package com.requena.supportdesk.server.application

import com.requena.supportdesk.server.data.datasource.InMemorySupportDeskDataSource
import com.requena.supportdesk.server.data.repository.InMemorySupportDeskRepository
import com.requena.supportdesk.server.domain.service.SupportDeskPlaceholderService
import com.requena.supportdesk.server.plugins.configureMonitoring
import com.requena.supportdesk.server.plugins.configureSerialization
import com.requena.supportdesk.server.routes.attachmentRoutes
import com.requena.supportdesk.server.routes.authRoutes
import com.requena.supportdesk.server.routes.clientRoutes
import com.requena.supportdesk.server.routes.dashboardRoutes
import com.requena.supportdesk.server.routes.deviceRoutes
import com.requena.supportdesk.server.routes.ticketRoutes
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun Application.configureSupportDeskModule() {
    configureSerialization()
    configureMonitoring()

    val service = SupportDeskPlaceholderService(
        repository = InMemorySupportDeskRepository(InMemorySupportDeskDataSource()),
    )

    routing {
        get("/") {
            call.respond(
                successResponse(
                    path = "/",
                    data = buildJsonObject {
                        put("service", "requenadesk-server")
                        put("status", "running")
                    },
                ),
            )
        }
        authRoutes(service)
        ticketRoutes(service)
        attachmentRoutes(service)
        clientRoutes(service)
        dashboardRoutes(service)
        deviceRoutes(service)
    }
}
