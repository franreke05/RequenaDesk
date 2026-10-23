package com.requena.supportdesk.server.domain.service

import com.requena.supportdesk.server.domain.model.CreateOrganizationInvitationRequest
import com.requena.supportdesk.server.domain.model.JoinOrganizationRequest
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.model.ServerForbiddenException
import com.requena.supportdesk.server.domain.model.ServerNotFoundException
import com.requena.supportdesk.server.domain.model.ServerOrganizationInvitationCreated
import com.requena.supportdesk.server.domain.model.ServerOrganizationInvitationSnapshot
import com.requena.supportdesk.server.domain.model.ServerOrganizationMemberSnapshot
import com.requena.supportdesk.server.domain.model.ServerOrganizationSnapshot
import com.requena.supportdesk.server.domain.model.ServerSession
import com.requena.supportdesk.server.domain.model.ServerValidationException
import com.requena.supportdesk.server.domain.model.SignupRequest
import com.requena.supportdesk.server.domain.model.UpdateOrganizationMemberRequest
import com.requena.supportdesk.server.domain.model.UpdateOrganizationRequest
import com.requena.supportdesk.server.domain.repository.OrganizationRepository
import com.requena.supportdesk.server.security.PasswordHasher
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** Company roles. Staff users are users.role = ADMIN; their company role lives here. */
object OrganizationRoles {
    const val OWNER = "OWNER"
    const val ADMIN = "ADMIN"
    const val MEMBER = "MEMBER"
    const val ACCOUNTANT = "ACCOUNTANT"

    val all = setOf(OWNER, ADMIN, MEMBER, ACCOUNTANT)

    /** Roles that can be given through an invitation (ownership is only transferred explicitly). */
    val invitable = setOf(ADMIN, MEMBER, ACCOUNTANT)

    /** Can manage the company: its data, members and invitations. */
    fun canManage(role: String?): Boolean = role == OWNER || role == ADMIN

    /** Gestoría: sees everything, changes nothing. */
    fun isReadOnly(role: String?): Boolean = role == ACCOUNTANT
}

/** Onboarding answers to "¿Qué quieres resolver?" (also drive the plan recommendation in phase 3). */
val ONBOARDING_GOALS = setOf("INVOICING", "WORK_TIME", "SUPPORT", "BOOKINGS", "DOCUMENTS")

/** Version of the (draft) terms and privacy policy accepted at signup; bump when they change. */
const val CURRENT_LEGAL_VERSION = "borrador-2026-10"

class OrganizationService(
    private val repository: OrganizationRepository,
    private val sessions: SupportDeskService,
    val signupEnabled: Boolean,
    private val clock: Clock = Clock.systemUTC(),
    // Lets the token check drop its cached view of a member right after a change.
    private val onMemberChanged: (userId: String) -> Unit = {},
    // Plan limits (phase 3); null in tests that do not care about plans.
    private val plans: OrganizationPlanService? = null,
) {
    // --- Public entry points (no token) -------------------------------------------------

    fun signup(request: SignupRequest): ServerSession {
        if (!signupEnabled) throw ServerNotFoundException("Signup is not available")
        val name = request.name.trim().requireText("name", max = 180)
        val organizationName = request.organizationName.trim().requireText("organizationName", max = 180)
        val email = request.email.trim().requireEmail()
        request.password.requirePassword()
        requireLegalAcceptance(request.acceptTerms, request.acceptPrivacy)
        val identity = repository.signupOrganization(name, email, request.password, organizationName, CURRENT_LEGAL_VERSION)
        return sessions.sessionFor(identity)
    }

    fun join(request: JoinOrganizationRequest): ServerSession {
        val code = normalizeInvitationCode(request.code)
        if (code.isBlank()) throw ServerValidationException("Invitation code is required")
        val name = request.name.trim().requireText("name", max = 180)
        val email = request.email.trim().requireEmail()
        request.password.requirePassword()
        requireLegalAcceptance(request.acceptTerms, request.acceptPrivacy)
        val codeHash = PasswordHasher.hashToken(code)
        // The invitation itself counted as a seat while pending, so it may take that seat now;
        // the check stops joins that would exceed the plan after a downgrade.
        repository.pendingInvitationTarget(codeHash)?.let { (organizationId, role) ->
            plans?.ensureUserCapacityForJoin(organizationId, role)
        }
        val identity = repository.acceptOrganizationInvitation(
            codeHash = codeHash,
            name = name,
            email = email,
            password = request.password,
            legalVersion = CURRENT_LEGAL_VERSION,
        )
        return sessions.sessionFor(identity)
    }

    // --- Company data -------------------------------------------------------------------

    /** Every staff member can read the company (name, onboarding state); portal users cannot. */
    fun organization(identity: ServerAuthIdentity): ServerOrganizationSnapshot {
        requireStaff(identity)
        return repository.organization(identity.tenant())
    }

    fun updatedOrganization(identity: ServerAuthIdentity, request: UpdateOrganizationRequest): ServerOrganizationSnapshot {
        requireManager(identity)
        return repository.updateOrganization(identity.tenant(), request.validated())
    }

    // --- Members ------------------------------------------------------------------------

    fun members(identity: ServerAuthIdentity): List<ServerOrganizationMemberSnapshot> {
        requireManager(identity)
        return repository.organizationMembers(identity.tenant())
    }

    fun updatedMember(
        identity: ServerAuthIdentity,
        userId: String,
        request: UpdateOrganizationMemberRequest,
    ): ServerOrganizationMemberSnapshot {
        requireManager(identity)
        val role = request.role.trim().uppercase()
        if (role !in OrganizationRoles.all) throw ServerValidationException("Unknown company role")
        val target = repository.organizationMembers(identity.tenant()).firstOrNull { it.userId == userId }
            ?: throw ServerNotFoundException("Member not found")
        // Only an owner can grant or take away ownership.
        if ((target.role == OrganizationRoles.OWNER || role == OrganizationRoles.OWNER) &&
            identity.organizationRole != OrganizationRoles.OWNER
        ) {
            throw ServerForbiddenException("Only an owner can change who owns the company")
        }
        return repository.updateOrganizationMemberRole(identity.tenant(), userId, role).also { onMemberChanged(userId) }
    }

    fun deactivatedMember(identity: ServerAuthIdentity, userId: String) {
        requireManager(identity)
        val target = repository.organizationMembers(identity.tenant()).firstOrNull { it.userId == userId }
            ?: throw ServerNotFoundException("Member not found")
        if (target.role == OrganizationRoles.OWNER && identity.organizationRole != OrganizationRoles.OWNER) {
            throw ServerForbiddenException("Only an owner can remove another owner")
        }
        repository.deactivateOrganizationMember(identity.tenant(), userId)
        onMemberChanged(userId)
    }

    // --- Invitations --------------------------------------------------------------------

    fun createdInvitation(
        identity: ServerAuthIdentity,
        request: CreateOrganizationInvitationRequest,
    ): ServerOrganizationInvitationCreated {
        requireManager(identity)
        val role = request.role.trim().uppercase()
        if (role !in OrganizationRoles.invitable) throw ServerValidationException("Invitations can grant ADMIN, MEMBER or ACCOUNTANT")
        plans?.ensureUserCapacity(identity.tenant(), role)
        val code = generateInvitationCode()
        val invitation = repository.createOrganizationInvitation(
            organizationId = identity.tenant(),
            role = role,
            createdByUserId = identity.userId,
            codeHash = PasswordHasher.hashToken(code),
            expiresAt = Instant.now(clock).plus(INVITATION_LIFETIME),
        )
        return ServerOrganizationInvitationCreated(invitation, code)
    }

    fun invitations(identity: ServerAuthIdentity): List<ServerOrganizationInvitationSnapshot> {
        requireManager(identity)
        return repository.organizationInvitations(identity.tenant())
    }

    fun revokedInvitation(identity: ServerAuthIdentity, invitationId: String) {
        requireManager(identity)
        repository.revokeOrganizationInvitation(identity.tenant(), invitationId)
    }

    // --- Rules --------------------------------------------------------------------------

    private fun requireStaff(identity: ServerAuthIdentity) {
        if (identity.role != "ADMIN") throw ServerForbiddenException("Only company staff can do this")
    }

    private fun requireManager(identity: ServerAuthIdentity) {
        requireStaff(identity)
        if (!OrganizationRoles.canManage(identity.organizationRole)) {
            throw ServerForbiddenException("Only owners and admins can manage the company")
        }
    }

    private fun ServerAuthIdentity.tenant(): String =
        organizationId?.takeIf(String::isNotBlank) ?: throw ServerForbiddenException("No company is linked to this user")

    private fun UpdateOrganizationRequest.validated(): UpdateOrganizationRequest {
        val legalForm = legalForm?.trim()?.uppercase()
        if (legalForm != null && legalForm !in setOf("AUTONOMO", "SOCIEDAD")) throw ServerValidationException("Invalid legal form")
        val goals = onboardingGoals?.map { it.trim().uppercase() }?.distinct()
        if (goals != null && !ONBOARDING_GOALS.containsAll(goals)) throw ServerValidationException("Unknown onboarding goal")
        val taxId = taxId?.uppercase()?.filter(Char::isLetterOrDigit)
        if (!taxId.isNullOrEmpty() && !TAX_ID_PATTERN.matches(taxId)) throw ServerValidationException("Invalid company tax id")
        val country = country?.trim()?.uppercase()
        if (!country.isNullOrEmpty() && !COUNTRY_PATTERN.matches(country)) throw ServerValidationException("Country must be a 2-letter code")
        return copy(
            name = name?.trim()?.requireText("name", max = 180),
            legalForm = legalForm,
            sector = sector?.trim()?.take(120),
            teamSize = teamSize?.trim()?.take(20),
            onboardingGoals = goals,
            legalName = legalName?.trim()?.take(180),
            taxId = taxId,
            addressLine = addressLine?.trim()?.take(240),
            postalCode = postalCode?.trim()?.take(12),
            city = city?.trim()?.take(120),
            province = province?.trim()?.take(120),
            country = country,
        )
    }

    private fun String.requireText(field: String, max: Int): String {
        if (isBlank()) throw ServerValidationException("$field is required")
        if (length > max) throw ServerValidationException("$field is too long")
        return this
    }

    private fun String.requireEmail(): String {
        if (!EMAIL_PATTERN.matches(this) || length > 254) throw ServerValidationException("A valid email is required")
        return lowercase()
    }

    private fun String.requirePassword() {
        if (length < MINIMUM_PASSWORD_LENGTH) {
            throw ServerValidationException("Password must contain at least $MINIMUM_PASSWORD_LENGTH characters")
        }
    }

    private fun requireLegalAcceptance(terms: Boolean, privacy: Boolean) {
        if (!terms || !privacy) throw ServerValidationException("Terms and privacy policy must be accepted")
    }

    private fun normalizeInvitationCode(raw: String): String = raw.trim().uppercase()

    private fun generateInvitationCode(): String = buildString {
        append("INV-")
        repeat(3) { group ->
            repeat(4) { append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]) }
            if (group < 2) append('-')
        }
    }

    private companion object {
        const val MINIMUM_PASSWORD_LENGTH = 12
        const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val INVITATION_LIFETIME: Duration = Duration.ofDays(7)
        val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        val TAX_ID_PATTERN = Regex("^[A-Z0-9]{5,20}$")
        val COUNTRY_PATTERN = Regex("^[A-Z]{2}$")
        val random = SecureRandom()
    }
}
