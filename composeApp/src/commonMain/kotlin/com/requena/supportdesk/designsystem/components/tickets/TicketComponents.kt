package com.requena.supportdesk.designsystem.components.tickets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Attachment
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.badges.SupportPlatformBadge
import com.requena.supportdesk.designsystem.components.badges.TicketCategoryBadge
import com.requena.supportdesk.designsystem.components.badges.TicketPriorityBadge
import com.requena.supportdesk.designsystem.components.badges.TicketStatusBadge
import com.requena.supportdesk.designsystem.components.badges.WaitingOnBadge
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime

@Composable
fun TicketListItem(
    ticket: Ticket,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showClient: Boolean = true,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val backgroundColor = when {
        selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        hovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val animatedBackground = animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(durationMillis = SupportDeskMotion.quick),
        label = "ticketItemBackground",
    )
    val animatedTextColor = animateColorAsState(
        targetValue = textColor,
        animationSpec = tween(durationMillis = SupportDeskMotion.regular),
        label = "ticketItemText",
    )
    val animatedElevation = animateDpAsState(
        targetValue = if (selected) SupportDeskThemeTokens.elevations.raised else if (hovered) SupportDeskThemeTokens.elevations.subtle * 2f else SupportDeskThemeTokens.elevations.subtle,
        animationSpec = tween(durationMillis = SupportDeskMotion.quick),
        label = "ticketItemElevation",
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        color = animatedBackground.value,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = animatedElevation.value,
        border = BorderStroke(
            width = if (selected) 1.dp else 0.dp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(spacing.md)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                ) {
                    Text(
                        text = "${ticket.ticketNumber}  ${ticket.affectedApp}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = ticket.subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = animatedTextColor.value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = ticket.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TicketPriorityBadge(priority = ticket.priority)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                TicketStatusBadge(ticket.status)
                WaitingOnBadge(ticket.waitingOn)
                TicketCategoryBadge(ticket.category)
                SupportPlatformBadge(ticket.platform)
                if (ticket.attachments.isNotEmpty()) {
                    SupportDeskBadge(
                        text = "${ticket.attachments.size} files",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = buildString {
                    if (showClient) {
                        append(ticket.requester.name)
                        append("   ")
                    }
                    append("Updated ")
                    append(formatSupportDeskDateTime(ticket.updatedAt))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun MessageBubble(
    authorName: String,
    body: String,
    timestamp: String,
    isOwnMessage: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.78f),
            shape = MaterialTheme.shapes.large,
            color = if (isOwnMessage) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            tonalElevation = SupportDeskThemeTokens.elevations.subtle,
        ) {
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = authorName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = formatSupportDeskDateTime(timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun CommentBubble(
    authorName: String,
    body: String,
    timestamp: String,
    modifier: Modifier = Modifier,
) {
    val semantic = SupportDeskThemeTokens.semanticColors
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = semantic.warningContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = "Internal comment  $authorName",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = semantic.warning,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = formatSupportDeskDateTime(timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AttachmentChip(
    attachment: Attachment,
    modifier: Modifier = Modifier,
) {
    SupportDeskBadge(
        text = attachment.fileName,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun AttachmentRow(
    attachment: Attachment,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), MaterialTheme.shapes.medium)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${attachment.contentType}  ${attachment.sizeBytes} bytes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatSupportDeskDateTime(attachment.uploadedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
