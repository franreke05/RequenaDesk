package com.requena.supportdesk.app.client.screens.business.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.app.client.components.ClientPortalSurfaceCard
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.feedback.ConfirmDialog
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.ErrorState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomer
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerDetail
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerStatus
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.presentation.BusinessCustomersUiState
import com.requena.supportdesk.features.business.sales.presentation.BusinessCustomersViewModel

@Composable
fun BusinessCustomersRoute(
    viewModel: BusinessCustomersViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    BusinessCustomersScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onSelect = viewModel::selectCustomer,
        onCreateCustomer = viewModel::createCustomer,
        onUpdateCustomer = viewModel::updateCustomer,
        onArchiveCustomer = viewModel::archiveCustomer,
        onCreateContact = viewModel::createContact,
        onUpdateContact = viewModel::updateContact,
        modifier = modifier,
    )
}

@Composable
fun BusinessCustomersScreen(
    state: BusinessCustomersUiState,
    onRefresh: () -> Unit,
    onSelect: (String) -> Unit,
    onCreateCustomer: (CreateBusinessCustomerInput) -> Unit,
    onUpdateCustomer: (String, UpdateBusinessCustomerInput) -> Unit,
    onArchiveCustomer: (String, Int) -> Unit,
    onCreateContact: (String, CreateBusinessContactInput) -> Unit,
    onUpdateContact: (String, String, UpdateBusinessContactInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        ClientPortalPageHeader(
            title = "Clientes y contactos",
            subtitle = "Agenda comercial privada de tu empresa. La beta es gratuita y no genera facturas ni cobros.",
        )
        state.errorMessage?.let { ErrorState(message = it, onRetry = onRefresh) }
        if (state.isLoading && state.customers.isEmpty()) LoadingState()
        CustomerCreateCard(isSaving = state.isSaving, onCreate = onCreateCustomer)
        if (state.customers.isEmpty() && !state.isLoading) {
            EmptyState("TodavÃ­a no hay clientes comerciales", "Crea el primero para organizar contactos y presupuestos.", actionText = "Actualizar", onAction = onRefresh)
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < SupportDeskBreakpoints.clientWide) {
                    CustomerList(state.customers, onSelect)
                    state.selectedCustomer?.let { CustomerDetailCard(it, state.isSaving, onUpdateCustomer, onArchiveCustomer, onCreateContact, onUpdateContact) }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                        Column(modifier = Modifier.weight(0.45f), verticalArrangement = Arrangement.spacedBy(spacing.sm)) { CustomerList(state.customers, onSelect) }
                        Column(modifier = Modifier.weight(0.55f)) {
                            state.selectedCustomer?.let { CustomerDetailCard(it, state.isSaving, onUpdateCustomer, onArchiveCustomer, onCreateContact, onUpdateContact) }
                                ?: EmptyState("Selecciona un cliente", "El detalle y sus contactos aparecerÃ¡n aquÃ­.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerCreateCard(isSaving: Boolean, onCreate: (CreateBusinessCustomerInput) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    ClientPortalSurfaceCard {
        Text("Nuevo cliente comercial", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre o razÃ³n social") }, singleLine = true, enabled = !isSaving)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            OutlinedTextField(email, { email = it }, Modifier.weight(1f), label = { Text("Email (opcional)") }, singleLine = true, enabled = !isSaving)
            OutlinedTextField(phone, { phone = it }, Modifier.weight(1f), label = { Text("TelÃ©fono (opcional)") }, singleLine = true, enabled = !isSaving)
        }
        PrimaryButton(
            text = "Crear cliente",
            fullWidth = true,
            isLoading = isSaving,
            enabled = name.isNotBlank(),
            onClick = { onCreate(CreateBusinessCustomerInput(name, email = email.ifBlank { null }, phone = phone.ifBlank { null })); name = ""; email = ""; phone = "" },
        )
    }
}

@Composable
private fun CustomerList(customers: List<BusinessCustomer>, onSelect: (String) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        customers.forEach { customer ->
            ClientPortalSurfaceCard(Modifier.semantics { contentDescription = "Cliente comercial ${customer.displayName}" }) {
                Text(customer.displayName, style = MaterialTheme.typography.titleSmall)
                Text(
                    if (customer.status == BusinessCustomerStatus.ACTIVE) "Activo" else "Archivado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SecondaryButton("Ver detalle", onClick = { onSelect(customer.id) }, fullWidth = true)
            }
        }
    }
}

@Composable
private fun CustomerDetailCard(
    detail: BusinessCustomerDetail,
    isSaving: Boolean,
    onUpdate: (String, UpdateBusinessCustomerInput) -> Unit,
    onArchive: (String, Int) -> Unit,
    onCreateContact: (String, CreateBusinessContactInput) -> Unit,
    onUpdateContact: (String, String, UpdateBusinessContactInput) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val customer = detail.customer
    var name by remember(customer.id, customer.version) { mutableStateOf(customer.displayName) }
    var email by remember(customer.id, customer.version) { mutableStateOf(customer.email.orEmpty()) }
    var phone by remember(customer.id, customer.version) { mutableStateOf(customer.phone.orEmpty()) }
    var confirmArchive by remember { mutableStateOf(false) }
    ClientPortalSurfaceCard {
        Text("Detalle del cliente", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre o razÃ³n social") }, enabled = !isSaving)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, enabled = !isSaving)
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("TelÃ©fono") }, enabled = !isSaving)
        PrimaryButton("Guardar cambios", onClick = { onUpdate(customer.id, UpdateBusinessCustomerInput(name, email = email.ifBlank { null }, phone = phone.ifBlank { null }, expectedVersion = customer.version)) }, fullWidth = true, isLoading = isSaving)
        if (customer.status == BusinessCustomerStatus.ACTIVE) SecondaryButton("Archivar cliente", onClick = { confirmArchive = true }, fullWidth = true, enabled = !isSaving)
        Text("Contactos", style = MaterialTheme.typography.titleSmall)
        detail.contacts.forEach { contact -> ContactEditor(customer.id, contact, isSaving, onUpdateContact) }
        ContactCreateForm(customer.id, isSaving, onCreateContact)
    }
    ConfirmDialog(
        visible = confirmArchive,
        title = "Â¿Archivar cliente?",
        message = "Se conservarÃ¡n los presupuestos y ventas histÃ³ricos.",
        confirmText = "Archivar",
        onConfirm = { confirmArchive = false; onArchive(customer.id, customer.version) },
        onDismiss = { confirmArchive = false },
    )
}

@Composable
private fun ContactCreateForm(customerId: String, isSaving: Boolean, onCreate: (String, CreateBusinessContactInput) -> Unit) {
    var name by remember(customerId) { mutableStateOf("") }
    var role by remember(customerId) { mutableStateOf("") }
    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre del contacto") }, enabled = !isSaving)
    OutlinedTextField(role, { role = it }, Modifier.fillMaxWidth(), label = { Text("Cargo (opcional)") }, enabled = !isSaving)
    SecondaryButton("AÃ±adir contacto", fullWidth = true, enabled = name.isNotBlank() && !isSaving, onClick = { onCreate(customerId, CreateBusinessContactInput(name, role = role.ifBlank { null })); name = ""; role = "" })
}

@Composable
private fun ContactEditor(
    customerId: String,
    contact: com.requena.supportdesk.features.business.sales.domain.BusinessContact,
    isSaving: Boolean,
    onUpdate: (String, String, UpdateBusinessContactInput) -> Unit,
) {
    var name by remember(contact.id, contact.version) { mutableStateOf(contact.fullName) }
    var role by remember(contact.id, contact.version) { mutableStateOf(contact.role.orEmpty()) }
    var email by remember(contact.id, contact.version) { mutableStateOf(contact.email.orEmpty()) }
    var phone by remember(contact.id, contact.version) { mutableStateOf(contact.phone.orEmpty()) }
    Text(if (contact.isPrimary) "Contacto principal" else "Contacto", style = MaterialTheme.typography.labelLarge)
    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, enabled = !isSaving)
    OutlinedTextField(role, { role = it }, Modifier.fillMaxWidth(), label = { Text("Cargo") }, enabled = !isSaving)
    OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, enabled = !isSaving)
    OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Teléfono") }, enabled = !isSaving)
    SecondaryButton(
        "Guardar contacto",
        fullWidth = true,
        enabled = name.isNotBlank() && !isSaving,
        onClick = {
            onUpdate(
                customerId,
                contact.id,
                UpdateBusinessContactInput(
                    fullName = name,
                    role = role.ifBlank { null },
                    email = email.ifBlank { null },
                    phone = phone.ifBlank { null },
                    isPrimary = contact.isPrimary,
                    status = contact.status,
                    expectedVersion = contact.version,
                ),
            )
        },
    )
}
