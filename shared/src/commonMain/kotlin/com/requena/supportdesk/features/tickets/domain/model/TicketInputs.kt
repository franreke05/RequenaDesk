package com.requena.supportdesk.features.tickets.domain.model

import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.WaitingOn

data class TicketFilters(
    val query: String = "",
    val status: TicketStatus? = null,
    val priority: TicketPriority? = null,
    val category: TicketCategory? = null,
    val platform: SupportPlatform? = null,
    val waitingOn: WaitingOn? = null,
)

data class CreateTicketInput(
    val clientId: String = "",
    val subject: String = "",
    val description: String = "",
    val category: TicketCategory = TicketCategory.QUESTION,
    val affectedApp: String = "",
    val platform: SupportPlatform = SupportPlatform.DESKTOP,
    val appVersion: String = "",
    val stepsToReproduce: String = "",
    val clientReference: String = "",
    val priority: TicketPriority = TicketPriority.MEDIUM,
)
