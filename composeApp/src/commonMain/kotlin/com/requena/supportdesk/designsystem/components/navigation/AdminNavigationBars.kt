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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Lucide
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
                val interactionSource = remember { MutableInteractionSource() }
                val hovered by interactionSource.collectIsHoveredAsState()
                val containerColor = animateColorAsState(
                    targetValue = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        hovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surface
                    },
                    animationSpec = tween(SupportDeskMotion.quick),
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
                        .hoverable(interactionSource)
                        .clickable(interactionSource = interactionSource, indication = null) { onSelect(item.key) },
                    color = containerColor.value,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = contentColor.value,
                            )
                        } ?: Text(
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

/**
 * Bottom navigation for narrow windows. Shows at most [maxVisible] destinations: when there are
 * more, the first ones stay and the rest go under "Más" (bottom-nav guideline: 5 items max).
 */
@Composable
fun <T> AdminBottomBar(
    items: List<NavigationItemSpec<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    maxVisible: Int = 5,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val (visible, overflow) = splitBottomBarItems(items, maxVisible)
    var moreOpen by remember { mutableStateOf(false) }
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
            visible.forEach { item ->
                BottomBarItem(
                    title = item.title,
                    icon = item.icon,
                    isSelected = item.key == selected,
                    onClick = { onSelect(item.key) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (overflow.isNotEmpty()) {
                val selectedInOverflow = overflow.firstOrNull { it.key == selected }
                Box(modifier = Modifier.weight(1f)) {
                    BottomBarItem(
                        title = selectedInOverflow?.title ?: "Más",
                        icon = Lucide.EllipsisVertical,
                        isSelected = selectedInOverflow != null,
                        onClick = { moreOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        overflow.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.title) },
                                leadingIcon = item.icon?.let { icon -> { Icon(icon, contentDescription = null) } },
                                onClick = {
                                    moreOpen = false
                                    onSelect(item.key)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Pure split used by [AdminBottomBar]: (shown in the bar, shown under "Más"). */
fun <T> splitBottomBarItems(items: List<T>, maxVisible: Int = 5): Pair<List<T>, List<T>> =
    if (items.size <= maxVisible) items to emptyList() else items.take(maxVisible - 1) to items.drop(maxVisible - 1)

@Composable
private fun BottomBarItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
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
        modifier = modifier
            .semantics { selected = isSelected }
            .clickable(role = Role.Tab, onClick = onClick),
        color = containerColor.value,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            icon?.let { Icon(imageVector = it, contentDescription = null, tint = contentColor.value) }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.value,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
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
