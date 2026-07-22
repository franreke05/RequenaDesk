package com.requena.supportdesk.app.client.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.client.initials
import com.requena.supportdesk.app.client.components.ClientPortalMetric
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.app.client.components.ClientPortalSectionTitle
import com.requena.supportdesk.app.client.components.ClientPortalSurfaceCard
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.displayName
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints

/** Settings-oriented account screen; operational work belongs in Inicio and Trabajo. */
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
    val thisMonthTickets = remember(tickets, today) { tickets.count { it.createdAt.take(7) == today.take(7) } }
    val thisMonthMinutes = remember(logs, today) { logs.filter { it.workDate.take(7) == today.take(7) }.sumOf { it.minutes } }
    val initials = remember(clientName) { clientName.initials() }
    val planName = when (client?.serviceTier?.name) {
        "PRIORITY" -> "Plan prioritario"
        "VIP" -> "Plan VIP"
        else -> "Plan esencial"
    }
    val planDescription = when (client?.serviceTier?.name) {
        "PRIORITY" -> "Atencion prioritaria para solicitudes y seguimiento."
        "VIP" -> "Acompanamiento y soporte dedicado."
        else -> "Seguimiento de solicitudes y trabajo compartido."
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        ClientPortalPageHeader(
            title = "Cuenta",
            subtitle = "Perfil, configuracion del servicio y acceso al portal.",
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= SupportDeskBreakpoints.clientWide) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    ProfileCard(clientName, contactName, client, initials, Modifier.weight(1.15f))
                    PlanCard(planName, planDescription, client, Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    ProfileCard(clientName, contactName, client, initials, Modifier.fillMaxWidth())
                    PlanCard(planName, planDescription, client, Modifier.fillMaxWidth())
                }
            }
        }

        ClientPortalSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            ClientPortalSectionTitle("Preferencias del servicio", "Informacion que el equipo utiliza para atenderte.")
            client?.let {
                AccountDetailRow("Producto", it.productName)
                AccountDetailRow("Correo", it.email)
                AccountDetailRow("Canal preferido", it.preferredContactChannel.name)
            } ?: Text(
                "La informacion de la cuenta se actualizara al sincronizar el portal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ClientPortalSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            ClientPortalSectionTitle("Componentes habilitados", "Las utilidades adicionales se gestionan desde Programas.")
            val components = client?.enabledComponents.orEmpty().sortedBy { it.name }
            if (components.isEmpty()) {
                Text(
                    "Tu portal incluye el seguimiento esencial de solicitudes y trabajo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                components.forEach { component -> AccountDetailRow(component.displayName(), "Activo") }
            }
        }

        ClientPortalSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            ClientPortalSectionTitle("Actividad de este mes", "Una referencia breve para tu seguimiento.")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                ClientPortalMetric("Solicitudes", thisMonthTickets.toString(), "creadas este mes", Modifier.weight(1f))
                ClientPortalMetric("Soporte", formatSupportDeskDuration(thisMonthMinutes), "tiempo registrado", Modifier.weight(1f))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
            SecondaryButton(text = "Actualizar datos", onClick = onRefresh)
            Surface(
                onClick = onSignOut,
                color = SupportDeskThemeTokens.semanticColors.dangerContainer,
                contentColor = SupportDeskThemeTokens.semanticColors.danger,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = "Cerrar sesion",
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    clientName: String,
    contactName: String,
    client: Client?,
    initials: String,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    // The client's own identity card - the natural hero of this screen.
    ClientPortalSurfaceCard(modifier, emphasized = true) {
        ClientPortalSectionTitle("Perfil", "Identidad visible en el portal")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thin ink ring around the avatar - a small "letterpress stamp" touch.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(clientName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (contactName.isNotBlank()) {
                    Text(contactName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        SupportDeskBadge(
            text = when (client?.accountStatus?.name) {
                "PAUSED" -> "Cuenta pausada"
                "INACTIVE" -> "Cuenta inactiva"
                else -> "Portal activo"
            },
            containerColor = SupportDeskThemeTokens.semanticColors.successContainer,
            contentColor = SupportDeskThemeTokens.semanticColors.success,
        )
    }
}

@Composable
private fun PlanCard(
    planName: String,
    planDescription: String,
    client: Client?,
    modifier: Modifier = Modifier,
) {
    ClientPortalSurfaceCard(modifier) {
        ClientPortalSectionTitle("Plan de soporte", "Configuracion actual de la cuenta")
        Text(planName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(planDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SupportDeskBadge(
            text = client?.serviceTier?.name?.replaceFirstChar { it.titlecase() } ?: "Standard",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun AccountDetailRow(label: String, value: String) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
