package com.requena.supportdesk.app.client.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.client.ClientDailyTaskLimit
import com.requena.supportdesk.app.client.ClientNotice
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState

// ── TASKS ─────────────────────────────────────────────────────────────────────
// WorkTask.categoryId is required by the shared tasks feature (admin-owned labels).
// The client add-task form has no label picker, so new tasks fall back to the
// first available category; if none exists yet, adding is disabled.

@Composable
fun ClientTasksScreen(
    state: TasksUiState,
    clientId: String?,
    today: String,
    onEvent: (TasksUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var newTitle by remember { mutableStateOf("") }
    var newNote by remember { mutableStateOf("") }
    val clientTasks = state.dashboardClientTasks
    val todayTasks = remember(clientTasks, today) { clientTasks.filter { (it.dueDate ?: it.createdAt.take(10)) == today } }
    val pastTasks = remember(clientTasks, today) { clientTasks.filter { (it.dueDate ?: it.createdAt.take(10)) != today } }
    val todayTaskCount = todayTasks.size
    val todayDone = remember(todayTasks) { todayTasks.count { it.completed } }
    val limitReached = todayTaskCount >= ClientDailyTaskLimit
    val taskProgress = (todayTaskCount.toFloat() / ClientDailyTaskLimit.toFloat()).coerceIn(0f, 1f)
    val defaultCategoryId = state.categories.firstOrNull()?.id

    LaunchedEffect(state.lastCreatedTaskId) {
        if (state.lastCreatedTaskId != null) {
            newTitle = ""
            newNote = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text("Mis tareas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Maximo $ClientDailyTaskLimit tareas nuevas por dia. Solo para uso personal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(
            modifier = Modifier.fillMaxWidth(),
            title = "Anadir tarea",
            subtitle = "$todayTaskCount/$ClientDailyTaskLimit usadas hoy",
        ) {
            LinearProgressIndicator(
                progress = { taskProgress },
                modifier = Modifier.fillMaxWidth(),
                color = if (limitReached) SupportDeskThemeTokens.semanticColors.danger else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            AnimatedVisibility(visible = limitReached, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
                ClientNotice(message = "Limite diario alcanzado. Vuelve manana para anadir mas tareas.", isError = true)
            }
            state.errorMessage?.let { ClientNotice(message = it, isError = true) }
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Titulo de la tarea") },
                singleLine = true,
                enabled = !limitReached,
                shape = RoundedCornerShape(8.dp),
            )
            OutlinedTextField(
                value = newNote,
                onValueChange = { newNote = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nota (opcional)") },
                minLines = 2,
                enabled = !limitReached,
                shape = RoundedCornerShape(8.dp),
            )
            PrimaryButton(
                text = "Anadir",
                onClick = {
                    val categoryId = defaultCategoryId ?: return@PrimaryButton
                    onEvent(
                        TasksUiEvent.CreateTask(
                            title = newTitle.trim(),
                            description = newNote.trim(),
                            clientId = clientId,
                            categoryId = categoryId,
                        ),
                    )
                },
                enabled = !limitReached && !state.isLoading && newTitle.isNotBlank() && defaultCategoryId != null,
                fullWidth = true,
                isLoading = state.isLoading,
            )
        }

        if (todayTasks.isNotEmpty()) {
            SectionCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Hoy",
                subtitle = "$todayDone/${todayTasks.size} completadas",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    todayTasks.forEach { task ->
                        TaskRow(task = task, onClick = { onEvent(TasksUiEvent.ToggleTaskCompletion(task.id)) })
                    }
                }
            }
        }

        if (pastTasks.isNotEmpty()) {
            SectionCard(modifier = Modifier.fillMaxWidth(), title = "Anteriores") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    pastTasks.take(10).forEach { task ->
                        TaskRow(task = task, onClick = { onEvent(TasksUiEvent.ToggleTaskCompletion(task.id)) })
                    }
                }
            }
        }

        if (clientTasks.isEmpty()) {
            EmptyState(title = "Sin tareas", message = "Usa el formulario para anadir tu primera tarea del dia.")
        }
    }
}

@Composable
private fun TaskRow(task: WorkTask, onClick: () -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val surfaceColor by animateColorAsState(
        targetValue = if (task.completed) semantic.successContainer.copy(alpha = 0.32f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        animationSpec = tween(280),
        label = "taskSurface",
    )
    val borderColor by animateColorAsState(
        targetValue = if (task.completed) semantic.success.copy(alpha = 0.40f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f),
        animationSpec = tween(280),
        label = "taskBorder",
    )
    val checkColor by animateColorAsState(
        targetValue = if (task.completed) semantic.success else MaterialTheme.colorScheme.outline.copy(alpha = 0.40f),
        animationSpec = tween(280),
        label = "taskCheck",
    )
    val textColor by animateColorAsState(
        targetValue = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(280),
        label = "taskText",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        color = surfaceColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(20.dp),
                shape = CircleShape,
                color = checkColor,
                border = if (!task.completed) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline) else null,
            ) {}
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (task.completed) {
                SupportDeskBadge(
                    text = "Hecho",
                    containerColor = semantic.successContainer,
                    contentColor = semantic.success,
                )
            }
        }
    }
}
