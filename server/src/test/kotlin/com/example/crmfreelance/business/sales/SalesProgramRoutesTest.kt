package com.example.crmfreelance.business.sales

import com.requena.supportdesk.server.business.sales.InMemorySalesProgramStore
import com.requena.supportdesk.server.business.sales.SalesProgramAccessGuard
import com.requena.supportdesk.server.business.sales.SalesProgramIdentity
import com.requena.supportdesk.server.business.sales.SalesProgramService
import com.requena.supportdesk.server.business.sales.salesProgramRoutes
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.plugins.configureMonitoring
import com.requena.supportdesk.server.plugins.configureRequestSecurity
import com.requena.supportdesk.server.plugins.configureSerialization
import com.requena.supportdesk.server.security.ServerAuthSettings
import com.requena.supportdesk.server.security.SupportDeskTokenService
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SalesProgramRoutesTest {
    private val tokenService = SupportDeskTokenService(
        ServerAuthSettings("route-test-secret-123456789", "test", "test", 30, 1),
    )
    private val clientA = SalesProgramIdentity("00000000-0000-0000-0000-000000000011", "00000000-0000-0000-0000-000000000001")
    private val clientB = SalesProgramIdentity("00000000-0000-0000-0000-000000000012", "00000000-0000-0000-0000-000000000002")

    @Test
    fun routesRequireBearerEntitlementAndValidatePayloads() = testApplication {
        application {
            configureSerialization()
            configureMonitoring()
            configureRequestSecurity()
            val service = SalesProgramService(
                InMemorySalesProgramStore(),
                SalesProgramAccessGuard { identity, _ -> identity.clientId == clientA.clientId },
            )
            routing { salesProgramRoutes(service, tokenService) }
        }
        assertEquals(HttpStatusCode.Unauthorized, client.get("/client/apps/customers").status)

        val denied = client.get("/client/apps/customers") { bearer(clientToken(clientB)) }
        assertEquals(HttpStatusCode.Forbidden, denied.status)

        val invalid = client.post("/client/apps/customers") {
            bearer(clientToken(clientA))
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":""}""")
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, invalid.status)

        val created = client.post("/client/apps/customers") {
            bearer(clientToken(clientA))
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"Cliente de prueba real"}""")
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val createdBody = created.bodyAsText()
        assertTrue(createdBody.contains("Cliente de prueba real"))

        val tag = client.post("/client/apps/customers/tags") {
            bearer(clientToken(clientA))
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Prioritario","colorHex":"#445566","clientId":"${clientB.clientId}"}""")
        }
        assertEquals(HttpStatusCode.Created, tag.status)
        val tagId = extract(tag.bodyAsText(), "id")

        val trackedCustomer = client.post("/client/apps/customers") {
            bearer(clientToken(clientA))
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "displayName":"Cliente con seguimiento",
                  "stage":"PROSPECT",
                  "tagIds":["$tagId"],
                  "clientId":"${clientB.clientId}",
                  "createdByUserId":"${clientB.userId}"
                }
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.Created, trackedCustomer.status)
        val trackedBody = trackedCustomer.bodyAsText()
        assertTrue(trackedBody.contains("\"stage\":\"PROSPECT\""))
        assertTrue(trackedBody.contains("Prioritario"))
        val customerId = extract(trackedBody, "id")

        val invalidActivity = client.post("/client/apps/customers/$customerId/activities") {
            bearer(clientToken(clientA))
            contentType(ContentType.Application.Json)
            setBody("""{"type":"FOLLOW_UP","subject":"Llamar","dueAt":"mañana"}""")
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, invalidActivity.status)

        val activity = client.post("/client/apps/customers/$customerId/activities") {
            bearer(clientToken(clientA))
            contentType(ContentType.Application.Json)
            setBody("""{"type":"FOLLOW_UP","subject":"Llamar","dueAt":"2026-08-03T09:00:00Z"}""")
        }
        assertEquals(HttpStatusCode.Created, activity.status)
        assertTrue(activity.bodyAsText().contains("Llamar"))

        val filtered = client.get("/client/apps/customers?stage=PROSPECT&tagId=$tagId") {
            bearer(clientToken(clientA))
        }
        assertEquals(HttpStatusCode.OK, filtered.status)
        assertTrue(filtered.bodyAsText().contains("Cliente con seguimiento"))
    }

    private fun clientToken(identity: SalesProgramIdentity): String = tokenService.createAccessToken(
        ServerAuthIdentity(identity.userId, "Cliente", "cliente@example.test", "CLIENT", identity.clientId, organizationId = "org-test"),
    )

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private fun extract(body: String, field: String): String =
        requireNotNull(Regex(""""$field":"([^"]+)"""").find(body)?.groupValues?.get(1))
}
