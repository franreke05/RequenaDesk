package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.delay

private val PinCardWidth = 240.dp

@Composable
fun AdminPinboardScreen(
    clients: List<Client>,
    tasksState: TasksUiState,
    onTasksEvent: (TasksUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val today = tasksState.todayIsoDate
    var categoryFilterId by rememberSaveable { mutableStateOf<String?>(null) }
    var secondaryExpanded by rememberSaveable { mutableStateOf(false) }

    val pendingToday = remember(tasksState.tasks, today, categoryFilterId) {
        tasksState.tasks.filter { task ->
            !task.completed && task.dueDate == today &&
                (categoryFilterId == null || task.categoryId == categoryFilterId)
        }
    }
    val secondaryTasks = remember(tasksState.tasks, today, categoryFilterId) {
        tasksState.tasks.filter { task ->
            (task.completed || task.dueDate != today) &&
                (categoryFilterId == null || task.categoryId == categoryFilterId)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${pendingToday.size} chinchetas pendientes para hoy",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
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
            label = "Etiqueta",
            options = tasksState.categories.map { FilterOption(it.id, it.name) },
            selected = categoryFilterId,
            onSelected = { categoryFilterId = it },
            allLabel = "Todas",
            wrap = true,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            CorkboardSurface {
                if (tasksState.isLoading && tasksState.tasks.isEmpty()) {
                    LoadingState(itemCount = 4)
                } else if (pendingToday.isEmpty()) {
                    EmptyState(
                        title = "Nada pendiente para hoy",
                        message = "Las tareas programadas para hoy aparecen aqui como chinchetas. Programa una tarea para hoy desde Tareas.",
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(spacing.lg),
                    ) {
                        pendingToday.forEach { task ->
                            key(task.id) {
                                PinCard(
                                    task = task,
                                    category = tasksState.categories.firstOrNull { it.id == task.categoryId },
                                    client = clients.firstOrNull { it.id == task.clientId },
                                    onToggleCompleted = { onTasksEvent(TasksUiEvent.ToggleTaskCompletion(task.id)) },
                                    onSelect = { onTasksEvent(TasksUiEvent.SelectTask(task.id)) },
                                    modifier = Modifier.width(PinCardWidth),
                                )
                            }
                        }
                    }
                }
            }

            SecondaryTasksSection(
                tasks = secondaryTasks,
                categories = tasksState.categories,
                expanded = secondaryExpanded,
                onExpandedChange = { secondaryExpanded = it },
                onToggleCompleted = { onTasksEvent(TasksUiEvent.ToggleTaskCompletion(it)) },
            )
        }
    }
}

@Composable
private fun CorkboardSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)),
    ) {
        Box(modifier = Modifier.padding(spacing.xl)) {
            content()
        }
    }
}

@Composable
private fun PinCard(
    task: WorkTask,
    category: TaskCategory?,
    client: Client?,
    onToggleCompleted: () -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    var expanded by rememberSaveable(task.id) { mutableStateOf(false) }
    var pendingComplete by remember(task.id) { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pinColor = parsePinColor(category?.colorHex)

    // Marking complete plays a shrink/fade before the state actually flips, so the
    // pin visibly leaves the board instead of instantly vanishing on the next recomposition.
    LaunchedEffect(pendingComplete) {
        if (pendingComplete) {
            delay(240)
            onToggleCompleted()
        }
    }

    val baseRotation = remember(task.id) { pinRotationDegrees(task.id) }
    val rotation by animateFloatAsState(if (hovered) 0f else baseRotation, tween(200), label = "pin_rotation")
    val lift by animateDpAsState(if (hovered) (-6).dp else 0.dp, tween(200), label = "pin_lift")
    val scale by animateFloatAsState(if (pendingComplete) 0.82f else 1f, tween(260), label = "pin_scale")
    val cardAlpha by animateFloatAsState(if (pendingComplete) 0f else 1f, tween(260), label = "pin_alpha")
    val elevation by animateDpAsState(if (hovered) 10.dp else 3.dp, tween(200), label = "pin_elevation")
    val containerColor by animateColorAsState(
        targetValue = if (task.completed) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(220),
        label = "pin_container",
    )

    Box(
        modifier = modifier
            .offset(y = lift)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
                alpha = cardAlpha
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(interactionSource)
                .clickable(interactionSource = interactionSource, indication = null) {
                    expanded = !expanded
                    onSelect()
                }
                .animateContentSize(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.md, start = spacing.md, end = spacing.md, bottom = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .background(pinColor.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = category?.name ?: "Sin etiqueta",
                            style = MaterialTheme.typography.labelSmall,
                            color = pinColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Box(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            if (task.completed) onToggleCompleted() else pendingComplete = true
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Lucide.CircleCheck,
                            contentDescription = if (task.completed) "Reabrir tarea" else "Marcar tarea como completada",
                            tint = if (task.completed) semantic.success else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = client?.companyName ?: "Sin cliente asociado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        if (task.description.isNotBlank()) {
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "Tiempo registrado: ${formatSupportDeskDuration(task.loggedMinutes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .offset(y = (-8).dp)
                .size(16.dp)
                .background(pinColor, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
        )
    }
}

@Composable
private fun SecondaryTasksSection(
    tasks: List<WorkTask>,
    categories: List<TaskCategory>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onToggleCompleted: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, tween(200), label = "secondary_chevron")

    SectionCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(
                    text = "Otras tareas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${tasks.size} completadas hoy o programadas para otro dia",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Lucide.ChevronDown,
                contentDescription = if (expanded) "Contraer otras tareas" else "Expandir otras tareas",
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            if (tasks.isEmpty()) {
                Text(
                    text = "Nada por aqui todavia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.sm),
                )
            } else {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    tasks.forEach { task ->
                        key(task.id) {
                            SecondaryTaskChip(
                                task = task,
                                category = categories.firstOrNull { it.id == task.categoryId },
                                onToggleCompleted = { onToggleCompleted(task.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecondaryTaskChip(
    task: WorkTask,
    category: TaskCategory?,
    onToggleCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val pinColor = parsePinColor(category?.colorHex)
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val backgroundColor by animateColorAsState(
        targetValue = if (hovered) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        },
        animationSpec = tween(150),
        label = "secondary_chip_bg",
    )

    Surface(
        modifier = modifier.hoverable(interactionSource),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Box(modifier = Modifier.size(8.dp).background(pinColor, CircleShape))
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodySmall,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp),
            )
            IconButton(
                onClick = onToggleCompleted,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Lucide.CircleCheck,
                    contentDescription = if (task.completed) "Reabrir tarea" else "Marcar tarea como completada",
                    tint = if (task.completed) semantic.success else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun pinRotationDegrees(taskId: String): Float {
    val bucket = ((taskId.hashCode() % 7) + 7) % 7
    return (bucket - 3) * 1.3f
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
