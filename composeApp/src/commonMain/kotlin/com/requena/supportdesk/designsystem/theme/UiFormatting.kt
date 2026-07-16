package com.requena.supportdesk.designsystem.theme

import com.requena.supportdesk.core.model.ClientAccountStatus
import com.requena.supportdesk.core.model.ClientServiceTier
import com.requena.supportdesk.core.model.PreferredContactChannel
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.model.WaitingOn
import com.requena.supportdesk.core.navigation.AppDestination

fun TicketStatus.displayName(): String = when (this) {
    TicketStatus.OPEN -> "Abierto"
    TicketStatus.IN_PROGRESS -> "En curso"
    TicketStatus.PENDING_CLIENT -> "Pendiente del cliente"
    TicketStatus.RESOLVED -> "Resuelto"
    TicketStatus.CLOSED -> "Cerrado"
}

fun TicketPriority.displayName(): String = when (this) {
    TicketPriority.LOW -> "Baja"
    TicketPriority.MEDIUM -> "Media"
    TicketPriority.HIGH -> "Alta"
    TicketPriority.URGENT -> "Urgente"
}

fun TicketCategory.displayName(): String = when (this) {
    TicketCategory.BUG -> "Bug"
    TicketCategory.ACCESS -> "Acceso"
    TicketCategory.BILLING -> "Facturacion"
    TicketCategory.CHANGE_REQUEST -> "Cambio solicitado"
    TicketCategory.QUESTION -> "Consulta"
    TicketCategory.OTHER -> "Otro"
}

fun SupportPlatform.displayName(): String = when (this) {
    SupportPlatform.ANDROID -> "Android"
    SupportPlatform.IOS -> "iOS"
    SupportPlatform.DESKTOP -> "Escritorio"
    SupportPlatform.WEB -> "Web"
    SupportPlatform.BACKEND -> "Backend"
    SupportPlatform.OTHER -> "Otro"
}

fun WaitingOn.displayName(): String = when (this) {
    WaitingOn.CLIENT -> "Esperando al cliente"
    WaitingOn.ADMIN -> "Esperando a admin"
}

fun ClientAccountStatus.displayName(): String = when (this) {
    ClientAccountStatus.ACTIVE -> "Activo"
    ClientAccountStatus.PAUSED -> "Pausado"
    ClientAccountStatus.INACTIVE -> "Inactivo"
}

fun ClientServiceTier.displayName(): String = when (this) {
    ClientServiceTier.STANDARD -> "Estandar"
    ClientServiceTier.PRIORITY -> "Prioritario"
    ClientServiceTier.VIP -> "VIP"
}

fun PreferredContactChannel.displayName(): String = when (this) {
    PreferredContactChannel.TICKET -> "Ticket"
    PreferredContactChannel.EMAIL -> "Correo"
    PreferredContactChannel.WHATSAPP -> "WhatsApp"
    PreferredContactChannel.CALL -> "Llamada"
}

fun UserRole.displayName(): String = when (this) {
    UserRole.CLIENT -> "Cliente"
    UserRole.ADMIN -> "Admin"
}

fun AppDestination.displayTitle(): String = when (this) {
    AppDestination.Login -> "Acceso Admin"
    AppDestination.Dashboard -> "Dashboard"
    AppDestination.Tasks -> "Tareas"
    AppDestination.Pinboard -> "Tablon"
    AppDestination.Labels -> "Etiquetas"
    AppDestination.Tickets -> "Agenda"
    is AppDestination.TicketDetail -> "Agenda"
    AppDestination.Clients -> "Clientes"
    AppDestination.Notifications -> "Etiquetas"
    is AppDestination.Invoices -> "Facturas"
}

fun AppDestination.displaySubtitle(): String = when (this) {
    AppDestination.Login -> "Workspace solo admin para operar clientes."
    AppDestination.Dashboard -> "Tiempo central, cliente activo y ruedas mensuales."
    AppDestination.Tasks -> "Trabajo operativo con cliente opcional y etiquetas."
    AppDestination.Pinboard -> "Chinchetas con las tareas pendientes de hoy."
    AppDestination.Labels -> "Colores y grupos para ordenar tareas."
    AppDestination.Tickets -> "Ruta legacy retirada de la navegacion principal."
    is AppDestination.TicketDetail -> "Ruta legacy retirada de la navegacion principal."
    AppDestination.Clients -> "Directorio limpio y ficha rapida de clientes."
    AppDestination.Notifications -> "Categorias de tareas y colores de seccion."
    is AppDestination.Invoices -> "Genera facturas en PDF para tus clientes."
}

fun formatSupportDeskDateTime(raw: String): String {
    if (raw.isBlank()) return "-"
    val cleaned = raw.removeSuffix("Z").replace("T", "  ")
    return if (cleaned.length > 16) cleaned.substring(0, 16) else cleaned
}

fun formatSupportDeskDuration(minutes: Int): String {
    if (minutes <= 0) return "0 h"
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours == 0) {
        "${remainingMinutes} m"
    } else if (remainingMinutes == 0) {
        "${hours} h"
    } else {
        "${hours} h ${remainingMinutes} m"
    }
}

fun formatSupportDeskPreciseDuration(totalSeconds: Int): String {
    if (totalSeconds <= 0) return "0 s"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return buildList {
        if (hours > 0) add("${hours} h")
        if (minutes > 0 || hours > 0) add("${minutes} m")
        add("${seconds} s")
    }.joinToString(" ")
}

fun formatSupportDeskClockDuration(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
