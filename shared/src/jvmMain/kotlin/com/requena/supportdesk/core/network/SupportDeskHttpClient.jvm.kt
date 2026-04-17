package com.requena.supportdesk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createSupportDeskHttpClient(): HttpClient = HttpClient(CIO)

actual fun supportDeskBaseUrl(): String = "http://127.0.0.1:8080"
