package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.ClientBillingProfile
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.clients.domain.model.clientBillingEmailProblem
import com.requena.supportdesk.features.clients.domain.model.clientTaxIdProblem
import com.requena.supportdesk.features.clients.presentation.event.ClientsUiEvent

private val EmailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

// Editing keeps the client's existing accountStatus/serviceTier/preferredContactChannel
// unchanged — this dialog only covers the fields the reference design shows plus fiscal data.
@Composable
fun AddEditClientDialog(
    client: Client?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onEvent: (ClientsUiEvent) -> Unit,
) {
    var companyName by rememberSaveable(client?.id) { mutableStateOf(client?.companyName.orEmpty()) }
    var productName by rememberSaveable(client?.id) { mutableStateOf(client?.productName.orEmpty()) }
    var contactName by rememberSaveable(client?.id) { mutableStateOf(client?.contactName.orEmpty()) }
    var email by rememberSaveable(client?.id) { mutableStateOf(client?.email.orEmpty()) }
    val billing = client?.billing ?: ClientBillingProfile()
    var legalName by rememberSaveable(client?.id) { mutableStateOf(billing.legalName) }
    var taxId by rememberSaveable(client?.id) { mutableStateOf(billing.taxId) }
    var addressLine by rememberSaveable(client?.id) { mutableStateOf(billing.addressLine) }
    var postalCode by rememberSaveable(client?.id) { mutableStateOf(billing.postalCode) }
    var city by rememberSaveable(client?.id) { mutableStateOf(billing.city) }
    var province by rememberSaveable(client?.id) { mutableStateOf(billing.province) }
    var country by rememberSaveable(client?.id) { mutableStateOf(billing.country) }
    var billingEmail by rememberSaveable(client?.id) { mutableStateOf(billing.email) }

    val emailIsValid = email.matches(EmailPattern)
    val taxIdProblem = clientTaxIdProblem(taxId)
    val billingEmailProblem = clientBillingEmailProblem(billingEmail)
    val isValid = companyName.isNotBlank() && productName.isNotBlank() && contactName.isNotBlank() &&
        emailIsValid && taxIdProblem == null && billingEmailProblem == null

    fun billingProfile() = ClientBillingProfile(
        legalName = legalName,
        taxId = taxId,
        addressLine = addressLine,
        postalCode = postalCode,
        city = city,
        province = province,
        country = country,
        email = billingEmail,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (client == null) "Nuevo cliente" else "Editar cliente",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Empresa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Producto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Contacto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    isError = email.isNotBlank() && !emailIsValid,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Datos fiscales",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Opcionales. Se usan para rellenar las facturas de este cliente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = legalName,
                    onValueChange = { legalName = it },
                    label = { Text("Razón social") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = taxId,
                    onValueChange = { taxId = it },
                    label = { Text("NIF/CIF") },
                    singleLine = true,
                    isError = taxIdProblem != null,
                    supportingText = taxIdProblem?.let { problem -> { Text(problem) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = addressLine,
                    onValueChange = { addressLine = it },
                    label = { Text("Dirección") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = postalCode,
                        onValueChange = { postalCode = it },
                        label = { Text("C. postal") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Población") },
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = province,
                        onValueChange = { province = it },
                        label = { Text("Provincia") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("País") },
                        placeholder = { Text("España") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = billingEmail,
                    onValueChange = { billingEmail = it },
                    label = { Text("Email de facturación") },
                    singleLine = true,
                    isError = billingEmailProblem != null,
                    supportingText = billingEmailProblem?.let { problem -> { Text(problem) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (client == null) "Crear cliente" else "Guardar cambios",
                enabled = isValid && !isLoading,
                isLoading = isLoading,
                onClick = {
                    if (client == null) {
                        onEvent(
                            ClientsUiEvent.CreateClient(
                                companyName = companyName,
                                productName = productName,
                                contactName = contactName,
                                email = email,
                                billing = billingProfile(),
                            ),
                        )
                    } else {
                        onEvent(
                            ClientsUiEvent.UpdateClient(
                                clientId = client.id,
                                companyName = companyName,
                                productName = productName,
                                contactName = contactName,
                                email = email,
                                accountStatus = client.accountStatus,
                                serviceTier = client.serviceTier,
                                preferredContactChannel = client.preferredContactChannel,
                                billing = billingProfile(),
                            ),
                        )
                    }
                },
            )
        },
        dismissButton = {
            SecondaryButton(text = "Cancelar", onClick = onDismiss)
        },
    )
}
