package com.requena.supportdesk.features.auth.domain.usecase

import com.requena.supportdesk.features.auth.domain.repository.AuthRepository

class RestoreSessionUseCase(
    private val repository: AuthRepository,
) {
    operator fun invoke() = repository.restoreSession()
}
