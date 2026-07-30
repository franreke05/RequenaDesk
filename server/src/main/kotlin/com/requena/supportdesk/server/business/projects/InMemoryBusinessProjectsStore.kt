package com.requena.supportdesk.server.business.projects

import com.requena.supportdesk.features.business.projects.BusinessDailyTimeEntry
import com.requena.supportdesk.features.business.projects.BusinessMutationResult
import com.requena.supportdesk.features.business.projects.BusinessProgramMetrics
import com.requena.supportdesk.features.business.projects.BusinessProject
import com.requena.supportdesk.features.business.projects.BusinessProjectStatus
import com.requena.supportdesk.features.business.projects.BusinessProjectsWorkspace
import com.requena.supportdesk.features.business.projects.BusinessTask
import com.requena.supportdesk.features.business.projects.BusinessTaskBoard
import com.requena.supportdesk.features.business.projects.BusinessTaskBoardPlacement
import com.requena.supportdesk.features.business.projects.BusinessTaskBoardPlacementInput
import com.requena.supportdesk.features.business.projects.BusinessTaskLabel
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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class InMemoryBusinessProjectsStore(
    private val now: () -> Instant = Instant::now,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : BusinessProjectsStore {
    private val lock = Any()
    private val projects = linkedMapOf<String, StoredProject>()
    private val tasks = linkedMapOf<String, StoredTask>()
    private val labels = linkedMapOf<String, StoredLabel>()
    private val placements = linkedMapOf<String, StoredPlacement>()
    private val sessions = linkedMapOf<String, StoredSession>()
    private val timeZone = ZoneId.of("Europe/Madrid")

    override fun workspace(identity: BusinessProjectsIdentity, query: BusinessProjectsQuery): BusinessProjectsWorkspace = synchronized(lock) {
        val today = ZonedDateTime.ofInstant(now(), timeZone).toLocalDate()
        val visibleProjects = projects.values
            .filter { it.clientId == identity.clientId }
            .map { hydrateProject(it.value, identity.clientId) }
            .filter { query.projectStatus == null || it.status.name == query.projectStatus }
            .filter { query.customerId == null || it.customerId == query.customerId }
            .filter { query.query == null || it.name.contains(query.query, ignoreCase = true) }
        val visibleTasks = tasks.values
            .filter { it.clientId == identity.clientId }
            .map { hydrateTask(it.value, identity.clientId) }
            .filter { query.taskStatus == null || it.status.name == query.taskStatus }
            .filter { query.projectId == null || it.projectId == query.projectId }
            .filter { query.customerId == null || it.customerId == query.customerId || it.projectId?.let(projects::get)?.value?.customerId == query.customerId }
            .filter { query.labelId == null || it.labels.any { label -> label.id == query.labelId } }
            .filter { query.query == null || it.title.contains(query.query, ignoreCase = true) }
        val currentPlacements = placements.values
            .filter { it.clientId == identity.clientId && it.value.boardDate >= today.toString() }
            .map { it.value }
        BusinessProjectsWorkspace(
            projects = visibleProjects.sortedByDescending(BusinessProject::updatedAt),
            tasks = visibleTasks.sortedByDescending(BusinessTask::updatedAt),
            labels = labels.values.filter { it.clientId == identity.clientId }.map { it.value }.sortedBy(BusinessTaskLabel::name),
            board = BusinessTaskBoard(
                today = today.toString(),
                timeZone = timeZone.id,
                refreshAfterSeconds = Duration.between(now(), today.plusDays(1).atStartOfDay(timeZone).toInstant()).seconds.coerceAtLeast(1),
                tasks = visibleTasks,
                placements = currentPlacements,
            ),
            activeWorkSession = activeWorkSession(identity),
        )
    }

    override fun createProject(identity: BusinessProjectsIdentity, input: CreateBusinessProjectInput): BusinessProject = synchronized(lock) {
        val value = BusinessProject(
            id = idFactory(),
            customerId = input.customerId,
            name = input.name,
            description = input.description,
            startDate = input.startDate,
            dueDate = input.dueDate,
            status = BusinessProjectStatus.ACTIVE,
            version = 1,
            updatedAt = now().toString(),
        )
        projects[value.id] = StoredProject(identity.clientId, value)
        value
    }

    override fun updateProject(identity: BusinessProjectsIdentity, projectId: String, input: UpdateBusinessProjectInput): BusinessProject = synchronized(lock) {
        val stored = project(identity, projectId)
        requireVersion(stored.value.version, input.expectedVersion)
        val completedAtStatus = input.status
        val updated = stored.value.copy(
            customerId = input.customerId,
            name = input.name,
            description = input.description,
            startDate = input.startDate,
            dueDate = input.dueDate,
            status = completedAtStatus,
            version = input.expectedVersion + 1,
            updatedAt = now().toString(),
        )
        projects[projectId] = stored.copy(value = updated)
        updated
    }

    override fun transitionProject(
        identity: BusinessProjectsIdentity,
        projectId: String,
        target: BusinessProjectStatus,
        expectedVersion: Int,
    ): BusinessProject = synchronized(lock) {
        val stored = project(identity, projectId)
        requireVersion(stored.value.version, expectedVersion)
        val updated = stored.value.copy(status = target, version = expectedVersion + 1, updatedAt = now().toString())
        projects[projectId] = stored.copy(value = updated)
        updated
    }

    override fun createTask(identity: BusinessProjectsIdentity, input: CreateBusinessTaskInput): BusinessTask = synchronized(lock) {
        validateParents(identity, input.projectId, input.customerId)
        val taskLabels = requireLabels(identity.clientId, input.labelIds)
        val value = BusinessTask(
            id = idFactory(),
            projectId = input.projectId,
            customerId = input.customerId,
            title = input.title,
            description = input.description,
            priority = input.priority,
            dueDate = input.dueDate,
            status = BusinessTaskStatus.TODO,
            labels = taskLabels,
            version = 1,
            updatedAt = now().toString(),
        )
        tasks[value.id] = StoredTask(identity.clientId, value)
        value
    }

    override fun updateTask(identity: BusinessProjectsIdentity, taskId: String, input: UpdateBusinessTaskInput): BusinessTask = synchronized(lock) {
        validateParents(identity, input.projectId, input.customerId)
        val stored = task(identity, taskId)
        requireVersion(stored.value.version, input.expectedVersion)
        val taskLabels = requireLabels(identity.clientId, input.labelIds)
        val updated = stored.value.copy(
            projectId = input.projectId,
            customerId = input.customerId,
            title = input.title,
            description = input.description,
            priority = input.priority,
            dueDate = input.dueDate,
            status = input.status,
            labels = taskLabels,
            version = input.expectedVersion + 1,
            updatedAt = now().toString(),
        )
        tasks[taskId] = stored.copy(value = updated)
        if (updated.status in setOf(BusinessTaskStatus.DONE, BusinessTaskStatus.ARCHIVED)) placements.remove(taskId)
        updated
    }

    override fun createLabel(identity: BusinessProjectsIdentity, input: CreateBusinessTaskLabelInput): BusinessTaskLabel = synchronized(lock) {
        if (labels.values.any { it.clientId == identity.clientId && !it.value.archived && it.value.name.equals(input.name, true) }) {
            throw BusinessProjectsConflictException("A label with that name already exists")
        }
        val value = BusinessTaskLabel(idFactory(), input.name, input.colorHex, false, 1, now().toString())
        labels[value.id] = StoredLabel(identity.clientId, value)
        value
    }

    override fun updateLabel(identity: BusinessProjectsIdentity, labelId: String, input: UpdateBusinessTaskLabelInput): BusinessTaskLabel = synchronized(lock) {
        val stored = label(identity.clientId, labelId)
        requireVersion(stored.value.version, input.expectedVersion)
        val updated = stored.value.copy(name = input.name, colorHex = input.colorHex, version = input.expectedVersion + 1, updatedAt = now().toString())
        labels[labelId] = stored.copy(value = updated)
        tasks.replaceAll { _, task ->
            if (task.clientId == identity.clientId) task.copy(value = task.value.copy(labels = task.value.labels.map { if (it.id == labelId) updated else it })) else task
        }
        updated
    }

    override fun archiveLabel(identity: BusinessProjectsIdentity, labelId: String, expectedVersion: Int): BusinessTaskLabel = synchronized(lock) {
        val stored = label(identity.clientId, labelId)
        requireVersion(stored.value.version, expectedVersion)
        val updated = stored.value.copy(archived = true, version = expectedVersion + 1, updatedAt = now().toString())
        labels[labelId] = stored.copy(value = updated)
        updated
    }

    override fun placeTask(
        identity: BusinessProjectsIdentity,
        taskId: String,
        input: BusinessTaskBoardPlacementInput,
    ): BusinessTaskBoardPlacement = synchronized(lock) {
        val task = task(identity, taskId).value
        if (task.status in setOf(BusinessTaskStatus.DONE, BusinessTaskStatus.ARCHIVED)) {
            throw BusinessProjectsConflictException("Closed tasks cannot be planned")
        }
        val today = ZonedDateTime.ofInstant(now(), timeZone).toLocalDate().toString()
        if (input.boardDate < today) throw BusinessProjectsConflictException("Past board dates are not available")
        val value = BusinessTaskBoardPlacement(taskId, input.boardDate, input.positionX, input.positionY, now().toString())
        placements[taskId] = StoredPlacement(identity.clientId, value)
        value
    }

    override fun removeTaskPlacement(identity: BusinessProjectsIdentity, taskId: String) = synchronized(lock) {
        task(identity, taskId)
        placements.remove(taskId)
        Unit
    }

    override fun activeWorkSession(identity: BusinessProjectsIdentity): BusinessWorkSession? = synchronized(lock) {
        sessions.values.firstOrNull {
            it.clientId == identity.clientId && it.authorUserId == identity.userId && it.status != BusinessWorkSessionStatus.FINISHED
        }?.let(::hydrateSession)
    }

    override fun taskWorkSessions(identity: BusinessProjectsIdentity, taskId: String): List<BusinessWorkSession> = synchronized(lock) {
        task(identity, taskId)
        sessions.values.filter { it.clientId == identity.clientId && it.taskId == taskId }.map(::hydrateSession).sortedByDescending(BusinessWorkSession::startedAt)
    }

    override fun startWorkSession(identity: BusinessProjectsIdentity, taskId: String): BusinessWorkSession = synchronized(lock) {
        val task = task(identity, taskId).value
        if (task.status in setOf(BusinessTaskStatus.DONE, BusinessTaskStatus.ARCHIVED)) {
            throw BusinessProjectsConflictException("Closed tasks cannot start a timer")
        }
        activeWorkSession(identity)?.let {
            throw BusinessProjectsConflictException("There is already an active timer on '${it.taskTitle}'")
        }
        val timestamp = now()
        val stored = StoredSession(
            id = idFactory(),
            clientId = identity.clientId,
            taskId = taskId,
            authorUserId = identity.userId,
            startedAt = timestamp,
            status = BusinessWorkSessionStatus.ACTIVE,
            segments = mutableListOf(StoredSegment(idFactory(), timestamp, null)),
        )
        sessions[stored.id] = stored
        hydrateSession(stored)
    }

    override fun pauseWorkSession(identity: BusinessProjectsIdentity, sessionId: String): BusinessWorkSession = synchronized(lock) {
        val stored = liveSession(identity, sessionId)
        if (stored.status == BusinessWorkSessionStatus.ACTIVE) {
            stored.segments.lastOrNull { it.endedAt == null }?.endedAt = now()
            stored.status = BusinessWorkSessionStatus.PAUSED
        }
        hydrateSession(stored)
    }

    override fun resumeWorkSession(identity: BusinessProjectsIdentity, sessionId: String): BusinessWorkSession = synchronized(lock) {
        val stored = liveSession(identity, sessionId)
        if (stored.status == BusinessWorkSessionStatus.PAUSED) {
            stored.segments += StoredSegment(idFactory(), now(), null)
            stored.status = BusinessWorkSessionStatus.ACTIVE
        }
        hydrateSession(stored)
    }

    override fun stopWorkSession(identity: BusinessProjectsIdentity, sessionId: String): BusinessWorkSession = synchronized(lock) {
        val stored = liveSession(identity, sessionId)
        val timestamp = now()
        stored.segments.lastOrNull { it.endedAt == null }?.endedAt = timestamp
        stored.status = BusinessWorkSessionStatus.FINISHED
        stored.endedAt = timestamp
        stored.dailyEntries = splitByMadridDay(stored.segments)
        hydrateSession(stored)
    }

    override fun checkpointWorkSession(identity: BusinessProjectsIdentity, sessionId: String): BusinessWorkSession = synchronized(lock) {
        hydrateSession(liveSession(identity, sessionId))
    }

    override fun metrics(clientId: String): BusinessProgramMetrics = synchronized(lock) {
        val tenantProjects = projects.values.filter { it.clientId == clientId }
        val tenantTasks = tasks.values.filter { it.clientId == clientId }
        val last = (tenantProjects.map { it.value.updatedAt } + tenantTasks.map { it.value.updatedAt }).maxOrNull()
        BusinessProgramMetrics(
            clientId = clientId,
            commercialCustomerCount = 0,
            activeProjectCount = tenantProjects.count { it.value.status == BusinessProjectStatus.ACTIVE },
            openTaskCount = tenantTasks.count { it.value.status !in setOf(BusinessTaskStatus.DONE, BusinessTaskStatus.ARCHIVED) },
            lastActivityAt = last,
        )
    }

    private fun hydrateProject(value: BusinessProject, clientId: String): BusinessProject {
        val projectTasks = tasks.values.filter { it.clientId == clientId && it.value.projectId == value.id }
        return value.copy(
            openTaskCount = projectTasks.count { it.value.status !in setOf(BusinessTaskStatus.DONE, BusinessTaskStatus.ARCHIVED) },
            totalSeconds = projectTasks.sumOf { task ->
                sessions.values.filter { it.clientId == clientId && it.taskId == task.value.id }
                    .sumOf { session -> elapsedSeconds(session) }
            },
        )
    }

    private fun hydrateTask(value: BusinessTask, clientId: String): BusinessTask = value.copy(
        labels = value.labels.mapNotNull { labels[it.id]?.takeIf { stored -> stored.clientId == clientId }?.value },
        totalSeconds = sessions.values.filter { it.clientId == clientId && it.taskId == value.id }.sumOf(::elapsedSeconds),
    )

    private fun hydrateSession(stored: StoredSession): BusinessWorkSession {
        val taskTitle = tasks[stored.taskId]?.value?.title ?: "Task"
        return BusinessWorkSession(
            id = stored.id,
            taskId = stored.taskId,
            taskTitle = taskTitle,
            authorUserId = stored.authorUserId,
            startedAt = stored.startedAt.toString(),
            endedAt = stored.endedAt?.toString(),
            status = stored.status,
            elapsedSeconds = elapsedSeconds(stored),
            segments = stored.segments.map { BusinessWorkSegment(it.id, it.startedAt.toString(), it.endedAt?.toString()) },
            dailyEntries = stored.dailyEntries,
        )
    }

    private fun elapsedSeconds(stored: StoredSession): Long = stored.segments.sumOf {
        Duration.between(it.startedAt, it.endedAt ?: now()).seconds.coerceAtLeast(0)
    }

    private fun splitByMadridDay(segments: List<StoredSegment>): List<BusinessDailyTimeEntry> {
        val totals = linkedMapOf<String, Long>()
        segments.forEach { segment ->
            var cursor = segment.startedAt
            val end = segment.endedAt ?: now()
            while (cursor < end) {
                val local = ZonedDateTime.ofInstant(cursor, timeZone)
                val boundary = local.toLocalDate().plusDays(1).atStartOfDay(timeZone).toInstant()
                val sliceEnd = minOf(end, boundary)
                totals.merge(local.toLocalDate().toString(), Duration.between(cursor, sliceEnd).seconds.coerceAtLeast(0), Long::plus)
                cursor = sliceEnd
            }
        }
        return totals.map { BusinessDailyTimeEntry(it.key, it.value) }
    }

    private fun project(identity: BusinessProjectsIdentity, id: String): StoredProject =
        projects[id]?.takeIf { it.clientId == identity.clientId } ?: throw BusinessProjectsNotFoundException()

    private fun task(identity: BusinessProjectsIdentity, id: String): StoredTask =
        tasks[id]?.takeIf { it.clientId == identity.clientId } ?: throw BusinessProjectsNotFoundException()

    private fun label(clientId: String, id: String): StoredLabel =
        labels[id]?.takeIf { it.clientId == clientId } ?: throw BusinessProjectsNotFoundException()

    private fun liveSession(identity: BusinessProjectsIdentity, id: String): StoredSession {
        val value = sessions[id]?.takeIf { it.clientId == identity.clientId && it.authorUserId == identity.userId }
            ?: throw BusinessProjectsNotFoundException()
        if (value.status == BusinessWorkSessionStatus.FINISHED) throw BusinessProjectsConflictException("The timer is already stopped")
        return value
    }

    private fun validateParents(identity: BusinessProjectsIdentity, projectId: String?, customerId: String?) {
        projectId?.let { project(identity, it) }
        if (customerId != null && customerId.isBlank()) throw BusinessProjectsNotFoundException()
    }

    private fun requireLabels(clientId: String, ids: List<String>): List<BusinessTaskLabel> =
        ids.map { label(clientId, it).value }.also {
            if (it.any(BusinessTaskLabel::archived)) throw BusinessProjectsConflictException("Archived labels cannot be assigned")
        }

    private fun requireVersion(actual: Int, expected: Int) {
        if (actual != expected) throw BusinessProjectsConflictException("The record changed; refresh and try again")
    }

    private data class StoredProject(val clientId: String, val value: BusinessProject)
    private data class StoredTask(val clientId: String, val value: BusinessTask)
    private data class StoredLabel(val clientId: String, val value: BusinessTaskLabel)
    private data class StoredPlacement(val clientId: String, val value: BusinessTaskBoardPlacement)
    private data class StoredSegment(val id: String, val startedAt: Instant, var endedAt: Instant?)
    private data class StoredSession(
        val id: String,
        val clientId: String,
        val taskId: String,
        val authorUserId: String,
        val startedAt: Instant,
        var endedAt: Instant? = null,
        var status: BusinessWorkSessionStatus,
        val segments: MutableList<StoredSegment>,
        var dailyEntries: List<BusinessDailyTimeEntry> = emptyList(),
    )
}
