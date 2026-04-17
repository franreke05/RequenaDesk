package com.requena.supportdesk.features.notifications.data.datasource

import com.requena.supportdesk.core.model.NotificationDevice
import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.notifications.data.dto.NotificationDeviceDto
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

interface NotificationsDataSource {
    suspend fun registerDevice(device: NotificationDevice): NotificationDeviceDto
}

class RemoteNotificationsDataSource(
    private val httpClient: HttpClient,
) : NotificationsDataSource {
    override suspend fun registerDevice(device: NotificationDevice): NotificationDeviceDto =
        httpClient.post("${supportDeskBaseUrl()}/devices/register") {
            setBody(jsonRequestBody(RegisterDeviceRequestDto(device.userId, device.token, device.platform)))
        }.requireApiData<NotificationDeviceDto>().copy(
            token = device.token,
            lastSeenAt = device.lastSeenAt,
        )
}

@Serializable
private data class RegisterDeviceRequestDto(
    val userId: String,
    val token: String,
    val platform: String,
)
