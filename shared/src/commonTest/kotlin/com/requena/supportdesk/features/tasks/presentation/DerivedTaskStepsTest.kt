package com.requena.supportdesk.features.tasks.presentation

import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.TaskLog
import com.requena.supportdesk.core.model.WorkSession
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.tasks.domain.model.MAX_DERIVED_TASK_STEPS
import com.requena.supportdesk.features.tasks.domain.model.TaskDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskLabelDraft
import com.requena.supportdesk.features.tasks.domain.model.TaskUpdateInput
import com.requena.supportdesk.features.tasks.domain.model.derivedTaskStepDescription
import com.requena.supportdesk.features.tasks.domain.model.derivedTaskStepTitle
import com.requena.supportdesk.features.tasks.domain.model.parseDerivedTaskSteps
import com.requena.supportdesk.features.tasks.domain.repository.TasksRepository
import com.requena.supportdesk.features.tasks.domain.usecase.CheckpointWorkSessionUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.CreateTaskLabelUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.CreateTaskUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.DeleteTaskLabelUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.DeleteTaskUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetActiveWorkSessionUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetTaskLabelsUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetTaskLogsUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetTasksUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.GetWorkSessionsForTaskUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.PauseWorkSessionUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.ResumeWorkSessionUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.StartWorkSessionUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.StopWorkSessionUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.UpdateTaskLabelUseCase
import com.requena.supportdesk.features.tasks.domain.usecase.UpdateTaskUseCase
import com.requena.supportdesk.features.tasks.presentation.event.TasksUiEvent
import com.requena.supportdesk.features.tasks.presentation.viewmodel.TasksViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DerivedTaskStepsTest {
    @Test
    fun `step parser removes blanks and duplicate lines and respects the limit`() {
        val raw = buildString {
            appendLine(" Diseñar base de datos ")
            appendLine()
            appendLine("Crear login")
            appendLine("crear LOGIN")
            repeat(25) { appendLine("Paso $it") }
        }

        val steps = parseDerivedTaskSteps(raw)

        assertEquals(MAX_DERIVED_TASK_STEPS, steps.size)
        assertEquals("Diseñar base de datos", steps.first())
        assertEquals(1, steps.count { it.equals("Crear login", ignoreCase = true) })
        assertEquals("[EduTrack] Crear login", derivedTaskStepTitle("EduTrack", "Crear login"))
        assertEquals("Paso derivado de la tarea «EduTrack».", derivedTaskStepDescription("EduTrack"))
    }

    @Test
    fun `creating steps reuses normal tasks and inherits parent data`() = runBlocking {
        val repository = DerivedStepsTasksRepository()
        val viewModel = tasksViewModel(repository)
        withTimeout(TEST_TIMEOUT) {
            viewModel.state.first { !it.isLoading && it.tasks.any { task -> task.id == "parent" } }
        }

        viewModel.onEvent(
            TasksUiEvent.CreateTaskSteps(
                parentTaskId = "parent",
                steps = "Diseñar base de datos\nCrear login\ncrear LOGIN\nProgramar dashboard",
            ),
        )

        val completed = withTimeout(TEST_TIMEOUT) {
            viewModel.state.first { it.taskStepsCreationVersion == 1 && !it.isCreatingTaskSteps && !it.isLoading }
        }

        assertEquals(3, repository.createdDrafts.size)
        assertEquals(
            listOf(
                "[EduTrack] Diseñar base de datos",
                "[EduTrack] Crear login",
                "[EduTrack] Programar dashboard",
            ),
            repository.createdDrafts.map(TaskDraft::title),
        )
        assertTrue(repository.createdDrafts.all { it.clientId == "client-1" })
        assertTrue(repository.createdDrafts.all { it.categoryId == "label-1" })
        assertTrue(repository.createdDrafts.all { it.dueDate == "2099-12-31" })
        assertTrue(repository.createdDrafts.all { it.description.contains("EduTrack") })
        assertEquals(4, completed.tasks.size)
        assertFalse(completed.errorMessage?.isNotBlank() == true)
        viewModel.clear()
    }
}

private class DerivedStepsTasksRepository : TasksRepository {
    private val parent = WorkTask(
        id = "parent",
        title = "EduTrack",
        clientId = "client-1",
        categoryId = "label-1",
        description = "Proyecto educativo",
        dueDate = "2099-12-31",
        completed = false,
        createdAt = "2026-07-30T08:00:00Z",
        updatedAt = "2026-07-30T08:00:00Z",
    )
    val createdDrafts = mutableListOf<TaskDraft>()

    override suspend fun getLabels(): AppResult<List<TaskCategory>> =
        AppResult.Success(listOf(TaskCategory("label-1", "EduTrack", "#336699")))

    override suspend fun getTasks(): AppResult<List<WorkTask>> =
        AppResult.Success(
            listOf(parent) + createdDrafts.mapIndexed { index, draft ->
                WorkTask(
                    id = "step-${index + 1}",
                    title = draft.title,
                    clientId = draft.clientId,
                    categoryId = draft.categoryId,
                    description = draft.description,
                    dueDate = draft.dueDate,
                    completed = false,
                    createdAt = "2026-07-30T08:00:00Z",
                    updatedAt = "2026-07-30T08:00:00Z",
                )
            },
        )

    override suspend fun getTimeLogs(): AppResult<List<TaskLog>> = AppResult.Success(emptyList())

    override suspend fun createTask(input: TaskDraft): AppResult<WorkTask> {
        createdDrafts += input
        return AppResult.Success(
            WorkTask(
                id = "step-${createdDrafts.size}",
                title = input.title,
                clientId = input.clientId,
                categoryId = input.categoryId,
                description = input.description,
                dueDate = input.dueDate,
                completed = false,
                createdAt = "2026-07-30T08:00:00Z",
                updatedAt = "2026-07-30T08:00:00Z",
            ),
        )
    }

    override suspend fun updateTask(taskId: String, input: TaskUpdateInput): AppResult<WorkTask> =
        AppResult.Error("Not used")

    override suspend fun deleteTask(taskId: String): AppResult<Unit> = AppResult.Error("Not used")

    override suspend fun createLabel(input: TaskLabelDraft): AppResult<TaskCategory> = AppResult.Error("Not used")

    override suspend fun updateLabel(labelId: String, input: TaskLabelDraft): AppResult<TaskCategory> =
        AppResult.Error("Not used")

    override suspend fun deleteLabel(labelId: String): AppResult<Unit> = AppResult.Error("Not used")

    override suspend fun getActiveWorkSession(): AppResult<WorkSession?> = AppResult.Success(null)

    override suspend fun getWorkSessionsForTask(taskId: String): AppResult<List<WorkSession>> =
        AppResult.Success(emptyList())

    override suspend fun startWorkSession(taskId: String): AppResult<WorkSession> = AppResult.Error("Not used")

    override suspend fun pauseWorkSession(sessionId: String): AppResult<WorkSession> = AppResult.Error("Not used")

    override suspend fun resumeWorkSession(sessionId: String): AppResult<WorkSession> = AppResult.Error("Not used")

    override suspend fun stopWorkSession(sessionId: String): AppResult<WorkSession> = AppResult.Error("Not used")

    override suspend fun checkpointWorkSession(sessionId: String): AppResult<WorkSession> = AppResult.Error("Not used")
}

private fun tasksViewModel(repository: TasksRepository) = TasksViewModel(
    getTaskLabelsUseCase = GetTaskLabelsUseCase(repository),
    getTasksUseCase = GetTasksUseCase(repository),
    getTaskLogsUseCase = GetTaskLogsUseCase(repository),
    createTaskUseCase = CreateTaskUseCase(repository),
    updateTaskUseCase = UpdateTaskUseCase(repository),
    deleteTaskUseCase = DeleteTaskUseCase(repository),
    createTaskLabelUseCase = CreateTaskLabelUseCase(repository),
    updateTaskLabelUseCase = UpdateTaskLabelUseCase(repository),
    deleteTaskLabelUseCase = DeleteTaskLabelUseCase(repository),
    getActiveWorkSessionUseCase = GetActiveWorkSessionUseCase(repository),
    startWorkSessionUseCase = StartWorkSessionUseCase(repository),
    pauseWorkSessionUseCase = PauseWorkSessionUseCase(repository),
    resumeWorkSessionUseCase = ResumeWorkSessionUseCase(repository),
    stopWorkSessionUseCase = StopWorkSessionUseCase(repository),
    checkpointWorkSessionUseCase = CheckpointWorkSessionUseCase(repository),
    getWorkSessionsForTaskUseCase = GetWorkSessionsForTaskUseCase(repository),
)

private const val TEST_TIMEOUT = 5_000L
