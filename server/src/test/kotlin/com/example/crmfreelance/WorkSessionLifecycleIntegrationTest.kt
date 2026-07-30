package com.example.crmfreelance

import com.requena.supportdesk.server.config.DatabaseSettings
import com.requena.supportdesk.server.data.datasource.PostgresDemoBootstrapper
import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import com.requena.supportdesk.server.data.repository.PostgresSupportDeskRepository
import com.requena.supportdesk.server.domain.model.CreateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.CreateTaskRequest
import com.requena.supportdesk.server.domain.model.ServerConflictException
import com.requena.supportdesk.server.domain.model.StartWorkSessionRequest
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the real work-session/segment engine against real Postgres: the single-active-timer
 * guarantee, pause/resume producing separate segments, and - most bug-prone - a session that
 * crosses local (Europe/Madrid) midnight splitting into two immutable time_logs rows without
 * inventing a second session. Segment timestamps are set directly via SQL where the assertion
 * needs a controlled instant (e.g. the midnight split), since the repository itself always
 * stamps segments with the real Postgres clock (NOW()) and has no injectable clock.
 */
class WorkSessionLifecycleIntegrationTest {
    @Test
    fun sessionLifecycleEnforcesSingleActiveTimerAndSplitsMidnightCorrectly() {
        EmbeddedPostgres.start().use { postgres ->
            postgres.postgresDatabase.connection.use { connection ->
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
                dataSource.migrate()
                PostgresDemoBootstrapper(dataSource).bootstrap(
                    adminPassword = "IntegrationAdminPassword1",
                    clientPassword = "IntegrationClientPassword1",
                )
                val repository = PostgresSupportDeskRepository(dataSource)
                val admin = requireNotNull(repository.authenticate("admin@orykai.dev", "IntegrationAdminPassword1"))
                val ownerAdminId = admin.userId

                val label = repository.createTaskLabel(
                    CreateTaskLabelRequest(ownerAdminId = ownerAdminId, name = "WorkSession Test", colorHex = "#334455"),
                    ownerAdminId,
                )
                val taskA = repository.createTask(
                    CreateTaskRequest(title = "Tarea A", description = "", clientId = null, labelId = label.id),
                    createdByUserId = ownerAdminId,
                    ownerAdminId = ownerAdminId,
                )
                val taskB = repository.createTask(
                    CreateTaskRequest(title = "Tarea B", description = "", clientId = null, labelId = label.id),
                    createdByUserId = ownerAdminId,
                    ownerAdminId = ownerAdminId,
                )

                // No active session initially.
                assertNull(repository.activeWorkSession(ownerAdminId))

                // Starting a session persists it immediately, with exactly one open segment.
                val session = repository.startedWorkSession(StartWorkSessionRequest(taskA.id), ownerAdminId, ownerAdminId)
                assertEquals("ACTIVE", session.status)
                assertEquals(1, session.segments.size)
                assertNotNull(session.openSegmentStartedAt)

                // A second timer on a different task must be rejected while one is live - both
                // at the domain layer and (if that check were bypassed) by the DB partial unique
                // index, never silently pausing the first one.
                assertFailsWith<ServerConflictException> {
                    repository.startedWorkSession(StartWorkSessionRequest(taskB.id), ownerAdminId, ownerAdminId)
                }

                // Pause closes the open segment; the session is recoverable exactly as paused.
                val paused = repository.pausedWorkSession(session.id, ownerAdminId)
                assertEquals("PAUSED", paused.status)
                assertNull(paused.openSegmentStartedAt)
                assertEquals(1, paused.segments.count { it.endedAt != null })

                // Resume opens a second, distinct segment within the SAME session - not a new session.
                val resumed = repository.resumedWorkSession(session.id, ownerAdminId)
                assertEquals("ACTIVE", resumed.status)
                assertEquals(2, resumed.segments.size)
                assertEquals(session.id, resumed.id)

                // A duplicate checkpoint call is idempotent: it never creates a row or changes duration.
                repository.checkpointedWorkSession(session.id, ownerAdminId)
                val afterSecondCheckpoint = repository.checkpointedWorkSession(session.id, ownerAdminId)
                assertEquals(2, afterSecondCheckpoint.segments.size)

                // --- Midnight-crossing split -------------------------------------------------
                // Force the currently-open segment to an ALREADY-CLOSED, controlled window that
                // crosses Europe/Madrid local midnight: 23:30 -> 01:00 the next day (30 min on
                // day 1, 60 min on day 2 - the exact example from the spec). Setting ended_at
                // here (not NULL) matters: stoppedWorkSession only overwrites segments that are
                // still open, so this is what makes the split deterministic instead of using
                // the real test-execution clock.
                dataSource.withConnection { connection ->
                    connection.prepareStatement(
                        """
                        UPDATE work_segments
                        SET started_at = TIMESTAMPTZ '2026-07-30 23:30:00+02', ended_at = TIMESTAMPTZ '2026-07-31 01:00:00+02'
                        WHERE session_id::text = ? AND ended_at IS NULL
                        """.trimIndent(),
                    ).use { statement ->
                        statement.setString(1, session.id)
                        statement.executeUpdate()
                    }
                }

                val stopped = repository.stoppedWorkSession(session.id, ownerAdminId)
                assertEquals("FINISHED", stopped.status)

                val timeLogsForTaskA = repository.getTimeLogs(clientId = null, taskId = taskA.id, ownerAdminId = ownerAdminId)
                    .filter { it.sessionId == session.id }
                val byDate = timeLogsForTaskA.associateBy { it.workDate }
                assertEquals(30, byDate.getValue("2026-07-30").minutes)
                assertEquals(60, byDate.getValue("2026-07-31").minutes)
                assertTrue(timeLogsForTaskA.all { !it.isHistoricMigrated })

                // Once finished, starting/pausing/stopping again must be a controlled error, not a
                // silent no-op or a second live session.
                assertFailsWith<ServerConflictException> { repository.pausedWorkSession(session.id, ownerAdminId) }
                assertFailsWith<ServerConflictException> { repository.stoppedWorkSession(session.id, ownerAdminId) }
                assertNull(repository.activeWorkSession(ownerAdminId))
            }
        }
    }
}
