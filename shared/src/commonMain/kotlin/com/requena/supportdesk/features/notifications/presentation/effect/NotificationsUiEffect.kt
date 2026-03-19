package com.requena.supportdesk.features.notifications.presentation.effect

sealed interface NotificationsUiEffect {
    data class ShowMessage(val message: String) : NotificationsUiEffect
}
