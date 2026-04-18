package com.example.crmfreelance

import com.requena.supportdesk.server.application.configureSupportDeskModule
import com.requena.supportdesk.server.data.datasource.InMemorySupportDeskDataSource
import com.requena.supportdesk.server.data.repository.InMemorySupportDeskRepository
import com.requena.supportdesk.server.utils.ADMIN_USER_ID_HEADER
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    private fun HttpRequestBuilder.asAdmin(ownerId: String = "user-admin") {
        header(ADMIN_USER_ID_HEADER, ownerId)
    }

    private fun io.ktor.server.application.Application.testModule() {
        configureSupportDeskModule(
            repositoryOverride = InMemorySupportDeskRepository(InMemorySupportDeskDataSource()),
        )
    }

    @Test
    fun testRoot() = testApplication {
        application {
            testModule()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("requenadesk-server"))
    }

    @Test
    fun testTicketsRoute() = testApplication {
        application { testModule() }
        val response = client.get("/tickets")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("SD-1001"))
        assertTrue(response.bodyAsText().contains("waitingOn"))
    }

    @Test
    fun testDashboardRoute() = testApplication {
        application { testModule() }
        val response = client.get("/admin/dashboard") {
            asAdmin()
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("openTickets"))
    }

    @Test
    fun testLoginRouteParsesJsonBody() = testApplication {
        application { testModule() }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@requenadesk.dev","password":"Admin1requena"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("admin@requenadesk.dev"))
        assertTrue(body.contains("refreshToken"))
    }

    @Test
    fun testClientsRouteIncludesNewFields() = testApplication {
        application { testModule() }
        val response = client.get("/admin/clients") {
            asAdmin()
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("productName"))
        assertTrue(response.bodyAsText().contains("serviceTier"))
    }

    @Test
    fun testDeleteTaskRemovesTaskFromList() = testApplication {
        application { testModule() }

        val deleteResponse = client.delete("/admin/tasks/task-1") {
            asAdmin()
        }
        assertEquals(HttpStatusCode.OK, deleteResponse.status)

        val tasksResponse = client.get("/admin/tasks") {
            asAdmin()
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
            asAdmin()
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("cannot be deleted"))
    }

    @Test
    fun testDeleteLabelReturnsConflictWhenLabelIsInUse() = testApplication {
        application { testModule() }

        val response = client.delete("/admin/labels/label-1")

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("cannot be deleted"))
    }

    @Test
    fun testDeleteMissingTaskReturnsNotFound() = testApplication {
        application { testModule() }

        val response = client.delete("/admin/tasks/task-missing") {
            asAdmin()
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Task not found"))
    }

    @Test
    fun testCreateAndDeleteFreeLabel() = testApplication {
        application { testModule() }

        val createResponse = client.post("/admin/labels") {
            contentType(ContentType.Application.Json)
            setBody("""{"ownerAdminId":"user-admin-2","name":"Backlog","colorHex":"#445566"}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdBody = createResponse.bodyAsText()
        assertTrue(createdBody.contains("Backlog"))
        assertTrue(createdBody.contains("\"ownerAdminId\":\"user-admin-2\""))

        val idRegex = """"id":"([^"]+)"""".toRegex()
        val labelId = requireNotNull(idRegex.find(createdBody)?.groupValues?.get(1))

        val deleteResponse = client.delete("/admin/labels/$labelId")
        assertEquals(HttpStatusCode.OK, deleteResponse.status)

        val labelsResponse = client.get("/admin/labels")
        assertTrue(!labelsResponse.bodyAsText().contains(labelId))
    }

    @Test
    fun testCreateAndDeleteFreeClient() = testApplication {
        application { testModule() }

        val createResponse = client.post("/admin/clients") {
            asAdmin()
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
        val idRegex = """"id":"([^"]+)"""".toRegex()
        val clientId = requireNotNull(idRegex.find(createdBody)?.groupValues?.get(1))

        val deleteResponse = client.delete("/admin/clients/$clientId") {
            asAdmin()
        }
        assertEquals(HttpStatusCode.OK, deleteResponse.status)

        val clientsResponse = client.get("/admin/clients") {
            asAdmin()
        }
        assertTrue(!clientsResponse.bodyAsText().contains(clientId))
    }

    @Test
    fun testCreateTaskStoresFutureDueDate() = testApplication {
        application { testModule() }
        val futureDate = LocalDate.now().plusDays(2).toString()

        val response = client.post("/admin/tasks") {
            asAdmin()
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
            asAdmin()
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "taskId":"task-1",
                  "authorId":"user-admin",
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
            asAdmin()
            contentType(ContentType.Application.Json)
            setBody("""{"companyName":"Renamed"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Client not found"))
    }

    @Test
    fun testAdminReadRoutesRequireOwnerHeader() = testApplication {
        application { testModule() }

        val clientsResponse = client.get("/admin/clients")
        val tasksResponse = client.get("/admin/tasks")
        val timeLogsResponse = client.get("/admin/time-logs")
        val dashboardResponse = client.get("/admin/dashboard")

        assertEquals(HttpStatusCode.BadRequest, clientsResponse.status)
        assertEquals(HttpStatusCode.BadRequest, tasksResponse.status)
        assertEquals(HttpStatusCode.BadRequest, timeLogsResponse.status)
        assertEquals(HttpStatusCode.BadRequest, dashboardResponse.status)
        assertTrue(clientsResponse.bodyAsText().contains(ADMIN_USER_ID_HEADER))
    }

    @Test
    fun testAdminWriteRoutesRequireOwnerHeader() = testApplication {
        application { testModule() }
        val futureDate = LocalDate.now().plusDays(1).toString()

        val createClientResponse = client.post("/admin/clients") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "companyName":"Headerless Client",
                  "productName":"Headerless Product",
                  "contactName":"Headerless Contact",
                  "email":"headerless@example.com",
                  "accountStatus":"ACTIVE",
                  "serviceTier":"STANDARD",
                  "preferredContactChannel":"TICKET"
                }
                """.trimIndent(),
            )
        }
        val createTaskResponse = client.post("/admin/tasks") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "title":"Headerless Task",
                  "description":"Debe fallar sin owner",
                  "labelId":"label-1",
                  "dueDate":"$futureDate"
                }
                """.trimIndent(),
            )
        }
        val createTimeLogResponse = client.post("/admin/time-logs") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "taskId":"task-1",
                  "authorId":"user-admin",
                  "workDate":"${LocalDate.now()}",
                  "minutes":15,
                  "seconds":900,
                  "note":"Debe fallar sin owner",
                  "billable":true
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, createClientResponse.status)
        assertEquals(HttpStatusCode.BadRequest, createTaskResponse.status)
        assertEquals(HttpStatusCode.BadRequest, createTimeLogResponse.status)
    }

    @Test
    fun testAdminRoutesFilterByOwnerHeader() = testApplication {
        application { testModule() }

        val adminClients = client.get("/admin/clients") {
            asAdmin("user-admin")
        }.bodyAsText()
        val partnerClients = client.get("/admin/clients") {
            asAdmin("user-admin-2")
        }.bodyAsText()
        val adminTasks = client.get("/admin/tasks") {
            asAdmin("user-admin")
        }.bodyAsText()
        val partnerTasks = client.get("/admin/tasks") {
            asAdmin("user-admin-2")
        }.bodyAsText()
        val adminLogs = client.get("/admin/time-logs") {
            asAdmin("user-admin")
        }.bodyAsText()
        val partnerLogs = client.get("/admin/time-logs") {
            asAdmin("user-admin-2")
        }.bodyAsText()
        val adminDashboard = client.get("/admin/dashboard") {
            asAdmin("user-admin")
        }.bodyAsText()
        val partnerDashboard = client.get("/admin/dashboard") {
            asAdmin("user-admin-2")
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
    fun testTimeLogsRouteIncludesOwnerAdminId() = testApplication {
        application { testModule() }

        val response = client.get("/admin/time-logs") {
            asAdmin()
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"ownerAdminId\":\"user-admin\""))
    }

    @Test
    fun testLabelsRouteDoesNotFilterByOwnerHeader() = testApplication {
        application { testModule() }

        val adminResponse = client.get("/admin/labels") {
            asAdmin("user-admin")
        }
        val partnerResponse = client.get("/admin/labels") {
            asAdmin("user-admin-2")
        }

        assertEquals(HttpStatusCode.OK, adminResponse.status)
        assertEquals(HttpStatusCode.OK, partnerResponse.status)
        assertTrue(adminResponse.bodyAsText().contains("\"id\":\"label-2\""))
        assertTrue(adminResponse.bodyAsText().contains("\"ownerAdminId\":\"user-admin-2\""))
        assertTrue(partnerResponse.bodyAsText().contains("\"id\":\"label-1\""))
        assertTrue(partnerResponse.bodyAsText().contains("\"id\":\"label-2\""))
    }

}
