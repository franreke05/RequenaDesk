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
        assertTrue(created.bodyAsText().contains("Cliente de prueba real"))
    }

    private fun clientToken(identity: SalesProgramIdentity): String = tokenService.createAccessToken(
        ServerAuthIdentity(identity.userId, "Cliente", "cliente@example.test", "CLIENT", identity.clientId),
    )

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }
}
