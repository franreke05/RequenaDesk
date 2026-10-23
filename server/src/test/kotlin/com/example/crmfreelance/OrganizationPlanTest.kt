package com.example.crmfreelance

import com.requena.supportdesk.server.application.configureSupportDeskModule
import com.requena.supportdesk.server.data.datasource.InMemorySupportDeskDataSource
import com.requena.supportdesk.server.data.repository.InMemorySupportDeskRepository
import com.requena.supportdesk.server.domain.model.OrganizationSubscription
import com.requena.supportdesk.server.domain.model.PlanKeys
import com.requena.supportdesk.server.domain.model.SubscriptionStatus
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

/** Phase 3: company plans, trial, module gate, limits and operator approval. */
class OrganizationPlanTest {

    private fun ApplicationTestBuilder.app(): InMemorySupportDeskRepository {
        val repository = InMemorySupportDeskRepository(InMemorySupportDeskDataSource())
        application {
            System.setProperty(SecurityConfig.authSecretKey, TEST_AUTH_SECRET)
            System.setProperty("SUPPORTDESK_SIGNUP_ENABLED", "true")
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

    /** New company through signup; returns (token, organizationId). */
    private suspend fun HttpClient.newCompany(email: String = "nueva@empresa.dev"): Pair<String, String> {
        val response = post("/auth/signup") {
            json(
                """{"name":"Nueva","email":"$email","password":"PasswordSegura123","organizationName":"Nueva SL",
                   "acceptTerms":true,"acceptPrivacy":true}""",
            )
        }
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        val body = response.bodyAsText()
        return field(body, "accessToken") to field(body, "organizationId")
    }

    private suspend fun HttpClient.requestPlan(token: String, plan: String, accept: Boolean = true): HttpResponse =
        post("/org/plan/requests") { bearer(token); json("""{"planKey":"$plan","billingPeriod":"MONTHLY","acceptPrice":$accept}""") }

    @Test
    fun newCompaniesStartAProTrialAndExistingOnesAreOnBeta() = testApplication {
        app()
        val (token, _) = client.newCompany()
        val plan = client.get("/org/plan") { bearer(token) }.bodyAsText()
        assertTrue(plan.contains("\"planKey\":\"PRO\""), plan)
        assertTrue(plan.contains("\"status\":\"TRIAL\""))
        assertTrue(plan.contains("\"trialDaysLeft\":14"))
        // The catalog shows the server's prices; BETA is never offered.
        assertTrue(plan.contains("\"monthlyPriceCents\":2900"))
        assertFalse(plan.contains("\"key\":\"BETA\""))

        val owner = client.token("admin@orykai.dev", "UnitTestAdminPassword1")
        val orykai = client.get("/org/plan") { bearer(owner) }.bodyAsText()
        assertTrue(orykai.contains("\"status\":\"BETA\""))
        assertTrue(orykai.contains("\"isPlatformOperator\":true"))
    }

    @Test
    fun anEndedTrialBecomesFreeAndGatesTheModulesItLacks() = testApplication {
        val repository = app()
        val (token, organizationId) = client.newCompany()
        client.patch("/org") { bearer(token); json("""{"onboardingGoals":["SUPPORT"]}""") }
        repository.setSubscriptionForTest(
            OrganizationSubscription(organizationId, PlanKeys.PRO, SubscriptionStatus.TRIAL, trialEndsAt = Instant.now().minusSeconds(60)),
        )

        val plan = client.get("/org/plan") { bearer(token) }.bodyAsText()
        assertTrue(plan.contains("\"planKey\":\"FREE\""), plan)
        assertTrue(plan.contains("\"status\":\"TRIAL_ENDED\""))
        assertTrue(plan.contains("\"freeModule\":\"SUPPORT\""))

        // Free with SUPPORT: tickets work, tasks need a plan (402). Nothing is deleted.
        assertEquals(HttpStatusCode.OK, client.get("/tickets") { bearer(token) }.status)
        val tasks = client.get("/admin/tasks") { bearer(token) }
        assertEquals(HttpStatusCode.PaymentRequired, tasks.status)
        assertTrue(tasks.bodyAsText().contains("WORK"))
        // Core keeps working.
        assertEquals(HttpStatusCode.OK, client.get("/admin/clients") { bearer(token) }.status)
    }

    @Test
    fun paidPlansAreRequestedThenApprovedByTheOperator() = testApplication {
        app()
        val (token, organizationId) = client.newCompany()
        client.requestPlan(token, "FREE") // drop the trial: Free applies at once, no approval
        assertTrue(client.get("/org/plan") { bearer(token) }.bodyAsText().contains("\"planKey\":\"FREE\""))
        client.patch("/org/plan/free-module") { bearer(token); json("""{"module":"SUPPORT"}""") }
        assertEquals(HttpStatusCode.PaymentRequired, client.get("/admin/tasks") { bearer(token) }.status)

        // A paid plan needs the price accepted, and only one request can be pending.
        assertEquals(HttpStatusCode.BadRequest, client.requestPlan(token, "PRO", accept = false).status)
        val requested = client.requestPlan(token, "PRO")
        assertEquals(HttpStatusCode.Created, requested.status, requested.bodyAsText())
        assertTrue(requested.bodyAsText().contains("\"quotedMonthlyPriceCents\":2900"))
        assertEquals(HttpStatusCode.Conflict, client.requestPlan(token, "TEAM").status)

        // Another company is not the operator.
        val second = client.token("admin2@orykai.dev", "UnitTestAdminPassword2")
        assertEquals(HttpStatusCode.Forbidden, client.get("/platform/plan-requests") { bearer(second) }.status)

        val operator = client.token("admin@orykai.dev", "UnitTestAdminPassword1")
        val queue = client.get("/platform/plan-requests?status=REQUESTED") { bearer(operator) }.bodyAsText()
        assertTrue(queue.contains(organizationId), queue)
        val requestId = field(queue, "id")
        assertEquals(
            HttpStatusCode.Forbidden,
            client.post("/platform/plan-requests/$requestId/approve") { bearer(token); json("{}") }.status,
        )
        val approved = client.post("/platform/plan-requests/$requestId/approve") { bearer(operator); json("""{"note":"Factura 2026-0042"}""") }
        assertEquals(HttpStatusCode.OK, approved.status, approved.bodyAsText())
        assertTrue(approved.bodyAsText().contains("\"invoiceManually\":true"))

        // The company is now on Pro and the gate opens at once.
        val plan = client.get("/org/plan") { bearer(token) }.bodyAsText()
        assertTrue(plan.contains("\"planKey\":\"PRO\"") && plan.contains("\"status\":\"ACTIVE\""), plan)
        assertEquals(HttpStatusCode.OK, client.get("/admin/tasks") { bearer(token) }.status)
        // A decided request cannot be approved twice.
        assertEquals(
            HttpStatusCode.NotFound,
            client.post("/platform/plan-requests/$requestId/approve") { bearer(operator); json("{}") }.status,
        )
    }

    @Test
    fun pendingRequestsCanBeCancelledAndInvalidChoicesAreRejected() = testApplication {
        app()
        val (token, _) = client.newCompany()
        val requested = client.requestPlan(token, "AUTONOMO").bodyAsText()
        val requestId = Regex(""""pendingRequest":\{"id":"([^"]+)"""").find(requested)!!.groupValues[1]
        assertEquals(HttpStatusCode.OK, client.delete("/org/plan/requests/$requestId") { bearer(token) }.status)
        assertTrue(client.get("/org/plan") { bearer(token) }.bodyAsText().contains("\"pendingRequest\":null"))

        assertEquals(HttpStatusCode.BadRequest, client.requestPlan(token, "BETA").status)
        assertEquals(HttpStatusCode.BadRequest, client.patch("/org/plan/free-module") { bearer(token); json("""{"module":"REPORTS"}""") }.status)
    }

    @Test
    fun freeCompaniesKeepWithinTheirClientAndUserLimits() = testApplication {
        app()
        val (token, _) = client.newCompany()
        client.requestPlan(token, "FREE")

        // 1 staff user on Free: inviting a member is refused, the gestoría is always allowed.
        val member = client.post("/org/invitations") { bearer(token); json("""{"role":"MEMBER"}""") }
        assertEquals(HttpStatusCode.Conflict, member.status, member.bodyAsText())
        val accountant = client.post("/org/invitations") { bearer(token); json("""{"role":"ACCOUNTANT"}""") }
        assertEquals(HttpStatusCode.Created, accountant.status, accountant.bodyAsText())

        // 25 clients on Free; the 26th is refused.
        repeat(25) { index ->
            val created = client.post("/admin/clients") {
                bearer(token)
                json(
                    """{"companyName":"Cliente $index","productName":"Web","contactName":"C","email":"c$index@x.dev",
                       "accountStatus":"ACTIVE","serviceTier":"STANDARD","preferredContactChannel":"TICKET"}""",
                )
            }
            assertEquals(HttpStatusCode.Created, created.status, created.bodyAsText())
        }
        val extra = client.post("/admin/clients") {
            bearer(token)
            json(
                """{"companyName":"Cliente 26","productName":"Web","contactName":"C","email":"c26@x.dev",
                   "accountStatus":"ACTIVE","serviceTier":"STANDARD","preferredContactChannel":"TICKET"}""",
            )
        }
        assertEquals(HttpStatusCode.Conflict, extra.status)
        assertTrue(extra.bodyAsText().contains("Client limit"))
    }

    @Test
    fun onlyOwnersAndAdminsDecideOnPortalPrograms() = testApplication {
        app()
        val owner = client.token("admin@orykai.dev", "UnitTestAdminPassword1")
        val code = field(client.post("/org/invitations") { bearer(owner); json("""{"role":"MEMBER"}""") }.bodyAsText(), "code")
        val joined = client.post("/auth/join") {
            json("""{"code":"$code","name":"M","email":"m@orykai.dev","password":"PasswordSegura123","acceptTerms":true,"acceptPrivacy":true}""")
        }.bodyAsText()
        val member = field(joined, "accessToken")
        val approve = client.post("/admin/program-requests/any/approve") { bearer(member); json("""{"monthlyPriceCents":0}""") }
        assertEquals(HttpStatusCode.Forbidden, approve.status)
        val reject = client.post("/admin/program-requests/any/reject") { bearer(member); json("{}") }
        assertEquals(HttpStatusCode.Forbidden, reject.status)
    }

    private fun field(body: String, name: String): String =
        requireNotNull(Regex(""""$name":"([^"]+)"""").find(body)?.groupValues?.get(1)) {
            "Missing field $name in response: $body"
        }

    private companion object {
        const val TEST_AUTH_SECRET = "supportdesk-test-secret-1234567890"
    }
}
