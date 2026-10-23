package com.requena.supportdesk.server.domain.repository

import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.model.ServerOrganizationInvitationSnapshot
import com.requena.supportdesk.server.domain.model.ServerOrganizationMemberSnapshot
import com.requena.supportdesk.server.domain.model.ServerOrganizationSnapshot
import com.requena.supportdesk.server.domain.model.UpdateOrganizationRequest
import java.time.Instant

/** Companies, their members and invitations (phase 2). Permission checks live in the service. */
interface OrganizationRepository {
    /** Creates company + ADMIN user + OWNER membership + legal acceptances in one transaction. */
    fun signupOrganization(
        name: String,
        email: String,
        password: String,
        organizationName: String,
        legalVersion: String,
    ): ServerAuthIdentity

    fun organization(organizationId: String): ServerOrganizationSnapshot
    fun updateOrganization(organizationId: String, request: UpdateOrganizationRequest): ServerOrganizationSnapshot
    fun organizationMembers(organizationId: String): List<ServerOrganizationMemberSnapshot>

    /** Changes a member's role; refuses to leave the company without an active OWNER. */
    fun updateOrganizationMemberRole(organizationId: String, userId: String, role: String): ServerOrganizationMemberSnapshot

    /** Deactivates the member's account and revokes its sessions; never removes the last OWNER. */
    fun deactivateOrganizationMember(organizationId: String, userId: String)

    fun createOrganizationInvitation(
        organizationId: String,
        role: String,
        createdByUserId: String,
        codeHash: String,
        expiresAt: Instant,
    ): ServerOrganizationInvitationSnapshot
    fun organizationInvitations(organizationId: String): List<ServerOrganizationInvitationSnapshot>
    fun revokeOrganizationInvitation(organizationId: String, invitationId: String)

    /** Company id and role of a pending, unexpired invitation, or null. Does not consume it. */
    fun pendingInvitationTarget(codeHash: String): Pair<String, String>?

    /** Consumes a pending invitation and creates the staff user with the invitation's role. */
    fun acceptOrganizationInvitation(
        codeHash: String,
        name: String,
        email: String,
        password: String,
        legalVersion: String,
    ): ServerAuthIdentity
}
