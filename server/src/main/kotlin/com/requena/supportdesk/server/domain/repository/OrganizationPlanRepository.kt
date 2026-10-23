package com.requena.supportdesk.server.domain.repository

import com.requena.supportdesk.server.domain.model.OrganizationSubscription
import com.requena.supportdesk.server.domain.model.PlanUsage
import com.requena.supportdesk.server.domain.model.ServerPlanRequest
import com.requena.supportdesk.server.domain.model.SubscriptionPlan

/** Company plans (V19). Rules (who may do what, limits) live in OrganizationPlanService. */
interface OrganizationPlanRepository {
    fun plans(): List<SubscriptionPlan>
    fun subscription(organizationId: String): OrganizationSubscription?

    /** Staff users that count against the plan (active non-gestoría members + pending non-gestoría invitations) and clients. */
    fun planUsage(organizationId: String): PlanUsage
    fun isPlatformOperator(organizationId: String): Boolean

    /** Sets plan/status/period and records an event, in one transaction. */
    fun changePlan(
        organizationId: String,
        planKey: String,
        status: String,
        billingPeriod: String?,
        action: String,
        actorUserId: String?,
        priceCents: Int?,
    ): OrganizationSubscription

    fun setFreeModule(organizationId: String, module: String, actorUserId: String): OrganizationSubscription

    fun createPlanRequest(
        organizationId: String,
        planKey: String,
        billingPeriod: String,
        quotedMonthlyPriceCents: Int,
        requestedBy: String,
    ): ServerPlanRequest

    /** All requests of a company, or (organizationId = null) of every company, newest first. */
    fun planRequests(organizationId: String?, status: String? = null): List<ServerPlanRequest>
    fun planRequest(requestId: String): ServerPlanRequest?

    /** Moves a REQUESTED request to [status]; returns null if it was no longer REQUESTED. */
    fun decidePlanRequest(requestId: String, status: String, decidedBy: String, note: String?): ServerPlanRequest?
}
