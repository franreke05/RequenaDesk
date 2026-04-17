package com.requena.supportdesk.designsystem.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens

@Composable
fun <T> AdminNavigationRail(
    items: List<NavigationItemSpec<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(112.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = SupportDeskThemeTokens.elevations.subtle,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = spacing.sm, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items.forEach { item ->
                val isSelected = item.key == selected
                val containerColor = animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    animationSpec = tween(SupportDeskMotion.regular),
                    label = "railItemContainer",
                )
                val contentColor = animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(SupportDeskMotion.regular),
                    label = "railItemContent",
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item.key) },
                    color = containerColor.value,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = item.title.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor.value,
                        )
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor.value,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <T> AdminBottomBar(
    items: List<NavigationItemSpec<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = SupportDeskThemeTokens.elevations.subtle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.sm, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items.forEach { item ->
                val isSelected = item.key == selected
                val containerColor = animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    animationSpec = tween(SupportDeskMotion.regular),
                    label = "bottomItemContainer",
                )
                val contentColor = animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(SupportDeskMotion.regular),
                    label = "bottomItemContent",
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(item.key) },
                    color = containerColor.value,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor.value,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSectionDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    )
}

@Composable
fun AdminInlineMeta(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.width(1.dp))
    }
}
