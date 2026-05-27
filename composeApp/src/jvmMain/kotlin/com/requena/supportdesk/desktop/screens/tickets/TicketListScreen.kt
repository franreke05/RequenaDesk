package com.requena.supportdesk.desktop.screens.tickets

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.model.WaitingOn
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.inputs.SearchField
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.components.tickets.TicketListItem
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TicketListScreen(
    state: TicketsUiState,
    role: UserRole,
    currentUserId: String?,
    onEvent: (TicketsUiEvent) -> Unit,
    onCreateTicket: () -> Unit,
    onOpenDetail: (Ticket) -> Unit,
    onReply: (String) -> Unit,
    onChangeStatus: (TicketStatus) -> Unit,
    onChangePriority: (TicketPriority) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val statusOptions = remember { TicketStatus.entries.map { FilterOption(value = it, label = it.displayName()) } }
    val priorityOptions = remember { TicketPriority.entries.map { FilterOption(value = it, label = it.displayName()) } }
    val categoryOptions = remember { TicketCategory.entries.map { FilterOption(value = it, label = it.displayName()) } }
    val platformOptions = remember { SupportPlatform.entries.map { FilterOption(value = it, label = it.displayName()) } }
    val waitingOnOptions = remember { WaitingOn.entries.map { FilterOption(value = it, label = it.displayName()) } }
    val errorMessage = state.errorMessage

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        PageHeader(
            title = "Tickets",
            subtitle = "Revisa la cola rápido, mantén la conversación activa abierta y decide el siguiente paso sin salir del espacio de trabajo.",
            eyebrow = if (role == UserRole.ADMIN) "Cola de administración" else "Tickets del cliente",
            actions = {
                PrimaryButton(text = "Crear ticket", onClick = onCreateTicket)
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            SectionCard(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                title = "Bandeja de entrada",
                subtitle = "${state.tickets.size} tickets visibles con los filtros actuales.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    SearchField(
                        value = state.searchQuery,
                        onValueChange = { onEvent(TicketsUiEvent.SearchChanged(it)) },
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        FilterBar(
                            label = "Estado",
                            options = statusOptions,
                            selected = state.statusFilter,
                            onSelected = { onEvent(TicketsUiEvent.StatusFilterChanged(it)) },
                        )
                        FilterBar(
                            label = "Prioridad",
                            options = priorityOptions,
                            selected = state.priorityFilter,
                            onSelected = { onEvent(TicketsUiEvent.PriorityFilterChanged(it)) },
                        )
                        FilterBar(
                            label = "Categoría",
                            options = categoryOptions,
                            selected = state.categoryFilter,
                            onSelected = { onEvent(TicketsUiEvent.CategoryFilterChanged(it)) },
                        )
                        FilterBar(
                            label = "Plataforma",
                            options = platformOptions,
                            selected = state.platformFilter,
                            onSelected = { onEvent(TicketsUiEvent.PlatformFilterChanged(it)) },
                        )
                        if (role == UserRole.ADMIN) {
                            FilterBar(
                                label = "Esperando a",
                                options = waitingOnOptions,
                                selected = state.waitingOnFilter,
                                onSelected = { onEvent(TicketsUiEvent.WaitingOnFilterChanged(it)) },
                            )
                        }
                    }
                    when {
                        state.isLoading && state.tickets.isEmpty() -> LoadingState(itemCount = 5)
                        errorMessage != null && state.tickets.isEmpty() -> EmptyState(
                            title = "Cola de tickets no disponible",
                            message = errorMessage,
                        )
                        state.tickets.isEmpty() -> EmptyState(
                            title = "Ningún ticket coincide con estos filtros",
                            message = "Elimina algunos filtros o crea un nuevo ticket para poblar el espacio de trabajo.",
                            actionText = "Crear ticket",
                            onAction = onCreateTicket,
                        )
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            items(state.tickets, key = { it.id }) { ticket ->
                                TicketListItem(
                                    ticket = ticket,
                                    selected = state.selectedTicket?.id == ticket.id,
                                    onClick = { onOpenDetail(ticket) },
                                    showClient = role == UserRole.ADMIN,
                                )
                            }
                        }
                    }
                }
            }

            SectionCard(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                title = "Ticket activo",
                subtitle = "La conversación, el contexto y las acciones siguientes permanecen visibles mientras trabajas la cola.",
            ) {
                Crossfade(
                    targetState = state.selectedTicket,
                    label = "desktopTicketDetail",
                ) { selectedTicket ->
                    if (selectedTicket == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Selecciona un ticket para revisar el hilo, campos de contexto, adjuntos y controles de flujo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        TicketDetailScreen(
                            ticket = selectedTicket,
                            currentRole = role,
                            currentUserId = currentUserId,
                            onReply = onReply,
                            onChangeStatus = onChangeStatus,
                            onChangePriority = onChangePriority,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
