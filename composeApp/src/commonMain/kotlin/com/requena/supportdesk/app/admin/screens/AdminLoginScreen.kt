package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.requena.supportdesk.designsystem.components.buttons.ThemeModeButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.auth.presentation.event.AuthUiEvent
import com.requena.supportdesk.features.auth.presentation.state.AuthUiState

@Composable
fun AdminLoginScreen(
    state: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.lg, vertical = spacing.lg),
    ) {
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            ThemeModeButton()
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            SectionCard(
                modifier = Modifier.widthIn(max = 960.dp),
                title = "OryKai software",
                subtitle = "Acceso por rol para admins y clientes.",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    SupportDeskBadge(
                        text = "Admin",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    SupportDeskBadge(
                        text = "Cliente",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            SectionCard(
                modifier = Modifier.widthIn(max = 520.dp),
                title = "Entrar",
                subtitle = "El escritorio abre el panel adecuado segun el rol de la cuenta.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { onEvent(AuthUiEvent.EmailChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Correo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { onEvent(AuthUiEvent.PasswordChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Contrasena") },
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
                        text = if (state.isLoading) "Entrando..." else "Entrar",
                        onClick = { onEvent(AuthUiEvent.Submit) },
                        fullWidth = true,
                    )
                }
            }
            SectionCard(
                modifier = Modifier.widthIn(max = 520.dp),
                title = "Primer acceso cliente",
                subtitle = "Usa la invitacion de tu admin para enlazar la cuenta.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    OutlinedTextField(
                        value = state.clientAccessCode,
                        onValueChange = { onEvent(AuthUiEvent.ClientAccessCodeChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Codigo de invitacion") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = { onEvent(AuthUiEvent.DisplayNameChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nombre visible") },
                        singleLine = true,
                    )
                    PrimaryButton(
                        text = if (state.isLoading) "Activando..." else "Activar acceso cliente",
                        onClick = { onEvent(AuthUiEvent.ClaimClientAccess) },
                        enabled = state.clientAccessCode.isNotBlank() && state.email.isNotBlank() && state.password.length >= 8,
                        fullWidth = true,
                    )
                }
            }
        }
    }
}
