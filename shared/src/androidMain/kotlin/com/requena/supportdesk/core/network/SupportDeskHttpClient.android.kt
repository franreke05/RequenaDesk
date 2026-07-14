package com.requena.supportdesk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createSupportDeskHttpClient(): HttpClient = HttpClient(CIO)

actual fun supportDeskBaseUrl(): String = resolveAndroidSupportDeskBaseUrl()

private fun resolveAndroidSupportDeskBaseUrl(): String {
    val configuredUrl = configuredBaseUrl() ?: "http://10.0.2.2:8080"
    return configuredUrl.removeSuffix("/")
}

private fun configuredBaseUrl(): String? {
    val propertyValue = System.getProperty("supportdesk.baseUrl")
        ?.trim()
        ?.takeIf(String::isNotBlank)
    if (propertyValue != null) return propertyValue

    return System.getenv("SUPPORTDESK_BASE_URL")
        ?.trim()
        ?.takeIf(String::isNotBlank)
}
