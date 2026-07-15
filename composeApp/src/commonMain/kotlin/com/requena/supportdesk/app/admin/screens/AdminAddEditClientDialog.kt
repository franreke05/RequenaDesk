package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.clients.presentation.event.ClientsUiEvent

private val EmailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

// Editing keeps the client's existing accountStatus/serviceTier/preferredContactChannel
// unchanged — this dialog only covers the fields the reference design shows.
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
    val emailIsValid = email.matches(EmailPattern)
    val isValid = companyName.isNotBlank() && productName.isNotBlank() && contactName.isNotBlank() && emailIsValid

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
            Column(verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm)) {
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Empresa") },
                    singleLine = true,
                    modifier = Modifier,
                )
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Producto") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Contacto") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    isError = email.isNotBlank() && !emailIsValid,
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
