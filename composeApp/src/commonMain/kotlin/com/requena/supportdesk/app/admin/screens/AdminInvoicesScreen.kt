package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.core.utils.toFixedString
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.layout.InfoRow
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceItemInput
import com.requena.supportdesk.features.invoices.presentation.event.InvoicesUiEvent
import com.requena.supportdesk.features.invoices.presentation.state.InvoicesUiState

// ─── Entry point: invoices are generated on demand for download, never persisted ──

@Composable
fun AdminInvoicesScreen(
    clients: List<Client>,
    state: InvoicesUiState,
    onEvent: (InvoicesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing

    data class ItemDraft(val description: String = "", val quantity: String = "1", val unitPrice: String = "")

    var selectedClientId by remember { mutableStateOf("") }
    var selectedClientName by remember { mutableStateOf("Seleccionar cliente") }
    var clientDropdownExpanded by remember { mutableStateOf(false) }
    var issuedAt by remember { mutableStateOf(currentIsoDate()) }
    var dueAt by remember { mutableStateOf("") }
    var taxPercent by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf(ItemDraft()) }

    val subtotal = items.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
    val tax = subtotal * ((taxPercent.toDoubleOrNull() ?: 0.0) / 100.0)
    val total = subtotal + tax
    val isValid = selectedClientId.isNotBlank() && issuedAt.isNotBlank() &&
        (taxPercent.toDoubleOrNull() ?: -1.0) >= 0.0 &&
        items.isNotEmpty() && items.all {
            it.description.isNotBlank() &&
                (it.quantity.toDoubleOrNull() ?: 0.0) > 0.0 &&
                (it.unitPrice.toDoubleOrNull() ?: 0.0) > 0.0
        }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        SectionCard(title = "Datos generales", subtitle = "Cliente, fechas e impuesto") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Box {
                    OutlinedTextField(
                        value = selectedClientName, onValueChange = {}, readOnly = true,
                        label = { Text("Cliente") },
                        modifier = Modifier.fillMaxWidth().clickable { clientDropdownExpanded = true },
                    )
                    DropdownMenu(expanded = clientDropdownExpanded, onDismissRequest = { clientDropdownExpanded = false }) {
                        clients.forEach { client ->
                            DropdownMenuItem(text = { Text(client.companyName) }, onClick = {
                                selectedClientId = client.id
                                selectedClientName = client.companyName
                                clientDropdownExpanded = false
                            })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    OutlinedTextField(value = issuedAt, onValueChange = { issuedAt = it },
                        label = { Text("Emisión (YYYY-MM-DD)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = dueAt, onValueChange = { dueAt = it },
                        label = { Text("Vencimiento (YYYY-MM-DD)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = taxPercent, onValueChange = { taxPercent = it },
                    label = { Text("Impuesto (%)") }, modifier = Modifier.fillMaxWidth(0.4f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Notas") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
            }
        }

        SectionCard(title = "Líneas de factura", subtitle = "Descripción, cantidad y precio unitario") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                items.forEachIndexed { index, item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = item.description, onValueChange = { items[index] = item.copy(description = it) },
                            label = { Text("Descripción") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = item.quantity, onValueChange = { items[index] = item.copy(quantity = it) },
                            label = { Text("Cant.") }, modifier = Modifier.width(72.dp), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(value = item.unitPrice, onValueChange = { items[index] = item.copy(unitPrice = it) },
                            label = { Text("Precio") }, modifier = Modifier.width(96.dp), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        SecondaryButton(text = "✕", onClick = { if (items.size > 1) items.removeAt(index) })
                    }
                }
                SecondaryButton(text = "+ Agregar línea", onClick = { items.add(ItemDraft()) }, fullWidth = true)
            }
        }

        SectionCard(title = "Resumen") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                InfoRow(label = "Subtotal", value = "$${subtotal.toFixedString(2)}")
                InfoRow(label = "Impuesto (${taxPercent}%)", value = "$${tax.toFixedString(2)}")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$${total.toFixedString(2)}", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), modifier = Modifier.fillMaxWidth()) {
            state.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            PrimaryButton(
                text = "Generar factura", enabled = isValid && !state.isGenerating,
                onClick = {
                    onEvent(InvoicesUiEvent.GenerateInvoice(CreateInvoiceInput(
                        clientId = selectedClientId, issuedAt = issuedAt,
                        dueAt = dueAt.ifBlank { null }, notes = notes.ifBlank { null },
                        taxPercent = taxPercent.toDoubleOrNull() ?: 0.0,
                        items = items.mapIndexed { idx, it ->
                            CreateInvoiceItemInput(description = it.description,
                                quantity = it.quantity.toDouble(),
                                unitPrice = it.unitPrice.toDouble(), sortOrder = idx)
                        },
                    )))
                },
                modifier = Modifier.weight(1f),
                isLoading = state.isGenerating,
            )
        }
    }
}
