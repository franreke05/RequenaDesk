package com.requena.supportdesk.app.client.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.client.monthAbbrev
import com.requena.supportdesk.app.client.monthLabel
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration

// ── MI SERVICIO ───────────────────────────────────────────────────────────────

@Composable
fun ClientServiceScreen(
    tickets: List<Ticket>,
    logs: List<TaskLog>,
    today: String,
    lastMonthMinutes: Int,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val allEntries = logs
    val currentMonthPrefix = today.take(7)

    val currentMonthMinutes = remember(allEntries, currentMonthPrefix) {
        allEntries.filter { it.workDate.take(7) == currentMonthPrefix }.sumOf { it.minutes }
    }
    val minutesDelta = currentMonthMinutes - lastMonthMinutes

    val resolvedThisMonth = remember(tickets, currentMonthPrefix) {
        tickets.count { (it.status == TicketStatus.RESOLVED || it.status == TicketStatus.CLOSED) && it.updatedAt.take(7) == currentMonthPrefix }
    }
    val activeTickets = remember(tickets) {
        tickets.count { it.status != TicketStatus.CLOSED && it.status != TicketStatus.RESOLVED }
    }
    val sixMonthTrend: List<Pair<String, Int>> = remember(logs, today) {
        val yr = today.take(4).toIntOrNull() ?: return@remember emptyList()
        val mo = today.drop(5).take(2).toIntOrNull() ?: return@remember emptyList()
        (0..5).map { offset ->
            var m = mo - offset
            var y = yr
            while (m <= 0) { m += 12; y-- }
            val prefix = "$y-${m.toString().padStart(2, '0')}"
            monthAbbrev(m) to allEntries.filter { it.workDate.take(7) == prefix }.sumOf { it.minutes }
        }.reversed()
    }

    val urgentCount = remember(tickets) {
        tickets.count { it.priority == TicketPriority.URGENT && it.status != TicketStatus.CLOSED && it.status != TicketStatus.RESOLVED }
    }

    val categoryBreakdown = remember(tickets) {
        TicketCategory.entries.map { cat -> cat to tickets.count { it.category == cat } }.filter { it.second > 0 }
    }

    val monthlyHistory: List<Triple<String, Int, Int>> = remember(logs, tickets, today) {
        val yr = today.take(4).toIntOrNull() ?: return@remember emptyList()
        val mo = today.drop(5).take(2).toIntOrNull() ?: return@remember emptyList()
        (0..5).map { offset ->
            var m = mo - offset
            var y = yr
            while (m <= 0) { m += 12; y-- }
            val prefix = "$y-${m.toString().padStart(2, '0')}"
            val fullName = when (m) {
                1 -> "Enero"; 2 -> "Febrero"; 3 -> "Marzo"; 4 -> "Abril"
                5 -> "Mayo"; 6 -> "Junio"; 7 -> "Julio"; 8 -> "Agosto"
                9 -> "Septiembre"; 10 -> "Octubre"; 11 -> "Noviembre"; else -> "Diciembre"
            }
            Triple(
                "$fullName $y",
                tickets.count { it.createdAt.take(7) == prefix },
                allEntries.filter { it.workDate.take(7) == prefix }.sumOf { it.minutes },
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text("Mi Servicio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Resumen de soporte · ${monthLabel(today)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(
            title = monthLabel(today),
            subtitle = "Mes actual",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                MetricCard(
                    label = "Horas este mes",
                    value = formatSupportDeskDuration(currentMonthMinutes),
                    supportingText = "tiempo acumulado",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Mes anterior",
                    value = formatSupportDeskDuration(lastMonthMinutes),
                    supportingText = "para comparar",
                    modifier = Modifier.weight(1f),
                )
            }
            val (deltaContainer, deltaContent) = when {
                minutesDelta > 0 -> semantic.successContainer to semantic.success
                minutesDelta < 0 -> semantic.dangerContainer to semantic.danger
                else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
            }
            SupportDeskBadge(
                text = when {
                    minutesDelta > 0 -> "+${formatSupportDeskDuration(minutesDelta)} vs mes anterior"
                    minutesDelta < 0 -> "−${formatSupportDeskDuration(-minutesDelta)} vs mes anterior"
                    else -> "Igual que el mes anterior"
                },
                containerColor = deltaContainer,
                contentColor = deltaContent,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                MetricCard(
                    label = "Tickets resueltos",
                    value = resolvedThisMonth.toString(),
                    supportingText = "este mes",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Urgentes activos",
                    value = urgentCount.toString(),
                    supportingText = "prioridad urgente",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                MetricCard(
                    label = "Activos",
                    value = activeTickets.toString(),
                    supportingText = "tickets en curso",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Total",
                    value = tickets.size.toString(),
                    supportingText = "tickets registrados",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        SectionCard(title = "Tendencia de soporte", subtitle = "Últimos 6 meses") {
            SupportBarChart(data = sixMonthTrend)
        }

        if (categoryBreakdown.isNotEmpty()) {
            SectionCard(title = "Por categoría", subtitle = "${tickets.size} tickets totales") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    categoryBreakdown.forEach { (cat, count) ->
                        CategoryProgressRow(label = cat.displayName(), count = count, total = tickets.size)
                    }
                }
            }
        }

        SectionCard(title = "Historial mensual") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                monthlyHistory.forEach { (label, ticketCount, minutes) ->
                    MonthlyHistoryRow(label = label, ticketCount = ticketCount, minutes = minutes)
                }
            }
        }
    }
}

@Composable
private fun SupportBarChart(data: List<Pair<String, Int>>) {
    val spacing = SupportDeskThemeTokens.spacing
    if (data.isEmpty() || data.all { it.second == 0 }) {
        Text(
            text = "Los datos aparecerán cuando haya registros de tiempo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val maxVal = data.maxOf { it.second }.coerceAtLeast(1).toFloat()
    var revealed by remember(data) { mutableStateOf(false) }
    LaunchedEffect(data) { revealed = true }
    val revealFraction by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "chartReveal",
    )

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            val barCount = data.size
            val barWidth = (size.width / barCount) * 0.55f
            val gap = (size.width / barCount) * 0.45f
            data.forEachIndexed { index, (_, minutes) ->
                val fraction = (minutes.toFloat() / maxVal) * revealFraction
                val barHeight = size.height * fraction
                val x = gap / 2f + index * (barWidth + gap)
                drawRect(color = trackColor, topLeft = Offset(x, 0f), size = Size(barWidth, size.height))
                drawRect(color = barColor, topLeft = Offset(x, size.height - barHeight), size = Size(barWidth, barHeight))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            data.forEach { (label, _) ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategoryProgressRow(label: String, count: Int, total: Int) {
    val spacing = SupportDeskThemeTokens.spacing
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(110.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.weight(1f))
        Text("$count", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MonthlyHistoryRow(label: String, ticketCount: Int, minutes: Int) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            "$ticketCount ticket${if (ticketCount == 1) "" else "s"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(spacing.md))
        Text(
            formatSupportDeskDuration(minutes),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
