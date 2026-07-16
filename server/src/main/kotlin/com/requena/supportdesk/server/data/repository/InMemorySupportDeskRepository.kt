package com.requena.supportdesk.server.data.repository

import com.requena.supportdesk.server.data.datasource.InMemorySupportDeskDataSource
import com.requena.supportdesk.server.data.mapper.SupportDeskMapper
import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.ServerConflictException
import com.requena.supportdesk.server.domain.model.ServerNotFoundException
import com.requena.supportdesk.server.domain.model.CreateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.CreateTaskRequest
import com.requena.supportdesk.server.domain.model.CreateTicketMessageRequest
import com.requena.supportdesk.server.domain.model.CreateTicketRequest
import com.requena.supportdesk.server.domain.model.CreateTimeLogRequest
import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.model.ServerAttachmentCreated
import com.requena.supportdesk.server.domain.model.ServerAttachmentSnapshot
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.model.ServerClientSnapshot
import com.requena.supportdesk.server.domain.model.ServerDailyMinutesSnapshot
import com.requena.supportdesk.server.domain.model.ServerDashboardSnapshot
import com.requena.supportdesk.server.domain.model.ServerDeviceRegistration
import com.requena.supportdesk.server.domain.model.ServerTaskLabelSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskSnapshot
import com.requena.supportdesk.server.domain.model.ServerTicketFieldUpdate
import com.requena.supportdesk.server.domain.model.ServerTicketMessageCreated
import com.requena.supportdesk.server.domain.model.ServerTicketSnapshot
import com.requena.supportdesk.server.domain.model.ServerTimeLogSnapshot
import com.requena.supportdesk.server.domain.model.UpdateClientRequest
import com.requena.supportdesk.server.domain.model.UpdateClientCredentialsRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketPriorityRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketStatusRequest
import com.requena.supportdesk.server.domain.model.UploadAttachmentRequest
import com.requena.supportdesk.server.domain.repository.SupportDeskRepository
import com.requena.supportdesk.server.security.PasswordHasher
import java.time.Instant

class InMemorySupportDeskRepository(
    private val dataSource: InMemorySupportDeskDataSource,
) : SupportDeskRepository {
    private data class LocalAdminAccount(
        val userId: String,
        val name: String,
        val email: String,
        val password: String,
        val role: String = "ADMIN",
        val clientId: String? = null,
    )

    private val adminAccounts = mutableListOf(
        LocalAdminAccount(
            userId = "user-admin",
            name = "Admin Requena",
            email = "admin@orykai.dev",
            password = "UnitTestAdminPassword1",
        ),
        LocalAdminAccount(
            userId = "user-admin-2",
            name = "Admin Sanchez",
            email = "admin2@orykai.dev",
            password = "UnitTestAdminPassword2",
        ),
        LocalAdminAccount(
            userId = "user-client",
            name = "Ana Northwind",
            email = "ana@northwind.dev",
            password = "UnitTestClientPassword1",
            role = "CLIENT",
            clientId = "client-1",
        ),
    )

    private val clients = dataSource.clients().map(SupportDeskMapper::client).toMutableList()
    private val clientOwners = mutableMapOf(
        "client-1" to "user-admin",
        "client-2" to "user-admin-2",
    )
    private val labels = mutableListOf(
        ServerTaskLabelSnapshot("label-1", "user-admin", "Hoy", "#6B7A5B", 1),
        ServerTaskLabelSnapshot("label-2", "user-admin-2", "Seguimiento", "#A67C52", 1),
    )
    private val tasks = mutableListOf(
        ServerTaskSnapshot(
            id = "task-1",
            ownerAdminId = "user-admin",
            title = "Revisar build de escritorio",
            description = "Validar arranque limpio",
            clientId = "client-1",
            clientName = "Northwind Studio",
            labelId = "label-1",
            labelName = "Hoy",
            labelColorHex = "#6B7A5B",
            dueDate = "2026-04-18",
            completed = false,
            loggedMinutes = 80,
            loggedSeconds = 80 * 60,
            createdAt = "2026-04-10T08:00:00Z",
            updatedAt = "2026-04-15T09:30:00Z",
        ),
        ServerTaskSnapshot(
            id = "task-2",
            ownerAdminId = "user-admin-2",
            title = "Enviar seguimiento",
            description = "Preparar siguiente iteracion",
            clientId = "client-2",
            clientName = "Pixel Forge",
            labelId = "label-2",
            labelName = "Seguimiento",
            labelColorHex = "#A67C52",
            dueDate = "2026-04-19",
            completed = false,
            loggedMinutes = 45,
            loggedSeconds = 45 * 60,
            createdAt = "2026-04-12T10:30:00Z",
            updatedAt = "2026-04-15T11:10:00Z",
        ),
        ServerTaskSnapshot(
            id = "task-3",
            ownerAdminId = "user-admin",
            title = "Planificar semana",
            description = "Ordenar prioridades internas",
            clientId = null,
            clientName = null,
            labelId = "label-1",
            labelName = "Hoy",
            labelColorHex = "#6B7A5B",
            dueDate = "2026-04-20",
            completed = false,
            loggedMinutes = 25,
            loggedSeconds = 25 * 60,
            createdAt = "2026-04-14T08:50:00Z",
            updatedAt = "2026-04-15T09:45:00Z",
        ),
    )
    private val taskOwners = mutableMapOf(
        "task-1" to "user-admin",
        "task-2" to "user-admin-2",
        "task-3" to "user-admin",
    )
    private val refreshTokens = mutableMapOf<String, String>()
    private val timeLogs = mutableListOf(
        ServerTimeLogSnapshot(
            id = "time-log-1",
            ownerAdminId = "user-admin",
            taskId = "task-1",
            clientId = "client-1",
            authorId = "user-admin",
            authorName = "Admin Requena",
            minutes = 80,
            seconds = 80 * 60,
            workDate = "2026-04-10",
            note = "Revision del arranque",
            billable = true,
            createdAt = "2026-04-10T12:10:00Z",
        ),
        ServerTimeLogSnapshot(
            id = "time-log-2",
            ownerAdminId = "user-admin-2",
            taskId = "task-2",
            clientId = "client-2",
            authorId = "user-admin-2",
            authorName = "Admin Sanchez",
            minutes = 45,
            seconds = 45 * 60,
            workDate = "2026-04-12",
            note = "Seguimiento con cliente",
            billable = true,
            createdAt = "2026-04-12T18:20:00Z",
        ),
        ServerTimeLogSnapshot(
            id = "time-log-3",
            ownerAdminId = "user-admin",
            taskId = "task-3",
            clientId = null,
            authorId = "user-admin",
            authorName = "Admin Requena",
            minutes = 25,
            seconds = 25 * 60,
            workDate = "2026-04-14",
            note = "Revision de agenda interna",
            billable = false,
            createdAt = "2026-04-14T09:10:00Z",
        ),
    )

    override fun authenticate(email: String, password: String): ServerAuthIdentity? =
        adminAccounts.firstOrNull { account ->
            account.email.equals(email.trim(), ignoreCase = true) && account.password == password
        }?.let { account ->
            ServerAuthIdentity(
                userId = account.userId,
                name = account.name,
                email = account.email,
                role = account.role,
                clientId = account.clientId,
            )
        }

    override fun storeRefreshToken(userId: String, refreshToken: String, expiresAt: Instant) {
        refreshTokens[PasswordHasher.hashToken(refreshToken)] = userId
    }

    override fun rotateRefreshToken(
        refreshToken: String,
        replacementRefreshToken: String,
        expiresAt: Instant,
    ): ServerAuthIdentity? {
        val userId = refreshTokens.remove(PasswordHasher.hashToken(refreshToken)) ?: return null
        refreshTokens[PasswordHasher.hashToken(replacementRefreshToken)] = userId
        val account = adminAccounts.firstOrNull { it.userId == userId } ?: return null
        return ServerAuthIdentity(
            userId = account.userId,
            name = account.name,
            email = account.email,
            role = account.role,
            clientId = account.clientId,
        )
    }

    override fun revokeRefreshToken(refreshToken: String): Boolean =
        refreshTokens.remove(PasswordHasher.hashToken(refreshToken)) != null

    override fun getTickets(): List<ServerTicketSnapshot> = dataSource.tickets().map(SupportDeskMapper::ticket)

    override fun getTicket(id: String): ServerTicketSnapshot? = dataSource.ticket(id)?.let(SupportDeskMapper::ticket)

    override fun createTicket(request: CreateTicketRequest): ServerTicketSnapshot = ServerTicketSnapshot(
        id = "ticket-created",
        clientId = request.clientId,
        ticketNumber = "RDS-999999",
        subject = request.subject.ifBlank { "Nuevo ticket" },
        description = request.description.ifBlank { "Ticket creado en modo local." },
        category = request.category,
        affectedApp = request.affectedApp.ifBlank { "Assigned product" },
        platform = request.platform,
        appVersion = request.appVersion,
        stepsToReproduce = request.stepsToReproduce,
        clientReference = request.clientReference,
        status = "OPEN",
        priority = request.priority,
        waitingOn = "ADMIN",
        resolutionSummary = null,
        requesterId = request.requesterId ?: "user-client",
        requesterName = clients.firstOrNull { it.id == request.clientId }?.contactName
            ?: adminAccounts.firstOrNull { it.userId == request.requesterId }?.name
            ?: "Cliente",
        requesterEmail = clients.firstOrNull { it.id == request.clientId }?.email
            ?: adminAccounts.firstOrNull { it.userId == request.requesterId }?.email
            .orEmpty(),
        createdAt = Instant.now().toString(),
        updatedAt = Instant.now().toString(),
    )

    override fun createTicketMessage(ticketId: String, request: CreateTicketMessageRequest): ServerTicketMessageCreated =
        ServerTicketMessageCreated(ticketId = ticketId, messageId = "message-1")

    override fun updateTicketStatus(ticketId: String, request: UpdateTicketStatusRequest): ServerTicketFieldUpdate =
        ServerTicketFieldUpdate(ticketId = ticketId, value = request.status)

    override fun updateTicketPriority(ticketId: String, request: UpdateTicketPriorityRequest): ServerTicketFieldUpdate =
        ServerTicketFieldUpdate(ticketId = ticketId, value = request.priority)

    override fun createAttachment(ticketId: String, request: UploadAttachmentRequest): ServerAttachmentCreated =
        ServerAttachmentCreated(ticketId = ticketId, attachmentId = "attachment-1")

    override fun getClients(ownerAdminId: String?): List<ServerClientSnapshot> = clients
        .filter { ownerAdminId == null || clientOwners[it.id] == ownerAdminId }
        .map { client ->
        client.copy(
            openTasksCount = tasks.count { it.clientId == client.id && !it.completed },
            monthlyLoggedMinutes = timeLogs.filter { it.clientId == client.id }.sumOf { it.minutes },
        )
    }

    override fun createClient(request: CreateClientRequest, ownerAdminId: String?): ServerClientSnapshot {
        val resolvedOwnerAdminId = requireNotNull(ownerAdminId) { "ownerAdminId is required" }
        val created = ServerClientSnapshot(
            id = "client-created-${clients.size + 1}",
            ownerAdminId = resolvedOwnerAdminId,
            companyName = request.companyName.ifBlank { "New client placeholder" },
            productName = request.productName.ifBlank { "Assigned product" },
            contactName = request.contactName.ifBlank { "Contacto" },
            email = request.email.ifBlank { "client@example.com" },
            accountStatus = request.accountStatus,
            serviceTier = request.serviceTier,
            preferredContactChannel = request.preferredContactChannel,
            activeTicketCount = 0,
            openTasksCount = 0,
            monthlyLoggedMinutes = 0,
        )
        clients.add(0, created)
        clientOwners[created.id] = resolvedOwnerAdminId
        return created
    }

    override fun updateClient(clientId: String, request: UpdateClientRequest, ownerAdminId: String?): ServerClientSnapshot {
        val index = requireClientIndex(clientId, ownerAdminId)
        val current = clients[index]
        val updated = current.copy(
            companyName = request.companyName ?: current.companyName,
            productName = request.productName ?: current.productName,
            contactName = request.contactName ?: current.contactName,
            email = request.email ?: current.email,
            accountStatus = request.accountStatus ?: current.accountStatus,
            serviceTier = request.serviceTier ?: current.serviceTier,
            preferredContactChannel = request.preferredContactChannel ?: current.preferredContactChannel,
        )
        clients[index] = updated
        return updated
    }

    override fun updateClientCredentials(
        clientId: String,
        request: UpdateClientCredentialsRequest,
        ownerAdminId: String?,
    ) {
        requireClientIndex(clientId, ownerAdminId)
        val normalizedEmail = request.email.trim()
        val existingIndex = adminAccounts.indexOfFirst { account ->
            account.role == "CLIENT" && account.clientId == clientId
        }
        val conflictingAccount = adminAccounts.firstOrNull { account ->
            account.email.equals(normalizedEmail, ignoreCase = true) && account.clientId != clientId
        }
        if (conflictingAccount != null) {
            throw ServerConflictException("Email is already used by another account")
        }
        val client = clients.first { it.id == clientId }
        val updatedAccount = LocalAdminAccount(
            userId = if (existingIndex >= 0) adminAccounts[existingIndex].userId else "user-client-$clientId",
            name = client.contactName,
            email = normalizedEmail,
            password = request.password,
            role = "CLIENT",
            clientId = clientId,
        )
        if (existingIndex >= 0) {
            adminAccounts[existingIndex] = updatedAccount
        } else {
            adminAccounts.add(updatedAccount)
        }
        refreshTokens.entries.removeAll { it.value == updatedAccount.userId }
    }

    override fun deleteClient(clientId: String, ownerAdminId: String?) {
        val index = requireClientIndex(clientId, ownerAdminId)
        val client = clients[index]
        if (client.activeTicketCount > 0) {
            throw ServerConflictException("Client has related tickets and cannot be deleted")
        }
        clients.removeAt(index)
        clientOwners.remove(clientId)
        tasks.replaceAll { task ->
            if (task.clientId == clientId) task.copy(clientId = null, clientName = null) else task
        }
        timeLogs.replaceAll { log ->
            if (log.clientId == clientId) log.copy(clientId = null) else log
        }
    }

    override fun getTaskLabels(ownerAdminId: String?): List<ServerTaskLabelSnapshot> = labels
        .filter { ownerAdminId == null || it.ownerAdminId == ownerAdminId }
        .map { label ->
            label.copy(tasksCount = tasks.count { it.labelId == label.id && (ownerAdminId == null || it.ownerAdminId == ownerAdminId) })
        }

    override fun createTaskLabel(request: CreateTaskLabelRequest, ownerAdminId: String?): ServerTaskLabelSnapshot {
        val label = ServerTaskLabelSnapshot(
            id = "label-${labels.size + 1}",
            ownerAdminId = ownerAdminId ?: request.ownerAdminId.ifBlank { "user-admin" },
            name = request.name,
            colorHex = request.colorHex,
            tasksCount = 0,
        )
        labels.add(0, label)
        return label
    }

    override fun updateTaskLabel(labelId: String, request: UpdateTaskLabelRequest, ownerAdminId: String?): ServerTaskLabelSnapshot {
        val index = requireLabelIndex(labelId, ownerAdminId)
        val current = labels[index]
        val updated = current.copy(
            name = request.name ?: current.name,
            colorHex = request.colorHex ?: current.colorHex,
            tasksCount = tasks.count { it.labelId == labelId },
        )
        labels[index] = updated
        return updated
    }

    override fun deleteTaskLabel(labelId: String, ownerAdminId: String?) {
        requireLabelIndex(labelId, ownerAdminId)
        if (tasks.any { it.labelId == labelId && (ownerAdminId == null || it.ownerAdminId == ownerAdminId) }) {
            throw ServerConflictException("Label is in use by tasks and cannot be deleted")
        }
        labels.removeAll { it.id == labelId && (ownerAdminId == null || it.ownerAdminId == ownerAdminId) }
    }

    override fun getTasks(clientId: String?, labelId: String?, ownerAdminId: String?): List<ServerTaskSnapshot> =
        tasks.filter { task ->
            (ownerAdminId == null || taskOwners[task.id] == ownerAdminId) &&
            (clientId == null || task.clientId == clientId) &&
                (labelId == null || task.labelId == labelId)
        }

    override fun createTask(request: CreateTaskRequest, ownerAdminId: String?): ServerTaskSnapshot {
        val resolvedOwnerAdminId = requireNotNull(ownerAdminId) { "ownerAdminId is required" }
        val label = labels.firstOrNull { it.id == request.labelId && it.ownerAdminId == resolvedOwnerAdminId }
            ?: throw ServerNotFoundException("Label not found")
        val client = request.clientId?.let { clientId ->
            clients.firstOrNull { it.id == clientId }?.also {
                requireClientOwnership(clientId, resolvedOwnerAdminId)
            } ?: throw ServerNotFoundException("Client not found")
        }
        val task = ServerTaskSnapshot(
            id = "task-${tasks.size + 1}",
            ownerAdminId = resolvedOwnerAdminId,
            title = request.title,
            description = request.description,
            clientId = client?.id,
            clientName = client?.companyName,
            labelId = label.id,
            labelName = label.name,
            labelColorHex = label.colorHex,
            dueDate = request.dueDate?.takeIf { it.isNotBlank() },
            completed = false,
            loggedMinutes = 0,
            loggedSeconds = 0,
            createdAt = "2026-04-16T10:00:00Z",
            updatedAt = "2026-04-16T10:00:00Z",
        )
        tasks.add(0, task)
        taskOwners[task.id] = resolvedOwnerAdminId
        return task
    }

    override fun updateTask(taskId: String, request: UpdateTaskRequest, ownerAdminId: String?): ServerTaskSnapshot {
        val index = requireTaskIndex(taskId, ownerAdminId)
        val current = tasks[index]
        val resolvedClientId = when {
            request.clientId == null -> current.clientId
            request.clientId.isBlank() -> null
            else -> request.clientId
        }
        val client = resolvedClientId?.let { clientId ->
            clients.firstOrNull { it.id == clientId }?.also {
                requireClientOwnership(clientId, ownerAdminId)
            } ?: throw ServerNotFoundException("Client not found")
        }
        val label = labels.firstOrNull { it.id == (request.labelId ?: current.labelId) && it.ownerAdminId == current.ownerAdminId }
            ?: throw ServerNotFoundException("Label not found")
        val updated = current.copy(
            title = request.title ?: current.title,
            description = request.description ?: current.description,
            clientId = resolvedClientId,
            clientName = client?.companyName,
            labelId = label.id,
            labelName = label.name,
            labelColorHex = label.colorHex,
            dueDate = request.dueDate?.let { it.takeIf(String::isNotBlank) } ?: if (request.dueDate == null) current.dueDate else null,
            completed = request.completed ?: current.completed,
            updatedAt = "2026-04-16T10:00:00Z",
        )
        tasks[index] = updated
        return updated
    }

    override fun deleteTask(taskId: String, ownerAdminId: String?) {
        val index = requireTaskIndex(taskId, ownerAdminId)
        tasks.removeAt(index)
        taskOwners.remove(taskId)
        timeLogs.removeAll { it.taskId == taskId }
    }

    override fun getTimeLogs(clientId: String?, taskId: String?, ownerAdminId: String?): List<ServerTimeLogSnapshot> =
        timeLogs.filter { log ->
            val taskOwner = taskOwners[log.taskId]
            (ownerAdminId == null || taskOwner == ownerAdminId) &&
            (clientId == null || log.clientId == clientId) &&
                (taskId == null || log.taskId == taskId)
        }

    override fun createTimeLog(request: CreateTimeLogRequest, ownerAdminId: String?): ServerTimeLogSnapshot {
        val resolvedOwnerAdminId = requireNotNull(ownerAdminId) { "ownerAdminId is required" }
        val task = tasks.firstOrNull { it.id == request.taskId }
            ?: throw ServerNotFoundException("Task not found")
        requireTaskOwnership(task.id, resolvedOwnerAdminId)
        val resolvedSeconds = request.seconds.takeIf { it > 0 } ?: (request.minutes * 60)
        val log = ServerTimeLogSnapshot(
            id = "time-log-${timeLogs.size + 1}",
            ownerAdminId = resolvedOwnerAdminId,
            taskId = request.taskId,
            clientId = task.clientId,
            authorId = request.authorId,
            authorName = adminAccounts.firstOrNull { it.userId == request.authorId }?.name ?: "Admin",
            minutes = resolvedSeconds / 60,
            seconds = resolvedSeconds,
            workDate = request.workDate,
            note = request.note,
            billable = request.billable,
            createdAt = "2026-04-16T10:00:00Z",
        )
        timeLogs.add(0, log)
        val taskIndex = tasks.indexOfFirst { it.id == task.id }
        tasks[taskIndex] = task.copy(
            loggedMinutes = (task.loggedSeconds + resolvedSeconds) / 60,
            loggedSeconds = task.loggedSeconds + resolvedSeconds,
            updatedAt = "2026-04-16T10:00:00Z",
        )
        return log
    }

    override fun getDashboard(clientId: String?, labelId: String?, ownerAdminId: String?): ServerDashboardSnapshot {
        val visibleClients = getClients(ownerAdminId)
        val filteredTasks = getTasks(clientId, labelId, ownerAdminId)
        val filteredLogs = getTimeLogs(clientId, null, ownerAdminId)
        return ServerDashboardSnapshot(
            openTickets = 3,
            pendingClientTickets = 1,
            resolvedToday = 1,
            activeClients = visibleClients.size,
            monthLabel = "April 2026",
            totalMinutes = filteredLogs.sumOf { it.minutes },
            billableMinutes = filteredLogs.filter { it.billable }.sumOf { it.minutes },
            selectedClientId = clientId,
            selectedClientMinutes = filteredLogs.sumOf { it.minutes },
            selectedClientBillableMinutes = filteredLogs.filter { it.billable }.sumOf { it.minutes },
            dailyMinutes = filteredLogs
                .groupBy { it.workDate }
                .map { (date, logs) -> ServerDailyMinutesSnapshot(date, logs.sumOf { it.minutes }) }
                .sortedBy { it.workDate },
            availableTasks = filteredTasks,
        )
    }

    override fun getAttachment(id: String): ServerAttachmentSnapshot? = dataSource.attachment(id)?.let(SupportDeskMapper::attachment)

    override fun registerDevice(request: RegisterDeviceRequest): ServerDeviceRegistration =
        SupportDeskMapper.device(dataSource.registerDevice()).copy(
            userId = request.userId,
            platform = request.platform,
        )

    private fun requireClientIndex(clientId: String, ownerAdminId: String? = null): Int = clients.indexOfFirst {
        it.id == clientId && (ownerAdminId == null || clientOwners[it.id] == ownerAdminId)
    }
        .takeIf { it >= 0 } ?: throw ServerNotFoundException("Client not found")

    private fun requireLabelIndex(labelId: String, ownerAdminId: String? = null): Int = labels.indexOfFirst {
        it.id == labelId && (ownerAdminId == null || it.ownerAdminId == ownerAdminId)
    }
        .takeIf { it >= 0 } ?: throw ServerNotFoundException("Label not found")

    private fun requireTaskIndex(taskId: String, ownerAdminId: String? = null): Int = tasks.indexOfFirst {
        it.id == taskId && (ownerAdminId == null || taskOwners[it.id] == ownerAdminId)
    }
        .takeIf { it >= 0 } ?: throw ServerNotFoundException("Task not found")

    private fun requireClientOwnership(clientId: String, ownerAdminId: String?) {
        if (ownerAdminId != null && clientOwners[clientId] != ownerAdminId) {
            throw ServerNotFoundException("Client not found")
        }
    }

    private fun requireTaskOwnership(taskId: String, ownerAdminId: String?) {
        if (ownerAdminId != null && taskOwners[taskId] != ownerAdminId) {
            throw ServerNotFoundException("Task not found")
        }
    }

}
