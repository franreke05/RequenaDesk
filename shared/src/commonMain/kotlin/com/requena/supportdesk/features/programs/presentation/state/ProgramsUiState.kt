package com.requena.supportdesk.features.programs.presentation.state

import com.requena.supportdesk.core.model.ClientProgramBillingPreview
import com.requena.supportdesk.core.model.ClientProgramRequest
import com.requena.supportdesk.core.model.ClientProgramsOverview

data class ProgramsUiState(
    val overview: ClientProgramsOverview = ClientProgramsOverview(),
    val selectedProgramKeys: Set<String> = emptySet(),
    val customerNote: String = "",
    val adminRequests: List<ClientProgramRequest> = emptyList(),
    val billingPreview: ClientProgramBillingPreview? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)
