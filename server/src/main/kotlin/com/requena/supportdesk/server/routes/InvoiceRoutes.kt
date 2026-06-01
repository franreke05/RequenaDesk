package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateInvoiceRequest
import com.requena.supportdesk.server.domain.model.UpdateInvoiceStatusRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.invoiceJson
import com.requena.supportdesk.server.utils.invoicesJson
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.requireAdminIdentity
import com.requena.supportdesk.server.utils.requireClientIdentity
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.invoiceRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    adminInvoiceRoutes(service, tokenService)
    clientInvoiceRoutes(service, tokenService)
}

private fun Route.adminInvoiceRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/admin/invoices") {
        get {
            val identity = call.requireAdminIdentity(tokenService) ?: return@get
            val limit = call.request.queryParameters.boundedInt("limit", default = 100, max = 200)
            val offset = call.request.queryParameters.boundedInt("offset", default = 0, max = 10_000)
            call.respondJson(
                body = successResponse("/admin/invoices",
                    invoicesJson(service.invoices(ownerAdminId = identity.userId, limit = limit, offset = offset))),
            )
        }

        get("/{id}") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@get
            val id = call.parameters["id"]
                ?: return@get call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing invoice id"))
            val invoice = service.invoice(id, ownerAdminId = identity.userId)
                ?: return@get call.respondJson(HttpStatusCode.NotFound, errorResponse("Invoice not found"))
            call.respondJson(body = successResponse("/admin/invoices/$id", invoiceJson(invoice)))
        }

        post {
            val identity = call.requireAdminIdentity(tokenService) ?: return@post
            val request = call.receiveOrDefault(CreateInvoiceRequest())
            if (request.clientId.isBlank() || request.issuedAt.isBlank() || request.items.isEmpty()) {
                return@post call.respondJson(
                    HttpStatusCode.BadRequest,
                    errorResponse("clientId, issuedAt y al menos un item son obligatorios"),
                )
            }
            val invoice = service.createdInvoice(request, ownerAdminId = identity.userId)
            call.respondJson(
                HttpStatusCode.Created,
                successResponse("/admin/invoices", invoiceJson(invoice)),
            )
        }

        patch("/{id}/status") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@patch
            val id = call.parameters["id"]
                ?: return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing invoice id"))
            val request = call.receiveOrDefault(UpdateInvoiceStatusRequest())
            if (request.status !in setOf("SENT", "PAID", "CANCELLED")) {
                return@patch call.respondJson(
                    HttpStatusCode.BadRequest,
                    errorResponse("status debe ser SENT, PAID o CANCELLED"),
                )
            }
            val invoice = service.updatedInvoiceStatus(id, request, ownerAdminId = identity.userId)
            call.respondJson(body = successResponse("/admin/invoices/$id/status", invoiceJson(invoice)))
        }

        // PDF: returns printable HTML (browser can print/save as PDF)
        get("/{id}/pdf") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@get
            val id = call.parameters["id"]
                ?: return@get call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing invoice id"))
            val invoice = service.invoice(id, ownerAdminId = identity.userId)
                ?: return@get call.respondJson(HttpStatusCode.NotFound, errorResponse("Invoice not found"))
            call.respondText(contentType = ContentType.Text.Html, status = HttpStatusCode.OK) {
                invoiceHtml(invoice)
            }
        }
    }
}

private fun Route.clientInvoiceRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/client/invoices") {
        get {
            val identity = call.requireClientIdentity(tokenService) ?: return@get
            val clientId = identity.clientId
                ?: return@get call.respondJson(HttpStatusCode.Forbidden, errorResponse("Client identity required"))
            val limit = call.request.queryParameters.boundedInt("limit", default = 100, max = 100)
            val offset = call.request.queryParameters.boundedInt("offset", default = 0, max = 10_000)
            call.respondJson(
                body = successResponse("/client/invoices",
                    invoicesJson(service.invoices(clientId = clientId, limit = limit, offset = offset))),
            )
        }

        get("/{id}") {
            val identity = call.requireClientIdentity(tokenService) ?: return@get
            val clientId = identity.clientId
                ?: return@get call.respondJson(HttpStatusCode.Forbidden, errorResponse("Client identity required"))
            val id = call.parameters["id"]
                ?: return@get call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing invoice id"))
            val invoice = service.invoice(id, clientId = clientId)
                ?: return@get call.respondJson(HttpStatusCode.NotFound, errorResponse("Invoice not found"))
            call.respondJson(body = successResponse("/client/invoices/$id", invoiceJson(invoice)))
        }

        get("/{id}/pdf") {
            val identity = call.requireClientIdentity(tokenService) ?: return@get
            val clientId = identity.clientId
                ?: return@get call.respondJson(HttpStatusCode.Forbidden, errorResponse("Client identity required"))
            val id = call.parameters["id"]
                ?: return@get call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing invoice id"))
            val invoice = service.invoice(id, clientId = clientId)
                ?: return@get call.respondJson(HttpStatusCode.NotFound, errorResponse("Invoice not found"))
            call.respondText(contentType = ContentType.Text.Html, status = HttpStatusCode.OK) {
                invoiceHtml(invoice)
            }
        }
    }
}

private fun io.ktor.http.Parameters.boundedInt(name: String, default: Int, max: Int): Int =
    this[name]?.toIntOrNull()?.coerceIn(0, max) ?: default

private fun invoiceHtml(invoice: com.requena.supportdesk.server.domain.model.ServerInvoiceSnapshot): String {
    val subtotal = invoice.items.sumOf { it.quantity * it.unitPrice }
    val taxAmount = subtotal * (invoice.taxPercent / 100.0)
    val total = subtotal + taxAmount

    val itemRows = invoice.items.sortedBy { it.sortOrder }.joinToString("\n") { item ->
        val itemSubtotal = item.quantity * item.unitPrice
        """
        <tr>
          <td>${item.description}</td>
          <td style="text-align:right">${"%.2f".format(item.quantity)}</td>
          <td style="text-align:right">${"%.2f".format(item.unitPrice)}</td>
          <td style="text-align:right">${"%.2f".format(itemSubtotal)}</td>
        </tr>
        """.trimIndent()
    }

    return """
    <!DOCTYPE html>
    <html lang="es">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <title>Factura ${invoice.invoiceNumber}</title>
      <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: Georgia, serif; color: #1a1a1a; background: #fff; padding: 48px; max-width: 800px; margin: 0 auto; }
        h1 { font-size: 28px; font-weight: bold; margin-bottom: 4px; }
        .subtitle { color: #666; font-size: 14px; margin-bottom: 32px; }
        .meta { display: flex; justify-content: space-between; margin-bottom: 32px; }
        .meta-block { font-size: 14px; line-height: 1.6; }
        .meta-block strong { display: block; font-size: 11px; text-transform: uppercase; letter-spacing: .05em; color: #999; margin-bottom: 2px; }
        table { width: 100%; border-collapse: collapse; margin: 24px 0; font-size: 14px; }
        th { background: #f5f0e8; padding: 10px 12px; text-align: left; font-size: 11px; text-transform: uppercase; letter-spacing: .05em; }
        td { padding: 10px 12px; border-bottom: 1px solid #eee; }
        .totals { margin-left: auto; width: 280px; font-size: 14px; }
        .totals tr td { border: none; padding: 6px 12px; }
        .totals .total-row td { font-weight: bold; font-size: 16px; border-top: 2px solid #1a1a1a; padding-top: 10px; }
        .status { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold;
                  background: ${when(invoice.status) { "PAID" -> "#e2efe6"; "SENT" -> "#e1eced"; "CANCELLED" -> "#f7e4e1"; else -> "#f0f0f0" }};
                  color: ${when(invoice.status) { "PAID" -> "#2e6a44"; "SENT" -> "#3d6468"; "CANCELLED" -> "#9a3e35"; else -> "#555" }}; }
        .notes { margin-top: 32px; font-size: 13px; color: #666; font-style: italic; }
        @media print { body { padding: 24px; } .no-print { display: none; } }
      </style>
    </head>
    <body>
      <h1>${invoice.invoiceNumber}</h1>
      <p class="subtitle">${invoice.clientName}</p>
      <div class="meta">
        <div class="meta-block">
          <strong>Estado</strong>
          <span class="status">${when(invoice.status) { "DRAFT" -> "Borrador"; "SENT" -> "Enviada"; "PAID" -> "Pagada"; "CANCELLED" -> "Cancelada"; else -> invoice.status }}</span>
        </div>
        <div class="meta-block">
          <strong>Fecha de emisión</strong>${invoice.issuedAt}
        </div>
        ${if (invoice.dueAt != null) """<div class="meta-block"><strong>Vencimiento</strong>${invoice.dueAt}</div>""" else ""}
      </div>

      <table>
        <thead>
          <tr><th>Descripción</th><th style="text-align:right">Cantidad</th><th style="text-align:right">Precio unit.</th><th style="text-align:right">Subtotal</th></tr>
        </thead>
        <tbody>
          $itemRows
        </tbody>
      </table>

      <table class="totals">
        <tr><td>Subtotal</td><td style="text-align:right">${"%.2f".format(subtotal)}</td></tr>
        <tr><td>Impuesto (${invoice.taxPercent}%)</td><td style="text-align:right">${"%.2f".format(taxAmount)}</td></tr>
        <tr class="total-row"><td>Total</td><td style="text-align:right">${"%.2f".format(total)}</td></tr>
      </table>

      ${if (!invoice.notes.isNullOrBlank()) """<p class="notes">${invoice.notes}</p>""" else ""}

      <p class="no-print" style="margin-top:32px; text-align:center">
        <button onclick="window.print()" style="padding:10px 24px; background:#b38349; color:white; border:none; border-radius:6px; font-size:14px; cursor:pointer">
          Imprimir / Guardar como PDF
        </button>
      </p>
    </body>
    </html>
    """.trimIndent()
}
