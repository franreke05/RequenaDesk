package com.requena.supportdesk.features.auth.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.auth.domain.usecase.ClearSessionUseCase
import com.requena.supportdesk.features.auth.domain.usecase.LoginUseCase
import com.requena.supportdesk.features.auth.domain.usecase.RestoreSessionUseCase
import com.requena.supportdesk.features.auth.domain.model.LoginCredentials
import com.requena.supportdesk.features.auth.presentation.effect.AuthUiEffect
import com.requena.supportdesk.features.auth.presentation.event.AuthUiEvent
import com.requena.supportdesk.features.auth.presentation.state.AuthUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val clearSessionUseCase: ClearSessionUseCase,
) : BaseViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<AuthUiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<AuthUiEffect> = _effects.asSharedFlow()

    init {
        restoreSession()
    }

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged -> _state.update { it.copy(email = event.value, errorMessage = null) }
            is AuthUiEvent.PasswordChanged -> _state.update { it.copy(password = event.value, errorMessage = null) }
            AuthUiEvent.Logout -> {
                clearSessionUseCase()
                _state.update {
                    it.copy(
                        email = "",
                        password = "",
                        authenticatedUser = null,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }
            AuthUiEvent.Submit -> submit()
        }
    }

    private fun restoreSession() {
        val restoredUser = restoreSessionUseCase() ?: return
        _state.update {
            it.copy(
                authenticatedUser = restoredUser,
                isLoading = false,
                errorMessage = null,
            )
        }
    }

    private fun submit() {
        val credentials = validatedCredentials() ?: return
        launch {
            when (val result = loginUseCase(credentials.email, credentials.password)) {
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
                        )
                    }
                    _effects.emit(AuthUiEffect.NavigateToHome)
                }
            }
        }
    }

    private fun validatedCredentials(): LoginCredentials? {
        val current = state.value
        if (current.isLoading) return null

        val email = current.email.trim()
        val password = current.password
        val errorMessage = when {
            email.isBlank() && password.isBlank() -> "Introduce correo y contrasena."
            email.isBlank() -> "Introduce el correo."
            !email.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) -> "Introduce un correo valido."
            password.isBlank() -> "Introduce la contrasena."
            else -> null
        }

        if (errorMessage != null) {
            _state.update { it.copy(email = email, isLoading = false, errorMessage = errorMessage) }
            _effects.tryEmit(AuthUiEffect.ShowMessage(errorMessage))
            return null
        }

        _state.update { it.copy(email = email, isLoading = true, errorMessage = null) }
        return LoginCredentials(email = email, password = password)
    }
}
