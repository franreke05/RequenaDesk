package com.requena.supportdesk.designsystem.components.badges

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.requena.supportdesk.core.model.ClientAccountStatus
import com.requena.supportdesk.core.model.ClientServiceTier
import com.requena.supportdesk.core.model.PreferredContactChannel
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName

@Composable
fun ClientAccountStatusBadge(
    status: ClientAccountStatus,
    modifier: Modifier = Modifier,
) {
    val semantic = SupportDeskThemeTokens.semanticColors
    val tone = when (status) {
        ClientAccountStatus.ACTIVE -> semantic.successContainer to semantic.success
        ClientAccountStatus.PAUSED -> semantic.warningContainer to semantic.warning
        ClientAccountStatus.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    SupportDeskBadge(
        text = status.displayName(),
        containerColor = tone.first,
        contentColor = tone.second,
        modifier = modifier,
    )
}

@Composable
fun ClientServiceTierBadge(
    tier: ClientServiceTier,
    modifier: Modifier = Modifier,
) {
    val semantic = SupportDeskThemeTokens.semanticColors
    val tone = when (tier) {
        ClientServiceTier.STANDARD -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ClientServiceTier.PRIORITY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        ClientServiceTier.VIP -> semantic.warningContainer to semantic.warning
    }
    SupportDeskBadge(
        text = tier.displayName(),
        containerColor = tone.first,
        contentColor = tone.second,
        modifier = modifier,
    )
}

@Composable
fun PreferredContactChannelBadge(
    channel: PreferredContactChannel,
    modifier: Modifier = Modifier,
) {
    SupportDeskBadge(
        text = channel.displayName(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
