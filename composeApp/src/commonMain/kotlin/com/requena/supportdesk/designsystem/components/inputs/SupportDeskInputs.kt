package com.requena.supportdesk.designsystem.components.inputs

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
    wrap: Boolean = false,
) {
    val spacing = SupportDeskThemeTokens.spacing
    if (wrap) {
        FlowRow(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
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
    } else {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            item(key = "${label}_all") {
                FilterChip(
                    selected = selected == null,
                    onClick = { onSelected(null) },
                    label = { Text("$label: $allLabel") },
                )
            }
            itemsIndexed(
                items = options,
                key = { index, option -> "${option.label}_$index" },
            ) { _, option ->
                FilterChip(
                    selected = selected == option.value,
                    onClick = { onSelected(option.value) },
                    label = { Text(option.label) },
                )
            }
        }
    }
}
