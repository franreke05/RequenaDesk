package com.requena.supportdesk.features.organization

import com.requena.supportdesk.core.model.LegalForm
import com.requena.supportdesk.core.model.OnboardingGoal
import com.requena.supportdesk.core.model.OrganizationRole
import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.model.canManageCompany
import com.requena.supportdesk.core.model.isReadOnly
import com.requena.supportdesk.features.auth.data.dto.AuthSessionDto
import com.requena.supportdesk.features.auth.data.mapper.AuthMapper
import com.requena.supportdesk.features.auth.presentation.state.AuthMode
import com.requena.supportdesk.features.auth.presentation.state.AuthUiState
import com.requena.supportdesk.features.auth.presentation.viewmodel.AuthViewModel
import com.requena.supportdesk.features.onboarding.OnboardingStep
import com.requena.supportdesk.features.onboarding.OnboardingUiState
import com.requena.supportdesk.features.onboarding.OnboardingViewModel
import com.requena.supportdesk.features.onboarding.RecommendedModule
import com.requena.supportdesk.features.onboarding.recommendedModules
import com.requena.supportdesk.features.organization.data.OrganizationUpdate
import com.requena.supportdesk.features.organization.data.organizationMessage
import com.requena.supportdesk.features.organization.presentation.OrganizationViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrganizationClientRulesTest {

    private fun session(role: String, orgRole: String? = null) = AuthSessionDto(
        userId = "u", name = "N", email = "n@x.dev", role = role,
        accessToken = "a", refreshToken = "r",
        organizationId = "org-1", organizationRole = orgRole, organizationName = "Empresa",
    )

    @Test
    fun mapperKeepsCompanyAndRejectsUnknownRoles() {
        val staff = AuthMapper.fromDto(session("ADMIN", "ACCOUNTANT"))
        assertEquals("org-1", staff.organizationId)
        assertEquals(OrganizationRole.ACCOUNTANT, staff.organizationRole)
        assertTrue(staff.isReadOnly)
        assertFalse(staff.canManageCompany)

        val portal = AuthMapper.fromDto(session("CLIENT"))
        assertEquals(UserRole.CLIENT, portal.role)
        assertNull(portal.organizationRole)

        // Before: any unknown role silently became CLIENT.
        assertFailsWith<IllegalArgumentException> { AuthMapper.fromDto(session("SUPERUSER")) }
    }

    @Test
    fun onlyOwnersAndAdminsManageTheCompany() {
        fun staff(role: OrganizationRole) = User("u", "N", "n@x.dev", UserRole.ADMIN, organizationRole = role)
        assertTrue(staff(OrganizationRole.OWNER).canManageCompany)
        assertTrue(staff(OrganizationRole.ADMIN).canManageCompany)
        assertFalse(staff(OrganizationRole.MEMBER).canManageCompany)
        assertFalse(staff(OrganizationRole.ACCOUNTANT).canManageCompany)
    }

    @Test
    fun accessFormValidatesEachMode() {
        val login = AuthUiState(email = "fran@orykai.dev", password = "x")
        assertNull(AuthViewModel.validationProblem(login))

        val signup = AuthUiState(
            mode = AuthMode.SIGNUP, name = "Lucía", organizationName = "Estudio",
            email = "lucia@x.dev", password = "PasswordSegura123", acceptTerms = true, acceptPrivacy = true,
        )
        assertNull(AuthViewModel.validationProblem(signup))
        assertNotNull(AuthViewModel.validationProblem(signup.copy(organizationName = " ")))
        assertNotNull(AuthViewModel.validationProblem(signup.copy(password = "corta")))
        assertNotNull(AuthViewModel.validationProblem(signup.copy(acceptPrivacy = false)))

        val join = signup.copy(mode = AuthMode.JOIN, organizationName = "", inviteCode = "")
        assertEquals("Introduce el codigo de invitacion.", AuthViewModel.validationProblem(join))
        assertNull(AuthViewModel.validationProblem(join.copy(inviteCode = "INV-AAAA-BBBB-CCCC")))
    }

    @Test
    fun goalsRecommendModulesInAStableOrder() {
        assertEquals(listOf(RecommendedModule.WORK), recommendedModules(emptySet()))
        assertEquals(
            listOf(RecommendedModule.SALES, RecommendedModule.INVOICING, RecommendedModule.SUPPORT, RecommendedModule.PORTAL),
            recommendedModules(setOf(OnboardingGoal.SUPPORT, OnboardingGoal.INVOICING)),
        )
    }

    @Test
    fun onboardingStepsValidateAndSaveOnlyTheirOwnFields() {
        val business = OnboardingUiState(step = OnboardingStep.BUSINESS, isLoading = false)
        assertTrue("legalForm" in OnboardingViewModel.stepErrors(business))
        val withForm = business.copy(legalForm = LegalForm.AUTONOMO, sector = " Diseño ", hasAccountant = true)
        assertTrue(OnboardingViewModel.stepErrors(withForm).isEmpty())
        assertEquals(
            OrganizationUpdate(legalForm = "AUTONOMO", sector = "Diseño", teamSize = "", hasAccountant = true),
            OnboardingViewModel.stepUpdate(withForm),
        )

        val fiscal = OnboardingUiState(step = OnboardingStep.FISCAL, taxId = "123", country = "España")
        val errors = OnboardingViewModel.stepErrors(fiscal)
        assertTrue("taxId" in errors && "country" in errors)
        // An empty tax id is fine: the step is optional.
        assertTrue(OnboardingViewModel.stepErrors(fiscal.copy(taxId = "", country = "es")).isEmpty())

        val style = OnboardingUiState(step = OnboardingStep.STYLE)
        assertEquals(OrganizationUpdate(), OnboardingViewModel.stepUpdate(style))
        assertFalse(OnboardingStep.STYLE.skippable)
    }

    @Test
    fun companyFormChecksNameTaxIdAndCountry() {
        assertNull(OrganizationViewModel.companyProblem(OrganizationUpdate(name = "OryKai", country = "ES")))
        assertNotNull(OrganizationViewModel.companyProblem(OrganizationUpdate(name = "  ")))
        assertNotNull(OrganizationViewModel.companyProblem(OrganizationUpdate(taxId = "12")))
        assertNotNull(OrganizationViewModel.companyProblem(OrganizationUpdate(country = "ESP")))
    }

    @Test
    fun serverErrorsAreShownInSpanish() {
        assertEquals(
            "Tu rol de gestoría es de solo lectura: no puedes hacer cambios.",
            organizationMessage("Your company role is read-only"),
        )
        assertEquals(
            "La empresa necesita al menos un dueño activo.",
            organizationMessage("A company always needs at least one active owner"),
        )
        assertEquals("Mensaje libre", organizationMessage("Mensaje libre"))
    }
}
