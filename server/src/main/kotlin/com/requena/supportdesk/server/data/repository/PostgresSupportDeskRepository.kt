package com.requena.supportdesk.server.data.repository

import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.CreateClientProgramRequestsRequest
import com.requena.supportdesk.server.domain.model.CreateClientActivityRequest
import com.requena.supportdesk.server.domain.model.CreateClientContactRequest
import com.requena.supportdesk.server.domain.model.CreateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.CreateTaskRequest
import com.requena.supportdesk.server.domain.model.CreateTicketMessageRequest
import com.requena.supportdesk.server.domain.model.CreateTicketRequest
import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.model.StartWorkSessionRequest
import com.requena.supportdesk.server.domain.model.ServerAttachmentCreated
import com.requena.supportdesk.server.domain.model.ServerAttachmentSnapshot
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.model.ServerClientSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientAccessCredentials
import com.requena.supportdesk.server.domain.model.ServerClientActivitySnapshot
import com.requena.supportdesk.server.domain.model.ServerClientContactSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientProvisioning
import com.requena.supportdesk.server.domain.model.ServerClientProgramsSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientProgramRequestSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientProductSubscriptionSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientBillingPreviewSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientBilling
import com.requena.supportdesk.server.domain.model.ClientBillingRequest
import com.requena.supportdesk.server.domain.model.ServerBillingPreviewLineSnapshot
import com.requena.supportdesk.server.domain.model.ServerProductCatalogSnapshot
import com.requena.supportdesk.server.domain.model.ServerConflictException
import com.requena.supportdesk.server.domain.model.ServerDailyMinutesSnapshot
import com.requena.supportdesk.server.domain.model.ServerDashboardSnapshot
import com.requena.supportdesk.server.domain.model.ServerDeviceRegistration
import com.requena.supportdesk.server.domain.model.ServerNotFoundException
import com.requena.supportdesk.server.domain.model.ServerValidationException
import com.requena.supportdesk.server.domain.model.ServerTaskLabelSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskBoardPlacementSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskBoardSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskSnapshot
import com.requena.supportdesk.server.domain.model.ServerTicketFieldUpdate
import com.requena.supportdesk.server.domain.model.ServerTicketMessageCreated
import com.requena.supportdesk.server.domain.model.ServerTicketSnapshot
import com.requena.supportdesk.server.domain.model.ServerTicketMessageSnapshot
import com.requena.supportdesk.server.domain.model.ServerInternalCommentSnapshot
import com.requena.supportdesk.server.domain.model.ServerTicketEventSnapshot
import com.requena.supportdesk.server.domain.model.ServerTicketAttachmentSnapshot
import com.requena.supportdesk.server.domain.model.ServerTimeLogSnapshot
import com.requena.supportdesk.server.domain.model.ServerWorkSegmentSnapshot
import com.requena.supportdesk.server.domain.model.ServerWorkSessionSnapshot
import com.requena.supportdesk.server.domain.model.UpdateClientRequest
import com.requena.supportdesk.server.domain.model.UpdateClientActivityRequest
import com.requena.supportdesk.server.domain.model.UpdateClientContactRequest
import com.requena.supportdesk.server.domain.model.UpdateClientCredentialsRequest
import com.requena.supportdesk.server.domain.model.UpdateClientComponentsRequest
import com.requena.supportdesk.server.domain.model.ApproveClientProgramRequest
import com.requena.supportdesk.server.domain.model.RejectClientProgramRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketPriorityRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketStatusRequest
import com.requena.supportdesk.server.utils.TASK_BOARD_TIME_ZONE
import com.requena.supportdesk.server.utils.madridTaskBoardDayBoundary
import com.requena.supportdesk.server.domain.model.UploadAttachmentRequest
import com.requena.supportdesk.server.domain.repository.SupportDeskRepository
import com.requena.supportdesk.server.domain.repository.OrganizationRepository
import com.requena.supportdesk.server.security.PasswordHasher
import com.requena.supportdesk.server.security.ClientAccessCodeGenerator
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.UUID
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Resolves a user's company: staff through organization_members, portal users through
// the client they belong to. Used by every query that builds a ServerAuthIdentity.
internal const val ORGANIZATION_COLUMNS =
    "org.id::text AS organization_id, om.role AS organization_role, org.name AS organization_name"
internal const val ORGANIZATION_JOINS = """
    LEFT JOIN organization_members om ON om.user_id = u.id
    LEFT JOIN clients user_client ON user_client.id = u.client_id
    LEFT JOIN organizations org ON org.id = COALESCE(om.organization_id, user_client.organization_id)
"""

// owner_admin_id must reference an active ADMIN (trigger enforce_admin_owner). New rows
// record the company's primary staff user (its OWNER) there; the tenant is organization_id.
// Expects the organization id as its single bind parameter.
internal const val ORGANIZATION_PRIMARY_ADMIN = """(
    SELECT om.user_id FROM organization_members om
    JOIN users u ON u.id = om.user_id AND u.is_active = TRUE AND u.role = 'ADMIN'
    WHERE om.organization_id = CAST(? AS uuid)
    ORDER BY (om.role = 'OWNER') DESC, om.created_at
    LIMIT 1
)"""

internal fun ResultSet.toAuthIdentity() = ServerAuthIdentity(
    userId = getString("user_id"),
    name = getString("name"),
    email = getString("email"),
    role = getString("role"),
    clientId = getString("client_id"),
    organizationId = getString("organization_id"),
    organizationRole = getString("organization_role"),
    organizationName = getString("organization_name"),
)

class PostgresSupportDeskRepository(
    private val dataSource: PostgresSupportDeskDataSource,
) : SupportDeskRepository, OrganizationRepository by PostgresOrganizationRepository(dataSource) {

    override fun identityForUser(userId: String): ServerAuthIdentity? = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT u.id::text AS user_id, u.name, u.email::text AS email, u.role, u.client_id::text AS client_id,
                   $ORGANIZATION_COLUMNS
            FROM users u
            $ORGANIZATION_JOINS
            WHERE u.id::text = ? AND u.is_active = TRUE
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, userId)
            statement.executeQuery().use { resultSet -> if (resultSet.next()) resultSet.toAuthIdentity() else null }
        }
    }

    override fun authenticate(email: String, password: String): ServerAuthIdentity? = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT u.id::text AS user_id, u.name, u.email::text AS email, u.role, u.client_id::text AS client_id, u.password_hash,
                   $ORGANIZATION_COLUMNS
            FROM users u
            $ORGANIZATION_JOINS
            WHERE u.email = ? AND u.is_active = TRUE
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, email)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next() && PasswordHasher.verify(password, resultSet.getString("password_hash"))) {
                    updateLastLogin(connection, resultSet.getString("user_id"))
                    resultSet.toAuthIdentity()
                } else {
                    null
                }
            }
        }
    }

    override fun storeRefreshToken(userId: String, refreshToken: String, expiresAt: Instant) {
        val refreshTokenHash = PasswordHasher.hashToken(refreshToken)
        dataSource.withConnection { connection ->
            connection.prepareStatement(
                """
                INSERT INTO refresh_tokens (user_id, token_hash, expires_at)
                VALUES (CAST(? AS uuid), ?, ?)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, userId)
                statement.setString(2, refreshTokenHash)
                statement.bindInstant(3, expiresAt)
                statement.executeUpdate()
            }
        }
    }

    override fun rotateRefreshToken(
        refreshToken: String,
        replacementRefreshToken: String,
        expiresAt: Instant,
    ): ServerAuthIdentity? = dataSource.withConnection { connection ->
        val refreshTokenHash = PasswordHasher.hashToken(refreshToken)
        val replacementRefreshTokenHash = PasswordHasher.hashToken(replacementRefreshToken)
        connection.autoCommit = false
        try {
            val identity = connection.prepareStatement(
                """
                SELECT u.id::text AS user_id, u.name, u.email::text AS email, u.role, u.client_id::text AS client_id,
                       $ORGANIZATION_COLUMNS
                FROM refresh_tokens rt
                JOIN users u ON u.id = rt.user_id
                $ORGANIZATION_JOINS
                WHERE rt.token_hash = ?
                  AND rt.revoked_at IS NULL
                  AND rt.expires_at > NOW()
                  AND u.is_active = TRUE
                LIMIT 1
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, refreshTokenHash)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.toAuthIdentity()
                    } else {
                        null
                    }
                }
            } ?: run {
                connection.rollback()
                return@withConnection null
            }

            connection.prepareStatement(
                "UPDATE refresh_tokens SET revoked_at = NOW() WHERE token_hash = ? AND revoked_at IS NULL",
            ).use { statement ->
                statement.setString(1, refreshTokenHash)
                statement.executeUpdate()
            }

            connection.prepareStatement(
                """
                INSERT INTO refresh_tokens (user_id, token_hash, expires_at)
                VALUES (CAST(? AS uuid), ?, ?)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, identity.userId)
                statement.setString(2, replacementRefreshTokenHash)
                statement.bindInstant(3, expiresAt)
                statement.executeUpdate()
            }

            connection.commit()
            identity
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = true
        }
    }

    override fun revokeRefreshToken(refreshToken: String): Boolean = dataSource.withConnection { connection ->
        val refreshTokenHash = PasswordHasher.hashToken(refreshToken)
        connection.prepareStatement(
            "UPDATE refresh_tokens SET revoked_at = NOW() WHERE token_hash = ? AND revoked_at IS NULL",
        ).use { statement ->
            statement.setString(1, refreshTokenHash)
            statement.executeUpdate() > 0
        }
    }

    override fun getTickets(): List<ServerTicketSnapshot> = dataSource.withConnection { connection ->
        val tickets = connection.prepareStatement(
            """
            SELECT t.id::text, t.client_id::text, t.ticket_number, t.subject, t.description, t.category,
                   t.affected_app, t.platform, t.app_version, t.steps_to_reproduce, t.client_reference,
                   t.status, t.priority, t.waiting_on, t.resolution_summary,
                   t.requester_id::text, requester.name AS requester_name, requester.email::text AS requester_email,
                   t.assignee_id::text, assignee.name AS assignee_name, assignee.email::text AS assignee_email,
                   t.created_at, t.updated_at
            FROM tickets t
            JOIN users requester ON requester.id = t.requester_id
            LEFT JOIN users assignee ON assignee.id = t.assignee_id
            ORDER BY t.updated_at DESC, t.created_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(ticketSnapshot(resultSet))
                    }
                }
            }
        }
        val ticketIds = tickets.map { it.id }
        val messagesByTicket = ticketMessagesByTicketIds(connection, ticketIds)
        val commentsByTicket = ticketInternalCommentsByTicketIds(connection, ticketIds)
        val eventsByTicket = ticketEventsByTicketIds(connection, ticketIds)
        val attachmentsByTicket = ticketAttachmentsByTicketIds(connection, ticketIds)
        tickets.map { ticket ->
            ticket.copy(
                messages = messagesByTicket[ticket.id].orEmpty(),
                internalComments = commentsByTicket[ticket.id].orEmpty(),
                events = eventsByTicket[ticket.id].orEmpty(),
                attachments = attachmentsByTicket[ticket.id].orEmpty(),
            )
        }
    }

    override fun getTicket(id: String): ServerTicketSnapshot? = dataSource.withConnection { connection ->
        val ticket = connection.prepareStatement(
            """
            SELECT t.id::text, t.client_id::text, t.ticket_number, t.subject, t.description, t.category,
                   t.affected_app, t.platform, t.app_version, t.steps_to_reproduce, t.client_reference,
                   t.status, t.priority, t.waiting_on, t.resolution_summary,
                   t.requester_id::text, requester.name AS requester_name, requester.email::text AS requester_email,
                   t.assignee_id::text, assignee.name AS assignee_name, assignee.email::text AS assignee_email,
                   t.created_at, t.updated_at
            FROM tickets t
            JOIN users requester ON requester.id = t.requester_id
            LEFT JOIN users assignee ON assignee.id = t.assignee_id
            WHERE t.id::text = ? OR t.ticket_number = ?
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, id)
            statement.setString(2, id)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) ticketSnapshot(resultSet) else null
            }
        }
        ticket?.let { hydrateTicket(connection, it) }
    }

    override fun createTicket(request: CreateTicketRequest): ServerTicketSnapshot = dataSource.withConnection { connection ->
        if (!isValidUuid(request.clientId)) {
            throw ServerValidationException("clientId must be a valid UUID")
        }
        val requesterId = request.requesterId ?: findRequesterId(connection, request.clientId)
        val affectedApp = request.affectedApp.ifBlank { findAffectedApp(connection, request.clientId) }

        connection.prepareStatement(
            """
            INSERT INTO tickets (
                client_id, requester_id, subject, description, category, affected_app,
                platform, app_version, steps_to_reproduce, client_reference, status, priority, waiting_on
            )
            VALUES (
                CAST(? AS uuid), CAST(? AS uuid), ?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', ?, 'ADMIN'
            )
            RETURNING id::text
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, request.clientId)
            statement.setString(2, requesterId)
            statement.setString(3, request.subject)
            statement.setString(4, request.description)
            statement.setString(5, request.category)
            statement.setString(6, affectedApp)
            statement.setString(7, request.platform)
            statement.setString(8, request.appVersion)
            statement.setString(9, request.stepsToReproduce)
            statement.setString(10, request.clientReference)
            statement.setString(11, request.priority)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) throw ServerValidationException("Could not create ticket with the given data")
                val ticketId = resultSet.getString("id")
                insertEvent(connection, ticketId, requesterId, "TICKET_CREATED", "Ticket created")
                findTicket(connection, ticketId) ?: throw ServerNotFoundException("Created ticket could not be loaded")
            }
        }
    }

    override fun createTicketMessage(ticketId: String, request: CreateTicketMessageRequest): ServerTicketMessageCreated =
        dataSource.withConnection { connection ->
            connection.prepareStatement(
                """
                INSERT INTO ticket_messages (ticket_id, author_id, body)
                VALUES (CAST(? AS uuid), CAST(? AS uuid), ?)
                RETURNING id::text
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, ticketId)
                statement.setString(2, request.authorId)
                statement.setString(3, request.body)
                statement.executeQuery().use { resultSet ->
                    if (!resultSet.next()) throw ServerNotFoundException("Ticket not found")
                    insertEvent(connection, ticketId, request.authorId, "MESSAGE_ADDED", "Reply added to ticket")
                    ServerTicketMessageCreated(ticketId = ticketId, messageId = resultSet.getString("id"))
                }
            }
        }

    override fun updateTicketStatus(ticketId: String, request: UpdateTicketStatusRequest): ServerTicketFieldUpdate =
        dataSource.withConnection { connection ->
            connection.prepareStatement(
                """
                UPDATE tickets
                SET status = ?,
                    waiting_on = CASE WHEN ? = 'PENDING_CLIENT' THEN 'CLIENT' ELSE 'ADMIN' END,
                    resolved_at = CASE
                        WHEN ? = 'RESOLVED' THEN COALESCE(resolved_at, NOW())
                        WHEN ? <> 'RESOLVED' THEN NULL
                        ELSE resolved_at
                    END,
                    closed_at = CASE
                        WHEN ? = 'CLOSED' THEN COALESCE(closed_at, NOW())
                        WHEN ? <> 'CLOSED' THEN NULL
                        ELSE closed_at
                    END
                WHERE id::text = ?
                RETURNING id::text, status
                """.trimIndent(),
            ).use { statement ->
                repeat(5) { statement.setString(it + 1, request.status) }
                statement.setString(6, ticketId)
                statement.executeQuery().use { resultSet ->
                    if (!resultSet.next()) throw ServerNotFoundException("Ticket not found")
                    ServerTicketFieldUpdate(
                        ticketId = resultSet.getString("id"),
                        value = resultSet.getString("status"),
                    )
                }
            }
        }

    override fun updateTicketPriority(ticketId: String, request: UpdateTicketPriorityRequest): ServerTicketFieldUpdate =
        dataSource.withConnection { connection ->
            connection.prepareStatement(
                """
                UPDATE tickets
                SET priority = ?
                WHERE id::text = ?
                RETURNING id::text, priority
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, request.priority)
                statement.setString(2, ticketId)
                statement.executeQuery().use { resultSet ->
                    if (!resultSet.next()) throw ServerNotFoundException("Ticket not found")
                    ServerTicketFieldUpdate(
                        ticketId = resultSet.getString("id"),
                        value = resultSet.getString("priority"),
                    )
                }
            }
        }

    override fun createAttachment(ticketId: String, request: UploadAttachmentRequest): ServerAttachmentCreated =
        dataSource.withConnection { connection ->
            connection.prepareStatement(
                """
                INSERT INTO attachments (
                    ticket_id, message_id, file_name, content_type, storage_key, size_bytes, uploaded_by
                )
                VALUES (
                    CASE WHEN ? IS NULL OR ? = '' THEN CAST(? AS uuid) ELSE NULL END,
                    CASE WHEN ? IS NULL OR ? = '' THEN NULL ELSE CAST(? AS uuid) END,
                    ?, ?, ?, ?, CAST(? AS uuid)
                )
                RETURNING id::text
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, request.messageId)
                statement.setString(2, request.messageId)
                statement.setString(3, ticketId)
                statement.setString(4, request.messageId)
                statement.setString(5, request.messageId)
                statement.setString(6, request.messageId)
                statement.setString(7, request.fileName)
                statement.setString(8, request.contentType)
                statement.setString(9, request.storageKey)
                statement.setLong(10, request.sizeBytes)
                statement.setString(11, request.uploadedBy)
                statement.executeQuery().use { resultSet ->
                    if (!resultSet.next()) throw ServerValidationException("Could not store attachment for the given ticket/message")
                    insertEvent(connection, ticketId, request.uploadedBy, "ATTACHMENT_ADDED", "Attachment metadata stored")
                    ServerAttachmentCreated(ticketId = ticketId, attachmentId = resultSet.getString("id"))
                }
            }
        }

    override fun getClients(organizationId: String): List<ServerClientSnapshot> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT
                c.id::text AS id,
                c.owner_admin_id::text AS owner_admin_id,
                c.company_name,
                c.product_name,
                c.contact_name,
                c.email::text AS email,
                c.account_status,
                c.service_tier,
                COALESCE(
                    ARRAY(
                        SELECT cce.component_key
                        FROM client_component_entitlements cce
                        WHERE cce.client_id = c.id
                        ORDER BY cce.component_key
                    ),
                    ARRAY[]::varchar[]
                ) AS enabled_components,
                c.preferred_contact_channel,
                (
                    SELECT COUNT(*)::integer
                    FROM tickets t
                    WHERE t.client_id = c.id
                      AND t.status NOT IN ('RESOLVED', 'CLOSED')
                ) AS active_ticket_count,
                (
                    SELECT COUNT(*)::integer
                    FROM tasks ts
                    WHERE ts.client_id = c.id
                      AND ts.completed = FALSE
                      AND (? IS NOT NULL AND ts.organization_id::text = ?)
                ) AS open_tasks_count,
                (
                    SELECT COALESCE(SUM(tl.minutes), 0)::integer
                    FROM time_logs tl
                    JOIN tasks ts ON ts.id = tl.task_id
                    WHERE tl.client_id = c.id
                      AND (? IS NOT NULL AND ts.organization_id::text = ?)
                      AND DATE_TRUNC('month', tl.work_date) = DATE_TRUNC('month', CURRENT_DATE)
                ) AS monthly_logged_minutes,
                $CLIENT_BILLING_COLUMNS
            FROM clients c
            WHERE (? IS NOT NULL AND c.organization_id::text = ?)
            ORDER BY c.company_name ASC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.setString(2, organizationId)
            statement.setString(3, organizationId)
            statement.setString(4, organizationId)
            statement.setString(5, organizationId)
            statement.setString(6, organizationId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            ServerClientSnapshot(
                                id = resultSet.getString("id"),
                                ownerAdminId = resultSet.getString("owner_admin_id"),
                                companyName = resultSet.getString("company_name"),
                                productName = resultSet.getString("product_name"),
                                contactName = resultSet.getString("contact_name"),
                                email = resultSet.getString("email"),
                                accountStatus = resultSet.getString("account_status"),
                                serviceTier = resultSet.getString("service_tier"),
                                enabledComponents = resultSet.textArray("enabled_components"),
                                preferredContactChannel = resultSet.getString("preferred_contact_channel"),
                                activeTicketCount = resultSet.getInt("active_ticket_count"),
                                openTasksCount = resultSet.getInt("open_tasks_count"),
                                monthlyLoggedMinutes = resultSet.getInt("monthly_logged_minutes"),
                                billing = resultSet.clientBilling(),
                            ),
                        )
                    }
                }
            }
        }
    }

    override fun createClient(request: CreateClientRequest, organizationId: String): ServerClientProvisioning = dataSource.withConnection { connection ->
        val resolvedOrganizationId = requireNotNull(organizationId) { "organizationId is required" }
        val previousAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            val email = request.email.trim()
            if (isEmailAssignedToAnotherUser(connection, email, currentUserId = null)) {
                throw ServerConflictException("Email is already used by another account")
            }
            val client = connection.prepareStatement(
                """
                INSERT INTO clients (
                    owner_admin_id, organization_id, company_name, product_name, contact_name, email, account_status, service_tier, preferred_contact_channel
                )
                VALUES ($ORGANIZATION_PRIMARY_ADMIN, CAST(? AS uuid), ?, ?, ?, ?, ?, ?, ?)
                RETURNING id::text, owner_admin_id::text, company_name, product_name, contact_name, email::text, account_status, service_tier, preferred_contact_channel
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, resolvedOrganizationId)
                statement.setString(2, resolvedOrganizationId)
                statement.setString(3, request.companyName)
                statement.setString(4, request.productName)
                statement.setString(5, request.contactName)
                statement.setString(6, email)
                statement.setString(7, request.accountStatus)
                statement.setString(8, request.serviceTier)
                statement.setString(9, request.preferredContactChannel)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    ServerClientSnapshot(
                        id = resultSet.getString("id"),
                        ownerAdminId = resultSet.getString("owner_admin_id"),
                        companyName = resultSet.getString("company_name"),
                        productName = resultSet.getString("product_name"),
                        contactName = resultSet.getString("contact_name"),
                        email = resultSet.getString("email"),
                        accountStatus = resultSet.getString("account_status"),
                        serviceTier = resultSet.getString("service_tier"),
                        enabledComponents = emptyList(),
                        preferredContactChannel = resultSet.getString("preferred_contact_channel"),
                        activeTicketCount = 0,
                        openTasksCount = 0,
                        monthlyLoggedMinutes = 0,
                    )
                }
            }.let { created ->
                writeClientBilling(connection, created.id, request.billing)
                created.copy(billing = request.billing.toSnapshot())
            }
            val accessCode = ClientAccessCodeGenerator.generate()
            connection.prepareStatement(
                """
                INSERT INTO users (client_id, name, email, password_hash, role, is_active)
                VALUES (CAST(? AS uuid), ?, ?, ?, 'CLIENT', TRUE)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, client.id)
                statement.setString(2, client.contactName)
                statement.setString(3, email)
                statement.setString(4, PasswordHasher.hash(accessCode))
                statement.executeUpdate()
            }
            connection.commit()
            ServerClientProvisioning(
                client = client,
                credentials = ServerClientAccessCredentials(client.id, email, accessCode),
            )
        } catch (throwable: Throwable) {
            connection.rollback()
            throw throwable
        } finally {
            connection.autoCommit = previousAutoCommit
        }
    }

    override fun updateClient(
        clientId: String,
        request: UpdateClientRequest,
        organizationId: String,
    ): ServerClientSnapshot = dataSource.withConnection { connection ->
        requireClientExists(connection, clientId, organizationId)
        val linkedClientUserId = findClientUserId(connection, clientId)
        if (
            linkedClientUserId != null &&
            !request.email.isNullOrBlank() &&
            isEmailAssignedToAnotherUser(connection, request.email.trim(), linkedClientUserId)
        ) {
            throw ServerConflictException("Email is already used by another account")
        }
        val updated = connection.prepareStatement(
            """
            UPDATE clients
            SET company_name = COALESCE(NULLIF(?, ''), company_name),
                product_name = COALESCE(NULLIF(?, ''), product_name),
                contact_name = COALESCE(NULLIF(?, ''), contact_name),
                email = COALESCE(NULLIF(?, ''), email),
                account_status = COALESCE(NULLIF(?, ''), account_status),
                service_tier = COALESCE(NULLIF(?, ''), service_tier),
                preferred_contact_channel = COALESCE(NULLIF(?, ''), preferred_contact_channel)
            WHERE id::text = ?
              AND (? IS NOT NULL AND organization_id::text = ?)
            RETURNING id::text, owner_admin_id::text, company_name, product_name, contact_name, email::text, account_status, service_tier, preferred_contact_channel
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, request.companyName)
            statement.setString(2, request.productName)
            statement.setString(3, request.contactName)
            statement.setString(4, request.email)
            statement.setString(5, request.accountStatus)
            statement.setString(6, request.serviceTier)
            statement.setString(7, request.preferredContactChannel)
            statement.setString(8, clientId)
            statement.setString(9, organizationId)
            statement.setString(10, organizationId)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) {
                    throw ServerNotFoundException("Client not found")
                }
                ServerClientSnapshot(
                    id = resultSet.getString("id"),
                    ownerAdminId = resultSet.getString("owner_admin_id"),
                    companyName = resultSet.getString("company_name"),
                    productName = resultSet.getString("product_name"),
                    contactName = resultSet.getString("contact_name"),
                    email = resultSet.getString("email"),
                    accountStatus = resultSet.getString("account_status"),
                    serviceTier = resultSet.getString("service_tier"),
                    enabledComponents = emptyList(),
                    preferredContactChannel = resultSet.getString("preferred_contact_channel"),
                    activeTicketCount = 0,
                )
            }
        }
        if (linkedClientUserId != null && (!request.email.isNullOrBlank() || !request.contactName.isNullOrBlank())) {
            connection.prepareStatement(
                """
                UPDATE users
                SET name = ?, email = COALESCE(NULLIF(?, ''), email), updated_at = NOW()
                WHERE id::text = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, updated.contactName)
                statement.setString(2, request.email?.trim())
                statement.setString(3, linkedClientUserId)
                statement.executeUpdate()
            }
        }
        request.billing?.let { billing -> writeClientBilling(connection, clientId, billing) }
        updated.copy(
            enabledComponents = clientComponents(connection, clientId),
            activeTicketCount = getActiveTicketCount(connection, clientId),
            openTasksCount = getOpenTasksCount(connection, clientId, organizationId),
            monthlyLoggedMinutes = getClientMonthlyMinutes(connection, clientId, organizationId),
            billing = readClientBilling(connection, clientId),
        )
    }

    override fun updateClientCredentials(
        clientId: String,
        request: UpdateClientCredentialsRequest,
        organizationId: String,
    ) {
        dataSource.withConnection { connection ->
            requireClientExists(connection, clientId, organizationId)
            val email = request.email.trim()
            val existingClientUserId = findClientUserId(connection, clientId)
            if (isEmailAssignedToAnotherUser(connection, email, existingClientUserId)) {
                throw ServerConflictException("Email is already used by another account")
            }
            val passwordHash = PasswordHasher.hash(request.password)
            val clientUserId = if (existingClientUserId == null) {
                connection.prepareStatement(
                    """
                    INSERT INTO users (client_id, name, email, password_hash, role, is_active)
                    SELECT id, contact_name, ?, ?, 'CLIENT', TRUE
                    FROM clients
                    WHERE id::text = ?
                    RETURNING id::text AS user_id
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, email)
                    statement.setString(2, passwordHash)
                    statement.setString(3, clientId)
                    statement.executeQuery().use { resultSet ->
                        if (!resultSet.next()) {
                            throw ServerNotFoundException("Client not found")
                        }
                        resultSet.getString("user_id")
                    }
                }
            } else {
                connection.prepareStatement(
                    """
                    UPDATE users
                    SET email = ?, password_hash = ?, is_active = TRUE, updated_at = NOW()
                    WHERE id::text = ?
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, email)
                    statement.setString(2, passwordHash)
                    statement.setString(3, existingClientUserId)
                    if (statement.executeUpdate() != 1) {
                        throw ServerNotFoundException("Client not found")
                    }
                }
                existingClientUserId
            }
            connection.prepareStatement(
                "UPDATE refresh_tokens SET revoked_at = NOW() WHERE user_id::text = ? AND revoked_at IS NULL",
            ).use { statement ->
                statement.setString(1, clientUserId)
                statement.executeUpdate()
            }
        }
    }

    override fun regenerateClientCredentials(
        clientId: String,
        organizationId: String,
    ): ServerClientAccessCredentials = dataSource.withConnection { connection ->
        requireClientExists(connection, clientId, organizationId)
        val previousAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            val client = clientSnapshot(connection, clientId, organizationId)
            val existingClientUserId = findClientUserId(connection, clientId)
            if (isEmailAssignedToAnotherUser(connection, client.email, existingClientUserId)) {
                throw ServerConflictException("Email is already used by another account")
            }
            val accessCode = ClientAccessCodeGenerator.generate()
            val passwordHash = PasswordHasher.hash(accessCode)
            val clientUserId = if (existingClientUserId == null) {
                connection.prepareStatement(
                    """
                    INSERT INTO users (client_id, name, email, password_hash, role, is_active)
                    VALUES (CAST(? AS uuid), ?, ?, ?, 'CLIENT', TRUE)
                    RETURNING id::text AS user_id
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, clientId)
                    statement.setString(2, client.contactName)
                    statement.setString(3, client.email)
                    statement.setString(4, passwordHash)
                    statement.executeQuery().use { resultSet ->
                        resultSet.next()
                        resultSet.getString("user_id")
                    }
                }
            } else {
                connection.prepareStatement(
                    """
                    UPDATE users
                    SET name = ?, email = ?, password_hash = ?, is_active = TRUE, updated_at = NOW()
                    WHERE id::text = ?
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, client.contactName)
                    statement.setString(2, client.email)
                    statement.setString(3, passwordHash)
                    statement.setString(4, existingClientUserId)
                    if (statement.executeUpdate() != 1) throw ServerNotFoundException("Client not found")
                }
                existingClientUserId
            }
            revokeRefreshTokens(connection, clientUserId)
            connection.commit()
            ServerClientAccessCredentials(clientId, client.email, accessCode)
        } catch (throwable: Throwable) {
            connection.rollback()
            throw throwable
        } finally {
            connection.autoCommit = previousAutoCommit
        }
    }

    override fun updateClientComponents(
        clientId: String,
        request: UpdateClientComponentsRequest,
        organizationId: String,
    ): ServerClientSnapshot = dataSource.withConnection { connection ->
        requireClientExists(connection, clientId, organizationId)
        val components = request.components.distinct().sorted()
        val previousAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            connection.prepareStatement(
                "DELETE FROM client_component_entitlements WHERE client_id::text = ?",
            ).use { statement ->
                statement.setString(1, clientId)
                statement.executeUpdate()
            }
            if (components.isNotEmpty()) {
                connection.prepareStatement(
                    """
                    INSERT INTO client_component_entitlements (client_id, component_key)
                    VALUES (CAST(? AS uuid), ?)
                    """.trimIndent(),
                ).use { statement ->
                    components.forEach { component ->
                        statement.setString(1, clientId)
                        statement.setString(2, component)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
            }
            syncLegacyServiceSubscription(connection, clientId, components.contains("SERVICE_SLA"), organizationId)
            val snapshot = clientSnapshot(connection, clientId, organizationId)
            connection.commit()
            snapshot
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = previousAutoCommit
        }
    }

    override fun getClientPrograms(clientId: String): ServerClientProgramsSnapshot =
        dataSource.withConnection { connection ->
            requireOwnClientExists(connection, clientId)
            ServerClientProgramsSnapshot(
                catalog = productCatalog(connection),
                subscriptions = clientProductSubscriptions(connection, clientId),
                requests = clientProgramRequests(connection, clientId = clientId),
            )
        }

    override fun createClientProgramRequests(
        clientId: String,
        requestedByUserId: String,
        request: CreateClientProgramRequestsRequest,
    ): List<ServerClientProgramRequestSnapshot> = dataSource.withConnection { connection ->
        val keys = request.productKeys
            .map { it.trim().uppercase() }
            .filter(String::isNotBlank)
            .distinct()
        if (keys.isEmpty()) throw ServerValidationException("At least one program is required")
        requireActiveClientPortalUser(connection, clientId, requestedByUserId)
        val previousAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            val createdIds = keys.map { productKey ->
                val product = productCatalogItem(connection, productKey)
                    ?: throw ServerNotFoundException("Program is not available")
                if (!product.isAvailable || !product.isRequestable) {
                    throw ServerConflictException("Program is not available for requests")
                }
                if (hasActiveProductSubscription(connection, clientId, productKey)) {
                    throw ServerConflictException("Program is already active")
                }
                if (hasPendingProgramRequest(connection, clientId, productKey)) {
                    throw ServerConflictException("Program request is already pending")
                }
                connection.prepareStatement(
                    """
                    INSERT INTO client_program_requests (client_id, product_key, requested_by_user_id, customer_note)
                    VALUES (CAST(? AS uuid), ?, CAST(? AS uuid), ?)
                    ON CONFLICT (client_id, product_key) WHERE status = 'REQUESTED' DO NOTHING
                    RETURNING id::text
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, clientId)
                    statement.setString(2, productKey)
                    statement.setString(3, requestedByUserId)
                    statement.setString(4, request.customerNote.trim().takeIf(String::isNotBlank))
                    statement.executeQuery().use { resultSet ->
                        if (!resultSet.next()) throw ServerConflictException("Program request is already pending")
                        resultSet.getString(1)
                    }
                }
            }
            createdIds.forEach { requestId ->
                val created = clientProgramRequest(connection, requestId)
                    ?: throw ServerNotFoundException("Program request not found")
                recordSubscriptionEvent(
                    connection = connection,
                    clientId = created.clientId,
                    productKey = created.productKey,
                    requestId = created.id,
                    actorUserId = requestedByUserId,
                    action = "REQUESTED",
                    previousStatus = null,
                    newStatus = "REQUESTED",
                    monthlyPriceCents = null,
                )
            }
            val created = createdIds.map { requestId ->
                requireNotNull(clientProgramRequest(connection, requestId))
            }
            connection.commit()
            created
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = previousAutoCommit
        }
    }

    override fun getClientProgramRequests(
        organizationId: String,
        status: String?,
    ): List<ServerClientProgramRequestSnapshot> = dataSource.withConnection { connection ->
        clientProgramRequests(connection, organizationId = organizationId, status = status)
    }

    override fun approveClientProgramRequest(
        requestId: String,
        request: ApproveClientProgramRequest,
        reviewedByUserId: String,
        organizationId: String,
    ): ServerClientProgramRequestSnapshot = dataSource.withConnection { connection ->
        val previousAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            val pending = clientProgramRequestForOwner(connection, requestId, organizationId, lockForUpdate = true)
                ?: throw ServerNotFoundException("Program request not found")
            if (pending.status != "REQUESTED") throw ServerConflictException("Program request has already been decided")
            val previousSubscriptionStatus = clientProductSubscriptionStatus(connection, pending.clientId, pending.productKey)
            connection.prepareStatement(
                """
                UPDATE client_program_requests
                SET status = 'APPROVED',
                    admin_note = ?,
                    quoted_monthly_price_cents = ?,
                    decided_at = NOW(),
                    decided_by_user_id = CAST(? AS uuid)
                WHERE id = CAST(? AS uuid)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, request.adminNote?.trim()?.takeIf(String::isNotBlank))
                statement.setLong(2, 0L)
                statement.setString(3, reviewedByUserId)
                statement.setString(4, requestId)
                check(statement.executeUpdate() == 1) { "Program request was not updated" }
            }
            connection.prepareStatement(
                """
                INSERT INTO client_product_subscriptions (
                    client_id, product_key, status, monthly_price_cents, currency, starts_on,
                    ends_on, approved_by_user_id, approved_at
                ) VALUES (CAST(? AS uuid), ?, 'ACTIVE', ?, 'EUR', CURRENT_DATE, NULL, CAST(? AS uuid), NOW())
                ON CONFLICT (client_id, product_key) DO UPDATE
                SET status = 'ACTIVE',
                    monthly_price_cents = EXCLUDED.monthly_price_cents,
                    currency = 'EUR',
                    starts_on = EXCLUDED.starts_on,
                    ends_on = NULL,
                    approved_by_user_id = EXCLUDED.approved_by_user_id,
                    approved_at = EXCLUDED.approved_at
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, pending.clientId)
                statement.setString(2, pending.productKey)
                statement.setLong(3, 0L)
                statement.setString(4, reviewedByUserId)
                statement.executeUpdate()
            }
            recordSubscriptionEvent(
                connection = connection,
                clientId = pending.clientId,
                productKey = pending.productKey,
                requestId = pending.id,
                actorUserId = reviewedByUserId,
                action = "APPROVED",
                previousStatus = previousSubscriptionStatus,
                newStatus = "ACTIVE",
                monthlyPriceCents = 0L,
            )
            val approved = requireNotNull(clientProgramRequest(connection, requestId))
            connection.commit()
            approved
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = previousAutoCommit
        }
    }

    override fun rejectClientProgramRequest(
        requestId: String,
        request: RejectClientProgramRequest,
        reviewedByUserId: String,
        organizationId: String,
    ): ServerClientProgramRequestSnapshot = dataSource.withConnection { connection ->
        val previousAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            val pending = clientProgramRequestForOwner(connection, requestId, organizationId, lockForUpdate = true)
                ?: throw ServerNotFoundException("Program request not found")
            if (pending.status != "REQUESTED") throw ServerConflictException("Program request has already been decided")
            connection.prepareStatement(
                """
                UPDATE client_program_requests
                SET status = 'REJECTED',
                    admin_note = ?,
                    decided_at = NOW(),
                    decided_by_user_id = CAST(? AS uuid)
                WHERE id = CAST(? AS uuid)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, request.adminNote?.trim()?.takeIf(String::isNotBlank))
                statement.setString(2, reviewedByUserId)
                statement.setString(3, requestId)
                check(statement.executeUpdate() == 1) { "Program request was not updated" }
            }
            recordSubscriptionEvent(
                connection = connection,
                clientId = pending.clientId,
                productKey = pending.productKey,
                requestId = pending.id,
                actorUserId = reviewedByUserId,
                action = "REJECTED",
                previousStatus = "REQUESTED",
                newStatus = "REJECTED",
                monthlyPriceCents = null,
            )
            val rejected = requireNotNull(clientProgramRequest(connection, requestId))
            connection.commit()
            rejected
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = previousAutoCommit
        }
    }

    override fun getClientBillingPreview(
        clientId: String,
        period: String,
        organizationId: String,
    ): ServerClientBillingPreviewSnapshot = dataSource.withConnection { connection ->
        requireClientExists(connection, clientId, organizationId)
        val firstDay = LocalDate.parse("$period-01")
        val lastDay = firstDay.plusMonths(1).minusDays(1)
        val lines = connection.prepareStatement(
            """
            SELECT subscription.product_key, catalog.name, subscription.monthly_price_cents, subscription.currency
            FROM client_product_subscriptions subscription
            JOIN product_catalog catalog ON catalog.product_key = subscription.product_key
            WHERE subscription.client_id = CAST(? AS uuid)
              AND subscription.status = 'ACTIVE'
              AND subscription.monthly_price_cents > 0
              AND subscription.starts_on <= ?
              AND (subscription.ends_on IS NULL OR subscription.ends_on >= ?)
            ORDER BY subscription.product_key
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setObject(2, lastDay)
            statement.setObject(3, firstDay)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            ServerBillingPreviewLineSnapshot(
                                productKey = resultSet.getString("product_key"),
                                name = resultSet.getString("name"),
                                monthlyPriceCents = resultSet.getLong("monthly_price_cents"),
                                currency = resultSet.getString("currency"),
                            ),
                        )
                    }
                }
            }
        }
        ServerClientBillingPreviewSnapshot(
            clientId = clientId,
            period = period,
            lines = lines,
            totalMonthlyPriceCents = lines.sumOf { it.monthlyPriceCents },
            currency = "EUR",
        )
    }

    override fun deleteClient(clientId: String, organizationId: String) {
        dataSource.withConnection { connection ->
            requireClientExists(connection, clientId, organizationId)
            if (hasLinkedTickets(connection, clientId)) {
                throw ServerConflictException("Client has related tickets and cannot be deleted")
            }
            connection.prepareStatement(
                """
                UPDATE tasks
                SET client_id = NULL
                WHERE client_id::text = ?
                  AND (? IS NOT NULL AND organization_id::text = ?)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, clientId)
                statement.setString(2, organizationId)
                statement.setString(3, organizationId)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "DELETE FROM clients WHERE id::text = ? AND (? IS NOT NULL AND organization_id::text = ?)",
            ).use { statement ->
                statement.setString(1, clientId)
                statement.setString(2, organizationId)
                statement.setString(3, organizationId)
                if (statement.executeUpdate() == 0) {
                    throw ServerNotFoundException("Client not found")
                }
            }
        }
    }

    override fun getClientContacts(clientId: String, organizationId: String): List<ServerClientContactSnapshot> =
        dataSource.withConnection { connection ->
            requireClientExists(connection, clientId, organizationId)
            connection.prepareStatement(
                """
                SELECT id::text, client_id::text, full_name, email::text, phone, role, is_primary, created_at, updated_at
                FROM client_contacts
                WHERE client_id::text = ?
                ORDER BY is_primary DESC, full_name ASC
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, clientId)
                statement.executeQuery().use { resultSet ->
                    buildList {
                        while (resultSet.next()) add(clientContactSnapshot(resultSet))
                    }
                }
            }
        }

    override fun createClientContact(
        clientId: String,
        request: CreateClientContactRequest,
        organizationId: String,
    ): ServerClientContactSnapshot = dataSource.withConnection { connection ->
        requireClientExists(connection, clientId, organizationId)
        val previousAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            if (request.isPrimary) {
                connection.prepareStatement(
                    "UPDATE client_contacts SET is_primary = FALSE WHERE client_id::text = ? AND is_primary = TRUE",
                ).use { statement ->
                    statement.setString(1, clientId)
                    statement.executeUpdate()
                }
            }
            val contact = connection.prepareStatement(
                """
                INSERT INTO client_contacts (client_id, full_name, email, phone, role, is_primary)
                VALUES (CAST(? AS uuid), ?, NULLIF(?, ''), NULLIF(?, ''), NULLIF(?, ''), ?)
                RETURNING id::text, client_id::text, full_name, email::text, phone, role, is_primary, created_at, updated_at
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, clientId)
                statement.setString(2, request.fullName.trim())
                statement.setString(3, request.email?.trim())
                statement.setString(4, request.phone?.trim())
                statement.setString(5, request.role?.trim())
                statement.setBoolean(6, request.isPrimary)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    clientContactSnapshot(resultSet)
                }
            }
            connection.commit()
            contact
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = previousAutoCommit
        }
    }

    override fun updateClientContact(
        clientId: String,
        contactId: String,
        request: UpdateClientContactRequest,
        organizationId: String,
    ): ServerClientContactSnapshot = dataSource.withConnection { connection ->
        requireClientExists(connection, clientId, organizationId)
        val previousAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            if (request.isPrimary == true) {
                connection.prepareStatement(
                    "UPDATE client_contacts SET is_primary = FALSE WHERE client_id::text = ? AND id::text <> ? AND is_primary = TRUE",
                ).use { statement ->
                    statement.setString(1, clientId)
                    statement.setString(2, contactId)
                    statement.executeUpdate()
                }
            }
            val contact = connection.prepareStatement(
                """
                UPDATE client_contacts
                SET full_name = COALESCE(NULLIF(?, ''), full_name),
                    email = CASE WHEN ? IS NULL THEN email ELSE NULLIF(?, '') END,
                    phone = CASE WHEN ? IS NULL THEN phone ELSE NULLIF(?, '') END,
                    role = CASE WHEN ? IS NULL THEN role ELSE NULLIF(?, '') END,
                    is_primary = COALESCE(CAST(? AS boolean), is_primary)
                WHERE id::text = ? AND client_id::text = ?
                RETURNING id::text, client_id::text, full_name, email::text, phone, role, is_primary, created_at, updated_at
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, request.fullName?.trim())
                statement.setString(2, request.email)
                statement.setString(3, request.email?.trim())
                statement.setString(4, request.phone)
                statement.setString(5, request.phone?.trim())
                statement.setString(6, request.role)
                statement.setString(7, request.role?.trim())
                statement.setObject(8, request.isPrimary)
                statement.setString(9, contactId)
                statement.setString(10, clientId)
                statement.executeQuery().use { resultSet ->
                    if (!resultSet.next()) throw ServerNotFoundException("Client contact not found")
                    clientContactSnapshot(resultSet)
                }
            }
            connection.commit()
            contact
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = previousAutoCommit
        }
    }

    override fun deleteClientContact(clientId: String, contactId: String, organizationId: String) {
        dataSource.withConnection { connection ->
            requireClientExists(connection, clientId, organizationId)
            connection.prepareStatement(
                "DELETE FROM client_contacts WHERE id::text = ? AND client_id::text = ?",
            ).use { statement ->
                statement.setString(1, contactId)
                statement.setString(2, clientId)
                if (statement.executeUpdate() == 0) throw ServerNotFoundException("Client contact not found")
            }
        }
    }

    override fun getClientActivities(clientId: String, organizationId: String): List<ServerClientActivitySnapshot> =
        dataSource.withConnection { connection ->
            requireClientExists(connection, clientId, organizationId)
            connection.prepareStatement(
                """
                SELECT a.id::text, a.client_id::text, a.contact_id::text, a.activity_type, a.subject, a.details,
                       a.due_date, a.completed_at, creator.name AS created_by_name, a.created_at, a.updated_at
                FROM client_activities a
                JOIN users creator ON creator.id = a.created_by
                WHERE a.client_id::text = ?
                ORDER BY a.completed_at NULLS FIRST, a.due_date ASC NULLS LAST, a.created_at DESC
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, clientId)
                statement.executeQuery().use { resultSet ->
                    buildList {
                        while (resultSet.next()) add(clientActivitySnapshot(resultSet))
                    }
                }
            }
        }

    override fun createClientActivity(
        clientId: String,
        request: CreateClientActivityRequest,
        createdById: String,
        organizationId: String,
    ): ServerClientActivitySnapshot = dataSource.withConnection { connection ->
        requireClientExists(connection, clientId, organizationId)
        val contactId = request.contactId?.trim()?.takeIf(String::isNotBlank)
        if (contactId != null) requireClientContactExists(connection, clientId, contactId)
        val activityId = connection.prepareStatement(
            """
            INSERT INTO client_activities (client_id, contact_id, created_by, activity_type, subject, details, due_date)
            VALUES (CAST(? AS uuid), CAST(NULLIF(?, '') AS uuid), CAST(? AS uuid), ?, ?, NULLIF(?, ''), CAST(NULLIF(?, '') AS date))
            RETURNING id::text
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, contactId)
            statement.setString(3, createdById)
            statement.setString(4, request.type)
            statement.setString(5, request.subject.trim())
            statement.setString(6, request.details?.trim())
            statement.setString(7, request.dueDate?.trim())
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getString("id")
            }
        }
        clientActivitySnapshot(connection, clientId, activityId)
    }

    override fun updateClientActivity(
        clientId: String,
        activityId: String,
        request: UpdateClientActivityRequest,
        organizationId: String,
    ): ServerClientActivitySnapshot = dataSource.withConnection { connection ->
        requireClientExists(connection, clientId, organizationId)
        val requestedContactId = request.contactId?.trim()?.takeIf(String::isNotBlank)
        if (requestedContactId != null) requireClientContactExists(connection, clientId, requestedContactId)
        val id = connection.prepareStatement(
            """
            UPDATE client_activities
            SET activity_type = COALESCE(NULLIF(?, ''), activity_type),
                subject = COALESCE(NULLIF(?, ''), subject),
                details = CASE WHEN ? IS NULL THEN details ELSE NULLIF(?, '') END,
                contact_id = CASE WHEN ? IS NULL THEN contact_id ELSE CAST(NULLIF(?, '') AS uuid) END,
                due_date = CASE WHEN ? IS NULL THEN due_date ELSE CAST(NULLIF(?, '') AS date) END,
                completed_at = CASE
                    WHEN CAST(? AS boolean) IS TRUE THEN COALESCE(completed_at, NOW())
                    WHEN CAST(? AS boolean) IS FALSE THEN NULL
                    ELSE completed_at
                END
            WHERE id::text = ? AND client_id::text = ?
            RETURNING id::text
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, request.type?.trim())
            statement.setString(2, request.subject?.trim())
            statement.setString(3, request.details)
            statement.setString(4, request.details?.trim())
            statement.setString(5, request.contactId)
            statement.setString(6, request.contactId?.trim())
            statement.setString(7, request.dueDate)
            statement.setString(8, request.dueDate?.trim())
            statement.setObject(9, request.completed)
            statement.setObject(10, request.completed)
            statement.setString(11, activityId)
            statement.setString(12, clientId)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) throw ServerNotFoundException("Client activity not found")
                resultSet.getString("id")
            }
        }
        clientActivitySnapshot(connection, clientId, id)
    }

    override fun deleteClientActivity(clientId: String, activityId: String, organizationId: String) {
        dataSource.withConnection { connection ->
            requireClientExists(connection, clientId, organizationId)
            connection.prepareStatement(
                "DELETE FROM client_activities WHERE id::text = ? AND client_id::text = ?",
            ).use { statement ->
                statement.setString(1, activityId)
                statement.setString(2, clientId)
                if (statement.executeUpdate() == 0) throw ServerNotFoundException("Client activity not found")
            }
        }
    }

    override fun getTaskLabels(organizationId: String): List<ServerTaskLabelSnapshot> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT tl.id::text AS id, tl.owner_admin_id::text AS owner_admin_id, tl.name, tl.color_hex, COUNT(t.id)::integer AS tasks_count
            FROM task_labels tl
            LEFT JOIN tasks t ON t.label_id = tl.id
                             AND (? IS NOT NULL AND t.organization_id::text = ?)
            WHERE (? IS NOT NULL AND tl.organization_id::text = ?)
            GROUP BY tl.id, tl.owner_admin_id, tl.name, tl.color_hex
            ORDER BY tl.name ASC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.setString(2, organizationId)
            statement.setString(3, organizationId)
            statement.setString(4, organizationId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            ServerTaskLabelSnapshot(
                                id = resultSet.getString("id"),
                                ownerAdminId = resultSet.getString("owner_admin_id"),
                                name = resultSet.getString("name"),
                                colorHex = resultSet.getString("color_hex"),
                                tasksCount = resultSet.getInt("tasks_count"),
                            ),
                        )
                    }
                }
            }
        }
    }

    override fun createTaskLabel(
        request: CreateTaskLabelRequest,
        organizationId: String,
    ): ServerTaskLabelSnapshot = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            INSERT INTO task_labels (owner_admin_id, organization_id, name, color_hex)
            VALUES ($ORGANIZATION_PRIMARY_ADMIN, CAST(? AS uuid), ?, ?)
            RETURNING id::text, owner_admin_id::text, name, color_hex
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.setString(2, organizationId)
            statement.setString(3, request.name.trim())
            statement.setString(4, normalizeHex(request.colorHex))
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                ServerTaskLabelSnapshot(
                    id = resultSet.getString("id"),
                    ownerAdminId = resultSet.getString("owner_admin_id"),
                    name = resultSet.getString("name"),
                    colorHex = resultSet.getString("color_hex"),
                    tasksCount = 0,
                )
            }
        }
    }

    override fun updateTaskLabel(
        labelId: String,
        request: UpdateTaskLabelRequest,
        organizationId: String,
    ): ServerTaskLabelSnapshot = dataSource.withConnection { connection ->
        requireLabelExists(connection, labelId, organizationId)
        connection.prepareStatement(
            """
            UPDATE task_labels
            SET name = COALESCE(NULLIF(?, ''), name),
                color_hex = COALESCE(NULLIF(?, ''), color_hex)
            WHERE id::text = ?
              AND (? IS NOT NULL AND organization_id::text = ?)
            RETURNING id::text, owner_admin_id::text, name, color_hex
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, request.name)
            statement.setString(2, request.colorHex?.let(::normalizeHex))
            statement.setString(3, labelId)
            statement.setString(4, organizationId)
            statement.setString(5, organizationId)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) {
                    throw ServerNotFoundException("Label not found")
                }
                ServerTaskLabelSnapshot(
                    id = resultSet.getString("id"),
                    ownerAdminId = resultSet.getString("owner_admin_id"),
                    name = resultSet.getString("name"),
                    colorHex = resultSet.getString("color_hex"),
                    tasksCount = countTasksForLabel(connection, labelId, organizationId),
                )
            }
        }
    }

    override fun deleteTaskLabel(labelId: String, organizationId: String) {
        dataSource.withConnection { connection ->
            requireLabelExists(connection, labelId, organizationId)
            if (hasTasksForLabel(connection, labelId, organizationId)) {
                throw ServerConflictException("Label is in use by tasks and cannot be deleted")
            }
            connection.prepareStatement(
                "DELETE FROM task_labels WHERE id::text = ? AND (? IS NOT NULL AND organization_id::text = ?)",
            ).use { statement ->
                statement.setString(1, labelId)
                statement.setString(2, organizationId)
                statement.setString(3, organizationId)
                statement.executeUpdate()
            }
        }
    }

    override fun getTasks(clientId: String?, labelId: String?, organizationId: String): List<ServerTaskSnapshot> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT
                t.id::text AS id,
                t.owner_admin_id::text AS owner_admin_id,
                t.created_by_user_id::text AS created_by_user_id,
                creator.name AS created_by_user_name,
                t.title,
                t.description,
                t.client_id::text AS client_id,
                c.company_name,
                tl.id::text AS label_id,
                tl.name AS label_name,
                tl.color_hex,
                t.due_date::text AS due_date,
                t.completed,
                t.logged_minutes,
                t.logged_seconds,
                t.created_at,
                t.updated_at
            FROM tasks t
            JOIN task_labels tl ON tl.id = t.label_id
            JOIN users creator ON creator.id = t.created_by_user_id
            LEFT JOIN clients c ON c.id = t.client_id
            WHERE (? IS NULL OR t.client_id::text = ?)
              AND (? IS NULL OR t.label_id::text = ?)
              AND (? IS NOT NULL AND t.organization_id::text = ?)
            ORDER BY t.completed ASC, t.due_date ASC NULLS LAST, t.updated_at DESC, t.created_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, clientId)
            statement.setString(3, labelId)
            statement.setString(4, labelId)
            statement.setString(5, organizationId)
            statement.setString(6, organizationId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(taskSnapshot(resultSet))
                    }
                }
            }
        }
    }

    override fun createTask(
        request: CreateTaskRequest,
        createdByUserId: String,
        organizationId: String,
    ): ServerTaskSnapshot = dataSource.withConnection { connection ->
        requireLabelExists(connection, request.labelId, organizationId)
        val resolvedOrganizationId = requireNotNull(organizationId) { "organizationId is required" }
        requireTaskCreatorBelongsToOwner(connection, createdByUserId, resolvedOrganizationId)
        request.clientId?.takeIf { it.isNotBlank() }?.let { requireClientExists(connection, it, resolvedOrganizationId) }
        connection.prepareStatement(
            """
            INSERT INTO tasks (
                title, description, client_id, owner_admin_id, organization_id, created_by_user_id,
                label_id, due_date, completed, logged_minutes, logged_seconds
            )
            VALUES (?, ?, CAST(? AS uuid), $ORGANIZATION_PRIMARY_ADMIN, CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), CAST(? AS date), FALSE, 0, 0)
            RETURNING
                id::text AS id,
                title,
                description,
                client_id::text AS client_id,
                due_date::text AS due_date,
                created_at,
                updated_at,
                completed,
                logged_minutes,
                logged_seconds
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, request.title.trim())
            statement.setString(2, request.description.trim())
            statement.setString(3, request.clientId?.takeIf { it.isNotBlank() })
            statement.setString(4, resolvedOrganizationId)
            statement.setString(5, resolvedOrganizationId)
            statement.setString(6, createdByUserId)
            statement.setString(7, request.labelId)
            statement.setString(8, request.dueDate?.trim()?.takeIf { it.isNotBlank() })
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                hydrateTaskSnapshot(connection, resultSet)
            }
        }
    }

    override fun updateTask(taskId: String, request: UpdateTaskRequest, organizationId: String): ServerTaskSnapshot = dataSource.withConnection { connection ->
        requireTaskExists(connection, taskId, organizationId)
        request.labelId?.takeIf { it.isNotBlank() }?.let { requireLabelExists(connection, it, organizationId) }
        request.clientId?.takeIf { it.isNotBlank() }?.let { requireClientExists(connection, it, organizationId) }
        connection.prepareStatement(
            """
            UPDATE tasks
            SET title = COALESCE(NULLIF(?, ''), title),
                description = COALESCE(?, description),
                client_id = CASE
                    WHEN ? = '__CLEAR__' THEN NULL
                    WHEN ? IS NULL OR ? = '' THEN client_id
                    ELSE CAST(? AS uuid)
                END,
                label_id = COALESCE(CAST(NULLIF(?, '') AS uuid), label_id),
                due_date = CASE
                    WHEN ? = '__CLEAR__' THEN NULL
                    WHEN ? IS NULL THEN due_date
                    WHEN ? = '' THEN NULL
                    ELSE CAST(? AS date)
                END,
                completed = COALESCE(?, completed)
            WHERE id::text = ?
              AND (? IS NOT NULL AND organization_id::text = ?)
            RETURNING
                id::text AS id,
                title,
                description,
                client_id::text AS client_id,
                due_date::text AS due_date,
                created_at,
                updated_at,
                completed,
                logged_minutes,
                logged_seconds
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, request.title)
            statement.setString(2, request.description)
            val clientValue = request.clientId ?: ""
            statement.setString(3, if (request.clientId == null) "" else if (request.clientId.isBlank()) "__CLEAR__" else request.clientId)
            statement.setString(4, clientValue)
            statement.setString(5, clientValue)
            statement.setString(6, clientValue)
            statement.setString(7, request.labelId)
            val dueDateValue = request.dueDate
            val dueDateMarker = if (request.dueDate == null) null else if (request.dueDate.isBlank()) "__CLEAR__" else request.dueDate
            statement.setString(8, dueDateMarker)
            statement.setString(9, dueDateValue)
            statement.setString(10, dueDateValue)
            statement.setString(11, dueDateValue)
            if (request.completed == null) {
                statement.setNull(12, java.sql.Types.BOOLEAN)
            } else {
                statement.setBoolean(12, request.completed)
            }
            statement.setString(13, taskId)
            statement.setString(14, organizationId)
            statement.setString(15, organizationId)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) {
                    throw ServerNotFoundException("Task not found")
                }
                hydrateTaskSnapshot(connection, resultSet)
            }
        }
    }

    override fun deleteTask(taskId: String, organizationId: String) {
        dataSource.withConnection { connection ->
            requireTaskExists(connection, taskId, organizationId)
            connection.prepareStatement(
                "DELETE FROM tasks WHERE id::text = ? AND (? IS NOT NULL AND organization_id::text = ?)",
            ).use { statement ->
                statement.setString(1, taskId)
                statement.setString(2, organizationId)
                statement.setString(3, organizationId)
                if (statement.executeUpdate() == 0) {
                    throw ServerNotFoundException("Task not found")
                }
            }
        }
    }

    override fun getTaskBoard(organizationId: String): ServerTaskBoardSnapshot = dataSource.withConnection { connection ->
        val today = connection.prepareStatement(
            "SELECT (CURRENT_TIMESTAMP AT TIME ZONE 'Europe/Madrid')::date::text AS today",
        ).use { statement ->
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getString("today")
            }
        }
        val placements = connection.prepareStatement(
            """
            SELECT
                placement.task_id::text AS task_id,
                placement.board_date::text AS board_date,
                placement.position_x,
                placement.position_y,
                placement.created_at,
                placement.updated_at
            FROM task_board_placements placement
            JOIN tasks task ON task.id = placement.task_id
            WHERE task.organization_id::text = ?
              AND task.completed = FALSE
              AND placement.board_date >= CAST(? AS date)
            ORDER BY placement.board_date, placement.updated_at, placement.task_id
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.setString(2, today)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) add(taskBoardPlacementSnapshot(resultSet))
                }
            }
        }
        ServerTaskBoardSnapshot(
            today = today,
            timeZone = TASK_BOARD_TIME_ZONE,
            refreshAfterSeconds = madridTaskBoardDayBoundary().refreshAfterSeconds,
            placements = placements,
        )
    }

    override fun upsertTaskBoardPlacement(
        taskId: String,
        organizationId: String,
        boardDate: String,
        positionX: Double,
        positionY: Double,
    ): ServerTaskBoardPlacementSnapshot = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            WITH target_task AS (
                SELECT id
                FROM tasks
                WHERE id::text = ?
                  AND organization_id::text = ?
                  AND completed = FALSE
                FOR UPDATE
            )
            INSERT INTO task_board_placements (task_id, board_date, position_x, position_y)
            SELECT id, CAST(? AS date), ?, ?
            FROM target_task
            ON CONFLICT (task_id) DO UPDATE
            SET board_date = EXCLUDED.board_date,
                position_x = EXCLUDED.position_x,
                position_y = EXCLUDED.position_y
            RETURNING
                task_id::text AS task_id,
                board_date::text AS board_date,
                position_x,
                position_y,
                created_at,
                updated_at
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, taskId)
            statement.setString(2, organizationId)
            statement.setString(3, boardDate)
            statement.setDouble(4, positionX)
            statement.setDouble(5, positionY)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) {
                    requireTaskExists(connection, taskId, organizationId)
                    throw ServerConflictException("Completed tasks cannot be placed on the board")
                }
                taskBoardPlacementSnapshot(resultSet)
            }
        }
    }

    override fun deleteTaskBoardPlacement(taskId: String, organizationId: String) {
        dataSource.withConnection { connection ->
            requireTaskExists(connection, taskId, organizationId)
            connection.prepareStatement(
                """
                DELETE FROM task_board_placements placement
                USING tasks task
                WHERE placement.task_id = task.id
                  AND task.id::text = ?
                  AND task.organization_id::text = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, taskId)
                statement.setString(2, organizationId)
                statement.executeUpdate()
            }
        }
    }

    override fun getTimeLogs(clientId: String?, taskId: String?, organizationId: String): List<ServerTimeLogSnapshot> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT
                tl.id::text AS id,
                t.owner_admin_id::text AS owner_admin_id,
                tl.task_id::text AS task_id,
                tl.client_id::text AS client_id,
                tl.author_id::text AS author_id,
                u.name AS author_name,
                tl.minutes,
                tl.seconds,
                tl.work_date::text AS work_date,
                tl.note,
                tl.billable,
                tl.created_at,
                tl.session_id::text AS session_id,
                tl.is_historic_migrated
            FROM time_logs tl
            JOIN tasks t ON t.id = tl.task_id
            JOIN users u ON u.id = tl.author_id
            WHERE (? IS NULL OR tl.client_id::text = ?)
              AND (? IS NULL OR tl.task_id::text = ?)
              AND (? IS NOT NULL AND t.organization_id::text = ?)
            ORDER BY tl.work_date DESC NULLS LAST, tl.created_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, clientId)
            statement.setString(3, taskId)
            statement.setString(4, taskId)
            statement.setString(5, organizationId)
            statement.setString(6, organizationId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(timeLogSnapshot(resultSet))
                    }
                }
            }
        }
    }

    override fun activeWorkSession(actorUserId: String): ServerWorkSessionSnapshot? = dataSource.withConnection { connection ->
        val sessionId = connection.prepareStatement(
            """
            SELECT id::text AS id FROM work_sessions
            WHERE owner_admin_id::text = ? AND status IN ('ACTIVE', 'PAUSED')
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, actorUserId)
            statement.executeQuery().use { resultSet -> if (resultSet.next()) resultSet.getString("id") else null }
        }
        sessionId?.let { hydrateWorkSession(connection, it) }
    }

    override fun workSessionsForTask(taskId: String, actorUserId: String): List<ServerWorkSessionSnapshot> = dataSource.withConnection { connection ->
        requireTaskExists(connection, taskId, staffOrganizationId(connection, actorUserId))
        val sessionIds = connection.prepareStatement(
            """
            SELECT id::text AS id FROM work_sessions
            WHERE task_id::text = ? AND owner_admin_id::text = ?
            ORDER BY started_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, taskId)
            statement.setString(2, actorUserId)
            statement.executeQuery().use { resultSet ->
                buildList { while (resultSet.next()) add(resultSet.getString("id")) }
            }
        }
        sessionIds.map { id -> requireNotNull(hydrateWorkSession(connection, id)) }
    }

    override fun startedWorkSession(request: StartWorkSessionRequest, actorUserId: String, authorId: String): ServerWorkSessionSnapshot =
        dataSource.withConnection { connection ->
            connection.autoCommit = false
            try {
                val organizationId = staffOrganizationId(connection, actorUserId)
                val task = connection.prepareStatement(
                    "SELECT client_id::text AS client_id FROM tasks WHERE id::text = ? AND organization_id::text = ? LIMIT 1",
                ).use { statement ->
                    statement.setString(1, request.taskId)
                    statement.setString(2, organizationId)
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) resultSet.getString("client_id") else throw ServerNotFoundException("Task not found")
                    }
                }

                val existingActive = connection.prepareStatement(
                    """
                    SELECT ws.id::text AS id, t.title AS title FROM work_sessions ws
                    JOIN tasks t ON t.id = ws.task_id
                    WHERE ws.owner_admin_id::text = ? AND ws.status IN ('ACTIVE', 'PAUSED')
                    LIMIT 1
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, actorUserId)
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) resultSet.getString("title") else null
                    }
                }
                if (existingActive != null) {
                    throw ServerConflictException("Ya hay un temporizador activo en la tarea '$existingActive'. Detenlo o pausalo antes de iniciar uno nuevo.")
                }

                val billable = task != null

                val sessionId = try {
                    connection.prepareStatement(
                        """
                        INSERT INTO work_sessions (task_id, owner_admin_id, organization_id, client_id, author_id, started_at, status, billable, time_source, business_timezone)
                        VALUES (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), NOW(), 'ACTIVE', ?, 'SERVER', 'Europe/Madrid')
                        RETURNING id::text AS id
                        """.trimIndent(),
                    ).use { statement ->
                        statement.setString(1, request.taskId)
                        statement.setString(2, actorUserId)
                        statement.setString(3, organizationId)
                        statement.setString(4, task)
                        statement.setString(5, authorId)
                        statement.setBoolean(6, billable)
                        statement.executeQuery().use { resultSet -> resultSet.next(); resultSet.getString("id") }
                    }
                } catch (error: java.sql.SQLException) {
                    if (error.sqlState == UNIQUE_VIOLATION_SQL_STATE) {
                        throw ServerConflictException("Ya hay un temporizador activo. Detenlo o pausalo antes de iniciar uno nuevo.")
                    }
                    throw error
                }

                connection.prepareStatement(
                    "INSERT INTO work_segments (session_id, started_at) VALUES (CAST(? AS uuid), NOW())",
                ).use { statement ->
                    statement.setString(1, sessionId)
                    statement.executeUpdate()
                }

                val snapshot = requireNotNull(hydrateWorkSession(connection, sessionId))
                connection.commit()
                snapshot
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit = true
            }
        }

    override fun pausedWorkSession(sessionId: String, actorUserId: String): ServerWorkSessionSnapshot = dataSource.withConnection { connection ->
        connection.autoCommit = false
        try {
            val status = requireSessionOwnership(connection, sessionId, actorUserId)
            if (status == "FINISHED") throw ServerConflictException("El temporizador ya esta detenido.")
            if (status == "ACTIVE") {
                connection.prepareStatement(
                    "UPDATE work_segments SET ended_at = NOW() WHERE session_id::text = ? AND ended_at IS NULL",
                ).use { statement ->
                    statement.setString(1, sessionId)
                    statement.executeUpdate()
                }
                connection.prepareStatement("UPDATE work_sessions SET status = 'PAUSED' WHERE id::text = ?").use { statement ->
                    statement.setString(1, sessionId)
                    statement.executeUpdate()
                }
            }
            val snapshot = requireNotNull(hydrateWorkSession(connection, sessionId))
            connection.commit()
            snapshot
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = true
        }
    }

    override fun resumedWorkSession(sessionId: String, actorUserId: String): ServerWorkSessionSnapshot = dataSource.withConnection { connection ->
        connection.autoCommit = false
        try {
            val status = requireSessionOwnership(connection, sessionId, actorUserId)
            if (status == "FINISHED") throw ServerConflictException("El temporizador ya esta detenido.")
            if (status == "PAUSED") {
                connection.prepareStatement(
                    "INSERT INTO work_segments (session_id, started_at) VALUES (CAST(? AS uuid), NOW())",
                ).use { statement ->
                    statement.setString(1, sessionId)
                    statement.executeUpdate()
                }
                connection.prepareStatement("UPDATE work_sessions SET status = 'ACTIVE' WHERE id::text = ?").use { statement ->
                    statement.setString(1, sessionId)
                    statement.executeUpdate()
                }
            }
            val snapshot = requireNotNull(hydrateWorkSession(connection, sessionId))
            connection.commit()
            snapshot
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = true
        }
    }

    override fun stoppedWorkSession(sessionId: String, actorUserId: String): ServerWorkSessionSnapshot = dataSource.withConnection { connection ->
        connection.autoCommit = false
        try {
            val status = requireSessionOwnership(connection, sessionId, actorUserId)
            if (status == "FINISHED") throw ServerConflictException("El temporizador ya esta detenido.")

            connection.prepareStatement(
                "UPDATE work_segments SET ended_at = NOW() WHERE session_id::text = ? AND ended_at IS NULL",
            ).use { statement ->
                statement.setString(1, sessionId)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "UPDATE work_sessions SET status = 'FINISHED', ended_at = NOW() WHERE id::text = ?",
            ).use { statement ->
                statement.setString(1, sessionId)
                statement.executeUpdate()
            }

            // Split the session's total real time by LOCAL (Europe/Madrid) calendar day and
            // write one immutable time_logs row per day touched - this is what makes a
            // midnight-crossing session (or one left running across several days) divide
            // correctly without inventing a second session. Done entirely in SQL so Postgres'
            // own tz database (not hand-rolled arithmetic) handles DST transitions correctly.
            val perDayMinutes = connection.prepareStatement(
                """
                SELECT (d.local_date::date)::text AS local_date,
                       GREATEST(0, SUM(
                           EXTRACT(EPOCH FROM (
                               LEAST(seg.ended_at, ((d.local_date::date + 1)::timestamp AT TIME ZONE 'Europe/Madrid'))
                               - GREATEST(seg.started_at, ((d.local_date::date)::timestamp AT TIME ZONE 'Europe/Madrid'))
                           ))
                       ))::bigint AS seconds
                FROM work_segments seg
                CROSS JOIN LATERAL generate_series(
                    (seg.started_at AT TIME ZONE 'Europe/Madrid')::date,
                    (seg.ended_at AT TIME ZONE 'Europe/Madrid')::date,
                    interval '1 day'
                ) AS d(local_date)
                WHERE seg.session_id::text = ? AND seg.ended_at IS NOT NULL
                GROUP BY d.local_date
                ORDER BY d.local_date
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, sessionId)
                statement.executeQuery().use { resultSet ->
                    val perDaySeconds = linkedMapOf<String, Long>()
                    while (resultSet.next()) {
                        perDaySeconds[resultSet.getString("local_date")] = resultSet.getLong("seconds")
                    }
                    perDaySeconds
                }
            }

            val session = requireNotNull(hydrateWorkSession(connection, sessionId))
            var totalSeconds = 0L
            perDayMinutes.forEach { (localDate, seconds) ->
                if (seconds <= 0) return@forEach
                totalSeconds += seconds
                connection.prepareStatement(
                    """
                    INSERT INTO time_logs (task_id, client_id, author_id, session_id, minutes, seconds, work_date, note, billable)
                    VALUES (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), ?, ?, CAST(? AS date), ?, ?)
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, session.taskId)
                    statement.setString(2, session.clientId)
                    statement.setString(3, session.authorId)
                    statement.setString(4, sessionId)
                    statement.setInt(5, (seconds / 60).toInt())
                    statement.setInt(6, seconds.toInt())
                    statement.setString(7, localDate)
                    statement.setString(8, "Sesion de trabajo")
                    statement.setBoolean(9, session.billable)
                    statement.executeUpdate()
                }
            }

            if (totalSeconds > 0) {
                connection.prepareStatement(
                    """
                    UPDATE tasks
                    SET logged_minutes = (logged_seconds + ?) / 60,
                        logged_seconds = logged_seconds + ?
                    WHERE id::text = ?
                    """.trimIndent(),
                ).use { statement ->
                    statement.setLong(1, totalSeconds)
                    statement.setLong(2, totalSeconds)
                    statement.setString(3, session.taskId)
                    statement.executeUpdate()
                }
            }

            val finalSnapshot = requireNotNull(hydrateWorkSession(connection, sessionId))
            connection.commit()
            finalSnapshot
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = true
        }
    }

    override fun checkpointedWorkSession(sessionId: String, actorUserId: String): ServerWorkSessionSnapshot = dataSource.withConnection { connection ->
        requireSessionOwnership(connection, sessionId, actorUserId)
        connection.prepareStatement("UPDATE work_sessions SET last_checkpoint_at = NOW() WHERE id::text = ?").use { statement ->
            statement.setString(1, sessionId)
            statement.executeUpdate()
        }
        requireNotNull(hydrateWorkSession(connection, sessionId))
    }

    /** Company of an active staff user; work sessions are per user but their tasks are per company. */
    private fun staffOrganizationId(connection: Connection, userId: String): String =
        connection.prepareStatement(
            """
            SELECT om.organization_id::text AS organization_id
            FROM organization_members om
            JOIN users u ON u.id = om.user_id AND u.is_active = TRUE
            WHERE om.user_id::text = ?
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, userId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.getString("organization_id") else throw ServerNotFoundException("Task not found")
            }
        }

    private fun requireSessionOwnership(connection: Connection, sessionId: String, actorUserId: String): String =
        connection.prepareStatement(
            "SELECT status FROM work_sessions WHERE id::text = ? AND owner_admin_id::text = ?",
        ).use { statement ->
            statement.setString(1, sessionId)
            statement.setString(2, actorUserId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.getString("status") else throw ServerNotFoundException("Work session not found")
            }
        }

    private fun hydrateWorkSession(connection: Connection, sessionId: String): ServerWorkSessionSnapshot? {
        val session = connection.prepareStatement(
            """
            SELECT
                ws.id::text AS id, ws.owner_admin_id::text AS owner_admin_id, ws.task_id::text AS task_id,
                ws.client_id::text AS client_id, ws.author_id::text AS author_id, ws.started_at, ws.ended_at,
                ws.status, ws.billable, ws.time_source, ws.business_timezone, ws.last_checkpoint_at,
                ws.created_at, ws.updated_at
            FROM work_sessions ws
            WHERE ws.id::text = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, sessionId)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) return null
                ServerWorkSessionSnapshot(
                    id = resultSet.getString("id"),
                    ownerAdminId = resultSet.getString("owner_admin_id"),
                    taskId = resultSet.getString("task_id"),
                    clientId = resultSet.getString("client_id"),
                    authorId = resultSet.getString("author_id"),
                    startedAt = formatTimestamp(resultSet.getObject("started_at")),
                    endedAt = resultSet.getObject("ended_at")?.let { formatTimestamp(it) },
                    status = resultSet.getString("status"),
                    billable = resultSet.getBoolean("billable"),
                    timeSource = resultSet.getString("time_source"),
                    businessTimezone = resultSet.getString("business_timezone"),
                    lastCheckpointAt = resultSet.getObject("last_checkpoint_at")?.let { formatTimestamp(it) },
                    createdAt = formatTimestamp(resultSet.getObject("created_at")),
                    updatedAt = formatTimestamp(resultSet.getObject("updated_at")),
                )
            }
        }
        val segments = connection.prepareStatement(
            """
            SELECT id::text AS id, session_id::text AS session_id, started_at, ended_at
            FROM work_segments
            WHERE session_id::text = ?
            ORDER BY started_at ASC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, sessionId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            ServerWorkSegmentSnapshot(
                                id = resultSet.getString("id"),
                                sessionId = resultSet.getString("session_id"),
                                startedAt = formatTimestamp(resultSet.getObject("started_at")),
                                endedAt = resultSet.getObject("ended_at")?.let { formatTimestamp(it) },
                            ),
                        )
                    }
                }
            }
        }
        val closedSeconds = connection.prepareStatement(
            """
            SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (ended_at - started_at))), 0)::bigint AS seconds
            FROM work_segments
            WHERE session_id::text = ? AND ended_at IS NOT NULL
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, sessionId)
            statement.executeQuery().use { resultSet -> resultSet.next(); resultSet.getLong("seconds") }
        }
        return session.copy(
            segments = segments,
            closedSegmentsSeconds = closedSeconds,
            openSegmentStartedAt = segments.firstOrNull { it.endedAt == null }?.startedAt,
        )
    }

    override fun getDashboard(clientId: String?, labelId: String?, organizationId: String): ServerDashboardSnapshot = dataSource.withConnection { connection ->
        val activeClients = connection.prepareStatement(
            """
            SELECT COUNT(*)::integer AS active_clients
            FROM clients
            WHERE account_status = 'ACTIVE'
              AND (? IS NOT NULL AND organization_id::text = ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.setString(2, organizationId)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getInt("active_clients")
            }
        }
        val totals = connection.prepareStatement(
            """
            SELECT
                COALESCE(SUM(tl.minutes), 0)::integer AS total_minutes,
                COALESCE(SUM(tl.minutes) FILTER (WHERE tl.billable = TRUE), 0)::integer AS billable_minutes,
                TO_CHAR(CURRENT_DATE, 'TMMonth YYYY') AS month_label
            FROM time_logs tl
            JOIN tasks t ON t.id = tl.task_id
            WHERE DATE_TRUNC('month', tl.work_date) = DATE_TRUNC('month', CURRENT_DATE)
              AND (? IS NOT NULL AND t.organization_id::text = ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.setString(2, organizationId)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                Triple(
                    resultSet.getInt("total_minutes"),
                    resultSet.getInt("billable_minutes"),
                    resultSet.getString("month_label").trim(),
                )
            }
        }
        val selectedClientTotals = if (clientId == null) {
            Pair(0, 0)
        } else {
            connection.prepareStatement(
                """
                SELECT
                    COALESCE(SUM(tl.minutes), 0)::integer AS total_minutes,
                    COALESCE(SUM(tl.minutes) FILTER (WHERE tl.billable = TRUE), 0)::integer AS billable_minutes
                FROM time_logs tl
                JOIN tasks t ON t.id = tl.task_id
                WHERE tl.client_id::text = ?
                  AND (? IS NOT NULL AND t.organization_id::text = ?)
                  AND DATE_TRUNC('month', tl.work_date) = DATE_TRUNC('month', CURRENT_DATE)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, clientId)
                statement.setString(2, organizationId)
                statement.setString(3, organizationId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt("total_minutes") to resultSet.getInt("billable_minutes")
                }
            }
        }
        val dailyMinutes = connection.prepareStatement(
            """
            SELECT tl.work_date::text AS work_date, COALESCE(SUM(tl.minutes), 0)::integer AS minutes
            FROM time_logs tl
            JOIN tasks t ON t.id = tl.task_id
            WHERE DATE_TRUNC('month', tl.work_date) = DATE_TRUNC('month', CURRENT_DATE)
              AND (? IS NULL OR tl.client_id::text = ?)
              AND (? IS NOT NULL AND t.organization_id::text = ?)
            GROUP BY tl.work_date
            ORDER BY tl.work_date ASC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, clientId)
            statement.setString(3, organizationId)
            statement.setString(4, organizationId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            ServerDailyMinutesSnapshot(
                                workDate = resultSet.getString("work_date"),
                                minutes = resultSet.getInt("minutes"),
                            ),
                        )
                    }
                }
            }
        }
        ServerDashboardSnapshot(
            openTickets = 0,
            pendingClientTickets = 0,
            resolvedToday = 0,
            activeClients = activeClients,
            monthLabel = totals.third,
            totalMinutes = totals.first,
            billableMinutes = totals.second,
            selectedClientId = clientId,
            selectedClientMinutes = selectedClientTotals.first,
            selectedClientBillableMinutes = selectedClientTotals.second,
            dailyMinutes = dailyMinutes,
            availableTasks = getTasks(clientId, labelId, organizationId),
        )
    }

    override fun getAttachment(id: String): ServerAttachmentSnapshot? = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT id::text, file_name, content_type
            FROM attachments
            WHERE id::text = ?
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, id)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    ServerAttachmentSnapshot(
                        id = resultSet.getString("id"),
                        fileName = resultSet.getString("file_name"),
                        contentType = resultSet.getString("content_type"),
                    )
                } else {
                    null
                }
            }
        }
    }

    override fun registerDevice(request: RegisterDeviceRequest): ServerDeviceRegistration = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            INSERT INTO notification_devices (user_id, platform, token, last_seen_at)
            VALUES (CAST(? AS uuid), ?, ?, NOW())
            ON CONFLICT (token) DO UPDATE
            SET user_id = EXCLUDED.user_id,
                platform = EXCLUDED.platform,
                last_seen_at = NOW()
            RETURNING id::text, user_id::text, platform
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, request.userId)
            statement.setString(2, request.platform)
            statement.setString(3, request.token)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                ServerDeviceRegistration(
                    id = resultSet.getString("id"),
                    userId = resultSet.getString("user_id"),
                    platform = resultSet.getString("platform"),
                )
            }
        }
    }

    private fun updateLastLogin(connection: Connection, userId: String) {
        connection.prepareStatement(
            "UPDATE users SET last_login_at = NOW() WHERE id::text = ?",
        ).use { statement ->
            statement.setString(1, userId)
            statement.executeUpdate()
        }
    }

    private fun clientSnapshot(
        connection: Connection,
        clientId: String,
        organizationId: String,
    ): ServerClientSnapshot {
        val base = connection.prepareStatement(
            """
            SELECT id::text, owner_admin_id::text, company_name, product_name, contact_name,
                   email::text, account_status, service_tier, preferred_contact_channel
            FROM clients
            WHERE id::text = ?
              AND (? IS NOT NULL AND organization_id::text = ?)
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, organizationId)
            statement.setString(3, organizationId)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) throw ServerNotFoundException("Client not found")
                ServerClientSnapshot(
                    id = resultSet.getString("id"),
                    ownerAdminId = resultSet.getString("owner_admin_id"),
                    companyName = resultSet.getString("company_name"),
                    productName = resultSet.getString("product_name"),
                    contactName = resultSet.getString("contact_name"),
                    email = resultSet.getString("email"),
                    accountStatus = resultSet.getString("account_status"),
                    serviceTier = resultSet.getString("service_tier"),
                    enabledComponents = emptyList(),
                    preferredContactChannel = resultSet.getString("preferred_contact_channel"),
                    activeTicketCount = 0,
                )
            }
        }
        return base.copy(
            enabledComponents = clientComponents(connection, clientId),
            activeTicketCount = getActiveTicketCount(connection, clientId),
            openTasksCount = getOpenTasksCount(connection, clientId, organizationId),
            monthlyLoggedMinutes = getClientMonthlyMinutes(connection, clientId, organizationId),
            billing = readClientBilling(connection, clientId),
        )
    }

    /** Replaces all fiscal data of the client; [billing] is already normalized by the route. */
    private fun writeClientBilling(connection: Connection, clientId: String, billing: ClientBillingRequest) {
        connection.prepareStatement(
            """
            UPDATE clients
            SET billing_legal_name = ?,
                billing_tax_id = ?,
                billing_address_line = ?,
                billing_postal_code = ?,
                billing_city = ?,
                billing_province = ?,
                billing_country = ?,
                billing_email = ?,
                updated_at = NOW()
            WHERE id::text = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, billing.legalName)
            statement.setString(2, billing.taxId)
            statement.setString(3, billing.addressLine)
            statement.setString(4, billing.postalCode)
            statement.setString(5, billing.city)
            statement.setString(6, billing.province)
            statement.setString(7, billing.country)
            statement.setString(8, billing.email)
            statement.setString(9, clientId)
            statement.executeUpdate()
        }
    }

    private fun readClientBilling(connection: Connection, clientId: String): ServerClientBilling =
        connection.prepareStatement(
            "SELECT $CLIENT_BILLING_COLUMNS FROM clients c WHERE c.id::text = ? LIMIT 1",
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.clientBilling() else ServerClientBilling()
            }
        }

    private fun ResultSet.clientBilling(): ServerClientBilling = ServerClientBilling(
        legalName = getString("billing_legal_name").orEmpty(),
        taxId = getString("billing_tax_id").orEmpty(),
        addressLine = getString("billing_address_line").orEmpty(),
        postalCode = getString("billing_postal_code").orEmpty(),
        city = getString("billing_city").orEmpty(),
        province = getString("billing_province").orEmpty(),
        country = getString("billing_country").orEmpty(),
        email = getString("billing_email").orEmpty(),
    )

    private fun clientComponents(connection: Connection, clientId: String): List<String> =
        connection.prepareStatement(
            """
            SELECT component_key
            FROM client_component_entitlements
            WHERE client_id::text = ?
            ORDER BY component_key
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) add(resultSet.getString("component_key"))
                }
            }
        }

    private fun ResultSet.textArray(column: String): List<String> {
        val sqlArray = getArray(column) ?: return emptyList()
        return try {
            (sqlArray.array as? Array<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        } finally {
            sqlArray.free()
        }
    }

    private fun findRequesterId(connection: Connection, clientId: String): String =
        connection.prepareStatement(
            """
            SELECT id::text
            FROM users
            WHERE client_id = CAST(? AS uuid) AND role = 'CLIENT' AND is_active = TRUE
            ORDER BY created_at ASC
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.getString("id") else throw ServerNotFoundException("No active client user found for client $clientId")
            }
        }

    private fun findAffectedApp(connection: Connection, clientId: String): String =
        connection.prepareStatement(
            """
            SELECT product_name
            FROM clients
            WHERE id = CAST(? AS uuid)
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.getString("product_name") else throw ServerNotFoundException("Client not found: $clientId")
            }
        }

    private fun insertEvent(connection: Connection, ticketId: String, actorId: String, type: String, description: String) {
        connection.prepareStatement(
            """
            INSERT INTO ticket_events (ticket_id, actor_id, type, description)
            VALUES (CAST(? AS uuid), CAST(? AS uuid), ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, ticketId)
            statement.setString(2, actorId)
            statement.setString(3, type)
            statement.setString(4, description)
            statement.executeUpdate()
        }
    }

    private fun isValidUuid(value: String): Boolean = runCatching { UUID.fromString(value) }.isSuccess

    private fun ticketSnapshot(resultSet: ResultSet): ServerTicketSnapshot = ServerTicketSnapshot(
        id = resultSet.getString("id"),
        clientId = resultSet.getString("client_id"),
        ticketNumber = resultSet.getString("ticket_number"),
        subject = resultSet.getString("subject"),
        description = resultSet.getString("description"),
        category = resultSet.getString("category"),
        affectedApp = resultSet.getString("affected_app"),
        platform = resultSet.getString("platform"),
        appVersion = resultSet.getString("app_version"),
        stepsToReproduce = resultSet.getString("steps_to_reproduce"),
        clientReference = resultSet.getString("client_reference"),
        status = resultSet.getString("status"),
        priority = resultSet.getString("priority"),
        waitingOn = resultSet.getString("waiting_on"),
        resolutionSummary = resultSet.getString("resolution_summary"),
        requesterId = resultSet.getString("requester_id"),
        requesterName = resultSet.getString("requester_name"),
        requesterEmail = resultSet.getString("requester_email"),
        assigneeId = resultSet.getString("assignee_id"),
        assigneeName = resultSet.getString("assignee_name"),
        assigneeEmail = resultSet.getString("assignee_email"),
        createdAt = resultSet.getTimestamp("created_at").toInstant().toString(),
        updatedAt = resultSet.getTimestamp("updated_at").toInstant().toString(),
    )

    private fun findTicket(connection: Connection, id: String): ServerTicketSnapshot? =
        connection.prepareStatement(
            """
            SELECT t.id::text, t.client_id::text, t.ticket_number, t.subject, t.description, t.category,
                   t.affected_app, t.platform, t.app_version, t.steps_to_reproduce, t.client_reference,
                   t.status, t.priority, t.waiting_on, t.resolution_summary,
                   t.requester_id::text, requester.name AS requester_name, requester.email::text AS requester_email,
                   t.assignee_id::text, assignee.name AS assignee_name, assignee.email::text AS assignee_email,
                   t.created_at, t.updated_at
            FROM tickets t
            JOIN users requester ON requester.id = t.requester_id
            LEFT JOIN users assignee ON assignee.id = t.assignee_id
            WHERE t.id::text = ?
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, id)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) hydrateTicket(connection, ticketSnapshot(resultSet)) else null
            }
        }

    private fun hydrateTicket(connection: Connection, ticket: ServerTicketSnapshot): ServerTicketSnapshot = ticket.copy(
        messages = ticketMessages(connection, ticket.id),
        internalComments = ticketInternalComments(connection, ticket.id),
        events = ticketEvents(connection, ticket.id),
        attachments = ticketAttachments(connection, ticket.id),
    )

    private fun ticketMessages(connection: Connection, ticketId: String): List<ServerTicketMessageSnapshot> =
        connection.prepareStatement(
            """
            SELECT m.id::text, m.ticket_id::text, m.author_id::text, u.name AS author_name, m.body, m.created_at
            FROM ticket_messages m
            JOIN users u ON u.id = m.author_id
            WHERE m.ticket_id::text = ?
            ORDER BY m.created_at ASC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, ticketId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            ServerTicketMessageSnapshot(
                                id = resultSet.getString("id"),
                                ticketId = resultSet.getString("ticket_id"),
                                authorId = resultSet.getString("author_id"),
                                authorName = resultSet.getString("author_name"),
                                body = resultSet.getString("body"),
                                createdAt = resultSet.getTimestamp("created_at").toInstant().toString(),
                            ),
                        )
                    }
                }
            }
        }

    private fun ticketInternalComments(connection: Connection, ticketId: String): List<ServerInternalCommentSnapshot> =
        connection.prepareStatement(
            """
            SELECT c.id::text, c.ticket_id::text, c.author_id::text, u.name AS author_name, c.body, c.created_at
            FROM internal_comments c
            JOIN users u ON u.id = c.author_id
            WHERE c.ticket_id::text = ?
            ORDER BY c.created_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, ticketId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            ServerInternalCommentSnapshot(
                                id = resultSet.getString("id"),
                                ticketId = resultSet.getString("ticket_id"),
                                authorId = resultSet.getString("author_id"),
                                authorName = resultSet.getString("author_name"),
                                body = resultSet.getString("body"),
                                createdAt = resultSet.getTimestamp("created_at").toInstant().toString(),
                            ),
                        )
                    }
                }
            }
        }

    private fun ticketEvents(connection: Connection, ticketId: String): List<ServerTicketEventSnapshot> =
        connection.prepareStatement(
            """
            SELECT e.id::text, e.ticket_id::text, e.type, e.description,
                   COALESCE(u.name, 'Sistema') AS actor_name, e.created_at
            FROM ticket_events e
            LEFT JOIN users u ON u.id = e.actor_id
            WHERE e.ticket_id::text = ?
            ORDER BY e.created_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, ticketId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            ServerTicketEventSnapshot(
                                id = resultSet.getString("id"),
                                ticketId = resultSet.getString("ticket_id"),
                                type = resultSet.getString("type"),
                                description = resultSet.getString("description"),
                                actorName = resultSet.getString("actor_name"),
                                createdAt = resultSet.getTimestamp("created_at").toInstant().toString(),
                            ),
                        )
                    }
                }
            }
        }

    private fun ticketAttachments(connection: Connection, ticketId: String): List<ServerTicketAttachmentSnapshot> =
        connection.prepareStatement(
            """
            SELECT a.id::text, a.file_name, a.content_type, a.size_bytes,
                   u.name AS uploaded_by, a.uploaded_at
            FROM attachments a
            JOIN users u ON u.id = a.uploaded_by
            LEFT JOIN ticket_messages m ON m.id = a.message_id
            WHERE a.ticket_id::text = ? OR m.ticket_id::text = ?
            ORDER BY a.uploaded_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, ticketId)
            statement.setString(2, ticketId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            ServerTicketAttachmentSnapshot(
                                id = resultSet.getString("id"),
                                fileName = resultSet.getString("file_name"),
                                contentType = resultSet.getString("content_type"),
                                sizeBytes = resultSet.getLong("size_bytes"),
                                uploadedBy = resultSet.getString("uploaded_by"),
                                uploadedAt = resultSet.getTimestamp("uploaded_at").toInstant().toString(),
                            ),
                        )
                    }
                }
            }
        }

    // Batch variants of ticketMessages/ticketInternalComments/ticketEvents/ticketAttachments above,
    // used by getTickets() to avoid a 1+4N query pattern when hydrating a whole ticket list.
    private fun ticketMessagesByTicketIds(connection: Connection, ticketIds: List<String>): Map<String, List<ServerTicketMessageSnapshot>> {
        if (ticketIds.isEmpty()) return emptyMap()
        return connection.prepareStatement(
            """
            SELECT m.id::text, m.ticket_id::text, m.author_id::text, u.name AS author_name, m.body, m.created_at
            FROM ticket_messages m
            JOIN users u ON u.id = m.author_id
            WHERE m.ticket_id = ANY(?)
            ORDER BY m.created_at ASC
            """.trimIndent(),
        ).use { statement ->
            statement.setArray(1, connection.createArrayOf("uuid", ticketIds.toTypedArray()))
            statement.executeQuery().use { resultSet ->
                val grouped = linkedMapOf<String, MutableList<ServerTicketMessageSnapshot>>()
                while (resultSet.next()) {
                    val ticketId = resultSet.getString("ticket_id")
                    grouped.getOrPut(ticketId) { mutableListOf() }.add(
                        ServerTicketMessageSnapshot(
                            id = resultSet.getString("id"),
                            ticketId = ticketId,
                            authorId = resultSet.getString("author_id"),
                            authorName = resultSet.getString("author_name"),
                            body = resultSet.getString("body"),
                            createdAt = resultSet.getTimestamp("created_at").toInstant().toString(),
                        ),
                    )
                }
                grouped
            }
        }
    }

    private fun ticketInternalCommentsByTicketIds(connection: Connection, ticketIds: List<String>): Map<String, List<ServerInternalCommentSnapshot>> {
        if (ticketIds.isEmpty()) return emptyMap()
        return connection.prepareStatement(
            """
            SELECT c.id::text, c.ticket_id::text, c.author_id::text, u.name AS author_name, c.body, c.created_at
            FROM internal_comments c
            JOIN users u ON u.id = c.author_id
            WHERE c.ticket_id = ANY(?)
            ORDER BY c.created_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.setArray(1, connection.createArrayOf("uuid", ticketIds.toTypedArray()))
            statement.executeQuery().use { resultSet ->
                val grouped = linkedMapOf<String, MutableList<ServerInternalCommentSnapshot>>()
                while (resultSet.next()) {
                    val ticketId = resultSet.getString("ticket_id")
                    grouped.getOrPut(ticketId) { mutableListOf() }.add(
                        ServerInternalCommentSnapshot(
                            id = resultSet.getString("id"),
                            ticketId = ticketId,
                            authorId = resultSet.getString("author_id"),
                            authorName = resultSet.getString("author_name"),
                            body = resultSet.getString("body"),
                            createdAt = resultSet.getTimestamp("created_at").toInstant().toString(),
                        ),
                    )
                }
                grouped
            }
        }
    }

    private fun ticketEventsByTicketIds(connection: Connection, ticketIds: List<String>): Map<String, List<ServerTicketEventSnapshot>> {
        if (ticketIds.isEmpty()) return emptyMap()
        return connection.prepareStatement(
            """
            SELECT e.id::text, e.ticket_id::text, e.type, e.description,
                   COALESCE(u.name, 'Sistema') AS actor_name, e.created_at
            FROM ticket_events e
            LEFT JOIN users u ON u.id = e.actor_id
            WHERE e.ticket_id = ANY(?)
            ORDER BY e.created_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.setArray(1, connection.createArrayOf("uuid", ticketIds.toTypedArray()))
            statement.executeQuery().use { resultSet ->
                val grouped = linkedMapOf<String, MutableList<ServerTicketEventSnapshot>>()
                while (resultSet.next()) {
                    val ticketId = resultSet.getString("ticket_id")
                    grouped.getOrPut(ticketId) { mutableListOf() }.add(
                        ServerTicketEventSnapshot(
                            id = resultSet.getString("id"),
                            ticketId = ticketId,
                            type = resultSet.getString("type"),
                            description = resultSet.getString("description"),
                            actorName = resultSet.getString("actor_name"),
                            createdAt = resultSet.getTimestamp("created_at").toInstant().toString(),
                        ),
                    )
                }
                grouped
            }
        }
    }

    private fun ticketAttachmentsByTicketIds(connection: Connection, ticketIds: List<String>): Map<String, List<ServerTicketAttachmentSnapshot>> {
        if (ticketIds.isEmpty()) return emptyMap()
        return connection.prepareStatement(
            """
            SELECT COALESCE(a.ticket_id, m.ticket_id)::text AS resolved_ticket_id,
                   a.id::text, a.file_name, a.content_type, a.size_bytes,
                   u.name AS uploaded_by, a.uploaded_at
            FROM attachments a
            JOIN users u ON u.id = a.uploaded_by
            LEFT JOIN ticket_messages m ON m.id = a.message_id
            WHERE a.ticket_id = ANY(?) OR m.ticket_id = ANY(?)
            ORDER BY a.uploaded_at DESC
            """.trimIndent(),
        ).use { statement ->
            val idsArray = connection.createArrayOf("uuid", ticketIds.toTypedArray())
            statement.setArray(1, idsArray)
            statement.setArray(2, idsArray)
            statement.executeQuery().use { resultSet ->
                val grouped = linkedMapOf<String, MutableList<ServerTicketAttachmentSnapshot>>()
                while (resultSet.next()) {
                    val ticketId = resultSet.getString("resolved_ticket_id")
                    grouped.getOrPut(ticketId) { mutableListOf() }.add(
                        ServerTicketAttachmentSnapshot(
                            id = resultSet.getString("id"),
                            fileName = resultSet.getString("file_name"),
                            contentType = resultSet.getString("content_type"),
                            sizeBytes = resultSet.getLong("size_bytes"),
                            uploadedBy = resultSet.getString("uploaded_by"),
                            uploadedAt = resultSet.getTimestamp("uploaded_at").toInstant().toString(),
                        ),
                    )
                }
                grouped
            }
        }
    }

    private fun taskSnapshot(resultSet: ResultSet): ServerTaskSnapshot = ServerTaskSnapshot(
        id = resultSet.getString("id"),
        ownerAdminId = resultSet.getString("owner_admin_id"),
        createdByUserId = resultSet.getString("created_by_user_id"),
        createdByUserName = resultSet.getString("created_by_user_name"),
        title = resultSet.getString("title"),
        description = resultSet.getString("description") ?: "",
        clientId = resultSet.getString("client_id"),
        clientName = resultSet.getString("company_name"),
        labelId = resultSet.getString("label_id"),
        labelName = resultSet.getString("label_name"),
        labelColorHex = resultSet.getString("color_hex"),
        dueDate = resultSet.getString("due_date"),
        completed = resultSet.getBoolean("completed"),
        loggedMinutes = resultSet.getInt("logged_minutes"),
        loggedSeconds = resultSet.getInt("logged_seconds"),
        createdAt = formatTimestamp(resultSet.getObject("created_at")),
        updatedAt = formatTimestamp(resultSet.getObject("updated_at")),
    )

    private fun hydrateTaskSnapshot(connection: Connection, resultSet: ResultSet): ServerTaskSnapshot {
        val taskId = resultSet.getString("id")
        return connection.prepareStatement(
            """
            SELECT
                t.id::text AS id,
                t.owner_admin_id::text AS owner_admin_id,
                t.created_by_user_id::text AS created_by_user_id,
                creator.name AS created_by_user_name,
                t.title,
                t.description,
                t.client_id::text AS client_id,
                c.company_name,
                tl.id::text AS label_id,
                tl.name AS label_name,
                tl.color_hex,
                t.due_date::text AS due_date,
                t.completed,
                t.logged_minutes,
                t.logged_seconds,
                t.created_at,
                t.updated_at
            FROM tasks t
            JOIN task_labels tl ON tl.id = t.label_id
            JOIN users creator ON creator.id = t.created_by_user_id
            LEFT JOIN clients c ON c.id = t.client_id
            WHERE t.id::text = ?
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, taskId)
            statement.executeQuery().use { hydrated ->
                hydrated.next()
                taskSnapshot(hydrated)
            }
        }
    }

    private fun taskBoardPlacementSnapshot(resultSet: ResultSet) = ServerTaskBoardPlacementSnapshot(
        taskId = resultSet.getString("task_id"),
        boardDate = resultSet.getString("board_date"),
        positionX = resultSet.getDouble("position_x"),
        positionY = resultSet.getDouble("position_y"),
        createdAt = formatTimestamp(resultSet.getObject("created_at")),
        updatedAt = formatTimestamp(resultSet.getObject("updated_at")),
    )

    private fun timeLogSnapshot(resultSet: ResultSet): ServerTimeLogSnapshot = ServerTimeLogSnapshot(
        id = resultSet.getString("id"),
        ownerAdminId = resultSet.getString("owner_admin_id"),
        taskId = resultSet.getString("task_id"),
        clientId = resultSet.getString("client_id"),
        authorId = resultSet.getString("author_id"),
        authorName = resultSet.getString("author_name"),
        minutes = resultSet.getInt("minutes"),
        seconds = resultSet.getInt("seconds"),
        workDate = resultSet.getString("work_date"),
        note = resultSet.getString("note") ?: "",
        billable = resultSet.getBoolean("billable"),
        createdAt = formatTimestamp(resultSet.getObject("created_at")),
        sessionId = resultSet.getString("session_id"),
        isHistoricMigrated = resultSet.getBoolean("is_historic_migrated"),
    )

    private fun resolveAuthorName(connection: Connection, authorId: String): String =
        connection.prepareStatement(
            "SELECT name FROM users WHERE id::text = ? LIMIT 1",
        ).use { statement ->
            statement.setString(1, authorId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.getString("name") else "Admin"
            }
        }

    private fun getOpenTasksCount(connection: Connection, clientId: String, organizationId: String): Int =
        connection.prepareStatement(
            """
            SELECT COUNT(*)::integer AS open_tasks_count
            FROM tasks
            WHERE client_id::text = ?
              AND completed = FALSE
              AND (? IS NOT NULL AND organization_id::text = ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, organizationId)
            statement.setString(3, organizationId)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getInt("open_tasks_count")
            }
        }

    private fun getActiveTicketCount(connection: Connection, clientId: String): Int =
        connection.prepareStatement(
            "SELECT COUNT(*)::integer AS active_ticket_count FROM tickets WHERE client_id::text = ? AND status NOT IN ('RESOLVED', 'CLOSED')",
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getInt("active_ticket_count")
            }
        }

    private fun getClientMonthlyMinutes(connection: Connection, clientId: String, organizationId: String): Int =
        connection.prepareStatement(
            """
            SELECT COALESCE(SUM(tl.minutes), 0)::integer AS monthly_minutes
            FROM time_logs tl
            JOIN tasks t ON t.id = tl.task_id
            WHERE tl.client_id::text = ?
              AND (? IS NOT NULL AND t.organization_id::text = ?)
              AND DATE_TRUNC('month', tl.work_date) = DATE_TRUNC('month', CURRENT_DATE)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, organizationId)
            statement.setString(3, organizationId)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getInt("monthly_minutes")
            }
        }

    private fun findClientUserId(connection: Connection, clientId: String): String? =
        connection.prepareStatement(
            "SELECT id::text AS user_id FROM users WHERE client_id::text = ? AND role = 'CLIENT' LIMIT 1",
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.getString("user_id") else null
            }
        }

    private fun clientContactSnapshot(resultSet: ResultSet): ServerClientContactSnapshot =
        ServerClientContactSnapshot(
            id = resultSet.getString("id"),
            clientId = resultSet.getString("client_id"),
            fullName = resultSet.getString("full_name"),
            email = resultSet.getString("email"),
            phone = resultSet.getString("phone"),
            role = resultSet.getString("role"),
            isPrimary = resultSet.getBoolean("is_primary"),
            createdAt = formatTimestamp(resultSet.getObject("created_at")),
            updatedAt = formatTimestamp(resultSet.getObject("updated_at")),
        )

    private fun clientActivitySnapshot(
        connection: Connection,
        clientId: String,
        activityId: String,
    ): ServerClientActivitySnapshot = connection.prepareStatement(
        """
        SELECT a.id::text, a.client_id::text, a.contact_id::text, a.activity_type, a.subject, a.details,
               a.due_date, a.completed_at, creator.name AS created_by_name, a.created_at, a.updated_at
        FROM client_activities a
        JOIN users creator ON creator.id = a.created_by
        WHERE a.id::text = ? AND a.client_id::text = ?
        LIMIT 1
        """.trimIndent(),
    ).use { statement ->
        statement.setString(1, activityId)
        statement.setString(2, clientId)
        statement.executeQuery().use { resultSet ->
            if (!resultSet.next()) throw ServerNotFoundException("Client activity not found")
            clientActivitySnapshot(resultSet)
        }
    }

    private fun clientActivitySnapshot(resultSet: ResultSet): ServerClientActivitySnapshot =
        ServerClientActivitySnapshot(
            id = resultSet.getString("id"),
            clientId = resultSet.getString("client_id"),
            contactId = resultSet.getString("contact_id"),
            type = resultSet.getString("activity_type"),
            subject = resultSet.getString("subject"),
            details = resultSet.getString("details"),
            dueDate = resultSet.getObject("due_date")?.toString(),
            completedAt = resultSet.getObject("completed_at")?.let(::formatTimestamp),
            createdByName = resultSet.getString("created_by_name"),
            createdAt = formatTimestamp(resultSet.getObject("created_at")),
            updatedAt = formatTimestamp(resultSet.getObject("updated_at")),
        )

    private fun revokeRefreshTokens(connection: Connection, userId: String) {
        connection.prepareStatement(
            "UPDATE refresh_tokens SET revoked_at = NOW() WHERE user_id::text = ? AND revoked_at IS NULL",
        ).use { statement ->
            statement.setString(1, userId)
            statement.executeUpdate()
        }
    }

    private fun isEmailAssignedToAnotherUser(connection: Connection, email: String, currentUserId: String?): Boolean =
        connection.prepareStatement(
            "SELECT 1 FROM users WHERE email = ? AND (? IS NULL OR id::text <> ?) LIMIT 1",
        ).use { statement ->
            statement.setString(1, email)
            statement.setString(2, currentUserId)
            statement.setString(3, currentUserId)
            statement.executeQuery().use { it.next() }
        }

    private fun requireClientExists(connection: Connection, clientId: String, organizationId: String) {
        if (!recordExists(connection, "clients", clientId, organizationId)) {
            throw ServerNotFoundException("Client not found")
        }
    }

    /** Portal-only: [clientId] comes from the caller's own token, so no company scope is needed. */
    private fun requireOwnClientExists(connection: Connection, clientId: String) {
        val exists = connection.prepareStatement("SELECT 1 FROM clients WHERE id::text = ? LIMIT 1").use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { it.next() }
        }
        if (!exists) throw ServerNotFoundException("Client not found")
    }

    private fun requireClientContactExists(connection: Connection, clientId: String, contactId: String) {
        val exists = connection.prepareStatement(
            "SELECT 1 FROM client_contacts WHERE id::text = ? AND client_id::text = ? LIMIT 1",
        ).use { statement ->
            statement.setString(1, contactId)
            statement.setString(2, clientId)
            statement.executeQuery().use { it.next() }
        }
        if (!exists) throw ServerNotFoundException("Client contact not found")
    }

    private fun requireLabelExists(connection: Connection, labelId: String, organizationId: String) {
        if (!recordExists(connection, "task_labels", labelId, organizationId)) {
            throw ServerNotFoundException("Label not found")
        }
    }

    private fun requireTaskExists(connection: Connection, taskId: String, organizationId: String) {
        if (!recordExists(connection, "tasks", taskId, organizationId)) {
            throw ServerNotFoundException("Task not found")
        }
    }

    private fun requireTaskCreatorBelongsToOwner(
        connection: Connection,
        createdByUserId: String,
        organizationId: String,
    ) {
        val belongsToOwner = connection.prepareStatement(
            """
            SELECT 1
            FROM users creator
            LEFT JOIN clients client ON client.id = creator.client_id
            LEFT JOIN organization_members member ON member.user_id = creator.id
            WHERE creator.id::text = ?
              AND (
                  member.organization_id::text = ?
                  OR client.organization_id::text = ?
              )
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, createdByUserId)
            statement.setString(2, organizationId)
            statement.setString(3, organizationId)
            statement.executeQuery().use { it.next() }
        }
        if (!belongsToOwner) throw ServerNotFoundException("Task creator not found")
    }

    private fun recordExists(connection: Connection, tableName: String, id: String, organizationId: String): Boolean =
        connection.prepareStatement(
            """
            SELECT 1
            FROM $tableName
            WHERE id::text = ?
              AND (? IS NOT NULL AND organization_id::text = ?)
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, id)
            statement.setString(2, organizationId)
            statement.setString(3, organizationId)
            statement.executeQuery().use { resultSet -> resultSet.next() }
        }

    private fun hasLinkedTickets(connection: Connection, clientId: String): Boolean =
        connection.prepareStatement(
            "SELECT 1 FROM tickets WHERE client_id::text = ? LIMIT 1",
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { resultSet -> resultSet.next() }
        }

    private fun hasTasksForLabel(connection: Connection, labelId: String, organizationId: String): Boolean =
        connection.prepareStatement(
            "SELECT 1 FROM tasks WHERE label_id::text = ? AND (? IS NOT NULL AND organization_id::text = ?) LIMIT 1",
        ).use { statement ->
            statement.setString(1, labelId)
            statement.setString(2, organizationId)
            statement.setString(3, organizationId)
            statement.executeQuery().use { resultSet -> resultSet.next() }
        }

    private fun countTasksForLabel(connection: Connection, labelId: String, organizationId: String): Int =
        connection.prepareStatement(
            """
            SELECT COUNT(*)::integer AS tasks_count
            FROM tasks
            WHERE label_id::text = ?
              AND (? IS NOT NULL AND organization_id::text = ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, labelId)
            statement.setString(2, organizationId)
            statement.setString(3, organizationId)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getInt("tasks_count")
            }
        }

    private fun productCatalog(connection: Connection): List<ServerProductCatalogSnapshot> =
        connection.prepareStatement(
            """
            SELECT product_key, name, short_description, category, icon_key, monthly_price_cents,
                   currency, is_requestable, is_available,
                   COALESCE(ARRAY(SELECT jsonb_array_elements_text(capabilities)), ARRAY[]::text[]) AS capabilities
            FROM product_catalog
            WHERE product_key LIKE 'BUSINESS_%'
            ORDER BY sort_order, product_key
            """.trimIndent(),
        ).use { statement ->
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) add(productCatalogSnapshot(resultSet))
                }
            }
        }

    private fun productCatalogItem(connection: Connection, productKey: String): ServerProductCatalogSnapshot? =
        connection.prepareStatement(
            """
            SELECT product_key, name, short_description, category, icon_key, monthly_price_cents,
                   currency, is_requestable, is_available,
                   COALESCE(ARRAY(SELECT jsonb_array_elements_text(capabilities)), ARRAY[]::text[]) AS capabilities
            FROM product_catalog
            WHERE product_key = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, productKey)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) productCatalogSnapshot(resultSet) else null
            }
        }

    private fun productCatalogSnapshot(resultSet: ResultSet) = ServerProductCatalogSnapshot(
        key = resultSet.getString("product_key"),
        name = resultSet.getString("name"),
        shortDescription = resultSet.getString("short_description"),
        category = resultSet.getString("category"),
        iconKey = resultSet.getString("icon_key"),
        monthlyPriceCents = resultSet.getLong("monthly_price_cents"),
        currency = resultSet.getString("currency"),
        isRequestable = resultSet.getBoolean("is_requestable"),
        isAvailable = resultSet.getBoolean("is_available"),
        capabilities = resultSet.textArray("capabilities"),
    )

    private fun clientProductSubscriptions(
        connection: Connection,
        clientId: String,
    ): List<ServerClientProductSubscriptionSnapshot> = connection.prepareStatement(
        """
        SELECT product_key, status, monthly_price_cents, currency, starts_on, ends_on
        FROM client_product_subscriptions
        WHERE client_id = CAST(? AS uuid)
        ORDER BY product_key
        """.trimIndent(),
    ).use { statement ->
        statement.setString(1, clientId)
        statement.executeQuery().use { resultSet ->
            buildList {
                while (resultSet.next()) add(clientProductSubscriptionSnapshot(resultSet))
            }
        }
    }

    private fun clientProductSubscriptionSnapshot(resultSet: ResultSet) = ServerClientProductSubscriptionSnapshot(
        productKey = resultSet.getString("product_key"),
        status = resultSet.getString("status"),
        monthlyPriceCents = resultSet.getLong("monthly_price_cents"),
        currency = resultSet.getString("currency"),
        startsOn = resultSet.getObject("starts_on", LocalDate::class.java).toString(),
        endsOn = resultSet.getObject("ends_on", LocalDate::class.java)?.toString(),
    )

    private fun clientProgramRequests(
        connection: Connection,
        clientId: String? = null,
        // Null only for a portal user reading its own client's requests (clientId from its token).
        organizationId: String? = null,
        status: String? = null,
    ): List<ServerClientProgramRequestSnapshot> = connection.prepareStatement(
        """
        SELECT request.id::text, request.client_id::text, client.company_name, request.product_key,
               request.status, request.customer_note, request.admin_note,
               requester.name AS requested_by_name, request.requested_at, request.decided_at,
               request.quoted_monthly_price_cents, request.currency
        FROM client_program_requests request
        JOIN clients client ON client.id = request.client_id
        JOIN users requester ON requester.id = request.requested_by_user_id
        WHERE (? IS NULL OR request.client_id = CAST(? AS uuid))
          AND (? IS NULL OR client.organization_id = CAST(? AS uuid))
          AND (? IS NULL OR request.status = ?)
        ORDER BY request.requested_at DESC, request.id DESC
        """.trimIndent(),
    ).also {
        require(clientId != null || organizationId != null) { "Program requests need a client or a company scope" }
    }.use { statement ->
        statement.setString(1, clientId)
        statement.setString(2, clientId)
        statement.setString(3, organizationId)
        statement.setString(4, organizationId)
        statement.setString(5, status)
        statement.setString(6, status)
        statement.executeQuery().use { resultSet ->
            buildList {
                while (resultSet.next()) add(clientProgramRequestSnapshot(resultSet))
            }
        }
    }

    private fun clientProgramRequest(
        connection: Connection,
        requestId: String,
    ): ServerClientProgramRequestSnapshot? = connection.prepareStatement(
        """
        SELECT request.id::text, request.client_id::text, client.company_name, request.product_key,
               request.status, request.customer_note, request.admin_note,
               requester.name AS requested_by_name, request.requested_at, request.decided_at,
               request.quoted_monthly_price_cents, request.currency
        FROM client_program_requests request
        JOIN clients client ON client.id = request.client_id
        JOIN users requester ON requester.id = request.requested_by_user_id
        WHERE request.id = CAST(? AS uuid)
        """.trimIndent(),
    ).use { statement ->
        statement.setString(1, requestId)
        statement.executeQuery().use { resultSet ->
            if (resultSet.next()) clientProgramRequestSnapshot(resultSet) else null
        }
    }

    private fun clientProgramRequestForOwner(
        connection: Connection,
        requestId: String,
        organizationId: String,
        lockForUpdate: Boolean,
    ): ServerClientProgramRequestSnapshot? = connection.prepareStatement(
        """
        SELECT request.id::text, request.client_id::text, client.company_name, request.product_key,
               request.status, request.customer_note, request.admin_note,
               requester.name AS requested_by_name, request.requested_at, request.decided_at,
               request.quoted_monthly_price_cents, request.currency
        FROM client_program_requests request
        JOIN clients client ON client.id = request.client_id
        JOIN users requester ON requester.id = request.requested_by_user_id
        WHERE request.id = CAST(? AS uuid)
          AND client.organization_id = CAST(? AS uuid)
        ${if (lockForUpdate) "FOR UPDATE OF request" else ""}
        """.trimIndent(),
    ).use { statement ->
        statement.setString(1, requestId)
        statement.setString(2, organizationId)
        statement.executeQuery().use { resultSet ->
            if (resultSet.next()) clientProgramRequestSnapshot(resultSet) else null
        }
    }

    private fun clientProgramRequestSnapshot(resultSet: ResultSet) = ServerClientProgramRequestSnapshot(
        id = resultSet.getString("id"),
        clientId = resultSet.getString("client_id"),
        clientCompanyName = resultSet.getString("company_name"),
        productKey = resultSet.getString("product_key"),
        status = resultSet.getString("status"),
        customerNote = resultSet.getString("customer_note"),
        adminNote = resultSet.getString("admin_note"),
        requestedByName = resultSet.getString("requested_by_name"),
        requestedAt = formatTimestamp(resultSet.getObject("requested_at")),
        decidedAt = resultSet.getObject("decided_at")?.let(::formatTimestamp),
        quotedMonthlyPriceCents = resultSet.getLong("quoted_monthly_price_cents").takeUnless { resultSet.wasNull() },
        currency = resultSet.getString("currency"),
    )

    private fun requireActiveClientPortalUser(connection: Connection, clientId: String, userId: String) {
        val exists = connection.prepareStatement(
            """
            SELECT 1
            FROM clients client
            JOIN users portal_user ON portal_user.client_id = client.id
            WHERE client.id = CAST(? AS uuid)
              AND client.account_status = 'ACTIVE'
              AND portal_user.id = CAST(? AS uuid)
              AND portal_user.role = 'CLIENT'
              AND portal_user.is_active = TRUE
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, userId)
            statement.executeQuery().use { it.next() }
        }
        if (!exists) throw ServerNotFoundException("Active client portal account not found")
    }

    private fun hasActiveProductSubscription(connection: Connection, clientId: String, productKey: String): Boolean =
        connection.prepareStatement(
            """
            SELECT 1
            FROM client_product_subscriptions
            WHERE client_id = CAST(? AS uuid) AND product_key = ? AND status = 'ACTIVE'
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, productKey)
            statement.executeQuery().use { it.next() }
        }

    private fun hasPendingProgramRequest(connection: Connection, clientId: String, productKey: String): Boolean =
        connection.prepareStatement(
            """
            SELECT 1
            FROM client_program_requests
            WHERE client_id = CAST(? AS uuid) AND product_key = ? AND status = 'REQUESTED'
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, productKey)
            statement.executeQuery().use { it.next() }
        }

    private fun clientProductSubscriptionStatus(connection: Connection, clientId: String, productKey: String): String? =
        connection.prepareStatement(
            """
            SELECT status FROM client_product_subscriptions
            WHERE client_id = CAST(? AS uuid) AND product_key = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, productKey)
            statement.executeQuery().use { resultSet -> if (resultSet.next()) resultSet.getString("status") else null }
        }

    private fun syncLegacyServiceSubscription(
        connection: Connection,
        clientId: String,
        isEnabled: Boolean,
        organizationId: String,
    ) {
        if (isEnabled) {
            connection.prepareStatement(
                """
                INSERT INTO client_product_subscriptions (
                    client_id, product_key, status, monthly_price_cents, currency,
                    starts_on, ends_on, approved_by_user_id, approved_at
                ) VALUES (CAST(? AS uuid), 'SERVICE_SLA', 'ACTIVE', 0, 'EUR', CURRENT_DATE, NULL, $ORGANIZATION_PRIMARY_ADMIN, NOW())
                ON CONFLICT (client_id, product_key) DO UPDATE
                SET status = 'ACTIVE', ends_on = NULL,
                    approved_by_user_id = COALESCE(EXCLUDED.approved_by_user_id, client_product_subscriptions.approved_by_user_id),
                    approved_at = NOW()
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, clientId)
                statement.setString(2, organizationId)
                statement.executeUpdate()
            }
        } else {
            connection.prepareStatement(
                """
                UPDATE client_product_subscriptions
                SET status = 'CANCELLED', ends_on = CURRENT_DATE
                WHERE client_id = CAST(? AS uuid) AND product_key = 'SERVICE_SLA' AND status <> 'CANCELLED'
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, clientId)
                statement.executeUpdate()
            }
        }
    }

    private fun recordSubscriptionEvent(
        connection: Connection,
        clientId: String,
        productKey: String,
        requestId: String?,
        actorUserId: String?,
        action: String,
        previousStatus: String?,
        newStatus: String,
        monthlyPriceCents: Long?,
    ) {
        connection.prepareStatement(
            """
            INSERT INTO client_subscription_events (
                client_id, product_key, request_id, actor_user_id, action,
                previous_status, new_status, monthly_price_cents, currency
            ) VALUES (
                CAST(? AS uuid), ?, CAST(? AS uuid), CAST(? AS uuid), ?, ?, ?, ?, 'EUR'
            )
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, productKey)
            statement.setString(3, requestId)
            statement.setString(4, actorUserId)
            statement.setString(5, action)
            statement.setString(6, previousStatus)
            statement.setString(7, newStatus)
            if (monthlyPriceCents == null) statement.setNull(8, java.sql.Types.BIGINT) else statement.setLong(8, monthlyPriceCents)
            statement.executeUpdate()
        }
    }

    private fun normalizeHex(raw: String): String {
        val trimmed = raw.trim().removePrefix("#").uppercase()
        return "#${trimmed.take(6).padEnd(6, '0')}"
    }

    private fun PreparedStatement.bindInstant(index: Int, value: Instant) {
        setTimestamp(index, Timestamp.from(value))
    }

    private fun formatTimestamp(value: Any?): String {
        val instant = when (value) {
            is Instant -> value
            is java.sql.Timestamp -> value.toInstant()
            is java.time.OffsetDateTime -> value.toInstant()
            is java.time.LocalDateTime -> value.toInstant(ZoneOffset.UTC)
            else -> return ""
        }
        return DateTimeFormatter.ISO_INSTANT.format(instant)
    }

    private companion object {
        const val DEFAULT_ADMIN_OWNER_ID = "22222222-2222-2222-2222-222222222222"
        const val UNIQUE_VIOLATION_SQL_STATE = "23505"
        const val CLIENT_BILLING_COLUMNS = "c.billing_legal_name, c.billing_tax_id, c.billing_address_line, " +
            "c.billing_postal_code, c.billing_city, c.billing_province, c.billing_country, c.billing_email"
    }
}
