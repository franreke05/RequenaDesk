package com.example.crmfreelance

import com.requena.supportdesk.server.application.configureSupportDeskModule
import com.requena.supportdesk.server.data.datasource.InMemorySupportDeskDataSource
import com.requena.supportdesk.server.data.repository.InMemorySupportDeskRepository
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.security.SecurityConfig
import com.requena.supportdesk.server.security.ServerAuthSettings
import com.requena.supportdesk.server.security.SupportDeskTokenService
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Company isolation (phase 2, block 1). The in-memory seed has two companies:
 * - OryKai: admin@orykai.dev (OWNER), client-1, task-1, task-3, label-1, ticket-1
 * - Empresa de Admin Sanchez: admin2@orykai.dev (OWNER), client-2, task-2, label-2, ticket-2
 */
class OrganizationIsolationTest {

    private fun io.ktor.server.application.Application.testModule() {
        System.setProperty(SecurityConfig.authSecretKey, TEST_AUTH_SECRET)
        configureSupportDeskModule(
            repositoryOverride = InMemorySupportDeskRepository(InMemorySupportDeskDataSource()),
        )
    }

    private fun HttpRequestBuilder.bearer(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private suspend fun HttpClient.login(email: String, password: String): String {
        val response = post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        return response.bodyAsText()
    }

    private suspend fun HttpClient.tokenFor(email: String, password: String): String =
        field(login(email, password), "accessToken")

    @Test
    fun loginReturnsTheCompanyOfStaffAndPortalUsers() = testApplication {
        application { testModule() }

        val staff = client.login("admin@orykai.dev", "UnitTestAdminPassword1")
        assertEquals("org-orykai", field(staff, "organizationId"))
        assertEquals("OWNER", field(staff, "organizationRole"))
        assertEquals("OryKai", field(staff, "organizationName"))

        // Portal users belong to the company of their client and have no staff role.
        val portal = client.login("ana@northwind.dev", "UnitTestClientPassword1")
        assertEquals("org-orykai", field(portal, "organizationId"))
        assertFalse(portal.contains("\"organizationRole\":\""))
    }

    @Test
    fun refreshedSessionKeepsTheCompany() = testApplication {
        application { testModule() }
        val session = client.login("admin2@orykai.dev", "UnitTestAdminPassword2")
        val refreshed = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"${field(session, "refreshToken")}"}""")
        }
        assertEquals(HttpStatusCode.OK, refreshed.status)
        assertEquals("org-2", field(refreshed.bodyAsText(), "organizationId"))
    }

    @Test
    fun tokensWithoutCompanyAreRejectedSoTheClientRefreshes() = testApplication {
        application { testModule() }
        // Same secret, issuer and audience as the server, but minted like before V17.
        val legacyToken = SupportDeskTokenService(
            ServerAuthSettings(
                secret = TEST_AUTH_SECRET,
                issuer = SecurityConfig.defaultIssuer,
                audience = SecurityConfig.defaultAudience,
                accessTokenLifetimeMinutes = 30,
                refreshTokenLifetimeDays = 1,
            ),
        ).createAccessToken(ServerAuthIdentity("user-admin", "Admin Requena", "admin@orykai.dev", "ADMIN"))

        val response = client.get("/admin/clients") { bearer(legacyToken) }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun aCompanyNeverSeesAnotherCompanysData() = testApplication {
        application { testModule() }
        val second = client.tokenFor("admin2@orykai.dev", "UnitTestAdminPassword2")

        val reads = mapOf(
            "/admin/clients" to listOf("client-1", "Northwind"),
            "/admin/tasks" to listOf("task-1", "task-3"),
            "/admin/labels" to listOf("label-1"),
            "/admin/time-logs" to listOf("time-log-1", "time-log-3"),
            "/admin/task-board" to listOf("task-1", "task-3"),
            "/tickets" to listOf("ticket-1"),
        )
        reads.forEach { (path, foreignIds) ->
            val response = client.get(path) { bearer(second) }
            assertEquals(HttpStatusCode.OK, response.status, "GET $path")
            val body = response.bodyAsText()
            foreignIds.forEach { id -> assertFalse(body.contains("\"$id\""), "GET $path leaked $id: $body") }
        }

        // Its own data is still there.
        val ownTasks = client.get("/admin/tasks") { bearer(second) }.bodyAsText()
        assertTrue(ownTasks.contains("\"task-2\""))

        val foreignTicket = client.get("/tickets/ticket-1") { bearer(second) }
        assertEquals(HttpStatusCode.NotFound, foreignTicket.status)
    }

    @Test
    fun aCompanyCannotChangeAnotherCompanysData() = testApplication {
        application { testModule() }
        val second = client.tokenFor("admin2@orykai.dev", "UnitTestAdminPassword2")

        val patchClient = client.patch("/admin/clients/client-1") {
            bearer(second)
            contentType(ContentType.Application.Json)
            setBody("""{"companyName":"Hijacked"}""")
        }
        assertEquals(HttpStatusCode.NotFound, patchClient.status)

        val patchTask = client.patch("/admin/tasks/task-1") {
            bearer(second)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Hijacked"}""")
        }
        assertEquals(HttpStatusCode.NotFound, patchTask.status)

        val patchLabel = client.patch("/admin/labels/label-1") {
            bearer(second)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Hijacked"}""")
        }
        assertEquals(HttpStatusCode.NotFound, patchLabel.status)

        val deleteTask = client.delete("/admin/tasks/task-1") { bearer(second) }
        assertEquals(HttpStatusCode.NotFound, deleteTask.status)

        // Nothing changed for the real owner.
        val owner = client.tokenFor("admin@orykai.dev", "UnitTestAdminPassword1")
        val ownerTasks = client.get("/admin/tasks") { bearer(owner) }.bodyAsText()
        assertTrue(ownerTasks.contains("\"task-1\""))
        assertFalse(ownerTasks.contains("Hijacked"))
    }

    @Test
    fun newRowsBelongToTheCreatorsCompany() = testApplication {
        application { testModule() }
        val second = client.tokenFor("admin2@orykai.dev", "UnitTestAdminPassword2")
        val created = client.post("/admin/labels") {
            bearer(second)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Solo empresa 2","colorHex":"#445566"}""")
        }
        assertEquals(HttpStatusCode.Created, created.status)

        val owner = client.tokenFor("admin@orykai.dev", "UnitTestAdminPassword1")
        val ownerLabels = client.get("/admin/labels") { bearer(owner) }.bodyAsText()
        assertFalse(ownerLabels.contains("Solo empresa 2"))
        val secondLabels = client.get("/admin/labels") { bearer(second) }.bodyAsText()
        assertTrue(secondLabels.contains("Solo empresa 2"))
    }

    private fun field(body: String, name: String): String =
        requireNotNull(Regex(""""$name":"([^"]+)"""").find(body)?.groupValues?.get(1)) {
            "Missing field $name in response: $body"
        }

    private companion object {
        const val TEST_AUTH_SECRET = "supportdesk-test-secret-1234567890"
    }
}
