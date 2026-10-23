package com.requena.supportdesk.core.network

import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ApiEnvelope<T>(
    val status: String,
    val path: String,
    val data: T,
)

@Serializable
data class ApiErrorEnvelope(
    val status: String,
    val message: String = "Unexpected error",
)

@PublishedApi
internal val supportDeskNetworkJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

suspend inline fun <reified T> HttpResponse.requireApiData(): T {
    val payload = bodyAsText()
    if (!status.isSuccess()) {
        val message = runCatching {
            supportDeskNetworkJson.decodeFromString<ApiErrorEnvelope>(payload).message
        }.getOrDefault("El servidor respondió con estado ${status.value}.")
        error(planAwareMessage(message))
    }
    return supportDeskNetworkJson.decodeFromString<ApiEnvelope<T>>(payload).data
}

suspend fun HttpResponse.requireSuccess() {
    val payload = bodyAsText()
    if (!status.isSuccess()) {
        val message = runCatching {
            supportDeskNetworkJson.decodeFromString<ApiErrorEnvelope>(payload).message
        }.getOrDefault("El servidor respondió con estado ${status.value}.")
        error(planAwareMessage(message))
    }
}

inline fun <reified T> jsonRequestBody(value: T): TextContent =
    TextContent(
        text = supportDeskNetworkJson.encodeToString(value),
        contentType = ContentType.Application.Json,
    )

/**
 * Plan gates (402) and plan limits (409) can come from any screen: show them in Spanish with
 * a pointer to Ajustes › Plan y pagos. Other messages pass through unchanged.
 */
@PublishedApi
internal fun planAwareMessage(message: String): String {
    val normalized = message.lowercase()
    return if ("not included in your plan" in normalized || "limit of your plan" in normalized) {
        com.requena.supportdesk.features.plan.planMessage(message)
    } else {
        message
    }
}
