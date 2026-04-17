package com.requena.supportdesk.android.app

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.android.di.AndroidAppModule
import com.requena.supportdesk.android.navigation.AndroidNavigationState
import com.requena.supportdesk.android.screens.auth.LoginScreen
import com.requena.supportdesk.android.screens.notifications.NotificationsScreen
import com.requena.supportdesk.android.screens.tickets.TicketDetailScreen
import com.requena.supportdesk.android.screens.tickets.TicketListScreen
import com.requena.supportdesk.android.theme.SupportDeskAndroidTheme
import com.requena.supportdesk.core.navigation.AppDestination
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.buttons.ThemeModeButton
import com.requena.supportdesk.designsystem.components.layout.TopBar
import com.requena.supportdesk.designsystem.theme.displaySubtitle
import com.requena.supportdesk.designsystem.theme.displayTitle
import com.requena.supportdesk.features.auth.presentation.effect.AuthUiEffect
import com.requena.supportdesk.features.notifications.presentation.effect.NotificationsUiEffect
import com.requena.supportdesk.features.notifications.presentation.event.NotificationsUiEvent
import com.requena.supportdesk.features.tickets.presentation.effect.TicketsUiEffect
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import kotlinx.coroutines.launch

@Composable
fun AndroidSupportDeskApp() {
    val module = remember { AndroidAppModule() }
    var navigation by remember { mutableStateOf(AndroidNavigationState()) }
    var statusMessage by remember { mutableStateOf("Android lite is ready for quick review.") }

    val authState by module.authViewModel.state.collectAsState()
    val ticketsState by module.ticketsViewModel.state.collectAsState()
    val notificationsState by module.notificationsViewModel.state.collectAsState()

    DisposableEffect(module) {
        onDispose { module.clear() }
    }

    LaunchedEffect(module) {
        launch {
            module.authViewModel.effects.collect { effect ->
                when (effect) {
                    is AuthUiEffect.NavigateToHome -> {
                        navigation = navigation.copy(destination = AppDestination.Tickets)
                        statusMessage = "Signed in as admin"
                    }
                    is AuthUiEffect.ShowMessage -> statusMessage = effect.message
                }
            }
        }
        launch {
            module.ticketsViewModel.effects.collect { effect ->
                when (effect) {
                    is TicketsUiEffect.ShowMessage -> statusMessage = effect.message
                    is TicketsUiEffect.TicketSelected -> statusMessage = "Ticket ${effect.ticketId} ready"
                }
            }
        }
        launch {
            module.notificationsViewModel.effects.collect { effect ->
                when (effect) {
                    is NotificationsUiEffect.ShowMessage -> statusMessage = effect.message
                }
            }
        }
    }

    SupportDeskAndroidTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        ),
                    ),
                ),
        ) {
            if (navigation.destination == AppDestination.Login) {
                LoginScreen(
                    state = authState,
                    onEvent = module.authViewModel::onEvent,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TopBar(
                        title = navigation.destination.displayTitle(),
                        subtitle = navigation.destination.displaySubtitle(),
                        statusText = statusMessage,
                        actions = {
                            SupportDeskBadge(
                                text = "Admin lite",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            ThemeModeButton()
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                            SecondaryButton(
                                text = "Sign out",
                                onClick = {
                                    navigation = AndroidNavigationState()
                                    statusMessage = "Session closed."
                                },
                            )
                        },
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        Crossfade(targetState = navigation.destination, label = "androidDestination") { destination ->
                            when (destination) {
                                AppDestination.Login -> LoginScreen(
                                    state = authState,
                                    onEvent = module.authViewModel::onEvent,
                                )
                                AppDestination.Tickets -> TicketListScreen(
                                    state = ticketsState,
                                    onEvent = module.ticketsViewModel::onEvent,
                                    onOpenDetail = {
                                        module.ticketsViewModel.onEvent(TicketsUiEvent.SelectTicket(it.id))
                                        navigation = navigation.copy(destination = AppDestination.TicketDetail(it.id))
                                    },
                                )
                                is AppDestination.TicketDetail -> TicketDetailScreen(
                                    ticket = ticketsState.selectedTicket,
                                    currentUserId = authState.authenticatedUser?.id,
                                    onBack = { navigation = navigation.copy(destination = AppDestination.Tickets) },
                                )
                                AppDestination.Notifications -> NotificationsScreen(
                                    state = notificationsState,
                                    onRegisterDevice = { module.notificationsViewModel.onEvent(NotificationsUiEvent.RegisterAdminDevice) },
                                )
                                AppDestination.Tasks,
                                AppDestination.Labels,
                                AppDestination.Dashboard,
                                AppDestination.CreateTicket,
                                AppDestination.Clients -> TicketListScreen(
                                    state = ticketsState,
                                    onEvent = module.ticketsViewModel::onEvent,
                                    onOpenDetail = {
                                        module.ticketsViewModel.onEvent(TicketsUiEvent.SelectTicket(it.id))
                                        navigation = navigation.copy(destination = AppDestination.TicketDetail(it.id))
                                    },
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val ticketsSelected = navigation.destination == AppDestination.Tickets || navigation.destination is AppDestination.TicketDetail
                        if (ticketsSelected) {
                            PrimaryButton(
                                text = "Tickets",
                                onClick = { navigation = navigation.copy(destination = AppDestination.Tickets) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            SecondaryButton(
                                text = "Tickets",
                                onClick = { navigation = navigation.copy(destination = AppDestination.Tickets) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (navigation.destination == AppDestination.Notifications) {
                            PrimaryButton(
                                text = "Notifications",
                                onClick = { navigation = navigation.copy(destination = AppDestination.Notifications) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            SecondaryButton(
                                text = "Notifications",
                                onClick = { navigation = navigation.copy(destination = AppDestination.Notifications) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
