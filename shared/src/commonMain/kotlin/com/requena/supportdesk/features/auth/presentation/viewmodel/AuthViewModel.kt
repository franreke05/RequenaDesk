package com.requena.supportdesk.features.auth.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.auth.domain.repository.AuthRepository
import com.requena.supportdesk.features.auth.domain.repository.JoinForm
import com.requena.supportdesk.features.auth.domain.repository.SignupForm
import com.requena.supportdesk.features.auth.domain.usecase.ClearSessionUseCase
import com.requena.supportdesk.features.auth.domain.usecase.LoginUseCase
import com.requena.supportdesk.features.auth.domain.usecase.RestoreSessionUseCase
import com.requena.supportdesk.features.auth.presentation.effect.AuthUiEffect
import com.requena.supportdesk.features.auth.presentation.event.AuthUiEvent
import com.requena.supportdesk.features.auth.presentation.state.AuthMode
import com.requena.supportdesk.features.auth.presentation.state.AuthUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val clearSessionUseCase: ClearSessionUseCase,
    private val authRepository: AuthRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<AuthUiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<AuthUiEffect> = _effects.asSharedFlow()

    private val submitMutex = Mutex()

    init {
        restoreSession()
        launch {
            val available = authRepository.isSignupAvailable()
            _state.update { it.copy(signupAvailable = available) }
        }
    }

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged -> edit { copy(email = event.value) }
            is AuthUiEvent.PasswordChanged -> edit { copy(password = event.value) }
            is AuthUiEvent.NameChanged -> edit { copy(name = event.value) }
            is AuthUiEvent.OrganizationNameChanged -> edit { copy(organizationName = event.value) }
            is AuthUiEvent.InviteCodeChanged -> edit { copy(inviteCode = event.value.uppercase()) }
            is AuthUiEvent.AcceptTermsChanged -> edit { copy(acceptTerms = event.value) }
            is AuthUiEvent.AcceptPrivacyChanged -> edit { copy(acceptPrivacy = event.value) }
            is AuthUiEvent.ModeChanged -> edit {
                // Signup is only offered when the server accepts it.
                val mode = if (event.mode == AuthMode.SIGNUP && !signupAvailable) AuthMode.LOGIN else event.mode
                copy(mode = mode, password = "", acceptTerms = false, acceptPrivacy = false)
            }
            is AuthUiEvent.SessionUserChanged -> _state.update {
                if (it.authenticatedUser == null) it else it.copy(authenticatedUser = event.user)
            }
            AuthUiEvent.Logout -> {
                clearSessionUseCase()
                _state.update { AuthUiState(signupAvailable = it.signupAvailable) }
            }
            AuthUiEvent.Submit -> submit()
        }
    }

    private fun edit(transform: AuthUiState.() -> AuthUiState) {
        _state.update { it.transform().copy(errorMessage = null) }
    }

    private fun restoreSession() {
        val restoredUser = restoreSessionUseCase() ?: return
        _state.update { it.copy(authenticatedUser = restoredUser, isLoading = false, errorMessage = null) }
        // Sessions saved before companies existed carry no company: refresh once to get it.
        if (restoredUser.organizationId == null) {
            launch {
                val refreshed = authRepository.refreshSession()
                _state.update { it.copy(authenticatedUser = refreshed) }
            }
        }
    }

    private fun submit() {
        val current = state.value
        val problem = validationProblem(current)
        if (problem != null) {
            _state.update { it.copy(errorMessage = problem) }
            _effects.tryEmit(AuthUiEffect.ShowMessage(problem))
            return
        }
        guardedLaunch(submitMutex) {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = when (current.mode) {
                AuthMode.LOGIN -> loginUseCase(current.email.trim(), current.password)
                AuthMode.SIGNUP -> authRepository.signup(
                    SignupForm(
                        name = current.name,
                        email = current.email,
                        password = current.password,
                        organizationName = current.organizationName,
                        acceptTerms = current.acceptTerms,
                        acceptPrivacy = current.acceptPrivacy,
                    ),
                )
                AuthMode.JOIN -> authRepository.join(
                    JoinForm(
                        code = current.inviteCode,
                        name = current.name,
                        email = current.email,
                        password = current.password,
                        acceptTerms = current.acceptTerms,
                        acceptPrivacy = current.acceptPrivacy,
                    ),
                )
            }
            handleResult(result)
        }
    }

    private suspend fun handleResult(result: AppResult<User>) {
        when (result) {
            is AppResult.Error -> {
                _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                _effects.emit(AuthUiEffect.ShowMessage(result.message))
            }
            is AppResult.Success -> {
                _state.update {
                    it.copy(
                        password = "",
                        isLoading = false,
                        authenticatedUser = result.data,
                        errorMessage = null,
                        acceptTerms = false,
                        acceptPrivacy = false,
                    )
                }
                _effects.emit(AuthUiEffect.NavigateToHome)
            }
        }
    }

    companion object {
        private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        const val MINIMUM_PASSWORD_LENGTH = 12

        /** First problem with the visible form, or null when it can be sent. Pure, for tests. */
        fun validationProblem(state: AuthUiState): String? {
            val email = state.email.trim()
            val creatingAccount = state.mode != AuthMode.LOGIN
            return when {
                state.isLoading -> "Espera a que termine la operacion en curso."
                state.mode == AuthMode.JOIN && state.inviteCode.isBlank() -> "Introduce el codigo de invitacion."
                creatingAccount && state.name.isBlank() -> "Introduce tu nombre."
                state.mode == AuthMode.SIGNUP && state.organizationName.isBlank() -> "Introduce el nombre de tu empresa."
                email.isBlank() && state.password.isBlank() -> "Introduce correo y contrasena."
                email.isBlank() -> "Introduce el correo."
                !email.matches(EMAIL_PATTERN) -> "Introduce un correo valido."
                state.password.isBlank() -> "Introduce la contrasena."
                creatingAccount && state.password.length < MINIMUM_PASSWORD_LENGTH ->
                    "La contrasena debe tener al menos $MINIMUM_PASSWORD_LENGTH caracteres."
                creatingAccount && (!state.acceptTerms || !state.acceptPrivacy) ->
                    "Tienes que aceptar los terminos y la politica de privacidad."
                else -> null
            }
        }
    }
}
