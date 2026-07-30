package com.requena.supportdesk.server.business.projects

import com.requena.supportdesk.features.business.projects.BusinessProjectStatus
import com.requena.supportdesk.features.business.projects.BusinessProjectsRules
import com.requena.supportdesk.features.business.projects.BusinessTaskStatus
import com.requena.supportdesk.features.business.projects.CreateBusinessProjectInput
import com.requena.supportdesk.features.business.projects.CreateBusinessTaskInput
import com.requena.supportdesk.features.business.projects.CreateBusinessTaskLabelInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessProjectInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessTaskInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessTaskLabelInput
import com.requena.supportdesk.features.business.projects.BusinessTaskBoardPlacementInput

class BusinessProjectsService(
    private val store: BusinessProjectsStore,
    private val accessGuard: BusinessProjectsAccessGuard,
) {
    fun workspace(identity: BusinessProjectsIdentity, query: BusinessProjectsQuery) =
        authorized(identity) { store.workspace(identity, query.validated()) }

    fun projects(identity: BusinessProjectsIdentity, query: BusinessProjectsQuery) =
        workspace(identity, query).projects

    fun project(identity: BusinessProjectsIdentity, projectId: String) =
        workspace(identity, BusinessProjectsQuery()).projects.firstOrNull { it.id == projectId }
            ?: throw BusinessProjectsNotFoundException()

    fun tasks(identity: BusinessProjectsIdentity, query: BusinessProjectsQuery) =
        workspace(identity, query).tasks

    fun task(identity: BusinessProjectsIdentity, taskId: String) =
        workspace(identity, BusinessProjectsQuery()).tasks.firstOrNull { it.id == taskId }
            ?: throw BusinessProjectsNotFoundException()

    fun board(identity: BusinessProjectsIdentity) =
        workspace(identity, BusinessProjectsQuery()).board

    fun createProject(identity: BusinessProjectsIdentity, input: CreateBusinessProjectInput) =
        authorized(identity) {
            BusinessProjectsRules.validate(input)
            store.createProject(identity, input.normalized())
        }

    fun updateProject(identity: BusinessProjectsIdentity, projectId: String, input: UpdateBusinessProjectInput) =
        authorized(identity) {
            BusinessProjectsRules.validate(input)
            store.updateProject(identity, projectId, input.normalized())
        }

    fun transitionProject(
        identity: BusinessProjectsIdentity,
        projectId: String,
        target: BusinessProjectStatus,
        expectedVersion: Int,
    ) = authorized(identity) {
        BusinessProjectsRules.version(expectedVersion)
        store.transitionProject(identity, projectId, target, expectedVersion)
    }

    fun createTask(identity: BusinessProjectsIdentity, input: CreateBusinessTaskInput) =
        authorized(identity) {
            BusinessProjectsRules.validate(input)
            store.createTask(identity, input.normalized())
        }

    fun updateTask(identity: BusinessProjectsIdentity, taskId: String, input: UpdateBusinessTaskInput) =
        authorized(identity) {
            BusinessProjectsRules.validate(input)
            store.updateTask(identity, taskId, input.normalized())
        }

    fun createLabel(identity: BusinessProjectsIdentity, input: CreateBusinessTaskLabelInput) =
        authorized(identity) {
            BusinessProjectsRules.validate(input)
            store.createLabel(identity, input.copy(name = input.name.trim(), colorHex = input.colorHex.uppercase()))
        }

    fun updateLabel(identity: BusinessProjectsIdentity, labelId: String, input: UpdateBusinessTaskLabelInput) =
        authorized(identity) {
            BusinessProjectsRules.validate(input)
            store.updateLabel(identity, labelId, input.copy(name = input.name.trim(), colorHex = input.colorHex.uppercase()))
        }

    fun archiveLabel(identity: BusinessProjectsIdentity, labelId: String, expectedVersion: Int) =
        authorized(identity) {
            BusinessProjectsRules.version(expectedVersion)
            store.archiveLabel(identity, labelId, expectedVersion)
        }

    fun placeTask(
        identity: BusinessProjectsIdentity,
        taskId: String,
        input: BusinessTaskBoardPlacementInput,
    ) = authorized(identity) {
        BusinessProjectsRules.validate(input)
        store.placeTask(identity, taskId, input)
    }

    fun removeTaskPlacement(identity: BusinessProjectsIdentity, taskId: String) =
        authorized(identity) { store.removeTaskPlacement(identity, taskId) }

    fun activeWorkSession(identity: BusinessProjectsIdentity) =
        authorized(identity) { store.activeWorkSession(identity) }

    fun taskWorkSessions(identity: BusinessProjectsIdentity, taskId: String) =
        authorized(identity) { store.taskWorkSessions(identity, taskId) }

    fun startWorkSession(identity: BusinessProjectsIdentity, taskId: String) =
        authorized(identity) { store.startWorkSession(identity, taskId) }

    fun pauseWorkSession(identity: BusinessProjectsIdentity, sessionId: String) =
        authorized(identity) { store.pauseWorkSession(identity, sessionId) }

    fun resumeWorkSession(identity: BusinessProjectsIdentity, sessionId: String) =
        authorized(identity) { store.resumeWorkSession(identity, sessionId) }

    fun stopWorkSession(identity: BusinessProjectsIdentity, sessionId: String) =
        authorized(identity) { store.stopWorkSession(identity, sessionId) }

    fun checkpointWorkSession(identity: BusinessProjectsIdentity, sessionId: String) =
        authorized(identity) { store.checkpointWorkSession(identity, sessionId) }

    fun metrics(clientId: String) = store.metrics(clientId)

    private inline fun <T> authorized(identity: BusinessProjectsIdentity, block: () -> T): T {
        if (!accessGuard.hasActiveEntitlement(identity)) throw BusinessProjectsForbiddenException()
        return block()
    }
}

private fun BusinessProjectsQuery.validated(): BusinessProjectsQuery {
    require(query == null || query.length <= 160) { "Search query is invalid" }
    require(projectStatus == null || projectStatus in BusinessProjectStatus.entries.map { it.name }) { "Project status is invalid" }
    require(taskStatus == null || taskStatus in BusinessTaskStatus.entries.map { it.name }) { "Task status is invalid" }
    return copy(query = query?.trim()?.takeIf(String::isNotBlank))
}

private fun CreateBusinessProjectInput.normalized() = copy(
    name = name.trim(),
    description = description?.trim()?.takeIf(String::isNotBlank),
)

private fun UpdateBusinessProjectInput.normalized() = copy(
    name = name.trim(),
    description = description?.trim()?.takeIf(String::isNotBlank),
)

private fun CreateBusinessTaskInput.normalized() = copy(
    title = title.trim(),
    description = description?.trim()?.takeIf(String::isNotBlank),
    labelIds = labelIds.distinct(),
)

private fun UpdateBusinessTaskInput.normalized() = copy(
    title = title.trim(),
    description = description?.trim()?.takeIf(String::isNotBlank),
    labelIds = labelIds.distinct(),
)
