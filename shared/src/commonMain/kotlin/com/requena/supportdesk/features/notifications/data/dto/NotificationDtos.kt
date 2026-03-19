package com.requena.supportdesk.features.notifications.data.dto

data class NotificationDeviceDto(
    val id: String,
    val userId: String,
    val platform: String,
    val token: String,
)
