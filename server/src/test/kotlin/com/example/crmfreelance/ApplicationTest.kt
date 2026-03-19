package com.example.crmfreelance

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("requenadesk-server"))
    }

    @Test
    fun testLoginRoute() = testApplication {
        application { module() }
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("placeholder-access-token"))
    }

    @Test
    fun testTicketsRoute() = testApplication {
        application { module() }
        val response = client.get("/tickets")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("SD-1001"))
        assertTrue(response.bodyAsText().contains("waitingOn"))
    }

    @Test
    fun testDashboardRoute() = testApplication {
        application { module() }
        val response = client.get("/admin/dashboard")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("openTickets"))
    }

    @Test
    fun testRegisterDeviceRoute() = testApplication {
        application { module() }
        val response = client.post("/devices/register") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("device-1"))
    }

    @Test
    fun testClientsRouteIncludesNewFields() = testApplication {
        application { module() }
        val response = client.get("/admin/clients")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("productName"))
        assertTrue(response.bodyAsText().contains("serviceTier"))
    }
}
