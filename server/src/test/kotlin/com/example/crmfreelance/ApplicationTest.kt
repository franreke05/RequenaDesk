package com.example.crmfreelance

import com.requena.supportdesk.server.application.configureSupportDeskModule
import com.requena.supportdesk.server.data.datasource.InMemorySupportDeskDataSource
import com.requena.supportdesk.server.data.repository.InMemorySupportDeskRepository
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

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
        val response = client.get("/admin/dashboard")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("openTickets"))
    }

    @Test
    fun testClientsRouteIncludesNewFields() = testApplication {
        application { testModule() }
        val response = client.get("/admin/clients")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("productName"))
        assertTrue(response.bodyAsText().contains("serviceTier"))
    }

    @Test
    fun testDeleteTaskRemovesTaskFromList() = testApplication {
        application { testModule() }

        val deleteResponse = client.delete("/admin/tasks/task-2")
        assertEquals(HttpStatusCode.OK, deleteResponse.status)

        val tasksResponse = client.get("/admin/tasks")
        assertEquals(HttpStatusCode.OK, tasksResponse.status)
        val body = tasksResponse.bodyAsText()
        assertTrue(body.contains("task-1"))
        assertTrue(!body.contains("task-2"))
    }

    @Test
    fun testDeleteClientReturnsConflictWhenClientHasTickets() = testApplication {
        application { testModule() }

        val response = client.delete("/admin/clients/client-1")

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

        val response = client.delete("/admin/tasks/task-missing")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Task not found"))
    }

    @Test
    fun testCreateAndDeleteFreeLabel() = testApplication {
        application { testModule() }

        val createResponse = client.post("/admin/labels") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Backlog","colorHex":"#445566"}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdBody = createResponse.bodyAsText()
        assertTrue(createdBody.contains("Backlog"))

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

        val deleteResponse = client.delete("/admin/clients/$clientId")
        assertEquals(HttpStatusCode.OK, deleteResponse.status)

        val clientsResponse = client.get("/admin/clients")
        assertTrue(!clientsResponse.bodyAsText().contains(clientId))
    }

    @Test
    fun testPatchMissingClientReturnsNotFound() = testApplication {
        application { testModule() }

        val response = client.patch("/admin/clients/client-missing") {
            contentType(ContentType.Application.Json)
            setBody("""{"companyName":"Renamed"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Client not found"))
    }

}
