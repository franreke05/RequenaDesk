package com.example.crmfreelance.business.projects

import com.requena.supportdesk.server.business.projects.BusinessProjectsAccessGuard
import com.requena.supportdesk.server.business.projects.BusinessProjectsIdentity
import com.requena.supportdesk.server.business.projects.BusinessProjectsService
import com.requena.supportdesk.server.business.projects.InMemoryBusinessProjectsStore
import com.requena.supportdesk.server.business.projects.businessProjectsRoutes
import com.requena.supportdesk.server.data.datasource.InMemorySupportDeskDataSource
import com.requena.supportdesk.server.data.repository.InMemorySupportDeskRepository
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.plugins.configureMonitoring
import com.requena.supportdesk.server.plugins.configureRequestSecurity
import com.requena.supportdesk.server.plugins.configureSerialization
import com.requena.supportdesk.server.security.ServerAuthSettings
import com.requena.supportdesk.server.security.SupportDeskTokenService
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BusinessProjectsRoutesTest {
    private val tokenService = SupportDeskTokenService(
        ServerAuthSettings("projects-route-secret-123456789", "test", "test", 30, 1),
    )
    private val allowed = BusinessProjectsIdentity(
        "00000000-0000-0000-0000-000000000011",
        "00000000-0000-0000-0000-000000000001",
    )
    private val denied = BusinessProjectsIdentity(
        "00000000-0000-0000-0000-000000000022",
        "00000000-0000-0000-0000-000000000002",
    )

    @Test
    fun routesRejectNonClientsMissingEntitlementsAndForgedTenantFields() = testApplication {
        application {
            configureSerialization()
            configureMonitoring()
            configureRequestSecurity()
            val projects = BusinessProjectsService(
                InMemoryBusinessProjectsStore(),
                BusinessProjectsAccessGuard { it.clientId == allowed.clientId },
            )
            val supportDesk = SupportDeskService(
                InMemorySupportDeskRepository(InMemorySupportDeskDataSource()),
                tokenService,
            )
            routing { businessProjectsRoutes(projects, tokenService, supportDesk) }
        }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/client/apps/projects").status)
        assertEquals(
            HttpStatusCode.Forbidden,
            client.get("/client/apps/projects") { bearer(clientToken(denied)) }.status,
        )
        assertEquals(
            HttpStatusCode.Forbidden,
            client.get("/client/apps/projects") { bearer(adminToken()) }.status,
        )

        val created = client.post("/client/apps/projects") {
            bearer(clientToken(allowed))
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "clientId":"${denied.clientId}",
                  "createdByUserId":"${denied.userId}",
                  "name":"Proyecto seguro"
                }
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val body = created.bodyAsText()
        assertTrue(body.contains("Proyecto seguro"))
        assertFalse(body.contains(denied.clientId))
        assertFalse(body.contains("createdByUserId"))
        val projectId = extract(body, "id")

        val task = client.post("/client/apps/tasks") {
            bearer(clientToken(allowed))
            contentType(ContentType.Application.Json)
            setBody("""{"projectId":"$projectId","title":"Tarea segura"}""")
        }
        assertEquals(HttpStatusCode.Created, task.status)
        val taskId = extract(task.bodyAsText(), "id")

        val invalidCoordinates = client.put("/client/apps/task-board/placements/$taskId") {
            bearer(clientToken(allowed))
            contentType(ContentType.Application.Json)
            setBody("""{"boardDate":"2030-01-02","positionX":1.5,"positionY":0.2}""")
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, invalidCoordinates.status)

        val started = client.post("/client/apps/work-sessions/start") {
            bearer(clientToken(allowed))
            contentType(ContentType.Application.Json)
            setBody("""{"taskId":"$taskId"}""")
        }
        assertEquals(HttpStatusCode.Created, started.status)
        val sessionId = extract(started.bodyAsText(), "id")
        val paused = client.post("/client/apps/work-sessions/$sessionId/pause") {
            bearer(clientToken(allowed))
        }
        assertEquals(HttpStatusCode.OK, paused.status)
        assertTrue(paused.bodyAsText().contains("\"status\":\"PAUSED\""))

        val workspace = client.get("/client/apps/projects/workspace") { bearer(clientToken(allowed)) }
        assertEquals(HttpStatusCode.OK, workspace.status)
        assertTrue(workspace.bodyAsText().contains("Europe/Madrid"))
    }

    private fun clientToken(identity: BusinessProjectsIdentity): String = tokenService.createAccessToken(
        ServerAuthIdentity(identity.userId, "Cliente", "cliente@example.test", "CLIENT", identity.clientId),
    )

    private fun adminToken(): String = tokenService.createAccessToken(
        ServerAuthIdentity(
            "00000000-0000-0000-0000-000000000099",
            "Admin",
            "admin@example.test",
            "ADMIN",
            null,
        ),
    )

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private fun extract(body: String, field: String): String =
        requireNotNull(Regex(""""$field":"([^"]+)"""").find(body)?.groupValues?.get(1))
}
