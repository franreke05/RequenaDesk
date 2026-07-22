package com.requena.supportdesk.designsystem.components.badges

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.WaitingOn
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName

@Composable
fun TicketStatusBadge(
    status: TicketStatus,
    modifier: Modifier = Modifier,
) {
    val semantic = SupportDeskThemeTokens.semanticColors
    val tone = when (status) {
        TicketStatus.OPEN -> semantic.infoContainer to semantic.info
        TicketStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        TicketStatus.PENDING_CLIENT -> semantic.warningContainer to semantic.warning
        TicketStatus.RESOLVED -> semantic.successContainer to semantic.success
        TicketStatus.CLOSED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    SupportDeskBadge(
        text = status.displayName(),
        containerColor = tone.first,
        contentColor = tone.second,
        modifier = modifier,
    )
}

@Composable
fun TicketPriorityBadge(
    priority: TicketPriority,
    modifier: Modifier = Modifier,
) {
    val semantic = SupportDeskThemeTokens.semanticColors
    val tone = when (priority) {
        TicketPriority.LOW -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        TicketPriority.MEDIUM -> semantic.infoContainer to semantic.info
        TicketPriority.HIGH -> semantic.warningContainer to semantic.warning
        TicketPriority.URGENT -> semantic.dangerContainer to semantic.danger
    }
    SupportDeskBadge(
        text = priority.displayName(),
        containerColor = tone.first,
        contentColor = tone.second,
        modifier = modifier,
    )
}

@Composable
fun TicketCategoryBadge(
    category: TicketCategory,
    modifier: Modifier = Modifier,
) {
    SupportDeskBadge(
        text = category.displayName(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
    )
}

@Composable
fun SupportPlatformBadge(
    platform: SupportPlatform,
    modifier: Modifier = Modifier,
) {
    SupportDeskBadge(
        text = platform.displayName(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun WaitingOnBadge(
    waitingOn: WaitingOn,
    modifier: Modifier = Modifier,
) {
    val semantic = SupportDeskThemeTokens.semanticColors
    val tone = when (waitingOn) {
        WaitingOn.CLIENT -> semantic.warningContainer to semantic.warning
        WaitingOn.ADMIN -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    }
    SupportDeskBadge(
        text = waitingOn.displayName(),
        containerColor = tone.first,
        contentColor = tone.second,
        modifier = modifier,
    )
}

@Composable
fun SupportDeskBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val animatedContainerColor = animateColorAsState(
        targetValue = containerColor,
        animationSpec = tween(durationMillis = SupportDeskMotion.regular),
        label = "badgeContainer",
    )
    val animatedContentColor = animateColorAsState(
        targetValue = contentColor,
        animationSpec = tween(durationMillis = SupportDeskMotion.regular),
        label = "badgeContent",
    )
    // Comic "caption" treatment: bold italic uppercase, like an action-word panel
    // caption, plus a thin ink border so every badge reads as a printed stamp.
    Surface(
        modifier = modifier,
        color = animatedContainerColor.value,
        contentColor = animatedContentColor.value,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, animatedContentColor.value),
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            letterSpacing = 0.4.sp,
        )
    }
}
