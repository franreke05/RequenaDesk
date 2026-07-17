package com.requena.supportdesk.app.client.screens.business.operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.app.client.components.ClientPortalSurfaceCard
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.business.operations.BookingResourceDto

/** First-use configuration. Each callback maps directly to the protected configuration endpoints. */
@Composable
fun BookingSetupScreen(
    resources: List<BookingResourceDto> = emptyList(),
    onCreateService: (name: String, durationMinutes: Int) -> Unit,
    onCreateResource: (name: String, timeZone: String) -> Unit,
    onCreateAvailability: (resourceId: String, weekday: Int, startsAt: String, endsAt: String, timeZone: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var serviceName by remember { mutableStateOf("") }; var duration by remember { mutableStateOf("30") }
    var resourceName by remember { mutableStateOf("") }; var zone by remember { mutableStateOf("Europe/Madrid") }
    var resourceId by remember { mutableStateOf("") }; var weekday by remember { mutableStateOf("1") }; var from by remember { mutableStateOf("09:00") }; var to by remember { mutableStateOf("18:00") }
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        ClientPortalPageHeader("Configurar agenda", "1. Servicio · 2. Recurso · 3. Horario. Solo responsables autorizados pueden cambiar esta configuración.")
        ClientPortalSurfaceCard {
            Text("1. Servicio", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(serviceName, { serviceName = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(duration, { duration = it }, label = { Text("Duración en minutos") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { duration.toIntOrNull()?.let { onCreateService(serviceName, it) } }, enabled = serviceName.isNotBlank()) { Text("Guardar servicio") }
        }
        ClientPortalSurfaceCard {
            Text("2. Recurso", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(resourceName, { resourceName = it }, label = { Text("Sala, persona o equipo") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(zone, { zone = it }, label = { Text("Zona IANA") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { onCreateResource(resourceName, zone) }, enabled = resourceName.isNotBlank()) { Text("Guardar recurso") }
            if (resources.isNotEmpty()) {
                Text("Recursos creados", fontWeight = FontWeight.SemiBold)
                resources.forEach { resource -> Text("${resource.name}: ${resource.id}") }
            }
        }
        ClientPortalSurfaceCard {
            Text("3. Horario semanal", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(resourceId, { resourceId = it }, label = { Text("ID del recurso creado") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(weekday, { weekday = it }, label = { Text("Día (1 lunes – 7 domingo)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(from, { from = it }, label = { Text("Desde HH:mm") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(to, { to = it }, label = { Text("Hasta HH:mm") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { weekday.toIntOrNull()?.let { onCreateAvailability(resourceId, it, from, to, zone) } }, enabled = resourceId.isNotBlank()) { Text("Guardar horario") }
        }
    }
}
