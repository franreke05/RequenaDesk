package com.requena.supportdesk.app.client.screens.business.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.app.client.components.ClientPortalSurfaceCard
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.ErrorState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
import com.requena.supportdesk.features.business.projects.BusinessCommercialCustomerReference
import com.requena.supportdesk.features.business.projects.BusinessProject
import com.requena.supportdesk.features.business.projects.BusinessProjectStatus
import com.requena.supportdesk.features.business.projects.BusinessProjectsUiState
import com.requena.supportdesk.features.business.projects.BusinessProjectsViewModel
import com.requena.supportdesk.features.business.projects.BusinessTask
import com.requena.supportdesk.features.business.projects.BusinessTaskBoardPlacement
import com.requena.supportdesk.features.business.projects.BusinessTaskLabel
import com.requena.supportdesk.features.business.projects.BusinessTaskPriority
import com.requena.supportdesk.features.business.projects.BusinessTaskStatus
import com.requena.supportdesk.features.business.projects.BusinessWorkSessionStatus
import com.requena.supportdesk.features.business.projects.CreateBusinessProjectInput
import com.requena.supportdesk.features.business.projects.CreateBusinessTaskInput
import com.requena.supportdesk.features.business.projects.CreateBusinessTaskLabelInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessTaskInput
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun BusinessProjectsRoute(
    viewModel: BusinessProjectsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    LaunchedEffect(state.board?.today, state.board?.refreshAfterSeconds) {
        val seconds = state.board?.refreshAfterSeconds ?: return@LaunchedEffect
        delay((seconds.coerceAtLeast(1) + 1) * 1_000)
        viewModel.refresh()
    }
    BusinessProjectsScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onSetFilters = { query, taskStatus, projectId, customerId, labelId ->
            viewModel.setFilters(
                query = query,
                taskStatus = taskStatus,
                projectId = projectId,
                customerId = customerId,
                labelId = labelId,
            )
        },
        onSelectDate = viewModel::selectDate,
        onCreateProject = viewModel::createProject,
        onCompleteProject = viewModel::completeProject,
        onReopenProject = viewModel::reopenProject,
        onArchiveProject = viewModel::archiveProject,
        onCreateTask = viewModel::createTask,
        onUpdateTask = viewModel::updateTask,
        onCreateLabel = viewModel::createLabel,
        onArchiveLabel = viewModel::archiveLabel,
        onPlaceTask = viewModel::placeTask,
        onRemoveTask = viewModel::removeTaskFromBoard,
        onStartTimer = viewModel::startTimer,
        onPauseTimer = viewModel::pauseTimer,
        onResumeTimer = viewModel::resumeTimer,
        onStopTimer = viewModel::stopTimer,
        modifier = modifier,
    )
}

@Composable
private fun BusinessProjectsScreen(
    state: BusinessProjectsUiState,
    onRefresh: () -> Unit,
    onSetFilters: (String, BusinessTaskStatus?, String?, String?, String?) -> Unit,
    onSelectDate: (String) -> Unit,
    onCreateProject: (CreateBusinessProjectInput) -> Unit,
    onCompleteProject: (BusinessProject) -> Unit,
    onReopenProject: (BusinessProject) -> Unit,
    onArchiveProject: (BusinessProject) -> Unit,
    onCreateTask: (CreateBusinessTaskInput) -> Unit,
    onUpdateTask: (String, UpdateBusinessTaskInput) -> Unit,
    onCreateLabel: (CreateBusinessTaskLabelInput) -> Unit,
    onArchiveLabel: (BusinessTaskLabel) -> Unit,
    onPlaceTask: (String, String, Double, Double) -> Unit,
    onRemoveTask: (String) -> Unit,
    onStartTimer: (String) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onStopTimer: () -> Unit,
    modifier: Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        ClientPortalPageHeader(
            "Proyectos y tareas",
            "Planifica el trabajo privado de tu negocio. El tablero diario y el tiempo no se mezclan con el soporte de RequenaDesk.",
        )
        state.errorMessage?.let { ErrorState(message = it, onRetry = onRefresh) }
        if (state.isLoading && state.projects.isEmpty() && state.tasks.isEmpty()) LoadingState()
        ActiveTimerCard(state, onPauseTimer, onResumeTimer, onStopTimer)
        SummaryRow(state)
        WorkspaceFilters(state, onSetFilters, onRefresh)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < SupportDeskBreakpoints.clientWide) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    ProjectCreateCard(state.customers, state.isSaving, onCreateProject)
                    TaskCreateCard(state.projects, state.customers, state.labels, state.isSaving, onCreateTask)
                    LabelsCard(state.labels, state.isSaving, onCreateLabel, onArchiveLabel)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    Box(Modifier.weight(0.8f)) { ProjectCreateCard(state.customers, state.isSaving, onCreateProject) }
                    Box(Modifier.weight(1.1f)) {
                        TaskCreateCard(state.projects, state.customers, state.labels, state.isSaving, onCreateTask)
                    }
                    Box(Modifier.weight(0.8f)) { LabelsCard(state.labels, state.isSaving, onCreateLabel, onArchiveLabel) }
                }
            }
        }
        ProjectsAndTasks(
            state,
            onUpdateTask,
            onStartTimer,
            onCompleteProject,
            onReopenProject,
            onArchiveProject,
        )
        DailyPlanner(state, onSelectDate, onPlaceTask, onRemoveTask)
        MonthlyCalendar(state, onSelectDate)
    }
}

@Composable
private fun SummaryRow(state: BusinessProjectsUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard("Proyectos activos", state.projects.count { it.status == BusinessProjectStatus.ACTIVE }.toString(), Modifier.weight(1f))
        SummaryCard("Tareas abiertas", state.tasks.count { it.status !in setOf(BusinessTaskStatus.DONE, BusinessTaskStatus.ARCHIVED) }.toString(), Modifier.weight(1f))
        SummaryCard("Pendientes", state.pendingTasks.size.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier) {
    ClientPortalSurfaceCard(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WorkspaceFilters(
    state: BusinessProjectsUiState,
    onSetFilters: (String, BusinessTaskStatus?, String?, String?, String?) -> Unit,
    onRefresh: () -> Unit,
) {
    var query by remember(state.query) { mutableStateOf(state.query) }
    ClientPortalSurfaceCard {
        Text("Buscar y filtrar", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Proyecto o tarea") },
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(
                "Todo",
                onClick = { onSetFilters(query, null, null, null, null) },
            )
            BusinessTaskStatus.entries.forEach { status ->
                SecondaryButton(
                    status.label(),
                    onClick = { onSetFilters(query, status, state.selectedProjectId, state.customerId, state.labelId) },
                )
            }
        }
        if (state.projects.isNotEmpty()) {
            Text("Proyecto", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Todos", onClick = { onSetFilters(query, state.taskStatus, null, state.customerId, state.labelId) })
                state.projects.take(12).forEach { project ->
                    SecondaryButton(
                        project.name,
                        onClick = { onSetFilters(query, state.taskStatus, project.id, state.customerId, state.labelId) },
                    )
                }
            }
        }
        if (state.customers.isNotEmpty()) {
            Text("Cliente", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Todos", onClick = { onSetFilters(query, state.taskStatus, state.selectedProjectId, null, state.labelId) })
                state.customers.take(16).forEach { customer ->
                    SecondaryButton(
                        customer.name,
                        onClick = { onSetFilters(query, state.taskStatus, state.selectedProjectId, customer.id, state.labelId) },
                    )
                }
            }
        }
        if (state.labels.any { !it.archived }) {
            Text("Etiqueta", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Todas", onClick = {
                    onSetFilters(query, state.taskStatus, state.selectedProjectId, state.customerId, null)
                })
                state.labels.filterNot(BusinessTaskLabel::archived).forEach { label ->
                    SecondaryButton(
                        label.name,
                        onClick = {
                            onSetFilters(query, state.taskStatus, state.selectedProjectId, state.customerId, label.id)
                        },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(
                "Aplicar filtros",
                onClick = {
                    onSetFilters(query, state.taskStatus, state.selectedProjectId, state.customerId, state.labelId)
                },
            )
            SecondaryButton("Actualizar", onClick = onRefresh)
        }
    }
}

@Composable
private fun ActiveTimerCard(
    state: BusinessProjectsUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    val session = state.activeWorkSession ?: return
    ClientPortalSurfaceCard(Modifier.semantics { contentDescription = "Temporizador activo en ${session.taskTitle}" }) {
        Text("Temporizador · ${session.taskTitle}", style = MaterialTheme.typography.titleMedium)
        Text(
            "${formatDuration(session.elapsedSeconds)} · ${if (session.status == BusinessWorkSessionStatus.PAUSED) "Pausado" else "En marcha"}",
            color = MaterialTheme.colorScheme.primary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (session.status == BusinessWorkSessionStatus.PAUSED) {
                PrimaryButton("Reanudar", onClick = onResume)
            } else {
                SecondaryButton("Pausar", onClick = onPause)
            }
            SecondaryButton("Detener", onClick = onStop)
        }
    }
}

@Composable
private fun ProjectCreateCard(
    customers: List<BusinessCommercialCustomerReference>,
    isSaving: Boolean,
    onCreate: (CreateBusinessProjectInput) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var customerId by remember { mutableStateOf<String?>(null) }
    ClientPortalSurfaceCard {
        Text("Nuevo proyecto", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, enabled = !isSaving)
        OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Descripcion opcional") }, enabled = !isSaving)
        OutlinedTextField(startDate, { startDate = it }, Modifier.fillMaxWidth(), label = { Text("Inicio AAAA-MM-DD") }, enabled = !isSaving)
        OutlinedTextField(dueDate, { dueDate = it }, Modifier.fillMaxWidth(), label = { Text("Limite AAAA-MM-DD") }, enabled = !isSaving)
        if (customers.isNotEmpty()) {
            Text("Cliente: ${customers.firstOrNull { it.id == customerId }?.name ?: "Sin cliente"}", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Ninguno", onClick = { customerId = null })
                customers.take(12).forEach { customer ->
                    SecondaryButton(customer.name, onClick = { customerId = customer.id })
                }
            }
        }
        PrimaryButton(
            "Crear proyecto",
            onClick = {
                onCreate(
                    CreateBusinessProjectInput(
                        customerId = customerId,
                        name = name,
                        description = description.ifBlank { null },
                        startDate = startDate.ifBlank { null },
                        dueDate = dueDate.ifBlank { null },
                    ),
                )
                name = ""
                description = ""
                startDate = ""
                dueDate = ""
                customerId = null
            },
            enabled = name.isNotBlank() && !isSaving,
            isLoading = isSaving,
            fullWidth = true,
        )
    }
}

@Composable
private fun TaskCreateCard(
    projects: List<BusinessProject>,
    customers: List<BusinessCommercialCustomerReference>,
    labels: List<BusinessTaskLabel>,
    isSaving: Boolean,
    onCreate: (CreateBusinessTaskInput) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var projectId by remember { mutableStateOf<String?>(null) }
    var customerId by remember { mutableStateOf<String?>(null) }
    var priority by remember { mutableStateOf(BusinessTaskPriority.NORMAL) }
    var labelIds by remember { mutableStateOf(emptySet<String>()) }
    ClientPortalSurfaceCard {
        Text("Nueva tarea", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Titulo") }, enabled = !isSaving)
        OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Descripcion opcional") }, enabled = !isSaving)
        OutlinedTextField(dueDate, { dueDate = it }, Modifier.fillMaxWidth(), label = { Text("Limite AAAA-MM-DD") }, enabled = !isSaving)
        Text("Proyecto: ${projects.firstOrNull { it.id == projectId }?.name ?: "Bandeja general"}", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("General", onClick = { projectId = null; customerId = null })
            projects.filter { it.status == BusinessProjectStatus.ACTIVE }.take(8).forEach { project ->
                SecondaryButton(project.name, onClick = { projectId = project.id; customerId = null })
            }
        }
        if (customers.isNotEmpty()) {
            Text("Cliente directo: ${customers.firstOrNull { it.id == customerId }?.name ?: "Ninguno"}", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                customers.take(12).forEach { customer ->
                    SecondaryButton(customer.name, onClick = { customerId = customer.id; projectId = null })
                }
            }
        }
        Text("Prioridad: ${priority.label()}", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BusinessTaskPriority.entries.forEach { value ->
                SecondaryButton(value.label(), onClick = { priority = value })
            }
        }
        if (labels.any { !it.archived }) {
            Text("Etiquetas", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.filterNot(BusinessTaskLabel::archived).forEach { label ->
                    SecondaryButton(
                        if (label.id in labelIds) "✓ ${label.name}" else label.name,
                        onClick = {
                            labelIds = if (label.id in labelIds) labelIds - label.id else labelIds + label.id
                        },
                    )
                }
            }
        }
        PrimaryButton(
            "Crear tarea",
            onClick = {
                onCreate(
                    CreateBusinessTaskInput(
                        projectId = projectId,
                        customerId = customerId,
                        title = title,
                        description = description.ifBlank { null },
                        priority = priority,
                        dueDate = dueDate.ifBlank { null },
                        labelIds = labelIds.toList(),
                    ),
                )
                title = ""
                description = ""
                dueDate = ""
                projectId = null
                customerId = null
                priority = BusinessTaskPriority.NORMAL
                labelIds = emptySet()
            },
            enabled = title.isNotBlank() && !isSaving,
            isLoading = isSaving,
            fullWidth = true,
        )
    }
}

@Composable
private fun LabelsCard(
    labels: List<BusinessTaskLabel>,
    isSaving: Boolean,
    onCreate: (CreateBusinessTaskLabelInput) -> Unit,
    onArchive: (BusinessTaskLabel) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#F9A825") }
    ClientPortalSurfaceCard {
        Text("Etiquetas de tareas", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, enabled = !isSaving)
        OutlinedTextField(color, { color = it }, Modifier.fillMaxWidth(), label = { Text("Color #RRGGBB") }, enabled = !isSaving)
        PrimaryButton(
            "Crear etiqueta",
            onClick = {
                onCreate(CreateBusinessTaskLabelInput(name, color))
                name = ""
            },
            enabled = name.isNotBlank() && color.length == 7 && !isSaving,
            isLoading = isSaving,
            fullWidth = true,
        )
        labels.filterNot(BusinessTaskLabel::archived).forEach { label ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label.name, color = colorFromHex(label.colorHex))
                SecondaryButton("Archivar", onClick = { onArchive(label) }, enabled = !isSaving)
            }
        }
    }
}

@Composable
private fun ProjectsAndTasks(
    state: BusinessProjectsUiState,
    onUpdateTask: (String, UpdateBusinessTaskInput) -> Unit,
    onStartTimer: (String) -> Unit,
    onCompleteProject: (BusinessProject) -> Unit,
    onReopenProject: (BusinessProject) -> Unit,
    onArchiveProject: (BusinessProject) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Text("Trabajo", style = MaterialTheme.typography.titleLarge)
    if (state.projects.isEmpty() && state.tasks.isEmpty() && !state.isLoading) {
        EmptyState("Todavia no hay trabajo", "Crea un proyecto o una tarea para empezar.")
        return
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < SupportDeskBreakpoints.clientWide) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                ProjectList(state.projects, state.isSaving, onCompleteProject, onReopenProject, onArchiveProject)
                TaskList(state, onUpdateTask, onStartTimer)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                Column(Modifier.weight(0.42f), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    ProjectList(state.projects, state.isSaving, onCompleteProject, onReopenProject, onArchiveProject)
                }
                Column(Modifier.weight(0.58f), verticalArrangement = Arrangement.spacedBy(spacing.sm)) { TaskList(state, onUpdateTask, onStartTimer) }
            }
        }
    }
}

@Composable
private fun ProjectList(
    projects: List<BusinessProject>,
    isSaving: Boolean,
    onComplete: (BusinessProject) -> Unit,
    onReopen: (BusinessProject) -> Unit,
    onArchive: (BusinessProject) -> Unit,
) {
    Text("Proyectos", style = MaterialTheme.typography.titleMedium)
    projects.forEach { project ->
        ClientPortalSurfaceCard {
            Text(project.name, style = MaterialTheme.typography.titleSmall)
            Text("${project.status.label()} · ${project.openTaskCount} abiertas · ${formatDuration(project.totalSeconds)}")
            project.customerName?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            project.dueDate?.let { Text("Limite $it", style = MaterialTheme.typography.bodySmall) }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (project.status) {
                    BusinessProjectStatus.ACTIVE, BusinessProjectStatus.ON_HOLD ->
                        SecondaryButton("Completar", onClick = { onComplete(project) }, enabled = !isSaving)
                    BusinessProjectStatus.COMPLETED ->
                        SecondaryButton("Reabrir", onClick = { onReopen(project) }, enabled = !isSaving)
                    BusinessProjectStatus.ARCHIVED -> Unit
                }
                if (project.status != BusinessProjectStatus.ARCHIVED) {
                    SecondaryButton("Archivar", onClick = { onArchive(project) }, enabled = !isSaving)
                }
            }
        }
    }
}

@Composable
private fun TaskList(
    state: BusinessProjectsUiState,
    onUpdateTask: (String, UpdateBusinessTaskInput) -> Unit,
    onStartTimer: (String) -> Unit,
) {
    Text("Tareas", style = MaterialTheme.typography.titleMedium)
    state.tasks.take(30).forEach { task ->
        ClientPortalSurfaceCard {
            Text(task.title, style = MaterialTheme.typography.titleSmall)
            Text(
                listOfNotNull(task.projectName, task.customerName, task.dueDate?.let { "Vence $it" }).joinToString(" · ").ifBlank { "Bandeja general" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (task.labels.isNotEmpty()) {
                Text(task.labels.joinToString(" · ") { it.name }, style = MaterialTheme.typography.labelSmall)
            }
            Text("${task.status.label()} · ${task.priority.label()} · ${formatDuration(task.totalSeconds)}")
            if (task.status !in setOf(BusinessTaskStatus.DONE, BusinessTaskStatus.ARCHIVED)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(
                        if (task.status == BusinessTaskStatus.IN_PROGRESS) "En curso" else "Empezar",
                        onClick = { onUpdateTask(task.id, task.update(status = BusinessTaskStatus.IN_PROGRESS)) },
                        enabled = task.status != BusinessTaskStatus.IN_PROGRESS && task.id !in state.savingIds,
                    )
                    SecondaryButton(
                        "Bloquear",
                        onClick = { onUpdateTask(task.id, task.update(status = BusinessTaskStatus.BLOCKED)) },
                        enabled = task.status != BusinessTaskStatus.BLOCKED,
                    )
                    SecondaryButton("Completar", onClick = { onUpdateTask(task.id, task.update(status = BusinessTaskStatus.DONE)) })
                    SecondaryButton("Archivar", onClick = { onUpdateTask(task.id, task.update(status = BusinessTaskStatus.ARCHIVED)) })
                    if (state.activeWorkSession == null) {
                        PrimaryButton("Temporizador", onClick = { onStartTimer(task.id) })
                    }
                }
            } else if (task.status == BusinessTaskStatus.DONE) {
                SecondaryButton(
                    "Reabrir",
                    onClick = { onUpdateTask(task.id, task.update(status = BusinessTaskStatus.TODO)) },
                    enabled = !state.isSaving,
                )
            }
        }
    }
}

@Composable
private fun DailyPlanner(
    state: BusinessProjectsUiState,
    onSelectDate: (String) -> Unit,
    onPlaceTask: (String, String, Double, Double) -> Unit,
    onRemoveTask: (String) -> Unit,
) {
    val board = state.board ?: return
    val selected = state.selectedDate.ifBlank { board.today }
    val weekStart = addDays(selected, -dayOfWeekIndex(selected))
    Text("Planificador diario", style = MaterialTheme.typography.titleLarge)
    ClientPortalSurfaceCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SecondaryButton("Semana anterior", onClick = { onSelectDate(addDays(weekStart, -7)) }, enabled = addDays(weekStart, -7) >= board.today)
            SecondaryButton("Hoy", onClick = { onSelectDate(board.today) })
            SecondaryButton("Semana siguiente", onClick = { onSelectDate(addDays(weekStart, 7)) })
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..6).forEach { offset ->
                val date = addDays(weekStart, offset)
                val count = board.placements.count { it.boardDate == date }
                DayButton(date, date == selected, date >= board.today, count) { onSelectDate(date) }
            }
        }
    }
    ClientPortalSurfaceCard {
        Text("Pendientes de planificar", style = MaterialTheme.typography.titleMedium)
        if (state.pendingTasks.isEmpty()) {
            Text("Todas las tareas abiertas estan planificadas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.pendingTasks.take(20).forEachIndexed { index, task ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleSmall)
                    Text(task.projectName ?: task.customerName ?: "Bandeja general", style = MaterialTheme.typography.bodySmall)
                }
                SecondaryButton(
                    "Añadir al dia",
                    onClick = {
                        val occupied = state.placementsForSelectedDate.size + index
                        onPlaceTask(task.id, selected, 0.04 + (occupied % 4) * 0.235, 0.05 + (occupied / 4) * 0.24)
                    },
                )
            }
        }
    }
    val horizontal = rememberScrollState()
    Box(Modifier.fillMaxWidth().horizontalScroll(horizontal)) {
        Box(
            Modifier
                .widthIn(min = 760.dp)
                .fillMaxWidth()
                .height(460.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF9A6847))
                .semantics { contentDescription = "Corcho del dia $selected" },
        ) {
            state.placementsForSelectedDate.forEach { placement ->
                board.tasks.firstOrNull { it.id == placement.taskId }?.let { task ->
                    DraggablePostIt(
                        task = task,
                        placement = placement,
                        saving = task.id in state.savingIds,
                        onMoved = { x, y -> onPlaceTask(task.id, selected, x, y) },
                        onRemove = { onRemoveTask(task.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayButton(date: String, selected: Boolean, enabled: Boolean, count: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.width(94.dp).padding(10.dp)) {
            Text(weekdayLabel(dayOfWeekIndex(date)), style = MaterialTheme.typography.labelMedium)
            Text(date.drop(5), style = MaterialTheme.typography.titleSmall)
            Text("$count tareas", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DraggablePostIt(
    task: BusinessTask,
    placement: BusinessTaskBoardPlacement,
    saving: Boolean,
    onMoved: (Double, Double) -> Unit,
    onRemove: () -> Unit,
) {
    var x by remember(placement.taskId, placement.updatedAt) { mutableDoubleStateOf(placement.positionX) }
    var y by remember(placement.taskId, placement.updatedAt) { mutableDoubleStateOf(placement.positionY) }
    var parentWidth by remember { mutableIntStateOf(1) }
    var parentHeight by remember { mutableIntStateOf(1) }
    var cardWidth by remember { mutableIntStateOf(190) }
    var cardHeight by remember { mutableIntStateOf(150) }
    var dragging by remember { mutableStateOf(false) }
    val maxX = (parentWidth - cardWidth).coerceAtLeast(1)
    val maxY = (parentHeight - cardHeight).coerceAtLeast(1)
    BoxWithConstraints(
        Modifier.fillMaxSize().onSizeChanged { parentWidth = it.width; parentHeight = it.height },
    ) {
        Surface(
            modifier = Modifier
                .offset { IntOffset((x * maxX).roundToInt(), (y * maxY).roundToInt()) }
                .width(190.dp)
                .onSizeChanged { cardWidth = it.width; cardHeight = it.height }
                .zIndex(if (dragging) 10f else 1f)
                .pointerInput(placement.taskId, placement.updatedAt, parentWidth, parentHeight) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            onMoved(x, y)
                        },
                        onDragCancel = { dragging = false },
                    ) { change, amount ->
                        change.consume()
                        x = (x + amount.x / maxX).coerceIn(0.0, 1.0)
                        y = (y + amount.y / maxY).coerceIn(0.0, 1.0)
                    }
                }
                .semantics { contentDescription = "Posit ${task.title}" },
            color = when (task.priority) {
                BusinessTaskPriority.URGENT -> Color(0xFFFFCDD2)
                BusinessTaskPriority.HIGH -> Color(0xFFFFE0B2)
                BusinessTaskPriority.NORMAL -> Color(0xFFFFF59D)
                BusinessTaskPriority.LOW -> Color(0xFFC8E6C9)
            },
            shadowElevation = if (dragging) 12.dp else 4.dp,
            shape = RoundedCornerShape(4.dp),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(task.title, style = MaterialTheme.typography.titleSmall, color = Color(0xFF2B2118))
                Text(task.projectName ?: task.customerName ?: "General", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4E4034))
                if (task.labels.isNotEmpty()) {
                    Text(task.labels.joinToString(" · ") { it.name }, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4E4034))
                }
                task.dueDate?.let { Text("Vence $it", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4E4034)) }
                Text(if (saving) "Guardando..." else task.status.label(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF4E4034))
                SecondaryButton("Quitar", onClick = onRemove, enabled = !saving)
            }
        }
    }
}

@Composable
private fun MonthlyCalendar(state: BusinessProjectsUiState, onSelectDate: (String) -> Unit) {
    val board = state.board ?: return
    val selected = state.selectedDate.ifBlank { board.today }
    var month by remember(selected.take(7)) { mutableStateOf(selected.take(7)) }
    val first = "$month-01"
    val leading = dayOfWeekIndex(first)
    val days = daysInMonth(first.take(4).toInt(), first.drop(5).take(2).toInt())
    Text("Calendario mensual", style = MaterialTheme.typography.titleLarge)
    ClientPortalSurfaceCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SecondaryButton("Mes anterior", onClick = { month = shiftMonth(month, -1) })
            Text(month, style = MaterialTheme.typography.titleMedium)
            SecondaryButton("Mes siguiente", onClick = { month = shiftMonth(month, 1) })
        }
        Row(Modifier.fillMaxWidth()) {
            (0..6).forEach { Text(weekdayLabel(it), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall) }
        }
        (0 until 6).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                (0..6).forEach { weekday ->
                    val day = week * 7 + weekday - leading + 1
                    if (day !in 1..days) {
                        Box(Modifier.weight(1f).height(54.dp))
                    } else {
                        val date = "$month-${day.toString().padStart(2, '0')}"
                        val count = board.placements.count { it.boardDate == date }
                        Surface(
                            onClick = { onSelectDate(date) },
                            enabled = date >= board.today,
                            color = if (date == selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(54.dp),
                        ) {
                            Column(Modifier.padding(6.dp)) {
                                Text(day.toString(), style = MaterialTheme.typography.bodySmall)
                                if (count > 0) Text("• $count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun BusinessTask.update(status: BusinessTaskStatus) = UpdateBusinessTaskInput(
    projectId = projectId,
    customerId = if (projectId == null) customerId else null,
    title = title,
    description = description,
    priority = priority,
    dueDate = dueDate,
    status = status,
    labelIds = labels.map { it.id },
    expectedVersion = version,
)

private fun BusinessProjectStatus.label() = when (this) {
    BusinessProjectStatus.ACTIVE -> "Activo"
    BusinessProjectStatus.ON_HOLD -> "En pausa"
    BusinessProjectStatus.COMPLETED -> "Completado"
    BusinessProjectStatus.ARCHIVED -> "Archivado"
}

private fun BusinessTaskStatus.label() = when (this) {
    BusinessTaskStatus.TODO -> "Pendiente"
    BusinessTaskStatus.IN_PROGRESS -> "En curso"
    BusinessTaskStatus.BLOCKED -> "Bloqueada"
    BusinessTaskStatus.DONE -> "Hecha"
    BusinessTaskStatus.ARCHIVED -> "Archivada"
}

private fun BusinessTaskPriority.label() = when (this) {
    BusinessTaskPriority.LOW -> "Baja"
    BusinessTaskPriority.NORMAL -> "Normal"
    BusinessTaskPriority.HIGH -> "Alta"
    BusinessTaskPriority.URGENT -> "Urgente"
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3_600
    val minutes = seconds % 3_600 / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun colorFromHex(hex: String): Color = runCatching {
    val value = hex.removePrefix("#").toLong(16)
    Color(
        red = ((value shr 16) and 0xFF).toInt() / 255f,
        green = ((value shr 8) and 0xFF).toInt() / 255f,
        blue = (value and 0xFF).toInt() / 255f,
        alpha = 1f,
    )
}.getOrElse { Color(0xFFF9A825) }

private fun weekdayLabel(index: Int) = listOf("Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom")[index.coerceIn(0, 6)]

private fun dayOfWeekIndex(date: String): Int {
    val year = date.take(4).toInt()
    val month = date.drop(5).take(2).toInt()
    val day = date.drop(8).take(2).toInt()
    var total = 365L * year + (year + 3) / 4 - (year + 99) / 100 + (year + 399) / 400
    total += (367 * month - 362) / 12 + day - 1
    if (month > 2) total -= if (isLeap(year)) 1 else 2
    val epochDay = total - 719528L
    return ((epochDay + 3L) % 7L).toInt().let { if (it < 0) it + 7 else it }
}

private fun addDays(date: String, delta: Int): String {
    var year = date.take(4).toInt()
    var month = date.drop(5).take(2).toInt()
    var day = date.drop(8).take(2).toInt()
    repeat(kotlin.math.abs(delta)) {
        if (delta >= 0) {
            day++
            if (day > daysInMonth(year, month)) {
                day = 1
                month++
                if (month > 12) {
                    month = 1
                    year++
                }
            }
        } else {
            day--
            if (day < 1) {
                month--
                if (month < 1) {
                    month = 12
                    year--
                }
                day = daysInMonth(year, month)
            }
        }
    }
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private fun shiftMonth(value: String, delta: Int): String {
    var year = value.take(4).toInt()
    var month = value.drop(5).take(2).toInt() + delta
    while (month > 12) { month -= 12; year++ }
    while (month < 1) { month += 12; year-- }
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}"
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    2 -> if (isLeap(year)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private fun isLeap(year: Int) = year % 4 == 0 && year % 100 != 0 || year % 400 == 0
