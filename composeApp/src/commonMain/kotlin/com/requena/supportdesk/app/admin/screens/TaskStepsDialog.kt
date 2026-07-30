package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.tasks.domain.model.MAX_DERIVED_TASK_STEPS
import com.requena.supportdesk.features.tasks.domain.model.derivedTaskStepTitle
import com.requena.supportdesk.features.tasks.domain.model.parseDerivedTaskSteps

@Composable
internal fun TaskStepsDialog(
    task: WorkTask?,
    client: Client?,
    category: TaskCategory?,
    isLoading: Boolean,
    errorMessage: String?,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (task == null) return

    var rawSteps by rememberSaveable(task.id) { mutableStateOf("") }
    val steps = remember(rawSteps) { parseDerivedTaskSteps(rawSteps) }
    val enteredLineCount = remember(rawSteps) {
        rawSteps.lineSequence().count { it.isNotBlank() }
    }
    val preview = remember(task.title, steps) {
        steps.take(3).map { derivedTaskStepTitle(task.title, it) }
    }
    val spacing = SupportDeskThemeTokens.spacing

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Crear pasos como pósits",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Text(
                    text = "Divide «${task.title}» escribiendo un paso por línea. Cada paso será una tarea real y aparecerá en Pendientes de planificar.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        Text("Datos heredados", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Cliente: ${client?.companyName ?: "Sin cliente"}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Etiqueta: ${category?.name ?: "Sin etiqueta"}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Vencimiento: ${task.dueDate ?: "Sin fecha"}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                OutlinedTextField(
                    value = rawSteps,
                    onValueChange = { rawSteps = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pasos, uno por línea") },
                    placeholder = {
                        Text("Diseñar base de datos\nCrear login\nProgramar dashboard")
                    },
                    supportingText = {
                        Text(
                            when {
                                enteredLineCount > MAX_DERIVED_TASK_STEPS ->
                                    "Se crearán los primeros $MAX_DERIVED_TASK_STEPS pasos."
                                steps.isEmpty() -> "Escribe al menos un paso."
                                else -> "${steps.size}/$MAX_DERIVED_TASK_STEPS pasos listos."
                            },
                        )
                    },
                    minLines = 5,
                    maxLines = 10,
                    enabled = !isLoading,
                )
                if (preview.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text("Vista previa", style = MaterialTheme.typography.labelLarge)
                        preview.forEach { title ->
                            Text(
                                text = "• $title",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (steps.size > preview.size) {
                            Text(
                                text = "… y ${steps.size - preview.size} más",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                errorMessage?.takeIf(String::isNotBlank)?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = when (steps.size) {
                    1 -> "Crear 1 paso"
                    else -> "Crear ${steps.size} pasos"
                },
                onClick = { onCreate(rawSteps) },
                enabled = steps.isNotEmpty() && !task.completed && !isLoading,
                isLoading = isLoading,
            )
        },
        dismissButton = {
            SecondaryButton(
                text = "Cancelar",
                onClick = onDismiss,
                enabled = !isLoading,
            )
        },
    )
}
