package com.requena.supportdesk.features.auth.data.repository

import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.network.SupportDeskSessionManager
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.auth.data.datasource.AuthDataSource
import com.requena.supportdesk.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource,
    private val sessionManager: SupportDeskSessionManager,
) : AuthRepository {
    override suspend fun login(email: String, password: String): AppResult<User> {
        return try {
            val session = dataSource.login(email, password)
            AppResult.Success(sessionManager.persistSession(session))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppResult.Error(message = error.toLoginMessage(), cause = error)
        }
    }

    override fun restoreSession(): User? = sessionManager.restoreSession()

    override fun clearSession() {
        sessionManager.clear()
    }

    private fun Throwable.toLoginMessage(): String {
        val rawMessage = message.orEmpty()
        val normalized = rawMessage.lowercase()
        return when {
            "invalid credentials" in normalized -> "Credenciales invalidas."
            "email and password" in normalized -> "Introduce correo y contrasena."
            "connection refused" in normalized ||
                "failed to connect" in normalized ||
                "connect timed out" in normalized ||
                "unresolved address" in normalized -> {
                "No se pudo conectar con el servidor. Revisa que Ktor este iniciado y que supportdesk.baseUrl apunte al backend."
            }
            rawMessage.isNotBlank() -> rawMessage
            else -> "No se pudo iniciar sesion."
        }
    }
}
