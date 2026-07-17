package com.example.crmfreelance

import com.requena.supportdesk.server.config.DatabaseSettings
import com.requena.supportdesk.server.application.configureSupportDeskModule
import com.requena.supportdesk.server.data.datasource.PostgresDemoBootstrapper
import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import com.requena.supportdesk.server.data.repository.PostgresSupportDeskRepository
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PostgresMigrationIntegrationTest {
    @Test
    fun migrationsBootstrapAndLoginWorkOnRealPostgres() {
        EmbeddedPostgres.start().use { postgres ->
            postgres.postgresDatabase.connection.use { connection ->
                connection.createStatement().use { it.execute("CREATE TABLE legacy_schema_marker (id integer)") }
                connection.createStatement().use {
                    it.execute("CREATE ROLE anon NOLOGIN")
                    it.execute("CREATE ROLE authenticated NOLOGIN")
                }
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
                assertEquals(12, dataSource.migrate())
                assertTrue(dataSource.isReady())
                listOf(
                    "clients",
                    "users",
                    "refresh_tokens",
                    "tickets",
                    "ticket_messages",
                    "internal_comments",
                    "attachments",
                    "ticket_events",
                    "notification_devices",
                    "task_labels",
                    "tasks",
                    "time_logs",
                    "client_component_entitlements",
                    "client_contacts",
                    "client_activities",
                    "product_catalog",
                    "client_product_subscriptions",
                    "client_program_requests",
                    "client_subscription_events",
                    "business_counterparties",
                    "business_issuer_profiles",
                    "business_finance_categories",
                    "business_sales_documents",
                    "business_sales_document_lines",
                    "business_sales_document_events",
                    "business_finance_entries",
                    "business_finance_entry_events",
                    "business_file_attachments",
                    "business_finance_entry_attachments",
                    "business_booking_services",
                    "business_booking_resources",
                    "business_availability_rules",
                    "business_availability_exceptions",
                    "business_appointments",
                    "business_document_folders",
                    "business_documents",
                    "business_document_versions",
                    "business_document_confirmation_requests",
                    "business_document_confirmations",
                    "business_operations_audit_events",
                    "business_sales_customers",
                    "business_sales_contacts",
                    "business_catalog_items",
                    "business_stock_movements",
                    "business_sales_document_counters",
                    "business_sales_quotes",
                    "business_sales_quote_lines",
                    "business_sales_sales",
                    "business_sales_sale_lines",
                    "business_sales_audit_events",
                ).forEach { table ->
                    assertTrue(dataSource.hasRowLevelSecurity(table), "RLS is not enabled for $table")
                }
                assertFalse(dataSource.hasTablePrivilege("anon", "clients", "SELECT"))
                assertFalse(dataSource.hasTablePrivilege("authenticated", "clients", "SELECT"))
                assertFalse(dataSource.hasTablePrivilege("anon", "product_catalog", "SELECT"))
                assertFalse(dataSource.hasTablePrivilege("authenticated", "client_program_requests", "SELECT"))
                assertFalse(dataSource.hasTablePrivilege("anon", "business_finance_entries", "SELECT"))
                assertFalse(dataSource.hasTablePrivilege("authenticated", "business_appointments", "SELECT"))
                assertFalse(dataSource.hasTablePrivilege("anon", "business_sales_quotes", "SELECT"))

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
                    val createdClientBody = clientResponse.bodyAsText()
                    val clientId = extractField(createdClientBody, "id")
                    val initialAccessCode = extractField(createdClientBody, "generatedAccessCode")
                    assertTrue(initialAccessCode.startsWith("SBS-"))
                    assertEquals(HttpStatusCode.OK, client.post("/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"email":"integration@example.com","password":"$initialAccessCode"}""")
                    }.status)

                    val regeneratedCredentialsResponse = client.post("/admin/clients/$clientId/credentials/regenerate") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                    assertEquals(HttpStatusCode.OK, regeneratedCredentialsResponse.status)
                    val regeneratedAccessCode = extractField(regeneratedCredentialsResponse.bodyAsText(), "accessCode")
                    assertTrue(regeneratedAccessCode != initialAccessCode)
                    assertEquals(HttpStatusCode.Unauthorized, client.post("/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"email":"integration@example.com","password":"$initialAccessCode"}""")
                    }.status)

                    val componentsResponse = client.put("/admin/clients/$clientId/components") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody("""{"components":["SERVICE_SLA"]}""")
                    }
                    assertEquals(HttpStatusCode.OK, componentsResponse.status)
                    assertTrue(componentsResponse.bodyAsText().contains("\"SERVICE_SLA\""))

                    val portalLogin = client.post("/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"email":"integration@example.com","password":"$regeneratedAccessCode"}""")
                    }
                    assertEquals(HttpStatusCode.OK, portalLogin.status)
                    val portalAccessToken = extractField(portalLogin.bodyAsText(), "accessToken")
                    val programRequest = client.post("/client/program-requests") {
                        header(HttpHeaders.Authorization, "Bearer $portalAccessToken")
                        contentType(ContentType.Application.Json)
                        setBody("""{"productKeys":["BUSINESS_QUOTES"],"customerNote":"Integration program request"}""")
                    }
                    assertEquals(HttpStatusCode.Created, programRequest.status)
                    val programRequestId = extractField(programRequest.bodyAsText(), "id")

                    val programApproval = client.post("/admin/program-requests/$programRequestId/approve") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody("""{"monthlyPriceCents":0,"adminNote":"Integration approval"}""")
                    }
                    assertEquals(HttpStatusCode.OK, programApproval.status)
                    assertTrue(programApproval.bodyAsText().contains("APPROVED"))

                    val billingPreview = client.get(
                        "/admin/clients/$clientId/billing-preview?period=${java.time.YearMonth.now()}",
                    ) {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                    assertEquals(HttpStatusCode.OK, billingPreview.status)
                    assertTrue(billingPreview.bodyAsText().contains("\"totalMonthlyPriceCents\":0"))

                    val contactResponse = client.post("/admin/clients/$clientId/contacts") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                              "fullName":"Integration Contact",
                              "email":"contact.integration@example.com",
                              "role":"Product owner",
                              "isPrimary":true
                            }
                            """.trimIndent(),
                        )
                    }
                    assertEquals(HttpStatusCode.Created, contactResponse.status)
                    val contactId = extractField(contactResponse.bodyAsText(), "id")

                    val activityResponse = client.post("/admin/clients/$clientId/activities") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                              "type":"FOLLOW_UP",
                              "subject":"Integration CRM follow-up",
                              "contactId":"$contactId"
                            }
                            """.trimIndent(),
                        )
                    }
                    assertEquals(HttpStatusCode.Created, activityResponse.status)
                    assertTrue(activityResponse.bodyAsText().contains("Integration CRM follow-up"))

                    val activitiesResponse = client.get("/admin/clients/$clientId/activities") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                    assertEquals(HttpStatusCode.OK, activitiesResponse.status)
                    assertTrue(activitiesResponse.bodyAsText().contains("Integration CRM follow-up"))

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

                }

                assertEquals(0, dataSource.migrate())
            }
        }
    }

    private fun extractField(body: String, fieldName: String): String =
        requireNotNull(Regex(""""$fieldName":"([^"]+)"""").find(body)?.groupValues?.get(1)) {
            "Missing field $fieldName in response"
        }

    private fun PostgresSupportDeskDataSource.hasRowLevelSecurity(table: String): Boolean = withConnection { connection ->
        connection.prepareStatement("SELECT relrowsecurity FROM pg_class WHERE oid = ?::regclass").use { statement ->
            statement.setString(1, "public.$table")
            statement.executeQuery().use { result -> result.next() && result.getBoolean(1) }
        }
    }

    private fun PostgresSupportDeskDataSource.hasTablePrivilege(
        role: String,
        table: String,
        privilege: String,
    ): Boolean = withConnection { connection ->
        connection.prepareStatement("SELECT has_table_privilege(?, ?::regclass, ?)").use { statement ->
            statement.setString(1, role)
            statement.setString(2, "public.$table")
            statement.setString(3, privilege)
            statement.executeQuery().use { result -> result.next() && result.getBoolean(1) }
        }
    }

    private companion object {
        const val ADMIN_PASSWORD = "IntegrationAdminPassword1"
        const val CLIENT_PASSWORD = "IntegrationClientPassword1"
        const val AUTH_SECRET = "supportdesk-integration-secret-1234567890"
    }
}
