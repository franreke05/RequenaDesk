package com.requena.supportdesk.app.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import com.requena.supportdesk.app.admin.screens.AdminTicketDetailScreen
import com.requena.supportdesk.core.navigation.AppDestination
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.buttons.ThemeModeButton
import com.requena.supportdesk.designsystem.components.navigation.AdminBottomBar
import com.requena.supportdesk.designsystem.components.navigation.AdminNavigationRail
import com.requena.supportdesk.designsystem.components.navigation.AppSidebar
import com.requena.supportdesk.designsystem.components.navigation.NavigationItemSpec
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.features.auth.presentation.effect.AuthUiEffect
import com.requena.supportdesk.features.auth.presentation.event.AuthUiEvent
import com.requena.supportdesk.features.business.finance.presentation.BusinessFinanceUiEffect
import com.requena.supportdesk.features.business.operations.OperationsUiEffect
import com.requena.supportdesk.features.business.sales.presentation.BusinessSalesUiEffect
import com.requena.supportdesk.features.clients.presentation.effect.ClientsUiEffect
import com.requena.supportdesk.features.clients.presentation.event.ClientsUiEvent
import com.requena.supportdesk.features.clients.presentation.state.ClientsUiState
import com.requena.supportdesk.features.invoices.presentation.effect.InvoicesUiEffect
import com.requena.supportdesk.features.invoices.presentation.state.InvoicesUiState
import com.requena.supportdesk.features.programs.presentation.effect.ProgramsUiEffect
import com.requena.supportdesk.features.programs.presentation.event.ProgramsUiEvent
import com.requena.supportdesk.features.programs.presentation.state.ProgramsUiState
import com.requena.supportdesk.features.tasks.presentation.effect.TasksUiEffect
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
    // Collected here (the only place with simultaneous access to all 9 client-portal-relevant
    // ViewModels via `module`), rendered inside ClientPortalScreen (the only place that knows
    // the content panel's actual bounds, as opposed to the full window behind the sidebar).
    val clientSnackbarHostState = remember { SnackbarHostState() }

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
    val programsState = if (currentUser != null) {
        module.programsViewModel.state.collectAsState().value
    } else {
        ProgramsUiState()
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
                        statusMessage = "Sesion iniciada."
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
                    is TicketsUiEffect.ShowMessage -> {
                        statusMessage = effect.message
                        // TicketsViewModel is shared with the client portal; only show the
                        // snackbar for a client session (ClientPortalScreen is the only place
                        // that mounts a SnackbarHost for this state - calling showSnackbar
                        // without one composed would suspend forever and stall this collector).
                        if (currentUser.role == UserRole.CLIENT) {
                            clientSnackbarHostState.showSnackbar(effect.message)
                        }
                    }
                    // Ticket creation now happens from a dialog on top of the Tickets screen
                    // itself; the ViewModel already selects the new ticket (renderTickets(selectId
                    // = ...) in createTicket()), so the list/detail panes update in place without
                    // forcing navigation away from wherever the admin already is.
                    is TicketsUiEffect.TicketCreated -> Unit
                }
            }
        }
    }

    // The following 8 collectors are gated to CLIENT sessions only (not just non-null, unlike
    // the 4 above): every one of these ViewModels' effects is unread by any admin screen today,
    // and ClientPortalScreen is the only place a SnackbarHost is actually mounted - collecting
    // for an admin session would call showSnackbar with nothing composed to dismiss it, stalling
    // the collector on its first message.
    LaunchedEffect(module, currentUser) {
        if (currentUser?.role == UserRole.CLIENT) {
            module.tasksViewModel.effects.collect { effect ->
                when (effect) {
                    is TasksUiEffect.ShowMessage -> clientSnackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    LaunchedEffect(module, currentUser) {
        if (currentUser?.role == UserRole.CLIENT) {
            module.programsViewModel.effects.collect { effect ->
                when (effect) {
                    is ProgramsUiEffect.ShowMessage -> clientSnackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    LaunchedEffect(module, currentUser) {
        if (currentUser?.role == UserRole.CLIENT) {
            module.businessInvoicingViewModel.effects.collect { effect ->
                when (effect) {
                    is BusinessFinanceUiEffect.ShowMessage -> clientSnackbarHostState.showSnackbar(effect.message)
                    // Access-denied already drives a persistent EmptyState in the workspace
                    // itself (state.accessDenied) - a toast on top would just duplicate it.
                    BusinessFinanceUiEffect.AccessDenied -> Unit
                }
            }
        }
    }

    LaunchedEffect(module, currentUser) {
        if (currentUser?.role == UserRole.CLIENT) {
            module.businessAccountingViewModel.effects.collect { effect ->
                when (effect) {
                    is BusinessFinanceUiEffect.ShowMessage -> clientSnackbarHostState.showSnackbar(effect.message)
                    BusinessFinanceUiEffect.AccessDenied -> Unit
                }
            }
        }
    }

    LaunchedEffect(module, currentUser) {
        if (currentUser?.role == UserRole.CLIENT) {
            module.operationsViewModel.effects.collect { effect ->
                when (effect) {
                    is OperationsUiEffect.ShowMessage -> clientSnackbarHostState.showSnackbar(effect.text)
                }
            }
        }
    }

    LaunchedEffect(module, currentUser) {
        if (currentUser?.role == UserRole.CLIENT) {
            module.businessCustomersViewModel.effects.collect { effect ->
                when (effect) {
                    is BusinessSalesUiEffect.ShowMessage -> clientSnackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    LaunchedEffect(module, currentUser) {
        if (currentUser?.role == UserRole.CLIENT) {
            module.businessCatalogViewModel.effects.collect { effect ->
                when (effect) {
                    is BusinessSalesUiEffect.ShowMessage -> clientSnackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    LaunchedEffect(module, currentUser) {
        if (currentUser?.role == UserRole.CLIENT) {
            module.businessQuotesViewModel.effects.collect { effect ->
                when (effect) {
                    is BusinessSalesUiEffect.ShowMessage -> clientSnackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    LaunchedEffect(currentUser?.id) {
        currentUser?.id ?: return@LaunchedEffect

        navigation = navigation.copy(destination = AppDestination.Dashboard)
        statusMessage = if (currentUser.role == com.requena.supportdesk.core.model.UserRole.CLIENT) {
            "Bienvenido al portal de cliente, ${currentUser.name}."
        } else {
            "Sesion iniciada como ${currentUser.name}."
        }

        module.clientsViewModel.onEvent(ClientsUiEvent.Load)
        module.ticketsViewModel.onEvent(TicketsUiEvent.Load)
        if (currentUser.role == com.requena.supportdesk.core.model.UserRole.CLIENT) {
            module.programsViewModel.onEvent(ProgramsUiEvent.RefreshClientPrograms)
        } else {
            module.programsViewModel.onEvent(ProgramsUiEvent.RefreshAdminRequests)
        }

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
                programsState = programsState,
                onProgramsEvent = module.programsViewModel::onEvent,
                businessInvoicingViewModel = module.businessInvoicingViewModel,
                businessAccountingViewModel = module.businessAccountingViewModel,
                operationsViewModel = module.operationsViewModel,
                businessCustomersViewModel = module.businessCustomersViewModel,
                businessCatalogViewModel = module.businessCatalogViewModel,
                businessQuotesViewModel = module.businessQuotesViewModel,
                clientId = currentUser.clientId,
                snackbarHostState = clientSnackbarHostState,
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
                        programsState = programsState,
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
                                programsState = programsState,
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
                            programsState = programsState,
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
    programsState: ProgramsUiState,
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

        AnimatedContent(
            targetState = navigation.destination,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            transitionSpec = {
                (fadeIn(tween(SupportDeskMotion.regular)) +
                    slideInVertically(tween(SupportDeskMotion.page)) { it / 18 }) togetherWith
                    (fadeOut(tween(SupportDeskMotion.quick)) +
                        slideOutVertically(tween(SupportDeskMotion.quick)) { -it / 24 })
            },
            label = "adminDestination",
        ) { destination ->
            when (destination) {
            AppDestination.Dashboard -> AdminDashboardScreen(
                clients = clientsState.clients,
                tasksState = tasksState,
                onTasksEvent = module.tasksViewModel::onEvent,
                modifier = Modifier.fillMaxSize(),
            )

            AppDestination.Tickets -> AdminTicketsScreen(
                layoutMode = layoutMode,
                state = ticketsState,
                clients = clientsState.clients,
                onEvent = module.ticketsViewModel::onEvent,
                onOpenDetail = { ticket -> onNavigate(AppDestination.TicketDetail(ticket.id)) },
                modifier = Modifier.fillMaxSize(),
            )

            is AppDestination.TicketDetail -> AdminTicketDetailScreen(
                ticket = ticketsState.tickets.firstOrNull { it.id == destination.ticketId }
                    ?: ticketsState.selectedTicket,
                clients = clientsState.clients,
                onBack = { onNavigate(AppDestination.Tickets) },
                onEvent = module.ticketsViewModel::onEvent,
                modifier = Modifier.fillMaxSize(),
            )

            AppDestination.Clients -> AdminClientsScreen(
                state = clientsState,
                tasksState = tasksState,
                ticketsState = ticketsState,
                programsState = programsState,
                currentAdminId = module.authViewModel.state.value.authenticatedUser?.id.orEmpty(),
                currentAdminName = module.authViewModel.state.value.authenticatedUser?.name.orEmpty(),
                onEvent = module.clientsViewModel::onEvent,
                onProgramsEvent = module.programsViewModel::onEvent,
                onNavigateToInvoices = { clientId -> onNavigate(AppDestination.Invoices(preselectedClientId = clientId)) },
                onNavigateToLabels = { onNavigate(AppDestination.Labels) },
                modifier = Modifier.fillMaxSize(),
            )

            AppDestination.Tasks -> AdminTasksScreen(
                clients = clientsState.clients,
                tasksState = tasksState,
                onTasksEvent = module.tasksViewModel::onEvent,
                modifier = Modifier.fillMaxSize(),
            )

            AppDestination.Pinboard -> AdminPinboardScreen(
                clients = clientsState.clients,
                tasksState = tasksState,
                onTasksEvent = module.tasksViewModel::onEvent,
                modifier = Modifier.fillMaxSize(),
            )

            AppDestination.Labels,
            AppDestination.Notifications -> AdminNotificationsScreen(
                tasksState = tasksState,
                onTasksEvent = module.tasksViewModel::onEvent,
                modifier = Modifier.fillMaxSize(),
            )

            AppDestination.Login -> AdminLoginScreen(
                state = module.authViewModel.state.value,
                onEvent = module.authViewModel::onEvent,
                modifier = Modifier.fillMaxSize(),
            )

            is AppDestination.Invoices -> AdminInvoicesScreen(
                clients = clientsState.clients,
                tasksState = tasksState,
                state = invoicesState,
                programBillingPreview = programsState.billingPreview,
                onLoadProgramBillingPreview = { clientId, period ->
                    module.programsViewModel.onEvent(ProgramsUiEvent.LoadBillingPreview(clientId, period))
                },
                onEvent = module.invoicesViewModel::onEvent,
                preselectedClientId = destination.preselectedClientId,
                modifier = Modifier.fillMaxSize(),
            )
                }
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
    is AppDestination.TicketDetail -> "Gestiona solicitudes, conversacion, estado y prioridad."
    AppDestination.Login -> "Workspace admin para organizar trabajo compartido."
}
