package com.requena.supportdesk.features.invoices.data.datasource

import com.requena.supportdesk.core.network.SupportDeskSessionManager
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.core.network.supportDeskNetworkJson
import com.requena.supportdesk.features.invoices.data.dto.CreateInvoiceRequestDto
import kotlin.io.encoding.Base64

interface InvoicesDataSource {
    fun buildGeneratedInvoiceUrl(request: CreateInvoiceRequestDto): String
}

class RemoteInvoicesDataSource(
    private val sessionManager: SupportDeskSessionManager,
) : InvoicesDataSource {

    override fun buildGeneratedInvoiceUrl(request: CreateInvoiceRequestDto): String {
        val accessToken = sessionManager.currentAccessToken()
            ?: error("No hay una sesion activa para generar la factura.")
        val payload = supportDeskNetworkJson.encodeToString(CreateInvoiceRequestDto.serializer(), request)
        val encodedData = Base64.UrlSafe.encode(payload.encodeToByteArray()).trimEnd('=')
        return "${supportDeskBaseUrl()}/admin/invoices/generate?data=$encodedData&access_token=$accessToken"
    }
}
