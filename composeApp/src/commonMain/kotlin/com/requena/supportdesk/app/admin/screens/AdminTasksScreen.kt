package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.WorkTask
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
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
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

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Buscar tarea por nombre o descripcion",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryButton(
                text = "Nueva tarea",
                onClick = { showCreateDialog = true },
            )
            tasksState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        FilterBar(
            label = "Etiquetas",
            options = tasksState.categories.map { FilterOption(it.id, it.name) },
            selected = tasksState.selectedCategoryId,
            onSelected = { onTasksEvent(TasksUiEvent.SelectCategory(it)) },
            allLabel = "Todas",
            wrap = true,
        )

        FilterBar(
            label = "Clientes",
            options = clients.map { FilterOption(it.id, it.companyName) },
            selected = tasksState.selectedClientFilterId,
            onSelected = { onTasksEvent(TasksUiEvent.SelectClientFilter(it)) },
            allLabel = "Todos",
            wrap = true,
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val stacked = maxWidth < SupportDeskBreakpoints.adminMedium
            if (stacked) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    TaskListPane(
                        tasks = visibleTasks,
                        categories = tasksState.categories,
                        clients = clients,
                        selectedTaskId = selectedTask?.id,
                        isLoading = tasksState.isLoading,
                        errorMessage = tasksState.errorMessage,
                        onSelect = { onTasksEvent(TasksUiEvent.SelectTask(it)) },
                        modifier = Modifier.weight(0.48f),
                    )
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
                        modifier = Modifier.weight(0.52f),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    TaskListPane(
                        tasks = visibleTasks,
                        categories = tasksState.categories,
                        clients = clients,
                        selectedTaskId = selectedTask?.id,
                        isLoading = tasksState.isLoading,
                        errorMessage = tasksState.errorMessage,
                        onSelect = { onTasksEvent(TasksUiEvent.SelectTask(it)) },
                        modifier = Modifier.weight(0.56f),
                    )
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
                        modifier = Modifier.weight(0.44f),
                    )
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
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(
        modifier = modifier.fillMaxSize(),
        title = "Tareas",
        subtitle = "Lista principal de trabajo con cliente y etiqueta siempre visibles.",
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
    onClick: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .background(parseColor(category?.colorHex ?: "#6B7A5B"), MaterialTheme.shapes.small)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = category?.name ?: "Sin etiqueta",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = if (task.completed) "Hecha" else "Activa",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = task.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (task.description.isNotBlank()) {
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = client?.companyName ?: "Sin cliente asociado",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = task.dueDate?.let { "Programada para $it" } ?: "Sin fecha programada",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatSupportDeskDuration(task.loggedMinutes),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = formatSupportDeskDateTime(task.updatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
            modifier = modifier.fillMaxSize(),
            title = "Ficha de tarea",
            subtitle = "Selecciona una tarea para editarla y asociarla a un cliente.",
        ) {
            EmptyState(
                title = "Nada seleccionado",
                message = "La ficha lateral aparecera aqui cuando elijas una tarea.",
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
    val linkedClient = clients.firstOrNull { it.id == task.clientId }

    SectionCard(
        modifier = modifier.fillMaxSize(),
        title = task.title,
        subtitle = "Ficha lateral para cliente, etiqueta y datos basicos.",
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryButton(
                    text = "Guardar",
                    onClick = {
                        onUpdateTask(task.id, title, description, selectedCategoryId, dueDate.takeIf { it.isNotBlank() })
                        onUpdateTaskClient(task.id, selectedClientId.takeUnless { it == "none" })
                    },
                    enabled = title.isNotBlank() && selectedCategoryId.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = if (task.completed) "Reabrir" else "Marcar hecha",
                    onClick = { onToggleCompleted(task.id) },
                    modifier = Modifier.weight(1f),
                )
            }
            SecondaryButton(
                text = "Borrar tarea",
                onClick = { confirmDelete = true },
                fullWidth = true,
            )

            SectionCard(
                title = linkedClient?.companyName ?: "Sin cliente asociado",
                subtitle = "Resumen rapido del cliente enlazado a esta tarea.",
            ) {
                if (linkedClient == null) {
                    EmptyState(
                        title = "Tarea interna",
                        message = "Puedes dejarla sin cliente o asociarla desde los filtros superiores.",
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) {
                            ClientServiceTierBadge(linkedClient.serviceTier)
                            ClientAccountStatusBadge(linkedClient.accountStatus)
                        }
                        InfoRow(label = "Contacto", value = linkedClient.contactName, supportingText = linkedClient.email)
                        InfoRow(label = "Producto", value = linkedClient.productName)
                        task.dueDate?.let { scheduledDate ->
                            InfoRow(label = "Programada", value = scheduledDate)
                        }
                    }
                }
            }
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
            text = "Programacion",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = selectedDate.ifBlank { "Sin fecha programada" },
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
                "El calendario se abre tomando como referencia el dia activo del dashboard: $selectablePlanningDay."
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                        text = month.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
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
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
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
