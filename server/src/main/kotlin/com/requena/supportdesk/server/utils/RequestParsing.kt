package com.requena.supportdesk.server.utils

import io.ktor.server.request.receiveText
import io.ktor.server.routing.RoutingCall
import kotlinx.serialization.json.Json

@PublishedApi
internal val requestJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
suspend inline fun <reified T> RoutingCall.receiveOrDefault(default: T): T =
    runCatching { requestJson.decodeFromString<T>(receiveText()) }.getOrDefault(default)
