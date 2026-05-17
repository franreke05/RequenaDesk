package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.ClientAccountStatus
import com.requena.supportdesk.core.model.ClientServiceTier
import com.requena.supportdesk.core.model.PreferredContactChannel
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.designsystem.components.badges.ClientAccountStatusBadge
import com.requena.supportdesk.designsystem.components.badges.ClientServiceTierBadge
import com.requena.supportdesk.designsystem.components.badges.PreferredContactChannelBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.ConfirmDialog
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.inputs.SearchField
import com.requena.supportdesk.designsystem.components.layout.InfoRow
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.features.clients.presentation.event.ClientsUiEvent
import com.requena.supportdesk.features.clients.presentation.state.ClientsUiState
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState

@Composable
fun AdminClientsScreen(
    state: ClientsUiState,
    tasksState: TasksUiState,
    onEvent: (ClientsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val selectedClient = state.selectedClient
    var showCreateClient by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        SearchField(
            value = state.query,
            onValueChange = { onEvent(ClientsUiEvent.SearchChanged(it)) },
            placeholder = "Buscar cliente, producto, contacto o correo",
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SecondaryButton(text = "Nuevo cliente", onClick = { showCreateClient = true })
        }

        if (showCreateClient) {
            CreateClientDialog(
                onDismiss = { showCreateClient = false },
                onCreate = { companyName, productName, contactName, email ->
                    onEvent(
                        ClientsUiEvent.CreateClient(
                            companyName = companyName,
                            productName = productName,
                            contactName = contactName,
                            email = email,
                        ),
                    )
                    showCreateClient = false
                },
            )
        }

        when {
            state.isLoading && state.clients.isEmpty() -> LoadingState(itemCount = 4)
            state.clients.isEmpty() -> EmptyState(
                title = "Sin clientes visibles",
                message = state.errorMessage ?: "No hay clientes que coincidan con la busqueda actual.",
            )

            else -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    val stacked = maxWidth < 1080.dp
                    if (stacked) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(spacing.lg),
                        ) {
                            ClientListPane(
                                clients = state.clients,
                                tasks = tasksState.tasks,
                                logs = tasksState.logs,
                                selectedClientId = selectedClient?.id,
                                onSelect = { onEvent(ClientsUiEvent.SelectClient(it)) },
                                modifier = Modifier.weight(0.44f),
                            )
                            ClientEditorPane(
                                client = selectedClient,
                                tasks = tasksState.tasks.filter { task -> task.clientId == selectedClient?.id },
                                logs = tasksState.logs.filter { log -> log.clientId == selectedClient?.id },
                                onEvent = onEvent,
                                modifier = Modifier.weight(0.56f),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                        ) {
                            ClientListPane(
                                clients = state.clients,
                                tasks = tasksState.tasks,
                                logs = tasksState.logs,
                                selectedClientId = selectedClient?.id,
                                onSelect = { onEvent(ClientsUiEvent.SelectClient(it)) },
                                modifier = Modifier.weight(0.42f),
                            )
                            ClientEditorPane(
                                client = selectedClient,
                                tasks = tasksState.tasks.filter { task -> task.clientId == selectedClient?.id },
                                logs = tasksState.logs.filter { log -> log.clientId == selectedClient?.id },
                                onEvent = onEvent,
                                modifier = Modifier.weight(0.58f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateClientDialog(
    onDismiss: () -> Unit,
    onCreate: (companyName: String, productName: String, contactName: String, email: String) -> Unit,
) {
    var companyName by rememberSaveable { mutableStateOf("") }
    var productName by rememberSaveable { mutableStateOf("") }
    var contactName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    val canCreate = companyName.isNotBlank() && productName.isNotBlank() && contactName.isNotBlank() && email.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Empresa") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Producto") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contacto") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Crear cliente",
                onClick = {
                    onCreate(companyName, productName, contactName, email)
                },
                enabled = canCreate,
            )
        },
        dismissButton = {
            SecondaryButton(text = "Cancelar", onClick = onDismiss)
        },
    )
}

@Composable
private fun ClientListPane(
    clients: List<Client>,
    tasks: List<WorkTask>,
    logs: List<TaskLog>,
    selectedClientId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(
        modifier = modifier.fillMaxSize(),
        title = "Clientes",
        subtitle = "Directorio operativo para consultar y enlazar trabajo.",
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items(clients, key = { it.id }) { client ->
                val selected = client.id == selectedClientId
                val clientTasks = tasks.filter { it.clientId == client.id }
                val clientMinutes = logs.filter { it.clientId == client.id }.sumOf { it.minutes }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(client.id) },
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                    },
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        Text(
                            text = client.companyName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = client.productName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            ClientServiceTierBadge(client.serviceTier)
                            ClientAccountStatusBadge(client.accountStatus)
                        }
                        Text(
                            text = "${clientTasks.count { !it.completed }} tareas abiertas - ${formatSupportDeskDuration(clientMinutes)} este mes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientEditorPane(
    client: Client?,
    tasks: List<WorkTask>,
    logs: List<TaskLog>,
    onEvent: (ClientsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    if (client == null) {
        SectionCard(
            modifier = modifier.fillMaxSize(),
            title = "Ficha de cliente",
            subtitle = "Selecciona un cliente para editarlo.",
        ) {
            EmptyState(
                title = "Nada seleccionado",
                message = "La ficha lateral aparecera aqui cuando elijas un cliente.",
            )
        }
        return
    }

    var confirmDelete by rememberSaveable(client.id) { mutableStateOf(false) }
    var companyName by remember(client.id) { mutableStateOf(client.companyName) }
    var productName by remember(client.id) { mutableStateOf(client.productName) }
    var contactName by remember(client.id) { mutableStateOf(client.contactName) }
    var email by remember(client.id) { mutableStateOf(client.email) }
    var accountStatus by remember(client.id) { mutableStateOf(client.accountStatus) }
    var serviceTier by remember(client.id) { mutableStateOf(client.serviceTier) }
    var preferredChannel by remember(client.id) { mutableStateOf(client.preferredContactChannel) }
    val clientMinutes = logs.sumOf { it.minutes }
    val recentTasks = tasks.sortedByDescending { it.updatedAt }.take(5)

    SectionCard(
        modifier = modifier.fillMaxSize(),
        title = client.companyName,
        subtitle = client.productName,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Empresa") },
                singleLine = true,
            )
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Producto") },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Contacto") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Email") },
                    singleLine = true,
                )
            }
            FilterBar(
                label = "Estado",
                options = ClientAccountStatus.entries.map { FilterOption(it, it.displayName()) },
                selected = accountStatus,
                onSelected = { accountStatus = it ?: ClientAccountStatus.ACTIVE },
            )
            FilterBar(
                label = "Tier",
                options = ClientServiceTier.entries.map { FilterOption(it, it.displayName()) },
                selected = serviceTier,
                onSelected = { serviceTier = it ?: ClientServiceTier.STANDARD },
            )
            FilterBar(
                label = "Canal",
                options = PreferredContactChannel.entries.map { FilterOption(it, it.displayName()) },
                selected = preferredChannel,
                onSelected = { preferredChannel = it ?: PreferredContactChannel.TICKET },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                PrimaryButton(
                    text = "Guardar",
                    onClick = {
                        onEvent(
                            ClientsUiEvent.UpdateClient(
                                clientId = client.id,
                                companyName = companyName,
                                productName = productName,
                                contactName = contactName,
                                email = email,
                                accountStatus = accountStatus,
                                serviceTier = serviceTier,
                                preferredContactChannel = preferredChannel,
                            ),
                        )
                    },
                    enabled = companyName.isNotBlank() && productName.isNotBlank() && contactName.isNotBlank() && email.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Borrar",
                    onClick = { confirmDelete = true },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                ClientServiceTierBadge(serviceTier)
                ClientAccountStatusBadge(accountStatus)
                PreferredContactChannelBadge(preferredChannel)
            }

            InfoRow(label = "Tareas abiertas", value = tasks.count { !it.completed }.toString())
            InfoRow(
                label = "Horas del mes",
                value = formatSupportDeskDuration(clientMinutes),
                supportingText = "${logs.size} registros asociados",
            )

            SectionCard(
                title = "Actividad reciente",
                subtitle = "Resumen corto para revisar trabajo reciente asociado al cliente.",
            ) {
                if (recentTasks.isEmpty()) {
                    EmptyState(
                        title = "Sin tareas asociadas",
                        message = "Este cliente todavia no tiene tareas vinculadas.",
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        recentTasks.forEach { task ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    if (task.description.isNotBlank()) {
                                        Text(
                                            text = task.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(
                                    text = formatSupportDeskDuration(task.loggedMinutes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    ConfirmDialog(
        visible = confirmDelete,
        title = "Borrar cliente",
        message = "Si el cliente tiene tickets relacionados, el servidor bloqueara el borrado.",
        confirmText = "Borrar",
        dismissText = "Cancelar",
        onConfirm = {
            confirmDelete = false
            onEvent(ClientsUiEvent.DeleteClient(client.id))
        },
        onDismiss = { confirmDelete = false },
    )
}

private fun ClientAccountStatus.displayName(): String = when (this) {
    ClientAccountStatus.ACTIVE -> "Activo"
    ClientAccountStatus.PAUSED -> "Pausado"
    ClientAccountStatus.INACTIVE -> "Inactivo"
}

private fun ClientServiceTier.displayName(): String = when (this) {
    ClientServiceTier.STANDARD -> "Standard"
    ClientServiceTier.PRIORITY -> "Priority"
    ClientServiceTier.VIP -> "VIP"
}

private fun PreferredContactChannel.displayName(): String = when (this) {
    PreferredContactChannel.TICKET -> "Ticket"
    PreferredContactChannel.EMAIL -> "Email"
    PreferredContactChannel.WHATSAPP -> "WhatsApp"
    PreferredContactChannel.CALL -> "Llamada"
}
