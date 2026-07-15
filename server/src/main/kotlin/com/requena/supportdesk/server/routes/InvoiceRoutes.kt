package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateInvoiceRequest
import com.requena.supportdesk.server.domain.model.ServerInvoiceSnapshot
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.isAdmin
import com.requena.supportdesk.server.utils.ownerAdminIdFor
import com.requena.supportdesk.server.utils.requestJson
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
import com.requena.supportdesk.server.utils.respondJson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.decodeFromString
import java.util.Base64

// Invoices are generated on demand for download only — nothing is persisted.
// Desktop's Desktop.browse() cannot set headers, so this stays a GET with
// query-token auth, matching the pattern the old /pdf endpoint used.
fun Route.invoiceRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/admin/invoices") {
        get("/generate") {
            val identity = call.requireAuthenticatedIdentity(tokenService, allowQueryToken = true) ?: return@get
            val ownerAdminId = service.ownerAdminIdFor(identity)
                ?: return@get call.respondJson(HttpStatusCode.Forbidden, errorResponse("No client account is linked to this user"))
            val encodedData = call.request.queryParameters["data"]
                ?: return@get call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing data parameter"))
            val decoded = runCatching {
                val jsonText = Base64.getUrlDecoder().decode(encodedData).toString(Charsets.UTF_8)
                requestJson.decodeFromString<CreateInvoiceRequest>(jsonText)
            }.getOrNull()
                ?: return@get call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid data parameter"))
            // Clients can only generate invoices for their own client id, mirroring
            // the clientId-spoofing guard already used by TaskRoutes.post("/tasks").
            val request = if (identity.isAdmin) decoded else decoded.copy(clientId = identity.clientId.orEmpty())

            if (request.clientId.isBlank() || request.issuedAt.isBlank() || request.items.isEmpty()) {
                return@get call.respondJson(
                    HttpStatusCode.BadRequest,
                    errorResponse("clientId, issuedAt y al menos un item son obligatorios"),
                )
            }

            val invoice = service.generateInvoice(request, ownerAdminId = ownerAdminId)
            call.respondText(contentType = ContentType.Text.Html, status = HttpStatusCode.OK) {
                invoiceHtml(invoice)
            }
        }
    }
}

private fun escapeHtml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

// Locale.ROOT keeps the decimal separator as "." regardless of the server's
// default locale, so the rendered total is deterministic and test-stable.
private fun money(value: Double): String = String.format(java.util.Locale.ROOT, "%.2f", value)

private fun invoiceHtml(invoice: ServerInvoiceSnapshot): String {
    val subtotal = invoice.items.sumOf { it.quantity * it.unitPrice }
    val taxAmount = subtotal * (invoice.taxPercent / 100.0)
    val total = subtotal + taxAmount
    val itemRows = invoice.items.sortedBy { it.sortOrder }.joinToString("") { item ->
        val s = item.quantity * item.unitPrice
        "<tr><td>${escapeHtml(item.description)}</td><td style='text-align:right'>${money(item.quantity)}</td><td style='text-align:right'>${money(item.unitPrice)}</td><td style='text-align:right'>${money(s)}</td></tr>"
    }
    return """<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8"><title>Factura ${escapeHtml(invoice.invoiceNumber)}</title>
<style>*{box-sizing:border-box;margin:0;padding:0}body{font-family:Georgia,serif;color:#1a1a1a;padding:48px;max-width:800px;margin:0 auto}
h1{font-size:28px;margin-bottom:4px}.sub{color:#666;font-size:14px;margin-bottom:32px}.meta{display:flex;justify-content:space-between;margin-bottom:32px}
.meta-block{font-size:14px;line-height:1.6}.meta-block strong{display:block;font-size:11px;text-transform:uppercase;letter-spacing:.05em;color:#999;margin-bottom:2px}
table{width:100%;border-collapse:collapse;margin:24px 0;font-size:14px}th{background:#f5f0e8;padding:10px 12px;text-align:left;font-size:11px;text-transform:uppercase}
td{padding:10px 12px;border-bottom:1px solid #eee}.totals{margin-left:auto;width:280px}.totals td{border:none;padding:6px 12px}
.total-row td{font-weight:bold;font-size:16px;border-top:2px solid #1a1a1a;padding-top:10px}
.notes{margin-top:32px;font-size:13px;color:#666;font-style:italic}@media print{.no-print{display:none}}</style></head>
<body><h1>${escapeHtml(invoice.invoiceNumber)}</h1><p class="sub">${escapeHtml(invoice.clientName)}</p>
<div class="meta"><div class="meta-block"><strong>Fecha emisión</strong>${invoice.issuedAt}</div>
${if (invoice.dueAt != null) "<div class='meta-block'><strong>Vencimiento</strong>${invoice.dueAt}</div>" else ""}</div>
<table><thead><tr><th>Descripción</th><th style='text-align:right'>Cant.</th><th style='text-align:right'>Precio unit.</th><th style='text-align:right'>Subtotal</th></tr></thead>
<tbody>$itemRows</tbody></table>
<table class="totals"><tr><td>Subtotal</td><td style='text-align:right'>${money(subtotal)}</td></tr>
<tr><td>Impuesto (${invoice.taxPercent}%)</td><td style='text-align:right'>${money(taxAmount)}</td></tr>
<tr class="total-row"><td>Total</td><td style='text-align:right'>${money(total)}</td></tr></table>
${if (!invoice.notes.isNullOrBlank()) "<p class='notes'>${escapeHtml(invoice.notes)}</p>" else ""}
<p class="no-print" style="margin-top:32px;text-align:center">
<button onclick="window.print()" style="padding:10px 24px;background:#b38349;color:white;border:none;border-radius:6px;font-size:14px;cursor:pointer">Imprimir / Guardar como PDF</button></p>
</body></html>"""
}
