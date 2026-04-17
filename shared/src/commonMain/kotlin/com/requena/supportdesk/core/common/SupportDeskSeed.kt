package com.requena.supportdesk.core.common

import com.requena.supportdesk.core.model.Attachment
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.ClientAccountStatus
import com.requena.supportdesk.core.model.ClientMonthlyHoursSummary
import com.requena.supportdesk.core.model.ClientNote
import com.requena.supportdesk.core.model.ClientServiceTier
import com.requena.supportdesk.core.model.DashboardSummary
import com.requena.supportdesk.core.model.InternalComment
import com.requena.supportdesk.core.model.NotificationDevice
import com.requena.supportdesk.core.model.PreferredContactChannel
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketEvent
import com.requena.supportdesk.core.model.TicketMessage
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.TimeEntry
import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.model.WaitingOn
import com.requena.supportdesk.core.model.WorkTask

object SupportDeskSeed {
    val adminUser = User(
        id = "user-admin",
        name = "Fran Requena",
        email = SUPPORT_DESK_ADMIN_EMAIL,
        role = UserRole.ADMIN,
    )

    val partnerAdminUser = User(
        id = "user-admin-2",
        name = "Socio Operaciones",
        email = SUPPORT_DESK_PARTNER_EMAIL,
        role = UserRole.ADMIN,
    )

    val clientUser = User(
        id = "user-client",
        name = "Cliente Demo",
        email = SUPPORT_DESK_CLIENT_EMAIL,
        role = UserRole.CLIENT,
        clientId = "client-1",
    )

    private val seededClientNotes = listOf(
        ClientNote(
            id = "client-note-1",
            clientId = "client-1",
            authorId = adminUser.id,
            authorName = adminUser.name,
            body = "Cliente sensible a cambios en instaladores. Hacer smoke test en Windows antes de cada entrega.",
            createdAt = "2026-04-12T09:00:00Z",
        ),
        ClientNote(
            id = "client-note-2",
            clientId = "client-1",
            authorId = partnerAdminUser.id,
            authorName = partnerAdminUser.name,
            body = "En mayo quieren revisar backlog y propuesta mensual de soporte.",
            createdAt = "2026-04-14T18:10:00Z",
        ),
        ClientNote(
            id = "client-note-3",
            clientId = "client-2",
            authorId = partnerAdminUser.id,
            authorName = partnerAdminUser.name,
            body = "Prefieren cambios agrupados y validacion por correo antes de desplegar.",
            createdAt = "2026-04-13T11:45:00Z",
        ),
    )

    private val seededTimeEntries = listOf(
        TimeEntry(
            id = "time-1",
            clientId = "client-1",
            ticketId = "ticket-1",
            authorId = adminUser.id,
            authorName = adminUser.name,
            minutes = 95,
            workDate = "2026-04-12",
            note = "Analisis de logs y reproduccion del fallo de arranque.",
            billable = true,
            createdAt = "2026-04-12T10:40:00Z",
        ),
        TimeEntry(
            id = "time-2",
            clientId = "client-1",
            ticketId = "ticket-1",
            authorId = partnerAdminUser.id,
            authorName = partnerAdminUser.name,
            minutes = 50,
            workDate = "2026-04-13",
            note = "Seguimiento con cliente y validacion interna del parche.",
            billable = false,
            createdAt = "2026-04-13T17:20:00Z",
        ),
        TimeEntry(
            id = "time-3",
            clientId = "client-2",
            ticketId = "ticket-2",
            authorId = adminUser.id,
            authorName = adminUser.name,
            minutes = 75,
            workDate = "2026-04-14",
            note = "Diseno funcional del campo de referencia y respuesta al cliente.",
            billable = true,
            createdAt = "2026-04-14T16:25:00Z",
        ),
    )

    private val seededTaskCategories = listOf(
        TaskCategory(
            id = "task-category-1",
            name = "Hoy",
            colorHex = "#6B7A5B",
        ),
        TaskCategory(
            id = "task-category-2",
            name = "Seguimiento",
            colorHex = "#A67C52",
        ),
        TaskCategory(
            id = "task-category-3",
            name = "Bloqueos",
            colorHex = "#7D4E57",
        ),
    )

    private val seededTaskLogs = listOf(
        TaskLog(
            id = "task-log-1",
            taskId = "task-1",
            clientId = "client-1",
            authorId = adminUser.id,
            authorName = adminUser.name,
            minutes = 80,
            workDate = "2026-04-10",
            note = "Revision del arranque en escritorio.",
            billable = true,
            createdAt = "2026-04-10T12:10:00Z",
        ),
        TaskLog(
            id = "task-log-2",
            taskId = "task-2",
            clientId = "client-2",
            authorId = partnerAdminUser.id,
            authorName = partnerAdminUser.name,
            minutes = 45,
            workDate = "2026-04-12",
            note = "Preparacion de feedback para cliente.",
            billable = true,
            createdAt = "2026-04-12T18:20:00Z",
        ),
        TaskLog(
            id = "task-log-3",
            taskId = "task-3",
            clientId = null,
            authorId = adminUser.id,
            authorName = adminUser.name,
            minutes = 35,
            workDate = "2026-04-14",
            note = "Plan semanal interna.",
            billable = false,
            createdAt = "2026-04-14T09:40:00Z",
        ),
    )

    private val seededTasks = listOf(
        WorkTask(
            id = "task-1",
            title = "Revisar build de escritorio de Northwind",
            clientId = "client-1",
            categoryId = "task-category-1",
            description = "Validar arranque limpio y registrar horas facturables.",
            dueDate = "2026-04-15",
            loggedMinutes = seededTaskLogs.filter { it.taskId == "task-1" }.sumOf { it.minutes },
            createdAt = "2026-04-10T08:00:00Z",
            updatedAt = "2026-04-14T16:40:00Z",
        ),
        WorkTask(
            id = "task-2",
            title = "Enviar seguimiento de Forge Flow",
            clientId = "client-2",
            categoryId = "task-category-2",
            description = "Cerrar dudas del formulario y preparar siguiente iteracion.",
            dueDate = "2026-04-16",
            loggedMinutes = seededTaskLogs.filter { it.taskId == "task-2" }.sumOf { it.minutes },
            createdAt = "2026-04-12T10:30:00Z",
            updatedAt = "2026-04-14T18:20:00Z",
        ),
        WorkTask(
            id = "task-3",
            title = "Planificar semana y priorizar agenda",
            clientId = null,
            categoryId = "task-category-3",
            description = "Ordenar huecos del mes y repartirse carga.",
            dueDate = "2026-04-15",
            loggedMinutes = seededTaskLogs.filter { it.taskId == "task-3" }.sumOf { it.minutes },
            createdAt = "2026-04-14T08:50:00Z",
            updatedAt = "2026-04-14T09:40:00Z",
        ),
    )

    val clients = listOf(
        Client(
            id = "client-1",
            companyName = "Northwind Studio",
            productName = "Northwind Desk",
            contactName = "Ana Torres",
            email = "ana@northwind.dev",
            accountStatus = ClientAccountStatus.ACTIVE,
            serviceTier = ClientServiceTier.PRIORITY,
            preferredContactChannel = PreferredContactChannel.TICKET,
            activeTicketCount = 2,
            notes = seededClientNotes.filter { it.clientId == "client-1" },
            monthlyHoursSummary = monthlySummaryFor("client-1"),
            timeEntries = seededTimeEntries.filter { it.clientId == "client-1" },
        ),
        Client(
            id = "client-2",
            companyName = "Pixel Forge",
            productName = "Forge Flow",
            contactName = "David Vega",
            email = "david@pixelforge.dev",
            accountStatus = ClientAccountStatus.ACTIVE,
            serviceTier = ClientServiceTier.STANDARD,
            preferredContactChannel = PreferredContactChannel.EMAIL,
            activeTicketCount = 1,
            notes = seededClientNotes.filter { it.clientId == "client-2" },
            monthlyHoursSummary = monthlySummaryFor("client-2"),
            timeEntries = seededTimeEntries.filter { it.clientId == "client-2" },
        ),
    )

    fun clientNotes(): List<ClientNote> = seededClientNotes

    fun timeEntries(): List<TimeEntry> = seededTimeEntries

    fun taskCategories(): List<TaskCategory> = seededTaskCategories

    fun taskLogs(): List<TaskLog> = seededTaskLogs

    fun workTasks(): List<WorkTask> = seededTasks

    fun tickets(): List<Ticket> {
        val firstAttachment = Attachment(
            id = "attachment-1",
            fileName = "error-log.txt",
            contentType = "text/plain",
            sizeBytes = 2048,
            uploadedBy = clientUser.name,
            uploadedAt = "2026-03-19T08:30:00Z",
        )

        val firstTicketMessages = listOf(
            TicketMessage(
                id = "message-1",
                ticketId = "ticket-1",
                authorId = clientUser.id,
                authorName = clientUser.name,
                body = "La aplicacion de escritorio no arranca despues de actualizar.",
                createdAt = "2026-03-19T08:30:00Z",
                attachments = listOf(firstAttachment),
            ),
            TicketMessage(
                id = "message-2",
                ticketId = "ticket-1",
                authorId = adminUser.id,
                authorName = adminUser.name,
                body = "He revisado el problema y estoy preparando un parche.",
                createdAt = "2026-03-19T09:10:00Z",
            ),
        )

        return listOf(
            Ticket(
                id = "ticket-1",
                clientId = "client-1",
                ticketNumber = "SD-1001",
                subject = "Desktop app no arranca",
                description = "Error critico al iniciar la ultima build enviada.",
                category = TicketCategory.BUG,
                affectedApp = "Northwind Desk",
                platform = SupportPlatform.DESKTOP,
                appVersion = "1.8.2 (418)",
                stepsToReproduce = "Abrir la app desde el acceso directo despues de instalar la ultima build.",
                clientReference = "NW-REL-418",
                status = TicketStatus.IN_PROGRESS,
                priority = TicketPriority.HIGH,
                waitingOn = WaitingOn.ADMIN,
                requester = clientUser,
                assignee = adminUser,
                createdAt = "2026-03-19T08:30:00Z",
                updatedAt = "2026-03-19T09:10:00Z",
                attachments = listOf(firstAttachment),
                messages = firstTicketMessages,
                internalComments = listOf(
                    InternalComment(
                        id = "comment-1",
                        ticketId = "ticket-1",
                        authorId = adminUser.id,
                        authorName = adminUser.name,
                        body = "Posible incompatibilidad con el nuevo empaquetado MSI.",
                        createdAt = "2026-03-19T09:20:00Z",
                    ),
                ),
                timeEntries = seededTimeEntries.filter { it.ticketId == "ticket-1" },
                events = listOf(
                    TicketEvent(
                        id = "event-1",
                        ticketId = "ticket-1",
                        type = "CREATED",
                        description = "Ticket creado por cliente",
                        actorName = clientUser.name,
                        createdAt = "2026-03-19T08:30:00Z",
                    ),
                    TicketEvent(
                        id = "event-2",
                        ticketId = "ticket-1",
                        type = "STATUS_CHANGED",
                        description = "Estado cambiado a IN_PROGRESS",
                        actorName = adminUser.name,
                        createdAt = "2026-03-19T09:10:00Z",
                    ),
                ),
            ),
            Ticket(
                id = "ticket-2",
                clientId = "client-2",
                ticketNumber = "SD-1002",
                subject = "Anadir campo de referencia en formulario",
                description = "Necesitamos un campo extra para enlazar pedidos con tickets.",
                category = TicketCategory.CHANGE_REQUEST,
                affectedApp = "Forge Flow",
                platform = SupportPlatform.WEB,
                appVersion = "2.4.0",
                stepsToReproduce = "Abrir el formulario de alta de ticket y revisar los campos visibles.",
                clientReference = "PF-Q2-REQ-12",
                status = TicketStatus.PENDING_CLIENT,
                priority = TicketPriority.MEDIUM,
                waitingOn = WaitingOn.CLIENT,
                requester = User(
                    id = "user-client-2",
                    name = "David Vega",
                    email = "david@pixelforge.dev",
                    role = UserRole.CLIENT,
                    clientId = "client-2",
                ),
                assignee = adminUser,
                createdAt = "2026-03-18T16:00:00Z",
                updatedAt = "2026-03-19T07:50:00Z",
                resolutionSummary = "Pending client confirmation about the exact reference format.",
                messages = listOf(
                    TicketMessage(
                        id = "message-3",
                        ticketId = "ticket-2",
                        authorId = "user-client-2",
                        authorName = "David Vega",
                        body = "Adjunto mockup y comportamiento esperado.",
                        createdAt = "2026-03-18T16:05:00Z",
                    ),
                ),
                internalComments = listOf(
                    InternalComment(
                        id = "comment-2",
                        ticketId = "ticket-2",
                        authorId = partnerAdminUser.id,
                        authorName = partnerAdminUser.name,
                        body = "Confirmar si el campo debe ser unico o solo visible en admin.",
                        createdAt = "2026-04-14T17:15:00Z",
                    ),
                ),
                timeEntries = seededTimeEntries.filter { it.ticketId == "ticket-2" },
                events = listOf(
                    TicketEvent(
                        id = "event-3",
                        ticketId = "ticket-2",
                        type = "PRIORITY_SET",
                        description = "Prioridad asignada a MEDIUM",
                        actorName = adminUser.name,
                        createdAt = "2026-03-18T16:10:00Z",
                    ),
                ),
            ),
        )
    }

    fun dashboardSummary(): DashboardSummary = DashboardSummary(
        openTickets = 3,
        inProgressTickets = 1,
        pendingClientTickets = 1,
        resolvedToday = 1,
        activeClients = clients.size,
    )

    fun defaultDevice(userId: String = adminUser.id): NotificationDevice = NotificationDevice(
        id = "device-1",
        userId = userId,
        platform = "ANDROID",
        token = "placeholder-device-token",
        lastSeenAt = "2026-03-19T10:00:00Z",
    )

    private fun monthlySummaryFor(clientId: String): ClientMonthlyHoursSummary {
        val entries = seededTimeEntries.filter { it.clientId == clientId }
        return ClientMonthlyHoursSummary(
            monthLabel = "April 2026",
            totalMinutes = entries.sumOf { it.minutes },
            billableMinutes = entries.filter { it.billable }.sumOf { it.minutes },
            entriesCount = entries.size,
        )
    }
}
