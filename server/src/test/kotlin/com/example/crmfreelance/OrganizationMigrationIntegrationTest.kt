package com.example.crmfreelance

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.flywaydb.core.Flyway
import java.sql.Connection
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * V17 backfill on real Postgres: data created before companies existed (two admins, each
 * with its own clients, labels, tasks and timer sessions) ends up in one company per admin,
 * with that admin as OWNER, and no row is lost or moved to the wrong company.
 */
class OrganizationMigrationIntegrationTest {

    @Test
    fun v17GivesEachExistingAdminItsOwnCompanyWithoutLosingRows() {
        EmbeddedPostgres.start().use { postgres ->
            val dataSource = postgres.postgresDatabase
            dataSource.connection.use { connection ->
                connection.exec("CREATE ROLE anon NOLOGIN")
                connection.exec("CREATE ROLE authenticated NOLOGIN")
            }

            flyway(dataSource, target = "16").migrate()

            dataSource.connection.use { connection ->
                connection.exec(
                    """
                    INSERT INTO users (id, name, email, password_hash, role, created_at) VALUES
                      ('00000000-0000-0000-0000-00000000000a', 'Fran', 'fran@orykai.dev', 'x', 'ADMIN', NOW() - INTERVAL '2 days'),
                      ('00000000-0000-0000-0000-00000000000b', 'Bea', 'bea@other.dev', 'x', 'ADMIN', NOW() - INTERVAL '1 day');

                    INSERT INTO clients (id, owner_admin_id, company_name, product_name, contact_name, email) VALUES
                      ('10000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-00000000000a', 'Cliente A1', 'P', 'C', 'a1@x.dev'),
                      ('10000000-0000-0000-0000-00000000000c', '00000000-0000-0000-0000-00000000000a', 'Cliente A2', 'P', 'C', 'a2@x.dev'),
                      ('10000000-0000-0000-0000-00000000000b', '00000000-0000-0000-0000-00000000000b', 'Cliente B1', 'P', 'C', 'b1@x.dev');

                    INSERT INTO users (name, email, password_hash, role, client_id) VALUES
                      ('Portal A1', 'portal-a1@x.dev', 'x', 'CLIENT', '10000000-0000-0000-0000-00000000000a');

                    INSERT INTO task_labels (id, owner_admin_id, name, color_hex) VALUES
                      ('20000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-00000000000a', 'Hoy', '#112233'),
                      ('20000000-0000-0000-0000-00000000000b', '00000000-0000-0000-0000-00000000000b', 'Hoy', '#112233');

                    INSERT INTO tasks (id, owner_admin_id, created_by_user_id, label_id, client_id, title) VALUES
                      ('30000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-00000000000a', '20000000-0000-0000-0000-00000000000a', '10000000-0000-0000-0000-00000000000a', 'Tarea A'),
                      ('30000000-0000-0000-0000-00000000000b', '00000000-0000-0000-0000-00000000000b', '00000000-0000-0000-0000-00000000000b', '20000000-0000-0000-0000-00000000000b', NULL, 'Tarea B');

                    INSERT INTO work_sessions (task_id, owner_admin_id, client_id, author_id, started_at, billable) VALUES
                      ('30000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-00000000000a', '10000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-00000000000a', NOW(), TRUE),
                      ('30000000-0000-0000-0000-00000000000b', '00000000-0000-0000-0000-00000000000b', NULL, '00000000-0000-0000-0000-00000000000b', NOW(), FALSE);
                    """.trimIndent(),
                )
            }
            val countsBefore = dataSource.connection.use(::rowCounts)

            assertEquals(2, flyway(dataSource, target = null).migrate().migrationsExecuted)

            dataSource.connection.use { connection ->
                assertEquals(countsBefore, rowCounts(connection), "no row may be lost")

                // One company per admin; the oldest admin's company is OryKai.
                assertEquals(
                    listOf("Bea" to "Empresa de Bea", "Fran" to "OryKai"),
                    connection.pairs(
                        """
                        SELECT u.name, o.name FROM organization_members m
                        JOIN users u ON u.id = m.user_id
                        JOIN organizations o ON o.id = m.organization_id
                        WHERE m.role = 'OWNER' ORDER BY u.name
                        """.trimIndent(),
                    ),
                )

                // Every root row now carries the company of the admin that owned it.
                listOf("clients", "task_labels", "tasks", "work_sessions").forEach { table ->
                    val misplaced = connection.single(
                        """
                        SELECT COUNT(*) FROM $table t
                        LEFT JOIN organization_members m ON m.user_id = t.owner_admin_id
                        WHERE t.organization_id IS NULL OR t.organization_id <> m.organization_id
                        """.trimIndent(),
                    )
                    assertEquals("0", misplaced, "$table rows in the wrong company")
                }

                // Same label name in two companies is allowed; uniqueness is per company now.
                assertEquals("2", connection.single("SELECT COUNT(*) FROM task_labels WHERE name = 'Hoy'"))

                // The views expose the company so dashboards can be filtered per tenant.
                val perCompany = connection.single("SELECT COUNT(*) FROM admin_dashboard_summary")
                assertEquals("2", perCompany)
                assertTrue(connection.single("SELECT COUNT(*) FROM organizations").toInt() == 2)

                // V18: only OryKai keeps the desktop's local PDF invoices (lane A).
                assertEquals(
                    listOf("Empresa de Bea" to "false", "OryKai" to "true"),
                    connection.pairs("SELECT name, local_pdf_invoices::text FROM organizations ORDER BY name"),
                )
            }
        }
    }

    private fun flyway(dataSource: DataSource, target: String?) = Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .baselineVersion("0")
        .apply { if (target != null) target(target) }
        .load()

    private fun rowCounts(connection: Connection): Map<String, String> =
        listOf("users", "clients", "task_labels", "tasks", "work_sessions")
            .associateWith { connection.single("SELECT COUNT(*) FROM $it") }

    private fun Connection.exec(sql: String) {
        createStatement().use { it.execute(sql) }
    }

    private fun Connection.single(sql: String): String =
        createStatement().use { statement ->
            statement.executeQuery(sql).use { resultSet ->
                resultSet.next()
                resultSet.getString(1)
            }
        }

    private fun Connection.pairs(sql: String): List<Pair<String, String>> =
        createStatement().use { statement ->
            statement.executeQuery(sql).use { resultSet ->
                buildList { while (resultSet.next()) add(resultSet.getString(1) to resultSet.getString(2)) }
            }
        }
}
