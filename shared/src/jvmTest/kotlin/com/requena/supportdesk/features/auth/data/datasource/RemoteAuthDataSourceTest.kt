package com.requena.supportdesk.features.auth.data.datasource

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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoteAuthDataSourceTest {
    @Test
    fun loginPostsJsonBodyWithoutCredentialsInUrl() = runBlocking {
        var requestBody = ""

        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/auth/login", request.url.encodedPath)
            assertFalse(request.url.parameters.contains("email"))
            assertFalse(request.url.parameters.contains("password"))

            requestBody = (request.body as TextContent).text

            respond(
                content = """
                    {
                      "status": "success",
                      "path": "/auth/login",
                      "data": {
                        "userId": "user-admin",
                        "name": "Admin Requena",
                        "email": "admin@orykai.dev",
                        "role": "ADMIN",
                        "accessToken": "access-token",
                        "refreshToken": "refresh-token"
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val session = RemoteAuthDataSource(HttpClient(engine))
            .login(email = " admin@orykai.dev ", password = "Admin1234!")

        assertEquals("admin@orykai.dev", session.email)
        assertTrue(requestBody.contains(""""email":"admin@orykai.dev""""))
        assertTrue(requestBody.contains(""""password":"Admin1234!""""))
    }
}
