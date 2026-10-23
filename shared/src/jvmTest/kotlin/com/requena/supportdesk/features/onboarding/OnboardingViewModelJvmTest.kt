package com.requena.supportdesk.features.onboarding

import com.requena.supportdesk.core.model.CreatedInvitation
import com.requena.supportdesk.core.model.LegalForm
import com.requena.supportdesk.core.model.OnboardingGoal
import com.requena.supportdesk.core.model.Organization
import com.requena.supportdesk.core.model.OrganizationInvitation
import com.requena.supportdesk.core.model.OrganizationMember
import com.requena.supportdesk.core.model.OrganizationRole
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.organization.data.OrganizationRepository
import com.requena.supportdesk.features.organization.data.OrganizationUpdate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingViewModelJvmTest {

    private class RecordingRepository(var organization: Organization) : OrganizationRepository {
        val updates = mutableListOf<OrganizationUpdate>()

        override suspend fun organization() = AppResult.Success(organization)
        override suspend fun update(update: OrganizationUpdate): AppResult<Organization> {
            updates += update
            organization = organization.copy(onboardingCompleted = organization.onboardingCompleted || update.onboardingCompleted == true)
            return AppResult.Success(organization)
        }
        override suspend fun members() = AppResult.Success(emptyList<OrganizationMember>())
        override suspend fun changeRole(userId: String, role: OrganizationRole): AppResult<OrganizationMember> = error("unused")
        override suspend fun deactivate(userId: String) = AppResult.Success(Unit)
        override suspend fun invitations() = AppResult.Success(emptyList<OrganizationInvitation>())
        override suspend fun invite(role: OrganizationRole): AppResult<CreatedInvitation> = error("unused")
        override suspend fun revoke(invitationId: String) = AppResult.Success(Unit)
    }

    private fun OnboardingViewModel.awaitState(predicate: (OnboardingUiState) -> Boolean) = runBlocking {
        withTimeout(2_000) { state.first(predicate) }
    }

    @Test
    fun walkingThroughStepsSavesEachOneAndCompletesAtTheEnd() {
        val repository = RecordingRepository(Organization(id = "org", name = "Estudio Lucía"))
        val viewModel = OnboardingViewModel(repository)
        viewModel.onEvent(OnboardingUiEvent.Load)
        viewModel.awaitState { !it.isLoading }
        // The company name prefills the legal name.
        assertEquals("Estudio Lucía", viewModel.state.value.legalName)

        // Step 2 needs a legal form before it can continue.
        viewModel.onEvent(OnboardingUiEvent.Next)
        assertTrue("legalForm" in viewModel.awaitState { it.fieldErrors.isNotEmpty() }.fieldErrors)

        viewModel.onEvent(OnboardingUiEvent.LegalFormChanged(LegalForm.SOCIEDAD))
        viewModel.onEvent(OnboardingUiEvent.Next)
        viewModel.awaitState { it.step == OnboardingStep.GOALS && !it.isSaving }

        viewModel.onEvent(OnboardingUiEvent.GoalToggled(OnboardingGoal.SUPPORT))
        viewModel.onEvent(OnboardingUiEvent.Next)
        viewModel.awaitState { it.step == OnboardingStep.FISCAL && !it.isSaving }

        // The fiscal step can be skipped without sending anything.
        viewModel.onEvent(OnboardingUiEvent.Skip)
        viewModel.awaitState { it.step == OnboardingStep.STYLE && !it.isSaving }

        viewModel.onEvent(OnboardingUiEvent.Next)
        val finished = viewModel.awaitState { it.completed }
        assertEquals(OnboardingStep.STYLE, finished.step)

        assertEquals(
            listOf(
                OrganizationUpdate(legalForm = "SOCIEDAD", sector = "", teamSize = "", hasAccountant = null),
                OrganizationUpdate(onboardingGoals = listOf("SUPPORT")),
                OrganizationUpdate(onboardingCompleted = true),
            ),
            repository.updates,
        )
        viewModel.clear()
    }
}
