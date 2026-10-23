package com.requena.supportdesk.features.organization.presentation

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.model.Organization
import com.requena.supportdesk.core.model.OrganizationInvitation
import com.requena.supportdesk.core.model.OrganizationMember
import com.requena.supportdesk.core.model.OrganizationRole
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.clients.domain.model.clientTaxIdProblem
import com.requena.supportdesk.features.organization.data.OrganizationRepository
import com.requena.supportdesk.features.organization.data.OrganizationUpdate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex

data class OrganizationUiState(
    val organization: Organization? = null,
    val members: List<OrganizationMember> = emptyList(),
    val invitations: List<OrganizationInvitation> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    /** Plain code of the invitation just created; shown once so it can be copied. */
    val newInvitationCode: String? = null,
    val errorMessage: String? = null,
)

sealed interface OrganizationUiEvent {
    /** Loads the company and, when [includeTeam] (owner/admin), members and invitations. */
    data class Load(val includeTeam: Boolean) : OrganizationUiEvent
    data class SaveCompany(val update: OrganizationUpdate) : OrganizationUiEvent
    data class Invite(val role: OrganizationRole) : OrganizationUiEvent
    data class Revoke(val invitationId: String) : OrganizationUiEvent
    data class ChangeRole(val userId: String, val role: OrganizationRole) : OrganizationUiEvent
    data class Deactivate(val userId: String) : OrganizationUiEvent
    object DismissInvitationCode : OrganizationUiEvent
}

sealed interface OrganizationUiEffect {
    data class ShowMessage(val message: String) : OrganizationUiEffect
}

class OrganizationViewModel(
    private val repository: OrganizationRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(OrganizationUiState())
    val state: StateFlow<OrganizationUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<OrganizationUiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<OrganizationUiEffect> = _effects.asSharedFlow()

    private val writeMutex = Mutex()

    fun onEvent(event: OrganizationUiEvent) {
        when (event) {
            is OrganizationUiEvent.Load -> load(event.includeTeam)
            is OrganizationUiEvent.SaveCompany -> saveCompany(event.update)
            is OrganizationUiEvent.Invite -> write("Invitación creada. Copia el código: solo se muestra ahora.") {
                when (val result = repository.invite(event.role)) {
                    is AppResult.Success -> {
                        _state.update { it.copy(newInvitationCode = result.data.code) }
                        refreshTeam()
                        AppResult.Success(Unit)
                    }
                    is AppResult.Error -> result
                }
            }
            is OrganizationUiEvent.Revoke -> write("Invitación revocada.") {
                repository.revoke(event.invitationId).also { refreshTeam() }
            }
            is OrganizationUiEvent.ChangeRole -> write("Rol actualizado.") {
                repository.changeRole(event.userId, event.role).also { refreshTeam() }
            }
            is OrganizationUiEvent.Deactivate -> write("Acceso desactivado.") {
                repository.deactivate(event.userId).also { refreshTeam() }
            }
            OrganizationUiEvent.DismissInvitationCode -> _state.update { it.copy(newInvitationCode = null) }
        }
    }

    private fun load(includeTeam: Boolean) {
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val organization = repository.organization()) {
                is AppResult.Success -> _state.update { it.copy(organization = organization.data) }
                is AppResult.Error -> _state.update { it.copy(errorMessage = organization.message) }
            }
            if (includeTeam) refreshTeam()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun refreshTeam() {
        (repository.members() as? AppResult.Success)?.let { members -> _state.update { it.copy(members = members.data) } }
        (repository.invitations() as? AppResult.Success)?.let { invitations ->
            _state.update { it.copy(invitations = invitations.data) }
        }
    }

    private fun saveCompany(update: OrganizationUpdate) {
        val problem = companyProblem(update)
        if (problem != null) {
            _effects.tryEmit(OrganizationUiEffect.ShowMessage(problem))
            return
        }
        write("Datos de la empresa guardados.") {
            when (val result = repository.update(update)) {
                is AppResult.Success -> {
                    _state.update { it.copy(organization = result.data) }
                    AppResult.Success(Unit)
                }
                is AppResult.Error -> result
            }
        }
    }

    private fun write(successMessage: String, block: suspend () -> AppResult<*>) {
        guardedLaunch(writeMutex) {
            _state.update { it.copy(isSaving = true) }
            val message = when (val result = block()) {
                is AppResult.Success -> successMessage
                is AppResult.Error -> result.message
            }
            _state.update { it.copy(isSaving = false) }
            _effects.emit(OrganizationUiEffect.ShowMessage(message))
        }
    }

    companion object {
        /** Client-side check before saving company data (the server validates again). */
        fun companyProblem(update: OrganizationUpdate): String? = when {
            update.name != null && update.name.isBlank() -> "El nombre de la empresa no puede quedar vacío."
            update.taxId != null -> clientTaxIdProblem(update.taxId)
            else -> null
        } ?: update.country?.trim()?.takeIf { it.isNotEmpty() && !Regex("^[A-Za-z]{2}$").matches(it) }
            ?.let { "El país debe ser un código de 2 letras (por ejemplo ES)." }
    }
}
