package com.requena.supportdesk.features.notifications.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDeviceDto(
    val id: String,
    val userId: String,
    val platform: String,
    val token: String = "",
    val lastSeenAt: String = "",
)
