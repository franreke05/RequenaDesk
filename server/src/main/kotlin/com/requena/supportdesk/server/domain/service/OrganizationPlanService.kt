package com.requena.supportdesk.server.domain.service

import com.requena.supportdesk.server.domain.model.EffectivePlan
import com.requena.supportdesk.server.domain.model.PlanKeys
import com.requena.supportdesk.server.domain.model.PlanUsage
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.model.ServerConflictException
import com.requena.supportdesk.server.domain.model.ServerForbiddenException
import com.requena.supportdesk.server.domain.model.ServerModuleNotIncludedException
import com.requena.supportdesk.server.domain.model.ServerNotFoundException
import com.requena.supportdesk.server.domain.model.ServerPlanRequest
import com.requena.supportdesk.server.domain.model.ServerValidationException
import com.requena.supportdesk.server.domain.model.SubscriptionPlan
import com.requena.supportdesk.server.domain.model.SubscriptionStatus
import com.requena.supportdesk.server.domain.model.effectivePlanOf
import com.requena.supportdesk.server.domain.repository.OrganizationPlanRepository
import com.requena.supportdesk.server.domain.repository.OrganizationRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** What GET /org/plan returns. */
data class PlanOverview(
    val effective: EffectivePlan,
    val usage: PlanUsage,
    val catalog: List<SubscriptionPlan>,
    val pendingRequest: ServerPlanRequest?,
    val isPlatformOperator: Boolean,
)

/**
 * Company plans: what each company can use (modules, limits), plan change requests and
 * their approval by the platform operator. No money moves here: approved plans are invoiced
 * manually by OryKai (CLAUDE.md lane A) until a payment provider exists (phase 6).
 */
class OrganizationPlanService(
    private val plans: OrganizationPlanRepository,
    private val organizations: OrganizationRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val cacheWindow: Duration = Duration.ofSeconds(30),
) {
    private data class Cached(val plan: EffectivePlan, val at: Instant)

    private val cache = ConcurrentHashMap<String, Cached>()

    fun invalidate(organizationId: String) {
        cache.remove(organizationId)
    }

    fun effectivePlan(organizationId: String): EffectivePlan {
        val now = Instant.now(clock)
        cache[organizationId]?.takeIf { Duration.between(it.at, now) < cacheWindow }?.let { return it.plan }
        val goals = runCatching { organizations.organization(organizationId).onboardingGoals }.getOrDefault(emptyList())
        val plan = effectivePlanOf(plans.subscription(organizationId), plans.plans().associateBy { it.key }, goals, now)
        cache[organizationId] = Cached(plan, now)
        return plan
    }

    // --- Gates used by the staff routes ------------------------------------------------

    /** Staff routes of [module] answer 402 when the company's plan does not include it. */
    fun requireModule(identity: ServerAuthIdentity, module: String) {
        if (identity.role != "ADMIN") return // portal users keep their own (program) checks
        val organizationId = identity.organizationId ?: return
        if (!effectivePlan(organizationId).includes(module)) throw ServerModuleNotIncludedException(module)
    }

    fun ensureClientCapacity(organizationId: String) {
        val max = effectivePlan(organizationId).plan.maxClients ?: return
        if (plans.planUsage(organizationId).clients >= max) {
            throw ServerConflictException("Client limit of your plan reached ($max)")
        }
    }

    /** Gestoría (ACCOUNTANT) never counts; any other new staff user must fit the plan. */
    fun ensureUserCapacity(organizationId: String, role: String) {
        if (role == OrganizationRoles.ACCOUNTANT) return
        val max = effectivePlan(organizationId).plan.includedUsers ?: return
        if (plans.planUsage(organizationId).staffUsers >= max) {
            throw ServerConflictException("User limit of your plan reached ($max)")
        }
    }

    /**
     * Joining with an invitation: the pending invitation is already counted in the usage, so
     * the join fits when usage (members + pending) does not exceed the plan.
     */
    fun ensureUserCapacityForJoin(organizationId: String, role: String) {
        if (role == OrganizationRoles.ACCOUNTANT) return
        val max = effectivePlan(organizationId).plan.includedUsers ?: return
        if (plans.planUsage(organizationId).staffUsers > max) {
            throw ServerConflictException("User limit of your plan reached ($max)")
        }
    }

    // --- Company side --------------------------------------------------------------------

    fun overview(identity: ServerAuthIdentity): PlanOverview {
        val organizationId = staffTenant(identity)
        return PlanOverview(
            effective = effectivePlan(organizationId),
            usage = plans.planUsage(organizationId),
            catalog = plans.plans().filter { it.isPurchasable },
            pendingRequest = plans.planRequests(organizationId, "REQUESTED").firstOrNull(),
            isPlatformOperator = plans.isPlatformOperator(organizationId),
        )
    }

    /**
     * Requests a plan. Going to FREE needs no approval (it never charges) and applies now;
     * paid plans wait for the operator. The price is always the server's, and the requester
     * must explicitly accept it.
     */
    fun requestPlan(identity: ServerAuthIdentity, planKey: String, billingPeriod: String, acceptPrice: Boolean): PlanOverview {
        val organizationId = managerTenant(identity)
        val plan = plans.plans().firstOrNull { it.key == planKey.trim().uppercase() && it.isPurchasable }
            ?: throw ServerValidationException("Unknown plan")
        val period = billingPeriod.trim().uppercase().takeIf { it in setOf("MONTHLY", "ANNUAL") }
            ?: throw ServerValidationException("Billing period must be MONTHLY or ANNUAL")
        if (plan.key == PlanKeys.FREE) {
            plans.changePlan(organizationId, PlanKeys.FREE, SubscriptionStatus.ACTIVE, null, "DOWNGRADED_TO_FREE", identity.userId, 0)
            invalidate(organizationId)
            return overview(identity)
        }
        if (!acceptPrice) throw ServerValidationException("The price must be accepted to request a paid plan")
        plans.createPlanRequest(organizationId, plan.key, period, plan.priceFor(period), identity.userId)
        return overview(identity)
    }

    fun cancelRequest(identity: ServerAuthIdentity, requestId: String) {
        val organizationId = managerTenant(identity)
        val request = plans.planRequest(requestId)?.takeIf { it.organizationId == organizationId }
            ?: throw ServerNotFoundException("Plan request not found")
        plans.decidePlanRequest(request.id, "CANCELLED", identity.userId, null)
            ?: throw ServerConflictException("The plan request is no longer pending")
    }

    fun chooseFreeModule(identity: ServerAuthIdentity, module: String): PlanOverview {
        val organizationId = managerTenant(identity)
        val free = plans.plans().first { it.key == PlanKeys.FREE }
        val normalized = module.trim().uppercase()
        if (normalized !in free.selectableFreeModules) throw ServerValidationException("That module cannot be chosen for the Free plan")
        plans.setFreeModule(organizationId, normalized, identity.userId)
        invalidate(organizationId)
        return overview(identity)
    }

    // --- Platform operator (OryKai) ------------------------------------------------------

    fun operatorRequests(identity: ServerAuthIdentity, status: String?): List<ServerPlanRequest> {
        requireOperator(identity)
        return plans.planRequests(organizationId = null, status = status?.trim()?.uppercase()?.takeIf(String::isNotBlank))
    }

    fun approve(identity: ServerAuthIdentity, requestId: String, note: String?): ServerPlanRequest {
        requireOperator(identity)
        val decided = plans.decidePlanRequest(requestId, "APPROVED", identity.userId, note?.take(500))
            ?: throw ServerNotFoundException("Plan request not found or no longer pending")
        plans.changePlan(
            organizationId = decided.organizationId,
            planKey = decided.planKey,
            status = SubscriptionStatus.ACTIVE,
            billingPeriod = decided.billingPeriod,
            action = "APPROVED",
            actorUserId = identity.userId,
            priceCents = decided.quotedMonthlyPriceCents,
        )
        invalidate(decided.organizationId)
        return decided
    }

    fun reject(identity: ServerAuthIdentity, requestId: String, note: String?): ServerPlanRequest {
        requireOperator(identity)
        return plans.decidePlanRequest(requestId, "REJECTED", identity.userId, note?.take(500))
            ?: throw ServerNotFoundException("Plan request not found or no longer pending")
    }

    // --- Rules ---------------------------------------------------------------------------

    private fun staffTenant(identity: ServerAuthIdentity): String {
        if (identity.role != "ADMIN") throw ServerForbiddenException("Only company staff can do this")
        return identity.organizationId ?: throw ServerForbiddenException("No company is linked to this user")
    }

    private fun managerTenant(identity: ServerAuthIdentity): String {
        val organizationId = staffTenant(identity)
        if (!OrganizationRoles.canManage(identity.organizationRole)) {
            throw ServerForbiddenException("Only owners and admins can manage the company")
        }
        return organizationId
    }

    private fun requireOperator(identity: ServerAuthIdentity) {
        val organizationId = managerTenant(identity)
        if (!plans.isPlatformOperator(organizationId)) throw ServerForbiddenException("Only the platform operator can do this")
    }
}
