package com.requena.supportdesk.features.notifications.data.datasource

import com.requena.supportdesk.features.notifications.data.dto.NotificationDeviceDto

interface NotificationsDataSource {
    suspend fun registerDevice(device: NotificationDeviceDto): NotificationDeviceDto
}
