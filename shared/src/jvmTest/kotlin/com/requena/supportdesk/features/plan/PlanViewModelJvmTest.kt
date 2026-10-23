package com.requena.supportdesk.features.plan

import com.requena.supportdesk.core.result.AppResult
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanViewModelJvmTest {
    private class FakePlans(var operator: Boolean) : PlanRepository {
        val requested = mutableListOf<Pair<String, BillingPeriod>>()
        val approved = mutableListOf<String>()
        private val offer = PlanOffer("PRO", "Pro", 2900, 2400, 3, null, setOf(PlanModule.CORE, PlanModule.SUPPORT), emptySet())

        private fun snapshot(pending: PlanRequest? = null): PlanOverview = PlanOverview(
            plan = CompanyPlan("PRO", "Pro", "TRIAL", offer.modules, 3, null, 14, null, null),
            staffUsers = 1, clients = 0, catalog = listOf(offer), pendingRequest = pending, isPlatformOperator = operator,
        )

        override suspend fun overview(): AppResult<PlanOverview> = AppResult.Success(snapshot())
        override suspend fun request(planKey: String, period: BillingPeriod): AppResult<PlanOverview> {
            requested += planKey to period
            return AppResult.Success(snapshot(PlanRequest("r1", "Empresa", planKey, period, 2400, "REQUESTED", "Ana", "2026-11-01")))
        }
        override suspend fun cancel(requestId: String) = AppResult.Success(Unit)
        override suspend fun chooseFreeModule(module: PlanModule): AppResult<PlanOverview> = AppResult.Success(snapshot())
        override suspend fun operatorQueue() = AppResult.Success(listOf(PlanRequest("r9", "Otra", "TEAM", BillingPeriod.MONTHLY, 4900, "REQUESTED", "Bea", "2026-11-01")))
        override suspend fun approve(requestId: String, note: String?): AppResult<Unit> { approved += requestId; return AppResult.Success(Unit) }
        override suspend fun reject(requestId: String, note: String?) = AppResult.Error("Only the platform operator can do this")
    }

    private fun <T> await(block: suspend () -> T): T = runBlocking { withTimeout(2_000) { block() } }

    /** Subscribes to the effects first, then fires the event (effects are not replayed). */
    private fun PlanViewModel.messageAfter(event: PlanUiEvent): String = runBlocking {
        withTimeout(2_000) {
            val next = async(start = CoroutineStart.UNDISPATCHED) { effects.first() }
            onEvent(event)
            (next.await() as PlanUiEffect.ShowMessage).message
        }
    }

    @Test
    fun requestsUseTheChosenPeriodAndExplainThereIsNoAutomaticCharge() {
        val repository = FakePlans(operator = false)
        val viewModel = PlanViewModel(repository)
        viewModel.onEvent(PlanUiEvent.Load)
        await { viewModel.state.first { it.overview != null } }
        viewModel.onEvent(PlanUiEvent.PeriodChanged(BillingPeriod.ANNUAL))
        val message = viewModel.messageAfter(PlanUiEvent.Request("PRO"))
        assertEquals(listOf("PRO" to BillingPeriod.ANNUAL), repository.requested)
        assertTrue(message.contains("no se cobra nada"))
        assertEquals("r1", viewModel.state.value.overview?.pendingRequest?.id)
        viewModel.clear()
    }

    @Test
    fun theOperatorSeesItsQueueAndErrorsAreReported() {
        val repository = FakePlans(operator = true)
        val viewModel = PlanViewModel(repository)
        viewModel.onEvent(PlanUiEvent.Load)
        assertEquals(listOf("r9"), await { viewModel.state.first { it.operatorQueue.isNotEmpty() } }.operatorQueue.map { it.id })

        assertTrue(viewModel.messageAfter(PlanUiEvent.Approve("r9", null)).contains("factura"))
        assertEquals(listOf("r9"), repository.approved)

        assertEquals("Only the platform operator can do this", viewModel.messageAfter(PlanUiEvent.Reject("r9", null)))
        viewModel.clear()
    }
}
