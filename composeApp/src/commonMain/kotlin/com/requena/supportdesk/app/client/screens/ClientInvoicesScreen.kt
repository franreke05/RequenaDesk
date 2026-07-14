package com.requena.supportdesk.app.client.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.invoices.domain.model.Invoice
import com.requena.supportdesk.features.invoices.domain.model.InvoiceStatus
import com.requena.supportdesk.features.invoices.presentation.event.InvoicesUiEvent
import com.requena.supportdesk.features.invoices.presentation.state.InvoicesUiState
import com.requena.supportdesk.core.utils.toFixedString

// ── INVOICES ──────────────────────────────────────────────────────────────────

@Composable
fun ClientInvoicesScreen(
    state: InvoicesUiState,
    onEvent: (InvoicesUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var selectedInvoice by remember { mutableStateOf<Invoice?>(null) }

    val visibleInvoices = remember(state.invoices) {
        state.invoices.filter { it.status == InvoiceStatus.SENT || it.status == InvoiceStatus.PAID }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        if (state.isLoading) {
            LoadingState()
        } else if (visibleInvoices.isEmpty()) {
            EmptyState(
                title = "Sin facturas",
                message = "Aún no tienes facturas emitidas.",
            )
        } else {
            SectionCard(title = "Mis facturas", subtitle = "Facturas emitidas y pagadas") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    visibleInvoices.forEach { invoice ->
                        val semantic = SupportDeskThemeTokens.semanticColors
                        val (bg, fg) = when (invoice.status) {
                            InvoiceStatus.SENT -> semantic.infoContainer to semantic.info
                            InvoiceStatus.PAID -> semantic.successContainer to semantic.success
                            else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            onClick = {
                                selectedInvoice = if (selectedInvoice?.id == invoice.id) null else invoice
                                onEvent(InvoicesUiEvent.SelectInvoice(invoice.id))
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedInvoice?.id == invoice.id)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(spacing.md).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(invoice.invoiceNumber, style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(invoice.issuedAt, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    SupportDeskBadge(
                                        text = if (invoice.status == InvoiceStatus.PAID) "Pagada" else "Pendiente",
                                        containerColor = bg, contentColor = fg,
                                    )
                                    Text("$${invoice.total.toFixedString(2)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            selectedInvoice?.let { invoice ->
                SectionCard(title = invoice.invoiceNumber, subtitle = "Emitida: ${invoice.issuedAt}") {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        invoice.items.sortedBy { it.sortOrder }.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(item.description, style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f))
                                Text("$${item.subtotal.toFixedString(2)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(Modifier.height(spacing.xs))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("$${invoice.total.toFixedString(2)}", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        invoice.notes?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        PrimaryButton(
                            text = "Descargar PDF",
                            onClick = { onEvent(InvoicesUiEvent.DownloadPdf(invoice.id)) },
                            fullWidth = true,
                        )
                    }
                }
            }
        }
    }
}
