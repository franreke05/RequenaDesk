package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateOrganizationInvitationRequest
import com.requena.supportdesk.server.domain.model.JoinOrganizationRequest
import com.requena.supportdesk.server.domain.model.ServerOrganizationInvitationSnapshot
import com.requena.supportdesk.server.domain.model.ServerOrganizationMemberSnapshot
import com.requena.supportdesk.server.domain.model.ServerOrganizationSnapshot
import com.requena.supportdesk.server.domain.model.SignupRequest
import com.requena.supportdesk.server.domain.model.UpdateOrganizationMemberRequest
import com.requena.supportdesk.server.domain.model.UpdateOrganizationRequest
import com.requena.supportdesk.server.domain.service.OrganizationService
import com.requena.supportdesk.server.plugins.AuthRateLimit
import com.requena.supportdesk.server.plugins.SensitiveOperationRateLimit
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.messageJson
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.sessionJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Self-service signup and invitation join (public, rate limited) plus company management
 * under /org. Role rules (owner/admin manage, accountant read-only) live in OrganizationService
 * and in requireAuthenticatedIdentity.
 */
fun Route.organizationRoutes(service: OrganizationService, tokenService: SupportDeskTokenService) {
    route("/auth") {
        // Lets the app show "Crear cuenta de empresa" only when the server accepts signups.
        get("/signup/status") {
            call.respondJson(
                body = successResponse("/auth/signup/status", buildJsonObject { put("enabled", service.signupEnabled) }),
            )
        }
        rateLimit(AuthRateLimit) {
            post("/signup") {
                if (!service.signupEnabled) {
                    return@post call.respondJson(HttpStatusCode.NotFound, errorResponse("Signup is not available"))
                }
                val session = service.signup(call.receiveOrDefault(SignupRequest()))
                call.respondJson(HttpStatusCode.Created, successResponse("/auth/signup", sessionJson(session)))
            }
            post("/join") {
                val session = service.join(call.receiveOrDefault(JoinOrganizationRequest()))
                call.respondJson(HttpStatusCode.Created, successResponse("/auth/join", sessionJson(session)))
            }
        }
    }

    route("/org") {
        get {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@get
            call.respondJson(body = successResponse("/org", organizationJson(service.organization(identity))))
        }
        patch {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@patch
            val request = call.receiveOrDefault(UpdateOrganizationRequest())
            call.respondJson(body = successResponse("/org", organizationJson(service.updatedOrganization(identity, request))))
        }

        get("/members") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@get
            call.respondJson(
                body = successResponse("/org/members", buildJsonArray { service.members(identity).forEach { add(memberJson(it)) } }),
            )
        }
        patch("/members/{userId}") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@patch
            val userId = call.parameters["userId"].orEmpty()
            val request = call.receiveOrDefault(UpdateOrganizationMemberRequest())
            call.respondJson(body = successResponse("/org/members/$userId", memberJson(service.updatedMember(identity, userId, request))))
        }
        delete("/members/{userId}") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@delete
            val userId = call.parameters["userId"].orEmpty()
            service.deactivatedMember(identity, userId)
            call.respondJson(body = successResponse("/org/members/$userId", messageJson("Member deactivated.")))
        }

        get("/invitations") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@get
            call.respondJson(
                body = successResponse(
                    "/org/invitations",
                    buildJsonArray { service.invitations(identity).forEach { add(invitationJson(it)) } },
                ),
            )
        }
        rateLimit(SensitiveOperationRateLimit) {
            post("/invitations") {
                val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@post
                val created = service.createdInvitation(identity, call.receiveOrDefault(CreateOrganizationInvitationRequest()))
                call.respondJson(
                    HttpStatusCode.Created,
                    successResponse(
                        "/org/invitations",
                        buildJsonObject {
                            put("invitation", invitationJson(created.invitation))
                            // Shown once: only its hash is stored.
                            put("code", created.code)
                        },
                    ),
                )
            }
        }
        delete("/invitations/{invitationId}") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@delete
            val invitationId = call.parameters["invitationId"].orEmpty()
            service.revokedInvitation(identity, invitationId)
            call.respondJson(body = successResponse("/org/invitations/$invitationId", messageJson("Invitation revoked.")))
        }
    }
}

private fun organizationJson(organization: ServerOrganizationSnapshot) = buildJsonObject {
    put("id", organization.id)
    put("name", organization.name)
    put("legalForm", organization.legalForm)
    put("sector", organization.sector)
    put("teamSize", organization.teamSize)
    put("hasAccountant", organization.hasAccountant)
    put("onboardingGoals", JsonArray(organization.onboardingGoals.map(::JsonPrimitive)))
    put("onboardingCompletedAt", organization.onboardingCompletedAt)
    put("onboardingCompleted", organization.onboardingCompletedAt != null)
    put("legalName", organization.legalName)
    put("taxId", organization.taxId)
    put("addressLine", organization.addressLine)
    put("postalCode", organization.postalCode)
    put("city", organization.city)
    put("province", organization.province)
    put("country", organization.country)
    put("createdAt", organization.createdAt)
}

private fun memberJson(member: ServerOrganizationMemberSnapshot) = buildJsonObject {
    put("userId", member.userId)
    put("name", member.name)
    put("email", member.email)
    put("role", member.role)
    put("isActive", member.isActive)
    put("joinedAt", member.joinedAt)
}

private fun invitationJson(invitation: ServerOrganizationInvitationSnapshot) = buildJsonObject {
    put("id", invitation.id)
    put("role", invitation.role)
    put("status", invitation.status())
    put("createdByName", invitation.createdByName)
    put("createdAt", invitation.createdAt)
    put("expiresAt", invitation.expiresAt)
    put("acceptedAt", invitation.acceptedAt)
    put("revokedAt", invitation.revokedAt)
}
