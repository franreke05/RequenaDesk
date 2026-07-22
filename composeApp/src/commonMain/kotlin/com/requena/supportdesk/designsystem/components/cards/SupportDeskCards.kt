package com.requena.supportdesk.designsystem.components.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion

private val CardTextCrossfade =
    fadeIn(tween(SupportDeskMotion.regular)) togetherWith fadeOut(tween(SupportDeskMotion.quick))

// Editorial "hairline" card border - a thin neutral rule, never a glassy shadow.
// Mirrors the portfolio's --border token: cards are defined by a crisp outline,
// not elevation. MaterialTheme.colorScheme.outlineVariant resolves per light/dark theme.
@Composable
private fun hairlineCardBorder(): BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

// Opt-in "printed panel" treatment for the rare card that should stand out (e.g. a
// hero metric or the active timer card): ink-colored outline + a hard, unblurred
// offset shadow in the accent color, borrowed from the portfolio's .comic-ink-outline
// utility. Deliberately not the default card style - used sparingly, like the source.
private fun Modifier.hardOffsetShadow(
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

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    emphasized: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val resolvedTitle = title?.takeIf { it.isNotBlank() }
    val resolvedSubtitle = subtitle?.takeIf { it.isNotBlank() }
    val emphasisColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier
            .animateContentSize()
            .let { if (emphasized) it.hardOffsetShadow(emphasisColor) else it },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (emphasized) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
        } else {
            hairlineCardBorder()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (resolvedTitle != null || resolvedSubtitle != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                    ) {
                        resolvedTitle?.let {
                            AnimatedContent(
                                targetState = it,
                                transitionSpec = { CardTextCrossfade },
                                label = "sectionCardTitle",
                            ) { titleValue ->
                                Text(titleValue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        resolvedSubtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    actions()
                }
            }
            content()
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    supportingText: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val emphasisColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier
            .heightIn(min = 132.dp)
            .animateContentSize()
            .let { if (emphasized) it.hardOffsetShadow(emphasisColor) else it },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (emphasized) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
        } else {
            hairlineCardBorder()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedContent(
                targetState = value,
                transitionSpec = { CardTextCrossfade },
                label = "metricCardValue",
            ) { valueText ->
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
