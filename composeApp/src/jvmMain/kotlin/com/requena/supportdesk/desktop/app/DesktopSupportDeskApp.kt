package com.requena.supportdesk.desktop.app

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
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
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.navigation.AppDestination
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.buttons.ThemeModeButton
import com.requena.supportdesk.designsystem.components.feedback.ConfirmDialog
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.navigation.AppSidebar
import com.requena.supportdesk.designsystem.components.navigation.NavigationItemSpec
import com.requena.supportdesk.designsystem.components.layout.TopBar
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.designsystem.theme.displaySubtitle
import com.requena.supportdesk.designsystem.theme.displayTitle
import com.requena.supportdesk.desktop.di.DesktopAppModule
import com.requena.supportdesk.desktop.navigation.DesktopNavigationState
import com.requena.supportdesk.desktop.navigation.desktopHomeFor
import com.requena.supportdesk.desktop.screens.auth.LoginScreen
import com.requena.supportdesk.desktop.screens.clients.ClientListScreen
import com.requena.supportdesk.desktop.screens.dashboard.DashboardScreen
import com.requena.supportdesk.desktop.screens.tickets.CreateTicketScreen
import com.requena.supportdesk.desktop.screens.tickets.TicketListScreen
import com.requena.supportdesk.desktop.theme.SupportDeskDesktopTheme
import com.requena.supportdesk.features.auth.presentation.effect.AuthUiEffect
import com.requena.supportdesk.features.clients.presentation.effect.ClientsUiEffect
import com.requena.supportdesk.features.dashboard.presentation.effect.DashboardUiEffect
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import com.requena.supportdesk.features.tickets.presentation.effect.TicketsUiEffect
import kotlinx.coroutines.launch

@Composable
fun DesktopSupportDeskApp() {
    val module = remember { DesktopAppModule() }
    var navigation by remember { mutableStateOf(DesktopNavigationState()) }
    var statusMessage by remember { mutableStateOf("Desktop workspace ready.") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val authState by module.authViewModel.state.collectAsState()
    val ticketsState by module.ticketsViewModel.state.collectAsState()
    val clientsState by module.clientsViewModel.state.collectAsState()
    val dashboardState by module.dashboardViewModel.state.collectAsState()

    DisposableEffect(module) {
        onDispose { module.clear() }
    }

    LaunchedEffect(module) {
        launch {
            module.authViewModel.effects.collect { effect ->
                when (effect) {
                    is AuthUiEffect.NavigateToHome -> {
                        navigation = navigation.copy(
                            role = UserRole.ADMIN,
                            destination = desktopHomeFor(UserRole.ADMIN),
                        )
                        statusMessage = "Signed in as ${UserRole.ADMIN.displayName()}"
                    }
                    is AuthUiEffect.ShowMessage -> statusMessage = effect.message
                }
            }
        }
        launch {
            module.ticketsViewModel.effects.collect { effect ->
                when (effect) {
                    is TicketsUiEffect.ShowMessage -> statusMessage = effect.message
                    is TicketsUiEffect.TicketSelected -> statusMessage = "Ticket ${effect.ticketId} selected"
                }
            }
        }
        launch {
            module.clientsViewModel.effects.collect { effect ->
                when (effect) {
                    is ClientsUiEffect.ShowMessage -> statusMessage = effect.message
                }
            }
        }
        launch {
            module.dashboardViewModel.effects.collect { effect ->
                when (effect) {
                    is DashboardUiEffect.ShowMessage -> statusMessage = effect.message
                }
            }
        }
    }

    SupportDeskDesktopTheme {
        val isLoggedIn = navigation.role != null && navigation.destination != AppDestination.Login
        val currentClientProduct = clientsState.clients.firstOrNull { it.id == authState.authenticatedUser?.clientId }?.productName
            ?: "Assigned product"
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        ),
                    ),
                ),
        ) {
            Crossfade(targetState = isLoggedIn, label = "desktopSession") { loggedIn ->
                if (!loggedIn) {
                    LoginScreen(
                        state = authState,
                        onEvent = module.authViewModel::onEvent,
                    )
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AppSidebar(
                            brandTitle = "RequenaDesk",
                            brandSubtitle = if (navigation.role == UserRole.ADMIN) {
                                "Admin workspace for freelance support."
                            } else {
                                "Client workspace to open and track tickets."
                            },
                            items = desktopSidebarItems(navigation.role ?: UserRole.CLIENT),
                            selected = sidebarDestinationFor(navigation.destination),
                            onSelect = { navigation = navigation.copy(destination = it) },
                            footer = {
                                SecondaryButton(
                                    text = "Sign out",
                                    onClick = { showLogoutDialog = true },
                                    fullWidth = true,
                                )
                            },
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .padding(24.dp),
                        ) {
                            DesktopWorkspace(
                                modifier = Modifier.fillMaxSize(),
                                title = navigation.destination.displayTitle(),
                                subtitle = navigation.destination.displaySubtitle(),
                                role = navigation.role,
                                statusMessage = statusMessage,
                                onLogout = { showLogoutDialog = true },
                            ) {
                                Crossfade(targetState = navigation.destination, label = "desktopDestination") { destination ->
                                    when (destination) {
                                        AppDestination.Login -> LoginScreen(
                                            state = authState,
                                            onEvent = module.authViewModel::onEvent,
                                        )
                                        AppDestination.Dashboard -> DashboardScreen(
                                            summary = dashboardState.summary,
                                            recentTickets = prioritizedTickets(ticketsState.tickets),
                                            isLoading = dashboardState.isLoading,
                                            errorMessage = dashboardState.errorMessage,
                                            onOpenTickets = { navigation = navigation.copy(destination = AppDestination.Tickets) },
                                            onOpenClients = { navigation = navigation.copy(destination = AppDestination.Clients) },
                                            onOpenTicket = {
                                                module.ticketsViewModel.onEvent(TicketsUiEvent.SelectTicket(it.id))
                                                navigation = navigation.copy(destination = AppDestination.Tickets)
                                            },
                                        )
                                        AppDestination.Tickets,
                                        is AppDestination.TicketDetail -> TicketListScreen(
                                            state = ticketsState,
                                            role = navigation.role ?: UserRole.CLIENT,
                                            currentUserId = authState.authenticatedUser?.id,
                                            onEvent = module.ticketsViewModel::onEvent,
                                            onCreateTicket = { navigation = navigation.copy(destination = AppDestination.CreateTicket) },
                                            onOpenDetail = {
                                                module.ticketsViewModel.onEvent(TicketsUiEvent.SelectTicket(it.id))
                                                navigation = navigation.copy(destination = AppDestination.TicketDetail(it.id))
                                            },
                                            onReply = { message ->
                                                module.ticketsViewModel.onEvent(TicketsUiEvent.ReplyToSelected(message))
                                            },
                                            onChangeStatus = { module.ticketsViewModel.onEvent(TicketsUiEvent.ChangeSelectedStatus(it)) },
                                            onChangePriority = { module.ticketsViewModel.onEvent(TicketsUiEvent.ChangeSelectedPriority(it)) },
                                        )
                                        AppDestination.CreateTicket -> CreateTicketScreen(
                                            affectedApp = currentClientProduct,
                                            onBack = { navigation = navigation.copy(destination = AppDestination.Tickets) },
                                            onCreateTicket = { input ->
                                                module.ticketsViewModel.onEvent(TicketsUiEvent.CreateTicket(input))
                                                navigation = navigation.copy(destination = AppDestination.Tickets)
                                                statusMessage = "Ticket draft created"
                                            },
                                        )
                                        AppDestination.Clients -> ClientListScreen(
                                            state = clientsState,
                                            onEvent = module.clientsViewModel::onEvent,
                                        )
                                        AppDestination.Tasks,
                                        AppDestination.Labels,
                                        AppDestination.Notifications -> EmptyState(
                                            title = "Mobile-only notifications",
                                            message = "Push registration and quick admin alerts stay in the Android lite app.",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ConfirmDialog(
                visible = showLogoutDialog,
                title = "Sign out from RequenaDesk?",
                message = "Your current desktop session will close and the app will return to the login screen.",
                confirmText = "Sign out",
                onConfirm = {
                    showLogoutDialog = false
                    navigation = DesktopNavigationState()
                    statusMessage = "Session closed."
                },
                onDismiss = { showLogoutDialog = false },
            )
        }
    }
}

@Composable
private fun DesktopWorkspace(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    role: UserRole?,
    statusMessage: String,
    onLogout: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                TopBar(
                    title = title,
                    subtitle = subtitle,
                    statusText = statusMessage,
                    actions = {
                        role?.let {
                            SupportDeskBadge(
                                text = it.displayName(),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                        }
                        ThemeModeButton()
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                        SecondaryButton(text = "Sign out", onClick = onLogout)
                    },
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp),
                    content = content,
                )
            }
        }
    }
}

private fun desktopSidebarItems(role: UserRole): List<NavigationItemSpec<AppDestination>> {
    val commonItems: List<NavigationItemSpec<AppDestination>> = listOf(
        NavigationItemSpec<AppDestination>(
            key = AppDestination.Tickets,
            title = "Tickets",
            supportingText = "Inbox, queue and active conversations",
        ),
    )
    return if (role == UserRole.ADMIN) {
        buildList {
            add(
                NavigationItemSpec<AppDestination>(
                    key = AppDestination.Dashboard,
                    title = "Dashboard",
                    supportingText = "Operational metrics and urgent work",
                ),
            )
            addAll(commonItems)
            add(
                NavigationItemSpec<AppDestination>(
                    key = AppDestination.Clients,
                    title = "Clients",
                    supportingText = "Accounts, contacts and open load",
                ),
            )
        }
    } else {
        commonItems
    }
}

private fun sidebarDestinationFor(destination: AppDestination): AppDestination = when (destination) {
    AppDestination.Dashboard -> AppDestination.Dashboard
    AppDestination.Clients -> AppDestination.Clients
    AppDestination.Tasks -> AppDestination.Tickets
    AppDestination.Labels -> AppDestination.Tickets
    AppDestination.Tickets,
    AppDestination.CreateTicket,
    is AppDestination.TicketDetail -> AppDestination.Tickets
    AppDestination.Login,
    AppDestination.Notifications -> AppDestination.Tickets
}

private fun prioritizedTickets(tickets: List<com.requena.supportdesk.core.model.Ticket>): List<com.requena.supportdesk.core.model.Ticket> =
    tickets.sortedWith(
        compareByDescending<com.requena.supportdesk.core.model.Ticket> { it.priority == com.requena.supportdesk.core.model.TicketPriority.URGENT }
            .thenByDescending { it.priority == com.requena.supportdesk.core.model.TicketPriority.HIGH }
            .thenByDescending { it.updatedAt },
    ).take(4)
