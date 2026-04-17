package com.requena.supportdesk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createSupportDeskHttpClient(): HttpClient

expect fun supportDeskBaseUrl(): String

fun configuredSupportDeskHttpClient(): HttpClient = createSupportDeskHttpClient().config {
    install(DefaultRequest) {
        AdminSessionContext.currentUserId()?.let { adminUserId ->
            headers.append("X-Admin-User-Id", adminUserId)
        }
    }
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
