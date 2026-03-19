package com.requena.supportdesk.designsystem.components.inputs

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search by subject, company or keyword",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        singleLine = true,
        label = { Text("Search") },
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
    )
}

@Immutable
data class FilterOption<T>(
    val value: T,
    val label: String,
)

@Composable
fun <T> FilterBar(
    label: String,
    options: List<FilterOption<T>>,
    selected: T?,
    onSelected: (T?) -> Unit,
    modifier: Modifier = Modifier,
    allLabel: String = "All",
) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelected(null) },
            label = { Text("$label: $allLabel") },
        )
        options.forEach { option ->
            FilterChip(
                selected = selected == option.value,
                onClick = { onSelected(option.value) },
                label = { Text(option.label) },
            )
        }
    }
}
