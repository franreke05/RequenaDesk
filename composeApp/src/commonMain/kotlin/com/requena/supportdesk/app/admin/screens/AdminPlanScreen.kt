package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.ConfirmDialog
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.plan.BillingPeriod
import com.requena.supportdesk.features.plan.PlanModule
import com.requena.supportdesk.features.plan.PlanOffer
import com.requena.supportdesk.features.plan.PlanOverview
import com.requena.supportdesk.features.plan.PlanUiEvent
import com.requena.supportdesk.features.plan.PlanUiState
import com.requena.supportdesk.features.plan.formatPlanPrice

/** Ajustes › Plan y pagos. Owners and admins can request changes; others only see the plan. */
@Composable
fun PlanTab(state: PlanUiState, canManage: Boolean, onEvent: (PlanUiEvent) -> Unit) {
    val overview = state.overview
    if (overview == null) {
        Text(state.errorMessage ?: "Cargando tu plan...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    var pendingChoice by remember { mutableStateOf<PlanOffer?>(null) }
    CurrentPlanCard(overview, canManage, onEvent)

    SectionCard(
        modifier = Modifier.widthIn(max = 980.dp),
        title = "Planes",
        subtitle = "Precios sin IVA. Sin cobro automático: OryKai te envía la factura cuando aprueba el cambio.",
        actions = {
            SingleChoiceSegmentedButtonRow {
                BillingPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = state.period == period,
                        onClick = { onEvent(PlanUiEvent.PeriodChanged(period)) },
                        shape = SegmentedButtonDefaults.itemShape(index, BillingPeriod.entries.size),
                    ) { Text(period.label) }
                }
            }
        },
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm),
        ) {
            overview.catalog.forEach { offer ->
                PlanCard(
                    offer = offer,
                    period = state.period,
                    isCurrent = offer.key == overview.plan.planKey && !overview.plan.isTrial,
                    isRecommended = offer.key == "PRO",
                    canRequest = canManage && overview.pendingRequest == null && !state.isSaving,
                    onChoose = { pendingChoice = offer },
                )
            }
        }
    }

    val choice = pendingChoice
    ConfirmDialog(
        visible = choice != null,
        title = if (choice?.key == "FREE") "¿Pasar al plan Gratis?" else "¿Solicitar el plan ${choice?.name}?",
        message = if (choice?.key == "FREE") {
            "Se aplica ahora. Mantienes Núcleo y un módulo; el resto queda guardado hasta que vuelvas a un plan que lo incluya."
        } else {
            "Precio: ${formatPlanPrice(choice?.priceCents(state.period) ?: 0)}/mes (${state.period.label.lowercase()}, sin IVA). " +
                "No se cobra nada automáticamente: OryKai revisa la solicitud y te envía la factura."
        },
        confirmText = if (choice?.key == "FREE") "Pasar a Gratis" else "Acepto el precio y lo solicito",
        onConfirm = {
            choice?.let { onEvent(PlanUiEvent.Request(it.key)) }
            pendingChoice = null
        },
        onDismiss = { pendingChoice = null },
    )
}

@Composable
private fun CurrentPlanCard(overview: PlanOverview, canManage: Boolean, onEvent: (PlanUiEvent) -> Unit) {
    val plan = overview.plan
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(
        modifier = Modifier.widthIn(max = 980.dp),
        title = "Tu plan: ${plan.planName}",
        subtitle = when {
            plan.isTrial -> "Prueba de Pro: te quedan ${plan.trialDaysLeft} días. Al acabar pasas a Gratis; nunca se cobra sin que lo pidas."
            plan.trialEnded -> "Tu prueba terminó y estás en Gratis. Tus datos siguen guardados."
            plan.status == "BETA" -> "Estás en la beta: todo incluido y gratis. Te avisaremos con 60 días antes de cualquier cambio."
            else -> plan.billingPeriod?.let { "Facturación ${it.label.lowercase()}." } ?: "Plan activo."
        },
        emphasized = plan.isTrial || plan.trialEnded,
    ) {
        Text(
            "Incluye: " + plan.modules.sortedBy { it.ordinal }.joinToString(" · ") { it.label },
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Usuarios: ${overview.staffUsers}${plan.includedUsers?.let { " de $it" } ?: ""} (la gestoría no cuenta) · " +
                "Clientes: ${overview.clients}${plan.maxClients?.let { " de $it" } ?: ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val free = overview.catalog.firstOrNull { it.key == "FREE" }
        if (plan.planKey == "FREE" && free != null) {
            Text("Módulo incluido en Gratis", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                free.selectableFreeModules.sortedBy { it.ordinal }.forEach { module ->
                    FilterChip(
                        selected = plan.freeModule == module,
                        onClick = { onEvent(PlanUiEvent.ChooseFreeModule(module)) },
                        enabled = canManage,
                        label = { Text(module.label) },
                    )
                }
            }
        }
        overview.pendingRequest?.let { request ->
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.fillMaxWidth().padding(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Solicitud pendiente: ${request.planKey} (${request.billingPeriod.label.lowercase()}, ${formatPlanPrice(request.quotedMonthlyPriceCents)}/mes).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    if (canManage) TextButton(onClick = { onEvent(PlanUiEvent.Cancel(request.id)) }) { Text("Cancelar") }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    offer: PlanOffer,
    period: BillingPeriod,
    isCurrent: Boolean,
    isRecommended: Boolean,
    canRequest: Boolean,
    onChoose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.widthIn(min = 200.dp, max = 230.dp),
        shape = MaterialTheme.shapes.medium,
        color = colors.surface,
        border = BorderStroke(if (isRecommended) 2.dp else 1.dp, if (isRecommended) colors.primary else colors.outlineVariant),
    ) {
        Column(Modifier.padding(SupportDeskThemeTokens.spacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(offer.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (isRecommended) Text("Recomendado", style = MaterialTheme.typography.labelSmall, color = colors.primary)
            }
            Text(
                if (offer.monthlyPriceCents == 0) "0 €" else "${formatPlanPrice(offer.priceCents(period))}/mes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                listOfNotNull(
                    offer.includedUsers?.let { if (it == 1) "1 usuario" else "$it usuarios" } ?: "Usuarios sin límite",
                    offer.maxClients?.let { "hasta $it clientes" },
                    "+ gestoría gratis",
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Text(
                if (offer.key == "FREE") {
                    "Núcleo + 1 a elegir: " + offer.selectableFreeModules.sortedBy { it.ordinal }.joinToString(", ") { it.label }
                } else {
                    offer.modules.filter { it != PlanModule.CORE }.sortedBy { it.ordinal }.joinToString(" · ") { it.label }
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Box(Modifier.padding(top = 4.dp)) {
                if (isCurrent) {
                    Text("Tu plan actual", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                } else if (isRecommended) {
                    PrimaryButton(text = if (offer.key == "FREE") "Pasar a Gratis" else "Solicitar", onClick = onChoose, enabled = canRequest)
                } else {
                    SecondaryButton(text = if (offer.key == "FREE") "Pasar a Gratis" else "Solicitar", onClick = onChoose, enabled = canRequest)
                }
            }
        }
    }
}

/** Ajustes › Solicitudes de plan, only for the platform operator (OryKai). */
@Composable
fun OperatorRequestsTab(state: PlanUiState, onEvent: (PlanUiEvent) -> Unit) {
    SectionCard(
        modifier = Modifier.widthIn(max = 980.dp),
        title = "Solicitudes de plan",
        subtitle = "Al aprobar, el plan se activa al momento. La factura la emites tú (carril A); aquí no se cobra nada.",
    ) {
        if (state.operatorQueue.isEmpty()) {
            Text("No hay solicitudes pendientes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.operatorQueue.forEach { request ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${request.organizationName} → ${request.planKey}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${formatPlanPrice(request.quotedMonthlyPriceCents)}/mes · ${request.billingPeriod.label} · pedido por ${request.requestedByName} el ${request.createdAt.take(10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onEvent(PlanUiEvent.Reject(request.id, null)) }, enabled = !state.isSaving) { Text("Rechazar") }
                PrimaryButton(text = "Aprobar", onClick = { onEvent(PlanUiEvent.Approve(request.id, null)) }, enabled = !state.isSaving)
            }
        }
    }
}

/** Dashboard banner while on the Pro trial or right after it ended (pure display). */
@Composable
fun TrialBanner(plan: com.requena.supportdesk.features.plan.CompanyPlan, onOpenPlans: () -> Unit) {
    val text = when {
        plan.isTrial -> "Estás probando Pro: te quedan ${plan.trialDaysLeft} días. Al acabar pasas a Gratis, sin cobros."
        plan.trialEnded -> "Tu prueba de Pro terminó: ahora estás en Gratis. Tus datos siguen ahí."
        else -> return
    }
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(SupportDeskThemeTokens.spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenPlans) { Text("Ver planes") }
        }
    }
}
