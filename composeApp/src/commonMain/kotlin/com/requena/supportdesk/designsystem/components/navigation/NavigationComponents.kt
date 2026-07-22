package com.requena.supportdesk.designsystem.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens

@Immutable
data class NavigationItemSpec<T>(
    val key: T,
    val title: String,
    val supportingText: String,
    val icon: ImageVector? = null,
)

@Composable
fun <T> AppSidebar(
    brandTitle: String,
    brandSubtitle: String,
    items: List<NavigationItemSpec<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(SupportDeskBreakpoints.clientSidebarWidth),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = SupportDeskThemeTokens.elevations.subtle,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = spacing.md, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(
                    text = brandTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = brandSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                items.forEach { item ->
                    val isSelected = selected == item.key
                    val interactionSource = remember { MutableInteractionSource() }
                    val hovered by interactionSource.collectIsHoveredAsState()
                    val selectedColors = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    } else if (hovered) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) to MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
                    }
                    val animatedBackground = animateColorAsState(
                        targetValue = selectedColors.first,
                        animationSpec = tween(durationMillis = SupportDeskMotion.quick),
                        label = "sidebarBackground",
                    )
                    val animatedContentColor = animateColorAsState(
                        targetValue = selectedColors.second,
                        animationSpec = tween(durationMillis = SupportDeskMotion.quick),
                        label = "sidebarContent",
                    )
                    val indicatorHeight = androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (isSelected) 40.dp else 20.dp,
                        animationSpec = tween(durationMillis = SupportDeskMotion.regular),
                        label = "sidebarIndicatorHeight",
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .hoverable(interactionSource)
                            .clickable(interactionSource = interactionSource, indication = null) { onSelect(item.key) },
                        shape = MaterialTheme.shapes.medium,
                        color = animatedBackground.value,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(indicatorHeight.value)
                                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraLarge),
                                )
                            } else {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            item.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = animatedContentColor.value,
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = animatedContentColor.value,
                                )
                                Text(
                                    text = item.supportingText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            }
            footer()
        }
    }
}
