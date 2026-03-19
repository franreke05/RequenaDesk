package com.requena.supportdesk.features.clients.data.mapper

import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.features.clients.data.dto.ClientDto

object ClientsMapper {
    fun fromDto(dto: ClientDto): Client = Client(
        id = dto.id,
        companyName = dto.companyName,
        contactName = dto.contactName,
        email = dto.email,
    )
}
