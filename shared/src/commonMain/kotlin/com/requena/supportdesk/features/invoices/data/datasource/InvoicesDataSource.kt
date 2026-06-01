package com.requena.supportdesk.features.invoices.data.datasource

import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.network.AdminSessionContext
import com.requena.supportdesk.core.network.NetworkLogger
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

    override suspend fun getInvoices(): List<InvoiceDto> {
        val url = "${supportDeskBaseUrl()}${invoicesPath()}"
        return try {
            NetworkLogger.addLog("[DEBUG] GET $url")
            httpClient.get(url).requireApiData()
        } catch (e: Exception) {
            NetworkLogger.addLog("[ERROR] getInvoices failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun getInvoice(id: String): InvoiceDto? {
        val url = "${supportDeskBaseUrl()}${invoicesPath()}/$id"
        return try {
            NetworkLogger.addLog("[DEBUG] GET $url")
            httpClient.get(url).requireApiData()
        } catch (e: Exception) {
            NetworkLogger.addLog("[ERROR] getInvoice($id) failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun createInvoice(request: CreateInvoiceRequestDto): InvoiceDto {
        val url = "${supportDeskBaseUrl()}/admin/invoices"
        return try {
            NetworkLogger.addLog("[DEBUG] POST $url")
            httpClient.post(url) {
                setBody(jsonRequestBody(request))
            }.requireApiData()
        } catch (e: Exception) {
            NetworkLogger.addLog("[ERROR] createInvoice failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun updateStatus(invoiceId: String, request: UpdateInvoiceStatusRequestDto): InvoiceDto {
        val url = "${supportDeskBaseUrl()}/admin/invoices/$invoiceId/status"
        return try {
            NetworkLogger.addLog("[DEBUG] PATCH $url")
            httpClient.patch(url) {
                setBody(jsonRequestBody(request))
            }.requireApiData()
        } catch (e: Exception) {
            NetworkLogger.addLog("[ERROR] updateStatus($invoiceId) failed: ${e.message} at $url")
            throw e
        }
    }

    override suspend fun getPdfUrl(invoiceId: String): InvoicePdfUrlDto {
        val url = "${supportDeskBaseUrl()}${invoicesPath()}/$invoiceId/pdf"
        return try {
            NetworkLogger.addLog("[DEBUG] GET $url")
            httpClient.get(url).requireApiData()
        } catch (e: Exception) {
            NetworkLogger.addLog("[ERROR] getPdfUrl($invoiceId) failed: ${e.message} at $url")
            throw e
        }
    }
}
