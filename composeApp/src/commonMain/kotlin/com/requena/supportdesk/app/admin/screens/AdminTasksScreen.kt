package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.ListTodo
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.UserRound
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.designsystem.components.badges.ClientAccountStatusBadge
import com.requena.supportdesk.designsystem.components.badges.ClientServiceTierBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.inputs.SearchField
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion
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
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var statusFilter by rememberSaveable { mutableStateOf(TaskStatusFilter.ALL) }
    var taskPendingDeletion by remember { mutableStateOf<WorkTask?>(null) }
    val clientsById = remember(clients) { clients.associateBy(Client::id) }
    val categoriesById = remember(tasksState.categories) { tasksState.categories.associateBy(TaskCategory::id) }
    val visibleTasks = remember(
        tasksState.filteredTasks,
        query,
        clientsById,
        categoriesById,
        statusFilter,
    ) {
        tasksState.filteredTasks.filter { task ->
            task.matchesTaskQuery(query, clientsById, categoriesById) && statusFilter.matches(task)
        }
    }
    val selectedTask = tasksState.selectedTask

    LaunchedEffect(tasksState.lastCreatedTaskId) {
        if (tasksState.lastCreatedTaskId != null) showCreateDialog = false
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layoutMode = resolveTaskLayoutMode(maxWidth)
        val listPane: @Composable (Modifier) -> Unit = { paneModifier ->
            TaskListPane(
                tasks = visibleTasks,
                totalTaskCount = tasksState.tasks.size,
                categories = tasksState.categories,
                clients = clients,
                query = query,
                filtersExpanded = filtersExpanded,
                selectedCategoryId = tasksState.selectedCategoryId,
                selectedClientId = tasksState.selectedClientFilterId,
                statusFilter = statusFilter,
                selectedTaskId = selectedTask?.id,
                isLoading = tasksState.isLoading,
                errorMessage = tasksState.errorMessage,
                onQueryChange = { query = it },
                onToggleFilters = { filtersExpanded = !filtersExpanded },
                onCategoryFilterChange = { onTasksEvent(TasksUiEvent.SelectCategory(it)) },
                onClientFilterChange = { onTasksEvent(TasksUiEvent.SelectClientFilter(it)) },
                onStatusFilterChange = { statusFilter = it },
                onSelect = { onTasksEvent(TasksUiEvent.SelectTask(it)) },
                onCreate = { showCreateDialog = true },
                onDeleteSelected = { selectedTask?.let { taskPendingDeletion = it } },
                modifier = paneModifier,
            )
        }
        val detailPane: @Composable (Modifier) -> Unit = { paneModifier ->
            TaskEditorPane(
                task = selectedTask,
                clients = clients,
                categories = tasksState.categories,
                selectedPlanningDay = tasksState.selectedDay,
                todayIsoDate = tasksState.todayIsoDate,
                isLoading = tasksState.isLoading,
                errorMessage = tasksState.errorMessage,
                onUpdateTask = { taskId, title, description, categoryId, dueDate, clientId ->
                    onTasksEvent(TasksUiEvent.UpdateTask(taskId, title, description, categoryId, dueDate, clientId))
                },
                onToggleCompleted = { onTasksEvent(TasksUiEvent.ToggleTaskCompletion(it)) },
                modifier = paneModifier,
            )
        }

        if (layoutMode == TaskLayoutMode.STACKED) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                listPane(Modifier.fillMaxWidth().weight(0.46f))
                detailPane(Modifier.fillMaxWidth().weight(0.54f))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                listPane(Modifier.fillMaxSize().weight(0.40f))
                detailPane(Modifier.fillMaxSize().weight(0.60f))
            }
        }
    }

    TaskCreateDialog(
        visible = showCreateDialog,
        clients = clients,
        categories = tasksState.categories,
        selectedPlanningDay = tasksState.selectedDay,
        todayIsoDate = tasksState.todayIsoDate,
        isLoading = tasksState.isLoading,
        errorMessage = tasksState.errorMessage,
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
        },
    )

    TaskDeleteConfirmDialog(
        task = taskPendingDeletion,
        onConfirm = {
            taskPendingDeletion?.id?.let { onTasksEvent(TasksUiEvent.DeleteTask(it)) }
            taskPendingDeletion = null
        },
        onDismiss = { taskPendingDeletion = null },
    )
}

@Composable
private fun TaskDeleteConfirmDialog(
    task: WorkTask?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (task == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar tarea", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                "Se eliminará «${task.title}» y también sus registros de tiempo asociados. Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = 44.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Eliminar definitivamente")
            }
        },
        dismissButton = {
            SecondaryButton(text = "Cancelar", onClick = onDismiss)
        },
    )
}

internal enum class TaskLayoutMode {
    STACKED,
    SPLIT,
}

internal fun resolveTaskLayoutMode(availableWidth: androidx.compose.ui.unit.Dp): TaskLayoutMode =
    if (availableWidth < SupportDeskBreakpoints.adminSplitPane) TaskLayoutMode.STACKED else TaskLayoutMode.SPLIT

internal enum class TaskStatusFilter(val label: String) {
    ALL("Todos los estados"),
    ACTIVE("Activas"),
    COMPLETED("Completadas"),
    ;

    fun matches(task: WorkTask): Boolean = when (this) {
        ALL -> true
        ACTIVE -> !task.completed
        COMPLETED -> task.completed
    }
}

internal fun WorkTask.matchesTaskQuery(
    query: String,
    clientsById: Map<String, Client>,
    categoriesById: Map<String, TaskCategory>,
): Boolean {
    if (query.isBlank()) return true
    return title.contains(query, ignoreCase = true) ||
        description.contains(query, ignoreCase = true) ||
        clientsById[clientId]?.companyName?.contains(query, ignoreCase = true) == true ||
        categoriesById[categoryId]?.name?.contains(query, ignoreCase = true) == true
}

@Composable
private fun TaskCreateDialog(
    visible: Boolean,
    clients: List<Client>,
    categories: List<TaskCategory>,
    selectedPlanningDay: String?,
    todayIsoDate: String,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String, String?, String, String?) -> Unit,
) {
    if (!visible) return

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedClientId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCategoryId by rememberSaveable(categories.firstOrNull()?.id) {
        mutableStateOf(categories.firstOrNull()?.id.orEmpty())
    }
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
                verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.md),
            ) {
                TaskFormFields(
                    title = title,
                    description = description,
                    selectedClientId = selectedClientId,
                    selectedCategoryId = selectedCategoryId,
                    dueDate = dueDate,
                    clients = clients,
                    categories = categories,
                    selectedPlanningDay = selectedPlanningDay,
                    todayIsoDate = todayIsoDate,
                    onTitleChange = { title = it },
                    onDescriptionChange = { description = it },
                    onClientChange = { selectedClientId = it },
                    onCategoryChange = { selectedCategoryId = it.orEmpty() },
                    onDueDateChange = { dueDate = it },
                )
                errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                    TaskInlineError(message)
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Crear",
                enabled = title.isNotBlank() && selectedCategoryId.isNotBlank() && !isLoading,
                isLoading = isLoading,
                onClick = {
                    onCreate(
                        title.trim(),
                        description.trim(),
                        selectedClientId,
                        selectedCategoryId,
                        dueDate.takeIf { it.isNotBlank() },
                    )
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
private fun TaskFormFields(
    title: String,
    description: String,
    selectedClientId: String?,
    selectedCategoryId: String,
    dueDate: String,
    clients: List<Client>,
    categories: List<TaskCategory>,
    selectedPlanningDay: String?,
    todayIsoDate: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onClientChange: (String?) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onDueDateChange: (String) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Título *") },
        supportingText = {
            Text(if (title.isBlank()) "Campo obligatorio" else "Nombre visible en la lista de trabajo")
        },
        singleLine = true,
    )
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Descripción") },
        supportingText = { Text("Contexto y resultado esperado de la tarea") },
        minLines = 3,
        maxLines = 6,
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compactFields = maxWidth < 560.dp
        val clientField: @Composable (Modifier) -> Unit = { fieldModifier ->
            TaskSelectionField(
                label = "Cliente asociado",
                selectedValue = selectedClientId,
                options = listOf(TaskSelectOption(null, "Sin cliente")) +
                    clients.map { TaskSelectOption(it.id, it.companyName) },
                onSelected = onClientChange,
                modifier = fieldModifier,
            )
        }
        val categoryField: @Composable (Modifier) -> Unit = { fieldModifier ->
            TaskSelectionField(
                label = "Etiqueta *",
                selectedValue = selectedCategoryId.takeIf { it.isNotBlank() },
                options = categories.map { TaskSelectOption(it.id, it.name) },
                onSelected = onCategoryChange,
                emptyLabel = "Selecciona una etiqueta",
                modifier = fieldModifier,
            )
        }
        if (compactFields) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                clientField(Modifier.fillMaxWidth())
                categoryField(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                clientField(Modifier.weight(1f))
                categoryField(Modifier.weight(1f))
            }
        }
    }

    TaskScheduleEditor(
        selectedDate = dueDate,
        onSelectedDateChange = onDueDateChange,
        selectedPlanningDay = selectedPlanningDay,
        todayIsoDate = todayIsoDate,
    )
}

@Immutable
private data class TaskSelectOption(
    val value: String?,
    val label: String,
)

@Composable
private fun TaskSelectionField(
    label: String,
    selectedValue: String?,
    options: List<TaskSelectOption>,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    emptyLabel: String = "Selecciona una opción",
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label ?: emptyLabel
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = selectedLabel,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(imageVector = Lucide.ChevronDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 240.dp, max = 420.dp),
            ) {
                if (options.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No hay opciones disponibles") },
                        enabled = false,
                        onClick = {},
                    )
                } else {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onSelected(option.value)
                                expanded = false
                            },
                            trailingIcon = if (option.value == selectedValue) {
                                {
                                    Icon(
                                        imageVector = Lucide.CircleCheck,
                                        contentDescription = "Seleccionado",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskInlineError(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TaskListPane(
    tasks: List<WorkTask>,
    totalTaskCount: Int,
    categories: List<TaskCategory>,
    clients: List<Client>,
    query: String,
    filtersExpanded: Boolean,
    selectedCategoryId: String?,
    selectedClientId: String?,
    statusFilter: TaskStatusFilter,
    selectedTaskId: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onQueryChange: (String) -> Unit,
    onToggleFilters: () -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onClientFilterChange: (String?) -> Unit,
    onStatusFilterChange: (TaskStatusFilter) -> Unit,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val activeCount = remember(tasks) { tasks.count { !it.completed } }
    val completedCount = tasks.size - activeCount
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            TaskListHeader(
                canDelete = selectedTaskId != null && !isLoading,
                onCreate = onCreate,
                onDelete = onDeleteSelected,
            )
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Buscar tarea, cliente o etiqueta",
            )
            SecondaryButton(
                text = if (filtersExpanded) "Ocultar filtros" else "Filtros",
                onClick = onToggleFilters,
                fullWidth = true,
            )
            AnimatedVisibility(visible = filtersExpanded) {
                TaskFilters(
                    categories = categories,
                    clients = clients,
                    selectedCategoryId = selectedCategoryId,
                    selectedClientId = selectedClientId,
                    statusFilter = statusFilter,
                    onCategoryChange = onCategoryFilterChange,
                    onClientChange = onClientFilterChange,
                    onStatusChange = onStatusFilterChange,
                )
            }
            errorMessage?.takeIf { it.isNotBlank() }?.let { TaskInlineError(it) }
            Text(
                text = "Mostrando ${tasks.size} de $totalTaskCount · $activeCount activas · $completedCount completadas",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    isLoading && tasks.isEmpty() -> LoadingState(itemCount = 4)
                    tasks.isEmpty() -> EmptyState(
                        title = "Sin tareas visibles",
                        message = errorMessage ?: "Prueba otra búsqueda, cambia los filtros o crea una tarea.",
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                category = categories.firstOrNull { it.id == task.categoryId },
                                client = clients.firstOrNull { it.id == task.clientId },
                                selected = task.id == selectedTaskId,
                                onClick = { onSelect(task.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskListHeader(
    canDelete: Boolean,
    onCreate: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compactHeader = maxWidth < 500.dp
        val heading: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text("Elegir tarea", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Busca, filtra y selecciona tu trabajo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val actions: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                PrimaryButton(text = "Nueva tarea", onClick = onCreate, icon = Lucide.Plus)
                TaskDeleteButton(enabled = canDelete, onClick = onDelete)
            }
        }
        if (compactHeader) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                heading()
                actions()
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                heading()
                actions()
            }
        }
    }
}

@Composable
private fun TaskDeleteButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        border = BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.heightIn(min = 44.dp),
    ) {
        Text("Eliminar", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TaskFilters(
    categories: List<TaskCategory>,
    clients: List<Client>,
    selectedCategoryId: String?,
    selectedClientId: String?,
    statusFilter: TaskStatusFilter,
    onCategoryChange: (String?) -> Unit,
    onClientChange: (String?) -> Unit,
    onStatusChange: (TaskStatusFilter) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        FilterBar(
            label = "Estado",
            options = TaskStatusFilter.entries
                .filterNot { it == TaskStatusFilter.ALL }
                .map { FilterOption(it, it.label) },
            selected = statusFilter.takeUnless { it == TaskStatusFilter.ALL },
            onSelected = { onStatusChange(it ?: TaskStatusFilter.ALL) },
            allLabel = "Todos",
            wrap = true,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val clientField: @Composable (Modifier) -> Unit = { fieldModifier ->
                TaskSelectionField(
                    label = "Cliente",
                    selectedValue = selectedClientId,
                    options = listOf(TaskSelectOption(null, "Todos los clientes")) +
                        clients.map { TaskSelectOption(it.id, it.companyName) },
                    onSelected = onClientChange,
                    modifier = fieldModifier,
                )
            }
            val categoryField: @Composable (Modifier) -> Unit = { fieldModifier ->
                TaskSelectionField(
                    label = "Etiqueta",
                    selectedValue = selectedCategoryId,
                    options = listOf(TaskSelectOption(null, "Todas las etiquetas")) +
                        categories.map { TaskSelectOption(it.id, it.name) },
                    onSelected = onCategoryChange,
                    modifier = fieldModifier,
                )
            }
            if (maxWidth < 600.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    clientField(Modifier.fillMaxWidth())
                    categoryField(Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    clientField(Modifier.weight(1f))
                    categoryField(Modifier.weight(1f))
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
    onClick: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            hovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f)
        },
        animationSpec = tween(SupportDeskMotion.quick),
        label = "taskRowBackground",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Lucide.ListTodo,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = task.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TaskStatusBadge(completed = task.completed)
                }
                Text(
                    text = client?.companyName ?: "Sin cliente asociado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    TaskCategoryBadge(category)
                    TaskFact(icon = Lucide.CalendarDays, text = task.dueDate?.let(::formatTaskDate) ?: "Sin fecha")
                    TaskFact(icon = Lucide.Clock, text = formatSupportDeskDuration(task.loggedMinutes))
                }
            }
        }
    }
}

@Composable
private fun TaskCategoryBadge(category: TaskCategory?) {
    val accent = parseColor(category?.colorHex ?: "#6B7A5B")
    Surface(
        shape = MaterialTheme.shapes.small,
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
    ) {
        Text(
            text = category?.name ?: "Sin etiqueta",
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TaskStatusBadge(completed: Boolean) {
    val label = if (completed) "Completada" else "Activa"
    val background = if (completed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val content = if (completed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Surface(shape = MaterialTheme.shapes.small, color = background, contentColor = content) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (completed) {
                Icon(imageVector = Lucide.CircleCheck, contentDescription = null, modifier = Modifier.size(14.dp))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TaskFact(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TaskEditorPane(
    task: WorkTask?,
    clients: List<Client>,
    categories: List<TaskCategory>,
    selectedPlanningDay: String?,
    todayIsoDate: String,
    isLoading: Boolean,
    errorMessage: String?,
    onUpdateTask: (String, String, String, String, String?, String?) -> Unit,
    onToggleCompleted: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    if (task == null) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(spacing.lg), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Ninguna tarea seleccionada",
                    message = "Elige una tarea de la lista para consultar y editar su ficha.",
                )
            }
        }
        return
    }

    var title by remember(task.id) { mutableStateOf(task.title) }
    var description by remember(task.id) { mutableStateOf(task.description) }
    var selectedCategoryId by remember(task.id) { mutableStateOf(task.categoryId) }
    var selectedClientId by remember(task.id) { mutableStateOf(task.clientId) }
    var dueDate by remember(task.id) { mutableStateOf(task.dueDate.orEmpty()) }
    val linkedClient = clients.firstOrNull { it.id == selectedClientId }
    val category = categories.firstOrNull { it.id == selectedCategoryId }
    val isDirty = title != task.title ||
        description != task.description ||
        selectedCategoryId != task.categoryId ||
        selectedClientId != task.clientId ||
        dueDate != task.dueDate.orEmpty()
    val saveTask = {
        onUpdateTask(
            task.id,
            title.trim(),
            description.trim(),
            selectedCategoryId,
            dueDate.takeIf { it.isNotBlank() },
            selectedClientId,
        )
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            TaskEditorHeader(
                task = task,
                category = category,
                isDirty = isDirty,
            )
            TaskEditorActions(
                completed = task.completed,
                isLoading = isLoading,
                canSave = title.isNotBlank() && selectedCategoryId.isNotBlank() && isDirty,
                onSave = saveTask,
                onToggleCompleted = { onToggleCompleted(task.id) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            TaskFormFields(
                title = title,
                description = description,
                selectedClientId = selectedClientId,
                selectedCategoryId = selectedCategoryId,
                dueDate = dueDate,
                clients = clients,
                categories = categories,
                selectedPlanningDay = selectedPlanningDay,
                todayIsoDate = todayIsoDate,
                onTitleChange = { title = it },
                onDescriptionChange = { description = it },
                onClientChange = { selectedClientId = it },
                onCategoryChange = { selectedCategoryId = it.orEmpty() },
                onDueDateChange = { dueDate = it },
            )
            errorMessage?.takeIf { it.isNotBlank() }?.let { TaskInlineError(it) }
            TaskClientSummary(linkedClient)
            TaskMetadata(task)
        }
    }
}

@Composable
private fun TaskEditorHeader(
    task: WorkTask,
    category: TaskCategory?,
    isDirty: Boolean,
) {
    val spacing = SupportDeskThemeTokens.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 540.dp
        val icon: @Composable () -> Unit = {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Lucide.ListTodo,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        val heading: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = task.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )
                    if (isDirty) {
                        Text(
                            "Cambios sin guardar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    TaskCategoryBadge(category)
                    TaskStatusBadge(task.completed)
                }
            }
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                icon()
                heading()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                icon()
                heading()
            }
        }
    }
}

@Composable
private fun TaskEditorActions(
    completed: Boolean,
    isLoading: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
    onToggleCompleted: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stackedActions = maxWidth < 480.dp
        if (stackedActions) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                PrimaryButton(
                    text = "Guardar cambios",
                    onClick = onSave,
                    enabled = canSave && !isLoading,
                    isLoading = isLoading,
                    fullWidth = true,
                )
                SecondaryButton(
                    text = if (completed) "Reabrir tarea" else "Marcar como hecha",
                    onClick = onToggleCompleted,
                    enabled = !isLoading,
                    fullWidth = true,
                    icon = Lucide.CircleCheck,
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                PrimaryButton(
                    text = "Guardar cambios",
                    onClick = onSave,
                    enabled = canSave && !isLoading,
                    isLoading = isLoading,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = if (completed) "Reabrir tarea" else "Marcar como hecha",
                    onClick = onToggleCompleted,
                    enabled = !isLoading,
                    icon = Lucide.CircleCheck,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TaskClientSummary(client: Client?) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Lucide.UserRound,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    client?.companyName ?: "Sin cliente asociado",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (client == null) {
                Text(
                    "Esta tarea es interna. Puedes asociar un cliente desde el selector superior.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    ClientServiceTierBadge(client.serviceTier)
                    ClientAccountStatusBadge(client.accountStatus)
                }
                Text(
                    listOf(client.contactName, client.email).filter(String::isNotBlank).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                client.productName.takeIf { it.isNotBlank() }?.let {
                    Text("Producto: $it", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun TaskMetadata(task: WorkTask) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text("Información de la tarea", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                TaskMetadataItem("ID", task.id)
                TaskMetadataItem("Tiempo registrado", formatSupportDeskDuration(task.loggedMinutes))
                TaskMetadataItem("Creada", formatSupportDeskDateTime(task.createdAt))
                TaskMetadataItem("Actualizada", formatSupportDeskDateTime(task.updatedAt))
            }
        }
    }
}

@Composable
private fun TaskMetadataItem(label: String, value: String) {
    Column(
        modifier = Modifier.widthIn(min = 150.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
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
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = "Programación",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = selectedDate.takeIf { it.isNotBlank() }?.let(::formatTaskDate) ?: "Sin fecha programada",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            text = if (selectablePlanningDay != null && selectablePlanningDay != todayIsoDate) {
                "El calendario se abre tomando como referencia el día activo del dashboard: ${formatTaskDate(selectablePlanningDay)}."
            } else {
                "Usa el calendario para elegir una fecha de hoy en adelante."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        modifier = modifier.heightIn(min = 44.dp),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            val previous = month.previous()
                            visibleYear = previous.year
                            visibleMonth = previous.month
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = Lucide.ChevronLeft,
                            contentDescription = "Mes anterior",
                        )
                    }
                    Text(
                        text = month.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(
                        onClick = {
                            val next = month.next()
                            visibleYear = next.year
                            visibleMonth = next.month
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = "Mes siguiente",
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
                    text = "Solo puedes programar tareas para hoy o días futuros.",
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
private fun TaskCalendarDayCell(
    day: TaskCalendarDay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        day.selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        day.isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
        day.inCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
    }
    val borderColor = when {
        day.selected -> MaterialTheme.colorScheme.primary
        day.isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }
    val contentColor = when {
        day.isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        day.inCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
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

private fun formatTaskDate(isoDate: String): String {
    val parts = isoDate.split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else isoDate
}

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

private fun parseColor(hex: String): Color = runCatching {
    val value = hex.removePrefix("#").toLong(16)
    val red = ((value shr 16) and 0xFF).toInt() / 255f
    val green = ((value shr 8) and 0xFF).toInt() / 255f
    val blue = (value and 0xFF).toInt() / 255f
    Color(red = red, green = green, blue = blue, alpha = 1f)
}.getOrElse {
    Color(0xFF6B7A5B)
}
