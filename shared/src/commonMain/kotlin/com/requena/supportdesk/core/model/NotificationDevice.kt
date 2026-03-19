package com.requena.supportdesk.core.model

data class NotificationDevice(
    val id: String,
    val userId: String,
    val platform: String,
    val token: String,
    val lastSeenAt: String,
)
