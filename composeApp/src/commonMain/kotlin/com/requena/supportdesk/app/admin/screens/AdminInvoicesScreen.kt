package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.admin.AdminLayoutMode
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.inputs.SearchField
import com.requena.supportdesk.designsystem.components.layout.InfoRow
import com.requena.supportdesk.designsystem.components.motion.SupportDeskEntrance
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceItemInput
import com.requena.supportdesk.features.invoices.domain.model.Invoice
import com.requena.supportdesk.features.invoices.domain.model.InvoiceStatus
import com.requena.supportdesk.features.invoices.presentation.event.InvoicesUiEvent
import com.requena.supportdesk.features.invoices.presentation.state.InvoicesUiState
import com.requena.supportdesk.features.invoices.presentation.viewmodel.InvoicesViewModel

// ─── Entry point ──────────────────────────────────────────────────────────────

@Composable
fun AdminInvoicesScreen(
    clients: List<Client>,
    state: InvoicesUiState,
    viewModel: InvoicesViewModel,
    onEvent: (InvoicesUiEvent) -> Unit,
    onNavigateToCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val uriHandler = LocalUriHandler.current

    // Collect PDF URL side effect
    LaunchedEffect(viewModel) {
        viewModel.pdfUrl.collect { url ->
            uriHandler.openUri(url)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isSplitPane = maxWidth >= 900.dp
        if (isSplitPane) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                SupportDeskEntrance(index = 0, horizontal = true, modifier = Modifier.weight(0.42f).fillMaxHeight()) {
                    InvoiceListPane(
                        state = state,
                        onEvent = onEvent,
                        onNavigateToCreate = onNavigateToCreate,
                        modifier = Modifier.fillMaxHeight(),
                    )
                }
                SupportDeskEntrance(index = 1, horizontal = true, modifier = Modifier.weight(0.58f).fillMaxHeight()) {
                    InvoiceDetailPane(
                        invoice = state.selectedInvoice,
                        onEvent = onEvent,
                        modifier = Modifier.fillMaxHeight(),
                    )
                }
            }
        } else {
            SupportDeskEntrance(index = 0, horizontal = true, modifier = Modifier.fillMaxSize()) {
                InvoiceListPane(
                    state = state,
                    onEvent = onEvent,
                    onNavigateToCreate = onNavigateToCreate,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

// ─── Create invoice screen (routed separately) ────────────────────────────────

@Composable
fun AdminCreateInvoiceScreen(
    clients: List<Client>,
    onBack: () -> Unit,
    onCreateInvoice: (CreateInvoiceInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing

    data class ItemDraft(
        val description: String = "",
        val quantity: String = "1",
        val unitPrice: String = "",
    )

    var selectedClientId by remember { mutableStateOf("") }
    var selectedClientName by remember { mutableStateOf("Seleccionar cliente") }
    var clientDropdownExpanded by remember { mutableStateOf(false) }
    var issuedAt by remember { mutableStateOf(currentIsoDate()) }
    var dueAt by remember { mutableStateOf("") }
    var taxPercent by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf(ItemDraft()) }

    val subtotal = items.sumOf {
        val q = it.quantity.toDoubleOrNull() ?: 0.0
        val p = it.unitPrice.toDoubleOrNull() ?: 0.0
        q * p
    }
    val tax = subtotal * ((taxPercent.toDoubleOrNull() ?: 0.0) / 100.0)
    val total = subtotal + tax

    val isValid = selectedClientId.isNotBlank() &&
        issuedAt.isNotBlank() &&
        items.isNotEmpty() &&
        items.all { it.description.isNotBlank() && (it.unitPrice.toDoubleOrNull() ?: 0.0) > 0.0 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        // General data section
        SectionCard(
            title = "Datos generales",
            subtitle = "Cliente, fechas e impuesto",
            neonAccentColor = MaterialTheme.colorScheme.primary,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                // Client selector
                Box {
                    OutlinedTextField(
                        value = selectedClientName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cliente") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clientDropdownExpanded = true },
                    )
                    DropdownMenu(
                        expanded = clientDropdownExpanded,
                        onDismissRequest = { clientDropdownExpanded = false },
                    ) {
                        clients.forEach { client ->
                            DropdownMenuItem(
                                text = { Text(client.companyName) },
                                onClick = {
                                    selectedClientId = client.id
                                    selectedClientName = client.companyName
                                    clientDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    OutlinedTextField(
                        value = issuedAt,
                        onValueChange = { issuedAt = it },
                        label = { Text("Fecha emisión (YYYY-MM-DD)") },
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
                    value = taxPercent,
                    onValueChange = { taxPercent = it },
                    label = { Text("Impuesto (%)") },
                    modifier = Modifier.fillMaxWidth(0.4f),
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
            }
        }

        // Line items section
        SectionCard(
            title = "Líneas de factura",
            subtitle = "Descripción, cantidad y precio unitario",
            neonAccentColor = MaterialTheme.colorScheme.secondary,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                items.forEachIndexed { index, item ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = item.description,
                            onValueChange = { items[index] = item.copy(description = it) },
                            label = { Text("Descripción") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = item.quantity,
                            onValueChange = { items[index] = item.copy(quantity = it) },
                            label = { Text("Cant.") },
                            modifier = Modifier.width(72.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                        OutlinedTextField(
                            value = item.unitPrice,
                            onValueChange = { items[index] = item.copy(unitPrice = it) },
                            label = { Text("Precio") },
                            modifier = Modifier.width(96.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                        SecondaryButton(
                            text = "✕",
                            onClick = { if (items.size > 1) items.removeAt(index) },
                        )
                    }
                }

                SecondaryButton(
                    text = "+ Agregar línea",
                    onClick = { items.add(ItemDraft()) },
                    fullWidth = true,
                )
            }
        }

        // Totals preview
        SectionCard(
            title = "Resumen",
            subtitle = "Vista previa del total",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                InfoRow(label = "Subtotal", value = "$${"%.2f".format(subtotal)}")
                InfoRow(label = "Impuesto (${taxPercent}%)", value = "$${"%.2f".format(tax)}")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "$${"%.2f".format(total)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SecondaryButton(text = "Cancelar", onClick = onBack, modifier = Modifier.weight(1f))
            PrimaryButton(
                text = "Crear borrador",
                enabled = isValid,
                onClick = {
                    onCreateInvoice(
                        CreateInvoiceInput(
                            clientId = selectedClientId,
                            issuedAt = issuedAt,
                            dueAt = dueAt.ifBlank { null },
                            notes = notes.ifBlank { null },
                            taxPercent = taxPercent.toDoubleOrNull() ?: 0.0,
                            items = items.mapIndexed { idx, it ->
                                CreateInvoiceItemInput(
                                    description = it.description,
                                    quantity = it.quantity.toDoubleOrNull() ?: 1.0,
                                    unitPrice = it.unitPrice.toDoubleOrNull() ?: 0.0,
                                    sortOrder = idx,
                                )
                            },
                        )
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ─── List pane ────────────────────────────────────────────────────────────────

@Composable
private fun InvoiceListPane(
    state: InvoicesUiState,
    onEvent: (InvoicesUiEvent) -> Unit,
    onNavigateToCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<InvoiceStatus?>(null) }

    val filtered = remember(state.invoices, searchQuery, statusFilter) {
        state.invoices.filter { inv ->
            (statusFilter == null || inv.status == statusFilter) &&
                (searchQuery.isBlank() || inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    inv.clientName.contains(searchQuery, ignoreCase = true))
        }
    }

    val draft = state.invoices.count { it.status == InvoiceStatus.DRAFT }
    val sent = state.invoices.count { it.status == InvoiceStatus.SENT }
    val paid = state.invoices.count { it.status == InvoiceStatus.PAID }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        // Metrics
        SupportDeskEntrance(index = 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                MetricCard(label = "Total", value = "${state.invoices.size}", supportingText = "facturas", modifier = Modifier.weight(1f))
                MetricCard(label = "Borrador", value = "$draft", supportingText = "pendientes", modifier = Modifier.weight(1f))
                MetricCard(label = "Enviadas", value = "$sent", supportingText = "al cliente", modifier = Modifier.weight(1f))
                MetricCard(label = "Cobradas", value = "$paid", supportingText = "pagadas", modifier = Modifier.weight(1f))
            }
        }

        // Search + filter
        SupportDeskEntrance(index = 1) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                SearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Buscar factura o cliente...",
                    modifier = Modifier.fillMaxWidth(),
                )
                FilterBar(
                    label = "Estado",
                    options = listOf(
                        FilterOption(InvoiceStatus.DRAFT, "Borrador"),
                        FilterOption(InvoiceStatus.SENT, "Enviada"),
                        FilterOption(InvoiceStatus.PAID, "Pagada"),
                        FilterOption(InvoiceStatus.CANCELLED, "Cancelada"),
                    ),
                    selected = statusFilter,
                    onSelected = { statusFilter = it },
                    allLabel = "Todas",
                )
            }
        }

        // New invoice button
        SupportDeskEntrance(index = 2) {
            PrimaryButton(
                text = "Nueva factura",
                onClick = onNavigateToCreate,
                fullWidth = true,
            )
        }

        // List
        when {
            state.isLoading -> LoadingState(modifier = Modifier.weight(1f))
            filtered.isEmpty() -> EmptyState(
                title = "Sin facturas",
                message = if (searchQuery.isNotBlank() || statusFilter != null)
                    "No hay facturas que coincidan con el filtro." else "Crea la primera factura.",
                modifier = Modifier.weight(1f),
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                itemsIndexed(filtered, key = { _, inv -> inv.id }) { index, invoice ->
                    SupportDeskEntrance(index = index + 3) {
                        InvoiceListItem(
                            invoice = invoice,
                            selected = state.selectedInvoice?.id == invoice.id,
                            onClick = { onEvent(InvoicesUiEvent.SelectInvoice(invoice.id)) },
                        )
                    }
                }
            }
        }
    }
}

// ─── Detail pane ──────────────────────────────────────────────────────────────

@Composable
private fun InvoiceDetailPane(
    invoice: Invoice?,
    onEvent: (InvoicesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing

    if (invoice == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Selecciona una factura para ver el detalle.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        // Header card
        SupportDeskEntrance(index = 0) {
            SectionCard(
                title = invoice.invoiceNumber,
                subtitle = invoice.clientName,
                neonAccentColor = invoiceStatusAccentColor(invoice.status),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        InvoiceStatusBadge(status = invoice.status)
                        Text(
                            text = "Emitida: ${invoice.issuedAt}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    invoice.dueAt?.let {
                        InfoRow(label = "Vencimiento", value = it)
                    }
                    invoice.sentAt?.let {
                        InfoRow(label = "Enviada el", value = formatSupportDeskDateTime(it))
                    }
                    invoice.paidAt?.let {
                        InfoRow(label = "Pagada el", value = formatSupportDeskDateTime(it))
                    }
                }
            }
        }

        // Items table
        SupportDeskEntrance(index = 1) {
            SectionCard(title = "Líneas", subtitle = "Detalle de servicios facturados") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    // Header row
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Descripción", style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Cant.", style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(48.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Precio", style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(72.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Subtotal", style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(80.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    invoice.items.sortedBy { it.sortOrder }.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.description, style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${"%.1f".format(item.quantity)}", style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(48.dp))
                            Text("$${"%.2f".format(item.unitPrice)}", style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(72.dp))
                            Text("$${"%.2f".format(item.subtotal)}", style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium, modifier = Modifier.width(80.dp))
                        }
                    }
                }
            }
        }

        // Totals
        SupportDeskEntrance(index = 2) {
            SectionCard(title = "Totales") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    InfoRow(label = "Subtotal", value = "$${"%.2f".format(invoice.subtotal)}")
                    InfoRow(label = "Impuesto (${invoice.taxPercent}%)", value = "$${"%.2f".format(invoice.taxAmount)}")
                    Spacer(Modifier.height(spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "$${"%.2f".format(invoice.total)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // Notes
        invoice.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            SupportDeskEntrance(index = 3) {
                SectionCard(title = "Notas") {
                    Text(notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Actions
        SupportDeskEntrance(index = 4) {
            SectionCard(title = "Acciones") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    // PDF download (always available)
                    PrimaryButton(
                        text = "Descargar PDF",
                        onClick = { onEvent(InvoicesUiEvent.DownloadPdf(invoice.id)) },
                        fullWidth = true,
                    )

                    when (invoice.status) {
                        InvoiceStatus.DRAFT -> {
                            PrimaryButton(
                                text = "Enviar al cliente",
                                onClick = { onEvent(InvoicesUiEvent.UpdateStatus(invoice.id, InvoiceStatus.SENT)) },
                                fullWidth = true,
                            )
                            SecondaryButton(
                                text = "Cancelar factura",
                                onClick = { onEvent(InvoicesUiEvent.UpdateStatus(invoice.id, InvoiceStatus.CANCELLED)) },
                                fullWidth = true,
                            )
                        }
                        InvoiceStatus.SENT -> {
                            PrimaryButton(
                                text = "Marcar como pagada",
                                onClick = { onEvent(InvoicesUiEvent.UpdateStatus(invoice.id, InvoiceStatus.PAID)) },
                                fullWidth = true,
                            )
                            SecondaryButton(
                                text = "Cancelar factura",
                                onClick = { onEvent(InvoicesUiEvent.UpdateStatus(invoice.id, InvoiceStatus.CANCELLED)) },
                                fullWidth = true,
                            )
                        }
                        InvoiceStatus.PAID, InvoiceStatus.CANCELLED -> Unit
                    }
                }
            }
        }
    }
}

// ─── Shared components ────────────────────────────────────────────────────────

@Composable
fun InvoiceStatusBadge(status: InvoiceStatus, modifier: Modifier = Modifier) {
    val semantic = SupportDeskThemeTokens.semanticColors
    val (containerColor, contentColor) = when (status) {
        InvoiceStatus.DRAFT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        InvoiceStatus.SENT -> semantic.infoContainer to semantic.info
        InvoiceStatus.PAID -> semantic.successContainer to semantic.success
        InvoiceStatus.CANCELLED -> semantic.dangerContainer to semantic.danger
    }
    val label = when (status) {
        InvoiceStatus.DRAFT -> "Borrador"
        InvoiceStatus.SENT -> "Enviada"
        InvoiceStatus.PAID -> "Pagada"
        InvoiceStatus.CANCELLED -> "Cancelada"
    }
    SupportDeskBadge(text = label, containerColor = containerColor, contentColor = contentColor, modifier = modifier)
}

@Composable
private fun InvoiceListItem(
    invoice: Invoice,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val elevations = SupportDeskThemeTokens.elevations

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 180),
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) elevations.raised else elevations.subtle,
        animationSpec = tween(durationMillis = 200),
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = elevation,
        color = backgroundColor,
        modifier = modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                InvoiceStatusBadge(status = invoice.status)
            }
            Text(
                text = invoice.clientName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = invoice.issuedAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$${"%.2f".format(invoice.total)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun invoiceStatusAccentColor(status: InvoiceStatus) = when (status) {
    InvoiceStatus.DRAFT -> MaterialTheme.colorScheme.outlineVariant
    InvoiceStatus.SENT -> SupportDeskThemeTokens.semanticColors.info
    InvoiceStatus.PAID -> SupportDeskThemeTokens.semanticColors.success
    InvoiceStatus.CANCELLED -> SupportDeskThemeTokens.semanticColors.danger
}
