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
            title = "Clientes",
            subtitle = "Vista administrativa clara de la titularidad de producto, nivel de servicio y carga de soporte actual.",
            eyebrow = "Directorio de administración",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            MetricCard(
                label = "Cuentas",
                value = state.clients.size.toString(),
                supportingText = "Registros de clientes visibles tras la búsqueda actual.",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Con tickets activos",
                value = activeLoad.toString(),
                supportingText = "Clientes que actualmente necesitan seguimiento.",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Cobertura prioritaria",
                value = priorityTier.toString(),
                supportingText = "Clientes en niveles de soporte prioritario o VIP.",
                modifier = Modifier.weight(1f),
            )
        }

        SectionCard(
            title = "Directorio de clientes",
            subtitle = "Busca por empresa, producto, contacto o correo y revisa la postura de soporte de un vistazo.",
        ) {
            SearchField(
                value = state.query,
                onValueChange = { onEvent(ClientsUiEvent.SearchChanged(it)) },
                placeholder = "Buscar empresa, producto, contacto o correo",
            )
            when {
                state.isLoading && state.clients.isEmpty() -> LoadingState(itemCount = 4)
                errorMessage != null && state.clients.isEmpty() -> EmptyState(
                    title = "Lista de clientes no disponible",
                    message = errorMessage,
                )
                state.clients.isEmpty() -> EmptyState(
                    title = "No se encontraron clientes",
                    message = "Ajusta la búsqueda o añade el primer registro de cliente cuando el flujo de admin esté listo.",
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
                                        text = "${client.activeTicketCount} abiertos",
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
            text = "Cliente / producto",
            modifier = Modifier.weight(0.24f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Contacto",
            modifier = Modifier.weight(0.2f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Canal",
            modifier = Modifier.weight(0.16f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Nivel",
            modifier = Modifier.weight(0.14f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Estado",
            modifier = Modifier.weight(0.14f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Abiertos",
            modifier = Modifier.weight(0.12f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
