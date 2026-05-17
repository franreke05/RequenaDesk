package com.requena.supportdesk.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.network.NetworkLogger
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens

@Composable
fun NetworkLogPanel(
    modifier: Modifier = Modifier,
) {
    val logs = NetworkLogger.logs.collectAsState()
    val spacing = SupportDeskThemeTokens.spacing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(spacing.sm))
            .border(1.dp, Color(0xFF404040), RoundedCornerShape(spacing.sm))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.sm)
                    .background(Color(0xFF252526), RoundedCornerShape(spacing.xs)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Network Logs",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCECECE),
                    modifier = Modifier.weight(1f).padding(spacing.sm)
                )
                Button(
                    onClick = { NetworkLogger.clear() },
                    modifier = Modifier.height(28.dp),
                ) {
                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(spacing.sm),
            ) {
                items(logs.value) { log ->
                    val textColor = when {
                        log.contains("[ERROR]") -> Color(0xFFF48771)
                        log.contains("[DEBUG]") -> Color(0xFF89D185)
                        else -> Color(0xFFCECECE)
                    }
                    Text(
                        text = log,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }
    }
}
