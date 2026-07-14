package com.requena.supportdesk.designsystem.components.buttons

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

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
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = (if (fullWidth) modifier.fillMaxWidth() else modifier).heightIn(min = 44.dp),
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
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = (if (fullWidth) modifier.fillMaxWidth() else modifier).heightIn(min = 44.dp),
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
