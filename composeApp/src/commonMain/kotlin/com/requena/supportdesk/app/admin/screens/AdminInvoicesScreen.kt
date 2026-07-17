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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.requena.supportdesk.designsystem.components.feedback.ConfirmDialog
import com.requena.supportdesk.designsystem.components.layout.InfoRow
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceItemInput
import com.requena.supportdesk.features.invoices.domain.model.InvoiceItemKind
import com.requena.supportdesk.features.invoices.domain.model.InvoicePdfFile
import com.requena.supportdesk.features.invoices.domain.model.InvoiceTotals
import com.requena.supportdesk.features.invoices.domain.model.calculateInvoiceTotals
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
            onDeleteInvoice = { onEvent(InvoicesUiEvent.DeleteSavedInvoice(it.fileName)) },
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
    onDeleteInvoice: (InvoicePdfFile) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var invoicePendingDeletion by rememberSaveable { mutableStateOf<String?>(null) }
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
                        "Todavía no hay PDFs guardados. Crea una factura para añadirla a esta biblioteca.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> state.savedInvoices.forEach { invoice ->
                    SavedInvoiceRow(
                        invoice = invoice,
                        isDeleting = state.deletingInvoiceFileName == invoice.fileName,
                        canDelete = state.deletingInvoiceFileName == null,
                        onOpen = { onOpenInvoice(invoice) },
                        onDelete = { invoicePendingDeletion = invoice.fileName },
                    )
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
    invoicePendingDeletion?.let { fileName ->
        ConfirmDialog(
            visible = true,
            title = "Borrar factura",
            message = "Se borrará el PDF local «$fileName». Esta acción no se puede deshacer.",
            confirmText = "Borrar",
            dismissText = "Cancelar",
            onConfirm = {
                invoicePendingDeletion = null
                state.savedInvoices.firstOrNull { it.fileName == fileName }?.let(onDeleteInvoice)
            },
            onDismiss = { invoicePendingDeletion = null },
        )
    }
}

@Composable
private fun SavedInvoiceRow(
    invoice: InvoicePdfFile,
    isDeleting: Boolean,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isDeleting, onClick = onOpen).padding(vertical = spacing.xs),
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
        SecondaryButton(text = "Abrir PDF", onClick = onOpen, enabled = !isDeleting)
        SecondaryButton(
            text = "Borrar",
            onClick = onDelete,
            enabled = !isDeleting && canDelete,
            isLoading = isDeleting,
        )
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
    val activities = remember { mutableStateListOf<InvoiceActivityDraft>() }
    var nextActivityId by remember { mutableStateOf(0) }
    var issuedAt by remember { mutableStateOf(currentIsoDate()) }
    var dueAt by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
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
    val parsedHourlyRate = hourlyRate.toInvoiceDecimalOrNull()
    val rate = parsedHourlyRate ?: 0.0
    val parsedTaxRate = taxPercent.toInvoiceDecimalOrNull()
    val taxRate = parsedTaxRate ?: 0.0
    val taskItems = selectedTasks.mapIndexed { index, task ->
        task.toInvoiceItem(
            hours = roundedInvoiceHours(tasksState.trackedSecondsFor(task)),
            hourlyRate = rate,
            sortOrder = index,
        )
    }
    val activityItems = activities.mapIndexedNotNull { index, draft ->
        draft.toInvoiceItem(sortOrder = selectedTasks.size + index)
    }
    val invoiceItems = taskItems + activityItems
    val totals = calculateInvoiceTotals(invoiceItems, taxRate)
    val activitiesAreValid = activities.all(InvoiceActivityDraft::isValid)
    val hasBillableTasks = selectedTasks.isNotEmpty()
    val isValid = selectedClient != null && invoiceItems.isNotEmpty() && activitiesAreValid &&
        selectedTasks.all { task -> roundedInvoiceHours(tasksState.trackedSecondsFor(task)) > 0 } &&
        issuedAt.isNotBlank() && parsedTaxRate != null && taxRate.isFinite() && taxRate in 0.0..100.0 &&
        (!hasBillableTasks || parsedHourlyRate != null && rate.isFinite() && rate > 0.0)

    SectionCard(
        title = "Nueva factura",
        subtitle = "Combina tareas por horas y actividades por cantidad en un mismo PDF.",
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
                    val hours = roundedInvoiceHours(tasksState.trackedSecondsFor(task))
                    SelectedInvoiceTaskRow(
                        task = task,
                        hours = hours,
                        hourlyRate = rate,
                        onRemove = { selectedTaskIds.remove(task.id) },
                    )
                }
            }

            SectionCard(
                title = "Actividades manuales",
                subtitle = "Añade servicios, materiales u otros conceptos con su cantidad y precio unitario.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    if (activities.isEmpty()) {
                        Text(
                            "No has añadido actividades. Las tareas siguen facturándose por horas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    activities.forEachIndexed { index, draft ->
                        InvoiceActivityRow(
                            index = index,
                            draft = draft,
                            onChange = { updated ->
                                val activityIndex = activities.indexOfFirst { it.id == updated.id }
                                if (activityIndex >= 0) activities[activityIndex] = updated
                            },
                            onRemove = { activities.remove(draft) },
                        )
                    }
                    SecondaryButton(
                        text = "Añadir actividad",
                        onClick = {
                            activities += InvoiceActivityDraft(id = nextActivityId)
                            nextActivityId += 1
                        },
                        fullWidth = true,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = issuedAt,
                    onValueChange = { issuedAt = it },
                    label = { Text("Emisión (YYYY-MM-DD)") },
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

            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text("Referencia (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            if (hasBillableTasks) {
                OutlinedTextField(
                    value = hourlyRate,
                    onValueChange = { hourlyRate = it },
                    label = { Text("Precio por hora") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            OutlinedTextField(
                value = taxPercent,
                onValueChange = { taxPercent = it },
                label = { Text("IVA (%)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            InvoiceSummary(totals = totals, taxPercent = taxRate)

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
                                items = invoiceItems,
                                reference = reference.ifBlank { null },
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
private fun InvoiceActivityRow(
    index: Int,
    draft: InvoiceActivityDraft,
    onChange: (InvoiceActivityDraft) -> Unit,
    onRemove: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(title = "Actividad ${index + 1}") {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            OutlinedTextField(
                value = draft.description,
                onValueChange = { onChange(draft.copy(description = it)) },
                label = { Text("Concepto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.detail,
                onValueChange = { onChange(draft.copy(detail = it)) },
                label = { Text("Detalle (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = draft.quantity,
                    onValueChange = { onChange(draft.copy(quantity = it)) },
                    label = { Text("Cantidad") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = draft.unitPrice,
                    onValueChange = { onChange(draft.copy(unitPrice = it)) },
                    label = { Text("Precio unitario") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            if (!draft.isValid) {
                Text(
                    "Indica un concepto, una cantidad y un precio unitario mayores que cero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            SecondaryButton(text = "Quitar actividad", onClick = onRemove, fullWidth = true)
        }
    }
}

@Composable
private fun SelectedInvoiceTaskRow(
    task: WorkTask,
    hours: Int,
    hourlyRate: Double,
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
                text = "$hours h × ${formatEuro(hourlyRate)} = ${formatEuro(hours * hourlyRate)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (hours > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
        SecondaryButton(text = "Quitar", onClick = onRemove)
    }
}

@Composable
private fun InvoiceSummary(
    totals: InvoiceTotals,
    taxPercent: Double,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(title = "Resumen") {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            InfoRow(label = "Subtotal", value = formatEuro(totals.subtotal))
            InfoRow(label = "IVA (${taxPercent.toFixedString(2)}%)", value = formatEuro(totals.tax))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    formatEuro(totals.total),
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

private data class InvoiceActivityDraft(
    val id: Int,
    val description: String = "",
    val detail: String = "",
    val quantity: String = "1",
    val unitPrice: String = "",
) {
    private val parsedQuantity: Double?
        get() = quantity.toInvoiceDecimalOrNull()

    private val parsedUnitPrice: Double?
        get() = unitPrice.toInvoiceDecimalOrNull()

    val isValid: Boolean
        get() {
            val quantityValue = parsedQuantity
            val unitPriceValue = parsedUnitPrice
            return description.isNotBlank() && quantityValue != null && quantityValue.isFinite() && quantityValue > 0.0 &&
                unitPriceValue != null && unitPriceValue.isFinite() && unitPriceValue > 0.0
        }

    fun toInvoiceItem(sortOrder: Int): CreateInvoiceItemInput? {
        if (!isValid) return null
        return CreateInvoiceItemInput(
            description = description.trim(),
            detail = detail.trim().ifBlank { null },
            quantity = checkNotNull(parsedQuantity),
            unitPrice = checkNotNull(parsedUnitPrice),
            sortOrder = sortOrder,
            kind = InvoiceItemKind.ACTIVITY,
        )
    }
}

private fun WorkTask.toInvoiceItem(
    hours: Int,
    hourlyRate: Double,
    sortOrder: Int,
): CreateInvoiceItemInput = CreateInvoiceItemInput(
    description = title,
    detail = description.ifBlank { null },
    quantity = hours.toDouble(),
    unitPrice = hourlyRate,
    sortOrder = sortOrder,
    kind = InvoiceItemKind.TASK_HOURS,
)

private fun taskSelectorLabel(
    selectedClient: Client?,
    clientTasks: List<WorkTask>,
    availableTasks: List<WorkTask>,
): String = when {
    selectedClient == null -> "Selecciona primero un cliente"
    clientTasks.isEmpty() -> "Este cliente no tiene tareas"
    availableTasks.isEmpty() -> "Todas las tareas están incluidas"
    else -> "Seleccionar tarea"
}

private fun formatEuro(value: Double): String = "${value.toFixedString(2).replace('.', ',')} EUR"

private fun String.toInvoiceDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    else -> "${bytes / 1_024} KB"
}
