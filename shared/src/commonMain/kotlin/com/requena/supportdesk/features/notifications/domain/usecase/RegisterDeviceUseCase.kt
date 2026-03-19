package com.requena.supportdesk.features.notifications.domain.usecase

import com.requena.supportdesk.core.model.NotificationDevice
import com.requena.supportdesk.features.notifications.domain.repository.NotificationsRepository

class RegisterDeviceUseCase(
    private val repository: NotificationsRepository,
) {
    suspend operator fun invoke(device: NotificationDevice) = repository.registerDevice(device)
}
