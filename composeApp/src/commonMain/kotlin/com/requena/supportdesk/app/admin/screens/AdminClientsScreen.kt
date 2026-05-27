package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.ClientAccountStatus
import com.requena.supportdesk.core.model.ClientPortalAccessStatus
import com.requena.supportdesk.core.model.ClientServiceTier
import com.requena.supportdesk.core.model.PreferredContactChannel
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.designsystem.components.badges.ClientAccountStatusBadge
import com.requena.supportdesk.designsystem.components.badges.ClientServiceTierBadge
import com.requena.supportdesk.designsystem.components.badges.PreferredContactChannelBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.feedback.ConfirmDialog
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.motion.SupportDeskEntrance
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.features.clients.presentation.event.ClientsUiEvent
import com.requena.supportdesk.features.clients.presentation.state.ClientsUiState
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun AdminClientsScreen(
    state: ClientsUiState,
    tasksState: TasksUiState,
    onEvent: (ClientsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val monthPrefix = tasksState.todayIsoDate.take(7)
    var showCreateClient by rememberSaveable { mutableStateOf(false) }
    var activeFilter by rememberSaveable { mutableStateOf(ClientVisualFilter.All) }

    val snapshots = remember(state.clients, tasksState.tasks, tasksState.logs, monthPrefix) {
        buildClientSnapshots(
            clients = state.clients,
            tasks = tasksState.tasks,
            logs = tasksState.logs,
            monthPrefix = monthPrefix,
        )
    }
    val filteredSnapshots = remember(snapshots, activeFilter) {
        snapshots.filter { activeFilter.matches(it) }
    }
    val selectedSnapshot = filteredSnapshots.firstOrNull { it.client.id == state.selectedClientId }
        ?: filteredSnapshots.firstOrNull()
    val stats = remember(selectedSnapshot) {
        selectedSnapshot?.let { buildClientStats(listOf(it)) } ?: buildClientStats(emptyList())
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxWidth < 1060.dp
        val pageModifier = if (compact) {
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        } else {
            Modifier.fillMaxSize()
        }

        if (compact) {
            Column(
                modifier = pageModifier,
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                SupportDeskEntrance(index = 0) {
                    ClientsHeroHeader(
                        query = state.query,
                        visibleCount = filteredSnapshots.size,
                        totalCount = snapshots.size,
                        activeFilter = activeFilter,
                        isLoading = state.isLoading,
                        errorMessage = state.errorMessage,
                        compact = true,
                        onSearchChanged = { onEvent(ClientsUiEvent.SearchChanged(it)) },
                        onFilterChanged = { activeFilter = it },
                        onRetry = { onEvent(ClientsUiEvent.Load) },
                        onCreate = { showCreateClient = true },
                    )
                }

                ClientKpiGrid(stats = stats)

                ClientResultsContent(
                    state = state,
                    snapshots = snapshots,
                    filteredSnapshots = filteredSnapshots,
                    selectedSnapshot = selectedSnapshot,
                    activeFilter = activeFilter,
                    onClearFilter = { activeFilter = ClientVisualFilter.All },
                    onSelect = { onEvent(ClientsUiEvent.SelectClient(it)) },
                    isSaving = state.isLoading,
                    isGeneratingCode = state.isGeneratingCode,
                    scrollDetailInside = false,
                    onEvent = onEvent,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    SupportDeskEntrance(index = 0) {
                        ClientsHeroHeader(
                            query = state.query,
                            visibleCount = filteredSnapshots.size,
                            totalCount = snapshots.size,
                            activeFilter = activeFilter,
                            isLoading = state.isLoading,
                            errorMessage = state.errorMessage,
                            compact = false,
                            onSearchChanged = { onEvent(ClientsUiEvent.SearchChanged(it)) },
                            onFilterChanged = { activeFilter = it },
                            onRetry = { onEvent(ClientsUiEvent.Load) },
                            onCreate = { showCreateClient = true },
                        )
                    }

                    ClientKpiGrid(stats = stats)

                    ClientListSlot(
                        state = state,
                        snapshots = snapshots,
                        filteredSnapshots = filteredSnapshots,
                        selectedSnapshot = selectedSnapshot,
                        activeFilter = activeFilter,
                        onClearFilter = { activeFilter = ClientVisualFilter.All },
                        onSelect = { onEvent(ClientsUiEvent.SelectClient(it)) },
                        modifier = Modifier.weight(1f),
                    )
                }

                SupportDeskEntrance(index = 6, horizontal = true, modifier = Modifier.weight(0.5f)) {
                    ClientDetailPanel(
                        snapshot = selectedSnapshot,
                        isSaving = state.isLoading,
                        isGeneratingCode = state.isGeneratingCode,
                        scrollInside = true,
                        onEvent = onEvent,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    state.generatedCode?.let { code ->
        AlertDialog(
            onDismissRequest = { onEvent(ClientsUiEvent.DismissInvitation) },
            title = { Text("Codigo de acceso generado") },
            text = {
                val clipboard = LocalClipboardManager.current
                Column(verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.md)) {
                    Text(
                        text = "Comparte este codigo con el cliente. Junto con su correo registrado, podra entrar al portal.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(SupportDeskThemeTokens.spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = code,
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            TextButton(
                                onClick = { clipboard.setText(AnnotatedString(code)) },
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Copiar")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(ClientsUiEvent.DismissInvitation) },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Listo")
                }
            },
        )
    }

    if (showCreateClient) {
        CreateClientDialog(
            onDismiss = { showCreateClient = false },
            onCreate = { companyName, productName, contactName, email, accountStatus, serviceTier, preferredChannel ->
                onEvent(
                    ClientsUiEvent.CreateClient(
                        companyName = companyName,
                        productName = productName,
                        contactName = contactName,
                        email = email,
                        accountStatus = accountStatus,
                        serviceTier = serviceTier,
                        preferredContactChannel = preferredChannel,
                    ),
                )
                showCreateClient = false
            },
        )
    }
}

@Composable
private fun ClientResultsContent(
    state: ClientsUiState,
    snapshots: List<ClientSnapshot>,
    filteredSnapshots: List<ClientSnapshot>,
    selectedSnapshot: ClientSnapshot?,
    activeFilter: ClientVisualFilter,
    onClearFilter: () -> Unit,
    onSelect: (String) -> Unit,
    isSaving: Boolean,
    isGeneratingCode: Boolean,
    scrollDetailInside: Boolean,
    onEvent: (ClientsUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    when {
        state.isLoading && snapshots.isEmpty() -> LoadingState(itemCount = 4)

        snapshots.isEmpty() -> EmptyState(
            title = if (state.query.isBlank()) "Sin clientes todavia" else "Sin resultados",
            message = state.errorMessage?.toClientFacingError()
                ?: if (state.query.isBlank()) {
                    "Crea el primer cliente para activar su ficha, tareas y acceso al portal."
                } else {
                    "No hay clientes que coincidan con la busqueda actual."
                },
        )

        filteredSnapshots.isEmpty() -> EmptyClientResults(
            query = state.query,
            activeFilter = activeFilter,
            onClearFilter = onClearFilter,
        )

        else -> Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            SupportDeskEntrance(index = 5) {
                ClientListPanel(
                    snapshots = filteredSnapshots,
                    selectedClientId = selectedSnapshot?.client?.id,
                    boundedHeight = true,
                    onSelect = onSelect,
                )
            }
            SupportDeskEntrance(index = 6) {
                ClientDetailPanel(
                    snapshot = selectedSnapshot,
                    isSaving = isSaving,
                    isGeneratingCode = isGeneratingCode,
                    scrollInside = scrollDetailInside,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun ClientListSlot(
    state: ClientsUiState,
    snapshots: List<ClientSnapshot>,
    filteredSnapshots: List<ClientSnapshot>,
    selectedSnapshot: ClientSnapshot?,
    activeFilter: ClientVisualFilter,
    onClearFilter: () -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading && snapshots.isEmpty() -> LoadingState(
            modifier = modifier,
            itemCount = 4,
        )

        snapshots.isEmpty() -> EmptyState(
            modifier = modifier,
            title = if (state.query.isBlank()) "Sin clientes todavia" else "Sin resultados",
            message = state.errorMessage?.toClientFacingError()
                ?: if (state.query.isBlank()) {
                    "Crea el primer cliente para activar su ficha, tareas y acceso al portal."
                } else {
                    "No hay clientes que coincidan con la busqueda actual."
                },
        )

        filteredSnapshots.isEmpty() -> EmptyClientResults(
            query = state.query,
            activeFilter = activeFilter,
            onClearFilter = onClearFilter,
            modifier = modifier,
        )

        else -> SupportDeskEntrance(index = 5, horizontal = true, modifier = modifier) {
            ClientListPanel(
                snapshots = filteredSnapshots,
                selectedClientId = selectedSnapshot?.client?.id,
                boundedHeight = false,
                onSelect = onSelect,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ClientsHeroHeader(
    query: String,
    visibleCount: Int,
    totalCount: Int,
    activeFilter: ClientVisualFilter,
    isLoading: Boolean,
    errorMessage: String?,
    compact: Boolean,
    onSearchChanged: (String) -> Unit,
    onFilterChanged: (ClientVisualFilter) -> Unit,
    onRetry: () -> Unit,
    onCreate: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val colors = MaterialTheme.colorScheme
    PremiumCard(
        accentColor = colors.primary,
        modifier = Modifier.fillMaxWidth(),
        compactPadding = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (totalCount == 1) "1 cuenta gestionada" else "$totalCount cuentas gestionadas",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                SyncStatePill(isLoading = isLoading, errorMessage = errorMessage)
            }

            if (errorMessage != null) {
                InlineServerAlert(
                    message = errorMessage.toClientFacingError(),
                    onRetry = onRetry,
                )
            }

            ClientSearchAndFilters(
                query = query,
                visibleCount = visibleCount,
                activeFilter = activeFilter,
                compact = compact,
                onSearchChanged = onSearchChanged,
                onFilterChanged = onFilterChanged,
                onCreate = onCreate,
            )
        }
    }
}

@Composable
private fun ClientSearchAndFilters(
    query: String,
    visibleCount: Int,
    activeFilter: ClientVisualFilter,
    compact: Boolean,
    onSearchChanged: (String) -> Unit,
    onFilterChanged: (ClientVisualFilter) -> Unit,
    onCreate: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val search = @Composable { fieldModifier: Modifier ->
        OutlinedTextField(
            value = query,
            onValueChange = onSearchChanged,
            modifier = fieldModifier,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            leadingIcon = {
                SearchGlyph(color = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                ClientFilterMenuButton(
                    activeFilter = activeFilter,
                    onFilterChanged = onFilterChanged,
                )
            },
            placeholder = {
                Text(
                    text = "Buscar cliente, email, etiqueta o proyecto...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        if (compact) {
            search(Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = if (visibleCount == 1) "1 cliente visible" else "$visibleCount clientes visibles",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(text = "Nuevo cliente", onClick = onCreate)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                search(
                    Modifier
                        .weight(1f)
                        .heightIn(max = 56.dp),
                )
                StatusPill(
                    text = if (visibleCount == 1) "1 cliente visible" else "$visibleCount clientes visibles",
                    accentColor = MaterialTheme.colorScheme.primary,
                )
                PrimaryButton(text = "Nuevo cliente", onClick = onCreate)
            }
        }
    }
}

@Composable
private fun ClientFilterMenuButton(
    activeFilter: ClientVisualFilter,
    onFilterChanged: (ClientVisualFilter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val active = activeFilter != ClientVisualFilter.All
    Box {
        Surface(
            modifier = Modifier
                .size(34.dp)
                .clickable { expanded = true },
            shape = RoundedCornerShape(9.dp),
            color = if (active) colors.primary.copy(alpha = 0.18f) else colors.surfaceVariant.copy(alpha = 0.34f),
            border = BorderStroke(
                width = 1.dp,
                color = if (active) colors.primary.copy(alpha = 0.42f) else colors.outlineVariant.copy(alpha = 0.42f),
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                FilterGlyph(
                    color = if (active) colors.primary else colors.onSurfaceVariant,
                    active = active,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ClientVisualFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (filter == activeFilter) {
                                StatusDot(color = colors.primary)
                            } else {
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                            Text(filter.label)
                        }
                    },
                    onClick = {
                        expanded = false
                        onFilterChanged(filter)
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterGlyph(
    color: Color,
    active: Boolean,
) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.8.dp.toPx()
        val ys = listOf(size.height * 0.26f, size.height * 0.50f, size.height * 0.74f)
        ys.forEachIndexed { index, y ->
            drawLine(
                color = color,
                start = Offset(size.width * 0.14f, y),
                end = Offset(size.width * 0.86f, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            val knobX = when (index) {
                0 -> if (active) 0.66f else 0.38f
                1 -> if (active) 0.34f else 0.62f
                else -> if (active) 0.58f else 0.46f
            }
            drawCircle(
                color = color,
                radius = size.minDimension * 0.10f,
                center = Offset(size.width * knobX, y),
            )
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Canvas(modifier = Modifier.size(8.dp)) {
        drawCircle(color = color, radius = size.minDimension / 2f)
    }
}

@Composable
private fun SearchGlyph(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.8.dp.toPx()
        drawCircle(
            color = color,
            radius = size.minDimension * 0.32f,
            center = Offset(size.width * 0.42f, size.height * 0.42f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.64f, size.height * 0.64f),
            end = Offset(size.width * 0.90f, size.height * 0.90f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun ClientKpiGrid(stats: ClientStats) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cards = listOf(
            KpiSpec(
                label = if (stats.totalClients == 1) "Cuenta activa" else "Clientes activos",
                numericValue = stats.activeClients,
                supportingText = if (stats.totalClients == 1) "cuenta seleccionada" else "de ${stats.totalClients} cuentas",
                trendText = "Cartera operativa",
                icon = "CL",
                accentColor = MaterialTheme.colorScheme.primary,
            ),
            KpiSpec(
                label = "Tareas abiertas",
                numericValue = stats.openTasks,
                supportingText = "${stats.scheduledOpenTasks} con fecha",
                trendText = "Trabajo pendiente",
                icon = "TA",
                accentColor = MaterialTheme.colorScheme.secondary,
            ),
            KpiSpec(
                label = "Tickets activos",
                numericValue = stats.activeTickets,
                supportingText = if (stats.totalClients == 1) "en cuenta seleccionada" else "en clientes visibles",
                trendText = "Soporte en curso",
                icon = "TI",
                accentColor = semantic.info,
            ),
            KpiSpec(
                label = "Horas este mes",
                durationMinutes = stats.monthMinutes,
                supportingText = "${stats.monthEntries} registros",
                trendText = "Tiempo facturable",
                icon = "HR",
                accentColor = semantic.warning,
            ),
        )

        when {
            maxWidth < 720.dp -> Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                cards.forEachIndexed { index, spec ->
                    SupportDeskEntrance(index = index + 1) {
                        KpiCard(spec = spec, index = index, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            maxWidth < 1120.dp -> Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                cards.chunked(2).forEachIndexed { rowIndex, rowCards ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        rowCards.forEachIndexed { index, spec ->
                            SupportDeskEntrance(index = rowIndex * 2 + index + 1, modifier = Modifier.weight(1f)) {
                                KpiCard(spec = spec, index = rowIndex * 2 + index, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }

            else -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                cards.forEachIndexed { index, spec ->
                    SupportDeskEntrance(index = index + 1, modifier = Modifier.weight(1f)) {
                        KpiCard(spec = spec, index = index, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    spec: KpiSpec,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    PremiumCard(
        modifier = modifier.heightIn(min = 104.dp),
        accentColor = spec.accentColor,
        compactPadding = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            IconBadge(text = spec.icon, accentColor = spec.accentColor)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = spec.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (spec.durationMinutes != null) {
                    AnimatedDurationValue(minutes = spec.durationMinutes, index = index, color = spec.accentColor)
                } else {
                    AnimatedNumberValue(value = spec.numericValue, index = index, color = spec.accentColor)
                }
                Text(
                    text = spec.supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        StatusPill(
            text = spec.trendText,
            accentColor = spec.accentColor,
        )
    }
}

@Composable
private fun ClientListPanel(
    snapshots: List<ClientSnapshot>,
    selectedClientId: String?,
    boundedHeight: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    PremiumCard(
        modifier = modifier,
        accentColor = MaterialTheme.colorScheme.secondary,
        compactPadding = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Cuentas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Selecciona una ficha para revisar actividad y acceso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(
                text = snapshots.size.toString(),
                accentColor = MaterialTheme.colorScheme.secondary,
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (boundedHeight) Modifier.heightIn(max = 420.dp) else Modifier.weight(1f)),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
            contentPadding = PaddingValues(bottom = spacing.xs),
        ) {
            itemsIndexed(snapshots, key = { _, snapshot -> snapshot.client.id }) { index, snapshot ->
                SupportDeskEntrance(index = index, horizontal = true) {
                    ClientListItem(
                        snapshot = snapshot,
                        selected = snapshot.client.id == selectedClientId,
                        onClick = { onSelect(snapshot.client.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientListItem(
    snapshot: ClientSnapshot,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val client = snapshot.client
    val accent = clientAccent(client)
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "clientRowBackground",
    )
    val border by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.72f) else Color.Transparent,
        animationSpec = tween(180),
        label = "clientRowBorder",
    )

    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        accentColor = accent,
        selected = selected,
        containerColor = background,
        borderColor = border,
        compactPadding = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            ClientAvatar(name = client.companyName, accentColor = accent, size = 42.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = client.companyName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = client.productName.ifBlank { "Proyecto sin nombre" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    StatusPill(text = snapshot.lastActivityLabel, accentColor = accent)
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    ClientServiceTierBadge(client.serviceTier)
                    ClientAccountStatusBadge(client.accountStatus)
                    PreferredContactChannelBadge(client.preferredContactChannel)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    ClientMiniMetric(label = "Horas", value = formatSupportDeskDuration(snapshot.monthMinutes), modifier = Modifier.weight(1f))
                    ClientMiniMetric(label = "Tareas", value = snapshot.openTasks.toString(), modifier = Modifier.weight(1f))
                    ClientMiniMetric(label = "Tickets", value = snapshot.activeTickets.toString(), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ClientDetailPanel(
    snapshot: ClientSnapshot?,
    isSaving: Boolean,
    isGeneratingCode: Boolean,
    scrollInside: Boolean,
    onEvent: (ClientsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = snapshot,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "client-detail-crossfade",
    ) { selectedSnapshot ->
        if (selectedSnapshot == null) {
            PremiumCard(
                modifier = modifier,
                accentColor = MaterialTheme.colorScheme.primary,
            ) {
                EmptyState(
                    title = "Selecciona un cliente",
                    message = "La ficha completa aparecera aqui con acceso, horas, tareas y actividad.",
                )
            }
        } else {
            ClientDetailContent(
                snapshot = selectedSnapshot,
                isSaving = isSaving,
                isGeneratingCode = isGeneratingCode,
                scrollInside = scrollInside,
                onEvent = onEvent,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ClientDetailContent(
    snapshot: ClientSnapshot,
    isSaving: Boolean,
    isGeneratingCode: Boolean,
    scrollInside: Boolean,
    onEvent: (ClientsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val client = snapshot.client
    val spacing = SupportDeskThemeTokens.spacing
    val accent = clientAccent(client)
    var confirmDelete by rememberSaveable(client.id) { mutableStateOf(false) }
    var showActions by rememberSaveable(client.id) { mutableStateOf(false) }
    var companyName by remember(client.id) { mutableStateOf(client.companyName) }
    var productName by remember(client.id) { mutableStateOf(client.productName) }
    var contactName by remember(client.id) { mutableStateOf(client.contactName) }
    var email by remember(client.id) { mutableStateOf(client.email) }
    var accountStatus by remember(client.id) { mutableStateOf(client.accountStatus) }
    var serviceTier by remember(client.id) { mutableStateOf(client.serviceTier) }
    var preferredChannel by remember(client.id) { mutableStateOf(client.preferredContactChannel) }

    val hasUnsavedChanges = companyName != client.companyName ||
        productName != client.productName ||
        contactName != client.contactName ||
        email != client.email ||
        accountStatus != client.accountStatus ||
        serviceTier != client.serviceTier ||
        preferredChannel != client.preferredContactChannel
    val canSave = hasUnsavedChanges &&
        companyName.isNotBlank() &&
        productName.isNotBlank() &&
        contactName.isNotBlank() &&
        email.isNotBlank() &&
        !isSaving
    val detailScrollState = rememberScrollState()

    LaunchedEffect(client.id) {
        detailScrollState.scrollTo(0)
    }

    val contentModifier = if (scrollInside) {
        Modifier
            .fillMaxSize()
            .verticalScroll(detailScrollState)
    } else {
        Modifier.fillMaxWidth()
    }

    PremiumCard(
        modifier = modifier,
        accentColor = accent,
    ) {
        Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            DetailHeader(
                client = client,
                accent = accent,
                hasUnsavedChanges = hasUnsavedChanges,
                isSaving = isSaving,
                canSave = canSave,
                showActions = showActions,
                onShowActionsChange = { showActions = it },
                onSave = {
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
                onDelete = { confirmDelete = true },
            )

            ClientSummaryGrid(snapshot = snapshot)

            ClientIdentityForm(
                companyName = companyName,
                productName = productName,
                contactName = contactName,
                email = email,
                accountStatus = accountStatus,
                serviceTier = serviceTier,
                preferredChannel = preferredChannel,
                onCompanyNameChange = { companyName = it },
                onProductNameChange = { productName = it },
                onContactNameChange = { contactName = it },
                onEmailChange = { email = it },
                onAccountStatusChange = { accountStatus = it },
                onServiceTierChange = { serviceTier = it },
                onPreferredChannelChange = { preferredChannel = it },
            )

            ClientAccessCard(
                client = client,
                isGeneratingCode = isGeneratingCode,
                onGenerate = { onEvent(ClientsUiEvent.GenerateInvitation(client.id)) },
            )

            ActivityTimeline(
                tasks = snapshot.recentTasks,
                logs = snapshot.recentLogs,
            )
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

@Composable
private fun DetailHeader(
    client: Client,
    accent: Color,
    hasUnsavedChanges: Boolean,
    isSaving: Boolean,
    canSave: Boolean,
    showActions: Boolean,
    onShowActionsChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 720.dp
        val copy = @Composable {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClientAvatar(name = client.companyName, accentColor = accent, size = 56.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = client.companyName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = client.productName.ifBlank { "Proyecto sin nombre" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        ClientAccountStatusBadge(client.accountStatus)
                        ClientServiceTierBadge(client.serviceTier)
                    }
                }
            }
        }
        val actions = @Composable {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = when {
                        isSaving -> "Guardando"
                        hasUnsavedChanges -> "Cambios pendientes"
                        else -> "Sincronizado"
                    },
                    accentColor = when {
                        hasUnsavedChanges -> SupportDeskThemeTokens.semanticColors.warning
                        else -> SupportDeskThemeTokens.semanticColors.success
                    },
                )
                PrimaryButton(
                    text = if (isSaving) "Guardando..." else "Guardar",
                    onClick = onSave,
                    enabled = canSave,
                )
                Box {
                    TextButton(
                        onClick = { onShowActionsChange(true) },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Mas acciones")
                    }
                    DropdownMenu(
                        expanded = showActions,
                        onDismissRequest = { onShowActionsChange(false) },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Borrar cliente",
                                    color = SupportDeskThemeTokens.semanticColors.danger,
                                )
                            },
                            onClick = {
                                onShowActionsChange(false)
                                onDelete()
                            },
                        )
                    }
                }
            }
        }

        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                copy()
                actions()
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(modifier = Modifier.weight(1f)) { copy() }
                actions()
            }
        }
    }
}

@Composable
private fun ClientSummaryGrid(snapshot: ClientSnapshot) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val metrics = listOf(
            SummarySpec("Horas mes", formatSupportDeskDuration(snapshot.monthMinutes), "${snapshot.monthEntries} registros", semantic.warning),
            SummarySpec("Tareas", snapshot.openTasks.toString(), "abiertas", MaterialTheme.colorScheme.primary),
            SummarySpec("Tickets", snapshot.activeTickets.toString(), "activos", semantic.info),
            SummarySpec("Actividad", snapshot.lastActivityLabel, "ultimo movimiento", semantic.success),
        )
        if (maxWidth < 640.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                metrics.forEach { SummaryTile(it, Modifier.fillMaxWidth()) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                metrics.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        rowItems.forEach { SummaryTile(it, Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(spec: SummarySpec, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        color = spec.color.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, spec.color.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = spec.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = spec.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = spec.color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = spec.supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ClientIdentityForm(
    companyName: String,
    productName: String,
    contactName: String,
    email: String,
    accountStatus: ClientAccountStatus,
    serviceTier: ClientServiceTier,
    preferredChannel: PreferredContactChannel,
    onCompanyNameChange: (String) -> Unit,
    onProductNameChange: (String) -> Unit,
    onContactNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAccountStatusChange: (ClientAccountStatus) -> Unit,
    onServiceTierChange: (ClientServiceTier) -> Unit,
    onPreferredChannelChange: (PreferredContactChannel) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)),
    ) {
        Column(
            modifier = Modifier.padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = "Datos de cuenta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = companyName,
                onValueChange = onCompanyNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Empresa") },
                singleLine = true,
            )
            OutlinedTextField(
                value = productName,
                onValueChange = onProductNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Proyecto o producto") },
                singleLine = true,
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 680.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = onContactNameChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Contacto principal") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = onEmailChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email") },
                            singleLine = true,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = onContactNameChange,
                            modifier = Modifier.weight(1f),
                            label = { Text("Contacto principal") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = onEmailChange,
                            modifier = Modifier.weight(1f),
                            label = { Text("Email") },
                            singleLine = true,
                        )
                    }
                }
            }
            FilterBar(
                label = "Estado",
                options = ClientAccountStatus.entries.map { FilterOption(it, it.displayName()) },
                selected = accountStatus,
                onSelected = { onAccountStatusChange(it ?: ClientAccountStatus.ACTIVE) },
                wrap = true,
            )
            FilterBar(
                label = "Plan",
                options = ClientServiceTier.entries.map { FilterOption(it, it.displayName()) },
                selected = serviceTier,
                onSelected = { onServiceTierChange(it ?: ClientServiceTier.STANDARD) },
                wrap = true,
            )
            FilterBar(
                label = "Canal",
                options = PreferredContactChannel.entries.map { FilterOption(it, it.displayName()) },
                selected = preferredChannel,
                onSelected = { onPreferredChannelChange(it ?: PreferredContactChannel.TICKET) },
                wrap = true,
            )
        }
    }
}

@Composable
private fun ClientAccessCard(
    client: Client,
    isGeneratingCode: Boolean,
    onGenerate: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val clipboard = LocalClipboardManager.current
    val portalCode = client.portalAccessCode
    val statusColor = when (client.portalAccessStatus) {
        ClientPortalAccessStatus.ACTIVE -> SupportDeskThemeTokens.semanticColors.success
        ClientPortalAccessStatus.EXPIRED -> SupportDeskThemeTokens.semanticColors.warning
        ClientPortalAccessStatus.MISSING -> MaterialTheme.colorScheme.error
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "Acceso de cliente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Codigo automatico ligado al correo ${client.email}. El cliente entra al portal con ese correo y este codigo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusPill(
                    text = client.portalAccessStatus.displayName(),
                    accentColor = statusColor,
                )
            }
            if (portalCode.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm)) {
                    EmptyState(
                        title = "Sin codigo de acceso",
                        message = "Este cliente aun no tiene codigo. Genéralo para que pueda entrar al portal con su correo.",
                    )
                    PrimaryButton(
                        text = if (isGeneratingCode) "Generando..." else "Generar codigo",
                        onClick = onGenerate,
                        enabled = !isGeneratingCode,
                        fullWidth = true,
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF17281E),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        Text(
                            text = "Codigo activo para ${client.contactName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFDDE8DD),
                        )
                        Text(
                            text = portalCode,
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB9D8C2),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            SecondaryButton(
                                text = "Copiar codigo",
                                onClick = { clipboard.setText(AnnotatedString(portalCode)) },
                            )
                            SecondaryButton(
                                text = if (isGeneratingCode) "Generando..." else "Regenerar",
                                onClick = onGenerate,
                                enabled = !isGeneratingCode,
                            )
                        }
                        Text(
                            text = client.portalAccessExpiresAt?.let { "Vigente hasta $it." }
                                ?: "Vigente mientras el acceso del cliente este activo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFDDE8DD).copy(alpha = 0.74f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityTimeline(
    tasks: List<WorkTask>,
    logs: List<TaskLog>,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val items = buildActivityItems(tasks, logs)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)),
    ) {
        Column(
            modifier = Modifier.padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = "Actividad reciente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (items.isEmpty()) {
                EmptyState(
                    title = "Sin actividad reciente",
                    message = "Cuando haya tareas u horas registradas se mostraran aqui.",
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    items.forEachIndexed { index, item ->
                        SupportDeskEntrance(index = index) {
                            TimelineItem(
                                item = item,
                                isFirst = index == 0,
                                showConnector = index < items.lastIndex,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(
    item: ActivityItem,
    isFirst: Boolean,
    showConnector: Boolean,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val pulse by animateFloatAsState(
        targetValue = if (isFirst) 1.18f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "timelinePulse",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size((10.dp.value * pulse).dp)
                    .background(item.accentColor, CircleShape),
            )
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(42.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.54f)),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = item.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = item.meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                StatusPill(text = item.type, accentColor = item.accentColor)
                if (item.durationLabel.isNotBlank()) {
                    StatusPill(text = item.durationLabel, accentColor = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun EmptyClientResults(
    query: String,
    activeFilter: ClientVisualFilter,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumCard(
        accentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxWidth(),
    ) {
        EmptyState(
            title = "Sin resultados",
            message = if (query.isBlank()) {
                "No hay clientes para el filtro ${activeFilter.label.lowercase()}."
            } else {
                "No hay clientes que coincidan con \"$query\" y el filtro ${activeFilter.label.lowercase()}."
            },
        )
        SecondaryButton(text = "Ver todos", onClick = onClearFilter)
    }
}

@Composable
private fun InlineServerAlert(
    message: String,
    onRetry: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val danger = SupportDeskThemeTokens.semanticColors.danger
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SupportDeskThemeTokens.semanticColors.dangerContainer.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, danger.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(text = "!", accentColor = danger)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "No se pudo conectar con el servidor",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = danger,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SecondaryButton(text = "Reintentar", onClick = onRetry)
        }
    }
}

@Composable
private fun SyncStatePill(
    isLoading: Boolean,
    errorMessage: String?,
) {
    val semantic = SupportDeskThemeTokens.semanticColors
    val text = when {
        errorMessage != null -> "Error del servidor"
        isLoading -> "Sincronizando"
        else -> "Sincronizado"
    }
    val color = when {
        errorMessage != null -> semantic.danger
        isLoading -> semantic.warning
        else -> semantic.success
    }
    StatusPill(text = text, accentColor = color)
}

@Composable
private fun PremiumCard(
    modifier: Modifier = Modifier,
    accentColor: Color,
    selected: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f),
    compactPadding: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val shape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val offsetY by animateDpAsState(
        targetValue = if (hovered) (-2).dp else 0.dp,
        animationSpec = tween(160),
        label = "premiumCardOffset",
    )
    val elevation by animateDpAsState(
        targetValue = when {
            selected -> 7.dp
            hovered -> 5.dp
            else -> 1.dp
        },
        animationSpec = tween(160),
        label = "premiumCardElevation",
    )
    val resolvedBorder by animateColorAsState(
        targetValue = if (selected || hovered) accentColor.copy(alpha = 0.46f) else borderColor,
        animationSpec = tween(180),
        label = "premiumCardBorder",
    )

    Surface(
        modifier = modifier
            .offset(y = offsetY)
            .shadow(elevation, shape)
            .border(BorderStroke(1.dp, resolvedBorder), shape)
            .hoverable(interactionSource)
            .animateContentSize(),
        shape = shape,
        color = containerColor,
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = if (selected) 0.10f else 0.055f),
                            containerColor,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f),
                        ),
                    ),
                )
                .padding(if (compactPadding) spacing.md else spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            content = content,
        )
    }
}

@Composable
private fun ClientAvatar(
    name: String,
    accentColor: Color,
    size: Dp = 44.dp,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = accentColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.34f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.initials(),
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun IconBadge(
    text: String,
    accentColor: Color,
) {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 26.dp),
        shape = RoundedCornerShape(999.dp),
        color = accentColor.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ClientMiniMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AnimatedNumberValue(
    value: Int,
    index: Int,
    color: Color,
) {
    var target by remember(value) { mutableStateOf(0f) }
    LaunchedEffect(value) {
        target = 0f
        delay(index * 70L)
        target = value.toFloat()
    }
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "numberCountUp",
    )
    Text(
        text = animated.roundToInt().toString(),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

@Composable
private fun AnimatedDurationValue(
    minutes: Int,
    index: Int,
    color: Color,
) {
    var target by remember(minutes) { mutableStateOf(0f) }
    LaunchedEffect(minutes) {
        target = 0f
        delay(index * 70L)
        target = minutes.toFloat()
    }
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "durationCountUp",
    )
    Text(
        text = formatSupportDeskDuration(animated.roundToInt()),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun CreateClientDialog(
    onDismiss: () -> Unit,
    onCreate: (
        companyName: String,
        productName: String,
        contactName: String,
        email: String,
        accountStatus: ClientAccountStatus,
        serviceTier: ClientServiceTier,
        preferredChannel: PreferredContactChannel,
    ) -> Unit,
) {
    var companyName by rememberSaveable { mutableStateOf("") }
    var productName by rememberSaveable { mutableStateOf("") }
    var contactName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var accountStatus by rememberSaveable { mutableStateOf(ClientAccountStatus.ACTIVE) }
    var serviceTier by rememberSaveable { mutableStateOf(ClientServiceTier.STANDARD) }
    var preferredChannel by rememberSaveable { mutableStateOf(PreferredContactChannel.TICKET) }
    val canCreate = companyName.isNotBlank() && productName.isNotBlank() && contactName.isNotBlank() && email.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo cliente") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(value = companyName, onValueChange = { companyName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Empresa") }, singleLine = true)
                OutlinedTextField(value = productName, onValueChange = { productName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Proyecto o producto") }, singleLine = true)
                OutlinedTextField(value = contactName, onValueChange = { contactName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Contacto principal") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
                FilterBar(
                    label = "Estado",
                    options = ClientAccountStatus.entries.map { FilterOption(it, it.displayName()) },
                    selected = accountStatus,
                    onSelected = { accountStatus = it ?: ClientAccountStatus.ACTIVE },
                    wrap = true,
                )
                FilterBar(
                    label = "Plan",
                    options = ClientServiceTier.entries.map { FilterOption(it, it.displayName()) },
                    selected = serviceTier,
                    onSelected = { serviceTier = it ?: ClientServiceTier.STANDARD },
                    wrap = true,
                )
                FilterBar(
                    label = "Canal",
                    options = PreferredContactChannel.entries.map { FilterOption(it, it.displayName()) },
                    selected = preferredChannel,
                    onSelected = { preferredChannel = it ?: PreferredContactChannel.TICKET },
                    wrap = true,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Crear cliente",
                onClick = {
                    onCreate(companyName, productName, contactName, email, accountStatus, serviceTier, preferredChannel)
                },
                enabled = canCreate,
            )
        },
        dismissButton = {
            SecondaryButton(text = "Cancelar", onClick = onDismiss)
        },
    )
}

private enum class ClientVisualFilter(val label: String) {
    All("Todos"),
    Active("Activos"),
    WithTasks("Con tareas"),
    WithTickets("Con tickets"),
    NoActivity("Sin actividad");

    fun matches(snapshot: ClientSnapshot): Boolean = when (this) {
        All -> true
        Active -> snapshot.client.accountStatus == ClientAccountStatus.ACTIVE
        WithTasks -> snapshot.openTasks > 0
        WithTickets -> snapshot.activeTickets > 0
        NoActivity -> snapshot.openTasks == 0 && snapshot.activeTickets == 0 && snapshot.monthEntries == 0
    }
}

private data class ClientSnapshot(
    val client: Client,
    val openTasks: Int,
    val activeTickets: Int,
    val monthMinutes: Int,
    val monthEntries: Int,
    val lastActivityLabel: String,
    val recentTasks: List<WorkTask>,
    val recentLogs: List<TaskLog>,
)

private data class ClientStats(
    val totalClients: Int,
    val activeClients: Int,
    val openTasks: Int,
    val scheduledOpenTasks: Int,
    val activeTickets: Int,
    val monthMinutes: Int,
    val monthEntries: Int,
)

private data class KpiSpec(
    val label: String,
    val numericValue: Int = 0,
    val durationMinutes: Int? = null,
    val supportingText: String,
    val trendText: String,
    val icon: String,
    val accentColor: Color,
)

private data class SummarySpec(
    val label: String,
    val value: String,
    val supportingText: String,
    val color: Color,
)

private data class ActivityItem(
    val title: String,
    val type: String,
    val meta: String,
    val dateLabel: String,
    val durationLabel: String,
    val accentColor: Color,
    val sortKey: String,
)

private fun buildClientSnapshots(
    clients: List<Client>,
    tasks: List<WorkTask>,
    logs: List<TaskLog>,
    monthPrefix: String,
): List<ClientSnapshot> = clients.map { client ->
    val clientTasks = tasks.filter { it.clientId == client.id }
    val openTasks = clientTasks.filter { !it.completed }
    val monthLogs = logs.filter { it.clientId == client.id && it.workDate.startsWith(monthPrefix) }
    val recentTasks = clientTasks.sortedByDescending { it.updatedAt }.take(5)
    val recentLogs = logs.filter { it.clientId == client.id }.sortedByDescending { it.createdAt }.take(5)
    val lastActivity = listOfNotNull(
        recentTasks.firstOrNull()?.updatedAt,
        recentLogs.firstOrNull()?.createdAt,
    ).maxOrNull()
    ClientSnapshot(
        client = client,
        openTasks = openTasks.size,
        activeTickets = client.activeTicketCount,
        monthMinutes = monthLogs.sumOf { it.minutes },
        monthEntries = monthLogs.size,
        lastActivityLabel = lastActivity.toActivityDateLabel(),
        recentTasks = recentTasks,
        recentLogs = recentLogs,
    )
}

private fun buildClientStats(snapshots: List<ClientSnapshot>): ClientStats = ClientStats(
    totalClients = snapshots.size,
    activeClients = snapshots.count { it.client.accountStatus == ClientAccountStatus.ACTIVE },
    openTasks = snapshots.sumOf { it.openTasks },
    scheduledOpenTasks = snapshots.sumOf { snapshot -> snapshot.recentTasks.count { !it.completed && it.dueDate != null } },
    activeTickets = snapshots.sumOf { it.activeTickets },
    monthMinutes = snapshots.sumOf { it.monthMinutes },
    monthEntries = snapshots.sumOf { it.monthEntries },
)

@Composable
private fun buildActivityItems(
    tasks: List<WorkTask>,
    logs: List<TaskLog>,
): List<ActivityItem> {
    val semantic = SupportDeskThemeTokens.semanticColors
    val taskItems = tasks.map { task ->
        ActivityItem(
            title = task.title,
            type = if (task.completed) "Tarea cerrada" else "Tarea abierta",
            meta = task.dueDate?.let { "Programada para $it" } ?: "Sin fecha programada",
            dateLabel = task.updatedAt.toActivityDateLabel(),
            durationLabel = if (task.loggedMinutes > 0) formatSupportDeskDuration(task.loggedMinutes) else "",
            accentColor = if (task.completed) semantic.success else MaterialTheme.colorScheme.primary,
            sortKey = task.updatedAt,
        )
    }
    val logItems = logs.map { log ->
        ActivityItem(
            title = log.note.ifBlank { "Registro de horas" },
            type = if (log.billable) "Horas facturables" else "Horas internas",
            meta = "Registrado por ${log.authorName.ifBlank { "Equipo" }}",
            dateLabel = log.createdAt.toActivityDateLabel(),
            durationLabel = formatSupportDeskDuration(log.minutes),
            accentColor = semantic.warning,
            sortKey = log.createdAt,
        )
    }
    return (taskItems + logItems)
        .sortedByDescending { it.sortKey }
        .take(6)
}

@Composable
private fun clientAccent(client: Client): Color = when (client.serviceTier) {
    ClientServiceTier.VIP -> MaterialTheme.colorScheme.tertiary
    ClientServiceTier.PRIORITY -> SupportDeskThemeTokens.semanticColors.warning
    ClientServiceTier.STANDARD -> MaterialTheme.colorScheme.primary
}

private fun String?.toActivityDateLabel(): String =
    this?.takeIf { it.isNotBlank() }?.substringBefore("T") ?: "Sin actividad"

private fun String.initials(): String {
    val words = trim().split(" ").filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "OK"
    }
}

private fun String.toClientFacingError(): String =
    if (contains("unexpected", ignoreCase = true) || contains("server", ignoreCase = true)) {
        "Revisa la conexion o intentalo de nuevo."
    } else {
        this
    }

private fun ClientAccountStatus.displayName(): String = when (this) {
    ClientAccountStatus.ACTIVE -> "Activo"
    ClientAccountStatus.PAUSED -> "Pausado"
    ClientAccountStatus.INACTIVE -> "Inactivo"
}

private fun ClientPortalAccessStatus.displayName(): String = when (this) {
    ClientPortalAccessStatus.ACTIVE -> "Activo"
    ClientPortalAccessStatus.MISSING -> "Pendiente"
    ClientPortalAccessStatus.EXPIRED -> "Caducado"
}

private fun ClientServiceTier.displayName(): String = when (this) {
    ClientServiceTier.STANDARD -> "Estandar"
    ClientServiceTier.PRIORITY -> "Prioritario"
    ClientServiceTier.VIP -> "VIP"
}

private fun PreferredContactChannel.displayName(): String = when (this) {
    PreferredContactChannel.TICKET -> "Ticket"
    PreferredContactChannel.EMAIL -> "Email"
    PreferredContactChannel.WHATSAPP -> "WhatsApp"
    PreferredContactChannel.CALL -> "Llamada"
}
