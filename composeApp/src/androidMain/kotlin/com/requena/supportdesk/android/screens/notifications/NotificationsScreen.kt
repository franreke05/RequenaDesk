package com.requena.supportdesk.android.screens.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.notifications.presentation.state.NotificationsUiState

@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onRegisterDevice: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val placeholderItems = listOf(
        "SD-1001 · Northwind Desk · Waiting on admin.",
        "SD-1002 · Forge Flow · Waiting on client confirmation.",
        "Device registration is ready for a real push integration.",
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        PageHeader(
            title = "Notifications",
            subtitle = "Register the device now and keep a compact feed ready for future push wiring.",
            eyebrow = "Admin alerts",
        )
        when {
            state.isRegistering -> LoadingState(itemCount = 3)
            state.device == null -> EmptyState(
                title = "No device registered",
                message = state.statusMessage,
                actionText = "Register device",
                onAction = onRegisterDevice,
            )
            else -> {
                SectionCard(
                    title = "Device ready",
                    subtitle = state.statusMessage,
                ) {
                    PrimaryButton(
                        text = "Register again",
                        onClick = onRegisterDevice,
                    )
                }
                SectionCard(
                    title = "Latest alerts",
                    subtitle = "Placeholder items to shape the mobile-lite notification feed.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        placeholderItems.forEach { item ->
                            SectionCard(subtitle = item) {}
                        }
                    }
                }
            }
        }
    }
}
