package com.requena.supportdesk.features.auth.data.dto

data class AuthSessionDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val clientId: String? = null,
)
