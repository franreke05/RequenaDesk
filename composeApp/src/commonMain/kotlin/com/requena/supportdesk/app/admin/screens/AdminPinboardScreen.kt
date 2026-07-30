package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.Building2
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.UserRound
import com.composables.icons.lucide.X
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.TaskBoardPlacement
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.ErrorState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.inputs.SearchField
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
import com.requena.supportdesk.features.taskboard.presentation.event.TaskBoardUiEvent
import com.requena.supportdesk.features.taskboard.presentation.state.TaskBoardUiState
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import kotlin.math.abs
import kotlin.math.roundToInt

private val PinWidth = 224.dp
private val PinHeight = 176.dp
private val BoardHeight = 570.dp
private const val BoardMinWidthDp = 760

@Composable
fun AdminPinboardScreen(
    clients: List<Client>,
    tasksState: TasksUiState,
    boardState: TaskBoardUiState,
    onTasksEvent: (TasksUiEvent) -> Unit,
    onBoardEvent: (TaskBoardUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var taskForSteps by remember { mutableStateOf<WorkTask?>(null) }
    val activeTasks = remember(tasksState.tasks) { tasksState.tasks.filterNot(WorkTask::completed) }
    val activeTaskIds = remember(activeTasks) { activeTasks.mapTo(mutableSetOf(), WorkTask::id) }
    val activePlacements = remember(boardState.placements, activeTaskIds) {
        boardState.placements.filter { it.taskId in activeTaskIds && it.boardDate >= boardState.today }
    }
    val placedTaskIds = remember(activePlacements) {
        activePlacements.mapTo(mutableSetOf(), TaskBoardPlacement::taskId)
    }
    val selectedPlacements = remember(activePlacements, boardState.selectedDate) {
        activePlacements.filter { it.boardDate == boardState.selectedDate }
    }
    LaunchedEffect(tasksState.taskStepsCreationVersion) {
        if (tasksState.taskStepsCreationVersion > 0) taskForSteps = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        BoardHeader(
            selectedDate = boardState.selectedDate,
            timeZone = boardState.timeZone,
            plannedCount = selectedPlacements.size,
            isRefreshing = boardState.isLoading,
            onRefresh = { onBoardEvent(TaskBoardUiEvent.Refresh) },
        )

        when {
            boardState.isLoading && boardState.today.isBlank() -> LoadingState(itemCount = 3)
            boardState.errorMessage != null && boardState.today.isBlank() -> ErrorState(
                message = boardState.errorMessage.orEmpty(),
                onRetry = { onBoardEvent(TaskBoardUiEvent.Refresh) },
            )
            boardState.today.isNotBlank() -> {
                WeekNavigator(
                    selectedDate = boardState.selectedDate,
                    today = boardState.today,
                    placements = activePlacements,
                    onSelectDate = { onBoardEvent(TaskBoardUiEvent.SelectDate(it)) },
                    onToday = { onBoardEvent(TaskBoardUiEvent.SelectToday) },
                )

                boardState.errorMessage?.let { message ->
                    InlineBoardError(message = message)
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val wideLayout = maxWidth >= SupportDeskBreakpoints.adminMedium
                    if (wideLayout) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(
                                modifier = Modifier.weight(2f),
                                verticalArrangement = Arrangement.spacedBy(spacing.lg),
                            ) {
                                DailyBoard(
                                    selectedDate = boardState.selectedDate,
                                    placements = selectedPlacements,
                                    tasks = activeTasks,
                                    categories = tasksState.categories,
                                    clients = clients,
                                    savingTaskIds = boardState.savingTaskIds,
                                    onMoveTask = { taskId, x, y ->
                                        onBoardEvent(TaskBoardUiEvent.MoveTask(taskId, x, y))
                                    },
                                    onRemoveTask = { onBoardEvent(TaskBoardUiEvent.RemoveTask(it)) },
                                    onCompleteTask = { onTasksEvent(TasksUiEvent.ToggleTaskCompletion(it)) },
                                    onCreateSteps = { taskForSteps = it },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                BoardCalendar(
                                    selectedDate = boardState.selectedDate,
                                    today = boardState.today,
                                    placements = activePlacements,
                                    onSelectDate = { onBoardEvent(TaskBoardUiEvent.SelectDate(it)) },
                                )
                            }
                            PlanningInbox(
                                tasks = activeTasks.filterNot { it.id in placedTaskIds },
                                allTasks = activeTasks,
                                categories = tasksState.categories,
                                clients = clients,
                                selectedDate = boardState.selectedDate,
                                selectedDayPlacements = selectedPlacements,
                                savingTaskIds = boardState.savingTaskIds,
                                onPlaceTask = { taskId, x, y ->
                                    onBoardEvent(
                                        TaskBoardUiEvent.PlaceTask(
                                            taskId = taskId,
                                            boardDate = boardState.selectedDate,
                                            positionX = x,
                                            positionY = y,
                                        ),
                                    )
                                },
                                onCreateSteps = { taskForSteps = it },
                                modifier = Modifier.weight(1f).widthIn(min = 300.dp, max = 400.dp),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(spacing.lg),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                DailyBoard(
                                    selectedDate = boardState.selectedDate,
                                    placements = selectedPlacements,
                                    tasks = activeTasks,
                                    categories = tasksState.categories,
                                    clients = clients,
                                    savingTaskIds = boardState.savingTaskIds,
                                    onMoveTask = { taskId, x, y ->
                                        onBoardEvent(TaskBoardUiEvent.MoveTask(taskId, x, y))
                                    },
                                    onRemoveTask = { onBoardEvent(TaskBoardUiEvent.RemoveTask(it)) },
                                    onCompleteTask = { onTasksEvent(TasksUiEvent.ToggleTaskCompletion(it)) },
                                    onCreateSteps = { taskForSteps = it },
                                    modifier = Modifier.requiredWidthIn(min = BoardMinWidthDp.dp),
                                )
                            }
                            PlanningInbox(
                                tasks = activeTasks.filterNot { it.id in placedTaskIds },
                                allTasks = activeTasks,
                                categories = tasksState.categories,
                                clients = clients,
                                selectedDate = boardState.selectedDate,
                                selectedDayPlacements = selectedPlacements,
                                savingTaskIds = boardState.savingTaskIds,
                                onPlaceTask = { taskId, x, y ->
                                    onBoardEvent(
                                        TaskBoardUiEvent.PlaceTask(
                                            taskId = taskId,
                                            boardDate = boardState.selectedDate,
                                            positionX = x,
                                            positionY = y,
                                        ),
                                    )
                                },
                                onCreateSteps = { taskForSteps = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            BoardCalendar(
                                selectedDate = boardState.selectedDate,
                                today = boardState.today,
                                placements = activePlacements,
                                onSelectDate = { onBoardEvent(TaskBoardUiEvent.SelectDate(it)) },
                            )
                        }
                    }
                }
            }
        }
    }

    TaskStepsDialog(
        task = taskForSteps,
        client = clients.firstOrNull { it.id == taskForSteps?.clientId },
        category = tasksState.categories.firstOrNull { it.id == taskForSteps?.categoryId },
        isLoading = tasksState.isCreatingTaskSteps,
        errorMessage = tasksState.errorMessage,
        onCreate = { steps ->
            taskForSteps?.id?.let { parentTaskId ->
                onTasksEvent(TasksUiEvent.CreateTaskSteps(parentTaskId, steps))
            }
        },
        onDismiss = { if (!tasksState.isCreatingTaskSteps) taskForSteps = null },
    )
}

@Composable
private fun BoardHeader(
    selectedDate: String,
    timeZone: String,
    plannedCount: Int,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 620.dp
        val title: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(
                    text = "Planificación diaria",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${formatBoardDate(selectedDate)} · $plannedCount tareas · medianoche en $timeZone",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                title()
                SecondaryButton(
                    text = "Actualizar",
                    onClick = onRefresh,
                    isLoading = isRefreshing,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                title()
                SecondaryButton(
                    text = "Actualizar",
                    onClick = onRefresh,
                    isLoading = isRefreshing,
                )
            }
        }
    }
}

@Composable
private fun WeekNavigator(
    selectedDate: String,
    today: String,
    placements: List<TaskBoardPlacement>,
    onSelectDate: (String) -> Unit,
    onToday: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val selected = remember(selectedDate) { BoardDate.parse(selectedDate) }
    val todayDate = remember(today) { BoardDate.parse(today) }
    val weekStart = remember(selected) { selected.startOfWeek() }
    val days = remember(weekStart) { (0..6).map(weekStart::plusDays) }
    val previousTarget = remember(selected) { selected.plusDays(-7) }
    val previousEnabled = weekStart > todayDate.startOfWeek()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val target = if (previousTarget < todayDate) todayDate else previousTarget
                            onSelectDate(target.iso)
                        },
                        enabled = previousEnabled,
                    ) {
                        Icon(Lucide.ChevronLeft, contentDescription = "Semana anterior")
                    }
                    Text(
                        text = weekRangeLabel(days.first(), days.last()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = { onSelectDate(selected.plusDays(7).iso) }) {
                        Icon(Lucide.ChevronRight, contentDescription = "Semana siguiente")
                    }
                }
                TextButton(onClick = onToday) { Text("Hoy") }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                days.forEachIndexed { index, date ->
                    val enabled = date >= todayDate
                    val selectedDay = date.iso == selectedDate
                    val count = placements.count { it.boardDate == date.iso }
                    DayChip(
                        weekday = WEEKDAY_NAMES[index],
                        day = date.day.toString(),
                        count = count,
                        selected = selectedDay,
                        enabled = enabled,
                        isToday = date == todayDate,
                        onClick = { onSelectDate(date.iso) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayChip(
    weekday: String,
    day: String,
    count: Int,
    selected: Boolean,
    enabled: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val background = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    Surface(
        modifier = Modifier
            .width(82.dp)
            .alpha(if (enabled) 1f else 0.42f)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = background,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(weekday, style = MaterialTheme.typography.labelSmall)
            Text(day, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = if (count == 1) "1 tarea" else "$count tareas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DailyBoard(
    selectedDate: String,
    placements: List<TaskBoardPlacement>,
    tasks: List<WorkTask>,
    categories: List<TaskCategory>,
    clients: List<Client>,
    savingTaskIds: Set<String>,
    onMoveTask: (String, Double, Double) -> Unit,
    onRemoveTask: (String) -> Unit,
    onCompleteTask: (String) -> Unit,
    onCreateSteps: (WorkTask) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.32f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Corcho · ${formatBoardDate(selectedDate)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Arrastra los pósits. La posición se guarda al soltarlos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${placements.size} planificadas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BoardHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
            ) {
                if (placements.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Lucide.CalendarDays,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.size(34.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Este día todavía no tiene tareas",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Añádelas desde Pendientes de planificar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                placements.forEachIndexed { index, placement ->
                    val task = tasks.firstOrNull { it.id == placement.taskId } ?: return@forEachIndexed
                    key(task.id) {
                        DraggablePin(
                            task = task,
                            placement = placement,
                            category = categories.firstOrNull { it.id == task.categoryId },
                            client = clients.firstOrNull { it.id == task.clientId },
                            saving = task.id in savingTaskIds,
                            boardWidth = maxWidth,
                            boardHeight = maxHeight,
                            onMove = { x, y -> onMoveTask(task.id, x, y) },
                            onRemove = { onRemoveTask(task.id) },
                            onComplete = { onCompleteTask(task.id) },
                            onCreateSteps = { onCreateSteps(task) },
                            modifier = Modifier.zIndex(index.toFloat() + 1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggablePin(
    task: WorkTask,
    placement: TaskBoardPlacement,
    category: TaskCategory?,
    client: Client?,
    saving: Boolean,
    boardWidth: androidx.compose.ui.unit.Dp,
    boardHeight: androidx.compose.ui.unit.Dp,
    onMove: (Double, Double) -> Unit,
    onRemove: () -> Unit,
    onComplete: () -> Unit,
    onCreateSteps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val trackWidthPx = with(density) { (boardWidth - PinWidth).coerceAtLeast(1.dp).toPx() }
    val trackHeightPx = with(density) { (boardHeight - PinHeight).coerceAtLeast(1.dp).toPx() }
    var xPx by remember(placement.taskId, placement.positionX, trackWidthPx) {
        mutableFloatStateOf(pixelPosition(placement.positionX, trackWidthPx))
    }
    var yPx by remember(placement.taskId, placement.positionY, trackHeightPx) {
        mutableFloatStateOf(pixelPosition(placement.positionY, trackHeightPx))
    }
    var dragging by remember(task.id) { mutableStateOf(false) }
    val pinColor = parsePinColor(category?.colorHex)
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    ElevatedCard(
        modifier = modifier
            .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
            .size(PinWidth, PinHeight)
            .shadow(if (dragging || hovered) 12.dp else 4.dp, RoundedCornerShape(12.dp))
            .pointerInput(task.id, trackWidthPx, trackHeightPx) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragCancel = {
                        dragging = false
                        xPx = pixelPosition(placement.positionX, trackWidthPx)
                        yPx = pixelPosition(placement.positionY, trackHeightPx)
                    },
                    onDragEnd = {
                        dragging = false
                        onMove(
                            normalizedPosition(xPx, trackWidthPx),
                            normalizedPosition(yPx, trackHeightPx),
                        )
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        xPx = (xPx + amount.x).coerceIn(0f, trackWidthPx)
                        yPx = (yPx + amount.y).coerceIn(0f, trackHeightPx)
                    },
                )
            },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(pinColor, CircleShape),
                )
                Text(
                    text = category?.name ?: "Sin etiqueta",
                    style = MaterialTheme.typography.labelSmall,
                    color = pinColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
                IconButton(onClick = onCreateSteps, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Lucide.Plus,
                        contentDescription = "Crear pasos como pósits",
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onComplete, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Lucide.CircleCheck,
                        contentDescription = "Completar tarea",
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Lucide.X,
                        contentDescription = "Quitar del tablero",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Lucide.UserRound, null, modifier = Modifier.size(14.dp))
                Text(
                    task.createdByUserName.ifBlank { "Usuario" },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Lucide.Building2, null, modifier = Modifier.size(14.dp))
                Text(
                    client?.companyName ?: "Tarea interna",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "Vence: ${task.dueDate?.let(::formatBoardDate) ?: "sin fecha límite"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlanningInbox(
    tasks: List<WorkTask>,
    allTasks: List<WorkTask>,
    categories: List<TaskCategory>,
    clients: List<Client>,
    selectedDate: String,
    selectedDayPlacements: List<TaskBoardPlacement>,
    savingTaskIds: Set<String>,
    onPlaceTask: (String, Double, Double) -> Unit,
    onCreateSteps: (WorkTask) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var query by rememberSaveable { mutableStateOf("") }
    var creatorFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var clientFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val creators = remember(allTasks) {
        allTasks
            .filter { it.createdByUserId.isNotBlank() }
            .distinctBy(WorkTask::createdByUserId)
            .sortedBy { it.createdByUserName.lowercase() }
    }
    val filtered = remember(tasks, query, creatorFilter, clientFilter, categoryFilter) {
        tasks.filter { task ->
            val normalizedQuery = query.trim()
            (normalizedQuery.isBlank() ||
                task.title.contains(normalizedQuery, ignoreCase = true) ||
                task.description.contains(normalizedQuery, ignoreCase = true) ||
                task.createdByUserName.contains(normalizedQuery, ignoreCase = true)) &&
                (creatorFilter == null || task.createdByUserId == creatorFilter) &&
                (clientFilter == null || task.clientId == clientFilter) &&
                (categoryFilter == null || task.categoryId == categoryFilter)
        }
    }

    OutlinedCard(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                "Pendientes de planificar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${tasks.size} tareas sin un día activo o futuro",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Buscar tarea o creador",
            )
            FilterBar(
                label = "Creador",
                options = creators.map {
                    FilterOption(it.createdByUserId, it.createdByUserName.ifBlank { "Usuario" })
                },
                selected = creatorFilter,
                onSelected = { creatorFilter = it },
                allLabel = "Todos",
            )
            FilterBar(
                label = "Cliente",
                options = clients.map { FilterOption(it.id, it.companyName) },
                selected = clientFilter,
                onSelected = { clientFilter = it },
                allLabel = "Todos",
            )
            FilterBar(
                label = "Etiqueta",
                options = categories.map { FilterOption(it.id, it.name) },
                selected = categoryFilter,
                onSelected = { categoryFilter = it },
                allLabel = "Todas",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (filtered.isEmpty()) {
                EmptyState(
                    title = "Sin tareas pendientes",
                    message = if (tasks.isEmpty()) {
                        "Todas las tareas activas ya están planificadas."
                    } else {
                        "No hay resultados con estos filtros."
                    },
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    filtered.forEach { task ->
                        key(task.id) {
                            InboxTaskCard(
                                task = task,
                                category = categories.firstOrNull { it.id == task.categoryId },
                                client = clients.firstOrNull { it.id == task.clientId },
                                selectedDate = selectedDate,
                                saving = task.id in savingTaskIds,
                                onAdd = {
                                    val position = nextAvailablePosition(selectedDayPlacements)
                                    onPlaceTask(task.id, position.first, position.second)
                                },
                                onCreateSteps = { onCreateSteps(task) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxTaskCard(
    task: WorkTask,
    category: TaskCategory?,
    client: Client?,
    selectedDate: String,
    saving: Boolean,
    onAdd: () -> Unit,
    onCreateSteps: () -> Unit,
) {
    val pinColor = parsePinColor(category?.colorHex)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(9.dp).background(pinColor, CircleShape))
                Text(
                    category?.name ?: "Sin etiqueta",
                    style = MaterialTheme.typography.labelSmall,
                    color = pinColor,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    task.dueDate?.let(::formatBoardDate) ?: "Sin vencimiento",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${task.createdByUserName.ifBlank { "Usuario" }} · ${client?.companyName ?: "Interna"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SecondaryButton(
                text = "Crear pasos como pósits",
                onClick = onCreateSteps,
                enabled = !saving,
                icon = Lucide.Plus,
                fullWidth = true,
            )
            SecondaryButton(
                text = "Añadir al ${formatBoardDate(selectedDate)}",
                onClick = onAdd,
                enabled = !saving,
                isLoading = saving,
                icon = Lucide.Plus,
                fullWidth = true,
            )
        }
    }
}

@Composable
private fun BoardCalendar(
    selectedDate: String,
    today: String,
    placements: List<TaskBoardPlacement>,
    onSelectDate: (String) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val selected = remember(selectedDate) { BoardDate.parse(selectedDate) }
    val todayDate = remember(today) { BoardDate.parse(today) }
    var visibleYear by rememberSaveable { mutableIntStateOf(selected.year) }
    var visibleMonth by rememberSaveable { mutableIntStateOf(selected.month) }
    LaunchedEffect(selectedDate) {
        visibleYear = selected.year
        visibleMonth = selected.month
    }
    val month = remember(visibleYear, visibleMonth) { BoardMonth(visibleYear, visibleMonth) }
    val cells = remember(month) { calendarCells(month) }
    val previousEnabled = month.previous().lastDay() >= todayDate

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Calendario de planificación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Elige cualquier fecha futura para preparar sus pósits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val previous = month.previous()
                            visibleYear = previous.year
                            visibleMonth = previous.month
                        },
                        enabled = previousEnabled,
                    ) {
                        Icon(Lucide.ChevronLeft, contentDescription = "Mes anterior")
                    }
                    Text(
                        month.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.widthIn(min = 130.dp),
                    )
                    IconButton(
                        onClick = {
                            val next = month.next()
                            visibleYear = next.year
                            visibleMonth = next.month
                        },
                    ) {
                        Icon(Lucide.ChevronRight, contentDescription = "Mes siguiente")
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                WEEKDAY_NAMES.forEach { name ->
                    Text(
                        name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    week.forEach { date ->
                        val enabled = date >= todayDate
                        val selectedDay = date == selected
                        val count = placements.count { it.boardDate == date.iso }
                        CalendarDay(
                            date = date,
                            inMonth = date.month == month.month,
                            selected = selectedDay,
                            today = date == todayDate,
                            enabled = enabled,
                            count = count,
                            onClick = { onSelectDate(date.iso) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: BoardDate,
    inMonth: Boolean,
    selected: Boolean,
    today: Boolean,
    enabled: Boolean,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        today -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (inMonth) 0.30f else 0.12f)
    }
    Surface(
        modifier = modifier
            .height(60.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = background,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                date.day.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected || today) FontWeight.Bold else FontWeight.Normal,
            )
            if (count > 0) {
                Text(
                    if (count == 1) "1 tarea" else "$count tareas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun InlineBoardError(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
}

internal fun nextAvailablePosition(
    placements: List<TaskBoardPlacement>,
): Pair<Double, Double> {
    val candidates = buildList {
        val xs = listOf(0.04, 0.38, 0.72)
        val ys = listOf(0.04, 0.34, 0.64)
        ys.forEach { y -> xs.forEach { x -> add(x to y) } }
    }
    return candidates.firstOrNull { candidate ->
        placements.none { placement ->
            abs(placement.positionX - candidate.first) < 0.18 &&
                abs(placement.positionY - candidate.second) < 0.18
        }
    } ?: run {
        val overflow = placements.size - candidates.size
        val x = (0.08 + (overflow % 5) * 0.16).coerceAtMost(0.84)
        val y = (0.10 + (overflow % 4) * 0.18).coerceAtMost(0.78)
        x to y
    }
}

internal fun pixelPosition(normalized: Double, trackSizePx: Float): Float =
    (normalized.coerceIn(0.0, 1.0) * trackSizePx.coerceAtLeast(1f)).toFloat()

internal fun normalizedPosition(pixel: Float, trackSizePx: Float): Double =
    (pixel / trackSizePx.coerceAtLeast(1f)).toDouble().coerceIn(0.0, 1.0)

internal data class BoardDate(
    val year: Int,
    val month: Int,
    val day: Int,
) : Comparable<BoardDate> {
    val iso: String
        get() = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    override fun compareTo(other: BoardDate): Int =
        compareValuesBy(this, other, BoardDate::year, BoardDate::month, BoardDate::day)

    fun plusDays(amount: Int): BoardDate {
        var value = this
        if (amount >= 0) {
            repeat(amount) { value = value.nextDay() }
        } else {
            repeat(-amount) { value = value.previousDay() }
        }
        return value
    }

    fun startOfWeek(): BoardDate = plusDays(-dayOfWeekMondayIndex(year, month, day))

    private fun nextDay(): BoardDate =
        if (day < daysInMonth(year, month)) {
            copy(day = day + 1)
        } else if (month < 12) {
            BoardDate(year, month + 1, 1)
        } else {
            BoardDate(year + 1, 1, 1)
        }

    private fun previousDay(): BoardDate =
        if (day > 1) {
            copy(day = day - 1)
        } else if (month > 1) {
            BoardDate(year, month - 1, daysInMonth(year, month - 1))
        } else {
            BoardDate(year - 1, 12, 31)
        }

    companion object {
        fun parse(iso: String): BoardDate {
            val parts = iso.split("-")
            return BoardDate(
                year = parts.getOrNull(0)?.toIntOrNull() ?: 2026,
                month = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 1,
                day = parts.getOrNull(2)?.toIntOrNull()?.coerceIn(1, 31) ?: 1,
            )
        }
    }
}

internal data class BoardMonth(val year: Int, val month: Int) {
    val label: String
        get() = "${MONTH_NAMES[month - 1]} $year"

    fun previous(): BoardMonth =
        if (month == 1) BoardMonth(year - 1, 12) else BoardMonth(year, month - 1)

    fun next(): BoardMonth =
        if (month == 12) BoardMonth(year + 1, 1) else BoardMonth(year, month + 1)

    fun lastDay() = BoardDate(year, month, daysInMonth(year, month))
}

internal fun calendarCells(month: BoardMonth): List<BoardDate> {
    val first = BoardDate(month.year, month.month, 1)
    val start = first.plusDays(-dayOfWeekMondayIndex(month.year, month.month, 1))
    return (0 until 42).map(start::plusDays)
}

private fun formatBoardDate(iso: String): String {
    if (iso.isBlank()) return "—"
    val date = BoardDate.parse(iso)
    return "${date.day.toString().padStart(2, '0')}/${date.month.toString().padStart(2, '0')}/${date.year}"
}

private fun weekRangeLabel(start: BoardDate, end: BoardDate): String =
    if (start.month == end.month) {
        "${start.day}–${end.day} de ${MONTH_NAMES[start.month - 1]} ${start.year}"
    } else {
        "${start.day} ${MONTH_NAMES[start.month - 1]} – ${end.day} ${MONTH_NAMES[end.month - 1]} ${end.year}"
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
    val sundayBased =
        (adjustedYear + adjustedYear / 4 - adjustedYear / 100 + adjustedYear / 400 + offsets[month - 1] + day) % 7
    return if (sundayBased == 0) 6 else sundayBased - 1
}

private fun parsePinColor(hex: String?): Color = runCatching {
    val value = hex.orEmpty().removePrefix("#").toLong(16)
    val red = ((value shr 16) and 0xFF).toInt() / 255f
    val green = ((value shr 8) and 0xFF).toInt() / 255f
    val blue = (value and 0xFF).toInt() / 255f
    Color(red = red, green = green, blue = blue, alpha = 1f)
}.getOrElse {
    Color(0xFF6B7A5B)
}

private val WEEKDAY_NAMES = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
private val MONTH_NAMES = listOf(
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
