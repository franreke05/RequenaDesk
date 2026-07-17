package com.requena.supportdesk.app.client.screens.business.sales

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.app.client.components.ClientPortalSurfaceCard
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.ErrorState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.business.sales.domain.BusinessQuoteStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessQuoteLineInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.presentation.BusinessQuotesUiState
import com.requena.supportdesk.features.business.sales.presentation.BusinessQuotesViewModel

@Composable
fun BusinessQuotesRoute(viewModel: BusinessQuotesViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    BusinessQuotesScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onSelect = viewModel::selectQuote,
        onCreate = viewModel::createQuote,
        onUpdate = viewModel::updateQuote,
        onTransition = viewModel::transitionQuote,
        onConvert = viewModel::convertQuote,
        modifier = modifier,
    )
}

@Composable
fun BusinessQuotesScreen(
    state: BusinessQuotesUiState,
    onRefresh: () -> Unit,
    onSelect: (String) -> Unit,
    onCreate: (CreateBusinessQuoteInput) -> Unit,
    onUpdate: (String, UpdateBusinessQuoteInput) -> Unit,
    onTransition: (String, BusinessQuoteStatus, Int) -> Unit,
    onConvert: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        ClientPortalPageHeader("Presupuestos y ventas", "Documentos comerciales internos. No son facturas, contratos, cobros ni documentos fiscales.")
        state.errorMessage?.let { ErrorState(message = it, onRetry = onRefresh) }
        QuoteCreateCard(state.isSaving, onCreate)
        if (state.isLoading && state.quotes.isEmpty()) LoadingState()
        if (state.quotes.isEmpty() && !state.isLoading) EmptyState("TodavÃ­a no hay presupuestos", "Crea un borrador para calcular y seguir una propuesta comercial.")
        state.quotes.forEach { quote ->
            ClientPortalSurfaceCard(Modifier.semantics { contentDescription = "Presupuesto ${quote.number} para ${quote.buyerName}" }) {
                Text("${quote.number} Â· ${quote.buyerName}", style = MaterialTheme.typography.titleMedium)
                Text("${quote.status.name.lowercase()} Â· ${formatCents(quote.totalCents)} EUR", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    SecondaryButton("Ver", onClick = { onSelect(quote.id) })
                    if (quote.status == BusinessQuoteStatus.DRAFT) PrimaryButton("Marcar enviado", onClick = { onTransition(quote.id, BusinessQuoteStatus.SENT, quote.version) })
                    if (quote.status == BusinessQuoteStatus.SENT) {
                        PrimaryButton("Aceptar", onClick = { onTransition(quote.id, BusinessQuoteStatus.ACCEPTED, quote.version) })
                        SecondaryButton("Rechazar", onClick = { onTransition(quote.id, BusinessQuoteStatus.REJECTED, quote.version) })
                    }
                    if (quote.status == BusinessQuoteStatus.ACCEPTED) PrimaryButton("Convertir en venta", onClick = { onConvert(quote.id, newIdempotencyKey(quote.id)) }, isLoading = state.isSaving)
                }
            }
        }
        state.selectedQuote?.let { quote ->
            ClientPortalSurfaceCard {
                Text("Detalle ${quote.number}", style = MaterialTheme.typography.titleMedium)
                quote.lines.forEach { line -> Text("${line.description}: ${formatMilli(line.quantityMilli)} Ã— ${formatCents(line.unitPriceCents)} EUR") }
                Text("Total calculado por servidor: ${formatCents(quote.totalCents)} EUR", style = MaterialTheme.typography.titleSmall)
                if (quote.status == BusinessQuoteStatus.DRAFT) {
                    QuoteDraftEditor(quote, state.isSaving, onUpdate)
                }
            }
        }
        if (state.sales.isNotEmpty()) {
            Text("Ventas confirmadas", style = MaterialTheme.typography.titleMedium)
            state.sales.forEach { sale -> Text("${sale.number} Â· ${sale.buyerName} Â· ${formatCents(sale.totalCents)} EUR") }
        }
    }
}

@Composable
private fun QuoteDraftEditor(
    quote: com.requena.supportdesk.features.business.sales.domain.BusinessQuote,
    isSaving: Boolean,
    onUpdate: (String, UpdateBusinessQuoteInput) -> Unit,
) {
    var buyer by remember(quote.id, quote.version) { mutableStateOf(quote.buyerName) }
    var email by remember(quote.id, quote.version) { mutableStateOf(quote.buyerEmail.orEmpty()) }
    var phone by remember(quote.id, quote.version) { mutableStateOf(quote.buyerPhone.orEmpty()) }
    var issueDate by remember(quote.id, quote.version) { mutableStateOf(quote.issueDate) }
    var validUntil by remember(quote.id, quote.version) { mutableStateOf(quote.validUntil.orEmpty()) }
    var notes by remember(quote.id, quote.version) { mutableStateOf(quote.notes.orEmpty()) }
    Text("Editar borrador", style = MaterialTheme.typography.titleSmall)
    OutlinedTextField(buyer, { buyer = it }, Modifier.fillMaxWidth(), label = { Text("Comprador") }, enabled = !isSaving)
    OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, enabled = !isSaving)
    OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Teléfono") }, enabled = !isSaving)
    OutlinedTextField(issueDate, { issueDate = it }, Modifier.fillMaxWidth(), label = { Text("Fecha de emisión (AAAA-MM-DD)") }, enabled = !isSaving)
    OutlinedTextField(validUntil, { validUntil = it }, Modifier.fillMaxWidth(), label = { Text("Válido hasta (opcional)") }, enabled = !isSaving)
    OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Notas (opcionales)") }, enabled = !isSaving)
    PrimaryButton(
        "Guardar borrador",
        fullWidth = true,
        isLoading = isSaving,
        enabled = buyer.isNotBlank() && issueDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")),
        onClick = {
            onUpdate(
                quote.id,
                UpdateBusinessQuoteInput(
                    customerId = quote.customerId,
                    buyerName = buyer,
                    buyerEmail = email.ifBlank { null },
                    buyerPhone = phone.ifBlank { null },
                    issueDate = issueDate,
                    validUntil = validUntil.ifBlank { null },
                    notes = notes.ifBlank { null },
                    lines = quote.lines.map { line ->
                        BusinessQuoteLineInput(
                            position = line.position,
                            sourceCatalogItemId = line.sourceCatalogItemId,
                            description = line.description,
                            quantityMilli = line.quantityMilli,
                            unitPriceCents = line.unitPriceCents,
                            discountBasisPoints = line.discountBasisPoints,
                            taxBasisPoints = line.taxBasisPoints,
                        )
                    },
                    expectedVersion = quote.version,
                ),
            )
        },
    )
}

@Composable
private fun QuoteCreateCard(isSaving: Boolean, onCreate: (CreateBusinessQuoteInput) -> Unit) {
    var buyer by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantityMilli by remember { mutableStateOf("1000") }
    var priceCents by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf("") }
    ClientPortalSurfaceCard {
        Text("Nuevo borrador", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(buyer, { buyer = it }, Modifier.fillMaxWidth(), label = { Text("Comprador") }, enabled = !isSaving)
        OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("DescripciÃ³n de lÃ­nea") }, enabled = !isSaving)
        OutlinedTextField(issueDate, { issueDate = it }, Modifier.fillMaxWidth(), label = { Text("Fecha de emisiÃ³n (AAAA-MM-DD)") }, enabled = !isSaving)
        OutlinedTextField(quantityMilli, { quantityMilli = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Cantidad en milÃ©simas") }, enabled = !isSaving)
        OutlinedTextField(priceCents, { priceCents = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Precio unitario en cÃ©ntimos") }, enabled = !isSaving)
        PrimaryButton("Crear borrador", fullWidth = true, isLoading = isSaving, enabled = buyer.isNotBlank() && description.isNotBlank() && issueDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && quantityMilli.toLongOrNull()?.let { it > 0 } == true && priceCents.toLongOrNull() != null, onClick = {
            onCreate(
                CreateBusinessQuoteInput(
                    buyerName = buyer,
                    issueDate = issueDate,
                    lines = listOf(BusinessQuoteLineInput(1, description = description, quantityMilli = requireNotNull(quantityMilli.toLongOrNull()), unitPriceCents = requireNotNull(priceCents.toLongOrNull()))),
                    idempotencyKey = newIdempotencyKey("quote"),
                ),
            )
            buyer = ""; description = ""; issueDate = ""; priceCents = ""
        })
    }
}

private fun formatCents(value: Long): String = "${value / 100},${(value % 100).toString().padStart(2, '0')}"
private fun formatMilli(value: Long): String = "${value / 1000},${(value % 1000).toString().padStart(3, '0')}"
private fun newIdempotencyKey(scope: String): String = "ui-$scope-${kotlin.random.Random.nextLong().toString().replace('-', 'n')}"
