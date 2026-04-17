package com.requena.supportdesk.core.network

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
        }.getOrDefault("Request failed with status ${status.value}")
        error(message)
    }
    return supportDeskNetworkJson.decodeFromString<ApiEnvelope<T>>(payload).data
}

suspend fun HttpResponse.requireSuccess() {
    val payload = bodyAsText()
    if (!status.isSuccess()) {
        val message = runCatching {
            supportDeskNetworkJson.decodeFromString<ApiErrorEnvelope>(payload).message
        }.getOrDefault("Request failed with status ${status.value}")
        error(message)
    }
}
