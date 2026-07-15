package com.requena.supportdesk.app.admin.screens

import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.WorkTask
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminTasksScreenTest {
    private val client = Client(
        id = "client-1",
        companyName = "OryKai Software",
        contactName = "Fran",
        email = "fran@example.com",
    )
    private val category = TaskCategory(
        id = "label-1",
        name = "Revisión interna",
        colorHex = "#2E7D5B",
    )
    private val task = WorkTask(
        id = "task-1",
        title = "Preparar propuesta",
        clientId = client.id,
        categoryId = category.id,
        description = "Revisar el alcance del proyecto",
        completed = false,
        createdAt = "2026-07-15T08:00:00Z",
        updatedAt = "2026-07-15T08:00:00Z",
    )

    @Test
    fun taskSearchUsesTaskClientAndCategoryRealFields() {
        val clients = mapOf(client.id to client)
        val categories = mapOf(category.id to category)

        assertTrue(task.matchesTaskQuery("propuesta", clients, categories))
        assertTrue(task.matchesTaskQuery("alcance", clients, categories))
        assertTrue(task.matchesTaskQuery("orykai", clients, categories))
        assertTrue(task.matchesTaskQuery("REVISIÓN", clients, categories))
        assertFalse(task.matchesTaskQuery("factura", clients, categories))
    }

    @Test
    fun statusFilterOnlyUsesThePersistedCompletionFlag() {
        assertTrue(TaskStatusFilter.ALL.matches(task))
        assertTrue(TaskStatusFilter.ACTIVE.matches(task))
        assertFalse(TaskStatusFilter.COMPLETED.matches(task))

        val completedTask = task.copy(completed = true)
        assertFalse(TaskStatusFilter.ACTIVE.matches(completedTask))
        assertTrue(TaskStatusFilter.COMPLETED.matches(completedTask))
    }

    @Test
    fun taskLayoutStacksBelowTheDesktopSplitBreakpoint() {
        assertTrue(resolveTaskLayoutMode(899.dp) == TaskLayoutMode.STACKED)
        assertTrue(resolveTaskLayoutMode(900.dp) == TaskLayoutMode.SPLIT)
        assertTrue(resolveTaskLayoutMode(1440.dp) == TaskLayoutMode.SPLIT)
    }
}
