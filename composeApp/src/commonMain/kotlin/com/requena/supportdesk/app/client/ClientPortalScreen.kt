package com.requena.supportdesk.app.client

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.TimeEntry
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.badges.SupportPlatformBadge
import com.requena.supportdesk.designsystem.components.badges.TicketCategoryBadge
import com.requena.supportdesk.designsystem.components.badges.TicketPriorityBadge
import com.requena.supportdesk.designsystem.components.badges.TicketStatusBadge
import com.requena.supportdesk.designsystem.components.badges.WaitingOnBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.buttons.ThemeModeButton
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.motion.SupportDeskEntrance
import com.requena.supportdesk.designsystem.components.navigation.AppSidebar
import com.requena.supportdesk.designsystem.components.navigation.NavigationItemSpec
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState

private const val ClientDailyUrgentLimit = 3
private const val ClientDailyTaskLimit = 5
private val RatingOptions = (1..5).toList()

private enum class ClientDestination { HOME, NEW_TICKET, TICKETS, TASKS, BOARD, SERVICE, ACTIVITY, ACCOUNT }

private data class ClientTask(
    val id: String,
    val title: String,
    val note: String = "",
    val createdAt: String,
    val done: Boolean = false,
)

private data class ClientActivityItem(
    val ticketSubject: String,
    val ticketNumber: String,
    val type: ClientActivityType,
    val description: String,
    val actor: String,
    val date: String,
    val minutes: Int = 0,
)

private enum class ClientActivityType { CREATED, STATUS_CHANGE, TIME_LOGGED, RESOLVED, CLOSED, RATED }

private val clientNavItems = listOf(
    NavigationItemSpec(ClientDestination.HOME, "Inicio", "Vista general"),
    NavigationItemSpec(ClientDestination.NEW_TICKET, "Nuevo ticket", "Abrir soporte"),
    NavigationItemSpec(ClientDestination.TICKETS, "Tickets", "Historial"),
    NavigationItemSpec(ClientDestination.TASKS, "Tareas", "Lista diaria"),
    NavigationItemSpec(ClientDestination.BOARD, "Tablero", "Vista kanban"),
    NavigationItemSpec(ClientDestination.SERVICE, "Mi Servicio", "Resumen de soporte"),
    NavigationItemSpec(ClientDestination.ACTIVITY, "Actividad", "Historial de eventos"),
    NavigationItemSpec(ClientDestination.ACCOUNT, "Mi Cuenta", "Perfil y acceso"),
)

// ── HELPERS ────────────────────────────────────────────────────────────────────

private fun String.initials(): String =
    split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { take(2).uppercase() }

private fun isoDateMinus(isoDate: String, days: Int): String {
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

private fun monthLabel(isoDate: String): String {
    val month = isoDate.drop(5).take(2).toIntOrNull() ?: return isoDate.take(7)
    val year = isoDate.take(4)
    val name = when (month) {
        1 -> "Enero"; 2 -> "Febrero"; 3 -> "Marzo"; 4 -> "Abril"
        5 -> "Mayo"; 6 -> "Junio"; 7 -> "Julio"; 8 -> "Agosto"
        9 -> "Septiembre"; 10 -> "Octubre"; 11 -> "Noviembre"; else -> "Diciembre"
    }
    return "$name $year"
}

private fun monthAbbrev(month: Int): String = when (month) {
    1 -> "Ene"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Abr"
    5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Ago"
    9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dic"
}

private fun buildActivityItems(tickets: List<com.requena.supportdesk.core.model.Ticket>): List<ClientActivityItem> {
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
        if (ticket.satisfactionRating != null) {
            items.add(
                ClientActivityItem(
                    ticketSubject = ticket.subject,
                    ticketNumber = ticket.ticketNumber,
                    type = ClientActivityType.RATED,
                    description = "Valoración registrada: ${ticket.satisfactionRating}/5",
                    actor = ticket.requester.name,
                    date = (ticket.clientAcceptedCloseAt ?: ticket.updatedAt).take(10),
                ),
            )
        }
    }
    return items.sortedByDescending { it.date }
}

@Composable
fun ClientPortalScreen(
    clientName: String,
    companyName: String = "",
    state: TicketsUiState,
    onEvent: (TicketsUiEvent) -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
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
    var tasks by remember { mutableStateOf(emptyList<ClientTask>()) }

    val todayUrgentCount = remember(state.tickets, today) {
        state.tickets.count { it.createdAt.take(10) == today && it.priority == TicketPriority.URGENT }
    }
    val todayTaskCount = remember(tasks, today) {
        tasks.count { it.createdAt.take(10) == today }
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
                    todayTasksDone = tasks.count { it.done && it.createdAt.take(10) == today },
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
                    tasks = tasks,
                    today = today,
                    todayTaskCount = todayTaskCount,
                    onAddTask = { title, note ->
                        tasks = listOf(
                            ClientTask(
                                id = "task-${tasks.size + 1}-$today",
                                title = title,
                                note = note,
                                createdAt = today,
                            ),
                        ) + tasks
                    },
                    onToggleDone = { id ->
                        tasks = tasks.map { if (it.id == id) it.copy(done = !it.done) else it }
                    },
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
            }
        }
    }
}

// ── HOME ─────────────────────────────────────────────────────────────────────

@Composable
private fun ClientHomeScreen(
    clientName: String,
    contactName: String = "",
    allTickets: List<Ticket>,
    openCount: Int,
    monthlyMinutes: Int,
    todayUrgentCount: Int,
    todayTaskCount: Int,
    todayTasksDone: Int,
    recentTickets: List<Ticket>,
    isLoading: Boolean,
    errorMessage: String?,
    onNavigate: (ClientDestination) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val ticketCount = allTickets.size
    val animatedTickets by animateIntAsState(ticketCount, tween(700), label = "h_tickets")
    val animatedOpen by animateIntAsState(openCount, tween(850), label = "h_open")
    val urgentRemaining = ClientDailyUrgentLimit - todayUrgentCount
    val taskProgress = todayTasksDone.toFloat() / ClientDailyTaskLimit.toFloat()

    val companyInitials = remember(clientName) { clientName.initials() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        SupportDeskEntrance(index = 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            ),
                        ),
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                    ),
                                ),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = companyInitials,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = "Bienvenido,",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = clientName,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (contactName.isNotBlank()) {
                        Text(
                            text = contactName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // ── Badges de identidad empresarial ──────────────────────
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        SupportDeskBadge(
                            text = "$companyInitials · Empresa",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        SupportDeskBadge(
                            text = "Portal activo",
                            containerColor = SupportDeskThemeTokens.semanticColors.successContainer,
                            contentColor = SupportDeskThemeTokens.semanticColors.success,
                        )
                    }
                    errorMessage?.let {
                        ClientNotice(message = it, isError = true)
                    }
                }
            }
        }

        SupportDeskEntrance(index = 1, horizontal = true) {
            ClientStatusBoard(tickets = allTickets)
        }

        SupportDeskEntrance(index = 2) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    MetricCard(
                        label = "Total tickets",
                        value = animatedTickets.toString(),
                        supportingText = "desde el inicio",
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        label = "Este mes",
                        value = formatSupportDeskDuration(monthlyMinutes),
                        supportingText = "horas de soporte",
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        label = "Abiertos",
                        value = animatedOpen.toString(),
                        supportingText = "en curso ahora",
                        modifier = Modifier.weight(1f),
                    )
                }
                SupportDeskBadge(
                    text = if (urgentRemaining <= 0) "Sin urgentes hoy" else "$urgentRemaining urgente${if (urgentRemaining == 1) "" else "s"} disponible${if (urgentRemaining == 1) "" else "s"}",
                    containerColor = if (urgentRemaining <= 0) SupportDeskThemeTokens.semanticColors.dangerContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = if (urgentRemaining <= 0) SupportDeskThemeTokens.semanticColors.danger else MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        SupportDeskEntrance(index = 3) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Text(
                    text = "Acciones rápidas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    QuickActionCard(
                        title = "Nuevo ticket",
                        subtitle = "Describe tu problema al equipo",
                        badge = if (urgentRemaining > 0) "$urgentRemaining urgentes disponibles" else "Sin urgentes hoy",
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { onNavigate(ClientDestination.NEW_TICKET) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionCard(
                        title = "Tablero",
                        subtitle = "Vista kanban de todos tus tickets",
                        badge = "$openCount activos",
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = { onNavigate(ClientDestination.BOARD) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    QuickActionCard(
                        title = "Mis tickets",
                        subtitle = "Historial y detalle de solicitudes",
                        badge = "$openCount abiertos",
                        accentColor = SupportDeskThemeTokens.semanticColors.info,
                        onClick = { onNavigate(ClientDestination.TICKETS) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionCard(
                        title = "Mis tareas",
                        subtitle = "Notas pendientes del día",
                        badge = "$todayTaskCount/$ClientDailyTaskLimit hoy",
                        accentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.80f),
                        onClick = { onNavigate(ClientDestination.TASKS) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        SupportDeskEntrance(index = 4, horizontal = true) {
            SectionCard(title = "Actividad reciente", subtitle = "Últimas actualizaciones") {
                if (recentTickets.isEmpty()) {
                    Text(
                        text = "Sin actividad reciente. Abre tu primer ticket para empezar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        recentTickets.forEach { ticket ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                                ) {
                                    Text(
                                        text = ticket.subject,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = formatSupportDeskDateTime(ticket.updatedAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TicketStatusBadge(ticket.status)
                            }
                        }
                    }
                }
            }
        }

        SupportDeskEntrance(index = 5, horizontal = true) {
            SectionCard(title = "Tareas de hoy", subtitle = "$todayTasksDone/$ClientDailyTaskLimit completadas") {
                LinearProgressIndicator(
                    progress = { taskProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$todayTasksDone de $ClientDailyTaskLimit tareas completadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AnimatedVisibility(visible = todayTasksDone < ClientDailyTaskLimit) {
                        SecondaryButton(text = "Ver tareas", onClick = { onNavigate(ClientDestination.TASKS) })
                    }
                }
            }
        }

        if (isLoading) {
            SupportDeskEntrance(index = 6) { LoadingState(itemCount = 3) }
        }
    }
}

@Composable
private fun HomeStatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val elevation by animateDpAsState(
        targetValue = if (hovered) 10.dp else 2.dp,
        animationSpec = tween(200),
        label = "card_elev",
    )
    ElevatedCard(
        modifier = modifier
            .heightIn(min = 130.dp)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.15f),
                            accentColor.copy(alpha = 0.04f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        ),
                    ),
                )
                .padding(spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    badge?.let {
                        SupportDeskBadge(
                            text = it,
                            containerColor = accentColor.copy(alpha = 0.18f),
                            contentColor = accentColor,
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(3.dp)
                        .background(accentColor.copy(alpha = if (hovered) 0.80f else 0.32f), RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

// ── NEW TICKET ────────────────────────────────────────────────────────────────

@Composable
private fun ClientNewTicketScreen(
    urgentToday: Int,
    isLoading: Boolean,
    errorMessage: String?,
    onEvent: (TicketsUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var subject by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(TicketCategory.QUESTION) }
    var platform by rememberSaveable { mutableStateOf(SupportPlatform.DESKTOP) }
    var priority by rememberSaveable { mutableStateOf(TicketPriority.MEDIUM) }
    val urgentLimitReached = priority == TicketPriority.URGENT && urgentToday >= ClientDailyUrgentLimit
    val categoryOptions = remember { TicketCategory.entries.map { FilterOption(it, it.displayName()) } }
    val platformOptions = remember { SupportPlatform.entries.map { FilterOption(it, it.displayName()) } }
    val priorityOptions = remember { TicketPriority.entries.map { FilterOption(it, it.displayName()) } }

    // Shared form content — called in both wide (left col) and narrow (single col) layouts
    val formContent: @Composable () -> Unit = {
        SupportDeskEntrance(index = 0) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text("Nuevo ticket", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Máximo $ClientDailyUrgentLimit tickets urgentes por día.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SupportDeskEntrance(index = 1) {
            val rem = ClientDailyUrgentLimit - urgentToday
            SectionCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Clasificación",
                subtitle = "$rem urgente${if (rem == 1) "" else "s"} disponible${if (rem == 1) "" else "s"}",
                neonAccentColor = if (urgentToday > 0) SupportDeskThemeTokens.semanticColors.warning else null,
            ) {
                UrgentSlotsBar(
                    used = urgentToday,
                    limit = ClientDailyUrgentLimit,
                    modifier = Modifier.fillMaxWidth(),
                )
                AnimatedVisibility(
                    visible = priority == TicketPriority.URGENT && !urgentLimitReached,
                    enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { -it / 2 },
                    exit = fadeOut(tween(180)),
                ) {
                    ClientNotice(
                        message = "Prioridad urgente ($urgentToday/$ClientDailyUrgentLimit usados). Solo para bloqueos críticos.",
                        isError = false,
                    )
                }
                FilterBar(
                    label = "Prioridad",
                    options = priorityOptions,
                    selected = priority,
                    onSelected = { priority = it ?: TicketPriority.MEDIUM },
                    wrap = true,
                    allLabel = "Media",
                )
                FilterBar(
                    label = "Tipo",
                    options = categoryOptions,
                    selected = category,
                    onSelected = { category = it ?: TicketCategory.QUESTION },
                    allLabel = "Consulta",
                    wrap = true,
                )
                FilterBar(
                    label = "Plataforma",
                    options = platformOptions,
                    selected = platform,
                    onSelected = { platform = it ?: SupportPlatform.DESKTOP },
                    allLabel = "Escritorio",
                    wrap = true,
                )
            }
        }
        SupportDeskEntrance(index = 2) {
            SectionCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Descripción",
                subtitle = "Detalla el problema para ayudarte mejor",
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Asunto") },
                    singleLine = true,
                    enabled = !urgentLimitReached,
                    shape = RoundedCornerShape(8.dp),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Detalle del problema") },
                    minLines = 5,
                    enabled = !urgentLimitReached,
                    shape = RoundedCornerShape(8.dp),
                )
                AnimatedVisibility(visible = urgentLimitReached, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
                    ClientNotice(
                        message = "Límite de urgentes alcanzado ($ClientDailyUrgentLimit/día). Cambia la prioridad o vuelve mañana.",
                        isError = true,
                    )
                }
                errorMessage?.let { ClientNotice(message = it, isError = true) }
                PrimaryButton(
                    text = if (isLoading) "Enviando..." else "Enviar ticket",
                    onClick = {
                        onEvent(
                            TicketsUiEvent.CreateTicket(
                                CreateTicketInput(
                                    subject = subject.trim(),
                                    description = description.trim(),
                                    category = category,
                                    platform = platform,
                                    priority = priority,
                                ),
                            ),
                        )
                        subject = ""
                        description = ""
                        priority = TicketPriority.MEDIUM
                    },
                    enabled = !urgentLimitReached && !isLoading && subject.isNotBlank() && description.isNotBlank(),
                    fullWidth = true,
                )
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 900.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xl),
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.56f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    formContent()
                }
                Column(
                    modifier = Modifier
                        .weight(0.44f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = spacing.xl, end = spacing.xl, bottom = spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    SupportDeskEntrance(index = 3, horizontal = true) {
                        TicketPreviewCard(
                            subject = subject,
                            description = description,
                            category = category,
                            platform = platform,
                            priority = priority,
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                formContent()
                SupportDeskEntrance(index = 3) {
                    TicketPreviewCard(
                        subject = subject,
                        description = description,
                        category = category,
                        platform = platform,
                        priority = priority,
                    )
                }
            }
        }
    }
}

@Composable
private fun UrgentSlotsBar(
    used: Int,
    limit: Int,
    modifier: Modifier = Modifier,
) {
    val semantic = SupportDeskThemeTokens.semanticColors
    val slotColors = listOf(semantic.info, semantic.warning, semantic.danger)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(limit) { index ->
            val filled = index < used
            val accent = slotColors.getOrNull(index) ?: semantic.danger
            val slotColor by animateColorAsState(
                targetValue = if (filled) accent else accent.copy(alpha = 0.15f),
                animationSpec = tween(350),
                label = "urgentSlot_$index",
            )
            val slotHeight by animateDpAsState(
                targetValue = if (filled) 10.dp else 6.dp,
                animationSpec = tween(350),
                label = "urgentSlotH_$index",
            )
            Surface(
                modifier = Modifier.weight(1f).height(slotHeight),
                shape = RoundedCornerShape(5.dp),
                color = slotColor,
            ) {}
        }
    }
}

@Composable
private fun TicketPreviewCard(
    subject: String,
    description: String,
    category: TicketCategory,
    platform: SupportPlatform,
    priority: TicketPriority,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val hasContent = subject.isNotBlank() || description.isNotBlank()
    val completeness = remember(subject, description) {
        var p = 0f
        if (subject.isNotBlank()) p += 0.40f
        if (description.length >= 10) p += 0.30f
        if (description.length >= 60) p += 0.20f
        if (description.length >= 120) p += 0.10f
        p.coerceIn(0f, 1f)
    }
    val animatedCompleteness by animateFloatAsState(completeness, tween(500), label = "completeness")
    val completenessColor = when {
        completeness >= 0.9f -> semantic.success
        completeness >= 0.4f -> MaterialTheme.colorScheme.primary
        completeness > 0f -> semantic.warning
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    SectionCard(
        modifier = modifier,
        title = "Vista previa",
        subtitle = if (!hasContent) "Completa el formulario" else "${(animatedCompleteness * 100).toInt()}% completado",
        neonAccentColor = if (completeness >= 0.7f) MaterialTheme.colorScheme.secondary else null,
    ) {
        // Live indicator dot
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (hasContent) semantic.success else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    )
            )
            Text(
                text = if (hasContent) "En progreso" else "Sin datos aún",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Mock ticket card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)),
        ) {
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "#????",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TicketStatusBadge(status = TicketStatus.OPEN)
                }
                Text(
                    text = subject.ifBlank { "Sin asunto" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (subject.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (subject.isNotBlank()) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description.take(120).ifBlank { "Sin descripción" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (description.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    AnimatedContent(
                        targetState = priority,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "preview_priority",
                    ) { p -> TicketPriorityBadge(priority = p) }
                    AnimatedContent(
                        targetState = category,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "preview_category",
                    ) { c -> TicketCategoryBadge(category = c) }
                    AnimatedContent(
                        targetState = platform,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "preview_platform",
                    ) { pl -> SupportPlatformBadge(platform = pl) }
                }
            }
        }

        // Completeness bar
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Completado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${(animatedCompleteness * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = completenessColor,
                )
            }
            LinearProgressIndicator(
                progress = { animatedCompleteness },
                modifier = Modifier.fillMaxWidth(),
                color = completenessColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

// ── TICKETS ───────────────────────────────────────────────────────────────────

@Composable
private fun ClientTicketsScreen(
    state: TicketsUiState,
    companyName: String = "",
    onEvent: (TicketsUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val statusOptions = remember { TicketStatus.entries.map { FilterOption(it, it.displayName()) } }
    val priorityOptions = remember { TicketPriority.entries.map { FilterOption(it, it.displayName()) } }
    val statusCounts = remember(state.tickets) { TicketStatus.entries.map { s -> s to state.tickets.count { it.status == s } } }
    val priorityCounts = remember(state.tickets) { TicketPriority.entries.map { p -> p to state.tickets.count { it.priority == p } } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val stacked = maxWidth < 820.dp
        val cols = when {
            maxWidth >= 1400.dp -> 3
            maxWidth >= 820.dp -> 2
            else -> 1
        }
        if (stacked) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                item {
                    SupportDeskEntrance(index = 0) {
                        ClientFiltersPanel(
                            state = state,
                            statusOptions = statusOptions,
                            priorityOptions = priorityOptions,
                            statusCounts = statusCounts,
                            priorityCounts = priorityCounts,
                            onEvent = onEvent,
                        )
                    }
                }
                item {
                    SupportDeskEntrance(index = 1) {
                        ClientTicketGridPanel(
                            state = state,
                            companyName = companyName,
                            cols = cols,
                            onEvent = onEvent,
                        )
                    }
                }
                item {
                    SupportDeskEntrance(index = 2) {
                        ClientTicketDetailPanel(ticket = state.selectedTicket, onEvent = onEvent)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize().padding(spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                SupportDeskEntrance(index = 0) {
                    ClientFiltersPanel(
                        modifier = Modifier.width(260.dp).fillMaxHeight(),
                        state = state,
                        statusOptions = statusOptions,
                        priorityOptions = priorityOptions,
                        statusCounts = statusCounts,
                        priorityCounts = priorityCounts,
                        onEvent = onEvent,
                    )
                }
                SupportDeskEntrance(index = 1) {
                    ClientTicketGridPanel(
                        state = state,
                        companyName = companyName,
                        cols = cols,
                        onEvent = onEvent,
                        modifier = Modifier.weight(0.48f).fillMaxHeight(),
                    )
                }
                SupportDeskEntrance(index = 2, horizontal = true) {
                    AnimatedContent(
                        targetState = state.selectedTicket,
                        transitionSpec = {
                            if (targetState != null) {
                                (fadeIn(tween(280)) + slideInHorizontally(tween(280)) { it / 8 }) togetherWith
                                    fadeOut(tween(200))
                            } else {
                                fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                            }
                        },
                        label = "ticketDetailTransition",
                        modifier = Modifier.weight(0.52f).fillMaxHeight(),
                    ) { ticket ->
                        ClientTicketDetailPanel(
                            ticket = ticket,
                            onEvent = onEvent,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientFiltersPanel(
    state: TicketsUiState,
    statusOptions: List<FilterOption<TicketStatus>>,
    priorityOptions: List<FilterOption<TicketPriority>>,
    statusCounts: List<Pair<TicketStatus, Int>>,
    priorityCounts: List<Pair<TicketPriority, Int>>,
    onEvent: (TicketsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(modifier = modifier.fillMaxWidth(), title = "Filtros", subtitle = "${state.tickets.size} tickets visibles") {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onEvent(TicketsUiEvent.SearchChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
        )
        FilterBar(
            label = "Estado",
            options = statusOptions,
            selected = state.statusFilter,
            onSelected = { onEvent(TicketsUiEvent.StatusFilterChanged(it)) },
            wrap = true,
            allLabel = "Todos",
        )
        FilterBar(
            label = "Prioridad",
            options = priorityOptions,
            selected = state.priorityFilter,
            onSelected = { onEvent(TicketsUiEvent.PriorityFilterChanged(it)) },
            wrap = true,
            allLabel = "Todas",
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            statusCounts.forEach { (status, count) ->
                BoardCounter(
                    label = status.displayName(),
                    value = count.toString(),
                    selected = state.statusFilter == status,
                    onClick = { onEvent(TicketsUiEvent.StatusFilterChanged(if (state.statusFilter == status) null else status)) },
                    modifier = Modifier.widthIn(min = 60.dp),
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            priorityCounts.forEach { (priority, count) ->
                BoardCounter(
                    label = priority.displayName(),
                    value = count.toString(),
                    selected = state.priorityFilter == priority,
                    onClick = { onEvent(TicketsUiEvent.PriorityFilterChanged(if (state.priorityFilter == priority) null else priority)) },
                    modifier = Modifier.widthIn(min = 60.dp),
                )
            }
        }
    }
}

@Composable
private fun ClientTicketGridPanel(
    state: TicketsUiState,
    companyName: String,
    cols: Int,
    onEvent: (TicketsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(
        modifier = modifier.fillMaxWidth(),
        title = "Mis tickets",
        subtitle = "${state.tickets.size} total",
    ) {
        Crossfade(targetState = state.isLoading, label = "ticketGridLoad") { loading ->
            if (loading) {
                LoadingState(itemCount = 6)
            } else if (state.tickets.isEmpty()) {
                EmptyState(
                    title = "Sin tickets",
                    message = state.errorMessage ?: "Cuando abras un ticket aparecerá aquí.",
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    gridItems(state.tickets, key = { it.id }) { ticket ->
                        TicketGridCard(
                            ticket = ticket,
                            companyName = companyName,
                            selected = state.selectedTicket?.id == ticket.id,
                            onClick = { onEvent(TicketsUiEvent.SelectTicket(ticket.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketGridCard(
    ticket: Ticket,
    companyName: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val elevation by animateDpAsState(
        targetValue = when {
            selected -> 6.dp
            hovered -> 4.dp
            else -> 1.dp
        },
        animationSpec = tween(200),
        label = "gridCardElev",
    )
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.60f)
            else
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                if (selected)
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        )
                    )
                else
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        )
                    )
            ),
        ) {
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                // Chip de empresa + número de ticket
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (companyName.isNotBlank()) {
                        SupportDeskBadge(
                            text = companyName.initials(),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    Text(
                        text = ticket.ticketNumber.ifBlank { "#—" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Asunto
                Text(
                    text = ticket.subject,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                // Estado + prioridad
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    TicketStatusBadge(ticket.status)
                    TicketPriorityBadge(ticket.priority)
                }
                // Categoría + plataforma
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    TicketCategoryBadge(ticket.category)
                    SupportPlatformBadge(ticket.platform)
                }
                // Footer: asignado + horas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ticket.assignee?.name?.let { "Admin: $it" } ?: "Sin asignar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatSupportDeskDuration(ticket.timeEntries.sumOf { it.minutes }),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (ticket.timeEntries.isNotEmpty()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientTicketDetailPanel(
    ticket: Ticket?,
    onEvent: (TicketsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    if (ticket == null) {
        EmptyState(
            title = "Selecciona un ticket",
            message = "El detalle, horas y cierre apareceran aqui.",
            modifier = modifier,
        )
        return
    }
    SectionCard(
        modifier = modifier.fillMaxWidth(),
        title = ticket.subject,
        subtitle = ticket.assignee?.name ?: "Pendiente de admin asignado",
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            TicketDetailHeader(ticket = ticket)
            TicketTimeDetail(ticket)
            ClientClosure(ticket = ticket, onEvent = onEvent)
        }
    }
}

// ── CHAT ─────────────────────────────────────────────────────────────────────

// ── TASKS ─────────────────────────────────────────────────────────────────────

@Composable
private fun ClientTasksScreen(
    tasks: List<ClientTask>,
    today: String,
    todayTaskCount: Int,
    onAddTask: (title: String, note: String) -> Unit,
    onToggleDone: (id: String) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var newTitle by remember { mutableStateOf("") }
    var newNote by remember { mutableStateOf("") }
    val limitReached = todayTaskCount >= ClientDailyTaskLimit
    val taskProgress = (todayTaskCount.toFloat() / ClientDailyTaskLimit.toFloat()).coerceIn(0f, 1f)
    val todayTasks = remember(tasks, today) { tasks.filter { it.createdAt.take(10) == today } }
    val todayDone = remember(todayTasks) { todayTasks.count { it.done } }
    val pastTasks = remember(tasks, today) { tasks.filter { it.createdAt.take(10) != today } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        SupportDeskEntrance(index = 0) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text("Mis tareas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Maximo $ClientDailyTaskLimit tareas nuevas por dia. Solo para uso personal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SupportDeskEntrance(index = 1) {
            SectionCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Anadir tarea",
                subtitle = "$todayTaskCount/$ClientDailyTaskLimit usadas hoy",
            ) {
                LinearProgressIndicator(
                    progress = { taskProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (limitReached) SupportDeskThemeTokens.semanticColors.danger else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                AnimatedVisibility(visible = limitReached, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
                    ClientNotice(message = "Limite diario alcanzado. Vuelve manana para anadir mas tareas.", isError = true)
                }
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Titulo de la tarea") },
                    singleLine = true,
                    enabled = !limitReached,
                    shape = RoundedCornerShape(8.dp),
                )
                OutlinedTextField(
                    value = newNote,
                    onValueChange = { newNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nota (opcional)") },
                    minLines = 2,
                    enabled = !limitReached,
                    shape = RoundedCornerShape(8.dp),
                )
                PrimaryButton(
                    text = "Anadir",
                    onClick = {
                        onAddTask(newTitle.trim(), newNote.trim())
                        newTitle = ""
                        newNote = ""
                    },
                    enabled = !limitReached && newTitle.isNotBlank(),
                    fullWidth = true,
                )
            }
        }

        if (todayTasks.isNotEmpty()) {
            SupportDeskEntrance(index = 2) {
                SectionCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Hoy",
                    subtitle = "$todayDone/${todayTasks.size} completadas",
                    neonAccentColor = if (todayDone == todayTasks.size) SupportDeskThemeTokens.semanticColors.success else null,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm)) {
                        todayTasks.forEachIndexed { index, task ->
                            SupportDeskEntrance(index = index) {
                                TaskRow(task = task, onClick = { onToggleDone(task.id) })
                            }
                        }
                    }
                }
            }
        }

        if (pastTasks.isNotEmpty()) {
            SupportDeskEntrance(index = 3) {
                SectionCard(modifier = Modifier.fillMaxWidth(), title = "Anteriores") {
                    Column(verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm)) {
                        pastTasks.take(10).forEachIndexed { index, task ->
                            SupportDeskEntrance(index = index) {
                                TaskRow(task = task, onClick = { onToggleDone(task.id) })
                            }
                        }
                    }
                }
            }
        }

        if (tasks.isEmpty()) {
            SupportDeskEntrance(index = 2) {
                EmptyState(title = "Sin tareas", message = "Usa el formulario para anadir tu primera tarea del dia.")
            }
        }
    }
}

@Composable
private fun TaskRow(task: ClientTask, onClick: () -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val surfaceColor by animateColorAsState(
        targetValue = if (task.done) semantic.successContainer.copy(alpha = 0.32f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        animationSpec = tween(280),
        label = "taskSurface",
    )
    val borderColor by animateColorAsState(
        targetValue = if (task.done) semantic.success.copy(alpha = 0.40f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f),
        animationSpec = tween(280),
        label = "taskBorder",
    )
    val checkColor by animateColorAsState(
        targetValue = if (task.done) semantic.success else MaterialTheme.colorScheme.outline.copy(alpha = 0.40f),
        animationSpec = tween(280),
        label = "taskCheck",
    )
    val textColor by animateColorAsState(
        targetValue = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(280),
        label = "taskText",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        color = surfaceColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(20.dp),
                shape = CircleShape,
                color = checkColor,
                border = if (!task.done) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline) else null,
            ) {}
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                )
                if (task.note.isNotBlank()) {
                    Text(
                        text = task.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (task.done) {
                SupportDeskBadge(
                    text = "Hecho",
                    containerColor = semantic.successContainer,
                    contentColor = semantic.success,
                )
            }
        }
    }
}

// ── SHARED HELPERS ─────────────────────────────────────────────────────────────

@Composable
private fun ClientNotice(message: String, isError: Boolean) {
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
private fun BoardCounter(label: String, value: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .animateContentSize(),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ClientTicketRow(ticket: Ticket, selected: Boolean, onClick: () -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.64f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(ticket.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(ticket.ticketNumber.ifBlank { formatSupportDeskDateTime(ticket.updatedAt) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatSupportDeskDuration(ticket.timeEntries.sumOf { it.minutes }), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                TicketStatusBadge(ticket.status)
                TicketPriorityBadge(ticket.priority)
                TicketCategoryBadge(ticket.category)
                SupportPlatformBadge(ticket.platform)
            }
            Text(
                text = "Admin: ${ticket.assignee?.name ?: "pendiente"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TicketDetailHeader(ticket: Ticket) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                TicketStatusBadge(ticket.status)
                TicketPriorityBadge(ticket.priority)
                WaitingOnBadge(ticket.waitingOn)
                TicketCategoryBadge(ticket.category)
                SupportPlatformBadge(ticket.platform)
            }
        }
        Text(ticket.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            MetricCard(
                label = "Horas ticket",
                value = formatSupportDeskDuration(ticket.timeEntries.sumOf { it.minutes }),
                supportingText = "${ticket.timeEntries.size} registros",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Actualizado",
                value = formatSupportDeskDateTime(ticket.updatedAt).takeLast(10).trim(),
                supportingText = ticket.ticketNumber.ifBlank { "Ticket cliente" },
                modifier = Modifier.weight(1f),
            )
        }
        ticket.resolutionSummary?.takeIf { it.isNotBlank() }?.let { summary ->
            ClientNotice(message = "Resolucion: $summary", isError = false)
        }
    }
}

@Composable
private fun TicketTimeDetail(ticket: Ticket) {
    val spacing = SupportDeskThemeTokens.spacing
    val entriesByMonth = remember(ticket.timeEntries) {
        ticket.timeEntries.groupBy { it.workDate.take(7).ifBlank { "Sin fecha" } }
            .toList()
            .sortedByDescending { it.first }
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text("Horas registradas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (ticket.timeEntries.isEmpty()) {
            EmptyState(title = "Sin horas", message = "El admin registrara tiempo cuando trabaje en este ticket.")
        } else {
            entriesByMonth.forEach { (month, entries) ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(month, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(formatSupportDeskDuration(entries.sumOf { it.minutes }), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        entries.forEach { entry ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                StatusDot()
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.note.ifBlank { "Trabajo registrado" }, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("${entry.authorName} · ${entry.workDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(formatSupportDeskDuration(entry.minutes), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot() {
    Box(
        modifier = Modifier
            .padding(top = 6.dp)
            .height(8.dp)
            .width(8.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
}

@Composable
private fun ClientClosure(ticket: Ticket, onEvent: (TicketsUiEvent) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    val canAcceptClose = ticket.status == TicketStatus.RESOLVED && ticket.clientAcceptedCloseAt == null
    val canRate = ticket.status == TicketStatus.RESOLVED || ticket.status == TicketStatus.CLOSED

    SectionCard(title = "Cierre y satisfacción") {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            ClosurePill("Cliente", ticket.clientAcceptedCloseAt != null)
            ClosurePill("Admin", ticket.adminAcceptedCloseAt != null)
            ticket.archivedAt?.let {
                SupportDeskBadge(text = "Archivado", containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        AnimatedVisibility(visible = canAcceptClose, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
            SecondaryButton(text = "Aceptar cierre", onClick = { onEvent(TicketsUiEvent.AcceptSelectedClose()) }, fullWidth = true)
        }
        if (ticket.satisfactionRating != null) {
            ClientNotice(message = "Valoración registrada: ${ticket.satisfactionRating}/5", isError = false)
        } else {
            Text(
                text = if (canRate) "Valora la resolución del 1 al 5." else "La valoración se activa cuando el ticket esté resuelto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                items(RatingOptions) { rating ->
                    SecondaryButton(
                        text = rating.toString(),
                        onClick = { onEvent(TicketsUiEvent.RateSelected(rating)) },
                        enabled = canRate,
                    )
                }
            }
        }
    }
}

@Composable
private fun ClosurePill(label: String, complete: Boolean) {
    SupportDeskBadge(
        text = if (complete) "$label aceptado" else "$label pendiente",
        containerColor = if (complete) SupportDeskThemeTokens.semanticColors.successContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (complete) SupportDeskThemeTokens.semanticColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.alpha(if (complete) 1f else 0.56f),
    )
}

// ── MI SERVICIO ───────────────────────────────────────────────────────────────

@Composable
private fun ClientServiceScreen(
    tickets: List<Ticket>,
    today: String,
    lastMonthMinutes: Int,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val allEntries = remember(tickets) { tickets.flatMap { it.timeEntries } }
    val currentMonthPrefix = today.take(7)

    val currentMonthMinutes = remember(allEntries, currentMonthPrefix) {
        allEntries.filter { it.workDate.take(7) == currentMonthPrefix }.sumOf { it.minutes }
    }
    val minutesDelta = currentMonthMinutes - lastMonthMinutes

    val resolvedThisMonth = remember(tickets, currentMonthPrefix) {
        tickets.count { (it.status == TicketStatus.RESOLVED || it.status == TicketStatus.CLOSED) && it.updatedAt.take(7) == currentMonthPrefix }
    }
    val activeTickets = remember(tickets) {
        tickets.count { it.status != TicketStatus.CLOSED && it.status != TicketStatus.RESOLVED }
    }
    val ratedTickets = remember(tickets) { tickets.filter { it.satisfactionRating != null } }
    val avgSatisfaction = remember(ratedTickets) {
        if (ratedTickets.isEmpty()) null
        else ratedTickets.sumOf { it.satisfactionRating!! }.toFloat() / ratedTickets.size
    }

    val sixMonthTrend: List<Pair<String, Int>> = remember(allEntries, today) {
        val yr = today.take(4).toIntOrNull() ?: return@remember emptyList()
        val mo = today.drop(5).take(2).toIntOrNull() ?: return@remember emptyList()
        (0..5).map { offset ->
            var m = mo - offset
            var y = yr
            while (m <= 0) { m += 12; y-- }
            val prefix = "$y-${m.toString().padStart(2, '0')}"
            monthAbbrev(m) to allEntries.filter { it.workDate.take(7) == prefix }.sumOf { it.minutes }
        }.reversed()
    }

    val urgentCount = remember(tickets) {
        tickets.count { it.priority == TicketPriority.URGENT && it.status != TicketStatus.CLOSED && it.status != TicketStatus.RESOLVED }
    }

    val categoryBreakdown = remember(tickets) {
        TicketCategory.entries.map { cat -> cat to tickets.count { it.category == cat } }.filter { it.second > 0 }
    }

    val monthlyHistory: List<Triple<String, Int, Int>> = remember(allEntries, tickets, today) {
        val yr = today.take(4).toIntOrNull() ?: return@remember emptyList()
        val mo = today.drop(5).take(2).toIntOrNull() ?: return@remember emptyList()
        (0..5).map { offset ->
            var m = mo - offset
            var y = yr
            while (m <= 0) { m += 12; y-- }
            val prefix = "$y-${m.toString().padStart(2, '0')}"
            val fullName = when (m) {
                1 -> "Enero"; 2 -> "Febrero"; 3 -> "Marzo"; 4 -> "Abril"
                5 -> "Mayo"; 6 -> "Junio"; 7 -> "Julio"; 8 -> "Agosto"
                9 -> "Septiembre"; 10 -> "Octubre"; 11 -> "Noviembre"; else -> "Diciembre"
            }
            Triple(
                "$fullName $y",
                tickets.count { it.createdAt.take(7) == prefix },
                allEntries.filter { it.workDate.take(7) == prefix }.sumOf { it.minutes },
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        SupportDeskEntrance(index = 0) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text("Mi Servicio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Resumen de soporte · ${monthLabel(today)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SupportDeskEntrance(index = 1, horizontal = true) {
            SectionCard(
                title = monthLabel(today),
                subtitle = "Mes actual",
                neonAccentColor = MaterialTheme.colorScheme.primary,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    MetricCard(
                        label = "Horas este mes",
                        value = formatSupportDeskDuration(currentMonthMinutes),
                        supportingText = "tiempo acumulado",
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        label = "Mes anterior",
                        value = formatSupportDeskDuration(lastMonthMinutes),
                        supportingText = "para comparar",
                        modifier = Modifier.weight(1f),
                    )
                }
                val (deltaContainer, deltaContent) = when {
                    minutesDelta > 0 -> semantic.successContainer to semantic.success
                    minutesDelta < 0 -> semantic.dangerContainer to semantic.danger
                    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                }
                SupportDeskBadge(
                    text = when {
                        minutesDelta > 0 -> "+${formatSupportDeskDuration(minutesDelta)} vs mes anterior"
                        minutesDelta < 0 -> "−${formatSupportDeskDuration(-minutesDelta)} vs mes anterior"
                        else -> "Igual que el mes anterior"
                    },
                    containerColor = deltaContainer,
                    contentColor = deltaContent,
                )
            }
        }

        SupportDeskEntrance(index = 2) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    MetricCard(
                        label = "Tickets resueltos",
                        value = resolvedThisMonth.toString(),
                        supportingText = "este mes",
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        label = "Urgentes activos",
                        value = urgentCount.toString(),
                        supportingText = "prioridad urgente",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    MetricCard(
                        label = "Satisfacción",
                        value = avgSatisfaction?.let { "%.1f/5".format(it) } ?: "—",
                        supportingText = "${ratedTickets.size} valoraciones",
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        label = "Activos",
                        value = activeTickets.toString(),
                        supportingText = "tickets en curso",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        SupportDeskEntrance(index = 3, horizontal = true) {
            SectionCard(title = "Tendencia de soporte", subtitle = "Últimos 6 meses") {
                SupportBarChart(data = sixMonthTrend)
            }
        }

        if (categoryBreakdown.isNotEmpty()) {
            SupportDeskEntrance(index = 4) {
                SectionCard(title = "Por categoría", subtitle = "${tickets.size} tickets totales") {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        categoryBreakdown.forEach { (cat, count) ->
                            CategoryProgressRow(label = cat.displayName(), count = count, total = tickets.size)
                        }
                    }
                }
            }
        }

        SupportDeskEntrance(index = 5) {
            SectionCard(title = "Historial mensual") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    monthlyHistory.forEach { (label, ticketCount, minutes) ->
                        MonthlyHistoryRow(label = label, ticketCount = ticketCount, minutes = minutes)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportBarChart(data: List<Pair<String, Int>>) {
    val spacing = SupportDeskThemeTokens.spacing
    if (data.isEmpty() || data.all { it.second == 0 }) {
        Text(
            text = "Los datos aparecerán cuando haya registros de tiempo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val maxVal = data.maxOf { it.second }.coerceAtLeast(1).toFloat()
    var revealed by remember(data) { mutableStateOf(false) }
    LaunchedEffect(data) { revealed = true }
    val revealFraction by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "chartReveal",
    )

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            val barCount = data.size
            val barWidth = (size.width / barCount) * 0.55f
            val gap = (size.width / barCount) * 0.45f
            data.forEachIndexed { index, (_, minutes) ->
                val fraction = (minutes.toFloat() / maxVal) * revealFraction
                val barHeight = size.height * fraction
                val x = gap / 2f + index * (barWidth + gap)
                drawRect(color = trackColor, topLeft = Offset(x, 0f), size = Size(barWidth, size.height))
                drawRect(color = barColor, topLeft = Offset(x, size.height - barHeight), size = Size(barWidth, barHeight))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            data.forEach { (label, _) ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategoryProgressRow(label: String, count: Int, total: Int) {
    val spacing = SupportDeskThemeTokens.spacing
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(110.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.weight(1f))
        Text("$count", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MonthlyHistoryRow(label: String, ticketCount: Int, minutes: Int) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            "$ticketCount ticket${if (ticketCount == 1) "" else "s"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(spacing.md))
        Text(
            formatSupportDeskDuration(minutes),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── ACTIVIDAD ─────────────────────────────────────────────────────────────────

@Composable
private fun ClientActivityScreen(activityItems: List<ClientActivityItem>) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val today = remember { currentIsoDate() }
    var activityFilter by remember { mutableStateOf<String?>(null) }

    val filteredItems = remember(activityItems, activityFilter, today) {
        when (activityFilter) {
            "week" -> {
                val weekStart = isoDateMinus(today, 7)
                activityItems.filter { it.date >= weekStart }
            }
            "month" -> activityItems.filter { it.date.take(7) == today.take(7) }
            else -> activityItems
        }
    }

    val totalMinutes = remember(filteredItems) { filteredItems.sumOf { it.minutes } }
    val statusChangeCount = remember(filteredItems) {
        filteredItems.count { it.type == ClientActivityType.STATUS_CHANGE || it.type == ClientActivityType.RESOLVED || it.type == ClientActivityType.CLOSED }
    }

    val grouped = remember(filteredItems) {
        filteredItems.groupBy { it.date }.toList().sortedByDescending { it.first }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        SupportDeskEntrance(index = 0) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text("Actividad", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Historial de eventos de tus tickets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SupportDeskEntrance(index = 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                listOf(null to "Todo", "week" to "Esta semana", "month" to "Este mes").forEach { (key, label) ->
                    FilterChip(
                        selected = activityFilter == key,
                        onClick = { activityFilter = key },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        }

        SupportDeskEntrance(index = 2, horizontal = true) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                SupportDeskBadge(
                    text = "${filteredItems.size} eventos",
                    containerColor = semantic.infoContainer,
                    contentColor = semantic.info,
                )
                if (totalMinutes > 0) {
                    SupportDeskBadge(
                        text = formatSupportDeskDuration(totalMinutes),
                        containerColor = semantic.successContainer,
                        contentColor = semantic.success,
                    )
                }
                SupportDeskBadge(
                    text = "$statusChangeCount cambios de estado",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        if (filteredItems.isEmpty()) {
            SupportDeskEntrance(index = 3) {
                EmptyState(
                    title = "Sin actividad",
                    message = "No hay eventos en el período seleccionado.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                grouped.forEach { (date, dayItems) ->
                    item(key = "header_$date") {
                        Text(
                            text = formatSupportDeskDateTime(date),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = spacing.xs),
                        )
                    }
                    items(dayItems, key = { "${it.date}_${it.ticketNumber}_${it.type}_${it.description.take(20)}" }) { item ->
                        ActivityItemRow(item = item, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityItemRow(item: ClientActivityItem, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            ActivityTypeChip(type = item.type)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = item.description,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.ticketNumber.isNotBlank()) {
                        SupportDeskBadge(
                            text = item.ticketNumber,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Text(
                    text = item.ticketSubject,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.actor.isNotBlank()) {
                    Text(
                        text = item.actor,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTypeChip(type: ClientActivityType) {
    val semantic = SupportDeskThemeTokens.semanticColors
    val (bg, fg, label) = when (type) {
        ClientActivityType.CREATED -> Triple(semantic.infoContainer, semantic.info, "N")
        ClientActivityType.STATUS_CHANGE -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, "→")
        ClientActivityType.TIME_LOGGED -> Triple(semantic.successContainer, semantic.success, "T")
        ClientActivityType.RESOLVED -> Triple(semantic.successContainer, semantic.success, "✓")
        ClientActivityType.CLOSED -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "✗")
        ClientActivityType.RATED -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary, "★")
    }
    Box(
        modifier = Modifier
            .padding(top = 2.dp)
            .size(24.dp)
            .background(bg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

// ── MI CUENTA ─────────────────────────────────────────────────────────────────

@Composable
private fun ClientAccountScreen(
    clientName: String,
    contactName: String = "",
    tickets: List<Ticket>,
    today: String,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val allEntries = remember(tickets) { tickets.flatMap { it.timeEntries } }
    val totalTickets = tickets.size
    val resolvedTickets = remember(tickets) { tickets.count { it.status == TicketStatus.RESOLVED || it.status == TicketStatus.CLOSED } }
    val totalMinutes = remember(allEntries) { allEntries.sumOf { it.minutes } }
    val ratedTickets = remember(tickets) { tickets.filter { it.satisfactionRating != null } }
    val avgSatisfaction = remember(ratedTickets) {
        if (ratedTickets.isEmpty()) null
        else ratedTickets.sumOf { it.satisfactionRating!! }.toFloat() / ratedTickets.size
    }
    val thisMonthTickets = remember(tickets, today) { tickets.count { it.createdAt.take(7) == today.take(7) } }
    val thisMonthMinutes = remember(allEntries, today) { allEntries.filter { it.workDate.take(7) == today.take(7) }.sumOf { it.minutes } }
    val lastTicketDate = remember(tickets) { tickets.maxOfOrNull { it.updatedAt }?.take(10) ?: "—" }
    val initials = remember(clientName) { clientName.initials() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        SupportDeskEntrance(index = 0) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text("Mi Cuenta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Perfil y acceso al portal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SupportDeskEntrance(index = 1, horizontal = true) {
            SectionCard(title = "Perfil", neonAccentColor = MaterialTheme.colorScheme.primary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                ),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = clientName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (contactName.isNotBlank()) {
                            Text(
                                text = contactName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            SupportDeskBadge(
                                text = "Portal activo",
                                containerColor = SupportDeskThemeTokens.semanticColors.successContainer,
                                contentColor = SupportDeskThemeTokens.semanticColors.success,
                            )
                            SupportDeskBadge(
                                text = "Cliente verificado",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
        }

        SupportDeskEntrance(index = 2) {
            SectionCard(title = "Estadísticas globales") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        MetricCard(
                            label = "Total tickets",
                            value = totalTickets.toString(),
                            supportingText = "desde el inicio",
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            label = "Resueltos",
                            value = resolvedTickets.toString(),
                            supportingText = "cerrados o resueltos",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        MetricCard(
                            label = "Horas de soporte",
                            value = formatSupportDeskDuration(totalMinutes),
                            supportingText = "tiempo total",
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            label = "Satisfacción",
                            value = avgSatisfaction?.let { "%.1f/5".format(it) } ?: "—",
                            supportingText = if (ratedTickets.isEmpty()) "Sin valoraciones" else "${ratedTickets.size} valoraciones",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        SupportDeskEntrance(index = 3) {
            SectionCard(title = "Plan de soporte", subtitle = "Contacta con tu admin para más información") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    ServiceTierCard(name = "Standard", description = "Soporte estándar", modifier = Modifier.weight(1f))
                    ServiceTierCard(name = "Prioritario", description = "Respuesta rápida", modifier = Modifier.weight(1f))
                    ServiceTierCard(name = "VIP", description = "Soporte dedicado", modifier = Modifier.weight(1f))
                }
            }
        }

        SupportDeskEntrance(index = 4) {
            SectionCard(title = "Resumen de actividad") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    AccountStatRow(label = "Último ticket actualizado", value = lastTicketDate)
                    AccountStatRow(label = "Tickets este mes", value = thisMonthTickets.toString())
                    AccountStatRow(label = "Horas este mes", value = formatSupportDeskDuration(thisMonthMinutes))
                }
            }
        }

        SupportDeskEntrance(index = 5) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                SecondaryButton(text = "Actualizar", onClick = onRefresh)
                Surface(
                    onClick = onSignOut,
                    color = SupportDeskThemeTokens.semanticColors.dangerContainer,
                    contentColor = SupportDeskThemeTokens.semanticColors.danger,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "Cerrar sesión",
                        modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceTierCard(name: String, description: String, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountStatRow(label: String, value: String) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

// ── TABLERO (BOARD) ───────────────────────────────────────────────────────────

private val StatusBoardConfig = listOf(
    TicketStatus.OPEN to "Abiertos",
    TicketStatus.IN_PROGRESS to "En proceso",
    TicketStatus.PENDING_CLIENT to "Pendiente",
    TicketStatus.RESOLVED to "Resueltos",
    TicketStatus.CLOSED to "Cerrados",
)

private val KanbanColumns = listOf(
    TicketStatus.OPEN to "Abiertos",
    TicketStatus.IN_PROGRESS to "En proceso",
    TicketStatus.PENDING_CLIENT to "Pendiente",
    TicketStatus.RESOLVED to "Resueltos",
    TicketStatus.CLOSED to "Cerrados",
)

@Composable
private fun ClientStatusBoard(tickets: List<Ticket>) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val statusColors = listOf(
        semantic.info,
        MaterialTheme.colorScheme.primary,
        semantic.warning,
        semantic.success,
        MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val countByStatus = remember(tickets) { tickets.groupingBy { it.status }.eachCount() }
    val activeCount = (countByStatus[TicketStatus.OPEN] ?: 0) + (countByStatus[TicketStatus.IN_PROGRESS] ?: 0)
    SectionCard(
        title = "Estado del soporte",
        subtitle = "${tickets.size} tickets · $activeCount activos",
        neonAccentColor = if (activeCount > 0) MaterialTheme.colorScheme.primary else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            StatusBoardConfig.forEachIndexed { index, (status, label) ->
                SupportDeskEntrance(
                    index = index,
                    modifier = Modifier.weight(1f),
                ) {
                    StatusPillCard(
                        label = label,
                        count = countByStatus[status] ?: 0,
                        accentColor = statusColors[index],
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPillCard(
    label: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val animatedCount by animateIntAsState(count, tween(700), label = "status_count")
    Surface(
        modifier = modifier,
        color = accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.26f)),
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accentColor.copy(alpha = 0.72f), RoundedCornerShape(2.dp)),
            )
            Text(
                text = animatedCount.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ClientBoardScreen(
    tickets: List<Ticket>,
    onTicketClick: (String) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val columnColors = listOf(
        semantic.info,
        MaterialTheme.colorScheme.primary,
        semantic.warning,
        semantic.success,
        MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        SupportDeskEntrance(index = 0) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text("Tablero", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${tickets.size} tickets · Vista kanban",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth >= 700.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    KanbanColumns.forEachIndexed { idx, (status, label) ->
                        val colTickets = remember(tickets, status) { tickets.filter { it.status == status } }
                        KanbanColumn(
                            label = label,
                            accentColor = columnColors[idx],
                            tickets = colTickets,
                            onTicketClick = onTicketClick,
                            scrollable = true,
                            modifier = Modifier.width(240.dp).fillMaxHeight(),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    KanbanColumns.forEachIndexed { idx, (status, label) ->
                        val colTickets = tickets.filter { it.status == status }
                        if (colTickets.isNotEmpty()) {
                            item(key = "col_${status.name}") {
                                SupportDeskEntrance(index = idx) {
                                    KanbanColumn(
                                        label = label,
                                        accentColor = columnColors[idx],
                                        tickets = colTickets,
                                        onTicketClick = onTicketClick,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KanbanColumn(
    label: String,
    accentColor: Color,
    tickets: List<Ticket>,
    onTicketClick: (String) -> Unit,
    scrollable: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val hasUrgent = tickets.any { it.priority == TicketPriority.URGENT }
    val animatedCount by animateIntAsState(targetValue = tickets.size, animationSpec = tween(300), label = "kanbanCount")
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = accentColor.copy(alpha = 0.06f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.20f)),
    ) {
        Column(
            modifier = if (scrollable) Modifier.fillMaxSize().padding(spacing.sm) else Modifier.padding(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                    )
                    if (hasUrgent) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(SupportDeskThemeTokens.semanticColors.danger, CircleShape),
                        )
                    }
                }
                SupportDeskBadge(
                    text = animatedCount.toString(),
                    containerColor = accentColor.copy(alpha = 0.18f),
                    contentColor = accentColor,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(accentColor.copy(alpha = 0.42f), RoundedCornerShape(1.dp)),
            )
            if (tickets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Sin tickets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.54f),
                    )
                }
            } else if (scrollable) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    itemsIndexed(tickets, key = { _, t -> t.id }) { _, ticket ->
                        KanbanTicketCard(
                            ticket = ticket,
                            onClick = { onTicketClick(ticket.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    tickets.forEachIndexed { index, ticket ->
                        SupportDeskEntrance(index = index) {
                            KanbanTicketCard(
                                ticket = ticket,
                                onClick = { onTicketClick(ticket.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KanbanTicketCard(ticket: Ticket, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val elevation by animateDpAsState(
        targetValue = if (hovered) 8.dp else 1.dp,
        animationSpec = tween(180),
        label = "kanban_elev",
    )
    val priorityAccent = when (ticket.priority) {
        TicketPriority.LOW -> MaterialTheme.colorScheme.secondary
        TicketPriority.MEDIUM -> semantic.info
        TicketPriority.HIGH -> semantic.warning
        TicketPriority.URGENT -> semantic.danger
    }
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(priorityAccent),
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = spacing.sm, vertical = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = ticket.ticketNumber.ifBlank { "#${ticket.id.take(6)}" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TicketPriorityBadge(ticket.priority)
            }
            Text(
                text = ticket.subject,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TicketCategoryBadge(ticket.category)
                SupportPlatformBadge(ticket.platform)
            }
            ticket.assignee?.let { assignee ->
                Text(
                    text = assignee.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            }
        }
    }
}
