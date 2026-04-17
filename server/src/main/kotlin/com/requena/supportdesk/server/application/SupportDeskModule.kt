package com.requena.supportdesk.server.application

import com.requena.supportdesk.server.data.datasource.PostgresDemoBootstrapper
import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import com.requena.supportdesk.server.data.repository.PostgresSupportDeskRepository
import com.requena.supportdesk.server.config.ServerEnvironment
import com.requena.supportdesk.server.domain.repository.SupportDeskRepository
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.plugins.configureMonitoring
import com.requena.supportdesk.server.plugins.configureSerialization
import com.requena.supportdesk.server.routes.attachmentRoutes
import com.requena.supportdesk.server.routes.authRoutes
import com.requena.supportdesk.server.routes.clientRoutes
import com.requena.supportdesk.server.routes.dashboardRoutes
import com.requena.supportdesk.server.routes.deviceRoutes
import com.requena.supportdesk.server.routes.labelRoutes
import com.requena.supportdesk.server.routes.taskRoutes
import com.requena.supportdesk.server.routes.ticketRoutes
import com.requena.supportdesk.server.routes.timeLogRoutes
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun Application.configureSupportDeskModule(
    repositoryOverride: SupportDeskRepository? = null,
) {
    configureSerialization()
    configureMonitoring()

    val environment = ServerEnvironment.load()
    val service = SupportDeskService(repository = repositoryOverride ?: supportDeskRepository(environment))

    routing {
        get("/") {
            call.respondJson(
                body = successResponse(
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
        labelRoutes(service)
        taskRoutes(service)
        timeLogRoutes(service)
        dashboardRoutes(service)
        deviceRoutes(service)
    }
}

private fun supportDeskRepository(environment: ServerEnvironment): SupportDeskRepository {
    val database = environment.database
    requireNotNull(database) {
        "Database configuration is required. Set SUPABASE_DB_HOST, SUPABASE_DB_PORT, SUPABASE_DB_NAME, SUPABASE_DB_USER and SUPABASE_DB_PASSWORD."
    }
    val dataSource = PostgresSupportDeskDataSource(database)
    if (environment.bootstrapDemoData) {
        PostgresDemoBootstrapper(dataSource).bootstrap(
            adminPassword = environment.bootstrapAdminPassword,
            clientPassword = environment.bootstrapClientPassword,
        )
    }
    return PostgresSupportDeskRepository(dataSource = dataSource)
}
