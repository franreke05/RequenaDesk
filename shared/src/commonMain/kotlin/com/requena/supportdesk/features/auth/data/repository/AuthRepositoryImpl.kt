package com.requena.supportdesk.features.auth.data.repository

import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.network.SupportDeskSessionManager
import com.requena.supportdesk.core.network.supportDeskBaseUrl
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
            onFailure = { AppResult.Error(message = it.toLoginMessage(), cause = it) },
        )
    }

    override suspend fun claimClientAccess(code: String, email: String): AppResult<User> {
        return runCatching {
            val session = dataSource.claimClientAccess(code, email)
            // El auth session puede no incluir companyName; enriquecemos con /client/me si está disponible
            val enriched = if (session.companyName.isBlank()) {
                runCatching { dataSource.getClientProfile() }
                    .getOrNull()
                    ?.takeIf { it.companyName.isNotBlank() }
                    ?.let { session.copy(companyName = it.companyName) }
                    ?: session
            } else {
                session
            }
            sessionManager.persistSession(enriched)
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(message = it.toClaimMessage(), cause = it) },
        )
    }

    override fun restoreSession(): User? = sessionManager.restoreSession()

    override fun clearSession() {
        sessionManager.clear()
    }

    private fun Throwable.toLoginMessage(): String {
        val rawMessage = message.orEmpty()
        return when {
            rawMessage.contains("connect_timeout", ignoreCase = true) ||
                rawMessage.contains("timeout", ignoreCase = true) ||
                rawMessage.contains("ConnectException", ignoreCase = true) ||
                rawMessage.contains("No route to host", ignoreCase = true) ||
                rawMessage.contains("No es posible conectar", ignoreCase = true) ->
                "No se puede conectar con OryKai Server. Backend configurado: ${supportDeskBaseUrl()}."
            rawMessage.isBlank() -> "No se pudo iniciar sesion."
            else -> rawMessage
        }
    }

    private fun Throwable.toClaimMessage(): String {
        val rawMessage = message.orEmpty()
        return when {
            rawMessage.contains("connect_timeout", ignoreCase = true) ||
            rawMessage.contains("timeout", ignoreCase = true) ||
                rawMessage.contains("ConnectException", ignoreCase = true) ->
                "No se puede conectar con OryKai Server para entrar como cliente. Backend configurado: ${supportDeskBaseUrl()}."
            rawMessage.isBlank() -> "No se pudo iniciar la sesion cliente."
            else -> rawMessage
        }
    }
}
