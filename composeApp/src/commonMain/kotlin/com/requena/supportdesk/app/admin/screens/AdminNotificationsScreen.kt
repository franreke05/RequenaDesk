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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.ConfirmDialog
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState

@Composable
fun AdminNotificationsScreen(
    tasksState: TasksUiState,
    onTasksEvent: (TasksUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        PageHeader(
            title = "Etiquetas",
            subtitle = "Gestiona colores y nombres para ordenar tareas y filtros operativos.",
            eyebrow = "Organizacion",
        )

        LabelCreateCard(
            onCreate = { name, colorHex ->
                onTasksEvent(TasksUiEvent.CreateCategory(name, colorHex))
            },
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val stacked = maxWidth < 1080.dp
            if (stacked) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    LabelListPane(
                        categories = tasksState.categories,
                        selectedCategoryId = tasksState.selectedCategoryId,
                        isLoading = tasksState.isLoading,
                        onSelect = { onTasksEvent(TasksUiEvent.SelectCategory(it)) },
                        modifier = Modifier.weight(0.46f),
                    )
                    LabelEditorPane(
                        category = tasksState.selectedCategory,
                        onUpdate = { labelId, name, colorHex ->
                            onTasksEvent(TasksUiEvent.UpdateLabel(labelId, name, colorHex))
                        },
                        onDelete = { onTasksEvent(TasksUiEvent.DeleteLabel(it)) },
                        modifier = Modifier.weight(0.54f),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    LabelListPane(
                        categories = tasksState.categories,
                        selectedCategoryId = tasksState.selectedCategoryId,
                        isLoading = tasksState.isLoading,
                        onSelect = { onTasksEvent(TasksUiEvent.SelectCategory(it)) },
                        modifier = Modifier.weight(0.46f),
                    )
                    LabelEditorPane(
                        category = tasksState.selectedCategory,
                        onUpdate = { labelId, name, colorHex ->
                            onTasksEvent(TasksUiEvent.UpdateLabel(labelId, name, colorHex))
                        },
                        onDelete = { onTasksEvent(TasksUiEvent.DeleteLabel(it)) },
                        modifier = Modifier.weight(0.54f),
                    )
                }
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
    var selectedColor by rememberSaveable { mutableStateOf("#6B7A5B") }
    val presetColors = remember { listOf("#6B7A5B", "#A67C52", "#7D4E57", "#355C5B", "#8C6A43") }

    SectionCard(
        modifier = modifier,
        title = "Nueva etiqueta",
        subtitle = "Cada etiqueta crea una agrupacion visual clara para el trabajo diario.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre de la etiqueta") },
                singleLine = true,
            )
            OutlinedTextField(
                value = selectedColor,
                onValueChange = { selectedColor = normalizeHex(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Color HEX") },
                singleLine = true,
            )
            ColorPaletteRow(
                presetColors = presetColors,
                selectedColor = selectedColor,
                onSelect = { selectedColor = it },
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
    }
}

@Composable
private fun LabelListPane(
    categories: List<TaskCategory>,
    selectedCategoryId: String?,
    isLoading: Boolean,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(
        modifier = modifier.fillMaxSize(),
        title = "Etiquetas activas",
        subtitle = "Selecciona una para editar nombre y color.",
    ) {
        if (isLoading && categories.isEmpty()) {
            LoadingState(itemCount = 4)
        } else if (categories.isEmpty()) {
            EmptyState(
                title = "Sin etiquetas",
                message = "Crea la primera etiqueta para empezar a organizar tareas.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                items(categories, key = { it.id }) { category ->
                    val selected = category.id == selectedCategoryId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(category.id) },
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                        },
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(parseCategoryColor(category.colorHex), MaterialTheme.shapes.small)
                                    .padding(12.dp),
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "${category.tasksCount} tareas asociadas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = category.colorHex,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
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
            modifier = modifier.fillMaxSize(),
            title = "Editor de etiqueta",
            subtitle = "Selecciona una etiqueta para editarla.",
        ) {
            EmptyState(
                title = "Nada seleccionado",
                message = "La ficha lateral aparecera aqui cuando elijas una etiqueta.",
            )
        }
        return
    }

    var confirmDelete by rememberSaveable(category.id) { mutableStateOf(false) }
    var name by remember(category.id) { mutableStateOf(category.name) }
    var colorHex by remember(category.id) { mutableStateOf(category.colorHex) }
    val presetColors = remember { listOf("#6B7A5B", "#A67C52", "#7D4E57", "#355C5B", "#8C6A43") }

    SectionCard(
        modifier = modifier.fillMaxSize(),
        title = category.name,
        subtitle = "Actualiza nombre y color sin perder las tareas enlazadas.",
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre") },
                singleLine = true,
            )
            OutlinedTextField(
                value = colorHex,
                onValueChange = { colorHex = normalizeHex(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Color HEX") },
                singleLine = true,
            )
            ColorPaletteRow(
                presetColors = presetColors,
                selectedColor = colorHex,
                onSelect = { colorHex = it },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .background(parseCategoryColor(colorHex), MaterialTheme.shapes.small)
                        .padding(18.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${category.tasksCount} tareas usan esta etiqueta",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = if (category.tasksCount > 0) {
                            "No se puede borrar mientras tenga tareas asociadas."
                        } else {
                            "Se puede borrar si ya no esta en uso."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                PrimaryButton(
                    text = "Guardar",
                    onClick = { onUpdate(category.id, name.trim(), normalizeHex(colorHex)) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Borrar",
                    onClick = { confirmDelete = true },
                    enabled = category.tasksCount == 0,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    ConfirmDialog(
        visible = confirmDelete,
        title = "Borrar etiqueta",
        message = "Esta accion eliminara la etiqueta seleccionada.",
        confirmText = "Borrar",
        dismissText = "Cancelar",
        onConfirm = {
            confirmDelete = false
            onDelete(category.id)
        },
        onDismiss = { confirmDelete = false },
    )
}

@Composable
private fun ColorPaletteRow(
    presetColors: List<String>,
    selectedColor: String,
    onSelect: (String) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = "Color",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            presetColors.forEach { colorHex ->
                ColorOption(
                    colorHex = colorHex,
                    selected = selectedColor == colorHex,
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
    SecondaryColorCard(
        color = parseCategoryColor(colorHex),
        border = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
        onClick = onClick,
    )
}

@Composable
private fun SecondaryColorCard(
    color: Color,
    border: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(border, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .background(color, MaterialTheme.shapes.small)
                .padding(14.dp),
        )
    }
}

private fun parseCategoryColor(hex: String): Color = runCatching {
    val value = hex.removePrefix("#").toLong(16)
    val red = ((value shr 16) and 0xFF).toInt() / 255f
    val green = ((value shr 8) and 0xFF).toInt() / 255f
    val blue = (value and 0xFF).toInt() / 255f
    Color(red = red, green = green, blue = blue, alpha = 1f)
}.getOrElse {
    Color(0xFF6B7A5B)
}

private fun normalizeHex(raw: String): String {
    val trimmed = raw.trim().removePrefix("#")
    val normalized = trimmed.take(6).padStart(6, '0').uppercase()
    return "#$normalized"
}
