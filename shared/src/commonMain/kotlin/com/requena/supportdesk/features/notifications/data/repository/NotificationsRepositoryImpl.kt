package com.requena.supportdesk.features.notifications.data.repository

import com.requena.supportdesk.core.model.NotificationDevice
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.notifications.data.datasource.NotificationsDataSource
import com.requena.supportdesk.features.notifications.data.mapper.NotificationsMapper
import com.requena.supportdesk.features.notifications.domain.repository.NotificationsRepository

class NotificationsRepositoryImpl(
    private val dataSource: NotificationsDataSource,
) : NotificationsRepository {
    override suspend fun registerDevice(device: NotificationDevice): AppResult<NotificationDevice> = runCatching {
        NotificationsMapper.fromDto(dataSource.registerDevice(device))
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo registrar el dispositivo.", cause = it) },
    )
}
