package com.requena.supportdesk.server.plugins

import com.requena.supportdesk.server.domain.model.PlanModules
import com.requena.supportdesk.server.domain.service.OrganizationPlanService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.request.path

/**
 * Staff routes that belong to a plan module. One table instead of a check in every handler:
 * a company whose plan lacks the module gets 402 on these paths (its data is kept). Portal
 * users (role CLIENT) are not gated here; their business programs keep their own checks.
 */
val MODULE_ROUTE_PREFIXES: List<Pair<String, String>> = listOf(
    "/admin/tasks" to PlanModules.WORK,
    "/admin/task-board" to PlanModules.WORK,
    "/admin/labels" to PlanModules.WORK,
    "/admin/time-logs" to PlanModules.WORK,
    "/admin/work-sessions" to PlanModules.WORK,
    "/tickets" to PlanModules.SUPPORT,
)

/** Module of a request path, or null when the path is not gated (pure, for tests). */
fun moduleForPath(path: String): String? =
    MODULE_ROUTE_PREFIXES.firstOrNull { (prefix, _) -> path == prefix || path.startsWith("$prefix/") }?.second

fun Application.configureModuleGate(tokenService: SupportDeskTokenService, plans: OrganizationPlanService) {
    install(
        createApplicationPlugin("ModuleGate") {
            onCall { call ->
                val module = moduleForPath(call.request.path()) ?: return@onCall
                val token = call.request.headers[HttpHeaders.Authorization]
                    ?.removePrefix("Bearer ")?.removePrefix("bearer ")?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: return@onCall // no token: the route itself answers 401
                val identity = tokenService.verifyAccessToken(token) ?: return@onCall
                plans.requireModule(identity, module) // throws -> 402 via StatusPages
            }
        },
    )
}
