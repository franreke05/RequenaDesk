package com.requena.supportdesk.app.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.LocalSupportDeskThemeController
import com.requena.supportdesk.core.model.LegalForm
import com.requena.supportdesk.core.model.OnboardingGoal
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.AppearancePicker
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.feedback.StepProgress
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion
import com.requena.supportdesk.features.onboarding.OnboardingStep
import com.requena.supportdesk.features.onboarding.OnboardingUiEvent
import com.requena.supportdesk.features.onboarding.OnboardingUiState
import com.requena.supportdesk.features.onboarding.OnboardingViewModel
import com.requena.supportdesk.features.plan.recommendedPlan

private val TEAM_SIZES = listOf("Solo yo", "2-5", "6-20", "Más de 20")

/**
 * Steps 2–5 of the onboarding (step 1, the account, is the signup). Shown to a company
 * owner until the company is marked as onboarded. Each step saves on "Siguiente".
 */
@Composable
fun OnboardingFlow(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.onEvent(OnboardingUiEvent.Load) }
    LaunchedEffect(state.completed) { if (state.completed) onFinished() }

    val spacing = SupportDeskThemeTokens.spacing
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.lg),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Configura tu espacio",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSignOut) { Text("Cerrar sesión") }
            }
            if (state.isLoading) {
                LoadingState(itemCount = 2)
                return@Column
            }
            StepProgress(current = state.step.number, total = OnboardingStep.TOTAL, title = state.step.title)
            SectionCard {
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = { fadeIn(tween(SupportDeskMotion.regular)) togetherWith fadeOut(tween(SupportDeskMotion.quick)) },
                    label = "onboardingStep",
                ) { step ->
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                        when (step) {
                            OnboardingStep.BUSINESS -> BusinessStep(state, viewModel::onEvent)
                            OnboardingStep.GOALS -> GoalsStep(state, viewModel::onEvent)
                            OnboardingStep.FISCAL -> FiscalStep(state, viewModel::onEvent)
                            OnboardingStep.STYLE -> StyleStep()
                        }
                    }
                }
            }
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                if (state.step != OnboardingStep.entries.first()) {
                    SecondaryButton(text = "Atrás", onClick = { viewModel.onEvent(OnboardingUiEvent.Back) })
                }
                Box(Modifier.weight(1f))
                if (state.step.skippable) {
                    TextButton(onClick = { viewModel.onEvent(OnboardingUiEvent.Skip) }) { Text("Saltar") }
                }
                PrimaryButton(
                    text = if (state.step == OnboardingStep.entries.last()) "Terminar" else "Siguiente",
                    onClick = { viewModel.onEvent(OnboardingUiEvent.Next) },
                    isLoading = state.isSaving,
                )
            }
        }
    }
}

@Composable
private fun BusinessStep(state: OnboardingUiState, onEvent: (OnboardingUiEvent) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    Text("¿Cómo trabajas?", style = MaterialTheme.typography.titleMedium)
    Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        LegalForm.entries.forEach { form ->
            ChoiceCard(
                title = form.label,
                description = if (form == LegalForm.AUTONOMO) "Facturas a tu nombre, con IRPF." else "S.L., S.A. u otra sociedad.",
                selected = state.legalForm == form,
                onClick = { onEvent(OnboardingUiEvent.LegalFormChanged(form)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    FieldError(state.fieldErrors["legalForm"])
    OutlinedTextField(
        value = state.sector,
        onValueChange = { onEvent(OnboardingUiEvent.SectorChanged(it)) },
        label = { Text("Sector") },
        placeholder = { Text("Diseño, software, reformas...") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Text("¿Cuántas personas sois?", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
        TEAM_SIZES.forEach { size ->
            FilterChip(
                selected = state.teamSize == size,
                onClick = { onEvent(OnboardingUiEvent.TeamSizeChanged(size)) },
                label = { Text(size) },
            )
        }
    }
    Text("¿Trabajas con gestoría?", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
        FilterChip(selected = state.hasAccountant == true, onClick = { onEvent(OnboardingUiEvent.HasAccountantChanged(true)) }, label = { Text("Sí") })
        FilterChip(selected = state.hasAccountant == false, onClick = { onEvent(OnboardingUiEvent.HasAccountantChanged(false)) }, label = { Text("No") })
    }
    if (state.hasAccountant == true) {
        Text(
            "Podrás invitar a tu gestoría gratis desde Ajustes › Usuarios, con acceso de solo lectura.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GoalsStep(state: OnboardingUiState, onEvent: (OnboardingUiEvent) -> Unit) {
    Text("Elige todo lo que quieras resolver. Lo usaremos para recomendarte qué activar.", style = MaterialTheme.typography.bodyMedium)
    OnboardingGoal.entries.forEach { goal ->
        val checked = goal in state.goals
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onEvent(OnboardingUiEvent.GoalToggled(goal)) }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Column {
                Text(goal.label, style = MaterialTheme.typography.titleSmall)
                Text(goal.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    FieldError(state.fieldErrors["goals"])
    if (state.goals.isNotEmpty()) {
        Text(
            "Te recomendamos: " + state.recommended.joinToString(" · ") { it.label },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        val planName = when (recommendedPlan(state.goals, state.teamSize)) {
            "TEAM" -> "Equipo"
            "PRO" -> "Pro"
            "AUTONOMO" -> "Autónomo"
            else -> "Gratis"
        }
        Text(
            "Plan que encaja contigo: $planName. Ahora tienes 14 días de Pro gratis; lo eliges después en Ajustes › Plan y pagos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FiscalStep(state: OnboardingUiState, onEvent: (OnboardingUiEvent) -> Unit) {
    Text(
        "Opcional. Saldrán en tus facturas y presupuestos; puedes completarlo más tarde en Ajustes › Empresa.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FiscalField(state, onEvent, "legalName", "Razón social o nombre fiscal", state.legalName)
    FiscalField(state, onEvent, "taxId", "NIF / CIF", state.taxId, KeyboardCapitalization.Characters)
    FiscalField(state, onEvent, "addressLine", "Dirección", state.addressLine)
    Row(horizontalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm)) {
        FiscalField(state, onEvent, "postalCode", "Código postal", state.postalCode, modifier = Modifier.weight(1f))
        FiscalField(state, onEvent, "city", "Ciudad", state.city, modifier = Modifier.weight(2f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm)) {
        FiscalField(state, onEvent, "province", "Provincia", state.province, modifier = Modifier.weight(2f))
        FiscalField(state, onEvent, "country", "País (ES)", state.country, KeyboardCapitalization.Characters, Modifier.weight(1f))
    }
}

@Composable
private fun FiscalField(
    state: OnboardingUiState,
    onEvent: (OnboardingUiEvent) -> Unit,
    field: String,
    label: String,
    value: String,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val error = state.fieldErrors[field]
    OutlinedTextField(
        value = value,
        onValueChange = { onEvent(OnboardingUiEvent.FiscalChanged(field, it)) },
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = ImeAction.Next),
        modifier = modifier,
        singleLine = true,
    )
}

@Composable
private fun StyleStep() {
    val controller = LocalSupportDeskThemeController.current
    Text(
        "Elige cómo quieres ver la app. Es solo para ti y puedes cambiarlo cuando quieras en Ajustes › Apariencia.",
        style = MaterialTheme.typography.bodyMedium,
    )
    AppearancePicker(
        selectedStyle = controller.style,
        selectedMode = controller.mode,
        previewDark = controller.isDarkMode,
        onStyleSelected = controller.setStyle,
        onModeSelected = controller.setMode,
    )
}

@Composable
private fun ChoiceCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
    ) {
        Column(Modifier.padding(SupportDeskThemeTokens.spacing.md)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (selected) colors.onPrimaryContainer else colors.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun FieldError(message: String?) {
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
}
