package com.requena.supportdesk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createSupportDeskHttpClient(): HttpClient = HttpClient(Darwin)

actual fun supportDeskBaseUrl(): String = "http://127.0.0.1:8080"
