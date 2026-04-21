package com.requena.supportdesk.core.network

import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.features.auth.data.dto.AuthSessionDto
import com.requena.supportdesk.features.auth.data.dto.RefreshSessionRequestDto
import com.requena.supportdesk.features.auth.data.mapper.AuthMapper
import com.requena.supportdesk.features.auth.data.session.AuthSessionStore
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SupportDeskSessionManager(
    private val sessionStore: AuthSessionStore,
) {
    private val refreshMutex = Mutex()
    private var currentSession: AuthSessionDto? = null

    fun currentAccessToken(): String? = currentSession?.accessToken?.takeIf(String::isNotBlank)

    fun currentRefreshToken(): String? = currentSession?.refreshToken?.takeIf(String::isNotBlank)

    fun restoreSession(): User? =
        runCatching {
            val storedSession = sessionStore.read()
                ?.let { supportDeskNetworkJson.decodeFromString(AuthSessionDto.serializer(), it) }
                ?: return@runCatching clear().let { null }

            storedSession
                .also(::applySession)
                .let(AuthMapper::fromDto)
        }.getOrElse {
            clear()
            null
        }

    fun persistSession(session: AuthSessionDto): User {
        sessionStore.write(supportDeskNetworkJson.encodeToString(AuthSessionDto.serializer(), session))
        applySession(session)
        return AuthMapper.fromDto(session)
    }

    fun clear() {
        currentSession = null
        sessionStore.clear()
        AdminSessionContext.update(null)
    }

    suspend fun refreshSession(refreshClient: HttpClient): Boolean = refreshMutex.withLock {
        val refreshToken = currentRefreshToken() ?: run {
            clear()
            return false
        }

        val refreshedSession = runCatching {
            refreshClient.post("${supportDeskBaseUrl()}/auth/refresh") {
                setBody(jsonRequestBody(RefreshSessionRequestDto(refreshToken)))
            }.requireApiData<AuthSessionDto>()
        }.getOrElse {
            clear()
            return false
        }

        persistSession(refreshedSession)
        true
    }

    private fun applySession(session: AuthSessionDto) {
        currentSession = session
        AdminSessionContext.update(AuthMapper.fromDto(session))
    }
}
