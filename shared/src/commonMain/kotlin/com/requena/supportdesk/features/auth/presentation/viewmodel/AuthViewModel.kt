package com.requena.supportdesk.features.auth.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.common.SUPPORT_DESK_ADMIN_EMAIL
import com.requena.supportdesk.core.common.SUPPORT_DESK_CLIENT_EMAIL
import com.requena.supportdesk.core.common.SUPPORT_DESK_DEFAULT_PASSWORD
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.auth.domain.usecase.LoginUseCase
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
) : BaseViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<AuthUiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<AuthUiEffect> = _effects.asSharedFlow()

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged -> _state.update { it.copy(email = event.value, errorMessage = null) }
            is AuthUiEvent.PasswordChanged -> _state.update { it.copy(password = event.value, errorMessage = null) }
            AuthUiEvent.LoginAsAdminDemo -> {
                _state.update {
                    it.copy(email = SUPPORT_DESK_ADMIN_EMAIL, password = SUPPORT_DESK_DEFAULT_PASSWORD)
                }
                submit()
            }
            AuthUiEvent.LoginAsClientDemo -> {
                _state.update {
                    it.copy(email = SUPPORT_DESK_CLIENT_EMAIL, password = SUPPORT_DESK_DEFAULT_PASSWORD)
                }
                submit()
            }
            AuthUiEvent.Submit -> submit()
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
                    _effects.emit(AuthUiEffect.NavigateToHome(result.data.role))
                }
            }
        }
    }
}
