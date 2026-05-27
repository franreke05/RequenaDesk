package com.requena.supportdesk.desktop.screens.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.ClientServiceTier
import com.requena.supportdesk.designsystem.components.badges.ClientAccountStatusBadge
import com.requena.supportdesk.designsystem.components.badges.ClientServiceTierBadge
import com.requena.supportdesk.designsystem.components.badges.PreferredContactChannelBadge
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.SearchField
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.clients.presentation.event.ClientsUiEvent
import com.requena.supportdesk.features.clients.presentation.state.ClientsUiState

@Composable
fun ClientListScreen(
    state: ClientsUiState,
    onEvent: (ClientsUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val activeLoad = state.clients.count { it.activeTicketCount > 0 }
    val priorityTier = state.clients.count { it.serviceTier == ClientServiceTier.PRIORITY || it.serviceTier == ClientServiceTier.VIP }
    val errorMessage = state.errorMessage

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        PageHeader(
            title = "Clients",
            subtitle = "A clear administrative view of product ownership, service tier and current support load.",
            eyebrow = "Admin directory",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            MetricCard(
                label = "Accounts",
                value = state.clients.size.toString(),
                supportingText = "Visible client records after the current search.",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "With active tickets",
                value = activeLoad.toString(),
                supportingText = "Clients that currently need follow-up work.",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Priority coverage",
                value = priorityTier.toString(),
                supportingText = "Clients on priority or VIP support tiers.",
                modifier = Modifier.weight(1f),
            )
        }

        SectionCard(
            title = "Client directory",
            subtitle = "Search by company, product, contact or email and scan support posture at a glance.",
        ) {
            SearchField(
                value = state.query,
                onValueChange = { onEvent(ClientsUiEvent.SearchChanged(it)) },
                placeholder = "Search company, product, contact or email",
            )
            when {
                state.isLoading && state.clients.isEmpty() -> LoadingState(itemCount = 4)
                errorMessage != null && state.clients.isEmpty() -> EmptyState(
                    title = "Client list unavailable",
                    message = errorMessage,
                )
                state.clients.isEmpty() -> EmptyState(
                    title = "No clients found",
                    message = "Adjust the search or add the first client record when the real admin flow is ready.",
                )
                else -> {
                    ClientHeaderRow()
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        items(state.clients, key = { it.id }) { client ->
                            val index = state.clients.indexOf(client)
                            val rowBg = if (index % 2 == 0)
                                MaterialTheme.colorScheme.surface
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            Row(
                                modifier = Modifier.fillMaxWidth().background(rowBg),
                                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                            ) {
                                Column(
                                    modifier = Modifier.weight(0.24f),
                                    verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                                ) {
                                    Text(client.companyName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = client.productName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(0.2f),
                                    verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                                ) {
                                    Text(
                                        text = client.contactName,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = client.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Row(
                                    modifier = Modifier.weight(0.16f),
                                    horizontalArrangement = Arrangement.Start,
                                ) {
                                    PreferredContactChannelBadge(client.preferredContactChannel)
                                }
                                Row(
                                    modifier = Modifier.weight(0.14f),
                                    horizontalArrangement = Arrangement.Start,
                                ) {
                                    ClientServiceTierBadge(client.serviceTier)
                                }
                                Row(
                                    modifier = Modifier.weight(0.14f),
                                    horizontalArrangement = Arrangement.Start,
                                ) {
                                    ClientAccountStatusBadge(client.accountStatus)
                                }
                                Row(
                                    modifier = Modifier.weight(0.12f),
                                    horizontalArrangement = Arrangement.Start,
                                ) {
                                    SupportDeskBadge(
                                        text = "${client.activeTicketCount} open",
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientHeaderRow() {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = "Client / product",
            modifier = Modifier.weight(0.24f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Contact",
            modifier = Modifier.weight(0.2f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Channel",
            modifier = Modifier.weight(0.16f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Tier",
            modifier = Modifier.weight(0.14f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Status",
            modifier = Modifier.weight(0.14f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Open",
            modifier = Modifier.weight(0.12f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
