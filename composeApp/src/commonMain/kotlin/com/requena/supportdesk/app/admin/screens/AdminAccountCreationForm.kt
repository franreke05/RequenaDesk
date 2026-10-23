package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Lucide
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.auth.presentation.event.AuthUiEvent
import com.requena.supportdesk.features.auth.presentation.state.AuthMode
import com.requena.supportdesk.features.auth.presentation.state.AuthUiState
import com.requena.supportdesk.features.auth.presentation.viewmodel.AuthViewModel

/** Links under the login button: create a company (when the server allows it) or join one. */
@Composable
internal fun LoginAlternatives(signupAvailable: Boolean, onEvent: (AuthUiEvent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (signupAvailable) {
            TextButton(onClick = { onEvent(AuthUiEvent.ModeChanged(AuthMode.SIGNUP)) }) {
                Text("¿Empiezas? Crea la cuenta de tu empresa")
            }
        }
        TextButton(onClick = { onEvent(AuthUiEvent.ModeChanged(AuthMode.JOIN)) }) {
            Text("Tengo un código de invitación")
        }
    }
}

/**
 * Signup (step 1 of 5 of the onboarding) and join-with-code form. Labels are always visible,
 * the error appears right under the fields and the button explains what is missing.
 */
@Composable
internal fun AccountCreationForm(state: AuthUiState, onEvent: (AuthUiEvent) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val problem = AuthViewModel.validationProblem(state.copy(isLoading = false))
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        if (state.mode == AuthMode.JOIN) {
            OutlinedTextField(
                value = state.inviteCode,
                onValueChange = { onEvent(AuthUiEvent.InviteCodeChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Código de invitación") },
                placeholder = { Text("INV-XXXX-XXXX-XXXX") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                singleLine = true,
            )
        }
        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(AuthUiEvent.NameChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tu nombre") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            singleLine = true,
        )
        if (state.mode == AuthMode.SIGNUP) {
            OutlinedTextField(
                value = state.organizationName,
                onValueChange = { onEvent(AuthUiEvent.OrganizationNameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre de tu negocio") },
                supportingText = { Text("Lo verán tus clientes en el portal. Puedes cambiarlo después.") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                singleLine = true,
            )
        }
        OutlinedTextField(
            value = state.email,
            onValueChange = { onEvent(AuthUiEvent.EmailChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Correo") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onEvent(AuthUiEvent.PasswordChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Contraseña") },
            supportingText = { Text("Al menos ${AuthViewModel.MINIMUM_PASSWORD_LENGTH} caracteres.") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Lucide.EyeOff else Lucide.Eye,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            singleLine = true,
        )
        // Separate, unticked consents (AJUSTES_LEGAL_Y_FACTURACION_ES.md).
        ConsentRow(
            checked = state.acceptTerms,
            text = "Acepto los términos de uso",
            onChange = { onEvent(AuthUiEvent.AcceptTermsChanged(it)) },
        )
        ConsentRow(
            checked = state.acceptPrivacy,
            text = "He leído la política de privacidad",
            onChange = { onEvent(AuthUiEvent.AcceptPrivacyChanged(it)) },
        )
        state.errorMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        PrimaryButton(
            text = if (state.mode == AuthMode.SIGNUP) "Crear cuenta y continuar" else "Unirme a la empresa",
            onClick = { onEvent(AuthUiEvent.Submit) },
            enabled = problem == null && !state.isLoading,
            fullWidth = true,
            isLoading = state.isLoading,
        )
        if (problem != null && state.errorMessage == null) {
            Text(problem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { onEvent(AuthUiEvent.ModeChanged(AuthMode.LOGIN)) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Ya tengo cuenta: entrar")
        }
    }
}

@Composable
private fun ConsentRow(checked: Boolean, text: String, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
