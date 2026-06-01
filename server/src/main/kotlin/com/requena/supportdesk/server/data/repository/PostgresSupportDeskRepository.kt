package com.requena.supportdesk.server.data.repository

import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import com.requena.supportdesk.server.domain.model.AddInternalCommentRequest
import com.requena.supportdesk.server.domain.model.AddTicketTimeEntryRequest
import com.requena.supportdesk.server.domain.model.ChangeTicketAssigneeRequest
import com.requena.supportdesk.server.domain.model.ClientAccessCodeClaimRequest
import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.CreateInvoiceRequest
import com.requena.supportdesk.server.domain.model.CreateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.CreateTaskRequest
import com.requena.supportdesk.server.domain.model.CreateTicketRequest
import com.requena.supportdesk.server.domain.model.CreateTimeLogRequest
import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.model.ServerAttachmentCreated
import com.requena.supportdesk.server.domain.model.ServerAttachmentSnapshot
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.model.ServerClientSnapshot
import com.requena.supportdesk.server.domain.model.InternalComment
import com.requena.supportdesk.server.domain.model.ServerConflictException
import com.requena.supportdesk.server.domain.model.ServerInvoiceItemSnapshot
import com.requena.supportdesk.server.domain.model.ServerInvoiceSnapshot
import com.requena.supportdesk.server.domain.model.UpdateInvoiceStatusRequest
import com.requena.supportdesk.server.domain.model.ServerDailyMinutesSnapshot
import com.requena.supportdesk.server.domain.model.ServerDashboardSnapshot
import com.requena.supportdesk.server.domain.model.ServerDeviceRegistration
import com.requena.supportdesk.server.domain.model.ServerNotFoundException
import com.requena.supportdesk.server.domain.model.ServerNotificationAlertSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskLabelSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskSnapshot
import com.requena.supportdesk.server.domain.model.ServerTicketFieldUpdate
import com.requena.supportdesk.server.domain.model.TicketTimeEntry
import com.requena.supportdesk.server.domain.model.ServerTicketSnapshot
import com.requena.supportdesk.server.domain.model.ServerTimeLogSnapshot
import com.requena.supportdesk.server.domain.model.UpdateClientRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketPriorityRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketStatusRequest
import com.requena.supportdesk.server.domain.model.UploadAttachmentRequest
import com.requena.supportdesk.server.domain.repository.SupportDeskRepository
import com.requena.supportdesk.server.security.PasswordHasher
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

class PostgresSupportDeskRepository(
    private val dataSource: PostgresSupportDeskDataSource,
) : SupportDeskRepository {
    private data class PortalAccessCode(
        val code: String?,
        val status: String,
        val expiresAt: String?,
    )

    private data class ClaimableClientAccess(
        val codeId: String,
        val clientId: String,
        val clientName: String,
        val clientEmail: String,
        val companyName: String = "",
    )

    override fun authenticate(email: String, password: String): ServerAuthIdentity? = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT u.id::text AS user_id, u.name, u.email::text AS email, u.role,
                   u.client_id::text AS client_id, u.password_hash,
                   COALESCE(c.company_name, '') AS company_name
            FROM users u
            LEFT JOIN clients c ON c.id = u.client_id
            WHERE u.email = ? AND u.is_active = TRUE
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, email.trim())
            statement.executeQuery().use { resultSet ->
                val storedHash = if (resultSet.next()) resultSet.getString("password_hash") else null
                if (storedHash != null && PasswordHasher.verifyPassword(password, storedHash)) {
                    val userId = resultSet.getString("user_id")
                    if (PasswordHasher.isLegacyPasswordHash(storedHash)) {
                        updatePasswordHash(connection, userId, PasswordHasher.hashPassword(password))
                    }
                    updateLastLogin(connection, userId)
                    ServerAuthIdentity(
                        userId = userId,
                        name = resultSet.getString("name"),
                        email = resultSet.getString("email"),
                        role = resultSet.getString("role"),
                        clientId = resultSet.getString("client_id"),
                        companyName = resultSet.getString("company_name") ?: "",
                    )
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

    override fun claimClientAccessCode(request: ClientAccessCodeClaimRequest): ServerAuthIdentity? = dataSource.withConnection { connection ->
        val codeHash = PasswordHasher.hashToken(request.code.trim().uppercase())
        val requestedEmail = request.email.trim()
        connection.autoCommit = false
        try {
            val access = connection.prepareStatement(
                """
                SELECT
                    cac.id::text AS code_id,
                    cac.client_id::text AS client_id,
                    COALESCE(NULLIF(c.contact_name, ''), c.company_name) AS client_name,
                    c.email::text AS client_email,
                    c.company_name
                FROM client_access_codes cac
                JOIN clients c ON c.id = cac.client_id
                WHERE cac.code_hash = ?
                  AND LOWER(c.email::text) = LOWER(?)
                  AND cac.expires_at > NOW()
                  AND c.account_status = 'ACTIVE'
                LIMIT 1
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, codeHash)
                statement.setString(2, requestedEmail)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        ClaimableClientAccess(
                            codeId = resultSet.getString("code_id"),
                            clientId = resultSet.getString("client_id"),
                            clientName = resultSet.getString("client_name"),
                            clientEmail = resultSet.getString("client_email"),
                            companyName = resultSet.getString("company_name") ?: "",
                        )
                    } else {
                        null
                    }
                }
            } ?: run {
                connection.rollback()
                return@withConnection null
            }

            val passwordHash = PasswordHasher.hashPassword(request.code.trim().uppercase())
            val identity = connection.prepareStatement(
                """
                INSERT INTO users (client_id, name, email, password_hash, role, is_active)
                VALUES (CAST(? AS uuid), ?, ?, ?, 'CLIENT', TRUE)
                ON CONFLICT (email) DO UPDATE
                SET client_id = EXCLUDED.client_id,
                    name = EXCLUDED.name,
                    password_hash = EXCLUDED.password_hash,
                    role = 'CLIENT',
                    is_active = TRUE
                WHERE users.role = 'CLIENT'
                RETURNING id::text AS user_id, name, email::text AS email, role, client_id::text AS client_id
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, access.clientId)
                statement.setString(2, access.clientName)
                statement.setString(3, access.clientEmail)
                statement.setString(4, passwordHash)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        ServerAuthIdentity(
                            userId = resultSet.getString("user_id"),
                            name = resultSet.getString("name"),
                            email = resultSet.getString("email"),
                            role = resultSet.getString("role"),
                            clientId = resultSet.getString("client_id"),
                            companyName = access.companyName,
                        )
                    } else {
                        null
                    }
                }
            } ?: run {
                connection.rollback()
                return@withConnection null
            }

            connection.prepareStatement(
                "UPDATE client_access_codes SET used_at = COALESCE(used_at, NOW()) WHERE id::text = ?",
            ).use { statement ->
                statement.setString(1, access.codeId)
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

    override fun createClientAccessCode(clientId: String, ownerAdminId: String, expiresInDays: Int): String = dataSource.withConnection { connection ->
        requireClientExists(connection, clientId, ownerAdminId)
        createPortalAccessCode(connection, clientId, ownerAdminId, expiresInDays).code.orEmpty()
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
                       COALESCE(c.company_name, '') AS company_name
                FROM refresh_tokens rt
                JOIN users u ON u.id = rt.user_id
                LEFT JOIN clients c ON c.id = u.client_id
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
                        ServerAuthIdentity(
                            userId = resultSet.getString("user_id"),
                            name = resultSet.getString("name"),
                            email = resultSet.getString("email"),
                            role = resultSet.getString("role"),
                            clientId = resultSet.getString("client_id"),
                            companyName = resultSet.getString("company_name") ?: "",
                        )
                    } else {
                        null
                    }
                }
            } ?: run {
                if (revokeRefreshTokenFamilyOnReuse(connection, refreshTokenHash)) {
                    connection.commit()
                } else {
                    connection.rollback()
                }
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

    private fun revokeRefreshTokenFamilyOnReuse(connection: Connection, refreshTokenHash: String): Boolean {
        val userId = connection.prepareStatement(
            "SELECT user_id::text AS user_id FROM refresh_tokens WHERE token_hash = ? LIMIT 1",
        ).use { statement ->
            statement.setString(1, refreshTokenHash)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.getString("user_id") else null
            }
        } ?: return false

        connection.prepareStatement(
            "UPDATE refresh_tokens SET revoked_at = COALESCE(revoked_at, NOW()) WHERE user_id::text = ?",
        ).use { statement ->
            statement.setString(1, userId)
            statement.executeUpdate()
        }
        return true
    }

    override fun getTickets(ownerAdminId: String?, clientId: String?, viewerUserId: String?, limit: Int, offset: Int): List<ServerTicketSnapshot> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT
                t.id::text,
                t.client_id::text AS client_id,
                t.ticket_number,
                t.subject,
                t.description,
                t.category,
                t.affected_app,
                t.platform,
                t.app_version,
                t.client_reference,
                t.status,
                t.priority,
                t.waiting_on,
                t.resolution_summary,
                t.requester_id::text AS requester_id,
                requester.name AS requester_name,
                requester.email::text AS requester_email,
                t.assignee_id::text AS assignee_id,
                assignee.name AS assignee_name,
                t.created_at,
                t.updated_at,
                t.client_accepted_close_at,
                t.admin_accepted_close_at,
                t.archived_at,
                t.satisfaction_rating
            FROM tickets t
            JOIN clients c ON c.id = t.client_id
            JOIN users requester ON requester.id = t.requester_id
            LEFT JOIN users assignee ON assignee.id = t.assignee_id
            WHERE (? IS NULL OR c.owner_admin_id::text = ?)
              AND (? IS NULL OR t.client_id::text = ?)
            ORDER BY t.updated_at DESC, t.created_at DESC
            LIMIT ? OFFSET ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, ownerAdminId)
            statement.setString(2, ownerAdminId)
            statement.setString(3, clientId)
            statement.setString(4, clientId)
            statement.setInt(5, limit.coerceIn(1, 200))
            statement.setInt(6, offset.coerceAtLeast(0))
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(ticketSnapshot(resultSet))
                    }
                }
            }
        }
    }

    override fun countClientTicketsCreatedOn(clientId: String, datePrefix: String, priority: String?): Int = dataSource.withConnection { connection ->
        val priorityClause = if (priority != null) "AND UPPER(priority) = ?" else ""
        connection.prepareStatement(
            """
            SELECT COUNT(*)::integer AS ticket_count
            FROM tickets
            WHERE client_id::text = ?
              AND created_at >= CAST(? AS date)
              AND created_at < CAST(? AS date) + INTERVAL '1 day'
              $priorityClause
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, datePrefix)
            statement.setString(3, datePrefix)
            if (priority != null) statement.setString(4, priority.uppercase())
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getInt("ticket_count")
            }
        }
    }

    override fun getTicket(id: String, ownerAdminId: String?, clientId: String?, viewerUserId: String?): ServerTicketSnapshot? = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT
                t.id::text,
                t.client_id::text AS client_id,
                t.ticket_number,
                t.subject,
                t.description,
                t.category,
                t.affected_app,
                t.platform,
                t.app_version,
                t.client_reference,
                t.status,
                t.priority,
                t.waiting_on,
                t.resolution_summary,
                t.requester_id::text AS requester_id,
                requester.name AS requester_name,
                requester.email::text AS requester_email,
                t.assignee_id::text AS assignee_id,
                assignee.name AS assignee_name,
                t.created_at,
                t.updated_at,
                t.client_accepted_close_at,
                t.admin_accepted_close_at,
                t.archived_at,
                t.satisfaction_rating
            FROM tickets t
            JOIN clients c ON c.id = t.client_id
            JOIN users requester ON requester.id = t.requester_id
            LEFT JOIN users assignee ON assignee.id = t.assignee_id
            WHERE (t.id::text = ? OR t.ticket_number = ?)
              AND (? IS NULL OR c.owner_admin_id::text = ?)
              AND (? IS NULL OR t.client_id::text = ?)
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, id)
            statement.setString(2, id)
            statement.setString(3, ownerAdminId)
            statement.setString(4, ownerAdminId)
            statement.setString(5, clientId)
            statement.setString(6, clientId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) ticketSnapshot(resultSet) else null
            }
        }
    }

    override fun createTicket(request: CreateTicketRequest, ownerAdminId: String?, requesterId: String?): ServerTicketSnapshot = dataSource.withConnection { connection ->
        if (ownerAdminId != null) {
            requireClientExists(connection, request.clientId, ownerAdminId)
        }
        val resolvedOwnerAdminId = ownerAdminId ?: ownerAdminIdForClient(connection, request.clientId)
        val resolvedRequesterId = requesterId
            ?: request.requesterId
            ?: findRequesterId(connection, request.clientId)
            ?: resolvedOwnerAdminId
        val affectedApp = request.affectedApp.ifBlank { findAffectedApp(connection, request.clientId) }

        connection.prepareStatement(
            """
            INSERT INTO tickets (
                client_id, requester_id, assignee_id, subject, description, category, affected_app,
                platform, app_version, steps_to_reproduce, client_reference, status, priority, waiting_on
            )
            VALUES (
                CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), ?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', ?, 'ADMIN'
            )
            RETURNING id::text, ticket_number, subject, description, category, affected_app, platform,
                      app_version, client_reference, status, priority, waiting_on, resolution_summary
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, request.clientId)
            statement.setString(2, resolvedRequesterId)
            statement.setString(3, resolvedOwnerAdminId)
            statement.setString(4, request.subject)
            statement.setString(5, request.description)
            statement.setString(6, request.category)
            statement.setString(7, affectedApp)
            statement.setString(8, request.platform)
            statement.setString(9, request.appVersion)
            statement.setString(10, request.stepsToReproduce)
            statement.setString(11, request.clientReference)
            statement.setString(12, request.priority)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                val ticketId = resultSet.getString("id")
                insertEvent(connection, ticketId, resolvedRequesterId, "TICKET_CREATED", "Ticket created")
                insertAlert(
                    connection = connection,
                    userId = resolvedOwnerAdminId,
                    ticketId = ticketId,
                    type = "NEW_TICKET",
                    title = "Nuevo ticket",
                    body = request.subject,
                )
                getTicket(ticketId, ownerAdminId, null, ownerAdminId) ?: throw ServerNotFoundException("Ticket not found")
            }
        }
    }

    override fun deleteTicket(ticketId: String, ownerAdminId: String?) {
        dataSource.withConnection { connection ->
            connection.prepareStatement(
                """
                DELETE FROM tickets
                WHERE id::text = ?
                  AND (? IS NULL OR client_id IN (
                      SELECT id FROM clients WHERE owner_admin_id::text = ?
                  ))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, ticketId)
                statement.setString(2, ownerAdminId)
                statement.setString(3, ownerAdminId)
                if (statement.executeUpdate() == 0) {
                    throw ServerNotFoundException("Ticket not found")
                }
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
                    resultSet.next()
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
                    resultSet.next()
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
                    CAST(? AS uuid),
                    NULL,
                    ?, ?, ?, ?, CAST(? AS uuid)
                )
                RETURNING id::text
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, ticketId)
                statement.setString(2, request.fileName)
                statement.setString(3, request.contentType)
                statement.setString(4, request.storageKey)
                statement.setLong(5, request.sizeBytes)
                statement.setString(6, request.uploadedBy)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    insertEvent(connection, ticketId, request.uploadedBy, "ATTACHMENT_ADDED", "Attachment metadata stored")
                    ServerAttachmentCreated(ticketId = ticketId, attachmentId = resultSet.getString("id"))
                }
            }
        }

    override fun acceptTicketClose(ticketId: String, actorId: String, actorRole: String, resolutionSummary: String?): ServerTicketSnapshot =
        dataSource.withConnection { connection ->
            connection.prepareStatement(
                """
                UPDATE tickets
                SET admin_accepted_close_at = CASE WHEN ? = 'ADMIN' THEN COALESCE(admin_accepted_close_at, NOW()) ELSE admin_accepted_close_at END,
                    client_accepted_close_at = CASE WHEN ? = 'CLIENT' THEN COALESCE(client_accepted_close_at, NOW()) ELSE client_accepted_close_at END,
                    resolution_summary = COALESCE(?, resolution_summary),
                    status = CASE
                        WHEN (? = 'ADMIN' AND client_accepted_close_at IS NOT NULL)
                          OR (? = 'CLIENT' AND admin_accepted_close_at IS NOT NULL)
                        THEN 'CLOSED'
                        ELSE 'RESOLVED'
                    END,
                    archived_at = CASE
                        WHEN (? = 'ADMIN' AND client_accepted_close_at IS NOT NULL)
                          OR (? = 'CLIENT' AND admin_accepted_close_at IS NOT NULL)
                        THEN COALESCE(archived_at, NOW())
                        ELSE archived_at
                    END
                WHERE id::text = ?
                RETURNING id::text
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, actorRole)
                statement.setString(2, actorRole)
                statement.setString(3, resolutionSummary)
                statement.setString(4, actorRole)
                statement.setString(5, actorRole)
                statement.setString(6, actorRole)
                statement.setString(7, actorRole)
                statement.setString(8, ticketId)
                statement.executeQuery().use { resultSet ->
                    if (!resultSet.next()) throw ServerNotFoundException("Ticket not found")
                }
            }
            insertEvent(connection, ticketId, actorId, "CLOSE_ACCEPTED", "$actorRole accepted ticket closure")
            getTicket(ticketId, null, null, null) ?: throw ServerNotFoundException("Ticket not found")
        }

    override fun rateTicket(ticketId: String, clientId: String, rating: Int): ServerTicketSnapshot = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            UPDATE tickets
            SET satisfaction_rating = ?,
                updated_at = NOW()
            WHERE id::text = ?
              AND client_id::text = ?
            RETURNING id::text
            """.trimIndent(),
        ).use { statement ->
            statement.setInt(1, rating.coerceIn(1, 5))
            statement.setString(2, ticketId)
            statement.setString(3, clientId)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) throw ServerNotFoundException("Ticket not found")
            }
        }
        getTicket(ticketId, null, clientId, null) ?: throw ServerNotFoundException("Ticket not found")
    }

    override fun getClients(ownerAdminId: String?): List<ServerClientSnapshot> = dataSource.withConnection { connection ->
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
                      AND (? IS NULL OR ts.owner_admin_id::text = ?)
                ) AS open_tasks_count,
                (
                    SELECT COALESCE(SUM(tl.minutes), 0)::integer
                    FROM time_logs tl
                    JOIN tasks ts ON ts.id = tl.task_id
                    WHERE tl.client_id = c.id
                      AND (? IS NULL OR ts.owner_admin_id::text = ?)
                      AND DATE_TRUNC('month', tl.work_date) = DATE_TRUNC('month', CURRENT_DATE)
                ) AS monthly_logged_minutes,
                pac.code_plain AS portal_access_code,
                CASE
                    WHEN pac.code_plain IS NOT NULL THEN 'ACTIVE'
                    WHEN latest_code.expires_at IS NOT NULL THEN 'EXPIRED'
                    ELSE 'MISSING'
                END AS portal_access_status,
                pac.expires_at AS portal_access_expires_at
            FROM clients c
            LEFT JOIN LATERAL (
                SELECT code_plain, expires_at
                FROM client_access_codes
                WHERE client_id = c.id
                  AND code_plain IS NOT NULL
                  AND expires_at > NOW()
                ORDER BY created_at DESC
                LIMIT 1
            ) pac ON TRUE
            LEFT JOIN LATERAL (
                SELECT expires_at
                FROM client_access_codes
                WHERE client_id = c.id
                  AND code_plain IS NOT NULL
                ORDER BY expires_at DESC
                LIMIT 1
            ) latest_code ON TRUE
            WHERE (? IS NULL OR c.owner_admin_id::text = ?)
            ORDER BY c.company_name ASC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, ownerAdminId)
            statement.setString(2, ownerAdminId)
            statement.setString(3, ownerAdminId)
            statement.setString(4, ownerAdminId)
            statement.setString(5, ownerAdminId)
            statement.setString(6, ownerAdminId)
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
                                preferredContactChannel = resultSet.getString("preferred_contact_channel"),
                                activeTicketCount = resultSet.getInt("active_ticket_count"),
                                openTasksCount = resultSet.getInt("open_tasks_count"),
                                monthlyLoggedMinutes = resultSet.getInt("monthly_logged_minutes"),
                                portalAccessCode = resultSet.getString("portal_access_code"),
                                portalAccessStatus = resultSet.getString("portal_access_status"),
                                portalAccessExpiresAt = formatNullableTimestamp(resultSet.getObject("portal_access_expires_at")),
                            ),
                        )
                    }
                }
            }
        }
    }

    override fun createClient(request: CreateClientRequest, ownerAdminId: String?): ServerClientSnapshot = dataSource.withConnection { connection ->
        val resolvedOwnerAdminId = requireNotNull(ownerAdminId) { "ownerAdminId is required" }
        connection.autoCommit = false
        try {
            val client = connection.prepareStatement(
                """
                INSERT INTO clients (
                    owner_admin_id, company_name, product_name, contact_name, email, account_status, service_tier, preferred_contact_channel
                )
                VALUES (CAST(? AS uuid), ?, ?, ?, ?, ?, ?, ?)
                RETURNING id::text, owner_admin_id::text, company_name, product_name, contact_name, email::text, account_status, service_tier, preferred_contact_channel
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, resolvedOwnerAdminId)
                statement.setString(2, request.companyName)
                statement.setString(3, request.productName)
                statement.setString(4, request.contactName)
                statement.setString(5, request.email)
                statement.setString(6, request.accountStatus)
                statement.setString(7, request.serviceTier)
                statement.setString(8, request.preferredContactChannel)
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
                        preferredContactChannel = resultSet.getString("preferred_contact_channel"),
                        activeTicketCount = 0,
                        openTasksCount = 0,
                        monthlyLoggedMinutes = 0,
                    )
                }
            }
            val accessCode = createPortalAccessCode(connection, client.id, resolvedOwnerAdminId, DEFAULT_CLIENT_ACCESS_CODE_DAYS)
            connection.commit()
            client.copy(
                portalAccessCode = accessCode.code,
                portalAccessStatus = accessCode.status,
                portalAccessExpiresAt = accessCode.expiresAt,
            )
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = true
        }
    }

    override fun updateClient(
        clientId: String,
        request: UpdateClientRequest,
        ownerAdminId: String?,
    ): ServerClientSnapshot = dataSource.withConnection { connection ->
        requireClientExists(connection, clientId, ownerAdminId)
        connection.prepareStatement(
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
              AND (? IS NULL OR owner_admin_id::text = ?)
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
            statement.setString(9, ownerAdminId)
            statement.setString(10, ownerAdminId)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) {
                    throw ServerNotFoundException("Client not found")
                }
                val portalAccess = portalAccessForClient(connection, clientId)
                ServerClientSnapshot(
                    id = resultSet.getString("id"),
                    ownerAdminId = resultSet.getString("owner_admin_id"),
                    companyName = resultSet.getString("company_name"),
                    productName = resultSet.getString("product_name"),
                    contactName = resultSet.getString("contact_name"),
                    email = resultSet.getString("email"),
                    accountStatus = resultSet.getString("account_status"),
                    serviceTier = resultSet.getString("service_tier"),
                    preferredContactChannel = resultSet.getString("preferred_contact_channel"),
                    activeTicketCount = getActiveTicketCount(connection, clientId),
                    openTasksCount = getOpenTasksCount(connection, clientId, ownerAdminId),
                    monthlyLoggedMinutes = getClientMonthlyMinutes(connection, clientId, ownerAdminId),
                    portalAccessCode = portalAccess.code,
                    portalAccessStatus = portalAccess.status,
                    portalAccessExpiresAt = portalAccess.expiresAt,
                )
            }
        }
    }

    override fun deleteClient(clientId: String, ownerAdminId: String?) {
        dataSource.withConnection { connection ->
            requireClientExists(connection, clientId, ownerAdminId)
            if (hasLinkedTickets(connection, clientId)) {
                throw ServerConflictException("Client has related tickets and cannot be deleted")
            }
            connection.prepareStatement(
                """
                UPDATE tasks
                SET client_id = NULL
                WHERE client_id::text = ?
                  AND (? IS NULL OR owner_admin_id::text = ?)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, clientId)
                statement.setString(2, ownerAdminId)
                statement.setString(3, ownerAdminId)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "DELETE FROM clients WHERE id::text = ? AND (? IS NULL OR owner_admin_id::text = ?)",
            ).use { statement ->
                statement.setString(1, clientId)
                statement.setString(2, ownerAdminId)
                statement.setString(3, ownerAdminId)
                if (statement.executeUpdate() == 0) {
                    throw ServerNotFoundException("Client not found")
                }
            }
        }
    }

    override fun getTaskLabels(ownerAdminId: String?): List<ServerTaskLabelSnapshot> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT tl.id::text AS id, COALESCE(tl.owner_admin_id::text, '') AS owner_admin_id, tl.name, tl.color_hex, COUNT(t.id)::integer AS tasks_count
            FROM task_labels tl
            LEFT JOIN tasks t ON t.label_id = tl.id
            GROUP BY tl.id, tl.owner_admin_id, tl.name, tl.color_hex
            ORDER BY tl.name ASC
            """.trimIndent(),
        ).use { statement ->
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
        ownerAdminId: String?,
    ): ServerTaskLabelSnapshot = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            INSERT INTO task_labels (owner_admin_id, name, color_hex)
            VALUES (NULL, ?, ?)
            RETURNING id::text, COALESCE(owner_admin_id::text, '') AS owner_admin_id, name, color_hex
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, request.name.trim())
            statement.setString(2, normalizeHex(request.colorHex))
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
        ownerAdminId: String?,
    ): ServerTaskLabelSnapshot = dataSource.withConnection { connection ->
        requireLabelExists(connection, labelId)
        connection.prepareStatement(
            """
            UPDATE task_labels
            SET name = COALESCE(NULLIF(?, ''), name),
                color_hex = COALESCE(NULLIF(?, ''), color_hex)
            WHERE id::text = ?
            RETURNING id::text, COALESCE(owner_admin_id::text, '') AS owner_admin_id, name, color_hex
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, request.name)
            statement.setString(2, request.colorHex?.let(::normalizeHex))
            statement.setString(3, labelId)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) {
                    throw ServerNotFoundException("Label not found")
                }
                ServerTaskLabelSnapshot(
                    id = resultSet.getString("id"),
                    ownerAdminId = resultSet.getString("owner_admin_id"),
                    name = resultSet.getString("name"),
                    colorHex = resultSet.getString("color_hex"),
                    tasksCount = countTasksForLabel(connection, labelId),
                )
            }
        }
    }

    override fun deleteTaskLabel(labelId: String, ownerAdminId: String?) {
        dataSource.withConnection { connection ->
            requireLabelExists(connection, labelId)
            if (hasTasksForLabel(connection, labelId)) {
                throw ServerConflictException("Label is in use by tasks and cannot be deleted")
            }
            connection.prepareStatement(
                "DELETE FROM task_labels WHERE id::text = ?",
            ).use { statement ->
                statement.setString(1, labelId)
                statement.executeUpdate()
            }
        }
    }

    override fun getTasks(clientId: String?, labelId: String?, ownerAdminId: String?, viewerUserId: String?): List<ServerTaskSnapshot> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT
                t.id::text AS id,
                t.owner_admin_id::text AS owner_admin_id,
                t.title,
                t.description,
                t.client_id::text AS client_id,
                c.company_name,
                tl.id::text AS label_id,
                tl.name AS label_name,
                tl.color_hex,
                t.due_date::text AS due_date,
                t.completed,
                t.status,
                t.logged_minutes,
                t.logged_seconds,
                pt.pinned_at,
                t.created_at,
                t.updated_at
            FROM tasks t
            JOIN task_labels tl ON tl.id = t.label_id
            LEFT JOIN clients c ON c.id = t.client_id
            LEFT JOIN pinned_tasks pt ON pt.task_id = t.id AND pt.user_id::text = ?
            WHERE (? IS NULL OR t.client_id::text = ?)
              AND (? IS NULL OR t.label_id::text = ?)
              AND (? IS NULL OR t.owner_admin_id::text = ?)
            ORDER BY pt.pinned_at DESC NULLS LAST, t.completed ASC, t.due_date ASC NULLS LAST, t.updated_at DESC, t.created_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, viewerUserId)
            statement.setString(2, clientId)
            statement.setString(3, clientId)
            statement.setString(4, labelId)
            statement.setString(5, labelId)
            statement.setString(6, ownerAdminId)
            statement.setString(7, ownerAdminId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(taskSnapshot(resultSet))
                    }
                }
            }
        }
    }

    override fun createTask(request: CreateTaskRequest, ownerAdminId: String?): ServerTaskSnapshot = dataSource.withConnection { connection ->
        requireLabelExists(connection, request.labelId)
        val resolvedOwnerAdminId = requireNotNull(ownerAdminId) { "ownerAdminId is required" }
        request.clientId?.takeIf { it.isNotBlank() }?.let { requireClientExists(connection, it, resolvedOwnerAdminId) }
        connection.prepareStatement(
            """
            INSERT INTO tasks (title, description, client_id, owner_admin_id, label_id, due_date, completed, status, logged_minutes, logged_seconds)
            VALUES (?, ?, CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), CAST(? AS date), FALSE, 'TODO', 0, 0)
            RETURNING
                id::text AS id,
                title,
                description,
                client_id::text AS client_id,
                due_date::text AS due_date,
                created_at,
                updated_at,
                completed,
                status,
                logged_minutes,
                logged_seconds
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, request.title.trim())
            statement.setString(2, request.description.trim())
            statement.setString(3, request.clientId?.takeIf { it.isNotBlank() })
            statement.setString(4, resolvedOwnerAdminId)
            statement.setString(5, request.labelId)
            statement.setString(6, request.dueDate?.trim()?.takeIf { it.isNotBlank() })
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                hydrateTaskSnapshot(connection, resultSet)
            }
        }
    }

    override fun updateTask(taskId: String, request: UpdateTaskRequest, ownerAdminId: String?): ServerTaskSnapshot = dataSource.withConnection { connection ->
        requireTaskExists(connection, taskId, ownerAdminId)
        request.labelId?.takeIf { it.isNotBlank() }?.let { requireLabelExists(connection, it) }
        request.clientId?.takeIf { it.isNotBlank() }?.let { requireClientExists(connection, it, ownerAdminId) }
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
                completed = COALESCE(?, completed),
                status = COALESCE(NULLIF(?, ''), CASE WHEN COALESCE(?, completed) = TRUE THEN 'DONE' ELSE status END)
            WHERE id::text = ?
              AND (? IS NULL OR owner_admin_id::text = ?)
            RETURNING
                id::text AS id,
                title,
                description,
                client_id::text AS client_id,
                due_date::text AS due_date,
                created_at,
                updated_at,
                completed,
                status,
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
            statement.setString(13, request.status)
            if (request.completed == null) {
                statement.setNull(14, java.sql.Types.BOOLEAN)
            } else {
                statement.setBoolean(14, request.completed)
            }
            statement.setString(15, taskId)
            statement.setString(16, ownerAdminId)
            statement.setString(17, ownerAdminId)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) {
                    throw ServerNotFoundException("Task not found")
                }
                hydrateTaskSnapshot(connection, resultSet)
            }
        }
    }

    override fun deleteTask(taskId: String, ownerAdminId: String?) {
        dataSource.withConnection { connection ->
            requireTaskExists(connection, taskId, ownerAdminId)
            connection.prepareStatement(
                "DELETE FROM tasks WHERE id::text = ? AND (? IS NULL OR owner_admin_id::text = ?)",
            ).use { statement ->
                statement.setString(1, taskId)
                statement.setString(2, ownerAdminId)
                statement.setString(3, ownerAdminId)
                if (statement.executeUpdate() == 0) {
                    throw ServerNotFoundException("Task not found")
                }
            }
        }
    }

    override fun setTaskPinned(taskId: String, userId: String, ownerAdminId: String, pinned: Boolean) {
        dataSource.withConnection { connection ->
            requireTaskExists(connection, taskId, ownerAdminId)
            if (pinned) {
                connection.prepareStatement(
                    """
                    INSERT INTO pinned_tasks (user_id, task_id, pinned_at)
                    VALUES (CAST(? AS uuid), CAST(? AS uuid), NOW())
                    ON CONFLICT (user_id, task_id) DO UPDATE SET pinned_at = EXCLUDED.pinned_at
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, userId)
                    statement.setString(2, taskId)
                    statement.executeUpdate()
                }
            } else {
                connection.prepareStatement(
                    "DELETE FROM pinned_tasks WHERE user_id::text = ? AND task_id::text = ?",
                ).use { statement ->
                    statement.setString(1, userId)
                    statement.setString(2, taskId)
                    statement.executeUpdate()
                }
            }
        }
    }

    override fun getTimeLogs(clientId: String?, taskId: String?, ownerAdminId: String?, limit: Int, offset: Int): List<ServerTimeLogSnapshot> = dataSource.withConnection { connection ->
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
                tl.created_at
            FROM time_logs tl
            JOIN tasks t ON t.id = tl.task_id
            JOIN users u ON u.id = tl.author_id
            WHERE (? IS NULL OR tl.client_id::text = ?)
              AND (? IS NULL OR tl.task_id::text = ?)
              AND (? IS NULL OR t.owner_admin_id::text = ?)
            ORDER BY tl.work_date DESC, tl.created_at DESC
            LIMIT ? OFFSET ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, clientId)
            statement.setString(3, taskId)
            statement.setString(4, taskId)
            statement.setString(5, ownerAdminId)
            statement.setString(6, ownerAdminId)
            statement.setInt(7, limit.coerceIn(1, 200))
            statement.setInt(8, offset.coerceAtLeast(0))
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(timeLogSnapshot(resultSet))
                    }
                }
            }
        }
    }

    override fun createTimeLog(request: CreateTimeLogRequest, ownerAdminId: String?): ServerTimeLogSnapshot = dataSource.withConnection { connection ->
        connection.autoCommit = false
        try {
            val resolvedSeconds = request.seconds.takeIf { it > 0 } ?: (request.minutes * 60)
            val resolvedMinutes = resolvedSeconds / 60
            val taskContext = connection.prepareStatement(
                """
                SELECT client_id::text AS client_id, owner_admin_id::text AS owner_admin_id
                FROM tasks
                WHERE id::text = ?
                  AND (? IS NULL OR owner_admin_id::text = ?)
                LIMIT 1
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, request.taskId)
                statement.setString(2, ownerAdminId)
                statement.setString(3, ownerAdminId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getString("client_id") to resultSet.getString("owner_admin_id")
                    } else {
                        throw ServerNotFoundException("Task not found")
                    }
                }
            }

            val created = connection.prepareStatement(
                """
                INSERT INTO time_logs (task_id, client_id, author_id, minutes, seconds, work_date, note, billable)
                VALUES (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), ?, ?, CAST(? AS date), ?, ?)
                RETURNING id::text AS id, task_id::text AS task_id, client_id::text AS client_id,
                          author_id::text AS author_id, minutes, seconds, work_date::text AS work_date, note, billable, created_at
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, request.taskId)
                statement.setString(2, taskContext.first)
                statement.setString(3, request.authorId)
                statement.setInt(4, resolvedMinutes)
                statement.setInt(5, resolvedSeconds)
                statement.setString(6, request.workDate)
                statement.setString(7, request.note)
                statement.setBoolean(8, request.billable)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    val authorName = resolveAuthorName(connection, request.authorId)
                    ServerTimeLogSnapshot(
                        id = resultSet.getString("id"),
                        ownerAdminId = taskContext.second,
                        taskId = resultSet.getString("task_id"),
                        clientId = resultSet.getString("client_id"),
                        authorId = resultSet.getString("author_id"),
                        authorName = authorName,
                        minutes = resultSet.getInt("minutes"),
                        seconds = resultSet.getInt("seconds"),
                        workDate = resultSet.getString("work_date"),
                        note = resultSet.getString("note"),
                        billable = resultSet.getBoolean("billable"),
                        createdAt = formatTimestamp(resultSet.getObject("created_at")),
                    )
                }
            }

            connection.prepareStatement(
                """
                UPDATE tasks
                SET logged_minutes = (logged_seconds + ?) / 60,
                    logged_seconds = logged_seconds + ?
                WHERE id::text = ?
                  AND (? IS NULL OR owner_admin_id::text = ?)
                """.trimIndent(),
            ).use { statement ->
                statement.setInt(1, resolvedSeconds)
                statement.setInt(2, resolvedSeconds)
                statement.setString(3, request.taskId)
                statement.setString(4, ownerAdminId)
                statement.setString(5, ownerAdminId)
                statement.executeUpdate()
            }
            connection.commit()
            created
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = true
        }
    }

    override fun getDashboard(clientId: String?, labelId: String?, ownerAdminId: String?): ServerDashboardSnapshot = dataSource.withConnection { connection ->
        val activeClients = connection.prepareStatement(
            """
            SELECT COUNT(*)::integer AS active_clients
            FROM clients
            WHERE account_status = 'ACTIVE'
              AND (? IS NULL OR owner_admin_id::text = ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, ownerAdminId)
            statement.setString(2, ownerAdminId)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getInt("active_clients")
            }
        }
        val totals = connection.prepareStatement(
            """
            SELECT
                COALESCE(SUM(minutes), 0)::integer AS total_minutes,
                COALESCE(SUM(minutes) FILTER (WHERE billable = TRUE), 0)::integer AS billable_minutes,
                TO_CHAR(CURRENT_DATE, 'TMMonth YYYY') AS month_label
            FROM time_logs tl
            JOIN tasks t ON t.id = tl.task_id
            WHERE DATE_TRUNC('month', work_date) = DATE_TRUNC('month', CURRENT_DATE)
              AND (? IS NULL OR t.owner_admin_id::text = ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, ownerAdminId)
            statement.setString(2, ownerAdminId)
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
                    COALESCE(SUM(minutes), 0)::integer AS total_minutes,
                    COALESCE(SUM(minutes) FILTER (WHERE billable = TRUE), 0)::integer AS billable_minutes
                FROM time_logs tl
                JOIN tasks t ON t.id = tl.task_id
                WHERE client_id::text = ?
                  AND (? IS NULL OR t.owner_admin_id::text = ?)
                  AND DATE_TRUNC('month', work_date) = DATE_TRUNC('month', CURRENT_DATE)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, clientId)
                statement.setString(2, ownerAdminId)
                statement.setString(3, ownerAdminId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt("total_minutes") to resultSet.getInt("billable_minutes")
                }
            }
        }
        val dailyMinutes = connection.prepareStatement(
            """
            SELECT work_date::text AS work_date, COALESCE(SUM(minutes), 0)::integer AS minutes
            FROM time_logs tl
            JOIN tasks t ON t.id = tl.task_id
            WHERE DATE_TRUNC('month', work_date) = DATE_TRUNC('month', CURRENT_DATE)
              AND (? IS NULL OR client_id::text = ?)
              AND (? IS NULL OR t.owner_admin_id::text = ?)
            GROUP BY work_date
            ORDER BY work_date ASC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, clientId)
            statement.setString(3, ownerAdminId)
            statement.setString(4, ownerAdminId)
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
            availableTasks = getTasks(clientId, labelId, ownerAdminId, ownerAdminId),
        )
    }

    override fun getAttachment(id: String, ownerAdminId: String?): ServerAttachmentSnapshot? = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT a.id::text, a.file_name, a.content_type
            FROM attachments a
            JOIN tickets t ON t.id = a.ticket_id
            JOIN clients c ON c.id = t.client_id
            WHERE a.id::text = ?
              AND (? IS NULL OR c.owner_admin_id::text = ?)
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, id)
            statement.setString(2, ownerAdminId)
            statement.setString(3, ownerAdminId)
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

    override fun getAlerts(userId: String, limit: Int, offset: Int): List<ServerNotificationAlertSnapshot> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT id::text, user_id::text, ticket_id::text, type, title, body, read_at, created_at
            FROM notification_alerts
            WHERE user_id::text = ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, userId)
            statement.setInt(2, limit.coerceIn(1, 100))
            statement.setInt(3, offset.coerceAtLeast(0))
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(alertSnapshot(resultSet))
                    }
                }
            }
        }
    }

    override fun markAlertRead(alertId: String, userId: String): ServerNotificationAlertSnapshot? = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            UPDATE notification_alerts
            SET read_at = COALESCE(read_at, NOW())
            WHERE id::text = ?
              AND user_id::text = ?
            RETURNING id::text, user_id::text, ticket_id::text, type, title, body, read_at, created_at
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, alertId)
            statement.setString(2, userId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) alertSnapshot(resultSet) else null
            }
        }
    }

    override fun addTicketTimeEntry(ticketId: String, authorId: String, request: AddTicketTimeEntryRequest): TicketTimeEntry = dataSource.withConnection { connection ->
        val resolvedWorkDate = request.workDate.trim().takeIf { it.isNotBlank() }
            ?: java.time.LocalDate.now().toString()
        val created = connection.prepareStatement(
            """
            INSERT INTO ticket_time_entries (ticket_id, author_id, minutes, work_date, note, billable)
            VALUES (CAST(? AS uuid), CAST(? AS uuid), ?, CAST(? AS date), ?, ?)
            RETURNING id::text AS id, ticket_id::text AS ticket_id, author_id::text AS author_id,
                      minutes, work_date::text AS work_date, note, billable, created_at
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, ticketId)
            statement.setString(2, authorId)
            statement.setInt(3, request.minutes)
            statement.setString(4, resolvedWorkDate)
            statement.setString(5, request.note)
            statement.setBoolean(6, request.billable)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                val authorName = resolveAuthorName(connection, authorId)
                TicketTimeEntry(
                    id = resultSet.getString("id"),
                    ticketId = resultSet.getString("ticket_id"),
                    authorId = resultSet.getString("author_id"),
                    authorName = authorName,
                    minutes = resultSet.getInt("minutes"),
                    workDate = resultSet.getString("work_date"),
                    note = resultSet.getString("note") ?: "",
                    billable = resultSet.getBoolean("billable"),
                    createdAt = formatTimestamp(resultSet.getObject("created_at")),
                )
            }
        }
        created
    }

    override fun getTicketTimeEntries(ticketId: String): List<TicketTimeEntry> = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT tte.id::text AS id, tte.ticket_id::text AS ticket_id, tte.author_id::text AS author_id,
                   u.name AS author_name, tte.minutes, tte.work_date::text AS work_date,
                   tte.note, tte.billable, tte.created_at
            FROM ticket_time_entries tte
            JOIN users u ON u.id = tte.author_id
            WHERE tte.ticket_id::text = ?
            ORDER BY tte.work_date DESC, tte.created_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, ticketId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            TicketTimeEntry(
                                id = resultSet.getString("id"),
                                ticketId = resultSet.getString("ticket_id"),
                                authorId = resultSet.getString("author_id"),
                                authorName = resultSet.getString("author_name"),
                                minutes = resultSet.getInt("minutes"),
                                workDate = resultSet.getString("work_date"),
                                note = resultSet.getString("note") ?: "",
                                billable = resultSet.getBoolean("billable"),
                                createdAt = formatTimestamp(resultSet.getObject("created_at")),
                            ),
                        )
                    }
                }
            }
        }
    }

    override fun addInternalComment(ticketId: String, authorId: String, request: AddInternalCommentRequest): InternalComment = dataSource.withConnection { connection ->
        val created = connection.prepareStatement(
            """
            INSERT INTO internal_comments (ticket_id, author_id, body)
            VALUES (CAST(? AS uuid), CAST(? AS uuid), ?)
            RETURNING id::text AS id, ticket_id::text AS ticket_id, author_id::text AS author_id, body, created_at
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, ticketId)
            statement.setString(2, authorId)
            statement.setString(3, request.body)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                val authorName = resolveAuthorName(connection, authorId)
                InternalComment(
                    id = resultSet.getString("id"),
                    ticketId = resultSet.getString("ticket_id"),
                    authorId = resultSet.getString("author_id"),
                    authorName = authorName,
                    body = resultSet.getString("body"),
                    createdAt = formatTimestamp(resultSet.getObject("created_at")),
                )
            }
        }
        created
    }

    override fun changeTicketAssignee(ticketId: String, request: ChangeTicketAssigneeRequest): ServerTicketSnapshot = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            UPDATE tickets SET assignee_id = CAST(? AS uuid), updated_at = NOW() WHERE id::text = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, request.assigneeId.takeIf { it.isNotBlank() })
            statement.setString(2, ticketId)
            statement.executeUpdate()
        }
        getTicket(ticketId) ?: throw ServerNotFoundException("Ticket not found")
    }

    private fun updateLastLogin(connection: Connection, userId: String) {
        connection.prepareStatement(
            "UPDATE users SET last_login_at = NOW() WHERE id::text = ?",
        ).use { statement ->
            statement.setString(1, userId)
            statement.executeUpdate()
        }
    }

    private fun updatePasswordHash(connection: Connection, userId: String, passwordHash: String) {
        connection.prepareStatement(
            "UPDATE users SET password_hash = ? WHERE id::text = ?",
        ).use { statement ->
            statement.setString(1, passwordHash)
            statement.setString(2, userId)
            statement.executeUpdate()
        }
    }

    private fun findRequesterId(connection: Connection, clientId: String): String? =
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
                if (resultSet.next()) resultSet.getString("id") else null
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
                if (resultSet.next()) resultSet.getString("product_name") else error("Client not found: $clientId")
            }
        }

    private fun ownerAdminIdForClient(connection: Connection, clientId: String): String =
        connection.prepareStatement(
            """
            SELECT owner_admin_id::text
            FROM clients
            WHERE id = CAST(? AS uuid)
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.getString("owner_admin_id") else error("Client not found: $clientId")
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

    private fun insertAlert(
        connection: Connection,
        userId: String,
        ticketId: String,
        type: String,
        title: String,
        body: String,
    ) {
        connection.prepareStatement(
            """
            INSERT INTO notification_alerts (user_id, ticket_id, type, title, body)
            VALUES (CAST(? AS uuid), CAST(? AS uuid), ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, userId)
            statement.setString(2, ticketId)
            statement.setString(3, type)
            statement.setString(4, title)
            statement.setString(5, body)
            statement.executeUpdate()
        }
    }

    private fun alertSnapshot(resultSet: ResultSet): ServerNotificationAlertSnapshot =
        ServerNotificationAlertSnapshot(
            id = resultSet.getString("id"),
            userId = resultSet.getString("user_id"),
            ticketId = resultSet.getString("ticket_id"),
            type = resultSet.getString("type"),
            title = resultSet.getString("title"),
            body = resultSet.getString("body"),
            readAt = formatNullableTimestamp(resultSet.getObject("read_at")),
            createdAt = formatTimestamp(resultSet.getObject("created_at")),
        )

    private fun ticketSnapshot(resultSet: ResultSet): ServerTicketSnapshot =
        ServerTicketSnapshot(
        id = resultSet.getString("id"),
        clientId = resultSet.getString("client_id") ?: "",
        ticketNumber = resultSet.getString("ticket_number"),
        subject = resultSet.getString("subject"),
        description = resultSet.getString("description"),
        category = resultSet.getString("category"),
        affectedApp = resultSet.getString("affected_app"),
        platform = resultSet.getString("platform"),
        appVersion = resultSet.getString("app_version"),
        clientReference = resultSet.getString("client_reference"),
        status = resultSet.getString("status"),
        priority = resultSet.getString("priority"),
        waitingOn = resultSet.getString("waiting_on"),
        resolutionSummary = resultSet.getString("resolution_summary"),
        requesterId = resultSet.getString("requester_id") ?: "",
        requesterName = resultSet.getString("requester_name") ?: "",
        requesterEmail = resultSet.getString("requester_email") ?: "",
        assigneeId = resultSet.getString("assignee_id"),
        assigneeName = resultSet.getString("assignee_name"),
        createdAt = formatTimestamp(resultSet.getObject("created_at")),
        updatedAt = formatTimestamp(resultSet.getObject("updated_at")),
        clientAcceptedCloseAt = formatNullableTimestamp(resultSet.getObject("client_accepted_close_at")),
        adminAcceptedCloseAt = formatNullableTimestamp(resultSet.getObject("admin_accepted_close_at")),
        archivedAt = formatNullableTimestamp(resultSet.getObject("archived_at")),
        satisfactionRating = resultSet.getObject("satisfaction_rating")?.let { resultSet.getInt("satisfaction_rating") },
        )

    private fun taskSnapshot(resultSet: ResultSet): ServerTaskSnapshot = ServerTaskSnapshot(
        id = resultSet.getString("id"),
        ownerAdminId = resultSet.getString("owner_admin_id"),
        title = resultSet.getString("title"),
        description = resultSet.getString("description") ?: "",
        clientId = resultSet.getString("client_id"),
        clientName = resultSet.getString("company_name"),
        labelId = resultSet.getString("label_id"),
        labelName = resultSet.getString("label_name"),
        labelColorHex = resultSet.getString("color_hex"),
        dueDate = resultSet.getString("due_date"),
        completed = resultSet.getBoolean("completed"),
        status = resultSet.getString("status") ?: if (resultSet.getBoolean("completed")) "DONE" else "TODO",
        loggedMinutes = resultSet.getInt("logged_minutes"),
        loggedSeconds = resultSet.getInt("logged_seconds"),
        pinnedAt = formatNullableTimestamp(resultSet.getObject("pinned_at")),
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
                t.title,
                t.description,
                t.client_id::text AS client_id,
                c.company_name,
                tl.id::text AS label_id,
                tl.name AS label_name,
                tl.color_hex,
                t.due_date::text AS due_date,
                t.completed,
                t.status,
                t.logged_minutes,
                t.logged_seconds,
                NULL::timestamptz AS pinned_at,
                t.created_at,
                t.updated_at
            FROM tasks t
            JOIN task_labels tl ON tl.id = t.label_id
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

    private fun createPortalAccessCode(
        connection: Connection,
        clientId: String,
        ownerAdminId: String,
        expiresInDays: Int,
    ): PortalAccessCode {
        connection.prepareStatement(
            """
            UPDATE client_access_codes
            SET expires_at = NOW()
            WHERE client_id::text = ?
              AND owner_admin_id::text = ?
              AND expires_at > NOW()
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, ownerAdminId)
            statement.executeUpdate()
        }

        val code = "ORY-${UUID.randomUUID().toString().replace("-", "").take(12).uppercase()}"
        return connection.prepareStatement(
            """
            INSERT INTO client_access_codes (client_id, owner_admin_id, code_plain, code_hash, expires_at, created_by)
            VALUES (CAST(? AS uuid), CAST(? AS uuid), ?, ?, NOW() + (?::text || ' days')::interval, CAST(? AS uuid))
            RETURNING code_plain, expires_at
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, ownerAdminId)
            statement.setString(3, code)
            statement.setString(4, PasswordHasher.hashToken(code))
            statement.setInt(5, expiresInDays.coerceIn(1, DEFAULT_CLIENT_ACCESS_CODE_DAYS))
            statement.setString(6, ownerAdminId)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                PortalAccessCode(
                    code = resultSet.getString("code_plain"),
                    status = "ACTIVE",
                    expiresAt = formatNullableTimestamp(resultSet.getObject("expires_at")),
                )
            }
        }
    }

    private fun portalAccessForClient(connection: Connection, clientId: String): PortalAccessCode =
        connection.prepareStatement(
            """
            SELECT code_plain, expires_at, 'ACTIVE' AS status
            FROM client_access_codes
            WHERE client_id::text = ?
              AND code_plain IS NOT NULL
              AND expires_at > NOW()
            ORDER BY created_at DESC
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            val activeCode = statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    PortalAccessCode(
                        code = resultSet.getString("code_plain"),
                        status = resultSet.getString("status"),
                        expiresAt = formatNullableTimestamp(resultSet.getObject("expires_at")),
                    )
                } else {
                    null
                }
            }
            activeCode ?: latestExpiredPortalAccessForClient(connection, clientId) ?: PortalAccessCode(
                code = null,
                status = "MISSING",
                expiresAt = null,
            )
        }

    private fun latestExpiredPortalAccessForClient(connection: Connection, clientId: String): PortalAccessCode? =
        connection.prepareStatement(
            """
            SELECT expires_at
            FROM client_access_codes
            WHERE client_id::text = ?
              AND code_plain IS NOT NULL
            ORDER BY expires_at DESC
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    PortalAccessCode(
                        code = null,
                        status = "EXPIRED",
                        expiresAt = formatNullableTimestamp(resultSet.getObject("expires_at")),
                    )
                } else {
                    null
                }
            }
        }

    private fun getOpenTasksCount(connection: Connection, clientId: String, ownerAdminId: String? = null): Int =
        connection.prepareStatement(
            """
            SELECT COUNT(*)::integer AS open_tasks_count
            FROM tasks
            WHERE client_id::text = ?
              AND completed = FALSE
              AND (? IS NULL OR owner_admin_id::text = ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, ownerAdminId)
            statement.setString(3, ownerAdminId)
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

    private fun getClientMonthlyMinutes(connection: Connection, clientId: String, ownerAdminId: String? = null): Int =
        connection.prepareStatement(
            """
            SELECT COALESCE(SUM(minutes), 0)::integer AS monthly_minutes
            FROM time_logs tl
            JOIN tasks t ON t.id = tl.task_id
            WHERE client_id::text = ?
              AND (? IS NULL OR t.owner_admin_id::text = ?)
              AND DATE_TRUNC('month', work_date) = DATE_TRUNC('month', CURRENT_DATE)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, ownerAdminId)
            statement.setString(3, ownerAdminId)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getInt("monthly_minutes")
            }
        }

    private fun requireClientExists(connection: Connection, clientId: String, ownerAdminId: String? = null) {
        if (!recordExists(connection, "clients", clientId, ownerAdminId)) {
            throw ServerNotFoundException("Client not found")
        }
    }

    private fun requireLabelExists(connection: Connection, labelId: String, ownerAdminId: String? = null) {
        if (!recordExists(connection, "task_labels", labelId, ownerAdminId)) {
            throw ServerNotFoundException("Label not found")
        }
    }

    private fun requireTaskExists(connection: Connection, taskId: String, ownerAdminId: String? = null) {
        if (!recordExists(connection, "tasks", taskId, ownerAdminId)) {
            throw ServerNotFoundException("Task not found")
        }
    }

    private fun recordExists(connection: Connection, tableName: String, id: String, ownerAdminId: String? = null): Boolean =
        connection.prepareStatement(
            """
            SELECT 1
            FROM $tableName
            WHERE id::text = ?
              AND (? IS NULL OR owner_admin_id::text = ?)
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, id)
            statement.setString(2, ownerAdminId)
            statement.setString(3, ownerAdminId)
            statement.executeQuery().use { resultSet -> resultSet.next() }
        }

    private fun hasLinkedTickets(connection: Connection, clientId: String): Boolean =
        connection.prepareStatement(
            "SELECT 1 FROM tickets WHERE client_id::text = ? LIMIT 1",
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { resultSet -> resultSet.next() }
        }

    private fun hasTasksForLabel(connection: Connection, labelId: String, ownerAdminId: String? = null): Boolean =
        connection.prepareStatement(
            "SELECT 1 FROM tasks WHERE label_id::text = ? LIMIT 1",
        ).use { statement ->
            statement.setString(1, labelId)
            statement.executeQuery().use { resultSet -> resultSet.next() }
        }

    private fun countTasksForLabel(connection: Connection, labelId: String, ownerAdminId: String? = null): Int =
        connection.prepareStatement(
            """
            SELECT COUNT(*)::integer AS tasks_count
            FROM tasks
            WHERE label_id::text = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, labelId)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                resultSet.getInt("tasks_count")
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

    private fun formatNullableTimestamp(value: Any?): String? =
        formatTimestamp(value).takeIf { it.isNotBlank() }

    // ── Invoices ─────────────────────────────────────────────────────────────

    override fun getInvoices(ownerAdminId: String?, clientId: String?, limit: Int, offset: Int): List<ServerInvoiceSnapshot> =
        dataSource.withConnection { connection ->
            connection.prepareStatement(
                """
                SELECT i.id::text, i.invoice_number, i.client_id::text,
                       COALESCE(c.company_name, '') AS client_name,
                       i.status, i.issued_at::text, i.due_at::text,
                       i.notes, i.tax_percent,
                       i.created_at, i.sent_at, i.paid_at
                FROM invoices i
                JOIN clients c ON c.id = i.client_id
                WHERE (? IS NULL OR c.owner_admin_id::text = ?)
                  AND (? IS NULL OR i.client_id::text = ?)
                  AND (? IS NULL OR i.status IN ('SENT','PAID'))
                ORDER BY i.created_at DESC
                LIMIT ? OFFSET ?
                """.trimIndent(),
            ).use { stmt ->
                stmt.setString(1, ownerAdminId)
                stmt.setString(2, ownerAdminId)
                stmt.setString(3, clientId)
                stmt.setString(4, clientId)
                // for client queries, only show SENT/PAID
                stmt.setString(5, clientId)
                stmt.setInt(6, limit.coerceIn(1, 200))
                stmt.setInt(7, offset.coerceAtLeast(0))
                stmt.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            val invoiceId = rs.getString("id")
                            add(invoiceSnapshot(rs, fetchItems(connection, invoiceId)))
                        }
                    }
                }
            }
        }

    override fun getInvoice(id: String, ownerAdminId: String?, clientId: String?): ServerInvoiceSnapshot? =
        dataSource.withConnection { connection ->
            connection.prepareStatement(
                """
                SELECT i.id::text, i.invoice_number, i.client_id::text,
                       COALESCE(c.company_name, '') AS client_name,
                       i.status, i.issued_at::text, i.due_at::text,
                       i.notes, i.tax_percent,
                       i.created_at, i.sent_at, i.paid_at
                FROM invoices i
                JOIN clients c ON c.id = i.client_id
                WHERE i.id = CAST(? AS uuid)
                  AND (? IS NULL OR c.owner_admin_id::text = ?)
                  AND (? IS NULL OR i.client_id::text = ?)
                LIMIT 1
                """.trimIndent(),
            ).use { stmt ->
                stmt.setString(1, id)
                stmt.setString(2, ownerAdminId)
                stmt.setString(3, ownerAdminId)
                stmt.setString(4, clientId)
                stmt.setString(5, clientId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) invoiceSnapshot(rs, fetchItems(connection, id)) else null
                }
            }
        }

    override fun createInvoice(request: CreateInvoiceRequest, createdBy: String): ServerInvoiceSnapshot =
        dataSource.withConnection { connection ->
            connection.autoCommit = false
            try {
                val invoiceId = UUID.randomUUID().toString()
                val invoiceNumber = generateInvoiceNumber(connection)

                connection.prepareStatement(
                    """
                    INSERT INTO invoices (id, invoice_number, client_id, status, issued_at, due_at,
                                         notes, tax_percent, created_by, created_at, updated_at)
                    VALUES (CAST(? AS uuid), ?, CAST(? AS uuid), 'DRAFT',
                            CAST(? AS date), CAST(? AS date),
                            ?, ?, CAST(? AS uuid), NOW(), NOW())
                    """.trimIndent(),
                ).use { stmt ->
                    stmt.setString(1, invoiceId)
                    stmt.setString(2, invoiceNumber)
                    stmt.setString(3, request.clientId)
                    stmt.setString(4, request.issuedAt)
                    stmt.setString(5, request.dueAt?.takeIf { it.isNotBlank() })
                    stmt.setString(6, request.notes?.takeIf { it.isNotBlank() })
                    stmt.setDouble(7, request.taxPercent.coerceIn(0.0, 100.0))
                    stmt.setString(8, createdBy)
                    stmt.executeUpdate()
                }

                request.items.forEachIndexed { index, item ->
                    connection.prepareStatement(
                        """
                        INSERT INTO invoice_items (id, invoice_id, description, quantity, unit_price, sort_order)
                        VALUES (gen_random_uuid(), CAST(? AS uuid), ?, ?, ?, ?)
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setString(1, invoiceId)
                        stmt.setString(2, item.description)
                        stmt.setDouble(3, item.quantity.coerceAtLeast(0.0))
                        stmt.setDouble(4, item.unitPrice.coerceAtLeast(0.0))
                        stmt.setInt(5, item.sortOrder.takeIf { it >= 0 } ?: index)
                        stmt.executeUpdate()
                    }
                }

                connection.commit()
                getInvoice(invoiceId) ?: throw ServerNotFoundException("Invoice not found after creation")
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }

    override fun updateInvoiceStatus(invoiceId: String, request: UpdateInvoiceStatusRequest, ownerAdminId: String): ServerInvoiceSnapshot =
        dataSource.withConnection { connection ->
            val sentAtClause = if (request.status == "SENT") ", sent_at = NOW()" else ""
            val paidAtClause = if (request.status == "PAID") ", paid_at = NOW()" else ""
            connection.prepareStatement(
                """
                UPDATE invoices
                SET status = ?, updated_at = NOW()$sentAtClause$paidAtClause
                WHERE id = CAST(? AS uuid)
                  AND EXISTS (
                      SELECT 1 FROM clients c
                      WHERE c.id = invoices.client_id
                        AND c.owner_admin_id::text = ?
                  )
                """.trimIndent(),
            ).use { stmt ->
                stmt.setString(1, request.status)
                stmt.setString(2, invoiceId)
                stmt.setString(3, ownerAdminId)
                stmt.executeUpdate()
            }
            getInvoice(invoiceId, ownerAdminId = ownerAdminId)
                ?: throw ServerNotFoundException("Invoice not found")
        }

    private fun fetchItems(connection: Connection, invoiceId: String): List<ServerInvoiceItemSnapshot> =
        connection.prepareStatement(
            """
            SELECT id::text, description, quantity, unit_price, sort_order
            FROM invoice_items
            WHERE invoice_id = CAST(? AS uuid)
            ORDER BY sort_order ASC
            """.trimIndent(),
        ).use { stmt ->
            stmt.setString(1, invoiceId)
            stmt.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(ServerInvoiceItemSnapshot(
                            id = rs.getString("id"),
                            description = rs.getString("description"),
                            quantity = rs.getDouble("quantity"),
                            unitPrice = rs.getDouble("unit_price"),
                            sortOrder = rs.getInt("sort_order"),
                        ))
                    }
                }
            }
        }

    private fun invoiceSnapshot(rs: ResultSet, items: List<ServerInvoiceItemSnapshot>) = ServerInvoiceSnapshot(
        id = rs.getString("id"),
        invoiceNumber = rs.getString("invoice_number"),
        clientId = rs.getString("client_id"),
        clientName = rs.getString("client_name"),
        status = rs.getString("status"),
        issuedAt = rs.getString("issued_at") ?: "",
        dueAt = rs.getString("due_at"),
        notes = rs.getString("notes"),
        taxPercent = rs.getDouble("tax_percent"),
        items = items,
        createdAt = formatTimestamp(rs.getObject("created_at")),
        sentAt = formatNullableTimestamp(rs.getObject("sent_at")),
        paidAt = formatNullableTimestamp(rs.getObject("paid_at")),
    )

    private fun generateInvoiceNumber(connection: Connection): String {
        val seq = connection.prepareStatement("SELECT nextval('invoice_number_seq')").use { stmt ->
            stmt.executeQuery().use { rs ->
                rs.next()
                rs.getLong(1)
            }
        }
        val year = java.time.LocalDate.now().year
        return "FAC-$year-${seq.toString().padStart(3, '0')}"
    }

    private companion object {
        const val DEFAULT_ADMIN_OWNER_ID = "22222222-2222-2222-2222-222222222222"
        const val DEFAULT_CLIENT_ACCESS_CODE_DAYS = 3650
    }
}
