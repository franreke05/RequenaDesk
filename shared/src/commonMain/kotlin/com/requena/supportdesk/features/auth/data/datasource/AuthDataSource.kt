package com.requena.supportdesk.features.auth.data.datasource

import com.requena.supportdesk.core.network.ApiEnvelope
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.auth.data.dto.AuthSessionDto
import com.requena.supportdesk.features.auth.data.dto.LoginRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

interface AuthDataSource {
    suspend fun login(email: String, password: String): AuthSessionDto
}

class RemoteAuthDataSource(
    private val httpClient: HttpClient,
) : AuthDataSource {
    override suspend fun login(email: String, password: String): AuthSessionDto {
        val response = httpClient.post("${supportDeskBaseUrl()}/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(email = email, password = password))
        }
        return response.body<ApiEnvelope<AuthSessionDto>>().data
    }
}
