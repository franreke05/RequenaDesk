package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.motion.SupportDeskEntrance
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskClockDuration
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.designsystem.theme.formatSupportDeskPreciseDuration
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import kotlin.math.abs
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
    val weekSeconds = tasksState.logs.filter { it.workDate in currentWeekStart..currentWeekEnd }.sumOf { it.seconds }
    val clientLogs = tasksState.logs.filter { log ->
        tasksState.selectedDashboardClientId == null || log.clientId == tasksState.selectedDashboardClientId
    }
    val clientMonthLogs = clientLogs.filter { it.workDate.startsWith(currentMonthPrefix) }
    val clientSeconds = clientMonthLogs.sumOf { it.seconds }
    val clientBillableSeconds = clientMonthLogs.filter { it.billable }.sumOf { it.seconds }
    val boardTasks = tasksState.tasks.filter { task ->
        tasksState.selectedDashboardClientId == null || task.clientId == tasksState.selectedDashboardClientId
    }
    val dashboardTasks = tasksState.tasks.filter { task ->
        (tasksState.selectedDashboardClientId == null || task.clientId == tasksState.selectedDashboardClientId) &&
            (task.id == tasksState.activeTaskId || task.id == tasksState.selectedTaskId || task.dueDate == selectedDay)
    }.sortedWith(
        compareByDescending<WorkTask> { it.id == tasksState.activeTaskId }
            .thenByDescending { it.id == tasksState.selectedTaskId }
            .thenBy { it.completed }
            .thenByDescending { it.updatedAt },
    )
    val inProgressCount = tasksState.tasks.count { it.status == WorkTaskStatus.IN_PROGRESS }
    val doneCount = tasksState.tasks.count { it.status == WorkTaskStatus.DONE }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        if (tasksState.isLoading && tasksState.tasks.isEmpty()) {
            LoadingState(itemCount = 3)
        }

        tasksState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            SupportDeskEntrance(index = 0) {
                SectionCard(title = "Datos no disponibles", subtitle = message) {
                    SecondaryButton(text = "Reintentar", onClick = { onTasksEvent(TasksUiEvent.Load) })
                }
            }
        }

        SupportDeskEntrance(index = 1) {
            CounterHeroCard(
                clients = clients,
                selectedClient = selectedClient,
                tasks = dashboardTasks,
                tasksState = tasksState,
                inProgressCount = inProgressCount,
                doneCount = doneCount,
                weekSeconds = weekSeconds,
                onTasksEvent = onTasksEvent,
            )
        }

        SupportDeskEntrance(index = 2) {
            StatusBoardSection(
                tasks = boardTasks,
                categories = tasksState.categories,
                selectedTaskId = tasksState.selectedTaskId,
                onSelectTask = { onTasksEvent(TasksUiEvent.SelectTask(it)) },
                onChangeTaskStatus = { taskId, status -> onTasksEvent(TasksUiEvent.ChangeTaskStatus(taskId, status)) },
            )
        }

        SupportDeskEntrance(index = 3) {
            TimeAnalyticsSection(
                logs = clientLogs,
                weekStart = currentWeekStart,
                weekEnd = currentWeekEnd,
                monthPrefix = currentMonthPrefix,
                selectedClientName = selectedClient?.companyName,
            )
        }

        SupportDeskEntrance(index = 4) {
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
}

// ─── Hero card ────────────────────────────────────────────────────────────────

@Composable
private fun CounterHeroCard(
    clients: List<Client>,
    selectedClient: Client?,
    tasks: List<WorkTask>,
    tasksState: TasksUiState,
    inProgressCount: Int,
    doneCount: Int,
    weekSeconds: Int,
    onTasksEvent: (TasksUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val timerTask = if (tasksState.isTimerRunning) {
        tasksState.activeTask ?: tasksState.selectedTask ?: tasks.firstOrNull()
    } else {
        tasksState.selectedTask ?: tasksState.activeTask ?: tasks.firstOrNull()
    }
    val timerSeconds = timerTask?.let(tasksState::trackedSecondsFor) ?: tasksState.activeTaskSeconds
    val selectedPlanningDay = tasksState.effectiveSelectedDay
    val trackingDate = tasksState.timeTrackingDate

    SectionCard(
        title = "Tiempo y foco",
        neonAccentColor = if (tasksState.isTimerRunning) MaterialTheme.colorScheme.primary else null,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {

            TimerStatusStrip(
                inProgressCount = inProgressCount,
                doneCount = doneCount,
                weekSeconds = weekSeconds,
            )

            // Client filter
            FilterBar(
                label = "Cliente",
                options = clients.map { FilterOption(it.id, it.companyName) },
                selected = tasksState.selectedDashboardClientId,
                onSelected = { onTasksEvent(TasksUiEvent.SelectDashboardClient(it)) },
                allLabel = "Todos",
                wrap = true,
            )

            selectedClient?.let {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ClientServiceTierBadge(it.serviceTier)
                    ClientAccountStatusBadge(it.accountStatus)
                    Text(
                        text = "${it.companyName} · ${it.productName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Tasks for today
            if (tasks.isEmpty()) {
                EmptyState(
                    title = "Sin tareas destacadas",
                    message = "Selecciona una tarea del tablero para contabilizarla, aunque no este programada para $selectedPlanningDay.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(tasks, key = { it.id }) { task ->
                        DashboardTaskRow(
                            task = task,
                            category = tasksState.categories.firstOrNull { it.id == task.categoryId },
                            selected = tasksState.selectedTaskId == task.id,
                            onSelect = { onTasksEvent(TasksUiEvent.SelectTask(task.id)) },
                        )
                    }
                }
            }

            // Timer block
            val timerBg by animateColorAsState(
                targetValue = if (tasksState.isTimerRunning)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                animationSpec = tween(400),
            )
            val timerBorderColor by animateColorAsState(
                targetValue = if (tasksState.isTimerRunning)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                else Color.Transparent,
                animationSpec = tween(400),
            )
            Surface(
                modifier = Modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
                shape = RoundedCornerShape(12.dp),
                color = timerBg,
                border = BorderStroke(1.dp, timerBorderColor),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = timerTask?.title ?: "Sin tarea seleccionada",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (tasksState.isTimerRunning) {
                                Text(
                                    text = "⏺ Grabando · imputacion $trackingDate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Text(
                                    text = if (selectedPlanningDay == trackingDate)
                                        "Imputacion abierta para hoy"
                                    else
                                        "Planificacion: $selectedPlanningDay · imputacion: $trackingDate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        val infiniteTransition = rememberInfiniteTransition("timerPulse")
                        val clockPulse by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.06f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(850, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse,
                            ),
                            label = "clockPulse",
                        )
                        val timerColor by animateColorAsState(
                            targetValue = if (tasksState.isTimerRunning)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            animationSpec = tween(400),
                        )
                        Text(
                            text = formatSupportDeskClockDuration(timerSeconds),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = timerColor,
                            modifier = if (tasksState.isTimerRunning) Modifier.scale(clockPulse) else Modifier,
                        )
                    }

                    if (selectedPlanningDay != trackingDate) {
                        Text(
                            text = "El calendario puede quedarse en otra fecha para planificar; el contador siempre guarda el tiempo real en hoy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(
                            text = if (tasksState.isTimerRunning) "En marcha" else "Iniciar",
                            onClick = { timerTask?.id?.let { onTasksEvent(TasksUiEvent.StartTimer(it)) } },
                            enabled = timerTask != null && !tasksState.isTimerRunning,
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
                }
            }
        }
    }
}

@Composable
private fun TimerStatusStrip(
    inProgressCount: Int,
    doneCount: Int,
    weekSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val semantic = SupportDeskThemeTokens.semanticColors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.xs),
    ) {
        StatusPill("En curso", inProgressCount.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        StatusPill("Hechas", doneCount.toString(), semantic.success, Modifier.weight(1f))
        StatusPill("Semana", formatSupportDeskDuration(weekSeconds / 60), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
    }
}

@Composable
private fun StatusPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CalendarChevronBtn(left: Boolean, onClick: () -> Unit) {
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        modifier = Modifier.size(30.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val arm = size.height * 0.50f
            val sw = 2.dp.toPx()
            if (left) {
                drawLine(iconColor, Offset(cx + arm * 0.5f, cy - arm), Offset(cx - arm * 0.5f, cy), sw, StrokeCap.Round)
                drawLine(iconColor, Offset(cx - arm * 0.5f, cy), Offset(cx + arm * 0.5f, cy + arm), sw, StrokeCap.Round)
            } else {
                drawLine(iconColor, Offset(cx - arm * 0.5f, cy - arm), Offset(cx + arm * 0.5f, cy), sw, StrokeCap.Round)
                drawLine(iconColor, Offset(cx + arm * 0.5f, cy), Offset(cx - arm * 0.5f, cy + arm), sw, StrokeCap.Round)
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
    val accentColor = parseColor(category?.colorHex ?: "#6B7A5B")
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        animationSpec = tween(200),
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onSelect),
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(50.dp)
                    .background(Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.3f)))),
            )
            Row(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (category != null) {
                            Box(modifier = Modifier.size(6.dp).background(accentColor, CircleShape))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        Text(
                            text = task.dueDate?.let { "📅 $it" } ?: "⏱ ${formatSupportDeskClockDuration(task.loggedSeconds)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "Activa",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

// ─── Task board with drag and drop ────────────────────────────────────────────

private data class DragState(
    val taskId: String,
    val fromStatus: WorkTaskStatus,
    val currentColumnIdx: Int,
    val thresholdProgress: Float = 0f,
) {
    val targetColumnIdx: Int get() = currentColumnIdx
}

@Composable
private fun StatusBoardSection(
    tasks: List<WorkTask>,
    categories: List<TaskCategory>,
    selectedTaskId: String?,
    onSelectTask: (String) -> Unit,
    onChangeTaskStatus: (String, WorkTaskStatus) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val thresholdPx = with(LocalDensity.current) { 60.dp.toPx() }
    val visibleStatuses = WorkTaskStatus.entries.filter { it != WorkTaskStatus.ARCHIVED }

    var drag by remember { mutableStateOf<DragState?>(null) }
    val draggingTargetStatus = drag?.let { visibleStatuses.getOrNull(it.targetColumnIdx) }
    val isDraggingAny = drag != null

    SectionCard(
        title = "Tablero de tareas",
        subtitle = if (isDraggingAny)
            "→ Soltá sobre la columna destino"
        else
            "Haz clic para seleccionar · Arrastra para mover",
    ) {
        if (tasks.isEmpty()) {
            EmptyState(title = "Sin tareas", message = "No hay tareas para los filtros actuales.")
            return@SectionCard
        }

        @Composable
        fun buildColumn(status: WorkTaskStatus, modifier: Modifier) {
            val fromIdx = drag?.let { visibleStatuses.indexOf(it.fromStatus) } ?: -1
            val targetIdx = drag?.targetColumnIdx ?: -1
            val colIdx = visibleStatuses.indexOf(status)
            val isSource = drag?.fromStatus == status
            val isDropTarget = isDraggingAny && targetIdx == colIdx && !isSource

            StatusColumn(
                status = status,
                tasks = tasks.filter { it.status == status }.take(6),
                totalCount = tasks.count { it.status == status },
                categories = categories,
                selectedTaskId = selectedTaskId,
                draggingTaskId = drag?.taskId,
                isDropTarget = isDropTarget,
                isSource = isSource,
                onSelectTask = onSelectTask,
                onDragStart = { taskId ->
                    onSelectTask(taskId)
                    drag = DragState(
                        taskId = taskId,
                        fromStatus = status,
                        currentColumnIdx = colIdx,
                    )
                },
                onDragDelta = { deltaX ->
                    drag = drag?.let { current ->
                        val newProgress = current.thresholdProgress + deltaX
                        when {
                            newProgress > thresholdPx -> {
                                val nextIdx = (current.currentColumnIdx + 1).coerceAtMost(visibleStatuses.size - 1)
                                current.copy(currentColumnIdx = nextIdx, thresholdProgress = 0f)
                            }
                            newProgress < -thresholdPx -> {
                                val prevIdx = (current.currentColumnIdx - 1).coerceAtLeast(0)
                                current.copy(currentColumnIdx = prevIdx, thresholdProgress = 0f)
                            }
                            else -> current.copy(thresholdProgress = newProgress)
                        }
                    }
                },
                onDragEnd = {
                    drag?.let { current ->
                        val target = visibleStatuses.getOrNull(current.targetColumnIdx)
                        if (target != null && target != current.fromStatus) {
                            onChangeTaskStatus(current.taskId, target)
                        }
                    }
                    drag = null
                },
                modifier = modifier,
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 1100.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    visibleStatuses.forEach { status -> buildColumn(status, Modifier.fillMaxWidth()) }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    visibleStatuses.forEach { status -> buildColumn(status, Modifier.weight(1f)) }
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
    draggingTaskId: String?,
    isDropTarget: Boolean,
    isSource: Boolean,
    onSelectTask: (String) -> Unit,
    onDragStart: (String) -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val headerColor = status.headerColor(
        primary = MaterialTheme.colorScheme.primary,
        secondary = MaterialTheme.colorScheme.secondary,
        semantic = semantic,
    )

    val bodyBg by animateColorAsState(
        targetValue = when {
            isDropTarget -> headerColor.copy(alpha = 0.12f)
            isSource -> headerColor.copy(alpha = 0.04f)
            else -> headerColor.copy(alpha = 0.02f)
        },
        animationSpec = tween(180),
    )
    val borderColor by animateColorAsState(
        targetValue = if (isDropTarget) headerColor.copy(alpha = 0.7f) else Color.Transparent,
        animationSpec = tween(180),
    )
    val colScale by animateFloatAsState(
        targetValue = if (isDropTarget) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
    )
    val colAlpha by animateFloatAsState(
        targetValue = if (isSource && draggingTaskId != null) 0.7f else 1f,
        animationSpec = tween(200),
    )

    Surface(
        modifier = modifier
            .scale(colScale)
            .alpha(colAlpha)
            .shadow(if (isDropTarget) 6.dp else 1.dp, RoundedCornerShape(spacing.md))
            .border(2.dp, borderColor, RoundedCornerShape(spacing.md)),
        shape = RoundedCornerShape(spacing.md),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(headerColor, headerColor.copy(alpha = 0.6f))))
                    .padding(horizontal = spacing.md, vertical = spacing.xs),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    if (isDropTarget) {
                        Text("⬇", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                    Text(
                        text = if (isDropTarget) "Soltar aquí" else status.displayName(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    if (totalCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(100.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text("$totalCount", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Drop zone indicator (shown at top of column when is drop target)
            if (isDropTarget) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.sm, vertical = spacing.xs)
                        .background(headerColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, headerColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "⬇  ${status.displayName()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = headerColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .background(bodyBg)
                    .padding(spacing.sm)
                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                if (tasks.isEmpty() && !isDropTarget) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = spacing.sm), contentAlignment = Alignment.Center) {
                        Text("Vacía", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
                tasks.forEach { task ->
                    DraggableMiniTaskCard(
                        task = task,
                        category = categories.firstOrNull { it.id == task.categoryId },
                        selected = selectedTaskId == task.id,
                        isDragging = draggingTaskId == task.id,
                        onSelect = { onSelectTask(task.id) },
                        onDragStart = { onDragStart(task.id) },
                        onDragDelta = onDragDelta,
                        onDragEnd = onDragEnd,
                    )
                }
                if (totalCount > 6) {
                    Text("+${totalCount - 6} más", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun DraggableMiniTaskCard(
    task: WorkTask,
    category: TaskCategory?,
    selected: Boolean,
    isDragging: Boolean,
    onSelect: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val accentColor = parseColor(category?.colorHex ?: "#6B7A5B")

    val bgColor by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.primary
            selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(180),
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.45f else 1f,
        animationSpec = tween(150),
    )
    val cardScale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
    )
    val shadowDp = if (isDragging) 8.dp else 1.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .alpha(cardAlpha)
            .shadow(shadowDp, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(task.id) {
                detectDragGestures(
                    onDragStart = { _ -> onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.x)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                )
            }
            .clickable { onSelect() },
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(3.dp).height(36.dp).background(accentColor))
            Row(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // Drag handle / status indicator
                Text(
                    text = if (isDragging) "↔" else "⠿",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDragging) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

// ─── Analytics ────────────────────────────────────────────────────────────────

@Composable
private fun TimeAnalyticsSection(
    logs: List<TaskLog>,
    weekStart: String,
    weekEnd: String,
    monthPrefix: String,
    selectedClientName: String?,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val weekSeconds = logs.filter { it.workDate in weekStart..weekEnd }.sumOf { it.seconds }
    val monthSeconds = logs.filter { it.workDate.startsWith(monthPrefix) }.sumOf { it.seconds }
    val historicSeconds = logs.sumOf { it.seconds }
    val periodLabel = selectedClientName ?: "Todos los clientes"

    SectionCard(
        title = "Horas",
        subtitle = "$periodLabel · semana, mes e histórico",
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 900.dp
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    TimeMetricBar("Semana", weekSeconds, "$weekStart → $weekEnd", MaterialTheme.colorScheme.secondary, Modifier.fillMaxWidth())
                    TimeMetricBar("Mes", monthSeconds, monthPrefix, semantic.warning, Modifier.fillMaxWidth())
                    TimeMetricBar("Histórico", historicSeconds, "${logs.size} registros", semantic.info, Modifier.fillMaxWidth())
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    TimeMetricBar("Semana", weekSeconds, "$weekStart → $weekEnd", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    TimeMetricBar("Mes", monthSeconds, monthPrefix, semantic.warning, Modifier.weight(1f))
                    TimeMetricBar("Histórico", historicSeconds, "${logs.size} registros", semantic.info, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TimeMetricBar(
    label: String,
    seconds: Int,
    supportingText: String,
    accentColor: Color,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.shadow(1.dp, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(3.dp)
                    .background(Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.3f)))),
            )
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = formatSupportDeskPreciseDuration(seconds), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = accentColor)
                Text(text = supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        WheelCard(title = "Global", totalSeconds = globalSeconds, billableSeconds = globalBillableSeconds, modifier = Modifier.weight(1f))
        WheelCard(title = clientName, totalSeconds = clientSeconds, billableSeconds = clientBillableSeconds, modifier = Modifier.weight(1f))
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
    val billableSweep by animateFloatAsState(targetValue = sweepTarget, animationSpec = tween(600))
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    SectionCard(modifier = modifier, title = title) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(142.dp)) {
                    drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 22f, cap = StrokeCap.Round))
                    drawArc(color = primaryColor, startAngle = -90f, sweepAngle = billableSweep, useCenter = false, style = Stroke(width = 22f, cap = StrokeCap.Round))
                    drawArc(color = secondaryColor, startAngle = -90f + billableSweep, sweepAngle = 360f - billableSweep, useCenter = false, style = Stroke(width = 22f, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = formatSupportDeskClockDuration(totalSeconds), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(text = "Mes actual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendChip("Facturable", formatSupportDeskPreciseDuration(billableSeconds), primaryColor, Modifier.weight(1f))
                LegendChip("Interno", formatSupportDeskPreciseDuration(nonBillableSeconds), secondaryColor, Modifier.weight(1f))
            }
        }
    }
}

// ─── Calendar ─────────────────────────────────────────────────────────────────

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
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CalendarChevronBtn(left = true) {
                    val p = month.previous(); visibleYear = p.year; visibleMonth = p.month
                }
                CalendarChevronBtn(left = false) {
                    val n = month.next(); visibleYear = n.year; visibleMonth = n.month
                }
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = month.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SPANISH_WEEKDAY_LABELS.forEach { weekday ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = weekday, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            cells.chunked(7).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { day ->
                        CompactCalendarDayCell(
                            day = day,
                            maxMinutes = maxMinutes,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSelectDay(day.isoDate)
                                if (!day.inCurrentMonth) { visibleYear = day.year; visibleMonth = day.month }
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
    val semantic = SupportDeskThemeTokens.semanticColors
    val minuteAlpha = if (maxMinutes > 0 && day.minutes > 0) 0.12f + (day.minutes.toFloat() / maxMinutes.toFloat()) * 0.30f else 0.06f
    val borderColor = when {
        day.selected -> MaterialTheme.colorScheme.primary
        day.isPast && day.minutes > 0 -> semantic.success.copy(alpha = 0.4f)
        day.isPast -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        day.inCurrentMonth -> Color.Transparent
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }
    val backgroundColor = when {
        day.selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        day.isPast && day.minutes > 0 -> semantic.successContainer.copy(alpha = minuteAlpha + 0.08f)
        day.isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
        day.inCurrentMonth -> MaterialTheme.colorScheme.primary.copy(alpha = minuteAlpha)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    }
    val dayNumberColor = when {
        day.isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f)
        day.inCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    }
    val minutesColor = when {
        day.selected -> MaterialTheme.colorScheme.primary
        day.isPast -> semantic.success.copy(alpha = 0.80f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1.1f)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .clickable(enabled = !day.isPast, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
    ) {
        Text(text = day.dayNumber, style = MaterialTheme.typography.labelSmall, color = dayNumberColor, fontWeight = if (day.selected) FontWeight.SemiBold else FontWeight.Normal)
        if (day.minutes > 0 && (day.inCurrentMonth || day.isPast)) {
            Text(text = "${day.minutes}m", style = MaterialTheme.typography.labelSmall, color = minutesColor, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.BottomStart))
        }
        if (day.scheduledTasks > 0) {
            Text(
                text = "${day.scheduledTasks}t",
                style = MaterialTheme.typography.labelSmall,
                color = if (day.isPast) MaterialTheme.colorScheme.secondary.copy(alpha = 0.60f) else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

// ─── Shared helpers ───────────────────────────────────────────────────────────

@Composable
private fun LegendChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(text = "$label: $value", style = MaterialTheme.typography.bodySmall)
    }
}

private fun WorkTaskStatus.displayName(): String = when (this) {
    WorkTaskStatus.TODO -> "Pendiente"
    WorkTaskStatus.IN_PROGRESS -> "En curso"
    WorkTaskStatus.WAITING_CLIENT -> "Cliente"
    WorkTaskStatus.REVIEW -> "Revisión"
    WorkTaskStatus.DONE -> "Hecha"
    WorkTaskStatus.ARCHIVED -> "Archivada"
}

@Composable
private fun WorkTaskStatus.headerColor(
    primary: Color,
    secondary: Color,
    semantic: com.requena.supportdesk.designsystem.tokens.SupportDeskSemanticColors,
): Color = when (this) {
    WorkTaskStatus.TODO -> MaterialTheme.colorScheme.outline
    WorkTaskStatus.IN_PROGRESS -> primary
    WorkTaskStatus.WAITING_CLIENT -> semantic.warning
    WorkTaskStatus.REVIEW -> secondary
    WorkTaskStatus.DONE -> semantic.success
    WorkTaskStatus.ARCHIVED -> MaterialTheme.colorScheme.outlineVariant
}

// ─── Calendar data model ──────────────────────────────────────────────────────

private data class CalendarDay(val isoDate: String, val year: Int, val month: Int, val dayNumber: String, val minutes: Int, val scheduledTasks: Int, val inCurrentMonth: Boolean, val selected: Boolean, val isPast: Boolean)

private data class CalendarMonth(val year: Int, val month: Int) {
    val label: String get() = "${SPANISH_MONTH_NAMES[month - 1]} de $year"
    fun previous(): CalendarMonth = if (month == 1) CalendarMonth(year - 1, 12) else CalendarMonth(year, month - 1)
    fun next(): CalendarMonth = if (month == 12) CalendarMonth(year + 1, 1) else CalendarMonth(year, month + 1)
}

private fun buildCalendarCells(month: CalendarMonth, logs: List<TaskLog>, tasks: List<WorkTask>, selectedDay: String?, todayIsoDate: String): List<CalendarDay> {
    val firstDayOffset = dayOfWeekMondayIndex(month.year, month.month, 1)
    val currentMonthDays = daysInMonth(month.year, month.month)
    val previousMonth = month.previous()
    val previousMonthDays = daysInMonth(previousMonth.year, previousMonth.month)
    val totalCells = if (firstDayOffset + currentMonthDays <= 35) 35 else 42
    val nextMonth = month.next()
    val leading = (previousMonthDays - firstDayOffset + 1..previousMonthDays).map { day -> buildCalendarDay(previousMonth.year, previousMonth.month, day, logs, tasks, selectedDay, todayIsoDate, false) }
    val current = (1..currentMonthDays).map { day -> buildCalendarDay(month.year, month.month, day, logs, tasks, selectedDay, todayIsoDate, true) }
    val trailing = (1..(totalCells - firstDayOffset - currentMonthDays)).map { day -> buildCalendarDay(nextMonth.year, nextMonth.month, day, logs, tasks, selectedDay, todayIsoDate, false) }
    return leading + current + trailing
}

private fun buildCalendarDay(year: Int, month: Int, day: Int, logs: List<TaskLog>, tasks: List<WorkTask>, selectedDay: String?, todayIsoDate: String, inCurrentMonth: Boolean): CalendarDay {
    val isoDate = isoDate(year, month, day)
    return CalendarDay(
        isoDate = isoDate, year = year, month = month, dayNumber = day.toString(),
        minutes = logs.filter { it.workDate == isoDate }.sumOf { it.minutes },
        scheduledTasks = tasks.count { it.dueDate == isoDate },
        inCurrentMonth = inCurrentMonth, selected = selectedDay == isoDate, isPast = isoDate < todayIsoDate,
    )
}

private fun parseCalendarMonth(isoDate: String): CalendarMonth {
    val parts = isoDate.split("-")
    return CalendarMonth(parts.getOrNull(0)?.toIntOrNull() ?: 2026, parts.getOrNull(1)?.toIntOrNull() ?: 4)
}

private fun isoDate(year: Int, month: Int, day: Int): String =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31; 4, 6, 9, 11 -> 30; 2 -> if (isLeapYear(year)) 29 else 28; else -> 30
}

private fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

private fun dayOfWeekMondayIndex(year: Int, month: Int, day: Int): Int {
    val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    var y = year; if (month < 3) y -= 1
    val s = (y + y / 4 - y / 100 + y / 400 + offsets[month - 1] + day) % 7
    return if (s == 0) 6 else s - 1
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
    repeat(abs(days)) {
        if (days > 0) { day++; if (day > daysInMonth(year, month)) { day = 1; month++; if (month > 12) { month = 1; year++ } } }
        else { day--; if (day < 1) { month--; if (month < 1) { month = 12; year-- }; day = daysInMonth(year, month) } }
    }
    return isoDate(year, month, day)
}

private val SPANISH_WEEKDAY_LABELS = listOf("L", "M", "X", "J", "V", "S", "D")
private val SPANISH_MONTH_NAMES = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")

private fun parseColor(hex: String): Color = runCatching {
    val v = hex.removePrefix("#").toLong(16)
    Color(red = ((v shr 16) and 0xFF).toInt() / 255f, green = ((v shr 8) and 0xFF).toInt() / 255f, blue = (v and 0xFF).toInt() / 255f, alpha = 1f)
}.getOrElse { Color(0xFF6B7A5B) }
