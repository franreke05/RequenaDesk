package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            options = listOf(FilterOption("all", "Todas")) + tasksState.categories.map { FilterOption(it.id, it.name) },
            selected = tasksState.selectedCategoryId ?: "all",
            onSelected = { onTasksEvent(TasksUiEvent.SelectCategory(it?.takeUnless { value -> value == "all" })) },
        )

        FilterBar(
            label = "Clientes",
            options = listOf(FilterOption("all", "Todos")) + clients.map { FilterOption(it.id, it.companyName) },
            selected = tasksState.selectedClientFilterId ?: "all",
            onSelected = { onTasksEvent(TasksUiEvent.SelectClientFilter(it?.takeUnless { value -> value == "all" })) },
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val stacked = maxWidth < 1180.dp
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
                        onUpdateTask = { taskId, title, description, categoryId ->
                            onTasksEvent(TasksUiEvent.UpdateTask(taskId, title, description, categoryId))
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
                        onUpdateTask = { taskId, title, description, categoryId ->
                            onTasksEvent(TasksUiEvent.UpdateTask(taskId, title, description, categoryId))
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
        onDismiss = { showCreateDialog = false },
        onCreate = { title, description, clientId, categoryId ->
            onTasksEvent(
                TasksUiEvent.CreateTask(
                    title = title,
                    description = description,
                    clientId = clientId,
                    categoryId = categoryId,
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
    onDismiss: () -> Unit,
    onCreate: (String, String, String?, String) -> Unit,
) {
    if (!visible) return

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedClientId by rememberSaveable { mutableStateOf("none") }
    var selectedCategoryId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id.orEmpty()) }

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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                )
                FilterBar(
                    label = "Cliente",
                    options = listOf(FilterOption("none", "Sin cliente")) + clients.map { FilterOption(it.id, it.companyName) },
                    selected = selectedClientId,
                    onSelected = { selectedClientId = it ?: "none" },
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
                    )
                    title = ""
                    description = ""
                    selectedClientId = "none"
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
    onUpdateTask: (String, String, String, String) -> Unit,
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
            )
            FilterBar(
                label = "Cliente",
                options = listOf(FilterOption("none", "Sin cliente")) + clients.map { FilterOption(it.id, it.companyName) },
                selected = selectedClientId,
                onSelected = { selectedClientId = it ?: "none" },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryButton(
                    text = "Guardar",
                    onClick = {
                        onUpdateTask(task.id, title, description, selectedCategoryId)
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

private fun parseColor(hex: String): Color = runCatching {
    val value = hex.removePrefix("#").toLong(16)
    val red = ((value shr 16) and 0xFF).toInt() / 255f
    val green = ((value shr 8) and 0xFF).toInt() / 255f
    val blue = (value and 0xFF).toInt() / 255f
    Color(red = red, green = green, blue = blue, alpha = 1f)
}.getOrElse {
    Color(0xFF6B7A5B)
}
