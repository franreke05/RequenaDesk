package com.requena.supportdesk.designsystem.components.feedback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide
import com.requena.supportdesk.core.appearance.SupportDeskStyle
import com.requena.supportdesk.core.appearance.ThemeModePreference
import com.requena.supportdesk.designsystem.components.cards.supportDeskCardBorder
import com.requena.supportdesk.designsystem.components.cards.supportDeskCardEmphasis
import com.requena.supportdesk.designsystem.theme.SupportDeskDesignTheme
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens

/**
 * Appearance picker: three style cards with a live preview each (rendered in that style's own
 * theme) plus a System / Light / Dark switch. Choices apply immediately.
 */
@Composable
fun AppearanceDialog(
    selectedStyle: SupportDeskStyle,
    selectedMode: ThemeModePreference,
    previewDark: Boolean,
    onStyleSelected: (SupportDeskStyle) -> Unit,
    onModeSelected: (ThemeModePreference) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
        title = { Text("Apariencia") },
        text = {
            AppearancePicker(
                selectedStyle = selectedStyle,
                selectedMode = selectedMode,
                previewDark = previewDark,
                onStyleSelected = onStyleSelected,
                onModeSelected = onModeSelected,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
    )
}

/** Style cards with live previews plus the System / Light / Dark switch (dialog and onboarding). */
@Composable
fun AppearancePicker(
    selectedStyle: SupportDeskStyle,
    selectedMode: ThemeModePreference,
    previewDark: Boolean,
    onStyleSelected: (SupportDeskStyle) -> Unit,
    onModeSelected: (ThemeModePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Text(
            "Elige cómo quieres ver RequenaDesk. Se guarda en este equipo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cards: @Composable (Modifier) -> Unit = { cardModifier ->
                SupportDeskStyle.entries.forEach { style ->
                    StyleOptionCard(
                        style = style,
                        selected = style == selectedStyle,
                        dark = previewDark,
                        onClick = { onStyleSelected(style) },
                        modifier = cardModifier,
                    )
                }
            }
            if (maxWidth >= 480.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) { cards(Modifier.weight(1f)) }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) { cards(Modifier.fillMaxWidth()) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text("Modo", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val modes = ThemeModePreference.entries
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == selectedMode,
                        onClick = { onModeSelected(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                    ) { Text(mode.label) }
                }
            }
        }
    }
}

@Composable
private fun StyleOptionCard(
    style: SupportDeskStyle,
    selected: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .clip(shape)
            .border(
                BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
                shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "Estilo ${style.label}. ${style.description}" }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StylePreview(style = style, dark = dark)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                style.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(Lucide.CircleCheck, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
            }
        }
        Text(style.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

/** Miniature dashboard rendered with the real theme of [style], so the preview is honest. */
@Composable
private fun StylePreview(style: SupportDeskStyle, dark: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clip(MaterialTheme.shapes.small),
    ) {
        SupportDeskDesignTheme(useDarkTheme = dark, style = style) {
            val colors = MaterialTheme.colorScheme
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SupportDeskThemeTokens.pageBackground)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Inicio", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
                Surface(
                    modifier = Modifier.fillMaxWidth().supportDeskCardEmphasis(true),
                    shape = MaterialTheme.shapes.medium,
                    color = colors.surface,
                    border = supportDeskCardBorder(true),
                ) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Text("1.240 €", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                        Text("Facturado este mes", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .clip(MaterialTheme.shapes.large)
                            .background(colors.primary)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) { Text("Nueva", style = MaterialTheme.typography.labelSmall, color = colors.onPrimary) }
                    Box(
                        Modifier
                            .clip(MaterialTheme.shapes.large)
                            .background(colors.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) { Text("Pendiente", style = MaterialTheme.typography.labelSmall, color = colors.onSecondaryContainer) }
                }
            }
        }
    }
}
