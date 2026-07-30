package com.requena.supportdesk.app.client.screens.business.sales

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.requena.supportdesk.features.business.sales.domain.BusinessContact
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomer
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerActivity
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerActivityType
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerDetail
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerStage
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerTag
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerActivityInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerTagInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCustomerActivityInput
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
        onSetFilters = viewModel::setFilters,
        onCreateCustomer = viewModel::createCustomer,
        onUpdateCustomer = viewModel::updateCustomer,
        onArchiveCustomer = viewModel::archiveCustomer,
        onRestoreCustomer = viewModel::restoreCustomer,
        onCreateContact = viewModel::createContact,
        onUpdateContact = viewModel::updateContact,
        onCreateTag = viewModel::createTag,
        onCreateActivity = viewModel::createActivity,
        onUpdateActivity = viewModel::updateActivity,
        modifier = modifier,
    )
}

@Composable
private fun BusinessCustomersScreen(
    state: BusinessCustomersUiState,
    onRefresh: () -> Unit,
    onSelect: (String) -> Unit,
    onSetFilters: (String, String?, BusinessCustomerStage?, String?) -> Unit,
    onCreateCustomer: (CreateBusinessCustomerInput) -> Unit,
    onUpdateCustomer: (String, UpdateBusinessCustomerInput) -> Unit,
    onArchiveCustomer: (String, Int) -> Unit,
    onRestoreCustomer: (String, Int) -> Unit,
    onCreateContact: (String, CreateBusinessContactInput) -> Unit,
    onUpdateContact: (String, String, UpdateBusinessContactInput) -> Unit,
    onCreateTag: (CreateBusinessCustomerTagInput) -> Unit,
    onCreateActivity: (String, CreateBusinessCustomerActivityInput) -> Unit,
    onUpdateActivity: (String, String, UpdateBusinessCustomerActivityInput) -> Unit,
    modifier: Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        ClientPortalPageHeader(
            "Clientes y contactos",
            "CRM comercial privado con fichas, etiquetas, contactos y seguimiento. No incluye credenciales, tickets, SLA ni facturas.",
        )
        state.errorMessage?.let { ErrorState(message = it, onRetry = onRefresh) }
        if (state.isLoading && state.customers.isEmpty()) LoadingState()
        CustomerFilters(state, onSetFilters)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < SupportDeskBreakpoints.clientWide) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    CustomerCreateCard(state.isSaving, onCreateCustomer)
                    TagCreateCard(state.tags, state.isSaving, onCreateTag)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    Box(Modifier.weight(1f)) { CustomerCreateCard(state.isSaving, onCreateCustomer) }
                    Box(Modifier.weight(1f)) { TagCreateCard(state.tags, state.isSaving, onCreateTag) }
                }
            }
        }
        if (state.customers.isEmpty() && !state.isLoading) {
            EmptyState(
                "Todavia no hay clientes comerciales",
                "Crea el primero o cambia los filtros.",
                actionText = "Actualizar",
                onAction = onRefresh,
            )
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < SupportDeskBreakpoints.clientWide) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                        CustomerList(state.customers, onSelect)
                        state.selectedCustomer?.let {
                            CustomerDetailCard(
                                it, state.tags, state.isSaving, onUpdateCustomer, onArchiveCustomer, onRestoreCustomer,
                                onCreateContact, onUpdateContact, onCreateActivity, onUpdateActivity,
                            )
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                        Column(Modifier.weight(0.42f), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            CustomerList(state.customers, onSelect)
                        }
                        Column(Modifier.weight(0.58f)) {
                            state.selectedCustomer?.let {
                                CustomerDetailCard(
                                    it, state.tags, state.isSaving, onUpdateCustomer, onArchiveCustomer, onRestoreCustomer,
                                    onCreateContact, onUpdateContact, onCreateActivity, onUpdateActivity,
                                )
                            } ?: EmptyState("Selecciona un cliente", "Su ficha y cronologia apareceran aqui.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerFilters(
    state: BusinessCustomersUiState,
    onSetFilters: (String, String?, BusinessCustomerStage?, String?) -> Unit,
) {
    var query by remember(state.query) { mutableStateOf(state.query) }
    ClientPortalSurfaceCard {
        Text("Buscar y filtrar", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Nombre comercial") }, singleLine = true)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Todos", onClick = { onSetFilters(query, null, null, null) })
            SecondaryButton("Activos", onClick = { onSetFilters(query, "ACTIVE", state.stage, state.tagId) })
            SecondaryButton("Archivados", onClick = { onSetFilters(query, "ARCHIVED", state.stage, state.tagId) })
            BusinessCustomerStage.entries.forEach { stage ->
                SecondaryButton(stage.label(), onClick = { onSetFilters(query, state.status, stage, state.tagId) })
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.tags.filterNot(BusinessCustomerTag::archived).forEach { tag ->
                SecondaryButton(tag.name, onClick = { onSetFilters(query, state.status, state.stage, tag.id) })
            }
        }
        PrimaryButton("Aplicar", onClick = { onSetFilters(query, state.status, state.stage, state.tagId) })
    }
}

@Composable
private fun CustomerCreateCard(isSaving: Boolean, onCreate: (CreateBusinessCustomerInput) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    ClientPortalSurfaceCard {
        Text("Nuevo cliente comercial", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre o razon social") }, enabled = !isSaving)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email opcional") }, enabled = !isSaving)
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Telefono opcional") }, enabled = !isSaving)
        PrimaryButton(
            "Crear cliente",
            onClick = {
                onCreate(CreateBusinessCustomerInput(name, email = email.ifBlank { null }, phone = phone.ifBlank { null }))
                name = ""; email = ""; phone = ""
            },
            enabled = name.isNotBlank(),
            isLoading = isSaving,
            fullWidth = true,
        )
    }
}

@Composable
private fun TagCreateCard(
    tags: List<BusinessCustomerTag>,
    isSaving: Boolean,
    onCreate: (CreateBusinessCustomerTagInput) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#5B8DEF") }
    ClientPortalSurfaceCard {
        Text("Etiquetas privadas", style = MaterialTheme.typography.titleMedium)
        Text(tags.filterNot(BusinessCustomerTag::archived).joinToString(" · ") { it.name }.ifBlank { "Sin etiquetas" })
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, enabled = !isSaving)
        OutlinedTextField(color, { color = it }, Modifier.fillMaxWidth(), label = { Text("Color #RRGGBB") }, enabled = !isSaving)
        SecondaryButton(
            "Crear etiqueta",
            onClick = { onCreate(CreateBusinessCustomerTagInput(name, color)); name = "" },
            enabled = name.isNotBlank() && color.matches(Regex("#[0-9A-Fa-f]{6}")) && !isSaving,
            fullWidth = true,
        )
    }
}

@Composable
private fun CustomerList(customers: List<BusinessCustomer>, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        customers.forEach { customer ->
            ClientPortalSurfaceCard(Modifier.semantics { contentDescription = "Cliente comercial ${customer.displayName}" }) {
                Text(customer.displayName, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${if (customer.status == BusinessCustomerStatus.ACTIVE) "Activo" else "Archivado"} · ${customer.stage.label()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (customer.tags.isNotEmpty()) Text(customer.tags.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodySmall)
                customer.nextAction?.dueAt?.let { Text("Proxima accion: ${it.take(16)}", style = MaterialTheme.typography.bodySmall) }
                SecondaryButton("Ver ficha", onClick = { onSelect(customer.id) }, fullWidth = true)
            }
        }
    }
}

@Composable
private fun CustomerDetailCard(
    detail: BusinessCustomerDetail,
    availableTags: List<BusinessCustomerTag>,
    isSaving: Boolean,
    onUpdate: (String, UpdateBusinessCustomerInput) -> Unit,
    onArchive: (String, Int) -> Unit,
    onRestore: (String, Int) -> Unit,
    onCreateContact: (String, CreateBusinessContactInput) -> Unit,
    onUpdateContact: (String, String, UpdateBusinessContactInput) -> Unit,
    onCreateActivity: (String, CreateBusinessCustomerActivityInput) -> Unit,
    onUpdateActivity: (String, String, UpdateBusinessCustomerActivityInput) -> Unit,
) {
    val customer = detail.customer
    var name by remember(customer.id, customer.version) { mutableStateOf(customer.displayName) }
    var email by remember(customer.id, customer.version) { mutableStateOf(customer.email.orEmpty()) }
    var phone by remember(customer.id, customer.version) { mutableStateOf(customer.phone.orEmpty()) }
    var source by remember(customer.id, customer.version) { mutableStateOf(customer.source.orEmpty()) }
    var stage by remember(customer.id, customer.version) { mutableStateOf(customer.stage) }
    var selectedTagIds by remember(customer.id, customer.version) { mutableStateOf(customer.tags.map { it.id }.toSet()) }
    var confirmArchive by remember { mutableStateOf(false) }
    ClientPortalSurfaceCard {
        Text("Ficha comercial", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, enabled = !isSaving)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, enabled = !isSaving)
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Telefono") }, enabled = !isSaving)
        OutlinedTextField(source, { source = it }, Modifier.fillMaxWidth(), label = { Text("Origen comercial") }, enabled = !isSaving)
        Text("Etapa: ${stage.label()}", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BusinessCustomerStage.entries.forEach { value -> SecondaryButton(value.label(), onClick = { stage = value }) }
        }
        Text("Etiquetas", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            availableTags.filterNot(BusinessCustomerTag::archived).forEach { tag ->
                SecondaryButton(
                    if (tag.id in selectedTagIds) "✓ ${tag.name}" else tag.name,
                    onClick = { selectedTagIds = if (tag.id in selectedTagIds) selectedTagIds - tag.id else selectedTagIds + tag.id },
                )
            }
        }
        PrimaryButton(
            "Guardar ficha",
            onClick = {
                onUpdate(
                    customer.id,
                    UpdateBusinessCustomerInput(
                        displayName = name,
                        taxId = customer.taxId,
                        email = email.ifBlank { null },
                        phone = phone.ifBlank { null },
                        address = customer.address,
                        expectedVersion = customer.version,
                        type = customer.type,
                        stage = stage,
                        source = source.ifBlank { null },
                        tagIds = selectedTagIds.toList(),
                    ),
                )
            },
            isLoading = isSaving,
            fullWidth = true,
        )
        if (customer.status == BusinessCustomerStatus.ACTIVE) {
            SecondaryButton("Archivar cliente", onClick = { confirmArchive = true }, enabled = !isSaving, fullWidth = true)
        } else {
            SecondaryButton("Restaurar cliente", onClick = { onRestore(customer.id, customer.version) }, enabled = !isSaving, fullWidth = true)
        }
        Text("Contactos", style = MaterialTheme.typography.titleSmall)
        detail.contacts.forEach { ContactEditor(customer.id, it, isSaving, onUpdateContact) }
        ContactCreateForm(customer.id, isSaving, onCreateContact)
        Text("Actividad y proxima accion", style = MaterialTheme.typography.titleSmall)
        ActivityCreateForm(customer.id, isSaving, onCreateActivity)
        detail.activities.forEach { ActivityCard(customer.id, it, isSaving, onUpdateActivity) }
    }
    ConfirmDialog(
        visible = confirmArchive,
        title = "¿Archivar cliente?",
        message = "La ficha y su actividad se conservaran y podras restaurarla.",
        confirmText = "Archivar",
        onConfirm = { confirmArchive = false; onArchive(customer.id, customer.version) },
        onDismiss = { confirmArchive = false },
    )
}

@Composable
private fun ContactCreateForm(
    customerId: String,
    isSaving: Boolean,
    onCreate: (String, CreateBusinessContactInput) -> Unit,
) {
    var name by remember(customerId) { mutableStateOf("") }
    var role by remember(customerId) { mutableStateOf("") }
    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre del contacto") }, enabled = !isSaving)
    OutlinedTextField(role, { role = it }, Modifier.fillMaxWidth(), label = { Text("Cargo opcional") }, enabled = !isSaving)
    SecondaryButton(
        "Añadir contacto",
        onClick = { onCreate(customerId, CreateBusinessContactInput(name, role = role.ifBlank { null })); name = ""; role = "" },
        enabled = name.isNotBlank() && !isSaving,
        fullWidth = true,
    )
}

@Composable
private fun ContactEditor(
    customerId: String,
    contact: BusinessContact,
    isSaving: Boolean,
    onUpdate: (String, String, UpdateBusinessContactInput) -> Unit,
) {
    var name by remember(contact.id, contact.version) { mutableStateOf(contact.fullName) }
    var role by remember(contact.id, contact.version) { mutableStateOf(contact.role.orEmpty()) }
    Text(if (contact.isPrimary) "Contacto principal" else "Contacto", style = MaterialTheme.typography.labelLarge)
    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, enabled = !isSaving)
    OutlinedTextField(role, { role = it }, Modifier.fillMaxWidth(), label = { Text("Cargo") }, enabled = !isSaving)
    SecondaryButton(
        "Guardar contacto",
        onClick = {
            onUpdate(
                customerId,
                contact.id,
                UpdateBusinessContactInput(
                    fullName = name,
                    role = role.ifBlank { null },
                    email = contact.email,
                    phone = contact.phone,
                    isPrimary = contact.isPrimary,
                    status = contact.status,
                    expectedVersion = contact.version,
                ),
            )
        },
        enabled = name.isNotBlank() && !isSaving,
        fullWidth = true,
    )
}

@Composable
private fun ActivityCreateForm(
    customerId: String,
    isSaving: Boolean,
    onCreate: (String, CreateBusinessCustomerActivityInput) -> Unit,
) {
    var subject by remember(customerId) { mutableStateOf("") }
    var details by remember(customerId) { mutableStateOf("") }
    var dueAt by remember(customerId) { mutableStateOf("") }
    var type by remember(customerId) { mutableStateOf(BusinessCustomerActivityType.NOTE) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton("Nota", onClick = { type = BusinessCustomerActivityType.NOTE })
        SecondaryButton("Seguimiento", onClick = { type = BusinessCustomerActivityType.FOLLOW_UP })
    }
    OutlinedTextField(subject, { subject = it }, Modifier.fillMaxWidth(), label = { Text("Asunto") }, enabled = !isSaving)
    OutlinedTextField(details, { details = it }, Modifier.fillMaxWidth(), label = { Text("Detalle privado") }, enabled = !isSaving)
    if (type == BusinessCustomerActivityType.FOLLOW_UP) {
        OutlinedTextField(dueAt, { dueAt = it }, Modifier.fillMaxWidth(), label = { Text("Vence, p. ej. 2026-08-15T09:00:00Z") }, enabled = !isSaving)
    }
    SecondaryButton(
        "Registrar actividad",
        onClick = {
            onCreate(
                customerId,
                CreateBusinessCustomerActivityInput(
                    type = type,
                    subject = subject,
                    details = details.ifBlank { null },
                    occurredAt = null,
                    dueAt = dueAt.ifBlank { null },
                ),
            )
            subject = ""; details = ""; dueAt = ""
        },
        enabled = subject.isNotBlank() && (type == BusinessCustomerActivityType.NOTE || dueAt.isNotBlank()) && !isSaving,
        fullWidth = true,
    )
}

@Composable
private fun ActivityCard(
    customerId: String,
    activity: BusinessCustomerActivity,
    isSaving: Boolean,
    onUpdate: (String, String, UpdateBusinessCustomerActivityInput) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(activity.subject, style = MaterialTheme.typography.titleSmall)
            activity.details?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(
                listOfNotNull(activity.type.name, activity.dueAt?.let { "Vence ${it.take(16)}" }, activity.completedAt?.let { "Completada" }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
            )
            if (activity.dueAt != null && activity.completedAt == null) {
                SecondaryButton(
                    "Marcar completada",
                    onClick = {
                        onUpdate(
                            customerId,
                            activity.id,
                            UpdateBusinessCustomerActivityInput(
                                contactId = activity.contactId,
                                type = activity.type,
                                subject = activity.subject,
                                details = activity.details,
                                occurredAt = activity.occurredAt,
                                dueAt = activity.dueAt,
                                completed = true,
                                expectedVersion = activity.version,
                            ),
                        )
                    },
                    enabled = !isSaving,
                )
            }
        }
    }
}

private fun BusinessCustomerStage.label() = when (this) {
    BusinessCustomerStage.LEAD -> "Lead"
    BusinessCustomerStage.PROSPECT -> "Prospecto"
    BusinessCustomerStage.CUSTOMER -> "Cliente"
    BusinessCustomerStage.INACTIVE -> "Inactivo"
}
