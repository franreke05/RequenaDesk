package com.requena.supportdesk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createSupportDeskHttpClient(): HttpClient = HttpClient(CIO)

actual fun supportDeskBaseUrl(): String {
    val envUrl = System.getenv("SUPPORTDESK_BASE_URL")
        ?.trim()
        ?.takeIf(String::isNotBlank)
    return (envUrl ?: "https://crm.franciscorequena.cloud").removeSuffix("/")
}
