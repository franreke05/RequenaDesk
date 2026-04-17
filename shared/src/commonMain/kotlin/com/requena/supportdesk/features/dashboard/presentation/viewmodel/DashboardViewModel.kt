package com.requena.supportdesk.features.dashboard.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.common.SupportDeskSeed
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.dashboard.domain.usecase.GetDashboardSummaryUseCase
import com.requena.supportdesk.features.dashboard.presentation.effect.DashboardUiEffect
import com.requena.supportdesk.features.dashboard.presentation.event.DashboardUiEvent
import com.requena.supportdesk.features.dashboard.presentation.state.DashboardUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DashboardViewModel(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
) : BaseViewModel() {
    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<DashboardUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<DashboardUiEffect> = _effects.asSharedFlow()

    init {
        onEvent(DashboardUiEvent.Refresh)
    }

    fun onEvent(event: DashboardUiEvent) {
        when (event) {
            DashboardUiEvent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getDashboardSummaryUseCase()) {
                is AppResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            summary = SupportDeskSeed.dashboardSummary(),
                            errorMessage = "Usando datos admin locales mientras el servidor no esta disponible.",
                        )
                    }
                    _effects.emit(DashboardUiEffect.ShowMessage("Panel cargado con datos locales"))
                }
                is AppResult.Success -> {
                    _state.update { it.copy(isLoading = false, summary = result.data) }
                }
            }
        }
    }
}
