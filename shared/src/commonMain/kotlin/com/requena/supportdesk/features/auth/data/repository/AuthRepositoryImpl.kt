package com.requena.supportdesk.features.auth.data.repository

import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.network.AdminSessionContext
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.core.network.supportDeskNetworkJson
import com.requena.supportdesk.features.auth.data.datasource.AuthDataSource
import com.requena.supportdesk.features.auth.data.mapper.AuthMapper
import com.requena.supportdesk.features.auth.data.session.AuthSessionStore
import com.requena.supportdesk.features.auth.data.dto.AuthSessionDto
import com.requena.supportdesk.features.auth.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource,
    private val sessionStore: AuthSessionStore,
) : AuthRepository {
    override suspend fun login(email: String, password: String): AppResult<User> {
        return runCatching {
            val session = dataSource.login(email, password)
            sessionStore.write(supportDeskNetworkJson.encodeToString(AuthSessionDto.serializer(), session))
            AuthMapper.fromDto(session).also(AdminSessionContext::update)
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(message = it.message ?: "No se pudo iniciar sesion.", cause = it) },
        )
    }

    override fun restoreSession(): User? =
        runCatching {
            sessionStore.read()
                ?.let { supportDeskNetworkJson.decodeFromString(AuthSessionDto.serializer(), it) }
                ?.let(AuthMapper::fromDto)
                ?.also(AdminSessionContext::update)
        }.getOrElse {
            sessionStore.clear()
            AdminSessionContext.update(null)
            null
        }

    override fun clearSession() {
        sessionStore.clear()
        AdminSessionContext.update(null)
    }
}
