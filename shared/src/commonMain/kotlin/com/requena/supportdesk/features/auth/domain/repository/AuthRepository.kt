package com.requena.supportdesk.features.auth.domain.repository

import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.result.AppResult

interface AuthRepository {
    suspend fun login(email: String, password: String): AppResult<User>
    fun restoreSession(): User?
    fun clearSession()

    /** Whether the server accepts self-service company signups (false when unreachable). */
    suspend fun isSignupAvailable(): Boolean

    suspend fun signup(form: SignupForm): AppResult<User>

    suspend fun join(form: JoinForm): AppResult<User>

    /** Refreshes the stored session (e.g. one saved before companies existed); null if it is no longer valid. */
    suspend fun refreshSession(): User?
}

data class SignupForm(
    val name: String,
    val email: String,
    val password: String,
    val organizationName: String,
    val acceptTerms: Boolean,
    val acceptPrivacy: Boolean,
)

data class JoinForm(
    val code: String,
    val name: String,
    val email: String,
    val password: String,
    val acceptTerms: Boolean,
    val acceptPrivacy: Boolean,
)
