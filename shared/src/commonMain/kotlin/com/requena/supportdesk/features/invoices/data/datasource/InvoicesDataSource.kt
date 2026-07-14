package com.requena.supportdesk.features.invoices.data.datasource

import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.network.AdminSessionContext
import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.invoices.data.dto.CreateInvoiceRequestDto
import com.requena.supportdesk.features.invoices.data.dto.InvoiceDto
import com.requena.supportdesk.features.invoices.data.dto.InvoicePdfUrlDto
import com.requena.supportdesk.features.invoices.data.dto.UpdateInvoiceStatusRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

interface InvoicesDataSource {
    suspend fun getInvoices(): List<InvoiceDto>
    suspend fun getInvoice(id: String): InvoiceDto?
    suspend fun createInvoice(request: CreateInvoiceRequestDto): InvoiceDto
    suspend fun updateStatus(invoiceId: String, request: UpdateInvoiceStatusRequestDto): InvoiceDto
    suspend fun getPdfUrl(invoiceId: String): InvoicePdfUrlDto
}

class RemoteInvoicesDataSource(
    private val httpClient: HttpClient,
) : InvoicesDataSource {

    private fun invoicesPath(): String =
        if (AdminSessionContext.currentUser()?.role == UserRole.CLIENT) "/client/invoices" else "/admin/invoices"

    override suspend fun getInvoices(): List<InvoiceDto> =
        httpClient.get("${supportDeskBaseUrl()}${invoicesPath()}").requireApiData()

    override suspend fun getInvoice(id: String): InvoiceDto? =
        httpClient.get("${supportDeskBaseUrl()}${invoicesPath()}/$id").requireApiData()

    override suspend fun createInvoice(request: CreateInvoiceRequestDto): InvoiceDto =
        httpClient.post("${supportDeskBaseUrl()}/admin/invoices") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun updateStatus(invoiceId: String, request: UpdateInvoiceStatusRequestDto): InvoiceDto =
        httpClient.patch("${supportDeskBaseUrl()}/admin/invoices/$invoiceId/status") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun getPdfUrl(invoiceId: String): InvoicePdfUrlDto =
        httpClient.get("${supportDeskBaseUrl()}${invoicesPath()}/$invoiceId/pdf").requireApiData()
}
