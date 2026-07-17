package com.requena.supportdesk.app.client

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.client.screens.ClientAccountScreen
import com.requena.supportdesk.app.client.screens.ClientActivityScreen
import com.requena.supportdesk.app.client.screens.ClientBoardScreen
import com.requena.supportdesk.app.client.screens.ClientHomeScreen
import com.requena.supportdesk.app.client.screens.ClientNewTicketScreen
import com.requena.supportdesk.app.client.screens.ClientProgramsScreen
import com.requena.supportdesk.app.client.screens.ClientSettingsScreen
import com.requena.supportdesk.app.client.screens.ClientServiceScreen
import com.requena.supportdesk.app.client.screens.ClientTasksScreen
import com.requena.supportdesk.app.client.screens.ClientTicketsScreen
import com.requena.supportdesk.app.client.screens.ClientWorkScreen
import com.requena.supportdesk.app.client.screens.business.ClientBusinessProgramWorkspace
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.ClientPortalComponent
import com.requena.supportdesk.core.model.ClientProgramSubscriptionStatus
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.buttons.ThemeModeButton
import com.requena.supportdesk.designsystem.components.navigation.AppSidebar
import com.requena.supportdesk.designsystem.components.navigation.NavigationItemSpec
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState
import com.requena.supportdesk.features.programs.presentation.event.ProgramsUiEvent
import com.requena.supportdesk.features.programs.presentation.state.ProgramsUiState
import com.requena.supportdesk.features.business.finance.presentation.BusinessAccountingViewModel
import com.requena.supportdesk.features.business.finance.presentation.BusinessInvoicingViewModel
import com.requena.supportdesk.features.business.operations.OperationsViewModel
import com.requena.supportdesk.features.business.sales.presentation.BusinessCatalogViewModel
import com.requena.supportdesk.features.business.sales.presentation.BusinessCustomersViewModel
import com.requena.supportdesk.features.business.sales.presentation.BusinessQuotesViewModel
import kotlinx.coroutines.delay
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.Columns3
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Headphones
import com.composables.icons.lucide.House
import com.composables.icons.lucide.ListTodo
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Ticket
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.UserRound

const val ClientDailyUrgentLimit = 3
const val ClientDailyTaskLimit = 5

enum class ClientDestination { HOME, WORK, PROGRAMS, BUSINESS_PROGRAM, ACCOUNT, SETTINGS, NEW_TICKET, SERVICE, TICKETS, TASKS, BOARD, ACTIVITY }

data class ClientActivityItem(
    val ticketSubject: String,
    val ticketNumber: String,
    val type: ClientActivityType,
    val description: String,
    val actor: String,
    val date: String,
    val minutes: Int = 0,
)

enum class ClientActivityType { CREATED, STATUS_CHANGE, TIME_LOGGED, RESOLVED, CLOSED, RATED }

private val clientNavItems = listOf(
    NavigationItemSpec(ClientDestination.HOME, "Inicio", "Vista general", Lucide.House),
    NavigationItemSpec(ClientDestination.WORK, "Trabajo", "Tareas y solicitudes", Lucide.ListTodo),
    NavigationItemSpec(ClientDestination.PROGRAMS, "Programas", "Utilidades para tu equipo", Lucide.Columns3),
    NavigationItemSpec(ClientDestination.ACCOUNT, "Cuenta", "Plan y acceso", Lucide.UserRound),
    NavigationItemSpec(ClientDestination.SETTINGS, "Ajustes", "Privacidad y suscripción", Lucide.Settings),
)

// ── HELPERS ────────────────────────────────────────────────────────────────────
// Public: shared across app.client.screens.* files within this module.

fun String.initials(): String =
    split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { take(2).uppercase() }

fun isoDateMinus(isoDate: String, days: Int): String {
    val year = isoDate.take(4).toIntOrNull() ?: return isoDate
    val month = isoDate.drop(5).take(2).toIntOrNull() ?: return isoDate
    val day = isoDate.drop(8).take(2).toIntOrNull() ?: return isoDate
    val daysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val isLeap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    if (isLeap) daysInMonth[2] = 29
    var d = day - days
    var m = month
    var y = year
    while (d <= 0) {
        m--
        if (m <= 0) { m = 12; y-- }
        val ly = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
        d += if (m == 2 && ly) 29 else daysInMonth[m]
    }
    return "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}

fun monthLabel(isoDate: String): String {
    val month = isoDate.drop(5).take(2).toIntOrNull() ?: return isoDate.take(7)
    val year = isoDate.take(4)
    val name = when (month) {
        1 -> "Enero"; 2 -> "Febrero"; 3 -> "Marzo"; 4 -> "Abril"
        5 -> "Mayo"; 6 -> "Junio"; 7 -> "Julio"; 8 -> "Agosto"
        9 -> "Septiembre"; 10 -> "Octubre"; 11 -> "Noviembre"; else -> "Diciembre"
    }
    return "$name $year"
}

fun monthAbbrev(month: Int): String = when (month) {
    1 -> "Ene"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Abr"
    5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Ago"
    9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dic"
}

private fun buildActivityItems(tickets: List<Ticket>): List<ClientActivityItem> {
    val items = mutableListOf<ClientActivityItem>()
    for (ticket in tickets) {
        items.add(
            ClientActivityItem(
                ticketSubject = ticket.subject,
                ticketNumber = ticket.ticketNumber,
                type = ClientActivityType.CREATED,
                description = "Ticket creado",
                actor = ticket.requester.name,
                date = ticket.createdAt.take(10),
            ),
        )
        for (event in ticket.events) {
            val type = when {
                event.type.contains("RESOLVED", ignoreCase = true) -> ClientActivityType.RESOLVED
                event.type.contains("CLOSED", ignoreCase = true) -> ClientActivityType.CLOSED
                else -> ClientActivityType.STATUS_CHANGE
            }
            items.add(
                ClientActivityItem(
                    ticketSubject = ticket.subject,
                    ticketNumber = ticket.ticketNumber,
                    type = type,
                    description = event.description,
                    actor = event.actorName,
                    date = event.createdAt.take(10),
                ),
            )
        }
        for (entry in ticket.timeEntries) {
            items.add(
                ClientActivityItem(
                    ticketSubject = ticket.subject,
                    ticketNumber = ticket.ticketNumber,
                    type = ClientActivityType.TIME_LOGGED,
                    description = entry.note.ifBlank { "Tiempo registrado" },
                    actor = entry.authorName,
                    date = entry.workDate.take(10),
                    minutes = entry.minutes,
                ),
            )
        }
    }
    return items.sortedByDescending { it.date }
}

@Composable
fun ClientNotice(message: String, isError: Boolean) {
    val semantic = SupportDeskThemeTokens.semanticColors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isError) semantic.dangerContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (isError) semantic.danger else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(SupportDeskThemeTokens.spacing.md),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun ClientPortalScreen(
    clientName: String,
    companyName: String = "",
    client: Client? = null,
    state: TicketsUiState,
    onEvent: (TicketsUiEvent) -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    tasksState: TasksUiState = TasksUiState(),
    onTasksEvent: (TasksUiEvent) -> Unit = {},
    programsState: ProgramsUiState = ProgramsUiState(),
    onProgramsEvent: (ProgramsUiEvent) -> Unit = {},
    businessInvoicingViewModel: BusinessInvoicingViewModel? = null,
    businessAccountingViewModel: BusinessAccountingViewModel? = null,
    operationsViewModel: OperationsViewModel? = null,
    businessCustomersViewModel: BusinessCustomersViewModel? = null,
    businessCatalogViewModel: BusinessCatalogViewModel? = null,
    businessQuotesViewModel: BusinessQuotesViewModel? = null,
    clientId: String? = null,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    if (clientId.isNullOrBlank()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(spacing.xl),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            SectionCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Acceso pendiente de configurar",
                subtitle = "Tu cuenta aun no esta vinculada a una empresa.",
            ) {
                Text(
                    text = "Pide al equipo de OryKai que active el acceso a tu portal. No mostramos datos hasta que la vinculacion este lista.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SecondaryButton(text = "Cerrar sesion", onClick = onSignOut)
            }
        }
        return
    }
    var today by remember { mutableStateOf(currentIsoDate()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            val now = currentIsoDate()
            if (now != today) today = now
        }
    }
    var destination by remember { mutableStateOf(ClientDestination.HOME) }
    var activeBusinessProgramKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(clientId) {
        onTasksEvent(TasksUiEvent.SelectDashboardClient(clientId))
    }
    val portalTickets = state.allTickets.ifEmpty { state.tickets }
    val clientTasks = tasksState.dashboardClientTasks
    val todayTasks = remember(clientTasks, today) {
        clientTasks.filter { (it.dueDate ?: it.createdAt.take(10)) == today }
    }

    val todayUrgentCount = remember(portalTickets, today) {
        portalTickets.count { it.createdAt.take(10) == today && it.priority == TicketPriority.URGENT }
    }
    val visibleEntries = remember(tasksState.logs, clientId) {
        tasksState.logs.filter { it.clientId == clientId }
    }
    val openCount = remember(portalTickets) { portalTickets.count { it.status != TicketStatus.CLOSED } }
    val monthlyMinutes = remember(visibleEntries, today) {
        visibleEntries.filter { it.workDate.take(7) == today.take(7) }.sumOf { it.minutes }
    }
    val lastMonthPrefix = remember(today) {
        val y = today.take(4).toIntOrNull() ?: 0
        val mo = today.drop(5).take(2).toIntOrNull() ?: 1
        val prevMo = if (mo == 1) 12 else mo - 1
        val prevY = if (mo == 1) y - 1 else y
        "$prevY-${prevMo.toString().padStart(2, '0')}"
    }
    val lastMonthMinutes = remember(visibleEntries, lastMonthPrefix) {
        visibleEntries.filter { it.workDate.take(7) == lastMonthPrefix }.sumOf { it.minutes }
    }
    val allActivityItems = remember(portalTickets) { buildActivityItems(portalTickets) }

    // Empresa (company name) es el campo principal; clientName es el nombre de contacto
    val displayCompany = companyName.ifBlank { clientName }
    val displayContact = if (companyName.isNotBlank() && clientName.isNotBlank() && clientName != companyName) clientName else ""
    val hasServiceSla = client?.hasComponent(ClientPortalComponent.SERVICE_SLA) == true

    val portalContent: @Composable (Modifier) -> Unit = { contentModifier ->
        AnimatedContent(
            targetState = destination,
            modifier = contentModifier,
            transitionSpec = {
                val from = ClientDestination.entries.indexOf(initialState)
                val to = ClientDestination.entries.indexOf(targetState)
                val forward = to > from
                (slideInHorizontally(tween(SupportDeskMotion.page)) { if (forward) it / 3 else -it / 3 } +
                    fadeIn(tween(SupportDeskMotion.regular))) togetherWith
                    (slideOutHorizontally(tween(SupportDeskMotion.regular)) { if (forward) -it / 3 else it / 3 } +
                        fadeOut(tween(SupportDeskMotion.quick)))
            },
            label = "client_nav",
        ) { dest ->
            when (dest) {
                ClientDestination.HOME -> ClientHomeScreen(
                    clientName = displayCompany,
                    contactName = displayContact,
                    allTickets = portalTickets,
                    openCount = openCount,
                    monthlyMinutes = monthlyMinutes,
                    todayTasks = todayTasks,
                    recentTickets = portalTickets.sortedByDescending { it.updatedAt }.take(3),
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    hasServiceSla = hasServiceSla,
                    activeProgramCount = programsState.overview.subscriptions.count {
                        it.status == ClientProgramSubscriptionStatus.ACTIVE
                    },
                    onNavigate = { destination = it },
                )
                ClientDestination.WORK -> ClientWorkScreen(
                    tickets = portalTickets,
                    tasks = clientTasks,
                    onNavigate = { destination = it },
                )
                ClientDestination.PROGRAMS -> ClientProgramsScreen(
                    state = programsState,
                    onEvent = onProgramsEvent,
                    onOpenProgram = { programKey ->
                        activeBusinessProgramKey = programKey
                        destination = ClientDestination.BUSINESS_PROGRAM
                    },
                )
                ClientDestination.BUSINESS_PROGRAM -> {
                    val key = activeBusinessProgramKey
                    if (
                        key != null &&
                        businessInvoicingViewModel != null &&
                        businessAccountingViewModel != null &&
                        operationsViewModel != null &&
                        businessCustomersViewModel != null &&
                        businessCatalogViewModel != null &&
                        businessQuotesViewModel != null
                    ) {
                        ClientBusinessProgramWorkspace(
                            programKey = key,
                            invoicingViewModel = businessInvoicingViewModel,
                            accountingViewModel = businessAccountingViewModel,
                            operationsViewModel = operationsViewModel,
                            customersViewModel = businessCustomersViewModel,
                            catalogViewModel = businessCatalogViewModel,
                            quotesViewModel = businessQuotesViewModel,
                            onBack = { destination = ClientDestination.PROGRAMS },
                        )
                    } else {
                        ClientProgramWorkspaceUnavailable(
                            onBack = { destination = ClientDestination.PROGRAMS },
                        )
                    }
                }
                ClientDestination.NEW_TICKET -> ClientNewTicketScreen(
                    clientId = clientId,
                    urgentToday = todayUrgentCount,
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    lastCreatedTicketId = state.lastCreatedTicketId,
                    onEvent = onEvent,
                )
                ClientDestination.TICKETS -> ClientTicketsScreen(
                    state = state,
                    companyName = displayCompany,
                    onEvent = onEvent,
                )
                ClientDestination.TASKS -> ClientTasksScreen(
                    state = tasksState,
                    clientId = clientId,
                    today = today,
                    onEvent = onTasksEvent,
                )
                ClientDestination.BOARD -> ClientBoardScreen(
                    tickets = portalTickets,
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    onRetry = onRefresh,
                    onTicketClick = { ticketId ->
                        onEvent(TicketsUiEvent.SelectTicket(ticketId))
                        destination = ClientDestination.TICKETS
                    },
                )
                ClientDestination.SERVICE -> ClientServiceScreen(
                    tickets = portalTickets,
                    logs = visibleEntries,
                    today = today,
                    lastMonthMinutes = lastMonthMinutes,
                    isEnabled = hasServiceSla,
                    onRequestActivation = { destination = ClientDestination.NEW_TICKET },
                )
                ClientDestination.ACTIVITY -> ClientActivityScreen(
                    activityItems = allActivityItems,
                )
                ClientDestination.ACCOUNT -> ClientAccountScreen(
                    clientName = displayCompany,
                    contactName = displayContact,
                    client = client,
                    tickets = portalTickets,
                    logs = visibleEntries,
                    today = today,
                    onRefresh = onRefresh,
                    onSignOut = onSignOut,
                )
                ClientDestination.SETTINGS -> ClientSettingsScreen(
                    activeProgramCount = programsState.overview.subscriptions.count {
                        it.status == ClientProgramSubscriptionStatus.ACTIVE
                    },
                    onManagePrograms = { destination = ClientDestination.PROGRAMS },
                    onOpenAccount = { destination = ClientDestination.ACCOUNT },
                    onContactSupport = { destination = ClientDestination.NEW_TICKET },
                )
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (maxWidth < SupportDeskBreakpoints.clientWide) {
            Column(modifier = Modifier.fillMaxSize()) {
                ClientCompactHeader(
                    companyName = displayCompany,
                    contactName = displayContact,
                    onRequestHelp = { destination = ClientDestination.NEW_TICKET },
                    onRefresh = onRefresh,
                    onSignOut = onSignOut,
                )
                ClientCompactNavigation(
                    selected = destination,
                    onSelect = { destination = it },
                )
                portalContent(Modifier.weight(1f).fillMaxWidth())
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                AppSidebar(
                    brandTitle = displayCompany,
                    brandSubtitle = if (displayContact.isNotBlank()) displayContact else "Portal cliente",
                    items = clientNavItems,
                    selected = destination,
                    onSelect = { destination = it },
                    footer = {
                        ThemeModeButton()
                        PrimaryButton(
                            text = "Solicitar ayuda",
                            icon = Lucide.Plus,
                            fullWidth = true,
                            onClick = { destination = ClientDestination.NEW_TICKET },
                        )
                        SecondaryButton(text = "Actualizar", onClick = onRefresh)
                        SecondaryButton(text = "Salir", onClick = onSignOut)
                    },
                )
                portalContent(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun ClientProgramWorkspaceUnavailable(onBack: () -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = Modifier.fillMaxSize().padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text("El programa se está preparando", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "Actualiza el portal o vuelve al catálogo para comprobar su estado.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecondaryButton(text = "Volver a programas", onClick = onBack)
    }
}

@Composable
private fun ClientCompactHeader(
    companyName: String,
    contactName: String,
    onRequestHelp: () -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = SupportDeskThemeTokens.elevations.subtle,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = companyName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (contactName.isBlank()) "Portal cliente" else contactName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ThemeModeButton()
            }
            PrimaryButton(
                text = "Solicitar ayuda",
                icon = Lucide.Plus,
                fullWidth = true,
                onClick = onRequestHelp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                SecondaryButton(
                    text = "Actualizar",
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Salir",
                    onClick = onSignOut,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ClientCompactNavigation(
    selected: ClientDestination,
    onSelect: (ClientDestination) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        clientNavItems.forEach { item ->
            val isSelected = item.key == selected
            Surface(
                modifier = Modifier.weight(1f).clickable { onSelect(item.key) },
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    item.icon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = item.title,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}
