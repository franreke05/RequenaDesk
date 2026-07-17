package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.ProgramRequestStatus
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.features.programs.presentation.event.ProgramsUiEvent
import com.requena.supportdesk.features.programs.presentation.state.ProgramsUiState

/**
 * The client can request beta utilities, but an administrator remains the
 * authority that activates them. Beta approval always sends a zero price to
 * the existing endpoint and never creates a billing decision in this UI.
 */
@Composable
internal fun ClientProgramsAdministrationTab(
    client: Client,
    state: ProgramsUiState,
    onEvent: (ProgramsUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    LaunchedEffect(client.id) {
        onEvent(ProgramsUiEvent.RefreshAdminRequests)
    }

    val clientRequests = state.adminRequests.filter { it.clientId == client.id }
    val pendingCount = clientRequests.count { it.status == ProgramRequestStatus.REQUESTED }
    val activeCount = clientRequests.count { it.status == ProgramRequestStatus.APPROVED }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        SectionCard(
            title = "Acceso beta gratuito",
            subtitle = "El cliente solicita cada utilidad y tú decides si se activa para su empresa.",
        ) {
            SupportDeskBadge(
                text = "Gratis durante la beta",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "Autorizar acceso no añade cuotas, cargos ni conceptos a una factura. Una conversión futura a pago requerirá una decisión independiente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$pendingCount solicitud${if (pendingCount == 1) "" else "es"} por revisar · $activeCount acceso${if (activeCount == 1) "" else "s"} autorizado${if (activeCount == 1) "" else "s"}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        SectionCard(
            title = "Solicitudes del cliente",
            subtitle = "Cada decisión queda registrada y sólo autoriza el uso de la beta.",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                if (clientRequests.isEmpty()) {
                    EmptyState(
                        title = "Sin solicitudes",
                        message = "El cliente aún no ha solicitado ningún programa beta.",
                    )
                } else {
                    clientRequests.forEach { request ->
                        ProgramRequestAdminRow(
                            requestId = request.id,
                            programName = programName(request.productKey),
                            status = request.status,
                            requestedAt = request.requestedAt,
                            customerNote = request.customerNote,
                            isSubmitting = state.isSubmitting,
                            onAuthorize = { note ->
                                onEvent(ProgramsUiEvent.ApproveRequest(request.id, note))
                            },
                            onReject = { note -> onEvent(ProgramsUiEvent.RejectRequest(request.id, note)) },
                        )
                    }
                }
                state.errorMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ProgramRequestAdminRow(
    requestId: String,
    programName: String,
    status: ProgramRequestStatus,
    requestedAt: String,
    customerNote: String?,
    isSubmitting: Boolean,
    onAuthorize: (String?) -> Unit,
    onReject: (String?) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var adminNote by rememberSaveable(requestId) { mutableStateOf("") }
    val isPending = status == ProgramRequestStatus.REQUESTED

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(programName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "${statusLabel(status)} · solicitada el ${requestedAt.take(10)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        customerNote?.takeIf { it.isNotBlank() }?.let { note ->
            Text("Nota del cliente: $note", style = MaterialTheme.typography.bodySmall)
        }
        if (isPending) {
            OutlinedTextField(
                value = adminNote,
                onValueChange = { adminNote = it },
                label = { Text("Mensaje para el cliente (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 3,
                enabled = !isSubmitting,
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 520.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        PrimaryButton(
                            text = "Autorizar y activar gratis",
                            fullWidth = true,
                            onClick = { onAuthorize(adminNote.ifBlank { null }) },
                            enabled = !isSubmitting,
                            isLoading = isSubmitting,
                        )
                        SecondaryButton(
                            text = "Rechazar",
                            fullWidth = true,
                            onClick = { onReject(adminNote.ifBlank { null }) },
                            enabled = !isSubmitting,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        SecondaryButton(
                            text = "Rechazar",
                            modifier = Modifier.weight(1f),
                            fullWidth = true,
                            onClick = { onReject(adminNote.ifBlank { null }) },
                            enabled = !isSubmitting,
                        )
                        PrimaryButton(
                            text = "Autorizar y activar gratis",
                            modifier = Modifier.weight(1f),
                            fullWidth = true,
                            onClick = { onAuthorize(adminNote.ifBlank { null }) },
                            enabled = !isSubmitting,
                            isLoading = isSubmitting,
                        )
                    }
                }
            }
        } else if (status == ProgramRequestStatus.APPROVED) {
            Text(
                text = "Acceso activado gratis durante la beta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun programName(key: String): String = when (key) {
    "BUSINESS_CUSTOMERS" -> "Clientes y contactos"
    "BUSINESS_QUOTES" -> "Presupuestos y ventas"
    "BUSINESS_CATALOG" -> "Productos, servicios y stock"
    "BUSINESS_INVOICING" -> "Facturación"
    "BUSINESS_ACCOUNTING" -> "Contabilidad y gastos"
    "BUSINESS_BOOKINGS" -> "Agenda y reservas"
    "BUSINESS_DOCUMENTS" -> "Documentos y firmas"
    else -> key.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
}

private fun statusLabel(status: ProgramRequestStatus): String = when (status) {
    ProgramRequestStatus.REQUESTED -> "Pendiente"
    ProgramRequestStatus.APPROVED -> "Autorizada"
    ProgramRequestStatus.REJECTED -> "Rechazada"
    ProgramRequestStatus.CANCELLED -> "Cancelada"
}
