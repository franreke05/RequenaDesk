package com.requena.supportdesk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

actual fun createSupportDeskHttpClient(): HttpClient = HttpClient(Darwin)

actual fun supportDeskBaseUrl(): String = resolveIosSupportDeskBaseUrl()

private fun resolveIosSupportDeskBaseUrl(): String {
    val configuredUrl = configuredBaseUrl() ?: "http://127.0.0.1:8080"
    return configuredUrl.removeSuffix("/")
}

@OptIn(ExperimentalForeignApi::class)
private fun configuredBaseUrl(): String? =
    getenv("SUPPORTDESK_BASE_URL")
        ?.toKString()
        ?.trim()
        ?.takeIf(String::isNotBlank)
