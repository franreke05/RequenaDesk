package com.requena.supportdesk.designsystem.components.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(animationSpec = androidx.compose.animation.core.tween(SupportDeskMotion.quick)),
    ) {
        SectionCard(
            modifier = modifier.fillMaxWidth(),
            title = title,
            subtitle = message,
        ) {
            if (actionText != null && onAction != null) {
                PrimaryButton(text = actionText, onClick = onAction)
            }
        }
    }
}

@Composable
fun ErrorState(
    title: String = "No se pudieron cargar los datos",
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(SupportDeskMotion.regular)) +
            scaleIn(initialScale = 0.96f, animationSpec = tween(SupportDeskMotion.regular)),
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = SupportDeskThemeTokens.semanticColors.dangerContainer,
            contentColor = SupportDeskThemeTokens.semanticColors.danger,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(SupportDeskThemeTokens.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.md),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
                SecondaryButton(text = "Reintentar", onClick = onRetry)
            }
        }
    }
}

@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    height: Int = 96,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val pulseTransition = rememberInfiniteTransition(label = "skeletonPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonPulseAlpha",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            repeat(3) { index ->
                val alpha = if (index == 0) 0.9f else 0.55f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (index == 0) 0.55f else 0.9f)
                        .height((height / 5).dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.08f * pulse),
                            MaterialTheme.shapes.small,
                        ),
                )
            }
        }
    }
}

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    itemCount: Int = 3,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(SupportDeskMotion.regular)),
            exit = fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                repeat(itemCount) {
                    SkeletonCard()
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String,
    dismissText: String = "Cancelar",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            PrimaryButton(text = confirmText, onClick = onConfirm)
        },
        dismissButton = {
            SecondaryButton(text = dismissText, onClick = onDismiss)
        },
    )
}
