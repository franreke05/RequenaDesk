package com.requena.supportdesk.features.business.projects

import kotlinx.serialization.Serializable

const val BUSINESS_PROJECTS = "BUSINESS_PROJECTS"

@Serializable
enum class BusinessProjectStatus { ACTIVE, ON_HOLD, COMPLETED, ARCHIVED }

@Serializable
enum class BusinessTaskStatus { TODO, IN_PROGRESS, BLOCKED, DONE, ARCHIVED }

@Serializable
enum class BusinessTaskPriority { LOW, NORMAL, HIGH, URGENT }

@Serializable
enum class BusinessWorkSessionStatus { ACTIVE, PAUSED, FINISHED }

@Serializable
data class BusinessProject(
    val id: String,
    val customerId: String? = null,
    val customerName: String? = null,
    val name: String,
    val description: String? = null,
    val startDate: String? = null,
    val dueDate: String? = null,
    val status: BusinessProjectStatus,
    val openTaskCount: Int = 0,
    val totalSeconds: Long = 0,
    val version: Int,
    val updatedAt: String,
)

@Serializable
data class BusinessTaskLabel(
    val id: String,
    val name: String,
    val colorHex: String,
    val archived: Boolean = false,
    val version: Int,
    val updatedAt: String,
)

@Serializable
data class BusinessCommercialCustomerReference(
    val id: String,
    val name: String,
)

@Serializable
data class BusinessTask(
    val id: String,
    val projectId: String? = null,
    val projectName: String? = null,
    val customerId: String? = null,
    val customerName: String? = null,
    val title: String,
    val description: String? = null,
    val priority: BusinessTaskPriority,
    val dueDate: String? = null,
    val status: BusinessTaskStatus,
    val labels: List<BusinessTaskLabel> = emptyList(),
    val totalSeconds: Long = 0,
    val version: Int,
    val updatedAt: String,
)

@Serializable
data class BusinessTaskBoardPlacement(
    val taskId: String,
    val boardDate: String,
    val positionX: Double,
    val positionY: Double,
    val updatedAt: String,
)

@Serializable
data class BusinessTaskBoard(
    val today: String,
    val timeZone: String,
    val refreshAfterSeconds: Long,
    val tasks: List<BusinessTask>,
    val placements: List<BusinessTaskBoardPlacement>,
)

@Serializable
data class BusinessWorkSegment(
    val id: String,
    val startedAt: String,
    val endedAt: String? = null,
)

@Serializable
data class BusinessDailyTimeEntry(
    val workDate: String,
    val seconds: Long,
)

@Serializable
data class BusinessWorkSession(
    val id: String,
    val taskId: String,
    val taskTitle: String,
    val authorUserId: String,
    val startedAt: String,
    val endedAt: String? = null,
    val status: BusinessWorkSessionStatus,
    val elapsedSeconds: Long,
    val segments: List<BusinessWorkSegment> = emptyList(),
    val dailyEntries: List<BusinessDailyTimeEntry> = emptyList(),
)

@Serializable
data class BusinessProjectsWorkspace(
    val projects: List<BusinessProject>,
    val tasks: List<BusinessTask>,
    val labels: List<BusinessTaskLabel>,
    val board: BusinessTaskBoard,
    val activeWorkSession: BusinessWorkSession? = null,
    val customers: List<BusinessCommercialCustomerReference> = emptyList(),
)

@Serializable
data class CreateBusinessProjectInput(
    val customerId: String? = null,
    val name: String,
    val description: String? = null,
    val startDate: String? = null,
    val dueDate: String? = null,
)

@Serializable
data class UpdateBusinessProjectInput(
    val customerId: String? = null,
    val name: String,
    val description: String? = null,
    val startDate: String? = null,
    val dueDate: String? = null,
    val status: BusinessProjectStatus,
    val expectedVersion: Int,
)

@Serializable
data class ProjectTransitionInput(val expectedVersion: Int)

@Serializable
data class CreateBusinessTaskInput(
    val projectId: String? = null,
    val customerId: String? = null,
    val title: String,
    val description: String? = null,
    val priority: BusinessTaskPriority = BusinessTaskPriority.NORMAL,
    val dueDate: String? = null,
    val labelIds: List<String> = emptyList(),
)

@Serializable
data class UpdateBusinessTaskInput(
    val projectId: String? = null,
    val customerId: String? = null,
    val title: String,
    val description: String? = null,
    val priority: BusinessTaskPriority = BusinessTaskPriority.NORMAL,
    val dueDate: String? = null,
    val status: BusinessTaskStatus,
    val labelIds: List<String> = emptyList(),
    val expectedVersion: Int,
)

@Serializable
data class CreateBusinessTaskLabelInput(
    val name: String,
    val colorHex: String,
)

@Serializable
data class UpdateBusinessTaskLabelInput(
    val name: String,
    val colorHex: String,
    val expectedVersion: Int,
)

@Serializable
data class BusinessTaskBoardPlacementInput(
    val boardDate: String,
    val positionX: Double,
    val positionY: Double,
)

@Serializable
data class StartBusinessWorkSessionInput(val taskId: String)

@Serializable
data class BusinessProgramMetrics(
    val clientId: String,
    val commercialCustomerCount: Int,
    val activeProjectCount: Int,
    val openTaskCount: Int,
    val lastActivityAt: String? = null,
)

@Serializable
data class BusinessMutationResult(val success: Boolean = true)

class BusinessProjectsValidationException(message: String) : IllegalArgumentException(message)

object BusinessProjectsRules {
    private val isoDate = Regex("\\d{4}-\\d{2}-\\d{2}")
    private val color = Regex("#[0-9A-Fa-f]{6}")
    private val uuid = Regex(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
    )

    fun validate(input: CreateBusinessProjectInput) {
        required(input.name, "Project name", 160)
        optional(input.description, "Description", 4_000)
        dates(input.startDate, input.dueDate)
        optionalId(input.customerId, "Customer")
    }

    fun validate(input: UpdateBusinessProjectInput) {
        validate(CreateBusinessProjectInput(input.customerId, input.name, input.description, input.startDate, input.dueDate))
        version(input.expectedVersion)
    }

    fun validate(input: CreateBusinessTaskInput) {
        if (input.projectId != null && input.customerId != null) invalid("A task cannot belong to a project and a direct customer at the same time")
        optionalId(input.projectId, "Project")
        optionalId(input.customerId, "Customer")
        required(input.title, "Task title", 200)
        optional(input.description, "Description", 4_000)
        input.dueDate?.let(::date)
        labelIds(input.labelIds)
    }

    fun validate(input: UpdateBusinessTaskInput) {
        validate(CreateBusinessTaskInput(input.projectId, input.customerId, input.title, input.description, input.priority, input.dueDate, input.labelIds))
        version(input.expectedVersion)
    }

    fun validate(input: CreateBusinessTaskLabelInput) {
        required(input.name, "Label name", 80)
        if (!color.matches(input.colorHex)) invalid("Label color must use #RRGGBB")
    }

    fun validate(input: UpdateBusinessTaskLabelInput) {
        validate(CreateBusinessTaskLabelInput(input.name, input.colorHex))
        version(input.expectedVersion)
    }

    fun validate(input: BusinessTaskBoardPlacementInput) {
        date(input.boardDate)
        if (!input.positionX.isFinite() || input.positionX !in 0.0..1.0 ||
            !input.positionY.isFinite() || input.positionY !in 0.0..1.0
        ) {
            invalid("Board coordinates must be between 0 and 1")
        }
    }

    fun version(value: Int) {
        if (value < 1) invalid("Expected version is invalid")
    }

    private fun dates(start: String?, due: String?) {
        start?.let(::date)
        due?.let(::date)
        if (start != null && due != null && due < start) invalid("Due date cannot precede start date")
    }

    private fun date(value: String) {
        if (!isoDate.matches(value)) invalid("Dates must use YYYY-MM-DD")
    }

    private fun labelIds(values: List<String>) {
        if (values.size > 20 || values.distinct().size != values.size || values.any { !uuid.matches(it) }) {
            invalid("Task labels are invalid")
        }
    }

    private fun optionalId(value: String?, label: String) {
        if (value != null && !uuid.matches(value)) invalid("$label identifier is invalid")
    }

    private fun required(value: String, label: String, max: Int) {
        if (value.trim().length !in 1..max) invalid("$label is invalid")
    }

    private fun optional(value: String?, label: String, max: Int) {
        if (value != null && value.trim().length > max) invalid("$label is invalid")
    }

    private fun invalid(message: String): Nothing = throw BusinessProjectsValidationException(message)
}
