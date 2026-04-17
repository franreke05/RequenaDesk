package com.requena.supportdesk.server.utils

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveNullable
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

suspend inline fun <reified T : Any> ApplicationCall.receiveOrDefault(default: T): T {
    runCatching { receiveNullable<T>() }
        .getOrNull()
        ?.let { return it }

    val rawBody = runCatching {
        request.receiveChannel().readRemaining().readText(Charsets.UTF_8)
    }.getOrDefault("").trim()
    if (rawBody.isBlank()) return default

    val normalizedBodies = listOf(
        rawBody,
        rawBody.removeSurrounding("'"),
        rawBody.removeSurrounding("\""),
        rawBody.removeSurrounding("\"").replace("\\\"", "\""),
    ).distinct().filter { it.isNotBlank() }

    normalizedBodies.forEach { body ->
        runCatching { requestJson.decodeFromString<T>(body) }
            .getOrNull()
            ?.let { return it }
    }

    return default
}
