package com.requena.supportdesk.server.data.datasource

import com.requena.supportdesk.server.security.PasswordHasher
import java.sql.Connection

class PostgresDemoBootstrapper(
    private val dataSource: PostgresSupportDeskDataSource,
) {
    fun bootstrap(adminPassword: String, clientPassword: String) {
        dataSource.withConnection { connection ->
            connection.autoCommit = false
            try {
                ensureClient(connection)
                ensurePrimaryAdminUser(connection, adminPassword)
                ensureSecondaryAdminUser(connection, adminPassword)
                ensureClientUser(connection, clientPassword)
                ensureTaskLabel(connection)
                ensureTask(connection)
                ensureTimeLog(connection)
                ensureTicket(connection)
                connection.commit()
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun ensureClient(connection: Connection) {
        connection.prepareStatement(
            """
            INSERT INTO clients (
                id, owner_admin_id, company_name, product_name, contact_name, email, account_status, service_tier, preferred_contact_channel
            )
            VALUES (
                CAST('11111111-1111-1111-1111-111111111111' AS uuid),
                CAST('22222222-2222-2222-2222-222222222222' AS uuid),
                'Requena Demo Client',
                'Requena Mobile Suite',
                'Demo Client',
                'client@orykai.local',
                'ACTIVE',
                'PRIORITY',
                'TICKET'
            )
            ON CONFLICT (id) DO NOTHING
            """.trimIndent(),
        ).use { it.executeUpdate() }
    }

    private fun ensurePrimaryAdminUser(connection: Connection, password: String) {
        connection.prepareStatement(
            """
            INSERT INTO users (id, name, email, password_hash, role, is_active)
            VALUES (
                CAST('22222222-2222-2222-2222-222222222222' AS uuid),
                'Admin Requena',
                'admin@orykai.dev',
                ?,
                'ADMIN',
                TRUE
            )
            ON CONFLICT (email) DO NOTHING
            """.trimIndent(),
        ).use {
            it.setString(1, PasswordHasher.hash(password))
            it.executeUpdate()
        }
    }

    private fun ensureSecondaryAdminUser(connection: Connection, password: String) {
        connection.prepareStatement(
            """
            INSERT INTO users (id, name, email, password_hash, role, is_active)
            VALUES (
                CAST('88888888-8888-8888-8888-888888888888' AS uuid),
                'Admin Sanchez',
                'admin2@orykai.dev',
                ?,
                'ADMIN',
                TRUE
            )
            ON CONFLICT (email) DO NOTHING
            """.trimIndent(),
        ).use {
            it.setString(1, PasswordHasher.hash(password))
            it.executeUpdate()
        }
    }

    private fun ensureClientUser(connection: Connection, password: String) {
        connection.prepareStatement(
            """
            INSERT INTO users (id, client_id, name, email, password_hash, role, is_active)
            VALUES (
                CAST('33333333-3333-3333-3333-333333333333' AS uuid),
                CAST('11111111-1111-1111-1111-111111111111' AS uuid),
                'Demo Client User',
                'client.user@orykai.local',
                ?,
                'CLIENT',
                TRUE
            )
            ON CONFLICT (email) DO NOTHING
            """.trimIndent(),
        ).use {
            it.setString(1, PasswordHasher.hash(password))
            it.executeUpdate()
        }
    }

    private fun ensureTicket(connection: Connection) {
        connection.prepareStatement(
            """
            INSERT INTO tickets (
                id, client_id, requester_id, assignee_id, ticket_number, subject, description, category,
                affected_app, platform, app_version, client_reference, status, priority, waiting_on
            )
            VALUES (
                CAST('44444444-4444-4444-4444-444444444444' AS uuid),
                CAST('11111111-1111-1111-1111-111111111111' AS uuid),
                CAST('33333333-3333-3333-3333-333333333333' AS uuid),
                CAST('22222222-2222-2222-2222-222222222222' AS uuid),
                'RDS-000001',
                'Demo ticket',
                'This ticket exists to validate the first backend connection.',
                'QUESTION',
                'Requena Mobile Suite',
                'ANDROID',
                '1.0.0',
                'DEMO-REF-1',
                'OPEN',
                'MEDIUM',
                'ADMIN'
            )
            ON CONFLICT (id) DO NOTHING
            """.trimIndent(),
        ).use { it.executeUpdate() }
    }

    private fun ensureTaskLabel(connection: Connection) {
        connection.prepareStatement(
            """
            INSERT INTO task_labels (id, owner_admin_id, name, color_hex)
            VALUES (
                CAST('55555555-5555-5555-5555-555555555555' AS uuid),
                CAST('22222222-2222-2222-2222-222222222222' AS uuid),
                'Hoy',
                '#6B7A5B'
            )
            ON CONFLICT (id) DO NOTHING
            """.trimIndent(),
        ).use { it.executeUpdate() }
    }

    private fun ensureTask(connection: Connection) {
        connection.prepareStatement(
            """
            INSERT INTO tasks (
                id, client_id, owner_admin_id, label_id, title, description, due_date, completed, logged_minutes, logged_seconds
            )
            VALUES (
                CAST('66666666-6666-6666-6666-666666666666' AS uuid),
                CAST('11111111-1111-1111-1111-111111111111' AS uuid),
                CAST('22222222-2222-2222-2222-222222222222' AS uuid),
                CAST('55555555-5555-5555-5555-555555555555' AS uuid),
                'Demo task',
                'Operational task to validate dashboard, timer and client linkage.',
                CURRENT_DATE + INTERVAL '1 day',
                FALSE,
                45,
                2700
            )
            ON CONFLICT (id) DO NOTHING
            """.trimIndent(),
        ).use { it.executeUpdate() }
    }

    private fun ensureTimeLog(connection: Connection) {
        connection.prepareStatement(
            """
            INSERT INTO time_logs (
                id, task_id, client_id, author_id, minutes, seconds, work_date, note, billable
            )
            VALUES (
                CAST('77777777-7777-7777-7777-777777777777' AS uuid),
                CAST('66666666-6666-6666-6666-666666666666' AS uuid),
                CAST('11111111-1111-1111-1111-111111111111' AS uuid),
                CAST('22222222-2222-2222-2222-222222222222' AS uuid),
                45,
                2700,
                CURRENT_DATE,
                'Demo time log for admin dashboard',
                TRUE
            )
            ON CONFLICT (id) DO NOTHING
            """.trimIndent(),
        ).use { it.executeUpdate() }
    }
}
