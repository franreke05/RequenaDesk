package com.requena.supportdesk.app.client.screens.business.operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.requena.supportdesk.app.client.components.ClientPortalMetric
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.app.client.components.ClientPortalSurfaceCard
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.business.operations.ClientAppointmentDto
import com.requena.supportdesk.features.business.operations.CreateAppointmentDto
import com.requena.supportdesk.features.business.operations.OperationsUiEvent
import com.requena.supportdesk.features.business.operations.OperationsUiState

/** Stand-alone portal view. The integrator connects it to the business-operations route/ViewModel. */
@Composable
fun BookingsOperationsScreen(
    state: OperationsUiState,
    onEvent: (OperationsUiEvent) -> Unit,
    onOpenConfiguration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var showCreate by remember { mutableStateOf(false) }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        item {
            ClientPortalPageHeader("Agenda y reservas", "Consulta disponibilidad, confirma citas y prepara el calendario de tu negocio.") {
                Button(onClick = onOpenConfiguration) { Text("Configurar") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), modifier = Modifier.fillMaxWidth()) {
                ClientPortalMetric("Próximas", state.appointments.count { it.status == "CONFIRMED" }.toString(), "reservas confirmadas", Modifier.weight(1f))
                ClientPortalMetric("Calendario", "IANA", "zonas horarias seguras", Modifier.weight(1f))
            }
        }
        item { Button(onClick = { showCreate = !showCreate }) { Text(if (showCreate) "Cerrar" else "Nueva reserva") } }
        if (showCreate) item { BookingCreateForm(onCreate = { onEvent(OperationsUiEvent.CreateAppointment(it)); showCreate = false }) }
        item { Text("Próximas reservas", fontWeight = FontWeight.SemiBold) }
        items(state.appointments, key = ClientAppointmentDto::id) { appointment ->
            ClientPortalSurfaceCard {
                Text("${appointment.startsAt} · ${appointment.status}", fontWeight = FontWeight.SemiBold)
                Text("Recurso: ${appointment.resourceId}")
                if (appointment.status in setOf("HELD", "CONFIRMED")) Button(onClick = { onEvent(OperationsUiEvent.CancelAppointment(appointment.id)) }) { Text("Cancelar") }
            }
        }
    }
}

@Composable private fun BookingCreateForm(onCreate: (CreateAppointmentDto) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    var serviceId by remember { mutableStateOf("") }; var resourceId by remember { mutableStateOf("") }
    var startsAt by remember { mutableStateOf("") }; var endsAt by remember { mutableStateOf("") }; var timeZone by remember { mutableStateOf("Europe/Madrid") }
    ClientPortalSurfaceCard {
        Text("Nueva reserva", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(serviceId, { serviceId = it }, label = { Text("ID de servicio") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(resourceId, { resourceId = it }, label = { Text("ID de recurso") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(startsAt, { startsAt = it }, label = { Text("Inicio UTC (2026-08-10T08:00:00Z)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(endsAt, { endsAt = it }, label = { Text("Fin UTC") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(timeZone, { timeZone = it }, label = { Text("Zona IANA") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onCreate(CreateAppointmentDto(serviceId, resourceId, startsAt, endsAt, timeZone)) }, enabled = serviceId.isNotBlank() && resourceId.isNotBlank()) { Text("Confirmar reserva") }
    }
}
