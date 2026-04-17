package com.requena.supportdesk.server.utils

import io.ktor.server.routing.RoutingCall
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import kotlinx.serialization.json.Json

@PublishedApi
internal val requestJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

suspend inline fun <reified T> RoutingCall.receiveOrDefault(default: T): T {
    val rawBody = runCatching {
        request.receiveChannel().readRemaining().readText(Charsets.UTF_8)
    }.getOrDefault("")

    if (rawBody.isBlank()) return default

    return runCatching {
        requestJson.decodeFromString<T>(rawBody)
    }.getOrDefault(default)
}
