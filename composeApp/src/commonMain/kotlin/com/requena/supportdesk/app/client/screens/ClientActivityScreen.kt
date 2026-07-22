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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.client.ClientActivityItem
import com.requena.supportdesk.app.client.ClientActivityType
import com.requena.supportdesk.app.client.isoDateMinus
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.X

// ── ACTIVIDAD ─────────────────────────────────────────────────────────────────

@Composable
fun ClientActivityScreen(activityItems: List<ClientActivityItem>) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val today = remember { currentIsoDate() }
    var activityFilter by remember { mutableStateOf<String?>(null) }

    val filteredItems = remember(activityItems, activityFilter, today) {
        when (activityFilter) {
            "week" -> {
                val weekStart = isoDateMinus(today, 7)
                activityItems.filter { it.date >= weekStart }
            }
            "month" -> activityItems.filter { it.date.take(7) == today.take(7) }
            else -> activityItems
        }
    }

    val totalMinutes = remember(filteredItems) { filteredItems.sumOf { it.minutes } }
    val statusChangeCount = remember(filteredItems) {
        filteredItems.count { it.type == ClientActivityType.STATUS_CHANGE || it.type == ClientActivityType.RESOLVED || it.type == ClientActivityType.CLOSED }
    }

    val grouped = remember(filteredItems) {
        filteredItems.groupBy { it.date }.toList().sortedByDescending { it.first }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        ClientPortalPageHeader(
            title = "Actividad",
            subtitle = "Historial de eventos de tus tickets.",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            listOf(null to "Todo", "week" to "Esta semana", "month" to "Este mes").forEach { (key, label) ->
                FilterChip(
                    selected = activityFilter == key,
                    onClick = { activityFilter = key },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            SupportDeskBadge(
                text = "${filteredItems.size} eventos",
                containerColor = semantic.infoContainer,
                contentColor = semantic.info,
            )
            if (totalMinutes > 0) {
                SupportDeskBadge(
                    text = formatSupportDeskDuration(totalMinutes),
                    containerColor = semantic.successContainer,
                    contentColor = semantic.success,
                )
            }
            SupportDeskBadge(
                text = "$statusChangeCount cambios de estado",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }

        if (filteredItems.isEmpty()) {
            EmptyState(
                title = "Sin actividad",
                message = "No hay eventos en el período seleccionado.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                grouped.forEach { (date, dayItems) ->
                    item(key = "header_$date") {
                        Text(
                            text = formatSupportDeskDateTime(date),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = spacing.xs),
                        )
                    }
                    items(dayItems, key = { "${it.date}_${it.ticketNumber}_${it.type}_${it.description.take(20)}" }) { item ->
                        ActivityItemRow(item = item, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityItemRow(item: ClientActivityItem, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            ActivityTypeChip(type = item.type)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = item.description,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.ticketNumber.isNotBlank()) {
                        SupportDeskBadge(
                            text = item.ticketNumber,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Text(
                    text = item.ticketSubject,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.actor.isNotBlank()) {
                    Text(
                        text = item.actor,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTypeChip(type: ClientActivityType) {
    val semantic = SupportDeskThemeTokens.semanticColors
    val visual = when (type) {
        ClientActivityType.CREATED -> ActivityTypeVisual(semantic.infoContainer, semantic.info, Lucide.Plus, "Ticket creado")
        ClientActivityType.STATUS_CHANGE -> ActivityTypeVisual(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, Lucide.Activity, "Cambio de estado")
        ClientActivityType.TIME_LOGGED -> ActivityTypeVisual(semantic.successContainer, semantic.success, Lucide.Clock, "Tiempo registrado")
        ClientActivityType.RESOLVED -> ActivityTypeVisual(semantic.successContainer, semantic.success, Lucide.CircleCheck, "Ticket resuelto")
        ClientActivityType.CLOSED -> ActivityTypeVisual(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Lucide.X, "Ticket cerrado")
        ClientActivityType.RATED -> ActivityTypeVisual(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary, Lucide.Star, "Valoración")
    }
    Box(
        modifier = Modifier
            .padding(top = 2.dp)
            .size(28.dp)
            .background(visual.background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = visual.description,
            tint = visual.foreground,
            modifier = Modifier.size(16.dp),
        )
    }
}

private data class ActivityTypeVisual(
    val background: Color,
    val foreground: Color,
    val icon: ImageVector,
    val description: String,
)
