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
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun Application.configureSupportDeskModule(
    repositoryOverride: SupportDeskRepository? = null,
) {
    configureSerialization()
    configureMonitoring()

    val serverEnvironment = ServerEnvironment.load()
    val tokenService = SupportDeskTokenService(serverEnvironment.auth)
    val runtime = repositoryOverride
        ?.let { SupportDeskRuntime(repository = it) }
        ?: supportDeskRuntime(serverEnvironment)
    monitor.subscribe(ApplicationStopped) {
        runtime.close()
    }
    val service = SupportDeskService(
        repository = runtime.repository,
        tokenService = tokenService,
    )

    routing {
        get("/") {
            call.respondJson(
                body = successResponse(
                    path = "/",
                    data = buildJsonObject {
                        put("service", "orykai-software-server")
                        put("status", "running")
                    },
                ),
            )
        }
        get("/health/live") {
            call.respondJson(
                body = successResponse(
                    path = "/health/live",
                    data = buildJsonObject { put("status", "alive") },
                ),
            )
        }
        get("/health/ready") {
            if (runtime.isReady()) {
                call.respondJson(
                    body = successResponse(
                        path = "/health/ready",
                        data = buildJsonObject { put("database", "ready") },
                    ),
                )
            } else {
                call.respondJson(
                    status = HttpStatusCode.ServiceUnavailable,
                    body = errorResponse("Database is not ready"),
                )
            }
        }
        authRoutes(service)
        ticketRoutes(service, tokenService)
        attachmentRoutes(service, tokenService)
        clientRoutes(service, tokenService)
        labelRoutes(service, tokenService)
        taskRoutes(service, tokenService)
        timeLogRoutes(service, tokenService)
        dashboardRoutes(service, tokenService)
        deviceRoutes(service, tokenService)
    }
}

private data class SupportDeskRuntime(
    val repository: SupportDeskRepository,
    val readinessCheck: () -> Boolean = { true },
    val closeAction: () -> Unit = {},
) {
    fun isReady(): Boolean = runCatching(readinessCheck).getOrDefault(false)

    fun close() = closeAction()
}

private fun Application.supportDeskRuntime(environment: ServerEnvironment): SupportDeskRuntime {
    val database = environment.database
    requireNotNull(database) {
        "Database configuration is required. Set SUPABASE_DATABASE_URL or DATABASE_URL, or configure database host, user and password."
    }
    val dataSource = PostgresSupportDeskDataSource(database)
    try {
        val migrationsExecuted = dataSource.migrate()
        log.info("Database migration completed; {} migration(s) applied", migrationsExecuted)
        if (environment.bootstrapDemoData) {
            PostgresDemoBootstrapper(dataSource).bootstrap(
                adminPassword = requireNotNull(environment.bootstrapAdminPassword),
                clientPassword = requireNotNull(environment.bootstrapClientPassword),
            )
            log.info("Demo users and records synchronized")
        }
        check(dataSource.isReady()) {
            "Database schema is incomplete after migration."
        }
    } catch (error: Throwable) {
        dataSource.close()
        throw error
    }
    return SupportDeskRuntime(
        repository = PostgresSupportDeskRepository(dataSource = dataSource),
        readinessCheck = dataSource::isReady,
        closeAction = dataSource::close,
    )
}
