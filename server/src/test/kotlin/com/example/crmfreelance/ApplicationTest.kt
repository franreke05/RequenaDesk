package com.example.crmfreelance

import com.requena.supportdesk.server.application.configureSupportDeskModule
import com.requena.supportdesk.server.data.datasource.InMemorySupportDeskDataSource
import com.requena.supportdesk.server.data.repository.InMemorySupportDeskRepository
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
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    private fun HttpRequestBuilder.bearer(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private fun io.ktor.server.application.Application.testModule() {
        System.setProperty(TEST_AUTH_SECRET_KEY, TEST_AUTH_SECRET)
        configureSupportDeskModule(
            repositoryOverride = InMemorySupportDeskRepository(InMemorySupportDeskDataSource()),
        )
    }

    private suspend fun HttpClient.accessToken(
        email: String = "admin@orykai.dev",
        password: String = "Admin1requena",
    ): String {
        val response = post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        return extractField(response.bodyAsText(), "accessToken")
    }

    @Test
    fun testRoot() = testApplication {
        application {
            testModule()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("orykai-software-server"))
    }

    @Test
    fun testTicketsRouteRequiresBearerToken() = testApplication {
        application { testModule() }

        val unauthorized = client.get("/tickets")
        assertEquals(HttpStatusCode.Unauthorized, unauthorized.status)

        val response = client.get("/tickets") {
            bearer(client.accessToken())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("SD-1001"))
        assertTrue(response.bodyAsText().contains("waitingOn"))
    }

    @Test
    fun testDashboardRouteRequiresBearerToken() = testApplication {
        application { testModule() }

        val unauthorized = client.get("/admin/dashboard")
        assertEquals(HttpStatusCode.Unauthorized, unauthorized.status)

        val response = client.get("/admin/dashboard") {
            bearer(client.accessToken())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("openTickets"))
    }

    @Test
    fun testLoginRouteParsesJsonBody() = testApplication {
        application { testModule() }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@orykai.dev","password":"Admin1requena"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("admin@orykai.dev"))
        assertTrue(body.contains("refreshToken"))
        assertTrue(body.contains("accessToken"))
    }

    @Test
    fun testLoginRouteRejectsCredentialsInQueryParams() = testApplication {
        application { testModule() }

        val response = client.post("/auth/login?email=admin@orykai.dev&password=Admin1requena")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Email and password are required"))
    }

    @Test
    fun testClientRoutesAreScopedToAuthenticatedClient() = testApplication {
        application { testModule() }
        val clientToken = client.accessToken(email = "ana@northwind.dev", password = "Client1234!")

        val forbiddenAdmin = client.get("/admin/clients") {
            bearer(clientToken)
        }
        val ownTickets = client.get("/client/tickets") {
            bearer(clientToken)
        }
        val spoofedCreate = client.post("/client/tickets") {
            bearer(clientToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "clientId":"client-2",
                  "subject":"Scope check",
                  "description":"Client cannot pick another client id.",
                  "category":"QUESTION",
                  "platform":"DESKTOP"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.Forbidden, forbiddenAdmin.status)
        assertEquals(HttpStatusCode.OK, ownTickets.status)
        assertTrue(ownTickets.bodyAsText().contains("ticket-1"))
        assertTrue(!ownTickets.bodyAsText().contains("ticket-2"))
        assertEquals(HttpStatusCode.Created, spoofedCreate.status)
        assertTrue(spoofedCreate.bodyAsText().contains("\"clientId\":\"client-1\""))
    }

    @Test
    fun testAdminTicketRoutesFilterByAuthenticatedAdmin() = testApplication {
        application { testModule() }
        val adminToken = client.accessToken()
        val partnerToken = client.accessToken(email = "admin2@orykai.dev", password = "Admin2Sanchez")

        val adminTickets = client.get("/admin/tickets") {
            bearer(adminToken)
        }.bodyAsText()
        val partnerTickets = client.get("/admin/tickets") {
            bearer(partnerToken)
        }.bodyAsText()
        val adminReadsPartnerTicket = client.get("/admin/tickets/ticket-2") {
            bearer(adminToken)
        }

        assertTrue(adminTickets.contains("\"id\":\"ticket-1\""))
        assertTrue(!adminTickets.contains("\"id\":\"ticket-2\""))
        assertTrue(partnerTickets.contains("\"id\":\"ticket-2\""))
        assertTrue(!partnerTickets.contains("\"id\":\"ticket-1\""))
        assertEquals(HttpStatusCode.NotFound, adminReadsPartnerTicket.status)
    }

    @Test
    fun testTicketMessagesAreScopedAndUseAuthenticatedAuthor() = testApplication {
        application { testModule() }
        val adminToken = client.accessToken()
        val partnerToken = client.accessToken(email = "admin2@orykai.dev", password = "Admin2Sanchez")
        val clientToken = client.accessToken(email = "ana@northwind.dev", password = "Client1234!")

        val partnerPostsAdminTicket = client.post("/admin/tickets/ticket-1/messages") {
            bearer(partnerToken)
            contentType(ContentType.Application.Json)
            setBody("""{"body":"cross tenant admin message"}""")
        }
        val clientPostsOtherTicket = client.post("/client/tickets/ticket-2/messages") {
            bearer(clientToken)
            contentType(ContentType.Application.Json)
            setBody("""{"authorId":"client-user-2","body":"cross tenant client message"}""")
        }
        val clientPostsOwnTicket = client.post("/client/tickets/ticket-1/messages") {
            bearer(clientToken)
            contentType(ContentType.Application.Json)
            setBody("""{"authorId":"client-user-2","body":"own scoped message"}""")
        }
        val ownTicket = client.get("/client/tickets/ticket-1") {
            bearer(clientToken)
        }.bodyAsText()

        assertEquals(HttpStatusCode.NotFound, partnerPostsAdminTicket.status)
        assertEquals(HttpStatusCode.NotFound, clientPostsOtherTicket.status)
        assertEquals(HttpStatusCode.OK, clientPostsOwnTicket.status)
        assertTrue(ownTicket.contains("own scoped message"))
        assertTrue(ownTicket.contains("\"authorId\":\"client-user-1\""))
        assertTrue(!ownTicket.contains("\"authorId\":\"client-user-2\""))
    }

    @Test
    fun testClientDailyTicketLimitIsEnforced() = testApplication {
        application { testModule() }
        val clientToken = client.accessToken(email = "ana@northwind.dev", password = "Client1234!")

        repeat(15) { index ->
            val response = client.post("/client/tickets") {
                bearer(clientToken)
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "subject":"Daily limit ${index + 1}",
                      "description":"Ticket within daily quota.",
                      "category":"QUESTION",
                      "platform":"DESKTOP"
                    }
                    """.trimIndent(),
                )
            }
            assertEquals(HttpStatusCode.Created, response.status)
        }

        val rejected = client.post("/client/tickets") {
            bearer(clientToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "subject":"Daily limit overflow",
                  "description":"This ticket should be rejected.",
                  "category":"QUESTION",
                  "platform":"DESKTOP"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, rejected.status)
        assertTrue(rejected.bodyAsText().contains("Daily ticket limit reached"))
    }

    @Test
    fun testDoubleCloseAcceptanceIsIdempotentAndScoped() = testApplication {
        application { testModule() }
        val adminToken = client.accessToken()
        val clientToken = client.accessToken(email = "ana@northwind.dev", password = "Client1234!")

        val adminAccept = client.post("/admin/tickets/ticket-1/accept-close") {
            bearer(adminToken)
            contentType(ContentType.Application.Json)
            setBody("""{"resolutionSummary":"Fixed"}""")
        }
        val clientAccept = client.post("/client/tickets/ticket-1/accept-close") {
            bearer(clientToken)
        }
        val clientAcceptAgain = client.post("/client/tickets/ticket-1/accept-close") {
            bearer(clientToken)
        }

        assertEquals(HttpStatusCode.OK, adminAccept.status)
        assertTrue(adminAccept.bodyAsText().contains("\"status\":\"RESOLVED\""))
        assertEquals(HttpStatusCode.OK, clientAccept.status)
        assertTrue(clientAccept.bodyAsText().contains("\"status\":\"CLOSED\""))
        assertTrue(clientAccept.bodyAsText().contains("archivedAt"))
        assertEquals(HttpStatusCode.OK, clientAcceptAgain.status)
        assertTrue(clientAcceptAgain.bodyAsText().contains("\"status\":\"CLOSED\""))
    }

    @Test
    fun testDeviceRegistrationUsesAuthenticatedUserId() = testApplication {
        application { testModule() }
        val token = client.accessToken()

        val response = client.post("/devices/register") {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"spoofed-user","token":"device-token-v2","platform":"ANDROID"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("\"userId\":\"user-admin\""))
        assertTrue(!response.bodyAsText().contains("spoofed-user"))
    }

    @Test
    fun testClientTicketCreationAddsAdminAlert() = testApplication {
        application { testModule() }
        val clientToken = client.accessToken(email = "ana@northwind.dev", password = "Client1234!")
        val adminToken = client.accessToken()

        val createResponse = client.post("/client/tickets") {
            bearer(clientToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "subject":"Alert check",
                  "description":"Admin should receive a polling alert.",
                  "category":"QUESTION",
                  "platform":"DESKTOP"
                }
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)

        val alertsResponse = client.get("/alerts") {
            bearer(adminToken)
        }
        assertEquals(HttpStatusCode.OK, alertsResponse.status)
        assertTrue(alertsResponse.bodyAsText().contains("NEW_TICKET"))
        assertTrue(alertsResponse.bodyAsText().contains("Alert check"))
    }

    @Test
    fun testClientsRouteIncludesNewFields() = testApplication {
        application { testModule() }

        val response = client.get("/admin/clients") {
            bearer(client.accessToken())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("productName"))
        assertTrue(response.bodyAsText().contains("serviceTier"))
    }

    @Test
    fun testDeleteTaskRemovesTaskFromList() = testApplication {
        application { testModule() }
        val token = client.accessToken()

        val deleteResponse = client.delete("/admin/tasks/task-1") {
            bearer(token)
        }
        assertEquals(HttpStatusCode.OK, deleteResponse.status)

        val tasksResponse = client.get("/admin/tasks") {
            bearer(token)
        }
        assertEquals(HttpStatusCode.OK, tasksResponse.status)
        val body = tasksResponse.bodyAsText()
        assertTrue(!body.contains("task-1"))
        assertTrue(body.contains("task-3"))
    }

    @Test
    fun testDeleteClientReturnsConflictWhenClientHasTickets() = testApplication {
        application { testModule() }

        val response = client.delete("/admin/clients/client-1") {
            bearer(client.accessToken())
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("cannot be deleted"))
    }

    @Test
    fun testDeleteLabelReturnsConflictWhenLabelIsInUse() = testApplication {
        application { testModule() }

        val response = client.delete("/admin/labels/label-1") {
            bearer(client.accessToken())
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("cannot be deleted"))
    }

    @Test
    fun testDeleteMissingTaskReturnsNotFound() = testApplication {
        application { testModule() }

        val response = client.delete("/admin/tasks/task-missing") {
            bearer(client.accessToken())
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Task not found"))
    }

    @Test
    fun testCreateAndDeleteFreeLabel() = testApplication {
        application { testModule() }
        val partnerToken = client.accessToken(
            email = "admin2@orykai.dev",
            password = "Admin2Sanchez",
        )

        val createResponse = client.post("/admin/labels") {
            bearer(partnerToken)
            contentType(ContentType.Application.Json)
            setBody("""{"ownerAdminId":"user-admin","name":"Backlog","colorHex":"#445566"}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdBody = createResponse.bodyAsText()
        assertTrue(createdBody.contains("Backlog"))
        assertTrue(createdBody.contains("\"ownerAdminId\":\"user-admin-2\""))

        val labelId = extractField(createdBody, "id")

        val deleteResponse = client.delete("/admin/labels/$labelId") {
            bearer(partnerToken)
        }
        assertEquals(HttpStatusCode.OK, deleteResponse.status)

        val labelsResponse = client.get("/admin/labels") {
            bearer(partnerToken)
        }
        assertTrue(!labelsResponse.bodyAsText().contains(labelId))
    }

    @Test
    fun testCreateAndDeleteFreeClient() = testApplication {
        application { testModule() }
        val token = client.accessToken()

        val createResponse = client.post("/admin/clients") {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "companyName":"Fresh Client",
                  "productName":"Fresh Product",
                  "contactName":"Fresh Contact",
                  "email":"fresh@example.com",
                  "accountStatus":"ACTIVE",
                  "serviceTier":"STANDARD",
                  "preferredContactChannel":"TICKET"
                }
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdBody = createResponse.bodyAsText()
        val clientId = extractField(createdBody, "id")

        val deleteResponse = client.delete("/admin/clients/$clientId") {
            bearer(token)
        }
        assertEquals(HttpStatusCode.OK, deleteResponse.status)

        val clientsResponse = client.get("/admin/clients") {
            bearer(token)
        }
        assertTrue(!clientsResponse.bodyAsText().contains(clientId))
    }

    @Test
    fun testCreateTaskStoresFutureDueDate() = testApplication {
        application { testModule() }
        val futureDate = LocalDate.now().plusDays(2).toString()

        val response = client.post("/admin/tasks") {
            bearer(client.accessToken())
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "title":"Future planning task",
                  "description":"Programada desde calendario",
                  "labelId":"label-1",
                  "dueDate":"$futureDate"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("\"dueDate\":\"$futureDate\""))
    }

    @Test
    fun testTimeLogsRejectPastDays() = testApplication {
        application { testModule() }
        val pastDate = LocalDate.now().minusDays(1).toString()

        val response = client.post("/admin/time-logs") {
            bearer(client.accessToken())
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "taskId":"task-1",
                  "authorId":"another-user",
                  "workDate":"$pastDate",
                  "minutes":15,
                  "seconds":900,
                  "note":"Intento invalido",
                  "billable":true
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("current day"))
    }

    @Test
    fun testPatchMissingClientReturnsNotFound() = testApplication {
        application { testModule() }

        val response = client.patch("/admin/clients/client-missing") {
            bearer(client.accessToken())
            contentType(ContentType.Application.Json)
            setBody("""{"companyName":"Renamed"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Client not found"))
    }

    @Test
    fun testProtectedRoutesRequireBearerToken() = testApplication {
        application { testModule() }

        val clientsResponse = client.get("/admin/clients")
        val tasksResponse = client.get("/admin/tasks")
        val timeLogsResponse = client.get("/admin/time-logs")
        val dashboardResponse = client.get("/admin/dashboard")
        val labelsResponse = client.get("/admin/labels")
        val attachmentsResponse = client.get("/attachments/attachment-1")
        val devicesResponse = client.post("/devices/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-admin","token":"device-token","platform":"ANDROID"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, clientsResponse.status)
        assertEquals(HttpStatusCode.Unauthorized, tasksResponse.status)
        assertEquals(HttpStatusCode.Unauthorized, timeLogsResponse.status)
        assertEquals(HttpStatusCode.Unauthorized, dashboardResponse.status)
        assertEquals(HttpStatusCode.Unauthorized, labelsResponse.status)
        assertEquals(HttpStatusCode.Unauthorized, attachmentsResponse.status)
        assertEquals(HttpStatusCode.Unauthorized, devicesResponse.status)
        assertTrue(clientsResponse.bodyAsText().contains("bearer token"))
    }

    @Test
    fun testAdminRoutesFilterByAuthenticatedAdmin() = testApplication {
        application { testModule() }
        val adminToken = client.accessToken()
        val partnerToken = client.accessToken(
            email = "admin2@orykai.dev",
            password = "Admin2Sanchez",
        )

        val adminClients = client.get("/admin/clients") {
            bearer(adminToken)
        }.bodyAsText()
        val partnerClients = client.get("/admin/clients") {
            bearer(partnerToken)
        }.bodyAsText()
        val adminTasks = client.get("/admin/tasks") {
            bearer(adminToken)
        }.bodyAsText()
        val partnerTasks = client.get("/admin/tasks") {
            bearer(partnerToken)
        }.bodyAsText()
        val adminLogs = client.get("/admin/time-logs") {
            bearer(adminToken)
        }.bodyAsText()
        val partnerLogs = client.get("/admin/time-logs") {
            bearer(partnerToken)
        }.bodyAsText()
        val adminDashboard = client.get("/admin/dashboard") {
            bearer(adminToken)
        }.bodyAsText()
        val partnerDashboard = client.get("/admin/dashboard") {
            bearer(partnerToken)
        }.bodyAsText()

        assertTrue(adminClients.contains("client-1"))
        assertTrue(!adminClients.contains("client-2"))
        assertTrue(partnerClients.contains("client-2"))
        assertTrue(!partnerClients.contains("client-1"))

        assertTrue(adminTasks.contains("task-1"))
        assertTrue(adminTasks.contains("task-3"))
        assertTrue(!adminTasks.contains("task-2"))
        assertTrue(partnerTasks.contains("task-2"))
        assertTrue(!partnerTasks.contains("task-1"))

        assertTrue(adminLogs.contains("time-log-1"))
        assertTrue(adminLogs.contains("time-log-3"))
        assertTrue(!adminLogs.contains("time-log-2"))
        assertTrue(partnerLogs.contains("time-log-2"))
        assertTrue(!partnerLogs.contains("time-log-1"))

        assertTrue(adminDashboard.contains("\"activeClients\":1"))
        assertTrue(adminDashboard.contains("task-1"))
        assertTrue(!adminDashboard.contains("task-2"))
        assertTrue(partnerDashboard.contains("\"activeClients\":1"))
        assertTrue(partnerDashboard.contains("task-2"))
        assertTrue(!partnerDashboard.contains("task-1"))
    }

    @Test
    fun testTimeLogsRouteUsesAuthenticatedAuthor() = testApplication {
        application { testModule() }
        val token = client.accessToken()

        val response = client.post("/admin/time-logs") {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "taskId":"task-1",
                  "authorId":"spoofed-user",
                  "workDate":"${LocalDate.now()}",
                  "minutes":15,
                  "seconds":900,
                  "note":"Tiempo real",
                  "billable":true
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("\"authorId\":\"user-admin\""))
    }

    @Test
    fun testLabelsRouteFiltersByAuthenticatedAdmin() = testApplication {
        application { testModule() }
        val adminToken = client.accessToken()
        val partnerToken = client.accessToken(
            email = "admin2@orykai.dev",
            password = "Admin2Sanchez",
        )

        val adminResponse = client.get("/admin/labels") {
            bearer(adminToken)
        }
        val partnerResponse = client.get("/admin/labels") {
            bearer(partnerToken)
        }

        assertEquals(HttpStatusCode.OK, adminResponse.status)
        assertEquals(HttpStatusCode.OK, partnerResponse.status)
        assertTrue(adminResponse.bodyAsText().contains("\"id\":\"label-1\""))
        assertTrue(!adminResponse.bodyAsText().contains("\"id\":\"label-2\""))
        assertTrue(partnerResponse.bodyAsText().contains("\"id\":\"label-2\""))
        assertTrue(!partnerResponse.bodyAsText().contains("\"id\":\"label-1\""))
    }

    @Test
    fun testRefreshRouteReturnsNewAccessToken() = testApplication {
        application { testModule() }

        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@orykai.dev","password":"Admin1requena"}""")
        }
        val refreshToken = extractField(loginResponse.bodyAsText(), "refreshToken")

        val refreshResponse = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$refreshToken"}""")
        }

        assertEquals(HttpStatusCode.OK, refreshResponse.status)
        val refreshedBody = refreshResponse.bodyAsText()
        assertTrue(extractField(refreshedBody, "accessToken").isNotBlank())
        assertTrue(extractField(refreshedBody, "refreshToken").isNotBlank())
    }

    private fun extractField(body: String, fieldName: String): String =
        requireNotNull(Regex(""""$fieldName":"([^"]+)"""").find(body)?.groupValues?.get(1)) {
            "Missing field $fieldName in response: $body"
        }

    private companion object {
        const val TEST_AUTH_SECRET_KEY = "SUPPORTDESK_AUTH_SECRET"
        const val TEST_AUTH_SECRET = "supportdesk-test-secret-1234567890"
    }
}
