package com.requena.supportdesk.features.notifications.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.common.SupportDeskSeed
import com.requena.supportdesk.core.network.AdminSessionContext
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.notifications.domain.usecase.RegisterDeviceUseCase
import com.requena.supportdesk.features.notifications.presentation.effect.NotificationsUiEffect
import com.requena.supportdesk.features.notifications.presentation.event.NotificationsUiEvent
import com.requena.supportdesk.features.notifications.presentation.state.NotificationsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NotificationsViewModel(
    private val registerDeviceUseCase: RegisterDeviceUseCase,
) : BaseViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<NotificationsUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<NotificationsUiEffect> = _effects.asSharedFlow()

    fun onEvent(event: NotificationsUiEvent) {
        when (event) {
            NotificationsUiEvent.RegisterAdminDevice -> registerDefaultDevice()
        }
    }

    private fun registerDefaultDevice() {
        launch {
            val userId = AdminSessionContext.currentUserId()
            if (userId == null) {
                val message = "No se encontro una sesion de administrador activa."
                _state.update { it.copy(isRegistering = false, statusMessage = message) }
                _effects.emit(NotificationsUiEffect.ShowMessage(message))
                return@launch
            }
            _state.update { it.copy(isRegistering = true, statusMessage = "Registrando dispositivo...") }
            when (val result = registerDeviceUseCase(SupportDeskSeed.defaultDevice(userId = userId))) {
                is AppResult.Error -> {
                    _state.update { it.copy(isRegistering = false, statusMessage = result.message) }
                    _effects.emit(NotificationsUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            isRegistering = false,
                            device = result.data,
                            statusMessage = "Dispositivo registrado",
                        )
                    }
                }
            }
        }
    }
}
