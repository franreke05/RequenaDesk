package com.requena.supportdesk.features.onboarding

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.model.LegalForm
import com.requena.supportdesk.core.model.OnboardingGoal
import com.requena.supportdesk.core.model.Organization
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.clients.domain.model.clientTaxIdProblem
import com.requena.supportdesk.features.organization.data.OrganizationRepository
import com.requena.supportdesk.features.organization.data.OrganizationUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex

/**
 * Onboarding after signup. Step 1 (the account) is the signup itself, so this flow covers
 * steps 2–5. Every "Siguiente" saves that step on the server, so closing the app half-way
 * resumes where it was left (the server is the draft).
 */
enum class OnboardingStep(val number: Int, val title: String, val skippable: Boolean) {
    BUSINESS(2, "Tu negocio", skippable = true),
    GOALS(3, "¿Qué quieres resolver?", skippable = true),
    FISCAL(4, "Datos fiscales", skippable = true),
    STYLE(5, "Elige tu estilo", skippable = false),
    ;

    companion object {
        const val TOTAL = 5
    }
}

/** Modules the plan recommends for the chosen goals (used by phase 3's plan suggestion). */
enum class RecommendedModule(val label: String) {
    SALES("Ventas: presupuestos y catálogo"),
    INVOICING("Facturación y gastos"),
    WORK("Trabajo y horas"),
    SUPPORT("Soporte y SLA"),
    PORTAL("Portal de cliente"),
    AGENDA("Agenda y reservas"),
    DOCUMENTS("Documentos"),
}

/** Pure mapping from onboarding answers to modules; empty answers get the core work module. */
fun recommendedModules(goals: Set<OnboardingGoal>): List<RecommendedModule> {
    val modules = linkedSetOf<RecommendedModule>()
    goals.sortedBy { it.ordinal }.forEach { goal ->
        when (goal) {
            OnboardingGoal.INVOICING -> modules += listOf(RecommendedModule.SALES, RecommendedModule.INVOICING)
            OnboardingGoal.WORK_TIME -> modules += RecommendedModule.WORK
            OnboardingGoal.SUPPORT -> modules += listOf(RecommendedModule.SUPPORT, RecommendedModule.PORTAL)
            OnboardingGoal.BOOKINGS -> modules += RecommendedModule.AGENDA
            OnboardingGoal.DOCUMENTS -> modules += RecommendedModule.DOCUMENTS
        }
    }
    if (modules.isEmpty()) modules += RecommendedModule.WORK
    return modules.toList()
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.BUSINESS,
    val legalForm: LegalForm? = null,
    val sector: String = "",
    val teamSize: String = "",
    val hasAccountant: Boolean? = null,
    val goals: Set<OnboardingGoal> = emptySet(),
    val legalName: String = "",
    val taxId: String = "",
    val addressLine: String = "",
    val postalCode: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = "ES",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    /** Field problems of the current step, keyed by field name. */
    val fieldErrors: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val completed: Boolean = false,
) {
    val recommended: List<RecommendedModule> get() = recommendedModules(goals)
}

sealed interface OnboardingUiEvent {
    object Load : OnboardingUiEvent
    data class LegalFormChanged(val value: LegalForm) : OnboardingUiEvent
    data class SectorChanged(val value: String) : OnboardingUiEvent
    data class TeamSizeChanged(val value: String) : OnboardingUiEvent
    data class HasAccountantChanged(val value: Boolean) : OnboardingUiEvent
    data class GoalToggled(val goal: OnboardingGoal) : OnboardingUiEvent
    data class FiscalChanged(val field: String, val value: String) : OnboardingUiEvent
    object Next : OnboardingUiEvent
    object Back : OnboardingUiEvent
    object Skip : OnboardingUiEvent
}

class OnboardingViewModel(
    private val repository: OrganizationRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val saveMutex = Mutex()

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            OnboardingUiEvent.Load -> load()
            is OnboardingUiEvent.LegalFormChanged -> edit { copy(legalForm = event.value) }
            is OnboardingUiEvent.SectorChanged -> edit { copy(sector = event.value) }
            is OnboardingUiEvent.TeamSizeChanged -> edit { copy(teamSize = event.value) }
            is OnboardingUiEvent.HasAccountantChanged -> edit { copy(hasAccountant = event.value) }
            is OnboardingUiEvent.GoalToggled -> edit {
                copy(goals = if (event.goal in goals) goals - event.goal else goals + event.goal)
            }
            is OnboardingUiEvent.FiscalChanged -> edit { withFiscal(event.field, event.value) }
            OnboardingUiEvent.Back -> _state.update { current ->
                val previous = OnboardingStep.entries.getOrNull(current.step.ordinal - 1) ?: current.step
                current.copy(step = previous, fieldErrors = emptyMap(), errorMessage = null)
            }
            OnboardingUiEvent.Next -> advance(save = true)
            OnboardingUiEvent.Skip -> if (state.value.step.skippable) advance(save = false)
        }
    }

    private fun edit(transform: OnboardingUiState.() -> OnboardingUiState) {
        _state.update { it.transform().copy(fieldErrors = emptyMap(), errorMessage = null) }
    }

    private fun load() {
        launch {
            when (val result = repository.organization()) {
                is AppResult.Success -> _state.update { it.prefilledFrom(result.data).copy(isLoading = false) }
                is AppResult.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    private fun advance(save: Boolean) {
        val current = state.value
        val errors = if (save) stepErrors(current) else emptyMap()
        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors) }
            return
        }
        val isLast = current.step == OnboardingStep.entries.last()
        val update = (if (save) stepUpdate(current) else OrganizationUpdate())
            .let { if (isLast) it.copy(onboardingCompleted = true) else it }
        guardedLaunch(saveMutex) {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val needsRequest = update != OrganizationUpdate()
            val result = if (needsRequest) repository.update(update) else null
            if (result is AppResult.Error) {
                _state.update { it.copy(isSaving = false, errorMessage = result.message) }
                return@guardedLaunch
            }
            _state.update {
                val next = OnboardingStep.entries.getOrNull(it.step.ordinal + 1)
                it.copy(
                    isSaving = false,
                    step = next ?: it.step,
                    completed = isLast,
                )
            }
        }
    }

    companion object {
        /** Validation of the visible step only (pure, for tests). Empty map = can continue. */
        fun stepErrors(state: OnboardingUiState): Map<String, String> = when (state.step) {
            OnboardingStep.BUSINESS -> buildMap {
                if (state.legalForm == null) put("legalForm", "Elige si eres autónomo/a o sociedad.")
            }
            OnboardingStep.GOALS -> buildMap {
                if (state.goals.isEmpty()) put("goals", "Elige al menos una opción o salta este paso.")
            }
            OnboardingStep.FISCAL -> buildMap {
                clientTaxIdProblem(state.taxId)?.let { put("taxId", it) }
                if (state.country.isNotBlank() && !Regex("^[A-Za-z]{2}$").matches(state.country.trim())) {
                    put("country", "Usa un código de 2 letras, por ejemplo ES.")
                }
            }
            OnboardingStep.STYLE -> emptyMap()
        }

        /** What a step saves on the server (only that step's fields). */
        fun stepUpdate(state: OnboardingUiState): OrganizationUpdate = when (state.step) {
            OnboardingStep.BUSINESS -> OrganizationUpdate(
                legalForm = state.legalForm?.name,
                sector = state.sector.trim(),
                teamSize = state.teamSize.trim(),
                hasAccountant = state.hasAccountant,
            )
            OnboardingStep.GOALS -> OrganizationUpdate(onboardingGoals = state.goals.map { it.name })
            OnboardingStep.FISCAL -> OrganizationUpdate(
                legalName = state.legalName.trim(),
                taxId = state.taxId.trim(),
                addressLine = state.addressLine.trim(),
                postalCode = state.postalCode.trim(),
                city = state.city.trim(),
                province = state.province.trim(),
                country = state.country.trim().uppercase(),
            )
            // The style is a per-user, per-device preference (AppearanceStore), not company data.
            OnboardingStep.STYLE -> OrganizationUpdate()
        }
    }
}

private fun OnboardingUiState.prefilledFrom(organization: Organization): OnboardingUiState = copy(
    legalForm = organization.legalForm,
    sector = organization.sector,
    teamSize = organization.teamSize,
    hasAccountant = organization.hasAccountant,
    goals = organization.onboardingGoals,
    legalName = organization.legalName.ifBlank { organization.name },
    taxId = organization.taxId,
    addressLine = organization.addressLine,
    postalCode = organization.postalCode,
    city = organization.city,
    province = organization.province,
    country = organization.country.ifBlank { "ES" },
    completed = organization.onboardingCompleted,
)

private fun OnboardingUiState.withFiscal(field: String, value: String): OnboardingUiState = when (field) {
    "legalName" -> copy(legalName = value)
    "taxId" -> copy(taxId = value)
    "addressLine" -> copy(addressLine = value)
    "postalCode" -> copy(postalCode = value)
    "city" -> copy(city = value)
    "province" -> copy(province = value)
    "country" -> copy(country = value)
    else -> this
}
