package com.requena.supportdesk.server.business.projects

import com.requena.supportdesk.features.business.projects.BusinessDailyTimeEntry
import com.requena.supportdesk.features.business.projects.BusinessCommercialCustomerReference
import com.requena.supportdesk.features.business.projects.BusinessProgramMetrics
import com.requena.supportdesk.features.business.projects.BusinessProject
import com.requena.supportdesk.features.business.projects.BusinessProjectStatus
import com.requena.supportdesk.features.business.projects.BusinessProjectsWorkspace
import com.requena.supportdesk.features.business.projects.BusinessTask
import com.requena.supportdesk.features.business.projects.BusinessTaskBoard
import com.requena.supportdesk.features.business.projects.BusinessTaskBoardPlacement
import com.requena.supportdesk.features.business.projects.BusinessTaskBoardPlacementInput
import com.requena.supportdesk.features.business.projects.BusinessTaskLabel
import com.requena.supportdesk.features.business.projects.BusinessTaskPriority
import com.requena.supportdesk.features.business.projects.BusinessTaskStatus
import com.requena.supportdesk.features.business.projects.BusinessWorkSegment
import com.requena.supportdesk.features.business.projects.BusinessWorkSession
import com.requena.supportdesk.features.business.projects.BusinessWorkSessionStatus
import com.requena.supportdesk.features.business.projects.CreateBusinessProjectInput
import com.requena.supportdesk.features.business.projects.CreateBusinessTaskInput
import com.requena.supportdesk.features.business.projects.CreateBusinessTaskLabelInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessProjectInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessTaskInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessTaskLabelInput
import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.UUID

class PostgresBusinessProjectsStore(
    private val dataSource: PostgresSupportDeskDataSource,
) : BusinessProjectsStore {
    override fun workspace(identity: BusinessProjectsIdentity, query: BusinessProjectsQuery): BusinessProjectsWorkspace =
        dataSource.withConnection { connection ->
            val projects = projects(connection, identity.clientId, query)
            val tasks = tasks(connection, identity.clientId, query)
            val todayInfo = connection.prepareStatement(
                """
                SELECT (CURRENT_TIMESTAMP AT TIME ZONE 'Europe/Madrid')::date::text AS today,
                       GREATEST(1, EXTRACT(EPOCH FROM (
                           date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE 'Europe/Madrid')
                           + interval '1 day'
                       ) AT TIME ZONE 'Europe/Madrid' - CURRENT_TIMESTAMP))::bigint AS refresh_seconds
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { result ->
                    result.next()
                    result.getString("today") to result.getLong("refresh_seconds")
                }
            }
            val placements = connection.prepareStatement(
                """
                SELECT placement.task_id::text AS task_id, placement.board_date::text AS board_date,
                       placement.position_x::double precision AS position_x,
                       placement.position_y::double precision AS position_y,
                       placement.updated_at::text AS updated_at
                FROM business_task_board_placements placement
                JOIN business_tasks task
                  ON task.id = placement.task_id AND task.client_id = placement.client_id
                WHERE placement.client_id = CAST(? AS uuid)
                  AND placement.board_date >= (CURRENT_TIMESTAMP AT TIME ZONE 'Europe/Madrid')::date
                  AND task.status NOT IN ('DONE', 'ARCHIVED')
                ORDER BY placement.updated_at
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, identity.clientId)
                statement.executeQuery().use { result -> result.map(::placement) }
            }
            BusinessProjectsWorkspace(
                projects = projects,
                tasks = tasks,
                labels = labels(connection, identity.clientId),
                board = BusinessTaskBoard(
                    today = todayInfo.first,
                    timeZone = "Europe/Madrid",
                    refreshAfterSeconds = todayInfo.second,
                    tasks = tasks,
                    placements = placements,
                ),
                activeWorkSession = activeSession(connection, identity),
                customers = customerReferences(connection, identity.clientId),
            )
        }

    override fun createProject(identity: BusinessProjectsIdentity, input: CreateBusinessProjectInput): BusinessProject =
        transaction { connection ->
            requireCustomer(connection, identity.clientId, input.customerId)
            val id = connection.prepareStatement(
                """
                INSERT INTO business_projects (
                    client_id, customer_id, name, description, start_date, due_date, created_by_user_id
                ) VALUES (CAST(? AS uuid), CAST(? AS uuid), ?, ?, CAST(? AS date), CAST(? AS date), CAST(? AS uuid))
                RETURNING id::text
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, identity.clientId)
                statement.nullableUuid(2, input.customerId)
                statement.setString(3, input.name)
                statement.setString(4, input.description)
                statement.nullableDate(5, input.startDate)
                statement.nullableDate(6, input.dueDate)
                statement.setString(7, identity.userId)
                statement.executeQuery().use { result -> result.next(); result.getString(1) }
            }
            audit(connection, identity, "PROJECT", id, "CREATED", 1)
            requireNotNull(project(connection, identity.clientId, id))
        }

    override fun updateProject(
        identity: BusinessProjectsIdentity,
        projectId: String,
        input: UpdateBusinessProjectInput,
    ): BusinessProject = transaction { connection ->
        requireCustomer(connection, identity.clientId, input.customerId)
        val updated = connection.prepareStatement(
            """
            UPDATE business_projects
            SET customer_id=CAST(? AS uuid), name=?, description=?, start_date=CAST(? AS date),
                due_date=CAST(? AS date), status=?, completed_at=CASE WHEN ?='COMPLETED' THEN COALESCE(completed_at, NOW()) ELSE NULL END,
                version=version+1
            WHERE client_id=CAST(? AS uuid) AND id=CAST(? AS uuid) AND version=?
            RETURNING id::text
            """.trimIndent(),
        ).use { statement ->
            statement.nullableUuid(1, input.customerId)
            statement.setString(2, input.name)
            statement.setString(3, input.description)
            statement.nullableDate(4, input.startDate)
            statement.nullableDate(5, input.dueDate)
            statement.setString(6, input.status.name)
            statement.setString(7, input.status.name)
            statement.setString(8, identity.clientId)
            statement.setString(9, projectId)
            statement.setInt(10, input.expectedVersion)
            statement.executeQuery().use { result -> result.nextOrStale(connection, "business_projects", identity.clientId, projectId) }
        }
        audit(connection, identity, "PROJECT", updated, "UPDATED", input.expectedVersion + 1)
        requireNotNull(project(connection, identity.clientId, updated))
    }

    override fun transitionProject(
        identity: BusinessProjectsIdentity,
        projectId: String,
        target: BusinessProjectStatus,
        expectedVersion: Int,
    ): BusinessProject = transaction { connection ->
        val updated = connection.prepareStatement(
            """
            UPDATE business_projects
            SET status=?, completed_at=CASE WHEN ?='COMPLETED' THEN NOW() ELSE NULL END, version=version+1
            WHERE client_id=CAST(? AS uuid) AND id=CAST(? AS uuid) AND version=?
            RETURNING id::text
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, target.name)
            statement.setString(2, target.name)
            statement.setString(3, identity.clientId)
            statement.setString(4, projectId)
            statement.setInt(5, expectedVersion)
            statement.executeQuery().use { result -> result.nextOrStale(connection, "business_projects", identity.clientId, projectId) }
        }
        audit(connection, identity, "PROJECT", updated, target.name, expectedVersion + 1)
        requireNotNull(project(connection, identity.clientId, updated))
    }

    override fun createTask(identity: BusinessProjectsIdentity, input: CreateBusinessTaskInput): BusinessTask =
        transaction { connection ->
            requireProject(connection, identity.clientId, input.projectId)
            requireCustomer(connection, identity.clientId, input.customerId)
            requireLabels(connection, identity.clientId, input.labelIds)
            val id = connection.prepareStatement(
                """
                INSERT INTO business_tasks (
                    client_id, project_id, direct_customer_id, title, description, priority,
                    due_date, created_by_user_id
                ) VALUES (
                    CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), ?, ?, ?, CAST(? AS date), CAST(? AS uuid)
                )
                RETURNING id::text
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, identity.clientId)
                statement.nullableUuid(2, input.projectId)
                statement.nullableUuid(3, input.customerId)
                statement.setString(4, input.title)
                statement.setString(5, input.description)
                statement.setString(6, input.priority.name)
                statement.nullableDate(7, input.dueDate)
                statement.setString(8, identity.userId)
                statement.executeQuery().use { result -> result.next(); result.getString(1) }
            }
            replaceTaskLabels(connection, identity, id, input.labelIds)
            audit(connection, identity, "TASK", id, "CREATED", 1)
            requireNotNull(task(connection, identity.clientId, id))
        }

    override fun updateTask(
        identity: BusinessProjectsIdentity,
        taskId: String,
        input: UpdateBusinessTaskInput,
    ): BusinessTask = transaction { connection ->
        requireProject(connection, identity.clientId, input.projectId)
        requireCustomer(connection, identity.clientId, input.customerId)
        requireLabels(connection, identity.clientId, input.labelIds)
        val id = connection.prepareStatement(
            """
            UPDATE business_tasks
            SET project_id=CAST(? AS uuid), direct_customer_id=CAST(? AS uuid), title=?, description=?,
                priority=?, due_date=CAST(? AS date), status=?,
                completed_at=CASE WHEN ?='DONE' THEN COALESCE(completed_at, NOW()) ELSE NULL END,
                version=version+1
            WHERE client_id=CAST(? AS uuid) AND id=CAST(? AS uuid) AND version=?
            RETURNING id::text
            """.trimIndent(),
        ).use { statement ->
            statement.nullableUuid(1, input.projectId)
            statement.nullableUuid(2, input.customerId)
            statement.setString(3, input.title)
            statement.setString(4, input.description)
            statement.setString(5, input.priority.name)
            statement.nullableDate(6, input.dueDate)
            statement.setString(7, input.status.name)
            statement.setString(8, input.status.name)
            statement.setString(9, identity.clientId)
            statement.setString(10, taskId)
            statement.setInt(11, input.expectedVersion)
            statement.executeQuery().use { result -> result.nextOrStale(connection, "business_tasks", identity.clientId, taskId) }
        }
        replaceTaskLabels(connection, identity, id, input.labelIds)
        audit(connection, identity, "TASK", id, "UPDATED", input.expectedVersion + 1)
        requireNotNull(task(connection, identity.clientId, id))
    }

    override fun createLabel(identity: BusinessProjectsIdentity, input: CreateBusinessTaskLabelInput): BusinessTaskLabel =
        transaction { connection ->
            try {
                val id = connection.prepareStatement(
                    """
                    INSERT INTO business_task_labels (client_id, name, color_hex, created_by_user_id)
                    VALUES (CAST(? AS uuid), ?, ?, CAST(? AS uuid))
                    RETURNING id::text
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, identity.clientId)
                    statement.setString(2, input.name)
                    statement.setString(3, input.colorHex)
                    statement.setString(4, identity.userId)
                    statement.executeQuery().use { result -> result.next(); result.getString(1) }
                }
                requireNotNull(label(connection, identity.clientId, id))
            } catch (error: SQLException) {
                if (error.sqlState == UNIQUE_VIOLATION) throw BusinessProjectsConflictException("A label with that name already exists")
                throw error
            }
        }

    override fun updateLabel(
        identity: BusinessProjectsIdentity,
        labelId: String,
        input: UpdateBusinessTaskLabelInput,
    ): BusinessTaskLabel = transaction { connection ->
        try {
            val id = connection.prepareStatement(
                """
                UPDATE business_task_labels SET name=?, color_hex=?, version=version+1
                WHERE client_id=CAST(? AS uuid) AND id=CAST(? AS uuid) AND version=?
                RETURNING id::text
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, input.name)
                statement.setString(2, input.colorHex)
                statement.setString(3, identity.clientId)
                statement.setString(4, labelId)
                statement.setInt(5, input.expectedVersion)
                statement.executeQuery().use { result -> result.nextOrStale(connection, "business_task_labels", identity.clientId, labelId) }
            }
            requireNotNull(label(connection, identity.clientId, id))
        } catch (error: SQLException) {
            if (error.sqlState == UNIQUE_VIOLATION) throw BusinessProjectsConflictException("A label with that name already exists")
            throw error
        }
    }

    override fun archiveLabel(
        identity: BusinessProjectsIdentity,
        labelId: String,
        expectedVersion: Int,
    ): BusinessTaskLabel = transaction { connection ->
        val id = connection.prepareStatement(
            """
            UPDATE business_task_labels SET is_archived=TRUE, version=version+1
            WHERE client_id=CAST(? AS uuid) AND id=CAST(? AS uuid) AND version=?
            RETURNING id::text
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, identity.clientId)
            statement.setString(2, labelId)
            statement.setInt(3, expectedVersion)
            statement.executeQuery().use { result -> result.nextOrStale(connection, "business_task_labels", identity.clientId, labelId) }
        }
        requireNotNull(label(connection, identity.clientId, id))
    }

    override fun placeTask(
        identity: BusinessProjectsIdentity,
        taskId: String,
        input: BusinessTaskBoardPlacementInput,
    ): BusinessTaskBoardPlacement = transaction { connection ->
        val task = task(connection, identity.clientId, taskId) ?: throw BusinessProjectsNotFoundException()
        if (task.status in setOf(BusinessTaskStatus.DONE, BusinessTaskStatus.ARCHIVED)) {
            throw BusinessProjectsConflictException("Closed tasks cannot be planned")
        }
        val isCurrentOrFuture = connection.prepareStatement(
            "SELECT CAST(? AS date) >= (CURRENT_TIMESTAMP AT TIME ZONE 'Europe/Madrid')::date",
        ).use { statement ->
            statement.setString(1, input.boardDate)
            statement.executeQuery().use { result -> result.next() && result.getBoolean(1) }
        }
        if (!isCurrentOrFuture) throw BusinessProjectsConflictException("Past board dates are not available")
        val id = connection.prepareStatement(
            """
            INSERT INTO business_task_board_placements (
                task_id, client_id, board_date, position_x, position_y, created_by_user_id
            ) VALUES (
                CAST(? AS uuid), CAST(? AS uuid), CAST(? AS date), ?, ?, CAST(? AS uuid)
            )
            ON CONFLICT (task_id) DO UPDATE
            SET board_date=EXCLUDED.board_date, position_x=EXCLUDED.position_x,
                position_y=EXCLUDED.position_y, created_by_user_id=EXCLUDED.created_by_user_id
            WHERE business_task_board_placements.client_id=EXCLUDED.client_id
              AND EXCLUDED.board_date >= (CURRENT_TIMESTAMP AT TIME ZONE 'Europe/Madrid')::date
            RETURNING task_id::text
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, taskId)
            statement.setString(2, identity.clientId)
            statement.setString(3, input.boardDate)
            statement.setDouble(4, input.positionX)
            statement.setDouble(5, input.positionY)
            statement.setString(6, identity.userId)
            statement.executeQuery().use { result ->
                if (!result.next()) throw BusinessProjectsConflictException("Past board dates are not available")
                result.getString(1)
            }
        }
        audit(connection, identity, "BOARD", id, "PLACED", null)
        requireNotNull(boardPlacement(connection, identity.clientId, id))
    }

    override fun removeTaskPlacement(identity: BusinessProjectsIdentity, taskId: String) =
        transaction { connection ->
            requireTask(connection, identity.clientId, taskId)
            connection.prepareStatement(
                "DELETE FROM business_task_board_placements WHERE client_id=CAST(? AS uuid) AND task_id=CAST(? AS uuid)",
            ).use { statement ->
                statement.setString(1, identity.clientId)
                statement.setString(2, taskId)
                statement.executeUpdate()
            }
            audit(connection, identity, "BOARD", taskId, "REMOVED", null)
        }

    override fun activeWorkSession(identity: BusinessProjectsIdentity): BusinessWorkSession? =
        dataSource.withConnection { connection -> activeSession(connection, identity) }

    override fun taskWorkSessions(identity: BusinessProjectsIdentity, taskId: String): List<BusinessWorkSession> =
        dataSource.withConnection { connection ->
            requireTask(connection, identity.clientId, taskId)
            connection.prepareStatement(
                """
                SELECT id::text
                FROM business_work_sessions
                WHERE client_id=CAST(? AS uuid) AND task_id=CAST(? AS uuid)
                ORDER BY started_at DESC
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, identity.clientId)
                statement.setString(2, taskId)
                statement.executeQuery().use { result ->
                    result.map { requireNotNull(workSession(connection, identity.clientId, it.getString(1))) }
                }
            }
        }

    override fun startWorkSession(identity: BusinessProjectsIdentity, taskId: String): BusinessWorkSession =
        transaction { connection ->
            val task = task(connection, identity.clientId, taskId) ?: throw BusinessProjectsNotFoundException()
            if (task.status in setOf(BusinessTaskStatus.DONE, BusinessTaskStatus.ARCHIVED)) {
                throw BusinessProjectsConflictException("Closed tasks cannot start a timer")
            }
            activeSession(connection, identity)?.let {
                throw BusinessProjectsConflictException("There is already an active timer on '${it.taskTitle}'")
            }
            val sessionId = try {
                connection.prepareStatement(
                    """
                    INSERT INTO business_work_sessions (
                        client_id, task_id, author_user_id, started_at, status
                    ) VALUES (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), NOW(), 'ACTIVE')
                    RETURNING id::text
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, identity.clientId)
                    statement.setString(2, taskId)
                    statement.setString(3, identity.userId)
                    statement.executeQuery().use { result -> result.next(); result.getString(1) }
                }
            } catch (error: SQLException) {
                if (error.sqlState == UNIQUE_VIOLATION) throw BusinessProjectsConflictException("There is already an active timer")
                throw error
            }
            connection.prepareStatement(
                """
                INSERT INTO business_work_segments (client_id, session_id, started_at)
                VALUES (CAST(? AS uuid), CAST(? AS uuid), NOW())
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, identity.clientId)
                statement.setString(2, sessionId)
                statement.executeUpdate()
            }
            audit(connection, identity, "WORK_SESSION", sessionId, "STARTED", null)
            requireNotNull(workSession(connection, identity.clientId, sessionId))
        }

    override fun pauseWorkSession(identity: BusinessProjectsIdentity, sessionId: String): BusinessWorkSession =
        transaction { connection ->
            val status = lockLiveSession(connection, identity, sessionId)
            if (status == BusinessWorkSessionStatus.ACTIVE) {
                closeOpenSegment(connection, identity.clientId, sessionId)
                setSessionStatus(connection, identity.clientId, sessionId, BusinessWorkSessionStatus.PAUSED)
                audit(connection, identity, "WORK_SESSION", sessionId, "PAUSED", null)
            }
            requireNotNull(workSession(connection, identity.clientId, sessionId))
        }

    override fun resumeWorkSession(identity: BusinessProjectsIdentity, sessionId: String): BusinessWorkSession =
        transaction { connection ->
            val status = lockLiveSession(connection, identity, sessionId)
            if (status == BusinessWorkSessionStatus.PAUSED) {
                connection.prepareStatement(
                    """
                    INSERT INTO business_work_segments (client_id, session_id, started_at)
                    VALUES (CAST(? AS uuid), CAST(? AS uuid), NOW())
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, identity.clientId)
                    statement.setString(2, sessionId)
                    statement.executeUpdate()
                }
                setSessionStatus(connection, identity.clientId, sessionId, BusinessWorkSessionStatus.ACTIVE)
                audit(connection, identity, "WORK_SESSION", sessionId, "RESUMED", null)
            }
            requireNotNull(workSession(connection, identity.clientId, sessionId))
        }

    override fun stopWorkSession(identity: BusinessProjectsIdentity, sessionId: String): BusinessWorkSession =
        transaction { connection ->
            lockLiveSession(connection, identity, sessionId)
            closeOpenSegment(connection, identity.clientId, sessionId)
            connection.prepareStatement(
                """
                UPDATE business_work_sessions
                SET status='FINISHED', ended_at=NOW(), last_checkpoint_at=NOW()
                WHERE client_id=CAST(? AS uuid) AND id=CAST(? AS uuid)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, identity.clientId)
                statement.setString(2, sessionId)
                statement.executeUpdate()
            }
            rebuildDailyEntries(connection, identity.clientId, sessionId)
            audit(connection, identity, "WORK_SESSION", sessionId, "STOPPED", null)
            requireNotNull(workSession(connection, identity.clientId, sessionId))
        }

    override fun checkpointWorkSession(identity: BusinessProjectsIdentity, sessionId: String): BusinessWorkSession =
        transaction { connection ->
            lockLiveSession(connection, identity, sessionId)
            connection.prepareStatement(
                """
                UPDATE business_work_sessions SET last_checkpoint_at=NOW()
                WHERE client_id=CAST(? AS uuid) AND id=CAST(? AS uuid)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, identity.clientId)
                statement.setString(2, sessionId)
                statement.executeUpdate()
            }
            requireNotNull(workSession(connection, identity.clientId, sessionId))
        }

    override fun metrics(clientId: String): BusinessProgramMetrics = dataSource.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT
                (SELECT COUNT(*) FROM business_sales_customers WHERE client_id=CAST(? AS uuid) AND status='ACTIVE')::int AS customers,
                (SELECT COUNT(*) FROM business_projects WHERE client_id=CAST(? AS uuid) AND status='ACTIVE')::int AS projects,
                (SELECT COUNT(*) FROM business_tasks WHERE client_id=CAST(? AS uuid) AND status NOT IN ('DONE','ARCHIVED'))::int AS tasks,
                (
                    SELECT MAX(activity_at)::text
                    FROM (
                        SELECT MAX(created_at) AS activity_at FROM business_sales_audit_events WHERE client_id=CAST(? AS uuid)
                        UNION ALL
                        SELECT MAX(created_at) AS activity_at FROM business_project_audit_events WHERE client_id=CAST(? AS uuid)
                    ) activity
                ) AS last_activity_at
            """.trimIndent(),
        ).use { statement ->
            repeat(5) { index -> statement.setString(index + 1, clientId) }
            statement.executeQuery().use { result ->
                result.next()
                BusinessProgramMetrics(
                    clientId = clientId,
                    commercialCustomerCount = result.getInt("customers"),
                    activeProjectCount = result.getInt("projects"),
                    openTaskCount = result.getInt("tasks"),
                    lastActivityAt = result.getString("last_activity_at"),
                )
            }
        }
    }

    private fun projects(connection: Connection, clientId: String, query: BusinessProjectsQuery): List<BusinessProject> =
        connection.prepareStatement(
            """
            SELECT project.id::text AS id, project.customer_id::text AS customer_id,
                   customer.display_name AS customer_name, project.name, project.description,
                   project.start_date::text AS start_date, project.due_date::text AS due_date,
                   project.status, project.version, project.updated_at::text AS updated_at,
                   COUNT(DISTINCT task.id) FILTER (WHERE task.status NOT IN ('DONE','ARCHIVED'))::int AS open_task_count,
                   COALESCE(SUM(entry.seconds), 0)::bigint AS total_seconds
            FROM business_projects project
            LEFT JOIN business_sales_customers customer
              ON customer.id=project.customer_id AND customer.client_id=project.client_id
            LEFT JOIN business_tasks task
              ON task.project_id=project.id AND task.client_id=project.client_id
            LEFT JOIN business_time_entries entry
              ON entry.task_id=task.id AND entry.client_id=task.client_id
            WHERE project.client_id=CAST(? AS uuid)
              AND (? IS NULL OR project.status=?)
              AND (? IS NULL OR project.customer_id=CAST(? AS uuid))
              AND (? IS NULL OR project.name ILIKE '%' || ? || '%')
            GROUP BY project.id, customer.display_name
            ORDER BY project.updated_at DESC, project.id
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.string(2, query.projectStatus)
            statement.string(3, query.projectStatus)
            statement.string(4, query.customerId)
            statement.nullableUuid(5, query.customerId)
            statement.string(6, query.query)
            statement.string(7, query.query)
            statement.executeQuery().use { it.map(::project) }
        }

    private fun customerReferences(
        connection: Connection,
        clientId: String,
    ): List<BusinessCommercialCustomerReference> =
        connection.prepareStatement(
            """
            SELECT id::text AS id, display_name
            FROM business_sales_customers
            WHERE client_id=CAST(? AS uuid) AND status='ACTIVE'
            ORDER BY lower(display_name), id
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { result ->
                result.map {
                    BusinessCommercialCustomerReference(
                        id = it.getString("id"),
                        name = it.getString("display_name"),
                    )
                }
            }
        }

    private fun project(connection: Connection, clientId: String, id: String): BusinessProject? =
        connection.prepareStatement(
            """
            SELECT project.id::text AS id, project.customer_id::text AS customer_id,
                   customer.display_name AS customer_name, project.name, project.description,
                   project.start_date::text AS start_date, project.due_date::text AS due_date,
                   project.status, project.version, project.updated_at::text AS updated_at,
                   COUNT(DISTINCT task.id) FILTER (WHERE task.status NOT IN ('DONE','ARCHIVED'))::int AS open_task_count,
                   COALESCE(SUM(entry.seconds), 0)::bigint AS total_seconds
            FROM business_projects project
            LEFT JOIN business_sales_customers customer
              ON customer.id=project.customer_id AND customer.client_id=project.client_id
            LEFT JOIN business_tasks task
              ON task.project_id=project.id AND task.client_id=project.client_id
            LEFT JOIN business_time_entries entry
              ON entry.task_id=task.id AND entry.client_id=task.client_id
            WHERE project.client_id=CAST(? AS uuid) AND project.id=CAST(? AS uuid)
            GROUP BY project.id, customer.display_name
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, id)
            statement.executeQuery().use { result -> if (result.next()) project(result) else null }
        }

    private fun tasks(connection: Connection, clientId: String, query: BusinessProjectsQuery): List<BusinessTask> =
        connection.prepareStatement(
            """
            SELECT task.id::text AS id, task.project_id::text AS project_id, project.name AS project_name,
                   COALESCE(task.direct_customer_id, project.customer_id)::text AS customer_id,
                   COALESCE(direct_customer.display_name, project_customer.display_name) AS customer_name,
                   task.title, task.description, task.priority, task.due_date::text AS due_date,
                   task.status, task.version, task.updated_at::text AS updated_at,
                   COALESCE((SELECT SUM(entry.seconds) FROM business_time_entries entry
                             WHERE entry.client_id=task.client_id AND entry.task_id=task.id), 0)::bigint AS total_seconds
            FROM business_tasks task
            LEFT JOIN business_projects project
              ON project.id=task.project_id AND project.client_id=task.client_id
            LEFT JOIN business_sales_customers direct_customer
              ON direct_customer.id=task.direct_customer_id AND direct_customer.client_id=task.client_id
            LEFT JOIN business_sales_customers project_customer
              ON project_customer.id=project.customer_id AND project_customer.client_id=project.client_id
            WHERE task.client_id=CAST(? AS uuid)
              AND (? IS NULL OR task.status=?)
              AND (? IS NULL OR task.project_id=CAST(? AS uuid))
              AND (? IS NULL OR task.direct_customer_id=CAST(? AS uuid) OR project.customer_id=CAST(? AS uuid))
              AND (? IS NULL OR EXISTS (
                    SELECT 1 FROM business_task_label_links link
                    WHERE link.client_id=task.client_id AND link.task_id=task.id AND link.label_id=CAST(? AS uuid)
              ))
              AND (? IS NULL OR task.title ILIKE '%' || ? || '%')
            ORDER BY task.updated_at DESC, task.id
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.string(2, query.taskStatus)
            statement.string(3, query.taskStatus)
            statement.string(4, query.projectId)
            statement.nullableUuid(5, query.projectId)
            statement.string(6, query.customerId)
            statement.nullableUuid(7, query.customerId)
            statement.nullableUuid(8, query.customerId)
            statement.string(9, query.labelId)
            statement.nullableUuid(10, query.labelId)
            statement.string(11, query.query)
            statement.string(12, query.query)
            statement.executeQuery().use { result -> result.map { task(it, taskLabels(connection, clientId, it.getString("id"))) } }
        }

    private fun task(connection: Connection, clientId: String, id: String): BusinessTask? =
        connection.prepareStatement(
            """
            SELECT task.id::text AS id, task.project_id::text AS project_id, project.name AS project_name,
                   COALESCE(task.direct_customer_id, project.customer_id)::text AS customer_id,
                   COALESCE(direct_customer.display_name, project_customer.display_name) AS customer_name,
                   task.title, task.description, task.priority, task.due_date::text AS due_date,
                   task.status, task.version, task.updated_at::text AS updated_at,
                   COALESCE((SELECT SUM(entry.seconds) FROM business_time_entries entry
                             WHERE entry.client_id=task.client_id AND entry.task_id=task.id), 0)::bigint AS total_seconds
            FROM business_tasks task
            LEFT JOIN business_projects project ON project.id=task.project_id AND project.client_id=task.client_id
            LEFT JOIN business_sales_customers direct_customer
              ON direct_customer.id=task.direct_customer_id AND direct_customer.client_id=task.client_id
            LEFT JOIN business_sales_customers project_customer
              ON project_customer.id=project.customer_id AND project_customer.client_id=project.client_id
            WHERE task.client_id=CAST(? AS uuid) AND task.id=CAST(? AS uuid)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, id)
            statement.executeQuery().use { result ->
                if (result.next()) task(result, taskLabels(connection, clientId, id)) else null
            }
        }

    private fun labels(connection: Connection, clientId: String): List<BusinessTaskLabel> =
        connection.prepareStatement(
            """
            SELECT id::text AS id, name, color_hex, is_archived, version, updated_at::text AS updated_at
            FROM business_task_labels WHERE client_id=CAST(? AS uuid)
            ORDER BY is_archived, lower(name), id
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.executeQuery().use { result -> result.map(::label) }
        }

    private fun label(connection: Connection, clientId: String, id: String): BusinessTaskLabel? =
        connection.prepareStatement(
            """
            SELECT id::text AS id, name, color_hex, is_archived, version, updated_at::text AS updated_at
            FROM business_task_labels WHERE client_id=CAST(? AS uuid) AND id=CAST(? AS uuid)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, id)
            statement.executeQuery().use { result -> if (result.next()) label(result) else null }
        }

    private fun taskLabels(connection: Connection, clientId: String, taskId: String): List<BusinessTaskLabel> =
        connection.prepareStatement(
            """
            SELECT label.id::text AS id, label.name, label.color_hex, label.is_archived,
                   label.version, label.updated_at::text AS updated_at
            FROM business_task_label_links link
            JOIN business_task_labels label
              ON label.id=link.label_id AND label.client_id=link.client_id
            WHERE link.client_id=CAST(? AS uuid) AND link.task_id=CAST(? AS uuid)
            ORDER BY lower(label.name), label.id
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, taskId)
            statement.executeQuery().use { it.map(::label) }
        }

    private fun boardPlacement(connection: Connection, clientId: String, taskId: String): BusinessTaskBoardPlacement? =
        connection.prepareStatement(
            """
            SELECT task_id::text AS task_id, board_date::text AS board_date,
                   position_x::double precision AS position_x,
                   position_y::double precision AS position_y, updated_at::text AS updated_at
            FROM business_task_board_placements
            WHERE client_id=CAST(? AS uuid) AND task_id=CAST(? AS uuid)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, taskId)
            statement.executeQuery().use { result -> if (result.next()) placement(result) else null }
        }

    private fun activeSession(connection: Connection, identity: BusinessProjectsIdentity): BusinessWorkSession? =
        connection.prepareStatement(
            """
            SELECT id::text FROM business_work_sessions
            WHERE client_id=CAST(? AS uuid) AND author_user_id=CAST(? AS uuid)
              AND status IN ('ACTIVE','PAUSED')
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, identity.clientId)
            statement.setString(2, identity.userId)
            statement.executeQuery().use { result ->
                if (result.next()) workSession(connection, identity.clientId, result.getString(1)) else null
            }
        }

    private fun workSession(connection: Connection, clientId: String, sessionId: String): BusinessWorkSession? {
        val header = connection.prepareStatement(
            """
            SELECT session.id::text AS id, session.task_id::text AS task_id, task.title AS task_title,
                   session.author_user_id::text AS author_user_id, session.started_at::text AS started_at,
                   session.ended_at::text AS ended_at, session.status,
                   COALESCE((
                       SELECT SUM(EXTRACT(EPOCH FROM (COALESCE(segment.ended_at, NOW()) - segment.started_at)))::bigint
                       FROM business_work_segments segment
                       WHERE segment.client_id=session.client_id AND segment.session_id=session.id
                   ), 0)::bigint AS elapsed_seconds
            FROM business_work_sessions session
            JOIN business_tasks task ON task.id=session.task_id AND task.client_id=session.client_id
            WHERE session.client_id=CAST(? AS uuid) AND session.id=CAST(? AS uuid)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, sessionId)
            statement.executeQuery().use { result ->
                if (!result.next()) return null
                SessionHeader(
                    id = result.getString("id"),
                    taskId = result.getString("task_id"),
                    taskTitle = result.getString("task_title"),
                    authorUserId = result.getString("author_user_id"),
                    startedAt = result.getString("started_at"),
                    endedAt = result.getString("ended_at"),
                    status = BusinessWorkSessionStatus.valueOf(result.getString("status")),
                    elapsedSeconds = result.getLong("elapsed_seconds"),
                )
            }
        }
        val segments = connection.prepareStatement(
            """
            SELECT id::text AS id, started_at::text AS started_at, ended_at::text AS ended_at
            FROM business_work_segments
            WHERE client_id=CAST(? AS uuid) AND session_id=CAST(? AS uuid)
            ORDER BY started_at, id
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, sessionId)
            statement.executeQuery().use { result ->
                result.map { BusinessWorkSegment(it.getString("id"), it.getString("started_at"), it.getString("ended_at")) }
            }
        }
        val entries = connection.prepareStatement(
            """
            SELECT work_date::text AS work_date, seconds
            FROM business_time_entries
            WHERE client_id=CAST(? AS uuid) AND session_id=CAST(? AS uuid)
            ORDER BY work_date
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, sessionId)
            statement.executeQuery().use { result ->
                result.map { BusinessDailyTimeEntry(it.getString("work_date"), it.getLong("seconds")) }
            }
        }
        return BusinessWorkSession(
            id = header.id,
            taskId = header.taskId,
            taskTitle = header.taskTitle,
            authorUserId = header.authorUserId,
            startedAt = header.startedAt,
            endedAt = header.endedAt,
            status = header.status,
            elapsedSeconds = header.elapsedSeconds,
            segments = segments,
            dailyEntries = entries,
        )
    }

    private fun replaceTaskLabels(
        connection: Connection,
        identity: BusinessProjectsIdentity,
        taskId: String,
        labelIds: List<String>,
    ) {
        connection.prepareStatement(
            "DELETE FROM business_task_label_links WHERE client_id=CAST(? AS uuid) AND task_id=CAST(? AS uuid)",
        ).use { statement ->
            statement.setString(1, identity.clientId)
            statement.setString(2, taskId)
            statement.executeUpdate()
        }
        labelIds.forEach { labelId ->
            connection.prepareStatement(
                """
                INSERT INTO business_task_label_links (client_id, task_id, label_id, created_by_user_id)
                VALUES (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid))
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, identity.clientId)
                statement.setString(2, taskId)
                statement.setString(3, labelId)
                statement.setString(4, identity.userId)
                statement.executeUpdate()
            }
        }
    }

    private fun requireProject(connection: Connection, clientId: String, projectId: String?) {
        if (projectId != null && project(connection, clientId, projectId) == null) throw BusinessProjectsNotFoundException()
    }

    private fun requireCustomer(connection: Connection, clientId: String, customerId: String?) {
        if (customerId == null) return
        val exists = connection.prepareStatement(
            "SELECT 1 FROM business_sales_customers WHERE client_id=CAST(? AS uuid) AND id=CAST(? AS uuid) AND status='ACTIVE'",
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, customerId)
            statement.executeQuery().use(ResultSet::next)
        }
        if (!exists) throw BusinessProjectsNotFoundException()
    }

    private fun requireTask(connection: Connection, clientId: String, taskId: String) {
        if (task(connection, clientId, taskId) == null) throw BusinessProjectsNotFoundException()
    }

    private fun requireLabels(connection: Connection, clientId: String, labelIds: List<String>) {
        labelIds.forEach { id ->
            val value = label(connection, clientId, id)
            if (value == null) throw BusinessProjectsNotFoundException()
            if (value.archived) throw BusinessProjectsConflictException("Archived labels cannot be assigned")
        }
    }

    private fun lockLiveSession(
        connection: Connection,
        identity: BusinessProjectsIdentity,
        sessionId: String,
    ): BusinessWorkSessionStatus =
        connection.prepareStatement(
            """
            SELECT status FROM business_work_sessions
            WHERE client_id=CAST(? AS uuid) AND author_user_id=CAST(? AS uuid) AND id=CAST(? AS uuid)
            FOR UPDATE
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, identity.clientId)
            statement.setString(2, identity.userId)
            statement.setString(3, sessionId)
            statement.executeQuery().use { result ->
                if (!result.next()) throw BusinessProjectsNotFoundException()
                BusinessWorkSessionStatus.valueOf(result.getString(1)).also {
                    if (it == BusinessWorkSessionStatus.FINISHED) throw BusinessProjectsConflictException("The timer is already stopped")
                }
            }
        }

    private fun closeOpenSegment(connection: Connection, clientId: String, sessionId: String) {
        connection.prepareStatement(
            """
            UPDATE business_work_segments SET ended_at=NOW()
            WHERE client_id=CAST(? AS uuid) AND session_id=CAST(? AS uuid) AND ended_at IS NULL
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, sessionId)
            statement.executeUpdate()
        }
    }

    private fun setSessionStatus(
        connection: Connection,
        clientId: String,
        sessionId: String,
        status: BusinessWorkSessionStatus,
    ) {
        connection.prepareStatement(
            """
            UPDATE business_work_sessions SET status=?
            WHERE client_id=CAST(? AS uuid) AND id=CAST(? AS uuid)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, status.name)
            statement.setString(2, clientId)
            statement.setString(3, sessionId)
            statement.executeUpdate()
        }
    }

    private fun rebuildDailyEntries(connection: Connection, clientId: String, sessionId: String) {
        connection.prepareStatement(
            "DELETE FROM business_time_entries WHERE client_id=CAST(? AS uuid) AND session_id=CAST(? AS uuid)",
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, sessionId)
            statement.executeUpdate()
        }
        connection.prepareStatement(
            """
            INSERT INTO business_time_entries (
                client_id, task_id, session_id, author_user_id, work_date, seconds
            )
            SELECT session.client_id, session.task_id, session.id, session.author_user_id,
                   day.local_date::date,
                   GREATEST(0, SUM(EXTRACT(EPOCH FROM (
                       LEAST(segment.ended_at, ((day.local_date::date + 1)::timestamp AT TIME ZONE 'Europe/Madrid'))
                       - GREATEST(segment.started_at, (day.local_date::date::timestamp AT TIME ZONE 'Europe/Madrid'))
                   ))))::int
            FROM business_work_sessions session
            JOIN business_work_segments segment
              ON segment.session_id=session.id AND segment.client_id=session.client_id
            CROSS JOIN LATERAL generate_series(
                (segment.started_at AT TIME ZONE 'Europe/Madrid')::date,
                (segment.ended_at AT TIME ZONE 'Europe/Madrid')::date,
                interval '1 day'
            ) AS day(local_date)
            WHERE session.client_id=CAST(? AS uuid) AND session.id=CAST(? AS uuid)
              AND segment.ended_at IS NOT NULL
            GROUP BY session.client_id, session.task_id, session.id, session.author_user_id, day.local_date
            HAVING SUM(EXTRACT(EPOCH FROM (
                LEAST(segment.ended_at, ((day.local_date::date + 1)::timestamp AT TIME ZONE 'Europe/Madrid'))
                - GREATEST(segment.started_at, (day.local_date::date::timestamp AT TIME ZONE 'Europe/Madrid'))
            ))) > 0
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, sessionId)
            statement.executeUpdate()
        }
    }

    private fun audit(
        connection: Connection,
        identity: BusinessProjectsIdentity,
        entityType: String,
        entityId: String,
        action: String,
        version: Int?,
    ) {
        connection.prepareStatement(
            """
            INSERT INTO business_project_audit_events (
                client_id, entity_type, entity_id, action, actor_user_id, version, metadata
            ) VALUES (CAST(? AS uuid), ?, CAST(? AS uuid), ?, CAST(? AS uuid), ?, '{}'::jsonb)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, identity.clientId)
            statement.setString(2, entityType)
            statement.setString(3, entityId)
            statement.setString(4, action)
            statement.setString(5, identity.userId)
            if (version == null) statement.setNull(6, Types.INTEGER) else statement.setInt(6, version)
            statement.executeUpdate()
        }
    }

    private fun <T> transaction(block: (Connection) -> T): T = dataSource.withConnection { connection ->
        connection.autoCommit = false
        try {
            block(connection).also { connection.commit() }
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = true
        }
    }

    private fun ResultSet.nextOrStale(connection: Connection, table: String, clientId: String, id: String): String {
        if (next()) return getString(1)
        val exists = connection.prepareStatement(
            "SELECT 1 FROM $table WHERE client_id=CAST(? AS uuid) AND id=CAST(? AS uuid)",
        ).use { statement ->
            statement.setString(1, clientId)
            statement.setString(2, id)
            statement.executeQuery().use(ResultSet::next)
        }
        if (exists) throw BusinessProjectsConflictException("The record changed; refresh and try again")
        throw BusinessProjectsNotFoundException()
    }

    private fun project(result: ResultSet) = BusinessProject(
        id = result.getString("id"),
        customerId = result.getString("customer_id"),
        customerName = result.getString("customer_name"),
        name = result.getString("name"),
        description = result.getString("description"),
        startDate = result.getString("start_date"),
        dueDate = result.getString("due_date"),
        status = BusinessProjectStatus.valueOf(result.getString("status")),
        openTaskCount = result.getInt("open_task_count"),
        totalSeconds = result.getLong("total_seconds"),
        version = result.getInt("version"),
        updatedAt = result.getString("updated_at"),
    )

    private fun task(result: ResultSet, labels: List<BusinessTaskLabel>) = BusinessTask(
        id = result.getString("id"),
        projectId = result.getString("project_id"),
        projectName = result.getString("project_name"),
        customerId = result.getString("customer_id"),
        customerName = result.getString("customer_name"),
        title = result.getString("title"),
        description = result.getString("description"),
        priority = BusinessTaskPriority.valueOf(result.getString("priority")),
        dueDate = result.getString("due_date"),
        status = BusinessTaskStatus.valueOf(result.getString("status")),
        labels = labels,
        totalSeconds = result.getLong("total_seconds"),
        version = result.getInt("version"),
        updatedAt = result.getString("updated_at"),
    )

    private fun label(result: ResultSet) = BusinessTaskLabel(
        id = result.getString("id"),
        name = result.getString("name"),
        colorHex = result.getString("color_hex"),
        archived = result.getBoolean("is_archived"),
        version = result.getInt("version"),
        updatedAt = result.getString("updated_at"),
    )

    private fun placement(result: ResultSet) = BusinessTaskBoardPlacement(
        taskId = result.getString("task_id"),
        boardDate = result.getString("board_date"),
        positionX = result.getDouble("position_x"),
        positionY = result.getDouble("position_y"),
        updatedAt = result.getString("updated_at"),
    )

    private fun <T> ResultSet.map(mapper: (ResultSet) -> T): List<T> =
        buildList { while (this@map.next()) add(mapper(this@map)) }

    private fun PreparedStatement.string(index: Int, value: String?) {
        if (value == null) setNull(index, Types.VARCHAR) else setString(index, value)
    }

    private fun PreparedStatement.nullableUuid(index: Int, value: String?) {
        if (value == null) setNull(index, Types.OTHER) else setObject(index, UUID.fromString(value))
    }

    private fun PreparedStatement.nullableDate(index: Int, value: String?) {
        if (value == null) setNull(index, Types.DATE) else setObject(index, java.time.LocalDate.parse(value))
    }

    private data class SessionHeader(
        val id: String,
        val taskId: String,
        val taskTitle: String,
        val authorUserId: String,
        val startedAt: String,
        val endedAt: String?,
        val status: BusinessWorkSessionStatus,
        val elapsedSeconds: Long,
    )

    private companion object {
        const val UNIQUE_VIOLATION = "23505"
    }
}
