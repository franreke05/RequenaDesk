package com.example.crmfreelance

import com.requena.supportdesk.server.config.DatabaseSettings
import com.requena.supportdesk.server.application.configureSupportDeskModule
import com.requena.supportdesk.server.data.datasource.PostgresDemoBootstrapper
import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import com.requena.supportdesk.server.data.repository.PostgresSupportDeskRepository
import com.requena.supportdesk.server.business.projects.BusinessProjectsIdentity
import com.requena.supportdesk.server.business.projects.BusinessProjectsQuery
import com.requena.supportdesk.server.business.projects.PostgresBusinessProjectsStore
import com.requena.supportdesk.server.business.sales.PostgresSalesProgramStore
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerActivityType
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerStage
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPageRequest
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerActivityInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerTagInput
import com.requena.supportdesk.features.business.projects.BusinessTaskBoardPlacementInput
import com.requena.supportdesk.features.business.projects.BusinessTaskPriority
import com.requena.supportdesk.features.business.projects.BusinessTaskStatus
import com.requena.supportdesk.features.business.projects.CreateBusinessProjectInput
import com.requena.supportdesk.features.business.projects.CreateBusinessTaskInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessTaskInput
import io.ktor.client.request.get
import io.ktor.client.request.patch
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
                assertEquals(19, dataSource.migrate())
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
                    "work_sessions",
                    "work_segments",
                    "task_board_placements",
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
                    "business_sales_customer_tags",
                    "business_sales_customer_tag_links",
                    "business_sales_customer_activities",
                    "business_projects",
                    "business_tasks",
                    "business_task_labels",
                    "business_task_label_links",
                    "business_task_board_placements",
                    "business_work_sessions",
                    "business_work_segments",
                    "business_time_entries",
                    "business_project_audit_events",
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
                assertFalse(dataSource.hasTablePrivilege("anon", "task_board_placements", "SELECT"))
                assertFalse(dataSource.hasTablePrivilege("authenticated", "task_board_placements", "SELECT"))
                assertFalse(dataSource.hasTablePrivilege("anon", "business_projects", "SELECT"))
                assertFalse(dataSource.hasTablePrivilege("authenticated", "business_work_sessions", "SELECT"))

                PostgresDemoBootstrapper(dataSource).bootstrap(
                    adminPassword = ADMIN_PASSWORD,
                    clientPassword = CLIENT_PASSWORD,
                )

                val projectsIdentity = dataSource.withConnection { connection ->
                    connection.prepareStatement(
                        """
                        SELECT users.id::text AS user_id, users.client_id::text AS client_id
                        FROM users WHERE users.email='client.user@orykai.local'
                        """.trimIndent(),
                    ).use { statement ->
                        statement.executeQuery().use { result ->
                            assertTrue(result.next())
                            BusinessProjectsIdentity(result.getString("user_id"), result.getString("client_id"))
                        }
                    }
                }
                val salesStore = PostgresSalesProgramStore(dataSource)
                val persistedCustomerTag = salesStore.createCustomerTag(
                    projectsIdentity.clientId,
                    projectsIdentity.userId,
                    CreateBusinessCustomerTagInput("Piloto V15", "#445566"),
                )
                val persistedCustomer = salesStore.createCustomer(
                    projectsIdentity.clientId,
                    projectsIdentity.userId,
                    CreateBusinessCustomerInput(
                        displayName = "Cliente comercial V15",
                        stage = BusinessCustomerStage.PROSPECT,
                        tagIds = listOf(persistedCustomerTag.id),
                    ),
                )
                salesStore.createCustomerActivity(
                    projectsIdentity.clientId,
                    projectsIdentity.userId,
                    persistedCustomer.id,
                    CreateBusinessCustomerActivityInput(
                        type = BusinessCustomerActivityType.FOLLOW_UP,
                        subject = "Seguimiento V15",
                        dueAt = "2030-08-03T09:00:00Z",
                    ),
                )
                assertEquals(
                    listOf(persistedCustomer.id),
                    salesStore.customers(
                        projectsIdentity.clientId,
                        BusinessSalesPageRequest(
                            stage = BusinessCustomerStage.PROSPECT,
                            tagId = persistedCustomerTag.id,
                        ),
                    ).items.map { it.id },
                )
                val projectsStore = PostgresBusinessProjectsStore(dataSource)
                val persistedProject = projectsStore.createProject(
                    projectsIdentity,
                    CreateBusinessProjectInput(
                        customerId = persistedCustomer.id,
                        name = "PostgreSQL V15 project",
                    ),
                )
                val persistedTask = projectsStore.createTask(
                    projectsIdentity,
                    CreateBusinessTaskInput(projectId = persistedProject.id, title = "PostgreSQL V15 task"),
                )
                val businessBoardDate = LocalDate.now(java.time.ZoneId.of("Europe/Madrid")).plusDays(1).toString()
                projectsStore.placeTask(
                    projectsIdentity,
                    persistedTask.id,
                    BusinessTaskBoardPlacementInput(businessBoardDate, 0.35, 0.65),
                )
                assertEquals(
                    businessBoardDate,
                    projectsStore.workspace(projectsIdentity, BusinessProjectsQuery()).board.placements.single().boardDate,
                )
                assertEquals(
                    listOf(persistedCustomer.id),
                    projectsStore.workspace(projectsIdentity, BusinessProjectsQuery()).customers.map { it.id },
                )
                val businessSession = projectsStore.startWorkSession(projectsIdentity, persistedTask.id)
                assertEquals("ACTIVE", businessSession.status.name)
                assertEquals("PAUSED", projectsStore.pauseWorkSession(projectsIdentity, businessSession.id).status.name)
                assertEquals("ACTIVE", projectsStore.resumeWorkSession(projectsIdentity, businessSession.id).status.name)
                assertEquals("FINISHED", projectsStore.stopWorkSession(projectsIdentity, businessSession.id).status.name)
                projectsStore.updateTask(
                    projectsIdentity,
                    persistedTask.id,
                    UpdateBusinessTaskInput(
                        projectId = persistedProject.id,
                        title = persistedTask.title,
                        priority = BusinessTaskPriority.NORMAL,
                        status = BusinessTaskStatus.DONE,
                        expectedVersion = persistedTask.version,
                    ),
                )
                assertTrue(projectsStore.workspace(projectsIdentity, BusinessProjectsQuery()).board.placements.isEmpty())

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
                        "/admin/task-board",
                        "/admin/time-logs",
                        "/admin/dashboard",
                    ).forEach { path ->
                        val response = client.get(path) {
                            header(HttpHeaders.Authorization, "Bearer $accessToken")
                        }
                        assertEquals(HttpStatusCode.OK, response.status, "GET $path failed")
                    }

                    val tasksBody = client.get("/admin/tasks") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                    }.bodyAsText()
                    assertTrue(tasksBody.contains("\"createdByUserId\""))
                    assertTrue(tasksBody.contains("\"createdByUserName\":\"Demo Client User\""))

                    val boardDate = LocalDate.now(java.time.ZoneId.of("Europe/Madrid")).plusDays(2)
                    val placementResponse = client.put(
                        "/admin/task-board/placements/66666666-6666-6666-6666-666666666666",
                    ) {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody("""{"boardDate":"$boardDate","positionX":0.25,"positionY":0.75}""")
                    }
                    assertEquals(HttpStatusCode.OK, placementResponse.status)
                    val persistedBoard = client.get("/admin/task-board") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                    }.bodyAsText()
                    assertTrue(persistedBoard.contains("\"boardDate\":\"$boardDate\""))
                    assertTrue(persistedBoard.contains("\"positionX\":0.25"))

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
                              "preferredContactChannel":"TICKET",
                              "billing":{
                                "legalName":"Integracion Ficticia S.L.",
                                "taxId":"b-0000000-0",
                                "addressLine":"Calle Inventada 1",
                                "postalCode":"00000",
                                "city":"Villa Prueba",
                                "province":"Provincia Prueba",
                                "country":"España",
                                "email":"facturas-integracion@example.com"
                              }
                            }
                            """.trimIndent(),
                        )
                    }
                    assertEquals(HttpStatusCode.Created, clientResponse.status)
                    val createdClientBody = clientResponse.bodyAsText()
                    val clientId = extractField(createdClientBody, "id")
                    assertTrue(createdClientBody.contains("\"taxId\":\"B00000000\""), createdClientBody)
                    assertTrue(createdClientBody.contains("\"legalName\":\"Integracion Ficticia S.L.\""), createdClientBody)

                    val billingEditResponse = client.patch("/admin/clients/$clientId") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {"billing":{"legalName":"Integracion Editada S.L.","taxId":"12345678Z",
                              "addressLine":"Avenida Ficticia 2","postalCode":"00001","city":"Otra Villa","email":""}}
                            """.trimIndent(),
                        )
                    }
                    assertEquals(HttpStatusCode.OK, billingEditResponse.status)
                    assertTrue(billingEditResponse.bodyAsText().contains("\"legalName\":\"Integracion Editada S.L.\""))
                    val clientsAfterBillingEdit = client.get("/admin/clients") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                    }.bodyAsText()
                    assertTrue(clientsAfterBillingEdit.contains("\"taxId\":\"12345678Z\""), clientsAfterBillingEdit)
                    assertTrue(clientsAfterBillingEdit.contains("\"city\":\"Otra Villa\""), clientsAfterBillingEdit)
                    assertFalse(clientsAfterBillingEdit.contains("facturas-integracion@example.com"))
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
                        setBody("""{"productKeys":["BUSINESS_PROJECTS"],"customerNote":"Integration program request"}""")
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

                    val startSessionResponse = client.post("/admin/work-sessions") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody("""{"taskId":"$taskId"}""")
                    }
                    assertEquals(HttpStatusCode.Created, startSessionResponse.status)
                    val sessionId = extractField(startSessionResponse.bodyAsText(), "id")
                    assertTrue(startSessionResponse.bodyAsText().contains("\"status\":\"ACTIVE\""))

                    val secondStartAttempt = client.post("/admin/work-sessions") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody("""{"taskId":"$taskId"}""")
                    }
                    assertEquals(HttpStatusCode.Conflict, secondStartAttempt.status)

                    val pauseResponse = client.post("/admin/work-sessions/$sessionId/pause") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                    assertEquals(HttpStatusCode.OK, pauseResponse.status)
                    assertTrue(pauseResponse.bodyAsText().contains("\"status\":\"PAUSED\""))

                    val resumeResponse = client.post("/admin/work-sessions/$sessionId/resume") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                    assertEquals(HttpStatusCode.OK, resumeResponse.status)
                    assertTrue(resumeResponse.bodyAsText().contains("\"status\":\"ACTIVE\""))

                    val checkpointResponse = client.post("/admin/work-sessions/$sessionId/checkpoint") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                    assertEquals(HttpStatusCode.OK, checkpointResponse.status)

                    val stopResponse = client.post("/admin/work-sessions/$sessionId/stop") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                    assertEquals(HttpStatusCode.OK, stopResponse.status)
                    assertTrue(stopResponse.bodyAsText().contains("\"status\":\"FINISHED\""))

                    // A second stop must not be silently re-applied (idempotency of the terminal transition).
                    val secondStopAttempt = client.post("/admin/work-sessions/$sessionId/stop") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                    assertEquals(HttpStatusCode.Conflict, secondStopAttempt.status)

                    val timeLogsResponse = client.get("/admin/time-logs?taskId=$taskId") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                    assertEquals(HttpStatusCode.OK, timeLogsResponse.status)
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
