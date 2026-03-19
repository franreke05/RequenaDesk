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
    TicketStatus.OPEN -> "Open"
    TicketStatus.IN_PROGRESS -> "In progress"
    TicketStatus.PENDING_CLIENT -> "Pending client"
    TicketStatus.RESOLVED -> "Resolved"
    TicketStatus.CLOSED -> "Closed"
}

fun TicketPriority.displayName(): String = when (this) {
    TicketPriority.LOW -> "Low"
    TicketPriority.MEDIUM -> "Medium"
    TicketPriority.HIGH -> "High"
    TicketPriority.URGENT -> "Urgent"
}

fun TicketCategory.displayName(): String = when (this) {
    TicketCategory.BUG -> "Bug"
    TicketCategory.ACCESS -> "Access"
    TicketCategory.BILLING -> "Billing"
    TicketCategory.CHANGE_REQUEST -> "Change request"
    TicketCategory.QUESTION -> "Question"
    TicketCategory.OTHER -> "Other"
}

fun SupportPlatform.displayName(): String = when (this) {
    SupportPlatform.ANDROID -> "Android"
    SupportPlatform.IOS -> "iOS"
    SupportPlatform.DESKTOP -> "Desktop"
    SupportPlatform.WEB -> "Web"
    SupportPlatform.BACKEND -> "Backend"
    SupportPlatform.OTHER -> "Other"
}

fun WaitingOn.displayName(): String = when (this) {
    WaitingOn.CLIENT -> "Waiting on client"
    WaitingOn.ADMIN -> "Waiting on admin"
}

fun ClientAccountStatus.displayName(): String = when (this) {
    ClientAccountStatus.ACTIVE -> "Active"
    ClientAccountStatus.PAUSED -> "Paused"
    ClientAccountStatus.INACTIVE -> "Inactive"
}

fun ClientServiceTier.displayName(): String = when (this) {
    ClientServiceTier.STANDARD -> "Standard"
    ClientServiceTier.PRIORITY -> "Priority"
    ClientServiceTier.VIP -> "VIP"
}

fun PreferredContactChannel.displayName(): String = when (this) {
    PreferredContactChannel.TICKET -> "Ticket"
    PreferredContactChannel.EMAIL -> "Email"
    PreferredContactChannel.WHATSAPP -> "WhatsApp"
    PreferredContactChannel.CALL -> "Call"
}

fun UserRole.displayName(): String = when (this) {
    UserRole.CLIENT -> "Client"
    UserRole.ADMIN -> "Admin"
}

fun AppDestination.displayTitle(): String = when (this) {
    AppDestination.Login -> "Welcome"
    AppDestination.Dashboard -> "Dashboard"
    AppDestination.Tickets -> "Tickets"
    AppDestination.CreateTicket -> "Create ticket"
    is AppDestination.TicketDetail -> "Ticket detail"
    AppDestination.Clients -> "Clients"
    AppDestination.Notifications -> "Notifications"
}

fun AppDestination.displaySubtitle(): String = when (this) {
    AppDestination.Login -> "Freelance support, one desktop product."
    AppDestination.Dashboard -> "Daily admin view with active workload."
    AppDestination.Tickets -> "Review, filter and resolve tickets fast."
    AppDestination.CreateTicket -> "Open a new ticket with the minimum friction."
    is AppDestination.TicketDetail -> "Conversation, metadata and next actions."
    AppDestination.Clients -> "Accounts, contacts and ticket activity."
    AppDestination.Notifications -> "Admin mobile alerts and device registration."
}

fun formatSupportDeskDateTime(raw: String): String {
    if (raw.isBlank()) return "-"
    val cleaned = raw.removeSuffix("Z").replace("T", "  ")
    return if (cleaned.length > 16) cleaned.substring(0, 16) else cleaned
}
