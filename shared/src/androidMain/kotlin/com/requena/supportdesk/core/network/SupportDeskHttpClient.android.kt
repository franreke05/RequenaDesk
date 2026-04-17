package com.requena.supportdesk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createSupportDeskHttpClient(): HttpClient = HttpClient(CIO)

actual fun supportDeskBaseUrl(): String = "http://10.0.2.2:8080"
