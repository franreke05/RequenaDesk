package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.EffectivePlan
import com.requena.supportdesk.server.domain.model.ServerPlanRequest
import com.requena.supportdesk.server.domain.model.SubscriptionPlan
import com.requena.supportdesk.server.domain.service.OrganizationPlanService
import com.requena.supportdesk.server.domain.service.PlanOverview
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.messageJson
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class PlanRequestBody(val planKey: String = "", val billingPeriod: String = "MONTHLY", val acceptPrice: Boolean = false)

@Serializable
data class FreeModuleBody(val module: String = "")

@Serializable
data class PlanDecisionBody(val note: String? = null)

/** Company plan (/org/plan) and the platform operator's approval queue (/platform). */
fun Route.planRoutes(plans: OrganizationPlanService, tokenService: SupportDeskTokenService) {
    route("/org/plan") {
        get {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@get
            call.respondJson(body = successResponse("/org/plan", overviewJson(plans.overview(identity))))
        }
        post("/requests") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@post
            val body = call.receiveOrDefault(PlanRequestBody())
            val overview = plans.requestPlan(identity, body.planKey, body.billingPeriod, body.acceptPrice)
            call.respondJson(HttpStatusCode.Created, successResponse("/org/plan/requests", overviewJson(overview)))
        }
        delete("/requests/{requestId}") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@delete
            plans.cancelRequest(identity, call.parameters["requestId"].orEmpty())
            call.respondJson(body = successResponse("/org/plan/requests", messageJson("Plan request cancelled.")))
        }
        patch("/free-module") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@patch
            val body = call.receiveOrDefault(FreeModuleBody())
            call.respondJson(body = successResponse("/org/plan/free-module", overviewJson(plans.chooseFreeModule(identity, body.module))))
        }
    }

    route("/platform/plan-requests") {
        get {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@get
            val requests = plans.operatorRequests(identity, call.request.queryParameters["status"])
            call.respondJson(body = successResponse("/platform/plan-requests", buildJsonArray { requests.forEach { add(planRequestJson(it)) } }))
        }
        post("/{requestId}/approve") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@post
            val body = call.receiveOrDefault(PlanDecisionBody())
            val decided = plans.approve(identity, call.parameters["requestId"].orEmpty(), body.note)
            call.respondJson(
                body = successResponse(
                    "/platform/plan-requests",
                    buildJsonObject {
                        put("request", planRequestJson(decided))
                        // No money moved: OryKai invoices this plan manually (lane A).
                        put("invoiceManually", true)
                    },
                ),
            )
        }
        post("/{requestId}/reject") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@post
            val body = call.receiveOrDefault(PlanDecisionBody())
            val decided = plans.reject(identity, call.parameters["requestId"].orEmpty(), body.note)
            call.respondJson(body = successResponse("/platform/plan-requests", planRequestJson(decided)))
        }
    }
}

private fun overviewJson(overview: PlanOverview) = buildJsonObject {
    put("plan", effectiveJson(overview.effective))
    put(
        "usage",
        buildJsonObject {
            put("staffUsers", overview.usage.staffUsers)
            put("clients", overview.usage.clients)
        },
    )
    put("catalog", buildJsonArray { overview.catalog.forEach { add(planJson(it)) } })
    put("pendingRequest", overview.pendingRequest?.let(::planRequestJson) ?: kotlinx.serialization.json.JsonNull)
    put("isPlatformOperator", overview.isPlatformOperator)
}

private fun effectiveJson(plan: EffectivePlan) = buildJsonObject {
    put("planKey", plan.plan.key)
    put("planName", plan.plan.name)
    put("status", plan.status)
    put("modules", JsonArray(plan.modules.sorted().map(::JsonPrimitive)))
    put("includedUsers", plan.plan.includedUsers)
    put("maxClients", plan.plan.maxClients)
    put("trialEndsAt", plan.trialEndsAt?.toString())
    put("trialDaysLeft", plan.trialDaysLeft)
    put("freeModule", plan.freeModule)
    put("billingPeriod", plan.billingPeriod)
    put("betaEndsOn", plan.betaEndsOn?.toString())
}

private fun planJson(plan: SubscriptionPlan) = buildJsonObject {
    put("key", plan.key)
    put("name", plan.name)
    put("monthlyPriceCents", plan.monthlyPriceCents)
    put("annualMonthlyPriceCents", plan.annualMonthlyPriceCents)
    put("includedUsers", plan.includedUsers)
    put("maxClients", plan.maxClients)
    put("modules", JsonArray(plan.modules.sorted().map(::JsonPrimitive)))
    put("selectableFreeModules", JsonArray(plan.selectableFreeModules.sorted().map(::JsonPrimitive)))
}

private fun planRequestJson(request: ServerPlanRequest) = buildJsonObject {
    put("id", request.id)
    put("organizationId", request.organizationId)
    put("organizationName", request.organizationName)
    put("planKey", request.planKey)
    put("billingPeriod", request.billingPeriod)
    put("quotedMonthlyPriceCents", request.quotedMonthlyPriceCents)
    put("status", request.status)
    put("requestedByName", request.requestedByName)
    put("createdAt", request.createdAt)
    put("decidedAt", request.decidedAt)
    put("note", request.note)
}
