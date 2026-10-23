package com.requena.supportdesk.app.admin.screens

import com.requena.supportdesk.core.navigation.AppDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirstStepsTest {
    @Test
    fun newCompanyHasEverythingPendingAndEachStepLeadsSomewhere() {
        val steps = firstSteps(hasClients = false, hasLoggedTime = false, hasTeamOrInvitations = false, hasFiscalData = false)
        assertEquals(4, steps.size)
        assertTrue(steps.none { it.done })
        assertEquals(
            listOf(AppDestination.Clients, AppDestination.Tasks, AppDestination.Settings, AppDestination.Settings),
            steps.map { it.destination },
        )
    }

    @Test
    fun stepsFollowTheCompanyData() {
        val steps = firstSteps(hasClients = true, hasLoggedTime = false, hasTeamOrInvitations = true, hasFiscalData = true)
        assertEquals(listOf(true, false, true, true), steps.map { it.done })
    }
}
