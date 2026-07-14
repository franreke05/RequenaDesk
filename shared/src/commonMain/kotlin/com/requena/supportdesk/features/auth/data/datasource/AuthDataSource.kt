package com.requena.supportdesk.features.auth.data.datasource

import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.auth.data.dto.AuthSessionDto
import com.requena.supportdesk.features.auth.data.dto.LoginRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody

interface AuthDataSource {
    suspend fun login(email: String, password: String): AuthSessionDto
}

class RemoteAuthDataSource(
    private val httpClient: HttpClient,
) : AuthDataSource {
    override suspend fun login(email: String, password: String): AuthSessionDto =
        httpClient.post("${supportDeskBaseUrl()}/auth/login") {
            setBody(jsonRequestBody(LoginRequestDto(email = email.trim(), password = password)))
        }.requireApiData()
}
