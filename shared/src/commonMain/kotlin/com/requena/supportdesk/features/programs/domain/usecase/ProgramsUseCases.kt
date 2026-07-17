package com.requena.supportdesk.features.programs.domain.usecase

import com.requena.supportdesk.features.programs.domain.repository.ProgramsRepository

class GetClientProgramsUseCase(private val repository: ProgramsRepository) {
    suspend operator fun invoke() = repository.getClientPrograms()
}

class RequestProgramsUseCase(private val repository: ProgramsRepository) {
    suspend operator fun invoke(productKeys: Set<String>, customerNote: String) =
        repository.createProgramRequests(productKeys, customerNote)
}

class GetAdminProgramRequestsUseCase(private val repository: ProgramsRepository) {
    suspend operator fun invoke() = repository.getAdminProgramRequests()
}

class ApproveProgramRequestUseCase(private val repository: ProgramsRepository) {
    suspend operator fun invoke(requestId: String, monthlyPriceCents: Long, adminNote: String?) =
        repository.approveProgramRequest(requestId, monthlyPriceCents, adminNote)
}

class RejectProgramRequestUseCase(private val repository: ProgramsRepository) {
    suspend operator fun invoke(requestId: String, adminNote: String?) =
        repository.rejectProgramRequest(requestId, adminNote)
}

class GetClientProgramBillingPreviewUseCase(private val repository: ProgramsRepository) {
    suspend operator fun invoke(clientId: String, period: String) = repository.getBillingPreview(clientId, period)
}
