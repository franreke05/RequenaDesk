package com.requena.supportdesk.app.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.admin.screens.AdminClientsScreen
import com.requena.supportdesk.app.admin.screens.AdminCreateTicketScreen
import com.requena.supportdesk.app.admin.screens.AdminDashboardScreen
import com.requena.supportdesk.app.admin.screens.AdminLoginScreen
import com.requena.supportdesk.app.admin.screens.AdminNotificationsScreen
import com.requena.supportdesk.app.admin.screens.AdminTicketDetailScreen
import com.requena.supportdesk.app.admin.screens.AdminTicketsScreen
import com.requena.supportdesk.app.admin.screens.AdminTasksScreen
import com.requena.supportdesk.app.client.ClientPortalScreen
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.navigation.AppDestination
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.buttons.ThemeModeButton
import com.requena.supportdesk.designsystem.components.navigation.AdminBottomBar
import com.requena.supportdesk.designsystem.components.navigation.AdminNavigationRail
import com.requena.supportdesk.designsystem.components.navigation.AppSidebar
import com.requena.supportdesk.designsystem.components.navigation.NavigationItemSpec
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.auth.presentation.effect.AuthUiEffect
import com.requena.supportdesk.features.auth.presentation.event.AuthUiEvent
import com.requena.supportdesk.features.clients.presentation.effect.ClientsUiEffect
import com.requena.supportdesk.features.clients.presentation.event.ClientsUiEvent
import com.requena.supportdesk.features.clients.presentation.state.ClientsUiState
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import com.requena.supportdesk.features.tickets.presentation.effect.TicketsUiEffect
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState
import kotlinx.coroutines.launch

@Composable
fun AdminWorkspaceApp() {
    val module = remember { AdminAppModule() }
    var navigation by remember { mutableStateOf(AdminNavigationState()) }
    var statusMessage by remember { mutableStateOf("Workspace admin listo.") }

    val authState by module.authViewModel.state.collectAsState()
    val currentUser = authState.authenticatedUser
    val isAdmin = currentUser?.role == UserRole.ADMIN
    val clientsState = if (isAdmin) {
        module.clientsViewModel.state.collectAsState().value
    } else {
        ClientsUiState()
    }
    val tasksState = if (isAdmin) {
        module.tasksViewModel.state.collectAsState().value
    } else {
        TasksUiState()
    }
    val ticketsState = if (currentUser != null) {
        module.ticketsViewModel.state.collectAsState().value
    } else {
        TicketsUiState()
    }

    DisposableEffect(module) {
        onDispose { module.clear() }
    }

    LaunchedEffect(module) {
        launch {
            module.authViewModel.effects.collect { effect ->
                when (effect) {
                    AuthUiEffect.NavigateToHome -> {
                        navigation = navigation.copy(destination = AppDestination.Dashboard)
                        statusMessage = "Sesion iniciada como admin"
                    }
                    is AuthUiEffect.ShowMessage -> statusMessage = effect.message
                }
            }
        }
    }

    LaunchedEffect(module, currentUser) {
        if (currentUser != null) {
            launch {
                module.clientsViewModel.effects.collect { effect ->
                    when (effect) {
                        is ClientsUiEffect.ShowMessage -> statusMessage = effect.message
                    }
                }
            }
            launch {
                module.ticketsViewModel.effects.collect { effect ->
                    when (effect) {
                        is TicketsUiEffect.ShowMessage -> statusMessage = effect.message
                        is TicketsUiEffect.TicketSelected -> navigation = navigation.copy(destination = AppDestination.TicketDetail(effect.ticketId))
                    }
                }
            }
        }
    }

    LaunchedEffect(currentUser?.id) {
        currentUser?.id ?: return@LaunchedEffect

        if (currentUser.role == UserRole.CLIENT) {
            navigation = navigation.copy(destination = AppDestination.Tickets)
            statusMessage = "Sesion iniciada como ${currentUser.name}"
            module.ticketsViewModel.onEvent(TicketsUiEvent.Load)
            return@LaunchedEffect
        }

        navigation = navigation.copy(destination = AppDestination.Dashboard)
        statusMessage = "Sesion iniciada como ${currentUser.name}"

        module.clientsViewModel.onEvent(ClientsUiEvent.Load)

        module.tasksViewModel.onEvent(TasksUiEvent.SelectTask(null))
        module.tasksViewModel.onEvent(TasksUiEvent.SelectCategory(null))
        module.tasksViewModel.onEvent(TasksUiEvent.SelectClientFilter(null))
        module.tasksViewModel.onEvent(TasksUiEvent.SelectDashboardClient(null))
        module.tasksViewModel.onEvent(TasksUiEvent.SelectDay(currentIsoDate()))
        module.tasksViewModel.onEvent(TasksUiEvent.Load)
        module.ticketsViewModel.onEvent(TicketsUiEvent.Load)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        val layoutMode = when {
            maxWidth < 760.dp -> AdminLayoutMode.COMPACT
            maxWidth < 1180.dp -> AdminLayoutMode.MEDIUM
            else -> AdminLayoutMode.EXPANDED
        }
        if (currentUser == null) {
            AdminLoginScreen(
                state = authState,
                onEvent = module.authViewModel::onEvent,
            )
        } else if (currentUser.role == UserRole.CLIENT) {
            ClientPortalScreen(
                clientName = currentUser.name,
                state = ticketsState,
                onEvent = module.ticketsViewModel::onEvent,
                onRefresh = { module.ticketsViewModel.onEvent(TicketsUiEvent.Load) },
                onSignOut = { module.authViewModel.onEvent(AuthUiEvent.Logout) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val navItems = remember {
                listOf<NavigationItemSpec<AppDestination>>(
                    NavigationItemSpec(
                        key = AppDestination.Dashboard,
                        title = "Dashboard",
                        supportingText = "Tiempo, calendario y ruedas",
                    ),
                    NavigationItemSpec(
                        key = AppDestination.Clients,
                        title = "Clientes",
                        supportingText = "Directorio y ficha rápida",
                    ),
                    NavigationItemSpec(
                        key = AppDestination.Tasks,
                        title = "Tareas",
                        supportingText = "Trabajo, cliente y etiqueta",
                    ),
                    NavigationItemSpec(
                        key = AppDestination.Tickets,
                        title = "Tickets",
                        supportingText = "Tablero, chat y prioridad",
                    ),
                    NavigationItemSpec(
                        key = AppDestination.Labels,
                        title = "Etiquetas",
                        supportingText = "Colores y organización",
                    ),
                )
            }
            val selectedNav = navDestinationFor(navigation.destination)
            val uiStatus = tasksState.statusMessage ?: statusMessage

            if (layoutMode == AdminLayoutMode.EXPANDED) {
                Row(modifier = Modifier.fillMaxSize()) {
                    AppSidebar(
                        brandTitle = "OryKai software",
                        brandSubtitle = "Agenda compartida para clientes, tareas y horas.",
                        items = navItems,
                        selected = selectedNav,
                        onSelect = { navigation = navigation.copy(destination = it) },
                        footer = {
                            SecondaryButton(
                                text = "Cerrar sesion",
                                onClick = { module.authViewModel.onEvent(AuthUiEvent.Logout) },
                                fullWidth = true,
                            )
                        },
                    )
                    AdminContentArea(
                        layoutMode = layoutMode,
                        statusMessage = uiStatus,
                        navigation = navigation,
                        onNavigate = { destination -> navigation = navigation.copy(destination = destination) },
                        onSignOut = { module.authViewModel.onEvent(AuthUiEvent.Logout) },
                        currentAdminId = currentUser.id,
                        currentAdminName = currentUser.name,
                        clientsState = clientsState,
                        tasksState = tasksState,
                        ticketsState = ticketsState,
                        module = module,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (layoutMode == AdminLayoutMode.MEDIUM) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            AdminNavigationRail(
                                items = navItems,
                                selected = selectedNav,
                                onSelect = { navigation = navigation.copy(destination = it) },
                            )
                            AdminContentArea(
                                layoutMode = layoutMode,
                                statusMessage = uiStatus,
                                navigation = navigation,
                                onNavigate = { destination -> navigation = navigation.copy(destination = destination) },
                                onSignOut = { module.authViewModel.onEvent(AuthUiEvent.Logout) },
                                currentAdminId = currentUser.id,
                                currentAdminName = currentUser.name,
                                clientsState = clientsState,
                                tasksState = tasksState,
                                ticketsState = ticketsState,
                                module = module,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        AdminContentArea(
                            layoutMode = layoutMode,
                            statusMessage = uiStatus,
                            navigation = navigation,
                            onNavigate = { destination -> navigation = navigation.copy(destination = destination) },
                            onSignOut = { module.authViewModel.onEvent(AuthUiEvent.Logout) },
                            currentAdminId = currentUser.id,
                            currentAdminName = currentUser.name,
                            clientsState = clientsState,
                            tasksState = tasksState,
                            ticketsState = ticketsState,
                            module = module,
                            modifier = Modifier.weight(1f),
                        )
                        AdminBottomBar(
                            items = navItems,
                            selected = selectedNav,
                            onSelect = { navigation = navigation.copy(destination = it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminContentArea(
    layoutMode: AdminLayoutMode,
    statusMessage: String,
    navigation: AdminNavigationState,
    onNavigate: (AppDestination) -> Unit,
    onSignOut: () -> Unit,
    currentAdminId: String,
    currentAdminName: String,
    clientsState: com.requena.supportdesk.features.clients.presentation.state.ClientsUiState,
    tasksState: com.requena.supportdesk.features.tasks.presentation.state.TasksUiState,
    ticketsState: com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState,
    module: AdminAppModule,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = if (layoutMode == AdminLayoutMode.COMPACT) spacing.md else spacing.xl, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        if (layoutMode == AdminLayoutMode.COMPACT) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text(
                    text = titleFor(navigation.destination),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                subtitleFor(navigation.destination)?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    SupportDeskBadge(
                        text = statusMessage,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    ThemeModeButton()
                    SecondaryButton(text = "Cerrar sesion", onClick = onSignOut)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                    Text(
                        text = titleFor(navigation.destination),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    subtitleFor(navigation.destination)?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SupportDeskBadge(
                    text = statusMessage,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                ThemeModeButton()
                SecondaryButton(text = "Cerrar sesion", onClick = onSignOut)
            }
        }

        when (navigation.destination) {
            AppDestination.Dashboard -> AdminDashboardScreen(
                clients = clientsState.clients,
                tasksState = tasksState,
                onTasksEvent = module.tasksViewModel::onEvent,
                modifier = Modifier.weight(1f),
            )

            AppDestination.Tickets -> AdminTicketsScreen(
                layoutMode = layoutMode,
                state = ticketsState,
                currentAdminId = currentAdminId,
                currentAdminName = currentAdminName,
                onEvent = module.ticketsViewModel::onEvent,
                onOpenCreateTicket = { onNavigate(AppDestination.CreateTicket) },
                onOpenDetail = { ticket -> onNavigate(AppDestination.TicketDetail(ticket.id)) },
                modifier = Modifier.weight(1f),
            )

            AppDestination.CreateTicket -> AdminCreateTicketScreen(
                clients = clientsState.clients,
                onBack = { onNavigate(AppDestination.Tickets) },
                onCreateTicket = { input ->
                    module.ticketsViewModel.onEvent(TicketsUiEvent.CreateTicket(input))
                    onNavigate(AppDestination.Tickets)
                },
                modifier = Modifier.weight(1f),
            )

            is AppDestination.TicketDetail -> AdminTicketDetailScreen(
                ticket = ticketsState.selectedTicket,
                currentAdminId = currentAdminId,
                currentAdminName = currentAdminName,
                onBack = { onNavigate(AppDestination.Tickets) },
                onEvent = module.ticketsViewModel::onEvent,
                modifier = Modifier.weight(1f),
            )

            AppDestination.Clients -> AdminClientsScreen(
                state = clientsState,
                tasksState = tasksState,
                onEvent = module.clientsViewModel::onEvent,
                modifier = Modifier.weight(1f),
            )

            AppDestination.Tasks -> AdminTasksScreen(
                clients = clientsState.clients,
                tasksState = tasksState,
                onTasksEvent = module.tasksViewModel::onEvent,
                modifier = Modifier.weight(1f),
            )

            AppDestination.Labels,
            AppDestination.Notifications -> AdminNotificationsScreen(
                tasksState = tasksState,
                onTasksEvent = module.tasksViewModel::onEvent,
                modifier = Modifier.weight(1f),
            )

            AppDestination.Login -> AdminLoginScreen(
                state = module.authViewModel.state.value,
                onEvent = module.authViewModel::onEvent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun navDestinationFor(destination: AppDestination): AppDestination = when (destination) {
    AppDestination.Dashboard -> AppDestination.Dashboard
    AppDestination.Clients -> AppDestination.Clients
    AppDestination.Tasks -> AppDestination.Tasks
    AppDestination.Labels,
    AppDestination.Notifications -> AppDestination.Labels
    AppDestination.Tickets,
    AppDestination.CreateTicket,
    is AppDestination.TicketDetail -> AppDestination.Tickets
    AppDestination.Login -> AppDestination.Dashboard
}

private fun titleFor(destination: AppDestination): String = when (destination) {
    AppDestination.Dashboard -> "Dashboard"
    AppDestination.Clients -> "Clientes"
    AppDestination.Tasks -> "Tareas"
    AppDestination.Labels,
    AppDestination.Notifications -> "Etiquetas"
    AppDestination.Tickets -> "Tickets"
    AppDestination.CreateTicket -> "Nuevo ticket"
    is AppDestination.TicketDetail -> "Detalle ticket"
    AppDestination.Login -> "OryKai software Admin"
}

private fun subtitleFor(destination: AppDestination): String? = when (destination) {
    AppDestination.Dashboard -> null
    AppDestination.Clients -> "Consulta clientes y enlaza contexto sin mezclar trabajo."
    AppDestination.Tasks -> "Lista principal de trabajo, cliente asociado y etiquetas."
    AppDestination.Labels,
    AppDestination.Notifications -> "Colores y nombres para estructurar el trabajo diario."
    AppDestination.Tickets,
    AppDestination.CreateTicket,
    is AppDestination.TicketDetail -> "Tablero de soporte, prioridad y chat por cliente."
    AppDestination.Login -> "Workspace admin para organizar trabajo compartido."
}
