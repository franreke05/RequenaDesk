package com.requena.supportdesk.desktop.screens.tickets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.designsystem.components.badges.SupportPlatformBadge
import com.requena.supportdesk.designsystem.components.badges.TicketCategoryBadge
import com.requena.supportdesk.designsystem.components.badges.TicketPriorityBadge
import com.requena.supportdesk.designsystem.components.badges.TicketStatusBadge
import com.requena.supportdesk.designsystem.components.badges.WaitingOnBadge
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.layout.InfoRow
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.components.layout.VerticalSectionSpacer
import com.requena.supportdesk.designsystem.components.tickets.AttachmentRow
import com.requena.supportdesk.designsystem.components.tickets.CommentBubble
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime

@Composable
fun TicketDetailScreen(
    ticket: Ticket?,
    currentRole: UserRole,
    currentUserId: String?,
    onReply: (String) -> Unit,
    onChangeStatus: (TicketStatus) -> Unit,
    onChangePriority: (TicketPriority) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    if (ticket == null) {
        EmptyState(
            title = "Ningún ticket seleccionado",
            message = "Elige un ticket de la cola para revisar la conversación, el contexto técnico y las siguientes acciones.",
            modifier = modifier,
        )
        return
    }

    val statusOptions = remember {
        TicketStatus.entries.map { FilterOption(value = it, label = it.displayName()) }
    }
    val priorityOptions = remember {
        TicketPriority.entries.map { FilterOption(value = it, label = it.displayName()) }
    }

    SectionCard(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            PageHeader(
                title = ticket.subject,
                subtitle = ticket.description,
                eyebrow = ticket.ticketNumber,
                actions = {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        TicketStatusBadge(ticket.status)
                        TicketPriorityBadge(ticket.priority)
                        WaitingOnBadge(ticket.waitingOn)
                    }
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                Column(
                    modifier = Modifier.weight(0.62f),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    SectionCard(
                        title = "Contexto del problema",
                        subtitle = "Campos técnicos que explican qué está afectado y cómo reproducirlo.",
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) {
                            TicketCategoryBadge(ticket.category)
                            SupportPlatformBadge(ticket.platform)
                            WaitingOnBadge(ticket.waitingOn)
                        }
                        InfoRow(label = "App afectada", value = ticket.affectedApp)
                        InfoRow(label = "Versión", value = ticket.appVersion ?: "-")
                        InfoRow(label = "Referencia", value = ticket.clientReference ?: "-")
                        InfoRow(
                            label = "Pasos para reproducir",
                            value = ticket.stepsToReproduce ?: "El cliente no incluyó pasos para reproducir aún.",
                        )
                    }

                    AnimatedVisibility(visible = currentRole == UserRole.ADMIN) {
                        var replyText by remember { mutableStateOf("") }
                        SectionCard(
                            title = "Comentarios internos",
                            subtitle = "Notas visibles únicamente para el rol de administrador.",
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                if (ticket.internalComments.isEmpty()) {
                                    Text(
                                        text = "Sin comentarios internos aún.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    ticket.internalComments.forEach { comment ->
                                        val isOwn = comment.authorId == currentUserId
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart,
                                        ) {
                                            CommentBubble(
                                                authorName = comment.authorName,
                                                body = comment.body,
                                                timestamp = comment.createdAt,
                                                modifier = Modifier.widthIn(max = 480.dp),
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = spacing.xs),
                                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    OutlinedTextField(
                                        value = replyText,
                                        onValueChange = { replyText = it },
                                        placeholder = { Text("Escribe una respuesta...") },
                                        modifier = Modifier.weight(1f),
                                        maxLines = 4,
                                        shape = MaterialTheme.shapes.medium,
                                    )
                                    Button(
                                        onClick = {
                                            onReply(replyText)
                                            replyText = ""
                                        },
                                        enabled = replyText.isNotBlank(),
                                    ) {
                                        Text("Enviar")
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(0.38f),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    SectionCard(
                        title = "Información del ticket",
                        subtitle = "Metadatos principales, propietario y controles de flujo.",
                    ) {
                        InfoRow(label = "Solicitante", value = ticket.requester.name, supportingText = ticket.requester.email)
                        InfoRow(
                            label = "Asignado a",
                            value = ticket.assignee?.name ?: "Sin asignar",
                            supportingText = ticket.assignee?.email,
                        )
                        InfoRow(label = "Creado", value = formatSupportDeskDateTime(ticket.createdAt))
                        InfoRow(label = "Actualizado", value = formatSupportDeskDateTime(ticket.updatedAt))
                        VerticalSectionSpacer()
                        if (currentRole == UserRole.ADMIN) {
                            Text(
                                text = "Estado",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            FilterBar(
                                label = "Estado",
                                options = statusOptions,
                                selected = ticket.status,
                                onSelected = { selected -> selected?.let(onChangeStatus) },
                            )
                            Text(
                                text = "Prioridad",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            FilterBar(
                                label = "Prioridad",
                                options = priorityOptions,
                                selected = ticket.priority,
                                onSelected = { selected -> selected?.let(onChangePriority) },
                            )
                        }
                    }

                    AnimatedVisibility(visible = !ticket.resolutionSummary.isNullOrBlank()) {
                        SectionCard(
                            title = "Resumen de resolución",
                            subtitle = "Nota de cierre breve que puede reutilizarse en informes y futuros triajes.",
                        ) {
                            Text(
                                text = ticket.resolutionSummary.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    SectionCard(
                        title = "Adjuntos",
                        subtitle = if (ticket.attachments.isEmpty()) {
                            "Aún no se han subido archivos para este ticket."
                        } else {
                            "${ticket.attachments.size} archivos disponibles."
                        },
                    ) {
                        if (ticket.attachments.isEmpty()) {
                            EmptyState(
                                title = "Sin adjuntos",
                                message = "Los archivos subidos en las respuestas aparecerán aquí cuando ese flujo de backend esté conectado.",
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                                ticket.attachments.forEach { attachment ->
                                    AttachmentRow(attachment)
                                }
                            }
                        }
                    }

                    SectionCard(
                        title = "Historial de eventos",
                        subtitle = "Registro de auditoría compacto de los cambios de estado del ticket.",
                    ) {
                        if (ticket.events.isEmpty()) {
                            EmptyState(
                                title = "Sin historial aún",
                                message = "Los eventos aparecerán aquí a medida que el flujo de trabajo se complete.",
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                ticket.events.forEach { event ->
                                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                                        Text(
                                            text = event.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            text = "${event.actorName}  ${formatSupportDeskDateTime(event.createdAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
