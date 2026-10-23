package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide
import com.requena.supportdesk.core.navigation.AppDestination
import com.requena.supportdesk.designsystem.components.cards.SectionCard

/** One item of the "Primeros pasos" checklist shown on the dashboard of a new company. */
data class FirstStep(val title: String, val done: Boolean, val destination: AppDestination)

/** Pure: the checklist from data the app already has. */
fun firstSteps(
    hasClients: Boolean,
    hasLoggedTime: Boolean,
    hasTeamOrInvitations: Boolean,
    hasFiscalData: Boolean,
): List<FirstStep> = listOf(
    FirstStep("Crea tu primer cliente (incluye su acceso al portal)", hasClients, AppDestination.Clients),
    FirstStep("Registra horas con el temporizador en una tarea", hasLoggedTime, AppDestination.Tasks),
    FirstStep("Invita a tu equipo o a tu gestoría", hasTeamOrInvitations, AppDestination.Settings),
    FirstStep("Completa los datos fiscales de tu empresa", hasFiscalData, AppDestination.Settings),
)

@Composable
fun FirstStepsCard(steps: List<FirstStep>, onNavigate: (AppDestination) -> Unit, modifier: Modifier = Modifier) {
    var dismissed by rememberSaveable { mutableStateOf(false) }
    if (dismissed || steps.all { it.done }) return
    val done = steps.count { it.done }
    SectionCard(
        modifier = modifier.fillMaxWidth(),
        title = "Primeros pasos",
        subtitle = "$done de ${steps.size} completados",
        emphasized = true,
        actions = { TextButton(onClick = { dismissed = true }) { Text("Ocultar") } },
    ) {
        steps.forEach { step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (step.done) Lucide.CircleCheck else Lucide.Circle,
                    contentDescription = if (step.done) "Hecho" else "Pendiente",
                    tint = if (step.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    step.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (step.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (!step.done) {
                    TextButton(onClick = { onNavigate(step.destination) }) { Text("Ir") }
                }
            }
        }
    }
}
