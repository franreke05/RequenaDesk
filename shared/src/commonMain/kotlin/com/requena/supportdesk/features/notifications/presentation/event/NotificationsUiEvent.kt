package com.requena.supportdesk.features.notifications.presentation.event

sealed interface NotificationsUiEvent {
    object RegisterAdminDevice : NotificationsUiEvent
}
