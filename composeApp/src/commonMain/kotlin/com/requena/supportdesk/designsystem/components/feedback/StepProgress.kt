package com.requena.supportdesk.designsystem.components.feedback

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion

/** "Paso n de total" header with an animated progress bar, for multi-step flows. */
@Composable
fun StepProgress(
    current: Int,
    total: Int,
    title: String,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = current.toFloat() / total,
        animationSpec = tween(SupportDeskMotion.regular),
        label = "stepProgress",
    )
    Column(
        modifier = modifier.semantics { contentDescription = "Paso $current de $total: $title" },
        verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.xs),
    ) {
        Text(
            text = "Paso $current de $total",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}
