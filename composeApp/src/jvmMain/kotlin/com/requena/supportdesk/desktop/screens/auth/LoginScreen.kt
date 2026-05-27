package com.requena.supportdesk.desktop.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
            .padding(horizontal = 40.dp, vertical = 32.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd),
        ) {
            ThemeModeButton()
        }
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionCard(
                modifier = Modifier.widthIn(min = 360.dp, max = 460.dp),
                title = "OryKai software",
                subtitle = "Un CRM de escritorio para soporte freelance. Un espacio de trabajo, dos roles, sin fricción.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    SupportDeskBadge(
                        text = "Escritorio primero",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Gestiona tickets abiertos, responde más rápido y mantén cada conversación con el cliente en un solo lugar.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LoginFeatureRow("Entorno compartido con flujos separados para admin y cliente.")
                    LoginFeatureRow("Cola de tickets diseñada para revisión diaria y triaje rápido.")
                    LoginFeatureRow("Interfaz visual pensada para clientes reales de pago.")
                }
            }
            SectionCard(
                modifier = Modifier.widthIn(min = 360.dp, max = 460.dp),
                title = "Iniciar sesión",
                subtitle = "Usa una de las cuentas de administrador configuradas para acceder al espacio de trabajo.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { onEvent(AuthUiEvent.EmailChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Correo electrónico") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { onEvent(AuthUiEvent.PasswordChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Contraseña") },
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
                        text = if (state.isLoading) "Iniciando sesión..." else "Iniciar sesión",
                        onClick = { onEvent(AuthUiEvent.Submit) },
                        fullWidth = true,
                    )
                    Text(
                        text = "El acceso es intencionalmente breve para que los tickets queden siempre en primer plano.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginFeatureRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
