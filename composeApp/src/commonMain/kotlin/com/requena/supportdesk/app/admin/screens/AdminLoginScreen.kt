package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.ThemeModeButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion
import com.requena.supportdesk.features.auth.presentation.event.AuthUiEvent
import com.requena.supportdesk.features.auth.presentation.state.AuthUiState
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Lucide

@Composable
fun AdminLoginScreen(
    state: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val canSubmit = state.email.isNotBlank() && state.password.isNotBlank() && !state.isLoading
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
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(SupportDeskMotion.emphasized)) +
                    scaleIn(initialScale = 0.94f, animationSpec = tween(SupportDeskMotion.emphasized)),
            ) {
                SectionCard(
                    modifier = Modifier.widthIn(max = 520.dp),
                    title = "OryKai software",
                    subtitle = "Acceso al espacio de gestion y al portal de cliente.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                        SupportDeskBadge(
                            text = "Acceso seguro",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        OutlinedTextField(
                            value = state.email,
                            onValueChange = { onEvent(AuthUiEvent.EmailChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Correo") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = { onEvent(AuthUiEvent.PasswordChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Contrasena") },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Lucide.EyeOff else Lucide.Eye,
                                        contentDescription = if (passwordVisible) "Ocultar contrasena" else "Mostrar contrasena",
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { if (canSubmit) onEvent(AuthUiEvent.Submit) },
                            ),
                            singleLine = true,
                        )
                        AnimatedVisibility(
                            visible = state.errorMessage != null,
                            enter = fadeIn(tween(SupportDeskMotion.regular)) + expandVertically(),
                            exit = fadeOut(tween(SupportDeskMotion.quick)) + shrinkVertically(),
                        ) {
                            Text(
                                text = state.errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        PrimaryButton(
                            text = "Entrar",
                            onClick = { onEvent(AuthUiEvent.Submit) },
                            enabled = canSubmit,
                            fullWidth = true,
                            isLoading = state.isLoading,
                        )
                    }
                }
            }
        }
    }
}
