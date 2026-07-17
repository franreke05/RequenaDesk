package com.requena.supportdesk.app.client.screens.business.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.app.client.components.ClientPortalSectionTitle
import com.requena.supportdesk.app.client.components.ClientPortalSurfaceCard
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceDirection
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntry
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntryStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessPaymentStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocument
import com.requena.supportdesk.features.business.finance.domain.FINANCE_BETA_DISCLAIMER
import com.requena.supportdesk.features.business.finance.domain.FinanceEntryInput
import com.requena.supportdesk.features.business.finance.domain.SalesDocumentDraftInput
import com.requena.supportdesk.features.business.finance.domain.SalesLineInput
import com.requena.supportdesk.features.business.finance.presentation.BusinessAccountingUiState
import com.requena.supportdesk.features.business.finance.presentation.BusinessInvoicingUiState

/**
 * Stateless integration surface: the portal owns navigation, API calls and
 * effects. These callbacks intentionally pass explicit versioned commands.
 */
@Composable
fun BusinessInvoicingScreen(
    state: BusinessInvoicingUiState,
    onSave: (documentId: String?, expectedVersion: Int?, input: SalesDocumentDraftInput) -> Unit,
    onArchive: (documentId: String, expectedVersion: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var editingId by remember { mutableStateOf<String?>(null) }
    var editingVersion by remember { mutableStateOf<Int?>(null) }
    var issuer by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    fun clearEditor() {
        editingId = null; editingVersion = null; issuer = ""; customer = ""; issueDate = ""; dueDate = ""
        description = ""; quantity = "1"; unitPrice = ""; formError = null
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        ClientPortalPageHeader(
            title = "Facturación comercial",
            subtitle = "Prepara borradores y proformas para tu negocio.",
        )
        ClientPortalSurfaceCard {
            Text(FINANCE_BETA_DISCLAIMER, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            Text(
                "Esta beta no emite ni envía facturas fiscales. Revisa los datos antes de usar cualquier documento fuera de la prueba.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (state.accessDenied) {
            EmptyState(title = "Autorización pendiente", message = "Pide al administrador que active Facturación comercial para tu empresa.")
            return@Column
        }
        ClientPortalSurfaceCard {
            ClientPortalSectionTitle(
                if (editingId == null) "Crear borrador" else "Editar borrador",
                "Añade una actividad con cantidad y precio unitario.",
            )
            OutlinedTextField(issuer, { issuer = it }, Modifier.fillMaxWidth(), label = { Text("Tu empresa o emisor") }, singleLine = true)
            OutlinedTextField(customer, { customer = it }, Modifier.fillMaxWidth(), label = { Text("Cliente") }, singleLine = true)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(issueDate, { issueDate = it }, Modifier.weight(1f), label = { Text("Fecha AAAA-MM-DD") }, singleLine = true)
                OutlinedTextField(dueDate, { dueDate = it }, Modifier.weight(1f), label = { Text("Vencimiento") }, singleLine = true)
            }
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Actividad o concepto") }, singleLine = true)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(quantity, { quantity = it }, Modifier.weight(1f), label = { Text("Cantidad") }, singleLine = true)
                OutlinedTextField(unitPrice, { unitPrice = it }, Modifier.weight(1f), label = { Text("Precio unitario (€)") }, singleLine = true)
            }
            formError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            PrimaryButton(
                text = if (editingId == null) "Guardar borrador" else "Guardar cambios",
                fullWidth = true,
                isLoading = state.isSaving,
                onClick = {
                    val quantityMilli = quantity.toQuantityMilliOrNull()
                    val cents = unitPrice.toCentsOrNull()
                    if (quantityMilli == null || cents == null || issuer.isBlank() || customer.isBlank() || description.isBlank() || issueDate.isBlank()) {
                        formError = "Completa los campos obligatorios con cantidades y precios válidos."
                    } else {
                        formError = null
                        onSave(
                            editingId,
                            editingVersion,
                            SalesDocumentDraftInput(
                                issuerName = issuer,
                                customerName = customer,
                                issueDate = issueDate,
                                dueDate = dueDate.ifBlank { null },
                                lines = listOf(SalesLineInput(description, quantityMilli, cents)),
                            ),
                        )
                    }
                },
            )
            if (editingId != null) SecondaryButton(text = "Cancelar edición", fullWidth = true, onClick = ::clearEditor)
        }
        ClientPortalSectionTitle("Tus borradores", "Puedes editar un borrador o archivarlo; no se elimina documentación fiscal.")
        if (state.documents.isEmpty()) {
            EmptyState(title = "Aún no hay borradores", message = "Crea una proforma de prueba para empezar.")
        } else {
            state.documents.forEach { document ->
                SalesDocumentCard(
                    document = document,
                    onEdit = {
                        editingId = document.id; editingVersion = document.version; issuer = document.issuerName
                        customer = document.customerName; issueDate = document.issueDate; dueDate = document.dueDate.orEmpty()
                        val first = document.lines.firstOrNull()
                        description = first?.description.orEmpty(); quantity = first?.quantityMilli?.toQuantityInput().orEmpty()
                        unitPrice = first?.unitPriceCents?.toEuroInput().orEmpty(); formError = null
                    },
                    onArchive = { onArchive(document.id, document.version) },
                )
            }
        }
    }
}

@Composable
fun BusinessAccountingScreen(
    state: BusinessAccountingUiState,
    onSave: (entryId: String?, expectedVersion: Int?, input: FinanceEntryInput) -> Unit,
    onRecord: (entryId: String, expectedVersion: Int) -> Unit,
    onVoid: (entryId: String, expectedVersion: Int, reason: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var editingId by remember { mutableStateOf<String?>(null) }
    var editingVersion by remember { mutableStateOf<Int?>(null) }
    var isExpense by remember { mutableStateOf(true) }
    var date by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var counterparty by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    fun clearEditor() {
        editingId = null; editingVersion = null; isExpense = true; date = ""; description = ""; amount = ""
        category = ""; counterparty = ""; formError = null
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        ClientPortalPageHeader("Contabilidad y gastos", "Control operativo de ingresos y gastos de tu empresa.")
        Text("Importes e IVA informativos; no es una liquidación tributaria ni contabilidad oficial.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.overview?.let { overview ->
            ClientPortalSurfaceCard {
                Text("Flujo de caja ${overview.period}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Ingresos ${overview.incomeCents.toEuroInput()} € · Gastos ${overview.expenseCents.toEuroInput()} €")
                Text("Neto ${overview.netCashFlowCents.toEuroInput()} € · Pendiente ${overview.pendingCents.toEuroInput()} €", style = MaterialTheme.typography.bodySmall)
            }
        }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (state.accessDenied) {
            EmptyState(title = "Autorización pendiente", message = "Pide al administrador que active Contabilidad y gastos para tu empresa.")
            return@Column
        }
        ClientPortalSurfaceCard {
            ClientPortalSectionTitle(if (editingId == null) "Añadir registro" else "Editar registro", "Guarda primero como borrador y regístralo cuando lo hayas revisado.")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                SecondaryButton("Gasto", modifier = Modifier.weight(1f), fullWidth = true, enabled = !isExpense, onClick = { isExpense = true })
                SecondaryButton("Ingreso", modifier = Modifier.weight(1f), fullWidth = true, enabled = isExpense, onClick = { isExpense = false })
            }
            OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Fecha AAAA-MM-DD") }, singleLine = true)
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Descripción") }, singleLine = true)
            OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("Base (€)") }, singleLine = true)
            OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Categoría (opcional)") }, singleLine = true)
            OutlinedTextField(counterparty, { counterparty = it }, Modifier.fillMaxWidth(), label = { Text("Proveedor o cliente (opcional)") }, singleLine = true)
            formError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            PrimaryButton(
                text = if (editingId == null) "Guardar borrador" else "Guardar cambios",
                fullWidth = true,
                isLoading = state.isSaving,
                onClick = {
                    val cents = amount.toCentsOrNull()
                    if (cents == null || date.isBlank() || description.isBlank()) {
                        formError = "Completa fecha, descripción y un importe válido."
                    } else {
                        formError = null
                        onSave(editingId, editingVersion, FinanceEntryInput(
                            direction = if (isExpense) BusinessFinanceDirection.EXPENSE else BusinessFinanceDirection.INCOME,
                            occurredOn = date, description = description, netCents = cents,
                            categoryName = category.ifBlank { null }, counterpartyName = counterparty.ifBlank { null },
                        ))
                    }
                },
            )
            if (editingId != null) SecondaryButton("Cancelar edición", fullWidth = true, onClick = ::clearEditor)
        }
        ClientPortalSectionTitle("Tus registros", "Puedes editar borradores, registrarlos o anularlos con motivo.")
        if (state.entries.isEmpty()) EmptyState(title = "Aún no hay registros", message = "Añade un gasto o ingreso para ver el control mensual.")
        state.entries.forEach { entry ->
            FinanceEntryCard(
                entry,
                onEdit = {
                    editingId = entry.id; editingVersion = entry.version; isExpense = entry.direction == BusinessFinanceDirection.EXPENSE
                    date = entry.occurredOn; description = entry.description; amount = entry.netCents.toEuroInput()
                    category = entry.categoryName.orEmpty(); counterparty = entry.counterpartyName.orEmpty(); formError = null
                },
                onRecord = { onRecord(entry.id, entry.version) },
                onVoid = { onVoid(entry.id, entry.version, "Anulado por el cliente") },
            )
        }
    }
}

@Composable
private fun SalesDocumentCard(document: BusinessSalesDocument, onEdit: () -> Unit, onArchive: () -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    ClientPortalSurfaceCard {
        Text(document.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("${document.issueDate} · ${document.totalCents.toEuroInput()} €", style = MaterialTheme.typography.bodyMedium)
        Text(document.status.name.lowercase(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (document.status.name == "DRAFT") Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            SecondaryButton("Editar", modifier = Modifier.weight(1f), fullWidth = true, onClick = onEdit)
            SecondaryButton("Archivar", modifier = Modifier.weight(1f), fullWidth = true, onClick = onArchive)
        }
    }
}

@Composable
private fun FinanceEntryCard(entry: BusinessFinanceEntry, onEdit: () -> Unit, onRecord: () -> Unit, onVoid: () -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    ClientPortalSurfaceCard {
        Text(entry.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("${entry.occurredOn} · ${entry.grossCents.toEuroInput()} €", style = MaterialTheme.typography.bodyMedium)
        Text(entry.status.name.lowercase(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (entry.status == BusinessFinanceEntryStatus.DRAFT) Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            SecondaryButton("Editar", modifier = Modifier.weight(1f), fullWidth = true, onClick = onEdit)
            PrimaryButton("Registrar", modifier = Modifier.weight(1f), fullWidth = true, onClick = onRecord)
        } else if (entry.status != BusinessFinanceEntryStatus.VOID) {
            SecondaryButton("Anular", fullWidth = true, onClick = onVoid)
        }
    }
}

private fun String.toQuantityMilliOrNull(): Long? {
    val normalized = trim().replace(',', '.')
    val pieces = normalized.split('.')
    if (pieces.size > 2 || pieces.first().toLongOrNull() == null) return null
    val fraction = pieces.getOrNull(1).orEmpty()
    if (fraction.length > 3 || fraction.any { !it.isDigit() }) return null
    val whole = pieces[0].toLongOrNull() ?: return null
    val milli = whole * 1_000L + fraction.padEnd(3, '0').toLongOrNull().orEmptyLong()
    return milli.takeIf { it > 0 }
}

private fun String.toCentsOrNull(): Long? {
    val normalized = trim().replace(',', '.')
    val pieces = normalized.split('.')
    if (pieces.size > 2 || pieces.first().toLongOrNull() == null) return null
    val fraction = pieces.getOrNull(1).orEmpty()
    if (fraction.length > 2 || fraction.any { !it.isDigit() }) return null
    val whole = pieces[0].toLongOrNull() ?: return null
    return (whole * 100L + fraction.padEnd(2, '0').toLongOrNull().orEmptyLong()).takeIf { it >= 0 }
}

private fun Long.toQuantityInput(): String = if (this % 1_000L == 0L) (this / 1_000L).toString() else "${this / 1_000L}.${(this % 1_000L).toString().padStart(3, '0').trimEnd('0')}"
private fun Long.toEuroInput(): String = "${this / 100L}.${(this % 100L).toString().padStart(2, '0')}"
private fun Long?.orEmptyLong(): Long = this ?: 0L
