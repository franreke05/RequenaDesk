package com.requena.supportdesk.features.auth.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.network.AdminSessionContext
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.auth.domain.usecase.ClaimClientAccessUseCase
import com.requena.supportdesk.features.auth.domain.usecase.ClearSessionUseCase
import com.requena.supportdesk.features.auth.domain.usecase.LoginUseCase
import com.requena.supportdesk.features.auth.domain.usecase.RestoreSessionUseCase
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
    private val claimClientAccessUseCase: ClaimClientAccessUseCase,
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val clearSessionUseCase: ClearSessionUseCase,
) : BaseViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<AuthUiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<AuthUiEffect> = _effects.asSharedFlow()

    init {
        restoreSession()
        launch {
            AdminSessionContext.sessionExpiredFlow.collect {
                onEvent(AuthUiEvent.Logout)
            }
        }
    }

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged -> _state.update { it.copy(email = event.value, errorMessage = null) }
            is AuthUiEvent.PasswordChanged -> _state.update { it.copy(password = event.value, errorMessage = null) }
            is AuthUiEvent.ClientAccessCodeChanged -> _state.update { it.copy(clientAccessCode = event.value, errorMessage = null) }
            is AuthUiEvent.DisplayNameChanged -> _state.update { it.copy(displayName = event.value, errorMessage = null) }
            AuthUiEvent.Logout -> {
                clearSessionUseCase()
                _state.update {
                    it.copy(
                        authenticatedUser = null,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }
            AuthUiEvent.Submit -> submit()
            AuthUiEvent.ClaimClientAccess -> claimClientAccess()
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
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = loginUseCase(state.value.email, state.value.password)) {
                is AppResult.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                    _effects.emit(AuthUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> {
                    _state.update { it.copy(isLoading = false, authenticatedUser = result.data, errorMessage = null) }
                    _effects.emit(AuthUiEffect.NavigateToHome)
                }
            }
        }
    }

    private fun claimClientAccess() {
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val current = state.value
            when (
                val result = claimClientAccessUseCase(
                    code = current.clientAccessCode,
                    name = current.displayName,
                    email = current.email,
                    password = current.password,
                )
            ) {
                is AppResult.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                    _effects.emit(AuthUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> {
                    _state.update { it.copy(isLoading = false, authenticatedUser = result.data, errorMessage = null) }
                    _effects.emit(AuthUiEffect.NavigateToHome)
                }
            }
        }
    }
}
