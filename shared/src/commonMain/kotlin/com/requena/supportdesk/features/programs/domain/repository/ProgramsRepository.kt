package com.requena.supportdesk.features.programs.domain.repository

import com.requena.supportdesk.core.model.ClientProgramBillingPreview
import com.requena.supportdesk.core.model.ClientProgramRequest
import com.requena.supportdesk.core.model.ClientProgramsOverview
import com.requena.supportdesk.core.result.AppResult

interface ProgramsRepository {
    suspend fun getClientPrograms(): AppResult<ClientProgramsOverview>
    suspend fun createProgramRequests(productKeys: Set<String>, customerNote: String): AppResult<List<ClientProgramRequest>>
    suspend fun getAdminProgramRequests(): AppResult<List<ClientProgramRequest>>
    suspend fun approveProgramRequest(
        requestId: String,
        monthlyPriceCents: Long,
        adminNote: String?,
    ): AppResult<ClientProgramRequest>
    suspend fun rejectProgramRequest(requestId: String, adminNote: String?): AppResult<ClientProgramRequest>
    suspend fun getBillingPreview(clientId: String, period: String): AppResult<ClientProgramBillingPreview>
}
