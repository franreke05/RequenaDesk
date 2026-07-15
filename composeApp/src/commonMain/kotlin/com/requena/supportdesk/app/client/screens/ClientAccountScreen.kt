package com.requena.supportdesk.app.client.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.client.initials
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration

// ── MI CUENTA ─────────────────────────────────────────────────────────────────

@Composable
fun ClientAccountScreen(
    clientName: String,
    contactName: String = "",
    client: Client?,
    tickets: List<Ticket>,
    logs: List<TaskLog>,
    today: String,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val allEntries = logs
    val totalTickets = tickets.size
    val resolvedTickets = remember(tickets) { tickets.count { it.status == TicketStatus.RESOLVED || it.status == TicketStatus.CLOSED } }
    val totalMinutes = remember(allEntries) { allEntries.sumOf { it.minutes } }
    val thisMonthTickets = remember(tickets, today) { tickets.count { it.createdAt.take(7) == today.take(7) } }
    val thisMonthMinutes = remember(allEntries, today) { allEntries.filter { it.workDate.take(7) == today.take(7) }.sumOf { it.minutes } }
    val lastTicketDate = remember(tickets) { tickets.maxOfOrNull { it.updatedAt }?.take(10) ?: "—" }
    val initials = remember(clientName) { clientName.initials() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text("Mi Cuenta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Perfil y acceso al portal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "Perfil") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = clientName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (contactName.isNotBlank()) {
                        Text(
                            text = contactName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        SupportDeskBadge(
                            text = when (client?.accountStatus?.name) {
                                "PAUSED" -> "Cuenta pausada"
                                "INACTIVE" -> "Cuenta inactiva"
                                else -> "Portal activo"
                            },
                            containerColor = SupportDeskThemeTokens.semanticColors.successContainer,
                            contentColor = SupportDeskThemeTokens.semanticColors.success,
                        )
                        SupportDeskBadge(
                            text = when (client?.serviceTier?.name) {
                                "PRIORITY" -> "Plan prioritario"
                                "VIP" -> "Plan VIP"
                                else -> "Plan standard"
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }

        SectionCard(title = "Estadísticas globales") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    MetricCard(
                        label = "Total tickets",
                        value = totalTickets.toString(),
                        supportingText = "desde el inicio",
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        label = "Resueltos",
                        value = resolvedTickets.toString(),
                        supportingText = "cerrados o resueltos",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    MetricCard(
                        label = "Horas de soporte",
                        value = formatSupportDeskDuration(totalMinutes),
                        supportingText = "tiempo total",
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        label = "Activos",
                        value = (totalTickets - resolvedTickets).toString(),
                        supportingText = "tickets en curso",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        SectionCard(title = "Plan de soporte", subtitle = "Configuracion actual de la cuenta") {
            val tierName = when (client?.serviceTier?.name) {
                "PRIORITY" -> "Prioritario"
                "VIP" -> "VIP"
                else -> "Standard"
            }
            val tierDescription = when (client?.serviceTier?.name) {
                "PRIORITY" -> "Atencion prioritaria"
                "VIP" -> "Soporte dedicado"
                else -> "Soporte estandar"
            }
            ServiceTierCard(name = tierName, description = tierDescription, modifier = Modifier.fillMaxWidth())
            client?.let {
                AccountStatRow(label = "Producto", value = it.productName)
                AccountStatRow(label = "Correo", value = it.email)
                AccountStatRow(label = "Canal preferido", value = it.preferredContactChannel.name)
            }
        }

        SectionCard(title = "Resumen de actividad") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                AccountStatRow(label = "Último ticket actualizado", value = lastTicketDate)
                AccountStatRow(label = "Tickets este mes", value = thisMonthTickets.toString())
                AccountStatRow(label = "Horas este mes", value = formatSupportDeskDuration(thisMonthMinutes))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
            SecondaryButton(text = "Actualizar", onClick = onRefresh)
            Surface(
                onClick = onSignOut,
                color = SupportDeskThemeTokens.semanticColors.dangerContainer,
                contentColor = SupportDeskThemeTokens.semanticColors.danger,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "Cerrar sesión",
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ServiceTierCard(name: String, description: String, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountStatRow(label: String, value: String) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}
