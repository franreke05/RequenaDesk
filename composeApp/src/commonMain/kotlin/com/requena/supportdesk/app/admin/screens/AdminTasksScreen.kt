package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.model.WorkTaskStatus
import com.requena.supportdesk.designsystem.components.badges.ClientAccountStatusBadge
import com.requena.supportdesk.designsystem.components.badges.ClientServiceTierBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.ConfirmDialog
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.inputs.SearchField
import com.requena.supportdesk.designsystem.components.layout.InfoRow
import com.requena.supportdesk.designsystem.components.motion.SupportDeskEntrance
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState

@Composable
fun AdminTasksScreen(
    clients: List<Client>,
    tasksState: TasksUiState,
    onTasksEvent: (TasksUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var query by rememberSaveable { mutableStateOf("") }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    val visibleTasks = remember(tasksState.filteredTasks, query) {
        tasksState.filteredTasks.filter { task ->
            query.isBlank() ||
                task.title.contains(query, ignoreCase = true) ||
                task.description.contains(query, ignoreCase = true)
        }
    }
    val selectedTask = tasksState.selectedTask
    val selectedPlanningDay = tasksState.selectedDay ?: tasksState.todayIsoDate
    val selectedCategoryAccent = tasksState.categories
        .firstOrNull { it.id == tasksState.selectedCategoryId }
        ?.colorHex
        ?.let(::parseColor)
        ?: MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val stacked = maxWidth < 1180.dp
        val compact = maxWidth < 760.dp
        val screenScrollState = rememberScrollState()
        val contentModifier = if (stacked) {
            Modifier
                .fillMaxSize()
                .verticalScroll(screenScrollState)
        } else {
            Modifier.fillMaxSize()
        }

        Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            SupportDeskEntrance(index = 0) {
                TaskWorkbenchToolbar(
                    query = query,
                    onQueryChange = { query = it },
                    visibleTasks = visibleTasks,
                    filteredTaskCount = tasksState.filteredTasks.size,
                    totalTaskCount = tasksState.tasks.size,
                    selectedPlanningDay = selectedPlanningDay,
                    selectedCategoryAccent = selectedCategoryAccent,
                    clients = clients,
                    categories = tasksState.categories,
                    selectedCategoryId = tasksState.selectedCategoryId,
                    selectedClientFilterId = tasksState.selectedClientFilterId,
                    statusMessage = tasksState.statusMessage,
                    errorMessage = tasksState.errorMessage,
                    onSelectCategory = { onTasksEvent(TasksUiEvent.SelectCategory(it)) },
                    onSelectClient = { onTasksEvent(TasksUiEvent.SelectClientFilter(it)) },
                    onCreateClick = { showCreateDialog = true },
                )
            }

            if (stacked) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    SupportDeskEntrance(index = 1, horizontal = true) {
                        TaskListPane(
                            tasks = visibleTasks,
                            categories = tasksState.categories,
                            clients = clients,
                            selectedTaskId = selectedTask?.id,
                            isLoading = tasksState.isLoading,
                            errorMessage = tasksState.errorMessage,
                            onSelect = { onTasksEvent(TasksUiEvent.SelectTask(it)) },
                            onTogglePin = { taskId, pinned -> onTasksEvent(TasksUiEvent.ToggleTaskPin(taskId, pinned)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min = if (compact) 340.dp else 400.dp,
                                    max = if (compact) 520.dp else 620.dp,
                                ),
                        )
                    }
                    SupportDeskEntrance(index = 2, horizontal = true) {
                        TaskEditorPane(
                            task = selectedTask,
                            clients = clients,
                            categories = tasksState.categories,
                            selectedPlanningDay = tasksState.selectedDay,
                            todayIsoDate = tasksState.todayIsoDate,
                            onUpdateTask = { taskId, title, description, categoryId, dueDate ->
                                onTasksEvent(TasksUiEvent.UpdateTask(taskId, title, description, categoryId, dueDate))
                            },
                            onUpdateTaskClient = { taskId, clientId ->
                                onTasksEvent(TasksUiEvent.UpdateTaskClient(taskId, clientId))
                            },
                            onToggleCompleted = { onTasksEvent(TasksUiEvent.ToggleTaskCompletion(it)) },
                            onDeleteTask = { onTasksEvent(TasksUiEvent.DeleteTask(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min = if (compact) 520.dp else 560.dp,
                                    max = if (compact) 720.dp else 780.dp,
                                ),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    SupportDeskEntrance(index = 1, horizontal = true, modifier = Modifier.weight(0.56f)) {
                        TaskListPane(
                            tasks = visibleTasks,
                            categories = tasksState.categories,
                            clients = clients,
                            selectedTaskId = selectedTask?.id,
                            isLoading = tasksState.isLoading,
                            errorMessage = tasksState.errorMessage,
                            onSelect = { onTasksEvent(TasksUiEvent.SelectTask(it)) },
                            onTogglePin = { taskId, pinned -> onTasksEvent(TasksUiEvent.ToggleTaskPin(taskId, pinned)) },
                            modifier = Modifier.fillMaxHeight(),
                        )
                    }
                    SupportDeskEntrance(index = 2, horizontal = true, modifier = Modifier.weight(0.44f)) {
                        TaskEditorPane(
                            task = selectedTask,
                            clients = clients,
                            categories = tasksState.categories,
                            selectedPlanningDay = tasksState.selectedDay,
                            todayIsoDate = tasksState.todayIsoDate,
                            onUpdateTask = { taskId, title, description, categoryId, dueDate ->
                                onTasksEvent(TasksUiEvent.UpdateTask(taskId, title, description, categoryId, dueDate))
                            },
                            onUpdateTaskClient = { taskId, clientId ->
                                onTasksEvent(TasksUiEvent.UpdateTaskClient(taskId, clientId))
                            },
                            onToggleCompleted = { onTasksEvent(TasksUiEvent.ToggleTaskCompletion(it)) },
                            onDeleteTask = { onTasksEvent(TasksUiEvent.DeleteTask(it)) },
                            modifier = Modifier.fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }

    TaskCreateDialog(
        visible = showCreateDialog,
        clients = clients,
        categories = tasksState.categories,
        selectedPlanningDay = tasksState.selectedDay,
        todayIsoDate = tasksState.todayIsoDate,
        onDismiss = { showCreateDialog = false },
        onCreate = { title, description, clientId, categoryId, dueDate ->
            onTasksEvent(
                TasksUiEvent.CreateTask(
                    title = title,
                    description = description,
                    clientId = clientId,
                    categoryId = categoryId,
                    dueDate = dueDate,
                ),
            )
            showCreateDialog = false
        },
    )
}

@Composable
private fun TaskWorkbenchToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    visibleTasks: List<WorkTask>,
    filteredTaskCount: Int,
    totalTaskCount: Int,
    selectedPlanningDay: String,
    selectedCategoryAccent: Color,
    clients: List<Client>,
    categories: List<TaskCategory>,
    selectedCategoryId: String?,
    selectedClientFilterId: String?,
    statusMessage: String?,
    errorMessage: String?,
    onSelectCategory: (String?) -> Unit,
    onSelectClient: (String?) -> Unit,
    onCreateClick: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val activeCount = visibleTasks.count { !it.completed }
    val doneCount = visibleTasks.count { it.completed }
    val plannedCount = visibleTasks.count { it.dueDate == selectedPlanningDay }
    val assignedCount = visibleTasks.count { it.clientId != null }
    var showFilterPopup by remember { mutableStateOf(false) }
    val hasActiveFilters = selectedCategoryId != null || selectedClientFilterId != null

    SectionCard(
        title = "Mesa de planificacion",
        subtitle = "${visibleTasks.size} visibles de $filteredTaskCount filtradas - $totalTaskCount totales - dia $selectedPlanningDay",
        neonAccentColor = selectedCategoryAccent,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 700.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SearchField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier.weight(1f),
                            placeholder = "Buscar tarea por nombre o descripcion",
                        )
                        Box {
                            TaskFilterIconButton(
                                hasActiveFilters = hasActiveFilters,
                                onClick = { showFilterPopup = !showFilterPopup },
                            )
                            DropdownMenu(
                                expanded = showFilterPopup,
                                onDismissRequest = { showFilterPopup = false },
                            ) {
                                Box(modifier = Modifier.width(320.dp).padding(spacing.md)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                        FilterBar(
                                            label = "Etiquetas",
                                            options = categories.map { FilterOption(it.id, it.name) },
                                            selected = selectedCategoryId,
                                            onSelected = onSelectCategory,
                                            allLabel = "Todas",
                                            wrap = true,
                                        )
                                        FilterBar(
                                            label = "Clientes",
                                            options = clients.map { FilterOption(it.id, it.companyName) },
                                            selected = selectedClientFilterId,
                                            onSelected = onSelectClient,
                                            allLabel = "Todos",
                                            wrap = true,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    PrimaryButton(
                        text = "Nueva tarea",
                        onClick = onCreateClick,
                        fullWidth = true,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = "Buscar tarea por nombre o descripcion",
                    )
                    Box {
                        TaskFilterIconButton(
                            hasActiveFilters = hasActiveFilters,
                            onClick = { showFilterPopup = !showFilterPopup },
                        )
                        DropdownMenu(
                            expanded = showFilterPopup,
                            onDismissRequest = { showFilterPopup = false },
                        ) {
                            Box(modifier = Modifier.width(320.dp).padding(spacing.md)) {
                                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                    FilterBar(
                                        label = "Etiquetas",
                                        options = categories.map { FilterOption(it.id, it.name) },
                                        selected = selectedCategoryId,
                                        onSelected = onSelectCategory,
                                        allLabel = "Todas",
                                        wrap = true,
                                    )
                                    FilterBar(
                                        label = "Clientes",
                                        options = clients.map { FilterOption(it.id, it.companyName) },
                                        selected = selectedClientFilterId,
                                        onSelected = onSelectClient,
                                        allLabel = "Todos",
                                        wrap = true,
                                    )
                                }
                            }
                        }
                    }
                    PrimaryButton(
                        text = "Nueva tarea",
                        onClick = onCreateClick,
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            PlanningStatPill(
                label = "Visibles",
                value = visibleTasks.size.toString(),
                supportingText = "cola actual",
                accentColor = selectedCategoryAccent,
            )
            PlanningStatPill(
                label = "Activas",
                value = activeCount.toString(),
                supportingText = "por cerrar",
                accentColor = MaterialTheme.colorScheme.primary,
            )
            PlanningStatPill(
                label = "Completadas",
                value = doneCount.toString(),
                supportingText = "en filtros",
                accentColor = semantic.success,
            )
            PlanningStatPill(
                label = "Plan dia",
                value = plannedCount.toString(),
                supportingText = selectedPlanningDay,
                accentColor = MaterialTheme.colorScheme.secondary,
            )
            PlanningStatPill(
                label = "Con cliente",
                value = assignedCount.toString(),
                supportingText = "${visibleTasks.size - assignedCount} internas",
                accentColor = semantic.info,
            )
        }

        statusMessage?.takeIf { it.isNotBlank() }?.let { message ->
            TaskNotice(
                text = message,
                accentColor = semantic.success,
                containerColor = semantic.successContainer,
            )
        }
        errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            TaskNotice(
                text = message,
                accentColor = MaterialTheme.colorScheme.error,
                containerColor = semantic.dangerContainer,
            )
        }
    }
}

@Composable
private fun PlanningStatPill(
    label: String,
    value: String,
    supportingText: String,
    accentColor: Color,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = Modifier
            .widthIn(min = 132.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor,
                                accentColor.copy(alpha = 0.38f),
                            ),
                        ),
                    ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    maxLines = 1,
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TaskNotice(
    text: String,
    accentColor: Color,
    containerColor: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = containerColor.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = accentColor,
        )
    }
}

@Composable
private fun TaskCreateDialog(
    visible: Boolean,
    clients: List<Client>,
    categories: List<TaskCategory>,
    selectedPlanningDay: String?,
    todayIsoDate: String,
    onDismiss: () -> Unit,
    onCreate: (String, String, String?, String, String?) -> Unit,
) {
    if (!visible) return

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedClientId by rememberSaveable { mutableStateOf("none") }
    var selectedCategoryId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id.orEmpty()) }
    var dueDate by rememberSaveable(visible, selectedPlanningDay) {
        mutableStateOf(selectedPlanningDay.takeIf { it != null && it >= todayIsoDate }.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nueva tarea",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre de la tarea") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descripcion") },
                    minLines = 3,
                )
                FilterBar(
                    label = "Etiqueta",
                    options = categories.map { FilterOption(it.id, it.name) },
                    selected = selectedCategoryId.takeIf { it.isNotBlank() },
                    onSelected = { selectedCategoryId = it.orEmpty() },
                    wrap = true,
                )
                FilterBar(
                    label = "Cliente",
                    options = listOf(FilterOption("none", "Sin cliente")) + clients.map { FilterOption(it.id, it.companyName) },
                    selected = selectedClientId,
                    onSelected = { selectedClientId = it ?: "none" },
                    wrap = true,
                )
                TaskScheduleEditor(
                    selectedDate = dueDate,
                    onSelectedDateChange = { dueDate = it },
                    selectedPlanningDay = selectedPlanningDay,
                    todayIsoDate = todayIsoDate,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Crear",
                enabled = title.isNotBlank() && selectedCategoryId.isNotBlank(),
                onClick = {
                    onCreate(
                        title.trim(),
                        description.trim(),
                        selectedClientId.takeUnless { it == "none" },
                        selectedCategoryId,
                        dueDate.takeIf { it.isNotBlank() },
                    )
                    title = ""
                    description = ""
                    selectedClientId = "none"
                    dueDate = selectedPlanningDay.takeIf { it != null && it >= todayIsoDate }.orEmpty()
                },
            )
        },
        dismissButton = {
            SecondaryButton(
                text = "Cancelar",
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun TaskListPane(
    tasks: List<WorkTask>,
    categories: List<TaskCategory>,
    clients: List<Client>,
    selectedTaskId: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onSelect: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val selectedAccent = tasks
        .firstOrNull { it.id == selectedTaskId }
        ?.let { selectedTask -> categories.firstOrNull { it.id == selectedTask.categoryId } }
        ?.colorHex
        ?.let(::parseColor)
        ?: MaterialTheme.colorScheme.primary
    SectionCard(
        modifier = modifier.fillMaxWidth(),
        title = "Backlog operativo",
        subtitle = "${tasks.size} tareas visibles con cliente, etiqueta y ventana de entrega.",
        neonAccentColor = selectedAccent,
        actions = {
            if (isLoading && tasks.isNotEmpty()) {
                Text(
                    text = "Actualizando",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) {
        if (isLoading && tasks.isEmpty()) {
            LoadingState(itemCount = 4)
        } else if (tasks.isEmpty()) {
            EmptyState(
                title = "Sin tareas visibles",
                message = errorMessage ?: "Cambia filtros o crea una nueva tarea.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                items(tasks, key = { it.id }) { task ->
                    val category = categories.firstOrNull { it.id == task.categoryId }
                    val client = clients.firstOrNull { it.id == task.clientId }
                    TaskRow(
                        task = task,
                        category = category,
                        client = client,
                        selected = task.id == selectedTaskId,
                        onTogglePin = { onTogglePin(task.id, task.pinnedAt == null) },
                        onClick = { onSelect(task.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: WorkTask,
    category: TaskCategory?,
    client: Client?,
    selected: Boolean,
    onTogglePin: () -> Unit,
    onClick: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val accentColor = parseColor(category?.colorHex ?: "#6B7A5B")
    val statusColor = task.status.statusAccentColor()
    val targetContainerColor = when {
        selected -> accentColor.copy(alpha = 0.16f)
        task.completed -> semantic.successContainer.copy(alpha = 0.26f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f)
    }
    val targetBorderColor = when {
        selected -> accentColor.copy(alpha = 0.82f)
        task.completed -> semantic.success.copy(alpha = 0.36f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f)
    }
    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(260),
    )
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(260),
    )
    val titleColor by animateColorAsState(
        targetValue = if (task.completed) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(260),
    )
    val rowScale by animateFloatAsState(
        targetValue = if (selected) 1.006f else 1f,
        animationSpec = tween(220),
    )
    val stripeAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.45f,
        animationSpec = tween(260),
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(rowScale)
            .animateContentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (selected) SupportDeskThemeTokens.elevations.subtle else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 112.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = stripeAlpha),
                                statusColor.copy(alpha = stripeAlpha * 0.72f),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.Top,
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        TaskChip(
                            text = category?.name ?: "Sin etiqueta",
                            color = accentColor,
                        )
                        TaskChip(
                            text = task.statusLabel(),
                            color = statusColor,
                        )
                        if (task.pinnedAt != null) {
                            TaskChip(
                                text = "Fijada",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        TaskChip(
                            text = task.dueDate?.let { "Plan $it" } ?: "Sin fecha",
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = formatSupportDeskDuration(task.loggedMinutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        SecondaryButton(
                            text = if (task.pinnedAt != null) "Desfijar" else "Fijar",
                            onClick = onTogglePin,
                        )
                    }
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = client?.companyName ?: "Sin cliente asociado",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatSupportDeskDateTime(task.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WorkTaskStatus.statusAccentColor(): Color {
    val semantic = SupportDeskThemeTokens.semanticColors
    return when (this) {
        WorkTaskStatus.TODO -> MaterialTheme.colorScheme.outline
        WorkTaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        WorkTaskStatus.WAITING_CLIENT -> semantic.warning
        WorkTaskStatus.REVIEW -> MaterialTheme.colorScheme.secondary
        WorkTaskStatus.DONE -> semantic.success
        WorkTaskStatus.ARCHIVED -> MaterialTheme.colorScheme.outlineVariant
    }
}

private fun WorkTask.statusLabel(): String = when {
    completed -> "Hecha"
    else -> status.displayName()
}

private fun WorkTaskStatus.displayName(): String = when (this) {
    WorkTaskStatus.TODO -> "Pendiente"
    WorkTaskStatus.IN_PROGRESS -> "En curso"
    WorkTaskStatus.WAITING_CLIENT -> "Cliente"
    WorkTaskStatus.REVIEW -> "Revision"
    WorkTaskStatus.DONE -> "Hecha"
    WorkTaskStatus.ARCHIVED -> "Archivada"
}

@Composable
private fun TaskEditorPane(
    task: WorkTask?,
    clients: List<Client>,
    categories: List<TaskCategory>,
    selectedPlanningDay: String?,
    todayIsoDate: String,
    onUpdateTask: (String, String, String, String, String?) -> Unit,
    onUpdateTaskClient: (String, String?) -> Unit,
    onToggleCompleted: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    if (task == null) {
        SectionCard(
            modifier = modifier.fillMaxWidth(),
            title = "Editor de planificacion",
            subtitle = "Selecciona una tarea para editar cliente, etiqueta, fecha y estado.",
            neonAccentColor = MaterialTheme.colorScheme.secondary,
        ) {
            EmptyState(
                title = "Nada seleccionado",
                message = "El editor aparecera aqui cuando elijas una tarea del backlog.",
            )
        }
        return
    }

    var confirmDelete by rememberSaveable(task.id) { mutableStateOf(false) }
    var title by remember(task.id) { mutableStateOf(task.title) }
    var description by remember(task.id) { mutableStateOf(task.description) }
    var selectedCategoryId by remember(task.id) { mutableStateOf(task.categoryId) }
    var selectedClientId by remember(task.id) { mutableStateOf(task.clientId ?: "none") }
    var dueDate by remember(task.id) { mutableStateOf(task.dueDate.orEmpty()) }
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
    val editorAccent = selectedCategory?.colorHex?.let(::parseColor) ?: MaterialTheme.colorScheme.primary
    val linkedClient = clients.firstOrNull { it.id == selectedClientId.takeUnless { id -> id == "none" } }

    SectionCard(
        modifier = modifier.fillMaxWidth(),
        title = "Editor de planificacion",
        subtitle = task.title,
        neonAccentColor = editorAccent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            TaskEditorSnapshot(
                task = task,
                category = selectedCategory,
                dueDate = dueDate,
                linkedClient = linkedClient,
                accentColor = editorAccent,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Titulo") },
                singleLine = true,
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descripcion") },
                minLines = 3,
            )
            FilterBar(
                label = "Etiqueta",
                options = categories.map { FilterOption(it.id, it.name) },
                selected = selectedCategoryId,
                onSelected = { selectedCategoryId = it.orEmpty() },
                wrap = true,
            )
            FilterBar(
                label = "Cliente",
                options = listOf(FilterOption("none", "Sin cliente")) + clients.map { FilterOption(it.id, it.companyName) },
                selected = selectedClientId,
                onSelected = { selectedClientId = it ?: "none" },
                wrap = true,
            )
            TaskScheduleEditor(
                selectedDate = dueDate,
                onSelectedDateChange = { dueDate = it },
                selectedPlanningDay = selectedPlanningDay,
                todayIsoDate = todayIsoDate,
            )
            TaskEditorActions(
                task = task,
                canSave = title.isNotBlank() && selectedCategoryId.isNotBlank(),
                onSave = {
                    onUpdateTask(task.id, title, description, selectedCategoryId, dueDate.takeIf { it.isNotBlank() })
                    onUpdateTaskClient(task.id, selectedClientId.takeUnless { it == "none" })
                },
                onToggleCompleted = { onToggleCompleted(task.id) },
                onDelete = { confirmDelete = true },
            )
            TaskClientPanel(
                client = linkedClient,
                dueDate = dueDate,
                accentColor = editorAccent,
            )
        }
    }

    ConfirmDialog(
        visible = confirmDelete,
        title = "Borrar tarea",
        message = "Esta accion eliminara la tarea y sus registros de tiempo asociados.",
        confirmText = "Borrar",
        dismissText = "Cancelar",
        onConfirm = {
            confirmDelete = false
            onDeleteTask(task.id)
        },
        onDismiss = { confirmDelete = false },
    )
}

@Composable
private fun TaskEditorSnapshot(
    task: WorkTask,
    category: TaskCategory?,
    dueDate: String,
    linkedClient: Client?,
    accentColor: Color,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val statusColor = task.status.statusAccentColor()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        color = accentColor.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Workbench",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = formatSupportDeskDuration(task.loggedMinutes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                TaskChip(
                    text = category?.name ?: "Sin etiqueta",
                    color = accentColor,
                )
                TaskChip(
                    text = task.statusLabel(),
                    color = statusColor,
                )
                TaskChip(
                    text = dueDate.ifBlank { "Sin fecha" },
                    color = MaterialTheme.colorScheme.secondary,
                )
                TaskChip(
                    text = linkedClient?.companyName ?: "Sin cliente",
                    color = SupportDeskThemeTokens.semanticColors.info,
                )
            }
        }
    }
}

@Composable
private fun TaskEditorActions(
    task: WorkTask,
    canSave: Boolean,
    onSave: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 560.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                PrimaryButton(
                    text = "Guardar",
                    onClick = onSave,
                    enabled = canSave,
                    fullWidth = true,
                )
                SecondaryButton(
                    text = if (task.completed) "Reabrir" else "Marcar hecha",
                    onClick = onToggleCompleted,
                    fullWidth = true,
                )
                SecondaryButton(
                    text = "Borrar tarea",
                    onClick = onDelete,
                    fullWidth = true,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                PrimaryButton(
                    text = "Guardar",
                    onClick = onSave,
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = if (task.completed) "Reabrir" else "Marcar hecha",
                    onClick = onToggleCompleted,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Borrar tarea",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TaskClientPanel(
    client: Client?,
    dueDate: String,
    accentColor: Color,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = client?.companyName ?: "Sin cliente asociado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Contexto operativo enlazado a la tarea.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (client == null) {
                Text(
                    text = "Puedes mantenerla como tarea interna o asociarla desde el selector de cliente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    ClientServiceTierBadge(client.serviceTier)
                    ClientAccountStatusBadge(client.accountStatus)
                }
                InfoRow(label = "Contacto", value = client.contactName, supportingText = client.email)
                InfoRow(label = "Producto", value = client.productName)
                InfoRow(label = "Programada", value = dueDate.ifBlank { "Sin fecha programada" })
            }
        }
    }
}

@Composable
private fun TaskScheduleEditor(
    selectedDate: String,
    onSelectedDateChange: (String) -> Unit,
    selectedPlanningDay: String?,
    todayIsoDate: String,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val selectablePlanningDay = selectedPlanningDay?.takeIf { it >= todayIsoDate }
    var showCalendarPicker by rememberSaveable(selectedDate, selectablePlanningDay, todayIsoDate) { mutableStateOf(false) }
    val dateAccent = if (selectedDate.isBlank()) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val panelColor by animateColorAsState(
        targetValue = if (selectedDate.isBlank()) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.24f)
        },
        animationSpec = tween(260),
    )
    val borderColor by animateColorAsState(
        targetValue = dateAccent.copy(alpha = if (selectedDate.isBlank()) 0.22f else 0.42f),
        animationSpec = tween(260),
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        color = panelColor,
        border = BorderStroke(1.dp, borderColor),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Programacion",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (selectablePlanningDay != null && selectablePlanningDay != todayIsoDate) {
                            "Referencia del dashboard: $selectablePlanningDay"
                        } else {
                            "Fechas desde hoy en adelante"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TaskChip(
                    text = selectedDate.ifBlank { "Sin fecha" },
                    color = dateAccent,
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                SecondaryButton(
                    text = "Sin fecha",
                    onClick = { onSelectedDateChange("") },
                )
                ScheduleCalendarButton(
                    text = if (selectedDate.isBlank()) "Elegir fecha" else "Cambiar fecha",
                    onClick = { showCalendarPicker = true },
                )
            }
            Text(
                text = "El calendario bloquea fechas pasadas para mantener la planificacion vigente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showCalendarPicker) {
        TaskCalendarPickerDialog(
            initialDate = selectedDate.takeIf { it.isNotBlank() } ?: selectablePlanningDay ?: todayIsoDate,
            selectedDate = selectedDate.takeIf { it.isNotBlank() } ?: selectablePlanningDay ?: todayIsoDate,
            todayIsoDate = todayIsoDate,
            onDismiss = { showCalendarPicker = false },
            onConfirm = {
                onSelectedDateChange(it)
                showCalendarPicker = false
            },
        )
    }
}

@Composable
private fun ScheduleCalendarButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalendarGlyph()
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun CalendarGlyph(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val strokeWidth = 1.4.dp.toPx()
        val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        val bindingRadius = 1.1.dp.toPx()
        val headerTop = 3.dp.toPx()
        val headerHeight = 4.dp.toPx()

        drawRoundRect(
            color = tint,
            size = size,
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, headerTop),
            size = Size(size.width, headerHeight),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        )
        drawCircle(
            color = tint,
            radius = bindingRadius,
            center = Offset(4.5.dp.toPx(), 2.2.dp.toPx()),
        )
        drawCircle(
            color = tint,
            radius = bindingRadius,
            center = Offset(size.width - 4.5.dp.toPx(), 2.2.dp.toPx()),
        )

        val dotRadius = 0.8.dp.toPx()
        val rows = listOf(10.dp.toPx(), 13.dp.toPx())
        val columns = listOf(5.dp.toPx(), 9.dp.toPx(), 13.dp.toPx())
        rows.forEach { y ->
            columns.forEach { x ->
                drawCircle(
                    color = tint.copy(alpha = 0.75f),
                    radius = dotRadius,
                    center = Offset(x, y),
                )
            }
        }
    }
}

@Composable
private fun TaskCalendarPickerDialog(
    initialDate: String,
    selectedDate: String,
    todayIsoDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val initialMonth = remember(initialDate) { parseTaskCalendarMonth(initialDate) }
    var visibleYear by rememberSaveable(initialDate) { mutableStateOf(initialMonth.year) }
    var visibleMonth by rememberSaveable(initialDate) { mutableStateOf(initialMonth.month) }
    var draftDate by rememberSaveable(selectedDate) { mutableStateOf(selectedDate) }
    val month = remember(visibleYear, visibleMonth) { TaskCalendarMonth(visibleYear, visibleMonth) }
    val cells = remember(month, draftDate, todayIsoDate) {
        buildTaskCalendarCells(
            month = month,
            selectedDate = draftDate,
            todayIsoDate = todayIsoDate,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Elegir fecha",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.sm, vertical = spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CalendarNavButton(
                            text = "<",
                            onClick = {
                                val previous = month.previous()
                                visibleYear = previous.year
                                visibleMonth = previous.month
                            },
                        )
                        Text(
                            text = month.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        CalendarNavButton(
                            text = ">",
                            onClick = {
                                val next = month.next()
                                visibleYear = next.year
                                visibleMonth = next.month
                            },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TASK_WEEKDAY_LABELS.forEach { weekday ->
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
                cells.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        week.forEach { day ->
                            TaskCalendarDayCell(
                                day = day,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    draftDate = day.isoDate
                                    if (!day.inCurrentMonth) {
                                        visibleYear = day.year
                                        visibleMonth = day.month
                                    }
                                },
                            )
                        }
                    }
                }
                Text(
                    text = "Solo puedes programar tareas para hoy o dias futuros.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Usar fecha",
                onClick = { onConfirm(draftDate) },
                enabled = draftDate.isNotBlank(),
            )
        },
        dismissButton = {
            SecondaryButton(
                text = "Cancelar",
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun CalendarNavButton(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TaskCalendarDayCell(
    day: TaskCalendarDay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetBackgroundColor = when {
        day.selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        day.isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
        day.inCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
    }
    val targetBorderColor = when {
        day.selected -> MaterialTheme.colorScheme.primary
        day.isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }
    val targetContentColor = when {
        day.isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        day.inCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(220),
    )
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(220),
    )
    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(220),
    )
    val cellScale by animateFloatAsState(
        targetValue = if (day.selected) 1.05f else 1f,
        animationSpec = tween(180),
    )

    Surface(
        modifier = modifier
            .scale(cellScale)
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clickable(enabled = !day.isPast, onClick = onClick)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.dayNumber,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (day.selected || day.isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
            )
            if (day.isToday) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(4.dp)
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(999.dp)),
                )
            }
        }
    }
}

@Immutable
private data class TaskCalendarDay(
    val isoDate: String,
    val year: Int,
    val month: Int,
    val dayNumber: String,
    val inCurrentMonth: Boolean,
    val selected: Boolean,
    val isPast: Boolean,
    val isToday: Boolean,
)

@Immutable
private data class TaskCalendarMonth(
    val year: Int,
    val month: Int,
) {
    val label: String
        get() = "${TASK_MONTH_NAMES[month - 1]} de $year"

    fun previous(): TaskCalendarMonth = if (month == 1) TaskCalendarMonth(year - 1, 12) else TaskCalendarMonth(year, month - 1)

    fun next(): TaskCalendarMonth = if (month == 12) TaskCalendarMonth(year + 1, 1) else TaskCalendarMonth(year, month + 1)
}

private fun buildTaskCalendarCells(
    month: TaskCalendarMonth,
    selectedDate: String,
    todayIsoDate: String,
): List<TaskCalendarDay> {
    val firstDayOffset = taskDayOfWeekMondayIndex(month.year, month.month, 1)
    val currentMonthDays = taskDaysInMonth(month.year, month.month)
    val previousMonth = month.previous()
    val previousMonthDays = taskDaysInMonth(previousMonth.year, previousMonth.month)
    val totalCells = if (firstDayOffset + currentMonthDays <= 35) 35 else 42
    val trailingDays = totalCells - firstDayOffset - currentMonthDays
    val nextMonth = month.next()

    val leading = (previousMonthDays - firstDayOffset + 1..previousMonthDays).map { day ->
        buildTaskCalendarDay(previousMonth.year, previousMonth.month, day, selectedDate, todayIsoDate, false)
    }
    val current = (1..currentMonthDays).map { day ->
        buildTaskCalendarDay(month.year, month.month, day, selectedDate, todayIsoDate, true)
    }
    val trailing = (1..trailingDays).map { day ->
        buildTaskCalendarDay(nextMonth.year, nextMonth.month, day, selectedDate, todayIsoDate, false)
    }
    return leading + current + trailing
}

private fun buildTaskCalendarDay(
    year: Int,
    month: Int,
    day: Int,
    selectedDate: String,
    todayIsoDate: String,
    inCurrentMonth: Boolean,
): TaskCalendarDay {
    val isoDate = taskIsoDate(year, month, day)
    return TaskCalendarDay(
        isoDate = isoDate,
        year = year,
        month = month,
        dayNumber = day.toString(),
        inCurrentMonth = inCurrentMonth,
        selected = selectedDate == isoDate,
        isPast = isoDate < todayIsoDate,
        isToday = isoDate == todayIsoDate,
    )
}

private fun parseTaskCalendarMonth(isoDate: String): TaskCalendarMonth {
    val parts = isoDate.split("-")
    return TaskCalendarMonth(
        year = parts.getOrNull(0)?.toIntOrNull() ?: 2026,
        month = parts.getOrNull(1)?.toIntOrNull() ?: 4,
    )
}

private fun taskIsoDate(year: Int, month: Int, day: Int): String =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

private fun taskDaysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (taskIsLeapYear(year)) 29 else 28
    else -> 30
}

private fun taskIsLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

private fun taskDayOfWeekMondayIndex(year: Int, month: Int, day: Int): Int {
    val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    var adjustedYear = year
    if (month < 3) adjustedYear -= 1
    val sundayBased = (adjustedYear + adjustedYear / 4 - adjustedYear / 100 + adjustedYear / 400 + offsets[month - 1] + day) % 7
    return if (sundayBased == 0) 6 else sundayBased - 1
}

private val TASK_WEEKDAY_LABELS = listOf("L", "M", "X", "J", "V", "S", "D")
private val TASK_MONTH_NAMES = listOf(
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

@Composable
private fun TaskFilterIconButton(hasActiveFilters: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val iconColor = if (hasActiveFilters) accent else MaterialTheme.colorScheme.onSurfaceVariant
    val bgColor = if (hasActiveFilters) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Surface(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = if (hasActiveFilters) BorderStroke(1.dp, accent.copy(alpha = 0.28f)) else null,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val w = size.width
                val h = size.height
                val sw = 2.2.dp.toPx()
                drawLine(iconColor, Offset(0f, 0f), Offset(w, 0f), strokeWidth = sw)
                drawLine(iconColor, Offset(w * 0.18f, h * 0.44f), Offset(w * 0.82f, h * 0.44f), strokeWidth = sw)
                drawLine(iconColor, Offset(w * 0.36f, h * 0.88f), Offset(w * 0.64f, h * 0.88f), strokeWidth = sw)
            }
        }
    }
}

private fun parseColor(hex: String): Color = runCatching {
    val value = hex.removePrefix("#").toLong(16)
    val red = ((value shr 16) and 0xFF).toInt() / 255f
    val green = ((value shr 8) and 0xFF).toInt() / 255f
    val blue = (value and 0xFF).toInt() / 255f
    Color(red = red, green = green, blue = blue, alpha = 1f)
}.getOrElse {
    Color(0xFF6B7A5B)
}
