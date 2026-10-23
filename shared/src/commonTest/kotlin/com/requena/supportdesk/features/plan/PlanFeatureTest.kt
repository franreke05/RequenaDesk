package com.requena.supportdesk.features.plan

import com.requena.supportdesk.core.model.OnboardingGoal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanFeatureTest {
    private val catalog = listOf(
        PlanOffer("FREE", "Gratis", 0, 0, 1, 25, setOf(PlanModule.CORE), setOf(PlanModule.WORK, PlanModule.SUPPORT)),
        PlanOffer("AUTONOMO", "Autónomo", 1200, 1000, 1, null, setOf(PlanModule.CORE, PlanModule.WORK), emptySet()),
        PlanOffer("PRO", "Pro", 2900, 2400, 3, null, setOf(PlanModule.CORE, PlanModule.WORK, PlanModule.SUPPORT), emptySet()),
    )

    @Test
    fun recommendationFollowsTeamSizeThenGoals() {
        assertEquals("FREE", recommendedPlan(emptySet(), ""))
        assertEquals("AUTONOMO", recommendedPlan(setOf(OnboardingGoal.INVOICING), "Solo yo"))
        assertEquals("PRO", recommendedPlan(setOf(OnboardingGoal.SUPPORT), "Solo yo"))
        assertEquals("PRO", recommendedPlan(setOf(OnboardingGoal.WORK_TIME), "2-5"))
        assertEquals("TEAM", recommendedPlan(setOf(OnboardingGoal.SUPPORT), "6-20"))
    }

    @Test
    fun cheapestPlanWithAModuleIsTheLockHint() {
        assertEquals("Autónomo", cheapestPlanWith(PlanModule.WORK, catalog)?.name)
        assertEquals("Pro", cheapestPlanWith(PlanModule.SUPPORT, catalog.drop(1))?.name)
    }

    @Test
    fun pricesAreShownInEurosWithoutCents() {
        assertEquals("29 €", formatPlanPrice(2900))
        assertEquals("24,50 €", formatPlanPrice(2450))
        assertEquals(2400, catalog[2].priceCents(BillingPeriod.ANNUAL))
    }

    @Test
    fun planErrorsAreExplainedInSpanish() {
        assertTrue(planMessage("Module not included in your plan: WORK").contains("Trabajo y horas"))
        assertTrue(planMessage("Client limit of your plan reached (25)").contains("máximo de clientes"))
        assertTrue(planMessage("User limit of your plan reached (1)").contains("gestoría no cuenta"))
        assertEquals("Otro", planMessage("Otro"))
    }
}
