package com.requena.supportdesk.features.programs.data.mapper

import com.requena.supportdesk.core.model.ClientProgram
import com.requena.supportdesk.core.model.ClientProgramBillingLine
import com.requena.supportdesk.core.model.ClientProgramBillingPreview
import com.requena.supportdesk.core.model.ClientProgramRequest
import com.requena.supportdesk.core.model.ClientProgramSubscription
import com.requena.supportdesk.core.model.ClientProgramSubscriptionStatus
import com.requena.supportdesk.core.model.ClientProgramsOverview
import com.requena.supportdesk.core.model.ProgramRequestStatus
import com.requena.supportdesk.features.programs.data.dto.ClientProgramBillingPreviewDto
import com.requena.supportdesk.features.programs.data.dto.ClientProgramDto
import com.requena.supportdesk.features.programs.data.dto.ClientProgramRequestDto
import com.requena.supportdesk.features.programs.data.dto.ClientProgramSubscriptionDto
import com.requena.supportdesk.features.programs.data.dto.ClientProgramsOverviewDto

object ProgramsMapper {
    fun overview(dto: ClientProgramsOverviewDto): ClientProgramsOverview = ClientProgramsOverview(
        catalog = dto.catalog.map(::program),
        subscriptions = dto.subscriptions.map(::subscription),
        requests = dto.requests.map(::request),
    )

    fun program(dto: ClientProgramDto): ClientProgram = ClientProgram(
        key = dto.key,
        name = dto.name,
        shortDescription = dto.shortDescription,
        category = dto.category,
        iconKey = dto.iconKey,
        monthlyPriceCents = dto.monthlyPriceCents,
        currency = dto.currency,
        isRequestable = dto.isRequestable,
        isAvailable = dto.isAvailable,
        capabilities = dto.capabilities,
    )

    fun subscription(dto: ClientProgramSubscriptionDto): ClientProgramSubscription = ClientProgramSubscription(
        productKey = dto.productKey,
        status = enumValueOrDefault(dto.status, ClientProgramSubscriptionStatus.CANCELLED),
        monthlyPriceCents = dto.monthlyPriceCents,
        currency = dto.currency,
        startsOn = dto.startsOn,
        endsOn = dto.endsOn,
    )

    fun request(dto: ClientProgramRequestDto): ClientProgramRequest = ClientProgramRequest(
        id = dto.id,
        productKey = dto.productKey,
        status = enumValueOrDefault(dto.status, ProgramRequestStatus.CANCELLED),
        customerNote = dto.customerNote,
        adminNote = dto.adminNote,
        requestedAt = dto.requestedAt,
        decidedAt = dto.decidedAt,
        quotedMonthlyPriceCents = dto.quotedMonthlyPriceCents,
        currency = dto.currency,
        clientId = dto.clientId,
        clientCompanyName = dto.clientCompanyName,
        requestedByName = dto.requestedByName,
    )

    fun billingPreview(dto: ClientProgramBillingPreviewDto): ClientProgramBillingPreview = ClientProgramBillingPreview(
        clientId = dto.clientId,
        period = dto.period,
        lines = dto.lines.map {
            ClientProgramBillingLine(it.productKey, it.name, it.monthlyPriceCents, it.currency)
        },
        totalMonthlyPriceCents = dto.totalMonthlyPriceCents,
        currency = dto.currency,
    )

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)
}
