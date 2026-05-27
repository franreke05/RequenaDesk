package com.requena.supportdesk.features.auth.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class ClientAccessClaimRequestDto(
    val code: String,
    val email: String,
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
    val companyName: String = "",
    val accessToken: String,
    val refreshToken: String,
)

/**
 * Perfil del cliente autenticado — obtenido vía GET /client/me.
 * Permite recuperar Client.companyName cuando el auth session no lo incluye.
 */
@Serializable
data class ClientProfileDto(
    val id: String = "",
    val companyName: String = "",
    val contactName: String = "",
    val email: String = "",
)
