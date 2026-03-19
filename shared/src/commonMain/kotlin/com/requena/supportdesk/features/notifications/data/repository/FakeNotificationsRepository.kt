package com.requena.supportdesk.features.notifications.data.repository

import com.requena.supportdesk.core.model.NotificationDevice
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.notifications.domain.repository.NotificationsRepository

class FakeNotificationsRepository : NotificationsRepository {
    private val registeredDevices = mutableListOf<NotificationDevice>()

    override suspend fun registerDevice(device: NotificationDevice): AppResult<NotificationDevice> {
        registeredDevices.removeAll { it.id == device.id }
        registeredDevices += device
        return AppResult.Success(device)
    }
}
