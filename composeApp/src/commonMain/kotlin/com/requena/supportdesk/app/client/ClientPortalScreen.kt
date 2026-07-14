package com.requena.supportdesk.app.client

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.client.screens.ClientAccountScreen
import com.requena.supportdesk.app.client.screens.ClientActivityScreen
import com.requena.supportdesk.app.client.screens.ClientBoardScreen
import com.requena.supportdesk.app.client.screens.ClientHomeScreen
import com.requena.supportdesk.app.client.screens.ClientInvoicesScreen
import com.requena.supportdesk.app.client.screens.ClientNewTicketScreen
import com.requena.supportdesk.app.client.screens.ClientServiceScreen
import com.requena.supportdesk.app.client.screens.ClientTasksScreen
import com.requena.supportdesk.app.client.screens.ClientTicketsScreen
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.buttons.ThemeModeButton
import com.requena.supportdesk.designsystem.components.navigation.AppSidebar
import com.requena.supportdesk.designsystem.components.navigation.NavigationItemSpec
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.invoices.presentation.event.InvoicesUiEvent
import com.requena.supportdesk.features.invoices.presentation.state.InvoicesUiState
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState
import kotlinx.coroutines.delay

const val ClientDailyUrgentLimit = 3
const val ClientDailyTaskLimit = 5

enum class ClientDestination { HOME, NEW_TICKET, TICKETS, TASKS, BOARD, SERVICE, ACTIVITY, ACCOUNT, INVOICES }

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
    NavigationItemSpec(ClientDestination.HOME, "Inicio", "Vista general"),
    NavigationItemSpec(ClientDestination.NEW_TICKET, "Nuevo ticket", "Abrir soporte"),
    NavigationItemSpec(ClientDestination.TICKETS, "Tickets", "Historial"),
    NavigationItemSpec(ClientDestination.TASKS, "Tareas", "Lista diaria"),
    NavigationItemSpec(ClientDestination.BOARD, "Tablero", "Vista kanban"),
    NavigationItemSpec(ClientDestination.SERVICE, "Mi Servicio", "Resumen de soporte"),
    NavigationItemSpec(ClientDestination.ACTIVITY, "Actividad", "Historial de eventos"),
    NavigationItemSpec(ClientDestination.ACCOUNT, "Mi Cuenta", "Perfil y acceso"),
    NavigationItemSpec(ClientDestination.INVOICES, "Facturas", "Mis pagos"),
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
    state: TicketsUiState,
    onEvent: (TicketsUiEvent) -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    invoicesState: InvoicesUiState = InvoicesUiState(),
    onInvoicesEvent: (InvoicesUiEvent) -> Unit = {},
    tasksState: TasksUiState = TasksUiState(),
    onTasksEvent: (TasksUiEvent) -> Unit = {},
    clientId: String? = null,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var today by remember { mutableStateOf(currentIsoDate()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            val now = currentIsoDate()
            if (now != today) today = now
        }
    }
    var destination by remember { mutableStateOf(ClientDestination.HOME) }

    LaunchedEffect(clientId) {
        onTasksEvent(TasksUiEvent.SelectDashboardClient(clientId))
    }
    val clientTasks = tasksState.dashboardClientTasks
    val todayTaskCount = remember(clientTasks, today) {
        clientTasks.count { it.createdAt.take(10) == today }
    }
    val todayTasksDone = remember(clientTasks, today) {
        clientTasks.count { it.completed && it.createdAt.take(10) == today }
    }

    val todayUrgentCount = remember(state.tickets, today) {
        state.tickets.count { it.createdAt.take(10) == today && it.priority == TicketPriority.URGENT }
    }
    val visibleEntries = remember(state.tickets) { state.tickets.flatMap { it.timeEntries } }
    val openCount = remember(state.tickets) { state.tickets.count { it.status != TicketStatus.CLOSED } }
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
    val allActivityItems = remember(state.tickets) { buildActivityItems(state.tickets) }

    // Empresa (company name) es el campo principal; clientName es el nombre de contacto
    val displayCompany = companyName.ifBlank { clientName }
    val displayContact = if (companyName.isNotBlank() && clientName.isNotBlank() && clientName != companyName) clientName else ""

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        AppSidebar(
            brandTitle = displayCompany,
            brandSubtitle = if (displayContact.isNotBlank()) displayContact else "Portal cliente",
            items = clientNavItems,
            selected = destination,
            onSelect = { destination = it },
            footer = {
                ThemeModeButton()
                SecondaryButton(text = "Actualizar", onClick = onRefresh)
                SecondaryButton(text = "Salir", onClick = onSignOut)
            },
        )

        AnimatedContent(
            targetState = destination,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val from = ClientDestination.entries.indexOf(initialState)
                val to = ClientDestination.entries.indexOf(targetState)
                val forward = to > from
                (slideInHorizontally(tween(320)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(260)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(200)))
            },
            label = "client_nav",
        ) { dest ->
            when (dest) {
                ClientDestination.HOME -> ClientHomeScreen(
                    clientName = displayCompany,
                    contactName = displayContact,
                    allTickets = state.tickets,
                    openCount = openCount,
                    monthlyMinutes = monthlyMinutes,
                    todayUrgentCount = todayUrgentCount,
                    todayTaskCount = todayTaskCount,
                    todayTasksDone = todayTasksDone,
                    recentTickets = state.tickets.sortedByDescending { it.updatedAt }.take(3),
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    onNavigate = { destination = it },
                )
                ClientDestination.NEW_TICKET -> ClientNewTicketScreen(
                    urgentToday = todayUrgentCount,
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
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
                    tickets = state.tickets,
                    onTicketClick = { ticketId ->
                        onEvent(TicketsUiEvent.SelectTicket(ticketId))
                        destination = ClientDestination.TICKETS
                    },
                )
                ClientDestination.SERVICE -> ClientServiceScreen(
                    tickets = state.tickets,
                    today = today,
                    lastMonthMinutes = lastMonthMinutes,
                )
                ClientDestination.ACTIVITY -> ClientActivityScreen(
                    activityItems = allActivityItems,
                )
                ClientDestination.ACCOUNT -> ClientAccountScreen(
                    clientName = displayCompany,
                    contactName = displayContact,
                    tickets = state.tickets,
                    today = today,
                    onRefresh = onRefresh,
                    onSignOut = onSignOut,
                )
                ClientDestination.INVOICES -> ClientInvoicesScreen(
                    state = invoicesState,
                    onEvent = onInvoicesEvent,
                )
            }
        }
    }
}
