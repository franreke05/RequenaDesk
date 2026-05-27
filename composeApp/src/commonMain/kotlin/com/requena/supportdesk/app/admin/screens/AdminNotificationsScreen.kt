package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.ConfirmDialog
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.components.motion.SupportDeskEntrance
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import kotlin.math.roundToInt

@Composable
fun AdminNotificationsScreen(
    tasksState: TasksUiState,
    onTasksEvent: (TasksUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val stacked = maxWidth < 1080.dp
        val compact = maxWidth < 680.dp
        val listMaxHeight = when {
            compact -> 360.dp
            stacked -> 460.dp
            else -> 620.dp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            SupportDeskEntrance(index = 0) {
                PageHeader(
                    title = "Etiquetas",
                    subtitle = "Taxonomia visual para clasificar tareas, filtros y prioridades operativas.",
                    eyebrow = "Organizacion",
                )
            }

            if (tasksState.isLoading && tasksState.categories.isEmpty()) {
                LoadingState(itemCount = 3)
            }

            tasksState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                SupportDeskEntrance(index = 1) {
                    SectionCard(
                        title = "Datos no disponibles",
                        subtitle = message,
                        neonAccentColor = MaterialTheme.colorScheme.error,
                    ) {
                        SecondaryButton(text = "Reintentar", onClick = { onTasksEvent(TasksUiEvent.Load) })
                    }
                }
            }

            SupportDeskEntrance(index = 2) {
                LabelMetricStrip(
                    categories = tasksState.categories,
                    selectedCategory = tasksState.selectedCategory,
                )
            }

            SupportDeskEntrance(index = 3) {
                LabelCreateCard(
                    onCreate = { name, colorHex ->
                        onTasksEvent(TasksUiEvent.CreateCategory(name, colorHex))
                    },
                )
            }

            if (stacked) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    SupportDeskEntrance(index = 4, horizontal = true) {
                        LabelListPane(
                            categories = tasksState.categories,
                            selectedCategoryId = tasksState.selectedCategoryId,
                            isLoading = tasksState.isLoading,
                            maxListHeight = listMaxHeight,
                            onSelect = { onTasksEvent(TasksUiEvent.SelectCategory(it)) },
                        )
                    }
                    SupportDeskEntrance(index = 5, horizontal = true) {
                        LabelEditorPane(
                            category = tasksState.selectedCategory,
                            onUpdate = { labelId, name, colorHex ->
                                onTasksEvent(TasksUiEvent.UpdateLabel(labelId, name, colorHex))
                            },
                            onDelete = { onTasksEvent(TasksUiEvent.DeleteLabel(it)) },
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                    verticalAlignment = Alignment.Top,
                ) {
                    SupportDeskEntrance(index = 4, horizontal = true, modifier = Modifier.weight(0.43f)) {
                        LabelListPane(
                            categories = tasksState.categories,
                            selectedCategoryId = tasksState.selectedCategoryId,
                            isLoading = tasksState.isLoading,
                            maxListHeight = listMaxHeight,
                            onSelect = { onTasksEvent(TasksUiEvent.SelectCategory(it)) },
                        )
                    }
                    SupportDeskEntrance(index = 5, horizontal = true, modifier = Modifier.weight(0.57f)) {
                        LabelEditorPane(
                            category = tasksState.selectedCategory,
                            onUpdate = { labelId, name, colorHex ->
                                onTasksEvent(TasksUiEvent.UpdateLabel(labelId, name, colorHex))
                            },
                            onDelete = { onTasksEvent(TasksUiEvent.DeleteLabel(it)) },
                        )
                    }
                }
            }

            Box(modifier = Modifier.height(spacing.md))
        }
    }
}

@Composable
private fun LabelMetricStrip(
    categories: List<TaskCategory>,
    selectedCategory: TaskCategory?,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val inUse = categories.count { it.tasksCount > 0 }
    val free = categories.size - inUse
    val linkedTasks = categories.sumOf { it.tasksCount }
    val accent = parseCategoryColor(
        selectedCategory?.colorHex
            ?: categories.maxByOrNull { it.tasksCount }?.colorHex
            ?: DEFAULT_LABEL_COLOR,
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val stacked = maxWidth < 820.dp
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                MetricCard(
                    label = "Etiquetas",
                    value = categories.size.toString(),
                    supportingText = if (categories.isEmpty()) {
                        "Sin taxonomia creada"
                    } else {
                        "${categories.size} colores registrados"
                    },
                    neonAccentColor = accent,
                    modifier = Modifier.fillMaxWidth(),
                )
                MetricCard(
                    label = "En uso",
                    value = inUse.toString(),
                    supportingText = taskCountLabel(linkedTasks) + " asociadas",
                    neonAccentColor = semantic.success,
                    modifier = Modifier.fillMaxWidth(),
                )
                MetricCard(
                    label = "Libres",
                    value = free.toString(),
                    supportingText = "Disponibles para borrar",
                    neonAccentColor = semantic.info,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                MetricCard(
                    label = "Etiquetas",
                    value = categories.size.toString(),
                    supportingText = if (categories.isEmpty()) {
                        "Sin taxonomia creada"
                    } else {
                        "${categories.size} colores registrados"
                    },
                    neonAccentColor = accent,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "En uso",
                    value = inUse.toString(),
                    supportingText = taskCountLabel(linkedTasks) + " asociadas",
                    neonAccentColor = semantic.success,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Libres",
                    value = free.toString(),
                    supportingText = "Disponibles para borrar",
                    neonAccentColor = semantic.info,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LabelCreateCard(
    onCreate: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var categoryName by rememberSaveable { mutableStateOf("") }
    var selectedColor by rememberSaveable { mutableStateOf(DEFAULT_LABEL_COLOR) }
    val spacing = SupportDeskThemeTokens.spacing
    val accent = parseCategoryColor(selectedColor)

    SectionCard(
        modifier = modifier.fillMaxWidth(),
        title = "Nuevo color de taxonomia",
        subtitle = "Define el nombre y su color antes de usarlo en tareas.",
        neonAccentColor = accent,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 760.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    LabelCreateFields(
                        categoryName = categoryName,
                        selectedColor = selectedColor,
                        onNameChange = { categoryName = it },
                        onColorSelect = { selectedColor = normalizeHex(it) },
                    )
                    LabelStudioPreview(
                        name = categoryName.takeIf { it.isNotBlank() } ?: "Nueva etiqueta",
                        colorHex = selectedColor,
                        tasksCount = 0,
                    )
                    PrimaryButton(
                        text = "Crear etiqueta",
                        onClick = {
                            onCreate(categoryName.trim(), normalizeHex(selectedColor))
                            categoryName = ""
                        },
                        enabled = categoryName.isNotBlank(),
                        fullWidth = true,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(0.58f),
                        verticalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        LabelCreateFields(
                            categoryName = categoryName,
                            selectedColor = selectedColor,
                            onNameChange = { categoryName = it },
                            onColorSelect = { selectedColor = normalizeHex(it) },
                        )
                        PrimaryButton(
                            text = "Crear etiqueta",
                            onClick = {
                                onCreate(categoryName.trim(), normalizeHex(selectedColor))
                                categoryName = ""
                            },
                            enabled = categoryName.isNotBlank(),
                        )
                    }
                    LabelStudioPreview(
                        name = categoryName.takeIf { it.isNotBlank() } ?: "Nueva etiqueta",
                        colorHex = selectedColor,
                        tasksCount = 0,
                        modifier = Modifier.weight(0.42f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelCreateFields(
    categoryName: String,
    selectedColor: String,
    onNameChange: (String) -> Unit,
    onColorSelect: (String) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        OutlinedTextField(
            value = categoryName,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre de la etiqueta") },
            singleLine = true,
        )
        ColorPickerField(
            selectedColor = selectedColor,
            onSelect = onColorSelect,
        )
    }
}

@Composable
private fun LabelListPane(
    categories: List<TaskCategory>,
    selectedCategoryId: String?,
    isLoading: Boolean,
    maxListHeight: Dp,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val accent = parseCategoryColor(
        categories.firstOrNull { it.id == selectedCategoryId }?.colorHex
            ?: categories.firstOrNull()?.colorHex
            ?: DEFAULT_LABEL_COLOR,
    )

    SectionCard(
        modifier = modifier.fillMaxWidth(),
        title = "Taxonomia activa",
        subtitle = "Selecciona una etiqueta para ajustar nombre, color y uso.",
        neonAccentColor = accent,
        actions = {
            if (selectedCategoryId != null) {
                SecondaryButton(text = "Limpiar", onClick = { onSelect(null) })
            }
        },
    ) {
        when {
            isLoading && categories.isEmpty() -> LoadingState(itemCount = 4)
            categories.isEmpty() -> EmptyState(
                title = "Sin etiquetas",
                message = "Crea la primera etiqueta para empezar a organizar tareas.",
            )
            else -> {
                PaletteRail(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onSelect = onSelect,
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    items(categories, key = { it.id }) { category ->
                        LabelListRow(
                            category = category,
                            selected = category.id == selectedCategoryId,
                            onSelect = { onSelect(category.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteRail(
    categories: List<TaskCategory>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(100.dp)),
        ) {
            categories.forEach { category ->
                Box(
                    modifier = Modifier
                        .weight(category.tasksCount.coerceAtLeast(1).toFloat())
                        .fillMaxSize()
                        .background(parseCategoryColor(category.colorHex)),
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            categories.forEach { category ->
                PaletteChip(
                    category = category,
                    selected = category.id == selectedCategoryId,
                    onClick = {
                        onSelect(if (category.id == selectedCategoryId) null else category.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun PaletteChip(
    category: TaskCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = parseCategoryColor(category.colorHex)
    val bgColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        animationSpec = tween(180),
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
        animationSpec = tween(180),
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
    )

    Surface(
        modifier = Modifier
            .scale(scale)
            .border(1.dp, borderColor, RoundedCornerShape(100.dp))
            .clip(RoundedCornerShape(100.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(100.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(9.dp).background(accent, CircleShape))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LabelListRow(
    category: TaskCategory,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val accent = parseCategoryColor(category.colorHex)
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            accent.copy(alpha = 0.13f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.13f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            accent.copy(alpha = 0.78f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
        },
        animationSpec = tween(180),
    )
    val rowScale by animateFloatAsState(
        targetValue = if (selected) 1.01f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
    )
    val rowShadow by animateDpAsState(
        targetValue = if (selected) 7.dp else 1.dp,
        animationSpec = tween(180),
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(rowScale)
            .shadow(rowShadow, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(74.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(accent, accent.copy(alpha = 0.30f)),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(accent.copy(alpha = 0.18f), CircleShape)
                        .border(1.dp, accent.copy(alpha = 0.72f), CircleShape)
                        .padding(7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(accent, CircleShape))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LabelUsagePill(category.tasksCount)
                        Text(
                            text = normalizeHex(category.colorHex),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    text = if (selected) "Activa" else "Editar",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LabelEditorPane(
    category: TaskCategory?,
    onUpdate: (String, String, String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    if (category == null) {
        SectionCard(
            modifier = modifier.fillMaxWidth(),
            title = "Editor de etiqueta",
            subtitle = "Selecciona una etiqueta para editar nombre, color y reglas de borrado.",
            neonAccentColor = parseCategoryColor(DEFAULT_LABEL_COLOR),
        ) {
            EmptyState(
                title = "Nada seleccionado",
                message = "El editor aparecera aqui cuando elijas una etiqueta real.",
            )
        }
        return
    }

    var confirmDelete by rememberSaveable(category.id) { mutableStateOf(false) }
    var name by rememberSaveable(category.id) { mutableStateOf(category.name) }
    var colorHex by rememberSaveable(category.id) { mutableStateOf(normalizeHex(category.colorHex)) }
    val normalizedColor = normalizeHex(colorHex)
    val accent = parseCategoryColor(normalizedColor)
    val deleteBlocked = category.tasksCount > 0
    val hasChanges = name.trim() != category.name || normalizedColor != normalizeHex(category.colorHex)

    SectionCard(
        modifier = modifier.fillMaxWidth(),
        title = "Editor de etiqueta",
        subtitle = category.name,
        neonAccentColor = accent,
        actions = {
            LabelStatusPill(
                text = if (deleteBlocked) "Bloqueada" else "Libre",
                color = if (deleteBlocked) SupportDeskThemeTokens.semanticColors.warning else SupportDeskThemeTokens.semanticColors.success,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compact = maxWidth < 780.dp
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                        LabelStudioPreview(
                            name = name.takeIf { it.isNotBlank() } ?: category.name,
                            colorHex = normalizedColor,
                            tasksCount = category.tasksCount,
                        )
                        LabelEditorFields(
                            name = name,
                            colorHex = colorHex,
                            onNameChange = { name = it },
                            onColorSelect = { colorHex = normalizeHex(it) },
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                        verticalAlignment = Alignment.Top,
                    ) {
                        LabelStudioPreview(
                            name = name.takeIf { it.isNotBlank() } ?: category.name,
                            colorHex = normalizedColor,
                            tasksCount = category.tasksCount,
                            modifier = Modifier.weight(0.42f),
                        )
                        LabelEditorFields(
                            name = name,
                            colorHex = colorHex,
                            onNameChange = { name = it },
                            onColorSelect = { colorHex = normalizeHex(it) },
                            modifier = Modifier.weight(0.58f),
                        )
                    }
                }
            }

            DeleteGuardPanel(tasksCount = category.tasksCount)

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compactActions = maxWidth < 520.dp
                if (compactActions) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        PrimaryButton(
                            text = if (hasChanges) "Guardar cambios" else "Guardado",
                            onClick = { onUpdate(category.id, name.trim(), normalizedColor) },
                            enabled = name.isNotBlank() && hasChanges,
                            fullWidth = true,
                        )
                        SecondaryButton(
                            text = "Borrar etiqueta",
                            onClick = {
                                if (!deleteBlocked) confirmDelete = true
                            },
                            enabled = !deleteBlocked,
                            fullWidth = true,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        PrimaryButton(
                            text = if (hasChanges) "Guardar cambios" else "Guardado",
                            onClick = { onUpdate(category.id, name.trim(), normalizedColor) },
                            enabled = name.isNotBlank() && hasChanges,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = "Borrar etiqueta",
                            onClick = {
                                if (!deleteBlocked) confirmDelete = true
                            },
                            enabled = !deleteBlocked,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    ConfirmDialog(
        visible = confirmDelete && !deleteBlocked,
        title = "Borrar etiqueta",
        message = "Esta accion eliminara la etiqueta seleccionada. No afecta a otras etiquetas.",
        confirmText = "Borrar",
        dismissText = "Cancelar",
        onConfirm = {
            confirmDelete = false
            if (category.tasksCount == 0) {
                onDelete(category.id)
            }
        },
        onDismiss = { confirmDelete = false },
    )
}

@Composable
private fun LabelEditorFields(
    name: String,
    colorHex: String,
    onNameChange: (String) -> Unit,
    onColorSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre") },
            singleLine = true,
        )
        ColorPickerField(
            selectedColor = colorHex,
            onSelect = onColorSelect,
        )
    }
}

@Composable
private fun LabelStudioPreview(
    name: String,
    colorHex: String,
    tasksCount: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val accentTarget = parseCategoryColor(colorHex)
    val accent by animateColorAsState(
        targetValue = accentTarget,
        animationSpec = tween(260),
    )
    val previewScale by animateFloatAsState(
        targetValue = if (name.isNotBlank()) 1f else 0.985f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(previewScale)
            .shadow(3.dp, RoundedCornerShape(14.dp))
            .border(1.dp, accent.copy(alpha = 0.46f), RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accent,
                                accent.copy(alpha = 0.62f),
                                MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ),
                    )
                    .padding(spacing.md),
            ) {
                Text(
                    text = "Color preview",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.TopStart),
                )
                Text(
                    text = normalizeHex(colorHex),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.86f),
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(accent.copy(alpha = 0.18f), CircleShape)
                            .border(1.dp, accent.copy(alpha = 0.7f), CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(accent, CircleShape))
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = taskCountLabel(tasksCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ColorProfile(accent)
            }
        }
    }
}

@Composable
private fun ColorProfile(color: Color) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        ColorChannelBar(label = "R", value = color.red, color = Color(0xFFE15B64))
        ColorChannelBar(label = "G", value = color.green, color = Color(0xFF4EA96B))
        ColorChannelBar(label = "B", value = color.blue, color = Color(0xFF4E7FD8))
    }
}

@Composable
private fun ColorChannelBar(label: String, value: Float, color: Color) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = tween(280),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(12.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedValue)
                    .height(7.dp)
                    .background(color, RoundedCornerShape(100.dp)),
            )
        }
        Text(
            text = "${(animatedValue * 255f).roundToInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
    }
}

@Composable
private fun DeleteGuardPanel(tasksCount: Int) {
    val spacing = SupportDeskThemeTokens.spacing
    val blocked = tasksCount > 0
    val color = if (blocked) {
        SupportDeskThemeTokens.semanticColors.warning
    } else {
        SupportDeskThemeTokens.semanticColors.success
    }
    val backgroundColor by animateColorAsState(
        targetValue = color.copy(alpha = 0.10f),
        animationSpec = tween(220),
    )
    val borderColor by animateColorAsState(
        targetValue = color.copy(alpha = 0.38f),
        animationSpec = tween(220),
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (blocked) "Borrado bloqueado" else "Borrado disponible",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (blocked) {
                        "No se puede borrar mientras ${taskCountLabel(tasksCount)} sigan asociadas."
                    } else {
                        "Esta etiqueta no tiene tareas asociadas."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LabelUsagePill(tasksCount: Int) {
    val color = if (tasksCount > 0) {
        SupportDeskThemeTokens.semanticColors.warning
    } else {
        SupportDeskThemeTokens.semanticColors.success
    }
    LabelStatusPill(
        text = if (tasksCount > 0) taskCountLabel(tasksCount) else "Libre",
        color = color,
    )
}

@Composable
private fun LabelStatusPill(
    text: String,
    color: Color,
) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = color.copy(alpha = 0.13f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ColorPickerField(
    selectedColor: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialogVisible by rememberSaveable { mutableStateOf(false) }
    val spacing = SupportDeskThemeTokens.spacing
    val accentTarget = parseCategoryColor(selectedColor)
    val accent by animateColorAsState(
        targetValue = accentTarget,
        animationSpec = tween(260),
    )
    val swatchSize by animateDpAsState(
        targetValue = if (dialogVisible) 48.dp else 42.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.34f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { dialogVisible = true },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(accent.copy(alpha = 0.16f), CircleShape)
                    .border(1.dp, accent.copy(alpha = 0.60f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(swatchSize).background(accent, CircleShape))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = normalizeHex(selectedColor),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SecondaryButton(
                text = "Elegir",
                onClick = { dialogVisible = true },
            )
        }
    }

    if (dialogVisible) {
        ColorPickerDialog(
            initialColor = selectedColor,
            onDismiss = { dialogVisible = false },
            onConfirm = {
                onSelect(it)
                dialogVisible = false
            },
        )
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var workingColor by rememberSaveable(initialColor) { mutableStateOf(normalizeHex(initialColor)) }
    val spacing = SupportDeskThemeTokens.spacing

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Text(
                    text = "Selector de color",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Ajusta el HEX o elige una muestra de la paleta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LabelStudioPreview(
                    name = "Preview",
                    colorHex = workingColor,
                    tasksCount = 0,
                )
                OutlinedTextField(
                    value = workingColor,
                    onValueChange = { workingColor = sanitizeHexInput(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Color HEX") },
                    singleLine = true,
                )
                ColorPaletteGrid(
                    presetColors = LABEL_PRESET_COLORS,
                    selectedColor = normalizeHex(workingColor),
                    onSelect = { workingColor = normalizeHex(it) },
                )
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val compact = maxWidth < 420.dp
                    if (compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            SecondaryButton(
                                text = "Cancelar",
                                onClick = onDismiss,
                                fullWidth = true,
                            )
                            PrimaryButton(
                                text = "Usar color",
                                onClick = { onConfirm(normalizeHex(workingColor)) },
                                fullWidth = true,
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.md),
                        ) {
                            SecondaryButton(
                                text = "Cancelar",
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                            )
                            PrimaryButton(
                                text = "Usar color",
                                onClick = { onConfirm(normalizeHex(workingColor)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPaletteGrid(
    presetColors: List<String>,
    selectedColor: String,
    onSelect: (String) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = "Paleta",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            presetColors.forEach { colorHex ->
                ColorOption(
                    colorHex = colorHex,
                    selected = normalizeHex(selectedColor) == normalizeHex(colorHex),
                    onClick = { onSelect(colorHex) },
                )
            }
        }
    }
}

@Composable
private fun ColorOption(
    colorHex: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = parseCategoryColor(colorHex)
    val borderColor by animateColorAsState(
        targetValue = if (selected) color.copy(alpha = 0.92f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
        animationSpec = tween(160),
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        animationSpec = tween(160),
    )
    val swatchScale by animateFloatAsState(
        targetValue = if (selected) 1.10f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
    )
    val swatchShadow by animateDpAsState(
        targetValue = if (selected) 6.dp else 1.dp,
        animationSpec = tween(160),
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(swatchShadow, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .scale(swatchScale)
                .background(color, CircleShape),
        )
    }
}

private fun parseCategoryColor(hex: String): Color = runCatching {
    val value = normalizeHex(hex).removePrefix("#").toLong(16)
    val red = ((value shr 16) and 0xFF).toInt() / 255f
    val green = ((value shr 8) and 0xFF).toInt() / 255f
    val blue = (value and 0xFF).toInt() / 255f
    Color(red = red, green = green, blue = blue, alpha = 1f)
}.getOrElse {
    Color(0xFF6B7A5B)
}

private fun normalizeHex(raw: String): String {
    val cleaned = raw
        .trim()
        .removePrefix("#")
        .filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        .take(6)
        .uppercase()
    val expanded = if (cleaned.length == 3) {
        cleaned.map { "$it$it" }.joinToString("")
    } else {
        cleaned
    }
    return "#${expanded.padEnd(6, '0').take(6)}"
}

private fun sanitizeHexInput(raw: String): String {
    val cleaned = raw
        .trim()
        .removePrefix("#")
        .filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        .take(6)
        .uppercase()
    return "#$cleaned"
}

private fun taskCountLabel(count: Int): String =
    if (count == 1) "1 tarea" else "$count tareas"

private const val DEFAULT_LABEL_COLOR = "#6B7A5B"

private val LABEL_PRESET_COLORS = listOf(
    "#6B7A5B",
    "#A67C52",
    "#7D4E57",
    "#355C5B",
    "#8C6A43",
    "#556B2F",
    "#B56576",
    "#6D597A",
    "#457B9D",
    "#2A9D8F",
    "#E07A5F",
    "#8D6E63",
)
