package com.requena.supportdesk.app.client.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.client.ClientDailyUrgentLimit
import com.requena.supportdesk.app.client.ClientNotice
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.designsystem.components.badges.SupportPlatformBadge
import com.requena.supportdesk.designsystem.components.badges.TicketCategoryBadge
import com.requena.supportdesk.designsystem.components.badges.TicketPriorityBadge
import com.requena.supportdesk.designsystem.components.badges.TicketStatusBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent

// ── NEW TICKET ────────────────────────────────────────────────────────────────

@Composable
fun ClientNewTicketScreen(
    urgentToday: Int,
    isLoading: Boolean,
    errorMessage: String?,
    onEvent: (TicketsUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var subject by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(TicketCategory.QUESTION) }
    var platform by rememberSaveable { mutableStateOf(SupportPlatform.DESKTOP) }
    var priority by rememberSaveable { mutableStateOf(TicketPriority.MEDIUM) }
    val urgentLimitReached = priority == TicketPriority.URGENT && urgentToday >= ClientDailyUrgentLimit
    val categoryOptions = remember { TicketCategory.entries.map { FilterOption(it, it.displayName()) } }
    val platformOptions = remember { SupportPlatform.entries.map { FilterOption(it, it.displayName()) } }
    val priorityOptions = remember { TicketPriority.entries.map { FilterOption(it, it.displayName()) } }

    // Shared form content — called in both wide (left col) and narrow (single col) layouts
    val formContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text("Nuevo ticket", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Máximo $ClientDailyUrgentLimit tickets urgentes por día.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val rem = ClientDailyUrgentLimit - urgentToday
        SectionCard(
            modifier = Modifier.fillMaxWidth(),
            title = "Clasificación",
            subtitle = "$rem urgente${if (rem == 1) "" else "s"} disponible${if (rem == 1) "" else "s"}",
        ) {
            UrgentSlotsBar(
                used = urgentToday,
                limit = ClientDailyUrgentLimit,
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedVisibility(
                visible = priority == TicketPriority.URGENT && !urgentLimitReached,
                enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { -it / 2 },
                exit = fadeOut(tween(180)),
            ) {
                ClientNotice(
                    message = "Prioridad urgente ($urgentToday/$ClientDailyUrgentLimit usados). Solo para bloqueos críticos.",
                    isError = false,
                )
            }
            FilterBar(
                label = "Prioridad",
                options = priorityOptions,
                selected = priority,
                onSelected = { priority = it ?: TicketPriority.MEDIUM },
                wrap = true,
                allLabel = "Media",
            )
            FilterBar(
                label = "Tipo",
                options = categoryOptions,
                selected = category,
                onSelected = { category = it ?: TicketCategory.QUESTION },
                allLabel = "Consulta",
                wrap = true,
            )
            FilterBar(
                label = "Plataforma",
                options = platformOptions,
                selected = platform,
                onSelected = { platform = it ?: SupportPlatform.DESKTOP },
                allLabel = "Escritorio",
                wrap = true,
            )
        }
        SectionCard(
            modifier = Modifier.fillMaxWidth(),
            title = "Descripción",
            subtitle = "Detalla el problema para ayudarte mejor",
        ) {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Asunto") },
                singleLine = true,
                enabled = !urgentLimitReached,
                shape = RoundedCornerShape(8.dp),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Detalle del problema") },
                minLines = 5,
                enabled = !urgentLimitReached,
                shape = RoundedCornerShape(8.dp),
            )
            AnimatedVisibility(visible = urgentLimitReached, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
                ClientNotice(
                    message = "Límite de urgentes alcanzado ($ClientDailyUrgentLimit/día). Cambia la prioridad o vuelve mañana.",
                    isError = true,
                )
            }
            errorMessage?.let { ClientNotice(message = it, isError = true) }
            PrimaryButton(
                text = if (isLoading) "Enviando..." else "Enviar ticket",
                onClick = {
                    onEvent(
                        TicketsUiEvent.CreateTicket(
                            CreateTicketInput(
                                subject = subject.trim(),
                                description = description.trim(),
                                category = category,
                                platform = platform,
                                priority = priority,
                            ),
                        ),
                    )
                    subject = ""
                    description = ""
                    priority = TicketPriority.MEDIUM
                },
                enabled = !urgentLimitReached && !isLoading && subject.isNotBlank() && description.isNotBlank(),
                fullWidth = true,
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= SupportDeskBreakpoints.clientWide
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xl),
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.56f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    formContent()
                }
                Column(
                    modifier = Modifier
                        .weight(0.44f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = spacing.xl, end = spacing.xl, bottom = spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    TicketPreviewCard(
                        subject = subject,
                        description = description,
                        category = category,
                        platform = platform,
                        priority = priority,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                formContent()
                TicketPreviewCard(
                    subject = subject,
                    description = description,
                    category = category,
                    platform = platform,
                    priority = priority,
                )
            }
        }
    }
}

@Composable
private fun UrgentSlotsBar(
    used: Int,
    limit: Int,
    modifier: Modifier = Modifier,
) {
    val semantic = SupportDeskThemeTokens.semanticColors
    val slotColors = listOf(semantic.info, semantic.warning, semantic.danger)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(limit) { index ->
            val filled = index < used
            val accent = slotColors.getOrNull(index) ?: semantic.danger
            val slotColor by animateColorAsState(
                targetValue = if (filled) accent else accent.copy(alpha = 0.15f),
                animationSpec = tween(350),
                label = "urgentSlot_$index",
            )
            val slotHeight by animateDpAsState(
                targetValue = if (filled) 10.dp else 6.dp,
                animationSpec = tween(350),
                label = "urgentSlotH_$index",
            )
            Surface(
                modifier = Modifier.weight(1f).height(slotHeight),
                shape = RoundedCornerShape(5.dp),
                color = slotColor,
            ) {}
        }
    }
}

@Composable
private fun TicketPreviewCard(
    subject: String,
    description: String,
    category: TicketCategory,
    platform: SupportPlatform,
    priority: TicketPriority,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val hasContent = subject.isNotBlank() || description.isNotBlank()
    val completeness = remember(subject, description) {
        var p = 0f
        if (subject.isNotBlank()) p += 0.40f
        if (description.length >= 10) p += 0.30f
        if (description.length >= 60) p += 0.20f
        if (description.length >= 120) p += 0.10f
        p.coerceIn(0f, 1f)
    }
    val animatedCompleteness by animateFloatAsState(completeness, tween(500), label = "completeness")
    val completenessColor = when {
        completeness >= 0.9f -> semantic.success
        completeness >= 0.4f -> MaterialTheme.colorScheme.primary
        completeness > 0f -> semantic.warning
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    SectionCard(
        modifier = modifier,
        title = "Vista previa",
        subtitle = if (!hasContent) "Completa el formulario" else "${(animatedCompleteness * 100).toInt()}% completado",
    ) {
        // Live indicator dot
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (hasContent) semantic.success else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    )
            )
            Text(
                text = if (hasContent) "En progreso" else "Sin datos aún",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Mock ticket card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)),
        ) {
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "#????",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TicketStatusBadge(status = TicketStatus.OPEN)
                }
                Text(
                    text = subject.ifBlank { "Sin asunto" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (subject.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (subject.isNotBlank()) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description.take(120).ifBlank { "Sin descripción" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (description.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    AnimatedContent(
                        targetState = priority,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "preview_priority",
                    ) { p -> TicketPriorityBadge(priority = p) }
                    AnimatedContent(
                        targetState = category,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "preview_category",
                    ) { c -> TicketCategoryBadge(category = c) }
                    AnimatedContent(
                        targetState = platform,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "preview_platform",
                    ) { pl -> SupportPlatformBadge(platform = pl) }
                }
            }
        }

        // Completeness bar
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Completado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${(animatedCompleteness * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = completenessColor,
                )
            }
            LinearProgressIndicator(
                progress = { animatedCompleteness },
                modifier = Modifier.fillMaxWidth(),
                color = completenessColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}
