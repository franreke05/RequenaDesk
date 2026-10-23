package com.requena.supportdesk.app.admin

import com.requena.supportdesk.core.navigation.AppDestination
import com.requena.supportdesk.features.plan.CompanyPlan
import com.requena.supportdesk.features.plan.PlanModule
import com.requena.supportdesk.features.plan.PlanOffer
import com.requena.supportdesk.features.plan.PlanOverview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LockedDestinationsTest {
    private val catalog = listOf(
        PlanOffer("AUTONOMO", "Autónomo", 1200, 1000, 1, null, setOf(PlanModule.CORE, PlanModule.WORK), emptySet()),
        PlanOffer("PRO", "Pro", 2900, 2400, 3, null, setOf(PlanModule.CORE, PlanModule.WORK, PlanModule.SUPPORT), emptySet()),
    )

    private fun overview(vararg modules: PlanModule) = PlanOverview(
        plan = CompanyPlan("FREE", "Gratis", "ACTIVE", modules.toSet(), 1, 25, null, null, null),
        staffUsers = 1,
        clients = 0,
        catalog = catalog,
        pendingRequest = null,
        isPlatformOperator = false,
    )

    @Test
    fun nothingIsLockedWhileThePlanIsUnknown() {
        assertTrue(lockedDestinations(null).isEmpty())
    }

    @Test
    fun freeWithSupportLocksTheWorkScreensWithTheCheapestPlanThatHasThem() {
        val locked = lockedDestinations(overview(PlanModule.CORE, PlanModule.SUPPORT))
        assertEquals(setOf(AppDestination.Tasks, AppDestination.Pinboard, AppDestination.Labels), locked.keys)
        assertEquals("Incluido en Autónomo", locked[AppDestination.Tasks])
    }

    @Test
    fun freeWithWorkLocksTickets() {
        val locked = lockedDestinations(overview(PlanModule.CORE, PlanModule.WORK))
        assertEquals<Map<AppDestination, String>>(mapOf(AppDestination.Tickets to "Incluido en Pro"), locked)
    }
}
