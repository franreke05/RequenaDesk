package com.requena.supportdesk.server.data.repository

import com.requena.supportdesk.server.data.datasource.InMemorySupportDeskDataSource
import com.requena.supportdesk.server.data.mapper.SupportDeskMapper
import com.requena.supportdesk.server.domain.model.ServerAttachmentSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientSnapshot
import com.requena.supportdesk.server.domain.model.ServerDashboardSnapshot
import com.requena.supportdesk.server.domain.model.ServerDeviceRegistration
import com.requena.supportdesk.server.domain.model.ServerTicketSnapshot
import com.requena.supportdesk.server.domain.repository.SupportDeskRepository

class InMemorySupportDeskRepository(
    private val dataSource: InMemorySupportDeskDataSource,
) : SupportDeskRepository {
    override fun getTickets(): List<ServerTicketSnapshot> = dataSource.tickets().map(SupportDeskMapper::ticket)

    override fun getTicket(id: String): ServerTicketSnapshot? = dataSource.ticket(id)?.let(SupportDeskMapper::ticket)

    override fun getClients(): List<ServerClientSnapshot> = dataSource.clients().map(SupportDeskMapper::client)

    override fun getDashboard(): ServerDashboardSnapshot = SupportDeskMapper.dashboard(dataSource.dashboard())

    override fun getAttachment(id: String): ServerAttachmentSnapshot? = dataSource.attachment(id)?.let(SupportDeskMapper::attachment)

    override fun registerDevice(): ServerDeviceRegistration = SupportDeskMapper.device(dataSource.registerDevice())
}
