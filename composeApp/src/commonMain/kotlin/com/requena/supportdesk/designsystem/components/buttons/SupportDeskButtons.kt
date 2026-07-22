package com.requena.supportdesk.designsystem.components.buttons

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else if (hovered) 1.02f else 1f,
        animationSpec = tween(SupportDeskMotion.quick),
        label = "primaryButtonScale",
    )
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.large,
        // Thin ink border on the fill color gives the primary action a printed-panel
        // edge instead of a flat Material fill, echoing the portfolio's ink-outline style.
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface),
        modifier = (if (fullWidth) modifier.fillMaxWidth() else modifier)
            .heightIn(min = 44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        ButtonContent(text = text, icon = icon, isLoading = isLoading)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else if (hovered) 1.02f else 1f,
        animationSpec = tween(SupportDeskMotion.quick),
        label = "secondaryButtonScale",
    )
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.large,
        modifier = (if (fullWidth) modifier.fillMaxWidth() else modifier)
            .heightIn(min = 44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        ButtonContent(text = text, icon = icon, isLoading = isLoading)
    }
}

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
    isLoading: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            isLoading -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = androidx.compose.material3.LocalContentColor.current,
            )
            icon != null -> Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
