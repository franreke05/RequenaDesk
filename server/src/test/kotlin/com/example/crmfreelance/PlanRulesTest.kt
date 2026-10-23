package com.example.crmfreelance

import com.requena.supportdesk.server.domain.model.OrganizationSubscription
import com.requena.supportdesk.server.domain.model.PlanKeys
import com.requena.supportdesk.server.domain.model.PlanModules
import com.requena.supportdesk.server.domain.model.SubscriptionPlan
import com.requena.supportdesk.server.domain.model.SubscriptionStatus
import com.requena.supportdesk.server.domain.model.effectivePlanOf
import com.requena.supportdesk.server.plugins.moduleForPath
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pure plan rules: trial expiry, Free module, missing subscription, gated paths. */
class PlanRulesTest {
    private val all = setOf(PlanModules.CORE, PlanModules.WORK, PlanModules.SUPPORT, PlanModules.SALES)
    private val plans = listOf(
        SubscriptionPlan(PlanKeys.FREE, "Gratis", 0, 0, 1, 25, setOf(PlanModules.CORE),
            setOf(PlanModules.WORK, PlanModules.SUPPORT, PlanModules.SALES, PlanModules.AGENDA), true, 10),
        SubscriptionPlan(PlanKeys.PRO, "Pro", 2900, 2400, 3, null, all, emptySet(), true, 30),
        SubscriptionPlan(PlanKeys.BETA, "Beta", 0, 0, null, null, all, emptySet(), false, 90),
    ).associateBy { it.key }
    private val now = Instant.parse("2026-11-01T10:00:00Z")

    @Test
    fun runningTrialGivesTheFullPlanAndCountsDaysUp() {
        val trial = OrganizationSubscription("org", PlanKeys.PRO, SubscriptionStatus.TRIAL, trialEndsAt = now.plus(13, ChronoUnit.DAYS).plusSeconds(5))
        val plan = effectivePlanOf(trial, plans, emptyList(), now)
        assertEquals(PlanKeys.PRO, plan.plan.key)
        assertEquals(SubscriptionStatus.TRIAL, plan.status)
        assertEquals(14, plan.trialDaysLeft)
        assertTrue(plan.includes(PlanModules.SUPPORT))
    }

    @Test
    fun expiredTrialBecomesFreeWithTheModuleOfTheFirstGoalNeverACharge() {
        val expired = OrganizationSubscription("org", PlanKeys.PRO, SubscriptionStatus.TRIAL, trialEndsAt = now.minusSeconds(1))
        val plan = effectivePlanOf(expired, plans, listOf("SUPPORT", "WORK_TIME"), now)
        assertEquals(PlanKeys.FREE, plan.plan.key)
        assertEquals(SubscriptionStatus.TRIAL_ENDED, plan.status)
        assertEquals(setOf(PlanModules.CORE, PlanModules.SUPPORT), plan.modules)
        assertFalse(plan.includes(PlanModules.WORK))
        assertEquals(0, plan.plan.monthlyPriceCents)
    }

    @Test
    fun freeUsesTheChosenModuleAndFallsBackToWork() {
        val chosen = OrganizationSubscription("org", PlanKeys.FREE, SubscriptionStatus.ACTIVE, freeModule = PlanModules.SALES)
        assertEquals(setOf(PlanModules.CORE, PlanModules.SALES), effectivePlanOf(chosen, plans, emptyList(), now).modules)
        // A module that cannot be chosen for Free is ignored.
        val invalid = chosen.copy(freeModule = PlanModules.REPORTS)
        assertEquals(setOf(PlanModules.CORE, PlanModules.WORK), effectivePlanOf(invalid, plans, emptyList(), now).modules)
    }

    @Test
    fun betaHasEverythingAndAMissingSubscriptionFailsClosedToFree() {
        val beta = OrganizationSubscription("org", PlanKeys.BETA, SubscriptionStatus.BETA)
        assertEquals(all, effectivePlanOf(beta, plans, emptyList(), now).modules)
        val missing = effectivePlanOf(null, plans, emptyList(), now)
        assertEquals(PlanKeys.FREE, missing.plan.key)
        assertEquals(setOf(PlanModules.CORE, PlanModules.WORK), missing.modules)
    }

    @Test
    fun onlyTheStaffWorkAndSupportPathsAreGated() {
        assertEquals(PlanModules.WORK, moduleForPath("/admin/tasks"))
        assertEquals(PlanModules.WORK, moduleForPath("/admin/tasks/task-1"))
        assertEquals(PlanModules.WORK, moduleForPath("/admin/task-board/placements/t"))
        assertEquals(PlanModules.SUPPORT, moduleForPath("/tickets/ticket-1/messages"))
        assertNull(moduleForPath("/admin/clients"))
        assertNull(moduleForPath("/admin/tasksx"))
        assertNull(moduleForPath("/org/plan"))
    }
}
