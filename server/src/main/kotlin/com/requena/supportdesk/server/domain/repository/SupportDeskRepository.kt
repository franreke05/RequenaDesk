package com.requena.supportdesk.server.domain.repository

import com.requena.supportdesk.server.domain.model.ServerAttachmentSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientSnapshot
import com.requena.supportdesk.server.domain.model.ServerDashboardSnapshot
import com.requena.supportdesk.server.domain.model.ServerDeviceRegistration
import com.requena.supportdesk.server.domain.model.ServerTicketSnapshot

interface SupportDeskRepository {
    fun getTickets(): List<ServerTicketSnapshot>
    fun getTicket(id: String): ServerTicketSnapshot?
    fun getClients(): List<ServerClientSnapshot>
    fun getDashboard(): ServerDashboardSnapshot
    fun getAttachment(id: String): ServerAttachmentSnapshot?
    fun registerDevice(): ServerDeviceRegistration
}
