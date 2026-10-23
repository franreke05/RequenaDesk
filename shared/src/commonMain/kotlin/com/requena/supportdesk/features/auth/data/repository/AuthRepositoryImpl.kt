package com.requena.supportdesk.features.auth.data.repository

import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.network.SupportDeskSessionManager
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.auth.data.datasource.AuthDataSource
import com.requena.supportdesk.features.auth.data.dto.AuthSessionDto
import com.requena.supportdesk.features.auth.data.dto.JoinOrganizationRequestDto
import com.requena.supportdesk.features.auth.data.dto.SignupRequestDto
import com.requena.supportdesk.features.auth.domain.repository.AuthRepository
import com.requena.supportdesk.features.auth.domain.repository.JoinForm
import com.requena.supportdesk.features.auth.domain.repository.SignupForm
import kotlinx.coroutines.CancellationException

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource,
    private val sessionManager: SupportDeskSessionManager,
) : AuthRepository {
    override suspend fun login(email: String, password: String): AppResult<User> =
        openSession { dataSource.login(email, password) }

    override fun restoreSession(): User? = sessionManager.restoreSession()

    override fun clearSession() {
        sessionManager.clear()
    }

    override suspend fun isSignupAvailable(): Boolean =
        try {
            dataSource.signupStatus().enabled
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            false
        }

    override suspend fun signup(form: SignupForm): AppResult<User> = openSession {
        dataSource.signup(
            SignupRequestDto(
                name = form.name.trim(),
                email = form.email.trim(),
                password = form.password,
                organizationName = form.organizationName.trim(),
                acceptTerms = form.acceptTerms,
                acceptPrivacy = form.acceptPrivacy,
            ),
        )
    }

    override suspend fun join(form: JoinForm): AppResult<User> = openSession {
        dataSource.join(
            JoinOrganizationRequestDto(
                code = form.code.trim(),
                name = form.name.trim(),
                email = form.email.trim(),
                password = form.password,
                acceptTerms = form.acceptTerms,
                acceptPrivacy = form.acceptPrivacy,
            ),
        )
    }

    override suspend fun refreshSession(): User? {
        val refreshToken = sessionManager.currentRefreshToken() ?: return null
        return try {
            sessionManager.persistSession(dataSource.refresh(refreshToken))
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            sessionManager.clear()
            null
        }
    }

    private suspend fun openSession(request: suspend () -> AuthSessionDto): AppResult<User> =
        try {
            AppResult.Success(sessionManager.persistSession(request()))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppResult.Error(message = error.toAuthMessage(), cause = error)
        }

    private fun Throwable.toAuthMessage(): String {
        val rawMessage = message.orEmpty()
        val normalized = rawMessage.lowercase()
        return when {
            "invalid credentials" in normalized -> "Credenciales invalidas."
            "email and password" in normalized -> "Introduce correo y contrasena."
            "already used by another account" in normalized -> "Ya existe una cuenta con ese correo."
            "invitation not found" in normalized -> "El codigo de invitacion no es valido, ya se uso o ha caducado."
            "signup is not available" in normalized -> "El alta de empresas no esta disponible en este servidor."
            "terms and privacy" in normalized -> "Tienes que aceptar los terminos y la politica de privacidad."
            "password must contain" in normalized -> "La contrasena debe tener al menos 12 caracteres."
            "valid email" in normalized -> "Introduce un correo valido."
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
