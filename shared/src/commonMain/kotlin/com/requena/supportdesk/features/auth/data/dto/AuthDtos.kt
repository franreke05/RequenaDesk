package com.requena.supportdesk.features.auth.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshSessionRequestDto(
    val refreshToken: String,
)

@Serializable
data class AuthSessionDto(
    val userId: String,
    val name: String,
    val email: String,
    val role: String,
    val clientId: String? = null,
    val accessToken: String,
    val refreshToken: String,
    val organizationId: String? = null,
    val organizationRole: String? = null,
    val organizationName: String? = null,
)

@Serializable
data class SignupRequestDto(
    val name: String,
    val email: String,
    val password: String,
    val organizationName: String,
    val acceptTerms: Boolean,
    val acceptPrivacy: Boolean,
)

@Serializable
data class JoinOrganizationRequestDto(
    val code: String,
    val name: String,
    val email: String,
    val password: String,
    val acceptTerms: Boolean,
    val acceptPrivacy: Boolean,
)

@Serializable
data class SignupStatusDto(
    val enabled: Boolean = false,
)
