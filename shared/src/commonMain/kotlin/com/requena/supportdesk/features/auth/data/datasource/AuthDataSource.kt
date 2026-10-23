package com.requena.supportdesk.features.auth.data.datasource

import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.auth.data.dto.AuthSessionDto
import com.requena.supportdesk.features.auth.data.dto.JoinOrganizationRequestDto
import com.requena.supportdesk.features.auth.data.dto.LoginRequestDto
import com.requena.supportdesk.features.auth.data.dto.RefreshSessionRequestDto
import com.requena.supportdesk.features.auth.data.dto.SignupRequestDto
import com.requena.supportdesk.features.auth.data.dto.SignupStatusDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

interface AuthDataSource {
    suspend fun login(email: String, password: String): AuthSessionDto
    suspend fun signupStatus(): SignupStatusDto
    suspend fun signup(request: SignupRequestDto): AuthSessionDto
    suspend fun join(request: JoinOrganizationRequestDto): AuthSessionDto
    suspend fun refresh(refreshToken: String): AuthSessionDto
}

class RemoteAuthDataSource(
    private val httpClient: HttpClient,
) : AuthDataSource {
    override suspend fun login(email: String, password: String): AuthSessionDto =
        httpClient.post("${supportDeskBaseUrl()}/auth/login") {
            setBody(jsonRequestBody(LoginRequestDto(email = email.trim(), password = password)))
        }.requireApiData()

    override suspend fun signupStatus(): SignupStatusDto =
        httpClient.get("${supportDeskBaseUrl()}/auth/signup/status").requireApiData()

    override suspend fun signup(request: SignupRequestDto): AuthSessionDto =
        httpClient.post("${supportDeskBaseUrl()}/auth/signup") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun join(request: JoinOrganizationRequestDto): AuthSessionDto =
        httpClient.post("${supportDeskBaseUrl()}/auth/join") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun refresh(refreshToken: String): AuthSessionDto =
        httpClient.post("${supportDeskBaseUrl()}/auth/refresh") {
            setBody(jsonRequestBody(RefreshSessionRequestDto(refreshToken)))
        }.requireApiData()
}
