package com.requena.supportdesk.core.model

data class Client(
    val id: String,
    val companyName: String,
    val productName: String = "",
    val contactName: String,
    val email: String,
    val accountStatus: ClientAccountStatus = ClientAccountStatus.ACTIVE,
    val serviceTier: ClientServiceTier = ClientServiceTier.STANDARD,
    val enabledComponents: Set<ClientPortalComponent> = emptySet(),
    val preferredContactChannel: PreferredContactChannel = PreferredContactChannel.TICKET,
    val activeTicketCount: Int = 0,
    val notes: List<ClientNote> = emptyList(),
    val monthlyHoursSummary: ClientMonthlyHoursSummary? = null,
    val timeEntries: List<TimeEntry> = emptyList(),
) {
    fun hasComponent(component: ClientPortalComponent): Boolean = component in enabledComponents
}
