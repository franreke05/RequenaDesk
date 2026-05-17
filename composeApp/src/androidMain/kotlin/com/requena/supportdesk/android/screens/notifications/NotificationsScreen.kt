package com.requena.supportdesk.android.screens.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.time.currentIsoDate
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
    scheduledTasks: List<WorkTask>,
    onRegisterDevice: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val todayIsoDate = currentIsoDate()
    val reminderItems = scheduledTasks
        .filter { task ->
            val dueDate = task.dueDate
            !task.completed && dueDate != null && dueDate >= todayIsoDate
        }
        .sortedBy { it.dueDate }
        .take(6)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        PageHeader(
            title = "Notifications",
            subtitle = "Register the device and surface future scheduled tasks from the admin workspace.",
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
                    title = "Upcoming reminders",
                    subtitle = "Tasks scheduled from today onward are surfaced here for mobile follow-up.",
                ) {
                    if (reminderItems.isEmpty()) {
                        EmptyState(
                            title = "No future tasks",
                            message = "Schedule tasks on future days and they will appear here.",
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            reminderItems.forEach { task ->
                                SectionCard(
                                    title = task.title,
                                    subtitle = "Scheduled for ${task.dueDate.orEmpty()}",
                                ) {}
                            }
                        }
                    }
                }
            }
        }
    }
}
