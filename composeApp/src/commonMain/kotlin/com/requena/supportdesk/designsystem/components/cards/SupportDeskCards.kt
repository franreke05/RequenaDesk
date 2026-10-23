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

// Card borders and the opt-in emphasis treatment (one hero card per screen) come from
// CardTreatment.kt and follow the selected visual style.

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
    Card(
        modifier = modifier
            .animateContentSize()
            .supportDeskCardEmphasis(emphasized),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = supportDeskCardBorder(emphasized),
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
    Card(
        modifier = modifier
            .heightIn(min = 132.dp)
            .animateContentSize()
            .supportDeskCardEmphasis(emphasized),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = supportDeskCardBorder(emphasized),
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
