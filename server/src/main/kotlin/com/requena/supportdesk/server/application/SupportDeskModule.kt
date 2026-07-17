package com.requena.supportdesk.server.application

import com.requena.supportdesk.server.business.finance.BusinessFinanceAccessGuard
import com.requena.supportdesk.server.business.finance.BusinessFinanceClientIdentity
import com.requena.supportdesk.server.business.finance.BusinessFinanceConnectionProvider
import com.requena.supportdesk.server.business.finance.BusinessFinanceService
import com.requena.supportdesk.server.business.finance.InMemoryBusinessFinanceStore
import com.requena.supportdesk.server.business.finance.PostgresBusinessFinanceStore
import com.requena.supportdesk.server.business.finance.businessFinanceRoutes
import com.requena.supportdesk.server.business.operations.BusinessOperationsService
import com.requena.supportdesk.server.business.operations.FailClosedDocumentContentScanner
import com.requena.supportdesk.server.business.operations.FailClosedPrivateDocumentStorage
import com.requena.supportdesk.server.business.operations.InMemoryBusinessOperationsRepository
import com.requena.supportdesk.server.business.operations.OperationsEntitlementGuard
import com.requena.supportdesk.server.business.operations.OperationsPrincipal
import com.requena.supportdesk.server.business.operations.OperationsRole
import com.requena.supportdesk.server.business.operations.PostgresBusinessOperationsRepository
import com.requena.supportdesk.server.business.operations.businessOperationsRoutes
import com.requena.supportdesk.server.business.sales.InMemorySalesProgramStore
import com.requena.supportdesk.server.business.sales.PostgresSalesProgramStore
import com.requena.supportdesk.server.business.sales.SalesProgramAccessGuard
import com.requena.supportdesk.server.business.sales.SalesProgramService
import com.requena.supportdesk.server.business.sales.salesProgramRoutes
import com.requena.supportdesk.server.data.datasource.PostgresDemoBootstrapper
import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import com.requena.supportdesk.server.data.repository.PostgresSupportDeskRepository
import com.requena.supportdesk.server.config.ServerEnvironment
import com.requena.supportdesk.server.domain.repository.SupportDeskRepository
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.plugins.configureMonitoring
import com.requena.supportdesk.server.plugins.configureRequestSecurity
import com.requena.supportdesk.server.plugins.configureSerialization
import com.requena.supportdesk.server.routes.attachmentRoutes
import com.requena.supportdesk.server.routes.authRoutes
import com.requena.supportdesk.server.routes.clientRoutes
import com.requena.supportdesk.server.routes.clientCrmRoutes
import com.requena.supportdesk.server.routes.clientPortalRoutes
import com.requena.supportdesk.server.routes.programRoutes
import com.requena.supportdesk.server.routes.dashboardRoutes
import com.requena.supportdesk.server.routes.deviceRoutes
import com.requena.supportdesk.server.routes.labelRoutes
import com.requena.supportdesk.server.routes.taskRoutes
import com.requena.supportdesk.server.routes.ticketRoutes
import com.requena.supportdesk.server.routes.timeLogRoutes
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
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
    configureRequestSecurity()
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
    val financeService = BusinessFinanceService(
        store = runtime.database?.let(::postgresBusinessFinanceStore) ?: InMemoryBusinessFinanceStore(),
        accessGuard = BusinessFinanceAccessGuard { identity, productKey ->
            service.hasActiveBusinessEntitlement(identity.clientId, productKey)
        },
    )
    val operationsService = BusinessOperationsService(
        repository = runtime.database?.let(::PostgresBusinessOperationsRepository) ?: InMemoryBusinessOperationsRepository(),
        storage = FailClosedPrivateDocumentStorage(),
        scanner = FailClosedDocumentContentScanner(),
    )
    val operationsGuard = OperationsEntitlementGuard { identity, productKey ->
        val clientId = identity.clientId
        if (identity.role == "CLIENT" && !clientId.isNullOrBlank() && service.hasActiveBusinessEntitlement(clientId, productKey)) {
            OperationsPrincipal(
                clientId = clientId,
                userId = identity.userId,
                role = OperationsRole.OWNER,
                email = identity.email,
            )
        } else {
            null
        }
    }
    val salesProgramService = SalesProgramService(
        store = runtime.database?.let(::PostgresSalesProgramStore) ?: InMemorySalesProgramStore(),
        accessGuard = SalesProgramAccessGuard { identity, productKey ->
            service.hasActiveBusinessEntitlement(identity.clientId, productKey)
        },
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
        clientCrmRoutes(service, tokenService)
        clientPortalRoutes(service, tokenService)
        programRoutes(service, tokenService)
        businessFinanceRoutes(financeService) { call ->
            val identity = call.requireAuthenticatedIdentity(tokenService)
            if (identity?.role == "CLIENT" && !identity.clientId.isNullOrBlank()) {
                BusinessFinanceClientIdentity(
                    userId = identity.userId,
                    clientId = requireNotNull(identity.clientId),
                )
            } else {
                null
            }
        }
        businessOperationsRoutes(operationsService, tokenService, operationsGuard)
        salesProgramRoutes(salesProgramService, tokenService)
        labelRoutes(service, tokenService)
        taskRoutes(service, tokenService)
        timeLogRoutes(service, tokenService)
        dashboardRoutes(service, tokenService)
        deviceRoutes(service, tokenService)
    }
}

private data class SupportDeskRuntime(
    val repository: SupportDeskRepository,
    val database: PostgresSupportDeskDataSource? = null,
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
        database = dataSource,
        readinessCheck = dataSource::isReady,
        closeAction = dataSource::close,
    )
}

private fun postgresBusinessFinanceStore(dataSource: PostgresSupportDeskDataSource): PostgresBusinessFinanceStore =
    PostgresBusinessFinanceStore(
        object : BusinessFinanceConnectionProvider {
            override fun <T> withConnection(block: (java.sql.Connection) -> T): T =
                dataSource.withConnection(block)
        },
    )

private fun SupportDeskService.hasActiveBusinessEntitlement(clientId: String, productKey: String): Boolean =
    runCatching {
        clientPrograms(clientId).subscriptions.any { subscription ->
            subscription.productKey == productKey && subscription.status == "ACTIVE"
        }
    }.getOrDefault(false)
