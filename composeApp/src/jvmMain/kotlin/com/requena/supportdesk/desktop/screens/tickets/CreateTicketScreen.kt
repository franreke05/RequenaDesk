package com.requena.supportdesk.desktop.screens.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.requena.supportdesk.core.model.Attachment
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.designsystem.components.badges.SupportPlatformBadge
import com.requena.supportdesk.designsystem.components.badges.TicketCategoryBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.feedback.ConfirmDialog
import com.requena.supportdesk.designsystem.components.layout.FormSection
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.tickets.AttachmentChip
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput

@Composable
fun CreateTicketScreen(
    affectedApp: String,
    onBack: () -> Unit,
    onCreateTicket: (CreateTicketInput) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var subject by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var appVersion by rememberSaveable { mutableStateOf("") }
    var clientReference by rememberSaveable { mutableStateOf("") }
    var stepsToReproduce by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(TicketCategory.BUG) }
    var selectedPlatform by rememberSaveable { mutableStateOf(SupportPlatform.DESKTOP) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val attachmentPlaceholders = listOf(
        Attachment(
            id = "local-brief",
            fileName = "brief.pdf",
            contentType = "application/pdf",
            sizeBytes = 48213,
            uploadedBy = "Local draft",
            uploadedAt = "Draft",
        ),
        Attachment(
            id = "local-screenshot",
            fileName = "screenshot.png",
            contentType = "image/png",
            sizeBytes = 128032,
            uploadedBy = "Local draft",
            uploadedAt = "Draft",
        ),
    )

    val categoryOptions = TicketCategory.entries.map { FilterOption(value = it, label = it.displayName()) }
    val platformOptions = SupportPlatform.entries.map { FilterOption(value = it, label = it.displayName()) }
    val isValid = subject.isNotBlank() && description.isNotBlank()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        PageHeader(
            title = "Crear ticket",
            subtitle = "Captura el problema rápido, añade el contexto técnico mínimo y deja que el triaje ocurra sin idas y vueltas extra.",
            eyebrow = "Flujo del cliente",
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    SecondaryButton(
                        text = "Volver",
                        onClick = {
                            if (subject.isBlank() && description.isBlank() && appVersion.isBlank() && clientReference.isBlank() && stepsToReproduce.isBlank()) {
                                onBack()
                            } else {
                                showDiscardDialog = true
                            }
                        },
                    )
                    PrimaryButton(
                        text = "Crear ticket",
                        onClick = {
                            showErrors = true
                            if (isValid) {
                                onCreateTicket(
                                    CreateTicketInput(
                                        subject = subject,
                                        description = description,
                                        category = selectedCategory,
                                        affectedApp = affectedApp,
                                        platform = selectedPlatform,
                                        appVersion = appVersion,
                                        stepsToReproduce = stepsToReproduce,
                                        clientReference = clientReference,
                                    ),
                                )
                            }
                        },
                    )
                }
            },
        )

        FormSection(
            title = "Resumen del problema",
            subtitle = "Mantén el primer bloque breve. Asunto, categoría y contexto del producto deben ser suficientes para enrutar el ticket correctamente.",
        ) {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Asunto") },
                placeholder = { Text("El instalador de escritorio falla tras la última actualización") },
                isError = showErrors && subject.isBlank(),
                singleLine = true,
            )
            OutlinedTextField(
                value = affectedApp,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("App afectada") },
                enabled = false,
                supportingText = { Text("Asignada desde la cuenta del cliente para mantener el formulario enfocado.") },
                singleLine = true,
            )
            Text(
                text = "Categoría",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilterBar(
                label = "Categoría",
                options = categoryOptions,
                selected = selectedCategory,
                onSelected = { selectedCategory = it ?: TicketCategory.BUG },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                TicketCategoryBadge(category = selectedCategory)
                SupportPlatformBadge(platform = selectedPlatform)
            }
        }

        FormSection(
            title = "Contexto técnico",
            subtitle = "Solo los detalles que ayudan a depurar rápido: plataforma, versión, pasos para reproducir y una referencia si el cliente la tiene.",
        ) {
            Text(
                text = "Plataforma",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilterBar(
                label = "Plataforma",
                options = platformOptions,
                selected = selectedPlatform,
                onSelected = { selectedPlatform = it ?: SupportPlatform.DESKTOP },
            )
            OutlinedTextField(
                value = appVersion,
                onValueChange = { appVersion = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Versión de la app") },
                placeholder = { Text("1.8.2 (418)") },
                singleLine = true,
            )
            OutlinedTextField(
                value = clientReference,
                onValueChange = { clientReference = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Referencia del cliente") },
                placeholder = { Text("Referencia opcional de compilación, factura o sprint") },
                singleLine = true,
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descripción") },
                placeholder = { Text("Describe qué ocurrió, qué cambió y el impacto en la app.") },
                isError = showErrors && description.isBlank(),
                minLines = 5,
            )
            OutlinedTextField(
                value = stepsToReproduce,
                onValueChange = { stepsToReproduce = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Pasos para reproducir") },
                placeholder = { Text("Abre la app, inicia sesión, y el fallo aparece al pulsar Sincronizar.") },
                minLines = 4,
            )
            if (showErrors && !isValid) {
                Text(
                    text = "El asunto y la descripción son obligatorios antes de enviar el ticket.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = "La prioridad se asigna durante el triaje para mantener el formulario del cliente breve y consistente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FormSection(
            title = "Adjuntos",
            subtitle = "El área de carga es solo un marcador de posición, pero capturas y logs ya tienen un lugar claro en el flujo.",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                attachmentPlaceholders.forEach { attachment ->
                    AttachmentChip(attachment = attachment)
                }
            }
            Text(
                text = "La conexión final de carga enlazará esta área con el endpoint de adjuntos más adelante.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PrimaryButton(
            text = "Crear ticket",
            onClick = {
                showErrors = true
                if (isValid) {
                    onCreateTicket(
                        CreateTicketInput(
                            subject = subject,
                            description = description,
                            category = selectedCategory,
                            affectedApp = affectedApp,
                            platform = selectedPlatform,
                            appVersion = appVersion,
                            stepsToReproduce = stepsToReproduce,
                            clientReference = clientReference,
                        ),
                    )
                }
            },
            fullWidth = true,
        )
    }

    ConfirmDialog(
        visible = showDiscardDialog,
        title = "¿Descartar este borrador?",
        message = "Se perderán el asunto, la descripción y el contexto técnico actuales.",
        confirmText = "Descartar borrador",
        onConfirm = {
            showDiscardDialog = false
            onBack()
        },
        onDismiss = { showDiscardDialog = false },
    )
}
