package com.requena.supportdesk.features.clients.data.mapper

import com.requena.supportdesk.core.model.ClientAccountStatus
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.ClientPortalComponent
import com.requena.supportdesk.core.model.ClientServiceTier
import com.requena.supportdesk.core.model.PreferredContactChannel
import com.requena.supportdesk.core.model.ClientBillingProfile
import com.requena.supportdesk.features.clients.data.dto.ClientBillingDto
import com.requena.supportdesk.features.clients.data.dto.ClientDto

object ClientsMapper {
    fun fromDto(dto: ClientDto): Client = Client(
        id = dto.id,
        companyName = dto.companyName,
        productName = dto.productName,
        contactName = dto.contactName.ifBlank { dto.companyName },
        email = dto.email,
        accountStatus = ClientAccountStatus.valueOf(dto.accountStatus),
        serviceTier = ClientServiceTier.valueOf(dto.serviceTier),
        enabledComponents = dto.enabledComponents.mapNotNull { component ->
            runCatching { ClientPortalComponent.valueOf(component) }.getOrNull()
        }.toSet(),
        preferredContactChannel = PreferredContactChannel.valueOf(dto.preferredContactChannel),
        activeTicketCount = dto.activeTicketCount,
        billing = fromDto(dto.billing),
    )

    fun fromDto(dto: ClientBillingDto): ClientBillingProfile = ClientBillingProfile(
        legalName = dto.legalName,
        taxId = dto.taxId,
        addressLine = dto.addressLine,
        postalCode = dto.postalCode,
        city = dto.city,
        province = dto.province,
        country = dto.country,
        email = dto.email,
    )

    fun toDto(billing: ClientBillingProfile): ClientBillingDto = ClientBillingDto(
        legalName = billing.legalName,
        taxId = billing.taxId,
        addressLine = billing.addressLine,
        postalCode = billing.postalCode,
        city = billing.city,
        province = billing.province,
        country = billing.country,
        email = billing.email,
    )
}
