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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.ClientProgramBillingPreview
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.core.utils.toFixedString
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.layout.InfoRow
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.features.invoices.domain.model.BillableUnit
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceItemInput
import com.requena.supportdesk.features.invoices.domain.model.InvoiceIssuer
import com.requena.supportdesk.features.invoices.domain.model.InvoiceItemKind
import com.requena.supportdesk.features.invoices.domain.model.InvoiceRecipient
import com.requena.supportdesk.features.invoices.domain.model.invoiceLegalProblem
import com.requena.supportdesk.features.invoices.domain.model.isSpanishPersonalTaxIdValid
import com.requena.supportdesk.features.invoices.domain.model.InvoicePdfFile
import com.requena.supportdesk.features.invoices.domain.model.InvoiceTotals
import com.requena.supportdesk.features.invoices.domain.model.TaskInvoiceLine
import com.requena.supportdesk.features.invoices.domain.model.buildTaskInvoiceLine
import com.requena.supportdesk.features.invoices.domain.model.billableUnitsFor
import com.requena.supportdesk.features.invoices.domain.model.calculateInvoiceTotals
import com.requena.supportdesk.features.invoices.domain.model.pending
import com.requena.supportdesk.features.invoices.presentation.event.InvoicesUiEvent
import com.requena.supportdesk.features.invoices.presentation.state.InvoicesUiState
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState

@Composable
fun AdminInvoicesScreen(
    clients: List<Client>,
    tasksState: TasksUiState,
    state: InvoicesUiState,
    onEvent: (InvoicesUiEvent) -> Unit,
    programBillingPreview: ClientProgramBillingPreview? = null,
    onLoadProgramBillingPreview: (clientId: String, period: String) -> Unit = { _, _ -> },
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
            onVoidInvoice = { invoice, reason -> onEvent(InvoicesUiEvent.VoidInvoice(invoice.fileName, reason)) },
        )

        if (showInvoiceForm) {
            InvoiceForm(
                clients = clients,
                tasksState = tasksState,
                state = state,
                preselectedClientId = preselectedClientId,
                programBillingPreview = programBillingPreview,
                onLoadProgramBillingPreview = onLoadProgramBillingPreview,
                onCancel = { showInvoiceForm = false },
                onLoadRecipient = { clientId -> onEvent(InvoicesUiEvent.LoadRecipient(clientId)) },
                onGenerate = { input, selectedLogIds ->
                    onEvent(InvoicesUiEvent.GenerateInvoice(input, selectedLogIds))
                },
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
    onVoidInvoice: (InvoicePdfFile, String?) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var invoicePendingVoid by rememberSaveable { mutableStateOf<String?>(null) }
    var voidReason by rememberSaveable { mutableStateOf("") }
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
                        isVoided = state.isVoided(invoice),
                        isVoiding = state.voidingInvoiceFileName == invoice.fileName,
                        canVoid = state.voidingInvoiceFileName == null,
                        onOpen = { onOpenInvoice(invoice) },
                        onVoid = {
                            voidReason = ""
                            invoicePendingVoid = invoice.fileName
                        },
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
    invoicePendingVoid?.let { fileName ->
        SectionCard(title = "Anular factura") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    "Se anulará «$fileName». El tiempo que llevaba asociado volverá a estar pendiente de facturar. La factura y el PDF se conservan para auditoría.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = voidReason,
                    onValueChange = { voidReason = it },
                    label = { Text("Motivo (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    SecondaryButton(
                        text = "Cancelar",
                        onClick = { invoicePendingVoid = null },
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = "Anular factura",
                        onClick = {
                            state.savedInvoices.firstOrNull { it.fileName == fileName }
                                ?.let { onVoidInvoice(it, voidReason.ifBlank { null }) }
                            invoicePendingVoid = null
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedInvoiceRow(
    invoice: InvoicePdfFile,
    isVoided: Boolean,
    isVoiding: Boolean,
    canVoid: Boolean,
    onOpen: () -> Unit,
    onVoid: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(invoice.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (isVoided) {
                    Text(
                        "ANULADA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                text = formatFileSize(invoice.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SecondaryButton(text = "Abrir PDF", onClick = onOpen)
        if (!isVoided) {
            SecondaryButton(
                text = "Anular factura",
                onClick = onVoid,
                enabled = !isVoiding && canVoid,
                isLoading = isVoiding,
            )
        }
    }
}

@Composable
private fun InvoiceForm(
    clients: List<Client>,
    tasksState: TasksUiState,
    state: InvoicesUiState,
    preselectedClientId: String?,
    programBillingPreview: ClientProgramBillingPreview?,
    onLoadProgramBillingPreview: (clientId: String, period: String) -> Unit,
    onCancel: () -> Unit,
    onLoadRecipient: (clientId: String) -> Unit,
    onGenerate: (CreateInvoiceInput, List<String>) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var selectedClientId by remember(preselectedClientId) { mutableStateOf(preselectedClientId.orEmpty()) }
    var selectedLogIds by remember { mutableStateOf(emptySet<String>()) }
    val activities = remember { mutableStateListOf<InvoiceActivityDraft>() }
    var nextActivityId by remember { mutableStateOf(0) }
    var issuedAt by remember { mutableStateOf(currentIsoDate()) }
    var operationDate by remember { mutableStateOf("") }
    var dueAt by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var taxPercent by remember { mutableStateOf("21") }
    var withholdingPercent by remember { mutableStateOf("0") }
    var taxExemptionNote by remember { mutableStateOf("") }
    var hourlyRate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val selectedClient = clients.firstOrNull { it.id == selectedClientId }
    // Prefilled from the local store; edits here are saved again when the PDF is generated.
    var issuer by remember(state.issuer) { mutableStateOf(state.issuer ?: InvoiceIssuer()) }
    val savedRecipient = state.recipientsByClientId[selectedClientId]
    var recipient by remember(selectedClientId, savedRecipient) {
        mutableStateOf(savedRecipient ?: InvoiceRecipient(legalName = selectedClient?.companyName.orEmpty()))
    }
    LaunchedEffect(selectedClientId) {
        if (selectedClientId.isNotBlank()) onLoadRecipient(selectedClientId)
    }
    val programBillingPeriod = issuedAt.take(7).takeIf { it.length == 7 }
    val programPreviewForSelection = programBillingPreview
        ?.takeIf { it.clientId == selectedClientId && it.period == programBillingPeriod }
    val programLines = programPreviewForSelection?.lines.orEmpty()
    val programLinesAlreadyAdded = programLines.isNotEmpty() && programLines.all { line ->
        activities.any { activity -> activity.detail == programInvoiceDetail(line.productKey, programBillingPeriod) }
    }

    LaunchedEffect(selectedClientId, programBillingPeriod) {
        if (selectedClientId.isNotBlank() && programBillingPeriod != null) {
            onLoadProgramBillingPreview(selectedClientId, programBillingPeriod)
        }
    }
    val clientTasks = tasksState.tasks.filter { it.clientId == selectedClientId }
    val clientTaskLogs = remember(tasksState.logs, clientTasks) {
        val clientTaskIds = clientTasks.map { it.id }.toSet()
        tasksState.logs.filter { it.taskId in clientTaskIds }
    }
    val pendingUnitsByTask = remember(clientTaskLogs, state.billedLogIds) {
        billableUnitsFor(clientTaskLogs, state.billedLogIds).pending().groupBy { it.taskId }
    }
    val allPendingLogIds = remember(pendingUnitsByTask) {
        pendingUnitsByTask.values.flatten().map { it.logId }.toSet()
    }
    // selectedLogIds may still reference units that are no longer pending (billed elsewhere,
    // or the client changed) - only the intersection is ever actually selectable/summarized.
    val selectedUnits = remember(pendingUnitsByTask, selectedLogIds) {
        pendingUnitsByTask.values.flatten().filter { it.logId in selectedLogIds }
    }
    val selectedTaskIdsDistinct = selectedUnits.map { it.taskId }.distinct()
    val taskLines = remember(selectedUnits, selectedTaskIdsDistinct) {
        selectedTaskIdsDistinct.map { taskId -> buildTaskInvoiceLine(taskId, selectedUnits) }
    }
    val parsedHourlyRate = hourlyRate.toInvoiceDecimalOrNull()
    val rate = parsedHourlyRate ?: 0.0
    val parsedTaxRate = taxPercent.toInvoiceDecimalOrNull()
    val taxRate = parsedTaxRate ?: 0.0
    val taskItems = taskLines.mapIndexed { index, line ->
        val task = clientTasks.first { it.id == line.taskId }
        task.toInvoiceItem(hours = line.decimalHours, hourlyRate = rate, sortOrder = index)
    }
    val activityItems = activities.mapIndexedNotNull { index, draft ->
        draft.toInvoiceItem(sortOrder = taskLines.size + index)
    }
    val invoiceItems = taskItems + activityItems
    val parsedWithholding = withholdingPercent.toInvoiceDecimalOrNull()
    val withholdingRate = parsedWithholding ?: 0.0
    val totals = calculateInvoiceTotals(invoiceItems, taxRate, withholdingRate)
    val activitiesAreValid = activities.all(InvoiceActivityDraft::isValid)
    val hasBillableTasks = taskLines.isNotEmpty()
    val draftInput = selectedClient?.let { client ->
        CreateInvoiceInput(
            clientId = selectedClientId,
            clientName = client.companyName,
            issuedAt = issuedAt,
            dueAt = dueAt.ifBlank { null },
            notes = notes.ifBlank { null },
            taxPercent = taxRate,
            items = invoiceItems,
            reference = reference.ifBlank { null },
            issuer = issuer,
            recipient = recipient,
            withholdingPercent = withholdingRate,
            operationDate = operationDate.ifBlank { null },
            taxExemptionNote = taxExemptionNote.ifBlank { null },
        )
    }
    val legalProblem = draftInput?.let(::invoiceLegalProblem)
    val isValid = draftInput != null && legalProblem == null && invoiceItems.isNotEmpty() && activitiesAreValid &&
        issuedAt.isNotBlank() && parsedTaxRate != null && taxRate.isFinite() && taxRate in 0.0..100.0 &&
        parsedWithholding != null &&
        (!hasBillableTasks || parsedHourlyRate != null && rate.isFinite() && rate > 0.0)

    SectionCard(
        title = "Nueva factura",
        subtitle = "Combina tareas por horas y actividades por cantidad en un mismo PDF.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            IssuerSection(issuer = issuer, onChange = { issuer = it })

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
                    selectedLogIds = emptySet()
                },
            )

            if (selectedClient != null) {
                RecipientSection(recipient = recipient, onChange = { recipient = it })
            }

            if (selectedClient != null) {
                Text("Tiempo pendiente de facturar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (pendingUnitsByTask.isEmpty()) {
                    Text(
                        "Este cliente no tiene tiempo facturable pendiente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        SecondaryButton(
                            text = "Seleccionar todo",
                            onClick = { selectedLogIds = allPendingLogIds },
                            enabled = selectedLogIds != allPendingLogIds,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = "Deseleccionar todo",
                            onClick = { selectedLogIds = emptySet() },
                            enabled = selectedLogIds.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    TaskUnitSelectionTree(
                        clientTasks = clientTasks,
                        pendingUnitsByTask = pendingUnitsByTask,
                        selectedLogIds = selectedLogIds,
                        onToggleUnit = { logId, checked ->
                            selectedLogIds = if (checked) selectedLogIds + logId else selectedLogIds - logId
                        },
                        onToggleGroup = { units, checked ->
                            val ids = units.map { it.logId }.toSet()
                            selectedLogIds = if (checked) selectedLogIds + ids else selectedLogIds - ids
                        },
                    )
                }
            }

            if (taskLines.isNotEmpty()) {
                Text("Resumen de la seleccion", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${selectedTaskIdsDistinct.size} tarea(s) · " +
                        "${selectedUnits.mapNotNull { it.workDate }.distinct().size} dia(s) · " +
                        "${selectedUnits.mapNotNull { it.sessionId }.distinct().size} sesion(es)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                taskLines.forEach { line ->
                    val task = clientTasks.firstOrNull { it.id == line.taskId }
                    SelectedInvoiceTaskLineRow(
                        taskTitle = task?.title ?: "Tarea",
                        line = line,
                        hourlyRate = rate,
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

            if (selectedClient != null) {
                SectionCard(
                    title = "Cuotas de programas",
                    subtitle = "Importa las suscripciones activas como actividades de este PDF local.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        when {
                            programPreviewForSelection == null -> Text(
                                "Cargando suscripciones activas…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            programLines.isEmpty() -> Text(
                                "Este cliente no tiene programas activos para este periodo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            else -> {
                                programLines.forEach { line ->
                                    Text(
                                        "${line.name}: ${formatProgramEuro(line.monthlyPriceCents)} / mes",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                SecondaryButton(
                                    text = if (programLinesAlreadyAdded) {
                                        "Cuotas de programas añadidas"
                                    } else {
                                        "Añadir cuotas de programas"
                                    },
                                    enabled = !programLinesAlreadyAdded,
                                    onClick = {
                                        programLines.forEach { line ->
                                            activities += InvoiceActivityDraft(
                                                id = nextActivityId,
                                                description = "Cuota mensual · ${line.name}",
                                                detail = programInvoiceDetail(line.productKey, programBillingPeriod),
                                                quantity = "1",
                                                unitPrice = centsToInvoiceInput(line.monthlyPriceCents),
                                            )
                                            nextActivityId += 1
                                        }
                                    },
                                    fullWidth = true,
                                )
                            }
                        }
                    }
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

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = operationDate,
                    onValueChange = { operationDate = it },
                    label = { Text("Fecha del servicio (si es distinta)") },
                    placeholder = { Text("p. ej. Octubre 2026") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Referencia (opcional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

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

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = taxPercent,
                    onValueChange = { taxPercent = it },
                    label = { Text("IVA (%)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = withholdingPercent,
                    onValueChange = { withholdingPercent = it },
                    label = { Text("Retención IRPF (%)") },
                    supportingText = { Text("0 si no aplica · 15 general · 7 nuevos autónomos") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            if (parsedTaxRate == 0.0) {
                OutlinedTextField(
                    value = taxExemptionNote,
                    onValueChange = { taxExemptionNote = it },
                    label = { Text("Motivo legal del IVA 0 %") },
                    placeholder = { Text("p. ej. Operación exenta de IVA según el art. 20 de la Ley 37/1992") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3,
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

            InvoiceSummary(totals = totals, taxPercent = taxRate, withholdingPercent = withholdingRate)

            if (legalProblem != null) {
                Text(
                    text = legalProblem,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

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
                        draftInput?.let { input -> onGenerate(input, selectedUnits.map { it.logId }) }
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
private fun SelectedInvoiceTaskLineRow(
    taskTitle: String,
    line: TaskInvoiceLine,
    hourlyRate: Double,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xs)) {
        Text(taskTitle, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "Real: ${formatSupportDeskDuration(line.realMinutes)} · Facturable: " +
                "${decimalHoursLabel(line.decimalHours)} h × ${formatEuro(hourlyRate)} = " +
                formatEuro(line.decimalHours * hourlyRate),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Task > day (or the historic bucket) > individual session, reused Checkbox/TriStateCheckbox
 * components only - matches the tri-state semantics required for hierarchical selection
 * (checked/unchecked/partially selected) without introducing custom controls.
 */
@Composable
private fun TaskUnitSelectionTree(
    clientTasks: List<WorkTask>,
    pendingUnitsByTask: Map<String, List<BillableUnit>>,
    selectedLogIds: Set<String>,
    onToggleUnit: (String, Boolean) -> Unit,
    onToggleGroup: (List<BillableUnit>, Boolean) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var expandedTaskIds by remember { mutableStateOf(emptySet<String>()) }
    var expandedDayKeys by remember { mutableStateOf(emptySet<String>()) }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        clientTasks.forEach { task ->
            val units = pendingUnitsByTask[task.id].orEmpty()
            if (units.isEmpty()) return@forEach
            val selectedCount = units.count { it.logId in selectedLogIds }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedTaskIds = expandedTaskIds.toggled(task.id) }
                        .padding(vertical = spacing.xs),
                ) {
                    TriStateCheckbox(
                        state = triState(selectedCount, units.size),
                        onClick = { onToggleGroup(units, selectedCount < units.size) },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            text = "Pendiente: ${formatSupportDeskDuration(units.sumOf { it.minutes })} ($selectedCount/${units.size} seleccionado)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = if (task.id in expandedTaskIds) "Ocultar dias" else "Ver dias",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (task.id in expandedTaskIds) {
                    val byDay = units.groupBy { it.workDate ?: HISTORIC_UNIT_KEY }
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        byDay.forEach { (dayKey, dayUnits) ->
                            val fullDayKey = "${task.id}|$dayKey"
                            val daySelectedCount = dayUnits.count { it.logId in selectedLogIds }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = dayUnits.size > 1) {
                                        expandedDayKeys = expandedDayKeys.toggled(fullDayKey)
                                    }
                                    .padding(vertical = 2.dp),
                            ) {
                                TriStateCheckbox(
                                    state = triState(daySelectedCount, dayUnits.size),
                                    onClick = { onToggleGroup(dayUnits, daySelectedCount < dayUnits.size) },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (dayKey == HISTORIC_UNIT_KEY) "Tiempo historico sin desglose" else dayKey,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = formatSupportDeskDuration(dayUnits.sumOf { it.minutes }),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (dayUnits.size > 1) {
                                    Text(
                                        text = if (fullDayKey in expandedDayKeys) "Ocultar sesiones" else "Ver sesiones",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            if (dayUnits.size > 1 && fullDayKey in expandedDayKeys) {
                                Column(modifier = Modifier.fillMaxWidth().padding(start = spacing.lg)) {
                                    dayUnits.forEach { unit ->
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Checkbox(
                                                checked = unit.logId in selectedLogIds,
                                                onCheckedChange = { checked -> onToggleUnit(unit.logId, checked) },
                                            )
                                            Text(
                                                text = "Sesion - ${formatSupportDeskDuration(unit.minutes)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(top = 12.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun triState(selectedCount: Int, total: Int): ToggleableState = when {
    selectedCount <= 0 -> ToggleableState.Off
    selectedCount >= total -> ToggleableState.On
    else -> ToggleableState.Indeterminate
}

private fun Set<String>.toggled(id: String): Set<String> = if (id in this) this - id else this + id

private fun decimalHoursLabel(hours: Double): String = hours.toFixedString(2).replace('.', ',')

private const val HISTORIC_UNIT_KEY = "__HISTORIC__"

@Composable
private fun InvoiceSummary(
    totals: InvoiceTotals,
    taxPercent: Double,
    withholdingPercent: Double,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(title = "Resumen") {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            InfoRow(label = "Base imponible", value = formatEuro(totals.subtotal))
            InfoRow(label = "IVA (${taxPercent.toFixedString(2)}%)", value = formatEuro(totals.tax))
            if (withholdingPercent > 0.0) {
                InfoRow(
                    label = "Retención IRPF (${withholdingPercent.toFixedString(2)}%)",
                    value = "-${formatEuro(totals.withholding)}",
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total a pagar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

/**
 * The self-employed user's fiscal data. Once complete it collapses to a summary, because it is
 * the same on every invoice and is remembered on this computer.
 */
@Composable
private fun IssuerSection(issuer: InvoiceIssuer, onChange: (InvoiceIssuer) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    val missing = issuer.missingRequiredFields()
    var editing by remember { mutableStateOf(false) }
    val expanded = editing || missing.isNotEmpty()
    SectionCard(
        title = "Tus datos fiscales",
        subtitle = if (missing.isEmpty()) {
            "Salen en todas tus facturas. Se guardan solo en este equipo."
        } else {
            "Rellénalos una vez: son obligatorios en la factura y se guardan solo en este equipo."
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            if (!expanded) {
                Text(
                    listOf(issuer.fullName, issuer.tradeName).filter(String::isNotBlank).joinToString(" · "),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "NIF ${issuer.taxId} · ${issuer.addressLine}, ${issuer.postalCode} ${issuer.city}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SecondaryButton(text = "Editar mis datos", onClick = { editing = true }, fullWidth = true)
                return@Column
            }
            InvoiceTextField(issuer.fullName, { onChange(issuer.copy(fullName = it)) }, "Nombre y apellidos *")
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                InvoiceTextField(issuer.taxId, { onChange(issuer.copy(taxId = it)) }, "NIF *", Modifier.weight(1f))
                InvoiceTextField(issuer.tradeName, { onChange(issuer.copy(tradeName = it)) }, "Nombre comercial", Modifier.weight(1f))
            }
            if (isSpanishPersonalTaxIdValid(issuer.taxId) == false) {
                Text(
                    "La letra del NIF no coincide con los números.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            InvoiceTextField(issuer.addressLine, { onChange(issuer.copy(addressLine = it)) }, "Domicilio fiscal (calle y número) *")
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                InvoiceTextField(issuer.postalCode, { onChange(issuer.copy(postalCode = it)) }, "Código postal *", Modifier.weight(1f))
                InvoiceTextField(issuer.city, { onChange(issuer.copy(city = it)) }, "Población *", Modifier.weight(1f))
                InvoiceTextField(issuer.province, { onChange(issuer.copy(province = it)) }, "Provincia", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                InvoiceTextField(issuer.email, { onChange(issuer.copy(email = it)) }, "Email", Modifier.weight(1f))
                InvoiceTextField(issuer.phone, { onChange(issuer.copy(phone = it)) }, "Teléfono", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                InvoiceTextField(issuer.paymentMethod, { onChange(issuer.copy(paymentMethod = it)) }, "Forma de pago", Modifier.weight(1f))
                InvoiceTextField(issuer.iban, { onChange(issuer.copy(iban = it)) }, "IBAN para el cobro", Modifier.weight(1f))
            }
            if (editing && missing.isEmpty()) {
                SecondaryButton(text = "Listo", onClick = { editing = false }, fullWidth = true)
            }
        }
    }
}

/** Fiscal data of the selected client: legal name, NIF/CIF, address and billing email. */
@Composable
private fun RecipientSection(recipient: InvoiceRecipient, onChange: (InvoiceRecipient) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(
        title = "Datos fiscales del cliente",
        subtitle = "Obligatorios cuando el cliente es empresa o autónomo. Se recuerdan para la próxima factura.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                InvoiceTextField(recipient.legalName, { onChange(recipient.copy(legalName = it)) }, "Razón social *", Modifier.weight(2f))
                InvoiceTextField(recipient.taxId, { onChange(recipient.copy(taxId = it)) }, "NIF/CIF *", Modifier.weight(1f))
            }
            InvoiceTextField(recipient.addressLine, { onChange(recipient.copy(addressLine = it)) }, "Dirección *")
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                InvoiceTextField(recipient.postalCode, { onChange(recipient.copy(postalCode = it)) }, "Código postal", Modifier.weight(1f))
                InvoiceTextField(recipient.city, { onChange(recipient.copy(city = it)) }, "Población *", Modifier.weight(1f))
                InvoiceTextField(recipient.province, { onChange(recipient.copy(province = it)) }, "Provincia", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                InvoiceTextField(recipient.email, { onChange(recipient.copy(email = it)) }, "Email de facturación", Modifier.weight(2f))
                InvoiceTextField(recipient.country, { onChange(recipient.copy(country = it)) }, "País", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InvoiceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
    )
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
    hours: Double,
    hourlyRate: Double,
    sortOrder: Int,
): CreateInvoiceItemInput = CreateInvoiceItemInput(
    description = title,
    detail = description.ifBlank { null },
    quantity = hours,
    unitPrice = hourlyRate,
    sortOrder = sortOrder,
    kind = InvoiceItemKind.TASK_HOURS,
)

private fun formatEuro(value: Double): String = "${value.toFixedString(2).replace('.', ',')} EUR"

private fun formatProgramEuro(cents: Long): String = "${cents / 100},${(cents % 100).toString().padStart(2, '0')} €"

private fun centsToInvoiceInput(cents: Long): String = "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

private fun programInvoiceDetail(productKey: String, period: String?): String =
    "Programa $productKey · cuota ${period.orEmpty()}"

private fun String.toInvoiceDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    else -> "${bytes / 1_024} KB"
}
