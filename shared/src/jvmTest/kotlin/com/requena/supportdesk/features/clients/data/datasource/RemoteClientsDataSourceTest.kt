package com.requena.supportdesk.features.clients.data.datasource

import com.requena.supportdesk.features.clients.data.dto.UpdateClientComponentsRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemoteClientsDataSourceTest {
    @Test
    fun updateComponentsUsesClientScopedAdminEndpointAndReturnsEntitlements() = runBlocking {
        var requestBody = ""
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Put, request.method)
            assertEquals("/admin/clients/client-1/components", request.url.encodedPath)
            requestBody = (request.body as TextContent).text
            respond(
                content =
                    """
                    {
                      "status": "success",
                      "path": "/admin/clients/client-1/components",
                      "data": {
                        "id": "client-1",
                        "ownerAdminId": "admin-1",
                        "companyName": "Northwind Studio",
                        "productName": "Northwind Desk",
                        "contactName": "Ana Northwind",
                        "email": "ana@northwind.dev",
                        "accountStatus": "ACTIVE",
                        "serviceTier": "PRIORITY",
                        "enabledComponents": ["SERVICE_SLA"],
                        "preferredContactChannel": "TICKET",
                        "activeTicketCount": 1,
                        "openTasksCount": 2,
                        "monthlyLoggedMinutes": 90
                      }
                    }
                    """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val client = RemoteClientsDataSource(HttpClient(engine)).updateClientComponents(
            clientId = "client-1",
            request = UpdateClientComponentsRequestDto(components = listOf("SERVICE_SLA")),
        )

        assertEquals(listOf("SERVICE_SLA"), client.enabledComponents)
        assertTrue(requestBody.contains("\"components\":[\"SERVICE_SLA\"]"))
    }
}
