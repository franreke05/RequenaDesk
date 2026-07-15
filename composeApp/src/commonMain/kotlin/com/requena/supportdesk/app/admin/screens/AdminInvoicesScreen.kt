package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.core.utils.toFixedString
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.layout.InfoRow
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceItemInput
import com.requena.supportdesk.features.invoices.domain.model.InvoicePdfFile
import com.requena.supportdesk.features.invoices.domain.model.roundedInvoiceHours
import com.requena.supportdesk.features.invoices.presentation.event.InvoicesUiEvent
import com.requena.supportdesk.features.invoices.presentation.state.InvoicesUiState
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState

@Composable
fun AdminInvoicesScreen(
    clients: List<Client>,
    tasksState: TasksUiState,
    state: InvoicesUiState,
    onEvent: (InvoicesUiEvent) -> Unit,
    preselectedClientId: String? = null,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var showInvoiceForm by remember(preselectedClientId) { mutableStateOf(preselectedClientId != null) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        InvoiceLibrary(
            state = state,
            onCreateInvoice = { showInvoiceForm = true },
            onRefresh = { onEvent(InvoicesUiEvent.RefreshSavedInvoices) },
            onOpenInvoice = { onEvent(InvoicesUiEvent.OpenSavedInvoice(it.fileName)) },
        )

        if (showInvoiceForm) {
            InvoiceForm(
                clients = clients,
                tasksState = tasksState,
                state = state,
                preselectedClientId = preselectedClientId,
                onCancel = { showInvoiceForm = false },
                onGenerate = { input -> onEvent(InvoicesUiEvent.GenerateInvoice(input)) },
            )
        }
    }
}

@Composable
private fun InvoiceLibrary(
    state: InvoicesUiState,
    onCreateInvoice: () -> Unit,
    onRefresh: () -> Unit,
    onOpenInvoice: (InvoicePdfFile) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(
        title = "Facturas guardadas",
        subtitle = "PDFs en Escritorio > Facturas OryKai",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                PrimaryButton(
                    text = "Nueva factura",
                    onClick = onCreateInvoice,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Actualizar",
                    onClick = onRefresh,
                    isLoading = state.isLoadingSavedInvoices,
                )
            }

            when {
                state.isLoadingSavedInvoices && state.savedInvoices.isEmpty() -> {
                    Text("Cargando facturas guardadas…", style = MaterialTheme.typography.bodyMedium)
                }

                state.savedInvoices.isEmpty() -> {
                    Text(
                        "Todavia no hay PDFs guardados. Crea una factura para añadirla a esta biblioteca.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> state.savedInvoices.forEach { invoice ->
                    SavedInvoiceRow(invoice = invoice, onOpen = { onOpenInvoice(invoice) })
                }
            }
            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SavedInvoiceRow(
    invoice: InvoicePdfFile,
    onOpen: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(invoice.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = formatFileSize(invoice.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SecondaryButton(text = "Abrir PDF", onClick = onOpen)
    }
}

@Composable
private fun InvoiceForm(
    clients: List<Client>,
    tasksState: TasksUiState,
    state: InvoicesUiState,
    preselectedClientId: String?,
    onCancel: () -> Unit,
    onGenerate: (CreateInvoiceInput) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var selectedClientId by remember(preselectedClientId) { mutableStateOf(preselectedClientId.orEmpty()) }
    val selectedTaskIds = remember { mutableStateListOf<String>() }
    var issuedAt by remember { mutableStateOf(currentIsoDate()) }
    var dueAt by remember { mutableStateOf("") }
    var taxPercent by remember { mutableStateOf("0") }
    var hourlyRate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val selectedClient = clients.firstOrNull { it.id == selectedClientId }
    val clientTasks = tasksState.tasks.filter { it.clientId == selectedClientId }
    val selectedTasks = clientTasks.filter { it.id in selectedTaskIds }
    val availableTasks = clientTasks.filter { it.id !in selectedTaskIds }
    val tasksWithRecordedHours = availableTasks.filter { task ->
        roundedInvoiceHours(tasksState.trackedSecondsFor(task)) > 0
    }
    val rate = hourlyRate.toDoubleOrNull() ?: 0.0
    val subtotal = selectedTasks.sumOf { task ->
        roundedInvoiceHours(tasksState.trackedSecondsFor(task)) * rate
    }
    val taxRate = taxPercent.toDoubleOrNull() ?: 0.0
    val tax = subtotal * (taxRate / 100.0)
    val total = subtotal + tax
    val isValid = selectedClient != null && selectedTasks.isNotEmpty() &&
        selectedTasks.all { task -> roundedInvoiceHours(tasksState.trackedSecondsFor(task)) > 0 } &&
        issuedAt.isNotBlank() && rate.isFinite() && rate > 0.0 &&
        taxRate.isFinite() && taxRate in 0.0..100.0

    SectionCard(
        title = "Nueva factura",
        subtitle = "Selecciona una o varias tareas y sus horas registradas",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            InvoiceSelector(
                label = "Cliente",
                selectedLabel = when {
                    selectedClient != null -> selectedClient.companyName
                    clients.isEmpty() -> "No hay clientes disponibles"
                    else -> "Seleccionar cliente"
                },
                options = clients.map { InvoiceOption(it.id, it.companyName) },
                enabled = clients.isNotEmpty(),
                onSelect = { clientId ->
                    selectedClientId = clientId
                    selectedTaskIds.clear()
                },
            )

            InvoiceSelector(
                label = "Agregar tarea",
                selectedLabel = taskSelectorLabel(selectedClient, clientTasks, availableTasks),
                options = availableTasks.map { InvoiceOption(it.id, it.title) },
                enabled = selectedClient != null && availableTasks.isNotEmpty(),
                onSelect = { taskId -> selectedTaskIds.add(taskId) },
            )

            if (selectedClient != null && availableTasks.isNotEmpty()) {
                SecondaryButton(
                    text = "Agregar todas las tareas con horas",
                    onClick = {
                        tasksWithRecordedHours.forEach { task -> selectedTaskIds.add(task.id) }
                    },
                    enabled = tasksWithRecordedHours.isNotEmpty(),
                    fullWidth = true,
                )
            }

            if (selectedTasks.isNotEmpty()) {
                Text("Tareas incluidas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                selectedTasks.forEach { task ->
                    SelectedInvoiceTaskRow(
                        task = task,
                        hours = roundedInvoiceHours(tasksState.trackedSecondsFor(task)),
                        onRemove = { selectedTaskIds.remove(task.id) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = issuedAt,
                    onValueChange = { issuedAt = it },
                    label = { Text("Emision (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = dueAt,
                    onValueChange = { dueAt = it },
                    label = { Text("Vencimiento (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = hourlyRate,
                    onValueChange = { hourlyRate = it },
                    label = { Text("Precio por hora") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = taxPercent,
                    onValueChange = { taxPercent = it },
                    label = { Text("Impuesto (%)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            InvoiceSummary(subtotal = subtotal, taxPercent = taxRate, tax = tax, total = total)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                SecondaryButton(text = "Cancelar", onClick = onCancel, modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = "Generar y guardar PDF",
                    enabled = isValid,
                    isLoading = state.isGenerating,
                    onClick = {
                        onGenerate(
                            CreateInvoiceInput(
                                clientId = selectedClientId,
                                clientName = checkNotNull(selectedClient).companyName,
                                issuedAt = issuedAt,
                                dueAt = dueAt.ifBlank { null },
                                notes = notes.ifBlank { null },
                                taxPercent = taxRate,
                                items = selectedTasks.mapIndexed { index, task ->
                                    val hours = roundedInvoiceHours(tasksState.trackedSecondsFor(task))
                                    CreateInvoiceItemInput(
                                        description = task.title,
                                        quantity = hours.toDouble(),
                                        unitPrice = rate,
                                        sortOrder = index,
                                    )
                                },
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SelectedInvoiceTaskRow(
    task: WorkTask,
    hours: Int,
    onRemove: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "$hours h registradas",
                style = MaterialTheme.typography.bodySmall,
                color = if (hours > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
        SecondaryButton(text = "Quitar", onClick = onRemove)
    }
}

@Composable
private fun InvoiceSummary(
    subtotal: Double,
    taxPercent: Double,
    tax: Double,
    total: Double,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(title = "Resumen") {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            InfoRow(label = "Subtotal", value = "$${subtotal.toFixedString(2)}")
            InfoRow(label = "Impuesto (${taxPercent.toFixedString(2)}%)", value = "$${tax.toFixedString(2)}")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "$${total.toFixedString(2)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun InvoiceSelector(
    label: String,
    selectedLabel: String,
    options: List<InvoiceOption>,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            SecondaryButton(
                text = selectedLabel,
                onClick = { expanded = true },
                enabled = enabled,
                fullWidth = true,
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSelect(option.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private data class InvoiceOption(
    val id: String,
    val label: String,
)

private fun taskSelectorLabel(
    selectedClient: Client?,
    clientTasks: List<WorkTask>,
    availableTasks: List<WorkTask>,
): String = when {
    selectedClient == null -> "Selecciona primero un cliente"
    clientTasks.isEmpty() -> "Este cliente no tiene tareas"
    availableTasks.isEmpty() -> "Todas las tareas estan incluidas"
    else -> "Seleccionar tarea"
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    else -> "${bytes / 1_024} KB"
}
