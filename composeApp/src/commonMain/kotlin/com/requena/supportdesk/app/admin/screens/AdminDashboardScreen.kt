package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.model.WorkTaskStatus
import com.requena.supportdesk.designsystem.components.badges.ClientAccountStatusBadge
import com.requena.supportdesk.designsystem.components.badges.ClientServiceTierBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskClockDuration
import com.requena.supportdesk.designsystem.theme.formatSupportDeskPreciseDuration
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import kotlin.math.max

@Composable
fun AdminDashboardScreen(
    clients: List<Client>,
    tasksState: TasksUiState,
    onTasksEvent: (TasksUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val selectedClient = clients.firstOrNull { it.id == tasksState.selectedDashboardClientId }
    val todayIsoDate = tasksState.todayIsoDate
    val selectedDay = tasksState.selectedDay ?: todayIsoDate
    val currentWeekStart = weekStartIsoDate(todayIsoDate)
    val currentWeekEnd = shiftIsoDate(currentWeekStart, 6)
    val currentMonthPrefix = todayIsoDate.take(7)
    val globalSeconds = tasksState.logs.filter { it.workDate.startsWith(currentMonthPrefix) }.sumOf { it.seconds }
    val globalBillableSeconds = tasksState.logs.filter { it.workDate.startsWith(currentMonthPrefix) && it.billable }.sumOf { it.seconds }
    val clientLogs = tasksState.logs.filter { log ->
        tasksState.selectedDashboardClientId == null || log.clientId == tasksState.selectedDashboardClientId
    }
    val clientMonthLogs = clientLogs.filter { it.workDate.startsWith(currentMonthPrefix) }
    val clientSeconds = clientMonthLogs.sumOf { it.seconds }
    val clientBillableSeconds = clientMonthLogs.filter { it.billable }.sumOf { it.seconds }
    val boardTasks = tasksState.tasks.filter { task ->
        (tasksState.selectedDashboardClientId == null || task.clientId == tasksState.selectedDashboardClientId) &&
            (tasksState.selectedCategoryId == null || task.categoryId == tasksState.selectedCategoryId)
    }
    val dashboardTasks = tasksState.tasks.filter { task ->
        (tasksState.selectedDashboardClientId == null || task.clientId == tasksState.selectedDashboardClientId) &&
            (tasksState.selectedCategoryId == null || task.categoryId == tasksState.selectedCategoryId) &&
            (task.id == tasksState.activeTaskId || task.dueDate == selectedDay)
    }.sortedWith(
        compareByDescending<WorkTask> { it.id == tasksState.activeTaskId }
            .thenBy { it.completed }
            .thenByDescending { it.updatedAt },
    )
    val selectedTask = tasksState.selectedTask?.takeIf { candidate -> dashboardTasks.any { it.id == candidate.id } } ?: dashboardTasks.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        if (tasksState.isLoading && tasksState.tasks.isEmpty()) {
            LoadingState(itemCount = 4)
        }

        tasksState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            SectionCard(title = "Datos no disponibles", subtitle = message) {
                SecondaryButton(text = "Reintentar", onClick = { onTasksEvent(TasksUiEvent.Load) })
            }
        }

        CounterHeroCard(
            clients = clients,
            selectedClient = selectedClient,
            categories = tasksState.categories,
            tasks = dashboardTasks,
            tasksState = tasksState,
            onTasksEvent = onTasksEvent,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            MetricCard(
                label = "Tarea activa",
                value = selectedTask?.title ?: "Sin tarea",
                supportingText = if (tasksState.activeTaskId != null) {
                    "Contador vinculado a la sesion actual."
                } else {
                    "Solo tareas iniciadas o programadas para $selectedDay."
                },
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Horas del mes",
                value = formatSupportDeskPreciseDuration(globalSeconds),
                supportingText = "${dashboardTasks.count()} tareas del dia visibles tras filtros.",
                modifier = Modifier.weight(1f),
            )
        }

        TimeAnalyticsSection(
            logs = clientLogs,
            weekStart = currentWeekStart,
            weekEnd = currentWeekEnd,
            monthPrefix = currentMonthPrefix,
            selectedClientName = selectedClient?.companyName,
        )

        StatusBoardSection(
            tasks = boardTasks,
            categories = tasksState.categories,
            selectedTaskId = tasksState.selectedTaskId,
            onSelectTask = { onTasksEvent(TasksUiEvent.SelectTask(it)) },
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 1180.dp
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    WheelSummaryRow(
                        globalSeconds = globalSeconds,
                        globalBillableSeconds = globalBillableSeconds,
                        clientName = selectedClient?.companyName ?: "Cliente",
                        clientSeconds = clientSeconds,
                        clientBillableSeconds = clientBillableSeconds,
                    )
                    CompactCalendarCard(
                        tasksState = tasksState,
                        onSelectDay = { onTasksEvent(TasksUiEvent.SelectDay(it)) },
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    WheelSummaryRow(
                        globalSeconds = globalSeconds,
                        globalBillableSeconds = globalBillableSeconds,
                        clientName = selectedClient?.companyName ?: "Cliente",
                        clientSeconds = clientSeconds,
                        clientBillableSeconds = clientBillableSeconds,
                        modifier = Modifier.weight(0.52f),
                    )
                    CompactCalendarCard(
                        tasksState = tasksState,
                        onSelectDay = { onTasksEvent(TasksUiEvent.SelectDay(it)) },
                        modifier = Modifier.weight(0.48f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CounterHeroCard(
    clients: List<Client>,
    selectedClient: Client?,
    categories: List<TaskCategory>,
    tasks: List<WorkTask>,
    tasksState: TasksUiState,
    onTasksEvent: (TasksUiEvent) -> Unit,
) {
    val selectedTask = tasksState.selectedTask?.takeIf { candidate -> tasks.any { it.id == candidate.id } } ?: tasks.firstOrNull()

    SectionCard(
        title = "Tiempo y foco",
        subtitle = null,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FilterBar(
                label = "Cliente activo",
                options = clients.map { FilterOption(it.id, it.companyName) },
                selected = tasksState.selectedDashboardClientId,
                onSelected = { onTasksEvent(TasksUiEvent.SelectDashboardClient(it)) },
                allLabel = "Todos",
                wrap = true,
            )

            selectedClient?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ClientServiceTierBadge(it.serviceTier)
                    ClientAccountStatusBadge(it.accountStatus)
                    Text(
                        text = "${it.companyName} · ${it.productName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FilterBar(
                label = "Etiquetas",
                options = categories.map { FilterOption(it.id, it.name) },
                selected = tasksState.selectedCategoryId,
                onSelected = { onTasksEvent(TasksUiEvent.SelectCategory(it)) },
                allLabel = "Todas",
                wrap = true,
            )

            if (tasks.isEmpty()) {
                EmptyState(
                    title = "Sin foco para ${tasksState.selectedDay ?: tasksState.todayIsoDate}",
                    message = "El dashboard muestra solo la tarea iniciada y las tareas programadas para el dia activo.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(tasks, key = { it.id }) { task ->
                        DashboardTaskRow(
                            task = task,
                            category = categories.firstOrNull { it.id == task.categoryId },
                            selected = tasksState.selectedTaskId == task.id,
                            onSelect = { onTasksEvent(TasksUiEvent.SelectTask(task.id)) },
                        )
                    }
                }
            }

            SectionCard(
                modifier = Modifier.animateContentSize(),
                title = selectedTask?.title ?: "Selecciona una tarea",
                subtitle = null,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = formatSupportDeskClockDuration(tasksState.selectedTaskTrackedSeconds),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (selectedTask == null) {
                            if (tasksState.selectedDayIsFuture) {
                                "Dia futuro: puedes revisar y planificar tareas, pero no imputar tiempo."
                            } else {
                                "Tiempo total de la tarea seleccionada"
                            }
                        } else if (tasksState.selectedDayIsFuture) {
                            "Planificada para ${selectedTask.dueDate ?: "sin fecha"}"
                        } else if (tasksState.selectedDayIsPast) {
                            "Los dias anteriores estan bloqueados para registrar horas."
                        } else if (tasksState.activeTaskId == selectedTask.id && tasksState.activeTaskSeconds > 0) {
                            "Sesion actual: ${formatSupportDeskClockDuration(tasksState.activeTaskSeconds)}"
                        } else {
                            "Total acumulado de esta tarea"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PrimaryButton(
                            text = if (tasksState.isTimerRunning) "En marcha" else "Iniciar",
                            onClick = {
                                selectedTask?.id?.let { onTasksEvent(TasksUiEvent.StartTimer(it)) }
                            },
                            enabled = selectedTask != null && !tasksState.isTimerRunning && tasksState.canTrackSelectedDay,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = "Pausar",
                            onClick = { onTasksEvent(TasksUiEvent.PauseTimer) },
                            enabled = tasksState.isTimerRunning,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = "Detener",
                            onClick = { onTasksEvent(TasksUiEvent.StopTimer) },
                            enabled = tasksState.activeTaskId != null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = "Dia imputado: ${tasksState.selectedDay ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!tasksState.canTrackSelectedDay) {
                        Text(
                            text = if (tasksState.selectedDayIsFuture) {
                                "Solo hoy permite registrar horas. El futuro queda para planificacion."
                            } else {
                                "Los dias anteriores no admiten cambios ni imputaciones."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTaskRow(
    task: WorkTask,
    category: TaskCategory?,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val surfaceColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor, MaterialTheme.shapes.medium)
            .clickable(onClick = onSelect)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(parseColor(category?.colorHex ?: "#6B7A5B"), CircleShape),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = task.dueDate?.let { "Planificada: $it" } ?: "${formatSupportDeskClockDuration(task.loggedSeconds)} registradas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Text(
                text = "Activa",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun WheelSummaryRow(
    globalSeconds: Int,
    globalBillableSeconds: Int,
    clientName: String,
    clientSeconds: Int,
    clientBillableSeconds: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WheelCard(
            title = "Global",
            totalSeconds = globalSeconds,
            billableSeconds = globalBillableSeconds,
            modifier = Modifier.weight(1f),
        )
        WheelCard(
            title = clientName,
            totalSeconds = clientSeconds,
            billableSeconds = clientBillableSeconds,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TimeAnalyticsSection(
    logs: List<TaskLog>,
    weekStart: String,
    weekEnd: String,
    monthPrefix: String,
    selectedClientName: String?,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val weekSeconds = logs.filter { it.workDate in weekStart..weekEnd }.sumOf { it.seconds }
    val monthSeconds = logs.filter { it.workDate.startsWith(monthPrefix) }.sumOf { it.seconds }
    val historicSeconds = logs.sumOf { it.seconds }
    val periodLabel = selectedClientName ?: "Todos los clientes"

    SectionCard(
        title = "Horas",
        subtitle = "$periodLabel - semana, mes e historico desde registros reales.",
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 900.dp
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    TimeMetricCard("Semana", weekSeconds, "$weekStart a $weekEnd", Modifier.fillMaxWidth())
                    TimeMetricCard("Mes", monthSeconds, monthPrefix, Modifier.fillMaxWidth())
                    TimeMetricCard("Historico", historicSeconds, "${logs.size} registros de tiempo", Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    TimeMetricCard("Semana", weekSeconds, "$weekStart a $weekEnd", Modifier.weight(1f))
                    TimeMetricCard("Mes", monthSeconds, monthPrefix, Modifier.weight(1f))
                    TimeMetricCard("Historico", historicSeconds, "${logs.size} registros de tiempo", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TimeMetricCard(
    label: String,
    seconds: Int,
    supportingText: String,
    modifier: Modifier,
) {
    MetricCard(
        label = label,
        value = formatSupportDeskPreciseDuration(seconds),
        supportingText = supportingText,
        modifier = modifier,
    )
}

@Composable
private fun StatusBoardSection(
    tasks: List<WorkTask>,
    categories: List<TaskCategory>,
    selectedTaskId: String?,
    onSelectTask: (String) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(
        title = "Tableros por estado",
        subtitle = "Carga real de trabajo agrupada por estado operativo.",
    ) {
        if (tasks.isEmpty()) {
            EmptyState(
                title = "Sin tareas para el tablero",
                message = "No hay tareas que cumplan los filtros actuales.",
            )
            return@SectionCard
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val visibleStatuses = WorkTaskStatus.entries.filter { it != WorkTaskStatus.ARCHIVED }
            if (maxWidth < 1180.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    visibleStatuses.forEach { status ->
                        StatusColumn(
                            status = status,
                            tasks = tasks.filter { it.status == status }.take(5),
                            totalCount = tasks.count { it.status == status },
                            categories = categories,
                            selectedTaskId = selectedTaskId,
                            onSelectTask = onSelectTask,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    visibleStatuses.forEach { status ->
                        StatusColumn(
                            status = status,
                            tasks = tasks.filter { it.status == status }.take(5),
                            totalCount = tasks.count { it.status == status },
                            categories = categories,
                            selectedTaskId = selectedTaskId,
                            onSelectTask = onSelectTask,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusColumn(
    status: WorkTaskStatus,
    tasks: List<WorkTask>,
    totalCount: Int,
    categories: List<TaskCategory>,
    selectedTaskId: String?,
    onSelectTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = "${status.displayName()} ($totalCount)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (tasks.isEmpty()) {
            Text(
                text = "Sin tareas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            tasks.forEach { task ->
                DashboardTaskRow(
                    task = task,
                    category = categories.firstOrNull { it.id == task.categoryId },
                    selected = selectedTaskId == task.id,
                    onSelect = { onSelectTask(task.id) },
                )
            }
        }
    }
}

@Composable
private fun WheelCard(
    title: String,
    totalSeconds: Int,
    billableSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val nonBillableSeconds = max(0, totalSeconds - billableSeconds)
    val safeTotal = max(1, totalSeconds)
    val sweepTarget = (billableSeconds.toFloat() / safeTotal.toFloat()) * 360f
    val billableSweep by animateFloatAsState(targetValue = sweepTarget)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    SectionCard(
        modifier = modifier,
        title = title,
        subtitle = null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(142.dp)) {
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 22f, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = billableSweep,
                        useCenter = false,
                        style = Stroke(width = 22f, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = secondaryColor,
                        startAngle = -90f + billableSweep,
                        sweepAngle = 360f - billableSweep,
                        useCenter = false,
                        style = Stroke(width = 22f, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatSupportDeskClockDuration(totalSeconds),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Mes actual",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LegendChip(
                    label = "Facturable",
                    value = formatSupportDeskPreciseDuration(billableSeconds),
                    color = primaryColor,
                    modifier = Modifier.weight(1f),
                )
                LegendChip(
                    label = "Interno",
                    value = formatSupportDeskPreciseDuration(nonBillableSeconds),
                    color = secondaryColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CompactCalendarCard(
    tasksState: TasksUiState,
    onSelectDay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialMonth = remember(tasksState.selectedDay) {
        tasksState.selectedDay?.let(::parseCalendarMonth) ?: parseCalendarMonth(tasksState.todayIsoDate)
    }
    var visibleYear by rememberSaveable(initialMonth.year) { mutableIntStateOf(initialMonth.year) }
    var visibleMonth by rememberSaveable(initialMonth.month) { mutableIntStateOf(initialMonth.month) }
    val month = remember(visibleYear, visibleMonth) { CalendarMonth(visibleYear, visibleMonth) }
    val maxMinutes = max(1, tasksState.logs.maxOfOrNull { it.minutes } ?: 0)
    val cells = remember(tasksState.logs, tasksState.tasks, tasksState.selectedDay, tasksState.todayIsoDate, month) {
        buildCalendarCells(month, tasksState.logs, tasksState.tasks, tasksState.selectedDay, tasksState.todayIsoDate)
    }

    SectionCard(
        modifier = modifier,
        title = "Calendario",
        subtitle = null,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "<",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable {
                        val previous = month.previous()
                        visibleYear = previous.year
                        visibleMonth = previous.month
                    },
                )
                Text(
                    text = ">",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable {
                        val next = month.next()
                        visibleYear = next.year
                        visibleMonth = next.month
                    },
                )
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = month.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SPANISH_WEEKDAY_LABELS.forEach { weekday ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
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
                        CompactCalendarDayCell(
                            day = day,
                            maxMinutes = maxMinutes,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSelectDay(day.isoDate)
                                if (!day.inCurrentMonth) {
                                    visibleYear = day.year
                                    visibleMonth = day.month
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCalendarDayCell(
    day: CalendarDay,
    maxMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (day.minutes == 0) 0.06f else 0.14f + (day.minutes.toFloat() / maxMinutes.toFloat()) * 0.28f
    val borderColor = when {
        day.selected -> MaterialTheme.colorScheme.primary
        day.isPast -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        day.inCurrentMonth -> Color.Transparent
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }
    val backgroundColor = when {
        day.selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        day.isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        day.inCurrentMonth -> MaterialTheme.colorScheme.primary.copy(alpha = alpha)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    }
    val contentColor = if (day.isPast) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
    } else if (day.inCurrentMonth) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1.1f)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .clickable(enabled = !day.isPast, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
    ) {
        Text(
            text = day.dayNumber,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = if (day.selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (day.minutes > 0 && day.inCurrentMonth) {
            Text(
                text = "${day.minutes}m",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
        if (day.scheduledTasks > 0) {
            Text(
                text = "${day.scheduledTasks}t",
                style = MaterialTheme.typography.labelSmall,
                color = if (day.isPast) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
private fun LegendChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape),
        )
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private data class CalendarDay(
    val isoDate: String,
    val year: Int,
    val month: Int,
    val dayNumber: String,
    val minutes: Int,
    val scheduledTasks: Int,
    val inCurrentMonth: Boolean,
    val selected: Boolean,
    val isPast: Boolean,
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

private fun buildCalendarCells(
    month: CalendarMonth,
    logs: List<TaskLog>,
    tasks: List<WorkTask>,
    selectedDay: String?,
    todayIsoDate: String,
): List<CalendarDay> {
    val firstDayOffset = dayOfWeekMondayIndex(month.year, month.month, 1)
    val currentMonthDays = daysInMonth(month.year, month.month)
    val previousMonth = month.previous()
    val previousMonthDays = daysInMonth(previousMonth.year, previousMonth.month)
    val totalCells = if (firstDayOffset + currentMonthDays <= 35) 35 else 42
    val trailingDays = totalCells - firstDayOffset - currentMonthDays
    val nextMonth = month.next()

    val leading = (previousMonthDays - firstDayOffset + 1..previousMonthDays).map { day ->
        buildCalendarDay(previousMonth.year, previousMonth.month, day, logs, tasks, selectedDay, todayIsoDate, false)
    }
    val current = (1..currentMonthDays).map { day ->
        buildCalendarDay(month.year, month.month, day, logs, tasks, selectedDay, todayIsoDate, true)
    }
    val trailing = (1..trailingDays).map { day ->
        buildCalendarDay(nextMonth.year, nextMonth.month, day, logs, tasks, selectedDay, todayIsoDate, false)
    }
    return leading + current + trailing
}

private fun buildCalendarDay(
    year: Int,
    month: Int,
    day: Int,
    logs: List<TaskLog>,
    tasks: List<WorkTask>,
    selectedDay: String?,
    todayIsoDate: String,
    inCurrentMonth: Boolean,
): CalendarDay {
    val isoDate = isoDate(year, month, day)
    val minutes = logs.filter { it.workDate == isoDate }.sumOf { it.minutes }
    val scheduledTasks = tasks.count { it.dueDate == isoDate }
    return CalendarDay(
        isoDate = isoDate,
        year = year,
        month = month,
        dayNumber = day.toString(),
        minutes = minutes,
        scheduledTasks = scheduledTasks,
        inCurrentMonth = inCurrentMonth,
        selected = selectedDay == isoDate,
        isPast = isoDate < todayIsoDate,
    )
}

private fun parseCalendarMonth(isoDate: String): CalendarMonth {
    val parts = isoDate.split("-")
    return CalendarMonth(
        year = parts.getOrNull(0)?.toIntOrNull() ?: 2026,
        month = parts.getOrNull(1)?.toIntOrNull() ?: 4,
    )
}

private fun isoDate(year: Int, month: Int, day: Int): String =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

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

private fun weekStartIsoDate(isoDate: String): String {
    val parts = isoDate.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return isoDate
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return isoDate
    val day = parts.getOrNull(2)?.toIntOrNull() ?: return isoDate
    return shiftIsoDate(isoDate, -dayOfWeekMondayIndex(year, month, day))
}

private fun shiftIsoDate(isoDate: String, days: Int): String {
    val parts = isoDate.split("-")
    var year = parts.getOrNull(0)?.toIntOrNull() ?: return isoDate
    var month = parts.getOrNull(1)?.toIntOrNull() ?: return isoDate
    var day = parts.getOrNull(2)?.toIntOrNull() ?: return isoDate

    repeat(kotlin.math.abs(days)) {
        if (days > 0) {
            day += 1
            if (day > daysInMonth(year, month)) {
                day = 1
                month += 1
                if (month > 12) {
                    month = 1
                    year += 1
                }
            }
        } else {
            day -= 1
            if (day < 1) {
                month -= 1
                if (month < 1) {
                    month = 12
                    year -= 1
                }
                day = daysInMonth(year, month)
            }
        }
    }
    return isoDate(year, month, day)
}

private fun WorkTaskStatus.displayName(): String = when (this) {
    WorkTaskStatus.TODO -> "Pendiente"
    WorkTaskStatus.IN_PROGRESS -> "En curso"
    WorkTaskStatus.WAITING_CLIENT -> "Cliente"
    WorkTaskStatus.REVIEW -> "Revision"
    WorkTaskStatus.DONE -> "Hecha"
    WorkTaskStatus.ARCHIVED -> "Archivada"
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

private fun parseColor(hex: String): Color = runCatching {
    val value = hex.removePrefix("#").toLong(16)
    val red = ((value shr 16) and 0xFF).toInt() / 255f
    val green = ((value shr 8) and 0xFF).toInt() / 255f
    val blue = (value and 0xFF).toInt() / 255f
    Color(red = red, green = green, blue = blue, alpha = 1f)
}.getOrElse {
    Color(0xFF6B7A5B)
}
