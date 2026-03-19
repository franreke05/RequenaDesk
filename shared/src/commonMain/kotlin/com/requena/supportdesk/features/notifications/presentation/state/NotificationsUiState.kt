package com.requena.supportdesk.features.notifications.presentation.state

import com.requena.supportdesk.core.model.NotificationDevice

data class NotificationsUiState(
    val device: NotificationDevice? = null,
    val isRegistering: Boolean = false,
    val statusMessage: String = "Sin registrar",
)
