package com.requena.supportdesk.app.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
import com.requena.supportdesk.features.auth.presentation.effect.AuthUiEffect
import com.requena.supportdesk.features.auth.presentation.event.AuthUiEvent
import com.requena.supportdesk.features.auth.presentation.state.AuthUiState
import com.requena.supportdesk.features.clients.presentation.effect.ClientsUiEffect
import com.requena.supportdesk.features.clients.presentation.event.ClientsUiEvent
import com.requena.supportdesk.features.clients.presentation.state.ClientsUiState
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import kotlinx.coroutines.launch

private enum class MobileTab(
    val title: String,
    val navLabel: String,
) {
    SUMMARY(
        title = "Resumen",
        navLabel = "Hoy",
    ),
    TASKS(
        title = "Tareas",
        navLabel = "Tareas",
    ),
    CLIENTS(
        title = "Clientes",
        navLabel = "Clientes",
    ),
    LABELS(
        title = "Etiquetas",
        navLabel = "Etiquetas",
    ),
}

private enum class TaskVisibilityFilter(val label: String) {
    ALL("Todas"),
    OPEN("Abiertas"),
    COMPLETED("Hechas"),
}

private data class ClientOverview(
    val client: Client,
    val loggedMinutes: Int,
    val billableMinutes: Int,
    val openTasks: Int,
)

private data class LabelOverview(
    val category: TaskCategory,
    val loggedMinutes: Int,
    val openTasks: Int,
    val clientsCount: Int,
)

private data class CalendarMonth(
    val year: Int,
    val month: Int,
) {
    val label: String
        get() = "${SPANISH_MONTH_NAMES[month - 1]} de $year"

    fun previous(): CalendarMonth = if (month == 1) CalendarMonth(year - 1, 12) else CalendarMonth(year, month - 1)

    fun next(): CalendarMonth = if (month == 12) CalendarMonth(year + 1, 1) else CalendarMonth(year, month + 1)
}

private data class CalendarDay(
    val isoDate: String,
    val year: Int,
    val month: Int,
    val dayNumber: String,
    val minutes: Int,
    val inCurrentMonth: Boolean,
    val selected: Boolean,
)

@Composable
fun MobileWorkspaceApp() {
    val module = remember { MobileAppModule() }
    var currentTab by remember { mutableStateOf(MobileTab.SUMMARY) }
    var statusMessage by remember { mutableStateOf("Movil listo.") }

    val authState by module.authViewModel.state.collectAsState()
    val currentUser = authState.authenticatedUser
    val tasksState = if (currentUser != null) {
        module.tasksViewModel.state.collectAsState().value
    } else {
        TasksUiState()
    }
    val clientsState = if (currentUser != null) {
        module.clientsViewModel.state.collectAsState().value
    } else {
        ClientsUiState()
    }

    DisposableEffect(module) {
        onDispose { module.clear() }
    }

    LaunchedEffect(module) {
        launch {
            module.authViewModel.effects.collect { effect ->
                when (effect) {
                    AuthUiEffect.NavigateToHome -> {
                        currentTab = MobileTab.SUMMARY
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

    LaunchedEffect(currentUser?.id) {
        currentUser?.id ?: return@LaunchedEffect

        currentTab = MobileTab.SUMMARY
        statusMessage = "Sesion iniciada como ${currentUser.name}"

        module.clientsViewModel.onEvent(ClientsUiEvent.Load)

        module.tasksViewModel.onEvent(TasksUiEvent.SelectTask(null))
        module.tasksViewModel.onEvent(TasksUiEvent.SelectCategory(null))
        module.tasksViewModel.onEvent(TasksUiEvent.SelectClientFilter(null))
        module.tasksViewModel.onEvent(TasksUiEvent.SelectDashboardClient(null))
        module.tasksViewModel.onEvent(TasksUiEvent.SelectDay(currentIsoDate()))
        module.tasksViewModel.onEvent(TasksUiEvent.Load)
    }

    SupportDeskMobileTheme {
        if (currentUser == null) {
            MobileLoginScreen(
                state = authState,
                onEvent = module.authViewModel::onEvent,
            )
        } else {
            MobileReadOnlyShell(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                statusMessage = tasksState.errorMessage ?: tasksState.statusMessage ?: statusMessage,
                currentUserName = currentUser.name,
                onRefresh = {
                    module.tasksViewModel.onEvent(TasksUiEvent.Load)
                    module.clientsViewModel.onEvent(ClientsUiEvent.Load)
                    statusMessage = "Actualizando."
                },
                onSignOut = {
                    currentTab = MobileTab.SUMMARY
                    module.authViewModel.onEvent(AuthUiEvent.Logout)
                },
                tasksState = tasksState,
                clientsState = clientsState,
                onTasksEvent = module.tasksViewModel::onEvent,
                onClientsEvent = module.clientsViewModel::onEvent,
            )
        }
    }
}

@Composable
private fun MobileLoginScreen(
    state: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 24.dp),
        ) {
            val compactLayout = maxWidth < SupportDeskBreakpoints.mobileCompact

            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            ) {
                PhoneCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f),
                ) {
                    Text(
                        text = "OryKai software Mobile",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (compactLayout) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TagChip(
                                text = "Solo lectura",
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            )
                            TagChip(
                                text = "Movil",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TagChip(
                                text = "Solo lectura",
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            )
                            TagChip(
                                text = "Movil",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                PhoneCard(modifier = Modifier.fillMaxWidth()) {
                    CardHeader(title = "Entrar")
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { onEvent(AuthUiEvent.EmailChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Correo") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { onEvent(AuthUiEvent.PasswordChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Contrasena") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    state.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    ActionButton(
                        text = if (state.isLoading) "Entrando..." else "Entrar",
                        emphasized = true,
                        onClick = { onEvent(AuthUiEvent.Submit) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileReadOnlyShell(
    currentTab: MobileTab,
    onTabSelected: (MobileTab) -> Unit,
    statusMessage: String?,
    currentUserName: String,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    tasksState: TasksUiState,
    clientsState: ClientsUiState,
    onTasksEvent: (TasksUiEvent) -> Unit,
    onClientsEvent: (ClientsUiEvent) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            MobileBottomBar(
                currentTab = currentTab,
                onTabSelected = onTabSelected,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MobileHeader(
                currentTab = currentTab,
                currentUserName = currentUserName,
                statusMessage = statusMessage ?: "Consulta",
                onRefresh = onRefresh,
                onSignOut = onSignOut,
            )

            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    MobileTab.SUMMARY -> MobileSummaryScreen(
                        tasksState = tasksState,
                        clientsState = clientsState,
                        onTasksEvent = onTasksEvent,
                    )

                    MobileTab.TASKS -> MobileTasksScreen(
                        tasksState = tasksState,
                        clients = clientsState.clients,
                        onTasksEvent = onTasksEvent,
                    )

                    MobileTab.CLIENTS -> MobileClientsScreen(
                        clientsState = clientsState,
                        tasksState = tasksState,
                        onClientsEvent = onClientsEvent,
                    )

                    MobileTab.LABELS -> MobileLabelsScreen(
                        tasksState = tasksState,
                        clients = clientsState.clients,
                        onTasksEvent = onTasksEvent,
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileHeader(
    currentTab: MobileTab,
    currentUserName: String,
    statusMessage: String,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    PhoneCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compactLayout = maxWidth < SupportDeskBreakpoints.mobileCompact

            if (compactLayout) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Hola, ${currentUserName.substringBefore(" ").ifBlank { "admin" }}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = currentTab.title,
                            style = MaterialTheme.typography.displayMedium,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(text = "Actualizar", compact = true, onClick = onRefresh)
                        ActionButton(text = "Salir", compact = true, onClick = onSignOut)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Hola, ${currentUserName.substringBefore(" ").ifBlank { "admin" }}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = currentTab.title,
                            style = MaterialTheme.typography.displayMedium,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(text = "Actualizar", compact = true, onClick = onRefresh)
                        ActionButton(text = "Salir", compact = true, onClick = onSignOut)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TagChip(
                text = "Solo lectura",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            statusMessage.takeIf { it.isNotBlank() }?.let {
                TagChip(
                    text = it,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MobileBottomBar(
    currentTab: MobileTab,
    onTabSelected: (MobileTab) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MobileTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onTabSelected(tab) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                        },
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = tab.navLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileSummaryScreen(
    tasksState: TasksUiState,
    clientsState: ClientsUiState,
    onTasksEvent: (TasksUiEvent) -> Unit,
) {
    val tasksById = remember(tasksState.tasks) { tasksState.tasks.associateBy { it.id } }
    val clientsById = remember(clientsState.clients) { clientsState.clients.associateBy { it.id } }
    val categoriesById = remember(tasksState.categories) { tasksState.categories.associateBy { it.id } }
    val selectedDayLogs = remember(tasksState.selectedDayLogs) {
        tasksState.selectedDayLogs.sortedByDescending { it.createdAt }
    }
    val topClients = remember(clientsState.clients, tasksState.tasks, tasksState.logs) {
        buildClientOverviews(
            clients = clientsState.clients,
            tasks = tasksState.tasks,
            logs = tasksState.logs,
        ).take(4)
    }
    val highlightedTasks = remember(tasksState.tasks) {
        tasksState.tasks
            .sortedWith(compareBy<WorkTask> { it.completed }.thenByDescending { it.updatedAt })
            .take(4)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            OverviewHeroCard(
                todayMinutes = tasksState.selectedDayMinutes,
                monthMinutes = tasksState.logs.sumOf { it.minutes },
                billableMinutes = tasksState.logs.filter { it.billable }.sumOf { it.minutes },
                openTasks = tasksState.tasks.count { !it.completed },
                labelsCount = tasksState.categories.size,
                selectedDay = tasksState.selectedDay ?: "-",
            )
        }
        item {
            MobileCalendarCard(
                tasksState = tasksState,
                onSelectDay = { onTasksEvent(TasksUiEvent.SelectDay(it)) },
            )
        }
        item {
            SelectedDayLogsCard(
                selectedDay = tasksState.selectedDay ?: "-",
                logs = selectedDayLogs,
                tasksById = tasksById,
                clientsById = clientsById,
                categoriesById = categoriesById,
            )
        }
        item {
            TopClientsCard(topClients = topClients)
        }
        item {
            HighlightedTasksCard(
                tasks = highlightedTasks,
                clientsById = clientsById,
                categoriesById = categoriesById,
            )
        }
    }
}

@Composable
private fun OverviewHeroCard(
    todayMinutes: Int,
    monthMinutes: Int,
    billableMinutes: Int,
    openTasks: Int,
    labelsCount: Int,
    selectedDay: String,
) {
    PhoneCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
    ) {
        CardHeader(title = "Carga actual")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoMetric(
                label = "Dia",
                value = formatSupportDeskDuration(todayMinutes),
                supporting = selectedDay,
                modifier = Modifier.weight(1f),
            )
            InfoMetric(
                label = "Mes",
                value = formatSupportDeskDuration(monthMinutes),
                supporting = "${formatSupportDeskDuration(billableMinutes)} facturable",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoMetric(
                label = "Abiertas",
                value = openTasks.toString(),
                supporting = "tareas activas",
                modifier = Modifier.weight(1f),
            )
            InfoMetric(
                label = "Etiquetas",
                value = labelsCount.toString(),
                supporting = "grupos activos",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MobileCalendarCard(
    tasksState: TasksUiState,
    onSelectDay: (String) -> Unit,
) {
    val initialMonth = remember(tasksState.selectedDay) {
        tasksState.selectedDay?.let(::parseCalendarMonth) ?: CalendarMonth(2026, 4)
    }
    var visibleMonth by remember { mutableStateOf(initialMonth) }

    LaunchedEffect(initialMonth.year, initialMonth.month) {
        visibleMonth = initialMonth
    }

    val maxMinutes = maxOf(1, tasksState.logs.maxOfOrNull { it.minutes } ?: 0)
    val cells = remember(tasksState.logs, tasksState.selectedDay, visibleMonth) {
        buildCalendarCells(
            month = visibleMonth,
            logs = tasksState.logs,
            selectedDay = tasksState.selectedDay,
        )
    }

    PhoneCard(modifier = Modifier.fillMaxWidth()) {
        CardHeader(
            title = "Calendario",
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        text = "<",
                        compact = true,
                        onClick = { visibleMonth = visibleMonth.previous() },
                    )
                    ActionButton(
                        text = ">",
                        compact = true,
                        onClick = { visibleMonth = visibleMonth.next() },
                    )
                }
            },
        )
        Text(
            text = visibleMonth.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SPANISH_WEEKDAY_LABELS.forEach { weekday ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = weekday,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        cells.chunked(7).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { day ->
                    MobileCalendarDayCell(
                        day = day,
                        maxMinutes = maxMinutes,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onSelectDay(day.isoDate)
                            if (!day.inCurrentMonth) {
                                visibleMonth = CalendarMonth(day.year, day.month)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileCalendarDayCell(
    day: CalendarDay,
    maxMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fillAlpha = if (day.minutes == 0) 0.08f else 0.14f + (day.minutes.toFloat() / maxMinutes.toFloat()) * 0.3f
    val backgroundColor = when {
        day.selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        day.inCurrentMonth -> MaterialTheme.colorScheme.primary.copy(alpha = fillAlpha)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    }
    val contentColor = when {
        day.selected -> MaterialTheme.colorScheme.primary
        day.inCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .aspectRatio(0.92f)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = backgroundColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (day.selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = day.dayNumber,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (day.selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
            )
            Text(
                text = if (day.minutes > 0 && day.inCurrentMonth) "${day.minutes}m" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectedDayLogsCard(
    selectedDay: String,
    logs: List<TaskLog>,
    tasksById: Map<String, WorkTask>,
    clientsById: Map<String, Client>,
    categoriesById: Map<String, TaskCategory>,
) {
    PhoneCard(modifier = Modifier.fillMaxWidth()) {
        CardHeader(
            title = "Actividad del dia",
            subtitle = selectedDay,
        )
        if (logs.isEmpty()) {
            EmptyPhoneState(
                title = "Sin registros",
            )
        } else {
            logs.forEach { log ->
                val task = tasksById[log.taskId]
                val client = log.clientId?.let(clientsById::get)
                val category = task?.categoryId?.let(categoriesById::get)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = task?.title ?: "Tarea sin resolver",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = client?.companyName ?: "Trabajo interno",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TagChip(
                                text = formatSupportDeskDuration(log.minutes),
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            category?.let {
                                TagChip(
                                    text = it.name,
                                    containerColor = parseColor(
                                        hex = it.colorHex,
                                        fallback = MaterialTheme.colorScheme.primary,
                                    ).copy(alpha = 0.2f),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            TagChip(
                                text = if (log.billable) "Facturable" else "Interno",
                                containerColor = if (log.billable) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                },
                                contentColor = if (log.billable) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                },
                            )
                        }
                        if (log.note.isNotBlank()) {
                            Text(
                                text = log.note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopClientsCard(
    topClients: List<ClientOverview>,
) {
    PhoneCard(modifier = Modifier.fillMaxWidth()) {
        CardHeader(title = "Clientes con mas carga")
        if (topClients.isEmpty()) {
            EmptyPhoneState(
                title = "Sin clientes",
            )
        } else {
            topClients.forEach { summary ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = summary.client.companyName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = summary.client.companyName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${summary.openTasks} tareas abiertas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatSupportDeskDuration(summary.loggedMinutes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${formatSupportDeskDuration(summary.billableMinutes)} facturable",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightedTasksCard(
    tasks: List<WorkTask>,
    clientsById: Map<String, Client>,
    categoriesById: Map<String, TaskCategory>,
) {
    PhoneCard(modifier = Modifier.fillMaxWidth()) {
        CardHeader(title = "Tareas destacadas")
        if (tasks.isEmpty()) {
            EmptyPhoneState(
                title = "Sin tareas",
            )
        } else {
            tasks.forEach { task ->
                TaskRowCard(
                    task = task,
                    client = task.clientId?.let(clientsById::get),
                    category = categoriesById[task.categoryId],
                    selected = false,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun MobileTasksScreen(
    tasksState: TasksUiState,
    clients: List<Client>,
    onTasksEvent: (TasksUiEvent) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf(TaskVisibilityFilter.ALL) }

    val clientsById = remember(clients) { clients.associateBy { it.id } }
    val categoriesById = remember(tasksState.categories) { tasksState.categories.associateBy { it.id } }
    val visibleTasks = remember(tasksState.filteredTasks, query, visibility) {
        tasksState.filteredTasks
            .filter { task ->
                query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)
            }
            .filter { task ->
                when (visibility) {
                    TaskVisibilityFilter.ALL -> true
                    TaskVisibilityFilter.OPEN -> !task.completed
                    TaskVisibilityFilter.COMPLETED -> task.completed
                }
            }
            .sortedByDescending { it.updatedAt }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PhoneCard(modifier = Modifier.fillMaxWidth()) {
                CardHeader(title = "Tareas")
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar por nombre o descripcion") },
                    singleLine = true,
                )
                TaskVisibilityRow(
                    selected = visibility,
                    onSelected = { visibility = it },
                )
                LabelFilterRow(
                    categories = tasksState.categories,
                    selectedCategoryId = tasksState.selectedCategoryId,
                    onSelect = { onTasksEvent(TasksUiEvent.SelectCategory(it)) },
                )
            }
        }
        tasksState.selectedTask?.let { selectedTask ->
            item {
                SelectedTaskCard(
                    task = selectedTask,
                    client = selectedTask.clientId?.let(clientsById::get),
                    category = categoriesById[selectedTask.categoryId],
                )
            }
        }
        if (visibleTasks.isEmpty()) {
            item {
                PhoneCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyPhoneState(
                        title = "Sin tareas visibles",
                    )
                }
            }
        } else {
            items(visibleTasks, key = { it.id }) { task ->
                TaskRowCard(
                    task = task,
                    client = task.clientId?.let(clientsById::get),
                    category = categoriesById[task.categoryId],
                    selected = task.id == tasksState.selectedTaskId,
                    onClick = { onTasksEvent(TasksUiEvent.SelectTask(task.id)) },
                )
            }
        }
    }
}

@Composable
private fun TaskVisibilityRow(
    selected: TaskVisibilityFilter,
    onSelected: (TaskVisibilityFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TaskVisibilityFilter.entries.forEach { filter ->
            val active = filter == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onSelected(filter) },
                shape = MaterialTheme.shapes.small,
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelFilterRow(
    categories: List<TaskCategory>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                text = "Todas",
                selected = selectedCategoryId == null,
                onClick = { onSelect(null) },
            )
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(
                text = category.name,
                selected = selectedCategoryId == category.id,
                tint = parseColor(
                    hex = category.colorHex,
                    fallback = MaterialTheme.colorScheme.primary,
                ),
                onClick = {
                    onSelect(
                        if (selectedCategoryId == category.id) null else category.id,
                    )
                },
            )
        }
    }
}

@Composable
private fun SelectedTaskCard(
    task: WorkTask,
    client: Client?,
    category: TaskCategory?,
) {
    PhoneCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
    ) {
        CardHeader(
            title = task.title,
            subtitle = client?.companyName ?: "Trabajo interno",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            category?.let {
                TagChip(
                    text = it.name,
                    containerColor = parseColor(
                        hex = it.colorHex,
                        fallback = MaterialTheme.colorScheme.primary,
                    ).copy(alpha = 0.22f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
            TagChip(
                text = if (task.completed) "Hecha" else "Activa",
                containerColor = if (task.completed) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (task.completed) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            )
        }
        if (task.description.isNotBlank()) {
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ValuePair(label = "Horas registradas", value = formatSupportDeskDuration(task.loggedMinutes))
        task.dueDate?.takeIf { it.isNotBlank() }?.let { dueDate ->
            ValuePair(label = "Vencimiento", value = dueDate)
        }
        ValuePair(label = "Ultima actualizacion", value = formatSupportDeskDateTime(task.updatedAt))
    }
}

@Composable
private fun TaskRowCard(
    task: WorkTask,
    client: Client?,
    category: TaskCategory?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    category?.let {
                        TagChip(
                            text = it.name,
                            containerColor = parseColor(
                                hex = it.colorHex,
                                fallback = MaterialTheme.colorScheme.primary,
                            ).copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    TagChip(
                        text = if (task.completed) "Hecha" else "Abierta",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatSupportDeskDuration(task.loggedMinutes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = client?.companyName ?: "Sin cliente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatSupportDeskDateTime(task.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MobileClientsScreen(
    clientsState: ClientsUiState,
    tasksState: TasksUiState,
    onClientsEvent: (ClientsUiEvent) -> Unit,
) {
    val selectedClient = clientsState.selectedClient
    val selectedOverview = remember(selectedClient, tasksState.tasks, tasksState.logs) {
        selectedClient?.let { client ->
            buildClientOverviews(
                clients = listOf(client),
                tasks = tasksState.tasks,
                logs = tasksState.logs,
            ).firstOrNull()
        }
    }
    val selectedClientTasks = remember(selectedClient, tasksState.tasks) {
        tasksState.tasks
            .filter { task -> task.clientId == selectedClient?.id }
            .sortedByDescending { it.updatedAt }
            .take(4)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PhoneCard(modifier = Modifier.fillMaxWidth()) {
                CardHeader(title = "Clientes")
                OutlinedTextField(
                    value = clientsState.query,
                    onValueChange = { onClientsEvent(ClientsUiEvent.SearchChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar empresa, producto, contacto o correo") },
                    singleLine = true,
                )
            }
        }
        item {
            if (selectedClient == null || selectedOverview == null) {
                PhoneCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyPhoneState(
                        title = "Sin cliente seleccionado",
                    )
                }
            } else {
                SelectedClientCard(
                    summary = selectedOverview,
                    recentTasks = selectedClientTasks,
                )
            }
        }
        if (clientsState.clients.isEmpty()) {
            item {
                PhoneCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyPhoneState(
                        title = "Sin clientes visibles",
                        message = clientsState.errorMessage,
                    )
                }
            }
        } else {
            items(clientsState.clients, key = { it.id }) { client ->
                val summary = buildClientOverviews(
                    clients = listOf(client),
                    tasks = tasksState.tasks,
                    logs = tasksState.logs,
                ).first()
                ClientRowCard(
                    summary = summary,
                    selected = client.id == clientsState.selectedClientId,
                    onClick = { onClientsEvent(ClientsUiEvent.SelectClient(client.id)) },
                )
            }
        }
    }
}

@Composable
private fun SelectedClientCard(
    summary: ClientOverview,
    recentTasks: List<WorkTask>,
) {
    PhoneCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
    ) {
        CardHeader(
            title = summary.client.companyName,
            subtitle = summary.client.productName.ifBlank { summary.client.contactName },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TagChip(
                text = summary.client.serviceTier.displayName(),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
            TagChip(
                text = summary.client.accountStatus.displayName(),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
            TagChip(
                text = summary.client.preferredContactChannel.displayName(),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
        ValuePair(label = "Contacto", value = summary.client.contactName)
        ValuePair(label = "Correo", value = summary.client.email)
        ValuePair(label = "Horas del mes", value = formatSupportDeskDuration(summary.loggedMinutes))
        ValuePair(label = "Horas facturables", value = formatSupportDeskDuration(summary.billableMinutes))
        ValuePair(label = "Tareas abiertas", value = summary.openTasks.toString())
        if (recentTasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Actividad reciente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            recentTasks.forEach { task ->
                ValuePair(
                    label = task.title,
                    value = formatSupportDeskDuration(task.loggedMinutes),
                    supporting = formatSupportDeskDateTime(task.updatedAt),
                )
            }
        }
    }
}

@Composable
private fun ClientRowCard(
    summary: ClientOverview,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        },
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = summary.client.companyName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = summary.client.productName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatSupportDeskDuration(summary.loggedMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip(
                    text = summary.client.serviceTier.displayName(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TagChip(
                    text = "${summary.openTasks} abiertas",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = summary.client.contactName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MobileLabelsScreen(
    tasksState: TasksUiState,
    clients: List<Client>,
    onTasksEvent: (TasksUiEvent) -> Unit,
) {
    val selectedLabel = tasksState.selectedCategory ?: tasksState.categories.firstOrNull()
    val overviews = remember(tasksState.categories, tasksState.tasks, tasksState.logs) {
        buildLabelOverviews(
            categories = tasksState.categories,
            tasks = tasksState.tasks,
            logs = tasksState.logs,
        )
    }
    val selectedOverview = remember(selectedLabel, overviews) {
        overviews.firstOrNull { it.category.id == selectedLabel?.id }
    }
    val selectedLabelTasks = remember(selectedLabel, tasksState.tasks) {
        tasksState.tasks
            .filter { task -> task.categoryId == selectedLabel?.id }
            .sortedByDescending { it.updatedAt }
            .take(5)
    }
    val clientsById = remember(clients) { clients.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            if (selectedOverview == null) {
                PhoneCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyPhoneState(
                        title = "Sin etiquetas",
                    )
                }
            } else {
                SelectedLabelCard(
                    overview = selectedOverview,
                    relatedTasks = selectedLabelTasks,
                    clientsById = clientsById,
                )
            }
        }
        if (overviews.isEmpty()) {
            item {
                PhoneCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyPhoneState(
                        title = "Nada que mostrar",
                    )
                }
            }
        } else {
            items(overviews, key = { it.category.id }) { overview ->
                LabelRowCard(
                    overview = overview,
                    selected = overview.category.id == selectedLabel?.id,
                    onClick = {
                        onTasksEvent(
                            TasksUiEvent.SelectCategory(
                                if (tasksState.selectedCategoryId == overview.category.id) null else overview.category.id,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectedLabelCard(
    overview: LabelOverview,
    relatedTasks: List<WorkTask>,
    clientsById: Map<String, Client>,
) {
    PhoneCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
    ) {
        CardHeader(
            title = overview.category.name,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(
                        parseColor(
                            hex = overview.category.colorHex,
                            fallback = MaterialTheme.colorScheme.primary,
                        ),
                        CircleShape,
                    ),
            )
            TagChip(
                text = overview.category.colorHex,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
        ValuePair(label = "Horas asociadas", value = formatSupportDeskDuration(overview.loggedMinutes))
        ValuePair(label = "Tareas abiertas", value = overview.openTasks.toString())
        ValuePair(label = "Clientes impactados", value = overview.clientsCount.toString())
        if (relatedTasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tareas relacionadas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            relatedTasks.forEach { task ->
                ValuePair(
                    label = task.title,
                    value = task.clientId?.let(clientsById::get)?.companyName ?: "Sin cliente",
                    supporting = formatSupportDeskDuration(task.loggedMinutes),
                )
            }
        }
    }
}

@Composable
private fun LabelRowCard(
    overview: LabelOverview,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        },
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        parseColor(
                            hex = overview.category.colorHex,
                            fallback = MaterialTheme.colorScheme.primary,
                        ),
                        CircleShape,
                    ),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = overview.category.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${overview.category.tasksCount} tareas - ${formatSupportDeskDuration(overview.loggedMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${overview.clientsCount} clientes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PhoneCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedColor = if (containerColor == Color.Unspecified) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    } else {
        containerColor
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = resolvedColor,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun CardHeader(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = trailing != null && maxWidth < SupportDeskBreakpoints.mobileStackedTrailing

        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                trailing.invoke()
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                trailing?.invoke()
            }
        }
    }
}

@Composable
private fun InfoMetric(
    label: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ValuePair(
    label: String,
    value: String,
    supporting: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        supporting?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
    }
}

@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val resolvedTint = tint ?: MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) resolvedTint.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (selected) resolvedTint.copy(alpha = 0.45f) else Color.Transparent),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    emphasized: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 12.dp,
                vertical = if (compact) 7.dp else 9.dp,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (emphasized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyPhoneState(
    title: String,
    message: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            message?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun buildClientOverviews(
    clients: List<Client>,
    tasks: List<WorkTask>,
    logs: List<TaskLog>,
): List<ClientOverview> = clients.map { client ->
    val clientTasks = tasks.filter { task -> task.clientId == client.id }
    val clientLogs = logs.filter { log -> log.clientId == client.id }
    ClientOverview(
        client = client,
        loggedMinutes = clientLogs.sumOf { it.minutes },
        billableMinutes = clientLogs.filter { it.billable }.sumOf { it.minutes },
        openTasks = clientTasks.count { !it.completed },
    )
}.sortedByDescending { it.loggedMinutes }

private fun buildLabelOverviews(
    categories: List<TaskCategory>,
    tasks: List<WorkTask>,
    logs: List<TaskLog>,
): List<LabelOverview> {
    val tasksByCategory = tasks.groupBy { it.categoryId }
    return categories.map { category ->
        val categoryTasks = tasksByCategory[category.id].orEmpty()
        val relatedTaskIds = categoryTasks.map { it.id }.toSet()
        val relatedLogs = logs.filter { log -> relatedTaskIds.contains(log.taskId) }
        val affectedClientIds = (
            relatedLogs.mapNotNull { it.clientId } +
                categoryTasks.mapNotNull { it.clientId }
            ).distinct()
        LabelOverview(
            category = category,
            loggedMinutes = relatedLogs.sumOf { it.minutes },
            openTasks = categoryTasks.count { !it.completed },
            clientsCount = affectedClientIds.size,
        )
    }.sortedByDescending { it.loggedMinutes }
}

private fun buildCalendarCells(
    month: CalendarMonth,
    logs: List<TaskLog>,
    selectedDay: String?,
): List<CalendarDay> {
    val firstDayOffset = dayOfWeekMondayIndex(month.year, month.month, 1)
    val currentMonthDays = daysInMonth(month.year, month.month)
    val previousMonth = month.previous()
    val previousMonthDays = daysInMonth(previousMonth.year, previousMonth.month)
    val totalCells = if (firstDayOffset + currentMonthDays <= 35) 35 else 42
    val trailingDays = totalCells - firstDayOffset - currentMonthDays
    val nextMonth = month.next()

    val leading = (previousMonthDays - firstDayOffset + 1..previousMonthDays).map { day ->
        buildCalendarDay(previousMonth.year, previousMonth.month, day, logs, selectedDay, false)
    }
    val current = (1..currentMonthDays).map { day ->
        buildCalendarDay(month.year, month.month, day, logs, selectedDay, true)
    }
    val trailing = (1..trailingDays).map { day ->
        buildCalendarDay(nextMonth.year, nextMonth.month, day, logs, selectedDay, false)
    }
    return leading + current + trailing
}

private fun buildCalendarDay(
    year: Int,
    month: Int,
    day: Int,
    logs: List<TaskLog>,
    selectedDay: String?,
    inCurrentMonth: Boolean,
): CalendarDay {
    val isoDate = isoDate(year, month, day)
    val minutes = logs.filter { it.workDate == isoDate }.sumOf { it.minutes }
    return CalendarDay(
        isoDate = isoDate,
        year = year,
        month = month,
        dayNumber = day.toString(),
        minutes = minutes,
        inCurrentMonth = inCurrentMonth,
        selected = selectedDay == isoDate,
    )
}

private fun parseCalendarMonth(isoDate: String): CalendarMonth {
    val parts = isoDate.split("-")
    return CalendarMonth(
        year = parts.getOrNull(0)?.toIntOrNull() ?: 2026,
        month = parts.getOrNull(1)?.toIntOrNull() ?: 4,
    )
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 30
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

private fun dayOfWeekMondayIndex(year: Int, month: Int, day: Int): Int {
    val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    var adjustedYear = year
    if (month < 3) adjustedYear -= 1
    val sundayBased = (adjustedYear + adjustedYear / 4 - adjustedYear / 100 + adjustedYear / 400 + offsets[month - 1] + day) % 7
    return if (sundayBased == 0) 6 else sundayBased - 1
}

private fun isoDate(year: Int, month: Int, day: Int): String =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

private fun parseColor(
    hex: String,
    fallback: Color,
): Color = runCatching {
    val value = hex.removePrefix("#").toLong(16)
    val red = ((value shr 16) and 0xFF).toInt() / 255f
    val green = ((value shr 8) and 0xFF).toInt() / 255f
    val blue = (value and 0xFF).toInt() / 255f
    Color(red = red, green = green, blue = blue, alpha = 1f)
}.getOrElse {
    fallback
}

private val SPANISH_WEEKDAY_LABELS = listOf("L", "M", "X", "J", "V", "S", "D")

private val SPANISH_MONTH_NAMES = listOf(
    "enero",
    "febrero",
    "marzo",
    "abril",
    "mayo",
    "junio",
    "julio",
    "agosto",
    "septiembre",
    "octubre",
    "noviembre",
    "diciembre",
)
