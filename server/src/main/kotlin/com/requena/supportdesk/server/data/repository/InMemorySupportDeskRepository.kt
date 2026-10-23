package com.requena.supportdesk.server.data.repository

import com.requena.supportdesk.server.data.datasource.InMemorySupportDeskDataSource
import com.requena.supportdesk.server.data.mapper.SupportDeskMapper
import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.CreateClientProgramRequestsRequest
import com.requena.supportdesk.server.domain.model.CreateClientActivityRequest
import com.requena.supportdesk.server.domain.model.CreateClientContactRequest
import com.requena.supportdesk.server.domain.model.ServerConflictException
import com.requena.supportdesk.server.domain.model.ServerNotFoundException
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
import com.requena.supportdesk.server.domain.model.ServerBillingPreviewLineSnapshot
import com.requena.supportdesk.server.domain.model.ServerProductCatalogSnapshot
import com.requena.supportdesk.server.domain.model.ServerDailyMinutesSnapshot
import com.requena.supportdesk.server.domain.model.ServerDashboardSnapshot
import com.requena.supportdesk.server.domain.model.ServerDeviceRegistration
import com.requena.supportdesk.server.domain.model.ServerTaskLabelSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskBoardPlacementSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskBoardSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskSnapshot
import com.requena.supportdesk.server.domain.model.ServerTicketFieldUpdate
import com.requena.supportdesk.server.domain.model.ServerTicketMessageCreated
import com.requena.supportdesk.server.domain.model.ServerTicketSnapshot
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
import com.requena.supportdesk.server.security.PasswordHasher
import com.requena.supportdesk.server.security.ClientAccessCodeGenerator
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

    // Two companies so tests can prove one never sees the other's data.
    private val organizationNames = mutableMapOf(
        ORG_ORYKAI to "OryKai",
        ORG_SECOND to "Empresa de Admin Sanchez",
    )

    // Staff user -> company, and their role inside it (organization_members).
    private val organizationOfUser = mutableMapOf(
        "user-admin" to ORG_ORYKAI,
        "user-admin-2" to ORG_SECOND,
    )
    private val organizationRoles = mutableMapOf(
        "user-admin" to "OWNER",
        "user-admin-2" to "OWNER",
    )

    private val clients = dataSource.clients().map(SupportDeskMapper::client).toMutableList()

    // Client / task -> company (the organization_id column of the root tables).
    private val clientOwners = mutableMapOf(
        "client-1" to ORG_ORYKAI,
        "client-2" to ORG_SECOND,
    )

    private fun organizationOfAccount(account: LocalAdminAccount): String? =
        if (account.role == "CLIENT") account.clientId?.let(clientOwners::get) else organizationOfUser[account.userId]

    private fun identityOf(account: LocalAdminAccount): ServerAuthIdentity {
        val organizationId = organizationOfAccount(account)
        return ServerAuthIdentity(
            userId = account.userId,
            name = account.name,
            email = account.email,
            role = account.role,
            clientId = account.clientId,
            organizationId = organizationId,
            organizationRole = if (account.role == "CLIENT") null else organizationRoles[account.userId],
            organizationName = organizationId?.let(organizationNames::get),
        )
    }

    /** Company's primary staff user (its OWNER), recorded as owner_admin_id on new rows. */
    private fun primaryAdminOf(organizationId: String): String =
        organizationOfUser.entries
            .filter { it.value == organizationId }
            .sortedByDescending { organizationRoles[it.key] == "OWNER" }
            .firstOrNull()?.key
            ?: throw ServerNotFoundException("Organization not found")

    private fun staffOrganization(userId: String): String =
        organizationOfUser[userId] ?: throw ServerNotFoundException("Task not found")

    override fun identityForUser(userId: String): ServerAuthIdentity? =
        adminAccounts.firstOrNull { it.userId == userId }?.let(::identityOf)

    private data class LocalProgramSubscription(
        val clientId: String,
        val snapshot: ServerClientProductSubscriptionSnapshot,
    )
    private val clientComponents = mutableMapOf(
        "client-1" to mutableSetOf("SERVICE_SLA"),
    )
    private val productCatalog = businessBetaCatalog()
    private val programSubscriptions = mutableListOf(
        LocalProgramSubscription(
            clientId = "client-1",
            snapshot = ServerClientProductSubscriptionSnapshot(
                productKey = "SERVICE_SLA",
                status = "ACTIVE",
                monthlyPriceCents = 0,
                currency = "EUR",
                startsOn = "2026-01-01",
            ),
        ),
    )
    private val programRequests = mutableListOf<ServerClientProgramRequestSnapshot>()
    private val subscriptionAudit = mutableListOf<String>()
    private val clientContacts = mutableListOf<ServerClientContactSnapshot>()
    private val clientActivities = mutableListOf<ServerClientActivitySnapshot>()
    private val labels = mutableListOf(
        ServerTaskLabelSnapshot("label-1", "user-admin", "Hoy", "#6B7A5B", 1),
        ServerTaskLabelSnapshot("label-2", "user-admin-2", "Seguimiento", "#A67C52", 1),
    )
    private val tasks = mutableListOf(
        ServerTaskSnapshot(
            id = "task-1",
            ownerAdminId = "user-admin",
            createdByUserId = "user-client",
            createdByUserName = "Ana Northwind",
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
            createdByUserId = "user-admin-2",
            createdByUserName = "Admin Sanchez",
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
            createdByUserId = "user-admin",
            createdByUserName = "Admin Requena",
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
        "task-1" to ORG_ORYKAI,
        "task-2" to ORG_SECOND,
        "task-3" to ORG_ORYKAI,
    )
    private val taskBoardPlacements = mutableMapOf<String, ServerTaskBoardPlacementSnapshot>()
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
        }?.let(::identityOf)

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
        return identityOf(account)
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

    override fun getClients(organizationId: String): List<ServerClientSnapshot> = clients
        .filter { clientOwners[it.id] == organizationId }
        .map { client ->
        client.copy(
            enabledComponents = clientComponents[client.id]?.sorted().orEmpty(),
            openTasksCount = tasks.count { it.clientId == client.id && !it.completed },
            monthlyLoggedMinutes = timeLogs.filter { it.clientId == client.id }.sumOf { it.minutes },
        )
    }

    override fun createClient(request: CreateClientRequest, organizationId: String): ServerClientProvisioning {
        val resolvedOrganizationId = organizationId
        val created = ServerClientSnapshot(
            id = "client-created-${clients.size + 1}",
            ownerAdminId = primaryAdminOf(resolvedOrganizationId),
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
            billing = request.billing.toSnapshot(),
        )
        clients.add(0, created)
        clientOwners[created.id] = resolvedOrganizationId
        clientComponents[created.id] = mutableSetOf()
        val accessCode = ClientAccessCodeGenerator.generate()
        adminAccounts.add(
            LocalAdminAccount(
                userId = "user-client-${created.id}",
                name = created.contactName,
                email = created.email,
                password = accessCode,
                role = "CLIENT",
                clientId = created.id,
            ),
        )
        return ServerClientProvisioning(
            client = created,
            credentials = ServerClientAccessCredentials(created.id, created.email, accessCode),
        )
    }

    override fun updateClient(clientId: String, request: UpdateClientRequest, organizationId: String): ServerClientSnapshot {
        val index = requireClientIndex(clientId, organizationId)
        val current = clients[index]
        val updated = current.copy(
            companyName = request.companyName ?: current.companyName,
            productName = request.productName ?: current.productName,
            contactName = request.contactName ?: current.contactName,
            email = request.email ?: current.email,
            accountStatus = request.accountStatus ?: current.accountStatus,
            serviceTier = request.serviceTier ?: current.serviceTier,
            preferredContactChannel = request.preferredContactChannel ?: current.preferredContactChannel,
            billing = request.billing?.toSnapshot() ?: current.billing,
        )
        clients[index] = updated
        val accountIndex = adminAccounts.indexOfFirst { account ->
            account.role == "CLIENT" && account.clientId == clientId
        }
        if (accountIndex >= 0 && (request.email != null || request.contactName != null)) {
            val account = adminAccounts[accountIndex]
            adminAccounts[accountIndex] = account.copy(
                name = if (request.contactName.isNullOrBlank()) account.name else updated.contactName,
                email = if (request.email.isNullOrBlank()) account.email else updated.email,
            )
        }
        return updated.copy(enabledComponents = clientComponents[clientId]?.sorted().orEmpty())
    }

    override fun updateClientCredentials(
        clientId: String,
        request: UpdateClientCredentialsRequest,
        organizationId: String,
    ) {
        requireClientIndex(clientId, organizationId)
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

    override fun regenerateClientCredentials(
        clientId: String,
        organizationId: String,
    ): ServerClientAccessCredentials {
        val clientIndex = requireClientIndex(clientId, organizationId)
        val client = clients[clientIndex]
        val accessCode = ClientAccessCodeGenerator.generate()
        val existingIndex = adminAccounts.indexOfFirst { account ->
            account.role == "CLIENT" && account.clientId == clientId
        }
        val account = LocalAdminAccount(
            userId = if (existingIndex >= 0) adminAccounts[existingIndex].userId else "user-client-$clientId",
            name = client.contactName,
            email = client.email,
            password = accessCode,
            role = "CLIENT",
            clientId = clientId,
        )
        if (existingIndex >= 0) {
            adminAccounts[existingIndex] = account
        } else {
            adminAccounts.add(account)
        }
        refreshTokens.entries.removeAll { it.value == account.userId }
        return ServerClientAccessCredentials(clientId, client.email, accessCode)
    }

    override fun deleteClient(clientId: String, organizationId: String) {
        val index = requireClientIndex(clientId, organizationId)
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
        clientContacts.removeAll { it.clientId == clientId }
        clientActivities.removeAll { it.clientId == clientId }
        programSubscriptions.removeAll { it.clientId == clientId }
        programRequests.removeAll { it.clientId == clientId }
    }

    override fun getClientContacts(clientId: String, organizationId: String): List<ServerClientContactSnapshot> {
        requireClientIndex(clientId, organizationId)
        return clientContacts.filter { it.clientId == clientId }
            .sortedWith(compareByDescending<ServerClientContactSnapshot> { it.isPrimary }.thenBy { it.fullName })
    }

    override fun createClientContact(
        clientId: String,
        request: CreateClientContactRequest,
        organizationId: String,
    ): ServerClientContactSnapshot {
        requireClientIndex(clientId, organizationId)
        if (request.isPrimary) {
            clientContacts.replaceAll { contact ->
                if (contact.clientId == clientId && contact.isPrimary) contact.copy(isPrimary = false, updatedAt = Instant.now().toString()) else contact
            }
        }
        val now = Instant.now().toString()
        return ServerClientContactSnapshot(
            id = "contact-${clientContacts.size + 1}",
            clientId = clientId,
            fullName = request.fullName.trim(),
            email = request.email.normalizedOptionalText(),
            phone = request.phone.normalizedOptionalText(),
            role = request.role.normalizedOptionalText(),
            isPrimary = request.isPrimary,
            createdAt = now,
            updatedAt = now,
        ).also(clientContacts::add)
    }

    override fun updateClientContact(
        clientId: String,
        contactId: String,
        request: UpdateClientContactRequest,
        organizationId: String,
    ): ServerClientContactSnapshot {
        requireClientIndex(clientId, organizationId)
        val index = clientContacts.indexOfFirst { it.id == contactId && it.clientId == clientId }
            .takeIf { it >= 0 } ?: throw ServerNotFoundException("Client contact not found")
        if (request.isPrimary == true) {
            clientContacts.replaceAll { contact ->
                if (contact.clientId == clientId && contact.id != contactId && contact.isPrimary) {
                    contact.copy(isPrimary = false, updatedAt = Instant.now().toString())
                } else {
                    contact
                }
            }
        }
        val current = clientContacts[index]
        return current.copy(
            fullName = request.fullName?.trim()?.takeIf(String::isNotBlank) ?: current.fullName,
            email = request.email.updatedOptionalText(current.email),
            phone = request.phone.updatedOptionalText(current.phone),
            role = request.role.updatedOptionalText(current.role),
            isPrimary = request.isPrimary ?: current.isPrimary,
            updatedAt = Instant.now().toString(),
        ).also { clientContacts[index] = it }
    }

    override fun deleteClientContact(clientId: String, contactId: String, organizationId: String) {
        requireClientIndex(clientId, organizationId)
        if (!clientContacts.removeIf { it.id == contactId && it.clientId == clientId }) {
            throw ServerNotFoundException("Client contact not found")
        }
        clientActivities.replaceAll { activity ->
            if (activity.clientId == clientId && activity.contactId == contactId) activity.copy(contactId = null) else activity
        }
    }

    override fun getClientActivities(clientId: String, organizationId: String): List<ServerClientActivitySnapshot> {
        requireClientIndex(clientId, organizationId)
        return clientActivities.filter { it.clientId == clientId }
            .sortedWith(compareBy<ServerClientActivitySnapshot> { it.completedAt != null }.thenByDescending { it.createdAt })
    }

    override fun createClientActivity(
        clientId: String,
        request: CreateClientActivityRequest,
        createdById: String,
        organizationId: String,
    ): ServerClientActivitySnapshot {
        requireClientIndex(clientId, organizationId)
        val contactId = request.contactId?.trim()?.takeIf(String::isNotBlank)
        if (contactId != null && clientContacts.none { it.id == contactId && it.clientId == clientId }) {
            throw ServerNotFoundException("Client contact not found")
        }
        val now = Instant.now().toString()
        return ServerClientActivitySnapshot(
            id = "activity-${clientActivities.size + 1}",
            clientId = clientId,
            contactId = contactId,
            type = request.type,
            subject = request.subject.trim(),
            details = request.details.normalizedOptionalText(),
            dueDate = request.dueDate.normalizedOptionalText(),
            createdByName = adminAccounts.firstOrNull { it.userId == createdById }?.name ?: "Administrador",
            createdAt = now,
            updatedAt = now,
        ).also(clientActivities::add)
    }

    override fun updateClientActivity(
        clientId: String,
        activityId: String,
        request: UpdateClientActivityRequest,
        organizationId: String,
    ): ServerClientActivitySnapshot {
        requireClientIndex(clientId, organizationId)
        val index = clientActivities.indexOfFirst { it.id == activityId && it.clientId == clientId }
            .takeIf { it >= 0 } ?: throw ServerNotFoundException("Client activity not found")
        val contactId = request.contactId.updatedOptionalText(clientActivities[index].contactId)
        if (contactId != null && clientContacts.none { it.id == contactId && it.clientId == clientId }) {
            throw ServerNotFoundException("Client contact not found")
        }
        val current = clientActivities[index]
        return current.copy(
            contactId = contactId,
            type = request.type?.trim()?.takeIf(String::isNotBlank) ?: current.type,
            subject = request.subject?.trim()?.takeIf(String::isNotBlank) ?: current.subject,
            details = request.details.updatedOptionalText(current.details),
            dueDate = request.dueDate.updatedOptionalText(current.dueDate),
            completedAt = when (request.completed) {
                true -> current.completedAt ?: Instant.now().toString()
                false -> null
                null -> current.completedAt
            },
            updatedAt = Instant.now().toString(),
        ).also { clientActivities[index] = it }
    }

    override fun deleteClientActivity(clientId: String, activityId: String, organizationId: String) {
        requireClientIndex(clientId, organizationId)
        if (!clientActivities.removeIf { it.id == activityId && it.clientId == clientId }) {
            throw ServerNotFoundException("Client activity not found")
        }
    }

    override fun updateClientComponents(
        clientId: String,
        request: UpdateClientComponentsRequest,
        organizationId: String,
    ): ServerClientSnapshot {
        val index = requireClientIndex(clientId, organizationId)
        clientComponents[clientId] = request.components.toMutableSet()
        syncLegacyServiceSubscription(clientId, request.components.contains("SERVICE_SLA"))
        val updated = clients[index].copy(enabledComponents = request.components.distinct().sorted())
        clients[index] = updated
        return updated
    }

    override fun getClientPrograms(clientId: String): ServerClientProgramsSnapshot {
        requireOwnClientIndex(clientId)
        return ServerClientProgramsSnapshot(
            catalog = productCatalog.sortedBy { it.key },
            subscriptions = programSubscriptions
                .filter { it.clientId == clientId }
                .map { it.snapshot }
                .sortedBy { it.productKey },
            requests = programRequests
                .filter { it.clientId == clientId }
                .sortedByDescending { it.requestedAt },
        )
    }

    override fun createClientProgramRequests(
        clientId: String,
        requestedByUserId: String,
        request: CreateClientProgramRequestsRequest,
    ): List<ServerClientProgramRequestSnapshot> {
        val clientIndex = requireOwnClientIndex(clientId)
        if (clients[clientIndex].accountStatus != "ACTIVE") {
            throw ServerConflictException("Client account must be active to request a program")
        }
        val requester = adminAccounts.firstOrNull { it.userId == requestedByUserId && it.clientId == clientId }
            ?: throw ServerNotFoundException("Client portal user not found")
        val normalizedNote = request.customerNote.trim().takeIf(String::isNotBlank)
        val now = Instant.now().toString()
        return request.productKeys.map { rawKey ->
            val productKey = rawKey.trim().uppercase()
            val product = productCatalog.firstOrNull { it.key == productKey && it.isAvailable && it.isRequestable }
                ?: throw ServerNotFoundException("Program is not available")
            if (programSubscriptions.any { it.clientId == clientId && it.snapshot.productKey == product.key && it.snapshot.status == "ACTIVE" }) {
                throw ServerConflictException("Program is already active")
            }
            if (programRequests.any { it.clientId == clientId && it.productKey == product.key && it.status == "REQUESTED" }) {
                throw ServerConflictException("Program request is already pending")
            }
            ServerClientProgramRequestSnapshot(
                id = "program-request-${programRequests.size + 1}",
                clientId = clientId,
                clientCompanyName = clients[clientIndex].companyName,
                productKey = product.key,
                status = "REQUESTED",
                customerNote = normalizedNote,
                requestedByName = requester.name,
                requestedAt = now,
                currency = "EUR",
            ).also {
                programRequests.add(it)
                subscriptionAudit.add("REQUESTED:${it.id}")
            }
        }
    }

    override fun getClientProgramRequests(
        organizationId: String,
        status: String?,
    ): List<ServerClientProgramRequestSnapshot> = programRequests
        .asSequence()
        .filter { clientOwners[it.clientId] == organizationId }
        .filter { status == null || it.status == status }
        .sortedBy { it.requestedAt }
        .toList()

    override fun approveClientProgramRequest(
        requestId: String,
        request: ApproveClientProgramRequest,
        reviewedByUserId: String,
        organizationId: String,
    ): ServerClientProgramRequestSnapshot {
        val requestIndex = requireProgramRequestIndex(requestId, organizationId)
        val current = programRequests[requestIndex]
        if (current.status != "REQUESTED") throw ServerConflictException("Program request has already been decided")
        val now = Instant.now().toString()
        val approved = current.copy(
            status = "APPROVED",
            adminNote = request.adminNote?.trim()?.takeIf(String::isNotBlank),
            decidedAt = now,
            quotedMonthlyPriceCents = 0,
        )
        programRequests[requestIndex] = approved
        upsertProgramSubscription(
            clientId = current.clientId,
            productKey = current.productKey,
            monthlyPriceCents = 0,
            startsOn = now.take(10),
        )
        subscriptionAudit.add("APPROVED:$requestId:$reviewedByUserId")
        return approved
    }

    override fun rejectClientProgramRequest(
        requestId: String,
        request: RejectClientProgramRequest,
        reviewedByUserId: String,
        organizationId: String,
    ): ServerClientProgramRequestSnapshot {
        val requestIndex = requireProgramRequestIndex(requestId, organizationId)
        val current = programRequests[requestIndex]
        if (current.status != "REQUESTED") throw ServerConflictException("Program request has already been decided")
        val rejected = current.copy(
            status = "REJECTED",
            adminNote = request.adminNote?.trim()?.takeIf(String::isNotBlank),
            decidedAt = Instant.now().toString(),
        )
        programRequests[requestIndex] = rejected
        subscriptionAudit.add("REJECTED:$requestId:$reviewedByUserId")
        return rejected
    }

    override fun getClientBillingPreview(
        clientId: String,
        period: String,
        organizationId: String,
    ): ServerClientBillingPreviewSnapshot {
        requireClientIndex(clientId, organizationId)
        val lines = programSubscriptions
            .filter { it.clientId == clientId && it.snapshot.status == "ACTIVE" && it.snapshot.monthlyPriceCents > 0 }
            .mapNotNull { subscription ->
                productCatalog.firstOrNull { it.key == subscription.snapshot.productKey }?.let { product ->
                    ServerBillingPreviewLineSnapshot(
                        productKey = product.key,
                        name = product.name,
                        monthlyPriceCents = subscription.snapshot.monthlyPriceCents,
                        currency = "EUR",
                    )
                }
            }
            .sortedBy { it.productKey }
        return ServerClientBillingPreviewSnapshot(
            clientId = clientId,
            period = period,
            lines = lines,
            totalMonthlyPriceCents = lines.sumOf { it.monthlyPriceCents },
            currency = "EUR",
        )
    }

    override fun getTaskLabels(organizationId: String): List<ServerTaskLabelSnapshot> = labels
        .filter { organizationOfUser[it.ownerAdminId] == organizationId }
        .map { label ->
            label.copy(tasksCount = tasks.count { it.labelId == label.id && (organizationOfUser[it.ownerAdminId] == organizationId) })
        }

    override fun createTaskLabel(request: CreateTaskLabelRequest, organizationId: String): ServerTaskLabelSnapshot {
        val label = ServerTaskLabelSnapshot(
            id = "label-${labels.size + 1}",
            ownerAdminId = primaryAdminOf(organizationId),
            name = request.name,
            colorHex = request.colorHex,
            tasksCount = 0,
        )
        labels.add(0, label)
        return label
    }

    override fun updateTaskLabel(labelId: String, request: UpdateTaskLabelRequest, organizationId: String): ServerTaskLabelSnapshot {
        val index = requireLabelIndex(labelId, organizationId)
        val current = labels[index]
        val updated = current.copy(
            name = request.name ?: current.name,
            colorHex = request.colorHex ?: current.colorHex,
            tasksCount = tasks.count { it.labelId == labelId },
        )
        labels[index] = updated
        return updated
    }

    override fun deleteTaskLabel(labelId: String, organizationId: String) {
        requireLabelIndex(labelId, organizationId)
        if (tasks.any { it.labelId == labelId && (organizationOfUser[it.ownerAdminId] == organizationId) }) {
            throw ServerConflictException("Label is in use by tasks and cannot be deleted")
        }
        labels.removeAll { it.id == labelId && (organizationOfUser[it.ownerAdminId] == organizationId) }
    }

    override fun getTasks(clientId: String?, labelId: String?, organizationId: String): List<ServerTaskSnapshot> =
        tasks.filter { task ->
            (taskOwners[task.id] == organizationId) &&
            (clientId == null || task.clientId == clientId) &&
                (labelId == null || task.labelId == labelId)
        }

    override fun createTask(
        request: CreateTaskRequest,
        createdByUserId: String,
        organizationId: String,
    ): ServerTaskSnapshot {
        val resolvedOrganizationId = organizationId
        val creator = adminAccounts.firstOrNull { account ->
            account.userId == createdByUserId && organizationOfAccount(account) == resolvedOrganizationId
        } ?: throw ServerNotFoundException("Task creator not found")
        val label = labels.firstOrNull { it.id == request.labelId && organizationOfUser[it.ownerAdminId] == resolvedOrganizationId }
            ?: throw ServerNotFoundException("Label not found")
        val client = request.clientId?.let { clientId ->
            clients.firstOrNull { it.id == clientId }?.also {
                requireClientOwnership(clientId, resolvedOrganizationId)
            } ?: throw ServerNotFoundException("Client not found")
        }
        val task = ServerTaskSnapshot(
            id = "task-${tasks.size + 1}",
            ownerAdminId = primaryAdminOf(resolvedOrganizationId),
            createdByUserId = creator.userId,
            createdByUserName = creator.name,
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
        taskOwners[task.id] = resolvedOrganizationId
        return task
    }

    override fun updateTask(taskId: String, request: UpdateTaskRequest, organizationId: String): ServerTaskSnapshot {
        val index = requireTaskIndex(taskId, organizationId)
        val current = tasks[index]
        val resolvedClientId = when {
            request.clientId == null -> current.clientId
            request.clientId.isBlank() -> null
            else -> request.clientId
        }
        val client = resolvedClientId?.let { clientId ->
            clients.firstOrNull { it.id == clientId }?.also {
                requireClientOwnership(clientId, organizationId)
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
        if (updated.completed) taskBoardPlacements.remove(taskId)
        return updated
    }

    override fun deleteTask(taskId: String, organizationId: String) {
        val index = requireTaskIndex(taskId, organizationId)
        tasks.removeAt(index)
        taskOwners.remove(taskId)
        taskBoardPlacements.remove(taskId)
        timeLogs.removeAll { it.taskId == taskId }
    }

    override fun getTaskBoard(organizationId: String): ServerTaskBoardSnapshot {
        val boundary = madridTaskBoardDayBoundary()
        val today = boundary.today.toString()
        val visibleTaskIds = tasks.asSequence()
            .filter { organizationOfUser[it.ownerAdminId] == organizationId && !it.completed }
            .map { it.id }
            .toSet()
        return ServerTaskBoardSnapshot(
            today = today,
            timeZone = TASK_BOARD_TIME_ZONE,
            refreshAfterSeconds = boundary.refreshAfterSeconds,
            placements = taskBoardPlacements.values
                .filter { it.taskId in visibleTaskIds && it.boardDate >= today }
                .sortedWith(compareBy(ServerTaskBoardPlacementSnapshot::boardDate, ServerTaskBoardPlacementSnapshot::updatedAt)),
        )
    }

    override fun upsertTaskBoardPlacement(
        taskId: String,
        organizationId: String,
        boardDate: String,
        positionX: Double,
        positionY: Double,
    ): ServerTaskBoardPlacementSnapshot {
        val task = tasks.firstOrNull { it.id == taskId && organizationOfUser[it.ownerAdminId] == organizationId }
            ?: throw ServerNotFoundException("Task not found")
        if (task.completed) throw ServerConflictException("Completed tasks cannot be placed on the board")
        val now = Instant.now().toString()
        val placement = ServerTaskBoardPlacementSnapshot(
            taskId = taskId,
            boardDate = boardDate,
            positionX = positionX,
            positionY = positionY,
            createdAt = taskBoardPlacements[taskId]?.createdAt ?: now,
            updatedAt = now,
        )
        taskBoardPlacements[taskId] = placement
        return placement
    }

    override fun deleteTaskBoardPlacement(taskId: String, organizationId: String) {
        requireTaskIndex(taskId, organizationId)
        taskBoardPlacements.remove(taskId)
    }

    override fun getTimeLogs(clientId: String?, taskId: String?, organizationId: String): List<ServerTimeLogSnapshot> =
        timeLogs.filter { log ->
            val taskOwner = taskOwners[log.taskId]
            (taskOwner == organizationId) &&
            (clientId == null || log.clientId == clientId) &&
                (taskId == null || log.taskId == taskId)
        }

    private data class LocalWorkSegment(
        val id: String,
        val startedAt: java.time.Instant,
        var endedAt: java.time.Instant? = null,
    )

    private data class LocalWorkSession(
        val id: String,
        val taskId: String,
        val ownerAdminId: String,
        val clientId: String?,
        val authorId: String,
        val startedAt: java.time.Instant,
        var endedAt: java.time.Instant? = null,
        var status: String = "ACTIVE",
        val billable: Boolean,
        val segments: MutableList<LocalWorkSegment> = mutableListOf(),
        var lastCheckpointAt: java.time.Instant? = null,
    )

    private val workSessions = mutableListOf<LocalWorkSession>()
    private var workSessionCounter = 0
    private var workSegmentCounter = 0

    override fun activeWorkSession(actorUserId: String): ServerWorkSessionSnapshot? =
        workSessions.firstOrNull { it.ownerAdminId == actorUserId && it.status != "FINISHED" }?.let(::workSessionSnapshot)

    override fun workSessionsForTask(taskId: String, actorUserId: String): List<ServerWorkSessionSnapshot> {
        requireTaskOwnership(taskId, staffOrganization(actorUserId))
        return workSessions.filter { it.taskId == taskId && it.ownerAdminId == actorUserId }
            .sortedByDescending { it.startedAt }
            .map(::workSessionSnapshot)
    }

    override fun startedWorkSession(request: StartWorkSessionRequest, actorUserId: String, authorId: String): ServerWorkSessionSnapshot {
        val task = tasks.firstOrNull { it.id == request.taskId } ?: throw ServerNotFoundException("Task not found")
        requireTaskOwnership(task.id, staffOrganization(actorUserId))
        val existing = workSessions.firstOrNull { it.ownerAdminId == actorUserId && it.status != "FINISHED" }
        if (existing != null) {
            val activeTaskTitle = tasks.firstOrNull { it.id == existing.taskId }?.title ?: existing.taskId
            throw ServerConflictException("Ya hay un temporizador activo en la tarea '$activeTaskTitle'. Detenlo o pausalo antes de iniciar uno nuevo.")
        }
        val now = java.time.Instant.now()
        val session = LocalWorkSession(
            id = "work-session-${++workSessionCounter}",
            taskId = task.id,
            ownerAdminId = actorUserId,
            clientId = task.clientId,
            authorId = authorId,
            startedAt = now,
            billable = task.clientId != null,
            segments = mutableListOf(LocalWorkSegment(id = "work-segment-${++workSegmentCounter}", startedAt = now)),
        )
        workSessions.add(session)
        return workSessionSnapshot(session)
    }

    override fun pausedWorkSession(sessionId: String, actorUserId: String): ServerWorkSessionSnapshot {
        val session = requireWorkSession(sessionId, actorUserId)
        if (session.status == "FINISHED") throw ServerConflictException("El temporizador ya esta detenido.")
        if (session.status == "ACTIVE") {
            session.segments.lastOrNull { it.endedAt == null }?.endedAt = java.time.Instant.now()
            session.status = "PAUSED"
        }
        return workSessionSnapshot(session)
    }

    override fun resumedWorkSession(sessionId: String, actorUserId: String): ServerWorkSessionSnapshot {
        val session = requireWorkSession(sessionId, actorUserId)
        if (session.status == "FINISHED") throw ServerConflictException("El temporizador ya esta detenido.")
        if (session.status == "PAUSED") {
            session.segments.add(LocalWorkSegment(id = "work-segment-${++workSegmentCounter}", startedAt = java.time.Instant.now()))
            session.status = "ACTIVE"
        }
        return workSessionSnapshot(session)
    }

    override fun stoppedWorkSession(sessionId: String, actorUserId: String): ServerWorkSessionSnapshot {
        val session = requireWorkSession(sessionId, actorUserId)
        if (session.status == "FINISHED") throw ServerConflictException("El temporizador ya esta detenido.")
        val now = java.time.Instant.now()
        session.segments.lastOrNull { it.endedAt == null }?.endedAt = now
        session.status = "FINISHED"
        session.endedAt = now

        val madrid = java.time.ZoneId.of("Europe/Madrid")
        val perDaySeconds = linkedMapOf<String, Long>()
        session.segments.forEach { segment ->
            val end = segment.endedAt ?: return@forEach
            var cursor = segment.startedAt
            while (cursor.isBefore(end)) {
                val localDate = java.time.LocalDateTime.ofInstant(cursor, madrid).toLocalDate()
                val nextMidnight = localDate.plusDays(1).atStartOfDay(madrid).toInstant()
                val sliceEnd = if (nextMidnight.isBefore(end)) nextMidnight else end
                val seconds = java.time.Duration.between(cursor, sliceEnd).seconds
                perDaySeconds.merge(localDate.toString(), seconds, Long::plus)
                cursor = sliceEnd
            }
        }
        var totalSeconds = 0L
        perDaySeconds.forEach { (date, seconds) ->
            if (seconds <= 0) return@forEach
            totalSeconds += seconds
            timeLogs.add(
                0,
                ServerTimeLogSnapshot(
                    id = "time-log-${timeLogs.size + 1}",
                    ownerAdminId = actorUserId,
                    taskId = session.taskId,
                    clientId = session.clientId,
                    authorId = session.authorId,
                    authorName = adminAccounts.firstOrNull { it.userId == session.authorId }?.name ?: "Admin",
                    minutes = (seconds / 60).toInt(),
                    seconds = seconds.toInt(),
                    workDate = date,
                    note = "Sesion de trabajo",
                    billable = session.billable,
                    createdAt = now.toString(),
                    sessionId = session.id,
                ),
            )
        }
        if (totalSeconds > 0) {
            val taskIndex = tasks.indexOfFirst { it.id == session.taskId }
            if (taskIndex >= 0) {
                val current = tasks[taskIndex]
                val updatedSeconds = current.loggedSeconds + totalSeconds.toInt()
                tasks[taskIndex] = current.copy(
                    loggedMinutes = updatedSeconds / 60,
                    loggedSeconds = updatedSeconds,
                    updatedAt = now.toString(),
                )
            }
        }
        return workSessionSnapshot(session)
    }

    override fun checkpointedWorkSession(sessionId: String, actorUserId: String): ServerWorkSessionSnapshot {
        val session = requireWorkSession(sessionId, actorUserId)
        session.lastCheckpointAt = java.time.Instant.now()
        return workSessionSnapshot(session)
    }

    private fun requireWorkSession(sessionId: String, actorUserId: String): LocalWorkSession =
        workSessions.firstOrNull { it.id == sessionId && it.ownerAdminId == actorUserId }
            ?: throw ServerNotFoundException("Work session not found")

    private fun workSessionSnapshot(session: LocalWorkSession): ServerWorkSessionSnapshot {
        val closedSeconds = session.segments.filter { it.endedAt != null }
            .sumOf { java.time.Duration.between(it.startedAt, it.endedAt).seconds }
        return ServerWorkSessionSnapshot(
            id = session.id,
            ownerAdminId = session.ownerAdminId,
            taskId = session.taskId,
            clientId = session.clientId,
            authorId = session.authorId,
            startedAt = session.startedAt.toString(),
            endedAt = session.endedAt?.toString(),
            status = session.status,
            billable = session.billable,
            timeSource = "SERVER",
            businessTimezone = "Europe/Madrid",
            lastCheckpointAt = session.lastCheckpointAt?.toString(),
            createdAt = session.startedAt.toString(),
            updatedAt = (session.lastCheckpointAt ?: session.endedAt ?: session.startedAt).toString(),
            segments = session.segments.map {
                ServerWorkSegmentSnapshot(id = it.id, sessionId = session.id, startedAt = it.startedAt.toString(), endedAt = it.endedAt?.toString())
            },
            closedSegmentsSeconds = closedSeconds,
            openSegmentStartedAt = session.segments.firstOrNull { it.endedAt == null }?.startedAt?.toString(),
        )
    }

    override fun getDashboard(clientId: String?, labelId: String?, organizationId: String): ServerDashboardSnapshot {
        val visibleClients = getClients(organizationId)
        val filteredTasks = getTasks(clientId, labelId, organizationId)
        val filteredLogs = getTimeLogs(clientId, null, organizationId)
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
                .mapNotNull { log -> log.workDate?.let { date -> date to log } }
                .groupBy({ it.first }, { it.second })
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

    /** Portal-only: [clientId] comes from the caller's own token, so no company scope is needed. */
    private fun requireOwnClientIndex(clientId: String): Int = clients.indexOfFirst { it.id == clientId }
        .takeIf { it >= 0 } ?: throw ServerNotFoundException("Client not found")

    private fun requireClientIndex(clientId: String, organizationId: String): Int = clients.indexOfFirst {
        it.id == clientId && (clientOwners[it.id] == organizationId)
    }
        .takeIf { it >= 0 } ?: throw ServerNotFoundException("Client not found")

    private fun requireLabelIndex(labelId: String, organizationId: String): Int = labels.indexOfFirst {
        it.id == labelId && (organizationOfUser[it.ownerAdminId] == organizationId)
    }
        .takeIf { it >= 0 } ?: throw ServerNotFoundException("Label not found")

    private fun requireTaskIndex(taskId: String, organizationId: String): Int = tasks.indexOfFirst {
        it.id == taskId && (taskOwners[it.id] == organizationId)
    }
        .takeIf { it >= 0 } ?: throw ServerNotFoundException("Task not found")

    private fun requireClientOwnership(clientId: String, organizationId: String) {
        if (clientOwners[clientId] != organizationId) {
            throw ServerNotFoundException("Client not found")
        }
    }

    private fun requireTaskOwnership(taskId: String, organizationId: String) {
        if (taskOwners[taskId] != organizationId) {
            throw ServerNotFoundException("Task not found")
        }
    }

    private fun requireProgramRequestIndex(requestId: String, organizationId: String): Int =
        programRequests.indexOfFirst { it.id == requestId && clientOwners[it.clientId] == organizationId }
            .takeIf { it >= 0 } ?: throw ServerNotFoundException("Program request not found")

    private fun upsertProgramSubscription(
        clientId: String,
        productKey: String,
        monthlyPriceCents: Long,
        startsOn: String,
    ) {
        val index = programSubscriptions.indexOfFirst {
            it.clientId == clientId && it.snapshot.productKey == productKey
        }
        val updated = LocalProgramSubscription(
            clientId = clientId,
            snapshot = ServerClientProductSubscriptionSnapshot(
                productKey = productKey,
                status = "ACTIVE",
                monthlyPriceCents = monthlyPriceCents,
                currency = "EUR",
                startsOn = startsOn,
            ),
        )
        if (index >= 0) programSubscriptions[index] = updated else programSubscriptions.add(updated)
    }

    private fun syncLegacyServiceSubscription(clientId: String, isEnabled: Boolean) {
        val index = programSubscriptions.indexOfFirst {
            it.clientId == clientId && it.snapshot.productKey == "SERVICE_SLA"
        }
        val current = programSubscriptions.getOrNull(index)?.snapshot
        val updated = ServerClientProductSubscriptionSnapshot(
            productKey = "SERVICE_SLA",
            status = if (isEnabled) "ACTIVE" else "CANCELLED",
            monthlyPriceCents = current?.monthlyPriceCents ?: 0,
            currency = "EUR",
            startsOn = current?.startsOn ?: Instant.now().toString().take(10),
            endsOn = if (isEnabled) null else Instant.now().toString().take(10),
        )
        val value = LocalProgramSubscription(clientId, updated)
        if (index >= 0) programSubscriptions[index] = value else programSubscriptions.add(value)
    }

    private fun String?.normalizedOptionalText(): String? =
        this?.trim()?.takeIf(String::isNotBlank)

    private fun String?.updatedOptionalText(current: String?): String? =
        if (this == null) current else normalizedOptionalText()

}

private fun businessBetaCatalog(): List<ServerProductCatalogSnapshot> = listOf(
    businessBetaProgram(
        key = "BUSINESS_INVOICING",
        name = "Facturación",
        description = "Crea borradores y documentos comerciales durante la beta.",
        category = "Finanzas",
        iconKey = "receipt-text",
        capabilities = listOf("invoice_drafts", "invoice_lines", "payment_status"),
    ),
    businessBetaProgram(
        key = "BUSINESS_ACCOUNTING",
        name = "Contabilidad y gastos",
        description = "Registra gastos, categorías y control interno de caja.",
        category = "Finanzas",
        iconKey = "landmark",
        capabilities = listOf("expenses", "categories", "cash_summary"),
    ),
    businessBetaProgram(
        key = "BUSINESS_CUSTOMERS",
        name = "Clientes y contactos",
        description = "Centraliza empresas, contactos y notas comerciales.",
        category = "Ventas",
        iconKey = "users",
        capabilities = listOf("companies", "contacts", "notes"),
    ),
    businessBetaProgram(
        key = "BUSINESS_QUOTES",
        name = "Presupuestos y ventas",
        description = "Prepara presupuestos con líneas y seguimiento.",
        category = "Ventas",
        iconKey = "file-text",
        capabilities = listOf("quote_drafts", "quote_lines", "sales_pipeline"),
    ),
    businessBetaProgram(
        key = "BUSINESS_CATALOG",
        name = "Productos, servicios y stock",
        description = "Gestiona catálogo, precios y existencias básicas.",
        category = "Operaciones",
        iconKey = "package",
        capabilities = listOf("products", "services", "stock_levels"),
    ),
    businessBetaProgram(
        key = "BUSINESS_BOOKINGS",
        name = "Agenda y reservas",
        description = "Organiza citas, reservas y disponibilidad.",
        category = "Operaciones",
        iconKey = "calendar-days",
        capabilities = listOf("appointments", "availability", "booking_status"),
    ),
    businessBetaProgram(
        key = "BUSINESS_DOCUMENTS",
        name = "Documentos y firmas",
        description = "Controla documentos, versiones y aceptaciones.",
        category = "Operaciones",
        iconKey = "file-signature",
        capabilities = listOf("document_drafts", "versions", "signature_requests"),
    ),
)

private fun businessBetaProgram(
    key: String,
    name: String,
    description: String,
    category: String,
    iconKey: String,
    capabilities: List<String>,
) = ServerProductCatalogSnapshot(
    key = key,
    name = name,
    shortDescription = description,
    category = category,
    iconKey = iconKey,
    monthlyPriceCents = 0,
    currency = "EUR",
    isRequestable = true,
    isAvailable = true,
    capabilities = capabilities,
)

// Fixed ids of the two seeded companies (tests rely on them).
const val ORG_ORYKAI = "org-orykai"
const val ORG_SECOND = "org-2"
