package com.requena.supportdesk.features.notifications.data.mapper

import com.requena.supportdesk.core.model.NotificationDevice
import com.requena.supportdesk.features.notifications.data.dto.NotificationDeviceDto

object NotificationsMapper {
    fun fromDto(dto: NotificationDeviceDto): NotificationDevice = NotificationDevice(
        id = dto.id,
        userId = dto.userId,
        platform = dto.platform,
        token = dto.token,
        lastSeenAt = "2026-03-19T10:00:00Z",
    )
}
