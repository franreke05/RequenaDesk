package com.requena.supportdesk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json

expect fun createSupportDeskHttpClient(): HttpClient

expect fun supportDeskBaseUrl(): String

fun configuredSupportDeskHttpClient(
    sessionManager: SupportDeskSessionManager? = null,
): HttpClient {
    val refreshClient = sessionManager?.let(::createRefreshHttpClient)
    val client = createSupportDeskHttpClient().config {
        installSupportDeskHttpPlugins()

        install(DefaultRequest) {
            headers.remove(HttpHeaders.Authorization)
            sessionManager?.currentAccessToken()?.let { accessToken ->
                headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }

    if (sessionManager != null && refreshClient != null) {
        client.plugin(HttpSend).intercept { request ->
            val initialCall = execute(request)
            if (initialCall.response.status != HttpStatusCode.Unauthorized) {
                return@intercept initialCall
            }
            if (request.url.encodedPath.isAuthSessionPath()) {
                return@intercept initialCall
            }
            if (request.attributes.getOrNull(refreshAttemptKey) == true) {
                return@intercept initialCall
            }
            if (!sessionManager.refreshSession(refreshClient)) {
                AdminSessionContext.notifySessionExpired()
                return@intercept initialCall
            }

            request.attributes.put(refreshAttemptKey, true)
            request.headers.remove(HttpHeaders.Authorization)
            sessionManager.currentAccessToken()?.let { refreshedAccessToken ->
                request.headers.append(HttpHeaders.Authorization, "Bearer $refreshedAccessToken")
            }
            execute(request)
        }
    }

    return client
}

private fun createRefreshHttpClient(sessionManager: SupportDeskSessionManager): HttpClient =
    createSupportDeskHttpClient().config {
        installSupportDeskHttpPlugins()
        install(DefaultRequest) {
            headers.remove(HttpHeaders.Authorization)
            sessionManager.currentAccessToken()?.let { accessToken ->
                headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }

private fun io.ktor.client.HttpClientConfig<*>.installSupportDeskHttpPlugins() {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            },
        )
    }
    install(Logging) {
        level = LogLevel.INFO
    }
}

private fun String.isAuthSessionPath(): Boolean =
    startsWith("/auth/login") || startsWith("/auth/refresh") || startsWith("/auth/logout")

private val refreshAttemptKey = AttributeKey<Boolean>("supportdesk.refreshAttempt")
