package com.requena.supportdesk.features.auth.domain.usecase

import com.requena.supportdesk.features.auth.domain.repository.AuthRepository

class LoginUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String) = repository.login(email, password)
}
