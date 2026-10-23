package com.requena.supportdesk.server.business.projects

import com.requena.supportdesk.features.business.projects.BusinessProgramMetrics
import com.requena.supportdesk.features.business.projects.BusinessProject
import com.requena.supportdesk.features.business.projects.BusinessProjectStatus
import com.requena.supportdesk.features.business.projects.BusinessProjectsWorkspace
import com.requena.supportdesk.features.business.projects.BusinessTask
import com.requena.supportdesk.features.business.projects.BusinessTaskBoardPlacement
import com.requena.supportdesk.features.business.projects.BusinessTaskBoardPlacementInput
import com.requena.supportdesk.features.business.projects.BusinessTaskLabel
import com.requena.supportdesk.features.business.projects.BusinessWorkSession
import com.requena.supportdesk.features.business.projects.CreateBusinessProjectInput
import com.requena.supportdesk.features.business.projects.CreateBusinessTaskInput
import com.requena.supportdesk.features.business.projects.CreateBusinessTaskLabelInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessProjectInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessTaskInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessTaskLabelInput

data class BusinessProjectsIdentity(
    val userId: String,
    val clientId: String,
)

data class BusinessProjectsQuery(
    val query: String? = null,
    val projectStatus: String? = null,
    val taskStatus: String? = null,
    val projectId: String? = null,
    val customerId: String? = null,
    val labelId: String? = null,
)

fun interface BusinessProjectsAccessGuard {
    fun hasActiveEntitlement(identity: BusinessProjectsIdentity): Boolean
}

interface BusinessProjectsStore {
    fun workspace(identity: BusinessProjectsIdentity, query: BusinessProjectsQuery): BusinessProjectsWorkspace
    fun createProject(identity: BusinessProjectsIdentity, input: CreateBusinessProjectInput): BusinessProject
    fun updateProject(identity: BusinessProjectsIdentity, projectId: String, input: UpdateBusinessProjectInput): BusinessProject
    fun transitionProject(identity: BusinessProjectsIdentity, projectId: String, target: BusinessProjectStatus, expectedVersion: Int): BusinessProject
    fun createTask(identity: BusinessProjectsIdentity, input: CreateBusinessTaskInput): BusinessTask
    fun updateTask(identity: BusinessProjectsIdentity, taskId: String, input: UpdateBusinessTaskInput): BusinessTask
    fun createLabel(identity: BusinessProjectsIdentity, input: CreateBusinessTaskLabelInput): BusinessTaskLabel
    fun updateLabel(identity: BusinessProjectsIdentity, labelId: String, input: UpdateBusinessTaskLabelInput): BusinessTaskLabel
    fun archiveLabel(identity: BusinessProjectsIdentity, labelId: String, expectedVersion: Int): BusinessTaskLabel
    fun placeTask(identity: BusinessProjectsIdentity, taskId: String, input: BusinessTaskBoardPlacementInput): BusinessTaskBoardPlacement
    fun removeTaskPlacement(identity: BusinessProjectsIdentity, taskId: String)
    fun activeWorkSession(identity: BusinessProjectsIdentity): BusinessWorkSession?
    fun taskWorkSessions(identity: BusinessProjectsIdentity, taskId: String): List<BusinessWorkSession>
    fun startWorkSession(identity: BusinessProjectsIdentity, taskId: String): BusinessWorkSession
    fun pauseWorkSession(identity: BusinessProjectsIdentity, sessionId: String): BusinessWorkSession
    fun resumeWorkSession(identity: BusinessProjectsIdentity, sessionId: String): BusinessWorkSession
    fun stopWorkSession(identity: BusinessProjectsIdentity, sessionId: String): BusinessWorkSession
    fun checkpointWorkSession(identity: BusinessProjectsIdentity, sessionId: String): BusinessWorkSession
    fun metrics(clientId: String): BusinessProgramMetrics
}

class BusinessProjectsForbiddenException : RuntimeException("Projects program access is required")
class BusinessProjectsNotFoundException : RuntimeException("Business project record was not found")
class BusinessProjectsConflictException(message: String) : RuntimeException(message)
