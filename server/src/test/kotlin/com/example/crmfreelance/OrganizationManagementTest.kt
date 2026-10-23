package com.example.crmfreelance

import com.requena.supportdesk.server.application.configureSupportDeskModule
import com.requena.supportdesk.server.data.datasource.InMemorySupportDeskDataSource
import com.requena.supportdesk.server.data.repository.InMemorySupportDeskRepository
import com.requena.supportdesk.server.domain.model.ServerOrganizationInvitationSnapshot
import com.requena.supportdesk.server.domain.service.CURRENT_LEGAL_VERSION
import com.requena.supportdesk.server.security.SecurityConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Phase 2, block 2: signup behind the switch, invitations by code and company role rules. */
class OrganizationManagementTest {

    private fun ApplicationTestBuilder.app(signupEnabled: Boolean = true): InMemorySupportDeskRepository {
        val repository = InMemorySupportDeskRepository(InMemorySupportDeskDataSource())
        application {
            System.setProperty(SecurityConfig.authSecretKey, TEST_AUTH_SECRET)
            System.setProperty(SIGNUP_FLAG, signupEnabled.toString())
            configureSupportDeskModule(repositoryOverride = repository)
        }
        return repository
    }

    private fun HttpRequestBuilder.bearer(token: String) = header(HttpHeaders.Authorization, "Bearer $token")

    private fun HttpRequestBuilder.json(body: String) {
        contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
        setBody(body)
    }

    private suspend fun HttpClient.token(email: String, password: String): String {
        val response = post("/auth/login") { json("""{"email":"$email","password":"$password"}""") }
        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        return field(response.bodyAsText(), "accessToken")
    }

    private suspend fun HttpClient.signup(
        email: String = "lucia@nueva.dev",
        terms: Boolean = true,
        privacy: Boolean = true,
        password: String = "PasswordSegura123",
    ): HttpResponse = post("/auth/signup") {
        json(
            """{"name":"Lucía","email":"$email","password":"$password","organizationName":"Estudio Lucía",
               "acceptTerms":$terms,"acceptPrivacy":$privacy}""",
        )
    }

    private suspend fun HttpClient.invite(ownerToken: String, role: String): String {
        val response = post("/org/invitations") {
            bearer(ownerToken)
            json("""{"role":"$role"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        return field(response.bodyAsText(), "code")
    }

    private suspend fun HttpClient.join(code: String, email: String): HttpResponse = post("/auth/join") {
        json(
            """{"code":"$code","name":"Persona","email":"$email","password":"PasswordSegura123",
               "acceptTerms":true,"acceptPrivacy":true}""",
        )
    }

    // --- Signup ---------------------------------------------------------------------------

    @Test
    fun signupIsUnavailableWhileTheSwitchIsOff() = testApplication {
        app(signupEnabled = false)
        assertTrue(client.get("/auth/signup/status").bodyAsText().contains("\"enabled\":false"))
        assertEquals(HttpStatusCode.NotFound, client.signup().status)
    }

    @Test
    fun signupCreatesACompanyWithItsOwnerAndRecordsTheLegalAcceptance() = testApplication {
        val repository = app()
        assertTrue(client.get("/auth/signup/status").bodyAsText().contains("\"enabled\":true"))

        val response = client.signup()
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        val body = response.bodyAsText()
        assertEquals("OWNER", field(body, "organizationRole"))
        assertEquals("Estudio Lucía", field(body, "organizationName"))
        assertEquals(
            listOf("TERMS" to CURRENT_LEGAL_VERSION, "PRIVACY" to CURRENT_LEGAL_VERSION),
            repository.legalAcceptancesFor(field(body, "userId")),
        )

        // The new company starts empty: none of OryKai's data is visible.
        val token = field(body, "accessToken")
        val clients = client.get("/admin/clients") { bearer(token) }
        assertEquals(HttpStatusCode.OK, clients.status)
        assertFalse(clients.bodyAsText().contains("client-1"))
        val organization = client.get("/org") { bearer(token) }.bodyAsText()
        assertTrue(organization.contains("\"onboardingCompleted\":false"))

        // And the new owner can log in normally afterwards.
        client.token("lucia@nueva.dev", "PasswordSegura123")
    }

    @Test
    fun signupValidatesConsentPasswordAndDuplicateEmail() = testApplication {
        app()
        assertEquals(HttpStatusCode.BadRequest, client.signup(terms = false).status)
        assertEquals(HttpStatusCode.BadRequest, client.signup(privacy = false).status)
        assertEquals(HttpStatusCode.BadRequest, client.signup(password = "corta").status)
        assertEquals(HttpStatusCode.Conflict, client.signup(email = "admin@orykai.dev").status)
    }

    // --- Onboarding data ------------------------------------------------------------------

    @Test
    fun ownerCompletesOnboardingAndInvalidAnswersAreRejected() = testApplication {
        app()
        val owner = client.token("admin@orykai.dev", "UnitTestAdminPassword1")
        val updated = client.patch("/org") {
            bearer(owner)
            json(
                """{"legalForm":"autonomo","sector":"Software","teamSize":"1","hasAccountant":true,
                   "onboardingGoals":["INVOICING","WORK_TIME"],"onboardingCompleted":true,
                   "taxId":"12345678-z","country":"es"}""",
            )
        }
        assertEquals(HttpStatusCode.OK, updated.status, updated.bodyAsText())
        val body = updated.bodyAsText()
        assertTrue(body.contains("\"legalForm\":\"AUTONOMO\""))
        assertTrue(body.contains("\"onboardingCompleted\":true"))
        assertTrue(body.contains("\"taxId\":\"12345678Z\""))
        assertTrue(body.contains("\"country\":\"ES\""))
        assertTrue(body.contains("\"INVOICING\""))

        val badGoal = client.patch("/org") {
            bearer(owner)
            json("""{"onboardingGoals":["CRYPTO"]}""")
        }
        assertEquals(HttpStatusCode.BadRequest, badGoal.status)
    }

    // --- Invitations ----------------------------------------------------------------------

    @Test
    fun invitationCodesAreSingleUseAndGiveTheirRole() = testApplication {
        app()
        val owner = client.token("admin@orykai.dev", "UnitTestAdminPassword1")
        val code = client.invite(owner, "MEMBER")

        val joined = client.join(code.lowercase(), "nuevo@orykai.dev")
        assertEquals(HttpStatusCode.Created, joined.status, joined.bodyAsText())
        assertEquals("MEMBER", field(joined.bodyAsText(), "organizationRole"))
        assertEquals("org-orykai", field(joined.bodyAsText(), "organizationId"))

        // The same code cannot be used twice.
        assertEquals(HttpStatusCode.NotFound, client.join(code, "otro@orykai.dev").status)

        val members = client.get("/org/members") { bearer(owner) }.bodyAsText()
        assertTrue(members.contains("nuevo@orykai.dev"))
        val invitations = client.get("/org/invitations") { bearer(owner) }.bodyAsText()
        assertTrue(invitations.contains("\"status\":\"ACCEPTED\""))
        assertFalse(invitations.contains(code), "the plain code must never be listed again")
    }

    @Test
    fun revokedInvitationsCannotBeUsedAndOnlyStaffRolesCanBeInvited() = testApplication {
        app()
        val owner = client.token("admin@orykai.dev", "UnitTestAdminPassword1")
        val code = client.invite(owner, "ACCOUNTANT")
        val invitationId = field(client.get("/org/invitations") { bearer(owner) }.bodyAsText(), "id")
        assertEquals(HttpStatusCode.OK, client.delete("/org/invitations/$invitationId") { bearer(owner) }.status)
        assertEquals(HttpStatusCode.NotFound, client.join(code, "gestoria@orykai.dev").status)

        val ownerInvite = client.post("/org/invitations") {
            bearer(owner)
            json("""{"role":"OWNER"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, ownerInvite.status)
    }

    @Test
    fun expiredInvitationsAreReportedAsExpired() {
        val invitation = ServerOrganizationInvitationSnapshot(
            id = "i",
            role = "MEMBER",
            createdByName = "Fran",
            createdAt = "2026-01-01T00:00:00Z",
            expiresAt = "2026-01-08T00:00:00Z",
        )
        assertEquals("PENDING", invitation.status(Instant.parse("2026-01-07T23:59:59Z")))
        assertEquals("EXPIRED", invitation.status(Instant.parse("2026-01-08T00:00:01Z")))
    }

    // --- Role rules -----------------------------------------------------------------------

    @Test
    fun membersWorkButCannotManageTheCompany() = testApplication {
        app()
        val owner = client.token("admin@orykai.dev", "UnitTestAdminPassword1")
        val member = field(client.join(client.invite(owner, "MEMBER"), "miembro@orykai.dev").bodyAsText(), "accessToken")

        assertEquals(HttpStatusCode.OK, client.get("/org") { bearer(member) }.status)
        assertEquals(HttpStatusCode.OK, client.get("/admin/tasks") { bearer(member) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.patch("/org") { bearer(member); json("""{"name":"X"}""") }.status)
        assertEquals(HttpStatusCode.Forbidden, client.get("/org/members") { bearer(member) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.get("/org/invitations") { bearer(member) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.post("/org/invitations") { bearer(member); json("""{"role":"MEMBER"}""") }.status)

        // A member can create work for the company.
        val label = client.post("/admin/labels") { bearer(member); json("""{"name":"Del miembro","colorHex":"#112233"}""") }
        assertEquals(HttpStatusCode.Created, label.status, label.bodyAsText())
    }

    @Test
    fun accountantsCanReadEverythingButChangeNothing() = testApplication {
        app()
        val owner = client.token("admin@orykai.dev", "UnitTestAdminPassword1")
        val accountant = field(client.join(client.invite(owner, "ACCOUNTANT"), "gestoria@orykai.dev").bodyAsText(), "accessToken")

        assertEquals(HttpStatusCode.OK, client.get("/admin/clients") { bearer(accountant) }.status)
        assertEquals(HttpStatusCode.OK, client.get("/admin/tasks") { bearer(accountant) }.status)
        assertEquals(HttpStatusCode.OK, client.get("/tickets") { bearer(accountant) }.status)

        val writes = listOf(
            client.post("/admin/labels") { bearer(accountant); json("""{"name":"No","colorHex":"#112233"}""") },
            client.patch("/admin/tasks/task-1") { bearer(accountant); json("""{"title":"No"}""") },
            client.post("/admin/tasks") { bearer(accountant); json("""{"title":"No","labelId":"label-1"}""") },
            client.delete("/admin/tasks/task-3") { bearer(accountant) },
        )
        writes.forEach { assertEquals(HttpStatusCode.Forbidden, it.status, it.bodyAsText()) }
    }

    @Test
    fun portalUsersCannotSeeTheCompany() = testApplication {
        app()
        val portal = client.token("ana@northwind.dev", "UnitTestClientPassword1")
        assertEquals(HttpStatusCode.Forbidden, client.get("/org") { bearer(portal) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.get("/org/members") { bearer(portal) }.status)
    }

    @Test
    fun aCompanyAlwaysKeepsAnActiveOwner() = testApplication {
        app()
        val owner = client.token("admin@orykai.dev", "UnitTestAdminPassword1")

        val demoteSelf = client.patch("/org/members/user-admin") { bearer(owner); json("""{"role":"ADMIN"}""") }
        assertEquals(HttpStatusCode.Conflict, demoteSelf.status)
        assertEquals(HttpStatusCode.Conflict, client.delete("/org/members/user-admin") { bearer(owner) }.status)

        // An admin cannot touch the owner.
        val admin = field(client.join(client.invite(owner, "ADMIN"), "admin3@orykai.dev").bodyAsText(), "accessToken")
        val adminDemotesOwner = client.patch("/org/members/user-admin") { bearer(admin); json("""{"role":"MEMBER"}""") }
        assertEquals(HttpStatusCode.Forbidden, adminDemotesOwner.status)
        assertEquals(HttpStatusCode.Forbidden, client.delete("/org/members/user-admin") { bearer(admin) }.status)
    }

    @Test
    fun deactivatedMembersCanNoLongerLogIn() = testApplication {
        app()
        val owner = client.token("admin@orykai.dev", "UnitTestAdminPassword1")
        val joined = client.join(client.invite(owner, "MEMBER"), "temporal@orykai.dev").bodyAsText()
        val memberId = field(joined, "userId")

        assertEquals(HttpStatusCode.OK, client.delete("/org/members/$memberId") { bearer(owner) }.status)
        val login = client.post("/auth/login") { json("""{"email":"temporal@orykai.dev","password":"PasswordSegura123"}""") }
        assertEquals(HttpStatusCode.Unauthorized, login.status)
        val refresh = client.post("/auth/refresh") { json("""{"refreshToken":"${field(joined, "refreshToken")}"}""") }
        assertEquals(HttpStatusCode.Unauthorized, refresh.status)
    }

    // A member of another company cannot be managed from here.
    @Test
    fun membersOfOtherCompaniesAreNotFound() = testApplication {
        app()
        val owner = client.token("admin@orykai.dev", "UnitTestAdminPassword1")
        val foreign = client.patch("/org/members/user-admin-2") { bearer(owner); json("""{"role":"MEMBER"}""") }
        assertEquals(HttpStatusCode.NotFound, foreign.status)
        assertEquals(HttpStatusCode.NotFound, client.delete("/org/members/user-admin-2") { bearer(owner) }.status)
    }

    private fun field(body: String, name: String): String =
        requireNotNull(Regex(""""$name":"([^"]+)"""").find(body)?.groupValues?.get(1)) {
            "Missing field $name in response: $body"
        }

    private companion object {
        const val TEST_AUTH_SECRET = "supportdesk-test-secret-1234567890"
        const val SIGNUP_FLAG = "SUPPORTDESK_SIGNUP_ENABLED"
    }
}
