package com.requena.supportdesk.android.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.buttons.ThemeModeButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.auth.presentation.event.AuthUiEvent
import com.requena.supportdesk.features.auth.presentation.state.AuthUiState

@Composable
fun LoginScreen(
    state: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            ThemeModeButton()
        }
        SectionCard(
            modifier = Modifier.align(Alignment.Center),
            title = "RequenaDesk admin",
            subtitle = "Mobile lite keeps the flow focused on fast review, notifications and quick ticket checks.",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                SupportDeskBadge(
                    text = "Admin lite",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { onEvent(AuthUiEvent.EmailChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { onEvent(AuthUiEvent.PasswordChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                state.errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                PrimaryButton(
                    text = if (state.isLoading) "Signing in..." else "Sign in",
                    onClick = { onEvent(AuthUiEvent.Submit) },
                    fullWidth = true,
                )
                SecondaryButton(
                    text = "Use admin demo",
                    onClick = { onEvent(AuthUiEvent.LoginAsAdminDemo) },
                    fullWidth = true,
                )
            }
        }
    }
}
