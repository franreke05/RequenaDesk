package com.requena.supportdesk.server.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Module keys a plan can include (see V19 and docs/product/SAAS_FASE_3_PLANES_PLAN.md). */
object PlanModules {
    const val CORE = "CORE"
    const val WORK = "WORK"
    const val SUPPORT = "SUPPORT"
    const val SALES = "SALES"
    const val BILLING = "BILLING"
    const val AGENDA = "AGENDA"
    const val DOCUMENTS = "DOCUMENTS"
    const val PORTAL = "PORTAL"
    const val REPORTS = "REPORTS"
}

object PlanKeys {
    const val FREE = "FREE"
    const val AUTONOMO = "AUTONOMO"
    const val PRO = "PRO"
    const val TEAM = "TEAM"
    const val BETA = "BETA"
}

object SubscriptionStatus {
    const val TRIAL = "TRIAL"
    const val ACTIVE = "ACTIVE"
    const val BETA = "BETA"

    /** Not stored: what an expired TRIAL is shown as. */
    const val TRIAL_ENDED = "TRIAL_ENDED"
}

const val TRIAL_DAYS = 14L

data class SubscriptionPlan(
    val key: String,
    val name: String,
    val monthlyPriceCents: Int,
    val annualMonthlyPriceCents: Int,
    /** Staff users included (gestoría not counted); null = unlimited. */
    val includedUsers: Int?,
    /** Null = unlimited. */
    val maxClients: Int?,
    val modules: Set<String>,
    val selectableFreeModules: Set<String> = emptySet(),
    val isPurchasable: Boolean,
    val sortOrder: Int,
) {
    fun priceFor(billingPeriod: String): Int = if (billingPeriod == "ANNUAL") annualMonthlyPriceCents else monthlyPriceCents
}

data class OrganizationSubscription(
    val organizationId: String,
    val planKey: String,
    val status: String,
    val billingPeriod: String? = null,
    val trialEndsAt: Instant? = null,
    val freeModule: String? = null,
    val betaEndsOn: LocalDate? = null,
    val founderDiscountPercent: Int = 0,
)

data class PlanUsage(
    val staffUsers: Int,
    val clients: Int,
)

/** What a company can use right now. Computed on read; never stored. */
data class EffectivePlan(
    val plan: SubscriptionPlan,
    val status: String,
    val modules: Set<String>,
    val trialEndsAt: Instant? = null,
    val trialDaysLeft: Int? = null,
    val freeModule: String? = null,
    val billingPeriod: String? = null,
    val betaEndsOn: LocalDate? = null,
) {
    fun includes(module: String): Boolean = module in modules
}

/**
 * Pure rule (no I/O): turns the stored subscription into what the company can use now.
 * An expired trial behaves as FREE; FREE gets the chosen module or one derived from the
 * onboarding goals. A missing subscription is treated as FREE (fail closed, no charge).
 */
fun effectivePlanOf(
    subscription: OrganizationSubscription?,
    plans: Map<String, SubscriptionPlan>,
    onboardingGoals: List<String>,
    now: Instant,
): EffectivePlan {
    val free = requireNotNull(plans[PlanKeys.FREE]) { "FREE plan missing" }
    fun freePlan(status: String, chosen: String?): EffectivePlan {
        val module = chosen?.takeIf { it in free.selectableFreeModules } ?: defaultFreeModule(onboardingGoals, free)
        return EffectivePlan(
            plan = free,
            status = status,
            modules = free.modules + module,
            freeModule = module,
            trialEndsAt = subscription?.trialEndsAt,
        )
    }
    if (subscription == null) return freePlan(SubscriptionStatus.ACTIVE, null)
    val plan = plans[subscription.planKey] ?: return freePlan(SubscriptionStatus.ACTIVE, subscription.freeModule)
    return when (subscription.status) {
        SubscriptionStatus.TRIAL -> {
            val endsAt = subscription.trialEndsAt
            if (endsAt == null || !now.isBefore(endsAt)) {
                freePlan(SubscriptionStatus.TRIAL_ENDED, subscription.freeModule)
            } else {
                val secondsLeft = Duration.between(now, endsAt).seconds
                EffectivePlan(
                    plan = plan,
                    status = SubscriptionStatus.TRIAL,
                    modules = plan.modules,
                    trialEndsAt = endsAt,
                    trialDaysLeft = ((secondsLeft + 86_399) / 86_400).toInt(),
                )
            }
        }
        else -> if (plan.key == PlanKeys.FREE) {
            freePlan(subscription.status, subscription.freeModule)
        } else {
            EffectivePlan(
                plan = plan,
                status = subscription.status,
                modules = plan.modules,
                billingPeriod = subscription.billingPeriod,
                betaEndsOn = subscription.betaEndsOn,
            )
        }
    }
}

/** FREE module suggested by the onboarding answers (the first one that maps), WORK otherwise. */
fun defaultFreeModule(onboardingGoals: List<String>, free: SubscriptionPlan): String {
    val byGoal = mapOf(
        "WORK_TIME" to PlanModules.WORK,
        "SUPPORT" to PlanModules.SUPPORT,
        "INVOICING" to PlanModules.SALES,
        "BOOKINGS" to PlanModules.AGENDA,
    )
    return onboardingGoals.firstNotNullOfOrNull { goal -> byGoal[goal]?.takeIf { it in free.selectableFreeModules } }
        ?: PlanModules.WORK
}

data class ServerPlanRequest(
    val id: String,
    val organizationId: String,
    val organizationName: String,
    val planKey: String,
    val billingPeriod: String,
    val quotedMonthlyPriceCents: Int,
    val status: String,
    val requestedByName: String,
    val createdAt: String,
    val decidedAt: String? = null,
    val note: String? = null,
)
