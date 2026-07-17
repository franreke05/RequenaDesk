package com.requena.supportdesk.app.client.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Columns3
import com.composables.icons.lucide.Headphones
import com.composables.icons.lucide.House
import com.composables.icons.lucide.ListTodo
import com.composables.icons.lucide.Lucide
import com.requena.supportdesk.app.client.ClientNotice
import com.requena.supportdesk.app.client.components.ClientPortalMetric
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.app.client.components.ClientPortalSectionTitle
import com.requena.supportdesk.app.client.components.ClientPortalSurfaceCard
import com.requena.supportdesk.core.model.ClientProgram
import com.requena.supportdesk.core.model.ClientProgramRequest
import com.requena.supportdesk.core.model.ClientProgramSubscription
import com.requena.supportdesk.core.model.ClientProgramSubscriptionStatus
import com.requena.supportdesk.core.model.ProgramRequestStatus
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.programs.presentation.event.ProgramsUiEvent
import com.requena.supportdesk.features.programs.presentation.state.ProgramsUiState

private enum class ProgramFilter(val label: String) {
    ALL("Todos"),
    ACTIVE("Activos"),
    REQUESTED("Solicitados"),
    AVAILABLE("Para tu equipo"),
}

/**
 * Comercial, pero no autoritativa: el servidor es quien decide el catálogo,
 * la disponibilidad, la autorización beta y el acceso efectivo.
 */
@Composable
fun ClientProgramsScreen(
    state: ProgramsUiState,
    onEvent: (ProgramsUiEvent) -> Unit,
    onOpenProgram: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val overview = state.overview
    val activeSubscriptions = remember(overview.subscriptions) {
        overview.subscriptions.filter { it.status == ClientProgramSubscriptionStatus.ACTIVE }
    }
    val activeKeys = remember(activeSubscriptions) { activeSubscriptions.map { it.productKey }.toSet() }
    val pendingKeys = remember(overview.requests) {
        overview.requests
            .filter { it.status == ProgramRequestStatus.REQUESTED }
            .map { it.productKey }
            .toSet()
    }
    var selectedFilter by remember { mutableStateOf(ProgramFilter.ALL) }

    val filteredPrograms = remember(
        overview.catalog,
        activeKeys,
        pendingKeys,
        selectedFilter,
    ) {
        overview.catalog.filter { program ->
            when (selectedFilter) {
                ProgramFilter.ALL -> true
                ProgramFilter.ACTIVE -> program.key in activeKeys
                ProgramFilter.REQUESTED -> program.key in pendingKeys
                ProgramFilter.AVAILABLE -> program.isAvailable && program.isRequestable &&
                    program.key !in activeKeys && program.key !in pendingKeys
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        ProgramsHeading(
            activeCount = activeSubscriptions.size,
        )

        if (state.isLoading && overview.catalog.isEmpty()) {
            LoadingState(itemCount = 3)
            return@Column
        }

        state.errorMessage?.let { message ->
            ClientNotice(message = message, isError = true)
            SecondaryButton(
                text = "Reintentar",
                onClick = { onEvent(ProgramsUiEvent.RefreshClientPrograms) },
            )
        }

        if (overview.requests.isNotEmpty()) {
            ProgramRequestsPanel(
                requests = overview.requests,
                catalog = overview.catalog,
            )
        }

        ProgramFilters(
            selected = selectedFilter,
            onSelect = { selectedFilter = it },
        )

        if (state.selectedProgramKeys.isNotEmpty()) {
            ProgramSelectionPanel(
                selectedPrograms = overview.catalog.filter { it.key in state.selectedProgramKeys },
                customerNote = state.customerNote,
                isSubmitting = state.isSubmitting,
                onNoteChanged = { onEvent(ProgramsUiEvent.CustomerNoteChanged(it)) },
                onClear = { onEvent(ProgramsUiEvent.ClearProgramSelection) },
                onSubmit = { onEvent(ProgramsUiEvent.SubmitProgramSelection) },
            )
        }

        when {
            filteredPrograms.isEmpty() && overview.catalog.isEmpty() -> EmptyState(
                title = "Aún no hay programas disponibles",
                message = "Cuando tu equipo tenga utilidades listas para activar, aparecerán aquí.",
                actionText = "Actualizar",
                onAction = { onEvent(ProgramsUiEvent.RefreshClientPrograms) },
            )

            filteredPrograms.isEmpty() -> EmptyState(
                title = "No hay programas en este filtro",
                message = "Cambia de vista para ver el catálogo completo.",
                actionText = "Ver todos",
                onAction = { selectedFilter = ProgramFilter.ALL },
            )

            else -> ProgramCatalogGrid(
                programs = filteredPrograms,
                selectedKeys = state.selectedProgramKeys,
                subscriptions = overview.subscriptions,
                requests = overview.requests,
                onToggleSelection = { onEvent(ProgramsUiEvent.ToggleProgramSelection(it)) },
                onOpenProgram = onOpenProgram,
            )
        }
    }
}

@Composable
private fun ProgramsHeading(
    activeCount: Int,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        ClientPortalPageHeader(
            title = "Programas",
            subtitle = "Elige utilidades para tu equipo. El administrador confirma cada activación gratuita durante la beta.",
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 560.dp
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    ClientPortalMetric(
                        label = "Programas activos",
                        value = activeCount.toString(),
                        supportingText = "disponibles en tu portal",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ClientPortalMetric(
                        label = "Acceso beta",
                        value = "Gratis",
                        supportingText = "sin cargos durante la beta",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    ClientPortalMetric(
                        label = "Programas activos",
                        value = activeCount.toString(),
                        supportingText = "disponibles en tu portal",
                        modifier = Modifier.weight(1f),
                    )
                    ClientPortalMetric(
                        label = "Acceso beta",
                        value = "Gratis",
                        supportingText = "sin cargos durante la beta",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramFilters(
    selected: ProgramFilter,
    onSelect: (ProgramFilter) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = "Explorar catálogo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            ProgramFilter.entries.forEach { filter ->
                FilterChip(
                    selected = filter == selected,
                    onClick = { onSelect(filter) },
                    label = { Text(filter.label) },
                )
            }
        }
    }
}

@Composable
private fun ProgramSelectionPanel(
    selectedPrograms: List<ClientProgram>,
    customerNote: String,
    isSubmitting: Boolean,
    onNoteChanged: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    ClientPortalSurfaceCard {
        ClientPortalSectionTitle("Tu selección", "Revisa la solicitud antes de enviarla al administrador.")
        selectedPrograms.forEach { program ->
            ProgramSelectionRow(program = program)
        }
        SupportDeskBadge(
            text = "Beta gratuita",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = "No se generará ningún cargo ni factura durante la beta. El administrador debe autorizar el acceso antes de activarlo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = customerNote,
            onValueChange = onNoteChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Cuéntanos qué necesita tu equipo (opcional)") },
            minLines = 2,
            maxLines = 4,
            enabled = !isSubmitting,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 520.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    PrimaryButton(
                        text = "Enviar solicitud",
                        fullWidth = true,
                        isLoading = isSubmitting,
                        onClick = onSubmit,
                    )
                    SecondaryButton(
                        text = "Limpiar selección",
                        fullWidth = true,
                        enabled = !isSubmitting,
                        onClick = onClear,
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    PrimaryButton(
                        text = "Enviar solicitud",
                        modifier = Modifier.weight(1f),
                        fullWidth = true,
                        isLoading = isSubmitting,
                        onClick = onSubmit,
                    )
                    SecondaryButton(
                        text = "Limpiar selección",
                        modifier = Modifier.weight(1f),
                        fullWidth = true,
                        enabled = !isSubmitting,
                        onClick = onClear,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramSelectionRow(program: ClientProgram) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = programIcon(program.iconKey),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = program.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ProgramCatalogGrid(
    programs: List<ClientProgram>,
    selectedKeys: Set<String>,
    subscriptions: List<ClientProgramSubscription>,
    requests: List<ClientProgramRequest>,
    onToggleSelection: (String) -> Unit,
    onOpenProgram: (String) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= 1320.dp -> 3
            maxWidth >= 720.dp -> 2
            else -> 1
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            programs.chunked(columns).forEach { rowPrograms ->
                if (columns == 1) {
                    ProgramCatalogCard(
                        program = rowPrograms.first(),
                        isSelected = rowPrograms.first().key in selectedKeys,
                        subscription = subscriptions.firstOrNull { it.productKey == rowPrograms.first().key },
                        request = requests.firstOrNull { it.productKey == rowPrograms.first().key && it.status == ProgramRequestStatus.REQUESTED },
                        onToggleSelection = onToggleSelection,
                        onOpenProgram = onOpenProgram,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        rowPrograms.forEach { program ->
                            ProgramCatalogCard(
                                program = program,
                                isSelected = program.key in selectedKeys,
                                subscription = subscriptions.firstOrNull { it.productKey == program.key },
                                request = requests.firstOrNull { it.productKey == program.key && it.status == ProgramRequestStatus.REQUESTED },
                                onToggleSelection = onToggleSelection,
                                onOpenProgram = onOpenProgram,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - rowPrograms.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramCatalogCard(
    program: ClientProgram,
    isSelected: Boolean,
    subscription: ClientProgramSubscription?,
    request: ClientProgramRequest?,
    onToggleSelection: (String) -> Unit,
    onOpenProgram: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val isActive = subscription?.status == ClientProgramSubscriptionStatus.ACTIVE
    val canRequest = program.isAvailable && program.isRequestable && !isActive && request == null
    ClientPortalSurfaceCard(modifier = modifier.heightIn(min = 310.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = program.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = programIcon(program.iconKey),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = program.shortDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        ProgramStateBadge(
            program = program,
            subscription = subscription,
            request = request,
            isSelected = isSelected,
        )
        Text(
            text = "Gratis durante la beta",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = program.category,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (program.capabilities.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                program.capabilities.take(3).forEach { capability ->
                    Text(
                        text = "• $capability",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        when {
            isActive -> SecondaryButton(
                text = "Abrir programa",
                fullWidth = true,
                onClick = { onOpenProgram(program.key) },
            )

            request != null -> SecondaryButton(
                text = "Solicitud enviada",
                fullWidth = true,
                enabled = false,
                onClick = {},
            )

            canRequest -> PrimaryButton(
                text = if (isSelected) "Quitar de la selección" else "Añadir a mi selección",
                fullWidth = true,
                onClick = { onToggleSelection(program.key) },
            )

            else -> SecondaryButton(
                text = "No disponible ahora",
                fullWidth = true,
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Composable
private fun ProgramStateBadge(
    program: ClientProgram,
    subscription: ClientProgramSubscription?,
    request: ClientProgramRequest?,
    isSelected: Boolean,
) {
    val semantic = SupportDeskThemeTokens.semanticColors
    when {
        subscription?.status == ClientProgramSubscriptionStatus.ACTIVE -> SupportDeskBadge(
            text = "Activo",
            containerColor = semantic.successContainer,
            contentColor = semantic.success,
        )

        subscription?.status == ClientProgramSubscriptionStatus.SUSPENDED -> SupportDeskBadge(
            text = "Suspendido",
            containerColor = semantic.warningContainer,
            contentColor = semantic.warning,
        )

        request != null -> SupportDeskBadge(
            text = "Pendiente de autorización",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )

        isSelected -> SupportDeskBadge(
            text = "En tu selección",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )

        !program.isAvailable -> SupportDeskBadge(
            text = "Próximamente",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        else -> SupportDeskBadge(
            text = "Disponible para solicitar",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun ProgramRequestsPanel(
    requests: List<ClientProgramRequest>,
    catalog: List<ClientProgram>,
) {
    val spacing = SupportDeskThemeTokens.spacing
    ClientPortalSurfaceCard {
        ClientPortalSectionTitle("Solicitudes de activación", "Consulta qué programas están en revisión y las decisiones ya tomadas.")
        requests.sortedByDescending { it.requestedAt }.forEach { request ->
            val program = catalog.firstOrNull { it.key == request.productKey }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                    Text(
                        text = program?.name ?: request.productKey,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = requestStatusSupportingText(request),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                RequestStatusBadge(status = request.status)
            }
        }
    }
}

private fun requestStatusSupportingText(request: ClientProgramRequest): String = when (request.status) {
    ProgramRequestStatus.REQUESTED -> "Enviada el ${request.requestedAt.take(10)} · pendiente de autorización"
    ProgramRequestStatus.APPROVED -> "Autorizada gratis durante la beta"
    ProgramRequestStatus.REJECTED -> request.adminNote?.takeIf { it.isNotBlank() } ?: "No autorizada en este momento"
    ProgramRequestStatus.CANCELLED -> "Solicitud cancelada"
}

@Composable
private fun RequestStatusBadge(status: ProgramRequestStatus) {
    val semantic = SupportDeskThemeTokens.semanticColors
    when (status) {
        ProgramRequestStatus.REQUESTED -> SupportDeskBadge(
            text = "En revisión",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        ProgramRequestStatus.APPROVED -> SupportDeskBadge(
            text = "Aprobada",
            containerColor = semantic.successContainer,
            contentColor = semantic.success,
        )
        ProgramRequestStatus.REJECTED -> SupportDeskBadge(
            text = "No autorizada",
            containerColor = semantic.dangerContainer,
            contentColor = semantic.danger,
        )
        ProgramRequestStatus.CANCELLED -> SupportDeskBadge(
            text = "Cancelada",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun programIcon(iconKey: String): ImageVector = when (iconKey.lowercase()) {
    "service", "sla", "support" -> Lucide.Headphones
    "tasks", "workflow", "forms" -> Lucide.ListTodo
    "home", "portal" -> Lucide.House
    else -> Lucide.Columns3
}
