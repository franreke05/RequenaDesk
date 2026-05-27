package com.requena.supportdesk.features.auth.domain.usecase

import com.requena.supportdesk.features.auth.domain.repository.AuthRepository

class ClaimClientAccessUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(code: String, email: String) =
        repository.claimClientAccess(code, email)
}
