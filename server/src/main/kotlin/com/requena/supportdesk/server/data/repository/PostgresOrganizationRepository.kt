package com.requena.supportdesk.server.data.repository

import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.model.ServerConflictException
import com.requena.supportdesk.server.domain.model.ServerNotFoundException
import com.requena.supportdesk.server.domain.model.ServerOrganizationInvitationSnapshot
import com.requena.supportdesk.server.domain.model.TRIAL_DAYS
import com.requena.supportdesk.server.domain.model.ServerOrganizationMemberSnapshot
import com.requena.supportdesk.server.domain.model.ServerOrganizationSnapshot
import com.requena.supportdesk.server.domain.model.UpdateOrganizationRequest
import com.requena.supportdesk.server.domain.repository.OrganizationRepository
import com.requena.supportdesk.server.security.PasswordHasher
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant

/** Postgres implementation of companies, members and invitations (tables from V17). */
class PostgresOrganizationRepository(
    private val dataSource: PostgresSupportDeskDataSource,
) : OrganizationRepository {

    override fun signupOrganization(
        name: String,
        email: String,
        password: String,
        organizationName: String,
        legalVersion: String,
    ): ServerAuthIdentity = transaction { connection ->
        val organizationId = connection.returningText(
            "INSERT INTO organizations (name) VALUES (?) RETURNING id::text",
            organizationName,
        )
        val userId = insertStaffUser(connection, name, email, password)
        insertMember(connection, organizationId, userId, "OWNER")
        recordLegalAcceptances(connection, userId, legalVersion)
        // Every new company starts with the Pro trial (V19); it becomes Free when it ends.
        connection.prepareStatement(
            """
            INSERT INTO organization_subscriptions (organization_id, plan_key, status, trial_ends_at)
            VALUES (CAST(? AS uuid), 'PRO', 'TRIAL', NOW() + make_interval(days => ?))
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.setInt(2, TRIAL_DAYS.toInt())
            statement.executeUpdate()
        }
        connection.prepareStatement(
            "INSERT INTO organization_plan_events (organization_id, action, to_plan, actor_user_id) VALUES (CAST(? AS uuid), 'TRIAL_STARTED', 'PRO', CAST(? AS uuid))",
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.setString(2, userId)
            statement.executeUpdate()
        }
        requireNotNull(identity(connection, userId))
    }

    override fun pendingInvitationTarget(codeHash: String): Pair<String, String>? = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT organization_id::text, role FROM organization_invitations
            WHERE code_hash = ? AND accepted_at IS NULL AND revoked_at IS NULL AND expires_at > NOW()
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, codeHash)
            statement.executeQuery().use { if (it.next()) it.getString(1) to it.getString(2) else null }
        }
    }

    override fun organization(organizationId: String): ServerOrganizationSnapshot = dataSource.withConnection { connection ->
        organization(connection, organizationId)
    }

    override fun updateOrganization(
        organizationId: String,
        request: UpdateOrganizationRequest,
    ): ServerOrganizationSnapshot = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            UPDATE organizations SET
                name = COALESCE(?, name),
                legal_form = COALESCE(?, legal_form),
                sector = COALESCE(?, sector),
                team_size = COALESCE(?, team_size),
                has_accountant = COALESCE(?, has_accountant),
                onboarding_goals = COALESCE(?, onboarding_goals),
                onboarding_completed_at = CASE
                    WHEN ? AND onboarding_completed_at IS NULL THEN NOW()
                    ELSE onboarding_completed_at
                END,
                legal_name = COALESCE(?, legal_name),
                tax_id = COALESCE(?, tax_id),
                address_line = COALESCE(?, address_line),
                postal_code = COALESCE(?, postal_code),
                city = COALESCE(?, city),
                province = COALESCE(?, province),
                country = COALESCE(?, country)
            WHERE id::text = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, request.name)
            statement.setString(2, request.legalForm)
            statement.setString(3, request.sector)
            statement.setString(4, request.teamSize)
            statement.setObject(5, request.hasAccountant, java.sql.Types.BOOLEAN)
            val goals = request.onboardingGoals
            if (goals == null) {
                statement.setNull(6, java.sql.Types.ARRAY)
            } else {
                statement.setArray(6, connection.createArrayOf("text", goals.toTypedArray()))
            }
            statement.setBoolean(7, request.onboardingCompleted == true)
            statement.setString(8, request.legalName)
            statement.setString(9, request.taxId)
            statement.setString(10, request.addressLine)
            statement.setString(11, request.postalCode)
            statement.setString(12, request.city)
            statement.setString(13, request.province)
            statement.setString(14, request.country)
            statement.setString(15, organizationId)
            if (statement.executeUpdate() == 0) throw ServerNotFoundException("Organization not found")
        }
        organization(connection, organizationId)
    }

    override fun organizationMembers(organizationId: String): List<ServerOrganizationMemberSnapshot> =
        dataSource.withConnection { connection -> members(connection, organizationId, userId = null) }

    override fun updateOrganizationMemberRole(
        organizationId: String,
        userId: String,
        role: String,
    ): ServerOrganizationMemberSnapshot = transaction { connection ->
        val current = members(connection, organizationId, userId).firstOrNull()
            ?: throw ServerNotFoundException("Member not found")
        if (current.role == "OWNER" && role != "OWNER") requireAnotherActiveOwner(connection, organizationId, userId)
        connection.prepareStatement(
            "UPDATE organization_members SET role = ? WHERE organization_id::text = ? AND user_id::text = ?",
        ).use { statement ->
            statement.setString(1, role)
            statement.setString(2, organizationId)
            statement.setString(3, userId)
            statement.executeUpdate()
        }
        members(connection, organizationId, userId).single()
    }

    override fun deactivateOrganizationMember(organizationId: String, userId: String) = transaction { connection ->
        val current = members(connection, organizationId, userId).firstOrNull()
            ?: throw ServerNotFoundException("Member not found")
        if (current.role == "OWNER") requireAnotherActiveOwner(connection, organizationId, userId)
        connection.prepareStatement("UPDATE users SET is_active = FALSE, updated_at = NOW() WHERE id::text = ?").use {
            it.setString(1, userId)
            it.executeUpdate()
        }
        connection.prepareStatement(
            "UPDATE refresh_tokens SET revoked_at = NOW() WHERE user_id::text = ? AND revoked_at IS NULL",
        ).use {
            it.setString(1, userId)
            it.executeUpdate()
        }
        Unit
    }

    override fun createOrganizationInvitation(
        organizationId: String,
        role: String,
        createdByUserId: String,
        codeHash: String,
        expiresAt: Instant,
    ): ServerOrganizationInvitationSnapshot = dataSource.withConnection { connection ->
        val id = connection.prepareStatement(
            """
            INSERT INTO organization_invitations (organization_id, code_hash, role, created_by, expires_at)
            VALUES (CAST(? AS uuid), ?, ?, CAST(? AS uuid), ?)
            RETURNING id::text
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.setString(2, codeHash)
            statement.setString(3, role)
            statement.setString(4, createdByUserId)
            statement.setTimestamp(5, Timestamp.from(expiresAt))
            statement.executeQuery().use { it.next(); it.getString(1) }
        }
        invitations(connection, organizationId).first { it.id == id }
    }

    override fun organizationInvitations(organizationId: String): List<ServerOrganizationInvitationSnapshot> =
        dataSource.withConnection { connection -> invitations(connection, organizationId) }

    override fun revokeOrganizationInvitation(organizationId: String, invitationId: String) {
        dataSource.withConnection { connection ->
            val updated = connection.prepareStatement(
                """
                UPDATE organization_invitations SET revoked_at = NOW()
                WHERE id::text = ? AND organization_id::text = ? AND accepted_at IS NULL AND revoked_at IS NULL
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, invitationId)
                statement.setString(2, organizationId)
                statement.executeUpdate()
            }
            if (updated == 0) throw ServerNotFoundException("Invitation not found")
        }
    }

    override fun acceptOrganizationInvitation(
        codeHash: String,
        name: String,
        email: String,
        password: String,
        legalVersion: String,
    ): ServerAuthIdentity = transaction { connection ->
        val (invitationId, organizationId, role) = connection.prepareStatement(
            """
            SELECT id::text, organization_id::text, role FROM organization_invitations
            WHERE code_hash = ? AND accepted_at IS NULL AND revoked_at IS NULL AND expires_at > NOW()
            FOR UPDATE
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, codeHash)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) throw ServerNotFoundException("Invitation not found or expired")
                Triple(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3))
            }
        }
        val userId = insertStaffUser(connection, name, email, password)
        insertMember(connection, organizationId, userId, role)
        recordLegalAcceptances(connection, userId, legalVersion)
        connection.prepareStatement(
            "UPDATE organization_invitations SET accepted_at = NOW(), accepted_by = CAST(? AS uuid) WHERE id::text = ?",
        ).use { statement ->
            statement.setString(1, userId)
            statement.setString(2, invitationId)
            statement.executeUpdate()
        }
        requireNotNull(identity(connection, userId))
    }

    // --- helpers -----------------------------------------------------------------------

    private fun <T> transaction(block: (Connection) -> T): T = dataSource.withConnection { connection ->
        val previousAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            block(connection).also { connection.commit() }
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = previousAutoCommit
        }
    }

    private fun insertStaffUser(connection: Connection, name: String, email: String, password: String): String = try {
        connection.returningText(
            "INSERT INTO users (name, email, password_hash, role, is_active) VALUES (?, ?, ?, 'ADMIN', TRUE) RETURNING id::text",
            name,
            email,
            PasswordHasher.hash(password),
        )
    } catch (error: SQLException) {
        if (error.sqlState == UNIQUE_VIOLATION) throw ServerConflictException("Email is already used by another account")
        throw error
    }

    private fun insertMember(connection: Connection, organizationId: String, userId: String, role: String) {
        connection.prepareStatement(
            "INSERT INTO organization_members (organization_id, user_id, role) VALUES (CAST(? AS uuid), CAST(? AS uuid), ?)",
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.setString(2, userId)
            statement.setString(3, role)
            statement.executeUpdate()
        }
    }

    private fun recordLegalAcceptances(connection: Connection, userId: String, version: String) {
        connection.prepareStatement(
            "INSERT INTO legal_acceptances (user_id, document, version) VALUES (CAST(? AS uuid), ?, ?)",
        ).use { statement ->
            listOf("TERMS", "PRIVACY").forEach { document ->
                statement.setString(1, userId)
                statement.setString(2, document)
                statement.setString(3, version)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun requireAnotherActiveOwner(connection: Connection, organizationId: String, exceptUserId: String) {
        val others = connection.prepareStatement(
            """
            SELECT COUNT(*) FROM organization_members m
            JOIN users u ON u.id = m.user_id AND u.is_active = TRUE
            WHERE m.organization_id::text = ? AND m.role = 'OWNER' AND m.user_id::text <> ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.setString(2, exceptUserId)
            statement.executeQuery().use { it.next(); it.getInt(1) }
        }
        if (others == 0) throw ServerConflictException("A company always needs at least one active owner")
    }

    private fun identity(connection: Connection, userId: String): ServerAuthIdentity? =
        connection.prepareStatement(
            """
            SELECT u.id::text AS user_id, u.name, u.email::text AS email, u.role, u.client_id::text AS client_id,
                   $ORGANIZATION_COLUMNS
            FROM users u
            $ORGANIZATION_JOINS
            WHERE u.id::text = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, userId)
            statement.executeQuery().use { if (it.next()) it.toAuthIdentity() else null }
        }

    private fun organization(connection: Connection, organizationId: String): ServerOrganizationSnapshot =
        connection.prepareStatement(
            """
            SELECT id::text, name, legal_form, sector, team_size, has_accountant, onboarding_goals,
                   onboarding_completed_at, legal_name, tax_id, address_line, postal_code, city,
                   province, country, created_at, local_pdf_invoices
            FROM organizations WHERE id::text = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.executeQuery().use { resultSet ->
                if (!resultSet.next()) throw ServerNotFoundException("Organization not found")
                ServerOrganizationSnapshot(
                    id = resultSet.getString("id"),
                    name = resultSet.getString("name"),
                    legalForm = resultSet.getString("legal_form"),
                    sector = resultSet.getString("sector"),
                    teamSize = resultSet.getString("team_size"),
                    hasAccountant = resultSet.getObject("has_accountant") as Boolean?,
                    onboardingGoals = (resultSet.getArray("onboarding_goals")?.array as? Array<*>)
                        ?.filterIsInstance<String>().orEmpty(),
                    onboardingCompletedAt = resultSet.instant("onboarding_completed_at"),
                    legalName = resultSet.getString("legal_name"),
                    taxId = resultSet.getString("tax_id"),
                    addressLine = resultSet.getString("address_line"),
                    postalCode = resultSet.getString("postal_code"),
                    city = resultSet.getString("city"),
                    province = resultSet.getString("province"),
                    country = resultSet.getString("country"),
                    createdAt = requireNotNull(resultSet.instant("created_at")),
                    localPdfInvoices = resultSet.getBoolean("local_pdf_invoices"),
                )
            }
        }

    private fun members(connection: Connection, organizationId: String, userId: String?): List<ServerOrganizationMemberSnapshot> =
        connection.prepareStatement(
            """
            SELECT u.id::text AS user_id, u.name, u.email::text AS email, m.role, u.is_active, m.created_at
            FROM organization_members m
            JOIN users u ON u.id = m.user_id
            WHERE m.organization_id::text = ? AND (? IS NULL OR m.user_id::text = ?)
            ORDER BY (m.role = 'OWNER') DESC, m.created_at, u.name
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.setString(2, userId)
            statement.setString(3, userId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            ServerOrganizationMemberSnapshot(
                                userId = resultSet.getString("user_id"),
                                name = resultSet.getString("name"),
                                email = resultSet.getString("email"),
                                role = resultSet.getString("role"),
                                isActive = resultSet.getBoolean("is_active"),
                                joinedAt = requireNotNull(resultSet.instant("created_at")),
                            ),
                        )
                    }
                }
            }
        }

    private fun invitations(connection: Connection, organizationId: String): List<ServerOrganizationInvitationSnapshot> =
        connection.prepareStatement(
            """
            SELECT i.id::text AS id, i.role, creator.name AS created_by_name, i.created_at, i.expires_at,
                   i.accepted_at, i.revoked_at
            FROM organization_invitations i
            JOIN users creator ON creator.id = i.created_by
            WHERE i.organization_id::text = ?
            ORDER BY i.created_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, organizationId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            ServerOrganizationInvitationSnapshot(
                                id = resultSet.getString("id"),
                                role = resultSet.getString("role"),
                                createdByName = resultSet.getString("created_by_name"),
                                createdAt = requireNotNull(resultSet.instant("created_at")),
                                expiresAt = requireNotNull(resultSet.instant("expires_at")),
                                acceptedAt = resultSet.instant("accepted_at"),
                                revokedAt = resultSet.instant("revoked_at"),
                            ),
                        )
                    }
                }
            }
        }

    private fun ResultSet.instant(column: String): String? = getTimestamp(column)?.toInstant()?.toString()

    private fun Connection.returningText(sql: String, vararg values: String): String =
        prepareStatement(sql).use { statement ->
            values.forEachIndexed { index, value -> statement.setString(index + 1, value) }
            statement.executeQuery().use { it.next(); it.getString(1) }
        }

    private companion object {
        const val UNIQUE_VIOLATION = "23505"
    }
}
