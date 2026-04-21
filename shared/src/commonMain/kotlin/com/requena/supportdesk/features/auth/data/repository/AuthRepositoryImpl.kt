package com.requena.supportdesk.features.auth.data.repository

import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.network.SupportDeskSessionManager
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.auth.data.datasource.AuthDataSource
import com.requena.supportdesk.features.auth.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource,
    private val sessionManager: SupportDeskSessionManager,
) : AuthRepository {
    override suspend fun login(email: String, password: String): AppResult<User> {
        return runCatching {
            val session = dataSource.login(email, password)
            sessionManager.persistSession(session)
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(message = it.message ?: "No se pudo iniciar sesion.", cause = it) },
        )
    }

    override fun restoreSession(): User? = sessionManager.restoreSession()

    override fun clearSession() {
        sessionManager.clear()
    }
}
