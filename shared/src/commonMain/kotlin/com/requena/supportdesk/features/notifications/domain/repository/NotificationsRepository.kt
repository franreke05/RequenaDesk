package com.requena.supportdesk.features.notifications.domain.repository

import com.requena.supportdesk.core.model.NotificationDevice
import com.requena.supportdesk.core.result.AppResult

interface NotificationsRepository {
    suspend fun registerDevice(device: NotificationDevice): AppResult<NotificationDevice>
}
