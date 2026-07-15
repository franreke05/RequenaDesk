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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.admin.screens.AdminClientsScreen
import com.requena.supportdesk.app.client.ClientPortalScreen
import com.requena.supportdesk.app.admin.screens.AdminDashboardScreen
import com.requena.supportdesk.app.admin.screens.AdminInvoicesScreen
import com.requena.supportdesk.app.admin.screens.AdminLoginScreen
import com.requena.supportdesk.app.admin.screens.AdminNotificationsScreen
import com.requena.supportdesk.app.admin.screens.AdminPinboardScreen
import com.requena.supportdesk.app.admin.screens.AdminTasksScreen
import com.requena.supportdesk.app.admin.screens.AdminTicketsScreen
import com.requena.supportdesk.app.admin.screens.AdminCreateTicketScreen
import com.requena.supportdesk.app.admin.screens.AdminTicketDetailScreen
import com.requena.supportdesk.core.navigation.AppDestination
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
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
import com.requena.supportdesk.features.invoices.presentation.effect.InvoicesUiEffect
import com.requena.supportdesk.features.invoices.presentation.state.InvoicesUiState
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import com.requena.supportdesk.features.tickets.presentation.effect.TicketsUiEffect
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState
import com.composables.icons.lucide.LayoutDashboard
import com.composables.icons.lucide.ListTodo
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pin
import com.composables.icons.lucide.ReceiptText
import com.composables.icons.lucide.Tags
import com.composables.icons.lucide.Ticket
import com.composables.icons.lucide.Users
import kotlinx.coroutines.launch

@Composable
fun AdminWorkspaceApp() {
    val module = remember { AdminAppModule() }
    var navigation by remember { mutableStateOf(AdminNavigationState()) }
    var statusMessage by remember { mutableStateOf("Workspace admin listo.") }

    val authState by module.authViewModel.state.collectAsState()
    val currentUser = authState.authenticatedUser
    val clientsState = if (currentUser != null) {
        module.clientsViewModel.state.collectAsState().value
    } else {
        ClientsUiState()
    }
    val tasksState = if (currentUser != null) {
        module.tasksViewModel.state.collectAsState().value
    } else {
        TasksUiState()
    }
    val invoicesState = if (currentUser != null) {
        module.invoicesViewModel.state.collectAsState().value
    } else {
        InvoicesUiState()
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
            module.clientsViewModel.effects.collect { effect ->
                when (effect) {
                    is ClientsUiEffect.ShowMessage -> statusMessage = effect.message
                }
            }
        }
    }

    LaunchedEffect(module, currentUser) {
        if (currentUser != null) {
            module.invoicesViewModel.effects.collect { effect ->
                when (effect) {
                    is InvoicesUiEffect.ShowMessage -> statusMessage = effect.message
                }
            }
        }
    }

    LaunchedEffect(module, currentUser) {
        if (currentUser != null) {
            module.ticketsViewModel.effects.collect { effect ->
                when (effect) {
                    is TicketsUiEffect.ShowMessage -> statusMessage = effect.message
                    is TicketsUiEffect.TicketSelected -> Unit
                    is TicketsUiEffect.TicketCreated -> {
                        if (currentUser.role == com.requena.supportdesk.core.model.UserRole.ADMIN) {
                            navigation = navigation.copy(destination = AppDestination.TicketDetail(effect.ticketId))
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(currentUser?.id) {
        currentUser?.id ?: return@LaunchedEffect

        navigation = navigation.copy(destination = AppDestination.Dashboard)
        statusMessage = "Sesion iniciada como ${currentUser.name}"

        module.clientsViewModel.onEvent(ClientsUiEvent.Load)
        module.ticketsViewModel.onEvent(TicketsUiEvent.Load)

        module.tasksViewModel.onEvent(TasksUiEvent.SelectTask(null))
        module.tasksViewModel.onEvent(TasksUiEvent.SelectCategory(null))
        module.tasksViewModel.onEvent(TasksUiEvent.SelectClientFilter(null))
        module.tasksViewModel.onEvent(TasksUiEvent.SelectDashboardClient(null))
        module.tasksViewModel.onEvent(TasksUiEvent.SelectDay(currentIsoDate()))
        module.tasksViewModel.onEvent(TasksUiEvent.Load)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val layoutMode = when {
            maxWidth < SupportDeskBreakpoints.adminCompact -> AdminLayoutMode.COMPACT
            maxWidth < SupportDeskBreakpoints.adminMedium -> AdminLayoutMode.MEDIUM
            else -> AdminLayoutMode.EXPANDED
        }
        if (currentUser == null) {
            AdminLoginScreen(
                state = authState,
                onEvent = module.authViewModel::onEvent,
            )
        } else if (currentUser.role == com.requena.supportdesk.core.model.UserRole.CLIENT) {
            ClientPortalScreen(
                clientName = currentUser.name,
                companyName = clientsState.clients.firstOrNull { it.id == currentUser.clientId }?.companyName.orEmpty(),
                client = clientsState.clients.firstOrNull { it.id == currentUser.clientId },
                state = module.ticketsViewModel.state.collectAsState().value,
                onEvent = module.ticketsViewModel::onEvent,
                onRefresh = { module.ticketsViewModel.onEvent(com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent.Load) },
                onSignOut = { module.authViewModel.onEvent(com.requena.supportdesk.features.auth.presentation.event.AuthUiEvent.Logout) },
                tasksState = tasksState,
                onTasksEvent = module.tasksViewModel::onEvent,
                clientId = currentUser.clientId,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val navItems = remember {
                listOf<NavigationItemSpec<AppDestination>>(
                    NavigationItemSpec(
                        key = AppDestination.Dashboard,
                        title = "Dashboard",
                        supportingText = "Tiempo, calendario y ruedas",
                        icon = Lucide.LayoutDashboard,
                    ),
                    NavigationItemSpec(
                        key = AppDestination.Clients,
                        title = "Clientes",
                        icon = Lucide.Users,
                        supportingText = "Directorio y ficha rápida",
                    ),
                    NavigationItemSpec(
                        key = AppDestination.Tasks,
                        title = "Tareas",
                        supportingText = "Trabajo, cliente y etiqueta",
                        icon = Lucide.ListTodo,
                    ),
                    NavigationItemSpec(
                        key = AppDestination.Pinboard,
                        title = "Tablon",
                        supportingText = "Chinchetas del dia",
                        icon = Lucide.Pin,
                    ),
                    NavigationItemSpec(
                        key = AppDestination.Tickets,
                        title = "Tickets",
                        supportingText = "Cola, respuestas y prioridad",
                        icon = Lucide.Ticket,
                    ),
                    NavigationItemSpec(
                        key = AppDestination.Labels,
                        title = "Etiquetas",
                        icon = Lucide.Tags,
                        supportingText = "Colores y organización",
                    ),
                    NavigationItemSpec(
                        key = AppDestination.Invoices(),
                        title = "Facturas",
                        supportingText = "Crear y gestionar pagos",
                        icon = Lucide.ReceiptText,
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
                        clientsState = clientsState,
                        tasksState = tasksState,
                        invoicesState = invoicesState,
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
                                clientsState = clientsState,
                                tasksState = tasksState,
                                invoicesState = invoicesState,
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
                            clientsState = clientsState,
                            tasksState = tasksState,
                            invoicesState = invoicesState,
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
    clientsState: com.requena.supportdesk.features.clients.presentation.state.ClientsUiState,
    tasksState: com.requena.supportdesk.features.tasks.presentation.state.TasksUiState,
    invoicesState: InvoicesUiState,
    ticketsState: TicketsUiState,
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
                onEvent = module.ticketsViewModel::onEvent,
                onOpenCreateTicket = { onNavigate(AppDestination.CreateTicket) },
                onOpenDetail = { ticket -> onNavigate(AppDestination.TicketDetail(ticket.id)) },
                modifier = Modifier.weight(1f),
            )

            AppDestination.CreateTicket -> AdminCreateTicketScreen(
                clients = clientsState.clients,
                isSubmitting = ticketsState.isLoading,
                errorMessage = ticketsState.errorMessage,
                onBack = { onNavigate(AppDestination.Tickets) },
                onCreateTicket = { input -> module.ticketsViewModel.onEvent(TicketsUiEvent.CreateTicket(input)) },
                modifier = Modifier.weight(1f),
            )

            is AppDestination.TicketDetail -> AdminTicketDetailScreen(
                ticket = ticketsState.tickets.firstOrNull { it.id == navigation.destination.ticketId }
                    ?: ticketsState.selectedTicket,
                onBack = { onNavigate(AppDestination.Tickets) },
                onEvent = module.ticketsViewModel::onEvent,
                modifier = Modifier.weight(1f),
            )

            AppDestination.Clients -> AdminClientsScreen(
                state = clientsState,
                tasksState = tasksState,
                ticketsState = ticketsState,
                currentAdminId = module.authViewModel.state.value.authenticatedUser?.id.orEmpty(),
                currentAdminName = module.authViewModel.state.value.authenticatedUser?.name.orEmpty(),
                onEvent = module.clientsViewModel::onEvent,
                onNavigateToInvoices = { clientId -> onNavigate(AppDestination.Invoices(preselectedClientId = clientId)) },
                onNavigateToLabels = { onNavigate(AppDestination.Labels) },
                modifier = Modifier.weight(1f),
            )

            AppDestination.Tasks -> AdminTasksScreen(
                clients = clientsState.clients,
                tasksState = tasksState,
                onTasksEvent = module.tasksViewModel::onEvent,
                modifier = Modifier.weight(1f),
            )

            AppDestination.Pinboard -> AdminPinboardScreen(
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

            is AppDestination.Invoices -> AdminInvoicesScreen(
                clients = clientsState.clients,
                tasksState = tasksState,
                state = invoicesState,
                onEvent = module.invoicesViewModel::onEvent,
                preselectedClientId = navigation.destination.preselectedClientId,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun navDestinationFor(destination: AppDestination): AppDestination = when (destination) {
    AppDestination.Dashboard -> AppDestination.Dashboard
    AppDestination.Clients -> AppDestination.Clients
    AppDestination.Tasks -> AppDestination.Tasks
    AppDestination.Pinboard -> AppDestination.Pinboard
    AppDestination.Labels,
    AppDestination.Notifications -> AppDestination.Labels
    is AppDestination.Invoices -> AppDestination.Invoices()
    AppDestination.Tickets,
    AppDestination.CreateTicket,
    is AppDestination.TicketDetail -> AppDestination.Tickets
    AppDestination.Login -> AppDestination.Dashboard
}

private fun titleFor(destination: AppDestination): String = when (destination) {
    AppDestination.Dashboard -> "Dashboard"
    AppDestination.Clients -> "Clientes"
    AppDestination.Tasks -> "Tareas"
    AppDestination.Pinboard -> "Tablon"
    AppDestination.Labels,
    AppDestination.Notifications -> "Etiquetas"
    is AppDestination.Invoices -> "Facturas"
    AppDestination.Tickets -> "Tickets"
    AppDestination.CreateTicket -> "Nuevo ticket"
    is AppDestination.TicketDetail -> "Detalle del ticket"
    AppDestination.Login -> "OryKai software Admin"
}

private fun subtitleFor(destination: AppDestination): String? = when (destination) {
    AppDestination.Dashboard -> null
    AppDestination.Clients -> "Consulta clientes y enlaza contexto sin mezclar trabajo."
    AppDestination.Tasks -> "Lista principal de trabajo, cliente asociado y etiquetas."
    AppDestination.Pinboard -> "Chinchetas con las tareas pendientes de hoy, organizadas por etiqueta."
    AppDestination.Labels,
    AppDestination.Notifications -> "Colores y nombres para estructurar el trabajo diario."
    is AppDestination.Invoices -> "Genera facturas en PDF para tus clientes."
    AppDestination.Tickets,
    AppDestination.CreateTicket,
    is AppDestination.TicketDetail -> "Gestiona solicitudes, conversacion, estado y prioridad."
    AppDestination.Login -> "Workspace admin para organizar trabajo compartido."
}
