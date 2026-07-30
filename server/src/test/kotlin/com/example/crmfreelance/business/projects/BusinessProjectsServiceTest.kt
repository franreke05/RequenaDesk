package com.example.crmfreelance.business.projects

import com.requena.supportdesk.features.business.projects.BusinessProjectStatus
import com.requena.supportdesk.features.business.projects.BusinessTaskBoardPlacementInput
import com.requena.supportdesk.features.business.projects.BusinessTaskPriority
import com.requena.supportdesk.features.business.projects.BusinessTaskStatus
import com.requena.supportdesk.features.business.projects.CreateBusinessProjectInput
import com.requena.supportdesk.features.business.projects.CreateBusinessTaskInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessTaskInput
import com.requena.supportdesk.server.business.projects.BusinessProjectsAccessGuard
import com.requena.supportdesk.server.business.projects.BusinessProjectsConflictException
import com.requena.supportdesk.server.business.projects.BusinessProjectsForbiddenException
import com.requena.supportdesk.server.business.projects.BusinessProjectsIdentity
import com.requena.supportdesk.server.business.projects.BusinessProjectsNotFoundException
import com.requena.supportdesk.server.business.projects.BusinessProjectsQuery
import com.requena.supportdesk.server.business.projects.BusinessProjectsService
import com.requena.supportdesk.server.business.projects.InMemoryBusinessProjectsStore
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BusinessProjectsServiceTest {
    private val tenantA = BusinessProjectsIdentity("00000000-0000-0000-0000-000000000011", "00000000-0000-0000-0000-000000000001")
    private val tenantB = BusinessProjectsIdentity("00000000-0000-0000-0000-000000000022", "00000000-0000-0000-0000-000000000002")

    @Test
    fun entitlementAndTenantIsolationAreServerAuthoritative() {
        val denied = BusinessProjectsService(InMemoryBusinessProjectsStore(), BusinessProjectsAccessGuard { false })
        assertFailsWith<BusinessProjectsForbiddenException> { denied.workspace(tenantA, BusinessProjectsQuery()) }

        val service = service()
        val project = service.createProject(tenantA, CreateBusinessProjectInput(name = "Privado"))
        assertFailsWith<BusinessProjectsNotFoundException> { service.project(tenantB, project.id) }
    }

    @Test
    fun taskHasOneParentAndClosingItRemovesItsDailyPlacement() {
        val service = service()
        val project = service.createProject(tenantA, CreateBusinessProjectInput(name = "Lanzamiento"))
        assertFailsWith<IllegalArgumentException> {
            service.createTask(
                tenantA,
                CreateBusinessTaskInput(
                    projectId = project.id,
                    customerId = "00000000-0000-0000-0000-000000000099",
                    title = "Ambigua",
                ),
            )
        }
        val task = service.createTask(tenantA, CreateBusinessTaskInput(projectId = project.id, title = "Preparar piloto"))
        service.placeTask(tenantA, task.id, BusinessTaskBoardPlacementInput("2030-01-02", 0.2, 0.7))
        assertEquals(1, service.board(tenantA).placements.size)

        service.updateTask(
            tenantA,
            task.id,
            UpdateBusinessTaskInput(
                projectId = project.id,
                title = task.title,
                priority = BusinessTaskPriority.NORMAL,
                status = BusinessTaskStatus.DONE,
                expectedVersion = task.version,
            ),
        )
        assertEquals(0, service.board(tenantA).placements.size)
    }

    @Test
    fun boardRejectsPastDatesAndKeepsOnlyOnePlacementPerTask() {
        var now = Instant.parse("2026-07-30T10:00:00Z")
        val service = service(InMemoryBusinessProjectsStore(now = { now }))
        val task = service.createTask(tenantA, CreateBusinessTaskInput(title = "Planificar"))

        assertFailsWith<BusinessProjectsConflictException> {
            service.placeTask(tenantA, task.id, BusinessTaskBoardPlacementInput("2026-07-29", 0.2, 0.2))
        }
        service.placeTask(tenantA, task.id, BusinessTaskBoardPlacementInput("2026-07-30", 0.2, 0.2))
        service.placeTask(tenantA, task.id, BusinessTaskBoardPlacementInput("2026-08-01", 0.8, 0.7))
        val board = service.board(tenantA)
        assertEquals(1, board.placements.size)
        assertEquals("2026-08-01", board.placements.single().boardDate)
    }

    @Test
    fun timerIsUniquePerUserAndSplitsAtMadridMidnight() {
        var now = Instant.parse("2026-07-30T21:59:00Z")
        val service = service(InMemoryBusinessProjectsStore(now = { now }))
        val firstTask = service.createTask(tenantA, CreateBusinessTaskInput(title = "Primera"))
        val secondTask = service.createTask(tenantA, CreateBusinessTaskInput(title = "Segunda"))
        val session = service.startWorkSession(tenantA, firstTask.id)

        assertFailsWith<BusinessProjectsConflictException> { service.startWorkSession(tenantA, secondTask.id) }
        service.pauseWorkSession(tenantA, session.id)
        assertFailsWith<BusinessProjectsConflictException> { service.startWorkSession(tenantA, secondTask.id) }
        service.resumeWorkSession(tenantA, session.id)

        now = Instant.parse("2026-07-30T22:01:00Z")
        val stopped = service.stopWorkSession(tenantA, session.id)
        assertEquals(listOf("2026-07-30", "2026-07-31"), stopped.dailyEntries.map { it.workDate })
        assertEquals(120, stopped.dailyEntries.sumOf { it.seconds })
        assertNull(service.activeWorkSession(tenantA))
    }

    private fun service(store: InMemoryBusinessProjectsStore = InMemoryBusinessProjectsStore()) =
        BusinessProjectsService(store, BusinessProjectsAccessGuard { true })
}
