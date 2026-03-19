package com.requena.supportdesk.core.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val clientId: String? = null,
)
