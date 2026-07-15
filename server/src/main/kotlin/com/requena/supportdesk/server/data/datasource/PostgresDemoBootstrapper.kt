package com.requena.supportdesk.server.data.datasource

import com.requena.supportdesk.server.security.PasswordHasher
import java.sql.Connection
import java.sql.PreparedStatement

class PostgresDemoBootstrapper(
    private val dataSource: PostgresSupportDeskDataSource,
) {
    fun bootstrap(adminPassword: String, clientPassword: String) {
        dataSource.withConnection { connection ->
            connection.autoCommit = false
            try {
                val primaryAdminId = ensureAdminUser(
                    connection = connection,
                    name = "Admin Requena",
                    email = "admin@orykai.dev",
                    password = adminPassword,
                )
                ensureAdminUser(
                    connection = connection,
                    name = "Admin Sanchez",
                    email = "admin2@orykai.dev",
                    password = adminPassword,
                )
                val clientId = ensureClient(connection, primaryAdminId)
                val clientUserId = ensureClientUser(connection, clientId, clientPassword)
                val labelId = ensureTaskLabel(connection, primaryAdminId)
                val taskId = ensureTask(connection, clientId, primaryAdminId, labelId)
                ensureTimeLog(connection, taskId, clientId, primaryAdminId)
                ensureTicket(connection, clientId, clientUserId, primaryAdminId)
                connection.commit()
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun ensureAdminUser(
        connection: Connection,
        name: String,
        email: String,
        password: String,
    ): String = connection.returningId(
        """
        INSERT INTO users (name, email, password_hash, role, is_active)
        VALUES (?, ?, ?, 'ADMIN', TRUE)
        ON CONFLICT (email) DO UPDATE SET
            name = EXCLUDED.name,
            password_hash = EXCLUDED.password_hash,
            role = EXCLUDED.role,
            client_id = NULL,
            is_active = TRUE
        RETURNING id::text
        """.trimIndent(),
    ) {
        setString(1, name)
        setString(2, email)
        setString(3, PasswordHasher.hash(password))
    }

    private fun ensureClient(connection: Connection, ownerAdminId: String): String = connection.returningId(
        """
        INSERT INTO clients (
            owner_admin_id, company_name, product_name, contact_name, email,
            account_status, service_tier, preferred_contact_channel
        )
        VALUES (
            CAST(? AS uuid), 'Requena Demo Client', 'Requena Mobile Suite', 'Demo Client',
            'client@orykai.local', 'ACTIVE', 'PRIORITY', 'TICKET'
        )
        ON CONFLICT (email) DO UPDATE SET
            owner_admin_id = EXCLUDED.owner_admin_id,
            company_name = EXCLUDED.company_name,
            product_name = EXCLUDED.product_name,
            contact_name = EXCLUDED.contact_name,
            account_status = EXCLUDED.account_status,
            service_tier = EXCLUDED.service_tier,
            preferred_contact_channel = EXCLUDED.preferred_contact_channel
        RETURNING id::text
        """.trimIndent(),
    ) {
        setString(1, ownerAdminId)
    }

    private fun ensureClientUser(
        connection: Connection,
        clientId: String,
        password: String,
    ): String = connection.returningId(
        """
        INSERT INTO users (client_id, name, email, password_hash, role, is_active)
        VALUES (CAST(? AS uuid), 'Demo Client User', 'client.user@orykai.local', ?, 'CLIENT', TRUE)
        ON CONFLICT (email) DO UPDATE SET
            client_id = EXCLUDED.client_id,
            name = EXCLUDED.name,
            password_hash = EXCLUDED.password_hash,
            role = EXCLUDED.role,
            is_active = TRUE
        RETURNING id::text
        """.trimIndent(),
    ) {
        setString(1, clientId)
        setString(2, PasswordHasher.hash(password))
    }

    private fun ensureTaskLabel(connection: Connection, ownerAdminId: String): String = connection.returningId(
        """
        INSERT INTO task_labels (owner_admin_id, name, color_hex)
        VALUES (CAST(? AS uuid), 'Hoy', '#6B7A5B')
        ON CONFLICT (owner_admin_id, name) DO UPDATE SET
            color_hex = EXCLUDED.color_hex
        RETURNING id::text
        """.trimIndent(),
    ) {
        setString(1, ownerAdminId)
    }

    private fun ensureTask(
        connection: Connection,
        clientId: String,
        ownerAdminId: String,
        labelId: String,
    ): String = connection.returningId(
        """
        INSERT INTO tasks (
            id, client_id, owner_admin_id, label_id, title, description,
            due_date, completed, logged_minutes, logged_seconds
        )
        VALUES (
            CAST('66666666-6666-6666-6666-666666666666' AS uuid),
            CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid),
            'Demo task', 'Operational task to validate dashboard, timer and client linkage.',
            CURRENT_DATE + INTERVAL '1 day', FALSE, 45, 2700
        )
        ON CONFLICT (id) DO UPDATE SET
            client_id = EXCLUDED.client_id,
            owner_admin_id = EXCLUDED.owner_admin_id,
            label_id = EXCLUDED.label_id,
            title = EXCLUDED.title,
            description = EXCLUDED.description
        RETURNING id::text
        """.trimIndent(),
    ) {
        setString(1, clientId)
        setString(2, ownerAdminId)
        setString(3, labelId)
    }

    private fun ensureTimeLog(
        connection: Connection,
        taskId: String,
        clientId: String,
        authorId: String,
    ) {
        connection.prepareStatement(
            """
            INSERT INTO time_logs (
                id, task_id, client_id, author_id, minutes, seconds, work_date, note, billable
            )
            VALUES (
                CAST('77777777-7777-7777-7777-777777777777' AS uuid),
                CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid),
                45, 2700, CURRENT_DATE, 'Demo time log for admin dashboard', TRUE
            )
            ON CONFLICT (id) DO UPDATE SET
                task_id = EXCLUDED.task_id,
                client_id = EXCLUDED.client_id,
                author_id = EXCLUDED.author_id,
                minutes = EXCLUDED.minutes,
                seconds = EXCLUDED.seconds,
                work_date = EXCLUDED.work_date,
                note = EXCLUDED.note,
                billable = EXCLUDED.billable
            """.trimIndent(),
        ).use {
            it.setString(1, taskId)
            it.setString(2, clientId)
            it.setString(3, authorId)
            it.executeUpdate()
        }
    }

    private fun ensureTicket(
        connection: Connection,
        clientId: String,
        requesterId: String,
        assigneeId: String,
    ) {
        connection.prepareStatement(
            """
            INSERT INTO tickets (
                client_id, requester_id, assignee_id, ticket_number, subject, description, category,
                affected_app, platform, app_version, client_reference, status, priority, waiting_on
            )
            VALUES (
                CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), 'RDS-000001',
                'Demo ticket', 'This ticket exists to validate the first backend connection.',
                'QUESTION', 'Requena Mobile Suite', 'ANDROID', '1.0.0', 'DEMO-REF-1',
                'OPEN', 'MEDIUM', 'ADMIN'
            )
            ON CONFLICT (ticket_number) DO UPDATE SET
                client_id = EXCLUDED.client_id,
                requester_id = EXCLUDED.requester_id,
                assignee_id = EXCLUDED.assignee_id,
                subject = EXCLUDED.subject,
                description = EXCLUDED.description,
                affected_app = EXCLUDED.affected_app,
                app_version = EXCLUDED.app_version,
                client_reference = EXCLUDED.client_reference
            """.trimIndent(),
        ).use {
            it.setString(1, clientId)
            it.setString(2, requesterId)
            it.setString(3, assigneeId)
            it.executeUpdate()
        }
    }

    private fun Connection.returningId(
        sql: String,
        bind: PreparedStatement.() -> Unit,
    ): String = prepareStatement(sql).use { statement ->
        statement.bind()
        statement.executeQuery().use { result ->
            check(result.next()) { "Bootstrap statement did not return an id." }
            result.getString(1)
        }
    }
}
