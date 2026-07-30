package com.requena.supportdesk.core.model

data class WorkSegment(
    val id: String,
    val sessionId: String,
    val startedAt: String,
    val endedAt: String? = null,
)

/**
 * The global timer's persisted state: one session covers a start-to-stop run on a single task,
 * made up of one or more [segments] separated by pauses. [status] follows the session lifecycle
 * (ACTIVE/PAUSED/FINISHED); [billable] is decided once at start and never changes afterwards.
 */
data class WorkSession(
    val id: String,
    val ownerAdminId: String,
    val taskId: String,
    val clientId: String? = null,
    val authorId: String,
    val startedAt: String,
    val endedAt: String? = null,
    val status: WorkSessionStatus,
    val billable: Boolean,
    val timeSource: String,
    val businessTimezone: String,
    val lastCheckpointAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val segments: List<WorkSegment> = emptyList(),
    val closedSegmentsSeconds: Long = 0,
    val openSegmentStartedAt: String? = null,
)

enum class WorkSessionStatus {
    ACTIVE,
    PAUSED,
    FINISHED,
    ;

    companion object {
        fun fromRaw(raw: String): WorkSessionStatus = entries.firstOrNull { it.name == raw } ?: FINISHED
    }
}
