package com.example.crmfreelance

import com.requena.supportdesk.server.config.DatabaseSettings
import com.requena.supportdesk.server.application.configureSupportDeskModule
import com.requena.supportdesk.server.data.datasource.PostgresDemoBootstrapper
import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import com.requena.supportdesk.server.data.repository.PostgresSupportDeskRepository
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PostgresMigrationIntegrationTest {
    @Test
    fun migrationsBootstrapAndLoginWorkOnRealPostgres() {
        EmbeddedPostgres.start().use { postgres ->
            postgres.postgresDatabase.connection.use { connection ->
                connection.createStatement().use { it.execute("CREATE TABLE legacy_schema_marker (id integer)") }
            }
            val dataSource = PostgresSupportDeskDataSource(
                DatabaseSettings(
                    jdbcUrl = postgres.getJdbcUrl("postgres", "postgres"),
                    username = "postgres",
                    password = "",
                    maximumPoolSize = 2,
                ),
            )

            dataSource.use {
                assertEquals(2, dataSource.migrate())
                assertTrue(dataSource.isReady())

                PostgresDemoBootstrapper(dataSource).bootstrap(
                    adminPassword = ADMIN_PASSWORD,
                    clientPassword = CLIENT_PASSWORD,
                )

                val repository = PostgresSupportDeskRepository(dataSource)
                val identity = repository.authenticate("admin@orykai.dev", ADMIN_PASSWORD)
                assertNotNull(identity)
                assertEquals("ADMIN", identity.role)

                System.setProperty("SUPPORTDESK_AUTH_SECRET", AUTH_SECRET)
                testApplication {
                    application {
                        configureSupportDeskModule(repositoryOverride = repository)
                    }

                    val loginResponse = client.post("/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"email":"admin@orykai.dev","password":"$ADMIN_PASSWORD"}""")
                    }
                    assertEquals(HttpStatusCode.OK, loginResponse.status)
                    val accessToken = extractField(loginResponse.bodyAsText(), "accessToken")

                    listOf(
                        "/tickets",
                        "/admin/clients",
                        "/admin/labels",
                        "/admin/tasks",
                        "/admin/time-logs",
                        "/admin/dashboard",
                        "/admin/invoices",
                    ).forEach { path ->
                        val response = client.get(path) {
                            header(HttpHeaders.Authorization, "Bearer $accessToken")
                        }
                        assertEquals(HttpStatusCode.OK, response.status, "GET $path failed")
                    }

                    val clientResponse = client.post("/admin/clients") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                              "companyName":"Integration Client",
                              "productName":"Integration Product",
                              "contactName":"Integration Contact",
                              "email":"integration@example.com",
                              "accountStatus":"ACTIVE",
                              "serviceTier":"STANDARD",
                              "preferredContactChannel":"TICKET"
                            }
                            """.trimIndent(),
                        )
                    }
                    assertEquals(HttpStatusCode.Created, clientResponse.status)
                    val clientId = extractField(clientResponse.bodyAsText(), "id")

                    val labelResponse = client.post("/admin/labels") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody("""{"name":"Integration","colorHex":"#445566"}""")
                    }
                    assertEquals(HttpStatusCode.Created, labelResponse.status)
                    val labelId = extractField(labelResponse.bodyAsText(), "id")

                    val taskResponse = client.post("/admin/tasks") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                              "title":"Integration task",
                              "description":"PostgreSQL mutation smoke test",
                              "clientId":"$clientId",
                              "labelId":"$labelId",
                              "dueDate":"${LocalDate.now().plusDays(1)}"
                            }
                            """.trimIndent(),
                        )
                    }
                    assertEquals(HttpStatusCode.Created, taskResponse.status)
                    val taskId = extractField(taskResponse.bodyAsText(), "id")

                    val timeLogResponse = client.post("/admin/time-logs") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                              "taskId":"$taskId",
                              "workDate":"${LocalDate.now()}",
                              "minutes":20,
                              "seconds":1200,
                              "note":"Integration log",
                              "billable":true
                            }
                            """.trimIndent(),
                        )
                    }
                    assertEquals(HttpStatusCode.Created, timeLogResponse.status)

                    val invoiceResponse = client.post("/admin/invoices") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                              "clientId":"$clientId",
                              "issuedAt":"${LocalDate.now()}",
                              "taxPercent":21.0,
                              "items":[
                                {"description":"Integration service","quantity":1.0,"unitPrice":100.0,"sortOrder":0}
                              ]
                            }
                            """.trimIndent(),
                        )
                    }
                    assertEquals(HttpStatusCode.Created, invoiceResponse.status)
                }

                assertEquals(0, dataSource.migrate())
            }
        }
    }

    private fun extractField(body: String, fieldName: String): String =
        requireNotNull(Regex(""""$fieldName":"([^"]+)"""").find(body)?.groupValues?.get(1)) {
            "Missing field $fieldName in response"
        }

    private companion object {
        const val ADMIN_PASSWORD = "IntegrationAdminPassword1"
        const val CLIENT_PASSWORD = "IntegrationClientPassword1"
        const val AUTH_SECRET = "supportdesk-integration-secret-1234567890"
    }
}
