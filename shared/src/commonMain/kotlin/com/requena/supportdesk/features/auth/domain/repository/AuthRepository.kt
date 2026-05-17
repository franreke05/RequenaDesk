package com.requena.supportdesk.features.auth.domain.repository

import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.result.AppResult

interface AuthRepository {
    suspend fun login(email: String, password: String): AppResult<User>
    suspend fun claimClientAccess(code: String, name: String, email: String, password: String): AppResult<User>
    fun restoreSession(): User?
    fun clearSession()
}
