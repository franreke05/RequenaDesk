package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.designsystem.components.badges.ClientAccountStatusBadge
import com.requena.supportdesk.designsystem.components.badges.ClientServiceTierBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskClockDuration
import com.requena.supportdesk.designsystem.theme.formatSupportDeskPreciseDuration
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.X
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
    val calendarState = rememberDashboardCalendarState(
        initialIsoDate = tasksState.selectedDay ?: tasksState.todayIsoDate,
    )
    val monthLogs = remember(tasksState.logs, calendarState.monthKey) {
        tasksState.logs.filter { it.workDate.take(7) == calendarState.monthKey }
    }
    val globalSeconds = monthLogs.sumOf { it.seconds }
    val globalBillableSeconds = monthLogs.filter { it.billable }.sumOf { it.seconds }
    val clientLogs = remember(monthLogs, tasksState.selectedDashboardClientId) {
        monthLogs.filter { log ->
            tasksState.selectedDashboardClientId != null && log.clientId == tasksState.selectedDashboardClientId
        }
    }
    val clientSeconds = clientLogs.sumOf { it.seconds }
    val clientBillableSeconds = clientLogs.filter { it.billable }.sumOf { it.seconds }
    val selectedPlanningDay = tasksState.selectedDay.takeIf { tasksState.selectedDayIsFuture }
    val dashboardTasks = remember(tasksState.dashboardFilteredTasks, selectedPlanningDay) {
        tasksState.dashboardFilteredTasks.filter { task ->
            selectedPlanningDay == null || task.dueDate == selectedPlanningDay
        }
    }
    val selectedTask = tasksState.selectedTask?.takeIf { candidate -> dashboardTasks.any { it.id == candidate.id } }
        ?: dashboardTasks.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        DashboardFocusPanel(
            clients = clients,
            selectedClient = selectedClient,
            categories = tasksState.categories,
            tasks = dashboardTasks,
            selectedTask = selectedTask,
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
                supportingText = selectedClient?.companyName ?: "Sin cliente fijo en la tarea",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Horas de ${calendarState.shortLabel}",
                value = formatSupportDeskPreciseDuration(globalSeconds),
                supportingText = when {
                    selectedPlanningDay != null -> "${dashboardTasks.count()} tareas programadas para $selectedPlanningDay."
                    globalSeconds == 0 -> "Sin registros en ${calendarState.label}."
                    else -> "${dashboardTasks.count()} tareas visibles tras los filtros."
                },
                modifier = Modifier.weight(1f),
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stacked = maxWidth < SupportDeskBreakpoints.adminMedium
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    WheelSummaryRow(
                        monthLabel = calendarState.label,
                        globalSeconds = globalSeconds,
                        globalBillableSeconds = globalBillableSeconds,
                        hasClientSelected = selectedClient != null,
                        clientName = selectedClient?.companyName ?: "Cliente",
                        clientSeconds = clientSeconds,
                        clientBillableSeconds = clientBillableSeconds,
                    )
                    DashboardCalendarCard(
                        calendarState = calendarState,
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
                        monthLabel = calendarState.label,
                        globalSeconds = globalSeconds,
                        globalBillableSeconds = globalBillableSeconds,
                        hasClientSelected = selectedClient != null,
                        clientName = selectedClient?.companyName ?: "Cliente",
                        clientSeconds = clientSeconds,
                        clientBillableSeconds = clientBillableSeconds,
                        modifier = Modifier.weight(0.52f),
                    )
                    DashboardCalendarCard(
                        calendarState = calendarState,
                        tasksState = tasksState,
                        onSelectDay = { onTasksEvent(TasksUiEvent.SelectDay(it)) },
                        modifier = Modifier.weight(0.48f),
                    )
                }
            }
        }
    }
}

// region Foco: filtros, selección de tarea y contador

@Composable
private fun DashboardFocusPanel(
    clients: List<Client>,
    selectedClient: Client?,
    categories: List<TaskCategory>,
    tasks: List<WorkTask>,
    selectedTask: WorkTask?,
    tasksState: TasksUiState,
    onTasksEvent: (TasksUiEvent) -> Unit,
) {
    SectionCard(
        title = "Tiempo y foco",
        subtitle = null,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            DashboardFiltersRow(
                clients = clients,
                selectedClient = selectedClient,
                categories = categories,
                selectedCategoryIds = tasksState.selectedDashboardCategoryIds,
                onTasksEvent = onTasksEvent,
            )

            DashboardTaskPicker(
                tasks = tasks,
                categories = categories,
                selectedTaskId = tasksState.selectedTaskId,
                selectedDayIsFuture = tasksState.selectedDayIsFuture,
                selectedDay = tasksState.selectedDay,
                onSelect = { taskId -> onTasksEvent(TasksUiEvent.SelectTask(taskId)) },
            )

            DashboardSelectedTaskPanel(
                selectedTask = selectedTask,
                tasksState = tasksState,
                onTasksEvent = onTasksEvent,
            )
        }
    }
}

@Composable
private fun DashboardFiltersRow(
    clients: List<Client>,
    selectedClient: Client?,
    categories: List<TaskCategory>,
    selectedCategoryIds: Set<String>,
    onTasksEvent: (TasksUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterBar(
            label = "Cliente activo",
            options = clients.map { FilterOption(it.id, it.companyName) },
            selected = selectedClient?.id,
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

        DashboardTagFilterRow(
            categories = categories,
            selectedCategoryIds = selectedCategoryIds,
            onToggle = { onTasksEvent(TasksUiEvent.ToggleDashboardCategoryFilter(it)) },
        )
    }
}

@Composable
private fun DashboardTagFilterRow(
    categories: List<TaskCategory>,
    selectedCategoryIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val selectedCategories = remember(categories, selectedCategoryIds) {
        categories.filter { it.id in selectedCategoryIds }
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        selectedCategories.forEach { category ->
            DashboardActiveTagChip(
                category = category,
                onRemove = { onToggle(category.id) },
            )
        }

        Box {
            OutlinedButton(
                onClick = { menuExpanded = true },
                shape = MaterialTheme.shapes.medium,
                enabled = categories.isNotEmpty(),
            ) {
                Icon(imageVector = Lucide.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    text = if (selectedCategories.isEmpty()) "Filtrar etiquetas" else "Editar etiquetas",
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.widthIn(min = 220.dp, max = 320.dp),
            ) {
                if (categories.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No hay etiquetas creadas") },
                        enabled = false,
                        onClick = {},
                    )
                } else {
                    categories.forEach { category ->
                        val checked = category.id in selectedCategoryIds
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            leadingIcon = {
                                Checkbox(checked = checked, onCheckedChange = { onToggle(category.id) })
                            },
                            onClick = { onToggle(category.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardActiveTagChip(
    category: TaskCategory,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(parseColor(category.colorHex).copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(8.dp).background(parseColor(category.colorHex), CircleShape))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Lucide.X,
            contentDescription = "Quitar etiqueta ${category.name}",
            modifier = Modifier.size(14.dp).clickable(onClick = onRemove),
        )
    }
}

@Composable
private fun DashboardTaskPicker(
    tasks: List<WorkTask>,
    categories: List<TaskCategory>,
    selectedTaskId: String?,
    selectedDayIsFuture: Boolean,
    selectedDay: String?,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Selecciona una tarea",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (tasks.isEmpty()) {
            EmptyState(
                title = if (selectedDayIsFuture) {
                    "Sin tareas programadas para $selectedDay"
                } else {
                    "Sin tareas para este contexto"
                },
                message = if (selectedDayIsFuture) {
                    "Programa nuevas tareas desde la pantalla de Tareas usando el dia seleccionado."
                } else {
                    "Prueba a quitar filtros de cliente o etiquetas."
                },
            )
        } else {
            val categoriesById = remember(categories) { categories.associateBy { it.id } }
            val taskItems = remember(tasks, categoriesById, selectedTaskId) {
                tasks.map { task ->
                    DashboardTaskItemUi(
                        task = task,
                        category = categoriesById[task.categoryId],
                        selected = selectedTaskId == task.id,
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(taskItems, key = { it.task.id }) { item ->
                    DashboardTaskRow(item = item, onSelect = { onSelect(item.task.id) })
                }
            }
        }
    }
}

private data class DashboardTaskItemUi(
    val task: WorkTask,
    val category: TaskCategory?,
    val selected: Boolean,
)

@Composable
private fun DashboardTaskRow(
    item: DashboardTaskItemUi,
    onSelect: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val surfaceColor by animateColorAsState(
        targetValue = when {
            item.selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            hovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
        },
        animationSpec = tween(SupportDeskMotion.quick),
        label = "dashboardTaskRowBackground",
    )
    val accentColor = parseColor(item.category?.colorHex ?: "#6B7A5B")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor, MaterialTheme.shapes.medium)
            .border(
                width = if (item.selected) 1.5.dp else 0.dp,
                color = if (item.selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = MaterialTheme.shapes.medium,
            )
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(12.dp).background(accentColor, CircleShape))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = item.task.dueDate?.let { "Planificada: $it" }
                    ?: "${formatSupportDeskClockDuration(item.task.loggedSeconds)} registradas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.selected) {
            Text(
                text = "Activa",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DashboardSelectedTaskPanel(
    selectedTask: WorkTask?,
    tasksState: TasksUiState,
    onTasksEvent: (TasksUiEvent) -> Unit,
) {
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

// endregion

// region Resumen: ruedas del mes

@Composable
private fun WheelSummaryRow(
    monthLabel: String,
    globalSeconds: Int,
    globalBillableSeconds: Int,
    hasClientSelected: Boolean,
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
            monthLabel = monthLabel,
            totalSeconds = globalSeconds,
            billableSeconds = globalBillableSeconds,
            modifier = Modifier.weight(1f),
        )
        if (hasClientSelected) {
            WheelCard(
                title = clientName,
                monthLabel = monthLabel,
                totalSeconds = clientSeconds,
                billableSeconds = clientBillableSeconds,
                modifier = Modifier.weight(1f),
            )
        } else {
            SectionCard(
                modifier = Modifier.weight(1f),
                title = "Cliente",
                subtitle = null,
            ) {
                EmptyState(
                    title = "Sin cliente seleccionado",
                    message = "Elige un cliente activo arriba para ver su tiempo de $monthLabel.",
                )
            }
        }
    }
}

@Composable
private fun WheelCard(
    title: String,
    monthLabel: String,
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
                        text = monthLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (totalSeconds == 0) {
                Text(
                    text = "Sin registros en $monthLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

// endregion

// region Calendario

@Stable
private class DashboardCalendarState(initialYear: Int, initialMonth: Int) {
    var year by mutableIntStateOf(initialYear)
        private set
    var month by mutableIntStateOf(initialMonth)
        private set

    val label: String
        get() = "${SPANISH_MONTH_NAMES[month - 1]} de $year"

    val shortLabel: String
        get() = "${SPANISH_MONTH_NAMES[month - 1]} $year"

    val monthKey: String
        get() = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}"

    fun previous() {
        if (month == 1) {
            month = 12
            year -= 1
        } else {
            month -= 1
        }
    }

    fun next() {
        if (month == 12) {
            month = 1
            year += 1
        } else {
            month += 1
        }
    }

    fun jumpTo(targetYear: Int, targetMonth: Int) {
        year = targetYear
        month = targetMonth
    }
}

private val DashboardCalendarStateSaver: Saver<DashboardCalendarState, List<Int>> = Saver(
    save = { listOf(it.year, it.month) },
    restore = { DashboardCalendarState(it[0], it[1]) },
)

@Composable
private fun rememberDashboardCalendarState(initialIsoDate: String): DashboardCalendarState {
    val initial = remember(initialIsoDate) { parseCalendarMonth(initialIsoDate) }
    return rememberSaveable(saver = DashboardCalendarStateSaver) {
        DashboardCalendarState(initial.year, initial.month)
    }
}

@Composable
private fun DashboardCalendarCard(
    calendarState: DashboardCalendarState,
    tasksState: TasksUiState,
    onSelectDay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxMinutes = max(1, tasksState.logs.maxOfOrNull { it.minutes } ?: 0)
    val cells = remember(
        tasksState.logs,
        tasksState.tasks,
        tasksState.selectedDay,
        tasksState.todayIsoDate,
        calendarState.year,
        calendarState.month,
    ) {
        buildCalendarCells(
            CalendarMonth(calendarState.year, calendarState.month),
            tasksState.logs,
            tasksState.tasks,
            tasksState.selectedDay,
            tasksState.todayIsoDate,
        )
    }

    SectionCard(
        modifier = modifier,
        title = "Calendario",
        subtitle = null,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = Lucide.ChevronLeft,
                    contentDescription = "Mes anterior",
                    modifier = Modifier
                        .clickable(onClick = calendarState::previous)
                        .padding(6.dp)
                        .size(18.dp),
                )
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = "Mes siguiente",
                    modifier = Modifier
                        .clickable(onClick = calendarState::next)
                        .padding(6.dp)
                        .size(18.dp),
                )
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = calendarState.label,
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
                                    calendarState.jumpTo(day.year, day.month)
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
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val alpha = if (day.minutes == 0) 0.06f else 0.14f + (day.minutes.toFloat() / maxMinutes.toFloat()) * 0.28f
    val borderColor = when {
        day.selected -> MaterialTheme.colorScheme.primary
        day.isPast -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        day.inCurrentMonth -> Color.Transparent
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }
    val backgroundColor = when {
        day.selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        !day.isPast && hovered -> MaterialTheme.colorScheme.primary.copy(alpha = alpha + 0.12f)
        day.isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        day.inCurrentMonth -> MaterialTheme.colorScheme.primary.copy(alpha = alpha)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    }
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(SupportDeskMotion.quick),
        label = "calendarDayBackground",
    )
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
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(animatedBackgroundColor, RoundedCornerShape(8.dp))
            .hoverable(interactionSource, enabled = !day.isPast)
            .clickable(interactionSource = interactionSource, indication = null, enabled = !day.isPast, onClick = onClick)
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

// endregion
