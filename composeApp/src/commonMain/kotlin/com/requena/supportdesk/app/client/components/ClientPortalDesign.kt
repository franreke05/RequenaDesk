package com.requena.supportdesk.app.client.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens

private val ClientPortalCardShape = RoundedCornerShape(8.dp)

// Same "printed panel" trick used on the admin side (see SupportDeskCards.kt): a hard,
// unblurred offset shadow instead of a Material elevation blur. Duplicated locally (not
// imported) so the portal card system stays independent of the admin one.
private fun Modifier.clientPortalHardShadow(
    color: Color,
    offset: Dp = 4.dp,
    cornerRadius: Dp = 8.dp,
): Modifier = drawBehind {
    val offsetPx = offset.toPx()
    val radiusPx = cornerRadius.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(offsetPx, offsetPx),
        size = size,
        cornerRadius = CornerRadius(radiusPx, radiusPx),
    )
}

/**
 * Shared visual language for the customer-facing portal: warm paper surface, hairline ink
 * border by default. Set [emphasized] on at most one hero card per screen for the "printed
 * panel" treatment: a 2dp ink outline plus a hard offset accent shadow. Full-width cards
 * only - never inside a multi-column Row/grid at any breakpoint, since the offset shadow
 * is sized against page padding, not a shared column gap.
 */
@Composable
fun ClientPortalSurfaceCard(
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val emphasisColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier.let { if (emphasized) it.clientPortalHardShadow(emphasisColor) else it },
        shape = ClientPortalCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (emphasized) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            content = content,
        )
    }
}

/** Portal-only replacement for the global accent card, keeping detail screens visually calm. */
@Composable
fun ClientPortalSectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    emphasized: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    ClientPortalSurfaceCard(modifier, emphasized = emphasized) {
        val resolvedTitle = title?.takeIf { it.isNotBlank() }
        val resolvedSubtitle = subtitle?.takeIf { it.isNotBlank() }
        if (resolvedTitle != null || resolvedSubtitle != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    resolvedTitle?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    resolvedSubtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                actions()
            }
        }
        content()
    }
}

@Composable
fun ClientPortalPageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {},
) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        action()
    }
}

@Composable
fun ClientPortalMetric(
    label: String,
    value: String,
    supportingText: String,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = modifier.heightIn(min = 88.dp),
        shape = ClientPortalCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ClientPortalSectionTitle(title: String, supportingText: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        supportingText?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
