package com.requena.supportdesk.features.organization.data

import com.requena.supportdesk.core.model.CreatedInvitation
import com.requena.supportdesk.core.model.InvitationStatus
import com.requena.supportdesk.core.model.LegalForm
import com.requena.supportdesk.core.model.OnboardingGoal
import com.requena.supportdesk.core.model.Organization
import com.requena.supportdesk.core.model.OrganizationInvitation
import com.requena.supportdesk.core.model.OrganizationMember
import com.requena.supportdesk.core.model.OrganizationRole
import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.requireSuccess
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.core.result.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

// --- DTOs (match server/routes/OrganizationRoutes.kt) -------------------------------------

@Serializable
data class OrganizationDto(
    val id: String,
    val name: String,
    val legalForm: String? = null,
    val sector: String? = null,
    val teamSize: String? = null,
    val hasAccountant: Boolean? = null,
    val onboardingGoals: List<String> = emptyList(),
    val onboardingCompleted: Boolean = false,
    val legalName: String? = null,
    val taxId: String? = null,
    val addressLine: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val province: String? = null,
    val country: String? = null,
    val localPdfInvoices: Boolean = false,
)

@Serializable
data class OrganizationMemberDto(
    val userId: String,
    val name: String,
    val email: String,
    val role: String,
    val isActive: Boolean = true,
)

@Serializable
data class OrganizationInvitationDto(
    val id: String,
    val role: String,
    val status: String,
    val createdByName: String = "",
    val expiresAt: String = "",
)

@Serializable
data class CreatedInvitationDto(
    val invitation: OrganizationInvitationDto,
    val code: String,
)

/** Partial update: null fields are not sent and keep their value on the server. */
@Serializable
data class OrganizationUpdate(
    val name: String? = null,
    val legalForm: String? = null,
    val sector: String? = null,
    val teamSize: String? = null,
    val hasAccountant: Boolean? = null,
    val onboardingGoals: List<String>? = null,
    val onboardingCompleted: Boolean? = null,
    val legalName: String? = null,
    val taxId: String? = null,
    val addressLine: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val province: String? = null,
    val country: String? = null,
)

@Serializable
private data class RoleBody(val role: String)

fun OrganizationDto.toModel() = Organization(
    id = id,
    name = name,
    legalForm = legalForm?.let { key -> LegalForm.entries.firstOrNull { it.name == key } },
    sector = sector.orEmpty(),
    teamSize = teamSize.orEmpty(),
    hasAccountant = hasAccountant,
    onboardingGoals = onboardingGoals.mapNotNull(OnboardingGoal::fromKey).toSet(),
    onboardingCompleted = onboardingCompleted,
    legalName = legalName.orEmpty(),
    taxId = taxId.orEmpty(),
    addressLine = addressLine.orEmpty(),
    postalCode = postalCode.orEmpty(),
    city = city.orEmpty(),
    province = province.orEmpty(),
    country = country ?: "ES",
    localPdfInvoices = localPdfInvoices,
)

private fun OrganizationMemberDto.toModel() = OrganizationMember(
    userId = userId,
    name = name,
    email = email,
    role = OrganizationRole.fromKey(role) ?: OrganizationRole.MEMBER,
    isActive = isActive,
)

private fun OrganizationInvitationDto.toModel() = OrganizationInvitation(
    id = id,
    role = OrganizationRole.fromKey(role) ?: OrganizationRole.MEMBER,
    status = InvitationStatus.fromKey(status),
    createdByName = createdByName,
    expiresAt = expiresAt,
)

// --- Repository -----------------------------------------------------------------------

interface OrganizationRepository {
    suspend fun organization(): AppResult<Organization>
    suspend fun update(update: OrganizationUpdate): AppResult<Organization>
    suspend fun members(): AppResult<List<OrganizationMember>>
    suspend fun changeRole(userId: String, role: OrganizationRole): AppResult<OrganizationMember>
    suspend fun deactivate(userId: String): AppResult<Unit>
    suspend fun invitations(): AppResult<List<OrganizationInvitation>>
    suspend fun invite(role: OrganizationRole): AppResult<CreatedInvitation>
    suspend fun revoke(invitationId: String): AppResult<Unit>
}

class RemoteOrganizationRepository(
    private val httpClient: HttpClient,
) : OrganizationRepository {
    private val base get() = "${supportDeskBaseUrl()}/org"

    override suspend fun organization() = call { httpClient.get(base).requireApiData<OrganizationDto>().toModel() }

    override suspend fun update(update: OrganizationUpdate) = call {
        httpClient.patch(base) { setBody(jsonRequestBody(update)) }.requireApiData<OrganizationDto>().toModel()
    }

    override suspend fun members() = call {
        httpClient.get("$base/members").requireApiData<List<OrganizationMemberDto>>().map { it.toModel() }
    }

    override suspend fun changeRole(userId: String, role: OrganizationRole) = call {
        httpClient.patch("$base/members/$userId") { setBody(jsonRequestBody(RoleBody(role.name))) }
            .requireApiData<OrganizationMemberDto>().toModel()
    }

    override suspend fun deactivate(userId: String) = call { httpClient.delete("$base/members/$userId").requireSuccess() }

    override suspend fun invitations() = call {
        httpClient.get("$base/invitations").requireApiData<List<OrganizationInvitationDto>>().map { it.toModel() }
    }

    override suspend fun invite(role: OrganizationRole) = call {
        val created = httpClient.post("$base/invitations") { setBody(jsonRequestBody(RoleBody(role.name))) }
            .requireApiData<CreatedInvitationDto>()
        CreatedInvitation(created.invitation.toModel(), created.code)
    }

    override suspend fun revoke(invitationId: String) = call {
        httpClient.delete("$base/invitations/$invitationId").requireSuccess()
    }

    private suspend fun <T> call(block: suspend () -> T): AppResult<T> =
        try {
            AppResult.Success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppResult.Error(organizationMessage(error.message.orEmpty()), error)
        }
}

/** Spanish text for the server's company errors (kept pure for tests). */
fun organizationMessage(raw: String): String {
    val normalized = raw.lowercase()
    return when {
        "read-only" in normalized -> "Tu rol de gestoría es de solo lectura: no puedes hacer cambios."
        "only owners and admins" in normalized -> "Solo los dueños y admins pueden gestionar la empresa."
        "only an owner" in normalized -> "Solo un dueño puede cambiar o quitar a otro dueño."
        "at least one active owner" in normalized -> "La empresa necesita al menos un dueño activo."
        "member not found" in normalized -> "Esa persona no pertenece a tu empresa."
        "invitation not found" in normalized -> "La invitación ya no existe o ya se usó."
        "invalid company tax id" in normalized -> "El NIF/CIF de la empresa no es válido."
        "country must be" in normalized -> "El país debe ser un código de 2 letras (por ejemplo ES)."
        "unknown onboarding goal" in normalized -> "Hay una respuesta del onboarding que no se reconoce."
        raw.isBlank() -> "No se pudo completar la operación."
        else -> raw
    }
}
